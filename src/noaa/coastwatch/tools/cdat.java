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
import java.awt.Desktop;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import noaa.coastwatch.gui.ReaderMetadataPanel;
import noaa.coastwatch.gui.SimpleFileFilter;
import noaa.coastwatch.gui.SplashScreenManager;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.gui.UpdateAgent;
import noaa.coastwatch.gui.ViewOperationChooser;
import noaa.coastwatch.gui.HelpOperationChooser;
import noaa.coastwatch.gui.WindowMonitor;
import noaa.coastwatch.gui.open.EarthDataReaderChooser;
import noaa.coastwatch.gui.open.EarthDataReaderChooser.State;
import noaa.coastwatch.gui.save.EarthDataExporter;
import noaa.coastwatch.gui.HelpOperationChooser;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.OverlayGroupManager;
import noaa.coastwatch.tools.Preferences;
import noaa.coastwatch.tools.ResourceManager;
import noaa.coastwatch.tools.ToolServices;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.gui.ScriptConsole;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>The analysis tool allows user to view, survey, and save datasets.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 *
 * <p>
 *   <!-- START NAME -->
 *   cdat - performs interactive earth data analysis.
 *   <!-- END NAME -->
 * </p>
 *
 * <h2>Synopsis</h2>
 *
 * <p>
 *   cdat [OPTIONS] [input]
 * </p>
 *
 * <h3>Options:</h3>
 *
 * <p>
 * -h, --help <br>
 * -g, --geometry=WxH <br>
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
 *   <dd>The optional input data file name.  If specified, the data file is
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
 * data from a CoastWatch HDF file:</p>
 * <pre>
 *   phollema$ cdat 2002_319_2144_n16_wl_c2.hdf
 * </pre>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public final class cdat
  extends JFrame {

  private static final String PROG = cdat.class.getName();
  private static final Logger LOGGER = Logger.getLogger (PROG);
  private static final Logger VERBOSE = Logger.getLogger (PROG + ".verbose");

  // Constants
  // ---------

  /** Minimum required command line parameters. */
  private static final int NARGS = 0;

  /** The short program name. */
  private static final String SHORT_NAME = "CDAT";

  /** The long program name. */
  private static final String LONG_NAME = "CoastWatch Data Analysis Tool";

  /** The URL to use for the online course. */
  private static final String COURSE_URL = "https://umd.instructure.com/courses/1336575/pages/coastwatch-utilities-tutorials";

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

  /** The help operation chooser. */
  private HelpOperationChooser helpChooser;

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
    this.setJMenuBar (menuBar);

    // TODO: Should we handle the About, Preferences, and Quit
    // commands differently for MacOS X?  Same for cwstatus and
    // cwmaster.  On Mac, these menu items are normally put into the
    // application menu.  We could do a MacOS detection, and then defer
    // to a Mac menubar setup routine for this.  See the java.awt.desktop
    // for the latest way to handle desktop environments.

    // Create the file menu with open, close, export, etc.

    JMenu fileMenu = new JMenu ("File");
    fileMenu.setMnemonic (KeyEvent.VK_F);
    menuBar.add (fileMenu);

    JMenuItem menuItem;
    int keymask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    menuItem = new JMenuItem (FileOperationChooser.OPEN);
    menuItem.setMnemonic (KeyEvent.VK_O);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_O, keymask));
    menuItem.addActionListener (event -> openFileEvent());
    fileMenu.add (menuItem);

    openRecentMenu = new JMenu ("Open Recent");
    fileMenu.add (openRecentMenu);
    rebuildRecentFilesMenu();

    menuItem = new JMenuItem (FileOperationChooser.CLOSE);
    menuItem.setMnemonic (KeyEvent.VK_C);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_W, keymask));
    menuItem.addActionListener (event -> closeFileEvent());
    fileMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem (FileOperationChooser.EXPORT);
    menuItem.setMnemonic (KeyEvent.VK_E);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_E, keymask));
    menuItem.addActionListener (event -> exportFileEvent()); 
    fileMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    if (!GUIServices.IS_AQUA) {
      fileMenu.addSeparator();
      menuItem = new JMenuItem ("Quit");
      menuItem.setMnemonic (KeyEvent.VK_Q);
      menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_Q, keymask));
      menuItem.addActionListener (event -> quitEvent());
      fileMenu.add (menuItem);
    } // if

    // Create the view menu with window size, full screen, view controls, etc.

    JMenu viewMenu = new JMenu ("View");
    viewMenu.setMnemonic (KeyEvent.VK_V);
    menuBar.add (viewMenu);

    JMenu submenu = new JMenu ("Window Size");
    submenu.setMnemonic (KeyEvent.VK_S);
    viewMenu.add (submenu);

    var smallWindow = "Small (960x720)";
    menuItem = new JMenuItem (smallWindow);
    menuItem.setMnemonic (KeyEvent.VK_S);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_1, keymask));
    menuItem.addActionListener (event -> windowSizeEvent (new Dimension (960, 720)));
    submenu.add (menuItem);

    menuItem = new JMenuItem ("Medium (1200x900)");
    menuItem.setMnemonic (KeyEvent.VK_M);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_2, keymask));
    menuItem.addActionListener (event -> windowSizeEvent (new Dimension (1200, 900)));
    submenu.add (menuItem);

    menuItem = new JMenuItem ("Large (1440x1080)");
    menuItem.setMnemonic (KeyEvent.VK_L);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_3, keymask));
    menuItem.addActionListener (event -> windowSizeEvent (new Dimension (1440, 1080)));
    submenu.add (menuItem);

    menuItem = new JMenuItem ("Custom Window Size");
    menuItem.setMnemonic (KeyEvent.VK_C);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_4, keymask));
    menuItem.addActionListener (event -> windowCustomSizeEvent());
    submenu.add (menuItem);

    menuItem = new JMenuItem ("Full Screen Mode");
    menuItem.setMnemonic (KeyEvent.VK_F);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_F, keymask));
    menuItem.addActionListener (event -> fullScreenEvent());
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

    submenu = new JMenu ("Tool bar");
    viewMenu.add (submenu);

    JCheckBoxMenuItem checkBoxMenuItem;
    checkBoxMenuItem = new JCheckBoxMenuItem ("Show bar");
    boolean isToolbarVisible = GUIServices.recallBooleanSettingForClass (true, "toolbar.visibility", cdat.class);
    checkBoxMenuItem.setState (isToolbarVisible);
    checkBoxMenuItem.addActionListener (event -> updateToolbarVisibility (event));
    submenu.add (checkBoxMenuItem);

    checkBoxMenuItem = new JCheckBoxMenuItem ("Show text labels");
    boolean isToolbarLabelVisible = GUIServices.recallBooleanSettingForClass (false, "toolbar.label.visibility", cdat.class);
    checkBoxMenuItem.setState (isToolbarLabelVisible);
    checkBoxMenuItem.addActionListener (event -> updateToolbarLabelVisibility (event));
    submenu.add (checkBoxMenuItem);

    checkBoxMenuItem = new JCheckBoxMenuItem ("Show Control tabs");
    boolean areControlTabsVisible = GUIServices.recallBooleanSettingForClass (true, "controltabs.visibility", cdat.class);
    checkBoxMenuItem.setState (areControlTabsVisible);
    checkBoxMenuItem.addActionListener (event -> updateControlTabsVisibility (event));
    viewMenu.add (checkBoxMenuItem);

    // Create the tools menu with preferences, profiles, file info, etc.

    JMenu toolsMenu = new JMenu ("Tools");
    toolsMenu.setMnemonic (KeyEvent.VK_T);
    menuBar.add (toolsMenu);

    submenu = new JMenu ("Preferences");
    toolsMenu.add (submenu);    

    menuItem = new JMenuItem ("Edit Preferences");
    menuItem.setMnemonic (KeyEvent.VK_P);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_COMMA, keymask));
    menuItem.addActionListener (event -> editPreferencesEvent());
    submenu.add (menuItem);
    
    menuItem = new JMenuItem ("Open User Resources Directory");
    menuItem.addActionListener (event -> openResourcesEvent());
    submenu.add (menuItem);

    submenu = new JMenu ("Profile");
    submenu.setMnemonic (KeyEvent.VK_R);
    toolsMenu.add (submenu);

    menuItem = new JMenuItem ("Load Profile");
    menuItem.setMnemonic (KeyEvent.VK_L);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_L, keymask));
    menuItem.addActionListener (event -> loadProfileEvent());
    submenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("Save Profile");
    menuItem.setMnemonic (KeyEvent.VK_S);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_S, keymask));
    menuItem.addActionListener (event -> saveProfileEvent());
    submenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("File Information");
    menuItem.setMnemonic (KeyEvent.VK_I);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_I, keymask));
    menuItem.addActionListener (event -> fileInfoEvent());; 
    toolsMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    menuItem = new JMenuItem ("Navigation Analysis");
    menuItem.setMnemonic (KeyEvent.VK_N);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_N, keymask));
    menuItem.addActionListener (event -> navAnalysisEvent()); 
    toolsMenu.add (menuItem);
    menuItemDisableList.add (menuItem);

    // Create the help menu with help, about, online course, etc.

    JMenu helpMenu = new JMenu ("Help");
    helpMenu.setMnemonic (KeyEvent.VK_H);
    menuBar.add (helpMenu);

    menuItem = new JMenuItem ("Help and Support");
    menuItem.setMnemonic (KeyEvent.VK_H);
    menuItem.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_F1, 0));
    menuItem.addActionListener (event -> showHelpEvent()); 
    helpMenu.add (menuItem);

    menuItem = new JMenuItem ("Open Online Course");
    menuItem.addActionListener (event -> openCourseEvent());
    helpMenu.add (menuItem);

    helpMenu.addSeparator();

    menuItem = new JMenuItem ("About " + LONG_NAME);
    menuItem.setMnemonic (KeyEvent.VK_A);
    menuItem.addActionListener (event -> showAboutEvent()); 
    helpMenu.add (menuItem);

    // Create the file chooser toolbar
    fileChooser = FileOperationChooser.getInstance();
    fileChooser.addPropertyChangeListener (FileOperationChooser.OPERATION_PROPERTY,
      new PropertyChangeAdapter (Map.of (
        FileOperationChooser.OPEN, () -> openFileEvent(),
        FileOperationChooser.CLOSE, () -> closeFileEvent(),
        FileOperationChooser.EXPORT, () -> exportFileEvent(),
        FileOperationChooser.INFO, () -> fileInfoEvent()
      )
    ));

    // Create the view chooser toolbar.
    viewChooser = ViewOperationChooser.getInstance(); 

    // Create the help chooser toolbar.
    helpChooser = HelpOperationChooser.getInstance();
    helpChooser.addPropertyChangeListener (HelpOperationChooser.OPERATION_PROPERTY,
      new PropertyChangeAdapter (Map.of (
        HelpOperationChooser.HELP, () -> showHelpEvent(),
        HelpOperationChooser.PREFERENCES, () -> editPreferencesEvent(),
        HelpOperationChooser.COURSE, () -> openCourseEvent()
      )
    ));

    // Create the profile chooser that saves and loads profiles of
    // CDAT enhancements and overlays.
    String currentDir = System.getProperty ("user.home");
    profileChooser = new JFileChooser (GUIServices.getPlatformDefaultDirectory());
    SimpleFileFilter filter = new SimpleFileFilter (
      new String[] {"profile"}, "CDAT profile");
    profileChooser.setFileFilter (filter);

    // Combine the various tool bars into a compound one that holds them all.
    toolBar = new CompoundToolBar (new JToolBar[] {fileChooser, viewChooser, helpChooser}, true);
    toolBar.setFloatable (false);
    toolBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    updateToolbarLabelVisibility (isToolbarLabelVisible);
    toolBar.setVisible (isToolbarVisible);
    this.getContentPane().add (toolBar, BorderLayout.NORTH);

    // Create the tabbed pane.
    tabbedPane = new JTabbedPane();
    tabbedPane.setTabLayoutPolicy (JTabbedPane.SCROLL_TAB_LAYOUT);
    this.getContentPane().add (tabbedPane, BorderLayout.CENTER);
    
    // Set all the various GUI elements to their initialk enabled/disabled
    // modes.
    updateEnabled();

    // Set the minimized window icon.  This icon is used in a number of 
    // places: the Windows 10 window frame at the top-left, and the Linux 
    // task bar when CDAT is running.  It's not used on the Mac as far as 
    // we know.    
    setIconImages (List.of (
      GUIServices.getIcon ("tools.cdat").getImage(),
      GUIServices.getIcon ("tools.cdat.taskbar").getImage()
    ));

    // Add a handler when a data file is dragged into the tabbed pane so 
    // that we open the file.
    Runnable runnable = new Runnable () {
        public void run () {
          openFile (dropHandler.getFile());
        } // run
      };
    dropHandler = new FileTransferHandler (runnable);
    tabbedPane.setTransferHandler (dropHandler);

    // Add file open support for when a file is double clicked on Mac
    // or Windows.
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

  /** Updates the visibility of the toolbar labels. */
  private void updateToolbarLabelVisibility (boolean flag) {

    fileChooser.setShowText (flag);
    viewChooser.setShowText (flag);
    helpChooser.setShowText (flag);
    toolBar.updateButtonSize();

  } // updateToolbarLabelVisibility

  ////////////////////////////////////////////////////////////

  /** Updates the visibility of the toolbar labels. */
  private void updateToolbarLabelVisibility (ActionEvent event) {

    boolean flag = ((JCheckBoxMenuItem) event.getSource()).getState();
    updateToolbarLabelVisibility (flag);
    GUIServices.storeBooleanSettingForClass (flag, "toolbar.label.visibility", cdat.class);

  } // updateToolbarLabelVisibility
  
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

  /** 
   * Shows the file information dialog window for the currently active
   * tab.
   * 
   * @since 3.8.1
   */
  private void showFileInformation () {

    EarthDataReader reader = getAnalysisPanel().getReader();

    var infoPanel = new ReaderInfoPanel (reader);
    var metadataPanel = new ReaderMetadataPanel (reader);
    var tabs = new JTabbedPane();
    tabs.add ("Summary", infoPanel);
    tabs.add ("Raw Metadata", metadataPanel);
    var pane = new JPanel (new BorderLayout());
    pane.add (tabs, BorderLayout.CENTER);

    JOptionPane optionPane = new JOptionPane (pane,
      JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
      null, new String [] {"Close"});
    JDialog dialog = optionPane.createDialog (cdat.this, "File Information");
    dialog.setResizable (true);
    dialog.setModal (false);
    dialog.setVisible (true);

  } // showFileInformation

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
    fileChooser.setInfo (enabled);
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

