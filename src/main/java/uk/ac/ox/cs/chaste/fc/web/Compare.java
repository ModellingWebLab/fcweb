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

import de.binfalse.bflog.LOGGER;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperiment;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperimentVersion;
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

public class Compare extends WebModule
{
	private final int TYPE_MODEL = 1;
	private final int TYPE_PROTOCOL = 2;
	private final int TYPE_EXPERIMENT = 4;
	
	private int type;

	public Compare () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		header.addScript (new PageHeaderScript ("res/js/compare.js", "text/javascript", "UTF-8", null));

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
		if (req[2].equals ("m"))
		{
			type = TYPE_MODEL;
			entityMgmt = new ModelManager (db, notifications, userMgmt, user);
		}
		else if (req[2].equals ("p"))
		{
			type = TYPE_PROTOCOL;
			entityMgmt = new ProtocolManager (db, notifications, userMgmt, user);
		}
		else if (req[2].equals ("e"))
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
		
		if (task.equals ("getEntityInfos"))
		{
			JSONObject obj = new JSONObject ();
			obj.put ("response", true);
			obj.put ("responseText", "getEntityInfos");
			
			ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt);
			
			JSONArray ids = (JSONArray) querry.get ("ids");
			JSONArray entities = new JSONArray ();
			for (Object id : ids)
			{
				int curId;
				try
				{
//					System.out.println ("parsing id string " + id);
					curId = Integer.parseInt (id.toString ());
				}
				catch (NumberFormatException e)
				{
					LOGGER.warn ("user provided number which isn't an int: " + id, e);
					continue;
				}
//				System.out.println ("id string " + curId);
				
				ChasteEntity entity = entityMgmt.getEntityById (curId);
//				System.out.println ("entity " + entity);
				if (entity != null)
				{
					ChasteEntityVersion version = entity.getLatestVersion ();
					if (version != null)
					{
						fileMgmt.getFiles (version, entityMgmt.getEntityFilesTable (), entityMgmt.getEntityColumn ());
						JSONObject v = version.toJson ();
						ChasteExperiment expt = (ChasteExperiment) entity;
						if (expt != null)
						{
							v.put ("modelName", expt.getModel().getName());
							v.put ("protoName", expt.getProtocol().getName());
						}
						entities.add (v);
	//					System.out.println ("appended");
					}
				}
			}
			
			obj.put ("entities", entities);
			
			answer.put ("getEntityInfos", obj);
		}
		
		return answer;
	}
}
