/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.beans;

import java.sql.Timestamp;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.mgmt.Tools;


/**
 * @author martin
 *
 */
public class ChasteFile
{
	
	private User author;
	private Timestamp filecreated;
	private String filetype;
	//private String filevis;
	private int id;
	private String name;
	private long size;
	private String url;
	private boolean masterFile;
	
	public ChasteFile (int id, String name, Timestamp filecreated,
		//String filevis,
		String filetype, long size, User author, boolean masterFile)
	{
		super ();
		this.id = id;
		this.name = name;
		this.filecreated = filecreated;
		//this.filevis = filevis;
		this.filetype = filetype;
		this.author = author;
		this.url = Tools.convertForURL (name);
		this.size = size;
		this.masterFile = masterFile;
	}
	
	public User getAuthor ()
	{
		return author;
	}
	public String getCreated ()
	{
		return Tools.formatTimeStamp (filecreated);
	}
	public Timestamp getFilecreated ()
	{
		return filecreated;
	}
	public String getFiletype ()
	{
		return filetype;
	}
	/*public String getFilevis ()
	{
		return filevis;
	}*/
	public int getId ()
	{
		return id;
	}

	public String getName ()
	{
		return name;
	}
	
	
	public long getSize ()
	{
		return size;
	}
	
	public boolean isMasterFile ()
	{
		return masterFile;
	}
	


	
	/**
	 * Returns an URL aware name of this file. That's not the URL to download the file!
	 *
	 * @return the url
	 */
	public String getUrl ()
	{
		return url;
	}




	@SuppressWarnings("unchecked")
	public JSONObject toJson ()
	{
		JSONObject json = new JSONObject ();

		json.put ("id", id);
		json.put ("created", getCreated ());
		json.put ("filetype", filetype);
		json.put ("author", author.getNick ());
		json.put ("size", size);
		json.put ("name", name);
		json.put ("masterFile", masterFile);
		
		return json;
	}
}
