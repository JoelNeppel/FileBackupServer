package fileUsage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;

import exceptions.ItemNotFoundException;

/**
 * Contains a hash map for the settings read from the given file. The scanner is
 * using the delimiter ":>" so each key and value must be separated by ":>" It
 * is recommended that each line be one key and its value.
 * 
 * @author JoelNeppel
 *
 */
public class SystemFileReader
{
	/**
	 * The map of the keys and values from the read file
	 */
	private HashMap<String, String> items;

	/**
	 * Creates a new list of keys and items read from the given file. Each element
	 * must be in the format: "key:>value:>" White space directly before ":>" will
	 * be ignored, recommended to have each key on a separate line.
	 * @param fileName
	 *     The name of the file to be read from
	 * @throws FileNotFoundException
	 *     If the file with the given name does not exist
	 * @throws InputMismatchException
	 *     If the file is not in the correct format
	 */
	public SystemFileReader(String fileName) throws FileNotFoundException, InputMismatchException
	{
		items = new HashMap<>();
		Scanner scan = new Scanner(new File(fileName));
		scan.useDelimiter(":>");

		while(scan.hasNext())
		{
			try
			{
				items.put(scan.next().trim(), scan.next().trim());
			}
			catch(NoSuchElementException e)
			{
				scan.close();
				throw new InputMismatchException("Expected file in format key:>Value:>");
			}
		}

		scan.close();
	}

	/**
	 * Returns the value of the given key or null if there is no value with the
	 * given key.
	 * @param key
	 *     The key for the desired value
	 * @return The value for the specified key or null if there isn't one
	 * @throws ItemNotFoundException
	 *     If the map does not contain a value for the given key
	 */
	public String get(String key) throws ItemNotFoundException
	{
		String got = items.get(key);

		if(null == got)
		{
			throw new ItemNotFoundException("The key " + key + " was not found in the map.");
		}

		return got;
	}

	@Override
	public String toString()
	{
		return items.toString();
	}
}