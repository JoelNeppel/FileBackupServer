package communications;

import java.io.IOException;
import java.net.Socket;

import lists.DoublyLinkedList;

/**
 * Connection for the client to communicate to the host with.
 *
 * @author JoelNeppel
 *
 */
public class ClientConnection extends AbstractConnection
{
	/**
	 * Creates a connection to handle data transfers through using the given socket.
	 * @param s
	 *     The socket to communicate through
	 * @param cipherKey
	 *     The string to use as the cipher key for encryption, null for no
	 *     encryption
	 * @throws IOException
	 */
	public ClientConnection(Socket s, String cipherKey) throws IOException
	{
		super(s, cipherKey);
	}

	/**
	 * Returns a list of the files available for backup from the host machine. Used
	 * to request any new or missing file not present on the client machine.
	 * @return A list of relative file paths available for backup from the host
	 *     machine
	 * @throws IOException
	 */
	public DoublyLinkedList<String> receiveFileList() throws IOException
	{
		DoublyLinkedList<String> list = new DoublyLinkedList<>();

		String cur = "";
		int got = read();
		while(0xFF != got)
		{
			if(0x0D == got)
			{
				list.add(cur);
				cur = "";
			}
			else
			{
				cur += (char) got;
			}

			got = read();
		}
		return list;
	}
}