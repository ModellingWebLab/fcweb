/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.mgmt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;

import de.binfalse.bflog.LOGGER;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntity;
import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.User;


/**
 * @author martin
 *
 * TODO: optimize (hashtables etc)
 * 
 * TODO: is current user allowed to see the stuff?
 */
public class ProtocolManager
extends ChasteEntityManager
{
	private static final String SQL_SELECT_BEGIN =  
		"SELECT u.id AS versionauthor,"
		+ " m.id AS versionid,"
		+ " m.version AS versionname,"
		+ " m.created AS versioncreated,"
		+ " m.filepath AS versionfilepath,"
		+ " m.visibility AS visibility,"
		+ " m.commitmsg AS commitmsg,"
		+ " u2.id AS entityauthor,"
		+ " mo.id AS entityid,"
		+ " mo.name AS entityname,"
		+ " mo.created AS entitycreated,"
		+ " COUNT(mf.file) AS numfiles,"
		+ " COUNT(pi.term) AS numterms"
		+ " FROM       `protocolversions` m"
		+ " INNER JOIN `user` u on m.author = u.id"
		+ " INNER JOIN `protocol_files` mf on mf.protocol = m.id"
		+ " INNER JOIN `protocols` mo on m.protocol=mo.id"
		+ " INNER JOIN `user` u2 on mo.author = u2.id"
		+ " LEFT JOIN  `protocolinterface` pi on m.id = pi.protocolversion";
	private static final String SQL_SELECT_END = 
		" GROUP BY m.id"
		+ " ORDER BY mo.name, m.version";
	
	public ProtocolManager (DatabaseConnector db, Notifications note, UserManager userMgmt, User user)
	{
		super (db, note, userMgmt, user);
		this.isProtocolManager = true;
		this.entityTable = "protocols";
		this.entityVersionsTable= "protocolversions";
		this.entityColumn = "protocol";
		this.entityFilesTable = "protocol_files";
		this.entityStorageDir = Tools.getProtocolStorageDir ();
	}

	@Override
	protected String buildSelectQuery (String where)
	{
		return SQL_SELECT_BEGIN + where + SQL_SELECT_END;
	}

	@Override
	protected ChasteEntity createEntity (ResultSet rs) throws SQLException
	{
		return new ChasteEntity (
			rs.getInt ("entityid"),
			rs.getString ("entityname"),
			userMgmt.getUser (rs.getInt ("entityauthor")),
			rs.getTimestamp ("entitycreated"),
			"protocol"
			);
	}

	/**
	 * Find all protocol version ids for which the interface (the set of ontology terms they reference) has not been determined.
	 */
	public Map<Integer, String> getVersionsWithUnknownInterface()
	{
		PreparedStatement st = db.prepareStatement("SELECT m.id as id, m.filepath AS path, COUNT(pi.term) AS numterms"
				+ " FROM `protocolversions` m LEFT JOIN `protocolinterface` pi on m.id = pi.protocolversion GROUP BY m.id HAVING numterms = 0");
		ResultSet rs = null;
		HashMap<Integer, String> res = new HashMap<Integer, String>();
		try
		{
			st.execute();
			rs = st.getResultSet();
			while (rs != null && rs.next())
			{
				res.put(rs.getInt("id"), rs.getString("path"));
			}
			db.closeRes(st);
			db.closeRes(rs);
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
		return res;
	}
	
	/**
	 * Update the ontology term interface recorded for the given protocol.
	 * @param protoVer  the protocol version to update
	 * @param requiredTerms  the terms that must be present in compatible models
	 * @param optionalTerms  the terms that may additionally be used in compatible models
	 */
	public void updateInterface(ChasteEntityVersion protoVer, JSONArray requiredTerms, JSONArray optionalTerms)
	{
		// Remove any old interface info
		PreparedStatement st = db.prepareStatement("DELETE FROM `protocolinterface` WHERE protocolversion=?");
		try
		{
			st.setInt(1, protoVer.getId());
			st.executeUpdate();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError("database error updating protocol interface: " + e.getMessage());
			LOGGER.error(e, "db problem removing old protocol interface");
		}
		finally
		{
			db.closeRes(st);
		}
		
		// Put the new interface in the DB
		st = db.prepareStatement("INSERT INTO `protocolinterface` (`protocolversion`, `optional`, `term`) VALUES (?,?,?)");
		try
		{
			int id = protoVer.getId();
			for (Object term : requiredTerms)
			{
				String t = (String)term;
				if (t != null)
					addTerm(st, id, false, t);
			}
			for (Object term : optionalTerms)
			{
				String t = (String)term;
				if (t != null)
					addTerm(st, id, true, t);
			}
			// Note for ourselves that we have analysed the interface, even if it was empty!
			addTerm(st, id, false, "");
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError("database error updating protocol interface: " + e.getMessage());
			LOGGER.error(e, "db problem adding new protocol interface");
		}
		finally
		{
			db.closeRes(st);
		}
	}
	
	/**
	 * Utility method for updateInterface.
	 * TODO: Consider batching this operation!
	 * @throws SQLException 
	 */
	private void addTerm(PreparedStatement st, int id, boolean optional, String term) throws SQLException
	{
		st.setInt(1, id);
		st.setBoolean(2, optional);
		st.setString (3, term);
		int affectedRows = st.executeUpdate();
		if (affectedRows == 0)
		{
			throw new SQLException("Updating protocol interface failed: no rows affected.");
		}
		
	}
}
