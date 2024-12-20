////////////////////////////////////////////////////////////////////////
/*

     File: MultiPointFeatureOverlaySymbolPanel.java
   Author: Peter Hollemans
     Date: 2017/05/20

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.SwingConstants;

import java.util.Collections;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import noaa.coastwatch.gui.visual.SymbolSwatch;
import noaa.coastwatch.render.MultiPointFeatureOverlay;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.SimpleSymbol;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.DateFormatter;

import com.braju.format.Format;

// Testing
import noaa.coastwatch.gui.visual.MultiPointFeatureOverlayPropertyChooser;

/** 
 * A <code>MultiPointFeatureOverlaySymbolPanel</code> shows a set of symbols
 * and an attribute value table for features displayed by a 
 * {@link MultiPointFeatureOverlay} object.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class MultiPointFeatureOverlaySymbolPanel
  extends JPanel {

  // Constants
  // ---------
  
  /** The date format for display. */
  private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss 'UTC '";

  // Variables
  // ---------
  
  /** The overlay to display features from in this panel. */
  private MultiPointFeatureOverlay<SimpleSymbol> multiPointOverlay;

  /** The list of symbol overlays. */
  private JList<PointFeatureOverlay> overlayList;

  /** The label displaying the number of matching features. */
  private JLabel matchingFeaturesLabel;

  /** The list of matching features. */
  private List<Feature> matchingFeatures;

  /** The list of feature attributes. */
  private List<Attribute> featureAttributes;
  
  /** The earth area for feature display. */
  private EarthArea featureArea;

  /** The model used by the table of feature attribute values. */
  private FeatureTableModel featureModel;

  /** The currently point overlay displayed in the table of features. */
  private PointFeatureOverlay displayedPointOverlay;

  /** The table of feature attribute values. */
  private JTable featureTable;
  
  /** The flag to indicate that the symbol overlay list is being internally updated. */
  private boolean isOverlayListUpdating;

  ////////////////////////////////////////////////////////////

  /** Paints an icon as it would appear in a rendered PointFeatureOverlay. */
  private class OverlayIcon
    extends SymbolSwatch {

    private Color lineColor;
    private Color fillColor;

    public OverlayIcon (
      PointFeatureOverlay overlay,
      int size
    ) {

      super (overlay.getSymbol(), size);
      lineColor = overlay.getColor();
      fillColor = overlay.getFillColor();
    
    } // OverlayIcon constructor
  
    @Override
    public void paintIcon (Component c, Graphics g, int x, int y) {

      Graphics2D g2d = (Graphics2D) g;
      Stroke saved = g2d.getStroke();

      g2d.setStroke (new BasicStroke (1.2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
      g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      symbol.setSize (size - 4);
      symbol.setBorderColor (lineColor);
      symbol.setFillColor (fillColor);
      symbol.draw (g2d, x+size/2, y+size/2);

      g2d.setStroke (saved);

    } // paintIcon

  } // OverlayIcon class

  ////////////////////////////////////////////////////////////

  /** Renders the lines in the list of symbol overlays. */
  private class SymbolOverlayRenderer
    extends JLabel
    implements ListCellRenderer<PointFeatureOverlay> {
    
    @Override
    public Component getListCellRendererComponent (
      JList<? extends PointFeatureOverlay> list,
      PointFeatureOverlay value,
      int index,
      boolean isSelected,
      boolean cellHasFocus
    ) {

      setText (value.getName() + "  ");
      setIcon (new OverlayIcon (value, list.getFont().getSize()));

      if (isSelected) {
        setBackground (list.getSelectionBackground());
        setForeground (list.getSelectionForeground());
      } // if
      else {
        setBackground (list.getBackground());
        setForeground (list.getForeground());
      } // else
      setEnabled (list.isEnabled());
      setFont (list.getFont());
      setOpaque (true);

      return (this);
      
    } // getListCellRendererComponent
     
  } // SymbolOverlayRenderer class

  ////////////////////////////////////////////////////////////

  /** Provides data for the feature table display. */
  private class FeatureTableModel
    extends AbstractTableModel {
    
    ////////////////////////////////////////////////////////

    @Override
    public int getRowCount() { return (matchingFeatures.size()); }

    ////////////////////////////////////////////////////////

    @Override
    public int getColumnCount () { return (featureAttributes.size()); }

    ////////////////////////////////////////////////////////

    @Override
    public Object getValueAt (int row, int column) {
    
      Object value = matchingFeatures.get (row).getAttribute (column);
      Class attClass = featureAttributes.get (column).getType();
      if (attClass.equals (Date.class))
        value = DateFormatter.formatDate ((Date) value, DATE_FORMAT);
      
      return (value);

    } // getValueAt

    ////////////////////////////////////////////////////////

    @Override
    public String getColumnName (int column) {
    
      return (featureAttributes.get (column).getName());
    
    } // getColumnName

    ////////////////////////////////////////////////////////

    @Override
    public Class<?> getColumnClass (int column) {
    
      Class attClass = featureAttributes.get (column).getType();
      if (attClass.equals (Date.class))
        attClass = String.class;

      return (attClass);
      
    } // getColumnClass

    ////////////////////////////////////////////////////////

  } // FeatureTableModel class

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the matching features table based on the currently selected
   * point overlay.
   *
   * @param featureDataChanged the feature data change flag, true if the
   * feature table should be updated due to a change in the underlying
   * features, or false if not.
   */
  private void updateMatchingFeatures (
    boolean featureDataChanged
  ) {

    boolean wasShowingData = (displayedPointOverlay != null);
    PointFeatureOverlay pointOverlay = overlayList.getSelectedValue();
    if ((pointOverlay != null && displayedPointOverlay != pointOverlay) || featureDataChanged) {

      // Update attributes (ie: table columns)
      // -------------------------------------
      if (!wasShowingData) featureAttributes = pointOverlay.getSource().getAttributes();

      // Update features (ie: table rows)
      // --------------------------------
      matchingFeatures = multiPointOverlay.getMatchingFeatures (pointOverlay, featureArea);
      matchingFeaturesLabel.setText ("Found " + matchingFeatures.size() + " matching feature(s)");
      displayedPointOverlay = pointOverlay;

      // Signal changes to model
      // -----------------------
      if (wasShowingData)
        featureModel.fireTableDataChanged();
      else {
        featureModel.fireTableStructureChanged();
        initTableColumns();
      } // else
      
    } // if
  
  } // updateMatchingFeatures

  ////////////////////////////////////////////////////////////

  /**
   * Gets a prototype value for the feature table cell at the specified 
   * column.
   *
   * @param column the column for the prototype.
   *
   * @return the prototype value to display in the cell.
   */
  private Object getPrototypeCellValue (
    int column
  ) {

    Object cellValue;
    Class attType = featureAttributes.get (column).getType();
    if (attType.equals (String.class)) cellValue = "1234567890";
    else if (attType.equals (Float.class)) cellValue = -Float.MAX_VALUE;
    else if (attType.equals (Double.class)) cellValue = -Double.MAX_VALUE;
    else if (attType.equals (Byte.class)) cellValue = Byte.MIN_VALUE;
    else if (attType.equals (Short.class)) cellValue = Short.MIN_VALUE;
    else if (attType.equals (Integer.class)) cellValue = Integer.MIN_VALUE;
    else if (attType.equals (Long.class)) cellValue = Long.MIN_VALUE;
    else if (attType.equals (Date.class)) cellValue = DateFormatter.formatDate (new Date(), DATE_FORMAT);
    else
      throw new RuntimeException ("Unsupported attribute class: " + attType);

    return (cellValue);
    
  } // getPrototypeCellValue

  ////////////////////////////////////////////////////////////

  /**
   * Renders a table cell data according to a format object.
   */
  private class FormatRenderer extends DefaultTableCellRenderer {

    /** The format to use for the cell value. */
    private String format;

    ////////////////////////////////////////////////////////

    /**
     * Creates a format-based renderer.
     *
     * @param format the format to use for cell values.
     */
    public FormatRenderer (
      String format
    ) {
    
      this.format = format;
      setHorizontalAlignment (SwingConstants.RIGHT);

    } // FormatRenderer constructor

    ////////////////////////////////////////////////////////

    @Override
    public void setValue (Object value) {

      try {
        if (value != null) value = Format.sprintf (format, new Object[] {value});
      } // try
      catch (IllegalArgumentException e) {}

      super.setValue (value);

    } // setValue

    ////////////////////////////////////////////////////////
    
  } // FormatRenderer class

  ////////////////////////////////////////////////////////////

  /**
   * Initializes the feature table columns based on the column names and 
   * column data types.
   */
  private void initTableColumns() {

    TableCellRenderer headerRenderer = featureTable.getTableHeader().getDefaultRenderer();

    for (int i = 0; i < featureAttributes.size(); i++) {

      // Get required header width
      // -------------------------
      TableColumn column = featureTable.getColumnModel().getColumn (i);
      Component headerComponent = headerRenderer.getTableCellRendererComponent (
        null, column.getHeaderValue(), false, false, 0, 0);
      int headerWidth = headerComponent.getPreferredSize().width;

      // Get prototype cell width
      // ------------------------
      TableCellRenderer renderer = null;
      Class colClass = featureModel.getColumnClass (i);
      if (colClass.equals (Double.class)) {
        renderer = new FormatRenderer ("%.15g");
        column.setCellRenderer (renderer);
      } // if
      else if (colClass.equals (Float.class)) {
        renderer = new FormatRenderer ("%.6g");
        column.setCellRenderer (renderer);
      } // else if
      else {
        renderer = featureTable.getDefaultRenderer (colClass);
      } // else
      Component cellComponent = renderer.getTableCellRendererComponent (
        featureTable, getPrototypeCellValue (i), false, false, 0, i);
      int cellWidth = cellComponent.getPreferredSize().width;

      // Set column width
      // ----------------
      column.setPreferredWidth (Math.max (headerWidth, cellWidth) + 5);

    } // for

  } // initTableColumns

  ////////////////////////////////////////////////////////////

  /** 
   * Signals that the feature overlay has changed in some way, and to update
   * the panel accordingly.
   */
  public void overlayChanged () {

    // Repopulate the list model if needed
    // -----------------------------------
    DefaultListModel<PointFeatureOverlay> model = (DefaultListModel<PointFeatureOverlay>) overlayList.getModel();
    List<PointFeatureOverlay<SimpleSymbol>> list = multiPointOverlay.getOverlayList();
    if (!list.equals (Collections.list (model.elements()))) {
      PointFeatureOverlay selectedOverlay = overlayList.getSelectedValue();
      isOverlayListUpdating = true;
      DefaultListModel<PointFeatureOverlay> newModel = new DefaultListModel<>();
      list.forEach (overlay -> newModel.addElement (overlay));
      overlayList.setModel (newModel);
      int index = model.indexOf (selectedOverlay);
      if (index < 0) index = 0;
      overlayList.setSelectionInterval (index, index);
      isOverlayListUpdating = false;
    } // if
  
    updateMatchingFeatures (true);
  
  } // overlayChanged

  ////////////////////////////////////////////////////////////
  
  /** 
   * Creates a new symbol display panel.
   *
   * @param multiPointOverlay the overlay the use for feature display.
   * @param area the earth area to use for feature display.
   */
  public MultiPointFeatureOverlaySymbolPanel (
    MultiPointFeatureOverlay<SimpleSymbol> multiPointOverlay,
    EarthArea area
  ) {

    this.multiPointOverlay = multiPointOverlay;
    this.featureArea = area;
    this.featureAttributes = new ArrayList<>();
    this.matchingFeatures = new ArrayList<>();
    
    // Setup layout
    // ------------
    setLayout (new BorderLayout());

    // Create left panel
    // -----------------
    JPanel leftPanel = new JPanel (new BorderLayout());
    this.add (leftPanel, BorderLayout.WEST);
    leftPanel.setBorder (new TitledBorder (new EtchedBorder(), "Symbols"));

    DefaultListModel<PointFeatureOverlay> model = new DefaultListModel<>();
    multiPointOverlay.getOverlayList().forEach (overlay -> model.addElement (overlay));

    overlayList = new JList<PointFeatureOverlay> (model);
    overlayList.setCellRenderer (new SymbolOverlayRenderer());
    overlayList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    overlayList.addListSelectionListener (event -> {
      if (!isOverlayListUpdating) updateMatchingFeatures (false);
    });
    JScrollPane listScroller = new JScrollPane (overlayList);
    leftPanel.add (listScroller, BorderLayout.CENTER);

    // Create center panel
    // -------------------
    JPanel centerPanel = new JPanel (new BorderLayout());
    this.add (centerPanel, BorderLayout.CENTER);
    centerPanel.setBorder (new TitledBorder (new EtchedBorder(), "Features"));
    featureModel = new FeatureTableModel();
    featureTable = new JTable (featureModel);
    featureTable.setAutoCreateRowSorter (true);
    JScrollPane tableScroller = new JScrollPane (featureTable,
      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    featureTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    centerPanel.add (tableScroller, BorderLayout.CENTER);

    matchingFeaturesLabel = new JLabel();
    centerPanel.add (matchingFeaturesLabel, BorderLayout.SOUTH);

    // Select first symbol
    // -------------------
    overlayList.setSelectionInterval (0, 0);

  } // MultiPointFeatureOverlaySymbolPanel constructor
  
  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    MultiPointFeatureOverlay overlay = MultiPointFeatureOverlayPropertyChooser.createTestOverlay();
    EarthArea area = new EarthArea();
    area.addAll();
    MultiPointFeatureOverlaySymbolPanel panel =
      new MultiPointFeatureOverlaySymbolPanel (overlay, area);

    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // MultiPointFeatureOverlaySymbolPanel class

////////////////////////////////////////////////////////////////////////

