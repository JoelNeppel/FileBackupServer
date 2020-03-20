package fileBackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import exceptions.SystemErrorException;
import fileUsage.BackupItem;
import fileUsage.FileStatus;

/**
 * @author JoelNeppel
 *
 */
public class ExternalStorageBackup extends FileChecker implements BackupInitilizer
{
	/**
	 * The name for this backup method
	 */
	private String name;

	/**
	 * The path to the folder where all the files will be backed up to
	 */
	private String folderPath;

	@Override
	public void initilize(LinkedList<String> got)
	{
		Iterator<String> iter = got.iterator();
		name = iter.next();
		folderPath = iter.next().trim();
	}

	@Override
	public boolean checkSystemReady()
	{
		System.out.println("Check ready " + toString());
		return new File(folderPath).isDirectory();
	}

	@Override
	public boolean createDirectory(BackupItem head, File directory) throws InterruptedException, SystemErrorException
	{
		File dir = new File(folderPath + head.getPathToSend(directory));
		System.out.println("Create directory " + dir + " using " + toString());
		return dir.exists() || dir.mkdir();
	}

	@Override
	public FileStatus getStatus(BackupItem head, File check) throws InterruptedException, SystemErrorException
	{
		System.out.println("Status of " + check + toString());
		File other = new File(folderPath + head.getPathToSend(check));
		if(other.exists())
		{
			if(other.lastModified() > check.lastModified())
			{
				return FileStatus.NEW_VERSION;
			}
			else if(other.lastModified() < check.lastModified())
			{
				return FileStatus.OLD_VERSION;
			}
			else
			{
				return FileStatus.SAME_VERSION;
			}
		}
		else
		{
			return FileStatus.NOT_FOUND;
		}
	}

	@Override
	public boolean getUpdatedFile(BackupItem head, File receive) throws InterruptedException, SystemErrorException
	{
		System.out.println("Receive " + receive + " using " + toString());
		return copy(new File(folderPath + head.getPathToSend(receive)), receive);
	}

	@Override
	public boolean sendUpdatedFile(BackupItem head, File send) throws InterruptedException, SystemErrorException
	{
		System.out.println("Send " + send + " using " + toString());
		return copy(send, new File(folderPath + head.getPathToSend(send)));
	}

	/**
	 * Copies the contents of the original file to the copyTo file and sets the date
	 * last modified to the time the original was last modified. Returns whether the
	 * copy was successful or not.
	 * @param original
	 *     The original file to copy
	 * @param copyTo
	 *     The file that will be deleted and copied to
	 * @return True if the copy was successful, false otherwise
	 */
	private boolean copy(File original, File copyTo)
	{
		copyTo.delete();
		FileInputStream in = null;
		FileOutputStream out = null;
		boolean success = false;
		try
		{
			in = new FileInputStream(original);
			out = new FileOutputStream(copyTo);
			long bytesLeft = original.length();
			// Array of arbitrarily large size
			byte[] data = new byte[Integer.MAX_VALUE / 2];
			while(bytesLeft > 0)
			{
				int read = in.read(data);
				out.write(data, 0, read);
				bytesLeft -= read;
			}
			copyTo.setLastModified(original.lastModified());
			success = true;
		}
		catch(Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(null != in)
				{
					in.close();
				}
				if(null != out)
				{
					out.close();
				}
			}
			catch(IOException e)
			{
			}
		}

		return success;
	}

	@Override
	public boolean getMissing(BackupItem check) throws InterruptedException, SystemErrorException
	{
		System.out.println("Missing " + check + toString());
		missingRecursive(check, new File(folderPath + check.getPathToSend()));
		return false;
	}

	/**
	 * Checks for missing files recursively.
	 * @param head
	 *     The head backup item used to convert between
	 * @param onBackup
	 *     The file on the backup drive being check if it is missing
	 */
	private void missingRecursive(BackupItem head, File onBackup)
	{
		File check = new File(head.getFullPath(onBackup.getAbsolutePath().replace(folderPath, "")));
		if(!onBackup.isHidden() && !check.exists())
		{
			if(onBackup.isDirectory() && null != onBackup.list())
			{
				check.mkdir();
			}
			else if(onBackup.isFile())
			{
				copy(onBackup, check);
			}
		}

		if(onBackup.isDirectory())
		{
			for(File f : onBackup.listFiles())
			{
				missingRecursive(head, f);
			}
		}
	}

	@Override
	public String toString()
	{
		return "EB " + name + " " + folderPath;
	}

	@Override
	public LinkedList<String> output()
	{
		LinkedList<String> list = new LinkedList<String>();
		list.add(name);
		list.add(folderPath);
		return list;
	}
}