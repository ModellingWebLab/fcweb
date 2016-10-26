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
import uk.ac.ox.cs.chaste.fc.beans.ChasteFile;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.PageHeaderScript;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteEntityManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ChasteFileManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ChastePermissionException;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.ModelManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ProtocolManager;
import de.binfalse.bflog.LOGGER;

public class Fitting extends WebModule
{
	private static final long	serialVersionUID	= 8100907290557329579L;

	public Fitting () throws NamingException, SQLException
	{
		super();
	}

	@Override
	protected String answerWebRequest(HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		if (!user.isAuthorized() || !user.isAllowedCreateEntityVersion())
		{
			notifications.addError("you must be logged in and your account approved to perform fitting");
			return errorPage(request, response, null);
		}
		
		header.addScript(new PageHeaderScript("res/js/fitting.js", "text/javascript", "UTF-8", null));
		// May want to send some data for the JSP to use, like:
//		request.setAttribute("Users", userMgmt.getUsers());
		// I'm thinking particularly the initial list of template fitting experiments, cf EntityView.java.
		// Some code fragments stolen from elsewhere below - will need checking/testing/updating!
		
		ProtocolManager protocolMgmt = new ProtocolManager(db, notifications, userMgmt, user);
		TreeSet<ChasteEntity> entity = protocolMgmt.getAll(false, true);
		ChasteFileManager fileMgmt = new ChasteFileManager(db, notifications, userMgmt);
		Vector<ChasteEntityVersion> entities = new Vector<ChasteEntityVersion>();
		for (ChasteEntity e : entity)
		{
			ChasteEntityVersion v = e.getLatestVersion(); // Show just latest version of each; probably better than listing all versions?
			fileMgmt.getFiles(v, protocolMgmt.getEntityFilesTable(), protocolMgmt.getEntityColumn());
			if (v.getNumFiles() > 1)
				entities.add(v); 
		}
		request.setAttribute("FittingProtocols", entities);
		
		return "Fitting.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest(HttpServletRequest request, HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject query, User user, HttpSession session) throws IOException, ChastePermissionException
	{
		if (!user.isAuthorized() || !user.isAllowedCreateEntityVersion())
		{
			notifications.addError("you must be logged in and your account approved to perform fitting");
			throw new ChastePermissionException("not allowed.");
		}
		
		JSONObject answer = new JSONObject();
		
		Object task = query.get("task");
		if (task == null)
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException("nothing to do.");
		}
		
		if (task.equals("getFittingProtocol"))
		{
			ProtocolManager protocolMgmt = new ProtocolManager(db, notifications, userMgmt, user);
			ChasteFileManager fileMgmt = new ChasteFileManager(db, notifications, userMgmt);

			int protoId = ((Long)query.get("id")).intValue();
			ChasteEntityVersion v = protocolMgmt.getVersionById(protoId);
			if (v == null)
			{
				notifications.addError("requested template not found");
			}
			else
			{
				answer.put("name", v.getName());
				answer.put("visibility", v.getVisibility());
				
				fileMgmt.getFiles(v, protocolMgmt.getEntityFilesTable(), protocolMgmt.getEntityColumn());
				Vector<ChasteFile> files = v.getFiles();
				for (ChasteFile f : files)
				{
					JSONObject fileRep = f.toJson();
					if (f.isMasterFile())
						answer.put("fitProto", fileRep);
					else if (f.getName().toLowerCase().endsWith(".csv"))
						answer.put("dataFile", fileRep);
					else
						answer.put("simProto", fileRep);
				}
			}

			answer.put("response", query.get("id"));
		}
		else if (task.equals("getModelList"))
		{
			JSONObject obj = new JSONObject();
			answer.put("latestVersions", obj);
			
			ChasteEntityManager modelMgmt = new ModelManager(db, notifications, userMgmt, user);
			ChasteFileManager fileMgmt = new ChasteFileManager(db, notifications, userMgmt);
			for (ChasteEntity entity : modelMgmt.getAll(false, true))
			{
				ChasteEntityVersion version = entity.getLatestVersion();
				fileMgmt.getFiles(version, modelMgmt.getEntityFilesTable(), modelMgmt.getEntityColumn());
				obj.put(version.getId(), version.toJson());
			}
		}
		
		return answer;
	}

}
