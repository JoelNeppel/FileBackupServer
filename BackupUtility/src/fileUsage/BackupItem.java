package fileUsage;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * This class stores the information for items that will be backed up. It
 * includes information such as read or write only and the folder or file
 * location.
 *
 * @author JoelNeppel
 *
 */
public class BackupItem
{
	/**
	 * The actions that can be performed on files
	 */
	public enum BackupAction
	{
		ALL(true, true, true, true),
		PUSH_ONLY(true, false, true, false),
		PULL_ONLY(false, true, false, true),
		MISSING_ONLY(false, false, true, true),
		CONTENTS_ONLY(true, true, false, false);

		/**
		 * True if the option should push the most recent version to host, false if not
		 */
		private boolean pushMostRecent;

		/**
		 * True if the option should pull the most version from host, false if not
		 */
		private boolean pullMostRecent;

		/**
		 * True to push any missing items to the host, false if not
		 */
		private boolean pushMissing;

		/**
		 * True to pull any missing items from the host, false if not
		 */
		private boolean pullMissing;

		/**
		 * Creates an option using the given options that can be performed.
		 * @param pushMostRecent
		 *     True to push the most recent version to host
		 * @param pullMostRecent
		 *     True to pull the most recent version from host
		 * @param pushMissing
		 *     True to push any missing items to host
		 * @param pullMissing
		 *     True to pull any missing items from host
		 */
		private BackupAction(boolean pushMostRecent, boolean pullMostRecent, boolean pushMissing, boolean pullMissing)
		{
			this.pushMostRecent = pushMostRecent;
			this.pullMostRecent = pullMostRecent;
			this.pushMissing = pushMissing;
			this.pullMissing = pullMissing;
		}

		/**
		 * Returns the value of pushMostRecent
		 * @return the pushMostRecent
		 */
		public boolean shouldPushMostRecent()
		{
			return pushMostRecent;
		}

		/**
		 * Returns the value of pullMostRecent
		 * @return the pullMostRecent
		 */
		public boolean shouldPullMostRecent()
		{
			return pullMostRecent;
		}

		/**
		 * Returns the value of pushMissing
		 * @return the pushMissing
		 */
		public boolean shouldPushMissing()
		{
			return pushMissing;
		}

		/**
		 * Returns the value of pullMissing
		 * @return the pullMissing
		 */
		public boolean shouldPullMissing()
		{
			return pullMissing;
		}

		/**
		 * Returns the BackupAction from a string used to create a BackupItem from a
		 * file.
		 * @param name
		 *     The string representation of the BackupAction
		 * @return The BackupAction represented by the string or null if there is no
		 *     correspondence
		 */
		public static BackupAction getFromString(String name)
		{
			for(BackupAction ba : BackupAction.values())
			{
				if(ba.toString().equals(name))
				{
					return ba;
				}
			}

			return null;
		}
	}

	/**
	 * The file that will be backed up
	 */
	private File file;

	/**
	 * The actions to perform with the given item to be backed up
	 */
	private BackupAction action;

	/**
	 * Null to use the name of the file. If not null, it will be the relative path
	 * returned
	 */
	private String relativePath;

	/**
	 * Creates a new item to add to the list of backup items using the given path
	 * and backup action to perform on the file.
	 * @param fileLocation
	 *     The path on the hard drive for the file that will be backed up
	 * @param action
	 *     The backup actions to perform on the file
	 * @param relative
	 *     The new relative path that will be the location on the backup location
	 * @throws FileNotFoundException
	 *     If the file at the given path does not exist
	 */
	public BackupItem(String fileLocation, BackupAction action, String relative) throws FileNotFoundException
	{
		file = new File(fileLocation);
		if(!file.exists())
		{
			throw new FileNotFoundException("File with path: " + fileLocation + " does not exist.");
		}

		this.action = action;
		relativePath = relative;
	}

	public BackupItem(String fileLocation, BackupAction action) throws FileNotFoundException
	{
		this(fileLocation, action, null);
	}

	/**
	 * Returns the backup action that was selected for this item.
	 * @return The backup action
	 */
	public BackupAction getAction()
	{
		return action;
	}

	/**
	 * Returns the file object for this item.
	 * @return The item file
	 */
	public File getFile()
	{
		return file;
	}

	/**
	 * Returns the full path for this item.
	 * @return The full path for the file
	 */
	public String getAbsolutePath()
	{
		return file.getAbsolutePath();
	}

	/**
	 * The path the is specific to the machine it is on and should be ignored when
	 * accessing on the backup device.
	 * @return The path string to remove in the path on other devices
	 */
	public String getPathToRemove()
	{
		if(null == relativePath)
		{
			return file.getAbsolutePath();
		}
		else
		{
			return file.getAbsolutePath().replaceAll(file.getName(), "");
		}
	}

	/**
	 * Returns the path to be sent by removing the machine specific path.
	 * @return The string to be sent
	 */
	public String getPathToSend()
	{
		if(null != relativePath)
		{
			return relativePath;
		}
		else
		{
			return file.getAbsolutePath().replace(getPathToRemove(), "");
		}
	}

	/**
	 * Returns the path to be sent for the given file. Replaces the full path of the
	 * backup item file with the path to be sent,by default the name of the backup
	 * item file that can be overridden to be a different relative location.
	 * @param send
	 *     The file path to be converted into path to send
	 * @return The relative path of the file to be sent
	 */
	public String getPathToSend(File send)
	{
		return send.getAbsolutePath().replace(file.getAbsolutePath(), getPathToSend());
	}

	/**
	 * Converts the relative path into an absolute path that will lead to the file
	 * on the local machine.
	 * @param relative
	 *     The relative path received
	 * @return The string of the absolute path of the file
	 */
	public String getFullPath(String relative)
	{
		if(null == relativePath)
		{
			return getAbsolutePath().concat(System.getProperty("file.separator") + relative.replaceFirst(getPathToSend(), ""));
		}

		return getPathToRemove().concat(relative);
	}
}