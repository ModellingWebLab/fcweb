package uk.ac.ox.cs.chaste.fc.beans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.mgmt.CookieManager;
import uk.ac.ox.cs.chaste.fc.mgmt.DatabaseConnector;
import uk.ac.ox.cs.chaste.fc.mgmt.Tools;
import uk.ac.ox.cs.chaste.fc.mgmt.UserManager;
import uk.ac.ox.cs.chaste.fc.web.NewExperiment;
import de.binfalse.bflog.LOGGER;


public class User
{
	private static final String COOKIE_NAME = "ChasteUser";
	private static final String SESSION_ATTR = "ChasteUser";

	public static final String ROLE_ADMIN = "ADMIN";
	public static final String ROLE_GUEST = "GUEST";
	public static final String ROLE_MODELER = "MODELER";
	
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
	
	public User (DatabaseConnector db, Notifications note, CookieManager cookieMgmt)
	{
		this.db = db;
		this.note = note;
		this.cookieMgmt = cookieMgmt;
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
			LOGGER.error ("db problem during session auth", e);
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
		// is he creating an experiment?
		NewExperiment.checkExprimentCreation (request, session, db, note, userMgmt, this);
		
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
				LOGGER.error ("db problem during session auth", e);
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
					LOGGER.error ("db problem during session auth", e);
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
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedToDeleteEntityVersion (ChasteEntityVersion version)
	{
		return isAuthorized () && ((role.equals (ROLE_MODELER) && version.getAuthor ().getId () == id) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedToDeleteEntity (ChasteEntity entity)
	{
		return isAuthorized () && ((role.equals (ROLE_MODELER) && entity.getAuthor ().getId () == id) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedCreateEntity ()
	{
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedCreateEntityVersion ()
	{
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedToUpload ()
	{
		return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_ADMIN));
	}
	
	public boolean isAllowedToSeeEntityVersion (ChasteEntityVersion version)
	{
		String vis = version.getVisibility ();

		if (vis.equals (ChasteEntityVersion.VISIBILITY_PUBLIC))
			return true;
		if (vis.equals (ChasteEntityVersion.VISIBILITY_RESTRICTED))
			return isAuthorized () && (role.equals (ROLE_MODELER) || role.equals (ROLE_ADMIN));
		
		return isAuthorized () && version.getAuthor ().getId () == id;
	}
	
	
}
