package de.treichels.hott.vdfeditor.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import com.sun.javafx.collections.ObservableListWrapper;

import de.treichels.hott.HoTTDecoder;
import gde.model.HoTTException;
import gde.model.enums.TransmitterType;
import gde.model.voice.VDFType;
import gde.model.voice.VoiceData;
import gde.model.voice.VoiceFile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

@SuppressWarnings("restriction")
public class Controller {
    private static final String USER_HOME = "user.home"; //$NON-NLS-1$
    private static final String _WAV = "*.wav"; //$NON-NLS-1$
    private static final String _MP3 = "*.mp3"; //$NON-NLS-1$
    private static final String _OGG = "*.ogg"; //$NON-NLS-1$
    private static final String _VDF = "*.vdf"; //$NON-NLS-1$
    static final String WAV = ".wav"; //$NON-NLS-1$
    private static final String MP3 = ".mp3"; //$NON-NLS-1$
    private static final String OGG = ".ogg"; //$NON-NLS-1$
    private static final String VDF = ".vdf"; //$NON-NLS-1$
    private static final String LAST_LOAD_VDF_DIR = "lastLoadVdfDir"; //$NON-NLS-1$
    private static final String LAST_SAVE_VDF_DIR = "lastSaveVdfDir"; //$NON-NLS-1$
    private static final String LAST_LOAD_SOUND_DIR = "lastLoadSoundDir"; //$NON-NLS-1$
    private static final Preferences PREFS = Preferences.userNodeForPackage(Controller.class);
    private static final ResourceBundle RES = ResourceBundle.getBundle(Controller.class.getName());

    public static boolean isSoundFormat(final File file) {
        final String name = file.getName();
        return name.endsWith(WAV) | name.endsWith(MP3) | name.endsWith(OGG);
    }

    public static boolean isVDF(final File file) {
        return file.getName().endsWith(VDF);
    }

    @FXML
    private ContextMenu contextMenu;
    @FXML
    private MenuItem contextMenuDelete;
    @FXML
    private MenuItem deleteSoundMenuItem;
    @FXML
    private Menu editMenu;
    @FXML
    private ListView<VoiceData> listView;
    @FXML
    private MenuItem moveDownMenuItem;
    @FXML
    private MenuItem moveUpMenuItem;
    @FXML
    private MenuItem playMenuItem;
    @FXML
    private MenuItem renameMenuItem;
    @FXML
    private MenuItem saveVDFMenuItem;
    @FXML
    MenuItem addSoundMenuItem;
    @FXML
    private ComboBox<TransmitterType> transmitterTypeCombo;
    @FXML
    private ComboBox<VDFType> vdfTypeCombo;
    @FXML
    private ComboBox<String> vdfVersionCombo;

    private final ObjectProperty<VoiceFile> voiceFileProperty = new SimpleObjectProperty<>();
    private boolean dirty = false;
    private File vdfFile = null;

    boolean askSave() {
        if (!dirty) return true;

        final ButtonType discardButton = new ButtonType(RES.getString("discard_button"));
        final ButtonType saveButton = new ButtonType(RES.getString("save_button"));
        final Alert alert = new Alert(AlertType.WARNING, RES.getString("save_changes"), saveButton, discardButton, ButtonType.CANCEL); //$NON-NLS-1$
        alert.setHeaderText(RES.getString("modified")); //$NON-NLS-1$
        final Optional<ButtonType> answer = alert.showAndWait();
        if (answer.get() == discardButton) return true;
        if (answer.get() == ButtonType.CANCEL) return false;

        return onSave();
    }

