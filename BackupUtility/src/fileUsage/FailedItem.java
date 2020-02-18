package fileUsage;

import java.io.File;

/**
 * Class for items that failed.
 * 
 * @author JoelNeppel
 *
 */
public class FailedItem
{
	/**
	 * The file that failed during backup
	 */
	private File failedFile;

	/**
	 * The head directory for the file that failed
	 */
	private BackupItem head;

	/**
	 * @param failed
	 *     The file that failed to be backed up
	 * @param head
	 *     The head for the file that failed
	 */
	public FailedItem(File failed, BackupItem head)
	{
		failedFile = failed;
		this.head = head;
	}

	/**
	 * Returns the file that failed during backup.
	 * @return The failed file
	 */
	public File getFile()
	{
		return failedFile;
	}

	/**
	 * Returns the head directory for the file to be used during backup.
	 * @return The head directory
	 */
	public BackupItem getHead()
	{
		return head;
	}

	@Override
	public String toString()
	{
		return "Failed: " + failedFile.toString();
	}
}