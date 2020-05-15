////////////////////////////////////////////////////////////////////////
/*

     File: cdat.java
   Author: Peter Hollemans
     Date: 2004/05/10

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.tools;

// Imports
// --------
import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.OptionException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;

import noaa.coastwatch.gui.CloseIcon;
import noaa.coastwatch.gui.CompoundToolBar;
import noaa.coastwatch.gui.EarthDataAnalysisPanel;
import noaa.coastwatch.gui.FileOperationChooser;
import noaa.coastwatch.gui.FileTransferHandler;
import noaa.coastwatch.gui.FullScreenWindow;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.HTMLPanel;
import noaa.coastwatch.gui.PreferencesChooser;
import noaa.coastwatch.gui.ReaderInfoPanel;
import noaa.coastwatch.gui.SimpleFileFilter;
import noaa.coastwatch.gui.SplashScreenManager;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.gui.UpdateAgent;
import noaa.coastwatch.gui.ViewOperationChooser;
import noaa.coastwatch.gui.WindowMonitor;
import noaa.coastwatch.gui.open.EarthDataChooser;
import noaa.coastwatch.gui.save.EarthDataExporter;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.OverlayGroupManager;
import noaa.coastwatch.tools.Preferences;
import noaa.coastwatch.tools.ResourceManager;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.gui.ScriptConsole;

/**
 * <p>The analysis tool allows user to view, survey, and save datasets.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 *
 * <p>
 *   <!-- START NAME -->
 *   cdat - performs visual earth data analysis.
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
 * --version <br>
 * </p>
 *
 * <h2>Description</h2>
 *
 * <p>The CoastWatch Data Analysis Tool (CDAT) allows users to view,
 * survey, and save earth datasets.  Detailed help on the usage of
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
 *   <dt>-g, --geometry=WxH</dt>
 *   <dd>The window geometry width and height in pixels.  The
 *   default is 960x720.</dd>
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
 *   <li> Invalid command line option </li>
 *   <li> Invalid input file name </li>
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
  private static final String LONG_NAME = "CoastWatch Data Analysis Tool";

  /** The File/Open menu command. */
  private static final String OPEN_COMMAND = FileOperationChooser.OPEN;

  /** The File/Close menu command. */
  private static final String CLOSE_COMMAND = FileOperationChooser.CLOSE;
  
  /** The File/Save As menu command. */
  private static final String EXPORT_COMMAND = FileOperationChooser.EXPORT;

  /** The File/Quit menu command. */
  private static final String QUIT_COMMAND = "Quit";

  /** The View/Window Size small command. */
  private static final String WINDOW_SIZE_SMALL_COMMAND = "Small (960x720)";

  /** The View/Window Size medium command. */
  private static final String WINDOW_SIZE_MEDIUM_COMMAND = "Medium (1200x900)";

  /** The View/Window Size large command. */
  private static final String WINDOW_SIZE_LARGE_COMMAND = "Large (1440x1080)";

  /** The View/Data Size custom command. */
  private static final String WINDOW_SIZE_CUSTOM_COMMAND = "Custom Window Size";

  /** The View/Data Size small command. */
  private static final String DATA_SIZE_SMALL_COMMAND = "Small (512x512)";

  /** The View/Data Size medium command. */
  private static final String DATA_SIZE_MEDIUM_COMMAND = "Medium (768x768)";

  /** The View/Data Size large command. */
  private static final String DATA_SIZE_LARGE_COMMAND = "Large (1024x1024)";

  /** The View/Data Size custom command. */
  private static final String DATA_SIZE_CUSTOM_COMMAND = "Custom Data Size";

  /** The View/Full Screen Mode menu command. */
  private static final String FULL_SCREEN_COMMAND = "Full Screen Mode";

  /** The Tools/Preferences menu command. */
  private static final String PREFS_COMMAND = "Preferences";
  
  /** The Tools/Profile/Save Profile menu command. **/
  private static final String SAVE_PROFILE_COMMAND = "Save Profile";

  /** The Tools/Profile/Load Profile menu command. **/
  private static final String LOAD_PROFILE_COMMAND = "Load Profile";

  /** The Tools/Navigation Analysis menu command. */
  private static final String NAV_ANALYSIS_COMMAND = "Navigation Analysis";

  /** The Tools/File Information menu command. */
  private static final String INFO_COMMAND = "File Information";

  /** The Tools/Script Console menu command. */
  private static final String SCRIPT_CONSOLE_COMMAND = "Script Console";

  /** The Help/Help menu command. */
  private static final String HELP_COMMAND = "Help and support";

  /** The Help/About menu command. */
  private static final String ABOUT_COMMAND = "About " + LONG_NAME;

  /** The help index file. */
  private static final String HELP_INDEX = "cdat_index.html";

  /** The default application window size. */
  private static final Dimension DEFAULT_FRAME_SIZE = new Dimension (960, 720);

  // Variables
  // ---------

  /** The file operation chooser. */
  private FileOperationChooser fileChooser;

  /** The view operation chooser. */
  private ViewOperationChooser viewChooser;

  /** The compound toolbar. */
  private CompoundToolBar toolBar;

  /** The tabbed pane used to hold analysis panels. */
  private JTabbedPane tabbedPane;

  /** The list of menu items that need to be enabled/disabled when all tabs are closed. */
  private List<JMenuItem> menuItemDisableList = new ArrayList<JMenuItem>();

  /** The handler for file drop operations. */
  private FileTransferHandler dropHandler;
  
  /** The file chooser for profile load/save. */
  private JFileChooser profileChooser;

  /** The help index URL. */
  private static URL helpIndex = cdat.class.getResource (HELP_INDEX);
  
  /** The open recent menu item. */
  private JMenu openRecentMenu;
  
  /** The saved view center location. */
  private EarthLocation savedViewCenter;

  /** The saved view scale. */
  private double savedViewScale;
  
  ////////////////////////////////////////////////////////////

  /**
   * Rebuilds the open recent file menu to reflect the current list of
   * recent files.
   *
   * @since 3.4.0
   */
  private void rebuildRecentFilesMenu () {

    List<String> recentFilesList = GUIServices.getRecentlyOpenedFiles (cdat.class);

    // Add recently opened files
    // -------------------------
    openRecentMenu.removeAll();
    for (int i = 0; i < recentFilesList.size(); i++) {
      JMenuItem menuItem = new JMenuItem (new File (recentFilesList.get (i)).getName());
      final int index = i;
      menuItem.addActionListener (event -> openRecentFile (index));
      openRecentMenu.insert (menuItem, 0);
    } // for

    // Add clear menu item
    // -------------------
    JMenuItem menuItem = new JMenuItem ("Clear Menu");
    if (recentFilesList.size() == 0)
      menuItem.setEnabled (false);
    else {
      menuItem.addActionListener (event -> clearRecentFilesMenu());
      openRecentMenu.addSeparator();
    } // else
    openRecentMenu.add (menuItem);

  } // rebuildRecentFilesMenu

  ////////////////////////////////////////////////////////////

  /**
   * Resets the list of recently opened files to zero length and updates
   * the menu to be empty.
   *
   * @since 3.4.0
   */
  private void clearRecentFilesMenu () {

    GUIServices.setRecentlyOpenedFiles (new ArrayList<String>(), cdat.class);
    rebuildRecentFilesMenu();

  } // clearRecentFilesMenu
  
  ////////////////////////////////////////////////////////////

  /**
   * Opens a file from the recently opened files list.
   *
   * @param index the index of the file to open.
   *
   * @since 3.4.0
   */
  private void openRecentFile (
    int index
  ) {

    List<String> recentFilesList = GUIServices.getRecentlyOpenedFiles (cdat.class);
    File file = new File (recentFilesList.get (index));
    openFile (file);

  } // openRecentFile
  
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
    // to a Mac menubar setup routine for this.  See the java.awt.desktop
    // for the latest way to handle desktop environments.

    // Create file menu
    // ----------------
    JMenu fileMenu = new JMenu ("File");
    fileMenu.setMnemonic (KeyEvent.VK_F);
    menuBar.add (fileMenu);

    FileMenuListener fileListener = new FileMenuListener();
    JMenuItem menuItem;
    int keymask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
