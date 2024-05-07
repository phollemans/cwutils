////////////////////////////////////////////////////////////////////////
/*

     File: NavigationAnalysisPanel.java
   Author: Peter Hollemans
     Date: 2006/12/11

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.nav;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import noaa.coastwatch.gui.DataViewOverlayControl;
import noaa.coastwatch.gui.EarthDataViewPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.LightTable;
import noaa.coastwatch.gui.nav.NavigationPoint;
import noaa.coastwatch.gui.nav.NavigationPointSavePanel;
import noaa.coastwatch.gui.nav.NavigationPointTable;
import noaa.coastwatch.gui.nav.NavigationPointTableModel;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.render.SolidBackground;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.NavigationOffsetEstimator;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * <p>The <code>NavigationAnalysisPanel</code> class is a panel that
 * allows the user to manipulate a list of image row and column
 * points and specify the offset between coastline data and image
 * data.</p>
 *
 * <p>The panel signals a change in the list of offsets or the
 * selected offset by firing an
 * <code>OVERLAY_LIST_PROPERTY</code> change event.</p>
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class NavigationAnalysisPanel
  extends JPanel
  implements DataViewOverlayControl {

  // Constants
  // ---------

  /** The add manual point command. */
  private static final String MANUAL_COMMAND = "Manual";

  /** The add automatic point command. */
  private static final String AUTO_COMMAND = "Automatic";

  /** The point remove command. */
  private static final String REMOVE_COMMAND = "Delete";

  /** The point clear command. */
  private static final String CLEAR_COMMAND = "Clear";

  /** The standard deviation units for normalization windows. */
  private static final double STDEV_UNITS = 1.5;

  /** The pixel spacing for data reference lines. */
  private static final double REF = 5.0;

  // Variables
  // ---------

  /** The data reader for navigation data. */
  private EarthDataReader reader;

  /** The list of variables for navigation data. */
  private List<String> variableList;

  /** The invisible button used for turning off the button group. */
  private JToggleButton hidden;

  /** The table for navigation points. */
  private JTable pointTable;

  /** The button for removing points from the table. */
  private JButton removeButton;

  /** The button for removing all points from the table. */
  private JButton clearButton;

  /** The combo box for data view variable. */
  private JComboBox variableCombo;

  /** The slider for navigation box size in data pixels. */
  private JSlider boxSlider;

  /** The data view panel for navigation offset. */
  private EarthDataViewPanel viewPanel;

  /** The null earth data view. */
  private EarthDataView nullView;

  /** The spinner for row offset. */
  private JSpinner rowSpinner;

  /** The spinner for column offset. */
  private JSpinner colSpinner;
  
  /** The action command for adding new points. */
  private String addCommand = "";
  
  /** The automatic navigation button. */
  private JButton autoButton;

  /** The reset navigation button. */
  private JButton resetButton;

  /** The normal (non-null) data view for the view panel. */
  private ColorEnhancement dataView;

  /** The estimator used for automatic navigation offsets. */
  private NavigationOffsetEstimator estimator;

  /** 
   * The view transform reset variable, true if a change was made
   * that requires the view transform to be completely reset.
   */
  private boolean needTransformReset;

  /** The light table for shifting the view visually. */
  private LightTable lightTable;

  ////////////////////////////////////////////////////////////

  @Override
  public void performOperation (
    Shape shape
  ) {

    Point2D point2d = ((Line2D) shape).getP1();
    DataLocation dataLoc = new DataLocation (point2d.getX(), point2d.getY());
    EarthLocation earthLoc = reader.getInfo().getTransform().transform (
      dataLoc);
    if (!earthLoc.isValid()) return;
    addPoint (earthLoc);

  } // performOperation

  ////////////////////////////////////////////////////////////

  /**
   * Adds a point to the list of navigation points.
   *
   * @param earthLoc the point to add.
   */
  public void addPoint (
    EarthLocation earthLoc
  ) {

    // Create point
    // ------------
    EarthTransform trans = reader.getInfo().getTransform();
    DataLocation dataLoc = trans.transform (earthLoc).round();
    if (!dataLoc.isValid()) return;
    earthLoc = trans.transform (dataLoc);
    NavigationPoint point = new NavigationPoint (trans.transform (dataLoc),
      dataLoc);

    // Perform automatic navigation
    // ----------------------------
    if (addCommand.equals (AUTO_COMMAND))
      navigatePoint (point);
    else 
      point.setComment (NavigationPointTable.MANUAL_COMMENT);

    // Add point to list
    // -----------------
    getTableModel().addPoint (point);

  } // addPoint

  ////////////////////////////////////////////////////////////

  @Override
  public LightTable.Mode getOperationMode () {

    return (isActive() ? LightTable.Mode.POINT : LightTable.Mode.NONE);

  } // getOperationMode

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isActive () { return (!hidden.isSelected()); }

  ////////////////////////////////////////////////////////////

  @Override
  public void deactivate () { hidden.setSelected (true); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates an analysis panel.
   * 
   * @param reader the reader to use for data variables.
   * @param variableList the list of allowed data variable names.
   */
  public NavigationAnalysisPanel (
    EarthDataReader reader,
    List<String> variableList
  ) {

    setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
    
    // Set data sources
    // ----------------
    this.reader = reader;
    this.variableList = variableList;

    // Create toolbar panel
    // --------------------
    JToolBar toolbar = new JToolBar();
    toolbar.setBorder (new TitledBorder (new EtchedBorder(), "Add Point"));
    ActionListener addButtonListener = new AddButtonListener();
    ButtonGroup group = new ButtonGroup();

    JToggleButton manualButton = GUIServices.getIconToggle ("analysis.manual");
    manualButton.setActionCommand (MANUAL_COMMAND);
    manualButton.addActionListener (addButtonListener);
    manualButton.setToolTipText (MANUAL_COMMAND);
    toolbar.add (manualButton);
    group.add (manualButton);

    JToggleButton autoAddButton = GUIServices.getIconToggle ("analysis.auto");
    autoAddButton.setActionCommand (AUTO_COMMAND);
    autoAddButton.addActionListener (addButtonListener);
    autoAddButton.setToolTipText (AUTO_COMMAND);
    toolbar.add (autoAddButton);
    group.add (autoAddButton);

    hidden = new JToggleButton();
    group.add (hidden);

    // Create point list panel
    // -----------------------
    JPanel pointPanel = new JPanel (new BorderLayout());
    pointPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Navigation Points"));
    pointTable = new NavigationPointTable (null);
    pointPanel.add (new JScrollPane (pointTable), BorderLayout.CENTER);
    pointTable.getSelectionModel().addListSelectionListener (
      new PointListener());
    pointTable.getModel().addTableModelListener (new TableDataListener());
    Dimension tableSize = new Dimension (0, pointTable.getRowHeight()*10);
    for (int i = 0; i < pointTable.getColumnCount(); i++) {
      tableSize.width += pointTable.getColumnModel().getColumn (i
        ).getPreferredWidth();
    } // for
    pointTable.setPreferredScrollableViewportSize (tableSize);

    Box listButtonPanel = Box.createHorizontalBox();
    pointPanel.add (listButtonPanel, BorderLayout.SOUTH);

    removeButton = GUIServices.getIconButton ("list.delete");
    GUIServices.setSquare (removeButton);
    removeButton.setActionCommand (REMOVE_COMMAND);
    ActionListener listButtonListener = new ListButtonListener();
    removeButton.addActionListener (listButtonListener);
    removeButton.setToolTipText (REMOVE_COMMAND);
    removeButton.setEnabled (false);
    listButtonPanel.add (removeButton);

    clearButton = GUIServices.getTextButton (CLEAR_COMMAND);
    clearButton.addActionListener (listButtonListener);
    clearButton.setToolTipText (CLEAR_COMMAND);
    clearButton.setEnabled (false);
    listButtonPanel.add (Box.createHorizontalGlue());
    listButtonPanel.add (clearButton);

    // Create offset view panel
    // ------------------------
    JPanel offsetPanel = new JPanel (new BorderLayout());
    offsetPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Navigation Offset"));

    JPanel topPanel = new JPanel (new GridBagLayout());
    offsetPanel.add (topPanel, BorderLayout.NORTH);
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;

    GUIServices.setConstraints (gc, 0, 0, 1, 1,
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    topPanel.add (new JLabel ("Variable:"), gc);

    variableCombo = new JComboBox (variableList.toArray());
    variableCombo.addActionListener (new VariableListener());
    GUIServices.setConstraints (gc, 1, 0, 1, 1, 
      GridBagConstraints.NONE, 1, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    topPanel.add (variableCombo, gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1,
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    topPanel.add (new JLabel ("Box size:"), gc);
    
    boxSlider = new JSlider (20, 100);
    boxSlider.setMajorTickSpacing (20);
    boxSlider.setMinorTickSpacing (10);
    boxSlider.setPaintTicks (true);
    boxSlider.setPaintLabels (true);
    boxSlider.setSnapToTicks (true);
    boxSlider.addChangeListener (new BoxSizeListener());
    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    topPanel.add (boxSlider, gc);

    try { nullView = new SolidBackground (Color.BLACK); }
    catch (Exception e) {
      e.printStackTrace();
    } // catch

    viewPanel = new EarthDataViewPanel (nullView);
    lightTable = new LightTable (viewPanel);
    lightTable.addChangeListener (new DrawListener()); 
    lightTable.setDrawingMode (LightTable.Mode.IMAGE_TRANSLATE);
    lightTable.setBackground (Color.BLACK);
    lightTable.setPreferredSize (new Dimension (256, 256));

    JPanel viewContainer = new JPanel (new BorderLayout());
    viewContainer.setBorder (new BevelBorder (BevelBorder.LOWERED));
    viewContainer.add (lightTable, BorderLayout.CENTER);
    offsetPanel.add (viewContainer, BorderLayout.CENTER);

    JPanel bottomPanel = new JPanel (new GridBagLayout());
    offsetPanel.add (bottomPanel, BorderLayout.SOUTH);

    GUIServices.setConstraints (gc, 0, 0, 1, 1,
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    bottomPanel.add (new JLabel ("Row offset:"), gc);

    rowSpinner = new JSpinner (new SpinnerNumberModel (0, -50, 50, 
      0.1));
    rowSpinner.addChangeListener (new RowOffsetListener());
    GUIServices.setConstraints (gc, 1, 0, 1, 1, 
      GridBagConstraints.NONE, 1, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    bottomPanel.add (rowSpinner, gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1,
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    bottomPanel.add (new JLabel ("Column offset:"), gc);

    colSpinner = new JSpinner (new SpinnerNumberModel (0, -50, 50, 
      0.1));
    colSpinner.addChangeListener (new ColumnOffsetListener());
    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.NONE, 1, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    bottomPanel.add (colSpinner, gc);

    autoButton = GUIServices.getTextButton ("Auto");
    autoButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          navigatePoint (getSelectedPoint());
        } // actionPerformed
      });
    gc.anchor = GridBagConstraints.EAST;
    GUIServices.setConstraints (gc, 2, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    bottomPanel.add (autoButton, gc);

    resetButton = GUIServices.getTextButton ("Reset");
    resetButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          resetPoint (getSelectedPoint());
        } // actionPerformed
      });
    GUIServices.setConstraints (gc, 2, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    bottomPanel.add (resetButton, gc);

    // Add panels to parent
    // --------------------
    JPanel leftPanel = new JPanel (new BorderLayout());
    leftPanel.add (toolbar, BorderLayout.NORTH);
    leftPanel.add (pointPanel, BorderLayout.CENTER);
    this.add (leftPanel);
    this.add (offsetPanel);

    // Create normal data view
    // -----------------------
    try {
      dataView = new ColorEnhancement (reader.getInfo().getTransform(),
        (Grid) reader.getVariable (variableList.get (0)),
        PaletteFactory.create ("BW-Linear"), 
        new LinearEnhancement (new double[] {0, 1}));
    } // try
    catch (Exception e) {
      throw new RuntimeException (e.getMessage());
    } // catch

    // Add coast overlay
    // -----------------
    CoastOverlay coast = new CoastOverlay (Color.RED);
    dataView.addOverlay (coast);

    // Create offset estimator
    // -----------------------
    estimator = new NavigationOffsetEstimator();

    // Clear point data
    // ----------------
    clearPoint();

  } // NavigationAnalysisPanel constructor

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the light table. */
  private class DrawListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {

      // Check if panel is showing
      // -------------------------
      if (!viewPanel.isShowing()) return;

      // Get user offset (to nearest 1/10th pixel)
      // -----------------------------------------
      Line2D line = (Line2D) lightTable.getShape();
      Point2D p1 = line.getP1();
      Point2D p2 = line.getP2();
      double magFactor = getMagFactor();
      if (dataView.getOrientationAffine().getType() != AffineTransform.TYPE_IDENTITY)
        magFactor *= -1;
      double rowOffsetUpdate = (p1.getY() - p2.getY()) / magFactor;
      rowOffsetUpdate = Math.rint (rowOffsetUpdate*10)/10;
      double colOffsetUpdate = (p1.getX() - p2.getX()) / magFactor;
      colOffsetUpdate = Math.rint (colOffsetUpdate*10)/10;

      // Add offset to current
      // ---------------------
      NavigationPoint point = getSelectedPoint();
      double[] offset = point.getOffset();
      offset[Grid.ROWS] += rowOffsetUpdate;
      offset[Grid.COLS] += colOffsetUpdate;
      point.setOffset (offset);
      point.setComment (NavigationPointTable.MANUAL_COMMENT);
      getTableModel().changePoint (point);

    } // stateChanged
  } // DrawListener class

  //////////////////////////////////////////////////////////////////////

  /** Handles changes in the table data. */
  private class TableDataListener implements TableModelListener {
    public void tableChanged (TableModelEvent event) {

      // Update point data
      // -----------------
      if (event.getType() == TableModelEvent.UPDATE) {
        updatePoint();
        viewPanel.repaint();
      } // if

      // Show new point data
      // -------------------
      else if (event.getType() == TableModelEvent.INSERT) {
        final int row = event.getFirstRow();
        /**
         * We do this interval selection later because if we do
         * it right now, the table doesn't update the selection
         * correctly.  Possibly it's in the middle of updating
         * its model and things are not in sync yet.
         */
        SwingUtilities.invokeLater (new Runnable () {
            public void run() { 
              pointTable.setRowSelectionInterval (row, row);
            } // run
          });
      } // else if

    } // tableChanged
  } // TableDataListener class

  //////////////////////////////////////////////////////////////////////

  /** Performs automatic navigation of the specified point. */
  private void navigatePoint (NavigationPoint point) {
  
    // Perform navigation estimation
    // -----------------------------
    int boxSize = boxSlider.getValue();
    int[] offset = estimator.getOffset (dataView.getGrid(), 
      reader.getInfo().getTransform(), point.getEarthLoc(), 
      boxSize, boxSize, 0);

    // Modify point
    // ------------
    if (offset != null) {
      point.setComment (NavigationPointTable.AUTO_OK_COMMENT);
      point.setOffset (new double[] {offset[0], offset[1]});
    } // if
    else {
      point.setComment (NavigationPointTable.AUTO_FAIL_COMMENT);
    } // else

    getTableModel().changePoint (point);

  } // navigatePoint

  //////////////////////////////////////////////////////////////////////

  /** Resets the navigation of the specified point. */
  private void resetPoint (NavigationPoint point) {
  
    point.setOffset (new double[] {0, 0});
    point.setComment (NavigationPointTable.MANUAL_COMMENT);
    getTableModel().changePoint (point);

  } // resetPoint

  //////////////////////////////////////////////////////////////////////

  /** Clears the point display to show nothing. */
  private void clearPoint () {

    // Reset spinner values
    // --------------------
    rowSpinner.setValue (Double.valueOf (0));
    colSpinner.setValue (Double.valueOf (0));

    // Disable buttons
    // ---------------
    rowSpinner.setEnabled (false);
    colSpinner.setEnabled (false);
    autoButton.setEnabled (false);
    resetButton.setEnabled (false);

    // Blank the view panel
    // --------------------
    viewPanel.setView (nullView);
    viewPanel.setDefaultCursor (Cursor.getDefaultCursor());
    lightTable.setActive (false);

  } // clearPoint

  //////////////////////////////////////////////////////////////////////

  /** Updates the grid variable displayed in the view. */
  private void updateGrid () {

    if (viewPanel.getView() == nullView) return;
    String varName = (String) variableCombo.getSelectedItem();
    if (!dataView.getGrid().getName().equals (varName)) {
      try { 
        dataView.setGrid ((Grid) reader.getVariable (varName));
      } // try
      catch (IOException e) {
        throw new RuntimeException (e.getMessage());
      } // catch
    } // if

  } // updateGrid

  //////////////////////////////////////////////////////////////////////

  /** Gets the point table model. */
  private NavigationPointTableModel getTableModel () {

    return ((NavigationPointTableModel) pointTable.getModel());

  } // getTableModel

  //////////////////////////////////////////////////////////////////////

  /** Gets the selected navigation point, or null for none. */
  private NavigationPoint getSelectedPoint () {

    // Check row selection count
    // -------------------------
    int rowCount = pointTable.getSelectedRowCount();
    if (rowCount != 1) return (null);

    // Get selected point
    // ------------------
    int row = pointTable.getSelectedRow();
    if (row == -1 || row > pointTable.getRowCount()-1)
      return (null);
    else
      return (getTableModel().getPoint (row));

  } // getSelectedPoint

  //////////////////////////////////////////////////////////////////////

  /** 
   * Gets the view magnification factor.  This is the ratio of
   * screen pixels to data pixels shown in the view panel,
   * determined by the box size and view panel dimensions.
   */
  private double getMagFactor () {

    Dimension panelDims = viewPanel.getSize();
    int boxSize = boxSlider.getValue();
    double magFactor = Math.min ((double) panelDims.width/boxSize, 
      (double) panelDims.height/boxSize);

    return (magFactor);

  } // getMagFactor

  //////////////////////////////////////////////////////////////////////

  /** Updates the data shown in the view. */
  private void updateView () {

    // Get the active point
    // --------------------
    NavigationPoint point = getSelectedPoint();
    if (point == null) return;

    // Compute the view magnification factor
    // -------------------------------------
    double magFactor = getMagFactor();

    // Set the view rectangle
    // ----------------------
    if (needTransformReset) {
      DataLocation dataLoc = point.getDataLoc();
      viewPanel.stopRendering();
      try { 
        dataView.reset();
        dataView.magnify (point.getDataLoc(), magFactor);
        dataView.setSize (viewPanel.getSize());
      } // try
      catch (Exception e) {
        throw new RuntimeException (e.getMessage());
      } // catch
      needTransformReset = false;
    } // if
    
    // Normalize the view data enhancement
    // -----------------------------------
    try { dataView.normalize (STDEV_UNITS); }
    catch (Exception e) { 
      dataView.setFunction (new LinearEnhancement (new double[] {0, 0}));
    } // catch

    // Set the view affine
    // -------------------
    double[] offset = point.getOffset();
    if (dataView.getOrientationAffine().getType() != AffineTransform.TYPE_IDENTITY)
      magFactor *= -1;
    viewPanel.setImageAffine (AffineTransform.getTranslateInstance (
      -offset[Grid.COLS]*magFactor, -offset[Grid.ROWS]*magFactor));

  } // updateView

  //////////////////////////////////////////////////////////////////////

  /** Updates the navigation point displayed. */
  private void updatePoint () {

    // Clear point data
    // ----------------
    NavigationPoint point = getSelectedPoint();
    if (point == null) {
      clearPoint();
    } // if

    // Display selected point
    // ----------------------
    else {

      // Set data grid
      // -------------
      if (viewPanel.getView() == nullView) {
        viewPanel.setView (dataView);
        viewPanel.setDefaultCursor (lightTable.getCursor());
        lightTable.setActive (true);
        updateGrid();
        needTransformReset = true;
      } // if

      // Show point in view
      // ------------------
      updateView();

      // Update offset spinners
      // ----------------------
      double[] offset = point.getOffset();
      rowSpinner.setValue (offset[Grid.ROWS]);
      colSpinner.setValue (offset[Grid.COLS]);

      // Enable buttons
      // --------------
      rowSpinner.setEnabled (true);
      colSpinner.setEnabled (true);
      autoButton.setEnabled (true);
      resetButton.setEnabled (true);

    } // else

  } // updatePoint

  ////////////////////////////////////////////////////////////

  /** Handles box size slider change events. */
  private class BoxSizeListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {

      if (!boxSlider.getValueIsAdjusting()) {
        needTransformReset = true;
        updateView();
        viewPanel.repaint();
      } // if

    } // stateChanged
  } // BoxSizeListener class

  ////////////////////////////////////////////////////////////

  /** Handles row offset spinner change events. */
  private class RowOffsetListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {

      NavigationPoint point = getSelectedPoint();
      if (point != null) {
        double rowOffset = ((Double) rowSpinner.getValue()).doubleValue();
        double[] offset = point.getOffset();
        if (offset[Grid.ROWS] != rowOffset) {
          offset[Grid.ROWS] = rowOffset;
          point.setOffset (offset);
          point.setComment (NavigationPointTable.MANUAL_COMMENT);
          getTableModel().changePoint (point);
        } // if
      } // if

    } // stateChanged
  } // RowOffsetListener class

  ////////////////////////////////////////////////////////////

  /** Handles column offset spinner change events. */
  private class ColumnOffsetListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {

      NavigationPoint point = getSelectedPoint();
      if (point != null) {
        double colOffset = ((Double) colSpinner.getValue()).doubleValue();
        double[] offset = point.getOffset();
        if (offset[Grid.COLS] != colOffset) {
          offset[Grid.COLS] = colOffset;
          point.setOffset (offset);
          point.setComment (NavigationPointTable.MANUAL_COMMENT);
          getTableModel().changePoint (point);
        } // if
      } // if

    } // stateChanged
  } // ColumnOffsetListener class

  //////////////////////////////////////////////////////////////////////

  /** Handles the point table row selection. */
  private class PointListener implements ListSelectionListener {
    public void valueChanged (ListSelectionEvent event) {

      if (!event.getValueIsAdjusting()) {
        needTransformReset = true;
        updatePoint();
        boolean enabled = (pointTable.getSelectedRowCount() != 0);
        removeButton.setEnabled (enabled);
        clearButton.setEnabled (enabled);
        viewPanel.repaint();
      } // if

    } // valueChanged 
  } // PointListener class

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the list buttons. */
  private class ListButtonListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Remove selected points
      // ----------------------
      if (command.equals (REMOVE_COMMAND)) {
        int[] rows = pointTable.getSelectedRows();
        for (int i = rows.length-1; i >= 0; i--)
          getTableModel().removePoint (rows[i]);
        if (pointTable.getRowCount() != 0)
          pointTable.setRowSelectionInterval (0, 0);
      } // if
      
      // Remove all points
      // -----------------
      else if (command.equals (CLEAR_COMMAND)) {
        getTableModel().clear();
      } // else if

    } // actionPerformed
  } // RemoveButtonListener class

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the variable combo box. */
  private class VariableListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      updateGrid();
      updateView();
      viewPanel.repaint();

    } // actionPerformed
  } // VariableListener class

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the add buttons. */
  private class AddButtonListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      addCommand = event.getActionCommand();
      firePropertyChange (OPERATION_MODE_PROPERTY, null, getOperationMode());

    } // actionPerformed
  } // AddButtonListener class

  ////////////////////////////////////////////////////////////

  /** Saves the points in the panel to a file. */
  private void savePoints () {

    // Create and show save panel
    // --------------------------
    List<String> allVariableList;
    try { allVariableList = reader.getAllGrids(); }
    catch (IOException e) { e.printStackTrace(); return; }
    NavigationPointSavePanel panel = new NavigationPointSavePanel (
      reader, allVariableList, getTableModel().getPointList());
    panel.showDialog (this);

  } // savePoints

  ////////////////////////////////////////////////////////////

  /**
   * Shows the analysis panel in a dialog window.
   *
   * @param parent the parent component to use for showing dialogs.
   */
  public void showDialog (
    Component parent
  ) { 

    // Create a save action here.  We want to only enable the save action
    // if there are points in the table to save.
    final Frame frame = JOptionPane.getFrameForComponent (parent);
    Action saveAction = GUIServices.createAction ("Save...", () -> savePoints());
    saveAction.setEnabled (getTableModel().getRowCount() != 0);
    TableModelListener saveListener = event -> saveAction.setEnabled (getTableModel().getRowCount() != 0);
    getTableModel().addTableModelListener (saveListener);

    // Create close action
    // -------------------
    Action closeAction = GUIServices.createAction ("Close", null);

    // Create chooser dialog
    // ---------------------
    Component[] controls = new Component[] {
      GUIServices.getHelpButton (NavigationAnalysisPanel.class),
      Box.createHorizontalGlue()
    };
    var dialog = GUIServices.createDialog (frame, "Navigation Analysis", false,
      this, controls, new Action[] {closeAction, saveAction}, 
      new boolean[] {true, false}, true);
    //    dialog[0].setLocationRelativeTo (frame);

    // Set the dialog visible and add a close event to clean up the save button
    // listener.
    dialog.setVisible (true);
    dialog.addWindowListener (new WindowAdapter() {
      public void windowClosed (WindowEvent event) {
        getTableModel().removeTableModelListener (saveListener);
      } // windowClosed
    });

  } // showDialog

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    // Get reader
    // ----------
    EarthDataReader reader = null;
    List<String> variableList = null;
    try { 
      reader = EarthDataReaderFactory.create (argv[0]); 
      variableList = reader.getAllGrids();
    } // try
    catch (Exception e) { e.printStackTrace(); System.exit (1); }

    // Create panel
    // ------------
    final NavigationAnalysisPanel panel = new NavigationAnalysisPanel (reader, 
      variableList);

    // Show dialog
    // -----------
    SwingUtilities.invokeLater (new Runnable () {
        public void run () {
          panel.showDialog (null);
          panel.addPoint (new EarthLocation (46.9471, -124.1762));
          panel.addPoint (new EarthLocation (48.3743, -124.7044));
        } // run
      });

  } // main

  ////////////////////////////////////////////////////////////

} // NavigationAnalysisPanel class

////////////////////////////////////////////////////////////////////////
