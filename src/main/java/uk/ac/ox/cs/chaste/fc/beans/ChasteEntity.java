/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.beans;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.mgmt.Tools;


/**
 * @author martin
 *
 */
public class ChasteEntity
{
	private int id;
	private String name;
	private User author;
	private String url;
	private Timestamp created;
	private HashMap<Integer, ChasteEntityVersion> versions;
	private String type;
	
	public static class SortByName implements Comparator<ChasteEntity>
	{
		@Override
		public int compare (ChasteEntity a, ChasteEntity b)
		{
			return a.name.compareTo (b.name);
		}
	}

	/**
	 * Order entities first by name, and then by id, newest (i.e. highest id) first.
	 */
	public static class SortByNameAndId implements Comparator<ChasteEntity>
	{
		@Override
		public int compare (ChasteEntity a, ChasteEntity b)
		{
			if (a.name.equals(b.name))
				return b.id - a.id; // a < b, i.e. prior in order, iff it has a higher id
			else
				return a.name.compareTo(b.name);
		}
	}

	public ChasteEntity (int id, String name, User author, Timestamp created, String type)
	{
		this.id = id;
		this.name = name;
		this.url = Tools.convertForURL (name);
		this.created = created;
		this.author = author;
		this.type = type;
		versions = new HashMap<Integer, ChasteEntityVersion> ();
	}
	
	public void debug ()
	{
		System.out.println ("model " + name);
		for (ChasteEntityVersion v : versions.values ())
			if (v.getVersion ().equals (name))
				v.debug ();
	}
	
	public ChasteEntityVersion getVersion (String name)
	{
		for (ChasteEntityVersion v : versions.values ())
			if (v.getVersion ().equals (name))
				return v;
		return null;
	}
		
	public String getUrl ()
	{
		return url;
	}
	
	public int getId ()
	{
		return id;
	}
	
	public Map<Integer, ChasteEntityVersion> getVersions ()
	{
		return versions;
	}

	public Map<Integer, ChasteEntityVersion> getOrderedVersions()
	{
		Map<Integer, ChasteEntityVersion> sorted = new TreeMap<Integer, ChasteEntityVersion>(Collections.reverseOrder());
		sorted.putAll(versions);
		return sorted;
	}
	
	public ChasteEntityVersion getLatestVersion ()
	{
		if (versions.isEmpty())
			return null;
		List<Integer> list = new ArrayList<Integer> (versions.keySet ());
		Collections.sort (list);

		return versions.get (list.get (list.size () - 1));
	}
	
	public void addVersion (ChasteEntityVersion version)
	{
		this.versions.put (version.getId (), version);
	}
	
	public boolean hasVersions ()
	{
		return !versions.isEmpty();
	}

	public User getAuthor ()
	{
		return author;
	}
	
	public String getCreated ()
	{
		return Tools.formatTimeStamp (created);
	}

	public String getName ()
	{
		return name;
	}
	
	public String getType ()
	{
		return type;
	}
	
	public ChasteEntityVersion getVersionByFilePath (String path)
	{
		for (ChasteEntityVersion v : versions.values ())
			if (v.getFilePath ().equals (path))
				return v;
		return null;
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson ()
	{
		JSONObject json = new JSONObject ();

		json.put ("name", name);
		json.put ("created", getCreated ());
		json.put ("author", getAuthor ().getNick ());
		json.put ("id", id);

		JSONObject version = new JSONObject ();
		if (!versions.isEmpty())
			for (ChasteEntityVersion v : versions.values ())
				version.put (v.getId (), v.toJson ());

		json.put ("versions", version);
		
		return json;
	}

/*
	@SuppressWarnings("unchecked")
	public JSONObject toJsonOnlyLatestVersion ()
	{
		JSONObject json = new JSONObject ();

		json.put ("name", name);
		json.put ("created", getCreated ());
		json.put ("author", getAuthor ().getNick ());
		json.put ("id", id);

		json.put ("latestversion", getLatestVersion ().toJson ());
		
		return json;
	}*/
}