//    menuItem = new JMenuItem (OPEN_COMMAND, GUIServices.getIcon ("menu.open"));
    menuItem = new JMenuItem (OPEN_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_O);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, keymask));
    menuItem.addActionListener (fileListener); 
    fileMenu.add (menuItem);

    openRecentMenu = new JMenu ("Open Recent");
    fileMenu.add (openRecentMenu);
    rebuildRecentFilesMenu();

//    menuItem = new JMenuItem (CLOSE_COMMAND, GUIServices.getIcon ("menu.close"));
    menuItem = new JMenuItem (CLOSE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_C);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_W, keymask));
    menuItem.addActionListener (fileListener); 
    fileMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

//    menuItem = new JMenuItem (EXPORT_COMMAND, GUIServices.getIcon ("menu.export"));
    menuItem = new JMenuItem (EXPORT_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_E);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_E, keymask));
    menuItem.addActionListener (fileListener); 
    fileMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    if (!GUIServices.IS_AQUA) {
      fileMenu.addSeparator();
      menuItem = new JMenuItem (QUIT_COMMAND);
      menuItem.setMnemonic (KeyEvent.VK_Q);
      menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Q, keymask));
      menuItem.addActionListener (fileListener); 
      fileMenu.add (menuItem);
    } // if

    // Create view menu
    // ----------------
    JMenu viewMenu = new JMenu ("View");
    viewMenu.setMnemonic (KeyEvent.VK_V);
    menuBar.add (viewMenu);

    ViewMenuListener viewListener = new ViewMenuListener();
    JMenu submenu = new JMenu ("Window size");
    submenu.setMnemonic (KeyEvent.VK_S);
    viewMenu.add (submenu);

    menuItem = new JMenuItem (WINDOW_SIZE_SMALL_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_S);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_1, keymask));
    menuItem.addActionListener (viewListener);
    submenu.add (menuItem);

    menuItem = new JMenuItem (WINDOW_SIZE_MEDIUM_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_M);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_2, keymask));
    menuItem.addActionListener (viewListener);
    submenu.add (menuItem);

    menuItem = new JMenuItem (WINDOW_SIZE_LARGE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_L);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_3, keymask));
    menuItem.addActionListener (viewListener);
    submenu.add (menuItem);

    menuItem = new JMenuItem (WINDOW_SIZE_CUSTOM_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_C);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_4, keymask));
    menuItem.addActionListener (viewListener);
    submenu.add (menuItem);

