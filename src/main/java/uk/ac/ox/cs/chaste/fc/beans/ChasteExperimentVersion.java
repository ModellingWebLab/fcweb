/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.beans;

import java.sql.Timestamp;

import org.json.simple.JSONObject;

import uk.ac.ox.cs.chaste.fc.mgmt.ExperimentManager;


/**
 * @author martin
 *
 */
public class ChasteExperimentVersion
extends ChasteEntityVersion
{
	// Note that these must match the `status` fields in the `experimentversions` table
	public static final String STATUS_QUEUED = "QUEUED";
	public static final String STATUS_RUNNING = "RUNNING";
	public static final String STATUS_SUCCESS = "SUCCESS";
	public static final String STATUS_PARTIAL = "PARTIAL";
	public static final String STATUS_FAILED = "FAILED";
	public static final String STATUS_INAPPRORIATE = "INAPPRORIATE";
	
	private Timestamp finished;
	private String status;
	private String returnText;
	private String taskId;
	
	public ChasteExperimentVersion (ChasteEntity entity, int id,
		User author, String filePath, Timestamp created, int numFiles,
		Timestamp finished, String status, String returnText, String visibility, String commitMsg, String taskId)
	{
		super (entity, id, created.toString (), author, filePath, created, numFiles, visibility, commitMsg);
		this.finished = finished;
		this.status = status;
		this.returnText = returnText;
		this.taskId = taskId;
	}

	
	public Timestamp getFinished ()
	{
		return finished;
	}

	
	public String getStatus ()
	{
		return status;
	}

	
	public String getReturnText ()
	{
		return returnText;
	}
	
	public String getTaskId()
	{
		return taskId;
	}
	
	public ChasteExperiment getExperiment ()
	{
		return (ChasteExperiment) getEntity ();
	}
	
	public boolean updateExperiment (ExperimentManager expMgmt, String returnMsg, String status)
	{
		return expMgmt.updateVersion (this, returnMsg, status);
	}
	
	public boolean setTaskId(ExperimentManager expMgmt, String taskId)
	{
		this.taskId = taskId;
		return expMgmt.updateTaskId(this, taskId);
	}
	
	public boolean isInProgress()
	{
		return isInProgressStatus(status);
	}
	
	public boolean isInProgressStatus(String status)
	{
		return status.equals(STATUS_QUEUED) || status.equals(STATUS_RUNNING);
	}

	/*
	 * Override the base method to instead return the more restrictive of the visibilities of the model & protocol involved.
	 * @see uk.ac.ox.cs.chaste.fc.beans.ChasteEntityVersion#getVisibility()
	 */
	public String getVisibility ()
	{
		ChasteExperiment exp = getExperiment ();
		return exp.getModel ().getJointVisibility (exp.getProtocol());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJson ()
	{
		JSONObject json = super.toJson();
		json.put("status", status);
		return json;
	}
}
