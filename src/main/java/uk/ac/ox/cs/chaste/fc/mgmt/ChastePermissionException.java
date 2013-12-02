/**
 * 
 */
package uk.ac.ox.cs.chaste.fc.mgmt;


/**
 * Exception that is thrown in case of permission problems
 * 
 * @author martin
 *
 */
public class ChastePermissionException
	extends Exception
{
	public ChastePermissionException (String exception)
	{
		super (exception);
	}
}
