package communications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Abstract connection handling for standard sending and receiving.
 *
 * @author JoelNeppel
 *
 */
public abstract class AbstractConnection
{
	/**
	 * The socket connecting the host and client
	 */
	private Socket socket;

	/**
	 * The output stream to write the data to
	 */
	private OutputStream out;

	/**
	 * The input stream to receive data from
	 */
	private InputStream in;

	/**
	 * The bytes to shift by during the encryption and decryption process
	 */
	private static byte[] shiftBytes;

	/**
	 * Creates a connection to handle using the given socket.
	 * @param s
	 *     The socket to use
	 * @param cipherKey
	 *     The string to use as a shift during encryption and decryption
	 * @throws IOException
	 */
	protected AbstractConnection(Socket s, String cipherKey) throws IOException
	{
		socket = s;
		s.setTcpNoDelay(true);
		socket.setSoTimeout(5000);
		out = s.getOutputStream();
		in = s.getInputStream();
		if(null != cipherKey)
		{
			shiftBytes = cipherKey.getBytes();
		}
	}

	/**
	 * Sends the given file. Writes file size to receiver first then sends file one
	 * byte at a time to easily accommodate large files and prevent errors.
	 * @param f
	 *     The file to send
	 * @throws IOException
	 */
	public void sendFile(File f) throws IOException
	{
		FileInputStream fileRead = null;
		System.out.println("Sending: " + f);
		try
		{
			fileRead = new FileInputStream(f);

			// Send file size to receiver for how many bytes to expect
			out.write(ByteHelp.toBytes(f.length()));
			long length = f.length();
			byte[] bytes = new byte[(int) Math.min(length, 1073741824)];
			int start = 0;
			while(length > 0)
			{
				int read = 0;
				if(length >= bytes.length)
				{
					read = fileRead.read(bytes);
				}
				else
				{
					read = fileRead.read(bytes, 0, (int) length);
				}

				start = encrypt(bytes, read, start);
				out.write(bytes, 0, read);
				length -= read;
			}
		}
		catch(IOException e)
		{
			if(null != fileRead)
			{
				fileRead.close();
			}
			throw e;
		}

		fileRead.close();
		System.out.println("Done sending");
	}

	/**
	 * Sends the given packet using the packet's byteData method
	 * @param p
	 *     The packet to send
	 * @throws IOException
	 */
	public void sendPacket(Packet p) throws IOException
	{
		byte[] data = p.byteData();
		out.write(ByteHelp.toBytes(data.length));
		out.write(data);
	}

