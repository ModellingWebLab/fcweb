package uk.ac.ox.cs.chaste.fc.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperimentVersion;
import uk.ac.ox.cs.chaste.fc.beans.ChasteFile;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteEntityManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteFileManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ChastePermissionException;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.ExperimentManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ModelManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ProtocolManager;
import uk.ac.ox.cs.chaste.fc.mgmt.Tools;
import uk.ac.ox.cs.chaste.fc.mgmt.UserManager;
import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineFormats;

@MultipartConfig
public class FileTransfer extends WebModule
{
  private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
	private static final long CACHE_TIME = 60 * 60 * 24 * 365; // one year
	public static final SimpleDateFormat dateFormater = new SimpleDateFormat ("EEE, d MMM yyyy HH:mm:ss Z");	

	public FileTransfer () throws NamingException, SQLException
	{
		super ();
		fileTransfer = true;
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		// TODO unauthed users can see some file. and authed users cannot see all files -> better rights management
		//if (!user.isAuthorized ())
			//return errorPage (request, response, null);
		
			// req[2] = m=model p=protocol e=experiment
			// req[3] = entity name
			// req[4] = entity id
			// req[5] = file id
			// req[6] = file name
		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		
		if (req.length == 3 && req[2].equals ("cleanUP"))
		{
			
			cleanUp (db, notifications, userMgmt, user);
			return "Index.jsp";
		}
		
		if (req.length != 7)
			return errorPage (request, response, null);
		
		System.out.println ("a");
		
		int entityId = -1;
		int fileId = -1;
		boolean archive = false;
		try
		{
			entityId = Integer.parseInt (req[4]);
			if (req[5].equals ("a"))
				archive = true;
			else
				fileId = Integer.parseInt (req[5]);
		}
		catch (NullPointerException | NumberFormatException e)
		{
			LOGGER.warn ("user provided unparsebale ids. download impossible: " + req[4] + " / " + req[5]);
			return errorPage (request, response, null);
		}
		System.out.println ("b " + entityId + " " + fileId);
		
		if (entityId < 0 || (fileId < 0 && !archive))
			return errorPage (request, response, null);
		
		System.out.println ("b2");

		if (req[2].equals ("m"))
			return passEntity (request, response, db, notifications, entityId, archive, fileId, new ModelManager (db, notifications, userMgmt, user));
		else if (req[2].equals ("p"))
			return passEntity (request, response, db, notifications, entityId, archive, fileId, new ProtocolManager (db, notifications, userMgmt, user));
		else if (req[2].equals ("e"))
			return passEntity (request, response, db, notifications, entityId, archive, fileId, new ExperimentManager (db, notifications, userMgmt, user, new ModelManager (db, notifications, userMgmt, user), new ProtocolManager (db, notifications, userMgmt, user)));

		System.out.println ("c");
		// nothing of the above?
		return errorPage (request, response, null);
		
	}
	
	private String passEntity (HttpServletRequest request, HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, int entityId, boolean archive, int fileId, ChasteEntityManager entityMgmt)
	{
		File file = null;
		String fileName = null;
		
		// get file from entity
		ChasteEntityVersion version = entityMgmt.getVersionById (entityId);
		System.out.println ("version: " + version);
		if (version == null)
			return errorPage (request, response, null);
		ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt);
		fileMgmt.getFiles (version, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
		System.out.println ("archive: " + archive);
		if (archive)
		{
			try
			{
				file = ChasteFileManager.createArchive (version, entityMgmt.getEntityStorageDir ());
				fileName = version.getEntity ().getName () + "--" + version.getVersion () + ".zip";
			}
			catch (Exception e)
			{
				e.printStackTrace();
				LOGGER.error ("cannot create combine archive of entity. (" + entityMgmt.getEntityColumn () + ")", e);
				return errorPage (request, response, "unable to create archive");
			}
		}
		else
		{
			ChasteFile f = version.getFileById (fileId);
			System.out.println ("f: " + f);
			if (f == null)
				return errorPage (request, response, null);
			
			fileName = f.getName ();
			file = new File (entityMgmt.getEntityStorageDir () + Tools.FILESEP + version.getFilePath () + Tools.FILESEP + f.getName ());
		}

		return passFile (request, response, file, fileName, fileId);
		
	}
	
