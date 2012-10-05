////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualServices.java
  PURPOSE: Performs various static services related to visual objects.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/07
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.lang.reflect.*;
import java.util.*;

/**
 * The <code>VisualServices</code> class contains static methods
 * that are used in conjunction with visual objects.
 */
public class VisualServices {

  ////////////////////////////////////////////////////////////

  /** 
   * Returns the first method for the specified object with the
   * specified name, or null if one cannot be found.
   */
  public static Method findMethod (
    Object object,
    String methodName
  ) {
  
    Method[] methods = object.getClass().getMethods();
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getName().equals (methodName)) return (methods[i]);
    } // for
    return (null);

  } // findMethod

  ////////////////////////////////////////////////////////////

  /** 
   * Returns the get method for the specified object and property, or
   * null if no method can be found.
   */
  public static Method findGetMethod (
    Object object,
    String property
  ) {

    return (findMethod (object, "get" + 
      property.substring (0, 1).toUpperCase() + property.substring (1)));

  } // findGetMethod

  ////////////////////////////////////////////////////////////

  /** 
   * Returns the get values method for the specified object and
   * property, or null if no method can be found.
   */
  public static Method findGetValuesMethod (
    Object object,
    String property
  ) {

    return (findMethod (object, "get" + 
      property.substring (0, 1).toUpperCase() + property.substring (1) +
      "Values"));

  } // findGetValuesMethod

  ////////////////////////////////////////////////////////////

  /** 
   * Returns the set method for the specified object and property, or
   * null if no method can be found.
   */
  public static Method findSetMethod (
    Object object,
    String property
  ) {

    return (findMethod (object, "set" + 
      property.substring (0, 1).toUpperCase() + property.substring (1)));

  } // findSetMethod

  ////////////////////////////////////////////////////////////

  /** 
   * Returns true if the object has the specified property, or false
   * if not.  An object only has the property if there exist get and
   * set methods for the property.
   *
   * @param object the object to search.
   * @param property the property name.
   *
   * @return true if the property is found, or false if not.
   */
  public static boolean hasProperty (
    Object object,
    String property
  ) {

    return (findGetMethod (object, property) != null && 
      findSetMethod (object, property) != null);

  } // hasProperty

  ////////////////////////////////////////////////////////////

  /**
   * Returns a list of properties for the specified object.
   * Properties are defined as those values which have corresponding
   * get and set methods.
   */
  public static List getProperties (
    Object object
  ) {

    // Create lists of get/set methods
    // -------------------------------
    Method[] methods = object.getClass().getMethods();
    Set getMethods = new HashSet();
    Set setMethods = new HashSet();
    for (int i = 0; i < methods.length; i++) {
      String name = methods[i].getName();
      if (name.startsWith ("get"))
        getMethods.add (name);
      else if (name.startsWith ("set"))
        setMethods.add (name);
    } // for

    // Create list of properties
    // -------------------------
    List properties = new ArrayList();
    for (Iterator iter = getMethods.iterator(); iter.hasNext();) {
      String getName = (String) iter.next();
      if (setMethods.contains (getName.replaceFirst ("get", "set"))) {
        String property = getName.replaceFirst ("get", "");
        property = property.substring (0, 1).toLowerCase() + 
          property.substring (1);
        properties.add (property);
      } // if
    } // for

    return (properties);

  } // getProperties

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the class of the specified property, or null if the object
   * has no property with the specified name. 
   */
  public static Class getPropertyType ( 
    Object object, 
    String property 
  ) {

    Method getMethod = findGetMethod (object, property);
    if (getMethod == null) return (null);
    else return (getMethod.getReturnType());

  } // getPropertyType

  ////////////////////////////////////////////////////////////

  private VisualServices () {}

  ////////////////////////////////////////////////////////////

} // VisualServices

////////////////////////////////////////////////////////////////////////
