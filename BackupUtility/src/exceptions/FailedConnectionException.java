package exceptions;

/**
 * Exception for when creating a connection failed.
 * 
 * @author JoelNeppel
 *
 */
public class FailedConnectionException extends Exception
{
	/**
	 * UID
	 */
	private static final long serialVersionUID = 742841124354284416L;

	/**
	 * Creates a default exception.
	 */
	public FailedConnectionException()
	{
	}

	/**
	 * Creates an exception with the given message.
	 * @param message
	 *     The message to be sent
	 */
	public FailedConnectionException(String message)
	{
		super(message);
	}
}