	private String passFile (HttpServletRequest request, HttpServletResponse response, File file, String fileName, int fileId)
	{
		if (file == null || !file.exists () || !file.canRead () || !file.isFile ())
		{
			// whoops, that's our fault. shouldn't happen. hopefully.
			LOGGER.error ("unable to find file " + fileId + " in " + file + " (at least not in an expected form)");
			return errorPage (request, response, null);
		}
		
		try
		{
	    ServletContext context  = getServletConfig().getServletContext();
	    String mimetype = context.getMimeType(file.getName ());
	    
	    System.out.println ("mime: " + mimetype);
	    
	    if (mimetype == null)
	        mimetype = "application/octet-stream";
	    
	    String contentType = getServletContext().getMimeType(file.getName());
      if (contentType == null)
          contentType = "application/octet-stream";
	    
      response.reset();
      response.setBufferSize(DEFAULT_BUFFER_SIZE);
      response.setContentType(contentType);
      response.setHeader("Content-Length", String.valueOf(file.length()));
      response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
      response.setHeader("Expires", dateFormater.format (new Date (System.currentTimeMillis () + CACHE_TIME * 1000)));
      response.setHeader("Cache-Control", "max-age="+ CACHE_TIME);
      response.setHeader("Last-Modified", dateFormater.format (file.lastModified ()));
      response.setHeader("ETag", Tools.hash (fileId + "-" + fileName));
      
	    BufferedInputStream input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
	    BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);

      // pass the stream to client
      byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
      int length;
      while ((length = input.read(buffer)) > 0)
      {
          output.write(buffer, 0, length);
      }
      
	    input.close();
	    output.close();
	    
			return null;
		}
		catch (IOException e)
		{
			// whoops, that's our fault. shouldn't happen. hopefully.
			e.printStackTrace ();
			LOGGER.error ("unable to dump file " + fileId + " in " + file + " (at least not in an expected form)");
		}
		return errorPage (request, response, null);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	private final static String buildMailBody (String nick, String result)
	{
		return "Hi " + nick + ",\n\nthe experiment you submitted is finished.\nReturn message: "
	+result
	+"\nHave a look at your files: "+Tools.getThisUrl ()+"myfiles.html"
	+ "\n\nSincerely,\nChaste dev-team";
	}
	
	
	

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, 
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException, ChastePermissionException
	{
		JSONObject answer = new JSONObject();
		
		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		
		System.out.println (req.length);
		for (String s : req)
			System.out.println (s);

		if (req != null && req.length == 2 && req[1].equals ("submitExperiment.html"))
		{
			//try
			//{
				String signature = request.getParameter ("signature");
				if (signature == null || signature.length () < 1)
				{
					answer.put ("error", "missing signature");
					return answer;
				}
				
				System.out.println ("signature: " + signature);

				String preRole = user.getRole ();
				String preMail = user.getRole ();
				user.setRole (User.ROLE_ADMIN);
				user.setMail ("somemail");
				
				ExperimentManager expMgmt = new ExperimentManager (db, notifications, userMgmt, user, new ModelManager (db, notifications, userMgmt, user), new ProtocolManager (db, notifications, userMgmt, user));
				

				System.out.println ("searching for experiment");
				ChasteExperimentVersion exp = expMgmt.getRunningExperiment (signature.trim ());
				if (exp == null || !exp.getStatus ().equals (ChasteExperimentVersion.STATUS_RUNNING))
				{
					System.out.println ("no experiment found");
					answer.put ("error", "invalid signature");
					return answer;
				}
				
				
				System.out.println ("exp: " + exp);
				
				String returnmsg = request.getParameter ("returnmsg");
				if (returnmsg == null)
					returnmsg = "finished";
				String returntype = request.getParameter ("returntype");
				if (returntype == null)
					returntype = "success";
				boolean returnType = returntype.trim ().equals ("success");
				
				System.out.println ("supp: " + returnmsg + " -- " + returntype + " --> " + returnType);
				
				
				user.setRole (preRole);
				user.setMail (preMail);
				User u = exp.getAuthor ();
				try
				{
					if (u.isSendMails ())
						Tools.sendMail (u.getMail (), u.getNick (), "Chaste Experiment finished", buildMailBody (u.getNick (), returnmsg));
				}
				catch (MessagingException e)
				{
					LOGGER.error ("couldn't send mail to user (exp finished)", e);
				}
				
				File destination = new File (expMgmt.getEntityStorageDir () + File.separator + signature);
				destination.mkdirs ();
				if (!destination.exists () || !destination.isDirectory ())
				{
					LOGGER.error ("cannot write returning experiment to " + destination.getAbsolutePath ());
					answer.put ("error", "cannot write to " + destination.getAbsolutePath ());
					exp.updateExperiment (expMgmt, "cannot write to " + destination.getAbsolutePath (), ChasteExperimentVersion.STATUS_FAILED);
					return answer;
				}

				System.out.println ("dest: " + destination);
				
				Part expPart = null;
				try
				{
					expPart = request.getPart("experiment");
				}
				catch (IOException | ServletException e)
				{
					LOGGER.warn ("wasn't able to find an attachment", e);
					e.printStackTrace ();
				}
				
				if (expPart != null)
				{
					File tmp = File.createTempFile ("chasteIncommingExperiment", ".combineArchive");
					expPart.write (tmp.getAbsolutePath ());
					
					System.out.println ("tmp: " + tmp);
					
					try
					{
						CombineArchive ca = CombineArchive.readArchive (tmp, destination);
						Collection<ArchiveEntry> entries = ca.getEntries ();
						
						ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt);
						
						for (ArchiveEntry entry : entries)
						{
							String format = entry.getFormat ();
							
							// lets drop the meta crap.
							// TODO: formats werden noch nicht aufgeloest....
							if (format.equals (CombineFormats.getFormatIdentifier ("omex")) || format.equals (CombineFormats.getFormatIdentifier ("manifest")))
								continue;
							
							// add the remaining to db
							int fileId = fileMgmt.addFile (entry.getRelativeName (), CombineFormats.getFormatFromIdentifier (format), exp.getAuthor (), entry.getFile ().length (), false);
							if (fileId < 0)
							{
								// TODO: cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
								LOGGER.error ("error inserting experiment file to db: " + entry.getRelativeName ());
								answer.put ("error", "couldn't insert into db");
								exp.updateExperiment (expMgmt, "error inserting experiment file to db: " + entry.getRelativeName (), ChasteExperimentVersion.STATUS_FAILED);
								return answer;
							}
							
							// associate files+exp
							if (!fileMgmt.associateFile (fileId, exp, expMgmt))
							{
								// TODO: cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
								LOGGER.error ("error inserting experiment to db (association failed): " + entry.getRelativeName ());
								answer.put ("error", "couldn't insert into db");
								exp.updateExperiment (expMgmt, "error inserting experiment to db (association failed): " + entry.getRelativeName (), ChasteExperimentVersion.STATUS_FAILED);
								return answer;
							}
						}
						
						File output = new File (destination.getAbsolutePath () + File.separator + "stdout.txt");
						if (output.exists () && output.canRead ())
						{
							int fileId = fileMgmt.addFile ("stdout.txt", "text/plain", exp.getAuthor (), output.length (), false);
							if (fileId < 0)
							{
								// TODO: cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
								LOGGER.error ("error inserting experiment output file to db: " + output.getAbsolutePath ());
								answer.put ("error", "couldn't insert output into db");
							}
							
							// associate files+exp
							if (!fileMgmt.associateFile (fileId, exp, expMgmt))
							{
								// TODO: cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
								LOGGER.error ("error inserting experiment output to db (association failed): " + output.getAbsolutePath ());
								answer.put ("error", "couldn't insert output into db");
							}
						}
						
						exp.updateExperiment (expMgmt, returnmsg, returnType ? ChasteExperimentVersion.STATUS_SUCCESS : ChasteExperimentVersion.STATUS_FAILED);
						answer.put ("experiment", "ok");
					}
					catch (Exception e)
					{
						e.printStackTrace();
						LOGGER.error ("error returning experiment", e);
						answer.put ("experiment", "failed");
						if (returnmsg.equals ("finished"))
							exp.updateExperiment (expMgmt, "error reading archive", ChasteExperimentVersion.STATUS_FAILED);
						else
							exp.updateExperiment (expMgmt, "error reading archive: " + returnmsg, ChasteExperimentVersion.STATUS_FAILED);
					}
					
					
					
					
					
					
					// inform the user
				}
				else
				{
					// delete from db
					// inform the user -> failed/succeeded
					exp.updateExperiment (expMgmt, returnmsg + " (backend returned no archive)", returnType ? ChasteExperimentVersion.STATUS_SUCCESS : ChasteExperimentVersion.STATUS_FAILED);
					
					//TODO write to db
					answer.put ("error", "no archive found");
				}
				return answer;
			/*}
			catch (ServletException e1)
			{
				e1.printStackTrace();
				answer.put ("error", "servlet error");
				return answer;
			}*/
		}
		
		
		
