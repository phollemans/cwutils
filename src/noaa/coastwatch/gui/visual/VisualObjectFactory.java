////////////////////////////////////////////////////////////////////////
/*

     File: VisualObjectFactory.java
   Author: Peter Hollemans
     Date: 2004/03/01

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
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import noaa.coastwatch.gui.visual.VisualArray;
import noaa.coastwatch.gui.visual.VisualBoolean;
import noaa.coastwatch.gui.visual.VisualChoice;
import noaa.coastwatch.gui.visual.VisualColor;
import noaa.coastwatch.gui.visual.VisualFont;
import noaa.coastwatch.gui.visual.VisualInteger;
import noaa.coastwatch.gui.visual.VisualObject;
import noaa.coastwatch.gui.visual.VisualServices;
import noaa.coastwatch.gui.visual.VisualString;
import noaa.coastwatch.gui.visual.VisualStroke;
import noaa.coastwatch.gui.visual.VisualSymbol;
import jahuwaldt.plot.PlotSymbol;

/**
 * The <code>VisualObjectFactory</code> class contains static methods
 * that create new visual objects based on an object and a property name.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualObjectFactory {

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new visual object.  The specified object is searched
   * for get/set methods based on the property name, and a new visual
   * object is created that controls the property value.
   *
   * @param object the object to create the visual object from.
   * @param property the property name to use.
   *
   * @return the newly created visual object.
   * 
   * @throws IllegalArgumentException if the property cannot be found, or
   * the property type is not supported by any known visual object.
   * @throws RuntimeException if an error occurred in the visual object 
   * constructor, or when retrieving the property value from the object.
   */
  public static VisualObject create (
    Object object,
    String property
  ) {

    // Find get/set methods
    // --------------------
    Method getMethod = VisualServices.findGetMethod (object, property);
    Method setMethod = VisualServices.findSetMethod (object, property);
    if (getMethod == null || setMethod == null) {
      throw (new IllegalArgumentException (
        "Cannot find object get/set methods for " + property + " property"));
    } // if
    Method getValuesMethod = VisualServices.findGetValuesMethod (object, 
      property);

    // Get property value
    // ------------------
    Object propertyValue;
    try { propertyValue = getMethod.invoke (object, (Object[]) null); }
    catch (Exception e) { throw (new RuntimeException (e)); }
    
    // Find visual object class
    // ------------------------
    Class propertyClass = getMethod.getReturnType();
    Class visualObjectClass;
    if (getValuesMethod != null) {
      visualObjectClass = VisualChoice.class;
      propertyClass = Object.class;
    } // if
    else if (propertyClass.equals (Color.class))
      visualObjectClass = VisualColor.class;
    else if (propertyClass.equals (Stroke.class))
      visualObjectClass = VisualStroke.class;
    else if (propertyClass.equals (Font.class)) 
      visualObjectClass = VisualFont.class;
    else if (propertyClass.equals (Boolean.TYPE)) {
      visualObjectClass = VisualBoolean.class;
      propertyClass = Boolean.class;
    } // else if
    else if (propertyClass.equals (Integer.TYPE)) {
      visualObjectClass = VisualInteger.class;
      propertyClass = Integer.class;
    } // else if
    else if (propertyClass.equals (String.class))
      visualObjectClass = VisualString.class;
    else if (propertyClass.equals (PlotSymbol.class))
      visualObjectClass = VisualSymbol.class;
    else if (propertyClass.isArray()) {
      visualObjectClass = VisualArray.class;
      propertyClass = Object.class;
    } // else if
    else {
      throw (new IllegalArgumentException (
        "Unsupported property class " + propertyClass.getName()));
    } // else

    // Create visual object
    // --------------------
    VisualObject visualObject;
    try {
      Constructor cons = visualObjectClass.getConstructor (
        new Class[] {propertyClass});
      visualObject = (VisualObject) cons.newInstance (
        new Object[] {propertyValue});
    } // try
    catch (Exception e) { throw (new RuntimeException (e)); }

    // Add choice restrictions
    // -----------------------
    if (getValuesMethod != null) {
      Object values;
      try { values = getValuesMethod.invoke (object, (Object[]) null); }
      catch (Exception e) { throw (new RuntimeException (e)); }
      visualObject.setRestrictions (values);
    } // if

    // Attach listener to visual object
    // --------------------------------
    final Method fSetMethod = setMethod;
    final Object fObject = object;
    final Component fComponent = visualObject.getComponent();
    visualObject.addPropertyChangeListener (new PropertyChangeListener() {
        public void propertyChange (PropertyChangeEvent event) {
          Object newValue = event.getNewValue();
          try { fSetMethod.invoke (fObject, new Object[] {newValue}); }
          catch (Exception e) { throw (new RuntimeException (e)); }
        } // propertyChange
      });

    return (visualObject);

  } // create

  ////////////////////////////////////////////////////////////

  private VisualObjectFactory () {}

  ////////////////////////////////////////////////////////////

} // VisualObjectFactory

////////////////////////////////////////////////////////////////////////
