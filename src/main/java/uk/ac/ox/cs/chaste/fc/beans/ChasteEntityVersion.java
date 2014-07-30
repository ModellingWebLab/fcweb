/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.beans;

import java.sql.Timestamp;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.mgmt.Tools;


/**
 * @author martin
 *
 */
public class ChasteEntityVersion
{
	// Note that these must match the `visibility` fields in the `*versions` tables
	public static final String VISIBILITY_PRIVATE = "PRIVATE";
	public static final String VISIBILITY_RESTRICTED = "RESTRICTED";
	public static final String VISIBILITY_PUBLIC = "PUBLIC";
	
	private ChasteEntity entity;
	private int id;
	private String version;
	private User author;
	private String filePath;
	private String visibility;
	private String url;
	private Timestamp created;
	private int numFiles;
	private Vector<ChasteFile> files;
	private Vector<ChasteExperiment> experiments;
	private String commitMsg;
	
	public ChasteEntityVersion (ChasteEntity entity, int id, String version, User author, String filePath, Timestamp created, int numFiles, String visibility, String commitMsg)
	{
		this.entity = entity;
		this.id = id;
		this.version = version;
		this.url = Tools.convertForURL (version);
		this.created = created;
		this.author = author;
		this.filePath = filePath;
		this.numFiles = numFiles;
		this.visibility = visibility;
		files = new Vector<ChasteFile> ();
		experiments = new Vector<ChasteExperiment> ();
		this.commitMsg = commitMsg;
	}
	
	public ChasteFile getFileById (int id)
	{
		for (ChasteFile cf : files)
			if (cf.getId () == id)
				return cf;
		return null;
	}
	
	
	public String getUrl ()
	{
		return url;
	}
	
	
	public String getCommitMessage ()
	{
		return commitMsg;
	}
	
	
	public String getFilePath ()
	{
		return filePath;
	}

	public int getId ()
	{
		return id;
	}

	
	public void setFiles (Vector<ChasteFile> files)
	{
		this.files = files;
	}

	
	public Vector<ChasteFile> getFiles ()
	{
		return files;
	}
	
	
	
	public void addFile (ChasteFile file)
	{
		this.files.add (file);
	}
	
	
	
	public void addExperiment (ChasteExperiment ev)
	{
		this.experiments.add (ev);
	}
	
	
	public int getNumFiles ()
	{
		return numFiles;
	}



	public User getAuthor ()
	{
		return author;
	}

	public String getVisibility ()
	{
		return visibility;
	}
	
	public String getJointVisibility(ChasteEntityVersion other)
	{
		String result = this.visibility;
		if ((other.visibility.equals(ChasteEntityVersion.VISIBILITY_PRIVATE)) ||
				(other.visibility.equals(ChasteEntityVersion.VISIBILITY_RESTRICTED)
				 && this.visibility.equals(ChasteEntityVersion.VISIBILITY_PUBLIC)))
		{
			result = other.visibility;
		}
		return result;
	}
	
	public String getCreated ()
	{
		return Tools.formatTimeStamp (created);
	}


	public ChasteEntity getEntity ()
	{
		return entity;
	}

	
	public String getVersion ()
	{
		return version;
	}

	
	public String getName ()
	{
		return entity.getName ();
	}


	public void debug ()
	{
		System.out.println ("\t" + getVersion ());
	}


	@SuppressWarnings("unchecked")
	public JSONObject toJson ()
	{
		JSONObject json = new JSONObject ();

		json.put ("version", version);
		json.put ("created", getCreated ());
		json.put ("author", getAuthor ().getNick ());
		json.put ("numFiles", numFiles);
		json.put ("visibility", visibility);
		json.put ("id", id);
		json.put ("entityId", entity.getId ());
		json.put ("name", entity.getName ());
		json.put ("commitMessage", getCommitMessage ());

		if (files != null && files.size () > 0)
		{
			JSONArray f = new JSONArray ();
			for (ChasteFile cf : files)
				f.add (cf.toJson ());
			json.put ("files", f);
		}
		
		if (experiments != null && experiments.size () > 0)
		{
			JSONArray e = new JSONArray ();
			for (ChasteExperiment ev : experiments)
				e.add (ev.toJson ());
			json.put ("experiments", e);
		}
		
		return json;
	}
	
	
}
