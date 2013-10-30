////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataBrowsePanel.java
  PURPOSE: Shows browse views of a set of reader objects.
   AUTHOR: Peter Hollemans
     DATE: 2006/03/30
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.browse;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;
import noaa.coastwatch.gui.*;
import noaa.coastwatch.gui.open.*;
import noaa.coastwatch.render.*;

/**
 * The <code>EarthDataBrowsePanel</code> combines a number of
 * components to present the user with a view of a
 * <code>ReaderList</code> object.  One of the variables from
 * each reader is selected to act as the browse image.  The user
 * can control the browse area, resolution, palette, enhancement,
 * and a select set of overlays.  The panel signals that it is
 * busy in some operation by firing a <code>BUSY_PROPERTY</code>
 * property event, true if the browse panel is busy or false if
 * not.
 *
 * @see noaa.coastwatch.io.EarthDataReader
 * @see noaa.coastwatch.io.ReaderList
 * @see noaa.coastwatch.render.EarthDataView
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class EarthDataBrowsePanel
  extends JPanel {

  // Constants
  // ---------

  /** The grid command. */
  private static final String GRID_COMMAND = "Grid Overlay";

  /** The coast command. */
  private static final String COAST_COMMAND = "Coast Overlay";

  /** The political command. */
  private static final String POLITICAL_COMMAND = "Political Overlay";

  /** The topography command. */
  private static final String TOPOGRAPHY_COMMAND = "Topography Overlay";

  /** The magnify operation. */
  public static final String MAGNIFY_COMMAND = "Zoom In x2";

  /** The shrink operation. */
  public static final String SHRINK_COMMAND = "Zoom Out x2";

  /** The zoom operation. */
  public static final String ZOOM_COMMAND = "Zoom to Selected";

  /** The pan operation. */
  public static final String PAN_COMMAND = "Pan View Area";

  /** The region operation. */
  public static final String REGION_COMMAND = "Show Region";

  /** The reset operation. */
  public static final String RESET_COMMAND = "Reset View";

  /** The change palette command. */
  public static final String PALETTE_COMMAND = "Change Palette";

  /** The change scale command. */
  public static final String SCALE_COMMAND = "Change Scale";

  /** The date format string. */
  private static final String DATE_FMT = "yyyy/MM/dd HH:mm 'UTC'    ";

  /** Color for view background. */
  private static final Color VIEW_BACK = Color.BLACK;

  /** Color for grid color. */
  private static final Color VIEW_GRID = Color.WHITE;

  /** Color for coast line. */
  private static final Color VIEW_COAST = Color.BLACK;

  /** Color for coast fill. */
  private static final Color VIEW_LAND = new Color (160, 174, 128);

  /** Color for international political lines. */
  private static final Color VIEW_INTER = Color.RED;

  /** Color for state political lines. */
  private static final Color VIEW_STATE = VIEW_LAND.darker();

  /** Color for topography lines. */
  private static final Color VIEW_TOPO = Color.WHITE;

  /** The busy property for property change events. */
  public static final String BUSY_PROPERTY = "busyStatus";

  // Variables
  // ---------

  /** The combo box for dates. */
  private JComboBox dateCombo;

  /** The button for next date. */
  private JButton nextButton;

  /** The button for previous date. */
  private JButton prevButton;

  /** The view panel for data display. */
  private EarthDataViewPanel viewPanel;

  /** The null preview Earth view. */
  private static EarthDataView nullView;

  /** The legend panel for color scale. */
  private LegendPanel legendPanel;

  /** The current variable name. */
  private String varName;

  /** The current identifier. */
  private String ident;

  /** The cache of data enhancement settings. */
  private HashMap settingsMap = new HashMap();

  /** The list of readers to display. */
  private ReaderList readerList;

  /** The current reader index */
  private int currentReader = -1;

  /** The light table for the view panel. */
  private LightTable lightTable;

  /** The button group for control toggle buttons. */
  private ButtonGroup group;

  /** The grid overlay for the view. */
  private LineOverlay gridOverlay;

  /** The coast overlay for the view. */
  private PolygonOverlay coastOverlay;

  /** The international lines overlay for the view. */
  private LineOverlay interOverlay;

  /** The state lines overlay for the view. */
  private LineOverlay stateOverlay;

  /** The topography overlay for the view. */
  private LineOverlay topoOverlay;

  /** The popup menu to show for the subregion list. */
  private JPopupMenu subregionMenu;

  /** The list of subregions for the subreion button. */
  private List subregionList;

  /** The button that shows a popup menu for the region list. */
  private JButton subregionButton;

  /** The listener for subregion menu item events. */
  private ActionListener subregionListener;

  /** The palette popup menu for showing the palette list. */
  private JPopupMenu palettePopupMenu;

  /** The palette scroll panel with palette list. */
  private JScrollPane paletteScrollPane;

  /** The list of palettes. */
  private JList paletteList;

  /** The window for showing palette list (alternative method). */
  private JWindow palettePopupWindow;

  /** The flag for using a menu or window for palette popup. */
  private boolean usePalettePopupMenu;

  /** The text field for minimum data value. */
  private JTextField minField;

  /** The text field for maximum data value. */
  private JTextField maxField;
  
  /** The dialog for range fields (or null if not created). */
  private JDialog rangeDialog;

  /** The panel for range dialog. */
  private JPanel rangePanel;

  /** The actions for the range dialog. */
  private Action[] rangeActions;

  ////////////////////////////////////////////////////////////


  /** 
   * Creates a new empty browse panel with no extra buttons and
   * no overlays.
   */
  public EarthDataBrowsePanel () {

    this (null, null, null, null, null, null);

  } // EarthDataBrowsePanel

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new empty browse panel.
   *
   * @param grid the grid overlay or null for none.
   * @param coast the coast overlay or null for none.
   * @param inter the international borders overlay or null for none.
   * @param state the state borders overlay or null for none.
   * @param topo the topographic overlay or null for none.
   * @param extraButtons a list of extra buttons to put into the
   * toolbar or null for no extra buttons.
   */
  public EarthDataBrowsePanel (
    LineOverlay grid,
    PolygonOverlay coast,
    LineOverlay inter,
    LineOverlay state,
    LineOverlay topo,
    List extraButtons
  ) {

    super (new BorderLayout());
 
    // Create control panel
    // --------------------
    Box controlPanel = Box.createVerticalBox();
    this.add (controlPanel, BorderLayout.NORTH);

    // Create date box and buttons
    // ---------------------------
    JPanel topControlPanel = new JPanel (new FlowLayout (FlowLayout.LEFT));
    topControlPanel.setAlignmentX (0.0f);
    controlPanel.add (topControlPanel);
    topControlPanel.add (new JLabel ("View Date:"));

    prevButton = GUIServices.getIconButton ("selection.previous");
    prevButton.setToolTipText ("Previous");
    GUIServices.setSquare (prevButton);
    prevButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          dateCombo.setSelectedIndex (dateCombo.getSelectedIndex()-1);
        } // performAction
      });
    topControlPanel.add (prevButton);

    dateCombo = new JComboBox();
    dateCombo.addActionListener (new DateListener());
    topControlPanel.add (dateCombo);

    /**
     * We do this next bit because in the Apple Aqua LAF, the
     * JComboBox doesn't line up correctly with the next and previous
     * buttons even though the FlowLayout is supposed to line them up
     * according to their vertical centers.
     */
    if (GUIServices.IS_MAC) {
      String laf = UIManager.getLookAndFeel().getClass().getName();
      if (laf.equals ("apple.laf.AquaLookAndFeel"))
        dateCombo.setBorder (BorderFactory.createEmptyBorder (5, 0, 0, 0));
    } // if    

    nextButton = GUIServices.getIconButton ("selection.next");
    nextButton.setToolTipText ("Next");
    GUIServices.setSquare (nextButton);
    nextButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          dateCombo.setSelectedIndex (dateCombo.getSelectedIndex()+1);
        } // performAction
      });
    topControlPanel.add (nextButton);

    // Create toolbar
    // --------------
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable (false);
    toolbar.setAlignmentX (0.0f);
    controlPanel.add (toolbar);

    // Add overlay buttons
    // -------------------
    ActionListener buttonListener = new ControlButtonListener();
    JToggleButton toggle;
    group = new ButtonGroup();

    if (grid != null) {
      toggle = GUIServices.getIconToggle ("overlay.grid");
      gridOverlay = grid;
      gridOverlay.setLayer (4);
      toggle.setSelected (gridOverlay.getVisible());
      toggle.setActionCommand (GRID_COMMAND);
      toggle.addActionListener (buttonListener);
      toggle.setToolTipText (GRID_COMMAND);
      toolbar.add (toggle);
    } // if

    if (coast != null) {
      toggle = GUIServices.getIconToggle ("overlay.coast");
      coastOverlay = coast;
      coastOverlay.setLayer (1);
      toggle.setSelected (coastOverlay.getVisible());
      toggle.setActionCommand (COAST_COMMAND);
      toggle.addActionListener (buttonListener);
      toggle.setToolTipText (COAST_COMMAND);
      toolbar.add (toggle);
    } // if

    if (inter != null && state != null) {
      toggle = GUIServices.getIconToggle ("overlay.political");
      interOverlay = inter;
      stateOverlay = state;
      interOverlay.setLayer (2);
      stateOverlay.setLayer (3);
      toggle.setSelected (
        interOverlay.getVisible() && stateOverlay.getVisible());
      toggle.setActionCommand (POLITICAL_COMMAND);
      toggle.addActionListener (buttonListener);
      toggle.setToolTipText (POLITICAL_COMMAND);
      toolbar.add (toggle);
    } // if

    if (topo != null) {
      toggle = GUIServices.getIconToggle ("overlay.topography");
      topoOverlay = topo;
      topoOverlay.setLayer (0);
      toggle.setSelected (topoOverlay.getVisible());
      toggle.setActionCommand (TOPOGRAPHY_COMMAND);
      toggle.addActionListener (buttonListener);
      toggle.setToolTipText (TOPOGRAPHY_COMMAND);
      toolbar.add (toggle);
    } // if

    toolbar.addSeparator();

    // Add view control buttons
    // ------------------------
    JButton button;

    button = GUIServices.getIconButton ("view.magnify");
    button.setActionCommand (MAGNIFY_COMMAND);
    button.addActionListener (buttonListener);
    button.setToolTipText (MAGNIFY_COMMAND);
    toolbar.add (button);

    button = GUIServices.getIconButton ("view.shrink");
    button.setActionCommand (SHRINK_COMMAND);
    button.addActionListener (buttonListener);
    button.setToolTipText (SHRINK_COMMAND);
    toolbar.add (button);

    toggle = GUIServices.getIconToggle ("view.zoom");
    toggle.setActionCommand (ZOOM_COMMAND);
    toggle.addActionListener (buttonListener);
    toggle.setToolTipText (ZOOM_COMMAND);
    toolbar.add (toggle);
    group.add (toggle);

    toggle = GUIServices.getIconToggle ("view.pan");
    toggle.setActionCommand (PAN_COMMAND);
    toggle.addActionListener (buttonListener);
    toggle.setToolTipText (PAN_COMMAND);
    toolbar.add (toggle);
    group.add (toggle);

    subregionButton = GUIServices.getIconButton ("view.region");
    subregionButton.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          subregionMenu.show (subregionButton, 0, subregionButton.getHeight());
        } // actionPerformed
      });
    subregionButton.setEnabled (false);
    subregionButton.setToolTipText (REGION_COMMAND);
    subregionListener = new SubregionMenuListener();
    toolbar.add (subregionButton);

    button = GUIServices.getIconButton ("view.reset");
    button.setActionCommand (RESET_COMMAND);
    button.addActionListener (buttonListener);
    button.setToolTipText (RESET_COMMAND);
    toolbar.add (button);

    toolbar.addSeparator();

    // Add color enhancement options
    // -----------------------------
    button = GUIServices.getIconButton ("view.palette");
    button.setActionCommand (PALETTE_COMMAND);
    button.addActionListener (buttonListener);
    button.setToolTipText (PALETTE_COMMAND);
    toolbar.add (button);

    button = GUIServices.getIconButton ("view.scale");
    button.setActionCommand (SCALE_COMMAND);
    button.addActionListener (buttonListener);
    button.setToolTipText (SCALE_COMMAND);
    toolbar.add (button);

    // Add extra buttons
    // -----------------
    if (extraButtons != null) {
      toolbar.addSeparator();
      for (Iterator iter = extraButtons.iterator(); iter.hasNext();) {
        toolbar.add ((AbstractButton) iter.next());
      } // for
    } // if

    // Create null Earth view
    // ----------------------
    if (nullView == null) {
      try { 
        nullView = new SolidBackground (null, new int[] {1, 1}, VIEW_BACK);
      } // try
      catch (Exception e) {
        e.printStackTrace();
      } // catch
    } // if

    // Create view panel
    // -----------------
    viewPanel = new EarthDataViewPanel (nullView);
    viewPanel.addPropertyChangeListener (EarthDataViewPanel.RENDERING_PROPERTY,
      new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent e) {
          firePropertyChange (BUSY_PROPERTY, e.getOldValue(), e.getNewValue());
        } // propertyChange
      });

    // Add track bar
    // -------------
    EarthDataViewPanel.TrackBar trackBar = viewPanel.new TrackBar (
      true, false, true, false);
    trackBar.setBorder (new BevelBorder (BevelBorder.RAISED));
    this.add (trackBar, BorderLayout.SOUTH);

    // Add color scale display
    // -----------------------
    legendPanel = new LegendPanel (null);
    legendPanel.setPreferredSize (new Dimension (90, 0));
    this.add (legendPanel, BorderLayout.EAST);

    // Create light table
    // --------------------
    lightTable = new LightTable (viewPanel);
    lightTable.addChangeListener (new DrawListener()); 
    lightTable.setBackground (VIEW_BACK);
    this.add (lightTable, BorderLayout.CENTER);

    // Get palette names and pre-load palettes
    // ---------------------------------------
    String[] paletteNames = 
      (String[]) PaletteFactory.getPredefined().toArray (new String[]{});
    for (int i = 0; i < paletteNames.length; i++)
      PaletteFactory.create (paletteNames[i]);

    // Create palette popup
    // --------------------
    paletteList = new JList (paletteNames);
    paletteList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    paletteList.addListSelectionListener (new PaletteListener());
    paletteList.setCellRenderer(new PaletteCellRenderer());
    paletteScrollPane = new JScrollPane (paletteList);
    usePalettePopupMenu = !GUIServices.IS_MAC;
    if (usePalettePopupMenu) {
      palettePopupMenu = new JPopupMenu();
      palettePopupMenu.add (paletteScrollPane);
    } // if

    // Create range panel
    // ------------------
    rangePanel = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    ActionListener fieldListener = new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          applyRange();
        } // actionPerformed
      };

    GUIServices.setConstraints (gc, 0, 0, 2, 1, GridBagConstraints.HORIZONTAL, 
      1, 0);
    gc.insets = new Insets (0, 0, 2, 5);
    rangePanel.add (new JLabel ("Select the scale range:"), gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL,
      0, 0);
    rangePanel.add (new JLabel ("Minimum:"), gc);
    minField = new JTextField (12);
    minField.addActionListener (fieldListener);
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 
      1, 0);
    rangePanel.add (minField, gc);
    
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL,
      0, 0);
    rangePanel.add (new JLabel ("Maximum:"), gc);
    maxField = new JTextField (12);
    maxField.addActionListener (fieldListener);
    GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL, 
      1, 0);
    rangePanel.add (maxField, gc);
 
    // Create range actions
    // --------------------
    Action applyAction = GUIServices.createAction ("Apply", new Runnable () {
        public void run () { applyRange(); }
      });
    Action closeAction = GUIServices.createAction ("Close", null);
    rangeActions = new Action[] {applyAction, closeAction};

  } // EarthDataBrowsePanel constructor

  ////////////////////////////////////////////////////////////

  /** Applies the scale range from the range dialog. */
  private void applyRange () {

    // Determine if function is logarithmic
    // ------------------------------------
    ColorEnhancement view = (ColorEnhancement) viewPanel.getView();
    EnhancementFunction func = view.getFunction();
    boolean isLog = (func instanceof LogEnhancement);

    // Get range values
    // ----------------
    double min, max;
    try {
      min = Double.parseDouble (minField.getText());
      max = Double.parseDouble (maxField.getText());
    } // try
    catch (NumberFormatException e) {
      JOptionPane.showMessageDialog (rangeDialog,
        "Error parsing minimum and maximum values.\n" + 
        e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      return;
    } // catch

    // Check for invalid values
    // ------------------------
    if (isLog && (min <= 0 || max <= 0)) {
      JOptionPane.showMessageDialog (rangeDialog,
        "Log functions must have both bounds >= 0.", 
        "Error", JOptionPane.ERROR_MESSAGE);
      return;
    } // if

    // Apply range
    // -----------
    func.setRange (new double[] {min, max});
    viewPanel.stopRendering();
    view.setFunction (func);
    legendPanel.setLegend (view.getLegend());
    legendPanel.repaint();
    viewPanel.repaint();

  } // applyRange

  ////////////////////////////////////////////////////////////

  /** Listens for palette change events. */
  private class PaletteListener implements ListSelectionListener {
    public void valueChanged (ListSelectionEvent event) {

      if (event.getValueIsAdjusting()) return;
      if (usePalettePopupMenu && !palettePopupMenu.isVisible()) return;
      else if (!usePalettePopupMenu && palettePopupWindow == null) return;

      // Get new palette
      // ---------------
      String paletteName = 
        (String) ((JList) event.getSource()).getSelectedValue();
      if (paletteName == null) return;
      Palette palette = PaletteFactory.create (paletteName);

      // Set palette in view
      // -------------------
      viewPanel.stopRendering();
      EarthDataView view = viewPanel.getView();
      ((ColorEnhancement) view).setPalette (palette);
      legendPanel.setLegend (view.getLegend());
      legendPanel.repaint();
      viewPanel.repaint();

      // Hide popup menu
      // ---------------
      if (usePalettePopupMenu) 
        palettePopupMenu.setVisible (false);
      else
        palettePopupWindow.setVisible (false);
        
    } // valueChanged

  } // PaletteListener class

  ////////////////////////////////////////////////////////////

  /** Renders list cells as palette icons and labels. */
  private class PaletteCellRenderer extends JLabel 
    implements ListCellRenderer {

    // Variables
    // ---------

    /** The name of the palette that we are displaying. */
    private String paletteName;

    /** The palette icon to display with. */
    private Icon paletteIcon = new PaletteIcon();

    /** Gets a label for the specified list value. */
    public Component getListCellRendererComponent (
      JList list,
      Object value,
      int index,
      boolean isSelected,
      boolean cellHasFocus
    ) {

      // Set label text and icon
      // -----------------------
      paletteName = value.toString();
      setText (paletteName);
      setIcon (paletteIcon);

      // Set label colors
      // ----------------
       if (isSelected) {
         setBackground (list.getSelectionBackground());
         setForeground (list.getSelectionForeground());
       } // if
       else {
         setBackground (list.getBackground());
         setForeground (list.getForeground());
       } // else

       // Set other label properties
       // --------------------------
       setEnabled (list.isEnabled());
       setFont (list.getFont());
       setOpaque (true);

       return (this);

     } // getListCellRendererComponent

    /** Renders a custom palette icon. */
    private class PaletteIcon implements Icon { 
      public int getIconWidth () { return (50); }
      public int getIconHeight () { return (10); }
      public void paintIcon (Component c, Graphics g, int x, int y) {
        IndexColorModel model = PaletteFactory.create (paletteName).getModel();
        int length = model.getMapSize();
        for (int i = 0; i < 50; i++) {
          g.setColor (new Color (model.getRGB ((int) (i/49.0*(length-1)))));
          g.drawLine (x+i, y, x+i, y+9);
        } // for
        g.setColor (Color.BLACK);
        g.drawLine (x, y, x+49, y);
        g.drawLine (x, y+9, x+49, y+9);
        g.drawLine (x, y, x, y+9);
        g.drawLine (x+49, y, x+49, y+9);
      } // paintIcon
    } // PaletteIcon
 
  } // PaletteCellRenderer

  ////////////////////////////////////////////////////////////
  
  /** Responds to changes in the date combo box. */
  private class DateListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      // Enable next/previous buttons
      // ----------------------------
      int readerIndex = dateCombo.getSelectedIndex();
      int readerCount = dateCombo.getItemCount();
      if (readerIndex == -1) {
        prevButton.setEnabled (false);
        nextButton.setEnabled (false);
        return;
      } // if
      prevButton.setEnabled (readerIndex != 0);
      nextButton.setEnabled (readerIndex != (readerCount-1));

      // Get new grid
      // ------------
      Grid grid = getGrid (readerIndex);
      if (grid == null) return;

      // Set data view to selected reader
      // --------------------------------
      viewPanel.stopRendering();
      ColorEnhancement view = (ColorEnhancement) viewPanel.getView();
      view.setGrid (grid);
      viewPanel.repaint();

    } // actionPerformed
  } // DateListener class

  ////////////////////////////////////////////////////////////

  /** Handles toolbar button events. */
  private class ControlButtonListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      // Get action command
      // ------------------
      String operation = event.getActionCommand();

      // Perform drawing-related operations
      // ----------------------------------
      if (operation.equals (ZOOM_COMMAND)) {
        lightTable.setDrawingMode (LightTable.BOX_ZOOM_MODE);
        lightTable.setActive (true);
        viewPanel.setDefaultCursor (lightTable.getCursor());
      } // if
      else if (operation.equals (PAN_COMMAND)) {
        lightTable.setDrawingMode (LightTable.IMAGE_MODE);
        lightTable.setActive (true);
        viewPanel.setDefaultCursor (lightTable.getCursor());
      } // else if
      
      // Perform single-click operations
      // -------------------------------
      else if (operation.equals (MAGNIFY_COMMAND)) {
        viewPanel.magnify (2);
        viewPanel.repaint();
      } // if
      else if (operation.equals (SHRINK_COMMAND)) {
        viewPanel.magnify (0.5);
        viewPanel.repaint();
      } // else if
      else if (operation.equals (RESET_COMMAND)) {
        viewPanel.reset();
        viewPanel.repaint();
      } // else if

      // Perform change in overlay
      // -------------------------
      else if (operation.equals (GRID_COMMAND)) {
        boolean visible = ((JToggleButton) event.getSource()).isSelected();
        viewPanel.stopRendering();
        gridOverlay.setVisible (visible);
        viewPanel.getView().setChanged();
        viewPanel.repaint();
      } // else if
      else if (operation.equals (COAST_COMMAND)) {
        boolean visible = ((JToggleButton) event.getSource()).isSelected();
        viewPanel.stopRendering();
        coastOverlay.setVisible (visible);
        viewPanel.getView().setChanged();
        viewPanel.repaint();
      } // else if
      else if (operation.equals (POLITICAL_COMMAND)) {
        boolean visible = ((JToggleButton) event.getSource()).isSelected();
        viewPanel.stopRendering();
        interOverlay.setVisible (visible);
        stateOverlay.setVisible (visible);
        viewPanel.getView().setChanged();
        viewPanel.repaint();
      } // else if
      else if (operation.equals (TOPOGRAPHY_COMMAND)) {
        boolean visible = ((JToggleButton) event.getSource()).isSelected();
        viewPanel.stopRendering();
        topoOverlay.setVisible (visible);
        viewPanel.getView().setChanged();
        viewPanel.repaint();
      } // else if

      // Show palette selection
      // ----------------------
      else if (operation.equals (PALETTE_COMMAND)) {
        JButton button = (JButton) event.getSource();
        if (usePalettePopupMenu) {
          palettePopupMenu.show (button, 0, button.getHeight());
        } // if
        else {
          palettePopupWindow = 
            new JWindow (SwingUtilities.getWindowAncestor (button));
          palettePopupWindow.add (paletteScrollPane, BorderLayout.CENTER);
          Point point = button.getLocationOnScreen();
          palettePopupWindow.setLocation (point.x, point.y+button.getHeight());
          palettePopupWindow.pack();
          palettePopupWindow.addWindowFocusListener (new WindowAdapter () {
              public void windowLostFocus (WindowEvent e) {
                if (palettePopupWindow.isVisible())
                  palettePopupWindow.setVisible (false);
                palettePopupWindow = null;
              } // windowLostFocus
            });
          palettePopupWindow.setVisible (true);
        } // else
      } // else if

      // Show range dialog
      // -----------------
      else if (operation.equals (SCALE_COMMAND)) {
        double[] range =  
          ((ColorEnhancement) viewPanel.getView()).getFunction().getRange();
        minField.setText (Double.toString (range[0]));
        maxField.setText (Double.toString (range[1]));
        if (rangeDialog == null) {
          rangeDialog = GUIServices.createDialog (EarthDataBrowsePanel.this,
            SCALE_COMMAND, true, rangePanel, null, rangeActions, 
            new boolean [] {false, true}, false);
        } // if
        else {
          rangeDialog.setLocationRelativeTo (EarthDataBrowsePanel.this);
        } // else
        rangeDialog.setVisible (true);
      } // else if

    } // actionPerformed
  } // ControlButtonListener

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the light table. */
  private class DrawListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {

      // Get drawing shape
      // -----------------
      Shape s = lightTable.getShape();

      // Get view operation
      // ------------------
      String operation = group.getSelection().getActionCommand();
    
      // Perform zoom
      // ------------
      if (operation.equals (ZOOM_COMMAND)) {
        Rectangle rect = ((Rectangle2D) s).getBounds();
        if (rect.width == 0 || rect.height == 0) return;
        viewPanel.magnify (rect);
        viewPanel.repaint();
      } // if

      // Perform pan
      // -----------
      else if (operation.equals (PAN_COMMAND)) {
        Line2D line = (Line2D) s;
        Point2D p1 = line.getP1();
        Point2D p2 = line.getP2();
        if (p1.equals (p2)) return;
        Dimension dims = viewPanel.getSize();
        Point2D center = new Point2D.Double (dims.width/2, dims.height/2);
        center.setLocation (
          center.getX() + (p1.getX() - p2.getX()),
          center.getY() + (p1.getY() - p2.getY())
        );
        viewPanel.setCenter (center);
        viewPanel.repaint();
      } // else if

    } // stateChanged
  } // DrawListener class

  ////////////////////////////////////////////////////////////

  /**
   * Opens the specified reader and gets the new data grid.
   * 
   * @param reader the new reader index to open.
   *
   * @return the new grid or null if the reader is the current reader.
   */
  private Grid getGrid (
    int reader
  ) {

    // Check for current reader
    // ------------------------
    if (currentReader == reader) return (null);
    else currentReader = reader;

    // Get the grid
    // ------------
    Grid grid;
    try { grid = (Grid) readerList.getVariable (reader, varName); }
    catch (IOException e) { throw new RuntimeException (e.getMessage()); }
    
    return (grid);

  } // getGrid

  ////////////////////////////////////////////////////////////

  /**
   * Sets the panel data readers.
   *
   * @param ident the identifier for this group of readers
   * and variable name.  The identifier is used to keep track of
   * color enhancement settings for this data.
   * @param newReaderList the list of readers to use for data.
   * @param varName the name of the variable from each reader to
   * display as a browse image.
   * @param settings the default enhancement settings for this
   * variable name.
   */
  public void setData (
    String ident,
    ReaderList newReaderList,
    String varName,
    ColorEnhancementSettings settings
  ) {

    // Set variable name
    // -----------------
    this.varName = varName;

    // Save old settings
    // -----------------
    EarthDataView view = viewPanel.getView();
    if (this.ident != null)
      settingsMap.put (this.ident, ((ColorEnhancement) view).saveSettings());
    this.ident = ident;

    // Add setting to cache
    // --------------------
    if (!settingsMap.containsKey (ident))
      settingsMap.put (ident, settings);
    else
      settings = (ColorEnhancementSettings) settingsMap.get (ident);

    // Get closest reader to selected
    // ------------------------------
    int oldReader = dateCombo.getSelectedIndex();
    int newReader;
    if (oldReader == -1) 
      newReader = newReaderList.size()-1;
    else {
      newReader = 
        newReaderList.getClosestIndex (readerList.getStartDate (oldReader));
    } // else

    // Get new grid
    // ------------
    readerList = newReaderList;
    EarthTransform trans = readerList.getTransform();
    currentReader = -1;
    Grid grid = getGrid (newReader);

    // Check for change in earth transform
    // -----------------------------------
    EarthTransform viewTrans = view.getTransform().getEarthTransform();
    if (viewTrans == null || !trans.equals (viewTrans)) {

      // Determine if transforms are compatible
      // --------------------------------------
      /**
       * We determine if the transforms are compatible here.  If
       * so, we can set the new view to display exactly the same
       * area as the old view.  If not, the best we can do is set
       * the same subregion which is usually pretty close.
       */
      boolean compatibleTrans = (viewTrans != null && 
        trans.getClass().equals (viewTrans.getClass()));
      Subregion subregion = null;
      EarthLocation centerEarth = null;
      double oldViewRes = 0;
      if (!compatibleTrans) {
        if (viewTrans != null) subregion = view.getSubregion();
      } // if
      else {
        centerEarth = viewTrans.transform (view.getCenter());
        oldViewRes = view.getResolution();
      } // else

      // Create new view
      // ---------------
      try {
        view = new ColorEnhancement (trans, grid, settings.getPalette(), 
          settings.getFunction());
      } // try
      catch (NoninvertibleTransformException e) {
        throw new RuntimeException (e.getMessage());
      } // catch

      // Add overlays
      // ------------
      if (gridOverlay != null) view.addOverlay (gridOverlay);
      if (coastOverlay != null) view.addOverlay (coastOverlay);
      if (interOverlay != null) view.addOverlay (interOverlay);
      if (stateOverlay != null) view.addOverlay (stateOverlay);
      if (topoOverlay != null) view.addOverlay (topoOverlay);

      // Restore transform
      // -----------------
      if (compatibleTrans) {
        try {
          view.setCenter (trans.transform (centerEarth));
          double factor = view.getResolution() / oldViewRes; 
          view.magnify (factor);
          view.setSize (viewPanel.getSize());
        } // try
        catch (NoninvertibleTransformException e) {
          throw new RuntimeException (e.getMessage());
        } // catch
      } // if
      else {
        if (subregion != null) view.showSubregion (subregion);
      } // else

      // Force repaint
      // -------------
      viewPanel.setView (view);
      viewPanel.repaint();

    } // if

    // Modify existing view
    // --------------------
    else {
      viewPanel.stopRendering();
      ColorEnhancement enhance = (ColorEnhancement) view;
      enhance.setGrid (grid);
      enhance.restoreSettings (settings);
      viewPanel.repaint();
    } // else

    // Modify legend
    // -------------
    legendPanel.setLegend (view.getLegend());
    legendPanel.repaint();

    // Update palette list
    // -------------------
    paletteList.setSelectedValue (
      ((ColorEnhancement) view).getPalette().getName(), true);

    // Add readers to date combo
    // -------------------------
    ActionListener[] listeners = dateCombo.getActionListeners();
    dateCombo.removeActionListener (listeners[0]);
    dateCombo.removeAllItems();
    for (int i = 0; i < readerList.size(); i++) {
      Date startDate = readerList.getStartDate (i);
      dateCombo.addItem (DateFormatter.formatDate (startDate, DATE_FMT));
    } // for
    dateCombo.addActionListener (listeners[0]);
    dateCombo.setSelectedIndex (newReader);
    
  } // setData

  ////////////////////////////////////////////////////////////

  /**
   * Sets the list of subregion specifications.
   *
   * @param subregionList the list of {@link
   * noaa.coastwatch.render.Subregion} objects.  The subregions
   * are used to quickly jump to a region of interest.
   */
  public void setSubregionList (
    List subregionList
  ) {

    this.subregionList = subregionList;

    // Set null subregion menu
    // -----------------------
    if (subregionList.size() == 0) {
      subregionMenu = null;
      subregionButton.setEnabled (false);
    } // if

    // Create new subregion menu
    // -------------------------
    else {
      subregionMenu = new JPopupMenu();
      for (Iterator iter = subregionList.iterator(); iter.hasNext();) {
        Subregion subregion = (Subregion) iter.next();
        if (subregion.getRadius() > 0) {
          JMenuItem item = subregionMenu.add (subregion.getName());
          item.addActionListener (subregionListener);
        } // if
      } // for
      subregionButton.setEnabled (true);
    } // else

  } // setSubregionList

  ////////////////////////////////////////////////////////////

  /** Responds to a subregion menu item by shifting the view. */    
  private class SubregionMenuListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String name = event.getActionCommand();
      for (Iterator iter = subregionList.iterator(); iter.hasNext();) {
        Subregion subregion = (Subregion) iter.next();
        if (subregion.getName().equals (name)) {
          viewPanel.stopRendering();
          viewPanel.getView().showSubregion (subregion);
          viewPanel.repaint();
          break;
        } // if
      } // for

    } // actionPerformed
  } // SubregionMenuListener

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the selected date index.  The date combo box is
   * populated with dates in the same order as they are delivered
   * by the reader list passed to {@link #setData}, so users can
   * rely on the same indexing as in the reader list.
   *
   * @param index the new index to make selected.
   */
  public void setSelectedIndex (
    int index
  ) { 
    
    dateCombo.setSelectedIndex (index); 

  } // setSelectedIndex

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the selected date index.  
   *
   * @return the selected index, or -1 if none is selected.
   *
   * @see #setSelectedIndex
   */
  public int getSelectedIndex () {
    
    return (dateCombo.getSelectedIndex());

  } // getSelectedIndex

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of data readers for testing purposes.
   *
   * @param lister the directory lister to use for listing.
   * @param match the file name matching pattern.
   * @param count the number of files to match at the end of the list.
   *
   * @return a list of matching reader objects.
   *
   * @see noaa.coastwatch.io.EarthDataReader
   */

  /*

  public static List getReaderList (
    DirectoryLister lister,
    String match,
    int count
  ) {

    // Get directory listing
    // ---------------------
    List entryList = lister.getEntries();
    Collections.reverse (entryList);
    String dir = lister.getDirectory();

    // Loop oever each directory entry
    // -------------------------------
    List readerList = new ArrayList();
    for (Iterator iter = entryList.iterator(); iter.hasNext();) {
      
      // Get name and size
      // -----------------
      DirectoryLister.Entry entry = (DirectoryLister.Entry) iter.next();
      String name = entry.getName();
      long size = entry.getSize();

      // Attempt to read
      // ---------------
      if (size != 0 && name.matches (match)) {
        EarthDataReader reader = null;
        System.out.print ("Attempting to read " + name + " ... ");
        try { reader = EarthDataReaderFactory.create (dir + "/" + name); }
        catch (IOException e) { }
        if (reader == null) System.out.println ("FAILED");
        else {
          System.out.println ("OK");
          readerList.add (reader);
        } // else
      } // if

      // Ignore remaining entries
      // ------------------------
      if (readerList.size() == count) break;

    } // for

    return (readerList);

  } // getReaderList

  */

  ////////////////////////////////////////////////////////////

  /** Tests this class. */

  /*

  public static void main (String[] argv) {

    // Get reader list
    // ---------------
    HTTPDirectoryLister lister = new HTTPDirectoryLister();
    lister.setRefFilter (new OpendapURLFilter());
    String dir = argv[0];
    String match = argv[1];
    try { lister.setDirectory (dir); } 
    catch (IOException e) { throw new RuntimeException (e.getMessage()); }
    List readerList = getReaderList (lister, match, 5);

    // Create panel
    // ------------
    EarthDataBrowsePanel panel = new EarthDataBrowsePanel();
    panel.setPreferredSize (new Dimension (512, 512));
    String varName = "sst";
    panel.setData (readerList, varName, 
      new ColorEnhancementSettings (varName, 
      PaletteFactory.create ("HSL256"), 
      new LinearEnhancement (new double[] {0, 30})));

    // Add panel to frame
    // ------------------
    final JFrame frame = new JFrame (EarthDataBrowsePanel.class.getName());
    frame.addWindowListener (new WindowMonitor());
    frame.setContentPane (panel);
    frame.pack();

    // Show frame
    // ----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          frame.setVisible (true);
        } // run
      });

  } // main

  */

  ////////////////////////////////////////////////////////////

} // EarthDataBrowsePanel class

////////////////////////////////////////////////////////////////////////


