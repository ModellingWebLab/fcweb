package uk.ac.ox.cs.chaste.fc.web;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
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
import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.bives.webservice.client.BivesComparisonRequest;
import de.unirostock.sems.bives.webservice.client.BivesComparisonResponse;
import de.unirostock.sems.bives.webservice.client.BivesWs;
import de.unirostock.sems.bives.webservice.client.exception.BivesClientException;
import de.unirostock.sems.bives.webservice.client.exception.BivesException;
import de.unirostock.sems.bives.webservice.client.impl.HttpBivesClient;

public class Compare extends WebModule
{
	private static final long serialVersionUID = -8671477576919542565L;

	public Compare () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		header.addScript(new PageHeaderScript("res/js/3rd/FileSaver/Blob.js", "text/javascript", "UTF-8", null));
		header.addScript(new PageHeaderScript("res/js/3rd/FileSaver/FileSaver.js", "text/javascript", "UTF-8", null));
		header.addScript (new PageHeaderScript ("res/js/compare.js", "text/javascript", "UTF-8", null));
		header.addScript (new PageHeaderScript ("res/js/expt_common.js", "text/javascript", "UTF-8", null));

		Vector<String> plugins = new Vector<String> ();
		plugins.add ("displayPlotFlot");
		plugins.add ("displayPlotHC");
		plugins.add ("displayUnixDiff");
		plugins.add ("displayBivesDiff");

		for (String s : plugins)
		{
			header.addScript (new PageHeaderScript ("res/js/visualizers/" + s + "/" + s + ".js", "text/javascript", "UTF-8", null));
			header.addLink (new PageHeaderLink ("res/js/visualizers/" + s + "/" + s + ".css", "text/css", "stylesheet"));
		}
		
