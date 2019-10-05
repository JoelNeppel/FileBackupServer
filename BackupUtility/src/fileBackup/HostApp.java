package fileBackup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;

import javax.management.timer.Timer;

import com.sun.scenario.Settings;

import communications.Command;
import communications.FileStatus;
import communications.HostConnection;
import communications.Packet;
import fileUsage.FileItemHandler;
import javafx.application.Application;
import lists.SinglyLinkedList;

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
	private static FileItemHandler files;

	private static SinglyLinkedList<Thread> clientHandlers;

	/**
	 * Creates server socket on the designated port and accepts any request to
	 * connect and begins handling using HostConnection.
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("System: Started");
		Thread main = Thread.currentThread();
		clientHandlers = new SinglyLinkedList<>();
		new Thread(()->
		{
			Application.launch(HostGraphic.class, args);
			for(Thread t : clientHandlers)
			{
				t.interrupt();
			}
			main.interrupt();
			System.exit(0);
		}).start();

		int port = 0;
		try
		{
			// Get settings from file
			Scanner scan = new Scanner(new File("HostSettings.txt"));
			String defaultPath = scan.nextLine();
			files = new FileItemHandler()
			{
				@Override
				public String getFullPath(String relative)
				{
					// Change to the given backup folder path
					return defaultPath + convertPath(relative);
				}
			};
			port = scan.nextInt();
			scan.close();
		}
		catch(FileNotFoundException e)
		{
			System.out.println("HostSettings.txt not found");
			System.exit(1);
		}

		ServerSocket ss = null;
		try
		{
			while(ss == null)
			{
				// Attempts to create server socket until it succeeds
				try
				{
					// Attempts to create socket server
					ss = new ServerSocket(port);
					HostGraphic.setPort(port);
				}
				catch(IOException e)
				{

					// Wait a bit before trying to connect again
					Thread.sleep(5 * Timer.ONE_MINUTE);
				}
			}
		}
		catch(InterruptedException e)
		{

		}

		while(!Thread.interrupted())
		{
			// Wait for a client to connect and begin handling
			try
			{
				// Accept and create HostConnection to handle connection
				handleConnection(new HostConnection(ss.accept(), Settings.get("Cipher Key")));
			}
			catch(IOException e)
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
		Thread t = new Thread(()->
		{
			HostGraphic.addClient(comms.getAddress(), "", "Connected");
			boolean close = false;
			File check = new File(files.getFullPath(""));
			while(!close)
			{
				// Reply to any requests until the client closes
				try
				{
					// HostGraphic.updateFileAndAction(comms.getAddress(), "", "Receiving packet");
					Packet got = comms.receivePacket();
					HostGraphic.updateFile(comms.getAddress(), got.getPath());
					if(!check.exists() || !check.isDirectory())
					{
						// Checks if the directory to write file to exists
						reportFail("File location to write items to does not exist.", comms);
					}
					else if(got.getCmd() == Command.CLOSE)
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
					reportFail("IO Exception Caught.", comms);
				}
				catch(InterruptedException e)
				{
				}
			}

			HostGraphic.removeClient(comms.getAddress());
			comms.close();
			clientHandlers.remove(Thread.currentThread());
		});
		t.start();
		clientHandlers.add(t);
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
				HostGraphic.updateAction(comms.getAddress(), "Creating directory");
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
				HostGraphic.updateAction(comms.getAddress(), "Sending file status");
				send.setCommand(Command.GET_STATUS);
				File f = new File(files.getFullPath(got.getPath()));
				if(f.exists())
				{
					send.setFileDate(f.lastModified());
					if(f.lastModified() > got.getFileDate())
					{
						send.setStatus(FileStatus.NEW_VERSION);
					}
					else if(f.lastModified() > got.getFileDate())
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
				HostGraphic.updateAction(comms.getAddress(), "Receiving file");
				File write = new File(files.getFullPath(got.getPath()));
				comms.receiveFile(write);
				write.setLastModified(got.getFileDate());
				send.setCommand(Command.SUCCESS);
				comms.sendPacket(send);
				break;
			case SEND_FILE:
				HostGraphic.updateAction(comms.getAddress(), "Sending file");
				send.setCommand(Command.RECEIVE_FILE);
				comms.sendPacket(send);
				comms.sendFile(new File(files.getFullPath(got.getPath())));
				break;
			case SEND_FILE_LIST:
				HostGraphic.updateAction(comms.getAddress(), "Sending file list");
				comms.sendFileList(new File(files.getFullPath(got.getPath())),
						files.getFullPath(got.getPath()).replaceAll(got.getPath(), ""));
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