	/**
	 * Attempts to send a packet until it succeeds or determines that there is a
	 * fatal error and ends the program.
	 * @param p
	 *     The packet to be sent
	 */
	public void forceSendPacket(Packet p)
	{
		boolean sent = false;
		int numAttempts = 0;
		while(!sent)
		{
			try
			{
				sendPacket(p);
				sent = true;
			}
			catch(IOException e)
			{
				numAttempts++;
				if(numAttempts > 10)
				{
					// Nothing left that can be done
					return;
				}

				try
				{
					Thread.sleep(1);
				}
				catch(InterruptedException e1)
				{
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Overwrites given file with the new received data. Expects the first eight
	 * bytes to be the file size and only reads that many bytes. Reads and writes
	 * one byte at a time to accommodate large files easily.
	 * @param write
	 *     The file to write the received file to
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void receiveFile(File write) throws IOException
	{
		System.out.println("Receiving: " + write);
		File temp = new File(write.getPath() + ".temp");
		if(temp.exists())
		{
			temp.delete();
		}
		write.renameTo(temp);
		FileOutputStream fileWrite = null;
		try
		{
			write.createNewFile();
			fileWrite = new FileOutputStream(write);

			byte[] size = new byte[Long.BYTES];
			in.read(size);
			long bytesLeft = ByteHelp.bytesToLong(size);
			double totalBytes = bytesLeft;
			byte[] bytes = new byte[(int) Math.min(totalBytes, 1073741824)];
			int start = 0;
			while(bytesLeft > 0)
			{
				int read = 0;
				if(bytesLeft >= bytes.length)
				{
					read = in.read(bytes);
				}
				else
				{
					read = in.read(bytes, 0, (int) bytesLeft);
				}

				start = decrypt(bytes, read, start);
				fileWrite.write(bytes, 0, read);
				bytesLeft -= read;
			}

			fileWrite.close();
			temp.delete();
		}
		catch(Exception e)
		{
			// TODO actually do this correctly
			write.delete();
			temp.renameTo(write);

			if(null != fileWrite)
			{
				fileWrite.close();
			}

			throw e;
		}

		System.out.println("Got: " + write);
	}

	/**
	 * Receives a packet.
	 * @return The packet that was received
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Packet receivePacket() throws IOException, InterruptedException
	{
		byte[] sizeBytes = new byte[Integer.BYTES];
		in.read(sizeBytes);

		int packetSize = ByteHelp.bytesToInt(sizeBytes);
		byte[] bytes = new byte[packetSize];
		in.read(bytes);

		return new Packet(bytes);
	}

	/**
	 * Reads one byte from the InputStream.
	 * @return An int from 0 to 255 that represents the byte or -1 if the end is
	 *     reached
	 * @throws IOException
	 */
	protected int read()
	{
		try
		{
			return in.read();
		}
		catch(IOException e)
		{
			return -1;
		}
	}

	/**
	 * Clears the input in the event of an exception
	 */
	public void clearInput()
	{
		try
		{
			while(in.available() > 0)
			{
				in.skip(1);
			}
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Writes the given byte to the OutputSteam.
	 * @param b
	 *     The byte to be written
	 * @throws IOException
	 */
	protected void write(byte b) throws IOException
	{
		out.write(b);
	}

	/**
	 * Writes the given byte array to the OutputStream.
	 * @param bytes
	 *     The byte array to write
	 * @throws IOException
	 */
	protected void write(byte[] bytes) throws IOException
	{
		out.write(bytes);
	}

	/**
	 * Closes the connection and all related items to end communications.
	 */
	public void close()
	{
		try
		{
			socket.close();
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Returns the address for the connected computer.
	 * @return The IP address
	 */
	public String getAddress()
	{
		return socket.getInetAddress().getHostAddress();
	}

	/**
	 * Encrypts the given bytes up until end using a basic password shift beginning
	 * with the index start.
	 * @param bytes
	 *     The bytes to encrypt
	 * @param end
	 *     The number of bytes to encrypt
	 * @param start
	 *     The start index for the shift bytes
	 * @return The new start index for the next set of encryption
	 */
	private static int encrypt(byte[] bytes, int end, int start)
	{
		// Return if shiftBytes is null meaning no encryption
		if(null == shiftBytes)
		{
			return 0;
		}

		for(int i = 0; i < end; i++)
		{
			// shift byte
			bytes[i] = (byte) (bytes[i] + shiftBytes[start]);

			start++;
			if(start >= shiftBytes.length)
			{
				start = 0;
			}
		}

		return start;
	}

	/**
	 * Decrypts the given bytes from a basic password shift up until the end index
	 * starting with the given start of the shift bytes.
	 * @param bytes
	 *     The bytes to decrypt
	 * @param end
	 *     The number of bytes to decrypt
	 * @param start
	 *     The index to start at in the decryption bytes
	 * @return The index to start at for the next round of decryption
	 */
	private static int decrypt(byte[] bytes, int end, int start)
	{
		// Return if shiftBytes is null meaning data is not encrypted
		if(null == shiftBytes)
		{
			return 0;
		}

		for(int i = 0; i < end; i++)
		{
			// Shift byte
			bytes[i] = (byte) (bytes[i] - shiftBytes[start]);

			start++;
			if(start >= shiftBytes.length)
			{
				start = 0;
			}
		}

		return start;
	}
	// /**
	// * Waits until the input has the given number of bytes available.
	// * @param size
	// * The number of bytes to wait for
	// * @throws InterruptedException
	// * @throws IOException
	// */
	// protected void waitForByte(int size) throws InterruptedException, IOException
	// {
	// while(in.available() < size)
	// {
	// Thread.sleep(1);
	// }
	// }
}