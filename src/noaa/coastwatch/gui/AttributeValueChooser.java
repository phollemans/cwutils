////////////////////////////////////////////////////////////////////////
/*

     File: AttributeValueChooser.java
   Author: Peter Hollemans
     Date: 2017/03/12

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
import java.util.HashMap;
import java.util.Date;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Window;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import noaa.coastwatch.render.feature.Attribute;
import noaa.coastwatch.render.feature.TimeWindow;
import noaa.coastwatch.gui.value.ValuePanel;
import noaa.coastwatch.gui.value.StringValuePanel;
import noaa.coastwatch.gui.value.FloatValuePanel;
import noaa.coastwatch.gui.value.DoubleValuePanel;
import noaa.coastwatch.gui.value.ByteValuePanel;
import noaa.coastwatch.gui.value.ShortValuePanel;
import noaa.coastwatch.gui.value.IntegerValuePanel;
import noaa.coastwatch.gui.value.LongValuePanel;
import noaa.coastwatch.gui.value.DateValuePanel;
import noaa.coastwatch.gui.value.TimeWindowValuePanel;

// Testing
import java.util.ArrayList;

/**
 * The <code>AttributeValueChooser</code> provides two components: one that can
 * be used to select a feature attribute, and a second that can be used
 * to select a value for that feature attribute.  The value component 
 * automatically reconfigures based on the attribute type.<p>
 *
 * The chooser signals a change by firing one or more <code>PropertyChangeEvent</code>
 * objects with property name as follows:
 * <ul>
 *   <li> {@link #ATTRIBUTE_PROPERTY} if the attribute name is changed -- 
 *   new value is attribute.</li>
 *   <li> {@link #VALUE_PROPERTY} if the attribute value is changed -- 
 *   new value is attribute value.</li>
 *   <li> {@link #STATE_PROPERTY} if the attribute name, value, or type is changed --
 *   new value is Object array containing attribute and value.</li>
 * </ul><p>
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class AttributeValueChooser
  extends JPanel {

  // Constants
  // ---------

  /** The name for attribute property change events. */
  public static final String ATTRIBUTE_PROPERTY = "attribute";

  /** The name for value property change events. */
  public static final String VALUE_PROPERTY = "value";

  /** The name for state property change events. */
  public static final String STATE_PROPERTY = "state";

  // Variables
  // ---------

  /** The combo box for choosing the attribute. */
  private JComboBox<Attribute> attributeCombo;

  /** The previously selected attribute. */
  private Attribute prevAtt;

  /** The attribute value panel. */
  private ValuePanel valuePanel;
  
  /** The value panel container. */
  private JPanel valuePanelContainer;

  /** The map of attribute type to panel. */
  private Map<Class, ValuePanel> typeToPanelMap;

  /** The flag that denotes we are in the middle of a panel reconfiguration. */
  private boolean isReconfiguring;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new chooser.
   * 
   * @param attributeList the list of attributes to choose from.
   * @param defaultTimeWindow the default time window to use for Date attributes.
   */
  public AttributeValueChooser (
    List<Attribute> attributeList,
    TimeWindow defaultTimeWindow
  ) {
  
    this (attributeList, null, defaultTimeWindow);
  
  } // AttributeValueChooser constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new chooser.
   * 
   * @param attributeList the list of attributes to choose from.
   * @param defaultDate the default date to use for date attributes.
   */
  public AttributeValueChooser (
    List<Attribute> attributeList,
    Date defaultDate
  ) {
  
    this (attributeList, defaultDate, null);
  
  } // AttributeValueChooser constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new chooser.
   * 
   * @param attributeList the list of attributes to choose from.
   * @param defaultDate the default date to use for date attributes, or null
   * to pair date attributes with a time window instead.
   * @param defaultTimeWindow the default time window to use for date attributes,
   * or null to pair date attributes with a date instead.
   */
  private AttributeValueChooser (
    List<Attribute> attributeList,
    Date defaultDate,
    TimeWindow defaultTimeWindow
  ) {

    // Create attribute combo box
    // --------------------------
    attributeCombo = new JComboBox<Attribute> (attributeList.toArray (new Attribute[0]));
    attributeCombo.addActionListener (event -> {if (!isReconfiguring) attributeChanged();});
    this.add (attributeCombo);

    // Loop over all attributes
    // ------------------------
    typeToPanelMap = new HashMap<>();
    for (Attribute att : attributeList) {

      // Check if we already have a panel for this type
      // ----------------------------------------------
      Class attType = att.getType();
      if (!typeToPanelMap.containsKey (attType)) {

        // Create value panel
        // ------------------
        ValuePanel panel;
        if (attType.equals (String.class)) panel = new StringValuePanel();
        else if (attType.equals (Float.class)) panel = new FloatValuePanel();
        else if (attType.equals (Double.class)) panel = new DoubleValuePanel();
        else if (attType.equals (Byte.class)) panel = new ByteValuePanel();
        else if (attType.equals (Short.class)) panel = new ShortValuePanel();
        else if (attType.equals (Integer.class)) panel = new IntegerValuePanel();
        else if (attType.equals (Long.class)) panel = new LongValuePanel();
        else if (attType.equals (Date.class)) {
          if (defaultDate != null) {
            panel = new DateValuePanel();
            panel.setValue (defaultDate);
          } // if
          else if (defaultTimeWindow != null) {
            panel = new TimeWindowValuePanel();
            panel.setValue (defaultTimeWindow);
          } // else if
          else
            throw new RuntimeException ("No default value specified for date attributes");
        } // else if
        else
          throw new RuntimeException ("Unsupported attribute class: " + attType);
        typeToPanelMap.put (attType, panel);

        // Add property listener for value change
        // --------------------------------------
        panel.addPropertyChangeListener (ValuePanel.VALUE_PROPERTY, event -> {
          if (!isReconfiguring) valueChanged();
        });
          
      } // if

    } // for

    // Add the value panel and container
    // ---------------------------------
    valuePanelContainer = new JPanel (new FlowLayout (FlowLayout.LEFT, 0, 0));
    this.add (valuePanelContainer);

    // Reconfigure for initial state
    // -----------------------------
    reconfigureForAttribute ((Attribute) attributeCombo.getSelectedItem());

  } // AttributeValueChooser constructor
    
  ////////////////////////////////////////////////////////////

  /**
   * Gets the value panel specific to an attribute.
   *
   * @param att the attribute to find a value panel for.
   *
   * @return the value panel.
   *
   * @throws RuntimeException if an appropriate value panel cannot be found.
   */
  private ValuePanel getValuePanelForAtttribute (
    Attribute att
  ) {

    ValuePanel panel = typeToPanelMap.get (att.getType());
    if (panel == null)
      throw new RuntimeException ("Unsupported attribute type: " + att.getType());

    return (panel);

  } // getValuePanelForAtttribute

  ////////////////////////////////////////////////////////////
  
  /**
   * Reconfigures this panel for the apecified attribute.
   *
   * @param att the attribute to reconfigure for.
   */
  private void reconfigureForAttribute (
    Attribute att
  ) {

    isReconfiguring = true;

    // Set attribute
    // -------------
    attributeCombo.setSelectedItem (att);
    prevAtt = att;
    
    // Check if we need to replace the value panel
    // -------------------------------------------
    ValuePanel newValuePanel = getValuePanelForAtttribute (att);
    if (newValuePanel != valuePanel) {
      valuePanel = newValuePanel;
      valuePanelContainer.removeAll();
      valuePanelContainer.add (valuePanel);
      valuePanelContainer.revalidate();
      valuePanelContainer.repaint();
    } // if
    
    isReconfiguring = false;

  } // reconfigureForAttribute
  
  ////////////////////////////////////////////////////////////

  /**
   * Sets the attribute and value displayed by the chooser.  No property
   * change events are fired during this operation.
   *
   * @param att the attribute to set.
   * @param value the value to set for the attribute.
   */
  public void setAttributeAndValue (
    Attribute att,
    Object value
  ) {

    reconfigureForAttribute (att);
    valuePanel.setValue (value);

  } // setAttributeAndValue

  ////////////////////////////////////////////////////////////

  /** Handles the attribute being changed by the user. */
  private void attributeChanged() {
    
    // Check attribute has actually changed
    // ------------------------------------
    Attribute newAtt = getAttribute();
    if (newAtt != prevAtt) {

      // Update UI for new attribute
      // ---------------------------
      Object oldValue = getValue();
      reconfigureForAttribute (newAtt);

      // Signal changes
      // --------------
      firePropertyChange (ATTRIBUTE_PROPERTY, null, newAtt);
      Object newValue = getValue();
      if (!oldValue.equals (newValue)) firePropertyChange (VALUE_PROPERTY, null, newValue);
      firePropertyChange (STATE_PROPERTY, null, new Object[] {newAtt, newValue});
    
    } // if

  } // attributeChanged
    
  ////////////////////////////////////////////////////////////

  /** Handles the value being changed by the user. */
  private void valueChanged() {

    Object newValue = getValue();
    firePropertyChange (VALUE_PROPERTY, null, newValue);
    Attribute att = getAttribute();
    firePropertyChange (STATE_PROPERTY, null, new Object[] {att, newValue});

  } // valueChanged

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current attribute in the chooser.
   *
   * @return the attribute selected.
   */
  public Attribute getAttribute () {
  
    return ((Attribute) attributeCombo.getSelectedItem());
    
  } // getAttribute

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current attribute value.
   *
   * @return the value selected.
   */
  public Object getValue () {
  
    return (valuePanel.getValue());
    
  } // getValue

  ////////////////////////////////////////////////////////////

  @Override
  public String toString () {
  
    return (getAttribute() + " -> " + getValue());
  
  } // toString

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
    attList.add (new Attribute ("l2p_flags", Short.class, ""));
    attList.add (new Attribute ("coordinate_id", Integer.class, ""));
    attList.add (new Attribute ("milliseconds", Long.class, ""));
    attList.add (new Attribute ("sst", Double.class, ""));
    attList.add (new Attribute ("solar_zenith", Float.class, ""));
    attList.add (new Attribute ("quality_level", Byte.class, ""));
    attList.add (new Attribute ("time", Date.class, ""));

    TimeWindow defaultTimeWindow = new TimeWindow (new Date(), 30*60*1000);
    AttributeValueChooser chooser = new AttributeValueChooser (attList, defaultTimeWindow);
    chooser.addPropertyChangeListener (ATTRIBUTE_PROPERTY, event -> System.out.println (event));
    chooser.addPropertyChangeListener (VALUE_PROPERTY, event -> System.out.println (event));
    chooser.addPropertyChangeListener (STATE_PROPERTY, event -> System.out.println (event));

    noaa.coastwatch.gui.TestContainer.showFrame (chooser);
    Runtime.getRuntime().addShutdownHook (new Thread (() -> System.out.println (chooser)));

  } // main

  ////////////////////////////////////////////////////////////

} // AttributeValueChooser class

////////////////////////////////////////////////////////////////////////
