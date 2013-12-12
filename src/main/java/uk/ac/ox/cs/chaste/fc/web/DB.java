package uk.ac.ox.cs.chaste.fc.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.Vector;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
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

			Vector<ChasteEntityVersion> modelVersions = getEntityVersions (modelMgmt);
			Vector<ChasteEntityVersion> protocolVersions = getEntityVersions (protocolMgmt);
			
			Vector<ChasteEntity> experiments = getExperimentVersions (modelVersions, protocolVersions, new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt));
			
			obj.put ("models", versionsToJson (modelVersions));//prepareEntities (modelMgmt, false));
			obj.put ("protocols", versionsToJson (protocolVersions));//prepareEntities (protocolMgmt, false));
			obj.put ("experiments", entitiesToJson (experiments));//prepareEntities (new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt), true));
			
			answer.put ("getMatrix", obj);
		}
		
		return answer;
	}
	
	private Vector<ChasteEntityVersion> getEntityVersions (ChasteEntityManager entityMgmt)
	{
		Vector<ChasteEntityVersion> entities = new Vector<ChasteEntityVersion> ();

		TreeSet<ChasteEntity> entity = entityMgmt.getAll (true);
		for (ChasteEntity e : entity)
			if (e.getVersions ().size () > 0)
				entities.add (e.getLatestVersion ());
		
		return entities;
	}
	
	private Vector<ChasteEntity> getExperimentVersions (Vector<ChasteEntityVersion> modelVersions, Vector<ChasteEntityVersion> protocolVersions, ExperimentManager entityMgmt)
	{
		Vector<ChasteEntity> exps = new Vector<ChasteEntity> ();

		for (ChasteEntityVersion model : modelVersions)
			for (ChasteEntityVersion protocol : protocolVersions)
			{
				ChasteEntity ent = entityMgmt.getExperiment (model.getId (), protocol.getId ());
				if (ent != null)
					exps.add (ent);
			}
		return exps;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject entitiesToJson (Vector<ChasteEntity> vec)
	{
		JSONObject obj = new JSONObject ();
		for (ChasteEntity v : vec)
			obj.put (v.getId (), v.toJson ());
		return obj;
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject versionsToJson (Vector<ChasteEntityVersion> vec)
	{
		JSONObject obj = new JSONObject ();
		for (ChasteEntityVersion e : vec)
			obj.put (e.getId (), e.toJson ());
		return obj;
	}
	
	/*prepareEntities (ChasteEntityManager entityMgmt, boolean exp)
	{
		JSONObject obj = new JSONObject ();
		
		TreeSet<ChasteEntity> entity = entityMgmt.getAll (true);
		for (ChasteEntity e : entity)
		{
			if (e.getVersions ().size () > 0)
			{
				if (exp)
					obj.put (e.getId (), e.toJson ());
				else
					obj.put (e.getId (), e.getLatestVersion ().toJson ());
			}
		}
		
		return obj;
	}*/

}