/**
 * TODO: This functionality doesn't work yet.  We leave it here for now in
 * case it's needed in the future.  For now, it may be sufficient for just
 * the window size changes to be implemented.
 
    submenu = new JMenu ("Data size");
    submenu.setMnemonic (KeyEvent.VK_S);
    viewMenu.add (submenu);

    menuItem = new JMenuItem (DATA_SIZE_SMALL_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_S);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_1, keymask | InputEvent.SHIFT_DOWN_MASK));
    menuItem.addActionListener (viewListener);
    submenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem (DATA_SIZE_MEDIUM_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_M);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_2, keymask | InputEvent.SHIFT_DOWN_MASK));
    menuItem.addActionListener (viewListener);
    submenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem (DATA_SIZE_LARGE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_L);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_3, keymask | InputEvent.SHIFT_DOWN_MASK));
    menuItem.addActionListener (viewListener);
    submenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem (DATA_SIZE_CUSTOM_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_C);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_4, keymask | InputEvent.SHIFT_DOWN_MASK));
    menuItem.addActionListener (viewListener);
    submenu.add (menuItem);
    menuItemDisableList.add (menuItem);


*/


    menuItem = new JMenuItem (FULL_SCREEN_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_F);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_F, keymask));
    menuItem.addActionListener (viewListener);
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    viewMenu.addSeparator();

    menuItem = new JMenuItem ("Fit Image to Window");
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_0, keymask));
    menuItem.addActionListener (event -> viewChooser.performOperation (ViewOperationChooser.RESET));
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("Actual Size");
    menuItem.addActionListener (event -> viewChooser.performOperation (ViewOperationChooser.ONE_TO_ONE));
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("Fill Window");
    menuItem.addActionListener (event -> viewChooser.performOperation (ViewOperationChooser.FIT));
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("Zoom In");
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_EQUALS, keymask));
    menuItem.addActionListener (event -> viewChooser.performOperation (ViewOperationChooser.MAGNIFY));
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("Zoom Out");
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_MINUS, keymask));
    menuItem.addActionListener (event -> viewChooser.performOperation (ViewOperationChooser.SHRINK));
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("Zoom to Selection");
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Z, keymask));
    menuItem.addActionListener (event -> viewChooser.performOperation (ViewOperationChooser.ZOOM));
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    viewMenu.addSeparator();

    menuItem = new JMenuItem ("Copy View Zoom");
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_C, keymask | InputEvent.SHIFT_DOWN_MASK));
    menuItem.addActionListener (event -> saveViewCenterAndScale());
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("Paste View Zoom");
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_V, keymask | InputEvent.SHIFT_DOWN_MASK));
    menuItem.addActionListener (event -> restoreViewCenterAndScale());
    viewMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    viewMenu.addSeparator();

    JCheckBoxMenuItem checkBoxMenuItem;
    checkBoxMenuItem = new JCheckBoxMenuItem ("Show Tool bar");
    boolean isToolbarVisible = GUIServices.recallBooleanSettingForClass (true, "toolbar.visibility", cdat.class);
    checkBoxMenuItem.setState (isToolbarVisible);
    checkBoxMenuItem.addActionListener (event -> updateToolbarVisibility (event));
    viewMenu.add (checkBoxMenuItem);

    checkBoxMenuItem = new JCheckBoxMenuItem ("Show Control tabs");
    boolean areControlTabsVisible = GUIServices.recallBooleanSettingForClass (true, "controltabs.visibility", cdat.class);
    checkBoxMenuItem.setState (areControlTabsVisible);
    checkBoxMenuItem.addActionListener (event -> updateControlTabsVisibility (event));
    viewMenu.add (checkBoxMenuItem);

    // Create tools menu
    // -----------------
    JMenu toolsMenu = new JMenu ("Tools");
    toolsMenu.setMnemonic (KeyEvent.VK_T);
    menuBar.add (toolsMenu);

    ToolsMenuListener toolsListener = new ToolsMenuListener();
