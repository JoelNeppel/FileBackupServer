package fileBackup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.InputMismatchException;

import javax.management.timer.Timer;

import com.sun.scenario.Settings;

import communications.ClientConnection;
import communications.Command;
import communications.FileStatus;
import communications.Packet;
import exceptions.ActionFailedException;
import exceptions.FailedConnectionException;
import fileUsage.BackupItem;
import fileUsage.BackupItem.BackupAction;
import fileUsage.FailedItem;
import fileUsage.FileItemHandler;
import fileUsage.SystemFileReader;
import javafx.application.Application;
import javafx.collections.ObservableList;
import lists.DoublyLinkedList;
import lists.SinglyLinkedList;

/**
 * Backup utility for the client side. Periodically backs up files in selected
 * locations
 *
 * @author JoelNeppel
 *
 */
public class ClientApp
{
	/**
	 * Contains list of files to backup and handles their modification
	 */
	private static FileItemHandler files;

	/**
	 * The client connection handler
	 */
	private static ClientConnection comms;

	/**
	 * The list of failed items
	 */
	private static SinglyLinkedList<FailedItem> failedItems;

	/**
	 * Stores settings for client read form a file
	 */
	private static SystemFileReader settings;

	/**
	 * True if the backup started, false otherwise
	 */
	private static boolean started;

	/**
	 * Reads user's settings from file and periodically backs up desired files.
	 * @param args
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args)
	{
		// Read files to back up and action from file
		File fileList = new File("ClientItemsList.txt");
		if(fileList.exists())
		{
			files = new FileItemHandler(fileList);
		}
		else
		{
			files = new FileItemHandler();
		}

		// Get settings from file
		try
		{
			settings = new SystemFileReader("ClientSettings.txt");
		}
		catch(InputMismatchException | FileNotFoundException e)
		{
			System.out.println("ClientSettings.txt file error: " + e.getMessage());
		}

		// Start graphics
		new Thread(()->
		{
			Application.launch(ClientGraphic.class, args);
			if(null != comms)
			{
				comms.close();
			}
			System.exit(0);
		}).start();
	}

	/**
	 * Backs up items to host.
	 */
	public static void backup()
	{
		if(started)
		{
			return;
		}
		started = true;

		try
		{
			// Checks if settings demands check of IP address to see if correct network
			// Check if connected to correct network based on IP address.
			ClientGraphic.setAction("Beginning backup...");
			if(!Boolean.parseBoolean(settings.get("Check IP"))
					|| settings.get("Expected IP").equals(InetAddress.getLocalHost().getHostAddress()))
			{
				setUpComms();
				failedItems = new SinglyLinkedList<>();

				for(BackupItem item : files.getItemList())
				{
					try
					{
						backupFile(item.getFile(), item.getAction(), item.getPathToRemove());

						if(item.getAction().shouldPullMissing())
						{
							checkMissing(item.getFile(), item.getPathToRemove(), item.getPathToSend());
						}
					}
					catch(IOException e)
					{
						// Add to list of failed items
						failedItems.add(new FailedItem(item.getPathToRemove() + item.getPathToSend(), item.getAction(),
								item.getPathToRemove()));
					}
				}

				// Attempt failed items again
				for(FailedItem item : failedItems)
				{
					try
					{
						backupFile(item.getFile(), item.getAction(), item.getPathToRemove());
						if(item.getAction().shouldPullMissing())
						{
							checkMissing(item.getFile(), item.getPathToRemove(), item.getPathToSend());
						}
					}
					catch(IOException e)
					{
						System.out.println(item + " failed");
					}
				}
			}
		}
		catch(FailedConnectionException | UnknownHostException e)
		{
			System.out.println("Could not connect to host.");
		}
		catch(FileNotFoundException e)
		{
			System.out.println("Settings file not found.");
		}
		catch(InterruptedException e)
		{
		}
		finally
		{
			if(null != comms)
			{
				comms.forceSendPacket(new Packet(Command.CLOSE));
				comms.close();
				comms = null;
			}

			failedItems = null;
			started = false;

			ClientGraphic.setHostInfo("Disconnected");
			ClientGraphic.setFileAction("Finished", "");
		}
	}

	/**
	 * Attempts to connect to the host five times and then gives up if still not
	 * connected.
	 * @param host
	 *     The host location
	 * @param port
	 *     The port to use
	 * @throws FailedConnectionException
	 *     If connecting to the host failed five times
	 */
	private static void setUpComms() throws FailedConnectionException
	{
		ClientGraphic.setHostInfo("Connecting...");
		int numAttempts = 0;
		while(null == comms)
		{
			try
			{
				Socket s = new Socket(settings.get("Host"), Integer.parseInt(settings.get("Port")));

				// Send password to connect
				s.getOutputStream().write(settings.get("Password").getBytes());
				int msg = -1;
				while(msg == -1)
				{
					Thread.sleep(10);
					msg = s.getInputStream().read();
				}
				if(msg != 1)
				{
					s.close();
					throw new FailedConnectionException();
				}

				comms = new ClientConnection(s, Settings.get("Cipher Key"));
				ClientGraphic.setHostInfo("Connected" + "\n IP Address: " + s.getInetAddress().getHostAddress()
						+ "\n Host Name: " + s.getInetAddress().getHostName() + "\n Port: " + s.getPort());
			}
			catch(IOException e)
			{
				if(numAttempts > 5)
				{
					throw new FailedConnectionException();
				}
				else
				{
					try
					{
						Thread.sleep(numAttempts * Timer.ONE_MINUTE);
					}
					catch(InterruptedException e1)
					{
						System.exit(0);
					}
					numAttempts++;
				}
			}
			catch(Exception e)
			{
				ClientGraphic.setHostInfo("ClientSettings.txt probable error: " + e.getMessage());
				throw new FailedConnectionException();
			}
		}
	}

