package fileBackup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;

import fileUsage.BackupItem;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * @author JoelNeppel
 *
 */
public class ClientGraphic extends Application
{
	/**
	 * The button to back up the items
	 */
	private static Button backupBtn = new Button("Backup now");

	private static Text action = new Text();

	private static Text file = new Text();

	private static String actionText;

	private static String fileText;

	/**
	 * The connected host information
	 */
	private static Text hostInfo = new Text("Disconnected");

	@SuppressWarnings("unchecked")
	@Override
	public void start(Stage primary) throws Exception
	{
		GridPane grid = new GridPane();
		grid.setHgap(20);
		grid.setVgap(20);
		grid.setPadding(new Insets(10));
		ColumnConstraints c1 = new ColumnConstraints();
		ColumnConstraints c2 = new ColumnConstraints();
		c1.setPercentWidth(60);
		c2.setPercentWidth(40);
		grid.getColumnConstraints().addAll(c1, c2);

		Scene s = new Scene(grid, 570, 500);

		Text address = new Text(InetAddress.getLocalHost().getHostAddress());
		Text name = new Text(InetAddress.getLocalHost().getHostName());
		GridPane.setHalignment(name, HPos.RIGHT);
		grid.add(address, 0, 2);
		grid.add(name, 1, 2);

		// Left side items
		TableView<BackupItem> table = new TableView<>();
		table.setItems(ClientApp.getItems());
		TableColumn<BackupItem, String> path = new TableColumn<>("File Path");
		TableColumn<BackupItem, BackupItem.BackupAction> action = new TableColumn<>("Backup Action");
		action.setMinWidth(100);
		action.setPrefWidth(125);
		action.setMaxWidth(150);
		path.setCellValueFactory(new Callback<CellDataFeatures<BackupItem, String>, ObservableValue<String>>()
		{
			public ObservableValue<String> call(CellDataFeatures<BackupItem, String> p)
			{
				return new SimpleStringProperty(p.getValue().getFullPath());
			}
		});
		action.setCellValueFactory(
				new Callback<CellDataFeatures<BackupItem, BackupItem.BackupAction>, ObservableValue<BackupItem.BackupAction>>()
				{
					public ObservableValue<BackupItem.BackupAction> call(
							CellDataFeatures<BackupItem, BackupItem.BackupAction> p)
					{
						return new SimpleObjectProperty<>(p.getValue().getAction());
					}
				});

		table.getColumns().setAll(path, action);

		Button addBtn = new Button("Add");
		Button editBtn = new Button("Edit");
		Button removeBtn = new Button("Remove");
		addBtn.setOnAction((ActionEvent a)->
		{
			modifyItems("", primary, s, -1);
		});
		editBtn.setOnAction((ActionEvent a)->
		{
			BackupItem select = table.getSelectionModel().getSelectedItem();
			if(null != select)
			{
				modifyItems(select.getFullPath(), primary, s, table.getSelectionModel().getSelectedIndex());
			}
		});
		removeBtn.setOnAction((ActionEvent a)->
		{
			if(table.getSelectionModel().getSelectedItem() != null)
			{
				ClientApp.getItems().remove(table.getSelectionModel().getSelectedItem());
			}
		});
		addBtn.setMaxWidth(Double.MAX_VALUE);
		editBtn.setMaxWidth(Double.MAX_VALUE);
		removeBtn.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(addBtn, Priority.ALWAYS);
		HBox.setHgrow(editBtn, Priority.ALWAYS);
		HBox.setHgrow(removeBtn, Priority.ALWAYS);
		HBox btns = new HBox();
		btns.setSpacing(20);
		btns.getChildren().addAll(addBtn, editBtn, removeBtn);
		grid.add(btns, 0, 1);
		grid.add(table, 0, 0);

		// Right side items
		backupBtn.setMaxWidth(Double.MAX_VALUE);
		backupBtn.setMaxHeight(Double.MAX_VALUE);
		backupBtn.setOnAction((ActionEvent a)->
		{
			new Thread(()->
			{
				Thread updates = new Thread(ClientGraphic::setInfo);
				updates.start();
				ClientApp.backup();
				updates.interrupt();
			}).start();
		});
		GridPane.setHgrow(backupBtn, Priority.SOMETIMES);
		GridPane.setVgrow(backupBtn, Priority.ALWAYS);
		GridPane.setValignment(backupBtn, VPos.TOP);
		VBox details = new VBox(ClientGraphic.action, file);
		file.setWrappingWidth(275);
		details.setAlignment(Pos.TOP_LEFT);
		GridPane.setValignment(details, VPos.CENTER);
		GridPane.setValignment(hostInfo, VPos.CENTER);
		GridPane rSide = new GridPane();
		RowConstraints r1 = new RowConstraints();
		RowConstraints r2 = new RowConstraints();
		RowConstraints r3 = new RowConstraints();
		r1.setPercentHeight(20);
		r2.setPercentHeight(60);
		r3.setPercentHeight(20);
		rSide.getRowConstraints().addAll(r1, r2, r3);
		rSide.setVgap(50);
		rSide.add(backupBtn, 0, 0);
		rSide.add(details, 0, 1);
		rSide.add(hostInfo, 0, 2);
		rSide.resize(table.getWidth(), table.getHeight());
		grid.add(rSide, 1, 0);

		// Set window settings
		primary.setTitle("Client Back-up Utility");
		primary.setScene(s);
		primary.show();
	}

