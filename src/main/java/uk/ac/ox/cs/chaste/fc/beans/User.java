package uk.ac.ox.cs.chaste.fc.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.mgmt.CookieManager;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.Tools;
import uk.ac.ox.cs.chaste.fc.mgmt.UserManager;
import de.binfalse.bflog.LOGGER;


public class User
{
	private static final String COOKIE_NAME = "ChasteUser";
	private static final String SESSION_ATTR = "ChasteUser";

	// Note that these must match the options for the `role` column in the `user` table
	public static final String ROLE_ADMIN = "ADMIN";
	public static final String ROLE_PROTO_AUTHOR = "PROTO_AUTHOR";
	public static final String ROLE_MODELER = "MODELER";
	public static final String ROLE_GUEST = "GUEST";
	
	private String givenName;

	private String familyName;
	private String nick;
	private String mail;
	private String institution;
	private String cookie;
	private Timestamp created;
	private String role;
	private boolean sendMails;

	private int id;
	
	private DatabaseConnector db;
	private Notifications note;
	private CookieManager cookieMgmt;
	
	private Map<String, String> preferences;
	
	public User (DatabaseConnector db, Notifications note, CookieManager cookieMgmt)
	{
		this.db = db;
		this.note = note;
		this.cookieMgmt = cookieMgmt;
		this.preferences = new HashMap <String, String> ();
	}
	
	
	/**
	 * Instantiates a new read-only user. No auth etc. Just to display.
	 */
	public User (int id, String givenName, String familyName, String mail, String nick, String institution,
		Timestamp created, String role, boolean sendMails)
	{
		this.id = id;
		this.givenName = givenName;
		this.familyName = familyName;
		this.mail = mail;
		this.nick = nick;
		this.institution = institution;
		this.created = created;
		this.role =role;
		this.sendMails = sendMails;
		this.preferences = new HashMap <String, String> ();
	}
	
