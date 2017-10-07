////////////////////////////////////////////////////////////////////////
/*

     File: MultiPointFeatureOverlayStatsPanel.java
   Author: Peter Hollemans
     Date: 2017/06/29

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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.SwingConstants;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;

import noaa.coastwatch.render.MultiPointFeatureOverlay;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.render.PointFeatureSymbol;
import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.ExpressionParserFactory;
import noaa.coastwatch.util.DataIterator;

import com.braju.format.Format;

import org.nfunk.jep.JEP;
import org.nfunk.jep.SymbolTable;

// Testing
import noaa.coastwatch.gui.visual.MultiPointFeatureOverlayPropertyChooser;

/** 
 * A <code>MultiPointFeatureOverlayStatsPanel</code> shows a set of statistics 
 * of expressions computed over the visible set of overlay symbols selected by
 * a {@link MultiPointFeatureOverlay} object.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class MultiPointFeatureOverlayStatsPanel
  extends JPanel {
  
  // Constants
  // ---------
  
  /** The prompt text for new expressions. */
  private static final String NEW_EXPRESSION_PROMPT = "New expression";

  // Variables
  // ---------
  
  /** The overlay to user for feature data in this panel. */
  private MultiPointFeatureOverlay<? extends PointFeatureSymbol> multiPointOverlay;

  /** The earth area for feaure data computation. */
  private EarthArea featureArea;

  /** The list of matching features. */
  private List<Feature> matchingFeatures;

  /** The model used by the table of feature statistics values. */
  private StatsTableModel statsModel;

  /** The table of feature statistics values. */
  private JTable statsTable;

  /** The map of expression to computed stats object. */
  private Map<String, Statistics> expressionStatsMap;

  /** The list of expressions to compute. */
  private List<String> expressionList;

  /** The buttons for manipulating expressions in the table. */
  private JButton addButton;
  private JButton editButton;
  private JButton dupButton;
  private JButton removeButton;

  ////////////////////////////////////////////////////////////

  /** 
   * The <code>Column</code> class is used to enumerate the columns
   * of the statistics table.
   */
  private enum Column {

    // The column enum values
    // ----------------------
    EXPRESSION,
    COUNT,
    VALID,
    MIN,
    MAX,
    MEAN,
    STDEV;

    private static Column[] cachedValues = null;

    ////////////////////////////////////////////////////////

    @Override
    public String toString() {
      String value = super.toString();
      value = Character.toUpperCase (value.charAt(0)) + value.substring(1).toLowerCase();
      return (value);
    } // toString

    ////////////////////////////////////////////////////////

    /** Gets an object from its corresponding integer. */
    public static Column fromInt (int i) {
      if (cachedValues == null) {
        cachedValues = Column.values();
      } // if
      return (cachedValues[i]);
    } // fromInt

    ////////////////////////////////////////////////////////
    
  } // Column class

  ////////////////////////////////////////////////////////////

  /**
   * Parses the specified expression and checks for errors.
   *
   * @param expression the expression to parse.
   *
   * @return the expression parser to use for evaluation.
   *
   * @throws IllegalArgumentException if an error occurred parsing the 
   * expression, or the expression contained an unknown attribute name.
   */
  private JEP parseExpression (
    String expression
  ) {
  
    // Parse expression
    // ----------------
    JEP parser = ExpressionParserFactory.getInstance();
    parser.parseExpression (expression);
    if (parser.hasError()) {
      throw new IllegalArgumentException ("Error parsing expression: " + 
        parser.getErrorInfo());
    } // if

    // Get required attribute names
    // ----------------------------
    HashSet<String> keySet = new HashSet<> (parser.getSymbolTable().keySet());
    keySet.remove ("e");
    keySet.remove ("pi");
    keySet.remove ("nan");
    
    // Check if expression attributes are available in each feature
    // ------------------------------------------------------------
    Map<String, Integer> attNameMap = multiPointOverlay.getOverlayList().get(0).getSource().getAttributeNameMap();
    keySet.forEach (attName -> {
      if (!attNameMap.containsKey (attName))
        throw new IllegalArgumentException ("Illegal attribute name: " + attName);
    });
  
    return (parser);
  
  } // parseExpression

  ////////////////////////////////////////////////////////////

  /**
   * Shows an error dialog in response to a checking input data.
   *
   * @param errorMessage the error message.
   */
  private void showError (
    String errorMessage
  ) {
  
    String message =
      "An error occurred checking the input.\n" +
      errorMessage + "\n" +
      "Please correct the problem and try again.";
    JOptionPane.showMessageDialog (this, message, "Error", JOptionPane.ERROR_MESSAGE);
    
  } // showError

  ////////////////////////////////////////////////////////////

  /** Provides data for the stats table display. */
  private class StatsTableModel
    extends AbstractTableModel {
    
    ////////////////////////////////////////////////////////

    @Override
    public void setValueAt (
      Object value,
      int rowIndex,
      int columnIndex
    ) {

      String expression = (String) value;
      String lastExpression = expressionList.get (rowIndex);
      Statistics lastStats = expressionStatsMap.get (lastExpression);
      
      if (value.equals (NEW_EXPRESSION_PROMPT)) {
        expressionList.remove (rowIndex);
      } // if
      else {
        try {
          setExpression (rowIndex, (expression));
        } // try
        catch (Exception e) {
          showError (e.getMessage());
          expressionList.remove (rowIndex);
          expressionList.add (rowIndex, expression);
          expressionStatsMap.remove (expression);
        } // catch
      } // else

    } // setValueAt

    ////////////////////////////////////////////////////////

    @Override
    public boolean isCellEditable (
      int rowIndex,
      int columnIndex
    ) {
 
      boolean isEditable = false;
      if (columnIndex == Column.EXPRESSION.ordinal()) isEditable = true;
      
      return (isEditable);
    
    } // isCellEditable

    ////////////////////////////////////////////////////////

    @Override
    public int getRowCount() { return (expressionList.size()); }

    ////////////////////////////////////////////////////////

    @Override
    public int getColumnCount () { return (Column.values().length); }

    ////////////////////////////////////////////////////////

    @Override
    public Object getValueAt (int row, int column) {

      Object value = null;
      
      String expression = expressionList.get (row);
      Column columnObj = Column.fromInt (column);
      if (columnObj.equals (Column.EXPRESSION))
        value = expression;
      else {
        Statistics stats = expressionStatsMap.get (expression);
        if (stats != null) {
          switch (columnObj) {
          case COUNT: value = stats.getValues(); break;
          case VALID: value = stats.getValid(); break;
  //        case MIN: value = stats.getMin(); break;
  //        case MAX: value = stats.getMax(); break;
  //        case MEAN: value = stats.getMean(); break;
  //        case STDEV: value = stats.getStdev(); break;
          case MIN: value = (float) stats.getMin(); break;
          case MAX: value = (float) stats.getMax(); break;
          case MEAN: value = (float) stats.getMean(); break;
          case STDEV: value = (float) stats.getStdev(); break;
          default:
            throw new RuntimeException ("No value for column " + columnObj);
          } // switch
        } // if
      } // else

      return (value);
      
    } // getValueAt

    ////////////////////////////////////////////////////////

    @Override
    public String getColumnName (int column) {
    
      return (Column.fromInt (column).toString());
    
    } // getColumnName

    ////////////////////////////////////////////////////////

    @Override
    public Class<?> getColumnClass (int column) {
    
      Class colClass;
    
      Column columnObj = Column.fromInt (column);
      switch (columnObj) {
      case EXPRESSION: colClass = String.class; break;
      case COUNT: colClass = Integer.class; break;
      case VALID: colClass = Integer.class; break;
//      case MIN: colClass = Double.class; break;
//      case MAX: colClass = Double.class; break;
//      case MEAN: colClass = Double.class; break;
//      case STDEV: colClass = Double.class; break;
      case MIN: colClass = Float.class; break;
      case MAX: colClass = Float.class; break;
      case MEAN: colClass = Float.class; break;
      case STDEV: colClass = Float.class; break;
      default:
        throw new RuntimeException ("No class for column " + columnObj);
      } // switch

      return (colClass);
      
    } // getColumnClass

    ////////////////////////////////////////////////////////

  } // StatsTableModel class

  ////////////////////////////////////////////////////////////

  /** 
   * Computes the statistics for an expression using data from the currently
   * matching set of features.
   *
   * @param expression the expression to compute statistics for.
   *
   * @return the resulting statistics.
   *
   * @throws IllegalArgumentException if an error occurred parsing the 
   * expression, or the expression contained an unknown attribute name or
   * invalid type.
   */
  private Statistics computeStatsForExpression (
    String expression
  ) {

    // Parse expression
    // ----------------
    JEP parser = parseExpression (expression);
    Map<String, Integer> attNameMap = multiPointOverlay.getOverlayList().get(0).getSource().getAttributeNameMap();
    HashSet<String> keySet = new HashSet<> (parser.getSymbolTable().keySet());
    keySet.remove ("e");
    keySet.remove ("pi");
    keySet.remove ("nan");

    // Check if matching features is initialized
    // -----------------------------------------
    if (matchingFeatures == null) {
      matchingFeatures = multiPointOverlay.getMatchingFeatures (featureArea);
    } // if
    
    // Compute expression values
    // -------------------------
    int valueCount = matchingFeatures.size();
    final double[] valueArray = new double[valueCount];
    int index = 0;
    for (Feature feature : matchingFeatures) {
      keySet.forEach (attName -> {
        Object obj = feature.getAttribute (attNameMap.get (attName));
        Number value;
        try { value = (Number) obj; }
        catch (Exception e) {
          throw new IllegalArgumentException ("Attribute " + attName + " cannot be converted to a number value");
        } // catch
        if (value == null)
          parser.addVariable (attName, Double.NaN);
        else
          parser.addVariable (attName, value.doubleValue());
      });
      valueArray[index++] = parser.getValue();
    } // for
    
    // Compute statistics
    // ------------------
    Statistics stats = new Statistics (new DataIterator () {
      private int index = 0;
      public boolean hasNext() { return (index < valueArray.length); }
      public Double next() { return (new Double (nextDouble())); }
      public double nextDouble () { return (valueArray[index++]); }
      public void reset() { index = 0; }
      public void remove () { throw new UnsupportedOperationException(); }
    });

    return (stats);

  } // computeStatsForExpression

  ////////////////////////////////////////////////////////////

  /** Updates the stats table based on the currently visible point overlays. */
  private void updateStats () {

    if (expressionList.size() != 0) {

      // Update expression stats
      // -----------------------
      matchingFeatures = multiPointOverlay.getMatchingFeatures (featureArea);
      expressionList.forEach (expression -> {
        expressionStatsMap.put (expression, computeStatsForExpression (expression));
      });

      // Signal changes to model
      // -----------------------
      statsModel.fireTableDataChanged();

    } // if
  
  } // updateMatchingFeatures

  ////////////////////////////////////////////////////////////

  /**
   * Gets a prototype value for the stats table cell at the specified
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
    Class dataType = statsModel.getColumnClass (column);
    if (dataType.equals (String.class)) cellValue = "12345678901234567890";
    else if (dataType.equals (Float.class)) cellValue = -Float.MAX_VALUE;
    else if (dataType.equals (Double.class)) cellValue = -Double.MAX_VALUE;
    else if (dataType.equals (Byte.class)) cellValue = Byte.MIN_VALUE;
    else if (dataType.equals (Short.class)) cellValue = Short.MIN_VALUE;
    else if (dataType.equals (Integer.class)) cellValue = Integer.MIN_VALUE;
    else if (dataType.equals (Long.class)) cellValue = Long.MIN_VALUE;
    else
      throw new RuntimeException ("Unsupported attribute class: " + dataType);

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

    TableCellRenderer headerRenderer = statsTable.getTableHeader().getDefaultRenderer();
    int columns = statsTable.getColumnCount();
    for (int i = 0; i < columns; i++) {

      // Get required header width
      // -------------------------
      TableColumn column = statsTable.getColumnModel().getColumn (i);
      Component headerComponent = headerRenderer.getTableCellRendererComponent (
        null, column.getHeaderValue(), false, false, 0, 0);
      int headerWidth = headerComponent.getPreferredSize().width;

      // Get prototype cell width
      // ------------------------
      TableCellRenderer renderer = null;
      Class colClass = statsModel.getColumnClass (i);
      if (colClass.equals (Double.class)) {
        renderer = new FormatRenderer ("%.15g");
        column.setCellRenderer (renderer);
      } // if
      else if (colClass.equals (Float.class)) {
        renderer = new FormatRenderer ("%.6g");
        column.setCellRenderer (renderer);
      } // else if
      else {
        renderer = statsTable.getDefaultRenderer (colClass);
      } // else
      Component cellComponent = renderer.getTableCellRendererComponent (
        statsTable, getPrototypeCellValue (i), false, false, 0, i);
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
  public void overlayChanged () { updateStats(); }

  ////////////////////////////////////////////////////////////

  /**
   * Puts a row in the table into editing mode and highlights the expression
   * column.
   *
   * @param rowIndex the row index to start editing.
   */
  private void setRowEditing (
    int rowIndex
  ) {
  
    statsTable.editCellAt (rowIndex, Column.EXPRESSION.ordinal());

    JTextField field = (JTextField) statsTable.getEditorComponent();
    if (field != null) {
      field.requestFocusInWindow();
      field.selectAll();
    } // if
  
  } // setRowEditing

  ////////////////////////////////////////////////////////////

  /** Adds a new expression to the table. */
  private void addExpression() {
    
    expressionList.add (NEW_EXPRESSION_PROMPT);
    statsModel.fireTableDataChanged();
    setRowEditing (expressionList.size()-1);
    
  } // addExpression

  ////////////////////////////////////////////////////////////

  /** Turns on editing of the currently selected expression. */
  private void editExpression() {
  
    int row = statsTable.getSelectedRow();
    statsTable.editCellAt (row, Column.EXPRESSION.ordinal());

  } // editExpression

  ////////////////////////////////////////////////////////////

  /** Duplicates the selected expression. */
  private void dupExpression() {

    int row = statsTable.getSelectedRow();
    expressionList.add (row+1, expressionList.get (row));
    statsModel.fireTableDataChanged();
    setRowEditing (row+1);
  
  } // dupExpression

  ////////////////////////////////////////////////////////////

  /** Removes the selected expressions from the table. */
  private void removeExpression() {
  
    int[] rows = statsTable.getSelectedRows();
    
    for (int i = rows.length-1; i >= 0; i--) {
      String expression = expressionList.remove (rows[i]);
      expressionStatsMap.remove (expression);
    } // for

    statsModel.fireTableDataChanged();
    
  } // removeExpression

  ////////////////////////////////////////////////////////////
  
  /** 
   * Creates a new expression statistics display panel.
   *
   * @param multiPointOverlay the overlay to use for feature data.
   * @param area the earth area to use for feature data.
   */
  public MultiPointFeatureOverlayStatsPanel (
    MultiPointFeatureOverlay<? extends PointFeatureSymbol> multiPointOverlay,
    EarthArea area
  ) {

    this.multiPointOverlay = multiPointOverlay;
    this.featureArea = area;
    this.matchingFeatures = null;

    this.expressionStatsMap = new LinkedHashMap<>();
    this.expressionList = new ArrayList<>();

    // Setup layout
    // ------------
    setLayout (new BorderLayout());

    // Create center panel
    // -------------------
    JPanel centerPanel = new JPanel (new BorderLayout());
    this.add (centerPanel, BorderLayout.CENTER);
    statsModel = new StatsTableModel();
    statsTable = new JTable (statsModel);
    statsTable.setAutoCreateRowSorter (true);
    JScrollPane tableScroller = new JScrollPane (statsTable,
      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    statsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    centerPanel.add (tableScroller, BorderLayout.CENTER);
    initTableColumns();

    statsTable.getSelectionModel().addListSelectionListener (
      new ListSelectionListener() {
        public void valueChanged (ListSelectionEvent event) {
          if (!event.getValueIsAdjusting()) updateButtons();
        } // valueChanged
      });

    // Create side panel
    // -----------------
    JPanel sidePanel = new JPanel (new GridBagLayout());
    
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.NORTH;
    int yPos = 0;

    addButton = GUIServices.getTextButton ("Add Expression");
    addButton.addActionListener (event -> addExpression());
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (addButton, gc);
    
    editButton = GUIServices.getTextButton ("Edit");
    editButton.addActionListener (event -> editExpression());
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (editButton, gc);
    
    dupButton = GUIServices.getTextButton ("Duplicate");
    dupButton.addActionListener (event -> dupExpression());
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (dupButton, gc);
    
    removeButton = GUIServices.getTextButton ("Remove");
    removeButton.addActionListener (event -> removeExpression());
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.HORIZONTAL, 0, 0);
    sidePanel.add (removeButton, gc);
    
    GUIServices.setConstraints (gc, 0, yPos++, 1, 1, GridBagConstraints.BOTH, 0, 1);
    sidePanel.add (new JPanel(), gc);
    
    this.add (sidePanel, BorderLayout.EAST);

    updateButtons();

  } // MultiPointFeatureOverlayStatsPanel constructor
  
  ////////////////////////////////////////////////////////////

  /** Updates the buttons enabled states. */
  private void updateButtons () {

    int[] rows = statsTable.getSelectedRows();

    if (rows.length == 0) {
      addButton.setEnabled (true);
      editButton.setEnabled (false);
      dupButton.setEnabled (false);
      removeButton.setEnabled (false);
    } // if
    else if (rows.length == 1) {
      addButton.setEnabled (true);
      editButton.setEnabled (true);
      dupButton.setEnabled (true);
      removeButton.setEnabled (true);
    } // else if
    else {
      addButton.setEnabled (true);
      editButton.setEnabled (false);
      dupButton.setEnabled (false);
      removeButton.setEnabled (true);
    } // else
  
  } // updateButtons

  ////////////////////////////////////////////////////////////

  /**
   * Sets the value of an expression in the statistics table.
   *
   * @param index the index to set the expression.
   * @param expression the expression to set.
   *
   * @throws IllegalArgumentException if an error occurred parsing the
   * expression, or the expression contained an unknown attribute name or
   * invalid type.
   */
  public void setExpression (
    int index,
    String expression
  ) {

    Statistics stats = computeStatsForExpression (expression);
    if (index < expressionList.size()) expressionList.remove (index);
    expressionList.add (index, expression);
    expressionStatsMap.put (expression, stats);
    statsModel.fireTableDataChanged();

  } // setExpression

  ////////////////////////////////////////////////////////////

  /**
   * Adds an expression to the end of the statistics table.
   *
   * @param expression the expression to add.
   *
   * @throws IllegalArgumentException if an error occurred parsing the
   * expression, or the expression contained an unknown attribute name or
   * invalid type.
   */
  public void addExpression (
    String expression
  ) {

    setExpression (expressionList.size(), expression);

  } // addExpression

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
    MultiPointFeatureOverlayStatsPanel panel =
      new MultiPointFeatureOverlayStatsPanel (overlay, area);
    panel.overlayChanged();
    panel.addExpression ("sst");
    panel.addExpression ("quality_level");

    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // MultiPointFeatureOverlayStatsPanel class

////////////////////////////////////////////////////////////////////////

