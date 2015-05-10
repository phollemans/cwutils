////////////////////////////////////////////////////////////////////////
/*
     FILE: NetworkFileChooser.java
  PURPOSE: Allows the user to choose files from a network connection.
   AUTHOR: Peter Hollemans
     DATE: 2005/07/01
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.open.DirectoryLister;
import noaa.coastwatch.gui.open.FileChooser;
import noaa.coastwatch.gui.open.HTTPDirectoryLister;
import noaa.coastwatch.gui.open.OpendapURLFilter;
import noaa.coastwatch.gui.open.ServerChooser;
import noaa.coastwatch.gui.open.ServerTableModel;

/** 
 * The <code>NetworkFileChooser</code> class allows the user to choose
 * a file from a network server.  Functionality is similar to a
 * <code>JFileChooser</code>, with the additional ability to select
 * from a list of abbreviated server names with location paths.  The
 * chooser is initialized using: 
 * <ul>
 *
 *   <li>A list of {@link ServerTableModel.Entry} objects for the
 *   names and locations of servers to show initially.  The user can
 *   edit the list of servers, so this list should be retrieved again
 *   when the chooser is done being used.</li>
 * 
 *   <li>A {@link DirectoryLister} object that knows how to connect to
 *   and list the contents of the server locations in the list.  The
 *   lister is used to drive a {@link FileChooser} object.</li>
 *
 * </ul>
 * Once a connection is established, the user may choose a file in the
 * directory listing.  When a new file is chosen, this class fires a
 * property change with the full file location and name as the value.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class NetworkFileChooser
  extends JPanel {

  // Constants
  // ---------

  /** The file selection property. */
  public static final String FILE_PROPERTY = "file";

  // Variables
  // ---------

  /** The directory lister used for the file chooser. */
  private DirectoryLister lister;

  /** The server chooser used for selecting the network server. */
  private ServerChooser serverChooser;

  /** The file chooser used for selecting directories and files. */
  private FileChooser fileChooser;

  /** The status field showing connection and listing status. */
  private JTextField statusField;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new chooser.
   * 
   * @param serverList the initial list of {@link
   * ServerTableModel.Entry} objects to display.
   * @param lister the directory lister to use for directory content
   * listing.
   */
  public NetworkFileChooser (
    List serverList,    
    DirectoryLister lister
  ) {

    super (new GridBagLayout());
    this.lister = lister;

    // Create server chooser
    // ---------------------
    serverChooser = new ServerChooser (serverList);
    serverChooser.setBorder (new TitledBorder (new EtchedBorder(), 
      "Servers"));
    serverChooser.addPropertyChangeListener (new ServerListener());
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.BOTH, 
      1, 0.5);
    this.add (serverChooser, gc);

    // Create file chooser
    // -------------------
    fileChooser = new FileChooser (lister);
    fileChooser.setBorder (new TitledBorder (new EtchedBorder(), 
      "Contents"));
    fileChooser.addPropertyChangeListener (new FileListener());
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.BOTH, 
      1, 1);
    this.add (fileChooser, gc);

    // Create status field
    // -------------------
    statusField = new JTextField();
    statusField.setEditable (false);
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.BOTH, 
      1, 0);
    this.add (statusField, gc);

  } // NetworkFileChooser constructor

  ////////////////////////////////////////////////////////////

  // Handles a new server connection request. */
  private class ServerListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      if (event.getPropertyName().equals (ServerChooser.SERVER_PROPERTY)) {
        ServerTableModel.Entry entry = 
          (ServerTableModel.Entry) event.getNewValue();
        fileChooser.setDirectory (entry.getLocation());
      } // if

    } // propertyChange
  } // ServerListener class

  ////////////////////////////////////////////////////////////

  // Handles a file chooser change. */
  private class FileListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      String prop = event.getPropertyName();

      // Update status
      // -------------
      if (prop.equals (FileChooser.STATUS_PROPERTY)) {
        statusField.setText ((String) event.getNewValue());
      } // if

      // Update file
      // -----------
      else if (prop.equals (FileChooser.ENTRY_PROPERTY)) {
        DirectoryLister.Entry entry = 
          (DirectoryLister.Entry) event.getNewValue();
        String name = null;
        if (entry != null && !entry.isDirectory()) {
          name = fileChooser.getFile();
        } // if
        NetworkFileChooser.this.firePropertyChange (FILE_PROPERTY, null, name);
      } // else if

    } // propertyChange
  } // FileListener class

  ////////////////////////////////////////////////////////////

  /** Runs a task in the file chooser. */
  public void runTask (FileChooser.Task task) { fileChooser.runTask (task); }

  ////////////////////////////////////////////////////////////

  /** Clears the current file table selection. */
  public void clearSelection () {

    fileChooser.clearSelection();

  } // clearSelection

  ////////////////////////////////////////////////////////////

  /** Gets the current list of server entries. */
  public List getServerList () { return (serverChooser.getServerList()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Create chooser
    // --------------
    HTTPDirectoryLister lister = new HTTPDirectoryLister();
    lister.setRefFilter (new OpendapURLFilter());
    List serverList = new LinkedList();
    serverList.add (new ServerTableModel.Entry ("CoastWatch Caribbean/GOM",
      "http://cwcaribbean.aoml.noaa.gov/cgi-bin/DODS/nph-dods/cwatch-web/data"));
    serverList.add (new ServerTableModel.Entry ("USF MODIS", 
      "http://www.imars.usf.edu/dods-bin/nph-dods/modis"));
    serverList.add (new ServerTableModel.Entry ("USF AVHRR",
      "http://www.imars.usf.edu/dods-bin/nph-dods/SST_NEW/husf/products"));
    NetworkFileChooser chooser = new NetworkFileChooser (serverList, lister);
    chooser.addPropertyChangeListener (new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          String name = event.getPropertyName();
          Object value = event.getNewValue();
          System.out.println ("Property change: " + name + " = " + value);
        } // propertyChange
      });

    // Create frame
    // ------------
    final JFrame frame = new JFrame (NetworkFileChooser.class.getName());
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

} // NetworkFileChooser class

////////////////////////////////////////////////////////////////////////