//    menuItem = new JMenuItem (PREFS_COMMAND, GUIServices.getIcon ("menu.prefs"));
    menuItem = new JMenuItem (PREFS_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_P);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_COMMA, keymask));
    menuItem.addActionListener (toolsListener);
    toolsMenu.add (menuItem);
    
    submenu = new JMenu ("Profile");
    submenu.setMnemonic (KeyEvent.VK_R);
    toolsMenu.add (submenu);

    menuItem = new JMenuItem (SAVE_PROFILE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_S);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_S, keymask));
    menuItem.addActionListener (toolsListener);
    submenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem (LOAD_PROFILE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_L);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_L, keymask));
    menuItem.addActionListener (toolsListener);
    submenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem (INFO_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_I);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_I, keymask));
    menuItem.addActionListener (toolsListener); 
    toolsMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem (NAV_ANALYSIS_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_N);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_N, keymask));
    menuItem.addActionListener (toolsListener); 
    toolsMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem (SCRIPT_CONSOLE_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_C);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_C, keymask));
    menuItem.addActionListener (toolsListener); 
    toolsMenu.add (menuItem);

    // Create help menu
    // ----------------
    JMenu helpMenu = new JMenu ("Help");
    helpMenu.setMnemonic (KeyEvent.VK_H);
    menuBar.add (helpMenu);

    HelpMenuListener helpListener = new HelpMenuListener();
