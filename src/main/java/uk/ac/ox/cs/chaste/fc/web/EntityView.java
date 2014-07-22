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
import java.util.TreeSet;
import java.util.Vector;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperiment;
import uk.ac.ox.cs.chaste.fc.beans.ChasteFile;
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
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;

public class EntityView extends WebModule
{
	private static final long serialVersionUID = 501895229328392660L;
	public static final int TYPE_MODEL = 1;
	public static final int TYPE_PROTOCOL = 2;
	public static final int TYPE_EXPERIMENT = 4;
	
	private int type;

	public EntityView () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		// req[1] = entity type
		// req[2] = entity name, or 'createnew' (in which case we don't have parts 3-7)
		// req[3] = entity id
		// req[4] = version, or 'latest' (in which case we don't have parts 5-7)
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
			if (type == TYPE_PROTOCOL && !user.isAllowedCreateProtocol ())
				return errorPage (request, response, null);
			if (type == TYPE_MODEL && !user.isAllowedCreateModel ())
				return errorPage (request, response, null);
			if (user.isAllowedCreateEntityVersion ())
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
						LOGGER.warn ("user requested ", entityMgmt.getEntityColumn (), " id ", request.getParameter("newentityname"), " is unparseable.");
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
				notifications.addError ("no entity found, or you do not have permission to view this entity");
				return errorPage (request, response, "no entity found, or you do not have permission to view this entity");
			}
			
			Map<Integer, ChasteEntityVersion> versions = entity.getOrderedVersions ();
			ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt);
			for (ChasteEntityVersion version : versions.values ())
				fileMgmt.getFiles (version, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
			
			request.setAttribute ("entity", entity);
			if (type == TYPE_EXPERIMENT)
			{
				ChasteExperiment exp = (ChasteExperiment) entity;
				request.setAttribute ("correspondingModel", exp.getModel ());
				request.setAttribute ("correspondingProtocol", exp.getProtocol ());
				header.addScript(new PageHeaderScript("res/js/3rd/FileSaver/Blob.js", "text/javascript", "UTF-8", null));
				header.addScript(new PageHeaderScript("res/js/3rd/FileSaver/FileSaver.js", "text/javascript", "UTF-8", null));
			}
			header.addScript (new PageHeaderScript ("res/js/3rd/showdown.js", "text/javascript", "UTF-8", null));
			header.addScript (new PageHeaderScript ("res/js/entity.js", "text/javascript", "UTF-8", null));
			header.addScript (new PageHeaderScript ("res/js/expt_common.js", "text/javascript", "UTF-8", null));
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace ();
			notifications.addError ("cannot find "+entityMgmt.getEntityColumn ());
			LOGGER.warn ("user requested ", entityMgmt.getEntityColumn (), " id ", req[3], " is unparseable.");
			return errorPage (request, response, null);
		}
		
		// version/file view/action will be done on client side

		Vector<String> plugins = new Vector<String> ();
		plugins.add ("displayContent");
		plugins.add ("displayTable");
		plugins.add ("displayPlotFlot");
		plugins.add ("displayPlotHC");
		plugins.add ("displayPlotD3");
		plugins.add("editMetadata");
		
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
		
		if ((task.equals("createNewEntity") || task.equals("verifyNewEntity") || task.equals("updateEntityFile")) && type != TYPE_EXPERIMENT)
		{
			if (type == TYPE_PROTOCOL && !user.isAllowedCreateProtocol ())
				throw new ChastePermissionException ("you are not allowed to create a new entity");
			if (type == TYPE_MODEL && !user.isAllowedCreateModel ())
				throw new ChastePermissionException ("you are not allowed to create a new entity");
			if (!user.isAllowedCreateEntityVersion ())
				throw new ChastePermissionException ("you are not allowed to create a new entity");
			
			if (task.equals("updateEntityFile"))
				updateEntityFile(notifications, db, querry, user, answer, entityMgmt, fileMgmt);
			else
				createNewEntity(task, notifications, db, querry, user, answer, entityMgmt, fileMgmt);
		}
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
			LOGGER.warn ("user provided version id not parseable: ", version, " (type: ", entityMgmt.getEntityColumn (), ")");
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
			LOGGER.warn ("user provided version id not parseable: ", version, " (type: ", entityMgmt.getEntityColumn (), ")");
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
				obj.put ("responseText", "Successfully deleted version " + vers.getVersion() + " of " + entityMgmt.getEntityColumn() + " " + vers.getName());
				// Determine if there are other versions of this entity still present, so the web page can show the new list
				ChasteEntity ent = entityMgmt.getEntityById(vers.getEntity().getId());
				if (ent != null && ent.hasVersions())
					obj.put("entityRemains", true);
			}
			return obj;
		}
		catch (NullPointerException | NumberFormatException e)
		{
			e.printStackTrace ();
			LOGGER.warn ("user provided version id not parseable: ", version, " (type: ", entityMgmt.getEntityColumn (), ")");
			throw new IOException ("version not found");
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject deleteEntity (Object entity, Notifications notifications, JSONObject answer, ChasteEntityManager entityMgmt) throws IOException, ChastePermissionException
	{
		try
		{
			int entId = Integer.parseInt (entity.toString ());
			ChasteEntity ent = entityMgmt.getEntityById (entId);
			
			JSONObject obj = new JSONObject ();
			obj.put ("response", false);
			obj.put ("responseText", "wasn't able to delete the entity");
			
			if (ent == null)
				notifications.addError ("no version found");
			else
			{
				entityMgmt.removeEntity (entId);
				obj.put ("response", true);
				obj.put ("responseText", "Successfully deleted " + entityMgmt.getEntityColumn() + " " + ent.getName());
			}
			return obj;
		}
		catch (NullPointerException | NumberFormatException e)
		{
			e.printStackTrace ();
			LOGGER.warn ("user provided entity id not parseable: ", entity, " (type: ", entityMgmt.getEntityColumn (), ")");
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
	
	/**
	 * Unpack a COMBINE archive uploaded as part of a new entity, and use the contents as (potentially only some of) the
	 * entity's files.
	 * @param query  the JSON specifying the new entity
	 * @param files  the list of files making up the entity
	 * @param archiveFile  the archive to unpack
	 * @param archiveFileName  the archive name, as specified in the upload form, for use in error messages
	 * @return true iff the archive was unpacked successfully
	 */
	@SuppressWarnings("unchecked")
	private boolean unpackArchive(JSONObject query, HashMap<String, NewFile> files, File archiveFile, String archiveFileName)
	{
		try
		{
			// Unpack archive
			CombineArchive ca = new CombineArchive (archiveFile);
			ArchiveEntry main_entry = ca.getMainEntry();
			
			// Record each entry as a file for this entity
			for (ArchiveEntry entry : ca.getEntries())
			{
				//String leaf_name = entry.getFileName();
				String leaf_name = entry.getFilePath ();
				if (leaf_name.startsWith ("./"))
					leaf_name = leaf_name.substring (2);
				while (leaf_name.startsWith ("/"))
					leaf_name = leaf_name.substring (1);
				
				String file_type = "unknown";
				if (leaf_name.endsWith(".cellml") || entry.getFormat().startsWith("http://identifiers.org/combine.specifications/cellml"))
					file_type = "CellML";
				else if (!entry.getFormat().startsWith("http"))
					file_type = entry.getFormat();
				File tmp = File.createTempFile("ChasteFCWEB", "unpackarchive");
				entry.extractFile(tmp).deleteOnExit();
				files.put(leaf_name, new NewFile (tmp, leaf_name, file_type, entry.equals(main_entry)));
			}
			
			// Use the archive's main entry, if present, as the main file for this entity, if not already specified
			if (query.get("mainFile") == null && main_entry != null)
			{
				String relativeName = main_entry.getFilePath ();
				if (relativeName.startsWith ("./"))
					relativeName = relativeName.substring (2);
				while (relativeName.startsWith ("/"))
					relativeName = relativeName.substring (1);
				query.put("mainFile", relativeName);
			}
			ca.close();
		}
		catch (Exception e)
		{
			LOGGER.error(e, "Error unpacking archive ", archiveFileName);
			return false;
		}
		return true;
	}
	
	/**
	 * Create a new version of an entity by altering one of its files.
	 * @param notifications  used to provide error/info notifications to the user
	 * @param db  our database connection
	 * @param query  the JSON object specifying the new version. It has keys:
	 *     task, entityId, entityName, baseVersionId, versionName, commitMsg, rerunExperiments, fileName, fileContents
	 * @param user  the user updating the entity
	 * @param answer  the JSON object providing a response to the user.
	 *     On return it will contain the following keys under 'updateEntityFile':
	 *     response, responseText, entityId, versionId, versionType, expCreation
	 * @param entityMgmt  manager for this kind of entity
	 * @param fileMgmt  manager for files
	 * @throws IOException
	 * @throws ChastePermissionException
	 */
	@SuppressWarnings("unchecked")
	private void updateEntityFile(Notifications notifications, DatabaseConnector db, JSONObject query, User user, JSONObject answer, ChasteEntityManager entityMgmt, ChasteFileManager fileMgmt) throws IOException, ChastePermissionException
	{
		LOGGER.debug("updating entity file");
		NewVersionInfo info = new NewVersionInfo();
		String fileName = null;
		File entityDir = null;
		HashMap<String, NewFile> files = new HashMap<String, NewFile> ();
		ChasteEntityVersion baseVersion = null;
		ChasteFile originalFile = null;

		boolean createOk = checkCommonCreationCriteria(info, notifications, db, query, user, answer, entityMgmt);
		
		// Check other required parameters have been supplied
		if (query.get("baseVersionId") == null)
		{
			notifications.addError("no base version supplied");
			createOk = false;
		}
		if (query.get("fileName") == null)
		{
			notifications.addError("no file name supplied");
			createOk = false;
		}
		else
		{
			fileName = query.get("fileName").toString().trim();
		}
		if (query.get("fileContents") == null)
		{
			notifications.addError("no file contents supplied");
			createOk = false;
		}
		
		// Find the version this update is based on
		if (createOk)
		{
			try
			{
				int versionId = Integer.parseInt(query.get("baseVersionId").toString());
				baseVersion = entityMgmt.getVersionById(versionId);
				if (baseVersion == null)
				{
					notifications.addError("base version not found");
					createOk = false;
				}
				else
				{
					fileMgmt.getFiles(baseVersion, entityMgmt.getEntityFilesTable(), entityMgmt.getEntityColumn());
				}
			}
			catch (NullPointerException | NumberFormatException e)
			{
				e.printStackTrace();
				LOGGER.warn("user provided version id not parseable: ", query.get("baseVersionId"));
				throw new IOException("base version not found");
			}
		}
		
		// Fill in the default file info based on the baseVersion (the updated file will be altered later)
		if (createOk)
		{
			for (ChasteFile cf: baseVersion.getFiles())
			{
				if (cf.getName().equals(fileName))
					originalFile = cf;
				File realFile = new File(entityMgmt.getEntityStorageDir() + Tools.FILESEP + baseVersion.getFilePath() + Tools.FILESEP + cf.getName());
				files.put(cf.getName(), new NewFile(realFile, cf.getName(), cf.getFiletype(), cf.isMasterFile()));
			}
			if (originalFile == null)
			{
				notifications.addError("supplied file name does not exist in base version");
				createOk = false;
			}
		}
		
		if (createOk)
		{
			// Create the new version, which requires knowledge of the storage folder
			entityDir = Tools.createUniqueSubDir(entityMgmt.getEntityStorageDir());
			LOGGER.debug("will write files to ", entityDir);
			int versionId = entityMgmt.createVersion(info.entity.getId(), info.versionName, info.commitMsg, entityDir.getName(), user, info.visibility);
			if (versionId < 0)
			{
				cleanUp(entityDir, versionId, files, fileMgmt, entityMgmt);
				LOGGER.error("error inserting/creating ", entityMgmt.getEntityColumn (), " version to db");
				throw new IOException("wasn't able to create/insert "+entityMgmt.getEntityColumn ()+" version to database.");
			}
			
			// Write new file version to disk directly in our storage folder
			BufferedWriter bw = null;
			try
			{
				File newFile = new File(entityDir + Tools.FILESEP + fileName);
				bw = new BufferedWriter(new FileWriter(newFile));
				bw.write(query.get("fileContents").toString());
				bw.close();
				bw = null;
				files.get(fileName).tmpFile = newFile;
			}
			catch (Exception e)
			{
				cleanUp(entityDir, versionId, files, fileMgmt, entityMgmt);
				LOGGER.error(e, "failed to write updated file version");
				throw new IOException("failed to write updated file to disk.");
			}
			finally
			{
				if (bw != null)
					try
					{
						bw.close();
					}
					catch (Exception e)
					{}
			}
			
			// Copy other files into our storage dir, and make database associations
			for (NewFile f : files.values ())
			{
				associateFile(f, fileMgmt, entityMgmt, user, entityDir, versionId, files);
			}
			
			// Report success, and optionally re-run experiments
			JSONObject res = new JSONObject ();
			res.put("response", true);
			res.put("responseText", "added version successfully");
			res.put("entityId", info.entity.getId());
			res.put("versionId", versionId);
			res.put("versionType", entityMgmt.getEntityColumn());
			
			rerunExperiments(notifications, db, query, user, entityMgmt, res, baseVersion, versionId);
			
			answer.put("updateEntityFile", res);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createNewEntity (Object task, Notifications notifications, DatabaseConnector db, JSONObject querry, User user, JSONObject answer, ChasteEntityManager entityMgmt, ChasteFileManager fileMgmt) throws IOException, ChastePermissionException
	{
		LOGGER.debug ("creating new entity; task=", task.toString(), ".");
		NewVersionInfo info = new NewVersionInfo();
		File entityDir = null;
		HashMap<String, NewFile> files = new HashMap<String, NewFile> ();
		String mainEntry = "";
		boolean createOk = task.equals ("createNewEntity");
		
		if (!checkCommonCreationCriteria(info, notifications, db, querry, user, answer, entityMgmt))
			createOk = false;
		
		LOGGER.debug ("creating entity ", info.entityName, " version ", info.versionName, " commit msg ", info.commitMsg);
		
		if (createOk)
		{
			if (querry.get ("mainFile") != null)
				mainEntry = querry.get ("mainFile").toString ().trim ();
			// do we have any files?
			// creating an empty entity makes no sense
			if (querry.get ("files") != null)
			{
				JSONArray array = (JSONArray) querry.get ("files");
				for (int i = 0; i < array.size (); i++)
				{
					JSONObject file = (JSONObject) array.get (i);

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
						throw new IOException ("detected empty file name. That's not allowed.");
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
						notifications.addError ("cannot find file " + name + ". Please upload again.");
						createOk = false;
					}
					else if (type.equals("COMBINE archive"))
					{
						if (!unpackArchive(querry, files, tmp, name))
						{
							notifications.addError("failed to unpack COMBINE archive " + name);
							createOk = false;
						}
					}
					else
						files.put (tmpName, new NewFile (tmp, name, type, mainEntry.equals(name)));
				}
			}
			
			if (files.size () < 1)
			{
				createOk = false;
				notifications.addError ("no files included in this "+entityMgmt.getEntityColumn ()+".");
			}
			
			if (FileTransfer.ambiguous (files))
			{
				createOk = false;
				notifications.addError ("there was an error with the files. The provided information is ambiguous.");
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
			if (info.entity == null)
				entityId = entityMgmt.createEntity (info.entityName, user);
			else
				entityId = info.entity.getId ();
			
			if (entityId < 0)
			{
				cleanUp (null, -1, files, fileMgmt, entityMgmt);
				LOGGER.error ("error inserting/creating ", entityMgmt.getEntityColumn (), " to db");
				throw new IOException ("wasn't able to create/insert "+entityMgmt.getEntityColumn ()+" to database.");
			}
			ChasteEntityVersion latestVersion = null;
			if (entityMgmt.getEntityById (entityId) != null)
				latestVersion = entityMgmt.getEntityById (entityId).getLatestVersion ();
			
			// Creating the version requires knowledge of the storage folder
			entityDir = Tools.createUniqueSubDir(entityMgmt.getEntityStorageDir());
			LOGGER.debug ("will write files to ", entityDir);

			// create version
			int versionId = entityMgmt.createVersion (entityId, info.versionName, info.commitMsg, entityDir.getName(), user, info.visibility);
			if (versionId < 0)
			{
				cleanUp (entityDir, versionId, files, fileMgmt, entityMgmt);
				LOGGER.error ("error inserting/creating ", entityMgmt.getEntityColumn (), " version to db");
				throw new IOException ("wasn't able to create/insert "+entityMgmt.getEntityColumn ()+" version to database.");
			}
			
			boolean extractReadme = entityMgmt.getEntityColumn () == "protocol";
			
			for (NewFile f : files.values ())
			{
				associateFile(f, fileMgmt, entityMgmt, user, entityDir, versionId, files);
				extractReadme = extractReadme && !f.name.toLowerCase ().equals ("readme.md");
			}
			
			if (extractReadme)
			{
				for (NewFile f : files.values ())
				{
					if (!mainEntry.isEmpty() && !f.name.equals(mainEntry))
						continue;
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
								
								files.put(readMeFile.getName (), new NewFile(readMeFile, readMeFile.getName(), "readme", false));
								associateFile(files.get(readMeFile.getName()), fileMgmt, entityMgmt, user, entityDir, versionId, files);
								
								break;
							}
							catch (Exception e)
							{
								LOGGER.warn (e, "tried writing extracted readme from protocol");
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
						LOGGER.warn (e, "tried extracting readme from protocol");
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
			
			rerunExperiments(notifications, db, querry, user, entityMgmt, res, latestVersion, versionId);
			
			res.put ("responseText", "added version successfully");
			res.put ("entityId", entityId);
			res.put ("versionId", versionId);
			res.put ("versionType", entityMgmt.getEntityColumn ());
			answer.put ("createNewEntity", res);
		}
		
		if (!createOk)
		{
			cleanUp (entityDir, -1, files, fileMgmt, entityMgmt);
		}
	}
	
	/**
	 * Re-run any experiments associated with this model/protocol using the new version?
	 * @param notifications  used to provide error/info notifications to the user
	 * @param db  our database connection
	 * @param query  the JSON object specifying the new version. It needs key rerunExperiments.
	 * @param user  the user updating the entity
	 * @param entityMgmt  manager for this kind of entity
	 * @param res  the JSON object providing a response to the user.
	 *     On return the key 'expCreation' will be added.
	 * @param baseVersion  the previous version of this entity, which will be checked for associated experiments
	 * @param newVersionId  the id of the new version of this entity
	 */
	@SuppressWarnings("unchecked")
	private void rerunExperiments(Notifications notifications, DatabaseConnector db, JSONObject query, User user, ChasteEntityManager entityMgmt,
								  JSONObject res, ChasteEntityVersion baseVersion, int newVersionId)
	{
		if (baseVersion != null && query.get ("rerunExperiments") != null)
		{
			try
			{
				boolean rerunExperiments = Boolean.parseBoolean (query.get ("rerunExperiments").toString ());
				
				if (rerunExperiments)
				{
					ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
					ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);
					
					ExperimentManager expMgmt = new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt);
					
					int ok = 0, failed = 0;
					
					ChasteEntityVersion newVersion = entityMgmt.getVersionById (newVersionId);
					
					TreeSet<ChasteEntity> exps = null;
					if (entityMgmt.getEntityColumn ().equals ("model"))
					{
						exps = expMgmt.getExperimentsByModel (baseVersion.getId (), false);
						if (exps != null)
						{
							for (ChasteEntity ex : exps)
							{
								ChasteExperiment e = (ChasteExperiment) ex;
								try
								{
									if (NewExperiment.createExperiment (db, notifications, expMgmt, userMgmt, user, newVersion, e.getProtocol (), modelMgmt, protocolMgmt, true) != null)
										ok++;
									else
										failed++;
								}
								catch (Exception e1)
								{
									failed++;
								}
							}
						}
					}
					else if (entityMgmt.getEntityColumn ().equals ("protocol"))
					{
						exps = expMgmt.getExperimentsByProtocol (baseVersion.getId (), false);
						if (exps != null)
						{
							for (ChasteEntity ex : exps)
							{
								ChasteExperiment e = (ChasteExperiment) ex;
								try
								{
									if (NewExperiment.createExperiment (db, notifications, expMgmt, userMgmt, user, e.getModel (), newVersion, modelMgmt, protocolMgmt, true) != null)
										ok++;
									else
										failed++;
								}
								catch (Exception e1)
								{
									failed++;
								}
							}
						}
					}
					
					res.put ("expCreation", "created " + ok + " experiments; " + failed + " failed");
				}
			}
			catch (NumberFormatException e)
			{}
		}
	}

	/**
	 * Add a file to the database, associate it with the entity version it's part of, and copy the physical file
	 * to the right place (if it isn't there already).
	 * @param f  the file information
	 * @param fileMgmt  the file manager
	 * @param entityMgmt  the entity manager
	 * @param user  the user owning this file
	 * @param entityDir  where the file should live
	 * @param versionId  the entity version it's associated with
	 * @param files  all the files making up this entity version
	 * @throws ChastePermissionException 
	 * @throws IOException 
	 */
	private void associateFile(NewFile f, ChasteFileManager fileMgmt, ChasteEntityManager entityMgmt, User user,
							   File entityDir, int versionId, HashMap<String, NewFile> files) throws ChastePermissionException, IOException
	{
		// insert to db
		int fileId = fileMgmt.addFile(f.name, f.type, user, f.tmpFile.length(), f.isMain);
		if (fileId < 0)
		{
			cleanUp(entityDir, versionId, files, fileMgmt, entityMgmt);
			LOGGER.error("error inserting file to db: ", f.name, " -> ", f.tmpFile);
			throw new IOException("wasn't able to insert file " + f.name + " to database.");
		}
		f.dbId = fileId;
		
		// associate files+version
		if (!fileMgmt.associateFile(fileId, versionId, entityMgmt.getEntityFilesTable(), entityMgmt.getEntityColumn()))
		{
			cleanUp(entityDir, versionId, files, fileMgmt, entityMgmt);
			LOGGER.error("error inserting file to db: ", f.name, " -> ", f.tmpFile);
			throw new IOException("wasn't able to insert file " + f.name + " to database.");
		}
		
		// copy file, if not already in target folder
		if (!entityDir.equals(f.tmpFile.getParentFile()))
		{
			try
			{
				FileTransfer.copyFile(f, entityDir);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				cleanUp(entityDir, versionId, files, fileMgmt, entityMgmt);
				LOGGER.error(e, "error copying file from tmp to ", entityMgmt.getEntityColumn(), " dir");
				throw new IOException("wasn't able to copy a file. Sorry, our fault.");
			}
		}
	}
	
	/**
	 * A utility class providing information about a new version of an entity, for passing between helper methods
	 * such as checkCommonCreationCriteria.
	 */
	class NewVersionInfo
	{
		ChasteEntity entity;
		String entityName;
		String versionName;
		String commitMsg;
		String visibility;
		
		public NewVersionInfo()
		{
			entity = null;
			entityName = null;
			versionName = null;
			commitMsg = "";
			visibility = null;
		}
	}
	
	/**
	 * Check criteria for entity creation common to updateEntityFile() and createNewEntity().
	 * The String parameters and entity are outputs from this method.
	 * @return whether creating the entity is permitted according to these checks
	 */
	@SuppressWarnings("unchecked")
	private boolean checkCommonCreationCriteria(NewVersionInfo info, Notifications notifications, DatabaseConnector db, JSONObject query, User user, JSONObject answer, ChasteEntityManager entityMgmt)
	{
		boolean createOk = true;
		
		if (query.get ("visibility") != null)
		{
			String userVisibility = query.get ("visibility").toString();
			if (!userVisibility.equals(ChasteEntityVersion.VISIBILITY_PRIVATE)
				&& !userVisibility.equals(ChasteEntityVersion.VISIBILITY_RESTRICTED)
				&& !userVisibility.equals(ChasteEntityVersion.VISIBILITY_PUBLIC))
			{
				LOGGER.warn("Invalid visibility '", userVisibility, "' sent.");
				notifications.addError("Invalid visibility '" + userVisibility + "' sent.");
				createOk = false;
			}
			else
			{
				info.visibility = userVisibility;
			}
		}
		
		if (query.get ("entityName") != null)
		{
			JSONObject obj = new JSONObject ();
			obj.put ("response", true);
			obj.put ("responseText", "nice name");
			answer.put ("entityName", obj);
			
			info.entityName = Tools.validataUserInput (query.get ("entityName").toString ().trim ());
			info.entity = entityMgmt.getEntityByName (info.entityName);
			if (info.entity == null)
			{
				if (info.entityName.length () < 2)
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
				if (info.entity.getAuthor ().getId () != user.getId ())
				{
					if (user.isAdmin())
					{
						// Pretend to be the original author and allow the creation to proceed
						user.authById(info.entity.getAuthor().getId());
					}
					else
					{
						// not allowed to create a new version to a model of somebody else -> otherwise we get a delete problem...
						obj.put ("response", false);
						obj.put ("responseText", "name exists, but "+entityMgmt.getEntityColumn ()+" belongs to somebody else. Please choose a different name.");
						createOk = false;
					}
				}
				else
				{
					obj.put ("response", true);
					obj.put ("responseText", "name exists. You're going to upload a new version to an existing "+entityMgmt.getEntityColumn ()+".");
					answer.put("isNewVersion", true);
				}
				// Set visibility to match the latest version, if not already specified
				if (info.visibility == null)
				{
					info.visibility = info.entity.getLatestVersion().getVisibility();
					answer.put("visibility", info.visibility);
				}
			}
		}
		else
			createOk = false;
		
		if (query.get ("versionName") != null)
		{
			JSONObject obj = new JSONObject ();
			obj.put ("response", true);
			obj.put ("responseText", "nice version identifier");
			answer.put ("versionName", obj);
			
			info.versionName = Tools.validataUserInput (query.get ("versionName").toString ().trim ());
			
			if (info.versionName.length () < 2)
			{
				obj.put ("response", false);
				obj.put ("responseText", "needs to be at least 2 characters in length");
				createOk = false;
			}
			// else ok
			
			if (info.entity != null)
			{
				if (info.entity.getVersion (info.versionName) != null)
				{
					obj.put ("response", false);
					obj.put ("responseText", "this "+entityMgmt.getEntityColumn ()+" already contains a version with that name");
					createOk = false;
				}
			}
			// else: new entity -> don't care about version names
		}
		else
			createOk = false;
		
		if (query.get ("commitMsg") != null)
		{
			info.commitMsg = Tools.validataUserInput (query.get ("commitMsg").toString ().trim ());
		}

		// default visibility if none specified
		if (info.visibility == null)
			info.visibility = ChasteEntityVersion.VISIBILITY_PRIVATE;

		return createOk;
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
				LOGGER.warn ("deleting of ", entityDir, " failed.");
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
