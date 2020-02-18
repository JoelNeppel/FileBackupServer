package communications;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

/**
 * Connection for the host machine to communicate with the client.
 *
 * @author JoelNeppel
 *
 */
public class HostConnection extends AbstractConnection
{
	/**
	 * Creates a new host connection over the given socket.
	 * @param s
	 *     The socket to communicate through
	 * @param cipherKey
	 *     The key to use for encryption, null for no encryption
	 * @throws IOException
	 */
	public HostConnection(Socket s, String cipherKey) throws IOException
	{
		super(s, cipherKey);
	}

	/**
	 * Sends a list of available files for backup to the client to check for any
	 * missing ones.
	 * @param f
	 *     The file being send
	 * @param pathRemove
	 *     The machine specific path to be removed before sending
	 * @throws IOException
	 */
	public void sendFileList(File f, String pathRemove) throws IOException
	{
		if(f.isDirectory())
		{
			for(File ff : f.listFiles())
			{
				sendFileListRecursive(ff, pathRemove);
			}
		}
		write((byte) 0xFF);
	}

	/**
	 * Sends a list of available files for backup to the client to check for any
	 * missing ones.
	 * @param f
	 *     The file being send
	 * @param pathRemove
	 *     The machine specific path to be removed before sending
	 * @throws IOException
	 */
	private void sendFileListRecursive(File f, String pathRemove) throws IOException
	{
		if(f.isHidden() || (f.isDirectory() && null == f.list()))
		{
			return;
		}

		String send = f.getPath().replace(pathRemove, "");
		write(send.getBytes());
		write((byte) 0x0D);

		if(f.isDirectory())
		{
			for(File ff : f.listFiles())
			{
				sendFileListRecursive(ff, pathRemove);
			}
		}
	}
}