	/**
	 * This method allows you to assume the identity of any user for which the id is known.
	 * It allows an admin account to pretend to be someone else, temporarily.
	 */
	public boolean authById(int id)
	{
		PreparedStatement st = db.prepareStatement ("SELECT * FROM `user` WHERE `id`=?");
		ResultSet rs = null;
		try
		{
			st.setInt (1, id);
			st.execute ();
			rs = st.getResultSet ();
			reqDB (rs);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err: " + e.getMessage ());
			LOGGER.error (e, "db problem during fake auth");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return this.mail != null;
	}
	
	private void reqDB (ResultSet rs) throws SQLException
	{
		if (rs == null)
			return;
		
		while (rs != null && rs.next())
		{
			givenName = rs.getString ("givenName");
			familyName = rs.getString ("familyName");
			nick = rs.getString ("acronym");
			mail = rs.getString ("mail");
			institution = rs.getString ("institution");
			cookie = rs.getString ("cookie");
			role = rs.getString ("role");
			created = rs.getTimestamp ("created");
			sendMails = rs.getBoolean ("sendMails");
			id = rs.getInt ("id");
		}
		
		// get the preferences
		try
		{
			PreparedStatement st = db.prepareStatement ("SELECT `key`, `val` FROM `settings` WHERE `user`=?");
			st.setInt (1, id);
			
			ResultSet res = st.executeQuery ();

			while (res != null && res.next())
				preferences.put (res.getString ("key"), res.getString ("val"));

			db.closeRes (st);
			db.closeRes (res);
		}
		catch (Exception e)
		{
			// let's gice a damn if settings failed...
			LOGGER.error (e, "failed to lookup settings");
		}
	}
	
	/**
	 * Gets the preference.
	 *
	 * @param key the key
	 * @param defaultValue the default value if there is no such pref stored in db
	 * @return the preference associated to this key
	 */
	public String getPreference (String key, String defaultValue)
	{
		String p = preferences.get (key);
		if (p == null)
			return defaultValue;
		return p;
	}
	
	/**
	 * Sets the preference.
	 *
	 * @param key the key
	 * @param value the value to be stored for this key
	 * @return true, if user is authenticated and db was updated successfully
	 */
	public boolean setPreference (String key, String value)
	{
		if (!this.isAuthorized ())
			return false;
		
		if (preferences.get (key) == null)
		{
			// insert to db
			PreparedStatement st = db.prepareStatement ("INSERT INTO `settings` (`user`, `key`, `val`) VALUES (?, ?, ?);");
			try
			{
				st.setInt (1, id);
				st.setString (2, key);
				st.setString (3, value);
				st.execute ();
			}
			catch (SQLException e)
			{
				LOGGER.error (e, "failed to insert new preferences to db");
			}
		}
		else
		{
			// update db entry
			PreparedStatement st = db.prepareStatement ("UPDATE SET `val`=? WHERE `user`=? AND `key`=?;");
			try
			{
				st.setString (1, value);
				st.setInt (2, id);
				st.setString (3, key);
				st.execute ();
			}
			catch (SQLException e)
			{
				LOGGER.error (e, "failed to insert new preferences to db");
			}
		}
		preferences.put (key, value);
		return true;
	}
	
	public boolean isAuthorized ()
	{
		return mail != null;
	}
	
	public boolean authByForm (HttpSession session, String mail, String password, boolean remember)
	{
		PreparedStatement st = db.prepareStatement ("SELECT * FROM `user` WHERE `mail`=? AND `password`=MD5(?)");
		ResultSet rs = null;
		try
		{
			st.setString (1, mail);
			st.setString (2, password);
			st.execute ();
			rs = st.getResultSet ();
			reqDB (rs);
			if (this.mail != null)
			{
				session.setAttribute (SESSION_ATTR, this.mail);
				if (cookie != null && cookie.length () > 5 && remember)
					setCookie ();
			}
			return this.mail != null;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err: " + e.getMessage ());
			LOGGER.error (e, "db problem during session auth");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return false;
	}
	
	
	public String toString ()
	{
		return nick;
	}
	
	public String getNick ()
	{
		return nick;
	}

	
	public void setMail (String mail)
	{
		this.mail = mail;
	}

	
	public String getMail ()
	{
		return mail;
	}

	
	public String getInstitution ()
	{
		return institution;
	}

	
	public String getCreated ()
	{
		return Tools.formatTimeStamp (created);
	}

	
	public void setRole (String role)
	{
		this.role = role;
	}
	
	public String getRole ()
	{
		return role;
	}
	
	public String getRoleDescription (boolean contactUrl)
	{
		String contact = "contact us";
		if (contactUrl)
		{
			contact = "<a href='contact.html'>" + contact + "</a>";
		}
		if (role.equals(ROLE_ADMIN))
		{
			return "Administrator: you have full administrative privileges.";
		}
		if (role.equals(ROLE_GUEST))
		{
			return "Guest: to upload new models please " + contact + ".";
		}
		if (role.equals(ROLE_MODELER))
		{
			return "Modeller: you may add your models to the system; to upload your own protocols please " + contact + ".";
		}
		if (role.equals(ROLE_PROTO_AUTHOR))
		{
			return "Advanced modeller: you may upload both models and protocols.";
		}
		return "";
	}
	
	public String getRoleDescription ()
	{
		return getRoleDescription(true);
	}
	
	public int getId ()
	{
		return id;
	}
	
	public String getGivenName ()
	{
		return givenName;
	}


	
	public String getFamilyName ()
	{
		return familyName;
	}

	
	public Notifications getNote ()
	{
		return note;
	}
	
	public boolean isSendMails ()
	{
		return sendMails;
	}
	
	public void logout (HttpSession session)
	{
		mail = null;
		session.setAttribute (SESSION_ATTR, null);
		Cookie userCookie = new Cookie (COOKIE_NAME, "");
		userCookie.setMaxAge (1);
		cookieMgmt.setCookie (userCookie);
	}

	public void authByRequest (HttpServletRequest request, HttpSession session, UserManager userMgmt)
	{
		String mail = (String) session.getAttribute (SESSION_ATTR);
		//System.out.println ("mail: " + mail);
		if (mail != null)
		{
			PreparedStatement st = db.prepareStatement ("SELECT * FROM `user` WHERE `mail`=?");
			ResultSet rs = null;
			try
			{
				st.setString (1, mail);
				st.execute ();
				rs = st.getResultSet ();
				reqDB (rs);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				note.addError ("sql err: " + e.getMessage ());
				LOGGER.error (e, "db problem during session auth");
			}
			finally
			{
				db.closeRes (st);
				db.closeRes (rs);
			}
		}
		Cookie cook = cookieMgmt.getCookie (COOKIE_NAME);
		if (cook != null)
		{
			if (this.mail == null)
			{
				// not yet authed
				String val = cook.getValue ();
				PreparedStatement st = db.prepareStatement ("SELECT * FROM `user` WHERE `cookie`=?");
				ResultSet rs = null;
				try
				{
					st.setString (1, val);
					st.execute ();
					rs = st.getResultSet ();
					reqDB (rs);
					rs.close ();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
					note.addError ("sql err: " + e.getMessage ());
					LOGGER.error (e, "db problem during session auth");
				}
				finally
				{
					db.closeRes (st);
					db.closeRes (rs);
				}
			}
			
			if (this.mail != null)
			{
				// we're authed -> reset cookie
				setCookie ();
			}
		}
		//System.out.println ("auth by req: " + isAuthorized ());
	}
	
	private void setCookie ()
	{
		Cookie userCookie = new Cookie (COOKIE_NAME, cookie);
		userCookie.setMaxAge (60*60*24*90);
		cookieMgmt.setCookie (userCookie);
	}
	
	
	
	
	

	
	////////////////////////// DRM /////////////////////////////
	
	@SuppressWarnings("unchecked")
	public String getRoleDump ()
	{
		JSONObject obj = new JSONObject ();
		obj.put ("isAllowedToForceNewExperiment", isAllowedToForceNewExperiment ());
		obj.put ("isAllowedToCreateNewExperiment", isAllowedToCreateNewExperiment ());
		obj.put ("isAllowedCreateEntity", isAllowedCreateEntity ());
		obj.put ("isAllowedCreateEntityVersion", isAllowedCreateEntityVersion ());
		/*obj.put ("",  ());
		obj.put ("",  ());
		obj.put ("",  ());*/
		return obj.toJSONString ();
	}
	
	public boolean isAdmin ()
	{
		return isAuthorized () && role.equals (ROLE_ADMIN);
	}
	
	public boolean isAllowedToForceNewExperiment ()
	{
		return isAuthorized () && role.equals (ROLE_ADMIN);
	}
	
	public boolean isAllowedToCreateNewExperiment ()
	{
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_PROTO_AUTHOR) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedToDeleteEntityVersion (ChasteEntityVersion version)
	{
		return isAuthorized () && (((role.equals (ROLE_MODELER) || role.equals (ROLE_PROTO_AUTHOR)) && version.getAuthor ().getId () == id) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedToDeleteEntity (ChasteEntity entity)
	{
		return isAuthorized () && (((role.equals (ROLE_MODELER) || role.equals (ROLE_PROTO_AUTHOR)) && entity.getAuthor ().getId () == id) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedCreateModel ()
	{
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_PROTO_AUTHOR) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedCreateProtocol ()
	{
		return isAuthorized () && (role.equals (ROLE_PROTO_AUTHOR) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedCreateEntity ()
	{
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_PROTO_AUTHOR) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedCreateEntityVersion ()
	{
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_PROTO_AUTHOR) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedToUpload ()
	{
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_PROTO_AUTHOR) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedToSeeEntityVersion (ChasteEntityVersion version)
	{
		String vis = version.getVisibility ();

		if (vis.equals (ChasteEntityVersion.VISIBILITY_PUBLIC))
			return true;
		if (vis.equals (ChasteEntityVersion.VISIBILITY_RESTRICTED))
			return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_PROTO_AUTHOR) || role.equals (ROLE_ADMIN));
		
		return isAuthorized () && version.getAuthor ().getId () == id;
	}
	
	
}
