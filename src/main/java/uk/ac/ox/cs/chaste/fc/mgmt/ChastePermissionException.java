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
	private static final long serialVersionUID = -3170550123399434153L;

	public ChastePermissionException (String exception)
	{
		super (exception);
	}
}
