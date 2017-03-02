////////////////////////////////////////////////////////////////////////
/*

     File: CompositeChooser.java
   Author: Peter Hollemans
     Date: 2004/05/25

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;

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
 *
 * @author Peter Hollemans
 * @since 3.1.7
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
  private JComboBox<String> redCombo;

  /** The green component combo. */
  private JComboBox<String> greenCombo;

  /** The blue component combo. */
  private JComboBox<String> blueCombo;

  /** The composite mode check box. */
  private JCheckBox modeCheck;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected green component variable name.
   *
   * @return the red variable name.
   */
  public String getRedComponent () {

    return ((String) redCombo.getSelectedItem()); 

  } // getRedComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected green component variable name.
   *
   * @return the green variable name.
   */
  public String getGreenComponent () { 

    return ((String) greenCombo.getSelectedItem()); 

  } // getGreenComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected blue component variable name.
   *
   * @return the blue variable name.
   */
  public String getBlueComponent () {

    return ((String) blueCombo.getSelectedItem()); 

  } // getBlueComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the composite mode flag.
   *
   * @return the mode flag, true if color composite mode is on
   * or false if not.
   */
  public boolean getCompositeMode () { return (modeCheck.isSelected()); }

  ////////////////////////////////////////////////////////////

  /**  
   * Creates a new composite chooser panel.
   *
   * @param variableList the list of variables to make available.
   */  
  public CompositeChooser (
    List<String> variableList
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

    String[] items = variableList.toArray (new String[]{});
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

  /** 
   * Disposes of any resources used by this chooser. 
   * 
   * @since 3.3.1
   */
  public void dispose () {

    /**
     * We do the following because it was found that retaining references
     * in this class were transitively holding references to the other
     * panels and data caches that were associated with this class.
     */
    removeAll();
    for (JComboBox combo : new JComboBox[] {redCombo, greenCombo, blueCombo}) {
      for (ActionListener listener : combo.getActionListeners()) {
        combo.removeActionListener (listener);
      } // for
    } // for
    redCombo = null;
    greenCombo = null;
    blueCombo = null;

  } // dispose
  
  ////////////////////////////////////////////////////////////

  @Override
  public Icon getIcon () { return (GUIServices.getIcon ("composite.tab")); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getToolTip () { return (COMPOSITE_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getTitle () { return (null); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
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
