package uk.ac.ox.cs.chaste.fc.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperiment;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperimentVersion;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.PageHeaderScript;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteEntityManager;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.ExperimentManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ModelManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ProtocolManager;
import uk.ac.ox.cs.chaste.fc.mgmt.Tools;
import uk.ac.ox.cs.chaste.fc.mgmt.UserManager;
import de.binfalse.bflog.LOGGER;

public class Batch extends WebModule
{
	private static final long serialVersionUID = 8334304634209822184L;
	private final int TYPE_MODEL = 1;
	private final int TYPE_PROTOCOL = 2;
	
	private int type;

	public Batch () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		// req[2] = model/protocol
		// req[3] = entity name
		// req[4] = entity id
		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		
		if (req == null || req.length < 5)
			return errorPage (request, response, null);

		
		ChasteEntityManager entityMgmt = null;
		ChasteEntityManager optionsMgmt = null;
		if (req[2].equals ("model"))
		{
			type = TYPE_MODEL;
			entityMgmt = new ModelManager (db, notifications, userMgmt, user);
			optionsMgmt = new ProtocolManager (db, notifications, userMgmt, user);
		}
		else if (req[2].equals ("protocol"))
		{
			type = TYPE_PROTOCOL;
			entityMgmt = new ProtocolManager (db, notifications, userMgmt, user);
			optionsMgmt = new ModelManager (db, notifications, userMgmt, user);
		}
		else
			return errorPage (request, response, null);
		
		
		try
		{
			int entityID = Integer.parseInt (req[4]);
			ChasteEntityVersion entity = entityMgmt.getVersionById (entityID);
			if (entity == null)
				return errorPage (request, response, null);
			request.setAttribute ("entity", entity);
			TreeSet<ChasteEntity> options = optionsMgmt.getAll (false, true);
			// Filter out any options that the current user is not allowed to run
			if (!user.isAllowedToForceNewExperiment())
			{
				ChasteEntityManager modelMgmt = null;
				ChasteEntityManager protoMgmt = null;
				ChasteEntityVersion model = null;
				ChasteEntityVersion proto = null;
				if (type == TYPE_MODEL)
				{
					modelMgmt = entityMgmt;
					protoMgmt = optionsMgmt;
					model = entity;
				}
				else
				{
					modelMgmt = optionsMgmt;
					protoMgmt = entityMgmt;
					proto = entity;
				}
				ExperimentManager exptMgmt = new ExperimentManager(db, notifications, userMgmt, user, modelMgmt, protoMgmt);
				
				Iterator<ChasteEntity> iter = options.iterator();
				while (iter.hasNext())
				{
					ChasteEntity opt = iter.next();
					// Check each version
					Map<Integer, ChasteEntityVersion> versions = opt.getVersions();
					Iterator<ChasteEntityVersion> vers_iter = versions.values().iterator();
					while (vers_iter.hasNext())
					{
						ChasteEntityVersion ver = vers_iter.next();
						if (type == TYPE_MODEL)
							proto = ver;
						else
							model = ver;
						ChasteEntity exp = exptMgmt.getExperiment(model.getId(), proto.getId(), false);
						if (exp != null) // Experiment already exists, so remove this option version
							vers_iter.remove();
					}
					// If option has no versions left, remove option
					if (!opt.hasVersions())
						iter.remove();
				}
			}
			request.setAttribute ("options", options);
			
			header.addScript (new PageHeaderScript ("res/js/batch.js", "text/javascript", "UTF-8", null));
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace ();
			notifications.addError ("cannot find "+entityMgmt.getEntityColumn ());
			LOGGER.warn ("user requested ", entityMgmt.getEntityColumn (), " id ", req[4], " is unparseable.");
			return errorPage (request, response, null);
		}
		
		return "Batch.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, 
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException
	{
		// TODO: regularly clean up:
		// uploaded files that were not used
		// created entity dirs that don't exist in entities in db
		
		String[] req =  request.getRequestURI().substring(request.getContextPath().length()).split ("/");
		
		ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
		ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);
		if (req[2].equals ("model"))
			type = TYPE_MODEL;
		else if (req[2].equals ("protocol"))
			type = TYPE_PROTOCOL;
		else if (req[2].equals ("batch"))
		{
			return processBatch (querry, notifications, db, modelMgmt, protocolMgmt, user);
		}
		else
			throw new IOException ("nothing to do.");
		
		
		JSONObject answer = new JSONObject();
		
