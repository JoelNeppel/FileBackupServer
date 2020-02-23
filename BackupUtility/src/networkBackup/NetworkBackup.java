package networkBackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.InputMismatchException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import communications.Command;
import communications.CommunicationHelp;
import communications.Packet;
import exceptions.ItemNotFoundException;
import exceptions.SystemErrorException;
import fileBackup.BackupInitilizer;
import fileBackup.BackupPreparer;
import fileBackup.FileChecker;
import fileUsage.BackupItem;
import fileUsage.FileStatus;
import fileUsage.SystemFileReader;
import lists.SinglyLinkedList;

/**
 * @author JoelNeppel
 *
 */
public class NetworkBackup extends FileChecker implements BackupPreparer, BackupInitilizer
{
	String name;

	private Socket comms;

	private Cipher encrypt;

	private Cipher decrypt;

	@Override
	public void initilize(String got)
	{
		name = got;
	}

	@Override
	public void setUp()
	{
		System.out.println("Setup " + toString());
		try
		{
			// Get settings
			SystemFileReader settings = new SystemFileReader("NetworkBackupSettings\\" + name + ".txt");

			// Connect to host
			try
			{
				comms = new Socket(settings.get("Host"), Integer.parseInt(settings.get("Port")));
			}
			catch(ItemNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Share AES cipher with host to securely send files
			// Get Host RSA public key and create cipher
			File encodedKey = new File("RSAPublicEncodedKey");
			FileInputStream in = new FileInputStream(encodedKey);
			byte[] read = new byte[(int) encodedKey.length()];
			in.read(read);
			in.close();
			X509EncodedKeySpec spec = new X509EncodedKeySpec(read);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PublicKey publicKey = kf.generatePublic(spec);
			Cipher publicHost = Cipher.getInstance("RSA");
			publicHost.init(Cipher.WRAP_MODE, publicKey);
			// Create AES key to share with host
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			keygen.init(128);
			SecretKey aeskey = keygen.generateKey();
			// Create cipher using AES key
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.WRAP_MODE, aeskey);
			// Send host 16 byte cipher IV, wrapped key length, and wrapped key
			byte[] wrappedKey = publicHost.wrap(aeskey); // Wrap using host's public key
			ByteBuffer buffer = ByteBuffer.allocate(16 + Integer.BYTES + wrappedKey.length);
			buffer.put(cipher.getIV());
			buffer.putInt(wrappedKey.length);
			buffer.put(wrappedKey);
			OutputStream out = comms.getOutputStream();
			out.write(buffer.array());

			// Create encryption and decryption cipher using shared AES key and IV
			encrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
			encrypt.init(Cipher.ENCRYPT_MODE, aeskey, cipher.getParameters());
			decrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
			decrypt.init(Cipher.DECRYPT_MODE, aeskey, cipher.getParameters());

			// Send host AES encrypted username and password for login
			try
			{
				byte[] username = encrypt.doFinal(settings.get("Username").getBytes());
				byte[] password = encrypt.doFinal(settings.get("Password").getBytes());
				buffer = ByteBuffer.allocate(2 * Integer.BYTES + username.length + password.length);
				buffer.putInt(username.length);
				buffer.put(username);
				buffer.putInt(password.length);
				buffer.put(password);
				out.write(buffer.array());
			}
			catch(ItemNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// TODO maybe check login accepted
		}
		catch(InputMismatchException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(NumberFormatException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(UnknownHostException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(InvalidKeySpecException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(NoSuchPaddingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(InvalidKeyException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IllegalBlockSizeException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(InvalidAlgorithmParameterException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(BadPaddingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void tearDown()
	{
		// TODO Auto-generated method stub
		System.out.println("Tear-down " + toString());
		if(null != comms)
		{
			try
			{
				CommunicationHelp.forceSendPacket(new Packet(Command.CLOSE), comms);
				comms.close();
				comms = null;
			}
			catch(IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		encrypt = null;
		decrypt = null;
	}

	@Override
	public boolean checkSystemReady()
	{
		System.out.println("Check ready " + toString());
		// True only if the socket is connected and not closed
		return null != comms && comms.isConnected() && !comms.isClosed();
	}

	@Override
	public FileStatus getStatus(BackupItem head, File check) throws InterruptedException, SystemErrorException
	{
		try
		{
			System.out.println("Status of " + check + toString());
			Packet send = new Packet(Command.GET_STATUS, check.lastModified(), head.getPathToSend(check));
			CommunicationHelp.sendPacket(send, comms);
			Packet got = CommunicationHelp.receivePacket(comms);
			return got.getStatus();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean createDirectory(BackupItem head, File directory) throws InterruptedException, SystemErrorException
	{
		System.out.println("Create directory " + directory + " " + toString());
		Packet got = new Packet();
		try
		{
			CommunicationHelp.sendPacket(new Packet(Command.CREATE_DIRECTORY, head.getPathToSend(directory)), comms);
			got = CommunicationHelp.receivePacket(comms);
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return got.getCmd() == Command.SUCCESS;
	}

	@Override
	public boolean getUpdatedFile(BackupItem head, File receive) throws InterruptedException, SystemErrorException
	{
		System.out.println("Receive " + receive + toString());
		try
		{
			CommunicationHelp.sendPacket(new Packet(Command.SEND_FILE, head.getPathToSend(receive)), comms);
			Packet got = CommunicationHelp.receivePacket(comms);
			if(got.getCmd() == Command.RECEIVE_FILE)
			{
				CommunicationHelp.receiveFile(receive, comms, decrypt);
				receive.setLastModified(got.getFileDate());
				return true;
			}
		}
		catch(IOException e)
		{
			// TODO
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean sendUpdatedFile(BackupItem head, File send) throws InterruptedException, SystemErrorException
	{
		System.out.println("Send " + send + toString());
		try
		{
			CommunicationHelp.sendPacket(new Packet(Command.RECEIVE_FILE, send.lastModified(), head.getPathToSend(send)), comms);
			CommunicationHelp.sendFile(send, comms, encrypt);
			Packet got = CommunicationHelp.receivePacket(comms);
			return got.getCmd() == Command.SUCCESS;
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean getMissing(BackupItem check) throws InterruptedException, SystemErrorException
	{
		System.out.println("Check Missing " + check + toString());
		try
		{
			CommunicationHelp.sendPacket(new Packet(Command.SEND_FILE_LIST, check.getPathToSend()), comms);
			SinglyLinkedList<File> missing = new SinglyLinkedList<>();
			InputStream in = comms.getInputStream();

			String cur = "";
			int got = in.read();
			while(0xFF != got)
			{
				if(0x0D == got)
				{
					File f = new File(check.getFullPath(cur));
					if(!f.exists())
					{
						missing.add(f);
					}
					cur = "";
				}
				else
				{
					cur += (char) got;
				}

				got = in.read();
			}

			for(File f : missing)
			{
				FileStatus status = getStatus(check, f);
				if(FileStatus.DIRECTORY == status)
				{
					// File is a directory, create directory
					if(!f.mkdir())
					{
						// Return if directory could not be created, upcoming files depend on creation
						return false;
					}
				}
				else
				{
					getUpdatedFile(check, f);
				}
			}

			return true;
		}
		catch(IOException e)
		{

		}

		return false;
	}

	@Override
	public String toString()
	{
		return "NB" + name;
	}

	@Override
	public String output()
	{
		return name;
	}
}