		if (!user.isAuthorized () || !user.isAllowedToUpload ())
			throw new ChastePermissionException ("not allowed.");
		
		// TODO: delete
		/*if (Math.random () > 0.5)
			throw new IOException ("you just ran into a random fail");*/
		
		try
		{
			Part filePart = request.getPart("file");
			// String filename = extractFileName (filePart);
			File tmpDir = new File (Tools.getTempDir ());
			if (!tmpDir.exists ())
				if (!tmpDir.mkdirs ())
					throw new IOException ("cannot create temp dir for file upload");
			
			File tmpFile = null;
			String tmpName = null;
			while (true)
			{
				tmpName = UUID.randomUUID().toString();
				tmpFile = new File (tmpDir.getAbsolutePath () + Tools.FILESEP + tmpName);
				if (!tmpFile.exists ())
					break;
			}
			
	    filePart.write (tmpFile.getAbsolutePath ());
	    
	    if (tmpFile.exists ())
	    {
				JSONObject res = new JSONObject ();
				res.put ("response", true);
				res.put ("tmpName", tmpName);
				answer.put ("upload", res);
	    }
	    	
		}
		catch (ServletException e)
		{
			e.printStackTrace();
			LOGGER.error ("Error storing uploaded file", e);
			throw new IOException ("file cannot be uploaded.");
		}
		
