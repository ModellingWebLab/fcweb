/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.mgmt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.TreeSet;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.User;
import de.binfalse.bflog.LOGGER;


/**
 * @author martin
 *
 * TODO: optimize (hashtables etc)
 */
public abstract class ChasteEntityManager
{
	protected DatabaseConnector db;
	private HashMap<Integer, ChasteEntity> knownEntities;
	private HashMap<Integer, ChasteEntityVersion> knownVersions;
	protected Notifications note;
	protected UserManager userMgmt;
	protected User user;
	
	protected abstract String buildSelectQuery (String where);
	protected String entityVersionsTable;
	protected String entityTable;
	protected String entityColumn;
	protected String entityFilesTable;
	protected String entityStorageDir;
	
	public String getEntityColumn ()
	{
		return entityColumn;
	}
	
	public String getEntityFilesTable ()
	{
		return entityFilesTable;
	}
	
	public String getEntityStorageDir ()
	{
		return entityStorageDir;
	}
	
	public ChasteEntityManager (DatabaseConnector db, Notifications note, UserManager userMgmt, User user)
	{
		this.userMgmt = userMgmt;
		this.db = db;
		knownEntities = new HashMap<Integer, ChasteEntity> ();
		knownVersions = new HashMap<Integer, ChasteEntityVersion> ();
		this.note = note;
		this.user = user;
	}
	
	protected abstract ChasteEntity createEntity (ResultSet rs) throws SQLException;
	
	protected ChasteEntityVersion createEntityVersion (ResultSet rs, ChasteEntity cur) throws SQLException
	{
		return new ChasteEntityVersion (
			cur,
			rs.getInt ("versionid"),
			rs.getString ("versionname"),
			userMgmt.getUser (rs.getInt ("versionauthor")),
			rs.getString ("versionfilepath"),
			rs.getTimestamp ("versioncreated"),
			rs.getInt ("numfiles"),
			rs.getString ("visibility")
		);
	}
	
	protected TreeSet<ChasteEntity> evaluateResult (ResultSet rs, boolean neglectPermissions) throws SQLException
	{
		TreeSet<ChasteEntity> res = new TreeSet<ChasteEntity> (new ChasteEntity.SortByName ());
		while (rs != null && rs.next ())
		{
			int vid = rs.getInt ("versionid");
			int mid = rs.getInt ("entityid");
			//LOGGER.debug("row " + rs.getRow());

			//System.out.println ("creating cur");
			ChasteEntity cur = null;
			if (knownEntities.get (mid) != null)
			{
				//System.out.println ("was there");
				cur = knownEntities.get (mid);
				res.add (cur);
			}
			else
			{
				//System.out.println ("wasn't there");
				cur = createEntity (rs);
				if (cur == null)
					continue;
				//System.out.println ("found");
				res.add (cur);
				knownEntities.put (mid, cur);
			}
			
			LOGGER.debug ("getting version of entity " + res.size() + ": " + cur.getId() + " " + cur.getName());
			
			if (knownVersions.get (vid) != null)
				cur.addVersion (knownVersions.get (vid));
			else
			{
				ChasteEntityVersion neu = createEntityVersion (rs, cur);
				if (neglectPermissions || user.isAllowedToSeeEntityVersion (neu))
				{
					LOGGER.debug ("user " + user.getNick() + " allowed; version=" + neu.getVersion());
					cur.addVersion (neu);
					knownVersions.put (vid, neu);
				}
				else
					LOGGER.debug ("user " + user.getNick() + " not allowed: " + neu.toJson ());
			}
			
			//System.out.println ("cur w/ version: " + cur.toJson ());
		}
		return res;
	}
	
