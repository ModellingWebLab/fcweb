/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.mgmt;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

import uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion;
import uk.ac.ox.cs.chaste.fc.beans.ChasteFile;
import uk.ac.ox.cs.chaste.fc.beans.Notifications;
import uk.ac.ox.cs.chaste.fc.beans.User;
import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineFormats;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;


/**
 * @author martin
 *
 * TODO: is current user allowed to see the stuff?
 */
public class ChasteFileManager
{
	private DatabaseConnector db;
	private HashMap<Integer, ChasteFile> knownFiles;
	private Notifications note;
	private UserManager userMgmt;
	
	public ChasteFileManager (DatabaseConnector db, Notifications note, UserManager userMgmt)
	{
		this.userMgmt = userMgmt;
		this.db = db;
		this.note = note;
		knownFiles = new HashMap<Integer, ChasteFile> ();
	}
	
	
	private Vector<ChasteFile> evaluateResult (ResultSet rs) throws SQLException
	{
		Vector<ChasteFile> res = new Vector<ChasteFile> ();
		while (rs != null && rs.next ())
		{
			
			int id = rs.getInt ("fileid");
			
			if (knownFiles.get (id) != null)
				res.add (knownFiles.get (id));
			else
			{
				ChasteFile file = new ChasteFile (
					id,
					rs.getString ("filepath"),
					rs.getTimestamp ("filecreated"),
					//rs.getString ("filevis"),
					rs.getString ("filetype"),
					rs.getLong ("filesize"),
					userMgmt.getUser (rs.getInt ("author")),
					rs.getBoolean ("masterFile")
					);
				knownFiles.put (id, file);
				res.add (file);
			}
		}
		return res;
		
	}


