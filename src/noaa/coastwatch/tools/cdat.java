////////////////////////////////////////////////////////////////////////
/*
     FILE: cdat.java
  PURPOSE: To view and analyze Earth data.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/10
  CHANGES: 2004/09/28, PFH, modified to use ToolServices.setCommandLine()
           2004/10/12, PFH, changed to use scrolling tab layout policy
           2005/01/30, PFH, added error dialog to replace standard error
           2005/02/04, PFH, changed "Options" to "Preferences"
           2005/03/14, PFH, reformatted documentation and usage note
           2005/04/04, PFH, added menu item icons
           2005/04/06, PFH, modified help dialog size
           2005/06/20, PFH, added minimized window icon
           2005/06/22, PFH, modified to use new open operation
           2005/07/04, PFH, modified to use local/network data chooser
           2005/07/07, PFH, added call to ResourceManager.checkResources()
           2005/07/07, PFH, added file information menu item
           2006/03/11, PFH, modified menu keymask for better Mac integration
           2006/03/16, PFH, added drag and drop event handling
           2006/03/31, PFH, added file open support on Mac
           2006/06/23, PFH, changed --nosplash to --splash command line option
           2006/10/19, PFH, changed save as to export
           2006/10/24, PFH, added icon for analysis tabs
           2006/10/30, PFH, increased default frame width for legend
           2006/11/03, PFH, added units preferences
           2006/11/04, PFH, changed to use LONG_NAME for GUI components
           2006/11/06, PFH, added context help support
           2006/12/14, PFH, added navigation analysis panel
           2007/04/19, PFH, added version printing
           2007/07/26, PFH, added full screen menu item
           2007/12/21, PFH, slightly increased frame size
           2011/05/16, XL, added menu items to save and load profiles of color 
                           enhancement information and overlays
           2014/08/11, PFH
           - Changes: Added command line parameter for window geometry.  Cleaned
             up profile load/save code.
           - Issue: We had a user feature request to be able to set the window 
             geometry.

  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.tools;

// Imports
// --------
import java.io.*;
import java.net.*;
import java.beans.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.gui.open.*;
import noaa.coastwatch.gui.save.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.render.*;
import jargs.gnu.*;
import jargs.gnu.CmdLineParser.*;

/**
 * <p>The analysis tool allows user to view, survey, and save datasets.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 *
 * <p>
 *   <!-- START NAME -->
 *   cdat - performs visual Earth data analysis.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>
 *   cdat [OPTIONS] input <br>
 *   cdat [OPTIONS]
 * </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -s, --splash <br>
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p>The CoastWatch Data Analysis Tool (CDAT) allows users to view,
 * survey, and save Earth datasets.  Detailed help on the usage of
 * CDAT is available from within the utility using the menu bar under
 * <i>Help | Help and Support</i>.</p>
 *
 * <h2>Parameters</h2>
 *
 * <h3>Main parameters:</h3>
 *
 * <dl>
 *
 *   <dt>input</dt>
 *   <dd>The input data file name.  If specified, the data file is
 *   opened immediately after CDAT starts.</dd>
 *
 * </dl>
 *
 * <h3>Options:</h3>
 *
 * <dl>
 *
 *   <dt>-h, --help</dt>
 *   <dd>Prints a brief help message.</dd>
 *
 *   <dt>-s, --splash</dt>
 *   <dd>Shows the splash window on startup.  The splash window
 *   shows the version, author, and web site information.</dd>
 *
 *   <dt>-g, --geometry=WxH</dt>
 *   <dd>The window geometry width and height in pixels.  The
 *   default is 900x700.</dd>
 *
 *   <dt>--version</dt>
 *
 *   <dd>Prints the software version.</dd>
 *
 * </dl>
 *
 * <h2>Exit status</h2>
 *
 * <p>0 on success, &gt; 0 on failure.  Possible causes of errors:</p>
 * <ul>
 *   <li> Invalid command line option. </li>
 *   <li> Invalid input file name. </li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <p>The following shows the use of CDAT to view 
 * data from a CoastWatch IMGMAP file:</p>
 * <pre>
 *   phollema$ cdat 2002_319_2144_n16_wl_c2.cwf
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public final class cdat
  extends JFrame {

  // Constants
  // ---------
  /** Minimum required command line parameters. */
  private static final int NARGS = 0;

  /** Name of program for command line. */
  private static final String PROG = "cdat";

  /** The short program name. */
  private static final String SHORT_NAME = "CDAT";

  /** The long program name. */
  private static final String LONG_NAME = 
    "CoastWatch Data Analysis Tool";

  /** The File/Open menu command. */
  private static final String OPEN_COMMAND = FileOperationChooser.OPEN;

  /** The File/Close menu command. */
  private static final String CLOSE_COMMAND = FileOperationChooser.CLOSE;
  
  /** The File/Save As menu command. */
  private static final String EXPORT_COMMAND = FileOperationChooser.EXPORT;

  /** The File/Quit menu command. */
  private static final String QUIT_COMMAND = "Quit";

  /** The Tools/Preferences menu command. */
  private static final String PREFS_COMMAND = "Preferences";
  
  /** The Tools/Profile/Save Profile menu command. **/
  private static final String SAVE_PROFILE_COMMAND = "Save Profile";

  /** The Tools/Profile/Load Profile menu command. **/
  private static final String LOAD_PROFILE_COMMAND = "Load Profile";

  /** The Tools/Navigation Analysis menu command. */
  private static final String NAV_ANALYSIS_COMMAND = "Navigation Analysis";

  /** The Tools/Full Screen Mode menu command. */
  private static final String FULL_SCREEN_COMMAND = "Full Screen " + 
    (!FullScreenWindow.isFullScreenSupported() ? "Emulation " : "") +
    "Mode";

  /** The Tools/File Information menu command. */
  private static final String INFO_COMMAND = "File Information";

  /** The Help/Help menu command. */
  private static final String HELP_COMMAND = "Help and support";

  /** The Help/About menu command. */
  private static final String ABOUT_COMMAND = "About " + LONG_NAME;

  /** The help index file. */
  private static final String HELP_INDEX = "cdat_index.html";

  /** The default application window size. */
  private static final String DEFAULT_FRAME_SIZE = "900x700";

  // Variables
  // ---------

  /** The file operation chooser. */
  private FileOperationChooser fileChooser;

  /** The view operation chooser. */
  private ViewOperationChooser viewChooser;

  /** The tabbed pane used to hold analysis panels. */
  private JTabbedPane tabbedPane;

  /** The close menu item. */
  private JMenuItem closeItem;

  /** The save as menu item. */
  private JMenuItem exportItem;

  /** The file information item. */
  private JMenuItem fileInfoItem;

  /** The navigation analysis item. */
  private JMenuItem navAnalysisItem;

  /** The full screen item. */
  private JMenuItem fullScreenItem;
  
  /** The save profile item. */
  private JMenuItem saveProfileItem;
  
  /** The load profile item. */
  private JMenuItem loadProfileItem;

  /** The handler for file drop operations. */
  private FileTransferHandler dropHandler;
  
  /** The file chooser for profile load/save. */
  private JFileChooser profileChooser;

  /** The help index URL. */
  private static URL helpIndex = cdat.class.getResource (HELP_INDEX);

  ////////////////////////////////////////////////////////////

  /** Creates a new CDAT frame. */
  public cdat () {

    // Initialize
    // ----------
    super (LONG_NAME);

    // Setup help support
    // ------------------
    GUIServices.setHelpIndex (helpIndex);

    // Create menu bar
    // ---------------
    JMenuBar menuBar = new JMenuBar();
    menuBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    this.setJMenuBar (menuBar);

    // TODO: Should we handle the About, Preferences, and Quit
    // commands differently for MacOS X?  Same for cwstatus and
    // cwmaster.  On Mac, these menu items are normally put into the
    // application menu.  We could do a MacOS detection, and then defer
    // to a Mac menubar setup routine for this.

    // Create file menu
    // ----------------
    JMenu fileMenu = new JMenu ("File");
    fileMenu.setMnemonic (KeyEvent.VK_F);
    menuBar.add (fileMenu);

    FileMenuListener fileListener = new FileMenuListener();
    JMenuItem menuItem;
    int keymask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    menuItem = new JMenuItem (OPEN_COMMAND, 
      GUIServices.getIcon ("menu.open"));
    menuItem.setMnemonic (KeyEvent.VK_O);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, keymask));
    menuItem.addActionListener (fileListener); 
    fileMenu.add (menuItem);

    menuItem = new JMenuItem (CLOSE_COMMAND, 
      GUIServices.getIcon ("menu.close"));
    menuItem.setMnemonic (KeyEvent.VK_C);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_W, keymask));
    menuItem.addActionListener (fileListener); 
    fileMenu.add (menuItem);
    closeItem = menuItem;

    menuItem = new JMenuItem (EXPORT_COMMAND, 
      GUIServices.getIcon ("menu.export"));
    menuItem.setMnemonic (KeyEvent.VK_E);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_E, keymask));
    menuItem.addActionListener (fileListener); 
    fileMenu.add (menuItem);
    exportItem = menuItem;

    if (!GUIServices.IS_AQUA) {
      fileMenu.addSeparator();
      menuItem = new JMenuItem (QUIT_COMMAND);
      menuItem.setMnemonic (KeyEvent.VK_Q);
      menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Q, keymask));
      menuItem.addActionListener (fileListener); 
      fileMenu.add (menuItem);
    } // if

    // Create tools menu
    // -----------------
    JMenu toolsMenu = new JMenu ("Tools");
    toolsMenu.setMnemonic (KeyEvent.VK_T);
    menuBar.add (toolsMenu);

    ToolsMenuListener toolsListener = new ToolsMenuListener();
    menuItem = new JMenuItem (PREFS_COMMAND, GUIServices.getIcon ("menu.prefs"));
    menuItem.setMnemonic (KeyEvent.VK_P);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_COMMA, keymask));
    menuItem.addActionListener (toolsListener);
    toolsMenu.add (menuItem);
    
    JMenu submenu = new JMenu ("Profile");
    menuItem.setMnemonic (KeyEvent.VK_R);
    toolsMenu.add (submenu);

    menuItem = saveProfileItem = new JMenuItem (SAVE_PROFILE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_S);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_S, keymask));
    menuItem.addActionListener (toolsListener);
    submenu.add (menuItem);

    menuItem = loadProfileItem = new JMenuItem (LOAD_PROFILE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_L);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_L, keymask));
    menuItem.addActionListener (toolsListener);
    submenu.add (menuItem);

    menuItem = fileInfoItem = new JMenuItem (INFO_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_I);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_I, keymask));
    menuItem.addActionListener (toolsListener); 
    toolsMenu.add (menuItem);

    menuItem = navAnalysisItem = new JMenuItem (NAV_ANALYSIS_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_N);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_N, keymask));
    menuItem.addActionListener (toolsListener); 
    toolsMenu.add (menuItem);

    menuItem = fullScreenItem = new JMenuItem (FULL_SCREEN_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_F);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_F, keymask));
    menuItem.addActionListener (toolsListener); 
    toolsMenu.add (menuItem);

    // Create help menu
    // ----------------
    JMenu helpMenu = new JMenu ("Help");
    helpMenu.setMnemonic (KeyEvent.VK_H);
    menuBar.add (helpMenu);

    HelpMenuListener helpListener = new HelpMenuListener();
    menuItem = new JMenuItem (HELP_COMMAND,
      GUIServices.getIcon ("menu.support"));
    menuItem.setMnemonic (KeyEvent.VK_H);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (
      KeyEvent.VK_F1, 0));
    menuItem.addActionListener (helpListener); 
    helpMenu.add (menuItem);
    helpMenu.addSeparator();

    menuItem = new JMenuItem (ABOUT_COMMAND, 
      GUIServices.getIcon ("menu.about"));
    menuItem.setMnemonic (KeyEvent.VK_A);
    menuItem.addActionListener (helpListener); 
    helpMenu.add (menuItem);

    // Create file chooser
    // -------------------
    fileChooser = FileOperationChooser.getInstance();
    fileChooser.addPropertyChangeListener (
      FileOperationChooser.OPERATION_PROPERTY, fileListener);

    // Create view chooser
    // -------------------
    viewChooser = ViewOperationChooser.getInstance(); 

    // Create profile chooser
    // ----------------------
    String currentDir = System.getProperty ("user.home");
    profileChooser = new JFileChooser (GUIServices.getPlatformDefaultDirectory());
    SimpleFileFilter filter = new SimpleFileFilter (
      new String[] {"profile"}, "CDAT profile");
    profileChooser.setFileFilter (filter);

    // Create tool bar
    // ---------------
    CompoundToolBar toolBar = 
      new CompoundToolBar (new JToolBar[] {fileChooser, viewChooser}, true);
    toolBar.setFloatable (false);
    toolBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    this.getContentPane().add (toolBar, BorderLayout.NORTH);

    // Create tabbed pane
    // ------------------
    tabbedPane = new JTabbedPane();
    tabbedPane.setTabLayoutPolicy (JTabbedPane.SCROLL_TAB_LAYOUT);
    this.getContentPane().add (tabbedPane, BorderLayout.CENTER);
    
    // Set initial enabled status
    // --------------------------
    updateEnabled();

    // Set minimized window icon
    // -------------------------
    setIconImage (GUIServices.getIcon ("tools.cdat").getImage());

    // Add drag and drop support
    // -------------------------
    Runnable runnable = new Runnable () {
        public void run () {
          openFile (dropHandler.getFile());
        } // run
      };
    dropHandler = new FileTransferHandler (runnable);
    tabbedPane.setTransferHandler (dropHandler);

    // Add file open support
    // ---------------------
    if (GUIServices.IS_MAC) {
      ActionListener listener = new ActionListener () {
          public void actionPerformed (ActionEvent event) {
            String name = event.getActionCommand();
            if (name != null) 
              openFile (new File (name));
          } // actionPerformed
        };
      GUIServices.addMacOpenFileListener (listener);
    } // if

  } // cdat constructor

  ////////////////////////////////////////////////////////////

  /** Handles tools menu commands. */
  private class ToolsMenuListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Show preferences dialog
      // -----------------------
      if (command.equals (PREFS_COMMAND)) {
        Preferences oldPrefs = ResourceManager.getPreferences();
        Preferences newPrefs = PreferencesChooser.showDialog (cdat.this,
          "Preferences", oldPrefs);
        if (newPrefs != null) {
          try { 
            ResourceManager.setPreferences (newPrefs);
            setEarthLocFormat (newPrefs);
            EarthDataReader.setUnitsMap (newPrefs.getUnitsMap());
          } // try
          catch (Exception e) {
            JOptionPane.showMessageDialog (cdat.this,
              "An error occurred writing the preferences file:\n" +
              e.toString() + "\n" + 
              "The preferences were not saved.", 
              "Error", JOptionPane.ERROR_MESSAGE);
          } // catch
        } // if
      } // if

      // Show info dialog
      // ----------------
      else if (command.equals (INFO_COMMAND)) {
        EarthDataReader reader = ((EarthDataAnalysisPanel) 
          tabbedPane.getSelectedComponent()).getReader();
        ReaderInfoPanel infoPanel = new ReaderInfoPanel (reader);
        infoPanel.showDialog (cdat.this, "File Information");
      } // else if

      // Show navigation analysis dialog
      // -------------------------------
      else if (command.equals (NAV_ANALYSIS_COMMAND)) {
        ((EarthDataAnalysisPanel) 
          tabbedPane.getSelectedComponent()).showNavAnalysisDialog();
      } // else if

      // Show navigation analysis dialog
      // -------------------------------
      else if (command.equals (FULL_SCREEN_COMMAND)) {
        ((EarthDataAnalysisPanel) 
          tabbedPane.getSelectedComponent()).showFullScreen();
      } // else if

      // Save profile
      // ------------
      else if (command.equals (SAVE_PROFILE_COMMAND)) {
    	int returnVal = profileChooser.showSaveDialog (cdat.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File saveFile = profileChooser.getSelectedFile();
          try {
            ((EarthDataAnalysisPanel)
              tabbedPane.getSelectedComponent()).saveProfile (saveFile);
          } // try
          catch (IOException e) {
            e.printStackTrace();
          } // catch
        } // if
      } // else if

      // Load profile
      // ------------
      else if (command.equals (LOAD_PROFILE_COMMAND)) {
    	int returnVal = profileChooser.showOpenDialog (cdat.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File loadFile = profileChooser.getSelectedFile();
          try {
            ((EarthDataAnalysisPanel)
              tabbedPane.getSelectedComponent()).loadProfile (loadFile);
          } // try
          catch (IOException e) {
            e.printStackTrace();
          } // catch
          catch (ClassNotFoundException e) {
            e.printStackTrace();
          } // catch
        } // if
      } // else if

    } // actionPerformed
  } // ToolsMenuListener class

  ////////////////////////////////////////////////////////////

  /** Handles help menu commands. */
  private class HelpMenuListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Show help dialog
      // ----------------
      if (command.equals (HELP_COMMAND)) {
        HTMLPanel helpPanel = new HTMLPanel (helpIndex, false);
        helpPanel.setPreferredSize (ToolServices.HELP_DIALOG_SIZE);
        helpPanel.showDialog (cdat.this, "Help");
      } // if

      // Show about dialog
      // -----------------
      else if (command.equals (ABOUT_COMMAND)) {
        JOptionPane.showMessageDialog (cdat.this, 
          ToolServices.getAbout (LONG_NAME), "About", 
          JOptionPane.INFORMATION_MESSAGE);
      } // else if

    } // actionPerformed
  } // HelpMenuListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the enabled status of the various components in response
   * to a tab addition or removal.
   */
  private void updateEnabled () {

    // Update toolbars
    // ---------------
    boolean enabled = (tabbedPane.getTabCount() != 0);
    fileChooser.setClosable (enabled);
    fileChooser.setSavable (enabled);
    viewChooser.setEnabled (enabled);

    // Update menu items
    // -----------------
    closeItem.setEnabled (enabled);
    exportItem.setEnabled (enabled);
    fileInfoItem.setEnabled (enabled);
    navAnalysisItem.setEnabled (enabled);
    fullScreenItem.setEnabled (enabled);
    saveProfileItem.setEnabled (enabled);
    loadProfileItem.setEnabled (enabled);

    // Update interaction mode
    // -----------------------
    for (int i = 0; i < tabbedPane.getTabCount(); i++)
      ((EarthDataAnalysisPanel) tabbedPane.getComponentAt (i)).
        resetInteraction();

  } // updateEnabled

  ////////////////////////////////////////////////////////////

  /** 
   * Opens a file and adds a new tab to the application.
   *
   * @param file the file to open, or null to allow the user to choose.
   */
  private void openFile (
    File file
  ) {

    // Get reader
    // ----------
    EarthDataReader reader = EarthDataChooser.showDialog (this, file);
    
    // Add analysis panel to tabbed pane
    // ---------------------------------
    if (reader != null) {
      List variables = reader.getStatisticsVariables();
      TabComponent tab = new EarthDataAnalysisPanel (reader, variables);
      tabbedPane.addTab (tab.getTitle(), tab.getIcon(), (Component) tab, 
        tab.getToolTip());
      tabbedPane.setSelectedComponent ((Component) tab);
      updateEnabled();
    } // if

  } // openFile

  ////////////////////////////////////////////////////////////

  /** Handles file menu commands. */
  private class FileMenuListener 
    implements ActionListener, PropertyChangeListener {

    /** Performs the specified action. */
    public void actionPerformed (ActionEvent event) {

      // Open new file
      // -------------
      String command = event.getActionCommand();
      if (command.equals (OPEN_COMMAND)) {
        openFile (null);
      } // if

      // Close file
      // ----------
      else if (command.equals (CLOSE_COMMAND)) {
        EarthDataAnalysisPanel analysisPanel = 
          (EarthDataAnalysisPanel) tabbedPane.getSelectedComponent();
        tabbedPane.remove (analysisPanel);
        analysisPanel.dispose();
        updateEnabled();
      } // if

      // Save to file
      // ------------
      else if (command.equals (EXPORT_COMMAND)) {
        EarthDataAnalysisPanel analysisPanel = 
          (EarthDataAnalysisPanel) tabbedPane.getSelectedComponent();
        EarthDataView view = analysisPanel.getView();
        EarthDataReader reader = analysisPanel.getReader();

        /*
        EarthDataSaveOperation operation = new EarthDataSaveOperation (
          view, reader.getInfo(), reader, reader.getStatisticsVariables());
        operation.perform (cdat.this);
        */
        EarthDataExporter exporter = new EarthDataExporter (
          view, reader.getInfo(), reader, reader.getStatisticsVariables());
        exporter.showDialog (cdat.this);

      } // else if

      // Quit program
      // ------------
      else if (command.equals (QUIT_COMMAND)) {
        System.exit (0);
      } // else if

    } // actionPerformed

    ////////////////////////////////////////////////////////

    /** Sends property changes to the action listener. */
    public void propertyChange (PropertyChangeEvent event) {
      actionPerformed (new ActionEvent (event.getSource(), 0, 
        (String) event.getNewValue()));
    } // propertyChange

    ////////////////////////////////////////////////////////

  } // FileMenuListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the Earth location formatting from the specified
   * preferences.
   *
   * @param prefs the preferences to use.
   */
  private static void setEarthLocFormat (
    Preferences prefs
  ) {

    int style = (prefs.getEarthLocDegrees() ? EarthLocation.DDDD : 
      EarthLocation.DDMMSS);
    EarthLocation.setFormatStyle (style);

  } // setEarthLocFormat

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.
   */
  public static void main (String argv[]) {

    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
    Option splashOpt = cmd.addBooleanOption ('s', "splash");
    Option geometryOpt = cmd.addStringOption ('g', "geometry");
    Option versionOpt = cmd.addBooleanOption ("version");
    Option memoryOpt = cmd.addBooleanOption ("memory");
    try { cmd.parse (argv); }
    catch (OptionException e) {
      System.err.println (PROG + ": " + e.getMessage());
      usage ();
      System.exit (1);
    } // catch

    // Print help message
    // ------------------
    if (cmd.getOptionValue (helpOpt) != null) {
      usage ();
      System.exit (0);
    } // if  

    // Print version message
    // ---------------------
    if (cmd.getOptionValue (versionOpt) != null) {
      System.out.println (ToolServices.getFullVersion (PROG));
      System.exit (0);
    } // if  

    // Get remaining arguments
    // -----------------------
    String[] remain = cmd.getRemainingArgs();
    if (remain.length < NARGS) {
      System.err.println (PROG + ": At least " + NARGS + 
        " argument(s) required");
      usage();
      System.exit (1);
    } // if
    final String input = (remain.length == 0 ? null : remain[0]);

    // Set defaults
    // ------------
    final boolean splash = (cmd.getOptionValue (splashOpt) != null);
    String geometry = (String) cmd.getOptionValue (geometryOpt);
    if (geometry == null) geometry = DEFAULT_FRAME_SIZE;

    // Get frame dimensions
    // --------------------
    String[] geometryArray = geometry.split ("x");
    if (geometryArray.length != 2) {
      System.err.println (PROG + ": Invalid geometry '" + geometry + "'");
      System.exit (2);
    } // if
    int width = 0;
    int height = 0;
    try {
      width = Integer.parseInt (geometryArray[0]);
      height = Integer.parseInt (geometryArray[1]);
    } // try
    catch (NumberFormatException e) {
      System.err.println (PROG + ": Error parsing geometry: " + e.getMessage());
      System.exit (2);
    } // catch
    final Dimension frameSize = new Dimension (width, height);

    // Create and show splash screen
    // -----------------------------
    if (splash) {
      GUIServices.invokeAndWait (new Runnable() {
        public void run() { 
          ToolSplashWindow.showSplash ("\n" + LONG_NAME);
        } // run
      });
      ToolSplashWindow.pauseSplash();
    } // if

    // Create and show frame
    // ---------------------
    SwingUtilities.invokeLater (new Runnable() {
      public void run() { 
        
        // Create main frame
        // -----------------
        final JFrame frame = new cdat();
        frame.addWindowListener (new WindowMonitor());
        frame.addWindowListener (new UpdateAgent (PROG));
        frame.pack();
        frame.setSize (frameSize);
        GUIServices.createErrorDialog (frame, "Error", 
          ToolServices.ERROR_INSTRUCTIONS);
        if (splash) ToolSplashWindow.hideSplash();

        // Check resources
        // ---------------
        try {
          String message = ResourceManager.checkResources();
          if (message != null) {
            JOptionPane.showMessageDialog (frame, message, "Warning", 
              JOptionPane.WARNING_MESSAGE);
          } // if
        } // try
        catch (Exception e) {
          JOptionPane.showMessageDialog (frame,
            "An error has been detected checking the user resource files:\n" +
            e.toString() + "\n" + 
            "Please correct the problem and try again.", 
            "Error", JOptionPane.ERROR_MESSAGE);
          System.exit (1);
        } // catch

        // Initialize resources
        // --------------------
        try {
          ResourceManager.setupPalettes();
          ResourceManager.setupOverlays();
          ResourceManager.getOverlayManager().getGroups();
          ResourceManager.setupPreferences();
          Preferences prefs = ResourceManager.getPreferences();
          setEarthLocFormat (prefs);
          EarthDataReader.setUnitsMap (prefs.getUnitsMap());
        } // try
        catch (Exception e) {
          JOptionPane.showMessageDialog (frame,
            "An error has been detected in the user resource files:\n" +
            e.toString() + "\n" + 
            "Please correct the problem and try again.", 
            "Error", JOptionPane.ERROR_MESSAGE);
          System.exit (1);
        } // catch

        // Show frame
        // ----------
        frame.setVisible (true);

        // Open file
        // ---------
        if (input != null)
          ((cdat) frame).openFile (new File (input).getAbsoluteFile());

      } // run
    });

    // Start memory monitoring (hidden option)
    // ---------------------------------------
    Boolean memory = (Boolean) cmd.getOptionValue (memoryOpt);
    if (memory != null && memory.booleanValue()) {
      ToolServices.startMemoryMonitor();
    } // if

  } // main

  ////////////////////////////////////////////////////////////

  /**
   * Prints a brief usage message.
   */
  private static void usage () {

    System.out.println (
"Usage: cdat [OPTIONS] input\n" +
"       cdat [OPTIONS]\n" +
"Allows users to view, survey, and save Earth datasets interactively.\n" +
"\n" +
"Main parameters:\n" +
"  input                      The initial input data file to open.\n" +
"\n" +
"Options:\n" +
"  -h, --help                 Show this help message.\n" +
"  -s, --splash               Show the splash screen.\n" +
"  -g, --geometry=WxH         Set width and height of the window.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

} // cdat class

////////////////////////////////////////////////////////////////////////