	public int createEntity (String name, User u) throws ChastePermissionException
	{
		if (!user.isAllowedCreateEntity ())
			throw new ChastePermissionException ("you are not allowed to create a new entity");
		
		PreparedStatement st = db.prepareStatement ("INSERT INTO `" + entityTable + "`(`name`, `author`) VALUES (?,?)");
    ResultSet rs = null;
    int id = -1;
		
		try
		{
			st.setString (1, name);
			st.setInt (2, u.getId ());
			
			int affectedRows = st.executeUpdate();
      if (affectedRows == 0)
      {
          throw new SQLException("Creating entity failed, no rows affected. (" + entityTable + ")");
      }

      rs = st.getGeneratedKeys();
      if (rs.next())
      	id = rs.getInt (1);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err creating entity: " + e.getMessage ());
			LOGGER.error ("db problem while creating entity (" + entityTable + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return id;
	}
	
	public int createVersion (int entityid, String versionName, String filePath, User u) throws ChastePermissionException
	{
		if (!user.isAllowedCreateEntityVersion ())
			throw new ChastePermissionException ("you are not allowed to create a new entity version");
		
		PreparedStatement st = db.prepareStatement ("INSERT INTO `" + entityVersionsTable + "`(`author`, `" + entityColumn + "`, `version`, `filepath`) VALUES (?,?,?,?)");
    ResultSet rs = null;
    int id = -1;
		
		try
		{
			st.setInt (1, u.getId ());
			st.setInt (2, entityid);
			st.setString (3, versionName);
			st.setString (4, filePath);
			
			int affectedRows = st.executeUpdate();
      if (affectedRows == 0)
      {
          throw new SQLException("Creating entity version failed, no rows affected. (" + entityVersionsTable + ")");
      }

      rs = st.getGeneratedKeys();
      if (rs.next())
      	id = rs.getInt (1);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err creating entity version: " + e.getMessage ());
			LOGGER.error ("db problem while creating entity version (" + entityVersionsTable + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return id;
	}
	
	public ChasteEntity getEntityById (int id)
	{
		// we need to execute an sql query. knownProtocols.get (id) may be !null, but it might be incomplete (user searched for certain versions...)
		
		PreparedStatement st = db.prepareStatement (buildSelectQuery (" WHERE mo.id=?"));
		ResultSet rs = null;
		try
		{
			st.setInt (1, id);
			st.execute ();
			rs = st.getResultSet ();
			evaluateResult (rs, false);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving entities (" + entityColumn + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return knownEntities.get (id);
	}
	
	public ChasteEntity getEntityByName (String name)
	{
		// we need to execute an sql query. knownProtocols.get (id) may be !null, but it might be incomplete (user searched for certain versions...)
		
		PreparedStatement st = db.prepareStatement (buildSelectQuery (" WHERE mo.name=?"));
		ResultSet rs = null;
		try
		{
			st.setString (1, name);
			st.execute ();
			rs = st.getResultSet ();
			evaluateResult (rs, false);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving entities (" + entityColumn + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		for (ChasteEntity m : knownEntities.values ())
			if (m.getName ().equals (name))
				return m;
		return null;
	}
	
	public ChasteEntityVersion getVersionById (int id)
	{
		return getVersionById (id, false);
	}
	public ChasteEntityVersion getVersionById (int id, boolean neglectPermissions)
	{
		if (knownVersions.get (id) != null)
			return knownVersions.get (id);
		
		//System.out.println ("getting version by id: " + buildSelectQuery (" WHERE m.id=" + id));
		
		PreparedStatement st = db.prepareStatement (buildSelectQuery (" WHERE m.id=?"));
		ResultSet rs = null;
		try
		{
			st.setInt (1, id);
			st.execute ();
			rs = st.getResultSet ();
			evaluateResult (rs, neglectPermissions);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entity version: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving entity version (" + entityColumn + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return knownVersions.get (id);
	}
	
	public ChasteEntityVersion getVersionByPath (String filePath)
	{
		PreparedStatement st = db.prepareStatement (buildSelectQuery (" WHERE m.filepath=?"));
		ResultSet rs = null;
		try
		{
			st.setString (1, filePath);
			st.execute ();
			rs = st.getResultSet ();
			TreeSet<ChasteEntity> entity = evaluateResult (rs, true);
			if (entity != null && entity.size () > 0)
				return entity.first ().getVersionByFilePath (filePath);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entity version: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving entity version (" + entityColumn + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return null;
	}
	
	public TreeSet<ChasteEntity> getEntitiesOfAuthor (String nick)
	{
		PreparedStatement st = db.prepareStatement (buildSelectQuery (" WHERE u.acronym=?"));
		ResultSet rs = null;
		try
		{
			st.setString (1, nick);
			st.execute ();
			rs = st.getResultSet ();
			TreeSet<ChasteEntity> res = evaluateResult (rs, false);
			db.closeRes (st);
			db.closeRes (rs);
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving entities (" + entityColumn + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return null;
	}
	
	public TreeSet<ChasteEntity> getAll (boolean neglectPermissions)
	{
		PreparedStatement st = db.prepareStatement (buildSelectQuery (""));
		ResultSet rs = null;
		try
		{
			st.execute ();
			rs = st.getResultSet ();
			TreeSet<ChasteEntity> res = evaluateResult (rs, neglectPermissions);
			db.closeRes (st);
			db.closeRes (rs);
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving entities (" + entityColumn + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return null;
	}
	
	
	/**
	 * Removes a version <strong>and all files associated to it</strong>.
	 *
	 * @param versionId the version id
	 * @return true, if successful
	 * @throws ChastePermissionException 
	 */
	public boolean removeVersion (int versionId) throws ChastePermissionException
	{
		
		ChasteEntityVersion version = getVersionById (versionId);
		if (version == null || !user.isAllowedToDeleteEntityVersion (version))
			throw new ChastePermissionException ("you are not allowed to delete an entity version");
		
		PreparedStatement st = db.prepareStatement ("DELETE FROM `"
			+ entityVersionsTable + "` WHERE `id`=?");
		ResultSet rs = null;
		boolean ok = false;
		
		try
		{
			st.setInt (1, versionId);
			
			int affectedRows = st.executeUpdate ();
			
			deleteEmptyEntities ();
			
			if (affectedRows == 0)
				throw new SQLException (
					"Deleting entity version failed, no rows affected.");
			
			ok = true;
		}
		catch (SQLException e)
		{
			e.printStackTrace ();
			note.addError ("sql err deleting entity version: " + e.getMessage ());
			LOGGER.error ("db problem while deleting entity version (" + entityColumn
				+ ")", e);
			ok = false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return ok;
	}
	
	public void deleteEmptyEntities ()
	{
		
		PreparedStatement st = db.prepareStatement ("DELETE FROM `" + entityTable + "` WHERE id NOT IN (SELECT DISTINCT `"+entityColumn+"` FROM `" + entityVersionsTable + "`)");
		ResultSet rs = null;
		try
		{
			st.execute ();
			rs = st.getResultSet ();
			db.closeRes (st);
			db.closeRes (rs);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error ("db problem while retrieving entities (" + entityColumn + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
	}
	
	public boolean removeEntity (int versionId) throws ChastePermissionException
	{
		
		ChasteEntity entity = getEntityById (versionId);
		if (entity == null || !user.isAllowedToDeleteEntity (entity))
			throw new ChastePermissionException ("you are not allowed to delete an entity");
		
		PreparedStatement st = db.prepareStatement ("DELETE FROM `"
			+ entityTable + "` WHERE `id`=?");
		ResultSet rs = null;
		boolean ok = false;
		
		try
		{
			st.setInt (1, versionId);
			
			int affectedRows = st.executeUpdate ();
			if (affectedRows == 0)
				throw new SQLException (
					"Deleting entity version failed, no rows affected.");
			ok = true;
		}
		catch (SQLException e)
		{
			e.printStackTrace ();
			note.addError ("sql err deleting entity version: " + e.getMessage ());
			LOGGER.error ("db problem while deleting entity version (" + entityColumn
				+ ")", e);
			ok = false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return ok;
	}
	
	
	public boolean updateVisibility (ChasteEntityVersion version, String visibility)
	{
		//System.out.println ("version" + version);
		//System.out.println ("visibility" + visibility);
		if (version == null)
			return false;
		if (!(visibility.equals ("PUBLIC") || visibility.equals ("RESTRICTED") || visibility.equals ("PRIVATE")))
			return false;
		
		//System.out.println ("go sql");
		
		PreparedStatement st = db.prepareStatement ("UPDATE `" + entityVersionsTable + 
			"` SET `visibility`=? WHERE id=?");
    ResultSet rs = null;
		
		try
		{
			st.setString (1, visibility);
			st.setInt (2, version.getId ());
			
			int affectedRows = st.executeUpdate();
      if (affectedRows == 0)
      {
          throw new SQLException("Updating visibility of version failed, no rows affected. (" + entityVersionsTable + ")");
      }
      return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err updating visibility of entity version: " + e.getMessage ());
			LOGGER.error ("db problem while updating visibility of entity version (" + entityVersionsTable + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return false;
	}
}