	/**
	 * Backs up a file and all sub files if it is a directory. Communicates with the
	 * host through the ClientConnection.
	 * @param file
	 *     The file being backed up
	 * @param action
	 *     The action to perform on the file
	 * @param pathRemove
	 *     The machine specific path to remove before sending
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ActionFailedException
	 */
	private static void backupFile(File file, BackupAction action, String pathRemove)
			throws IOException, InterruptedException
	{
		if(file.isHidden())
		{
			// Don't let hidden files be backed-up, not usually wanted ie. temp files
			return;
		}

		ClientGraphic.setFile(file.getPath());
		if(file.isDirectory())
		{
			if(null == file.listFiles())
			{
				// Weird reason directory not directory look into later
				return;
			}

			ClientGraphic.setAction("Checking folder");

			// Check if folder exists
			Packet p = new Packet(Command.CREATE_DIRECTORY, 0, file.getPath().replace(pathRemove, ""));
			comms.sendPacket(p);

			Packet got = comms.receivePacket();
			if(Command.SUCCESS != got.getCmd())
			{
				failedItems.add(new FailedItem(file.getPath(), action, pathRemove));
			}
			else
			{
				// Backup each item in the folder if directory was successfully created/already
				// existed
				for(File f : file.listFiles())
				{
					try
					{
						backupFile(f, action, pathRemove);
					}
					catch(IOException e)
					{
						// Add to list of failed items
						failedItems.add(new FailedItem(file.getPath(), action, pathRemove));
					}
				}
			}
		}
		else
		{
			// File is a single item
			// Get status of file on host
			ClientGraphic.setAction("Getting status");
			Packet pac = new Packet(Command.GET_STATUS, file.lastModified(), file.getPath().replace(pathRemove, ""));
			comms.sendPacket(pac);
			Packet got = comms.receivePacket();
			FileStatus status = got.getStatus();
			if(FileStatus.NEW_VERSION == status)
			{
				if(action.shouldPullMostRecent())
				{
					// Receive most recent version if action requires pull
					ClientGraphic.setAction("Receiving file");
					pac.setCommand(Command.SEND_FILE);
					comms.sendPacket(pac);
					comms.receiveFile(file);
					file.setLastModified(got.getFileDate());
				}
			}
			else if((FileStatus.OLD_VERSION == status && action.shouldPushMostRecent())
					|| (FileStatus.NOT_FOUND == status && action.shouldPushMissing()))
			{
				// Send most recent version if:
				// host is out dated and action demands host has most recent
				// host is missing file and action demands push of missing
				ClientGraphic.setAction("Sending file");
				pac.setCommand(Command.RECEIVE_FILE);
				comms.sendPacket(pac);
				comms.sendFile(file);

				// Confirm success
				got = comms.receivePacket();
				if(Command.SUCCESS != got.getCmd())
				{
					failedItems.add(new FailedItem(file.getPath(), action, pathRemove));
				}
			}
		}
	}

	/**
	 * Checks for file missing on client by receiving a file list from the host and
	 * making sure all files in that list exist.
	 * @param file
	 *     The file being checked, usually a directory
	 * @param addPath
	 *     The path that will be added to each relative path
	 * @param sendPath
	 *     The relative file path that will be sent
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void checkMissing(File file, String addPath, String sendPath)
			throws IOException, InterruptedException
	{
		// Get list
		comms.sendPacket(new Packet(Command.SEND_FILE_LIST, 0, sendPath));
		DoublyLinkedList<String> list = comms.receiveFileList();

		// Check that each file in the path exists
		for(String path : list)
		{
			// TODO error handling
			File check = new File(addPath + path);
			if(!check.exists())
			{
				// Receive file if it does not exist
				comms.sendPacket(new Packet(Command.SEND_FILE, 0, path));
				Packet got = comms.receivePacket();

				if(Command.CREATE_DIRECTORY == got.getCmd())
				{
					check.mkdir();
				}
				else if(Command.RECEIVE_FILE == got.getCmd())
				{
					comms.receiveFile(check);
					check.setLastModified(got.getFileDate());
				}
				else
				{
					// TODO bad command error
				}
			}
		}
	}

	/**
	 * Returns the list of items to be backed up.
	 * @return The list of items
	 */
	public static ObservableList<BackupItem> getItems()
	{
		return files.getItemList();
	}
}