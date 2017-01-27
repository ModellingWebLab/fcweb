/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.mgmt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.User;
import uk.ac.ox.cs.chaste.fc.web.FileTransfer;
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
	
	protected boolean isProtocolManager;

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
	
	public User getUser()
	{
		return this.user;
	}
	
	public ChasteEntityManager (DatabaseConnector db, Notifications note, UserManager userMgmt, User user)
	{
		this.userMgmt = userMgmt;
		this.db = db;
		knownEntities = new HashMap<Integer, ChasteEntity> ();
		knownVersions = new HashMap<Integer, ChasteEntityVersion> ();
		this.note = note;
		this.user = user;
		this.isProtocolManager = false;
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
			rs.getString ("visibility"),
			rs.getString ("commitmsg"),
			(isProtocolManager ? rs.getInt ("numterms") : 0)
		);
	}
	
	/**
	 * Convert the results of a database query into a set of entities, with version information attached.
	 * @param rs  the query results
	 * @param neglectPermissions  whether to include all entities (true), or only those the current user has permission to view (false)
	 * @param filterEmptyEntities  whether to filter out entities for which the user cannot see any version
	 * @param sortById  whether to sort by entity id as well as name
	 * @return  entities sorted by name, and optionally by id also
	 * @throws SQLException
	 */
	protected TreeSet<ChasteEntity> evaluateResult (ResultSet rs, boolean neglectPermissions, boolean filterEmptyEntities, boolean sortById) throws SQLException
	{
		Comparator<ChasteEntity> comparator;
		if (sortById)
			comparator = new ChasteEntity.SortByNameAndId();
		else
			comparator = new ChasteEntity.SortByName();
		TreeSet<ChasteEntity> res = new TreeSet<ChasteEntity>(comparator);
		while (rs != null && rs.next ())
		{
			int vid = rs.getInt ("versionid");
			int mid = rs.getInt ("entityid");
			//LOGGER.debug("row " + rs.getRow());

			ChasteEntity cur = null;
			if (knownEntities.get (mid) != null)
			{
				cur = knownEntities.get (mid);
				res.add (cur);
			}
			else
			{
				cur = createEntity (rs);
				if (cur == null)
					continue;
				res.add (cur);
				knownEntities.put (mid, cur);
			}
			
//			LOGGER.debug ("getting version of entity ", res.size(), ": ", cur.getId(), " ", cur.getName());
			
			ChasteEntityVersion ver;
			if (knownVersions.get (vid) != null)
				ver = knownVersions.get (vid);
			else
			{
				ver = createEntityVersion (rs, cur);
				knownVersions.put (vid, ver);
			}
			if (neglectPermissions || user.isAllowedToSeeEntityVersion (ver))
			{
//				LOGGER.debug ("user ", user.getNick(), " allowed", (neglectPermissions ? " (perm ignored)" : ""), ": ", ver.toJson());
				cur.addVersion (ver);
			}
//			else
//				LOGGER.debug ("user ", user.getNick(), " not allowed: ", ver.toJson ());
		}
		if (filterEmptyEntities)
		{
			Iterator<ChasteEntity> it=res.iterator();
			while (it.hasNext())
				if (!it.next().hasVersions())
					it.remove();
		}
		return res;
	}
	
	public int createEntity (String name, User u) throws ChastePermissionException
	{
		if (!user.isAllowedToCreateEntity ())
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
			LOGGER.error (e, "db problem while creating entity (", entityTable, ")");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return id;
	}
	
	public int createVersion (int entityid, String versionName, String commitMsg, String filePath, User u, String visibility) throws ChastePermissionException
	{
		if (!user.isAllowedToCreateEntityVersion ())
			throw new ChastePermissionException ("you are not allowed to create a new entity version");
		if (visibility.equals(ChasteEntityVersion.VISIBILITY_MODERATED) && !user.isAdmin())
			throw new ChastePermissionException ("only an admin may set MODERATED visibility");
		
		PreparedStatement st = db.prepareStatement ("INSERT INTO `" + entityVersionsTable + "`(`author`, `" + entityColumn + "`, `version`, `filepath`, `visibility`, `commitmsg`) VALUES (?,?,?,?,?,?)");
		ResultSet rs = null;
		int id = -1;
		
		try
		{
			st.setInt (1, u.getId ());
			st.setInt (2, entityid);
			st.setString (3, versionName);
			st.setString (4, filePath);
			st.setString (5, visibility);
			st.setString (6, commitMsg);
			
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
			LOGGER.error (e, "db problem while creating entity version (", entityVersionsTable, ")");
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
			evaluateResult (rs, false, false, false);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error (e, "db problem while retrieving entities (", entityColumn, ")");
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
			evaluateResult (rs, false, false, false);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error (e, "db problem while retrieving entities (", entityColumn, ")");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		// TODO: Be more efficient here? Surely we can use the TreeSet<ChasteEntity> given by evaluateResult?
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
			evaluateResult (rs, neglectPermissions, false, false);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entity version: " + e.getMessage ());
			LOGGER.error (e, "db problem while retrieving entity version (", entityColumn, ")");
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
			TreeSet<ChasteEntity> entity = evaluateResult (rs, true, true, false);
			if (entity != null && !entity.isEmpty())
				return entity.first ().getVersionByFilePath (filePath);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entity version: " + e.getMessage ());
			LOGGER.error (e, "db problem while retrieving entity version (", entityColumn, ")");
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
			TreeSet<ChasteEntity> res = evaluateResult (rs, false, false, true);
			db.closeRes (st);
			db.closeRes (rs);
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error (e, "db problem while retrieving entities (", entityColumn, ")");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return null;
	}

	/**
	 * Get the requested entities from the DB, if user can see them.
	 * @param entityIds  the entities to fetch
	 * @param neglectPermissions  if false, only return entity versions the current user has permission to view
	 * @param filterEmptyEntities  if true, don't return entities for which the current user can't see any versions
	 * @return  a set of all (visible) entities of this manager's type
	 */
	public TreeSet<ChasteEntity> getEntities(ArrayList<Integer> entityIds, boolean neglectPermissions, boolean filterEmptyEntities)
	{
		if (entityIds.isEmpty())
			return new TreeSet<ChasteEntity>();
		StringBuilder query_marks = new StringBuilder();
		for (int i=1; i<entityIds.size(); i++)
			query_marks.append("?,");
		query_marks.append("?");
		PreparedStatement st = db.prepareStatement(buildSelectQuery(" WHERE mo.id IN (" + query_marks.toString() + ")"));
		ResultSet rs = null;
		try
		{
			for (int i=0; i<entityIds.size(); i++)
				st.setInt(i+1, entityIds.get(i));
			st.execute();
			rs = st.getResultSet();
			TreeSet<ChasteEntity> res = evaluateResult(rs, neglectPermissions, filterEmptyEntities, false);
			db.closeRes(st);
			db.closeRes(rs);
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError("sql err retrieving entities: " + e.getMessage());
			LOGGER.error(e, "db problem while retrieving entities (", entityColumn, ")");
		}
		finally
		{
			db.closeRes(st);
			db.closeRes(rs);
		}
		return null;
	}
	
	/**
	 * @param neglectPermissions  if false, only return entity versions the current user has permission to view
	 * @param filterEmptyEntities  if true, don't return entities for which the current user can't see any versions
	 * @return  a set of all (visible) entities of this manager's type
	 */
	public TreeSet<ChasteEntity> getAll (boolean neglectPermissions, boolean filterEmptyEntities)
	{
		PreparedStatement st = db.prepareStatement (buildSelectQuery (""));
		ResultSet rs = null;
		try
		{
			st.execute ();
			rs = st.getResultSet ();
			TreeSet<ChasteEntity> res = evaluateResult (rs, neglectPermissions, filterEmptyEntities, false);
			db.closeRes (st);
			db.closeRes (rs);
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving entities: " + e.getMessage ());
			LOGGER.error (e, "db problem while retrieving entities (", entityColumn, ")");
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
		
		cancelExperiments(entityVersionsTable, versionId);
		
		knownVersions.remove(versionId);
		knownEntities.remove(version.getEntity().getId()); // Just in case!
		
		PreparedStatement st = db.prepareStatement ("DELETE FROM `" + entityVersionsTable + "` WHERE `id`=?");
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
			LOGGER.error (e, "db problem while deleting entity version (", entityColumn, ")");
			ok = false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return ok;
	}
	
	/**
	 * If a deleted entity or version references a queued or running experiment (even indirectly) cancel its execution.
	 * 
	 * @param columnName  the table containing the deleted entity/version, which is also the column name in the runningexperiments table to query
	 * @param id  the id of the deleted entity/version
	 */
	private void cancelExperiments(String columnName, int id)
	{
		PreparedStatement st = db.prepareStatement("SELECT task_id FROM `runningexperiments` WHERE `" + columnName + "`=?");
		ResultSet rs = null;
		try
		{
			st.setInt(1, id);
			rs = st.executeQuery();
			ArrayList<String> taskIds = new ArrayList<String>();
			while (rs != null && rs.next())
			{
				String taskId = rs.getString("task_id");
				LOGGER.debug("cancelling expt ", taskId, " due to ", columnName, " ", id);
				taskIds.add(taskId);
			}
			FileTransfer.cancelExperiments(taskIds);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError("SQL error cancelling experiment(s): " + e.getMessage ());
			LOGGER.error(e, "db problem while cancelling experiments");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
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
			LOGGER.error (e, "db problem while retrieving entities (", entityColumn, ")");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
	}
	
	public boolean removeEntity (int entityId) throws ChastePermissionException
	{
		ChasteEntity entity = getEntityById (entityId);
		if (entity == null || !user.isAllowedToDeleteEntity (entity))
			throw new ChastePermissionException ("you are not allowed to delete an entity");
		
		cancelExperiments(entityTable, entityId);
		
		knownEntities.remove(entityId);
		
		// Note: cascading settings in the database ensure that all versions are deleted too.
		PreparedStatement st = db.prepareStatement ("DELETE FROM `" + entityTable + "` WHERE `id`=?");
		ResultSet rs = null;
		boolean ok = false;
		
		try
		{
			st.setInt (1, entityId);
			
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
			LOGGER.error (e, "db problem while deleting entity version (", entityColumn, ")");
			ok = false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return ok;
	}
	
	
	public boolean updateVisibility (ChasteEntityVersion version, String visibility) throws ChastePermissionException
	{
		if (version == null)
			return false;
		if (!version.isValidVisibility(visibility))
			return false;
		if (!user.isAllowedToUpdateEntityVersion(version))
			throw new ChastePermissionException ("you are not allowed to update this entity version");
		if (visibility.equals(ChasteEntityVersion.VISIBILITY_MODERATED) && !user.isAdmin())
			throw new ChastePermissionException ("only admins may set MODERATED visibility");
		
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
			LOGGER.error (e, "db problem while updating visibility of entity version (", entityVersionsTable, ")");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return false;
	}
}
