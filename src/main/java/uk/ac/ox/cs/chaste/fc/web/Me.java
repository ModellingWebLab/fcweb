package uk.ac.ox.cs.chaste.fc.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.PageHeaderScript;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.ExperimentManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ModelManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ProtocolManager;

public class Me
extends WebModule
{

	public Me () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		if (!user.isAuthorized ())
			return errorPage (request, response, null);
		
		header.addScript (new PageHeaderScript ("res/js/me.js", "text/javascript", "UTF-8", null));
		
		
		if (request.getServletPath ().endsWith ("myfiles.html"))
		{
			ModelManager modelMgmt = new ModelManager (db, notifications, userMgmt, user);
			ProtocolManager protocolMgmt = new ProtocolManager (db, notifications, userMgmt, user);
			request.setAttribute ("models", modelMgmt.getEntitiesOfAuthor (user.getNick ()));
			request.setAttribute ("protocols", protocolMgmt.getEntitiesOfAuthor (user.getNick ()));
			request.setAttribute ("experiments", new ExperimentManager (db, notifications, userMgmt, user, modelMgmt, protocolMgmt).getEntitiesOfAuthor (user.getNick ()));
			return "MyFiles.jsp";
		}
		
		return "MyAccount.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, 
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException, SQLException
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
		
		if (task.equals ("updatePassword"))
		{
			String oldPw = 	querry.get ("prev").toString ();
			String newPw = 	querry.get ("next").toString ();
			
			JSONObject obj = new JSONObject ();
				obj.put ("response", false);
				obj.put ("responseText", "old password seems to be wrong");
			answer.put ("updatePassword", obj);
			
			if (userMgmt.updatePassword (user, oldPw, newPw, UUID.randomUUID ().toString ()))
			{
				obj.put ("response", true);
				obj.put ("responseText", "changed password. please log in again.");
				user.logout (session);
			}
			
		}
		
		return answer;
	}

}
