/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.mgmt;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import javax.mail.MessagingException;

import de.binfalse.bflog.LOGGER;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.User;


/**
 * @author martin
 *
 */
public class UserManager
{
	public HashMap<Integer, User> knownUsers;
	private Notifications note;
	private DatabaseConnector db;
	
	public UserManager (DatabaseConnector db, Notifications note)
	{
		this.db = db;
		this.note = note;
		knownUsers = new HashMap<Integer, User> ();
	}
	
	public User getUser (int id)
	{
		if (knownUsers.get (id) != null)
			return knownUsers.get (id);
		

		PreparedStatement st = db.prepareStatement (
			"SELECT * FROM `user` WHERE `id`=?");
    ResultSet rs = null;
		try
		{
			st.setInt (1, id);
			st.execute ();
			rs = st.getResultSet ();
			while (rs != null && rs.next ())
			{
				User u = new User (
					rs.getInt ("id"),
					rs.getString ("givenName"),
					rs.getString ("familyName"),
					rs.getString ("mail"),
					rs.getString ("acronym"),
					rs.getString ("institution"),
					rs.getTimestamp ("created"),
					rs.getString ("role"),
					rs.getBoolean ("sendMails")
					);
				knownUsers.put (u.getId (), u);
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving files: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving files", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return knownUsers.get (id);
	}
	
	public Vector<User> getUsers ()
	{
		Vector<User> res = new Vector<User> ();
		

		PreparedStatement st = db.prepareStatement (
			"SELECT * FROM `user`");
    ResultSet rs = null;
		try
		{
			st.execute ();
			rs = st.getResultSet ();
			while (rs != null && rs.next ())
			{
				User u = new User (
					rs.getInt ("id"),
					rs.getString ("givenName"),
					rs.getString ("familyName"),
					rs.getString ("mail"),
					rs.getString ("acronym"),
					rs.getString ("institution"),
					rs.getTimestamp ("created"),
					rs.getString ("role"),
					rs.getBoolean ("sendMails")
					);
				res.add (u);
				knownUsers.put (u.getId (), u);
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving files: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving files", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return res;
	}

	public void updateUserRole (int id, String role)
	{
		// Check whether the role is changing, so we can send them an email if so
		User user = getUser(id);
		if (!user.getRole().equals(role))
		{
			user.setRole(role);
			String body = "Hi " + user.getNick() + ",\n\n"
					+ "Your account permissions have been updated to:\n  " + user.getRoleDescription(false) + "\n"
					+ "Visit the site at " + Tools.getThisUrl() + "\n\n"
					+ "Yours sincerely,\nCardiac functional curation website";
			try
			{
				Tools.sendMail(user.getMail(), user.getNick(), "Cardiac Functional Curation account updated", body);
			}
			catch (MessagingException | UnsupportedEncodingException e)
			{
				LOGGER.error ("couldn't send mail to user (role changed)", e);
			}
		}
		
		// Update the role in the DB
		PreparedStatement st = db.prepareStatement ("UPDATE `user` SET `role`=? WHERE `id`=?");
		ResultSet rs = null;
		
		try
		{
			st.setString (1, role);
			st.setInt (2, id);
			
			int affectedRows = st.executeUpdate();
			if (affectedRows == 0)
			{
				throw new SQLException("Creating file failed, no rows affected.");
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err updating user: " + e.getMessage ());
			LOGGER.error ("db problem while updating user", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
	}
	
	public boolean updatePassword (User user, String oldPw, String newPw, String newCookie) throws SQLException
	{
		PreparedStatement st = db.prepareStatement ("UPDATE `user` SET `password`=MD5(?), `cookie`=? WHERE `id`=? AND `password`=MD5(?)");
    ResultSet rs = null;
		
		try
		{
			st.setString (1, newPw);
			st.setString (2, newCookie);
			st.setInt (3, user.getId ());
			st.setString (4, oldPw);
			
			int affectedRows = st.executeUpdate();
      if (affectedRows == 0)
      {
          return false;//throw new SQLException("Update failed. No such user/password combination.");
      }
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err updating password of user: " + e.getMessage ());
			LOGGER.error ("db problem while updating password of user", e);
      return false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return true;
	}
	
	public boolean updateInstitution (User user, String institution) throws SQLException
	{
		PreparedStatement st = db.prepareStatement ("UPDATE `user` SET `institution`=? WHERE `id`=?");
    ResultSet rs = null;
		
		try
		{
			st.setString (1, institution);
			st.setInt (2, user.getId ());
			
			int affectedRows = st.executeUpdate();
      if (affectedRows == 0)
      {
          return false;//throw new SQLException("Update failed. No such user/password combination.");
      }
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err updating institution of user: " + e.getMessage ());
			LOGGER.error ("db problem while updating institution of user", e);
      return false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return true;
	}
	
	public boolean updateSendMails (User user, boolean sendMails) throws SQLException
	{
		PreparedStatement st = db.prepareStatement ("UPDATE `user` SET `sendMails`=? WHERE `id`=?");
    ResultSet rs = null;
		
		try
		{
			st.setBoolean (1, sendMails);
			st.setInt (2, user.getId ());
			
			int affectedRows = st.executeUpdate();
      if (affectedRows == 0)
      {
          return false;//throw new SQLException("Update failed. No such user/password combination.");
      }
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err updating sendMails of user: " + e.getMessage ());
			LOGGER.error ("db problem while updating sendMails of user", e);
      return false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return true;
	}
}
