package networkBackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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
import exceptions.ItemNotFoundException;
import fileUsage.FileStatus;
import fileUsage.SystemFileReader;

/**
 * Backup server class for distributing the most up to date files throughout all
 * connected devices by starting responding to each device. The server socket
 * will remain open waiting to accept any connection request.
 *
 * @author JoelNeppel
 *
 */
public class HostApp
{
	/**
	 * Hash map for the settings
	 */
	private static SystemFileReader settings;

	/**
	 * RSA Cipher with private key. Used to share AES encryption with client
	 */
	private static Cipher privateCipher;

	/**
	 * The path where all the system files are to be located
	 */
	private static final String SYSTEM_PATH = "/mnt/BackupDrive/ProtectedResources/";

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
			settings = new SystemFileReader(SYSTEM_PATH + "HostSettings.txt");

			// Check head directory where files will be backed up to
			String check = settings.get("Storage Location");
			if(!new File(check).isDirectory())
			{
				throw new FileNotFoundException("The given path to back up files " + check + " must be a directory.");
			}

			// Create RSA cipher using a private encoded key
			privateCipher = Cipher.getInstance("RSA");
			File encodedKey = new File(SYSTEM_PATH + settings.get("Encoded Key File"));
			if(!encodedKey.exists())
			{
				throw new FileNotFoundException("The public key must be included in the file: " + encodedKey.getAbsolutePath());
			}
			FileInputStream fileIn = new FileInputStream(encodedKey);
			byte[] read = new byte[(int) encodedKey.length()];
			fileIn.read(read);
			fileIn.close();
			PKCS8EncodedKeySpec spec2 = new PKCS8EncodedKeySpec(read);
			KeyFactory kf2 = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = kf2.generatePrivate(spec2);
			// Only function for cipher will be to unwrap shared AES key
			privateCipher.init(Cipher.UNWRAP_MODE, privateKey);
		}
		catch(InputMismatchException | IOException | ItemNotFoundException e)
		{
			// Problem from getting settings or key from file
			e.printStackTrace();
			System.exit(1);
		}
		catch(InvalidKeySpecException | InvalidKeyException e)
		{
			System.out.println("Problem with RSA private key.");
			e.printStackTrace();
			System.exit(2);
		}
		catch(NoSuchAlgorithmException | NoSuchPaddingException e)
		{
			// Will not happen, algorithm is in code
		}

		ServerSocket server = null;
		while(null == server)
		{
			// Attempts to create server socket until it succeeds
			try
			{
				// Attempts to create socket server
				server = new ServerSocket(Integer.parseInt(settings.get("Port")));
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
				Socket s = server.accept();
				System.out.println("Started client connection: " + s);
				handleConnection(s);
			}
			catch(IOException e)
			{
				// Connection to client failed, ignore and allow other clients to connect
				System.out.println("Connection to client failed.");
			}
		}

		try
		{
			if(null != server)
			{
				server.close();
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
				// Create ciphers from shared AES IV and key
				IvParameterSpec parameter = getIV(comms);
				Key AESKey = getAESKey(comms);
				encrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
				encrypt.init(Cipher.ENCRYPT_MODE, AESKey, parameter);
				decrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
				decrypt.init(Cipher.DECRYPT_MODE, AESKey, parameter);

				// Only respond if user is approved
				if(!accessAllowed(comms, decrypt))
				{
					comms.close();
					return;
				}
			}
			catch(IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e)
			{
				System.out.println("Problem with shared AES cipher.");
				e.printStackTrace();
				try
				{
					comms.close();
				}
				catch(IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return;
			}

			boolean close = false;
			// Checks again if the directory to write files to exists
			File check = new File(getFullPath(""));
			if(!check.exists() || !check.isDirectory())
			{
				// reportFail("File location to write items to does not exist.", comms);
				System.out.println("Given directory " + check.getAbsolutePath() + " does not exist");
				try
				{
					comms.close();
				}
				catch(IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}

			while(!close && !comms.isClosed())
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

	/**
	 * Receives the IV from the client when sharing an AES cipher.
	 * @param comms
	 *     The socket to use to communicate with the client
	 * @return The 16 byte IV used in the creation of an AES cipher
	 * @throws IOException
	 */
	private static IvParameterSpec getIV(Socket comms) throws IOException
	{
		InputStream in = comms.getInputStream();
		byte[] IVData = new byte[16];
		in.read(IVData);
		return new IvParameterSpec(IVData);
	}

	/**
	 * Receives the encoded AES key being shared by the client.
	 * @param comms
	 *     The socket to be used to communicate with the client
	 * @return The AES key used to create the shared AES cipher
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 */
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

	/**
	 * Another security step that both checks if the user is allowed and supplies
	 * the correct password while using the shared cipher to ensure it is correct.
	 * Receives encrypted username and password and decrypts using the given cipher
	 * which is then checked if it is contained in the username file supplied by the
	 * host settings file.
	 * @param comms
	 *     The socket to receive the encrypted username and password from
	 * @param decrypt
	 *     The cipher to be used to decrypt the username and password
	 * @return True if the user is in the allowed users file and supplied the
	 *     matching password, false otherwise
	 * @throws IOException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private static boolean accessAllowed(Socket comms, Cipher decrypt) throws IOException, IllegalBlockSizeException, BadPaddingException
	{
		InputStream in = comms.getInputStream();
		byte[] dataLen = new byte[4];

		// Get username
		in.read(dataLen);
		int usernameLen = ByteHelp.bytesToInt(dataLen);
		byte[] encryptedUsername = new byte[usernameLen];
		in.read(encryptedUsername);
		String username = new String(decrypt.doFinal(encryptedUsername));

		// Create map of usernames and passwords
		SystemFileReader users = null;
		try
		{
			users = new SystemFileReader(SYSTEM_PATH + settings.get("Users File"));
		}
		catch(ItemNotFoundException | FileNotFoundException | InputMismatchException e)
		{
			// Critical file is missing
			e.printStackTrace();
			System.exit(3);
		}

		// See if user is in file
		try
		{
			String expectedPassword = users.get(username);

			// Username is in file now check password
			in.read(dataLen);
			int passwordLen = ByteHelp.bytesToInt(dataLen);
			byte[] encryptedPassword = new byte[passwordLen];
			in.read(encryptedPassword);
			String gotPassword = new String(decrypt.doFinal(encryptedPassword));
			System.out.println("allowed access to: " + username);
			return expectedPassword.equals(gotPassword);
		}
		catch(ItemNotFoundException e)
		{
			// Username is not in file
			return false;
		}
	}

	/**
	 * Does the appropriate actions requested by the received packet and will send a
	 * response if necessary.
	 * @param got
	 *     The packet to respond to
	 * @param comms
	 *     The socket used to communicate with the client
	 * @param encrypt
	 *     The encryption algorithm to use if needed
	 * @param decrypt
	 *     The decryption algorithm to use if needed
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void respond(Packet got, Socket comms, Cipher encrypt, Cipher decrypt) throws IOException, InterruptedException
	{
		Packet send = new Packet(null, 0, got.getPath());
		switch(got.getCmd())
		{
			case GET_STATUS:
				// Send whether the file on host is new, old, or same version from date modified
				send.setCommand(Command.GET_STATUS);
				File f = new File(getFullPath(got.getPath()));
				if(f.exists())
				{
					// Checks if the requested file is a directory, normally only used after
					// checking for missing
					if(f.isDirectory())
					{
						send.setStatus(FileStatus.DIRECTORY);
					}
					else
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
				}
				else
				{
					send.setStatus(FileStatus.NOT_FOUND);
				}
				CommunicationHelp.sendPacket(send, comms);
				break;
			case CREATE_DIRECTORY:
				// Create requested directory and report result
				File newDirectory = new File(getFullPath(got.getPath()));
				if(newDirectory.exists() || newDirectory.mkdirs())
				{
					send.setCommand(Command.SUCCESS);
				}
				else
				{
					send.setCommand(Command.FAILED);
				}
				CommunicationHelp.sendPacket(send, comms);
				break;
			case SEND_FILE:
				// Prepares client to receive and sends file
				send.setCommand(Command.RECEIVE_FILE);
				File sendFile = new File(getFullPath(got.getPath()));
				send.setFileDate(sendFile.lastModified());
				CommunicationHelp.sendPacket(send, comms);
				CommunicationHelp.sendFile(sendFile, comms, encrypt);
				break;
			case RECEIVE_FILE:
				// Writes file to host drive and sends result
				File write = new File(getFullPath(got.getPath()));
				CommunicationHelp.receiveFile(write, comms, decrypt);
				write.setLastModified(got.getFileDate());
				send.setCommand(Command.SUCCESS);
				CommunicationHelp.sendPacket(send, comms);
				break;
			case SEND_FILE_LIST:
				// Sends list of files on host for client to request missing ones
				File check = new File(getFullPath(got.getPath()));
				sendFileList(comms.getOutputStream(), check, check.getAbsolutePath().replace(got.getPath(), ""));
				break;
			default:
				// reportFail("Invalid request to host.", comms);
				break;
		}
	}

	/**
	 * Sends a list of available files for backup to the client to check for any
	 * missing ones.
	 * @param out
	 *     The output stream used to send data to the client
	 * @param f
	 *     The file being sent
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
		// Finished byte
		out.write((byte) 0xFF);
	}

	/**
	 * Sends a list of available files for backup to the client to check for any
	 * missing ones.
	 * @param out
	 *     The output stream used to send data to the client
	 * @param f
	 *     The file being sent
	 * @param pathRemove
	 *     The machine specific path to be removed before sending
	 * @throws IOException
	 */
	private static void sendFileListRecursive(OutputStream out, File f, String pathRemove) throws IOException
	{
		if(f.isHidden() || (!f.isFile() && !f.isDirectory()) || (f.isDirectory() && null == f.listFiles()))
		{
			return;
		}

		String send = f.getPath().replace(pathRemove, "");
		out.write(send.getBytes());
		// File separation byte
		out.write((byte) 0x0D);

		if(f.isDirectory())
		{
			for(File ff : f.listFiles())
			{
				sendFileListRecursive(out, ff, pathRemove);
			}
		}
	}

	/**
	 * Returns the full path for the location where to store the file. The full path
	 * is from the system set backup location with the relative path appended.
	 * @param relative
	 *     The relative path sent by the client
	 * @return The full path for where to store the file
	 */
	private static String getFullPath(String relative)
	{
		try
		{
			return settings.get("Storage Location") + relative;
		}
		catch(ItemNotFoundException e)
		{
			// Already checked, should not happen
			return null;
		}
	}
}