//    menuItem = new JMenuItem (HELP_COMMAND, GUIServices.getIcon ("menu.support"));
    menuItem = new JMenuItem (HELP_COMMAND);
    menuItem.setMnemonic (KeyEvent.VK_H);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (
      KeyEvent.VK_F1, 0));
    menuItem.addActionListener (helpListener); 
    helpMenu.add (menuItem);
    helpMenu.addSeparator();

//    menuItem = new JMenuItem (ABOUT_COMMAND, GUIServices.getIcon ("menu.about"));
    menuItem = new JMenuItem (ABOUT_COMMAND);
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
    toolBar = new CompoundToolBar (new JToolBar[] {fileChooser, viewChooser}, true);
    toolBar.setFloatable (false);
    toolBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    toolBar.setVisible (isToolbarVisible);
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
    GUIServices.addOpenFileListener (event -> {
      String name = event.getActionCommand();
      if (name != null)
        openFile (new File (name));
    });

  } // cdat constructor

  ////////////////////////////////////////////////////////////

  /** Updates the visibility of the toolbar. */
  private void updateToolbarVisibility (ActionEvent event) {

    boolean flag = ((JCheckBoxMenuItem) event.getSource()).getState();
    toolBar.setVisible (flag);
    GUIServices.storeBooleanSettingForClass (flag, "toolbar.visibility", cdat.class);

  } // updateToolbarVisibility
  
  ////////////////////////////////////////////////////////////

  /** Updates the visibility of the control tabs. */
  private void updateControlTabsVisibility (ActionEvent event) {

    boolean flag = ((JCheckBoxMenuItem) event.getSource()).getState();
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      getAnalysisPanelAt (i).setTabbedPaneVisible (flag);
    } // for
    GUIServices.storeBooleanSettingForClass (flag, "controltabs.visibility", cdat.class);

  } // updateControlTabsVisibility
  
  ////////////////////////////////////////////////////////////

  /** Saves the view center and scale for later recall. */
  private void saveViewCenterAndScale () {

    EarthDataView view = getAnalysisPanel().getView();
    DataLocation center = view.getCenter();
    savedViewCenter = view.getTransform().getEarthTransform().transform (center);
    savedViewScale = view.getScale();
  
  } // saveViewCenterAndScale

  ////////////////////////////////////////////////////////////

  /** Saves the view center and scale for later recall. */
  private void restoreViewCenterAndScale () {

    if (savedViewCenter != null && savedViewScale != 0) {
      EarthDataView view = getAnalysisPanel().getView();
      DataLocation center = view.getTransform().getEarthTransform().transform (savedViewCenter);
      if (center.isValid()) {
        try {
          view.setCenterAndScale (center, savedViewScale);
          getAnalysisPanel().repaint();
        } // try
        catch (NoninvertibleTransformException e) {}
      } // if
    } // if
  
  } // restoreViewCenterAndScale
  
  ////////////////////////////////////////////////////////////

  /** 
   * Gets a specific analysis panel.
   *
   * @param index the analysis panel index.
   */
  private EarthDataAnalysisPanel getAnalysisPanelAt (int index) {
  
    return ((EarthDataAnalysisPanel) tabbedPane.getComponentAt (index));
    
  } // getAnalysisPanelAt
    
  ////////////////////////////////////////////////////////////

  /** Gets the active analysis panel. */
  private EarthDataAnalysisPanel getAnalysisPanel() {
  
    return ((EarthDataAnalysisPanel) tabbedPane.getSelectedComponent());
    
  } // getAnalysisPanel
    
  ////////////////////////////////////////////////////////////

  /** Handles view menu commands. */
  private class ViewMenuListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Change window size to small/medium/large
      // ----------------------------------------
      if (
        command.equals (WINDOW_SIZE_SMALL_COMMAND) ||
        command.equals (WINDOW_SIZE_MEDIUM_COMMAND) ||
        command.equals (WINDOW_SIZE_LARGE_COMMAND)
      ) {
        String sizeString = command.replaceFirst ("^.*\\(([0-9]+x[0-9]+)\\).*$", "$1");
        String[] sizeArray = sizeString.split ("x");
        try {
          int width = Integer.parseInt (sizeArray[0]);
          int height = Integer.parseInt (sizeArray[1]);
          Dimension frameSize = new Dimension (width, height);
          cdat.this.setSize (frameSize);
        } // try
        catch (NumberFormatException e) {}
      } // if
      
      // Change view size to custom
      // --------------------------
      else if (command.equals (WINDOW_SIZE_CUSTOM_COMMAND)) {

        JPanel dimPanel = new JPanel();
        dimPanel.setLayout (new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;

        Dimension size = cdat.this.getSize();
        NumberFormat format = NumberFormat.getIntegerInstance();
        format.setGroupingUsed (false);

        GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
        gc.insets = new Insets (2, 0, 2, 10);
        dimPanel.add (new JLabel ("Width:"), gc);
        gc.insets = new Insets (2, 0, 2, 0);
        GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.NONE, 1, 0);
        final JFormattedTextField widthField = new JFormattedTextField (format);
        widthField.setValue (size.width);
        widthField.setEditable (true);
        widthField.setColumns (8);
        dimPanel.add (widthField, gc);
        
        GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
        gc.insets = new Insets (2, 0, 2, 10);
        dimPanel.add (new JLabel ("Height:"), gc);
        gc.insets = new Insets (2, 0, 2, 0);
        GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.NONE, 1, 0);
        final JFormattedTextField heightField = new JFormattedTextField (format);
        heightField.setValue (size.height);
        heightField.setEditable (true);
        heightField.setColumns (8);
        dimPanel.add (heightField, gc);

        Action okAction = GUIServices.createAction ("OK", new Runnable() {
          public void run () {
            Dimension newSize = new Dimension();
            newSize.width = Integer.parseInt (widthField.getText());
            newSize.height = Integer.parseInt (heightField.getText());
            cdat.this.setSize (newSize);
          } // run
        });
        Action cancelAction = GUIServices.createAction ("Cancel", null);
        JDialog dialog = GUIServices.createDialog (
          cdat.this, "Set window size", true, dimPanel,
          null, new Action[] {okAction, cancelAction}, null, true);

        dialog.setVisible (true);

      } // else if

      // Change data size to small/medium/large
      // --------------------------------------

