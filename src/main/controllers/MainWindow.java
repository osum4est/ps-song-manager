package main.controllers;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import main.handlers.OggClip;
import main.handlers.OggHandler;
import main.other.Song;
import org.ini4j.Ini;
import org.ini4j.Profile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by osum4est on 10/5/16.
 */
public class MainWindow implements Initializable {

    @FXML private TableView mainTable;
    @FXML private TableColumn colTitle;
    @FXML private TableColumn colArtist;
    @FXML private TableColumn colAlbum;
    @FXML private TableColumn colEnclosingFolder;
    @FXML private TableColumn colInLibrary;
    @FXML private TableColumn colGenre;
    @FXML private TableColumn colYear;
    @FXML private TableColumn colBand;
    @FXML private TableColumn colGuitar;
    @FXML private TableColumn colVocals;
    @FXML private TableColumn colDrums;
    @FXML private TableColumn colBass;
    @FXML private TableColumn colKeys;

    @FXML private ChoiceBox choiceSearchType;
    @FXML private TextField textSearchText;

    @FXML private Label lblFilesListed;

    @FXML private Label lblNowPlaying;
    @FXML private Slider sldVolume;
    @FXML private Button btnStop;

    @FXML private CheckBox cbMyLibrary;
    @FXML private Button btnAddToLibrary;
    @FXML private Button btnRemoveFromLibrary;

    @FXML private TextField txtSongDir;
    @FXML private TextField txtLibraryDir;
    @FXML private Button btnSongDirBrowse;
    @FXML private Button btnLibraryDirBrowse;


    private ObservableList<String> searchTypes;

    private ObservableList<Song> songList;
    private FilteredList<Song> filteredSongList;
    private SortedList<Song> sortedSongList;

    private String songDir; // = "/Volumes/FLASH/Home/Phase Shift/Rock Band";
    private String libraryDir; // = "/Users/osum4est/Desktop/ps lib";
    private Song nowPlaying;