		Object task = querry.get ("task");
		if (task == null || !task.equals ("batchSubmit"))
		{
			response.setStatus (HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException ("nothing to do.");
		}
		

		boolean force = false;
		try
		{
			Object o = querry.get ("force");
			if (o != null)
				force = Boolean.parseBoolean (o.toString ());
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		force &= user.isAllowedToForceNewExperiment ();
		
		
		ChasteEntityVersion model = null;
		ChasteEntityVersion protocol = null;
		
		try
		{
			int entityID = Integer.parseInt (req[4]);
			if (type == TYPE_MODEL)
			{
				model = modelMgmt.getVersionById (entityID);
				if (model == null)
					throw new NumberFormatException ("model not found");
			}
			else
			{
				protocol = protocolMgmt.getVersionById (entityID);
				if (protocol == null)
					throw new NumberFormatException ("protocol not found");
			}
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace ();
			notifications.addError ("cannot find entity");
			LOGGER.error (e, "user requested ", type, " with id: ", req[4], " invalid or not found");
			JSONObject obj = new JSONObject ();
				obj.put ("response", false);
				obj.put ("responseText", "no entity found");
			answer.put ("batchSubmit", obj);
			return answer;
		}

		int expOK = 0;
		int expFAIL = 0;
		ExperimentManager expMgmt = new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt);
		JSONArray ids = (JSONArray) querry.get ("entities");
		JSONArray createdExps = new JSONArray();
		for (int i = 0; i < ids.size (); i++)
		{
			try
			{
				int entityID = Integer.parseInt ((String) ids.get (i));
				if (type == TYPE_MODEL)
				{
					protocol = protocolMgmt.getVersionById (entityID);
					if (protocol == null)
						throw new NumberFormatException ("protocol not found");
				}
				else
				{
					model = modelMgmt.getVersionById (entityID);
					if (model == null)
						throw new NumberFormatException ("model not found");
				}
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace ();
				notifications.addError ("cannot find entity");
				LOGGER.warn ("user requested ", type, " with id: ", req[4], " invalid or not found: ", e.getMessage ());
				expFAIL++;
				continue;
			}
			
			if (tryCreateExperiment(model, protocol, db, notifications, expMgmt, userMgmt, user, modelMgmt, protocolMgmt, force, createdExps))
				expOK++;
			else
				expFAIL++;
		}

		JSONObject obj = new JSONObject ();
			obj.put ("response", expFAIL == 0);
			obj.put ("responseText", expOK + " experiments started; " + expFAIL + " experiments failed to start");
			obj.put ("createdExps", createdExps);
		answer.put ("batchSubmit", obj);
		return answer;
	}

	@SuppressWarnings("unchecked")
	private JSONObject processBatch (JSONObject querry,
		Notifications notifications, DatabaseConnector db, ModelManager modelMgmt,
		ProtocolManager protocolMgmt, User user)
	{
		JSONObject answer = new JSONObject();
		
		int ok = 0;
		int failed = 0;

		boolean force = false;
		try
		{
			Object o = querry.get ("force");
			if (o != null)
				force = Boolean.parseBoolean (o.toString ());
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}
		force &= user.isAllowedToForceNewExperiment ();
		
		JSONObject batchResp = new JSONObject();
		answer.put("batchTasks", batchResp);
		JSONArray createdExps = new JSONArray();
		batchResp.put("createdExps", createdExps);

		JSONArray array = (JSONArray) querry.get ("batchTasks");
		for (Object task : array)
		{
			JSONObject t = (JSONObject) task;

			int model = -1;
			int protocol = -1;
			int experiment = -1;
			
			try
			{
				if (t.get ("model") != null)
					model = Integer.parseInt (t.get ("model").toString ());
			}
			catch (NumberFormatException e)
			{
				LOGGER.warn (e, "user provided model in batch invalid: ", t.get ("model"));
			}
			
			try
			{
				if (t.get ("protocol") != null)
					protocol = Integer.parseInt (t.get ("protocol").toString ());
			}
			catch (NumberFormatException e)
			{
				LOGGER.warn (e, "user provided protocol in batch invalid: ", t.get ("protocol"));
			}
			
			try
			{
				Object o = t.get ("experiment");
				if (o != null)
				{
					if (o.equals ("*"))
						experiment = -42;
					else
						experiment = Integer.parseInt (t.get ("experiment").toString ());
				}
			}
			catch (NumberFormatException e)
			{
				LOGGER.warn (e, "user provided experiment in batch invalid: ", t.get ("experiment"));
			}
			
			ExperimentManager expMgmt = new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt);
			
			if (experiment == -42)
			{
				// rerun all experiments
				TreeSet<ChasteEntity> experiments = expMgmt.getAll (false, false);
				for (ChasteEntity exp : experiments)
				{
					if (reRunExperiment ((ChasteExperiment) exp, db, notifications, expMgmt, userMgmt, user, modelMgmt, protocolMgmt, force, createdExps))
						ok++;
					else
						failed++;
				}
			}
			else if (experiment >= 0)
			{
				ChasteExperiment exp = (ChasteExperiment) expMgmt.getEntityById (experiment);
				if (reRunExperiment ((ChasteExperiment) exp, db, notifications, expMgmt, userMgmt, user, modelMgmt, protocolMgmt, force, createdExps))
					ok++;
				else
					failed++;
			}
			else if (model >= 0 || protocol >= 0)
			{
				ChasteEntityVersion modelVersion = modelMgmt.getVersionById (model);
				ChasteEntityVersion protocolVersion = protocolMgmt.getVersionById (protocol);
				
				try
				{
					if (modelVersion == null)
						throw new NumberFormatException ("model not found");
					if (protocolVersion == null)
						throw new NumberFormatException ("protocol not found");
				}
				catch (NumberFormatException e)
				{
					e.printStackTrace ();
					notifications.addError (e.getMessage ());
					LOGGER.error (e, "user requested entity invalid or not found");
					failed++;
					continue;
				}
				
				if (tryCreateExperiment(modelVersion, protocolVersion, db, notifications, expMgmt, userMgmt, user, modelMgmt, protocolMgmt, force, createdExps))
					ok++;
				else
					failed++;
			}
			else
			{
				LOGGER.warn ("user provided batch invalid. one job doesn't provide necessary information");
				notifications.addError ("job doesn't provide necessary information");
				failed++;
			}
		}

