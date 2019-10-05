package communications;

/**
 * Enum for the different status of files between the client and host. The
 * status represents the file on the responder compared to the one on the
 * requester.
 *
 * @author JoelNeppel
 *
 */
public enum FileStatus
{
	NOT_FOUND((byte) 0x46),
	SAME_VERSION((byte) 0x53),
	OLD_VERSION((byte) 0x4F),
	NEW_VERSION((byte) 0x4E);

	/**
	 * The byte representation for the enum
	 */
	private byte rep;

	/**
	 * Creates an enum with the given byte that will represent the option when being
	 * sent.
	 * @param b
	 *     The byte that will represent the option
	 */
	private FileStatus(byte b)
	{
		rep = b;
	}

	/**
	 * Returns the byte representation.
	 * @return The byte representation
	 */
	public byte getByteRespresentation()
	{
		return rep;
	}

	/**
	 * Converts the given byte into the FileStatus with the corresponding enum.
	 * @param b
	 *     The byte that represents the file status
	 * @return The corresponding file status for the given byte
	 */
	public static FileStatus byteToStatus(byte b)
	{
		for(FileStatus status : FileStatus.values())
		{
			if(status.rep == b)
			{
				return status;
			}
		}

		return null;
	}
}