		return answer;
	}
	
	
	protected static final String extractFileName (Part part)
	{
		String[] items = part.getHeader ("content-disposition").split (";");
		for (String s : items)
			if (s.trim ().startsWith ("filename"))
				return s.substring (s.indexOf ("=") + 2, s.length () - 1);
		return "";
	}

	public static File getTempFile (String tmpName)
	{
		File tmpDir = new File (Tools.getTempDir () + Tools.FILESEP + tmpName);
		if (tmpDir.exists () && tmpDir.isFile () && tmpDir.canRead ())
			return tmpDir;
		return null;
	}
	
	public static void copyFile (NewFile sourceFile, File targetDirectory) throws IOException
	{
		if (sourceFile.name.contains ("/") || sourceFile.name.contains ("\\"))
			throw new IOException ("'/' and '\\' are not allowed in file names.");
		
		if (!targetDirectory.exists ())
			targetDirectory.mkdirs ();
		
		InputStream in = new FileInputStream (sourceFile.tmpFile);
    OutputStream out = new FileOutputStream (targetDirectory + Tools.FILESEP + sourceFile.name);
    
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0)
        out.write(buf, 0, len);
    in.close();
    out.close();
	}

	
	static public class NewFile
	{
		File tmpFile;
		String name;
		String type;
		int dbId;
		public NewFile (File tmpFile, String name, String type)
		{
			this.tmpFile = tmpFile;
			this.name = name;
			this.type = type;
			dbId = -1;
		}
	}
	
	public static boolean ambiguous (HashMap<String, NewFile> files)
	{
		// the number of files should always be quite small,
		// otherwise we need some other technique to find duplicates..
		List<String> f = new ArrayList<String> (files.keySet ());
		for (int i = 0; i < f.size (); i++)
			for (int j = i + 1; j < f.size (); j++)
			{
				NewFile a = files.get (f.get (i));
				NewFile b = files.get (f.get (j));
				if (a.name.equals (b.name))
					return true;
				if (a.tmpFile.equals (b.tmpFile))
					return true;
			}
		return false;
	}
	
	public static class SubmitResult
	{
		boolean result;
		String response;
		public SubmitResult (boolean result, String response)
		{
			this.result = result;
			this.response = response;
		}
		
	}
	
	public static SubmitResult submitExperiment (File model, File protocol, String signature) throws Exception
	{
		// the current chaste web service doesn't have a valid SSL cert. thus, the following will fail.
		// to import a self-signed cert to your keystore run smth like:
		// /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/keytool 
		//           -import -alias [SOME ALIAS]
		//           -file [CERT]
		//           -keystore [KEYSTORE]
		// keystore is usually $JAVAHOME/jre/lib/security/cacerts
    
	    HttpClient client = new DefaultHttpClient();
	    HttpPost post = new HttpPost(Tools.getChasteUrl ());
	    MultipartEntityBuilder builder = MultipartEntityBuilder.create();        
	    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

	    FileBody modelBody = new FileBody(model);
	    FileBody protocolBody = new FileBody(protocol);

	    builder.addPart("model", modelBody);
	    builder.addPart("protocol", protocolBody);
	    builder.addTextBody("signature", signature);
	    builder.addTextBody("callBack", Tools.getThisUrl () + "submitExperiment.html");
	    builder.addTextBody("password", Tools.getChastePassword ());
	    HttpEntity entity = builder.build();
	    ProgressiveEntity myEntity = new ProgressiveEntity(entity);

	    post.setEntity(myEntity);
	    HttpResponse response = client.execute(post);
	    String res = getContent (response);
	    System.out.println ("response: " + res);
            if (res.trim ().equals (signature + " succ"))
		return new SubmitResult (true, res.substring (signature.length ()).trim ());
            if (res.trim ().startsWith (signature))
            {
		LOGGER.error ("Chaste backend answered with !succ: " + res);
		return new SubmitResult (false, res.substring (signature.length ()).trim ());
            }
            LOGGER.error ("chaste backend answered w/ smth unexpected: " + res);
            throw new IOException ("Chaste backend response not expected.");
	} 

	private static String getContent(HttpResponse response) throws IOException
	{
	    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	    String body = "";
	    String content = "";

	    while ((body = rd.readLine()) != null)
	        content += body + "\n";
	    
	    return content.trim();
	}
	
	public static void scheduleCleanUp ()
	{
		new Thread () {
			public void run ()
			{
				try
				{
					URL url = new URL(Tools.getThisUrl () + "download/cleanUP");
					InputStream is = url.openStream();
					is.close ();
				}
				catch (IOException e)
				{
					LOGGER.warn ("couldn't schedule cleanup script", e);
				}
			}
		}.start ();
	}
	
	
	private void cleanUp (DatabaseConnector db, Notifications notifications, UserManager userMgmt, User user)
	{
		LOGGER.info ("cleaning");

		String preRole = user.getRole ();
		String preMail = user.getRole ();
		user.setRole (User.ROLE_ADMIN);
		user.setMail ("somemail");
		
		ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
		ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);
		ExperimentManager expMgmt = new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt);

		modelMgmt.deleteEmptyEntities ();
		protocolMgmt.deleteEmptyEntities ();
		expMgmt.deleteEmptyEntities ();
		
		int removedFiles = 0;
		
		// check if files are unused
		int maxAgeTmpFiles = 1000 * 60 * 60 * 24; //1d
		
		// -> tmp dir
		File dir = new File (Tools.getTempDir ());
		if (dir.exists ())
		{
			File [] files = dir.listFiles ();
			
			long curTime = System.currentTimeMillis ();
			
			for (File f : files)
				if (curTime - f.lastModified () > maxAgeTmpFiles) // more than 1d
				{
					LOGGER.info ("remove tmp file " + f.getAbsolutePath () + " because it's older than " + maxAgeTmpFiles + "ms");
					f.delete ();
					removedFiles++;
				}
		}
		
		// -> models/protocols/experiments
		dir = new File (modelMgmt.getEntityStorageDir ());
		File [] files = dir.listFiles ();
		for (File f : files)
		{
			// is there a model in that dir?
			if (modelMgmt.getVersionByPath (f.getName ()) == null)
				try
				{
					LOGGER.info ("remove file " + f.getAbsolutePath () + " because it's not in db");
					Tools.deleteRecursively (f, false);
					removedFiles++;
				}
				catch (IOException e)
				{
					LOGGER.error ("couldn't remove file " + f.getAbsolutePath () + " during clean up!");
				}
		}
		
		
		dir = new File (protocolMgmt.getEntityStorageDir ());
		files = dir.listFiles ();
		for (File f : files)
		{
			// is there a protocol in that dir?
			if (protocolMgmt.getVersionByPath (f.getName ()) == null)
				try
				{
					LOGGER.info ("remove file " + f.getAbsolutePath () + " because it's not in db");
					Tools.deleteRecursively (f, false);
					removedFiles++;
				}
				catch (IOException e)
				{
					LOGGER.error ("couldn't remove file " + f.getAbsolutePath () + " during clean up!");
				}
		}
		
		
		dir = new File (expMgmt.getEntityStorageDir ());
		files = dir.listFiles ();
		for (File f : files)
		{
			// is there a exp in that dir?
			if (expMgmt.getVersionByPath (f.getName ()) == null)
				try
				{
					LOGGER.info ("remove file " + f.getAbsolutePath () + " because it's not in db");
					Tools.deleteRecursively (f, false);
					removedFiles++;
				}
				catch (IOException e)
				{
					LOGGER.error ("couldn't remove file " + f.getAbsolutePath () + " during clean up!");
				}
		}

		LOGGER.info ("cleaning finished, removed " + removedFiles + " files");

		user.setRole (preRole);
		user.setMail (preMail);
	}
	
	static class ProgressiveEntity implements HttpEntity {
		HttpEntity entity;
		public ProgressiveEntity (HttpEntity entity)
		{
			this.entity = entity;
		}
		
    @Override
    public void consumeContent() throws IOException {
        //yourEntity.consumeContent();   
        EntityUtils.consume(entity);
    }
    @Override
    public InputStream getContent() throws IOException,
            IllegalStateException {
        return entity.getContent();
    }
    @Override
    public Header getContentEncoding() {             
        return entity.getContentEncoding();
    }
    @Override
    public long getContentLength() {
        return entity.getContentLength();
    }
    @Override
    public Header getContentType() {
        return entity.getContentType();
    }
    @Override
    public boolean isChunked() {             
        return entity.isChunked();
    }
    @Override
    public boolean isRepeatable() {
        return entity.isRepeatable();
    }
    @Override
    public boolean isStreaming() {             
        return entity.isStreaming();
    } // CONSIDER put a _real_ delegator into here!

    @Override
    public void writeTo(OutputStream outstream) throws IOException {

        class ProxyOutputStream extends FilterOutputStream {
            /**
             * @author Stephen Colebourne
             */

            public ProxyOutputStream(OutputStream proxy) {
                super(proxy);    
            }
            public void write(int idx) throws IOException {
                out.write(idx);
            }
            public void write(byte[] bts) throws IOException {
                out.write(bts);
            }
            public void write(byte[] bts, int st, int end) throws IOException {
                out.write(bts, st, end);
            }
            public void flush() throws IOException {
                out.flush();
            }
            public void close() throws IOException {
                out.close();
            }
        } // CONSIDER import this class (and risk more Jar File Hell)

        class ProgressiveOutputStream extends ProxyOutputStream {
            public ProgressiveOutputStream(OutputStream proxy) {
                super(proxy);
            }
            public void write(byte[] bts, int st, int end) throws IOException {

                // FIXME  Put your progress bar stuff here!

                out.write(bts, st, end);
            }
        }

        entity.writeTo(new ProgressiveOutputStream(outstream));
    }

};
}
