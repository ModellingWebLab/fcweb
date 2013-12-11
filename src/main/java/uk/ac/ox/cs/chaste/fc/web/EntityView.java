package uk.ac.ox.cs.chaste.fc.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.PageHeaderLink;
import uk.ac.ox.cs.chaste.fc.beans.PageHeaderScript;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteEntityManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteFileManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ChastePermissionException;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.ExperimentManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ModelManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ProtocolManager;
import uk.ac.ox.cs.chaste.fc.mgmt.Tools;
import uk.ac.ox.cs.chaste.fc.web.FileTransfer.NewFile;
import de.binfalse.bflog.LOGGER;

public class EntityView extends WebModule
{
	private final int TYPE_MODEL = 1;
	private final int TYPE_PROTOCOL = 2;
	private final int TYPE_EXPERIMENT = 4;
	
	private int type;

	public EntityView () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		// req[2] = entity name
		// req[3] = entity id
		// req[4] = version
		// req[5] = version id
		// req[6] = file
		// req[7] = action
		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		
		
		if (req == null || req.length < 3)
			return errorPage (request, response, null);

		
		ChasteEntityManager entityMgmt = null;
		if (req[1].equals ("model"))
		{
			type = TYPE_MODEL;
			request.setAttribute ("newentitytype", "Model");
			entityMgmt = new ModelManager (db, notifications, userMgmt, user);
		}
		else if (req[1].equals ("protocol"))
		{
			type = TYPE_PROTOCOL;
			request.setAttribute ("newentitytype", "Protocol");
			entityMgmt = new ProtocolManager (db, notifications, userMgmt, user);
		}
		else if (req[1].equals ("experiment"))
		{
			type = TYPE_EXPERIMENT;
			request.setAttribute ("newentitytype", "Experiment");
			entityMgmt = new ExperimentManager (db, notifications, userMgmt, user, new ModelManager (db, notifications, userMgmt, user), new ProtocolManager (db, notifications, userMgmt, user));
		}
		else
			return errorPage (request, response, null);
		
		
		if (req[2].equals ("createnew") && type != TYPE_EXPERIMENT)
		{
			if (user.isAuthorized ())
			{
				header.addScript (new PageHeaderScript ("res/js/upload.js", "text/javascript", "UTF-8", null));
				header.addScript (new PageHeaderScript ("res/js/entitynew.js", "text/javascript", "UTF-8", null));
				if (request.getParameter("newentityname") != null)
				{
					try
					{
						int entityId = Integer.parseInt (request.getParameter("newentityname"));
						ChasteEntity m = entityMgmt.getEntityById (entityId);
						request.setAttribute ("newentityname", m.getName ());
					}
					catch (NumberFormatException | NullPointerException e)
					{
						e.printStackTrace ();
						notifications.addError ("couldn't find desired "+entityMgmt.getEntityColumn ());
						LOGGER.warn ("user requested "+entityMgmt.getEntityColumn ()+" id " + request.getParameter("newentityname") + " is unparseable.");
					}
				}
				return "EntityNew.jsp";
			}
			else
				return errorPage (request, response, null);
		}
		
		if (req.length < 4)
			return errorPage (request, response, null);
		

		
		try
		{
			int entityID = Integer.parseInt (req[3]);
			ChasteEntity entity = entityMgmt.getEntityById (entityID);
			if (entity == null || entity.getVersions ().size () < 1)
			{
				notifications.addError ("no entity found");
				return errorPage (request, response, "no entity found");
			}
			
			Map<Integer, ChasteEntityVersion> versions = entity.getVersions ();
			ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt); 
			for (ChasteEntityVersion version : versions.values ())
				fileMgmt.getFiles (version, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
			
			request.setAttribute ("entity", entity);
			header.addScript (new PageHeaderScript ("res/js/3rd/showdown.js", "text/javascript", "UTF-8", null));
			header.addScript (new PageHeaderScript ("res/js/entity.js", "text/javascript", "UTF-8", null));
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace ();
			notifications.addError ("cannot find "+entityMgmt.getEntityColumn ());
			LOGGER.warn ("user requested "+entityMgmt.getEntityColumn ()+" id " + req[3] + " is unparseable.");
			return errorPage (request, response, null);
		}
		
		// version/file view/action will be done on client side

		Vector<String> plugins = new Vector<String> ();
		plugins.add ("displayContent");
		plugins.add ("displayTable");
		plugins.add ("displayPlotFlot");
		plugins.add ("displayPlotHC");
		plugins.add ("displayPlotD3");
		
