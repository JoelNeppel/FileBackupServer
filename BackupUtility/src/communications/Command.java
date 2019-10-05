package communications;

/**
 * Enum for commands that will be sent between server and client for the backup
 * process.
 *
 * @author JoelNeppel
 *
 */
public enum Command
{
	// Request
	CLOSE((byte) 0x04),
	RECEIVE_FILE((byte) 0x52),
	RECEIVE_FILE_LIST((byte) 0x49),
	SEND_FILE((byte) 0x53),
	SEND_FILE_LIST((byte) 0x4C),
	CREATE_DIRECTORY((byte) 0x44),
	GET_STATUS((byte) 0x3F),
	// Response
	SUCCESS((byte) 0x55),
	FAILED((byte) 0x21);

	/**
	 * The command byte for the enum
	 */
	private byte cmd;

	/**
	 * Creates new enum option.
	 * @param command
	 *     The command byte
	 */
	private Command(byte command)
	{
		cmd = command;
	}

	/**
	 * Returns the command byte to be sent.
	 * @return The command byte
	 */
	public byte getCommand()
	{
		return cmd;
	}

	/**
	 * Returns the corresponding enum of the given command byte.
	 * @param command
	 *     The command byte being compared
	 * @return The enum option that corresponds with the provided byte null if byte
	 *     does not match any command
	 */
	public static Command byteToCommand(byte command)
	{
		for(Command c : Command.values())
		{
			if(c.cmd == command)
			{
				return c;
			}
		}

		return null;
	}
}