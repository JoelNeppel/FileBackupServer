package fileUsage;

import java.io.FileNotFoundException;

/**
 * Class for items that failed.
 * 
 * @author JoelNeppel
 *
 */
public class FailedItem extends BackupItem
{
	/**
	 * The path to remove for the item
	 */
	private String pathRemove;

	/**
	 * @param fileLocation
	 *     The location for this file
	 * @param action
	 *     The action to perform on this item
	 * @param pathToRemove
	 *     The path to remove for this item
	 * @throws FileNotFoundException
	 */
	public FailedItem(String fileLocation, BackupAction action, String pathToRemove) throws FileNotFoundException
	{
		super(fileLocation, action);
		pathRemove = pathToRemove;
	}

	@Override
	public String getPathToRemove()
	{
		return pathRemove;
	}
}