    @FXML
    public void initialize() {
        listView.setCellFactory(lv -> new VoiceDataListCell());
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // clear selection if clicked on an empty row
        listView.setOnMouseClicked(ev -> listView.getSelectionModel().clearSelection());

        // clear selection if ESC key was pressed
        listView.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) listView.getSelectionModel().clearSelection();
        });

        // accept a drop into empty area
        listView.setOnDragOver(ev -> {
            if (ev.getGestureSource() == null && ev.getDragboard().hasFiles())
                // allow only copy from desktop (import of sound file)
                ev.acceptTransferModes(TransferMode.COPY);
            ev.consume();
        });

        listView.setOnDragEntered(ev -> listView.setOpacity(0.3d));
        listView.setOnDragEntered(ev -> listView.setOpacity(1.0d));

        // perform drop actions
        listView.setOnDragDropped(ev -> {
            try {
                if (voiceFileProperty.get() == null) onNew();
                final Dragboard dragboard = ev.getDragboard();
                final ObservableList<VoiceData> items = listView.getItems();

                if (dragboard.hasContent(VoiceDataListCell.DnD_DATA_FORMAT)) {
                    // DnD between VDFEditor instances
                    @SuppressWarnings("unchecked")
                    final ArrayList<VoiceData> droppedItems = (ArrayList<VoiceData>) dragboard.getContent(VoiceDataListCell.DnD_DATA_FORMAT);
                    for (final VoiceData vd : droppedItems)
                        items.add(vd);
                    ev.setDropCompleted(true);
                } else if (ev.getDragboard().hasFiles()) {
                    // import sound or .vdf files from desktop
                    final List<File> files = ev.getDragboard().getFiles();

                    // import first .vdf file
                    final Optional<File> vdf = files.stream().filter(Controller::isVDF).findFirst();
                    if (vdf.isPresent() && askSave()) {
                        vdfFile = vdf.get();
                        dirty = false;
                        try {
                            open(HoTTDecoder.decodeVDF(vdfFile));
                        } catch (final IOException e) {
                            ExceptionDialog.show(e);
                        }
                    }

                    // import any sound files
                    items.addAll(files.stream().filter(Controller::isSoundFormat).map(VoiceData::readSoundFile).collect(Collectors.toList()));

                    ev.setDropCompleted(true);
                }

                ev.consume();
            } catch (final RuntimeException e) {
                ExceptionDialog.show(e);
            }
        });

        editMenu.disableProperty().bind(voiceFileProperty.isNull());
        saveVDFMenuItem.disableProperty().bind(voiceFileProperty.isNull());
        vdfVersionCombo.getItems().addAll(Float.toString(2.0f), Float.toString(2.5f), Float.toString(3.0f));
        vdfVersionCombo.disableProperty().bind(voiceFileProperty.isNull());
        vdfTypeCombo.getItems().addAll(VDFType.values());
        vdfTypeCombo.disableProperty().bind(voiceFileProperty.isNull());
        transmitterTypeCombo.getItems().addAll(TransmitterType.values());
        transmitterTypeCombo.getItems().remove(TransmitterType.mz12);
        transmitterTypeCombo.getItems().remove(TransmitterType.mz18);
        transmitterTypeCombo.getItems().remove(TransmitterType.mz24);
        transmitterTypeCombo.getItems().remove(TransmitterType.mz24Pro);
        transmitterTypeCombo.getItems().remove(TransmitterType.unknown);
        transmitterTypeCombo.disableProperty().bind(voiceFileProperty.isNull());

        contextMenuDelete.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        moveDownMenuItem.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        moveUpMenuItem.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        playMenuItem.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        renameMenuItem.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        deleteSoundMenuItem.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        addSoundMenuItem.disableProperty().bind(voiceFileProperty.isNull());

        onNew();
    }

    @FXML
    public void onAbout() {
        final Alert alert = new Alert(AlertType.INFORMATION, RES.getString("about_text"), ButtonType.CLOSE); //$NON-NLS-1$
        alert.setTitle(RES.getString("about"));
        alert.setHeaderText(Launcher.getTitle());
        alert.setGraphic(new ImageView(getClass().getResource("icon.png").toString()));
        alert.showAndWait();
    }

    @FXML
    public void onAddSound() {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(RES.getString("load_sound")); //$NON-NLS-1$
        final File dir = new File(PREFS.get(LAST_LOAD_SOUND_DIR, System.getProperty(USER_HOME)));
        if (dir != null && dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("sound_files"), _WAV, _MP3, _OGG)); //$NON-NLS-1$
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("wav_files"), _WAV)); //$NON-NLS-1$
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("mp3_files"), _MP3)); //$NON-NLS-1$
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("ogg_files"), _OGG)); //$NON-NLS-1$

        final List<File> files = chooser.showOpenMultipleDialog(listView.getScene().getWindow());
        if (files != null) {
            // store dir in prefs
            files.stream().filter(Controller::isSoundFormat).findFirst().map(File::getParentFile).map(File::getAbsolutePath)
                    .ifPresent(s -> PREFS.put(LAST_LOAD_SOUND_DIR, s));
            final int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            final List<VoiceData> sounds = files.stream().filter(Controller::isSoundFormat).map(VoiceData::readSoundFile).collect(Collectors.toList());
            if (selectedIndex == -1)
                listView.getItems().addAll(sounds);
            else
                listView.getItems().addAll(selectedIndex, sounds);
        }
    }

    @FXML
    public void onClose() {
        if (askSave()) System.exit(0);
    }

    @FXML
    public void onDeleteSound() {
        final ObservableList<Integer> selectedIndices = listView.getSelectionModel().getSelectedIndices();
        // sort indices in reverse order and remove from high to low
        selectedIndices.stream().sorted((i, j) -> j - i).forEach(i -> listView.getItems().remove(i.intValue()));
    }

    @FXML
    public void onMoveDown() {
        final MultipleSelectionModel<VoiceData> selectionModel = listView.getSelectionModel();
        final VoiceData selectedItem = selectionModel.getSelectedItem();
        final int selectedIndex = selectionModel.getSelectedIndex();
        final ObservableList<VoiceData> items = listView.getItems();

        items.remove(selectedIndex);
        items.add(selectedIndex + 1, selectedItem);
        selectionModel.clearSelection();
        selectionModel.select(selectedIndex + 1);
    }

    @FXML
    public void onMoveUp() {
        final MultipleSelectionModel<VoiceData> selectionModel = listView.getSelectionModel();
        final VoiceData selectedItem = selectionModel.getSelectedItem();
        final int selectedIndex = selectionModel.getSelectedIndex();
        final ObservableList<VoiceData> items = listView.getItems();

        items.remove(selectedIndex);
        items.add(selectedIndex - 1, selectedItem);
        selectionModel.clearSelection();
        selectionModel.select(selectedIndex - 1);
    }

    @FXML
    public void onNew() {
        if (askSave()) {
            vdfFile = null;
            dirty = false;
            open(new VoiceFile());
        }
    }

    @FXML
    public void onOpen() throws IOException {
        if (askSave()) {
            final FileChooser chooser = new FileChooser();
            chooser.setTitle(RES.getString("open_vdf")); //$NON-NLS-1$
            final File dir = new File(PREFS.get(LAST_LOAD_VDF_DIR, System.getProperty(USER_HOME)));
            if (dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);
            chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("vdf_files"), _VDF)); //$NON-NLS-1$

            final File vdf = chooser.showOpenDialog(listView.getScene().getWindow());
            if (vdf != null) {
                PREFS.put(LAST_LOAD_VDF_DIR, vdf.getParentFile().getAbsolutePath());
                vdfFile = vdf;
                dirty = false;
                open(HoTTDecoder.decodeVDF(vdf));
            }
        }
    }

    @FXML
    public void onPlay() {
        listView.getSelectionModel().getSelectedItems().forEach(VoiceData::play);
    }

    @FXML
    public void onRename() {
        listView.edit(listView.getSelectionModel().getSelectedIndex());
    }

    @FXML
    public boolean onSave() {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(RES.getString("save_vdf")); //$NON-NLS-1$
        final File dir = new File(PREFS.get(LAST_SAVE_VDF_DIR, System.getProperty(USER_HOME)));
        if (dir.exists() && dir.isDirectory()) chooser.setInitialDirectory(dir);
        if (vdfFile != null) chooser.setInitialFileName(vdfFile.getName());
        chooser.getExtensionFilters().add(new ExtensionFilter(RES.getString("vdf_files"), _VDF)); //$NON-NLS-1$

        final File vdf = chooser.showSaveDialog(listView.getScene().getWindow());
        if (vdf != null) {
            vdfFile = vdf;
            ((Stage) listView.getScene().getWindow()).setTitle(vdf.getName());
            PREFS.put(LAST_SAVE_VDF_DIR, vdf.getParentFile().getAbsolutePath());

            try {
                HoTTDecoder.encodeVDF(voiceFileProperty.get(), vdf);
                dirty = false;
            } catch (final IOException e) {
                ExceptionDialog.show(e);
            }
        }

        return vdf != null;
    }

    @FXML
    public void onTransmitterTypeChanged(final ActionEvent ev) {
        final VoiceFile voiceFile = voiceFileProperty.get();
        final TransmitterType oldTtransmitterType = voiceFile.getTransmitterType();
        voiceFile.setTransmitterType(transmitterTypeCombo.getValue());

        try {
            HoTTDecoder.verityVDF(voiceFile);
            setTitle();
        } catch (final HoTTException e) {
            voiceFile.setTransmitterType(oldTtransmitterType);
            transmitterTypeCombo.setValue(oldTtransmitterType);
            ExceptionDialog.show(e);
        }

        ev.consume();
    }

    @FXML
    public void onVDFTypeChanged(final ActionEvent ev) {
        final VoiceFile voiceFile = voiceFileProperty.get();
        final VDFType oldVDFType = voiceFile.getVdfType();
        voiceFile.setVdfType(vdfTypeCombo.getValue());

        try {
            HoTTDecoder.verityVDF(voiceFile);
            setTitle();
        } catch (final HoTTException e) {
            voiceFile.setVdfType(oldVDFType);
            vdfTypeCombo.setValue(oldVDFType);
            ExceptionDialog.show(e);
        }

        ev.consume();
    }

    private void open(final VoiceFile voiceFile) {
        voiceFileProperty.set(voiceFile);
        vdfTypeCombo.setValue(voiceFile.getVdfType());
        vdfVersionCombo.setValue(String.format("%1.1f", voiceFile.getVdfVersion() / 1000.0));
        transmitterTypeCombo.setValue(voiceFile.getTransmitterType());

        final ObservableListWrapper<VoiceData> items = new ObservableListWrapper<>(voiceFile.getVoiceData());
        items.addListener((ListChangeListener<VoiceData>) c -> {
            dirty = true;

            while (c.next())
                if (c.wasAdded()) {
                    int lastIndex = c.getTo() - 1;

                    // remove added entry until validation succeeds
                    while (true)
                        try {
                            // validate VDF
                            HoTTDecoder.verityVDF(voiceFileProperty.get());

                            // validation succeeded - no exception was thrown
                            break;
                        } catch (final HoTTException e) {
                            // validation failed, remove last added item and try again
                            items.remove(lastIndex--);
                            ExceptionDialog.show(e);
                        }
                }

            setTitle();
        });

        dirty = false;
        listView.setItems(items);

        setTitle();

    }

    void setTitle() {
        final StringBuilder sb = new StringBuilder();

        if (dirty) sb.append("*"); //$NON-NLS-1$
        if (vdfFile == null)
            sb.append(RES.getString("empty")); //$NON-NLS-1$
        else
            sb.append(vdfFile.getName());

        if (voiceFileProperty.isNotNull().get()) {
            final VoiceFile voiceFile = voiceFileProperty.get();
            try {
                final int maxDataSize = HoTTDecoder.getMaxDataSize(voiceFile);
                final int dataSize = voiceFile.getDataSize();

                sb.append(String.format(" - %d kb / %d kb (%d%%)", dataSize / 1024, maxDataSize / 1024, dataSize * 100 / maxDataSize)); //$NON-NLS-1$
            } catch (final HoTTException e) {
                ExceptionDialog.show(e);
            }
        }

        final Scene scene = listView.getScene();
        if (scene != null) ((Stage) scene.getWindow()).setTitle(sb.toString());
    }
}
