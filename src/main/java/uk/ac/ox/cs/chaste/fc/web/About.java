package uk.ac.ox.cs.chaste.fc.web;

import java.sql.SQLException;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.PageHeader;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;

public class About extends WebModule {

	private static final long serialVersionUID = 2748971863067247230L;

	public About () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		if (request.getRequestURI().endsWith("for_authors.html"))
			return "ForAuthors.jsp";
		return "About.jsp";
	}

	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, 
		HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session)
	{
		return null;
	}

}
