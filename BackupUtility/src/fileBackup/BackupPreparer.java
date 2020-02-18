package fileBackup;

/**
 * This interface prepares and cleans up any resources necessary for the system
 * to use FileChecker interface.
 * 
 * @author JoelNeppel
 *
 */
public interface BackupPreparer
{
	/**
	 * Sets up any resources needed to prepare for the system to back up files.
	 * Returns if the set up was successful or not.
	 */
	void setUp();

	/**
	 * Cleans up any system resources that may need to be after backup has been
	 * complete.
	 */
	void tearDown();
}
