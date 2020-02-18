package communications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * Abstract connection handling for standard sending and receiving.
 *
 * @author JoelNeppel
 *
 */
public abstract class CommunicationHelp
{
	/**
	 * Static class not for construction.
	 */
	private CommunicationHelp()
	{
	}

	/**
	 * Sends the given file. Writes file size to receiver first then sends file one
	 * byte at a time to easily accommodate large files and prevent errors.
	 * @param f
	 *     The file to send
	 * @param s
	 * @param out
	 * @param encryption
	 * @throws IOException
	 */
	public static void sendFile(File f, Socket s, Cipher encryption) throws IOException
	{
		OutputStream out = s.getOutputStream();
		FileInputStream fileRead = null;
		System.out.println("Sending: " + f);
		try
		{
			fileRead = new FileInputStream(f);

			// Send file size to receiver for how many bytes to expect
			out.write(ByteHelp.toBytes(f.length()));
			long bytesRemaining = f.length();
			byte[] readData = new byte[(int) Math.min(bytesRemaining, Integer.MAX_VALUE / 8)]; // Limit maximum size of array arbitrarily
			while(bytesRemaining > 0)
			{
				int bytesRead = 0;
				if(bytesRemaining >= readData.length)
				{
					bytesRead = fileRead.read(readData);
				}
				else
				{
					bytesRead = fileRead.read(readData, 0, (int) bytesRemaining);
				}

				byte[] encryptedData = encryption.doFinal(readData, 0, bytesRead);
				out.write(ByteHelp.toBytes(encryptedData.length));
				out.write(encryptedData);
				bytesRemaining -= bytesRead;
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
		catch(IllegalBlockSizeException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(BadPaddingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		fileRead.close();
		System.out.println("Done sending");
	}

	/**
	 * Sends the given packet using the packet's byteData method
	 * @param p
	 *     The packet to send
	 * @param s
	 * @throws IOException
	 */
	public static void sendPacket(Packet p, Socket s) throws IOException
	{
		OutputStream out = s.getOutputStream();
		byte[] data = p.byteData();
		out.write(ByteHelp.toBytes(data.length));
		out.write(data);
	}

	/**
	 * Attempts to send a packet until it succeeds or determines that there is a
	 * fatal error and ends the program.
	 * @param p
	 *     The packet to be sent
	 * @param s
	 */
	public void forceSendPacket(Packet p, Socket s)
	{
		boolean sent = false;
		int numAttempts = 0;
		while(!sent)
		{
			try
			{
				sendPacket(p, s);
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
	 * @param s
	 * @param decryption
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void receiveFile(File write, Socket s, Cipher decryption) throws IOException
	{
		InputStream in = s.getInputStream();
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
			byte[] bytes = new byte[(int) Math.min(totalBytes, Integer.MAX_VALUE / 8)]; // Limit maximum size of array arbitrarily
			while(bytesLeft > 0)
			{
				byte[] num = new byte[4];
				in.read(num);
				int amountToReceive = ByteHelp.bytesToInt(num);
				int read = 0;
				byte[] encryptedData = new byte[amountToReceive];
				while(amountToReceive > read)
				{
					read += in.read(encryptedData, read, amountToReceive - read);
				}

				byte[] decryptedData = decryption.doFinal(encryptedData);
				fileWrite.write(decryptedData.length);
				bytesLeft -= decryptedData.length;
			}

			fileWrite.close();
			temp.delete();
		}
		catch(BadPaddingException e)
		{
			// TODO
		}
		catch(IllegalBlockSizeException e)
		{
			// TODO
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
	 * @param s
	 * @return The packet that was received
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Packet receivePacket(Socket s) throws IOException, InterruptedException
	{
		InputStream in = s.getInputStream();
		byte[] sizeBytes = new byte[Integer.BYTES];
		in.read(sizeBytes);

		int packetSize = ByteHelp.bytesToInt(sizeBytes);
		byte[] bytes = new byte[packetSize];
		in.read(bytes);

		return new Packet(bytes);
	}

	/**
	 * Reads one byte from the InputStream.
	 * @param s
	 * @return An int from 0 to 255 that represents the byte or -1 if the end is
	 *     reached
	 * @throws IOException
	 */
	public static int read(Socket s)
	{

		try
		{
			return s.getInputStream().read();
		}
		catch(IOException e)
		{
			return -1;
		}
	}

	/**
	 * Clears the input in the event of an exception
	 * @param s
	 */
	public static void clearInput(Socket s)
	{
		try
		{
			InputStream in = s.getInputStream();

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
	 * @param s
	 * @throws IOException
	 */
	public static void write(byte b, Socket s) throws IOException
	{
		s.getOutputStream().write(b);
	}

	/**
	 * Writes the given byte array to the OutputStream.
	 * @param bytes
	 *     The byte array to write
	 * @param s
	 * @throws IOException
	 */
	public static void write(byte[] bytes, Socket s) throws IOException
	{
		s.getOutputStream().write(bytes);
	}
}