	public boolean removeFile (int fileId)
	{
		PreparedStatement st = db.prepareStatement ("DELETE FROM `files` WHERE `id`=?");
	    ResultSet rs = null;
	    boolean ok = false;
		
		try
		{
			st.setInt (1, fileId);
			
			int affectedRows = st.executeUpdate();
	      if (affectedRows == 0)
	          throw new SQLException("Removing file failed, no rows affected.");
	      ok = true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err removing file: " + e.getMessage ());
			LOGGER.error (e, "db problem while removing file");
			ok = false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return ok;
	}
	
	public int addFile (String name, String type, User u, long size, boolean mainFile)
	{
		return addFile (name, type, u.getId (), size, mainFile);
	}
	
	public int addFile (String name, String type, int user, long size, boolean mainFile)
	{
		PreparedStatement st = db.prepareStatement ("INSERT INTO `files`(`relpath`, `type`, `author`, `size`, `masterFile`) VALUES (?,?,?,?,?)");
	    ResultSet rs = null;
	    int id = -1;
		
		try
		{
			st.setString (1, name);
			st.setString (2, type);
			st.setInt (3, user);
			st.setLong (4, size);
			st.setBoolean (5, mainFile);
			
			int affectedRows = st.executeUpdate();
			if (affectedRows == 0)
			{
				throw new SQLException("Creating file failed, no rows affected.");
			}

			rs = st.getGeneratedKeys();
			if (rs.next())
				id = rs.getInt (1);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err adding file: " + e.getMessage ());
			LOGGER.error (e, "db problem while adding file");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return id;
	}
	

	// TODO: Implement this method by calling the variant below!
	public boolean associateFile (int fileId, ChasteEntityVersion entity, ChasteEntityManager entityMgmt)
	{
		PreparedStatement st = db.prepareStatement ("INSERT INTO `"+entityMgmt.getEntityFilesTable ()+"`(`"+entityMgmt.getEntityColumn ()+"`, `file`) VALUES (?,?)");
		ResultSet rs = null;
		boolean ok = false;
		
		try
		{
			st.setInt (1, entity.getId ());
			st.setInt (2, fileId);
			
			int affectedRows = st.executeUpdate();
			if (affectedRows == 0)
				throw new SQLException("Associating file to "+entityMgmt.getEntityColumn ()+" failed, no rows affected.");
			ok = true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err associating file to "+entityMgmt.getEntityColumn ()+": " + e.getMessage ());
			LOGGER.error (e, "db problem while associating file to ", entityMgmt.getEntityColumn ());
			ok = false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return ok;
	}
	

	public boolean associateFile (int fileId, int versionId, String tableName, String columnName)
	{
		PreparedStatement st = db.prepareStatement ("INSERT INTO `"+tableName+"`(`"+columnName+"`, `file`) VALUES (?,?)");
	    ResultSet rs = null;
	    boolean ok = false;
		
		try
		{
			st.setInt (1, versionId);
			st.setInt (2, fileId);
			
			int affectedRows = st.executeUpdate();
			if (affectedRows == 0)
				throw new SQLException("Associating file to "+columnName+" failed, no rows affected.");
			ok = true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err associating file to "+columnName+": " + e.getMessage ());
			LOGGER.error (e, "db problem while associating file to ", columnName);
			ok = false;
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return ok;
	}

	public boolean getFiles (ChasteEntityVersion vers, String filesTable, String enityColumn)
	{
		int id = vers.getId ();
		ResultSet rs = null;

		PreparedStatement st = db.prepareStatement (
			"SELECT f.id AS fileid, f.relpath AS filepath, f.created AS filecreated, f.type AS filetype, u.id AS author, f.size AS filesize, f.masterFile AS masterFile FROM "
			+ "`files` f"
			+ " INNER JOIN `" + filesTable + "` mf on mf.file = f.id"
			+ " INNER JOIN `user` u on f.author = u.id"
			+ " WHERE mf." + enityColumn + "=?"
			+ " ORDER BY f.relpath");
		try
		{
			st.setInt (1, id);
			st.execute ();
			rs = st.getResultSet ();
			Vector<ChasteFile> res = evaluateResult (rs);
			vers.setFiles (res);
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError ("sql err retrieving files: " + e.getMessage ());
			LOGGER.error (e, "db problem while retrieving files (", filesTable, " - ", enityColumn, ")");
		}
		finally
		{
			db.closeRes (st);
			db.closeRes (rs);
		}
		
		return false;
	}


	public File getFileById(int fileId, ChasteEntityManager entityMgmt)
	{
		File f = null;
		ResultSet rs = null;

		PreparedStatement st = db.prepareStatement("SELECT f.id AS fileid, f.relpath AS filename, v.filepath AS dirname"
				+ " FROM `files` f"
				+ " INNER JOIN `" + entityMgmt.getEntityFilesTable() + "` vf ON vf.file = f.id"
				+ " INNER JOIN `" + entityMgmt.getEntityVersionsTable() + "` v ON v.id = vf." + entityMgmt.getEntityColumn()
				+ " WHERE f.id=?");
		try
		{
			st.setInt(1, fileId);
			st.execute();
			rs = st.getResultSet();
			while (rs != null && rs.next())
			{
				String name = rs.getString("filename");
				String dir = rs.getString("dirname");
				String fullPath = entityMgmt.getEntityStorageDir() + Tools.FILESEP + dir + Tools.FILESEP + name;
				f = new File(fullPath);
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			note.addError("sql err retrieving file: " + e.getMessage ());
			LOGGER.error(e, "db problem while retrieving file " + fileId);
		}
		finally
		{
			db.closeRes(st);
			db.closeRes(rs);
		}
		
		return f;
	}


	public static File createArchive (ChasteEntityVersion version, String storageDir) throws Exception
	{
		File tmpDir = new File (Tools.getTempDir ());
		if (!tmpDir.exists ())
			if (!tmpDir.mkdirs ())
				throw new IOException ("cannot create temp dir for file upload");
		
		File tmpFile = new File (tmpDir.getAbsolutePath () + Tools.FILESEP + UUID.randomUUID().toString());
		while (tmpFile.exists ())
			tmpFile = new File (tmpDir.getAbsolutePath () + Tools.FILESEP + UUID.randomUUID().toString());
		
		CombineArchive ca = new CombineArchive (tmpFile);
		Vector<ChasteFile> files = version.getFiles ();
		
		File basePath = new File (storageDir + Tools.FILESEP + version.getFilePath ());
		String entityPath = storageDir + Tools.FILESEP + version.getFilePath () + Tools.FILESEP;

		LOGGER.debug ("base path will be: ", basePath);
		LOGGER.debug ("entityPath will be: ", entityPath);
		
		for (ChasteFile file : files)
		{
			User u = file.getAuthor ();
			VCard vcard = new VCard (u.getFamilyName (), u.getGivenName (), u.getMail (), u.getInstitution ());
			OmexDescription od = new OmexDescription (vcard, file.getFilecreated ());

			LOGGER.debug ("add: ", file.getName ());
			
			
			/*ca.addEntry (basePath,
					new File (entityPath + file.getName ()),
					CombineFormats.getFormatIdentifier (file.getFiletype ().toLowerCase ()),
					od,
					file.isMasterFile ());*/
			
			ArchiveEntry entry = ca.addEntry(basePath, new File (entityPath + file.getName ()), CombineFormats.getFormatIdentifier (file.getFiletype ().toLowerCase ()), file.isMasterFile ());
			entry.addDescription(new OmexMetaDataObject (od));
			
			
		}
		
		
		ca.pack();
		ca.close();
		
		return tmpFile;
	}
	
}
