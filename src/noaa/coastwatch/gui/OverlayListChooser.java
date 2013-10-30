////////////////////////////////////////////////////////////////////////
/*
     FILE: OverlayListChooser.java
  PURPOSE: Allows the user to manipulate a list of Earth data overlays.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/21
  CHANGES: 2004/05/22, PFH, modified to use GUIServices.getIcon()
           2005/03/21, PFH, added shape overlays
           2005/04/12, PFH, removed dbf and shx shapefile extensions
           2006/03/15, PFH, modified to use GUIServices.getIconButton()
           2006/10/31, PFH, added expression masking
           2006/11/02, PFH, modified showDialog() for validation
           2006/12/22, PFH, added data reference grid overlay
           2012/08/30, PFH, fixed toURL() deprecation

  CoastWatch Software Library and Utilities
  Copyright 1998-2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;

import java.util.List;
import java.util.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.gui.visual.*;

/**
 * The <code>OverlayListChooser</code> class is a panel that
 * allows the user to manipulate a list of {@link
 * EarthDataOverlay} objects.  The user may add a new overlay,
 * delete an overlay, edit the overlay properties, and change the
 * overlay layer.<p>
 *
 * The chooser signals a change in the overlay list by firing a
 * property change event whose property name is given by
 * <code>OVERLAY_LIST_PROPERTY</code>.  See the {@link
 * AbstractOverlayListPanel} class for details on how the property
 * change events should be interpreted.<p>
 *
 * @see LatLonOverlay
 * @see CoastOverlay
 * @see PoliticalOverlay
 * @see TopographyOverlay
 * @see BitmaskOverlay
 * @see MultilayerBitmaskOverlay
 * @see ExpressionMaskOverlay
 * @see ESRIShapefileReader
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class OverlayListChooser
  extends JPanel
  implements TabComponent {

  // Constants
  // ---------

  /** The overlay list property. */
  public static final String OVERLAY_LIST_PROPERTY = 
    AbstractOverlayListPanel.OVERLAY_PROPERTY;

  /** The grid command. */
  private static final String GRID_COMMAND = "Grid";

  /** The lat/lon grid command. */
  private static final String LATLON_COMMAND = "Lat/Lon";

  /** The data reference command. */
  private static final String DATAREF_COMMAND = "Data reference";

  /** The coast command. */
  private static final String COAST_COMMAND = "Coast";

  /** The political command. */
  private static final String POLITICAL_COMMAND = "Political";

  /** The topography command. */
  private static final String TOPOGRAPHY_COMMAND = "Topography";

  /** The mask command. */
  private static final String MASK_COMMAND = "Mask";

  /** The bitmask command. */
  private static final String BITMASK_COMMAND = "Bitmask";

  /** The multilayer bitmask command. */
  private static final String MULTILAYER_COMMAND = "Multilayer";

  /** The expression mask command. */
  private static final String EXPRMASK_COMMAND = "Expression mask";

  /** The shapefile command. */
  private static final String SHAPE_COMMAND = "Shape";

  /** The overlay list tooltip. */
  private static final String OVERLAY_LIST_TOOLTIP = "Overlay Layers";

  /** The overlay list panel title. */
  private static final String OVERLAY_TITLE = "Overlay";

  // Variables
  // ---------

  /** The overlay list panel. */
  private OverlayListPanel listPanel;

  /** The data reader for bitmask overlays. */
  private EarthDataReader reader;

  /** The list of variables for bitmask overlays. */
  private List variableList;

  /** The chooser dialog for overlay properties. */
  private JDialog chooserDialog;

  /** The popup menu for mask overlays. */
  private JPopupMenu maskMenu;

  /** The popup menu for grid overlays. */
  private JPopupMenu gridMenu;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new overlay list chooser. 
   * 
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   */
  public OverlayListChooser (
    EarthDataReader reader,
    List variableList
  ) {

    super (new BorderLayout());
    
    // Set bitmask overlay data sources
    // --------------------------------
    this.reader = reader;
    this.variableList = variableList;

    // Create main panel
    // -----------------
    listPanel = new OverlayListPanel();
    this.add (listPanel, BorderLayout.CENTER);

  } // OverlayListChooser constructor

  ////////////////////////////////////////////////////////////

  /** Gets the overlay list chooser tooltip. */
  public String getToolTip () { return (OVERLAY_LIST_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  /** Gets the overlay list chooser title. */
  public String getTitle () { return (null); }
  
  ////////////////////////////////////////////////////////////

  /** Gets the list of overlays. */
  public List getOverlays(){
	  return listPanel.getOverlayList();
  }
  
  ////////////////////////////////////////////////////////////

  /** Gets a list of overlays to the chooser. */
  public void addOverlays(List overlays){
	  for (Iterator iter = overlays.iterator(); iter.hasNext(); ) {
		  EarthDataOverlay overlay = (EarthDataOverlay) iter.next();
		  if (overlay instanceof GridContainerOverlay)
		        ((GridContainerOverlay) overlay).setDataSource(reader, variableList);
		  listPanel.addOverlay(overlay);
	  }
  }

  ////////////////////////////////////////////////////////////

  /** Gets the overlay list tab icon. */
  public Icon getIcon () {

    return (GUIServices.getIcon ("overlay.tab"));

  } // getIcon

  ////////////////////////////////////////////////////////////

  /** Redirects overlay property listeners to the list panel. */
  public void addPropertyChangeListener (
    String propertyName,
    PropertyChangeListener listener
  ) {

    if (propertyName.equals (AbstractOverlayListPanel.OVERLAY_PROPERTY))
      listPanel.addPropertyChangeListener (propertyName, listener);
    else
      super.addPropertyChangeListener (propertyName, listener);

  } // addPropertyChangeListener

  ////////////////////////////////////////////////////////////
  
  /** Implements overlay list buttons and title. */
  private class OverlayListPanel 
    extends AbstractOverlayListPanel {

    ////////////////////////////////////////////////////////

    /** Creates a new list panel. */
    public OverlayListPanel () {

      super (true, true, true, false, true);
      setDataSource (reader, variableList);

    } // OverlayListPanel constructor

    ////////////////////////////////////////////////////////

    /** Creates the overlay list add buttons. */
    protected List getAddButtons () {

      // Create button list and listener
      // -------------------------------
      List buttons = new LinkedList();
      ActionListener buttonListener = new OverlayListener();
      
      // Create buttons
      // --------------
      String[] commandArray = new String[] {
        GRID_COMMAND,
        COAST_COMMAND,
        POLITICAL_COMMAND,
        TOPOGRAPHY_COMMAND,
        MASK_COMMAND,
        SHAPE_COMMAND
      };
      String[] iconArray = new String[] {
        "overlay.grid",
        "overlay.coast",
        "overlay.political",
        "overlay.topography",
        "overlay.masks",
        "overlay.shape"
      };
      for (int i = 0; i < commandArray.length; i++) {
        JButton button = GUIServices.getIconButton (iconArray[i]);
        button.setActionCommand (commandArray[i]);
        button.addActionListener (buttonListener);
        button.setToolTipText (commandArray[i]);
        buttons.add (button);
      } // for

      // Create mask overlay menu
      // ------------------------
      maskMenu = new JPopupMenu();
      String[] maskCommands = new String[] {
        BITMASK_COMMAND,
        MULTILAYER_COMMAND,
        EXPRMASK_COMMAND
      };
      String[] maskIcons = new String[] {
        "overlay.menu.bitmask",
        "overlay.menu.multilayer",
        "overlay.menu.expression"
      };
      for (int i = 0; i < maskCommands.length; i++) {
        JMenuItem item = new JMenuItem (maskCommands[i], 
          GUIServices.getIcon (maskIcons[i]));
        maskMenu.add (item);
        item.addActionListener (buttonListener);
      } // for

      // Create grid overlay menu
      // ------------------------
      gridMenu = new JPopupMenu();
      String[] gridCommands = new String[] {
        LATLON_COMMAND,
        DATAREF_COMMAND
      };
      String[] gridIcons = new String[] {
        "overlay.menu.latlon",
        "overlay.menu.dataref"
      };
      for (int i = 0; i < gridCommands.length; i++) {
        JMenuItem item = new JMenuItem (gridCommands[i], 
          GUIServices.getIcon (gridIcons[i]));
        gridMenu.add (item);
        item.addActionListener (buttonListener);
      } // for

      return (buttons);

    } // getAddButtons

    ////////////////////////////////////////////////////////

    /** Gets the overlay list title. */
    protected String getTitle () { return (OVERLAY_TITLE); }

    ////////////////////////////////////////////////////////

    /** 
     * Adds a new overlay to the list panel and sets the overlay name
     * appropriately. 
     */
    private void addNewOverlay (
      EarthDataOverlay overlay,
      String name
    ) {

      overlay.setName (name + listPanel.getOverlayCount (name));
      listPanel.addOverlay (overlay);

    } // addNewOverlay

    ////////////////////////////////////////////////////////

    /** Handles overlay add events. */
    private class OverlayListener implements ActionListener {
      public void actionPerformed (ActionEvent event) {

        final String command = event.getActionCommand();

        // Show mask menu
        // --------------
        if (command.equals (MASK_COMMAND)) {
          JButton button = (JButton) event.getSource();
          maskMenu.show (button, 0, button.getHeight());
        } // if

        // Show grid menu
        // --------------
        if (command.equals (GRID_COMMAND)) {
          JButton button = (JButton) event.getSource();
          gridMenu.show (button, 0, button.getHeight());
        } // if

        // Create interactive overlay (extra input required)
        // -------------------------------------------------
        else if (command.equals (BITMASK_COMMAND) || 
            command.equals (MULTILAYER_COMMAND) ||
            command.equals (SHAPE_COMMAND) ||
            command.equals (EXPRMASK_COMMAND)) {

          // Create bitmask overlay
          // ----------------------
          if (command.equals (BITMASK_COMMAND)) {
            OverlayPropertyChooser chooserPanel =
              OverlayPropertyChooserFactory.create (
                new BitmaskOverlay (Color.GRAY, reader, variableList, 
                  (String) variableList.get (0), 255));
            showDialog (chooserPanel, command);
          } // if

          // Create multilayer bitmask overlay
          // ---------------------------------
          else if (command.equals (MULTILAYER_COMMAND)) {
            OverlayPropertyChooser chooserPanel =
              OverlayPropertyChooserFactory.create (createDefaultMultilayer());
            showDialog (chooserPanel, command);
          } // else if

          // Create expression mask overlay
          // ------------------------------
          else if (command.equals (EXPRMASK_COMMAND)) {
            OverlayPropertyChooser chooserPanel =
              OverlayPropertyChooserFactory.create (
                new ExpressionMaskOverlay (Color.GRAY, reader, variableList, 
                  ""));
            showDialog (chooserPanel, command);
          } // else if

          // Create shape overlay
          // --------------------
          else if (command.equals (SHAPE_COMMAND)) {
            EarthDataOverlay overlay = createShapeOverlay();
            if (overlay != null) addNewOverlay (overlay, command);
          } // else if

        } // if

        // Create non-interactive overlay
        // ------------------------------
        else {
          EarthDataOverlay overlay = null;
          if (command.equals (LATLON_COMMAND))
            overlay = new LatLonOverlay (Color.WHITE);
          else if (command.equals (DATAREF_COMMAND))
            overlay = new DataReferenceOverlay (Color.WHITE);
          else if (command.equals (COAST_COMMAND))
            overlay = new CoastOverlay (Color.WHITE);
          else if (command.equals (POLITICAL_COMMAND))
            overlay = new PoliticalOverlay (Color.WHITE);
          else if (command.equals (TOPOGRAPHY_COMMAND)) {
            try {
              TopographyOverlay topo = new TopographyOverlay (Color.WHITE);
              topo.setLevels (TopographyOverlay.BATH_LEVELS);
              overlay = topo;
            } // try
            catch (Exception e) { throw new RuntimeException (e); }
          } // else if
          if (overlay != null) addNewOverlay (overlay, command);
        } // else

      } // actionPerformed
    } // OverlayListener class

    ////////////////////////////////////////////////////////

    /** Shows a dialog for the specified chooser panel. */
    private void showDialog (
      final OverlayPropertyChooser chooserPanel,
      final String command
    ) { 

      // Create dialog
      // -------------
      final JDialog[] dialog = new JDialog[1];
      Action okAction = GUIServices.createAction ("OK", new Runnable() {
          public void run () {
            try { 
              addNewOverlay (chooserPanel.getOverlay(), command); 
              dialog[0].dispose();
            } // try
            catch (IllegalStateException e) {
              String errorMessage = 
                "An error occurred checking the input:\n" +
                e.getMessage() + "\n" + 
                "Please correct the problem and try again.";
              JOptionPane.showMessageDialog (dialog[0], errorMessage,
                "Error", JOptionPane.ERROR_MESSAGE);
            } // catch
          } // run
        });
      Action cancelAction = GUIServices.createAction ("Cancel", null);
      dialog[0] = GUIServices.createDialog (
        OverlayListChooser.this, "Select the overlay properties", true, 
        chooserPanel, null, new Action[] {okAction, cancelAction}, 
        new boolean[] {false, true}, true);

      // Show dialog
      // -----------
      dialog[0].setVisible (true);

    } // showDialog

    ////////////////////////////////////////////////////////

    /** Creates a new default multilayer bitmask. */
    private EarthDataOverlay createDefaultMultilayer () {

      MultilayerBitmaskOverlay multilayer = new MultilayerBitmaskOverlay();
      List bitmaskList = 
        MultilayerBitmaskOverlayPropertyChooser.createBitmaskList (1, 8,
          reader, variableList, (String) variableList.get (0));
      multilayer.addOverlays (bitmaskList);
      return (multilayer);

    } // getDefaultMultilayer

    ////////////////////////////////////////////////////////

    /** Creates a new shape overlay. */
    private EarthDataOverlay createShapeOverlay () {

      // Show file chooser
      // -----------------
      JFileChooser fileChooser = GUIServices.getFileChooser();
      SimpleFileFilter filter = new SimpleFileFilter (
        new String[] {"shp"}, "ESRI shapefile data");
      fileChooser.addChoosableFileFilter (filter);
      fileChooser.setDialogType (JFileChooser.OPEN_DIALOG);
      final Frame frame = 
        JOptionPane.getFrameForComponent (OverlayListChooser.this);
      int returnVal = fileChooser.showOpenDialog (frame);

      // Open selected file
      // ------------------
      EarthDataOverlay overlay = null;
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try {
          ESRIShapefileReader reader = new ESRIShapefileReader (file.toURI().toURL());
          overlay = reader.getOverlay();
          if (overlay instanceof PolygonOverlay)
            ((PolygonOverlay) overlay).setFillColor (null);
          overlay.setColor (Color.WHITE);
        } // try
        catch (IOException e) {
          String errorMessage = 
            "An error occurred creating the shape overlay:\n" +
            e.toString() + "\n" + 
            "Please choose another shape data file and try again.";
          JOptionPane.showMessageDialog (frame, errorMessage,
            "Error", JOptionPane.ERROR_MESSAGE);
        } // catch
      } // if

      return (overlay);

    } // createShapeOverlay

    ////////////////////////////////////////////////////////

  } // OverlayListPanel class

  ////////////////////////////////////////////////////////////

} // OverlayListChooser class

////////////////////////////////////////////////////////////////////////
