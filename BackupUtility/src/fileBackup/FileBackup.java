package fileBackup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import exceptions.SystemErrorException;
import fileUsage.BackupItem;
import fileUsage.FileStatus;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import lists.SinglyLinkedList;
import networkBackup.NetworkBackup;

/**
 * @author JoelNeppel
 *
 */
public class FileBackup extends Application
{
	/**
	 * List of backup methods that will be used
	 */
	private static SinglyLinkedList<FileChecker> backups;

	/**
	 * Contains list of files to backup and handles relative path modifications
	 */
	private static ObservableList<BackupItem> files;

	private static Thread backupThread;

	private static Text itemInfo;

	private static Text backupMethodInfo;

	private static Text fileInfo;

	private static Text overallStatus;

	private static String curItemInfo;

	private static String curBackupMethodInfo;

	private static String curFileInfo;

	private static String curStatus;

	private static boolean runStatusUpdates;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		new Thread(()->
		{
			Application.launch();
			System.exit(0);
		}).start();

		backups = getBackups(new File("BackupsList.txt"));
		System.out.println(backups);
		files = getBackupItems(new File("BackupItemsList.txt"));
		System.out.println(files);
		backupThread = null;
		// TODO read backup methods to use from file
	}

	private static SinglyLinkedList<FileChecker> getBackups(File read)
	{
		SinglyLinkedList<FileChecker> list = new SinglyLinkedList<>();

		// Map for potential FileCheckers, any future addition will need to be added
		// here
		HashMap<String, Class<? extends FileChecker>> map = new HashMap<>();
		map.put(NetworkBackup.class.getSimpleName(), NetworkBackup.class);
		map.put(ExternalStorageBackup.class.getSimpleName(), ExternalStorageBackup.class);

		Scanner fileScan;
		try
		{
			fileScan = new Scanner(read);
		}
		catch(FileNotFoundException e)
		{
			return list;
		}

		while(fileScan.hasNextLine())
		{
			Scanner lineScan = new Scanner(fileScan.nextLine());
			lineScan.useDelimiter(":>");
			Class<? extends FileChecker> got = map.get(lineScan.next().trim());
			if(null != got)
			{
				try
				{
					FileChecker toAdd = got.newInstance();
					if(toAdd instanceof BackupInitilizer)
					{
						LinkedList<String> settings = new LinkedList<>();
						while(lineScan.hasNext())
						{
							settings.add(lineScan.next().trim());
						}

						((BackupInitilizer) toAdd).initilize(settings);
					}
					list.add(toAdd);
				}
				catch(InstantiationException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch(IllegalAccessException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				System.out.println("Unknown backup method");
			}
			lineScan.close();
		}

		if(null != fileScan)
		{
			fileScan.close();
		}

		return list;
	}

	private static ObservableList<BackupItem> getBackupItems(File read)
	{
		ObservableList<BackupItem> list = FXCollections.observableArrayList();

		if(read.exists() && read.canRead())
		{
			Scanner scan = null;
			try
			{
				scan = new Scanner(read);
				scan.useDelimiter(":>");

				while(scan.hasNextLine())
				{
					Scanner lineScan = null;
					try
					{
						lineScan = new Scanner(scan.nextLine());
						lineScan.useDelimiter(":>");

						String path = lineScan.next().trim();
						BackupItem.BackupAction action = BackupItem.BackupAction.getFromString(lineScan.next().trim());

						if(lineScan.hasNext())
						{
							// Backup item has custom location on backup location
							String newPath = lineScan.next().trim();
							list.add(new BackupItem(path, action, newPath));
						}
						else
						{
							list.add(new BackupItem(path, action));
						}
					}
					catch(FileNotFoundException e)
					{
						System.out.println(e.getMessage());
					}
					catch(NoSuchElementException e)
					{
						System.out.println("Backup item list not in expected format.");
					}
					finally
					{
						if(null != lineScan)
						{
							lineScan.close();
						}
					}
				}
			}
			catch(FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				if(null != scan)
				{
					scan.close();
				}
			}
		}

		return list;
	}

	private static void backup()
	{
		// Only allow one backup to be active at a time
		if(null != backupThread)
		{
			return;
		}

		// Make sure status is empty
		curItemInfo = "";
		curBackupMethodInfo = "";
		curFileInfo = "";
		curStatus = "";

		// Begin displaying status
		runStatusUpdates = true;
		beginStatusUpdates();

		backupThread = new Thread(()->
		{
			System.out.println("Beginning backup");
			try
			{
				// Set up any backup that needs it and check if they are ready. If not ready, it
				// it will be removed
				curStatus = "Getting Ready...";
				for(FileChecker checker : backups)
				{
					if(checker instanceof BackupPreparer)
					{
						((BackupPreparer) checker).setUp();
						// TODO report removal
					}

					boolean ready = checker.checkSystemReady();
					if(!ready)
					{
						System.out.println("Removed " + checker);
						backups.remove(checker);
					}
				}

				// TODO
				curStatus = "Backing up files...";
				for(FileChecker checker : backups)
				{
					curBackupMethodInfo = checker.toString();
					for(BackupItem item : files)
					{
						curItemInfo = item.getPathToSend();
						try
						{
							backupFile(item.getFile(), item, checker);
							if(item.getAction().shouldPullMissing())
							{
								curFileInfo = "Getting Missing";
								checker.getMissing(item);
							}
						}
						catch(SystemErrorException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			catch(InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				curStatus = "Cleaning up...";
				for(FileChecker checker : backups)
				{
					if(checker instanceof BackupPreparer)
					{
						((BackupPreparer) checker).tearDown();
					}
				}

				backupThread = null;
			}

			System.out.println("Finished Backup");

			// Clear status updates
			curBackupMethodInfo = "";
			curFileInfo = "";
			curItemInfo = "";
			curStatus = "";
			runStatusUpdates = false;
			// Ensure text display is empty
			backupMethodInfo.setText("");
			fileInfo.setText("");
			itemInfo.setText("");
			overallStatus.setText("Finished");
		});

		backupThread.start();
	}

	private static void backupFile(File file, BackupItem head, FileChecker backuper) throws InterruptedException, SystemErrorException
	{
		curFileInfo = file.getAbsolutePath();
		System.out.println("Backing up " + file);
		// Do not backup hidden files or abnormal files
		if(file.isHidden() || (!file.isFile() && !file.isDirectory()))
		{
			return;
		}

		if(file.isDirectory())
		{
			if(null == file.list())
			{
				// Do not backup any unusual directories
				return;
			}

			boolean successful = backuper.createDirectory(head, file);
			if(!successful)
			{
				// Return if the file could not be created on the backup
				// TODO report fail
				return;
			}

			for(File f : file.listFiles())
			{
				backupFile(f, head, backuper);
			}
		}
		else
		{
			FileStatus status = backuper.getStatus(head, file);
			if(FileStatus.NEW_VERSION == status)
			{
				if(head.getAction().shouldPullMostRecent())
				{
					boolean success;
					try
					{
						// Receive most recent version if action requires pull
						success = backuper.getUpdatedFile(head, file);
					}
					catch(Exception e)
					{
						success = false;
					}

					if(!success)
					{

					}
					else
					{

					}
				}
			}
			else if((FileStatus.OLD_VERSION == status && head.getAction().shouldPushMostRecent()) || (FileStatus.NOT_FOUND == status && head.getAction().shouldPushMissing()))
			{
				// Send most recent version if:
				// host is out dated and action demands host has most recent
				// host is missing file and action demands push of missing
				backuper.sendUpdatedFile(head, file);
			}
		}
		// TODO failed
	}

	/*
	 * Graphic methods below here
	 */

	@SuppressWarnings("unchecked")
	@Override
	public void start(Stage primary) throws Exception
	{
		// Set up main grid
		GridPane grid = new GridPane();
		grid.setPadding(new Insets(10));
		grid.setHgap(10);
		grid.setVgap(10);
		ColumnConstraints c1 = new ColumnConstraints();
		ColumnConstraints c2 = new ColumnConstraints();
		c1.setPercentWidth(50);
		c2.setPercentWidth(50);
		grid.getColumnConstraints().addAll(c1, c2);

		RowConstraints r1 = new RowConstraints();
		RowConstraints r2 = new RowConstraints();
		RowConstraints r3 = new RowConstraints();
		RowConstraints r4 = new RowConstraints();
		r1.setPercentHeight(5);
		r2.setPercentHeight(70);
		r3.setPercentHeight(5);
		r4.setPercentHeight(20);
		grid.getRowConstraints().addAll(r1, r2, r3, r4);

		Scene scene = new Scene(grid, 600, 600);

		Button backupBtn = new Button("Backup");
		backupBtn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		backupBtn.setOnAction((ActionEvent a)->
		{
			backup();
		});

		// Table for items to be backed up
		TableView<BackupItem> table = new TableView<>();
		table.setItems(files);
		TableColumn<BackupItem, String> relative = new TableColumn<>("Backup Path");
		TableColumn<BackupItem, BackupItem.BackupAction> action = new TableColumn<>("Backup Action");
		TableColumn<BackupItem, String> path = new TableColumn<>("File Path");
		// Set table columns
		relative.setCellValueFactory(new Callback<CellDataFeatures<BackupItem, String>, ObservableValue<String>>()
		{
			public ObservableValue<String> call(CellDataFeatures<BackupItem, String> item)
			{
				return new SimpleStringProperty(item.getValue().getPathToSend());
			}
		});
		action.setCellValueFactory(new Callback<CellDataFeatures<BackupItem, BackupItem.BackupAction>, ObservableValue<BackupItem.BackupAction>>()
		{
			public ObservableValue<BackupItem.BackupAction> call(CellDataFeatures<BackupItem, BackupItem.BackupAction> item)
			{
				return new SimpleObjectProperty<>(item.getValue().getAction());
			}
		});
		path.setCellValueFactory(new Callback<CellDataFeatures<BackupItem, String>, ObservableValue<String>>()
		{
			public ObservableValue<String> call(CellDataFeatures<BackupItem, String> item)
			{
				return new SimpleStringProperty(item.getValue().getAbsolutePath());
			}
		});
		table.getColumns().setAll(relative, action, path);

		// Buttons that modify backup item list
		Button addBtn = new Button("Add");
		Button editBtn = new Button("Edit");
		Button removeBtn = new Button("Remove");
		addBtn.setOnAction((ActionEvent a)->
		{
			modifyItems("", primary, scene, -1);
		});
		editBtn.setOnAction((ActionEvent a)->
		{
			BackupItem select = table.getSelectionModel().getSelectedItem();
			if(null != select)
			{
				modifyItems(select.getAbsolutePath(), primary, scene, table.getSelectionModel().getSelectedIndex());
			}
		});
		removeBtn.setOnAction((ActionEvent a)->
		{
			if(table.getSelectionModel().getSelectedItem() != null)
			{
				files.remove(table.getSelectionModel().getSelectedItem());
				updateFile();
			}
		});

		// Button to add backup method
		Button addBackup = new Button("Add Backup");
		addBackup.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		// TODO Make button add backup

		// HBox for even spacing and size
		HBox btns = new HBox();
		btns.setSpacing(20);
		addBtn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		editBtn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		removeBtn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		HBox.setHgrow(addBtn, Priority.ALWAYS);
		HBox.setHgrow(editBtn, Priority.ALWAYS);
		HBox.setHgrow(removeBtn, Priority.ALWAYS);
		btns.getChildren().addAll(addBtn, editBtn, removeBtn);

		// VBox for all backup options
		VBox box = new VBox();
		for(FileChecker backup : backups)
		{
			GridPane add = backup.getGrid();
			box.getChildren().add(add);
			VBox.setVgrow(add, Priority.ALWAYS);
		}

		// GridPane for status updates
		GridPane status = new GridPane();

		status.setHgap(10);
		status.setVgap(10);
		c1 = new ColumnConstraints();
		c1.setPercentWidth(22);
		c2 = new ColumnConstraints();
		c2.setPercentWidth(25);
		ColumnConstraints c3 = new ColumnConstraints();
		c3.setPercentWidth(53);
		status.getColumnConstraints().addAll(c1, c2, c3);
		r1 = new RowConstraints();
		r1.setPercentHeight(20);
		r2 = new RowConstraints();
		r2.setPercentHeight(20);
		r3 = new RowConstraints();
		r3.setPercentHeight(60);
		status.getRowConstraints().addAll(r1, r2, r3);

		// Text to be added
		Text working = new Text("Working on:");
		Text using = new Text("Using backup method:");
		Text backingUp = new Text("Backing up file:");
		itemInfo = new Text();
		backupMethodInfo = new Text();
		fileInfo = new Text();
		overallStatus = new Text("Waiting...");
		// Set wrapping
		// TODO
		fileInfo.wrappingWidthProperty().bind(status.widthProperty().divide(2));
		status.add(overallStatus, 0, 0);
		status.add(working, 0, 1);
		status.add(using, 0, 2);
		status.add(itemInfo, 1, 1);
		status.add(backupMethodInfo, 1, 2);
		status.add(backingUp, 2, 1);
		status.add(fileInfo, 2, 2);
		GridPane.setValignment(overallStatus, VPos.TOP);
		GridPane.setValignment(working, VPos.TOP);
		GridPane.setValignment(using, VPos.TOP);
		GridPane.setValignment(itemInfo, VPos.TOP);
		GridPane.setValignment(backupMethodInfo, VPos.TOP);
		GridPane.setValignment(backingUp, VPos.TOP);
		GridPane.setValignment(fileInfo, VPos.TOP);

		// Add items to grid
		grid.add(backupBtn, 0, 0);
		grid.add(table, 0, 1);
		grid.add(btns, 0, 2);
		grid.add(box, 1, 1);
		grid.add(addBackup, 1, 2);
		grid.add(status, 0, 3);

		GridPane.setColumnSpan(status, 2);

		// Set up window
		primary.setTitle("Backup Utility");
		primary.setScene(scene);
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

		// Alternate host location
		// TODO edit alternate location

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
						files.add(new BackupItem(path.getText().trim(), select.getSelectionModel().getSelectedItem()));
					}
					else
					{
						// Replace item at given index with the new one with set properties
						files.set(index, new BackupItem(path.getText().trim(), select.getSelectionModel().getSelectedItem()));
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
		File f = new File("BackupItemsList.txt");
		f.delete();
		try
		{
			f.createNewFile();
			PrintWriter out = new PrintWriter(f);
			for(BackupItem item : files)
			{
				out.write(item.getAbsolutePath() + ":>");
				out.write(item.getAction().toString() + ":>");
				if(!item.getFile().getName().equals(item.getPathToSend()))
				{
					out.write(item.getPathToSend() + ":>");
				}
				out.write('\n');
				out.flush();
			}
			out.close();
		}
		catch(IOException e)
		{
		}
	}

	private static void beginStatusUpdates()
	{
		new Thread(()->
		{
			while(runStatusUpdates)
			{
				itemInfo.setText(curItemInfo);
				backupMethodInfo.setText(curBackupMethodInfo);
				fileInfo.setText(curFileInfo);
				overallStatus.setText(curStatus);
				try
				{
					// Only update 20 times every second
					Thread.sleep(50);
				}
				catch(InterruptedException e)
				{
					// Prevent too many updates each second
					return;
				}
			}
		}).start();
	}
}