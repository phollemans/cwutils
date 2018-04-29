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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.Color;
import java.awt.Cursor;

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
import javax.swing.SwingWorker;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.Timer;
import javax.swing.InputVerifier;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.BorderFactory;

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
import noaa.coastwatch.util.expression.ExpressionParserFactory;
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
  extends JLayeredPane {
  
  // Constants
  // ---------
  
  /** The prompt text for new expressions. */
  private static final String NEW_EXPRESSION_PROMPT = "New expression";

  // Variables
  // ---------
  
  /** The overlay to user for feature data in this panel. */
  private MultiPointFeatureOverlay<? extends PointFeatureSymbol> multiPointOverlay;

  /** The earth area for feature data computation. */
  private EarthArea featureArea;

  /**
   * The list of matching features.  When null, it means that we need to
   * update the list of matching features from the overlay.
   */
  private List<Feature> matchingFeatures;

  /** The model used by the table of feature statistics values. */
  private StatsTableModel statsModel;

  /** The table of feature statistics values. */
  private JTable statsTable;

  /**
   * The map of expression to computed stats result.  There may be expressions
   * in the list that have no corresponding stats result in the map,
   * because their stats have not been computed yet.
   */
  private Map<String, StatsResult> expressionStatsResultMap;

  /**
   * The list of expressions in the table. This list may include expressions
   * that are invalid, for example if the user entered an expression with
   * invalid attributes.  In that case the  corresponding entry in the result
   * map may be marked with an error string.
   */
  private List<String> expressionList;

  /** The buttons for manipulating expressions in the table. */
  private JButton addButton;
  private JButton editButton;
  private JButton dupButton;
  private JButton removeButton;

  /** The worker used for computing stats in the background. */
  private StatsComputation statsWorker;

  /** The panel used to block inputs during stats computation. */
  private JPanel glassPanel;

  /** The computing stats label. */
  private JLabel computingLabel;

  ////////////////////////////////////////////////////////////

  /** Holds a statistics computation result and its originating expression. */
  private static class StatsResult {

    /** The expression being calculated. */
    public String expression;

    /** The expression stats or null on error. */
    public Statistics stats;

    /** The error string if the stats failed to compute. */
    public String error;

  } // StatsResult

  ////////////////////////////////////////////////////////////

  /**
   * Implements the statistics computation as a combination of background
   * task and Swing event thread updates.  Each time an expression has its
   * statistics computed, the computation delivers the stats result object for
   * display in the data table.  Only expressions with no valid stats are
   * computed.
   *
   * @since 3.4.1
   */
  private class StatsComputation extends SwingWorker<Void, StatsResult> {

    /** The expressions that will be computed by this computation run. */
    private List<String> expressionsToCompute;

    /** The computation message timer. */
    private Timer messageTimer;

    ////////////////////////////////////////////////////////

    /** Shows the stats computation message and input blocker. */
    private void showComputationMessage () {

      glassPanel.setVisible (true);
      computingLabel.setText ("Computing statistics ...");

    } // showComputationMessage

    ////////////////////////////////////////////////////////

    /** Hides the stats computation message and input blocker. */
    private void hideComputationMessage () {

      glassPanel.setVisible (false);
      computingLabel.setText (" ");

    } // hideComputationMessage

    ////////////////////////////////////////////////////////

    /** Creates a new stats computation. */
    public StatsComputation() {

      // Build list of expressions that need stats
      // -----------------------------------------
      expressionsToCompute = new ArrayList<>();
      for (String expression : expressionList) {
        if (!expressionStatsResultMap.containsKey (expression))
          expressionsToCompute.add (expression);
      } // for

      // Create a message timer to inform the user of computation
      // --------------------------------------------------------
      messageTimer = new Timer (500, event -> showComputationMessage());
      messageTimer.setRepeats (false);

    } // StatsComputation constructor

    ////////////////////////////////////////////////////////

    @Override
    public Void doInBackground() {
    
      messageTimer.start();
      
      for (String expression : expressionsToCompute) {

        // Check if we are cancelled
        // -------------------------
        if (isCancelled()) break;

        // Get matching features if needed
        // -------------------------------
        if (matchingFeatures == null) {
          matchingFeatures = multiPointOverlay.getMatchingFeatures (featureArea);
        } // if

        // Compute stats and report
        // ------------------------
        StatsResult result = new StatsResult();
        result.expression = expression;
        try {
          result.stats = computeStatsForExpression (expression);
        } // try
        catch (Exception e) {
          result.error = e.getMessage();
        } // catch
        publish (result);

      } // for

      return (null);

    } // doInBackground

    ////////////////////////////////////////////////////////

    @Override
    protected void process (List<StatsResult> resultList) {

      // Update map of expression to stats
      // ---------------------------------
      for (StatsResult result : resultList) {
        expressionStatsResultMap.put (result.expression, result);
      } // for

      // Signal changes to model
      // -----------------------
      statsModel.fireTableDataChanged();
    
    } // process

    ////////////////////////////////////////////////////////

    @Override
    protected void done() {

      if (messageTimer.isRunning())
        messageTimer.stop();
      else
        hideComputationMessage();
        
      statsWorker = null;

    } // done
  
  } // StatsComputation class

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

  /**
   * A table cell renderer that handles expressions, either showing the
   * expression normally when the expression has valid statistics, or
   * an error mode when the expression had an error parsing.
   *
   * @since 3.4.1
   */
  private class ExpressionCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent (
      JTable table,
      Object value,
      boolean isSelected,
      boolean hasFocus,
      int row,
      int column
    ) {

      JLabel label = (JLabel) super.getTableCellRendererComponent (table, value,
        isSelected, hasFocus, row, column);

      String expression = (String) value;
      StatsResult result = expressionStatsResultMap.get (expression);
      if (result != null && result.error != null) {
        label.setText (null);
        label.setIcon (GUIServices.getIcon ("table.error"));
        label.setHorizontalAlignment (SwingConstants.CENTER);
        label.setVerticalAlignment (SwingConstants.TOP);
        label.setToolTipText (result.error);
      } // if
      else {
        label.setIcon (null);
        label.setHorizontalAlignment (SwingConstants.LEADING);
        label.setVerticalAlignment (SwingConstants.CENTER);
        label.setToolTipText (null);
      } // else

      return (label);

    } // getTableCellRendererComponent
    
  } // ExpressionCellRenderer class

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
      
      if (value.equals (NEW_EXPRESSION_PROMPT))
        expressionList.remove (rowIndex);
      else
        setExpression (rowIndex, expression);

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
        StatsResult result = expressionStatsResultMap.get (expression);
        Statistics stats = (result != null ? result.stats : null);
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

    // Set renderer for invalid expressions
    // ------------------------------------
    TableColumn expressionColumn = statsTable.getColumnModel().getColumn (Column.EXPRESSION.ordinal());
    expressionColumn.setCellRenderer (new ExpressionCellRenderer());

  } // initTableColumns

  ////////////////////////////////////////////////////////////

  /** Determines if a stats computation is in progress. */
  private boolean isComputationRunning() {
  
    return (statsWorker != null && !statsWorker.isDone());
  
  } // isComputationRunning

  ////////////////////////////////////////////////////////////

  /** Stops any stats computation in progress. */
  private void stopComputation() {

    if (isComputationRunning()) {
      statsWorker.cancel (false);
    } // if
  
  } // stopComputation

  ////////////////////////////////////////////////////////////

  /** Starts a new stats computation. */
  private void startComputation() {

    statsWorker = new StatsComputation();
    statsWorker.execute();
    
  } // startComputation

  ////////////////////////////////////////////////////////////

  /** 
   * Signals that the feature overlay has changed in some way, and to update
   * the stats panel accordingly.
   */
  public void overlayChanged() {

    stopComputation();
    matchingFeatures = null;
    expressionStatsResultMap.clear();
    if (this.isShowing()) startComputation();
  
  } // overlayChanged
  
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
      expressionStatsResultMap.remove (expression);
    } // for

    statsModel.fireTableDataChanged();
    
  } // removeExpression

  ////////////////////////////////////////////////////////////

  /**
   * Updates the panel contents if needed after the panel has
   * been made visible.  This could include the case of expressions that
   * need stats to be calculated for the first time, or possibly a change in
   * the set of features.
   *
   * @since 3.4.1
   */
  private void updateContentsAfterShown() {

    if (!isComputationRunning()) {

      boolean needsUpdate = false;

      // Need tp update if the features have been invalidated
      // ----------------------------------------------------
      if (matchingFeatures == null)
        needsUpdate = true;

      // Need to update if one of the expressions has no valid stats
      // -----------------------------------------------------------
      else {
        for (String expression : expressionList) {
          StatsResult result = expressionStatsResultMap.get (expression);
          if (result == null) {
            needsUpdate = true;
            break;
          } // if
        } // for
      } // else

      // Start computation if any updating needed
      // ----------------------------------------
      if (needsUpdate) startComputation();

    } // if
  
  } // updateContentsAfterShown

  ////////////////////////////////////////////////////////////

  @Override
  public void doLayout () {

    // We do this because the layered pane doesn't manage the layout of
    // its layers.  We have to explicitly set the bounds of all the children
    // layers to the size of the pane.

    Rectangle bounds = new Rectangle (0, 0, getWidth(), getHeight());
    Component[] componentArray = getComponents();
    for (Component component : componentArray) component.setBounds (bounds);

    super.doLayout();

  } // doLayout

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

    this.expressionStatsResultMap = new LinkedHashMap<>();
    this.expressionList = new ArrayList<>();
  
    // Update panel contents when shown
    // --------------------------------
    this.addComponentListener (new ComponentAdapter() {
      public void componentShown (ComponentEvent e) { updateContentsAfterShown(); }
    });

    // Setup layout
    // ------------
    JPanel contentPanel = new JPanel (new BorderLayout());
    this.add (contentPanel, JLayeredPane.DEFAULT_LAYER);

    // Create center panel
    // -------------------
    JPanel centerPanel = new JPanel (new BorderLayout());
    contentPanel.add (centerPanel, BorderLayout.CENTER);
    statsModel = new StatsTableModel();
    statsTable = new JTable (statsModel);
    statsTable.setAutoCreateRowSorter (true);
    JScrollPane tableScroller = new JScrollPane (statsTable,
      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    statsTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);
    JPanel tablePanel = new JPanel (new BorderLayout());
    tablePanel.add (tableScroller, BorderLayout.CENTER);
    tablePanel.setBorder (BorderFactory.createEmptyBorder (3, 3, 3, 3));
    centerPanel.add (tablePanel, BorderLayout.CENTER);
    initTableColumns();

    computingLabel = new JLabel (" ");
    tablePanel.add (computingLabel, BorderLayout.SOUTH);

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
    
    contentPanel.add (sidePanel, BorderLayout.EAST);

    updateButtons();

    // Create input blocking panel
    // ---------------------------
    glassPanel = new JPanel();
    glassPanel.setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));
    glassPanel.setOpaque (false);
    glassPanel.addMouseListener (new MouseAdapter() {});
    glassPanel.setInputVerifier (new InputVerifier() {
      public boolean verify (JComponent input) { return (false); }
    });
    glassPanel.setVisible (false);
    add (glassPanel, JLayeredPane.POPUP_LAYER);


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
   */
  private void setExpression (
    int index,
    String expression
  ) {

    if (index < expressionList.size()) expressionList.remove (index);
    expressionList.add (index, expression);
    if (this.isShowing()) startComputation();

  } // setExpression

  ////////////////////////////////////////////////////////////

  /**
   * Adds an expression to the end of the statistics table.
   *
   * @param expression the expression to add.
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

