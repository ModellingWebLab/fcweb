package uk.ac.ox.cs.chaste.fc.web;

import java.io.IOException;
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
import de.binfalse.bflog.LOGGER;

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
		header.addScript (new PageHeaderScript ("res/js/compare.js", "text/javascript", "UTF-8", null));
		header.addScript (new PageHeaderScript ("res/js/expt_common.js", "text/javascript", "UTF-8", null));

		Vector<String> plugins = new Vector<String> ();
		plugins.add ("displayPlotFlot");
		plugins.add ("displayPlotHC");

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
								}
							}
							entities.add (v);
						}
						LOGGER.warn ("couldn't find lates version of entity with id ", curId);
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
					LOGGER.warn ("couldn't find version with id ", curId);
				}
				
			}
			
			obj.put ("entities", entities);
			answer.put ("getEntityInfos", obj);
		}
		
		return answer;
	}
}
