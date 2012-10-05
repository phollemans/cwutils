////////////////////////////////////////////////////////////////////////
/*
     FILE: CompositeChooser.java
  PURPOSE: Selects a set of data composite variables.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/25
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.io.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import java.util.List;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.*;

/**
 * The <code>CompositeChooser</code> class allows the user to select a
 * red, green, and blue component variable for a color composite.
 *
 * The chooser signals a change in the composite specifications by
 * firing a <code>PropertyChangeEvent</code> whose property name is
 * <code>CompositeChooser.RED_COMPONENT_PROPERTY</code>,
 * <code>CompositeChooser.GREEN_COMPONENT_PROPERTY</code>, or
 * <code>CompositeChooser.BLUE_COMPONENT_PROPERTY</code> and new value
 * contains a string for the new variable name.  The chooser signals a
 * change in composite mode by firing a
 * <code>CompositeChooser.COMPOSITE_MODE_PROPERTY</code> with a
 * boolean value.
 *
 * @see noaa.coastwatch.render.ColorComposite
 */
public class CompositeChooser
  extends JPanel
  implements TabComponent {

  // Constants
  // ---------

  /** The red component property. */
  public static final String RED_COMPONENT_PROPERTY = "redComponent";

  /** The green component property. */
  public static final String GREEN_COMPONENT_PROPERTY = "greenComponent";

  /** The blue component property. */
  public static final String BLUE_COMPONENT_PROPERTY = "blueComponent";

  /** The composite mode property. */
  public static final String COMPOSITE_MODE_PROPERTY = "compositeMode";

  /** The composite tooltip. */
  private static final String COMPOSITE_TOOLTIP = "Color Composite";

  // Variables
  // ---------    

  /** The red component combo. */
  private JComboBox redCombo;

  /** The green component combo. */
  private JComboBox greenCombo;

  /** The blue component combo. */
  private JComboBox blueCombo;

  /** The composite mode check box. */
  private JCheckBox modeCheck;

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected red component variable name. */
  public String getRedComponent () { 

    return ((String) redCombo.getSelectedItem()); 

  } // getRedComponent

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected green component variable name. */
  public String getGreenComponent () { 

    return ((String) greenCombo.getSelectedItem()); 

  } // getGreenComponent

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected blue component variable name. */
  public String getBlueComponent () { 

    return ((String) blueCombo.getSelectedItem()); 

  } // getBlueComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the composite mode flag, true if color composite mode is on
   * and false if not.
   */
  public boolean getCompositeMode () { return (modeCheck.isSelected()); }

  ////////////////////////////////////////////////////////////

  /**  
   * Creates a new composite chooser panel.
   *
   * @param variableList the list of variables to make available.
   */  
  public CompositeChooser (
    List variableList
  ) {

    // Initialize
    // ----------
    super (new GridBagLayout());

    // Create component panel
    // ----------------------
    JPanel componentPanel = new JPanel (new GridBagLayout());
    componentPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Color Components"));
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    this.add (componentPanel, gc);

    Object[] items = variableList.toArray();
    ComponentComboListener componentListener = new ComponentComboListener();
    redCombo = new JComboBox (items);
    redCombo.addActionListener (componentListener);
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.NONE,
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    componentPanel.add (new JLabel ("Red component:"), gc);
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.NONE,
      1, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    componentPanel.add (redCombo, gc);

    greenCombo = new JComboBox (items);
    greenCombo.addActionListener (componentListener);
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.NONE,
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    componentPanel.add (new JLabel ("Green component:"), gc);
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.NONE,
      1, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    componentPanel.add (greenCombo, gc);

    blueCombo = new JComboBox (items);
    blueCombo.addActionListener (componentListener);
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.NONE,
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    componentPanel.add (new JLabel ("Blue component:"), gc);
    GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.NONE,
      1, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    componentPanel.add (blueCombo, gc);
    
    // Create mode checkbox
    // --------------------
    modeCheck = new JCheckBox ("Perform color composite");
    modeCheck.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          firePropertyChange (COMPOSITE_MODE_PROPERTY, null, 
            new Boolean (modeCheck.isSelected()));
        } // actionPerformed
      });
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.NONE,
      1, 1);
    gc.anchor = GridBagConstraints.NORTHWEST;
    this.add (modeCheck, gc);

  } // CompositeChooser constructor

  ////////////////////////////////////////////////////////////

  /** Handles component combo events. */
  private class ComponentComboListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      // Get property
      // ------------
      String property = null;
      Object source = event.getSource();
      if (source == redCombo) property = RED_COMPONENT_PROPERTY;
      else if (source == greenCombo) property = GREEN_COMPONENT_PROPERTY;
      else if (source == blueCombo) property = BLUE_COMPONENT_PROPERTY;

      // Fire change event
      // -----------------
      firePropertyChange (property, null, 
        ((JComboBox) source).getSelectedItem());

    } // actionPerformed
  } // ComponentComboListener class

  ////////////////////////////////////////////////////////////

  /** Gets the composite chooser tab icon. */
  public Icon getIcon () {

    return (GUIServices.getIcon ("composite.tab"));

  } // getIcon

  ////////////////////////////////////////////////////////////

  /** Gets the composite chooser tooltip. */
  public String getToolTip () { return (COMPOSITE_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  /** Gets the composite chooser title. */
  public String getTitle () { return (null); }

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) {

    List gridList = null;
    try { 
      EarthDataReader reader = 
        EarthDataReaderFactory.create (argv[0]); 
      gridList = reader.getAllGrids();
    } // try
    catch (Exception e) { e.printStackTrace(); System.exit (1); }
    CompositeChooser chooser = new CompositeChooser (gridList);
    chooser.addPropertyChangeListener (new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          System.out.println ("got property change, name = " + 
            event.getPropertyName() + ", new value = " + event.getNewValue());
        } // propertyChange
      });
    noaa.coastwatch.gui.TestContainer.showFrame (chooser);

  } // main

  ////////////////////////////////////////////////////////////

} // CompositeChooser class

////////////////////////////////////////////////////////////////////////
