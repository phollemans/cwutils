/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2023 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui.open;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Insets;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.SwingWorker;
import javax.swing.SwingConstants;
import javax.swing.JList;
import javax.swing.InputVerifier;
import javax.swing.JButton;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.ReaderSummaryProducer;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.gui.EarthDataViewFactory;
import noaa.coastwatch.gui.EarthDataViewPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.SimpleFileFilter;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.SolidBackground;
import noaa.coastwatch.util.trans.SwathProjection;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.VariableStatisticsGenerator;

import java.util.logging.Logger;
import java.util.logging.Level;

/** 
 * The <code>EarthDataReaderChooser</code> class allows the user to choose a
 * dataset and list of variables for opening geographic data files recognized
 * by the {@link noaa.coastwatch.io.EarthDataReaderFactory} class.  For ease of
 * use, a static instance can be obtained from {@link #getInstance}.  Use the
 * {@link #showDialog} method to display the instance which blocks the parent
 * until the user hits OK or Cancel, then check that the state returned by 
 * {@link #getState} gives a value of <code>State.SELECTED</code>.  You can
 * then use the result of the {@link #getReader} method which returns the
 * selected reader with statistics assigned to the variables that the user
 * chose, which you can obtain from {@link EarthDataReader#getStatisticsVariables}.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class EarthDataReaderChooser extends JPanel {

  private static final Logger LOGGER = Logger.getLogger (EarthDataReaderChooser.class.getName());

  // This is the state property that is updated when a new reader and list of
  // variables becomes available.
  public static final String STATE_PROPERTY = "state";

  private JFileChooser fileChooser;
  private EarthDataViewPanel dataViewPanel;
  private JPanel previewContent;
  private JList<String> variableList;
  private ReaderOpenOperation readerOperation;
  private PreviewLoadOperation previewOperation;
  private StatsComputationOperation statsOperation;
  private EarthDataReader activeReader;
  private EarthDataView nullDataView;
  private CoastOverlay coastOverlay;
  private State state;
  private JDialog fileOpenDialog;
  private JLabel dialogInfoLabel;
  private JProgressBar dialogProgressBar;
  private EarthDataReader selectedReader;
  private Map<EarthDataReader, Integer> readerRefs;
  private List<DataViewRenderingContext> viewRenderingContextList;

  public static EarthDataReaderChooser instance;

  ////////////////////////////////////////////////////////////

  public enum State { UNSELECTED, READY, SELECTED };

  ////////////////////////////////////////////////////////////

  private static class DataViewRenderingContext {

    public EarthDataViewPanel panel;
    public EarthDataReader reader;

    public DataViewRenderingContext (EarthDataViewPanel panel, EarthDataReader reader) {
      this.panel = panel;
      this.reader = reader;
    } // DataViewRenderingContext

  } // DataViewRenderingContext class

  ////////////////////////////////////////////////////////////

  public static EarthDataReaderChooser getInstance () {

    if (instance == null) instance = new EarthDataReaderChooser();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  private void acquire (EarthDataReader reader) {

    readerRefs.compute (reader, (obj,count) -> ((count == null) ? Integer.valueOf (1) : Integer.valueOf (count+1)));
    LOGGER.fine ("Acquired reader " + reader + " with new reference count of " + readerRefs.get (reader));
    LOGGER.fine ("Reader refs now contains " + readerRefs.size() + " entries");

  } // acquire

  ////////////////////////////////////////////////////////////

  private void release (EarthDataReader reader) {

    var count = readerRefs.get (reader);
    if (count == null) throw new IllegalStateException ("Release called on untracked object");

    if (count == 1) {
      LOGGER.fine ("Closing released reader " + reader);
      try { reader.close(); }
      catch (IOException e) { 
        LOGGER.log (Level.FINE, "Error closing reader", e);
      } // catch
      readerRefs.remove (reader);
    } // if
    else {
      readerRefs.put (reader, Integer.valueOf (count-1));
    } // else

    LOGGER.fine ("Released reader " + reader + " with new reference count of " + (count-1));
    LOGGER.fine ("Reader refs now contains " + readerRefs.size() + " entries");

  } // release

  ////////////////////////////////////////////////////////////

  /**
   * Gets the state of the chooser after being shown by the {@link #showDialog} 
   * method.  The <code>State.UNSELECTED</code> value indicates that either the 
   * user cancelled the dialog, or the file selected is not a valid reader.  
   * The <code>State.SELECTED</code> value indicates that a valid reader can
   * be obtained from {@link #getReader}.
   */
  public State getState() { return (state); }

  ////////////////////////////////////////////////////////////

  private JLabel formatLabel (String text, Font font, int width) {

    JLabel label = new JLabel();
    label.setFont (font);
    int length = text.length();
    do {
      String shortText = GUIServices.ellipsisString (text, length);
      label.setText (shortText);
      length = length - 1;
    } while (label.getPreferredSize().getWidth() > width);

    return (label);

  } // formatLabel

  ////////////////////////////////////////////////////////////

  private void dataViewRenderingEvent (PropertyChangeEvent event) {

    var rendering = (boolean) event.getNewValue();
    var sourcePanel = (EarthDataViewPanel) event.getSource();

    // If the property event indicates that we just started rendering a new 
    // view, and the view is not the null view, then store a context that saves
    // the panel and the reader so that we can make sure that the reader 
    // stays open while the view is being rendered.
    if (rendering) {
      if (sourcePanel.getView() != nullDataView) {
        var context = new DataViewRenderingContext (sourcePanel, activeReader);
        viewRenderingContextList.add (context);
        acquire (context.reader);
        LOGGER.fine ("Data preview image started rendering, context list contains " + viewRenderingContextList.size() + " entries");
      } // if
    } // if

    // If the property event indicates that the view is finished rendering,
    // then we can release the reader.
    else {
      if (sourcePanel.getView() != nullDataView) {
        DataViewRenderingContext context = null;
        for (var candidate : viewRenderingContextList) {
          if (candidate.panel == sourcePanel) { context = candidate; break; }
        }  // for
        if (context == null) throw new IllegalStateException ("Cannot locate context for data view rendering");
        release (context.reader);
        viewRenderingContextList.remove (context);
        LOGGER.fine ("Data preview image finished rendering, context list contains " + viewRenderingContextList.size() + " entries");
      } // if
    } // else

  } // dataViewRenderingEvent

  ////////////////////////////////////////////////////////////

  private void updatePreviewContent (EarthDataReader reader) {

    previewContent.removeAll();
    int previewWidth = 300;
    var labelFont = new JLabel().getFont();
    var boldFont = labelFont.deriveFont (Font.BOLD);
    var smallFont = labelFont.deriveFont (labelFont.getSize() * 0.8f);
    var smallBoldFont = boldFont.deriveFont (boldFont.getSize() * 0.8f);
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;

    // Add the data preview image, to be populated later when the
    // user selects a variable from the list.
    dataViewPanel = new EarthDataViewPanel (nullDataView);
    dataViewPanel.setBackground (Color.BLACK);
    dataViewPanel.setPreferredSize (new Dimension (150, 150));
    dataViewPanel.addPropertyChangeListener (EarthDataViewPanel.RENDERING_PROPERTY, event -> dataViewRenderingEvent (event));
    previewContent.add (dataViewPanel, gc);
    previewContent.add (Box.createVerticalStrut (10), gc);

    // Add the file name, data format, and size.
    var file = new File (reader.getSource());
    var fileLabel = formatLabel (file.getName(), boldFont, previewWidth);
    previewContent.add (fileLabel, gc);

    double size = file.length();
    String unit = "bytes";
    if (size > 1024) { size /= 1024; unit = "kB"; }
    if (size > 1024) { size /= 1024; unit = "MB"; }
    if (size > 1024) { size /= 1024; unit = "GB"; }
    if (size > 1024) { size /= 1024; unit = "TB"; }

    var sizeLabel = new JLabel (" - " + String.format ("%.1f", size) + " " + unit);
    var fileFormatLabel = formatLabel (reader.getDataFormat(), labelFont, previewWidth - (int) sizeLabel.getPreferredSize().getWidth());
    var fileFormatSizeLabel = new JLabel (fileFormatLabel.getText() + sizeLabel.getText());
    previewContent.add (fileFormatSizeLabel, gc);

    previewContent.add (Box.createVerticalStrut (10), gc);

    // Add the list of variables.
    List<String> variables = null;
    try { variables = reader.getAllGrids(); }
    catch (IOException e) { 
      LOGGER.log (Level.FINE, "Error getting list of grid variable names", e);
      variables = List.<String> of();
    } // catch

    var variableHeaderLine = Box.createHorizontalBox();
    var variableLabel = new JLabel ("Variables (" + variables.size() + ")");
    variableLabel.setFont (boldFont);
    variableHeaderLine.add (variableLabel);
    var selectAllButton = new JButton ("Select all");
    selectAllButton.setFont (smallFont);
    selectAllButton.setMargin (new Insets (2, 5, 2, 5));
    selectAllButton.addActionListener (event -> variableSelectAllEvent());
    variableHeaderLine.add (Box.createHorizontalGlue());
    variableHeaderLine.add (selectAllButton);
    previewContent.add (variableHeaderLine, gc);

    previewContent.add (Box.createVerticalStrut (5), gc);

    variableList = new JList<String> (variables.toArray (new String[0]));
    variableList.setVisibleRowCount (5);
    variableList.addListSelectionListener (event -> variableSelectEvent (event));
    var variableScrollPane = new JScrollPane (variableList);
    previewContent.add (variableScrollPane, gc);

    previewContent.add (Box.createVerticalStrut (10), gc);

    // Add the file information metadata.
    var infoLabel = new JLabel ("Information");
    infoLabel.setFont (boldFont);
    previewContent.add (infoLabel, gc);

    previewContent.add (Box.createVerticalStrut (5), gc);

    var summaryMap = ReaderSummaryProducer.getInstance().getGlobalSummary (reader);
    var ignore = List.<String>of ("Transform ident", "Format", "Reader ident", "Map affine", "Scene time");
    summaryMap.forEach ((key, value) -> {
      if (!ignore.contains (key)) {
        Box box = Box.createHorizontalBox();
        previewContent.add (box, gc);
        JLabel keyLabel = new JLabel (key);      
        keyLabel.setFont (smallFont);
        box.add (keyLabel);
        box.add (Box.createHorizontalStrut (10));
        box.add (Box.createHorizontalGlue());
        int keyWidth = (int) keyLabel.getPreferredSize().getWidth();
        JLabel valueLabel = formatLabel (value, smallBoldFont, previewWidth - (keyWidth + 10));
        box.add (valueLabel);
      } // if
    });

    // Add a final line to the bottom to take up space.
    gc.weighty = 1;
    gc.fill = GridBagConstraints.BOTH;
    previewContent.add (Box.createGlue(), gc);

    this.validate();

  } // updatePreviewContent

  ////////////////////////////////////////////////////////////

  private void addInstructionsContent() {

    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;

    previewContent.add (Box.createVerticalStrut (50), gc);

    var graphic = new JLabel (GUIServices.getIcon ("chooser.graphic")) {
      @Override
      public void paint (Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite (AlphaComposite.getInstance (AlphaComposite.SRC_OVER, 0.2f));
        super.paint (g2);
        g2.dispose();
      } // paint
    };
    graphic.setHorizontalAlignment (SwingConstants.CENTER);
    graphic.setOpaque (false);
    previewContent.add (graphic, gc);

    previewContent.add (Box.createVerticalStrut (20), gc);

    var label = new JLabel ("<html>" +
      "<center>&#8592; Choose a data file on the left and select<br>" +
      "your variables of interest from the list.<br>" +
      "Then click OK to open the file.</center>");
    label.setHorizontalAlignment (SwingConstants.CENTER);
    previewContent.add (label, gc);

    // Add a final line to the bottom to take up space.
    gc.weighty = 1;
    gc.fill = GridBagConstraints.BOTH;
    previewContent.add (Box.createGlue(), gc);

  } // addInstructionsContent

  ////////////////////////////////////////////////////////////

  private void clearPreviewContent () {

    previewContent.removeAll();
    addInstructionsContent();
    dataViewPanel = null;
    variableList = null;
    this.validate();

  } // clearPreviewcContent

  ////////////////////////////////////////////////////////////

  /** Creates a new chooser panel. */
  public EarthDataReaderChooser () {

    setLayout (new BorderLayout());

    // Create a file chooser that is customized to select geographic data 
    // files of various extensions.  We get it from GUIServices so that it
    // tracks and recalls the current directory between program invocations.
    fileChooser = GUIServices.getFileChooser();
    var extensions = new String[] {"hdf", "nc4", "nc"};
    SimpleFileFilter filter = new SimpleFileFilter (extensions, "Scientific data files");
    fileChooser.addChoosableFileFilter (filter);
    fileChooser.setDialogType (JFileChooser.OPEN_DIALOG);
    fileChooser.setControlButtonsAreShown (false);
    fileChooser.setFileFilter (filter);
    fileChooser.addPropertyChangeListener (JFileChooser.DIRECTORY_CHANGED_PROPERTY, event -> fileChooser.setSelectedFile (new File ("")));
    fileChooser.addPropertyChangeListener (JFileChooser.FILE_FILTER_CHANGED_PROPERTY, event -> fileChooser.setSelectedFile (new File ("")));
    fileChooser.addPropertyChangeListener (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, event -> fileChangedEvent (event));

    this.add (fileChooser, BorderLayout.CENTER);

    // Create a preview content pane inside a scroll window.
    previewContent = new JPanel();
    previewContent.setLayout (new GridBagLayout());
    previewContent.setBorder (BorderFactory.createEmptyBorder (10, 20, 10, 20));
    var scrollPane = new JScrollPane (previewContent, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setPreferredSize (new Dimension (350, 300));
    scrollPane.setMaximumSize (new Dimension (350, 300));
    scrollPane.setBorder (BorderFactory.createEmptyBorder (0, 0, 0, 0));
    addInstructionsContent();
    this.add (scrollPane, BorderLayout.EAST);

    // Create the null data view that we'll use to show a blank background
    // in the data view panel.
    try { nullDataView = new SolidBackground (new Color (0, 0, 32)); }
    catch (Exception e) {
      LOGGER.log (Level.WARNING, "Cannot create null data view", e);
    } // catch
    
    // Create the coast overlay that is used to show a coastline on top of
    // map projected data.
    coastOverlay = new CoastOverlay (Color.WHITE);

    // Create a cache of EarthDataReaders that may need to be closed.
    readerRefs = new HashMap<>();

    // Create a list of view rendering contexts that store the data view panel
    // and reader used when the data view is rendering.  This way we can
    // properly release the reader when it's no longer needed.
    viewRenderingContextList = new ArrayList<>();

    // Explicitly set the initial state to unselected.
    state = State.UNSELECTED;

  } // EarthDataReaderChooser constructor

  ////////////////////////////////////////////////////////////

  private void clearDataPreview () {

    dataViewPanel.setView (nullDataView);
    dataViewPanel.repaint();

  } // clearDataPreview  

  ////////////////////////////////////////////////////////////

  private void updateState () {

    // The state of readiness depends on if we have a valid reader, and
    // if the list of variables has at least one selection.
    State newState;
    if (activeReader != null && variableList.getSelectionModel().getSelectedItemsCount() != 0)
      newState = State.READY;
    else if (selectedReader != null)
      newState = State.SELECTED;
    else
      newState = State.UNSELECTED;

    if (state != newState) {
      State oldState = state;
      state = newState;
      firePropertyChange (STATE_PROPERTY, oldState, newState);
      LOGGER.fine ("Chooser state transitioned from " + oldState + " to " + newState);
    } // if

  } // updateState

  ////////////////////////////////////////////////////////////

  private void variableSelectAllEvent () {

    variableList.setSelectionInterval (0, variableList.getModel().getSize()-1);
    
  } // variableSelectAllEvent

  ////////////////////////////////////////////////////////////

  private void variableSelectEvent (ListSelectionEvent event) {

    if (!event.getValueIsAdjusting() && activeReader != null) {

      // If there is a single variable selected, update the view and 
      // show that variable preview using a background process.
      var selectedVariableList = variableList.getSelectedValuesList(); 
      int listSize = selectedVariableList.size();
      if (listSize == 1) {
        var varName = (String) selectedVariableList.get (0);
        startPreviewLoad (varName);
      } // if     

      // Otherwise, clear the preview panel.
      else {
        clearDataPreview();
      } // else

      updateState();

    } // if

  } // variableSelectEvent

  ////////////////////////////////////////////////////////////

  private void startPreviewLoad (String varName) {

    // First cancel any previous preview load operation that is still running.
    if (previewOperation != null && !previewOperation.isDone()) {
      previewOperation.cancel (false);
    } // if

    // Now start executing a new preview load operation.
    variableList.setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
    previewOperation = new PreviewLoadOperation (activeReader, varName);
    previewOperation.execute();

  } // startPreviewLoad

  ////////////////////////////////////////////////////////////

  private void completePreviewLoad (
    EarthDataReader reader,
    String varName,
    EarthDataView view
  ) {

    // Update the data preview if non-null.
    if (view != null) {
      LOGGER.fine ("Updating preview to show " + varName);
      if (!(reader.getInfo().getTransform() instanceof SwathProjection))
        view.addOverlay (coastOverlay);
      dataViewPanel.setView (view);
      dataViewPanel.repaint();
    } // if

    // Otherwise, clear the data preview.
    else {
      clearDataPreview();
    } // else

    variableList.setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));

  } // completePreviewLoad

  ////////////////////////////////////////////////////////////

  private class PreviewLoadOperation extends SwingWorker<EarthDataView, Object> {

    private EarthDataReader reader;
    private String varName;

    public PreviewLoadOperation (
      EarthDataReader reader,
      String varName
    ) {      
      acquire (reader);
      this.reader = reader;
      this.varName = varName;
    } // PreviewLoadOperation

    @Override
    public EarthDataView doInBackground() {

      EarthDataView view = null;

      // Create the view from a factory using the reader and variable name.
      if (!isCancelled()) {
        LOGGER.fine ("Loading preview for " + varName + " in background thread");
        var factory = EarthDataViewFactory.getInstance();
        synchronized (factory) { view = factory.create (reader, varName); }
      } // if

      // If we were cancelled during the operation, set the view to null. 
      if (isCancelled()) {
        LOGGER.fine ("Preview load operation has been cancelled");
        view = null;
      } // if

      return (view);

    } // doInBackground

    @Override
    protected void done() {

      if (!isCancelled()) {
        EarthDataView view;
        try { view = get(); }
        catch (Exception e) { view = null; }
        completePreviewLoad (reader, varName, view);
        LOGGER.fine ("Preview load operation is now complete");
      } // if

      previewOperation = null;
      release (reader);

    } // done

  } // PreviewLoadOperation class

  ////////////////////////////////////////////////////////////

  private void fileChangedEvent (PropertyChangeEvent event) {

    // The file change event spawns a sequences of operations to open and
    // show a preview of a file as follows:
    //
    // - Close the active data reader if needed.  This is the reader that is 
    // currently being used to populate the preview content, list of variables,
    // and preview image.
    //
    // - Cancel any reader open operation that may be alread running.  This 
    // has the effect of closing the reader that the operation has created, 
    // if any.
    //
    // - Start a new reader open operation in a background thread.
    //
    // - When the reader open operation is done, update the active reader
    // and populate the preview content and list of variables.

    // We check for a null new value here because some file choosers generate
    // two events when a new file in the same directory is selected: the first 
    // event that signals a new value of null and immediately after that the 
    // second event signals a non-null new value with the new file name.  The
    // issue with this is that it makes this routine cause a blink in the 
    // data preview before the new data preview is displayed.  To prevent
    // this blink we stop the change from being responded to until an actual
    // non-null value is available.  The only downside is that if the user 
    // deselects a file by holding down the control key, the data preview 
    // doesn't go away.  The other alternative would be to attempt to 
    // coalesce the two property change events by setting a timer after the
    // first event received, and then to actually process the null if the next
    // non-null event never actually arrives.

    if (event.getNewValue() != null) {

      // Start by closing any existing active reader.  Whether the new reader
      // is null or not, we'll be replacing the active.
      if (activeReader != null) {
        release (activeReader);
        activeReader = null;
      } // if

      // First we cancel any reader operation that may be in progress.  A new
      // operation will either take its place or we'll need to clear out the 
      // preview content.
      if (readerOperation != null && !readerOperation.isDone()) {
        readerOperation.cancel (false);
      } // if

      // If the file selected is legitimate and exists, we start a new
      // process in a background thread to open the file and see if 
      // a reader can be created from it.
      File file = fileChooser.getSelectedFile();
      if (file != null && file.isFile() && file.exists()) {
        var name = file.getPath();
        fileChooser.setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
        readerOperation = new ReaderOpenOperation (name);
        readerOperation.execute();
      } // if

      // Otherwise, we clear out the preview content.
      else {
        clearPreviewContent();
      } // else

      updateState();

    } // if

  } // fileChangedEvent

  ////////////////////////////////////////////////////////////

  private void completeReaderOpen (EarthDataReader reader) {

    // Update the active reader and display if non-null.
    if (reader != null) {
      updatePreviewContent (reader);
      activeReader = reader;
      acquire (activeReader);
    } // if

    // Otherwise, clear the preview contents.
    else {
      clearPreviewContent();
    } // else

    fileChooser.setCursor (Cursor.getPredefinedCursor (Cursor.DEFAULT_CURSOR));

  } // completeReaderOpen

  ////////////////////////////////////////////////////////////

  private class ReaderOpenOperation extends SwingWorker<EarthDataReader, Object> {

    private String filename;

    public ReaderOpenOperation (
      String filename
    ) {
      this.filename = filename;
    } // ReaderOpenOperation

    @Override
    public EarthDataReader doInBackground() {

      EarthDataReader reader = null;

      // Create the reader from the filename using the read factory.  To
      // speed things up, turn off swath mode so that we get the reader 
      // faster.
      if (!isCancelled()) {
        LOGGER.fine ("Creating reader from " + filename + " in background thread");
        SwathProjection.setNullMode (true);
        try { reader = EarthDataReaderFactory.create (filename); }
        catch (IOException e) { 
          LOGGER.log (Level.FINE, "Error creating reader", e);
        } // create
        SwathProjection.setNullMode (false);
        if (reader != null) LOGGER.fine ("Opened reader " + reader);
      } // if

      // If we were cancelled during the operation, close the reader and 
      // set to null.
      if (isCancelled()) {
        LOGGER.fine ("Reader open operation has been cancelled");
        if (reader != null) {
          try { reader.close(); }
          catch (IOException e) { 
            LOGGER.log (Level.FINE, "Error closing reader", e);
          } // catch
          reader = null;
        } // if
      } // if

      return (reader);

    } // doInBackground

    @Override
    protected void done() {

      if (!isCancelled()) {
        EarthDataReader reader;
        try { reader = get(); }
        catch (Exception e) { reader = null; }
        completeReaderOpen (reader);
        LOGGER.fine ("Reader open operation is now complete");
      } // else

      readerOperation = null;

    } // done

  } // ReaderOpenOperation class

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected reader.
   * 
   * @return the currently selected reader, or null if no file 
   * is selected, or the selected file does not contain data supported
   * by the {@link noaa.coastwatch.io.EarthDataReaderFactory}.  The selected
   * file is only valid when {@link #getState} returns 
   * <code>State.SELECTED</code>.
   * 
   * @see #getState
   */
  public EarthDataReader getReader () { return (selectedReader); }

  ////////////////////////////////////////////////////////////

  private void startStatsComputation () {

    // Check that we don't have a stats operation already in progress.  That
    // would mean that something has gone wrong.
    if (statsOperation != null && !statsOperation.isDone()) {
      throw new IllegalStateException ("Statistics computation already in progress");
    } // if

    // Set up the stats computation label and progress bar.  Initially
    // the progress bar will be indeterminate and then we'll switch to
    // determinate right after we start processing the first variable.
    dialogInfoLabel.setText ("Computing statistics for data ...");
    dialogProgressBar.setMinimum (0);
    var variables = variableList.getSelectedValuesList();
    dialogProgressBar.setMaximum (variables.size());
    dialogProgressBar.setValue (0);
    dialogProgressBar.setIndeterminate (true);
    dialogInfoLabel.setVisible (true);
    dialogProgressBar.setVisible (true);

    // Block any input from the chooser while the stats are being computed.
    // That would just confuse things.
    fileOpenDialog.getGlassPane().setVisible (true);
    fileOpenDialog.getGlassPane().requestFocusInWindow();

    // Start the new stats computation with the currenly active reader and
    // the list of variables that the user has selected.
    statsOperation = new StatsComputationOperation (activeReader.getSource(), variables);
    statsOperation.execute();

  } // startStatsComputation

  ////////////////////////////////////////////////////////////

  private void statsProgressEvent (int value) {

    if (dialogProgressBar.isIndeterminate()) dialogProgressBar.setIndeterminate (false);
    dialogProgressBar.setValue (value);

  } // statsProgressEvent

  ////////////////////////////////////////////////////////////

  private void completeStatsComputation (EarthDataReader reader) {

    // Set the final selected reader to the reader just returned.
    selectedReader = reader;

    // Clean up the dialog and hide the progress bar and label.  Also,
    // restore the dialog input capability by hiding the input
    // blocking pane.
    dialogInfoLabel.setVisible (false);
    dialogProgressBar.setVisible (false);
    fileOpenDialog.getGlassPane().setVisible (false);
    fileOpenDialog.setVisible (false);

    // Tell the file chooser to clear its value.  That way the next time
    // this chooser is used, it's not selected in the file chooser and the
    // preview details are blank.  This also has the carry on effect of 
    // releasing the final active reader value and setting it to null. 
    fileChooser.setSelectedFile (new File (""));

  } // completeStatsComputation

  ////////////////////////////////////////////////////////////

  private class StatsComputationOperation extends SwingWorker<EarthDataReader, Integer> {

    private String filename;
    private List<String> variables;

    public StatsComputationOperation (
      String filename,
      List<String> variables
    ) {

      this.filename = filename;
      this.variables = variables;

    } // StatsComputationOperation

    @Override
    public EarthDataReader doInBackground() {

      EarthDataReader reader = null;

      // Create the reader from the filename using the read factory.  Note
      // that in this case, we don't turn off swath mode, because we want
      // the resulting reader to have a swath transform that works so that
      // we can pass it back to the user.
      LOGGER.fine ("Opening reader from " + filename + " in background thread");
      try { reader = EarthDataReaderFactory.create (filename); }
      catch (IOException e) { 
        LOGGER.log (Level.FINE, "Error creating reader", e);
      } // create
      if (reader != null) LOGGER.fine ("Opened reader " + reader);

      // Now loop over each variable and compute its stats, and save the stats
      // to the reader.  While we do this, we report our progress back to the
      // main application thread.
      if (reader != null) {
        for (int i = 0; i < variables.size(); i++) {
          publish (Integer.valueOf (i+1));
          var name = variables.get (i);     
          LOGGER.fine ("Computing statistics [" + (i+1) + "/" + variables.size() + "] for " + name);
          DataVariable var;
          try { var = reader.getVariable (name); }
          catch (IOException e) { 
            LOGGER.log (Level.FINE, "Error getting variable " + name, e);
            continue;
          } // catch
          var constraints = new DataLocationConstraints();
          constraints.fraction = 0.01;
          var stats = VariableStatisticsGenerator.getInstance().generate (var, constraints);
          reader.putStatistics (name, stats);          
        } // for
      } // if

      return (reader);

    } // doInBackground

    @Override
    protected void process (List<Integer> chunks) {
      for (int value : chunks) statsProgressEvent (value);
    } // process

    @Override
    protected void done() {
      EarthDataReader reader;
      try { reader = get(); }
      catch (Exception e) { 
        LOGGER.log (Level.WARNING, "Error getting reader from background process", e);
        reader = null; 
      } // catch
      completeStatsComputation (reader);
      LOGGER.fine ("Statistics computation operation is now complete");
      statsOperation = null;
    } // done

  } // StatisticsComputationOperation class

  ////////////////////////////////////////////////////////////

  private void dialogOKEvent () {

    LOGGER.fine ("Detected a dialog OK event");
    startStatsComputation();

  } // dialogOKEvent

  ////////////////////////////////////////////////////////////

  private void dialogCancelEvent () {

    LOGGER.fine ("Detected a dialog cancel event");
    fileChooser.setSelectedFile (new File (""));

  } // dialogCancelEvent

  ////////////////////////////////////////////////////////////

  private JPanel createInputBlockingPanel () {

    var panel = new JPanel();
    panel.setOpaque (false);
    panel.addMouseListener (new MouseAdapter() {});
    panel.setInputVerifier (new InputVerifier() { 
      public boolean verify (JComponent input) { 
        return (!panel.isVisible());
      } // verify
    }); 

    return (panel);

  } // createInputBlockingPanel

  ////////////////////////////////////////////////////////////

  private JDialog createDialog (Component parent) {

    var okAction = GUIServices.createAction ("OK", () -> dialogOKEvent());
    okAction.setEnabled (false);
    addPropertyChangeListener (STATE_PROPERTY, event -> { okAction.setEnabled (event.getNewValue() == State.READY); });
    var cancelAction = GUIServices.createAction ("Cancel", () -> dialogCancelEvent());

    dialogInfoLabel = new JLabel();
    dialogProgressBar = new JProgressBar();
    if (!GUIServices.IS_AQUA) dialogProgressBar.setStringPainted (true);
    dialogInfoLabel.setVisible (false);
    dialogProgressBar.setVisible (false);

    var controls = new Component[] {
      GUIServices.getHelpButton (EarthDataReaderChooser.class),
      Box.createHorizontalGlue(),
      dialogInfoLabel,
      Box.createHorizontalStrut (5),
      dialogProgressBar,
      Box.createHorizontalGlue()
    };

    var dialog = GUIServices.createDialog (parent, "Open", true,
      this, controls, new Action[] {okAction, cancelAction}, 
      new boolean[] {false, true}, false);

    dialog.addWindowListener (new WindowAdapter() { 
      public void windowClosing (WindowEvent we) { dialogCancelEvent(); }
    });
    dialog.setGlassPane (createInputBlockingPanel());
    var size = dialog.getPreferredSize();
    size.height = 510;
    dialog.setSize (size);

    return (dialog);

  } // createDialog

  ////////////////////////////////////////////////////////////

  /**
   * Shows this chooser in a dialog window with an OK and Cancel button.
   * The user can select a file and variables of interest, and statistics
   * are computed for the variables.
   * 
   * @param parent the parent component to use for the dialog or null for no
   * parent.
   * @param selectedFile the file to initially show as selected, or null to
   * use the currently selected file.
   * 
   * @see #setSelectedFile
   */
  public void showDialog (
    Component parent,
    File selectedFile
  ) {

    // We start by creating a new dialog if one doesn't already exist.
    if (fileOpenDialog == null) fileOpenDialog = createDialog (parent);

    // In case we were used before for selecting a file, we set the
    // selected reader to null.  It's up to the code that used this 
    // chooser previously to close that reader, we shouldn't close it
    // here in case it's still being used.
    selectedReader = null;

    // Set the selected file so that the file chooser shows this file
    // as selected and also its file details.
    if (selectedFile != null) setSelectedFile (selectedFile);

    // Now open the file choosing dialog and set its location relative
    // to the parent.
    fileOpenDialog.setLocationRelativeTo (parent);
    fileOpenDialog.setVisible (true);

  } // showDialog

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the file selected in the chooser.
   * 
   * @param file the file to show selected.
   */
  public void setSelectedFile (File file) {

    fileChooser.rescanCurrentDirectory();
    if (file.exists() && file.isDirectory()) {
      LOGGER.fine ("Setting file chooser directory to " + file);
      fileChooser.setCurrentDirectory (file);
    } // if
    else {
      LOGGER.fine ("Setting file chooser selected file to " + file);
      fileChooser.setSelectedFile (file);
    } // else

  } // setSelectedFile

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    final File file = (argv.length > 0 ? new File (argv[0]) : null);
    SwingUtilities.invokeLater (() -> {
      var chooser = EarthDataReaderChooser.getInstance();
      chooser.showDialog (null, file);
      if (chooser.getState() == State.SELECTED) {
        var reader = chooser.getReader();
        var vars = reader.getStatisticsVariables();        
        LOGGER.fine ("Opened reader " + reader.getSource() + " with variables " + vars);
      } // if
      else {
        LOGGER.fine ("Reader choosing cancelled");
      } // else
      System.exit (0);
    });

  } // main

  ////////////////////////////////////////////////////////////

} // EarthDataReaderChooser class