//    EarthDataReader reader = EarthDataChooser.showDialog (this, file);
    var chooser = EarthDataReaderChooser.getInstance();
    chooser.showDialog (this, file);
//    var reader = EarthDataChooser.showDialog (this, file);
    EarthDataReader reader = (chooser.getState() == State.SELECTED ? chooser.getReader() : null);




// FIXME: What happens here if the directory or file no longer exist?  In some
// cases this method is being called from the recent file menu and a file or
// directory may have been deleted entirely.
// There's a problem that is not easy to reproduce, but the symptom is when
// hitting Cancel on the file open dialog, the dialog doesn't disappear, and
// can't be dismissed and you have to close CDAT.




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

  // THese are various events performed in response to clicks from 
  // buttons or menus.

  private void openFileEvent () { openFile (null); }

  private void closeFileEvent () {
    EarthDataAnalysisPanel analysisPanel = getAnalysisPanel();
    tabbedPane.remove (analysisPanel);
    analysisPanel.dispose();
    updateEnabled();
  } // closeFileEvent

  private void exportFileEvent () {
    EarthDataAnalysisPanel analysisPanel = getAnalysisPanel();
    EarthDataView view = analysisPanel.getView();
    EarthDataReader reader = analysisPanel.getReader();
    EarthDataExporter exporter = new EarthDataExporter (
      view, reader.getInfo(), reader, reader.getStatisticsVariables());
    exporter.showDialog (cdat.this);
  } // exportFileEvent

  private void fileInfoEvent () {
    showFileInformation();
  } // fileInfoEvent

  private void quitEvent () {
    ToolServices.exitWithCode (0);
    return;
  } // quitEvent

  private void windowSizeEvent (Dimension size) { cdat.this.setSize (size); }

  private void windowCustomSizeEvent () {

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

  } // windowCustomSizeEvent

  private void fullScreenEvent () { getAnalysisPanel().showFullScreen(); }

  private void editPreferencesEvent() {
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
  } // editPreferencesEvent

  private void openResourcesEvent() {
    try { ResourceManager.showResourcesDirectory(); }
    catch (IOException e) {
      JOptionPane.showMessageDialog (cdat.this,
        "An error occurred showing the user\n" +
        "resources directory:\n" + e.toString(),
        "Error", JOptionPane.ERROR_MESSAGE);
    } // catch
  } // openResourcesEvent

  private void navAnalysisEvent() { getAnalysisPanel().showNavAnalysisDialog(); }

  private void saveProfileEvent() {
    int returnVal = profileChooser.showSaveDialog (cdat.this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File saveFile = profileChooser.getSelectedFile();
      try {
        getAnalysisPanel().saveProfile (saveFile);
      } // try
      catch (IOException e) {
        JOptionPane.showMessageDialog (cdat.this,
          "An error occurred saving the profile:\n" +
          e.toString() + "\n" + 
          "The profile was not saved.", 
          "Error", JOptionPane.ERROR_MESSAGE);
      } // catch
    } // if
  } // saveProfileEvent

  private void loadProfileEvent() {
    int returnVal = profileChooser.showOpenDialog (cdat.this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File loadFile = profileChooser.getSelectedFile();
      try {
        getAnalysisPanel().loadProfile (loadFile);
      } // try
      catch (IOException e) {
        JOptionPane.showMessageDialog (cdat.this,
          "An error occurred loading the profile:\n" +
          e.toString() + "\n" + 
          "Try again with another file.", 
          "Error", JOptionPane.ERROR_MESSAGE);
      } // catch
      catch (ClassNotFoundException e) {
        LOGGER.log (Level.WARNING, "Error loading profile", ToolServices.shortTrace (e, "noaa.coastwatch")); 
      } // catch
    } // if
  } // loadProfileEvent

  private void showHelpEvent() {
    HTMLPanel helpPanel = new HTMLPanel (helpIndex, false);
    helpPanel.setPreferredSize (ToolServices.HELP_DIALOG_SIZE);
    helpPanel.showDialog (cdat.this, "Help");
  } // showHelpEvent

  private void openCourseEvent() {
    try { Desktop.getDesktop().browse (new URI (COURSE_URL)); }
    catch (Exception e) {
      JOptionPane.showMessageDialog (cdat.this,
        "Error opening the online course:\n" + e.toString(),
        "Error", JOptionPane.ERROR_MESSAGE);
    } // catch
  } // openCourseEvent

  private void showAboutEvent() {
    JOptionPane.showMessageDialog (cdat.this, 
      ToolServices.getAbout (LONG_NAME), "About", 
      JOptionPane.INFORMATION_MESSAGE);
  } // showAboutEvent

  ////////////////////////////////////////////////////////////

  /** 
   * This is a utility class that takes a new property value and matches
   * it to a runnable item.  This is a custom adapter that's used with the
   * tool bar choosers.
   */
  private class PropertyChangeAdapter implements PropertyChangeListener {

    private Map<String, Runnable> runTable;

    public PropertyChangeAdapter (Map<String, Runnable> runTable) { this.runTable = runTable; }

    public void propertyChange (PropertyChangeEvent event) {
      String value = (String) event.getNewValue();
      var runnable = runTable.get (value);
      if (runnable != null) runnable.run();
    } // propertyChange

  } // PropertyChangeAdapter class

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

  /** 
   * This is a utility class that handles window resize events 
   * and saves the new window size so that it can be recalled later
   * the next time the application starts.
   */
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
      LOGGER.warning (e.getMessage());
      usage();
      ToolServices.exitWithCode (1);
      return;
    } // catch

    // Print help message
    // ------------------
    if (cmd.getOptionValue (helpOpt) != null) {
      usage ();
      ToolServices.exitWithCode (0);
      return;
    } // if  

    // Print version message
    // ---------------------
    if (cmd.getOptionValue (versionOpt) != null) {
      System.out.println (ToolServices.getFullVersion (PROG));
      ToolServices.exitWithCode (0);
      return;
    } // if  

    // Get remaining arguments
    // -----------------------
    String[] remain = cmd.getRemainingArgs();
    if (remain.length < NARGS) {
      LOGGER.warning ("At least " + NARGS + " argument(s) required");
      usage();
      ToolServices.exitWithCode (1);
      return;
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
        LOGGER.severe ("Invalid geometry '" + geometry + "'");
        ToolServices.exitWithCode (2);
        return;
      } // if
      try {
        int width = Integer.parseInt (geometryArray[0]);
        int height = Integer.parseInt (geometryArray[1]);
        userFrameSize = new Dimension (width, height);
      } // try
      catch (NumberFormatException e) {
        LOGGER.severe ("Error parsing geometry: " + e.getMessage());
        ToolServices.exitWithCode (2);
        return;
      } // catch
    } // if
    final Dimension frameSize = (userFrameSize != null ? userFrameSize : lastFrameSize);

    // Create and show frame
    // ---------------------
    SwingUtilities.invokeLater (new Runnable() {
      public void run() { 

        GUIServices.initializeLaf();

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
          ToolServices.exitWithCode (1);
          return;
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
          ToolServices.exitWithCode (1);
          return;
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
          ToolServices.exitWithCode (1);
          return;
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

  private static void usage () { System.out.println (getUsage()); }

  ////////////////////////////////////////////////////////////

  /** Gets the usage info for this tool. */
  static UsageInfo getUsage () {

    UsageInfo info = new UsageInfo ("cdat");

    info.func ("Performs interactive earth data analysis");

    info.param ("[input]", "The optional initial input data file to open");

    info.option ("-h, --help", "Show help message");
    info.option ("-g, --geometry=WxH", "Set window width and height");
    info.option ("--version", "Show version information");

    return (info);

  } // getUsage

  ////////////////////////////////////////////////////////////

} // cdat class

////////////////////////////////////////////////////////////////////////
