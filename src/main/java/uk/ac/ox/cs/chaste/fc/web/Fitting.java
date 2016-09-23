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
		
		return "Fitting.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest(HttpServletRequest request, HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException, ChastePermissionException
	{
		if (!user.isAuthorized() || !user.isAllowedCreateEntityVersion())
		{
			notifications.addError("you must be logged in and your account approved to perform fitting");
			throw new ChastePermissionException("not allowed.");
		}
		
		JSONObject answer = new JSONObject();
		
		Object task = querry.get("task");
		if (task == null)
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException("nothing to do.");
		}
		
		if (task.equals("TODO define tasks!"))
		{
			// Can send data back to JS like:
//			JSONObject obj = new JSONObject();
//			obj.put("response", true);
//			obj.put("responseText", "updated user permissions");
//			answer.put("updateUserRole", obj);
		}
		
		return answer;
	}

}
