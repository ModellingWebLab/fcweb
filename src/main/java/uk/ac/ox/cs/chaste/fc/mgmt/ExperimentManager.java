/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.mgmt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.TreeSet;

import de.binfalse.bflog.LOGGER;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperiment;
import uk.ac.ox.cs.chaste.fc.beans.ChasteExperimentVersion;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.User;


/**
 * @author martin
 *
 * TODO: optimize (hashtables etc)
 * 
 * TODO: is current user allowed to see the stuff?
 */
public class ExperimentManager
extends ChasteEntityManager
{
	
	private static final String SQL_SELECT_BEGIN =  
		"SELECT u.id AS versionauthor,"
		+ " m.id AS versionid,"
		+ " m.created AS versioncreated,"
		+ " m.filepath AS versionfilepath,"
		+ " m.finished AS versionfinished,"
		+ " m.status AS versionstatus,"
		+ " m.returnmsg AS versionreturnmsg,"
		+ " m.visibility AS visibility,"
		
		+ " u2.id AS entityauthor,"
		+ " mo.id AS entityid,"
		+ " mo.created AS entitycreated,"
		+ " mo.model AS entitymodel,"
		+ " mo.protocol AS entityprotocol,"
		
		+ " COUNT(mf.file) AS numfiles"
		+ " FROM       `experimentversions` m"
		+ " INNER JOIN `user` u on m.author = u.id"
		+ " INNER JOIN `experiments` mo on m.experiment=mo.id"
		+ " INNER JOIN `user` u2 on mo.author = u2.id"
		+ " LEFT JOIN `experiment_files` mf on mf.experiment = m.id";
	private static final String SQL_SELECT_END = 
		" GROUP BY m.id"
		+ " ORDER BY mo.created DESC, m.created DESC";

	private ChasteEntityManager modelMgmt;
	private ChasteEntityManager protocolMgmt;
	
	public ExperimentManager (DatabaseConnector db, Notifications note, UserManager userMgmt, User user, ChasteEntityManager modelMgmt, ChasteEntityManager protocolMgmt)
	{
		super (db, note, userMgmt, user);
		this.entityTable = "experiments";
		this.entityVersionsTable= "experimentversions";
		this.entityColumn = "experiment";
		this.entityFilesTable = "experiment_files";
		this.entityStorageDir = Tools.getExperimentStorageDir ();

		this.modelMgmt = modelMgmt;
		this.protocolMgmt = protocolMgmt;
	}
	

	@Override
	protected ChasteEntity createEntity (ResultSet rs) throws SQLException
	{
		ChasteEntityVersion model = modelMgmt.getVersionById (rs.getInt ("entitymodel"), true);
		ChasteEntityVersion protocol = protocolMgmt.getVersionById (rs.getInt ("entityprotocol"), true);
		
		if (model == null || protocol == null)
		{
			if (model != null)
				LOGGER.info ("model is: " + model.toJson () + " but protocol missing");
			if (protocol != null)
				LOGGER.info ("protocol is: " + protocol.toJson () + " but model missing");
			return null;
		}
		
		return new ChasteExperiment (
			rs.getInt ("entityid"),
			userMgmt.getUser (rs.getInt ("entityauthor")),
			rs.getTimestamp ("entitycreated"),
			model,
			protocol
			);
	}

	@Override
	protected ChasteEntityVersion createEntityVersion (ResultSet rs, ChasteEntity cur) throws SQLException
	{
		return new ChasteExperimentVersion (
			cur,
			rs.getInt ("versionid"),
			userMgmt.getUser (rs.getInt ("versionauthor")),
			rs.getString ("versionfilepath"),
			rs.getTimestamp ("versioncreated"),
			rs.getInt ("numfiles"),
			rs.getTimestamp ("versionfinished"),
			rs.getString ("versionstatus"),
			rs.getString ("versionreturnmsg"),
			rs.getString ("visibility")
		);
	}
	

	/**
	 * This is not supported for experiments. Use
	 * <pre>
	 * createVersion (int, String, User, String)
	 * </pre>
	 * or
	 * <pre>
	 * createVersion (ChasteEntityVersion, ChasteEntityVersion, String, User, boolean)
	 * </pre>
	 * instead.
	 * 
	 * @deprecated
	 */
	public int createVersion (int entityid, String versionName, String filePath, User u, String visibility)
	{
		throw new IllegalStateException ("not supported for experiments");
	}
	
	/**
	 * This is not supported for experiments. Use
	 * <pre>
	 * createEntity(ChasteEntityVersion, ChasteEntityVersion, User)
	 * </pre>
	 * instead.
	 * 
	 * @deprecated
	 */
	public int createEntity (String name, User u)
	{
		throw new IllegalStateException ("not supported for experiments");
	}
	
	
	
	public int createEntity (ChasteEntityVersion model, ChasteEntityVersion protocol, User u) throws ChastePermissionException
	{
		if (!user.isAllowedToCreateNewExperiment ())
			throw new ChastePermissionException ("you are not allowed to create a new experiment");
		
		PreparedStatement st = db.prepareStatement ("INSERT INTO `" + entityTable + "`(`model`, `protocol`, `author`) VALUES (?,?,?)");
    ResultSet rs = null;
    int id = -1;
		
		try
		{
			st.setInt (1, model.getId ());
			st.setInt (2, protocol.getId ());
			st.setInt (3, u.getId ());
			
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
	
	
	public int createVersion (ChasteEntityVersion model, ChasteEntityVersion protocol, String filePath, User u, boolean force) throws ChastePermissionException
	{
		if (!user.isAllowedToCreateNewExperiment ())
			throw new ChastePermissionException ("you are not allowed to create a new experiment");
		
		ChasteEntity exp = getExperiment (model.getId (), protocol.getId (), false);
		int expId = -1;
		
		if (exp == null)
			expId = createEntity (model, protocol, u);
		else
		{
			if (force)
			{
				if (!user.isAllowedToForceNewExperiment ())
					throw new ChastePermissionException ("you are not allowed to force a new experiment version");
				expId = exp.getId ();
			}
			else
				return -1;
		}
		
		return createVersion (expId, filePath, u, model.getJointVisibility(protocol));
	}
	
	
	private int createVersion (int entityid, String filePath, User u, String visibility) throws ChastePermissionException
	{
		/*if (!user.isAllowedToCreateNewExperiment ())
			throw new ChastePermissionException ("you are not allowed to create a new experiment");*/
		
		if (entityid < 0)
			return entityid;
		
		PreparedStatement st = db.prepareStatement ("INSERT INTO `" + entityVersionsTable + 
			"`(`author`, `" + entityColumn + "`, `filepath`, `returnmsg`, `visibility`) VALUES (?,?,?,?,?)");
		ResultSet rs = null;
		int id = -1;
		
		try
		{
			st.setInt (1, u.getId ());
			st.setInt (2, entityid);
			st.setString (3, filePath);
			st.setString (4, "");
			st.setString (5, visibility);
			
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
	
	public ChasteEntity getExperiment (int modelId, int protocolId, boolean filterEmptyEntities)
	{
		PreparedStatement st = db.prepareStatement (buildSelectQuery (" WHERE mo.model=? AND mo.protocol=?"));
		ResultSet rs = null;
		try
		{
			st.setInt (1, modelId);
			st.setInt (2, protocolId);
			st.execute ();
			rs = st.getResultSet ();
			TreeSet<ChasteEntity> res = evaluateResult (rs, false, filterEmptyEntities);
			if (!res.isEmpty())
				return res.first ();
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
	

	/* (non-Javadoc)
	 * @see uk.ac.ox.cs.chaste.fc.mgmt.ChasteEntityManager#buildSelectQuery(java.lang.String)
	 */
	@Override
	protected String buildSelectQuery (String where)
	{
		return SQL_SELECT_BEGIN + where + SQL_SELECT_END;
	}
	
	public ChasteExperimentVersion getRunningExperiment (String signature)
	{
		/*String SQL_SELECT_BEGIN =  
			"SELECT u.id AS versionauthor,"
			+ " m.id AS versionid,"
			+ " m.created AS versioncreated,"
			+ " m.filepath AS versionfilepath,"
			+ " m.finished AS versionfinished,"
			+ " m.status AS versionstatus,"
			+ " m.returnmsg AS versionreturnmsg,"
			
			+ " u2.id AS entityauthor,"
			+ " mo.id AS entityid,"
			+ " mo.created AS entitycreated,"
			+ " mo.model AS entitymodel,"
			+ " mo.protocol AS entityprotocol,"
			
			+ " COUNT(mf.file) AS numfiles"
			+ " FROM       `experimentversions` m"
			+ " INNER JOIN `user` u on m.author = u.id"
			+ " LEFT JOIN `experiment_files` mf on mf.experiment = m.id"
			+ " INNER JOIN `experiments` mo on m.experiment=mo.id"
			+ " INNER JOIN `user` u2 on mo.author = u2.id";*/
		
		
		
		PreparedStatement st = db.prepareStatement (SQL_SELECT_BEGIN + " WHERE m.filepath=?" + SQL_SELECT_END);
		ResultSet rs = null;
		try
		{
			st.setString (1, signature);
			st.execute ();
			rs = st.getResultSet ();
			TreeSet<ChasteEntity> entity = evaluateResult (rs, true, false);
			if (entity != null && !entity.isEmpty())
				return (ChasteExperimentVersion) entity.first ().getVersionByFilePath (signature);
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


	public boolean updateVersion (ChasteExperimentVersion exp, String returnMsg, String status)
	{
		if (exp == null)
			return false;
		
		PreparedStatement st = db.prepareStatement ("UPDATE `" + entityVersionsTable +
			"` SET `returnmsg`=?, `status`=?, `finished`=? WHERE id=?");
		ResultSet rs = null;
		
		try
		{
			st.setString (1, returnMsg);
			st.setString (2, status);
			st.setTimestamp (3, new Timestamp (System.currentTimeMillis ()));
			st.setInt (4, exp.getId ());
			
			int affectedRows = st.executeUpdate();
			if (affectedRows == 0)
			{
				throw new SQLException("Updating experiment version failed, no rows affected. (" + entityVersionsTable + ")");
			}
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err updating experiment version: " + e.getMessage ());
			LOGGER.error ("db problem while updating experiment version (" + entityVersionsTable + ")", e);
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		return false;
	}

	
	/**
	 * Get all experiments involving the given protocol.
	 * @param filterEmptyEntities  whether to filter out experiments that don't have any visible versions
	 */
	public TreeSet<ChasteEntity> getExperimentsByProtocol (int protocolId, boolean filterEmptyEntities)
	{
		PreparedStatement st = db.prepareStatement (buildSelectQuery (" WHERE mo.protocol=?"));
		ResultSet rs = null;
		try
		{
			st.setInt (1, protocolId);
			st.execute ();
			rs = st.getResultSet ();
			TreeSet<ChasteEntity> res = evaluateResult (rs, false, filterEmptyEntities);
			if (!res.isEmpty())
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
	 * Get all experiments involving the given model.
	 * @param filterEmptyEntities  whether to filter out experiments that don't have any visible versions
	 */
	public TreeSet<ChasteEntity> getExperimentsByModel (int modelId, boolean filterEmptyEntities)
	{
		PreparedStatement st = db.prepareStatement (buildSelectQuery (" WHERE mo.model=?"));
		ResultSet rs = null;
		try
		{
			st.setInt (1, modelId);
			st.execute ();
			rs = st.getResultSet ();
			TreeSet<ChasteEntity> res = evaluateResult (rs, false, filterEmptyEntities);
			if (!res.isEmpty())
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
	 * Get all experiments involving the given entity version for which we are able to see at least one version.
	 */
	public void getExperiments (ChasteEntityVersion vers, String entityColumn)
	{
		TreeSet<ChasteEntity> exp = null;
		if (entityColumn.equals ("model"))
			 exp = getExperimentsByModel (vers.getId (), true);
		else if (entityColumn.equals ("protocol"))
			 exp = getExperimentsByProtocol (vers.getId (), true);
		
		if (exp != null)
		{
			for (ChasteEntity e : exp)
			{
				vers.addExperiment ((ChasteExperiment) e);
			}
		}
	}
}