		for (String s : plugins)
		{
			header.addScript (new PageHeaderScript ("res/js/visualizers/" + s + "/" + s + ".js", "text/javascript", "UTF-8", null));
			header.addLink (new PageHeaderLink ("res/js/visualizers/" + s + "/" + s + ".css", "text/css", "stylesheet"));
		}
		
		return "Entity.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, 
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException, ChastePermissionException
	{
		
		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		
		ChasteEntityManager entityMgmt = null;
		if (req[1].equals ("model"))
		{
			type = TYPE_MODEL;
			entityMgmt = new ModelManager (db, notifications, userMgmt, user);
		}
		else if (req[1].equals ("protocol"))
		{
			type = TYPE_PROTOCOL;
			entityMgmt = new ProtocolManager (db, notifications, userMgmt, user);
		}
		else if (req[1].equals ("experiment"))
		{
			type = TYPE_EXPERIMENT;
			entityMgmt = new ExperimentManager (db, notifications, userMgmt, user, new ModelManager (db, notifications, userMgmt, user), new ProtocolManager (db, notifications, userMgmt, user));
		}
		else
			throw new IOException ("nothing to do.");
		
		
		JSONObject answer = new JSONObject();
		
		Object task = querry.get ("task");
		if (task == null)
		{
			response.setStatus (HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException ("nothing to do.");
		}
		
		ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt); 
		
		if ((task.equals ("createNewEntity") || task.equals ("verifyNewEntity")) && type != TYPE_EXPERIMENT)
			createNewEntity (task, notifications, querry, user, answer, entityMgmt, fileMgmt);
		else if (task.equals ("getInfo"))
		{

			if (querry.get ("version") != null)
				getVersion (querry.get ("version"), notifications, answer, entityMgmt, fileMgmt, new ExperimentManager (db, notifications, userMgmt, user, new ModelManager (db, notifications, userMgmt, user), new ProtocolManager (db, notifications, userMgmt, user)));
			if (querry.get ("versionfile") != null)
				throw new IOException ("unsupported yet");
			
				//getFile (querry.get ("versionfile"), task, notifications, querry, user, answer, entityMgmt, fileMgmt);
		}
		else if (task.equals ("updateVisibility"))
		{
			answer.put ("updateVisibility", updateVisibility (querry.get ("version"), querry.get ("visibility"), notifications, answer, entityMgmt));
		}
		else if (task.equals ("deleteVersion"))
		{
			answer.put ("deleteVersion", deleteVersion (querry.get ("version"), notifications, answer, entityMgmt));
		}
		else if (task.equals ("deleteEntity"))
		{
			answer.put ("deleteEntity", deleteEntity (querry.get ("entity"), notifications, answer, entityMgmt));
		}
		
		
		
		return answer;
	}
	
