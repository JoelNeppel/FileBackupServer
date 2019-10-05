package fileBackup;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * @author JoelNeppel
 *
 */
public class HostGraphic extends Application
{
	/**
	 * The text for the string representation of the port
	 */
	private static Text portString;

	/**
	 * The 2D array for all the client text and their information
	 */
	private static Text[][] clients = setUp();

	private static long lastUpdate = 0;

	@Override
	public void start(Stage primary)
	{
		// Set up grid
		GridPane grid = new GridPane();
		grid.setMaxSize(800, 500);
		grid.setAlignment(Pos.CENTER);
		grid.setGridLinesVisible(true);
		grid.setManaged(true);
		grid.setHgap(6);

		// Set column constraints
		ColumnConstraints c1 = new ColumnConstraints(120, 120, 120);
		ColumnConstraints c2 = new ColumnConstraints(508, 508, 508);
		ColumnConstraints c3 = new ColumnConstraints(160, 160, 160);
		grid.getColumnConstraints().addAll(c1, c2, c3);

		// Set row constraints
		RowConstraints[] rcs = new RowConstraints[10];
		for(int i = 0; i < rcs.length; i++)
		{
			rcs[i] = new RowConstraints();
			if(i == rcs.length - 1)
			{
				rcs[i].setPercentHeight(14.5);
			}
			else
			{
				rcs[i].setPercentHeight(9.5);
			}
			grid.getRowConstraints().add(rcs[i]);
		}

		try
		{
			// Set up host info
			Font hostInfo = new Font("Courier New Bold", 15);
			Text address = new Text(InetAddress.getLocalHost().getHostAddress());
			Text name = new Text(InetAddress.getLocalHost().getHostName());
			if(null == portString)
			{
				portString = new Text("" + 0);
			}
			address.setFont(hostInfo);
			name.setFont(hostInfo);
			portString.setFont(hostInfo);
			address.setFill(Color.WHITE);
			name.setFill(Color.WHITE);
			portString.setFill(Color.WHITE);
			GridPane.setHalignment(address, HPos.CENTER);
			GridPane.setHalignment(name, HPos.CENTER);
			GridPane.setHalignment(portString, HPos.CENTER);
			grid.add(address, 0, 9);
			grid.add(name, 1, 9);
			grid.add(portString, 2, 9);
		}
		catch(UnknownHostException e)
		{
		}

		Text title = new Text("Connected Clients");
		title.setFill(Color.WHITE);
		GridPane.setHalignment(title, HPos.CENTER);
		grid.add(title, 1, 0);

		// Add all text for clients
		for(int i = 0; i < clients.length; i++)
		{
			for(int j = 0; j < clients[0].length; j++)
			{
				grid.add(clients[i][j], j, i + 1);
			}
		}

		// Set up scene
		Scene scene = new Scene(grid, 800, 500);
		scene.setFill(new Color(.15, .15, .15, 1));

		primary.setTitle("Backup App Server");
		primary.setScene(scene);
		primary.show();
	}

	/**
	 * sets the port being used by the host app.
	 * @param port
	 *     The port the host app is connected to
	 */
	public static void setPort(int port)
	{
		if(null == portString)
		{
			portString = new Text("" + port);
			portString.setFill(Color.WHITE);
		}
		else
		{
			portString.setText("" + port);
		}
	}

	/**
	 * Sets up the 2D array for Texts by creating a new, empty, white text
	 * @return Returns clients array to make compiler happy
	 */
	private static Text[][] setUp()
	{
		Font font = new Font("Courier New", 12);
		clients = new Text[8][3];
		for(int i = 0; i < clients.length; i++)
		{
			for(int j = 0; j < clients[0].length; j++)
			{
				clients[i][j] = new Text();
				clients[i][j].setFill(Color.WHITE);
				clients[i][j].setFont(font);
				if(1 == j)
				{
					clients[i][j].setWrappingWidth(508);
				}

				if(0 == j)
				{
					GridPane.setHalignment(clients[i][j], HPos.CENTER);
				}
				else
				{
					GridPane.setHalignment(clients[i][j], HPos.LEFT);
				}
			}
		}
		return clients;
	}

	/**
	 * Modifies the text for the client info.
	 * @param address
	 *     Address for the client
	 * @param name
	 *     The client name
	 * @param filePath
	 *     The file path
	 * @param action
	 *     The action being performed on the file
	 */
	public static void addClient(String address, String filePath, String action)
	{
		int row = getRow(address);

		if(row < clients.length)
		{
			Text[] r = clients[row];
			r[0].setText(address);
			r[1].setText(filePath);
			r[2].setText(action);
		}
	}

	/**
	 * Sets text for the file path and action for the given client.
	 * @param address
	 *     The client's address
	 * @param filePath
	 *     The file path being backed up
	 * @param action
	 *     The action being performed on the file
	 */
	public static void updateFileAndAction(String address, String filePath, String action)
	{
		if(System.currentTimeMillis() - lastUpdate > 100)
		{
			int row = getRow(address);

			if(row < clients.length && !clients[row][0].getText().isEmpty())
			{
				clients[row][1].setText(filePath);
				clients[row][2].setText(action);
			}

			lastUpdate = System.currentTimeMillis();
		}
	}

	/**
	 * Updates the file path being used for the client with the given address.
	 * @param address
	 *     The client's address
	 * @param filePath
	 *     The file path being worked on
	 */
	public static void updateFile(String address, String filePath)
	{
		if(System.currentTimeMillis() - lastUpdate > 100)
		{
			int row = getRow(address);

			if(row < clients.length && !clients[row][0].getText().isEmpty())
			{
				clients[row][1].setText(filePath);
			}

			lastUpdate = System.currentTimeMillis();
		}
	}

	/**
	 * Updates the action being performed for the given client.
	 * @param address
	 *     The client's address
	 * @param action
	 *     The action being performed
	 */
	public static void updateAction(String address, String action)
	{
		if(System.currentTimeMillis() - lastUpdate > 100)
		{
			int row = getRow(address);

			if(row < clients.length && !clients[row][0].getText().isEmpty())
			{
				clients[row][2].setText(action);
			}
		}
	}

	/**
	 * Removes the client from the client list.
	 * @param address
	 *     Address for the client to remove
	 */
	public static void removeClient(String address)
	{
		int row = getRow(address);

		// Remove data if it was found - text was not empty
		if(row < clients.length && !clients[row][0].getText().isEmpty())
		{
			for(int i = row; i < clients.length; i++)
			{
				if(i < clients.length - 1)
				{
					// Copy text from next row
					for(int j = 0; j < clients[row].length; j++)
					{
						clients[i][j].setText(clients[i + 1][j].getText());
					}

					if(clients[i + 1][0].getText().isEmpty())
					{
						return;
					}
				}
				else
				{
					// Set row text to empty
					for(int j = 0; j < clients[row].length; j++)
					{
						clients[row][j].setText("");
					}
				}
			}
		}
	}

	/**
	 * Returns the row that contains the given address or the first empty row
	 * meaning the address was not found.
	 * @param address
	 *     The address to search for
	 * @return The row that contains the address or first empty row
	 */
	private static int getRow(String address)
	{
		int row = 0;

		// Find row with data to remove
		while(row < clients.length && !clients[row][0].getText().isEmpty()
				&& !clients[row][0].getText().equals(address))
		{
			row++;
		}
		return row;
	}
}