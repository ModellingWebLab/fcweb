/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.web;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

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
	private static final long serialVersionUID = 8633946624445531956L;


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
			throw new IOException ("You do not have permission to do that. Are you logged in?");
		
		
		JSONObject answer = new JSONObject();

		Object task = querry.get ("task");
		if (task == null)
		{
			response.setStatus (HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException ("nothing to do.");
		}

		if (task.equals ("newExperiment"))
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
				if (o != null)
					force = Boolean.parseBoolean (o.toString ());
			}
			catch (Exception e)
			{
				e.printStackTrace ();
				notifications.addError ("force not valid");
			}
			force &= user.isAllowedToForceNewExperiment ();

			ChasteExperimentVersion expVer =
				createExperiment (db, notifications, new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt), userMgmt, user, model, protocol, modelMgmt, protocolMgmt, force);
			
			// TODO: cronjob for experiments that are not processed after 24h? -> send mails to admins?
			JSONObject obj = new JSONObject ();
				obj.put ("response", true);
				obj.put ("responseText", "Experiment submitted. Based on the size of the queue it might take some time until we can process your job.");
				obj.put ("versionId", expVer.getId());
				obj.put ("expId", expVer.getExperiment().getId());
				obj.put ("expName", expVer.getName());
			answer.put ("newExperiment", obj);
			return answer;
			
		}

		throw new IOException ("nothing to do.");
	}
	
	public static ChasteExperimentVersion createExperiment (DatabaseConnector db,
		Notifications notifications, ExperimentManager expMgmt, UserManager userMgmt, User user, ChasteEntityVersion model,
		ChasteEntityVersion protocol, ModelManager modelMgmt, ProtocolManager protocolMgmt, boolean force) throws Exception
	{
		// Create temporary folder for uploading experiment results, and hence unique signature for submission
		File tmpFile = Tools.createUniqueSubDir(Tools.getTempDir());
		String signature = tmpFile.getName();
		
		// insert in db
		int expID = expMgmt.createVersion (model, protocol, signature, user, force);
		if (expID < 0)
		{
			LOGGER.error ("couldn't create new DB entry for experiment");
			throw new IOException ("couldn't register experiment (there may already exist an experiment of this model/protocol combination and you're not allowed to overwrite it)");
		}
		
		// submit for processing
		FileTransfer.SubmitResult res = null;
		try
		{
			res = FileTransfer.submitExperiment(model.getId(), protocol.getId(), signature, user);
		}
		catch (Exception e)
		{
			String msg = e.toString();
			Throwable cause = e.getCause();
			while (cause != null)
			{
				msg += "\n" + cause.toString();
				cause = cause.getCause();
			}
			ChasteExperimentVersion exp = (ChasteExperimentVersion) expMgmt.getVersionById (expID);
			expMgmt.updateVersion (exp, msg, ChasteExperimentVersion.STATUS_FAILED);
			
			throw e;
		}
		
		ChasteExperimentVersion exp;
		if (!res.result)
		{
			exp = (ChasteExperimentVersion) expMgmt.getVersionById (expID);
			if (exp.getStatus().equals(ChasteExperimentVersion.STATUS_QUEUED))
				expMgmt.updateVersion (exp, res.response, res.status);
		}
		else
		{
			// Experiment was queued successfully. Status is QUEUED by default so no need to update, but do need to store task id.
			exp = (ChasteExperimentVersion) expMgmt.getVersionById (expID);
			exp.setTaskId(expMgmt, res.response);
		}
		return exp;
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
}