		return "Compare.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, 
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException, ChastePermissionException
	{
		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		
		ChasteEntityManager entityMgmt = null;
		int type = 0;
		if (req[2].equals ("m"))
		{
			entityMgmt = new ModelManager (db, notifications, userMgmt, user);
			type = EntityView.TYPE_MODEL;
		}
		else if (req[2].equals ("p"))
		{
			entityMgmt = new ProtocolManager (db, notifications, userMgmt, user);
			type = EntityView.TYPE_PROTOCOL;
		}
		else if (req[2].equals ("e"))
		{
			entityMgmt = new ExperimentManager (db, notifications, userMgmt, user, new ModelManager (db, notifications, userMgmt, user), new ProtocolManager (db, notifications, userMgmt, user));
			type = EntityView.TYPE_EXPERIMENT;
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
		
		if (task.equals ("getEntityInfos"))
		{
			JSONObject obj = new JSONObject ();
			obj.put ("response", true);
			obj.put ("responseText", "getEntityInfos");
			
			ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt);
			
			// comparing experiments -> usual using the latest versions
			// comparing models/protocols -> use versions by id
			boolean latestVersion = true;
			if (querry.get ("getBy") != null && querry.get ("getBy").equals ("versionId"))
				latestVersion = false;
			
			JSONArray ids = (JSONArray) querry.get ("ids");
			JSONArray entities = new JSONArray ();
			for (Object id : ids)
			{
				int curId;
				try
				{
					curId = Integer.parseInt (id.toString ());
				}
				catch (NumberFormatException e)
				{
					LOGGER.warn (e, "user provided number which isn't an int: ", id);
					continue;
				}
				
				if (latestVersion)
				{
					ChasteEntity entity = entityMgmt.getEntityById (curId);
					if (entity != null)
					{
						ChasteEntityVersion version = entity.getLatestVersion ();
						if (version != null)
						{
							fileMgmt.getFiles (version, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
							JSONObject v = version.toJson ();
							if (type == EntityView.TYPE_EXPERIMENT)
							{
								ChasteExperiment expt = (ChasteExperiment) entity;
								if (expt != null)
								{
									v.put ("modelName", expt.getModel().getName());
									v.put ("protoName", expt.getProtocol().getName());
									v.put ("modelVersion", expt.getModel().getVersion());
									v.put ("protoVersion", expt.getProtocol().getVersion());
								}
							}
							entities.add (v);
						}
						else
							LOGGER.warn ("couldn't find latest version of entity with id ", curId);
					}
					else
						LOGGER.warn ("user requested entity with id ", curId, " but there is no such entity");
				}
				else
				{
					ChasteEntityVersion version = entityMgmt.getVersionById (curId);
					if (version != null)
					{
						fileMgmt.getFiles (version, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
						JSONObject v = version.toJson ();
						entities.add (v);
					}
					else
						LOGGER.warn ("couldn't find version with id ", curId);
				}
				
			}
			
			obj.put ("entities", entities);
			answer.put ("getEntityInfos", obj);
		}
		else if (task.equals ("getUnixDiff") || task.equals ("getBivesDiff"))
		{
			JSONObject obj = new JSONObject ();
			answer.put (task, obj);
			obj.put ("response", false);
			obj.put ("responseText", task);
			
			// select files
			int entity1 = -1,
				entity2 = -1,
				file1 = -1,
				file2 = -1;
			
			try
			{
				entity1 = Integer.parseInt ("" + querry.get ("entity1"));
				entity2 = Integer.parseInt ("" + querry.get ("entity2"));
				file1 = Integer.parseInt ("" + querry.get ("file1"));
				file2 = Integer.parseInt ("" + querry.get ("file2"));
			}
			catch (NullPointerException | NumberFormatException e)
			{
				LOGGER.warn ("user supplied invalid ids for diffing");
				obj.put ("responseText", "cannot understand request");
				return answer;
			}
			ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt);
			
			ChasteEntityVersion entityVersion1 = entityMgmt.getVersionById (entity1, false),
				entityVersion2 = entityMgmt.getVersionById (entity2, false);
			
			if (entityVersion1 == null || entityVersion2 == null)
			{
				LOGGER.warn ("user supplied invalid ids for diffing -> versions not found");
				obj.put ("responseText", "invalid request");
				return answer;
			}

			fileMgmt.getFiles (entityVersion1, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
			fileMgmt.getFiles (entityVersion2, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
			
			ChasteFile version1 = entityVersion1.getFileById (file1), version2 = entityVersion2.getFileById (file2);
			
			if (version1 == null || version2 == null)
			{
				LOGGER.warn ("user supplied invalid ids for diffing -> versions not found");
				obj.put ("responseText", "invalid request");
				return answer;
			}

			String pathToVersionA = entityMgmt.getEntityStorageDir () + Tools.FILESEP + entityVersion1.getFilePath () + Tools.FILESEP + version1.getName ();
			String pathToVersionB = entityMgmt.getEntityStorageDir () + Tools.FILESEP + entityVersion2.getFilePath () + Tools.FILESEP + version2.getName ();
			
			
			// compute model structure diff
			if (task.equals ("getBivesDiff"))
			{
				String id = Tools.hash (pathToVersionA + "--  --" + pathToVersionB);
				
				// read files and send them to bives
				StringBuilder sb = new StringBuilder ();
				String s = null;
				try
				{
				BufferedReader br = new BufferedReader (new FileReader (pathToVersionA));
				while (br.ready())
				{
					sb.append(br.readLine()).append("\n");
				}
				br.close ();
				s = sb.toString();
				sb = new StringBuilder ();
				br = new BufferedReader (new FileReader (pathToVersionB));
				while (br.ready())
				{
					sb.append(br.readLine()).append("\n");
				}
				br.close ();
				}
				catch (Exception e)
				{
					LOGGER.error (e, "error comparing with bives: couldn't read files");
	 				obj.put ("responseText", "bives comparison failed: couldn't read files");
					return answer;
				}
				
				BivesComparisonRequest bivesRequest = new BivesComparisonRequest (
						s,
						sb.toString());

				bivesRequest.addCommand (BivesComparisonRequest.COMMAND_COMPONENT_HIERARCHY_JSON);
				bivesRequest.addCommand (BivesComparisonRequest.COMMAND_REACTIONS_JSON);
				bivesRequest.addCommand (BivesComparisonRequest.COMMAND_REPORT_HTML);
				bivesRequest.addCommand (BivesComparisonRequest.COMMAND_XML_DIFF);
				
				BivesWs bives = new HttpBivesClient("http://bives.sems.uni-rostock.de/");
				LOGGER.debug ("Calling BiVeS at http://bives.sems.uni-rostock.de/");
				BivesComparisonResponse result;
				try
				{
					result = bives.performRequest(bivesRequest);
				}
				catch (BivesClientException | BivesException e)
				{
					LOGGER.error (e, "error executing bives request");
	 				obj.put ("responseText", "bives request failed: " + e.getMessage ());
					return answer;
				}
				
				if (result.hasError ())
				{
					String errors = "";
					for (String err : result.getErrors ())
					{
						LOGGER.error ("error from bives comparison request: " + err);
						errors += " [" + err + "] ";
					}
					obj.put ("responseText", errors);
					return answer;
				}
				
				
				JSONObject bivesResult = new JSONObject ();
				
				bivesResult.put (BivesComparisonRequest.COMMAND_COMPONENT_HIERARCHY_JSON, result.getResult (BivesComparisonRequest.COMMAND_COMPONENT_HIERARCHY_JSON));
				bivesResult.put (BivesComparisonRequest.COMMAND_REACTIONS_JSON, result.getResult (BivesComparisonRequest.COMMAND_REACTIONS_JSON));
				bivesResult.put (BivesComparisonRequest.COMMAND_REPORT_HTML, result.getResult (BivesComparisonRequest.COMMAND_REPORT_HTML));
				bivesResult.put (BivesComparisonRequest.COMMAND_XML_DIFF, result.getResult (BivesComparisonRequest.COMMAND_XML_DIFF));
				bivesResult.put ("id", id);
				
				obj.put ("bivesDiff", bivesResult);
				obj.put ("response", true);
				
			}
			else
			{
				try
				{
					if (!pathToVersionA.equals (pathToVersionB))
					{
						ProcessBuilder pb = new ProcessBuilder("diff", "-a", pathToVersionA, pathToVersionB);
						Process p = pb.start();
						BufferedReader br = new BufferedReader (new InputStreamReader(p.getInputStream()));
						StringBuffer diff = new StringBuffer ();
						String line;
						while ((line = br.readLine()) != null)
							diff.append (line).append (Tools.NEWLINE);
						br.close ();
						
						obj.put ("unixDiff", diff.toString ());
					}
					else
						obj.put ("unixDiff", "same file..");
					
					obj.put ("response", true);
				}
				catch (Exception e)
				{
					LOGGER.error (e, "couldn't compute unix diff of ", version1.getName (), " and ", version2.getName ());
				}
			}
		}
		
		return answer;
	}
	
}
