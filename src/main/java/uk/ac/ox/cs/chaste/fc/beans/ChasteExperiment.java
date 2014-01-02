/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.beans;

import java.sql.Timestamp;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.mgmt.ModelManager;
import uk.ac.ox.cs.chaste.fc.mgmt.ProtocolManager;


/**
 * @author martin
 *
 */
public class ChasteExperiment
extends ChasteEntity
{
	/*private int modelId;
	private int protocolId;*/

	private ChasteEntityVersion model;
	private ChasteEntityVersion protocol;
	
	public ChasteExperiment (int id, User author, Timestamp created, ChasteEntityVersion model, ChasteEntityVersion protocol)
	{
		super (id, "Experiment of " + model.getEntity ().getName () + " &amp; " + protocol.getEntity ().getName (), author, created, "experiment");
		this.model = model;
		this.protocol = protocol;
	}

	public ChasteEntityVersion getModel ()
	{
		return model;
	}

	public ChasteEntityVersion getProtocol ()
	{
		return protocol;
	}
	

	@SuppressWarnings("unchecked")
	public JSONObject toJson ()
	{
		JSONObject json = new JSONObject ();

		System.out.println ("model: " + model.getEntity().getName() + " @ " + model.getVersion());
		
		JSONObject tmp = model.toJson ();
		tmp.put ("name", model.getEntity ().getName ());
		tmp.put ("version", model.getVersion ());
		tmp.put ("id", model.getId ());
		json.put ("model", tmp);

		System.out.println ("protocol: " + protocol.getEntity().getName() + " @ " + protocol.getVersion());
		
		tmp = protocol.toJson ();
		tmp.put ("name", protocol.getEntity ().getName ());
		tmp.put ("version", protocol.getVersion ());
		tmp.put ("id", protocol.getId ());
		json.put ("protocol", tmp);

		json.put ("id", getId ());
		ChasteExperimentVersion version = (ChasteExperimentVersion) this.getLatestVersion ();
		System.out.println ("latest version: " + version);
		if (version != null)
			json.put ("latestResult", version.getStatus ());
		
		return json;
	}
	
	
}
