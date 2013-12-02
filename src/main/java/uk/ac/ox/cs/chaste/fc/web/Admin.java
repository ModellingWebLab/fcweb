package uk.ac.ox.cs.chaste.fc.web;

import java.io.IOException;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.PageHeaderScript;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.ChastePermissionException;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.UserManager;
import de.binfalse.bflog.LOGGER;

public class Admin extends WebModule
{
	private static final long	serialVersionUID	= 8100907290557329579L;

	public Admin () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		if (!user.isAuthorized () || !user.getRole ().equals ("ADMIN"))
		{
			notifications.addError ("only admins are allowed to access this page");
			return errorPage (request, response, null);
		}
		
		header.addScript (new PageHeaderScript ("res/js/admin.js", "text/javascript", "UTF-8", null));

		request.setAttribute ("Users", userMgmt.getUsers ());
		
		return "Admin.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, 
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException, ChastePermissionException
	{
		if (!user.isAuthorized () || !user.getRole ().equals ("ADMIN"))
		{
			notifications.addError ("only admins are allowed to access this page");
			throw new ChastePermissionException ("not allowed.");
		}
		
		JSONObject answer = new JSONObject();
		
		
		Object task = querry.get ("task");
		if (task == null)
		{
			response.setStatus (HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException ("nothing to do.");
		}
		
		if (task.equals ("updateUserRole"))
		{
			int id = -1;
			try
			{
				id = Integer.parseInt (querry.get ("user").toString ());
			}
			catch (NumberFormatException | NullPointerException e)
			{
				e.printStackTrace ();
				LOGGER.warn ("cannot parse user id from user provided string: " + querry.get ("user"));
				throw new IOException ("user not found");
			}
			
			if (id == user.getId ())
				throw new IOException ("not allowed to change your own rule");
			
			Object role = querry.get ("role");
			if (role.equals (UserManager.ROLE_ADMIN))
				userMgmt.updateUserRole (id, UserManager.ROLE_ADMIN);
			else if (role.equals (UserManager.ROLE_MODELER))
				userMgmt.updateUserRole (id, UserManager.ROLE_MODELER);
			else if (role.equals (UserManager.ROLE_GUEST))
				userMgmt.updateUserRole (id, UserManager.ROLE_GUEST);
			else
				throw new IOException ("unknown rule");
			
			JSONObject obj = new JSONObject ();
			obj.put ("response", true);
			obj.put ("responseText", "updated user");
			answer.put ("updateUserRole", obj);
		}
		
		return answer;
	}

}
