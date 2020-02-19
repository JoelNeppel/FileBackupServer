package networkBackup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.InputMismatchException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import communications.ByteHelp;
import communications.Command;
import communications.CommunicationHelp;
import communications.Packet;
import fileUsage.FileItemHandlerNoGUI;
import fileUsage.FileStatus;
import fileUsage.SystemFileReader;

/**
 * Backup server class for distributing the most up to date files throughout all
 * connected devices by starting HostConnection. The server socket will remain
 * open waiting to accept any connection request.
 *
 * @author JoelNeppel
 *
 */
public class HostApp
{
	/**
	 * The file handler for the list of items
	 */
	private static FileItemHandlerNoGUI files;

	/**
	 * Has map for the settings
	 */
	private static SystemFileReader settings;

	private static Cipher privateCipher;

	/**
	 * Creates server socket on the designated port and accepts any request to
	 * connect and begins handling using HostConnection.
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("System: Started");
		try
		{
			// Get settings from file
			settings = new SystemFileReader("/mnt/BackupDrive/ProtectedResources/HostSettings.txt");
		}
		catch(InputMismatchException | FileNotFoundException e)
		{
			System.out.println(e.getMessage());
			System.exit(1);
		}
		// Create new file handler with given location
		String storageLocation = settings.get("Storage Location");
		if(null == storageLocation)
		{
			System.out.println("Settings file must include a default location in format \"Storage Location:> location to use:>\"");
			System.exit(2);
		}
		files = new FileItemHandlerNoGUI()
		{
			@Override
			public String getFullPath(String relative)
			{
				// Change location to the given backup folder path
				return storageLocation + convertPath(relative);
			}
		};

		ServerSocket ss = null;
		while(ss == null)
		{
			// Attempts to create server socket until it succeeds
			try
			{
				// Attempts to create socket server
				ss = new ServerSocket(Integer.parseInt(settings.get("Port")));
				System.out.println("Created Server");
			}
			catch(IOException e)
			{
				// Wait a bit before trying to connect again
				try
				{
					Thread.sleep(30000);
				}
				catch(InterruptedException e1)
				{
				}
			}
			catch(Exception e)
			{
				System.out.println("Possible invalid port setting: " + e.getMessage());
				System.exit(2);
			}
		}

		while(!Thread.interrupted())
		{
			// Wait for a client to connect and begin handling
			try
			{
				// Accept connection and begin handling
				Socket s = ss.accept();
				System.out.println("Started client connection: " + s);
				handleConnection(s);
			}
			catch(IOException e)
			{
				// Connection to client failed, ignore and allow other clients to connect
			}
		}

		try
		{
			if(null != ss)
			{
				ss.close();
			}
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Handles the connection by responding to all requests until the client closes
	 * the connection.
	 * @param comms
	 *     The host connection that will be used to handle the connection
	 */
	private static void handleConnection(Socket comms)
	{
		new Thread(()->
		{
			Cipher encrypt = null;
			Cipher decrypt = null;

			try
			{
				IvParameterSpec parameter = getIV(comms);
				Key AESKey = getAESKey(comms);

				encrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
				encrypt.init(Cipher.ENCRYPT_MODE, AESKey, parameter);
				decrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
				decrypt.init(Cipher.DECRYPT_MODE, AESKey, parameter);
			}
			catch(IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e1)
			{
				return;
			}

			try
			{
				if(!accessAllowed(comms, decrypt))
				{
					return;
				}
			}
			catch(InputMismatchException | IllegalBlockSizeException | BadPaddingException | IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}

			boolean close = false;
			// Checks if the directory to write file to exists
			File check = new File(files.getFullPath(""));
			if(!check.exists() || !check.isDirectory())
			{
				// reportFail("File location to write items to does not exist.", comms);
				System.out.println("Given directory location does not exist");
				return;
			}

			while(!close)
			{
				// Reply to any requests until the client closes
				try
				{
					// HostGraphic.updateFileAndAction(comms.getAddress(), "", "Receiving packet");
					Packet got = CommunicationHelp.receivePacket(comms);
					if(got.getCmd() == Command.CLOSE)
					{
						// Close connection when requested by client
						close = true;
					}
					else
					{
						// Respond to the packet received
						respond(got, comms, encrypt, decrypt);
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
					// reportFail("IO Exception Caught", comms);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			System.out.println("Closed: " + comms);
			try
			{
				comms.close();
			}
			catch(IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();
	}

	private static IvParameterSpec getIV(Socket comms) throws IOException
	{
		InputStream in = comms.getInputStream();
		byte[] IVData = new byte[16];
		in.read(IVData);
		return new IvParameterSpec(IVData);
	}

	private static Key getAESKey(Socket comms) throws IOException, InvalidKeyException, NoSuchAlgorithmException
	{
		InputStream in = comms.getInputStream();
		byte[] keyLenBytes = new byte[4];
		in.read(keyLenBytes);
		int keyLen = ByteHelp.bytesToInt(keyLenBytes);
		byte[] wrappedKey = new byte[keyLen];
		in.read(wrappedKey);

		return privateCipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
	}

	private static boolean accessAllowed(Socket comms, Cipher decrypt) throws InputMismatchException, IOException, IllegalBlockSizeException, BadPaddingException
	{
		SystemFileReader users = new SystemFileReader("users.txt");

		InputStream in = comms.getInputStream();
		byte[] dataLen = new byte[4];

		// Get username
		in.read(dataLen);
		int usernameLen = ByteHelp.bytesToInt(dataLen);
		byte[] encryptedUsername = new byte[usernameLen];
		in.read(encryptedUsername);
		String username = new String(decrypt.doFinal(encryptedUsername));

		// See if user is in file
		String expectedPassword = users.get(username);
		if(null != expectedPassword)
		{
			// Username is in file now check password
			in.read(dataLen);
			int passwordLen = ByteHelp.bytesToInt(dataLen);
			byte[] encryptedPassword = new byte[passwordLen];
			in.read(encryptedPassword);
			String gotPassword = new String(decrypt.doFinal(encryptedPassword));

			return expectedPassword.equals(gotPassword);
		}

		return false;
	}

	/**
	 * Responds to the given packet using the given handler.
	 * @param got
	 *     The packet to respond to
	 * @param comms
	 *     The host connection to use
	 * @param encrypt
	 * @param decrypt
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void respond(Packet got, Socket comms, Cipher encrypt, Cipher decrypt) throws IOException, InterruptedException
	{
		Packet send = new Packet(null, 0, got.getPath());
		switch(got.getCmd())
		{
			case CREATE_DIRECTORY:
				// Create requested directory and report result
				if(files.createDirectory(got.getPath()))
				{
					send.setCommand(Command.SUCCESS);
				}
				else
				{
					send.setCommand(Command.FAILED);
				}
				break;
			case GET_STATUS:
				// Send whether the file on host is new, old, or same version from date modified
				send.setCommand(Command.GET_STATUS);
				File f = new File(files.getFullPath(got.getPath()));
				if(f.exists())
				{
					send.setFileDate(f.lastModified());
					if(f.lastModified() > got.getFileDate())
					{
						send.setStatus(FileStatus.NEW_VERSION);
					}
					else if(f.lastModified() < got.getFileDate())
					{
						send.setStatus(FileStatus.OLD_VERSION);
					}
					else
					{
						send.setStatus(FileStatus.SAME_VERSION);
					}
				}
				else
				{
					send.setStatus(FileStatus.NOT_FOUND);
				}
				CommunicationHelp.sendPacket(send, comms);
				break;
			case RECEIVE_FILE:
				// Writes file to host drive and sends result
				File write = new File(files.getFullPath(got.getPath()));
				CommunicationHelp.receiveFile(write, comms, decrypt);
				write.setLastModified(got.getFileDate());
				send.setCommand(Command.SUCCESS);
				CommunicationHelp.sendPacket(send, comms);
				break;
			case SEND_FILE:
				// Prepares client to receive and sends file
				send.setCommand(Command.RECEIVE_FILE);
				File sendFile = new File(files.getFullPath(got.getPath()));
				send.setFileDate(sendFile.lastModified());
				CommunicationHelp.sendPacket(send, comms);
				CommunicationHelp.sendFile(sendFile, comms, encrypt);
				break;
			case SEND_FILE_LIST:
				// Sends list of files on host for client to request missing ones
				// TODO
				sendFileList(comms.getOutputStream(), new File(files.getFullPath(got.getPath())), files.getFullPath(got.getPath()));
				break;
			default:
				// reportFail("Invalid request to host.", comms);
				break;
		}
	}

	private static void reportFail(String message, Socket comms)
	{
		CommunicationHelp.forceSendPacket(new Packet(Command.FAILED), comms);
		CommunicationHelp.clearInput(comms);
	}

	/**
	 * Sends a list of available files for backup to the client to check for any
	 * missing ones.
	 * @param out
	 * @param f
	 *     The file being send
	 * @param pathRemove
	 *     The machine specific path to be removed before sending
	 * @throws IOException
	 */
	public static void sendFileList(OutputStream out, File f, String pathRemove) throws IOException
	{
		if(f.isDirectory())
		{
			for(File ff : f.listFiles())
			{
				sendFileListRecursive(out, ff, pathRemove);
			}
		}
		out.write((byte) 0xFF);
	}

	/**
	 * Sends a list of available files for backup to the client to check for any
	 * missing ones.
	 * @param out
	 * @param f
	 *     The file being send
	 * @param pathRemove
	 *     The machine specific path to be removed before sending
	 * @throws IOException
	 */
	private static void sendFileListRecursive(OutputStream out, File f, String pathRemove) throws IOException
	{
		if(f.isHidden() || (f.isDirectory() && null == f.list()))
		{
			return;
		}

		String send = f.getPath().replace(pathRemove, "");
		out.write(send.getBytes());
		out.write((byte) 0x0D);

		if(f.isDirectory())
		{
			for(File ff : f.listFiles())
			{
				sendFileListRecursive(out, ff, pathRemove);
			}
		}
	}
}