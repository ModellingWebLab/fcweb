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
	private static final long serialVersionUID = -687450967894650274L;

	public DB () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		header.addScript (new PageHeaderScript ("res/js/db.js", "text/javascript", "UTF-8", null));
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
			
			obj.put ("models", versionsToJson (modelVersions));
			obj.put ("protocols", versionsToJson (protocolVersions));
			obj.put ("experiments", entitiesToJson (experiments));
			
			answer.put ("getMatrix", obj);
		}
		
		return answer;
	}
	
	/**
	 * Get the latest visible version of all visible entities from the given manager.
	 * The list will be sorted by name by the manager.
	 */
	private Vector<ChasteEntityVersion> getEntityVersions (ChasteEntityManager entityMgmt)
	{
		Vector<ChasteEntityVersion> entities = new Vector<ChasteEntityVersion> ();

		TreeSet<ChasteEntity> entity = entityMgmt.getAll (false, true);
		for (ChasteEntity e : entity)
			entities.add (e.getLatestVersion ());
		
		return entities;
	}
	
	/**
	 * Get all visible experiment versions involving a model/protocol combination from the given lists
	 * (i.e. form the cross product).
	 */
	private Vector<ChasteEntity> getExperimentVersions (Vector<ChasteEntityVersion> modelVersions, Vector<ChasteEntityVersion> protocolVersions, ExperimentManager entityMgmt)
	{
		Vector<ChasteEntity> exps = new Vector<ChasteEntity> ();

		for (ChasteEntityVersion model : modelVersions)
			for (ChasteEntityVersion protocol : protocolVersions)
			{
				ChasteEntity ent = entityMgmt.getExperiment (model.getId (), protocol.getId (), true);
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
}