    private Ini settingsIni;
    private static final String settingsLocation = "settings.ini";


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            File file = new File(settingsLocation);
            if (!file.exists())
                file.createNewFile();
            settingsIni = new Ini(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadSettings();

        txtSongDir.setText(songDir);
        txtLibraryDir.setText(libraryDir);


        searchTypes = FXCollections.observableArrayList();
        searchTypes.addAll("All", "Title", "Artist", "Album", "Genre");

        colTitle.setCellValueFactory(new PropertyValueFactory<Song, String>("title"));
        colTitle.setCellFactory((p) -> new ToolTipCell());
        colArtist.setCellValueFactory(new PropertyValueFactory<Song, String>("artist"));
        colArtist.setCellFactory((p) -> new ToolTipCell());
        colAlbum.setCellValueFactory(new PropertyValueFactory<Song, String>("album"));
        colAlbum.setCellFactory((p) -> new ToolTipCell());
        colEnclosingFolder.setCellValueFactory(new PropertyValueFactory<Song, String>("enclosingFolder"));
        colEnclosingFolder.setCellFactory((p) -> new ToolTipCell());
        colInLibrary.setCellValueFactory(new PropertyValueFactory<Song, Boolean>("inLibrary"));
        colInLibrary.setCellFactory(column -> {
            return new TableCell<Song, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty || !item)
                        setText("");
                    else {
                        setText("✓");
                        setStyle("-fx-alignment: CENTER");
                    }
                }
            };
        });
        colGenre.setCellValueFactory(new PropertyValueFactory<Song, String>("genre"));
        colGenre.setCellFactory((p) -> new ToolTipCell());
        colYear.setCellValueFactory(new PropertyValueFactory<Song, String>("year"));
        colYear.setCellFactory((p) -> new ToolTipCell());
        colBand.setCellValueFactory(new PropertyValueFactory<Song, String>("band"));
        colGuitar.setCellValueFactory(new PropertyValueFactory<Song, String>("guitar"));
        colVocals.setCellValueFactory(new PropertyValueFactory<Song, String>("vocals"));
        colDrums.setCellValueFactory(new PropertyValueFactory<Song, String>("drums"));
        colBass.setCellValueFactory(new PropertyValueFactory<Song, String>("bass"));
        colKeys.setCellValueFactory(new PropertyValueFactory<Song, String>("keys"));
        mainTable.setRowFactory(new Callback<TableView<Song>, TableRow<Song>>() {
            @Override
            public TableRow call(TableView param) {
                TableRow<Song> row = new TableRow<>();
                ContextMenu contextMenu = new ContextMenu();
                MenuItem playMenuItem = new MenuItem("Play");
                playMenuItem.setOnAction(event -> {
                    Song song = row.getItem();
                    try
                    {   if (nowPlaying != null)
                        nowPlaying.stop();

                        nowPlaying = song;
                        nowPlaying.play();
                        nowPlaying.setVolume((float)sldVolume.getValue());

                        lblNowPlaying.setText("Now Playing: " + nowPlaying.getTitle());

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                });
                MenuItem stopMenuItem =new MenuItem("Stop");
                stopMenuItem.setOnAction(event -> {
                    nowPlaying.stop();
                    lblNowPlaying.setText("Now Playing:");
                });
                MenuItem addMenuItem = new MenuItem("Add to library");
                addMenuItem.setOnAction(event -> {
                    for (Song song : (ObservableList<Song>)mainTable.getSelectionModel().getSelectedItems()) {
                        song.addToLibrary(libraryDir);
                    }
                    mainTable.refresh();
                    refreshFilter(textSearchText.getText(), choiceSearchType.getSelectionModel().getSelectedIndex(), cbMyLibrary.isSelected());
                });
                MenuItem removeMenuItem = new MenuItem("Remove from library");
                removeMenuItem.setOnAction(event -> {
                    for (Song song : (ObservableList<Song>)mainTable.getSelectionModel().getSelectedItems()) {
                        song.removeFromLibrary(libraryDir);
                    }
                    mainTable.refresh();
                    refreshFilter(textSearchText.getText(), choiceSearchType.getSelectionModel().getSelectedIndex(), cbMyLibrary.isSelected());
                });

                contextMenu.getItems().add(playMenuItem);
                contextMenu.getItems().add(stopMenuItem);
                contextMenu.getItems().add(new SeparatorMenuItem());
                contextMenu.getItems().add(addMenuItem);
                contextMenu.getItems().add(removeMenuItem);
                // Set context menu on row, but use a binding to make it only show for non-empty rows:
                row.contextMenuProperty().bind(
                        Bindings.when(row.emptyProperty())
                                .then((ContextMenu)null)
                                .otherwise(contextMenu)
                );
                return row ;
            }
        });

        songList = FXCollections.observableArrayList();
        filteredSongList = new FilteredList<>(songList, p -> true);
        sortedSongList = new SortedList<Song>(filteredSongList);
        sortedSongList.comparatorProperty().bind(mainTable.comparatorProperty());

        choiceSearchType.setItems(searchTypes);
        choiceSearchType.getSelectionModel().selectFirst();
        choiceSearchType.getSelectionModel().selectedIndexProperty().addListener(
            ((observable, oldValue, newValue) -> {
                refreshFilter(textSearchText.getText(), newValue.intValue(), cbMyLibrary.isSelected());
            })
        );
        textSearchText.textProperty().addListener(
            ((observable, oldValue, newValue) -> {
                refreshFilter(newValue, choiceSearchType.getSelectionModel().getSelectedIndex(), cbMyLibrary.isSelected());
            })
        );
        cbMyLibrary.selectedProperty().addListener(
            ((observable, oldValue, newValue) -> {
                refreshFilter(textSearchText.getText(), choiceSearchType.getSelectionModel().getSelectedIndex(), newValue);
            })
        );


        btnStop.setOnAction(event -> {
            nowPlaying.stop();
            lblNowPlaying.setText("Now Playing:");
        });
        sldVolume.valueProperty().addListener(observable -> {
            nowPlaying.setVolume((float)sldVolume.getValue());
        });

        btnAddToLibrary.setOnAction(event -> {
            for (Song song : (ObservableList<Song>)mainTable.getSelectionModel().getSelectedItems()) {
                song.addToLibrary(libraryDir);
            }
            mainTable.refresh();
            refreshFilter(textSearchText.getText(), choiceSearchType.getSelectionModel().getSelectedIndex(), cbMyLibrary.isSelected());
        });
        btnRemoveFromLibrary.setOnAction(event -> {
            for (Song song : (ObservableList<Song>)mainTable.getSelectionModel().getSelectedItems()) {
                song.removeFromLibrary(libraryDir);
            }
            mainTable.refresh();
            refreshFilter(textSearchText.getText(), choiceSearchType.getSelectionModel().getSelectedIndex(), cbMyLibrary.isSelected());
        });


        btnSongDirBrowse.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File file = directoryChooser.showDialog(mainTable.getScene().getWindow());
            if (!file.getPath().isEmpty()) {
                txtSongDir.setText(file.getPath());
                songDir = file.getPath();
            }
            saveSettings();
            refresh();
            refreshFilter(textSearchText.getText(), choiceSearchType.getSelectionModel().getSelectedIndex(), cbMyLibrary.isSelected());
        });
        btnLibraryDirBrowse.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File file = directoryChooser.showDialog(mainTable.getScene().getWindow());
            if (!file.getPath().isEmpty()) {
                txtLibraryDir.setText(file.getPath());
                libraryDir = file.getPath();
            }
            saveSettings();
            refresh();
            refreshFilter(textSearchText.getText(), choiceSearchType.getSelectionModel().getSelectedIndex(), cbMyLibrary.isSelected());
        });


        mainTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mainTable.setItems(sortedSongList);
        mainTable.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
