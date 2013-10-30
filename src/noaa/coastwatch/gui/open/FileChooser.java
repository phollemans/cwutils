////////////////////////////////////////////////////////////////////////
/*
     FILE: FileChooser.java
  PURPOSE: Allows user to select a file.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/27
  CHANGES: 2006/03/15, PFH, modified to use GUIServices.getIconButton()

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.gui.open.DirectoryLister.Entry;

/**
 * The <code>FileChooser</code> class is a simplified version of the
 * Swing <code>JFileChooser</code> that allows the user to select a
 * file on any directory listing service supported by a {@link
 * DirectoryLister} object.  The chooser signals changes in the
 * current directory and file choice via property changes.  The file
 * property may have a null value if the user changes directories or
 * deselects the file.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class FileChooser
  extends JPanel {

  // Constants
  // ---------

  /** The up button command and tool tip. */
  private static final String UP_COMMAND = "Up";

  /** The refresh button command and tool tip. */
  private static final String REFRESH_COMMAND = "Refresh";

  /** The stop button command and tool tip. */
  private static final String STOP_COMMAND = "Stop";

  /** The directory entry property. */
  public static final String ENTRY_PROPERTY = "entry";

  /** The current directory property. */
  public static final String DIR_PROPERTY = "directory";

  /** The current status property. */
  public static final String STATUS_PROPERTY = "status";

  // Variables
  // ---------
  
  /** The lister to use for directory contents. */
  private DirectoryLister lister;

  /** The text field for the location string. */
  private JTextField locationField;

  /** The table of directories and files. */
  private JTable fileTable;

  /** The text field for the filter string. */
  private JTextField filterField;

  /** The table model for files and directories. */
  private FileTableModel fileModel;

  /** The current update worker thread. */
  private Thread worker;

  /** The ip navigation button. */
  private JButton upButton;

  /** The refresh navigation button. */
  private JButton refreshButton;

  /** The stop navigation button. */
  private JButton stopButton;
  
  /** The default panel cursor. */
  private Cursor normalCursor;

  ////////////////////////////////////////////////////////////
  
  /** Creates a new file chooser with no location. */
  public FileChooser (
    DirectoryLister lister
  ) {

    super (new BorderLayout());
    this.lister = lister;

    // Create button bar
    // -----------------
    Box buttonBar = new Box (BoxLayout.X_AXIS);
    ActionListener listener = new NavigationListener();

    upButton = GUIServices.getIconButton ("filenavigation.up");
    GUIServices.setSquare (upButton);
    upButton.setActionCommand (UP_COMMAND);
    upButton.addActionListener (listener);
    upButton.setToolTipText (UP_COMMAND);
    buttonBar.add (upButton);
    upButton.setEnabled (false);

    refreshButton = GUIServices.getIconButton ("filenavigation.refresh");
    GUIServices.setSquare (refreshButton);
    refreshButton.setActionCommand (REFRESH_COMMAND);
    refreshButton.addActionListener (listener);
    refreshButton.setToolTipText (REFRESH_COMMAND);
    buttonBar.add (refreshButton);
    refreshButton.setEnabled (false);

    stopButton = GUIServices.getIconButton ("filenavigation.stop");
    GUIServices.setSquare (stopButton);
    stopButton.setActionCommand (STOP_COMMAND);
    stopButton.addActionListener (listener);
    stopButton.setToolTipText (STOP_COMMAND);
    buttonBar.add (stopButton);
    stopButton.setEnabled (false);

    // Create location bar
    // -------------------
    JPanel locationBar = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
      GridBagConstraints.NONE, 0, 0);
    gc.insets = new Insets (2, 2, 2, 2);
    locationBar.add (new JLabel ("Location:"), gc);
    GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    locationField = new JTextField (15);
    locationField.addActionListener (new LocationEntered());
    locationBar.add (locationField, gc);
    GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
      GridBagConstraints.NONE, 0, 0);
    locationBar.add (buttonBar, gc);
    this.add (locationBar, BorderLayout.NORTH);

    // Create file table
    // -----------------
    fileTable = new FileTable();
    fileModel = (FileTableModel) fileTable.getModel();
    JScrollPane scrollPane = new JScrollPane (fileTable);
    this.add (scrollPane, BorderLayout.CENTER);
    fileTable.addMouseListener (new FileSelected());
    fileTable.getSelectionModel().addListSelectionListener (new FileChanged());

    // Create filter bar
    // -----------------
    JPanel filterBar = new JPanel (new GridBagLayout());
    filterBar.add (new JLabel ("File Filter:"), gc);
    GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    filterField = new JTextField (15);
    filterField.addActionListener (new FilterEntered());
    filterBar.add (filterField, gc);
    GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
      GridBagConstraints.NONE, 0, 0);
    JButton clearButton = new JButton ("Clear");
    clearButton.addActionListener (new ClearClicked());
    filterBar.add (clearButton, gc);
    this.add (filterBar, BorderLayout.SOUTH);

    // Set file table size
    // -------------------
    Dimension size = fileTable.getPreferredScrollableViewportSize();
    int rowHeight = fileTable.getRowHeight();
    size.height = rowHeight*10;
    fileTable.setPreferredScrollableViewportSize (size);

  } // FileChooser constructor

  ////////////////////////////////////////////////////////////

  /**
   * The <code>Task</code> class allows the user to run a file chooser
   * task via {@link FileChooser#runTask} in a separate thread from
   * the AWT event queue, and output status messages to the user.  The
   * task may be interrupted with the stop button.  An optional
   * follow-up for the task will be run in the AWT event queue only if
   * the task {@link #run} method did not throw an exception and the
   * stop button was not pressed.
   */
  public abstract static class Task {

    /** Gets the status message for the task start. */
    public abstract String getMessage();

    /** Performs the task. */
    public abstract void run() throws IOException;

    /** Performs the optional followup task (only if no exception). */
    public void followup() { }

  } // Task class

  ////////////////////////////////////////////////////////////

  /** Refreshes the entry list. */
  private void refresh () {

    runTask (new Task() {

        private DirectoryLister listerCopy;

        public String getMessage () { 
          return ("Refreshing directory contents ...");
        } // getMessage

        public void run () throws IOException {
          listerCopy = (DirectoryLister) lister.clone();
          listerCopy.refresh();
        } // run

        public void followup () {
          lister = listerCopy;
          updateFileTable();
         } // followup

      });

  } // refresh

  ////////////////////////////////////////////////////////////

  /** 
   * Runs the specified file chooser task in a separate thread.  See
   * the {@link Task} class for details.  Only one task may be running
   * at a time.  If a new task is started while an existing one is
   * running, the existing task's followup method is never called.
   */
  public void runTask (
    final Task task
  ) {

    // Save normal cursor
    // ------------------
    if (normalCursor == null) normalCursor = getCursor();

    // Create worker thread
    // --------------------
    worker = new Thread () {
        public void run () {

          // Perform pre-task startup
          // ------------------------
          SwingUtilities.invokeLater (new Runnable() {
              public void run() { 
                setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
                upButton.setEnabled (false);
                refreshButton.setEnabled (false);
                stopButton.setEnabled (true);
                firePropertyChange (STATUS_PROPERTY, null, task.getMessage());
              } // run
            });

          // Perform task
          // ------------
          IOException error = null;
          try { task.run(); }
          catch (IOException e) { error = e; }
          final Thread fThread = Thread.currentThread();
          final IOException fError = error;

          // Perform post-task followup
          // --------------------------
          SwingUtilities.invokeLater (new Runnable() {
              public void run() { 
                if (fThread == worker) {

                  // Perform followup
                  // ----------------
                  if (fError == null) {
                    task.followup();
                    firePropertyChange (STATUS_PROPERTY, null, "Done");
                  } // if

                  // Notify about error
                  // ------------------
                  else {
                    firePropertyChange (STATUS_PROPERTY, null, "Error: " + 
                      fError);
                  } // else

                  // Set cursor and buttons
                  // ----------------------
                  setCursor (normalCursor);
                  if (lister.getDirectory() != null) {
                    upButton.setEnabled (true);
                    refreshButton.setEnabled (true);
                  } // if
                  stopButton.setEnabled (false);

                } // if
              } // run
            });

        } // run
      };
    worker.start();

  } // runTask

  ////////////////////////////////////////////////////////////

  /**
   * Sets the directory name of the file chooser and updates the entry
   * list.
   *
   * @param name the new directory name.
   */
  public void setDirectory (
    final String name
  ) {

    runTask (new Task() {

        private DirectoryLister listerCopy;

        public String getMessage () {
          return ("Listing directory contents ...");
        } // getMessage

        public void run () throws IOException {
          listerCopy = (DirectoryLister) lister.clone();
          listerCopy.setDirectory (name);
        } // run

        public void followup () {
          lister = listerCopy;
          updateFileTable();
          firePropertyChange (DIR_PROPERTY, null, lister.getDirectory());
        } // followup

      });

  } // setDirectory

  ////////////////////////////////////////////////////////////

  /** Updates the file table and location field. */
  private void updateFileTable () {

    // Get filter expression
    // ---------------------
    String expression = filterField.getText().trim();
    if (!expression.equals ("")) {
      expression = expression.replaceAll ("\\.", "\\\\.");
      expression = expression.replaceAll ("\\*", ".*");
      expression = expression.replaceAll ("\\?", ".");
    } // if

    // Filter entry list
    // -----------------
    List entries = lister.getEntries();
    if (!expression.equals ("")) {
      for (Iterator iter = entries.iterator(); iter.hasNext();) {
        Entry entry = (Entry) iter.next();
        if (!entry.isDirectory() && !entry.getName().matches (expression))
          iter.remove();
      } // for
    } // if

    // Set entry list and location
    // ---------------------------
    Collections.sort (entries);
    fileModel.setEntryList (entries);
    String location = lister.getDirectory();
    if (location != null && !location.equals (locationField.getText()))
      locationField.setText (location);

  } // updateFileTable

  ////////////////////////////////////////////////////////////

  /** Responds to a new file selection. */
  private class FileChanged implements ListSelectionListener {
    public void valueChanged (ListSelectionEvent event) {

      if (event.getValueIsAdjusting()) return;
      Entry entry = null;
      int row = fileTable.getSelectedRow();
      if (row != -1) {
        entry = fileModel.getEntry (row);
        if (entry == FileTableModel.EMPTY) entry = null;
      } // if
      firePropertyChange (ENTRY_PROPERTY, null, entry);

    } // valueChanged
  } // FileChanged class

  ////////////////////////////////////////////////////////////

  /** Responds to a directory double-click. */
  private class FileSelected extends MouseAdapter {
     public void mouseClicked (MouseEvent event){

      if (event.getClickCount() == 2) {
        int row = fileTable.getSelectedRow();
        if (row == -1) return;
        Entry entry = fileModel.getEntry (row);
        if (entry.isDirectory()) {
          String newDir = lister.getChild (lister.getDirectory(),
            entry.getName());
          setDirectory (newDir);
        } // if
      } // if

     } // mouseClicked
  } // FileSelected

  ////////////////////////////////////////////////////////////

  /** Responds to a new location being entered in the location field. */
  private class LocationEntered extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      setDirectory (locationField.getText());

    } // actionPerformed
  } // LocationEntered class

  ////////////////////////////////////////////////////////////

  /** Responds to a new filter being entered in the filter field. */
  private class FilterEntered extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      updateFileTable();

    } // actionPerformed
  } // FilterEntered class

  ////////////////////////////////////////////////////////////

  /** Responds to the filter clear button being clicked. */
  private class ClearClicked extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      filterField.setText ("");
      updateFileTable();

    } // actionPerformed
  } // ClearClicked class

  ////////////////////////////////////////////////////////////

  /** Responds to a navigation button being clicked. */
  private class NavigationListener extends AbstractAction {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Go up one directory level
      // -------------------------
      if (command.equals (UP_COMMAND)) {
        String currentDir = lister.getDirectory();
        String newDir = lister.getParent (currentDir);
        if (!newDir.equals (currentDir)) setDirectory (newDir);
      } // if

      // Refresh the current directory contents
      // --------------------------------------
      else if (command.equals (REFRESH_COMMAND)) {
        refresh();
      } // else if

      // Stop the current listing
      // ------------------------
      else if (command.equals (STOP_COMMAND)) {
        setCursor (normalCursor);
        if (lister.getDirectory() != null) {
          upButton.setEnabled (true);
          refreshButton.setEnabled (true);
        } // if
        stopButton.setEnabled (false);
        worker = null;
        FileChooser.this.firePropertyChange (STATUS_PROPERTY, null, "Stopped");
      } // else if

    } // actionPerformed
  } // NavigationListener class

  ////////////////////////////////////////////////////////////

  /** Gets the current directory or null if no directory is set. */
  public String getDirectory () { return (lister.getDirectory()); }

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected file or null if none is selected. */
  public String getFile () { 

    int row = fileTable.getSelectedRow();
    if (row == -1) return (null);
    Entry entry = fileModel.getEntry (row);
    if (entry == FileTableModel.EMPTY) return (null);
    return (lister.getChild (lister.getDirectory(), entry.getName()));

  } // getFile

  ////////////////////////////////////////////////////////////

  /** Clears the current file table selection. */
  public void clearSelection () {

    fileTable.clearSelection();

  } // clearSelection

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) throws Exception {

    // Create chooser
    // --------------
    DirectoryLister lister = null;
    if (argv.length > 0) {
      if (argv[0].equals ("http"))
        lister = new HTTPDirectoryLister();
      else if (argv[0].equals ("opendap")) {
        HTTPDirectoryLister httpLister = new HTTPDirectoryLister();
        httpLister.setRefFilter (new OpendapURLFilter());
        lister = httpLister;
      } // else if
    } // if
    else 
      lister = new LocalDirectoryLister();
    FileChooser chooser = new FileChooser (lister);
    chooser.addPropertyChangeListener (new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          String name = event.getPropertyName();
          Object value = event.getNewValue();
          System.out.println ("Property change: " + name + " = " + value);
        } // propertyChange
      });

    // Create frame
    // ------------
    final JFrame frame = new JFrame (FileChooser.class.getName());
    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
    frame.setContentPane (chooser);
    frame.pack();

    // Show frame
    // ----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          frame.setVisible (true);
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // FileChooser class

////////////////////////////////////////////////////////////////////////