		batchResp.put ("response", failed == 0);
		batchResp.put ("responseText", ok + " experiments started; " + failed + " experiments failed to start");
		return answer;
	}
	
	/**
	 * Try to create a new experiment with the given model & protocol versions, and return whether it was queued for execution.
	 * As a side effect, set error/info notifications indicating success or failure.
	 */
	@SuppressWarnings("unchecked")
	public static boolean tryCreateExperiment(ChasteEntityVersion model, ChasteEntityVersion protocol,
			DatabaseConnector db, Notifications notifications, ExperimentManager expMgmt, UserManager userMgmt, User user, ModelManager modelMgmt, ProtocolManager protocolMgmt, boolean force,
			JSONArray createdExps)
	{
		String expName = "[" + model.getName() + "@" + model.getVersion() + "] -- [" + protocol.getName() + "@" + protocol.getVersion() + "]";
		ChasteExperimentVersion expVer = null;
		try
		{
			expVer = NewExperiment.createExperiment (db, notifications, expMgmt, userMgmt, user, model, protocol, modelMgmt, protocolMgmt, force);
		}
		catch (Exception e)
		{
			notifications.addError ("experiment failed to start: " + expName + ": " + e.getLocalizedMessage());
			LOGGER.warn (e, "exp failed to start: ", expName);
			return false;
		}
		ChasteExperiment exp = expVer.getExperiment();
		String expUrl = Tools.getThisUrl() + "experiment/" + Tools.convertForURL(expVer.getName()) + "/" + exp.getId() + "/" + Tools.convertForURL(expVer.getVersion()) + "/" + expVer.getId() + "/";
		notifications.addInfo ("experiment queued: <a href='" + expUrl + "'>" + expName + "</a>");
		JSONObject obj = new JSONObject();
		obj.put("versId", expVer.getId());
		obj.put("url", expUrl);
		createdExps.add(obj);
		return true;
	}
	
	/**
	 * Try to run a new version of the given experiment, and return whether it was queued for execution.
	 * As a side effect, set error/info notifications indicating success or failure.
	 */
	public static boolean reRunExperiment (ChasteExperiment exp, DatabaseConnector db, Notifications notifications, ExperimentManager expMgmt, UserManager userMgmt, User user, ModelManager modelMgmt, ProtocolManager protocolMgmt, boolean force, JSONArray createdExps)
	{
		if (exp == null)
		{
			notifications.addError ("experiment not found");
			LOGGER.error ("user requested experiment invalid or not found");
			return false;
		}
		
		ChasteEntityVersion modelVersion = exp.getModel ();
		ChasteEntityVersion protocolVersion = exp.getProtocol ();
		return tryCreateExperiment(modelVersion, protocolVersion, db, notifications, expMgmt, userMgmt, user, modelMgmt, protocolMgmt, force, createdExps);
	}
}
