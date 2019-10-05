package fileUsage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Class for handling the backup items and their files and file paths to help
 * perform the correct actions for each file.
 *
 * @author JoelNeppel
 *
 */
public class FileItemHandler
{
	/**
	 * The items that will be backed up
	 */
	private ObservableList<BackupItem> items;
	// private DoublyLinkedList<BackupItem> items;

	/**
	 * Creates a new handler with an empty list
	 */
	public FileItemHandler()
	{
		items = FXCollections.observableArrayList();
	}

	/**
	 * Creates a new handler and adds the items from the file to the list of items
	 * to be backed up
	 * @param fileList
	 *     The text file that includes a list of items to be added to the backup
	 *     list
	 */
	public FileItemHandler(File fileList)
	{
		this();
		Scanner scan = null;
		try
		{
			scan = new Scanner(fileList);

			while(scan.hasNextLine())
			{
				Scanner lineScan = null;
				try
				{
					lineScan = new Scanner(scan.nextLine());
					lineScan.useDelimiter(":>");

					String path = lineScan.next().trim();
					BackupItem.BackupAction action = BackupItem.BackupAction.getFromString(lineScan.next().trim());

					if(lineScan.hasNext())
					{
						addItem(new BackupItem(path, action)
						{

						});
					}
					else
					{
						addItem(new BackupItem(path, action));
					}
				}
				catch(FileNotFoundException e)
				{
					System.out.println(e.getMessage());
				}
				catch(NoSuchElementException e)
				{
					System.out.println("Backup item list not in expected format.");
				}
				finally
				{
					if(null != lineScan)
					{
						lineScan.close();
					}
				}
			}
		}
		catch(FileNotFoundException e)
		{
			System.out.println("Item list file not found.");
		}
		finally
		{
			if(null != scan)
			{
				scan.close();
			}
		}
	}

	/**
	 * Returns the DoublyLinkedList of the backup items.
	 * @return The backup item list
	 */
	public ObservableList<BackupItem> getItemList()
	{
		return items;
	}

	/**
	 * Adds an item to the backup list.
	 * @param item
	 *     The item to be added
	 */
	public void addItem(BackupItem item)
	{
		items.add(item);
	}

	/**
	 * Removes an item from the list of items to be backed up
	 * @param item
	 *     The item to be removed
	 */
	public void removeItem(BackupItem item)
	{
		items.remove(item);
	}

	/**
	 * Checks if a file is missing using the relative path.
	 * @param relativePath
	 *     The relative path of the item
	 * @return True if the file is not found, false otherwise
	 */
	public boolean checkFileMissing(String relativePath)
	{
		String full = getFullPath(relativePath);
		if(null == full)
		{
			return false;
		}

		return !new File(full).exists();
	}

	/**
	 * Returns the full path for the item being backed up using the list of items to
	 * be backed up to find the one with the most consistent path with that of the
	 * relative path
	 * @param relativePath
	 *     The relative path for the file
	 * @return The full path for the file or null if one is not found in the list of
	 *     backup items
	 */
	public String getFullPath(String relativePath)
	{
		relativePath = convertPath(relativePath);
		for(BackupItem item : items)
		{
			if(relativePath.contains(item.getPathToSend()))
			{
				return item.getPathToRemove().concat(relativePath);
			}
		}

		return null;
	}

	/**
	 * Creates a directory at the relative path.
	 * @param relativePath
	 *     The relative path for the directory
	 * @return True if the directory was created or already exists, false otherwise
	 */
	public boolean createDirectory(String relativePath)
	{
		String path = getFullPath(relativePath);
		if(null == path)
		{
			return false;
		}

		File f = new File(path);
		if(f.exists())
		{
			return true;
		}

		return f.mkdir();
	}

	/**
	 * Converts the given path into the path for the file system of the machine
	 * running the program.
	 * @param path
	 *     The path that may or may not be in the correct file path format
	 * @return The path in the correct file path for the machine
	 */
	public String convertPath(String path)
	{
		if(path.contains(System.getProperty("file.separator")))
		{
			return path;
		}
		else
		{
			char replaceWith;
			char toReplace;
			if("/".equals(System.getProperty("file.separator")))
			{
				toReplace = '\\';
				replaceWith = '/';
			}
			else
			{
				toReplace = '/';
				replaceWith = '\\';
			}

			String result = path.replace(toReplace, replaceWith);
			return result;
		}
	}
}