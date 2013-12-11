package uk.ac.ox.cs.chaste.fc.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.PageHeaderLink;
import uk.ac.ox.cs.chaste.fc.beans.PageHeaderScript;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteEntityManager;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.ExperimentManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ModelManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ProtocolManager;

public class DB extends WebModule
{

	public DB () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		header.addScript (new PageHeaderScript ("res/js/db.js", "text/javascript", "UTF-8", null));
		header.addScript (new PageHeaderScript ("res/js/3rd/jquery-cluetip-master/jquery.cluetip.js", "text/javascript", "UTF-8", null));
		header.addScript (new PageHeaderScript ("res/js/3rd/jquery.hoverIntent.js", "text/javascript", "UTF-8", null));
		header.addLink (new PageHeaderLink ("res/js/3rd/jquery-cluetip-master/jquery.cluetip.css", "text/css", "stylesheet"));
		return "Db.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, 
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException
	{
		JSONObject answer = new JSONObject();
		
		Object task = querry.get ("task");
		if (task == null)
		{
			response.setStatus (HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException ("nothing to do.");
		}
		
		if (task.equals ("getMatrix"))
		{
			ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
			ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);
			JSONObject obj = new JSONObject ();
			obj.put ("models", prepareEntities (modelMgmt));
			obj.put ("protocols", prepareEntities (protocolMgmt));
			obj.put ("experiments", prepareEntities (new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt)));
			
			answer.put ("getMatrix", obj);
		}
		
		return answer;
	}
	@SuppressWarnings("unchecked")
	private JSONObject prepareEntities (ChasteEntityManager entityMgmt)
	{
		JSONObject obj = new JSONObject ();
		
		TreeSet<ChasteEntity> entity = entityMgmt.getAll (true);
		for (ChasteEntity e : entity)
		{
			if (e.getVersions ().size () > 0)
				obj.put (e.getId (), e.toJson ());
		}
		
		return obj;
	}

}
