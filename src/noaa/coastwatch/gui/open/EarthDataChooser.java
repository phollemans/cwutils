////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataChooser.java
   Author: Peter Hollemans
     Date: 2005/06/23

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
  All rights reserved.

  Developed by: CoastWatch / OceanWatch
                Center for Satellite Applications and Research
                http://coastwatch.noaa.gov

  For conditions of distribution and use, see the accompanying
  license.txt file.

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import noaa.coastwatch.gui.EarthDataViewFactory;
import noaa.coastwatch.gui.EarthDataViewPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.SimpleFileFilter;
import noaa.coastwatch.gui.open.BasicReaderInfoPanel;
import noaa.coastwatch.gui.open.DataVariableTableModel;
import noaa.coastwatch.gui.open.FileChooser;
import noaa.coastwatch.gui.open.HTTPDirectoryLister;
import noaa.coastwatch.gui.open.NetworkFileChooser;
import noaa.coastwatch.gui.open.OpendapURLFilter;
import noaa.coastwatch.gui.open.THREDDSFileChooser;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.SolidBackground;
import noaa.coastwatch.tools.ResourceManager;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.SwathProjection;
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.VariableStatisticsGenerator;

/** 
 * The <code>EarthDataChooser</code> class allows the user to choose a
 * dataset and list of variables for opening earth locatable data.
 * Data must be in a format recognizable by the {@link
 * noaa.coastwatch.io.EarthDataReaderFactory} class.  Property changes
 * are used to signal a change in reader or list of variables names.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class EarthDataChooser 
  extends JPanel {

  // Constants
  // ---------

  /** The reader property. */
  public static final String READER_PROPERTY = "reader";

  /** The variable property. */
  public static final String VARIABLE_PROPERTY = "variable";

  /** The variable hint text. */
  private static final String VARIABLE_HINT = 
    "Select variables of interest and click OK to open";

  // Variables
  // ---------

  /** The file chooser for local files. */
  private JFileChooser fileChooser;

  /** The file chooser for network files. */
  private NetworkFileChooser networkChooser;

  /** The global reader info panel. */
  private BasicReaderInfoPanel readerInfoPanel;

  /** The variable table model. */
  private DataVariableTableModel variableModel;

  /** The variable table. */
  private JTable variableTable;

  /** The active earth data reader. */
  private EarthDataReader reader;

  /** The dialog created by the showDialog() method. */
  private static JDialog openDialog;

  /** The static chooser panel created by the showDialog() method. */
  private static EarthDataChooser chooser;

  /** The static return value used by the showDialog() method. */
  private static EarthDataReader retVal;

  /** The preview panel. */
  private EarthDataViewPanel previewPanel;

  /** The null preview earth view. */
  private static EarthDataView nullView;

  /** The coastline to use for the earth view. */
  private static CoastOverlay coast;

  /** The tabbed pane with file and network choosers. */
  private JTabbedPane tabbedPane;

  /** The timer for file information popup. */
  private Timer popupTimer;

  /** The popup for file information pending message. */
  private Popup popupMessage;
  
  /** The file chooser for the THREDDS server. */
  private THREDDSFileChooser threddsChooser;

  ////////////////////////////////////////////////////////////

  /** Creates a new chooser panel. */
  public EarthDataChooser () {

    setLayout (new BoxLayout (this, BoxLayout.X_AXIS));

    // Create tabbed pane
    // ------------------
    tabbedPane = new JTabbedPane();
    this.add (tabbedPane);

    // Create local file chooser
    // -------------------------
    fileChooser = GUIServices.getFileChooser();
    SimpleFileFilter filter = new SimpleFileFilter (
      new String[] {"hdf", "nc4", "nc"}, "CoastWatch data");
    fileChooser.addChoosableFileFilter (filter);
    fileChooser.setDialogType (JFileChooser.OPEN_DIALOG);
    fileChooser.addPropertyChangeListener (new LocalFileListener());
    fileChooser.setControlButtonsAreShown (false);
    tabbedPane.addTab ("Local", GUIServices.getIcon ("open.local"), 
      fileChooser);
    
    // Create THREDDS file chooser
    // ---------------------------
    threddsChooser = new THREDDSFileChooser(null);
    threddsChooser.addPropertyChangeListener (new THREDDSFileListener());


// FIXME: For now we disable the network and THREDDS file tabs because
// for there are no reliable servers for either one for testing.

/*
    tabbedPane.addTab ("THREDDS", GUIServices.getIcon ("open.network"),
      threddsChooser);
*/


    // Create network file chooser
    // ---------------------------
    HTTPDirectoryLister lister = new HTTPDirectoryLister();
    lister.setRefFilter (new OpendapURLFilter());
    networkChooser = new NetworkFileChooser (ResourceManager.getOpendapList(), 
      lister);
    networkChooser.addPropertyChangeListener (new NetworkFileListener());

/*
    tabbedPane.addTab ("Network", GUIServices.getIcon ("open.network"),
      networkChooser);
*/



    // Create information panel
    // ------------------------
    JPanel infoPanel = new JPanel (new GridLayout (2, 1));
    this.add (infoPanel);
    infoPanel.setPreferredSize (new Dimension (400, 0));

    // Create global info panel
    // ------------------------
    readerInfoPanel = new BasicReaderInfoPanel();
    infoPanel.add (readerInfoPanel);
    readerInfoPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Global Information"));

    // Create variable preview panel
    // -----------------------------
    JPanel variablePanel = new JPanel (new GridLayout (1, 2));
    infoPanel.add (variablePanel);

    // Create variable info panel
    // --------------------------
    variableModel = new DataVariableTableModel();
    variableTable = new JTable (variableModel);
    variableTable.getSelectionModel().addListSelectionListener (
      new VariableListener());
    JScrollPane scrollPane = new JScrollPane (variableTable);
    JPanel scrollPanePanel = new JPanel (new BorderLayout());
    scrollPanePanel.add (scrollPane, BorderLayout.CENTER);
    variablePanel.add (scrollPanePanel);
    scrollPanePanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Variables"));
    int rowHeight = variableTable.getRowHeight();
    int colWidth = 
      (int) new JLabel ("123456789012").getPreferredSize().getWidth();
    variableTable.setPreferredScrollableViewportSize (
      new Dimension (colWidth*2, rowHeight*10));

    // Create null earth view
    // ----------------------
    if (nullView == null) {
      try { nullView = new SolidBackground (Color.BLACK); }
      catch (Exception e) {
        e.printStackTrace();
      } // catch
    } // if

    // Create coast
    // ------------
    if (coast == null) {
      coast = new CoastOverlay (Color.WHITE);
    } // if

    // Create preview panel
    // --------------------
    JPanel previewContainer = new JPanel (new BorderLayout());
    previewContainer.setBorder (new TitledBorder (new EtchedBorder(), 
      "Preview"));
    variablePanel.add (previewContainer);
    previewPanel = new EarthDataViewPanel (nullView);
    previewPanel.setBackground (Color.BLACK);
    JPanel previewPanelContainer = new JPanel (new BorderLayout());
    previewPanelContainer.setBorder (new BevelBorder (BevelBorder.LOWERED));
    previewPanelContainer.add (previewPanel, BorderLayout.CENTER);
    previewContainer.add (previewPanelContainer, BorderLayout.CENTER);

    // Set initial display to null
    // ---------------------------
    updateFromLocalFile (null);

  } // EarthDataChooser constructor

  ////////////////////////////////////////////////////////////

  /** Gets the list of selected variable names. */
  private List getVariableList () {

    List list = new ArrayList();
    int[] rows = variableTable.getSelectedRows();
    for (int i = 0; i < rows.length; i++) {
      list.add (variableModel.getVariable (rows[i]).getName());
    } // for
    return (list);

  } // getVariableList

  ////////////////////////////////////////////////////////////

  /** Clears an active preview panel. */
  private void clearPreview () {

    previewPanel.setView (nullView);
    previewPanel.repaint();

  } // clearPreview  

  ////////////////////////////////////////////////////////////

  /** Responds to a selection change in the variable list. */
  private class VariableListener implements ListSelectionListener {
    public void valueChanged (ListSelectionEvent event) {

      // Check for adjusting
      // -------------------
      if (event.getValueIsAdjusting()) return;

      // Fire change in variable list
      // ----------------------------
      List variableList = getVariableList();
      firePropertyChange (VARIABLE_PROPERTY, null, variableList);

      // Update variable preview
      // -----------------------
      int listSize = variableList.size();
      if (listSize == 0) {
        clearPreview();
      } // if
      else if (listSize == 1) {
        clearPreview();
        String varName = (String) variableList.get (0);
        Cursor saved = variableTable.getCursor();
        variableTable.setCursor (Cursor.getPredefinedCursor (
          Cursor.WAIT_CURSOR));
        EarthDataView view = EarthDataViewFactory.create (reader, varName);
        if (view != null) {
          if (!(reader.getInfo().getTransform() instanceof SwathProjection))
            view.addOverlay (coast);
          previewPanel.setView (view);
          previewPanel.repaint();
        } // if
        variableTable.setCursor (saved);        
      } // else if

    } // valueChanged
  } // VariableListener

  ////////////////////////////////////////////////////////////

  /** 
   * Completes the opening of the reader by computing statistics
   * and closing any open dialogs.
   */
  private void openReader () {

    // Get longest variable name
    // -------------------------
    final List variableNames = getVariableList();
    String longestName = "";
    for (Iterator iter = variableNames.iterator(); iter.hasNext();) {
      String name = (String) iter.next();
      if (name.length() > longestName.length()) 
        longestName = name;
    } // for

    // Create progress monitor
    // -----------------------
    final JProgressBar bar = new JProgressBar (0, variableNames.size());
    String fileName = new File (reader.getSource()).getName();
    final JLabel note = new JLabel ("Computing statistics for " + longestName);
    JOptionPane pane = new JOptionPane (
      new Object[] {"Reading data from " + fileName, note, bar},
      JOptionPane.INFORMATION_MESSAGE);
    pane.setOptions (new Object[] {});
    final JDialog progressDialog = pane.createDialog (openDialog, 
      "Progress...");
    progressDialog.setDefaultCloseOperation (JDialog.DO_NOTHING_ON_CLOSE);
    note.setText (" ");

    // Set view panel to null
    // ----------------------
    /**
     * We do this so that the view panel has no further need to access
     * the file that is about to be closed and reopened by the stats
     * thread.  But we don't want any existing preview to go away
     * visually from the screen, so we don't call repaint().
     */
    previewPanel.setView (nullView);

    // Create statistics thread
    // ------------------------
    Thread statsThread = new Thread () {
        public void run () {

          // Reopen reader
          // -------------
          try { 
            reader.close();
            reader = EarthDataReaderFactory.create (reader.getSource());
          } // try
          catch (IOException e) {
            throw new RuntimeException ("Error reopening reader");
          } // catch

          // Loop over each variable name
          // ----------------------------
          for (int i = 0; i < variableNames.size(); i++) {

            // Update monitor
            // --------------
            final String name = (String) variableNames.get(i);
            final int progress = i;
            SwingUtilities.invokeLater (new Runnable () {
                public void run () {
                  note.setText ("Computing statistics for " + name);
                  bar.setValue (progress);
                } // run
              });

            // Compute stats
            // -------------
            DataVariable var;
            try { var = reader.getVariable (name); }
            catch (IOException e) { continue; }
            DataLocationConstraints lc = new DataLocationConstraints();
            lc.fraction = 0.01;
            Statistics stats = VariableStatisticsGenerator.getInstance().generate (var, lc);
            reader.putStatistics (name, stats);
            
          } // for

          // Dispose of dialogs
          // ------------------
          SwingUtilities.invokeLater (new Runnable () {
              public void run () {
                progressDialog.dispose();
                openDialog.dispose();
              } // run
            });
          
        } // run
      };
    statsThread.start();
    progressDialog.setVisible (true);

  } // openReader

  ////////////////////////////////////////////////////////////

  /** Responds to a change in the local file chooser. */
  private class LocalFileListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      String prop = event.getPropertyName();

      // Update with new file name
      // -------------------------
      if (prop.equals (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
        String name = null;
        File file = fileChooser.getSelectedFile();
        if (file != null) name = file.getPath();
        if (name != null && !name.equals (""))
          networkChooser.clearSelection();
        updateFromLocalFile (name);
      } // if

    } // propertyChange
  } // LocalFileListener class

  ////////////////////////////////////////////////////////////

  /** Responds to a change in the network file chooser. */
  private class NetworkFileListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      String prop = event.getPropertyName();

      // Update with new file
      // --------------------
      if (prop.equals (NetworkFileChooser.FILE_PROPERTY)) {
        String name = (String) event.getNewValue();
        if (name != null && !name.equals (""))
          fileChooser.setSelectedFile (new File (""));
        updateFromNetworkFile (name);
      } // if

    } // propertyChange
  } // NetworkFileListener class

  ////////////////////////////////////////////////////////////
  
  /** Responds to a change in the network file chooser. */
  private class THREDDSFileListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      String prop = event.getPropertyName();

      // Update with new file
      // --------------------
      if (prop.equals (THREDDSFileChooser.FILE_PROPERTY)) {
        String name = (String) event.getNewValue();
        if (name != null && !name.equals (""))
          //threddsChooser.setSelectedFile (new File (""));
        //updateFromNetworkFile (name);
        closeOldReader();
        reader = null;
        if (name == null) {
          updateDisplay();
          return;
        } // if

        // Otherwise try opening network file
        // ----------------------------------
        final String aname=new String(name);
        try { reader = EarthDataReaderFactory.create (aname); }
        catch (IOException e) { reader = null; }
        updateDisplay();
      } // if

    } // propertyChange
  } // NetworkFileListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Closes any existing reader.  The file information display
   * including info, variables, and preview are also cleared.  This
   * should be called prior to reassigning the reader so that we don't
   * have various readers lying around open.
   */
  private void closeOldReader () {

    if (reader != null) {
      clearPreview();
      readerInfoPanel.clear();
      variableModel.clear();
      try { 
        reader.close(); 
      } // try
      catch (IOException e) { 
        throw new RuntimeException ("Error closing data reader");
      } // catch
    } // if

  } // closeOldReader

  ////////////////////////////////////////////////////////////

  /**
   * Clears the selected reader.  Subsequent calls to {@link #getReader}
   * will return null.  
   * 
   * @param doClose true to close any existing reader, or false to
   * leave reader open.
   */
  public void clearReader (
    boolean doClose 
  ) {

    if (doClose) closeOldReader();
    reader = null; 
    networkChooser.clearSelection();
    fileChooser.setSelectedFile (new File (""));        

  } // clearReader

  ////////////////////////////////////////////////////////////

  /** Implements a glass pane that blocks mouse and keyboard events. */
  private static class BlockingGlassPane extends JPanel { 
    public BlockingGlassPane () { 

      setOpaque (false); 
      addMouseListener (new MouseAdapter() {});
      setInputVerifier (new InputVerifier() { 
          public boolean verify (JComponent input) { 
            return (!isVisible());
          } // verify
        }); 

    } // BlockingGlassPane constructor
  } // BlockingGlassPane class

  ////////////////////////////////////////////////////////////

  /** Updates the display from the specified local file. */ 
  private void updateFromLocalFile (final String name) {

    // Get new reader
    // --------------
    closeOldReader();
    reader = null;
    File file = (name == null ? null : new File (name));
    if (file != null && file.isFile() && file.exists()) {




// TODO: Clean this up and try to understand why it fails
// to remove the message in some cases.  This is the old source code:


/**



      // Create worker thread
      // --------------------
      Thread worker = new Thread () {
          public void run () {

            SwingUtilities.invokeLater (new Runnable() {
                public void run () {

                  // Create popup message
                  // --------------------
                  PopupFactory factory = PopupFactory.getSharedInstance();
                  JLabel label = new JLabel ("Reading file information ...");
                  label.setBorder (BorderFactory.createEmptyBorder (10, 10, 
                    10, 10));
                  Dimension labelSize = label.getPreferredSize();
                  Component parent = openDialog;
                  Point topLeft = parent.getLocationOnScreen();
                  popupMessage = factory.getPopup (parent, label,
                    topLeft.x + parent.getWidth()/2 - 
                      (int) labelSize.getWidth()/2,
                    topLeft.y + parent.getHeight()/2 - 
                      (int) labelSize.getHeight()/2);

                  // Set blocking glass pane to inhibit input
                  // ----------------------------------------
                  openDialog.getGlassPane().setVisible (true);
                  openDialog.getGlassPane().requestFocusInWindow();

                  // Start timer for popup
                  // ---------------------
                  Action showPopupAction = new AbstractAction() {
                      public void actionPerformed (ActionEvent e) {
                        popupMessage.show();
                      } // actionPerformed
                    };
                  popupTimer = new Timer (500, showPopupAction);
                  popupTimer.setRepeats (false);
                  popupTimer.start();
                  
                } // run
                
              });

            // Open data file
            // --------------
            SwathProjection.setNullMode (true);
            try { reader = EarthDataReaderFactory.create (name); }
            catch (IOException e) { }
            SwathProjection.setNullMode (false);
            
            // Finish up
            // ---------
            SwingUtilities.invokeLater (new Runnable() {
                public void run () {
                  if (popupTimer.isRunning())
                    popupTimer.stop();
                  else
                    popupMessage.hide();
                  updateDisplay();
                  openDialog.getGlassPane().setVisible (false);
                } // run
              });

          } // run
        };
      worker.start();


**/

      
      // Create worker thread
      // --------------------
      Thread worker = new Thread () {
          public void run () {

            SwingUtilities.invokeLater (new Runnable() {
                public void run () {

                  // Set blocking glass pane to inhibit input
                  // ----------------------------------------
                  openDialog.getGlassPane().setVisible (true);
                  openDialog.getGlassPane().requestFocusInWindow();
            
                } // run
                
              });

            // Start timer for popup
            // ---------------------
            
            Action showPopupAction = new AbstractAction() {
                public void actionPerformed (ActionEvent e) {
                        	// Create popup message
                            // --------------------
                	PopupFactory factory = PopupFactory.getSharedInstance();
                    JLabel label = new JLabel ("Reading file information ...");
                    label.setBorder (BorderFactory.createEmptyBorder (10, 10, 
                              10, 10));
                    Dimension labelSize = label.getPreferredSize();
                    Component parent = openDialog;
                    Point topLeft = parent.getLocationOnScreen();
                    popupMessage = factory.getPopup (parent, label,
                              topLeft.x + parent.getWidth()/2 - 
                                (int) labelSize.getWidth()/2,
                              topLeft.y + parent.getHeight()/2 - 
                                (int) labelSize.getHeight()/2);
                    popupMessage.show();
                } // actionPerformed
              };                
           
            popupTimer = new Timer (500, showPopupAction);
            popupTimer.setRepeats (false);
            popupTimer.start();

            // Open data file
            // --------------
            SwathProjection.setNullMode (true);
            try { reader = EarthDataReaderFactory.create (name); }
            catch (IOException e) { }
            SwathProjection.setNullMode (false);

            if (popupTimer.isRunning())
                popupTimer.stop();
            else
                if(popupMessage != null) popupMessage.hide();

            // Finish up
            // ---------
            SwingUtilities.invokeLater (new Runnable() {
                public void run () {
                  updateDisplay();
                  openDialog.getGlassPane().setVisible (false);
                } // run
              });

          } // run
        };
      worker.start();

    } // if

    // Otherwise, just update the display
    // ----------------------------------
    else {
      updateDisplay();
    } // else

  } // updateFromLocalFile

  ////////////////////////////////////////////////////////////

  /** Updates the display for the specified network file. */ 
  private void updateFromNetworkFile (final String name) {

    // Clear display if name is null
    // -----------------------------
    closeOldReader();
    reader = null;
    if (name == null) {
      updateDisplay();
      return;
    } // if

    // Otherwise try opening network file
    // ----------------------------------
    networkChooser.runTask (new FileChooser.Task () {
        public String getMessage () { 
          return ("Getting file information ..."); 
        } // getMessage
        public void run () throws IOException {
          try { reader = EarthDataReaderFactory.create (name); }
          catch (IOException e) { reader = null; }
        } // run
        public void followup () {
          updateDisplay();
        } // followup
      });

  } // updateFromNetworkFile

  ////////////////////////////////////////////////////////////
    
  /** Updates the display information for the newly opened reader. */
  private void updateDisplay () {

    // Get grid previews
    // -----------------
    List gridPreviews = null;
    if (reader != null) {
      try { 
        gridPreviews = new ArrayList();
        List gridNames = reader.getAllGrids();
        for (Iterator iter = gridNames.iterator(); iter.hasNext();)
          gridPreviews.add (reader.getPreview ((String) iter.next()));
      } // try
      catch (IOException e) { }
    } // if

    // Update info display
    // -------------------
    if (reader != null) {
      readerInfoPanel.setReader (reader);
      variableModel.setVariableList (gridPreviews);
    } // if
    else {
      readerInfoPanel.clear();
      variableModel.clear();
    } // else
    clearPreview();

    // Fire event
    // -----------
    firePropertyChange (READER_PROPERTY, null, reader);

  } // updateDisplay

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected reader, or null for none. */
  public EarthDataReader getReader () { return (reader); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates and shows a chooser in a dialog.  The dialog takes care
   * of saving any changes to the editable server table.
   *
   * @param component the dialog parent component.
   * @param selectedFile the file to initially show selected, or null
   * for none.
   *
   * @return the user-selected reader or null if the user cancelled
   * the dialog.
   */
  public static EarthDataReader showDialog (
    Component component,
    File selectedFile
  ) {

    // Create dialog
    // -------------
    if (openDialog == null) {

      // Create chooser panel
      // --------------------
      chooser = new EarthDataChooser();
      
      // Create hint label
      // -----------------
      final JLabel hintLabel = new JLabel ("");
      chooser.addPropertyChangeListener (READER_PROPERTY, 
        new PropertyChangeListener() {
          public void propertyChange (PropertyChangeEvent event) {
            boolean showHint = event.getNewValue() != null;
            hintLabel.setText (showHint ? VARIABLE_HINT : "");
          } // propertyChange
        });

      // Create OK action
      // ----------------
      final Action okAction = GUIServices.createAction ("OK", new Runnable() {
          public void run () { 
            chooser.openReader(); 
            retVal = chooser.getReader();
            chooser.clearReader (false);
          } // run
        });
      okAction.setEnabled (false);
      chooser.addPropertyChangeListener (VARIABLE_PROPERTY, 
        new PropertyChangeListener() {
          public void propertyChange (PropertyChangeEvent event) {
            boolean enabled = ((List) event.getNewValue()).size() != 0;
            okAction.setEnabled (enabled);
          } // propertyChange
        });

      // Create cancel action
      // --------------------
      Action cancelAction = GUIServices.createAction ("Cancel", new Runnable(){
          public void run () { 
            chooser.clearReader (true);
            retVal = null; 
          } // run
        });

      // Create chooser dialog
      // ---------------------
      Component[] controls = new Component[] {
        GUIServices.getHelpButton (EarthDataChooser.class),
        Box.createHorizontalGlue(),
        hintLabel,
        Box.createRigidArea (new Dimension (10, 0))
      };
      openDialog = GUIServices.createDialog (component, "Open", true,
        chooser, controls, new Action[] {okAction, cancelAction}, 
        new boolean[] {false, true}, false);

      // Set reader to null on window cancel
      // -----------------------------------
      openDialog.addWindowListener (new WindowAdapter() {
          public void windowClosing (WindowEvent we) { 
            chooser.clearReader (true);
            retVal = null; 
          } // windowClosing
        });
      openDialog.setGlassPane (new BlockingGlassPane());

    } // if

    // Update the directory listing
    // ----------------------------
    chooser.fileChooser.rescanCurrentDirectory();

    // Select initial file if requested
    // --------------------------------
    if (selectedFile != null) {
      chooser.fileChooser.setSelectedFile (selectedFile);
      chooser.tabbedPane.setSelectedComponent (chooser.fileChooser);
    } // if

    // Show the dialog and wait for return value
    // -----------------------------------------
    openDialog.setLocationRelativeTo (component);
    openDialog.setVisible (true);

    // Save any changes to the server list
    // -----------------------------------
    List serverList = chooser.networkChooser.getServerList();
    ResourceManager.setOpendapList (serverList);

    // Return a reader value
    // ---------------------
    /**
     * We do it this way so that there is no reference left in
     * the chooser to the newly opened reader after this function
     * returns.
     */
    EarthDataReader readerValue = retVal;
    retVal = null;
    return (readerValue);

  } // showDialog

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    final File file = (argv.length > 0 ? new File (argv[0]) : null);
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          EarthDataReader reader = EarthDataChooser.showDialog (null, file);
          System.out.println ("Got reader = " + reader);
          System.exit (0);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // EarthDataChooser class

////////////////////////////////////////////////////////////////////////
