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

import java.util.List;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

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
  private JList<String> redList;
  private JList<String> greenList;
  private JList<String> blueList;
  private JToggleButton modeButton;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected green component variable name.
   *
   * @return the red variable name.
   */
  public String getRedComponent () {

    return (redList.getSelectedValue());

  } // getRedComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected green component variable name.
   *
   * @return the green variable name.
   */
  public String getGreenComponent () { 

    return (greenList.getSelectedValue());

  } // getGreenComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the currently selected blue component variable name.
   *
   * @return the blue variable name.
   */
  public String getBlueComponent () {

    return (blueList.getSelectedValue());

  } // getBlueComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the composite mode flag.
   *
   * @return the mode flag, true if color composite mode is on
   * or false if not.
   */
  public boolean getCompositeMode () { return (modeButton.isSelected()); }

  ////////////////////////////////////////////////////////////

  /**  
   * Creates a new composite chooser panel.
   *
   * @param variableList the list of variables to make available.
   */  
  public CompositeChooser (
    List<String> variableList
  ) {

    super (new GridBagLayout());

    String[] items = variableList.toArray (new String[]{});

    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;

    var redPanel = new JPanel (new BorderLayout());
    redPanel.setBorder (new TitledBorder (new EtchedBorder(), "Red Component"));
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.BOTH, 1, 1);
    this.add (redPanel, gc);

    redList = new JList<> (items);
    redList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    redList.setSelectedIndex (0);
    redList.addListSelectionListener (event -> selectionChangedEvent (event));
    redPanel.add (new JScrollPane (redList), BorderLayout.CENTER);

    var greenPanel = new JPanel (new BorderLayout());
    greenPanel.setBorder (new TitledBorder (new EtchedBorder(), "Green Component"));
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.BOTH, 1, 1);
    this.add (greenPanel, gc);

    greenList = new JList<> (items);
    greenList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    greenList.setSelectedIndex (0);
    greenList.addListSelectionListener (event -> selectionChangedEvent (event));
    greenPanel.add (new JScrollPane (greenList), BorderLayout.CENTER);

    var bluePanel = new JPanel (new BorderLayout());
    bluePanel.setBorder (new TitledBorder (new EtchedBorder(), "Blue Component"));
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.BOTH, 1, 1);
    this.add (bluePanel, gc);

    blueList = new JList<> (items);
    blueList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    blueList.setSelectedIndex (0);
    blueList.addListSelectionListener (event -> selectionChangedEvent (event));
    bluePanel.add (new JScrollPane (blueList), BorderLayout.CENTER);

    modeButton = new JToggleButton ("Toggle Color Composite Mode (OFF)");
    modeButton.addActionListener (event -> modeChangedEvent (event));

    gc.anchor = GridBagConstraints.NORTHWEST;
    gc.insets = new Insets (2, 0, 2, 0);
    GUIServices.setConstraints (gc, 0, 3, 1, 1, GridBagConstraints.NONE, 0, 0);
    this.add (modeButton, gc);

  } // CompositeChooser constructor

  ////////////////////////////////////////////////////////////

  private void modeChangedEvent (ActionEvent event) {

    var modeOn = modeButton.isSelected();
    modeButton.setText ("Toggle Color Composite Mode (" + (modeOn ? "ON" : "OFF") + ")");
    firePropertyChange (COMPOSITE_MODE_PROPERTY, null, Boolean.valueOf (modeOn));

  } // modeChangedEvent

  ////////////////////////////////////////////////////////////

  private void selectionChangedEvent (ListSelectionEvent event) {

    if (!event.getValueIsAdjusting()) {

      String property = null;
      var source = (JList<String>) event.getSource();

      if (source == redList) property = RED_COMPONENT_PROPERTY;
      else if (source == greenList) property = GREEN_COMPONENT_PROPERTY;
      else if (source == blueList) property = BLUE_COMPONENT_PROPERTY;

      var value = source.getSelectedValue();
      if (value != null) firePropertyChange (property, null, value);

    } // if

  } // selectionChangedEvent

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
    for (var list : List.of (redList, greenList, blueList)) {
      for (var listener : list.getListSelectionListeners()) list.removeListSelectionListener (listener);
    } // for
    redList = null;
    greenList = null;
    blueList = null;

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
    chooser.addPropertyChangeListener (event -> {
      System.out.println ("Property " + event.getPropertyName() + " changed to " + event.getNewValue());
    });
    noaa.coastwatch.gui.TestContainer.showFrame (chooser);

  } // main

  ////////////////////////////////////////////////////////////

} // CompositeChooser class

////////////////////////////////////////////////////////////////////////
