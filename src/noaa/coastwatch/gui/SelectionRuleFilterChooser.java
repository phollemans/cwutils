////////////////////////////////////////////////////////////////////////
/*
     FILE: SelectionRuleFilterChooser.java
  PURPOSE: Allows the user to manipulate a list of selection rules.
   AUTHOR: Peter Hollemans
     DATE: 2017/01/26
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2017, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Container;
import java.awt.Window;
import java.awt.Dimension;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;
import java.util.TimeZone;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerModel;
import javax.swing.JSpinner;
import javax.swing.Box;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.border.EtchedBorder;

import noaa.coastwatch.render.feature.SelectionRuleFilter;
import noaa.coastwatch.render.feature.SelectionRuleFilter.FilterMode;
import noaa.coastwatch.render.feature.SelectionRule;
import noaa.coastwatch.render.feature.AttributeRule;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.render.feature.TextRule;
import noaa.coastwatch.render.feature.NumberRule;
import noaa.coastwatch.render.feature.DateRule;
import noaa.coastwatch.gui.visual.ComponentList;
import noaa.coastwatch.gui.visual.ComponentProducer;
import noaa.coastwatch.util.DateFormatter;
import noaa.coastwatch.gui.GUIServices;

/**
 * The <code>SelectionRuleFilterChooser</code> class is a panel that allows the 
 * user to manipulate a {@link SelectionRuleFilter}.  If no filter is set, an
 * initial default filter is created and displayed.
 *
 * The chooser signals a change in the filter by firing a
 * <code>PropertyChangeEvent</code> with property name {@link #FILTER_PROPERTY}, 
 * and new value containing an object of type {@link SelectionRuleFilter}.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class SelectionRuleFilterChooser
  extends JPanel {

  // Constants
  // ---------

  /** The name for filter property change events. */
  public static final String FILTER_PROPERTY = "filter";

  // Variables
  // ---------

  /** The feature filter to be modified. */
  private SelectionRuleFilter filter;

  /** The attribute list to use for filter rules. */
  private List<Attribute> attributeList;

  /** The attribute name map for filter rules. */
  private Map<String, Integer> attributeNameMap;

  /** The combo box for selecting the feature filter type. */
  private JComboBox<FilterMode> filterModeCombo;
  
  /** The component list showing the list of filter line panels. */
  private ComponentList<FilterLine> componentList;

  /** The flag that denotes we are in the middle of a panel reconfiguration. */
  private boolean isReconfiguring;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new empty feature filter chooser.
   * 
   * @param attributeList the list of attributes to use for the filtering
   * rules.  
   * @param attributeNameMap the map of attribute name to index for features
   * to be filtered.
   */
  public SelectionRuleFilterChooser (
    List<Attribute> attributeList,
    Map<String, Integer> attributeNameMap
  ) {

    super (new BorderLayout());

    // Create top panel with filter mode selector
    // ------------------------------------------
    JPanel topPanel = new JPanel (new FlowLayout (FlowLayout.LEFT, 2, 0));
    topPanel.add (new JLabel ("If a feature"));
    FilterMode[] modes = new FilterMode[] {FilterMode.MATCHES_ANY, FilterMode.MATCHES_ALL};
    filterModeCombo = new JComboBox<FilterMode> (modes);
    filterModeCombo.addActionListener (event -> { if (!isReconfiguring) modeChanged();});
    topPanel.add (filterModeCombo);
    topPanel.add (new JLabel ("of the following conditions:"));
    this.add (topPanel, BorderLayout.NORTH);
    
    // Create center panel with list of rules
    // --------------------------------------
    componentList = new ComponentList<FilterLine>();
    componentList.setSelectable (false);
    componentList.setBorder (new EtchedBorder());
    Box centerPanel = Box.createHorizontalBox();
    centerPanel.add (Box.createHorizontalStrut (20));
    centerPanel.add (componentList);
    centerPanel.add (Box.createHorizontalStrut (20));
    this.add (centerPanel, BorderLayout.CENTER);
    
    // Final setup
    // -----------
    this.attributeList = attributeList;
    this.attributeNameMap = attributeNameMap;
    setFilter (createDefaultFilter());

  } // SelectionRuleFilterChooser constructor

  ////////////////////////////////////////////////////////////

  /**
   * Updates the remove button enabled flag on the first component in the
   * list.
   */
  private void updateRemovable () {

    int elements = componentList.getElements();
    if (elements > 0) {
      componentList.getElement (0).setRemovable (elements != 1);
    } // if
    
  } // checkRemoveEnabled

  ////////////////////////////////////////////////////////////

  /**
   * Reconfigures this chooser to display the data from the specified
   * filter.
   *
   * @param newFilter the new filter to use in this chooser.
   */
  private void reconfigureForFilter (
    SelectionRuleFilter newFilter
  ) {
  
    isReconfiguring = true;
    filter = newFilter;
  
    // Set filter mode
    // ---------------
    filterModeCombo.setSelectedItem (filter.getMode());

    // Set filter lines
    // ----------------
    componentList.clear();
    filter.forEach (rule -> {
      try { addFilterLine (new FilterLine ((AttributeRule) rule)); }
      catch (ClassCastException e) {
      
        // Here, we ignore any rules that are not attribute rules,
        // since we currently do not support the editing of other
        // rule types.  The non-attribute rules should remain intact
        // in the filter.
      
      } // catch
    });

    isReconfiguring = false;

  } // reconfigureForFilter
  
  ////////////////////////////////////////////////////////////

  /** Handles a change in filter mode. */
  private void modeChanged () {
  
    filter.setMode ((FilterMode) filterModeCombo.getSelectedItem());
    signalFilterChanged();
    
  } // modeChanged

  ////////////////////////////////////////////////////////////

  /**
   * Creates a default filter to be shown when the chooser is first created.
   *
   * @return the default filter.
   */
  private SelectionRuleFilter createDefaultFilter () {
  
    SelectionRuleFilter filter = new SelectionRuleFilter();
    filter.setMode (FilterMode.MATCHES_ANY);
    filter.add (createDefaultRule (attributeList.get (0)));
    return (filter);
  
  } // createDefaultFilter

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the top level window size for this panel. We need to do this
   * whenever the panel changes size due to a filter row being added or
   * removed.
   */
  private void updateWindowSize () {

    Window root = javax.swing.SwingUtilities.windowForComponent (this);
    if (root != null) {
      Dimension minSize = root.getMinimumSize();
      Dimension size = root.getSize();
      Dimension newSize = new Dimension (size.width, minSize.height);
      root.setPreferredSize (newSize);
      root.validate();
      root.pack();
    } // if
  
  } // updateWindowSize

  ////////////////////////////////////////////////////////////

  /**
   * Adds a new default filter line to the list after the specified line.
   *
   * @param line the line to add a new filter line directly after.
   */
  private void addDefaultFilterLineAfter (
    FilterLine line
  ) {
  
    int index = componentList.indexOf (line);
    FilterLine newLine = new FilterLine (createDefaultRule (attributeList.get (0)));
    componentList.addElement (index+1, newLine);
    filter.add (filter.indexOf (line.getRule())+1, newLine.getRule());
    updateRemovable();
    updateWindowSize();
    signalFilterChanged();

  } // addDefaultFilterLineAfter

  ////////////////////////////////////////////////////////////

  /**
   * Adds the specified filter line to the end of the list.
   *
   * @param line the line to add.
   */
  private void addFilterLine (
    FilterLine line
  ) {
  
    componentList.addElement (line);
    // Note that we currently do not modify the filter in this routine,
    // because the only place that this is called is from within the
    // reconfigure method, which is reconfiguring based on an existing
    // filter that we shouldn't modify.
    updateRemovable();
    updateWindowSize();

  } // addFilterLine

  ////////////////////////////////////////////////////////////

  /**
   * Removes the specified filter line from the list.
   *
   * @param line the line to remove from the list.
   */
  private void removeFilterLine (
    FilterLine line
  ) {

    boolean isRemoved = componentList.removeElement (line);
    if (!isRemoved) throw new RuntimeException ("Failed to remove filter line");
    filter.remove (line.getRule());
    updateRemovable();
    updateWindowSize();
    signalFilterChanged();

  } // removeFilterLine

  ////////////////////////////////////////////////////////////

  /**
   * Sets the feature filter displayed and manipulated by this chooser.
   *
   * @param filter the filter to use.
   */
  public void setFilter (
    SelectionRuleFilter filter
  ) {

    reconfigureForFilter (filter);

  } // setFilter

  ////////////////////////////////////////////////////////////

  /**
   * Gets the currently configured filter.
   *
   * @return the filter created/modified by the user.
   */
  public SelectionRuleFilter getFilter () { return (filter); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a default attribute rule for a given attribute.
   *
   * @param att the attribute to create a default rule for.
   *
   * @return the default attribute rule.
   *
   * @throws RuntimeException if the attribute type is not supported.
   */
  private AttributeRule createDefaultRule (
    Attribute att
  ) {

    String attName = att.getName();
    Class attType = att.getType();
    AttributeRule rule;
    if (attType.equals (String.class)) {
      rule = new TextRule (attName, attributeNameMap, "");
    } // if
    else if (Number.class.isAssignableFrom (attType)) {
      rule = new NumberRule (attName, attributeNameMap, 0);
    } // else if
    else if (attType.equals (Date.class)) {
      rule = new DateRule (attName, attributeNameMap, new Date (0));
    } // else if
    else
      throw new RuntimeException ("Unsupported attribute type: " + attType);

    return (rule);
  
  } // createDefaultRule

  ////////////////////////////////////////////////////////////

  /** Signals that the filter contained in the chooser has been altered. */
  private void signalFilterChanged() {
  
    firePropertyChange (FILTER_PROPERTY, null, filter);

  } // signalFilterChanged
  
  ////////////////////////////////////////////////////////////

  /**
   * A <code>FilterLine</code> provides a component to show in the list of
   * selection rules.  It holds the details about the selection rule and buttons
   * for adding and removing rules.
   */
  private class FilterLine
    implements ComponentProducer {
  
    // Constants
    // ---------
  
    /** The date format for display and parsing. */
    private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss 'UTC    '";
  
    // Variables
    // ---------
    
    /** The panel to show for this line in the list. */
    private JPanel panel;

    /** The attribute rule for this line. */
    private AttributeRule rule;

    /** The attribute combo box. */
    private JComboBox<Attribute> attributeCombo;

    /** The operator combo box. */
    private JComboBox<Enum> operatorCombo;
    
    /** The data entry text field. */
    private JTextField dataField;
    
    /** The date spinner. */
    private JSpinner dateSpinner;
    
    /** The label that goes after the data field for optional extra words. */
    private JLabel postDataLabel;
    
    /** The current attribute selected in the attribute combo. */
    private Attribute currentAtt;
    
    /** The flag that denotes we are in the middle of a panel reconfiguration. */
    private boolean isReconfiguring;

    /** The remove button. */
    private JButton removeButton;
    
    ////////////////////////////////////////////////////////

    /**
     * Sets this filter line as removable from the list.  When the line is
     * removable, its remove button is enabled, otherwise it is disabled.
     * By default a line's remove button is enabled.
     *
     * @param removableFlag the removable flag, true if the line is
     * removable or false if not.
     */
    public void setRemovable (
      boolean removableFlag
    ) {
    
      removeButton.setEnabled (removableFlag);
      
    } // setRemovable

    ////////////////////////////////////////////////////////

    /** 
     * Creates a new filter line based on the specified rule.
     *
     * @param rule the attribute rule to use for creating components for this
     * filter line.
     */
    public FilterLine (
      AttributeRule rule
    ) {
    
      // Create panel
      // ------------
      panel = new JPanel (new GridBagLayout());
      GridBagConstraints gc = new GridBagConstraints();
      gc.anchor = GridBagConstraints.WEST;
      int xPos = 0;
      
      attributeCombo = new JComboBox<Attribute> (attributeList.toArray (new Attribute[0]));
      attributeCombo.addActionListener (event -> {if (!isReconfiguring) attributeChanged();});
      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.NONE, 0, 0);
      panel.add (attributeCombo, gc);

      operatorCombo = new JComboBox<Enum>();
      operatorCombo.addActionListener (event -> {if (!isReconfiguring) operatorChanged();});
      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.NONE, 0, 0);
      panel.add (operatorCombo, gc);

      dataField = new JTextField();
      DataVerifier verifier = new DataVerifier();
      dataField.setInputVerifier (verifier);
      dataField.addActionListener (verifier);
      dataField.setColumns (10);
      dataField.setMinimumSize (dataField.getPreferredSize());
      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.NONE, 0, 0);
      panel.add (dataField, gc);

      SpinnerModel dateModel = new SpinnerDateModel();
      dateModel.setValue (new Date());
      dateSpinner = new JSpinner (dateModel);
      JSpinner.DateEditor editor = new JSpinner.DateEditor (dateSpinner, DATE_FORMAT);
      SimpleDateFormat dateFormat = editor.getFormat();
      dateFormat.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
      dateSpinner.setEditor (editor);
      dateSpinner.addChangeListener (listener -> {if (!isReconfiguring) dateChanged();});
      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.NONE, 0, 0);
      panel.add (dateSpinner, gc);

      postDataLabel = new JLabel();
      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.NONE, 0, 0);
      panel.add (postDataLabel, gc);

      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.NONE, 0, 0);
      panel.add (Box.createHorizontalStrut (20), gc);
      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      panel.add (new JPanel(), gc);

      JButton addButton = GUIServices.getIconButton ("list.add");
      GUIServices.setSquare (addButton);
      addButton.addActionListener (event -> addDefaultFilterLineAfter (FilterLine.this));
      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.NONE, 0, 0);
      panel.add (addButton, gc);

      removeButton = GUIServices.getIconButton ("list.remove");
      GUIServices.setSquare (removeButton);
      removeButton.addActionListener (event -> removeFilterLine (FilterLine.this));
      GUIServices.setConstraints (gc, xPos++, 0, 1, 1, GridBagConstraints.NONE, 0, 0);
      panel.add (removeButton, gc);
      
      // Reconfigure panel content
      // -------------------------
      reconfigureForRule (rule);

    } // Filterline constructor

    ////////////////////////////////////////////////////////

    /** Handles input verification for the data text field. */
    private class DataVerifier extends InputVerifier implements ActionListener {

      //////////////////////////////////////////////////

      @Override
      public boolean verify (JComponent input) {
        boolean isValid = true;
        if (Number.class.isAssignableFrom (currentAtt.getType())) {
          try { Double.parseDouble (dataField.getText()); }
          catch (NumberFormatException e) { isValid = false; }
        } // if
        return (isValid);
      } // verify
      
      //////////////////////////////////////////////////

      @Override
      public boolean shouldYieldFocus (JComponent input) {
        boolean isValid = verify (input);
        if (!isValid) {
          Toolkit.getDefaultToolkit().beep();
          dataField.setText (rule.getValue().toString());
          dataField.selectAll();
        } // if
        dataChanged();
        return (isValid);
      } // shouldYieldFocus
      
      //////////////////////////////////////////////////

      @Override
      public void actionPerformed (ActionEvent e) {
        shouldYieldFocus (dataField);
        dataChanged();
      } // actionPerformed

      //////////////////////////////////////////////////
      
    } // DataVerifier class

    ////////////////////////////////////////////////////////

    /** Handles the rule attribute name being changed. */
    private void attributeChanged() {
      
      // Detect a change in attribute type
      // ---------------------------------
      Attribute att = (Attribute) attributeCombo.getSelectedItem();
      Class attType = att.getType();
      boolean isDifferentType = true;
      if (currentAtt != null) {
        if (currentAtt.getType().equals (attType)) isDifferentType = false;
      } // if
      currentAtt = att;

      // Create new rule if needed
      // -------------------------
      if (isDifferentType) {
        AttributeRule newRule = createDefaultRule (att);
        filter.set (filter.indexOf (rule), newRule);
        reconfigureForRule (newRule);
      } // if

      // Change attribute for existing rule
      // ----------------------------------
      else {
        rule.setAttribute (att.getName());
      } // else

      signalFilterChanged();

    } // attributeChanged
    
    ////////////////////////////////////////////////////////

    /** Handles the rule operator being changed. */
    private void operatorChanged() {

      rule.setOperator ((Enum) operatorCombo.getSelectedItem());
      signalFilterChanged();

    } // operatorChanged

    ////////////////////////////////////////////////////////

    /** Handles the rule date value being changed. */
    private void dateChanged() {

      Object newValue = dateSpinner.getValue();
      rule.setValue (newValue);
      signalFilterChanged();
      
    } // dateChanged

    ////////////////////////////////////////////////////////

    /** Handles the rule data value being changed. */
    private void dataChanged() {

      Class attType = currentAtt.getType();
      String text = dataField.getText();
      Object newValue;

      // Get new string value
      // --------------------
      if (attType.equals (String.class)) {
        newValue = text;
      } // if

      // Get new number value
      // --------------------
      else if (Number.class.isAssignableFrom (attType)) {
        Double doubleValue = null;
        Integer intValue = null;
        try { doubleValue = Double.parseDouble (text); }
        catch (NumberFormatException e) { }
        try { intValue = Integer.parseInt (text); }
        catch (NumberFormatException e) { }
        if (intValue != null)
          newValue = intValue;
        else if (doubleValue != null)
          newValue = doubleValue;
        else
          throw new RuntimeException ("Cannot parse '" + text + "' to number");
      } // else if

      // Unsupported type
      // ----------------
      else
        throw new RuntimeException ("Unsupported attribute type: " + attType);

      rule.setValue (newValue);
      signalFilterChanged();

    } // dataChanged

    ////////////////////////////////////////////////////////

    /**
     * Reconfigures this line to display the data from the specified
     * rule.  
     *
     * @param newRule the new rule to use in this line.
     */
    private void reconfigureForRule (
      AttributeRule newRule
    ) {

      isReconfiguring = true;
      rule = newRule;

      // Update panel components for rule
      // --------------------------------
      String attName = rule.getAttribute();
      if (!((Attribute) attributeCombo.getSelectedItem()).getName().equals (attName)) {
        currentAtt = attributeList.stream().filter (att -> att.getName().equals (attName)).findFirst().get();
        attributeCombo.setSelectedItem (currentAtt);
      } // if
      else {
        currentAtt = (Attribute) attributeCombo.getSelectedItem();
      } // else
      operatorCombo.removeAllItems();
      Arrays.stream (rule.operators()).forEach (op -> operatorCombo.addItem (op));
      operatorCombo.setSelectedItem (rule.getOperator());
      if (currentAtt.getType().equals (Date.class)) {
        dataField.setVisible (false);
        dateSpinner.setVisible (true);
        dateSpinner.setValue (rule.getValue());
      } // if
      else {
        dateSpinner.setVisible (false);
        dataField.setVisible (true);
        dataField.setText (rule.getValue().toString());
      } // else

      isReconfiguring = false;
      
    } // reconfigureForRule

    ////////////////////////////////////////////////////////

    @Override
    public Component getComponent() { return (panel); }

    ////////////////////////////////////////////////////////

    @Override
    public void refreshComponent() { throw new UnsupportedOperationException(); }
    
    ////////////////////////////////////////////////////////

    /** 
     * Gets the attribute rule for this line.
     *
     * @return the rule for this line.
     */
    public AttributeRule getRule() { return (rule); }

    ////////////////////////////////////////////////////////

  } // FilterLine class

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {
  
    List<Attribute> attList = new ArrayList<Attribute>();
    attList.add (new Attribute ("platform_type", Byte.class, ""));
    attList.add (new Attribute ("platform_id", String.class, ""));
    attList.add (new Attribute ("sst", Double.class, ""));
    attList.add (new Attribute ("quality_level", Byte.class, ""));
    attList.add (new Attribute ("time", Date.class, ""));
    Map<String, Integer> attNameMap = new HashMap<String, Integer>();
    int i = 0;
    attNameMap.put ("platform_type", i++);
    attNameMap.put ("platform_id", i++);
    attNameMap.put ("sst", i++);
    attNameMap.put ("quality_level", i++);
    attNameMap.put ("time", i++);

    SelectionRuleFilterChooser chooser = new SelectionRuleFilterChooser (attList, attNameMap);

    SelectionRuleFilter filter = new SelectionRuleFilter();
    filter.add (new TextRule ("platform_id", attNameMap, "NMM"));
    filter.add (new NumberRule ("quality_level", attNameMap, 5));
    filter.add (new DateRule ("time", attNameMap, new Date()));
    chooser.setFilter (filter);

    noaa.coastwatch.gui.TestContainer.showFrame (chooser);
    Runtime.getRuntime().addShutdownHook (new Thread (() -> System.out.println (chooser.getFilter())));

  } // main

  ////////////////////////////////////////////////////////////

} // SelectionRuleFilterChooser class

////////////////////////////////////////////////////////////////////////
