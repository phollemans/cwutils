////////////////////////////////////////////////////////////////////////
/*
     FILE: GenericOverlayPropertyChooser.java
  PURPOSE: Allows the user to edit overlay properties.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/06
  CHANGES: 2005/03/21, PFH, added transparency property
           2005/04/05, PFH, made getLabel public static
           2006/11/02, PFH, added getComponent()
           2006/12/21, PFH, changed labels to drawLabels
           2006/12/24, PFH, added properties for data reference overlays
           
  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.beans.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.gui.*;

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

  // Variables
  // ---------

  /** The map used for overlay labels. */
  private static Map labelMap;

  /** The map used for overlay property dependencies. */
  private static Map dependencyMap;

  /** The map used for ordering overlays properties. */
  private static Map orderMap;

  /** The map used for overlay property visual object restrictions. */
  private static Map restrictionMap;

  /** The map of property to label. */
  private Map propertyMap;

  ////////////////////////////////////////////////////////////

  /** Sets up the static resources for overlay properties. */
  static {

    // Setup label map
    // ---------------
    labelMap = new HashMap();
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

    // Setup dependency map
    // --------------------
    dependencyMap = new HashMap();
    dependencyMap.put ("manualIncrement",
      Arrays.asList (new Object[] {"increment"}));
    dependencyMap.put ("manualLines",
      Arrays.asList (new Object[] {"rows", "cols"}));
    dependencyMap.put ("drawLabels",
      Arrays.asList (new Object[] {"font", "textDropShadow"}));

    // Setup order map
    // ---------------
    orderMap = new HashMap();
    int n = 0;
    orderMap.put ("gridName", new Integer (n++));
    orderMap.put ("color", new Integer (n++));
    orderMap.put ("stroke", new Integer (n++));
    orderMap.put ("fillColor", new Integer (n++));
    orderMap.put ("transparency", new Integer (n++));
    orderMap.put ("dropShadow", new Integer (n++));
    orderMap.put ("manualIncrement", new Integer (n++));
    orderMap.put ("increment", new Integer (n++));
    orderMap.put ("manualLines", new Integer (n++));
    orderMap.put ("rows", new Integer (n++));
    orderMap.put ("cols", new Integer (n++));
    orderMap.put ("drawLabels", new Integer (n++));
    orderMap.put ("font",new Integer (n++));
    orderMap.put ("textDropShadow",new Integer (n++));
    orderMap.put ("smallPolygons", new Integer (n++));
    orderMap.put ("levels", new Integer (n++));
    orderMap.put ("mask", new Integer (n++));
    orderMap.put ("inverse", new Integer (n++));
    orderMap.put ("state", new Integer (n++));
    orderMap.put ("international", new Integer (n++));

    // Setup restriction map
    // ---------------------
    restrictionMap = new HashMap();
    restrictionMap.put ("increment", new Object[] {
      new Integer (1),
      new Integer (30),
      new Integer (1)});
    restrictionMap.put ("mask", new Object[] {
      new Integer (0),
      new Integer (255),
      new Integer (1)});
    restrictionMap.put ("transparency", new Object[] {
      new Integer (0),
      new Integer (100),
      new Integer (5)});

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
    propertyMap = new HashMap();
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

    return (((JLabel) propertyMap.get (property)).getLabelFor());

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
      (JCheckBox) ((JLabel) propertyMap.get (property)).getLabelFor();
    boolean isEnabled = checkBox.isSelected();

    // Update dependencies
    // -------------------
    List dependencies = getDependencies (property);
    for (Iterator iter = dependencies.iterator(); iter.hasNext();) {
      String dependentProperty = (String) iter.next();
      JLabel label = (JLabel) propertyMap.get (dependentProperty);
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

    String label = (String) labelMap.get (property);
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
  private List getDependencies (String property) {

    return ((List) dependencyMap.get (property));

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
