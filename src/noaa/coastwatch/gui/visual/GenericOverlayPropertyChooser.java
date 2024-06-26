////////////////////////////////////////////////////////////////////////
/*

     File: GenericOverlayPropertyChooser.java
   Author: Peter Hollemans
     Date: 2004/03/06

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
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.visual.OverlayPropertyChooser;
import noaa.coastwatch.gui.visual.VisualArray;
import noaa.coastwatch.gui.visual.VisualBoolean;
import noaa.coastwatch.gui.visual.VisualObject;
import noaa.coastwatch.gui.visual.VisualObjectFactory;
import noaa.coastwatch.gui.visual.VisualServices;
import noaa.coastwatch.render.EarthDataOverlay;

import java.util.logging.Logger;

/** 
 * The <code>GenericOverlayPropertyChooser</code> class is an
 * implementation of an <code>OverlayPropertyChooser</code> that
 * allows the user to edit the properties of an overlay.  The generic
 * chooser automatically detects the names of the desired properties
 * by examining the get/set methods and derives the label names from a
 * list of known labels.  The title is derived from the overlay class
 * name.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class GenericOverlayPropertyChooser 
  extends OverlayPropertyChooser {

  private static final Logger LOGGER = Logger.getLogger (GenericOverlayPropertyChooser.class.getName());

  // Variables
  // ---------

  /** The map used for overlay labels. */
  private static Map<String, String> labelMap;

  /** The map used for overlay property dependencies. */
  private static Map<String, List<String>> dependencyMap;

  /** The map used for ordering overlays properties. */
  private static Map<String, Integer> orderMap;

  /** The map used for overlay property visual object restrictions. */
  private static Map<String, Object> restrictionMap;

  /** The map of property to label. */
  private Map<String, JLabel> propertyMap;

  ////////////////////////////////////////////////////////////

  /** Sets up the static resources for overlay properties. */
  static {

    // Setup label map
    // ---------------
    labelMap = new HashMap<String, String>();
    labelMap.put ("color", 
                  "Drawing color");
    labelMap.put ("fillColor", 
                  "Fill color");
    labelMap.put ("transparency",
                  "Transparency");
    labelMap.put ("manualIncrement", 
                  "Set grid increment manually");
    labelMap.put ("manualLines", 
                  "Set grid lines manually");
    labelMap.put ("rows", 
                  "Row lines (pixels)");
    labelMap.put ("cols", 
                  "Column lines (pixels)");
    labelMap.put ("increment", 
                  "Grid increment (deg)");
    labelMap.put ("drawLabels", 
                  "Draw line labels");
    labelMap.put ("font",
                  "Text font");
    labelMap.put ("stroke", 
                  "Line style");
    labelMap.put ("smallPolygons", 
                  "Draw small polygons");
    labelMap.put ("levels",
                  "Levels (meters)");
    labelMap.put ("mask",
                  "Integer mask value");
    labelMap.put ("inverse",
                  "Invert mask results");
    labelMap.put ("gridName",
                  "Mask variable");
    labelMap.put ("state",
                  "Draw state borders");
    labelMap.put ("international",
                  "Draw international borders");
    labelMap.put ("dropShadow",
                  "Draw line drop shadow");
    labelMap.put ("textDropShadow",
                  "Draw text drop shadow");
    labelMap.put ("visible",
                  "Visibility");
    labelMap.put ("name",
                  "Name");
    labelMap.put ("expression",
                  "Mask expression");
    labelMap.put ("symbol",
                  "Plot symbol");
    labelMap.put ("plotSymbol",
                  "Symbol");

    // Setup dependency map
    // --------------------
    dependencyMap = new HashMap<String, List<String>>();
    dependencyMap.put ("manualIncrement",
      Arrays.asList (new String[] {"increment"}));
    dependencyMap.put ("manualLines",
      Arrays.asList (new String[] {"rows", "cols"}));
    dependencyMap.put ("drawLabels",
      Arrays.asList (new String[] {"font", "textDropShadow"}));

    // Setup order map
    // ---------------
    orderMap = new HashMap<String, Integer>();
    int n = 0;
    orderMap.put ("gridName", Integer.valueOf (n++));
    orderMap.put ("color", Integer.valueOf (n++));
    orderMap.put ("stroke", Integer.valueOf (n++));
    orderMap.put ("symbol", Integer.valueOf (n++));
    orderMap.put ("fillColor", Integer.valueOf (n++));
    orderMap.put ("transparency", Integer.valueOf (n++));
    orderMap.put ("dropShadow", Integer.valueOf (n++));
    orderMap.put ("manualIncrement", Integer.valueOf (n++));
    orderMap.put ("increment", Integer.valueOf (n++));
    orderMap.put ("manualLines", Integer.valueOf (n++));
    orderMap.put ("rows", Integer.valueOf (n++));
    orderMap.put ("cols", Integer.valueOf (n++));
    orderMap.put ("drawLabels", Integer.valueOf (n++));
    orderMap.put ("font",Integer.valueOf (n++));
    orderMap.put ("textDropShadow",Integer.valueOf (n++));
    orderMap.put ("smallPolygons", Integer.valueOf (n++));
    orderMap.put ("levels", Integer.valueOf (n++));
    orderMap.put ("mask", Integer.valueOf (n++));
    orderMap.put ("inverse", Integer.valueOf (n++));
    orderMap.put ("state", Integer.valueOf (n++));
    orderMap.put ("international", Integer.valueOf (n++));

    // Setup restriction map
    // ---------------------
    restrictionMap = new HashMap<String, Object>();
    restrictionMap.put ("increment", new Object[] {
      Integer.valueOf (1),
      Integer.valueOf (30),
      Integer.valueOf (1)});
    restrictionMap.put ("mask", new Object[] {
      Integer.valueOf (0),
      Integer.valueOf (Integer.MAX_VALUE),
      Integer.valueOf (1)});
    restrictionMap.put ("transparency", new Object[] {
      Integer.valueOf (0),
      Integer.valueOf (100),
      Integer.valueOf (5)});

  } // static

  ////////////////////////////////////////////////////////////

  /** Creates a new generic overlay property chooser panel. */
  public GenericOverlayPropertyChooser (
    EarthDataOverlay newOverlay
  ) {

    super (newOverlay);

    // Setup layout
    // ------------
    setLayout (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;

    // Loop over each exposed property
    // -------------------------------
    propertyMap = new HashMap<String, JLabel>();
    List properties = getProperties();
    List enablingProperties = new ArrayList();
    int lineIndex = 0;
    for (int i = 0; i < properties.size(); i++) {

      // Create visual component
      // -----------------------
      final String property = (String) properties.get (i);
      VisualObject visual = VisualObjectFactory.create (overlay, property);
      Component component = visual.getComponent();

      // Set restrictions
      // ----------------
      Object restrict = getRestrictions (property);
      if (restrict != null) visual.setRestrictions (restrict);

      // Create label
      // ------------
      StringBuffer labelText = new StringBuffer (getLabel (property));
      boolean isBoolean = (visual instanceof VisualBoolean);
      boolean isArray = (visual instanceof VisualArray);
      if (!isBoolean) labelText.append (":");
      JLabel label = new JLabel (labelText.toString());

      // Add listener for property changes
      // ---------------------------------
      visual.addPropertyChangeListener (new PropertyChangeListener () {
          public void propertyChange (PropertyChangeEvent event) {
            firePropertyChange (OVERLAY_PROPERTY, null, overlay);
          } // propertyChange
        });

      // Add label to map
      // ----------------
      label.setLabelFor (component);
      propertyMap.put (property, label);

      // If boolean, setup checkbox
      // --------------------------
      if (isBoolean) {

        // Set text and add to layout
        // --------------------------
        JCheckBox checkBox = (JCheckBox) component;
        checkBox.setText (label.getText());
        GUIServices.setConstraints (gc, 0, lineIndex, 2, 1, 
          GridBagConstraints.HORIZONTAL, 0, 0);
        lineIndex++;
        this.add (checkBox, gc);

        // Attach dependency listener
        // --------------------------
        List dependencies = getDependencies (property);
        if (dependencies != null) {
          checkBox.addActionListener (new ActionListener() {
              public void actionPerformed (ActionEvent event) {
                updateEnabled (property);
              } // actionPerformed
            });
          enablingProperties.add (property);
        } // if

      } // if

      // If array, setup with label above
      // --------------------------------
      else if (isArray) {
        GUIServices.setConstraints (gc, 0, lineIndex, 2, 1, 
          GridBagConstraints.HORIZONTAL, 1, 0);
        lineIndex++;
        gc.insets = new Insets (2, 0, 2, 0);
        this.add (label, gc);
        GUIServices.setConstraints (gc, 0, lineIndex, 2, 1, 
          GridBagConstraints.BOTH, 1, 1);
        lineIndex++;
        this.add (component, gc);
      } // else if

      // Otherwise, setup normal component
      // ---------------------------------
      else {
        GUIServices.setConstraints (gc, 0, lineIndex, 1, 1, 
          GridBagConstraints.HORIZONTAL, 0, 0);
        gc.insets = new Insets (2, 0, 2, 10);
        this.add (label, gc);
        gc.insets = new Insets (2, 0, 2, 0);
        GUIServices.setConstraints (gc, 1, lineIndex, 1, 1, 
          GridBagConstraints.NONE, 1, 0);
        this.add (component, gc);
        lineIndex++;
      } // else

    } // for

    // Initialize enabled status of dependent components
    // -------------------------------------------------
    for (int i = 0; i < enablingProperties.size(); i++) {
      String property = (String) enablingProperties.get (i);
      updateEnabled (property);
    } // for

  } // GenericOverlayPropertyChooser constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the component for a property name.  This can be used by
   * subclasses to modify the component in ways that cannot
   * otherwise be done through restrictions and dependencies.
   *
   * @param property the name of the property.
   *
   * @return the component for the property.
   */
  protected Component getComponent (
    String property
  ) {

    return (propertyMap.get (property).getLabelFor());

  } // getComponent

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the enabled status of dependent components.
   *
   * @param property the boolean property that enables other properties.
   */
  private void updateEnabled (
    String property
  ) {

    // Get enabled status
    // ------------------
    JCheckBox checkBox = 
      (JCheckBox) propertyMap.get (property).getLabelFor();
    boolean isEnabled = checkBox.isSelected();

    // Update dependencies
    // -------------------
    List<String> dependencies = getDependencies (property);
    for (String dependentProperty : dependencies) {
      JLabel label = propertyMap.get (dependentProperty);
      label.setEnabled (isEnabled);
      label.getLabelFor().setEnabled (isEnabled);
    } // for

  } // updateEnabled

  ////////////////////////////////////////////////////////////

  /** Gets the chooser panel title. */
  protected String getTitle () {

    String className = overlay.getClass().getName();
    String title = className.replaceFirst (".*\\.([a-zA-Z]*)Overlay", "$1");
    title = title.replaceAll ("([a-z])([A-Z])", "$1 $2");
    return (title);

  } // getTitle

  ////////////////////////////////////////////////////////////

  /** Reorders the properties according to the property order map. */
  private void reorderProperties (
    List properties
  ) {

    Collections.sort (properties, new Comparator() {
        public int compare (Object o1, Object o2) {
          Integer i1 = (Integer) orderMap.get (o1);
          Integer i2 = (Integer) orderMap.get (o2);
          if (i1 == null || i2 == null) return (0);
          else return (i1.compareTo (i2));
        } // compare
      });

  } // reorderProperties

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the list of overlay properties to display in the chooser.
   * The chooser will contain a label and visual property component
   * for each property in the list.
   */
  private List getProperties () {

    List properties = VisualServices.getProperties (overlay);
    properties.remove ("layer");
    properties.remove ("name");
    properties.remove ("visible");
    reorderProperties (properties);

    LOGGER.fine ("Property list = " + java.util.Arrays.toString (properties.toArray()));

    return (properties);

  } // getProperties

  ////////////////////////////////////////////////////////////

  /**
   * Gets the label for the specified property.  The label is used to
   * annotate the visual property component in the chooser.
   */
  public static String getLabel (
    String property
  ) {

    String label = labelMap.get (property);
    if (label == null) label = property;
    return (label);

  } // getLabel

  ////////////////////////////////////////////////////////////

  /**
   * Gets the property dependencies for the specified boolean
   * property, or null if there are no dependencies.  The dependencies
   * for a boolean property type are used to determine which
   * properties are active or inactive depending on the boolean
   * property value.
   */
  private List<String> getDependencies (String property) {

    return (dependencyMap.get (property));

  } // getDependencies

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the restrictions on the allowed visual object values for the
   * specified property, or null if there are no restrictions.  The
   * restrictions are used in the visual object
   * <code>setRestrictions()</code> method to set property-specific
   * restrictions.
   */
  private Object getRestrictions (String property) {

    return (restrictionMap.get (property));

  } // getRestrictions

  ////////////////////////////////////////////////////////////

} // GenericOverlayPropertyChooser class

////////////////////////////////////////////////////////////////////////
