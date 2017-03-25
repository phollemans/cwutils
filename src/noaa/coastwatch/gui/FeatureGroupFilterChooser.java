////////////////////////////////////////////////////////////////////////
/*

     File: FeatureGroupFilterChooser.java
   Author: Peter Hollemans
     Date: 2017/03/07

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
import java.util.List;
import java.util.Map;
import java.util.Date;

import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;

import noaa.coastwatch.render.feature.FeatureGroupFilter;
import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.gui.AttributeValueChooser;

// Testing
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The <code>FeatureGroupFilterChooser</code> class is a panel that allows the 
 * user to manipulate a {@link FeatureGroupFilter}.  If no filter is set, an
 * initial default filter is created and displayed.
 *
 * The chooser signals a change in the filter by firing a
 * <code>PropertyChangeEvent</code> with property name {@link #FILTER_PROPERTY}, 
 * and new value containing an object of type {@link FeatureGroupFilter}.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class FeatureGroupFilterChooser
  extends JPanel {

  // Constants
  // ---------

  /** The name for filter property change events. */
  public static final String FILTER_PROPERTY = "filter";

  // Variables
  // ---------

  /** The feature group filter to be modified. */
  private FeatureGroupFilter filter;

  /** The attribute list to use for filter rules. */
  private List<Attribute> attributeList;

  /** The attribute name map for filter rules. */
  private Map<String, Integer> attributeNameMap;

  /** The combo box for selecting the group attribute name. */
  private JComboBox<Attribute> groupAttCombo;

  /** The attribute value chooser for filter attribute and target value. */
  private AttributeValueChooser attChooser;

  /** The flag that denotes we are in the middle of a panel reconfiguration. */
  private boolean isReconfiguring;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new empty feature group filter chooser.
   * 
   * @param attributeList the list of attributes to use for the feature grouping
   * and selection.
   * rules.  
   * @param attributeNameMap the map of attribute name to index for features
   * to be filtered.
   * @param defaultDate the default date to use for date attributes.
   */
  public FeatureGroupFilterChooser (
    List<Attribute> attributeList,
    Map<String, Integer> attributeNameMap,
    Date defaultDate
  ) {

    setLayout (new BoxLayout (this, BoxLayout.Y_AXIS));

    // Create first line with group attribute selector
    // -----------------------------------------------
    JPanel firstLine = new JPanel (new FlowLayout (FlowLayout.LEFT, 2, 0));
    firstLine.add (new JLabel ("Group features by"));
    Attribute[] attributeArray = attributeList.toArray (new Attribute[0]);
    groupAttCombo = new JComboBox<Attribute> (attributeArray);
    groupAttCombo.addActionListener (event -> { if (!isReconfiguring) groupAttChanged();});
    firstLine.add (groupAttCombo);
    firstLine.add (new JLabel ("and"));
    this.add (firstLine);

    // Create second line with filter attribute and target
    // ---------------------------------------------------
    attChooser = new AttributeValueChooser (attributeList, defaultDate);
    attChooser.setLayout (new FlowLayout (FlowLayout.LEFT, 2, 0));
    attChooser.add (new JLabel ("select feature with"), 0);
    attChooser.add (new JLabel ("closest to"), 2);
    attChooser.addPropertyChangeListener (AttributeValueChooser.ATTRIBUTE_PROPERTY, event -> {
      if (!isReconfiguring) filterAttChanged();
    });
    attChooser.addPropertyChangeListener (AttributeValueChooser.VALUE_PROPERTY, event -> {
      if (!isReconfiguring) filterValueChanged();
    });
    this.add (attChooser);
    
    // Final setup
    // -----------
    this.attributeList = attributeList;
    this.attributeNameMap = attributeNameMap;
    setFilter (createDefaultFilter());

  } // FeatureGroupFilterChooser constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a default filter to be shown when the chooser is first created.
   *
   * @return the default filter.
   */
  private FeatureGroupFilter createDefaultFilter () {
  
    String groupAttName = attributeList.get (0).getName();
    String filterAttName = attChooser.getAttribute().getName();
    Object targetAttValue = attChooser.getValue();
    FeatureGroupFilter filter = new FeatureGroupFilter (groupAttName,
      attributeNameMap, filterAttName, targetAttValue);

    return (filter);
  
  } // createDefaultFilter

  ////////////////////////////////////////////////////////////

  /**
   * Reconfigures this chooser to display the data from the specified
   * filter.
   *
   * @param newFilter the new filter to use in this chooser.
   */
  private void reconfigureForFilter (
    FeatureGroupFilter newFilter
  ) {
    
    isReconfiguring = true;
    filter = newFilter;

    // Set group combo
    // ---------------
    String groupAttName = filter.getGroupAttribute();
    Attribute groupAtt = attributeList
      .stream()
      .filter (att -> att.getName().equals (groupAttName))
      .findFirst().orElse (null);
    if (groupAtt == null)
      throw new RuntimeException ("No attribute found with name: " + groupAttName);
    groupAttCombo.setSelectedItem (groupAtt);

    // Set filter attribute and value
    // ------------------------------
    String filterAttName = filter.getFilterAttribute();
    Attribute filterAtt = attributeList
      .stream()
      .filter (att -> att.getName().equals (filterAttName))
      .findFirst().orElse (null);
    if (filterAtt == null)
      throw new RuntimeException ("No attribute found with name: " + filterAttName);
    attChooser.setAttributeAndValue (filterAtt, filter.getTargetValue());

    isReconfiguring = false;

  } // reconfigureForFilter

  ////////////////////////////////////////////////////////////

  /**
   * Gets the currently configured filter.
   *
   * @return the filter created/modified by the user.
   */
  public FeatureGroupFilter getFilter () { return (filter); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the group filter displayed and manipulated by this chooser.
   *
   * @param filter the filter to use.
   */
  public void setFilter (
    FeatureGroupFilter filter
  ) {
  
    reconfigureForFilter (filter);

  } // setFilter

  ////////////////////////////////////////////////////////////

  /** Signals that the filter contained in the chooser has been altered. */
  private void signalFilterChanged() {
  
    firePropertyChange (FILTER_PROPERTY, null, filter);

  } // signalFilterChanged

  ////////////////////////////////////////////////////////////

  /** Handles a change in the grouping attribute chosen. */
  private void groupAttChanged () {
  
    Attribute att = (Attribute) groupAttCombo.getSelectedItem();
    String newAttName = att.getName();
    if (!newAttName.equals (filter.getGroupAttribute())) {
      filter.setGroupAttribute (newAttName);
      signalFilterChanged();
    } // if
  
  } // groupAttChanged

  ////////////////////////////////////////////////////////////

  /** Handles a change in the filtering attribute chosen. */
  private void filterAttChanged () {
  
    String newAttName = attChooser.getAttribute().getName();
    if (!newAttName.equals (filter.getFilterAttribute())) {
      filter.setFilterAttribute (newAttName);
      signalFilterChanged();
    } // if
  
  } // filterAttChanged

  ////////////////////////////////////////////////////////////

  /** Handles a change in the filtering target value chosen. */
  private void filterValueChanged () {
  
    Object newValue = attChooser.getValue();
    if (!newValue.equals (filter.getTargetValue())) {
      filter.setTargetValue (newValue);
      signalFilterChanged();
    } // if
  
  } // filterValueChanged

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

    FeatureGroupFilterChooser chooser = new FeatureGroupFilterChooser (attList, attNameMap, new Date());
    chooser.addPropertyChangeListener (FILTER_PROPERTY, event -> System.out.println (event.getNewValue()));


/*
    SelectionRuleFilter filter = new SelectionRuleFilter();
    filter.add (new TextRule ("platform_id", attNameMap, "NMM"));
    filter.add (new NumberRule ("quality_level", attNameMap, 5));
    filter.add (new DateRule ("time", attNameMap, new Date()));
    chooser.setFilter (filter);
*/

    noaa.coastwatch.gui.TestContainer.showFrame (chooser);
    Runtime.getRuntime().addShutdownHook (new Thread (() -> System.out.println (chooser.getFilter())));

  } // main

  ////////////////////////////////////////////////////////////

} // FeatureGroupFilterChooser class

////////////////////////////////////////////////////////////////////////
