package networkBackup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.InputMismatchException;

import communications.Command;
import communications.HostConnection;
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

		int failedLogins = 0;
		while(!Thread.interrupted())
		{
			// Wait for a client to connect and begin handling
			try
			{
				// Check password
				Socket s = ss.accept();
				Thread.sleep(500);
				byte[] received = new byte[settings.get("Password").length()];
				s.getInputStream().read(received);

				if(settings.get("Password").equals(new String(received)))
				{
					// Accept and create HostConnection to handle connection
					s.getOutputStream().write(1);
					System.out.println("Started client connection: " + s);
					handleConnection(new HostConnection(s, settings.get("Cipher Key")));
				}
				else
				{
					// Password was incorrect, close connection
					s.close();
					failedLogins++;
					System.out.println("Security: Failed login");

					// Exit if too many failed attempts
					if(failedLogins >= 5)
					{
						System.out.println("Security exit");
						System.exit(3);
					}
				}
			}
			catch(IOException e)
			{
				// Connection to client failed, ignore and allow other clients to connect
			}
			catch(InterruptedException e)
			{
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
	private static void handleConnection(HostConnection comms)
	{
		new Thread(()->
		{
			boolean close = false;
			// Checks if the directory to write file to exists
			File check = new File(files.getFullPath(""));
			if(!check.exists() || !check.isDirectory())
			{
				reportFail("File location to write items to does not exist.", comms);
				System.out.println("Given directory location does not exist");
				return;
			}

			while(!close)
			{
				// Reply to any requests until the client closes
				try
				{
					// HostGraphic.updateFileAndAction(comms.getAddress(), "", "Receiving packet");
					Packet got = comms.receivePacket();
					if(got.getCmd() == Command.CLOSE)
					{
						// Close connection when requested by client
						close = true;
					}
					else
					{
						// Respond to the packet received
						respond(got, comms);
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
					reportFail("IO Exception Caught", comms);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			System.out.println("Closed: " + comms.getAddress());
			comms.close();
		}).start();
	}

	/**
	 * Responds to the given packet using the given handler.
	 * @param got
	 *     The packet to respond to
	 * @param comms
	 *     The host connection to use
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void respond(Packet got, HostConnection comms) throws IOException, InterruptedException
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
				comms.sendPacket(send);
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
				comms.sendPacket(send);
				break;
			case RECEIVE_FILE:
				// Writes file to host drive and sends result
				File write = new File(files.getFullPath(got.getPath()));
				comms.receiveFile(write);
				write.setLastModified(got.getFileDate());
				send.setCommand(Command.SUCCESS);
				comms.sendPacket(send);
				break;
			case SEND_FILE:
				// Prepares client to receive and sends file
				send.setCommand(Command.RECEIVE_FILE);
				File sendFile = new File(files.getFullPath(got.getPath()));
				send.setFileDate(sendFile.lastModified());
				comms.sendPacket(send);
				comms.sendFile(sendFile);
				break;
			case SEND_FILE_LIST:
				// Sends list of files on host for client to request missing ones
				comms.sendFileList(new File(files.getFullPath(got.getPath())), files.getFullPath(got.getPath()));
				break;
			default:
				reportFail("Invalid request to host.", comms);
				break;
		}
	}

	private static void reportFail(String message, HostConnection comms)
	{
		comms.forceSendPacket(new Packet(Command.FAILED));
		comms.clearInput();
	}
}