package fileBackup;

import java.io.File;

import exceptions.SystemErrorException;
import fileUsage.BackupItem;
import fileUsage.FileStatus;

/**
 * @author JoelNeppel
 *
 */
public class ExternalStorageBackup extends FileChecker
{

	@Override
	public boolean checkSystemReady()
	{
		System.out.println("Check ready " + toString());
		return false;
	}

	@Override
	public boolean createDirectory(BackupItem head, File directory) throws InterruptedException, SystemErrorException
	{
		System.out.println("Create directory " + directory + toString());
		return false;
	}

	@Override
	public FileStatus getStatus(BackupItem head, File check) throws InterruptedException, SystemErrorException
	{
		System.out.println("Status of " + check + toString());
		return null;
	}

	@Override
	public boolean getUpdatedFile(BackupItem head, File receive) throws InterruptedException, SystemErrorException
	{
		System.out.println("Receive " + receive + toString());
		return false;
	}

	@Override
	public boolean sendUpdatedFile(BackupItem head, File send) throws InterruptedException, SystemErrorException
	{
		System.out.println("Send " + send + toString());
		return false;
	}

	@Override
	public boolean getMissing(BackupItem check) throws InterruptedException, SystemErrorException
	{
		System.out.println("Missing " + check + toString());
		return false;
	}

	@Override
	public String toString()
	{
		return "External Backup";
	}
}