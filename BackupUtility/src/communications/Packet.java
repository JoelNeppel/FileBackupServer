package communications;

import fileUsage.FileStatus;

/**
 * Stores data to be sent or received and processes into or out of byte form.
 *
 * @author JoelNeppel
 *
 */
public class Packet
{
	/**
	 * The command for this packet
	 */
	private Command cmd;

	/**
	 * The status of the file
	 */
	private FileStatus status;

	/**
	 * The date the file was last modified
	 */
	private long fileDate;

	/**
	 * The relative path for the file being backed up
	 */
	private String path;

	/**
	 * @param cmd
	 * @param status
	 * @param fileDate
	 * @param path
	 */
	public Packet(Command cmd, FileStatus status, long fileDate, String path)
	{
		this.cmd = cmd;
		this.status = status;
		this.fileDate = fileDate;
		this.path = path;
	}

	/**
	 * Creates packet to be sent using the given data.
	 * @param command
	 *     The command that will be sent
	 * @param dateModified
	 *     The date the file was last modified
	 * @param relativePath
	 *     The relative path of the file
	 */
	public Packet(Command command, long dateModified, String relativePath)
	{
		cmd = command;
		fileDate = dateModified;
		path = relativePath;
	}

	/**
	 * Creates packet to be sent using the given data.
	 * @param command
	 *     The command that will be sent
	 * @param dateModified
	 *     The date the file was last modified
	 * @param relativePath
	 *     The relative path of the file
	 */
	public Packet(Command command, String relativePath)
	{
		this(command, FileStatus.UNKNOWN, 0, relativePath);
	}

	/**
	 * Creates a packet with the given command, commonly only used for failed and
	 * success.
	 * @param command
	 *     The command for this packet
	 */
	public Packet(Command command)
	{
		this(command, FileStatus.UNKNOWN, 0, null);
	}

	public Packet()
	{
		this(null, FileStatus.UNKNOWN, 0, null);
	}

	/**
	 * Creates a packet from the byte array that was received.
	 * @param bytes
	 *     The byte array to be parsed
	 */
	public Packet(byte[] bytes)
	{
		cmd = Command.byteToCommand(bytes[0]);

		if(cmd == null)
		{
			throw new IllegalStateException("Byte received did not correspond with any command");
		}

		fileDate = 0;
		for(int i = 1; i < 9; i++)
		{
			fileDate <<= 8;
			fileDate |= (bytes[i] & 0xFF);
		}

		status = FileStatus.byteToStatus(bytes[9]);

		path = new String(bytes, 10, bytes.length - 10);
		// Convert between different operating systems file separators
		String separator = System.getProperty("file.separator");
		if("\\".equals(separator))
		{
			path = path.replace('/', '\\');
		}
		else
		{
			path = path.replace('\\', '/');
		}
	}

	/**
	 * Converts the data in the packet into a byte array to be sent.
	 * @return The array of bytes to be sent
	 */
	public byte[] byteData()
	{
		// 1 byte for command, 8 bytes for long, 1 byte file status, 1 byte per
		// character in string
		int size = 10;
		if(null != path)
		{
			size += path.length();
		}
		byte[] bytes = new byte[size];

		// Add command byte first
		if(null == cmd)
		{
			throw new IllegalStateException("Command cannot be null when sending packet.");
		}
		bytes[0] = cmd.getCommand();

		// Add long
		int at = 1;
		while(at < 9)
		{
			bytes[at] = (byte) (fileDate >> (64 - at * 8) & 0xFF);
			at++;
		}

		// Add file status
		if(status != null)
		{
			bytes[at] = status.getByteRespresentation();
		}
		at++;

		// Add path
		if(null != path)
		{
			for(byte b : path.getBytes())
			{
				bytes[at] = b;
				at++;
			}
		}
		return bytes;
	}

	/**
	 * Returns the value of cmd
	 * @return the cmd
	 */
	public Command getCmd()
	{
		return cmd;
	}

	/**
	 * Sets the command for this packet to the given one.
	 * @param c
	 *     The command to set this packet to
	 */
	public void setCommand(Command c)
	{
		cmd = c;
	}

	/**
	 * Returns the value of status
	 * @return the status
	 */
	public FileStatus getStatus()
	{
		return status;
	}

	/**
	 * Sets status to the given value.
	 * @param status
	 *     The status to set
	 */
	public void setStatus(FileStatus status)
	{
		this.status = status;
	}

	/**
	 * Returns the value of fileDate
	 * @return the fileDate
	 */
	public long getFileDate()
	{
		return fileDate;
	}

	/**
	 * Sets fileDate to the given value.
	 * @param fileDate
	 *     The fileDate to set
	 */
	public void setFileDate(long fileDate)
	{
		this.fileDate = fileDate;
	}

	/**
	 * Returns the value of path
	 * @return the path
	 */
	public String getPath()
	{
		return path;
	}

	@Override
	public String toString()
	{
		return cmd + " " + status + " " + fileDate + " " + path;
	}
}