	/**
	 * Modifies the item at the given index using the user selection.
	 * @param defaultPath
	 *     The current path to use for the item
	 * @param stage
	 *     The stage to use to display
	 * @param home
	 *     The previous scene to be displayed after the selection
	 * @param index
	 */
	private static void modifyItems(String defaultPath, Stage stage, Scene home, int index)
	{
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(10));
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(20);
		grid.setVgap(20);

		// Backup action selector
		ComboBox<BackupItem.BackupAction> select = new ComboBox<>();
		select.getItems().addAll(BackupItem.BackupAction.values());
		grid.add(select, 1, 0);

		// File path display
		Text pathText = new Text("File path:");
		TextField path = new TextField();
		path.setText(defaultPath);
		grid.add(pathText, 0, 1);
		grid.add(path, 1, 1);

		// Choose file or folder button
		Button fileBtn = new Button("Choose File");
		Button dirBtn = new Button("Choose Folder");
		fileBtn.setOnAction((ActionEvent a)->
		{
			FileChooser fc = new FileChooser();
			File f = fc.showOpenDialog(stage);
			if(null != f)
			{
				path.setText(f.getPath());
			}
		});
		dirBtn.setOnAction((ActionEvent a)->
		{
			DirectoryChooser dc = new DirectoryChooser();
			File f = dc.showDialog(stage);
			if(null != f)
			{
				path.setText(f.getPath());
			}
		});
		grid.add(fileBtn, 0, 2);
		grid.add(dirBtn, 1, 2);

		// Buttons to accept or cancel changes
		Button okBtn = new Button("Ok");
		Button cancelBtn = new Button("Cancel");
		okBtn.setOnAction((ActionEvent a)->
		{
			// Modify items or discard changes if not all properties set
			if(!new File(path.getText().trim()).exists())
			{
				// Don't allow files that don't exist
				reportError("The file " + path.getText() + " does not exist.");
			}
			else if(null == select.getSelectionModel().getSelectedItem())
			{
				// There needs to be a backup item action
				reportError("No back-up action was selected.");
			}
			else
			{
				try
				{
					if(-1 == index)
					{
						// Add to path if index -1
						ClientApp.getItems().add(
								new BackupItem(path.getText().trim(), select.getSelectionModel().getSelectedItem()));
					}
					else
					{
						// Replace item at given index with the new one with set properties
						ClientApp.getItems().set(index,
								new BackupItem(path.getText().trim(), select.getSelectionModel().getSelectedItem()));
					}
					updateFile();
				}
				catch(FileNotFoundException e)
				{
				}
			}
			// Return to home scene
			stage.setScene(home);
			stage.centerOnScreen();
		});
		cancelBtn.setOnAction((ActionEvent a)->
		{
			// Discard changes go back to home scene
			stage.setScene(home);
		});
		grid.add(okBtn, 0, 3);
		grid.add(cancelBtn, 1, 3);

		Scene scene = new Scene(grid, 300, 300);
		stage.setScene(scene);
		stage.centerOnScreen();
	}

	/**
	 * Creates a new window to display an error message.
	 * @param message
	 *     The message to display
	 */
	private static void reportError(String message)
	{
		Stage stage = new Stage();
		GridPane grid = new GridPane();
		Text report = new Text(message);
		GridPane.setHalignment(report, HPos.CENTER);
		grid.add(report, 0, 0);
		grid.setPadding(new Insets(10, 10, 10, 10));
		Scene scene = new Scene(grid);
		stage.setScene(scene);
		stage.setResizable(false);
		stage.setAlwaysOnTop(true);
		stage.show();
	}

	/**
	 * Updates the file containing the list of files and backup actions.
	 */
	private static void updateFile()
	{
		File f = new File("ClientItemsList.txt");
		f.delete();
		try
		{
			f.createNewFile();
			PrintWriter out = new PrintWriter(f);
			for(BackupItem item : ClientApp.getItems())
			{
				out.write(item.toString() + "\n");
				out.flush();
			}
			out.close();
		}
		catch(IOException e)
		{
		}
	}

	/**
	 * Sets the string to be displayed as the host info.
	 * @param info
	 *     The string to be displayed
	 */
	public static void setHostInfo(String info)
	{
		hostInfo.setText(info);
	}

	/**
	 * Sets the file path and the action being performed.
	 * @param info
	 *     The file path
	 * @param path
	 *     The action being performed
	 */
	public static void setFileAction(String info, String path)
	{
		setAction(info);
		setFile(path);
	}

	/**
	 * Sets the text for the action being performed.
	 * @param info
	 *     The action text
	 */
	public static void setAction(String info)
	{
		actionText = info;
	}

	/**
	 * Sets the path for the file being worked on.
	 * @param path
	 *     The of the file
	 */
	public static void setFile(String path)
	{
		fileText = path;
	}

	/**
	 * Sets the text for the detailed info at periodic intervals.
	 */
	private static void setInfo()
	{
		try
		{
			while(true)
			{
				action.setText(actionText);
				file.setText(fileText);
				Thread.sleep(50);
			}
		}
		catch(InterruptedException e)
		{
			action.setText(actionText);
			file.setText(fileText);
		}
	}
}