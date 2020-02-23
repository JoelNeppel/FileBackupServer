package exceptions;

/**
 * @author JoelNeppel
 *
 */
public class ItemNotFoundException extends Exception
{

	/**
	 * Generated ID
	 */
	private static final long serialVersionUID = 5595577542777201908L;

	public ItemNotFoundException()
	{
		super();
	}

	public ItemNotFoundException(String message)
	{
		super(message);
	}
}