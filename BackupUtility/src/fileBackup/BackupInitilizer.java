package fileBackup;

import java.util.LinkedList;

/**
 * @author JoelNeppel
 *
 */
public interface BackupInitilizer
{
	void initilize(LinkedList<String> got);

	LinkedList<String> output();
}