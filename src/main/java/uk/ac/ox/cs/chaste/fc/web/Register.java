package uk.ac.ox.cs.chaste.fc.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
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
import uk.ac.ox.cs.chaste.fc.mgmt.Tools;
import de.binfalse.bflog.LOGGER;

public class Register extends WebModule
{
	private static final Pattern PATTERN_MAIL = Pattern.compile(
		"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
		+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
	private static final Pattern PATTERN_NICK = Pattern.compile(
		"^[A-Za-z0-9]+[_A-Za-z0-9-]*$");

	public Register () throws NamingException, SQLException
	{
		super ();
	}

	@Override
	protected String answerWebRequest (HttpServletRequest request, HttpServletResponse response, PageHeader header, DatabaseConnector db,
		Notifications notifications, User user, HttpSession session)
	{
		if (user.isAuthorized ())
			return "Index.jsp";
		
		header.addScript (new PageHeaderScript ("res/js/register.js", "text/javascript", "UTF-8", null));
		return "Register.jsp";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JSONObject answerApiRequest (HttpServletRequest request, HttpServletResponse response, DatabaseConnector db,
		Notifications notifications, JSONObject querry, User user, HttpSession session) throws IOException
	{
		JSONObject answer = new JSONObject();
		
		
		
		Object task = querry.get ("task");
		if (task == null)
		{
			response.setStatus (HttpServletResponse.SC_BAD_REQUEST);
			throw new IOException ("nothing to do.");
		}
		
		String mail = null;
		String nick = null;
		String inst = null;
		String givenName = null;
		String familyName = null;
		
		
		boolean regOk = task.equals ("register");
		if (querry.get ("mail") != null)
		{
			mail = querry.get ("mail").toString ().trim ();
			JSONObject res = new JSONObject ();
			regOk &= validateMail (mail.toString (), res, db);
			answer.put ("mail", res);
		}
		else
			regOk = false;
		
		if (querry.get ("nick") != null)
		{
			nick = querry.get ("nick").toString ().trim ();
			JSONObject res = new JSONObject ();
			regOk &= validateNick (nick.toString (), res, db);
			answer.put ("nick", res);
		}
		else
			regOk = false;
		
		if (querry.get ("inst") != null)
		{
			inst = Tools.validataUserInput (querry.get ("inst").toString ()).trim ();
		}
		else
			inst = "";
		
		if (querry.get ("givenName") != null)
		{
			givenName = Tools.validataUserInput (querry.get ("givenName").toString ()).trim ();
		}
		else
			givenName = "";
		
		if (querry.get ("familyName") != null)
		{
			familyName = Tools.validataUserInput (querry.get ("familyName").toString ()).trim ();
		}
		else
			familyName = "";
		
		if (task.equals ("register") && !regOk)
		{
			JSONObject res = new JSONObject ();
			res.put ("response", false);
			res.put ("responseText", "failed due to previous errors");
			answer.put ("register", res);
		}
		else if (regOk)
		{
				String password = Tools.getPassword (10, 12);
				
				// store the credentials

				PreparedStatement st = db.prepareStatement ("INSERT INTO `user` (`mail`, `password`, `acronym`, `institution`, `cookie`, `givenName`, `familyName`) VALUES (?,MD5(?),?,?,?,?,?)");
				
				try
				{
					st.setString (1, mail.toString ());
					st.setString (2, password);
					st.setString (3, nick);
					st.setString (4, inst);
					st.setString (5, UUID.randomUUID().toString ());
					st.setString (6, givenName);
					st.setString (7, familyName);
					st.executeUpdate ();
					
					
					try
					{
						Tools.sendMail (mail, nick, "Successful Registration for Functional Curation", buildMailBody (nick, password));
						informAdmins (nick);
						
						JSONObject res = new JSONObject ();
						res.put ("response", true);
						res.put ("responseText", "registered successfully");
						answer.put ("register", res);
					}
					catch (MessagingException e)
					{
						e.printStackTrace();
						LOGGER.error ("error sending mail after registration.", e);
						throw new IOException ("cannot send an email.");
					}
				}
				catch (SQLException e)
				{
					e.printStackTrace();
					LOGGER.error ("SQLException: cannot execute statement. not able to register new user", e);
					throw new IOException ("sql problem registering new user.");
				}
				finally
				{
					db.closeRes (st);
				}
		}
		
		return answer;
	}
	
	private void informAdmins (String newUser)
	{
		String mailSubject = "New Registration at Cardiac Functional Curation Web Interface";
		String mailBody = "Hi Admin,\n\nGood news: "+newUser+" just registered for the Cardiac Functional Curation Web Interface!"
			+ "\nPlease go to the Admin interface and check their role:\n\n" + Tools.getThisUrl () + "admin.html"
			+ "\n\nBest wishes,\nCardiac functional curation website";
		
		Vector<User> users = userMgmt.getUsers ();
		for (User u: users)
		{
			if (u.getRole ().equals (User.ROLE_ADMIN))
			{
				try
				{
					Tools.sendMail (u.getMail (), u.getNick (), mailSubject, mailBody);
				}
				catch (UnsupportedEncodingException | MessagingException e)
				{
					LOGGER.error ("could not notify admins about new user registration", e);
				}
			}
		}
	}
	
	private final static String buildMailBody (String nick, String password)
	{
		return "Hi " + nick + ",\n\nWe successfully registered an account for you in the Cardiac Functional Curation system!\n\n"
				+ "Your password is: " + password + "\n\nNow go to " + Tools.getThisUrl () + " to see if everything's working.\n\n"
				+ "Yours sincerely,\nCardiac functional curation website";
	}
	
	@SuppressWarnings("unchecked")
	private boolean validateNick (String nick, JSONObject obj, DatabaseConnector db) throws IOException
	{
		if (nick.length () < 3)
		{
			obj.put ("response", false);
			obj.put ("responseText", "needs to be at least 3 characters in length");
			return false;
		}
		if (nick.length () > 99)
		{
			obj.put ("response", false);
			obj.put ("responseText", "sorry, we didn't expect such a long username. Please contact us to ask for the limit to be extended.");
			return false;
		}

		Matcher regMatcher = PATTERN_NICK.matcher(nick);
		if (!regMatcher.matches())
		{
			obj.put ("response", false);
			obj.put ("responseText", "username has to start with [A-Za-z0-9] and only the following characters are allowed: [_A-Za-z0-9-]");
			return false;
		}

		PreparedStatement st = db.prepareStatement ("SELECT COUNT(*) FROM user WHERE acronym=?");
		ResultSet rs = null;
		
		try
		{
			st.setString (1, nick.toString ());
			st.execute ();
			rs = st.getResultSet();
			rs.next();
			if (rs.getInt(1) > 0)
			{
				obj.put ("response", false);
				obj.put ("responseText", "username already in use");
				return false;
			}
			else
			{
				obj.put ("response", true);
				obj.put ("responseText", "great choice");
				return true;
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			LOGGER.error ("SQLException: cannot execute statement", e);
			throw new IOException ("sql problem");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
    
	}
	
	@SuppressWarnings("unchecked")
	private boolean validateMail (String mail, JSONObject obj, DatabaseConnector db) throws IOException
	{
		
		// some checks for the realization of the mail.
		if (mail.length () < 1)
		{
			obj.put ("response", false);
			obj.put ("responseText", "invalid");
			return false;
		}
		if (mail.length () > 99)
		{
			obj.put ("response", false);
			obj.put ("responseText", "sorry, we didn't expect such a long mail address. Please contact us to have the limit extended.");
			return false;
		}
		
		Matcher regMatcher   = PATTERN_MAIL.matcher(mail);
    if(!regMatcher.matches())
		{
			obj.put ("response", false);
			obj.put ("responseText", "invalid");
			return false;
		}
    

		PreparedStatement st = db.prepareStatement ("SELECT COUNT(*) FROM user WHERE mail=?");
		ResultSet rs = null;
		
		try
		{
			st.setString (1, mail.toString ());
			st.execute ();
			rs = st.getResultSet();
			rs.next();
			if (rs.getInt(1) > 0)
			{
				obj.put ("response", false);
				obj.put ("responseText", "this address is already registered");
				return false;
			}
			else
			{
				obj.put ("response", true);
				obj.put ("responseText", "great choice");
				return true;
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			LOGGER.error ("SQLException: cannot execute statement", e);
			throw new IOException ("sql problem");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
	}
	
}
