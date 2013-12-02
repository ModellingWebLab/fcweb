/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.web;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperimentVersion;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteEntityManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteFileManager;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.ExperimentManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ModelManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ProtocolManager;
import uk.ac.ox.cs.chaste.fc.mgmt.Tools;
import uk.ac.ox.cs.chaste.fc.mgmt.UserManager;
import de.binfalse.bflog.LOGGER;


/**
 * @author martin
 *
 */
public class NewExperiment
	extends WebModule
{
	public static final String NEWEXP_MODEL = "newExpModel";
	public static final String NEWEXP_PROTOCOL = "newExpProtocol";
	
	public NewExperiment () throws NamingException, SQLException
	{
		super ();
	}


	/* (non-Javadoc)
	 * @see uk.ac.ox.cs.chaste.fc.web.WebModule#answerApiRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector, uk.ac.ox.cs.chaste.fc.beans.Notifications, org.json.simple.JSONObject, uk.ac.ox.cs.chaste.fc.beans.User, javax.servlet.http.HttpSession)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request,
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user,
		HttpSession session) throws Exception
	{
		if (!user.isAuthorized ())
			throw new IOException ("not allowed.");
		
		
		JSONObject answer = new JSONObject();

		Object task = querry.get ("task");
		if (task == null)
		{
			response.setStatus (HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException ("nothing to do.");
		}

		if (task.equals ("newExpModel"))
		{
			try
			{
				int modelid = Integer.parseInt (querry.get ("model").toString ());
				ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
				ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);
				ChasteEntityVersion version = modelMgmt.getVersionById (modelid);
				
				if (version != null)
				{
					session.setAttribute (NEWEXP_MODEL, modelid);
					JSONObject obj = new JSONObject ();
						obj.put ("response", true);
						obj.put ("responseText", "model selected");
					answer.put ("newExpModel", obj);
				}
				else
				{
					JSONObject obj = new JSONObject ();
						obj.put ("response", false);
						obj.put ("responseText", "model not found");
					answer.put ("newExpModel", obj);
				}

				ChasteEntityVersion model = getSchedEntity (request, session, db, notifications, userMgmt, modelMgmt, NEWEXP_MODEL);
				ChasteEntityVersion protocol = getSchedEntity (request, session, db, notifications, userMgmt, protocolMgmt, NEWEXP_PROTOCOL);
				if (model != null)
				answer.put ("scheduledModel", model.getEntity ().getName () + " @ " + model.getVersion ());
				if (protocol != null)
				answer.put ("scheduledProtocol", protocol.getEntity ().getName () + " @ " + protocol.getVersion ());
				
				return answer;
			}
			catch (NumberFormatException e)
			{
				
				throw new IOException ("something went wrong while selecting the model");
			}
			
		}
		else if (task.equals ("newExpProtocol"))
		{
			try
			{
				int protocolid = Integer.parseInt (querry.get ("protocol").toString ());
				ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
				ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);
				ChasteEntityVersion version = protocolMgmt.getVersionById (protocolid);
				
				if (version != null)
				{
					session.setAttribute (NEWEXP_PROTOCOL, protocolid);
					JSONObject obj = new JSONObject ();
						obj.put ("response", true);
						obj.put ("responseText", "protocol selected");
					answer.put ("newExpProtocol", obj);
				}
				else
				{
					JSONObject obj = new JSONObject ();
						obj.put ("response", false);
						obj.put ("responseText", "protocol not found");
					answer.put ("newExpProtocol", obj);
				}

				ChasteEntityVersion model = getSchedEntity (request, session, db, notifications, userMgmt, modelMgmt, NEWEXP_MODEL);
				ChasteEntityVersion protocol = getSchedEntity (request, session, db, notifications, userMgmt, protocolMgmt, NEWEXP_PROTOCOL);
				if (model != null)
				answer.put ("scheduledModel", model.getEntity ().getName () + " @ " + model.getVersion ());
				if (protocol != null)
				answer.put ("scheduledProtocol", protocol.getEntity ().getName () + " @ " + protocol.getVersion ());
				
				return answer;
			}
			catch (NumberFormatException e)
			{
				
				throw new IOException ("something went wrong while selecting the protocol");
			}
			
		}
		else if (task.equals ("runExperiment"))
		{
			ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
			ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);
			
			ChasteEntityVersion model = getSchedEntity (request, session, db, notifications, userMgmt, modelMgmt, NEWEXP_MODEL);
			ChasteEntityVersion protocol = getSchedEntity (request, session, db, notifications, userMgmt, protocolMgmt, NEWEXP_PROTOCOL);
			
			boolean force = false;
			try
			{
				Object o = querry.get ("forceNewVersion");
				System.out.println ("o: " + o);
				if (o != null)
					force = Boolean.parseBoolean (o.toString ());
				System.out.println ("force: " + force);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				notifications.addError ("force not valid");
			}
			System.out.println ("force after parse: " + force);
			force &= user.isAllowedToForceNewExperiment ();
			System.out.println ("force after perm check: " + force);
			
			// everything's ready to run the experiment?
			if (model == null)
			{
				JSONObject obj = new JSONObject ();
					obj.put ("response", false);
					obj.put ("responseText", "no model chosen");
				answer.put ("runExperiment", obj);
				return answer;
			}
			if (protocol == null)
			{
				JSONObject obj = new JSONObject ();
					obj.put ("response", false);
					obj.put ("responseText", "no protocol chosen");
				answer.put ("runExperiment", obj);
				return answer;
			}
			
			//int expID = 
				createExperiment (db, notifications, new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt), userMgmt, user, model, protocol, modelMgmt, protocolMgmt, force);
			

				session.removeAttribute (NEWEXP_PROTOCOL);
				session.removeAttribute (NEWEXP_MODEL);
				
				
			// TODO: cronjob for experiments that are not processed after 24h? -> send mails to admins?
			JSONObject obj = new JSONObject ();
				obj.put ("response", true);
				obj.put ("responseText", "Experiment submitted. Based on the size of the Queue it might take some time until we can process your job.");
				//obj.put ("expID", expID);
			answer.put ("runExperiment", obj);
			return answer;
		}
		else if (task.equals ("newExperiment"))
		{
			ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
			ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);

			int modelId = -1;
			int protocolId = -1;
			
			try
			{
				modelId = Integer.parseInt (querry.get ("model").toString ());
				protocolId = Integer.parseInt (querry.get ("protocol").toString ());
				if (modelId < 0 || protocolId < 0)
					throw new NumberFormatException ("invalid id");
			}
			catch (NumberFormatException e)
			{
				notifications.addError ("number parsing error");
				LOGGER.warn ("user supplied model/protocol id not valid: "+querry.get ("model")+"/"+querry.get ("protocol"));
				throw new IOException ("something went wrong while selecting the model");
			}
			
			ChasteEntityVersion model = new ModelManager (db, notifications, userMgmt, user).getVersionById (modelId);
			ChasteEntityVersion protocol = new ProtocolManager (db, notifications, userMgmt, user).getVersionById (protocolId);

			if (model == null)
			{
				notifications.addError ("model not found");
				JSONObject obj = new JSONObject ();
					obj.put ("response", false);
					obj.put ("responseText", "model not found");
				answer.put ("newExperiment", obj);
				return answer;
			}
			if (protocol == null)
			{
				notifications.addError ("protocol not found");
				JSONObject obj = new JSONObject ();
					obj.put ("response", false);
					obj.put ("responseText", "protocol not found");
				answer.put ("newExperiment", obj);
				return answer;
			}

			boolean force = false;
			try
			{
				Object o = querry.get ("forceNewVersion");
				System.out.println ("o: " + o);
				if (o != null)
					force = Boolean.parseBoolean (o.toString ());
				System.out.println ("force: " + force);
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				notifications.addError ("force not valid");
			}
			System.out.println ("force after parse: " + force);
			force &= user.isAllowedToForceNewExperiment ();
			System.out.println ("force after perm check: " + force);

			//int expID = 
				createExperiment (db, notifications, new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt), userMgmt, user, model, protocol, modelMgmt, protocolMgmt, force);
			
			// TODO: cronjob for experiments that are not processed after 24h? -> send mails to admins?
			JSONObject obj = new JSONObject ();
				obj.put ("response", true);
				obj.put ("responseText", "Experiment submitted. Based on the size of the Queue it might take some time until we can process your job.");
				//obj.put ("expID", expID);
			answer.put ("newExperiment", obj);
			return answer;
			
		}

		throw new IOException ("nothing to do.");
	}
	
	public static int createExperiment (DatabaseConnector db,
		Notifications notifications, ExperimentManager expMgmt, UserManager userMgmt, User user, ChasteEntityVersion model,
		ChasteEntityVersion protocol, ModelManager modelMgmt, ProtocolManager protocolMgmt, boolean force) throws Exception
	{

		// create archives
		File modelFile = null;
		File protocolFile = null;
		try
		{
				ChasteFileManager fileMgmt = new ChasteFileManager (db, notifications, userMgmt);	
				fileMgmt.getFiles (model, modelMgmt.getEntityFilesTable (), modelMgmt.getEntityColumn ());
				fileMgmt.getFiles (protocol, protocolMgmt.getEntityFilesTable (), protocolMgmt.getEntityColumn ());
			 modelFile = ChasteFileManager.createArchive (model, modelMgmt.getEntityStorageDir ());
			 protocolFile = ChasteFileManager.createArchive (protocol, protocolMgmt.getEntityStorageDir ());

			 modelFile.deleteOnExit ();
			 protocolFile.deleteOnExit ();
		}
		catch (Exception e)
		{
			LOGGER.error ("couldn't create combinde archives to run experiment", e);
			e.printStackTrace ();
			throw new IOException ("error creating combine archives");
		}
		
		// create experiment directory file
		File tmpDir = new File (Tools.getTempDir ());
		if (!tmpDir.exists ())
			if (!tmpDir.mkdirs ())
				throw new IOException ("cannot create temp dir for file upload");
		String signature = null;

		File tmpFile = null;
		while (true)
		{
			signature = UUID.randomUUID().toString();
			tmpFile = new File (tmpDir.getAbsolutePath () + Tools.FILESEP + signature);
			if (!tmpFile.exists ())
				break;
		}
		
		tmpFile.mkdirs ();
		
		// insert in db
		int expID = expMgmt.createVersion (model, protocol, signature, user, force);
		if (expID < 0)
		{
			LOGGER.error ("couldn't create new DB entry for experiment");
			throw new IOException ("couldn't create new DB entry for experiment. (there may already exist an experiment of this model/protocol combination and you're not allowed to overwrite it.)");
		}
		
		
		// send file for processing
		FileTransfer.SubmitResult res = FileTransfer.submitExperiment (modelFile, protocolFile, signature);
		if (!res.result)
		{
			ChasteExperimentVersion exp = (ChasteExperimentVersion) expMgmt.getVersionById (expID);
			expMgmt.updateVersion (exp, res.response, ChasteExperimentVersion.STATUS_INAPPRORIATE);
		}
		else
		{
			ChasteExperimentVersion exp = (ChasteExperimentVersion) expMgmt.getVersionById (expID);
			expMgmt.updateVersion (exp, "running", ChasteExperimentVersion.STATUS_RUNNING);
		}
		//return true;
		return expID;
	}
	
	
	/* (non-Javadoc)
	 * @see uk.ac.ox.cs.chaste.fc.web.WebModule#answerWebRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, uk.ac.ox.cs.chaste.fc.beans.PageHeader, uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector, uk.ac.ox.cs.chaste.fc.beans.Notifications, uk.ac.ox.cs.chaste.fc.beans.User, javax.servlet.http.HttpSession)
	 */
	@Override
	protected String answerWebRequest (HttpServletRequest request,
		HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		return errorPage (request, response, null);
	}
	
	

	private static ChasteEntityVersion getSchedEntity (HttpServletRequest request, HttpSession session, DatabaseConnector db, Notifications notifications, UserManager userMgmt, ChasteEntityManager entityMgmt, String attrName)
	{
		Integer id = (Integer) session.getAttribute (attrName);
		if (id != null)
		{
			ChasteEntityVersion version = entityMgmt.getVersionById (id);
			if (version != null)
			{
				request.setAttribute (attrName + "Name", version.getEntity ().getName () + " @ " + version.getVersion ());
				return version;
			}
		}
		return null;
	}
	
	public static void checkExprimentCreation (HttpServletRequest request, HttpSession session, DatabaseConnector db, Notifications notifications, UserManager userMgmt, User user)
	{
		// is he creating an experiment?
		getSchedEntity (request, session, db, notifications, userMgmt, new ModelManager (db, notifications, userMgmt, user), NEWEXP_MODEL);
		getSchedEntity (request, session, db, notifications, userMgmt, new ProtocolManager (db, notifications, userMgmt, user), NEWEXP_PROTOCOL);
	}
}