	@SuppressWarnings("unchecked")
	private void getVersion (Object version, Notifications notifications, JSONObject answer, ChasteEntityManager entityMgmt, ChasteFileManager fileMgmt, ExperimentManager expMgmt) throws IOException
	{
		try
		{
			int versionId = Integer.parseInt (version.toString ());
			ChasteEntityVersion vers = entityMgmt.getVersionById (versionId);
			if (vers == null)
				notifications.addError ("no version found");
			else
			{
				fileMgmt.getFiles (vers, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
				expMgmt.getExperiments (vers, entityMgmt.getEntityColumn ());
				answer.put ("version", vers.toJson ());
				System.out.println (answer.toString ());
			}
		}
		catch (NullPointerException | NumberFormatException e)
		{
			e.printStackTrace ();
			LOGGER.warn ("user provided version id not parseable: " + version + " (type: " + entityMgmt.getEntityColumn () + ")");
			throw new IOException ("version not found");
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject updateVisibility (Object version, Object visibility, Notifications notifications, JSONObject answer, ChasteEntityManager entityMgmt) throws IOException
	{
		try
		{
			int versionId = Integer.parseInt (version.toString ());
			ChasteEntityVersion vers = entityMgmt.getVersionById (versionId);
			
			JSONObject obj = new JSONObject ();
			obj.put ("response", false);
			obj.put ("responseText", "wasn't able to update the entity");
			
			if (vers == null)
				notifications.addError ("no version found");
			else
			{
				if (entityMgmt.updateVisibility (vers, visibility.toString ()))
				{
					obj.put ("response", true);
					obj.put ("responseText", "successfully updated");
				}
				else
				{
					notifications.addError ("updating visibility failed");
				}
			}
			return obj;
		}
		catch (NullPointerException | NumberFormatException e)
		{
			e.printStackTrace ();
			LOGGER.warn ("user provided version id not parseable: " + version + " (type: " + entityMgmt.getEntityColumn () + ")");
			throw new IOException ("version not found");
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject deleteVersion (Object version, Notifications notifications, JSONObject answer, ChasteEntityManager entityMgmt) throws IOException, ChastePermissionException
	{
		try
		{
			int versionId = Integer.parseInt (version.toString ());
			ChasteEntityVersion vers = entityMgmt.getVersionById (versionId);
			
			JSONObject obj = new JSONObject ();
			obj.put ("response", false);
			obj.put ("responseText", "wasn't able to delete the entity version");
			
			if (vers == null)
				notifications.addError ("no version found");
			else
			{
				entityMgmt.removeVersion (versionId);
				obj.put ("response", true);
				obj.put ("responseText", "successfully deleted");
			}
			return obj;
		}
		catch (NullPointerException | NumberFormatException e)
		{
			e.printStackTrace ();
			LOGGER.warn ("user provided version id not parseable: " + version + " (type: " + entityMgmt.getEntityColumn () + ")");
			throw new IOException ("version not found");
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject deleteEntity (Object version, Notifications notifications, JSONObject answer, ChasteEntityManager entityMgmt) throws IOException, ChastePermissionException
	{
		try
		{
			int versionId = Integer.parseInt (version.toString ());
			ChasteEntity vers = entityMgmt.getEntityById (versionId);
			
			JSONObject obj = new JSONObject ();
			obj.put ("response", false);
			obj.put ("responseText", "wasn't able to delete the entity");
			
			if (vers == null)
				notifications.addError ("no version found");
			else
			{
				entityMgmt.removeEntity (versionId);
				obj.put ("response", true);
				obj.put ("responseText", "successfully deleted");
			}
			return obj;
		}
		catch (NullPointerException | NumberFormatException e)
		{
			e.printStackTrace ();
			LOGGER.warn ("user provided entity id not parseable: " + version + " (type: " + entityMgmt.getEntityColumn () + ")");
			throw new IOException ("version not found");
		}
	}
	
	
	/*@SuppressWarnings("unchecked")
	private void getFile (Object versionFile, Object task, Notifications notifications, JSONObject querry, User user, JSONObject answer, ModelManager modelMgmt, ChasteFileManager fileMgmt) throws IOException
	{
		try
		{
			int fileId = Integer.parseInt (versionFile.toString ());
			ChasteFile file = fileMgmt.getFileById (fileId);
			if (file == null)
				notifications.addError ("no file found");
			else
			{
				answer.put ("modelversionfile", file.toJson ());
			}
		}
		catch (NullPointerException | NumberFormatException e)
		{
			e.printStackTrace ();
			LOGGER.warn ("user provided file id not parseable: " + versionFile);
			throw new IOException ("file not found");
		}
	}*/
	
	
	@SuppressWarnings("unchecked")
	private void createNewEntity (Object task, Notifications notifications, JSONObject querry, User user, JSONObject answer, ChasteEntityManager entityMgmt, ChasteFileManager fileMgmt) throws IOException, ChastePermissionException
	{

		String entityName = null;
		String versionName = null;
		String filePath = null;
		File entityDir = null;
		HashMap<String, NewFile> files = new HashMap<String, NewFile> ();
		boolean createOk = task.equals ("createNewEntity");
		ChasteEntity entity = null;
		if (querry.get ("entityName") != null)
		{
			JSONObject obj = new JSONObject ();
			obj.put ("response", true);
			obj.put ("responseText", "nice name");
			answer.put ("entityName", obj);
			
			entityName = querry.get ("entityName").toString ().trim ();
			entity = entityMgmt.getEntityByName (entityName);
			if (entity == null)
			{
				if (entityName.length () < 2)
				{
					obj.put ("response", false);
					obj.put ("responseText", "needs to be at least 2 characters in length");
					createOk = false;
				}
				// else name ok
			}
			// else entity exists -> great, but warn
			else
			{
				if (entity.getAuthor ().getId () != user.getId ())
				{
					// not allowed to create a new version to a model of somebody else -> otherwise we get a delte problem...
					obj.put ("response", false);
					obj.put ("responseText", "name exists, but "+entityMgmt.getEntityColumn ()+" belongs to somebody else. please choose a different name.");
					createOk = false;
				}
				else
				{
					obj.put ("response", true);
					obj.put ("responseText", "name exists. you're going to upload a new version to an existing "+entityMgmt.getEntityColumn ()+".");
				}
			}
		}
		else
			createOk = false;
		
		if (querry.get ("versionName") != null)
		{
			JSONObject obj = new JSONObject ();
			obj.put ("response", true);
			obj.put ("responseText", "nice version identifier");
			answer.put ("versionName", obj);
			
			versionName = querry.get ("versionName").toString ().trim ();
			
			if (versionName.length () < 2)
			{
				obj.put ("response", false);
				obj.put ("responseText", "needs to be at least 5 characters in length");
				createOk = false;
			}
			// else ok
			

			if (entity != null)
			{
				if (entity.getVersion (versionName) != null)
				{
					obj.put ("response", false);
					obj.put ("responseText", "this "+entityMgmt.getEntityColumn ()+" already contains a version with that name");
					createOk = false;
				}
			}
			// else: new entity -> dont care about version names
		}
		else
			createOk = false;
		
		if (createOk)
		{
			// do we have any files?
			// creating an empty entities makes no sense..
			if (querry.get ("files") != null)
			{
				filePath = UUID.randomUUID ().toString ();
				String storageFir = type == TYPE_MODEL ? Tools.getModelStorageDir () : Tools.getProtocolStorageDir ();
				entityDir = new File (storageFir + Tools.FILESEP + filePath);
				while (entityDir.exists ())
				{
					filePath = UUID.randomUUID ().toString ();
					entityDir = new File (storageFir + Tools.FILESEP + filePath);
				}
				
				if (!entityDir.mkdirs ())
				{
					LOGGER.error ("cannot create dir: " + entityDir);
					throw new IOException ("cannot create directory");
				}
				
				JSONArray array= (JSONArray) querry.get ("files");
				for (int i = 0; i < array.size (); i++)
				{
					JSONObject file=(JSONObject) array.get (i);

					String tmpName = null;
					String name = null;
					String type = null;
					try
					{
						tmpName = file.get ("tmpName").toString ();
						name = file.get ("fileName").toString ();
						type = file.get ("fileType").toString ();
					}
					catch (NullPointerException e)
					{
						throw new IOException ("incomplete file information");
					}
					if (name == null || name.length () < 1)
					{
						LOGGER.warn ("user provided file name is empty or null.");
						throw new IOException ("detected empty file name. thats not allowed.");
					}
					if (name.contains ("/") || name.contains ("\\"))
					{
						LOGGER.warn ("user provided file name contains / or \\.");
						throw new IOException ("'/' or '\\' are not allowed in file names.");
					}
					if (type == null || type.length () < 1)
						type = "unknown";
					
					File tmp = FileTransfer.getTempFile (tmpName);
					if (tmp == null)
					{
						notifications.addError ("cannot find file " + name + ". please upload again.");
						createOk = false;
					}
					else
						files.put (tmpName, new NewFile (tmp, name, type));
				}
			}
			
			if (files.size () < 1)
			{
				createOk = false;
				notifications.addError ("no files chosen. empty "+entityMgmt.getEntityColumn ()+"s don't make much sense");
			}
			
			if (FileTransfer.ambiguous (files))
			{
				createOk = false;
				notifications.addError ("there was an error with the files. the provided information is ambiguous");
			}
		}
		

		if (task.equals ("createNewEntity") && !createOk)
		{
			JSONObject res = new JSONObject ();
			res.put ("response", false);
			res.put ("responseText", "failed due to previous errors");
			answer.put ("createNewEntity", res);
		}
		
		if (createOk)
		{
			// create a entity if it not yet exists
			int entityId = -1;
			if (entity == null)
				entityId = entityMgmt.createEntity (entityName, user);
			else
				entityId = entity.getId ();
			
			if (entityId < 0)
			{
				cleanUp (entityDir, -1, files, fileMgmt, entityMgmt);
				LOGGER.error ("error inserting/creating "+entityMgmt.getEntityColumn ()+" to db");
				throw new IOException ("wasn't able to create/insert "+entityMgmt.getEntityColumn ()+" to db.");
			}
			
			// create version
			int versionId = entityMgmt.createVersion (entityId, versionName, filePath, user);
			if (versionId < 0)
			{
				cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
				LOGGER.error ("error inserting/creating "+entityMgmt.getEntityColumn ()+" version to db");
				throw new IOException ("wasn't able to create/insert "+entityMgmt.getEntityColumn ()+" version to db.");
			}
			
			boolean extractReadme = entityMgmt.getEntityColumn () == "protocol";
			
			for (NewFile f : files.values ())
			{
				//copy file
				try
				{
					FileTransfer.copyFile (f, entityDir);
				}
				catch (IOException e)
				{
					e.printStackTrace ();
					cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
					LOGGER.error ("error copying file from tmp to "+entityMgmt.getEntityColumn ()+" dir", e);
					throw new IOException ("wasn't able to copy a file. sry, our fault.");
				}
				
				// insert to db
				int fileId = fileMgmt.addFile (f.name, f.type, user, f.tmpFile.length ());
				if (fileId < 0)
				{
					cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
					LOGGER.error ("error inserting file to db: " + f.name + " -> " + f.tmpFile);
					throw new IOException ("wasn't able to insert file " + f.name + " to db.");
				}
				f.dbId = fileId;
				
				// associate files+version
				if (!fileMgmt.associateFile (fileId, versionId, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ()))
				{
					cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
					LOGGER.error ("error inserting file to db: " + f.name + " -> " + f.tmpFile);
					throw new IOException ("wasn't able to insert file " + f.name + " to db.");
				}
				
				extractReadme = extractReadme && !f.name.toLowerCase ().equals ("readme.md");
			}
			
			if (extractReadme)
			{
				for (NewFile f : files.values ())
				{
					//parse protocol
					BufferedReader br = null; 
					try
					{
						br = new BufferedReader (new FileReader (f.tmpFile));
						String line = "", doc = "";
						boolean read = false;
						while (br.ready ())
						{
							line = br.readLine ();
							
							if (read && line.startsWith ("}"))
								break;
							
							if (read)
								doc += line + Tools.NEWLINE;
							
							if (line.startsWith ("documentation"))
								read = true;
						}
						br.close ();
						
						if (doc.length () > 0)
						{
							// write a readme.md
							BufferedWriter bw = null;
							try
							{
								File readMeFile = new File (entityDir + File.separator + "README.md");
								bw = new BufferedWriter (new FileWriter (readMeFile));
								bw.write (doc);
								bw.close ();
								bw = null;
								
								// insert to db
								int fileId = fileMgmt.addFile (readMeFile.getName (), "readme", user, readMeFile.length ());
								if (fileId < 0)
								{
									readMeFile.delete ();
									LOGGER.error ("error inserting file to db: README.md");
									throw new IOException ("wasn't able to insert file README.md to db.");
								}
								
								// associate files+version
								if (!fileMgmt.associateFile (fileId, versionId, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ()))
								{
									readMeFile.delete ();
									LOGGER.error ("error inserting file to db: README.md");
									throw new IOException ("wasn't able to insert file README.md to db.");
								}
								
								break;
							}
							catch (Exception e)
							{
								LOGGER.warn ("tried writing extracted readme from protocol", e);
							}
							finally
							{
								if (bw != null)
									try
									{
										bw.close ();
									}
									catch (Exception e)
									{}
							}
						}
					}
					catch (Exception e)
					{
						LOGGER.warn ("tried extracting readme from protocol", e);
					}
					finally
					{
						if (br != null)
							try
						{
							br.close ();
							}
						catch (Exception e)
						{}
					}
				}
			}
			

			JSONObject res = new JSONObject ();
			res.put ("response", true);
			res.put ("responseText", "added version successfully");
			res.put ("versionId", versionId);
			res.put ("versionType", entityMgmt.getEntityColumn ());
			answer.put ("createNewEntity", res);
			
		}
		
		if (!createOk)
		{
			cleanUp (entityDir, -1, files, fileMgmt, entityMgmt);
		}
	}
	
	private void cleanUp (File entityDir, int versionId, HashMap<String, NewFile> files, ChasteFileManager fileMgmt, ChasteEntityManager entityMgmt) throws ChastePermissionException
	{
		// delete directory recursively
		if (entityDir != null && entityDir.exists ())
			try
			{
				Tools.deleteRecursively (entityDir, false);
			}
			catch (IOException e)
			{
				// in general we don't really care about that in this special case. but lets log it...
				e.printStackTrace();
				LOGGER.warn ("deleting of " + entityDir + " failed.");
			}
		
		// remove entities from db
		for (NewFile f : files.values ())
			if (f.dbId >= 0)
				fileMgmt.removeFile (f.dbId);
		
		// delete version from db
		if (versionId >= 0)
			entityMgmt.removeVersion (versionId);
	}
}
