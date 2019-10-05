package communications;

/**
 * Class for converting numbers to bytes and bytes to numbers.
 *
 * @author JoelNeppel
 *
 */
public class ByteHelp
{
	/**
	 * Don't construct static
	 */
	private ByteHelp()
	{
	}

	/**
	 * Converts a long into an array of eight bytes.
	 * @param l
	 *     The long to be converted to bytes
	 * @return The array of eight bytes that represents the long
	 */
	public static byte[] toBytes(long l)
	{
		byte[] bytes = new byte[Long.BYTES];

		for(int i = bytes.length - 1; i >= 0; i--)
		{
			bytes[i] = (byte) (l & 0xFF);
			l >>= Byte.SIZE;
		}
		return bytes;
	}

	/**
	 * Converts an int into an array of four bytes.
	 * @param num
	 *     The int to be converted to bytes
	 * @return The array of four bytes that represents the int
	 */
	public static byte[] toBytes(int num)
	{
		byte[] bytes = new byte[Integer.BYTES];

		for(int i = bytes.length - 1; i >= 0; i--)
		{
			bytes[i] = (byte) (num & 0xFF);
			num >>= Byte.SIZE;
		}

		return bytes;
	}

	/**
	 * Takes an array of bytes and converts into a long starting with the byte at
	 * index 0 as the most significant byte.
	 * @param bytes
	 *     The bytes being converted into a long
	 * @return The long created from the bytes
	 */
	public static long bytesToLong(byte[] bytes)
	{
		long l = 0;

		for(int i = 0; i < Long.BYTES; i++)
		{
			l <<= Byte.SIZE;
			l |= bytes[i] & 0xFF;
		}

		return l;
	}

	/**
	 * Takes an array of bytes and converts into an int starting with the byte at
	 * index 0 as the most significant byte.
	 * @param bytes
	 *     The bytes being converted into an int
	 * @return The int created from the bytes
	 */
	public static int bytesToInt(byte[] bytes)
	{
		int num = 0;

		for(int i = 0; i < Integer.BYTES; i++)
		{
			num <<= Byte.SIZE;
			num |= bytes[i] & 0xFF;
		}

		return num;
	}
}