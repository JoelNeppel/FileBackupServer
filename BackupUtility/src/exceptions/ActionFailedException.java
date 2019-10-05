package exceptions;

/**
 * Exception for when an action requested fails.
 * 
 * @author JoelNeppel
 *
 */
public class ActionFailedException extends Exception
{
	/**
	 * UID
	 */
	private static final long serialVersionUID = -8535668528842260907L;

	/**
	 * Default exception.
	 */
	public ActionFailedException()
	{
	}

	/**
	 * Exception with the given message.
	 * @param message
	 *     The message to be sent with the exception
	 */
	public ActionFailedException(String message)
	{
		super(message);
	}
}