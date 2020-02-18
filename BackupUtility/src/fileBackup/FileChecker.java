package fileBackup;

import java.io.File;

import exceptions.SystemErrorException;
import fileUsage.BackupItem;
import fileUsage.FileStatus;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

/**
 * Interface for different backup methods that compares and transfers files from
 * the local machine to a backup location.
 * 
 * @author JoelNeppel
 *
 */
public abstract class FileChecker
{
	/**
	 * The display name for the backup method
	 */
	private String name;

	private GridPane grid;

	protected FileChecker()
	{
		name = this.getClass().getSimpleName();
	}

	/**
	 * Returns true if the system can use this type for backup, false if it is not
	 * ready and should not be used.
	 * @return True if ready, false otherwise
	 */
	public abstract boolean checkSystemReady();

	/**
	 * Creates the given directory on the backup location
	 * @param head
	 *     The head file or directory that can be used to obtain the relative path
	 *     for the backup location
	 * @param directory
	 *     The directory to create
	 * @return True if the directory was created or already exists, false if it
	 *     failed
	 * @throws InterruptedException
	 *     If the system was interrupted
	 * @throws SystemErrorException
	 *     If there was a critical error that cannot be recovered from
	 */
	public abstract boolean createDirectory(BackupItem head, File directory) throws InterruptedException, SystemErrorException;

	/**
	 * Compares the given file to the one at the backup location and returns the
	 * comparison.
	 * @param head
	 *     The head file or directory that can be used to obtain the relative path
	 *     for the backup location
	 * @param check
	 *     The file to check the status of
	 * @return The comparison of the file at the backup location compared to the
	 *     given one
	 * @throws InterruptedException
	 *     If the system was interrupted
	 * @throws SystemErrorException
	 *     If there was a critical error that cannot be recovered from
	 */
	public abstract FileStatus getStatus(BackupItem head, File check) throws InterruptedException, SystemErrorException;

	/**
	 * Gets the file from the backup location and copies it to the given file.
	 * @param head
	 *     The head file or directory that can be used to obtain the relative path
	 *     for the backup location
	 * @param receive
	 *     The file to copy to
	 * @return True if the file was successfully transfered, false if not and the
	 *     system should restore the original file
	 * @throws InterruptedException
	 *     If the system was interrupted
	 * @throws SystemErrorException
	 *     If there was a critical error that cannot be recovered from
	 */
	public abstract boolean getUpdatedFile(BackupItem head, File receive) throws InterruptedException, SystemErrorException;

	/**
	 * Transfers the given file to the backup location to be overwritten.
	 * @param head
	 *     The head file or directory that can be used to obtain the relative path
	 *     for the backup location
	 * @param send
	 *     The file to send
	 * @return True if the file was successfully sent, false otherwise
	 * @throws InterruptedException
	 *     If the system was interrupted
	 * @throws SystemErrorException
	 *     If there was a critical error that cannot be recovered from
	 */
	public abstract boolean sendUpdatedFile(BackupItem head, File send) throws InterruptedException, SystemErrorException;

	/**
	 * Compares the contents of the given BackupItem to the contents at the backup
	 * location. Transfers any missing files from the backup location to the
	 * location.
	 * @param check
	 *     The BackupItem to check for missing
	 * @return True if the method was successful, false otherwise
	 * @throws InterruptedException
	 *     If the system was interrupted
	 * @throws SystemErrorException
	 *     If there was a critical error that cannot be recovered from
	 */
	public abstract boolean getMissing(BackupItem check) throws InterruptedException, SystemErrorException;

	public final GridPane getGrid()
	{
		if(null == grid)
		{
			grid = new GridPane();
			grid.setAlignment(Pos.CENTER);
			HBox buttons = new HBox();
			Node toAdd = getCustomSection(buttons);
			if(null != toAdd)
			{
				grid.add(toAdd, 0, 1);
			}

			grid.add(new Text(name), 0, 0);
		}

		return grid;
	}

	protected Node getCustomSection(HBox buttons)
	{
		return null;
	}
}