/*

      else if (
        command.equals (DATA_SIZE_SMALL_COMMAND) ||
        command.equals (DATA_SIZE_MEDIUM_COMMAND) ||
        command.equals (DATA_SIZE_LARGE_COMMAND)
      ) {
        String sizeString = command.replaceFirst ("^.*\\(([0-9]+x[0-9]+)\\).*$", "$1");
        String[] sizeArray = sizeString.split ("x");
        try {
          int width = Integer.parseInt (sizeArray[0]);
          int height = Integer.parseInt (sizeArray[1]);
          Dimension panelSize = new Dimension (width, height);


          // FIXME: This doesn't quite work!
          
          System.out.println ("setting panelSize = " + panelSize);
          
          getAnalysisPanel().setViewPanelSize (panelSize);
          cdat.this.pack();


        } // try
        catch (NumberFormatException e) {}
      } // if


*/






      // Show view full screen
      // ---------------------
      else if (command.equals (FULL_SCREEN_COMMAND)) {
        getAnalysisPanel().showFullScreen();
      } // else if

    } // actionPerformed
  } // ViewMenuListener class

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
        EarthDataReader reader = getAnalysisPanel().getReader();
        ReaderInfoPanel infoPanel = new ReaderInfoPanel (reader);
        infoPanel.showDialog (cdat.this, "File Information");
      } // else if

      // Show navigation analysis dialog
      // -------------------------------
      else if (command.equals (NAV_ANALYSIS_COMMAND)) {
        getAnalysisPanel().showNavAnalysisDialog();
      } // else if

      // Save profile
      // ------------
      else if (command.equals (SAVE_PROFILE_COMMAND)) {
    	int returnVal = profileChooser.showSaveDialog (cdat.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File saveFile = profileChooser.getSelectedFile();
          try {
            getAnalysisPanel().saveProfile (saveFile);
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
            getAnalysisPanel().loadProfile (loadFile);
          } // try
          catch (IOException e) {
            e.printStackTrace();
          } // catch
          catch (ClassNotFoundException e) {
            e.printStackTrace();
          } // catch
        } // if
      } // else if
      
      // Show script console
      // -------------------
      else if (command.equals (SCRIPT_CONSOLE_COMMAND)) {
        ScriptConsole.getInstance().showRelativeTo (cdat.this);
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
    for (JMenuItem item : menuItemDisableList) item.setEnabled (enabled);

    // Update interaction mode
    // -----------------------
    for (int i = 0; i < tabbedPane.getTabCount(); i++)
      getAnalysisPanelAt (i).resetInteraction();

  } // updateEnabled

  ////////////////////////////////////////////////////////////

  /**
   * Creates a tab panel with a close button that runs the action listener.
   *
   * @param title the title to use for the panel.
   * @param listener the action listener to run for the tab close button.
   *
   * @return the panel to use for the tab.
   *
   * @since 3.4.0
   */
  private JPanel getTabTitlePanel (
    String title,
    ActionListener listener
  ) {

    // Create the tab panel
    // --------------------
    JPanel tabPanel = new JPanel();
    tabPanel.setLayout (new BoxLayout (tabPanel, BoxLayout.X_AXIS));

    // Add the close button
    // --------------------
    int size = 14;
    JButton closeButton = new JButton (new CloseIcon (CloseIcon.Mode.NORMAL, size));
    closeButton.setRolloverIcon (new CloseIcon (CloseIcon.Mode.HOVER, size));
    closeButton.setPressedIcon (new CloseIcon (CloseIcon.Mode.PRESSED, size));
    closeButton.setOpaque (false);
    closeButton.setContentAreaFilled (false);
    closeButton.setBorderPainted (false);
    closeButton.setFocusPainted (false);
    closeButton.setMargin (new Insets (2, 2, 2, 2));
    closeButton.addActionListener (listener);
    tabPanel.add (closeButton);
    tabPanel.add (Box.createHorizontalStrut (5));
    
    // Add the title label
    // -------------------
    tabPanel.setOpaque (false);
    JLabel titleLabel = new JLabel (title);
    tabPanel.add (titleLabel);
    tabPanel.add (Box.createHorizontalStrut (5));

    return (tabPanel);
    
  } // getTabTitlePanel

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
    if (file == null) {
      String dir = GUIServices.recallStringSettingForClass (null, "last.directory", cdat.class);
      if (dir != null) file = new File (dir);
    } // if
    EarthDataReader reader = EarthDataChooser.showDialog (this, file);
    
    // Add analysis panel to tabbed pane
    // ---------------------------------
    if (reader != null) {

      // Create and show the analysis panel here.  We also set up the
      // control tabs according to the current preference setting.

      List<String> variables = reader.getStatisticsVariables();
      EarthDataAnalysisPanel panel = new EarthDataAnalysisPanel (reader, variables);
      boolean areControlTabsVisible = GUIServices.recallBooleanSettingForClass (true, "controltabs.visibility", cdat.class);
      panel.setTabbedPaneVisible (areControlTabsVisible);
      tabbedPane.addTab (panel.getTitle(), panel.getIcon(), panel, panel.getToolTip());
      JPanel tabTitlePanel = getTabTitlePanel (panel.getTitle(), event -> {
        tabbedPane.remove (panel);
        panel.dispose();
        updateEnabled();
      });
      tabbedPane.setTabComponentAt (tabbedPane.indexOfComponent (panel), tabTitlePanel);
      tabbedPane.setSelectedComponent (panel);
      updateEnabled();
      
      // Save the file opened to the recently opened list and rebuild the
      // menu for the list.  We also store the directory here so that we
      // can use it later to open the next file.
      
      String source = reader.getSource();
      GUIServices.addFileToRecentlyOpened (source, cdat.class, 10);
      rebuildRecentFilesMenu();
      String dir = new File (source).getParent();
      if (dir != null) GUIServices.storeStringSettingForClass (dir, "last.directory", cdat.class);

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
        EarthDataAnalysisPanel analysisPanel = getAnalysisPanel();
        tabbedPane.remove (analysisPanel);
        analysisPanel.dispose();
        updateEnabled();
      } // if

      // Save to file
      // ------------
      else if (command.equals (EXPORT_COMMAND)) {
        EarthDataAnalysisPanel analysisPanel = getAnalysisPanel();
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
   * Sets the earth location formatting from the specified
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

  /** Handles window resizes and saves the new window size. */
  private static class WindowResizeListener extends ComponentAdapter {
    public void componentResized (ComponentEvent event) {
      Dimension windowSize = event.getComponent().getSize();
      GUIServices.storeWindowSizeForClass (windowSize, cdat.class);
    } // componentResized
  } // WindowResizeListener class

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.
   */
  public static void main (String argv[]) {

    SplashScreenManager.updateSplash (LONG_NAME);
    ToolServices.setCommandLine (PROG, argv);

    // Parse command line
    // ------------------
    CmdLineParser cmd = new CmdLineParser ();
    Option helpOpt = cmd.addBooleanOption ('h', "help");
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
    String geometry = (String) cmd.getOptionValue (geometryOpt);
    Dimension lastFrameSize = GUIServices.recallWindowSizeForClass (cdat.class);
    if (lastFrameSize == null) lastFrameSize = DEFAULT_FRAME_SIZE;

    // Parse user-specified geometry
    // -----------------------------
    Dimension userFrameSize = null;
    if (geometry != null) {
      String[] geometryArray = geometry.split ("x");
      if (geometryArray.length != 2) {
        System.err.println (PROG + ": Invalid geometry '" + geometry + "'");
        System.exit (2);
      } // if
      try {
        int width = Integer.parseInt (geometryArray[0]);
        int height = Integer.parseInt (geometryArray[1]);
        userFrameSize = new Dimension (width, height);
      } // try
      catch (NumberFormatException e) {
        System.err.println (PROG + ": Error parsing geometry: " + e.getMessage());
        System.exit (2);
      } // catch
    } // if
    final Dimension frameSize = (userFrameSize != null ? userFrameSize : lastFrameSize);

    // Create and show frame
    // ---------------------
    SwingUtilities.invokeLater (new Runnable() {
      public void run() { 
        
        // Create main frame
        // -----------------
        final JFrame frame = new cdat();
        frame.addComponentListener (new WindowResizeListener());
        frame.addWindowListener (new WindowMonitor());
        frame.addWindowListener (new UpdateAgent (PROG));
        frame.pack();
        frame.setSize (frameSize);
        GUIServices.createErrorDialog (frame, "Error",
          ToolServices.ERROR_INSTRUCTIONS);

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

        // Check setup
        // -----------
        String errorStr = ToolServices.checkSetup();
        if (errorStr != null) {
          JOptionPane.showMessageDialog (frame,
            "An error has been detected in the setup check:\n" +
            errorStr + "\n" +
            "Please correct the problem and try again.",
            "Error", JOptionPane.ERROR_MESSAGE);
          System.exit (1);
        } // if

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
"Allows users to view, survey, and save earth datasets interactively.\n" +
"\n" +
"Main parameters:\n" +
"  input                      The initial input data file to open.\n" +
"\n" +
"Options:\n" +
"  -h, --help                 Show this help message.\n" +
"  -g, --geometry=WxH         Set width and height of the window.\n" +
"  --version                  Show version information.\n"
    );

  } // usage

  ////////////////////////////////////////////////////////////

} // cdat class

////////////////////////////////////////////////////////////////////////