//                System.out.println(mainTable.getSelectionModel().getSelectedItem());
                Song song = (Song)mainTable.getSelectionModel().getSelectedItem();
                try
                {   if (nowPlaying != null)
                        nowPlaying.stop();

                    nowPlaying = song;
                    nowPlaying.play();
                    nowPlaying.setVolume((float)sldVolume.getValue());

                    lblNowPlaying.setText("Now Playing: " + nowPlaying.getTitle());

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        refresh();
        refreshFilter(textSearchText.getText(), choiceSearchType.getSelectionModel().getSelectedIndex(), cbMyLibrary.isSelected());
    }

    public void loadSettings() {
        Profile.Section sec = settingsIni.get("pssongmanager");
        if (sec != null) {
            songDir = sec.get("all_songs_dir");
            libraryDir = sec.get("ps_library_dir");
        }
    }

    public void saveSettings() {
        settingsIni.put("pssongmanager", "all_songs_dir", songDir);
        settingsIni.put("pssongmanager", "ps_library_dir", libraryDir);
        try {
            settingsIni.store();
        } catch (Exception e) {
            System.out.println("Unable to save settings");
        }

    }

    public void refreshFilter(String text, int type, boolean myLibrary)
    {
        filteredSongList.setPredicate(song -> {
            if (myLibrary && !song.getInLibrary())
                return false;

            if (textSearchText == null || text.isEmpty())
                return true;

            String lowerCaseValue = text.toLowerCase();

            try {
                switch (searchTypes.get(type)) {
                    case "All":
                        if (song.getTitle().toLowerCase().contains(lowerCaseValue) ||
                                song.getArtist().toLowerCase().contains(lowerCaseValue) ||
                                song.getAlbum().toLowerCase().contains(lowerCaseValue) ||
                                song.getGenre().toLowerCase().contains(lowerCaseValue))
                            return true;
                        break;
                    case "Title":
                        if (song.getTitle().toLowerCase().contains(lowerCaseValue))
                            return true;
                        break;
                    case "Artist":
                        if (song.getArtist().toLowerCase().contains(lowerCaseValue))
                            return true;
                        break;
                    case "Album":
                        if (song.getAlbum().toLowerCase().contains(lowerCaseValue))
                            return true;
                        break;
                    case "Genre":
                        if (song.getGenre().toLowerCase().contains(lowerCaseValue))
                            return true;
                        break;
                }
            }
            catch (Exception e) { return false; }

            return false;
        });

        lblFilesListed.setText(filteredSongList.size() + " song(s) listed");
    }

    public void refresh()
    {
        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(null, createScanFilesTask());
        progressDialog.showAndWait();
    }

    public static int scanCurrentAmount;
    public static int scanCurrentErrors;
    private Task createScanFilesTask() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                songList.clear();
                scanCurrentAmount = 0;
                scanCurrentErrors = 0;

                if (songDir == null || songDir.isEmpty())
                    return false;

                Path startPath = Paths.get(songDir);

                if (!startPath.toFile().exists())
                    return false;

                Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir,
                                                             BasicFileAttributes attrs) {
//                    System.out.println("Dir: " + dir.toString());
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
//                    System.out.println("File: " + file.toString());
                        if (file.getFileName().toString().endsWith(".ini")) {
                            Ini ini;
                            try {
                                ini = new Ini(file.toFile());
                            } catch (Exception e) {
                                return FileVisitResult.CONTINUE;
                            }
                            songList.add(new Song(ini, file.getParent(), new File(libraryDir + "/" + file.getParent().getFileName()).exists()));
                            scanCurrentAmount++;
                            updateMessage("Scanned " + scanCurrentAmount + " files");
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException e) {
                        System.out.println("File visit failed");
                        scanCurrentErrors++;
                        return FileVisitResult.CONTINUE;
                    }
                });

                return true;
            }
        };
    }

    static class ToolTipCell extends TableCell<String, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            this.setText(item);
            this.setTooltip(
                    (empty || item==null) ? null : new Tooltip(item));
        }
    }
}
