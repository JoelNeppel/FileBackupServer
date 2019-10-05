package lists;

import java.util.Iterator;

/**
 * This is a very basic singly linked list intended only for adding an unknown
 * amount of data and iterating through the whole list.
 *
 * @author JoelNeppel
 *
 * @param <E>
 *     The data that will be stored in this list
 *
 */
public class SinglyLinkedList<E> implements Iterable<E>
{
	/**
	 * The node that will hold the data and the next element.
	 *
	 * @author JoelNeppel
	 *
	 */
	private class Node
	{
		/**
		 * The next element in the list
		 */
		private Node next;

		/**
		 * The data stored in the node
		 */
		private E data;

		/**
		 * Creates a node with the given data
		 * @param data
		 *     The data to be stored
		 */
		private Node(E data)
		{
			this.data = data;
		}
	}

	/**
	 * The beginning of the list
	 */
	private Node head;

	/**
	 * The end of the list
	 */
	private Node tail;

	/**
	 * The number of elements in the list
	 */
	private int size;

	/**
	 * Creates an empty linked list
	 */
	public SinglyLinkedList()
	{
		size = 0;
	}

	/**
	 * Returns how many elements are in the list.
	 * @return The number of elements in the list
	 */
	public int getSize()
	{
		return size;
	}

	/**
	 * Returns whether the list is empty or not.
	 * @return True if empty, false otherwise
	 */
	public boolean isEmpty()
	{
		return 0 == size;
	}

	/**
	 * Adds an element to the list. Must not be null.
	 * @param data
	 *     The data to add
	 * @throws IllegalArgumentException
	 *     If the data entered is null
	 */
	public void add(E data) throws IllegalArgumentException
	{
		if(data == null)
		{
			throw new IllegalArgumentException("Entered data must not be null.");
		}

		Node add = new Node(data);

		if(head == null)
		{
			head = add;
			tail = add;
		}
		else
		{
			tail.next = add;
			tail = add;
		}

		size++;
	}

	/**
	 * Removes the given data from the list if it exists.
	 * @param data
	 *     The data to remove
	 * @return True if the data was removed, false not found
	 */
	public boolean remove(E data)
	{
		if(isEmpty())
		{
			return false;
		}

		if(head.data.equals(data))
		{
			head = head.next;
			size--;
			return true;
		}

		Node cur = head;
		while(cur.next != null && !cur.next.data.equals(data))
		{
			cur = cur.next;
		}

		if(cur == tail)
		{
			tail = cur;
			cur.next = null;
		}
		else
		{
			cur.next = cur.next.next;
			size--;
			return true;
		}

		return false;
	}

	@Override
	public Iterator<E> iterator()
	{
		return new SinglyLinkedIterator();
	}

	/**
	 * Iterator class for the singly linked list. Contains three nodes for quick
	 * removal.
	 *
	 * @author JoelNeppel
	 *
	 */
	private class SinglyLinkedIterator implements Iterator<E>
	{
		/**
		 * The node whose data will be returned with a call next()
		 */
		private Node next;

		/**
		 * The node whose data was just returned and is pending for removal
		 */
		private Node pending;

		/**
		 * Node that helps remove pending
		 */
		private Node help;

		/**
		 * Creates an iterator starting at the head.
		 */
		public SinglyLinkedIterator()
		{
			next = head;
		}

		@Override
		public boolean hasNext()
		{
			return next != null;
		}

		@Override
		public E next()
		{
			if(!hasNext())
			{
				throw new IllegalArgumentException("There are no more elements in this list");
			}

			help = pending;
			pending = next;
			next = next.next;
			return pending.data;
		}

		@Override
		public void remove()
		{
			if(pending == null)
			{
				throw new IllegalStateException();
			}

			if(help == null)
			{
				head = next;
			}
			else
			{
				help.next = next;
			}

			size--;

			pending = null;
		}
	}
}