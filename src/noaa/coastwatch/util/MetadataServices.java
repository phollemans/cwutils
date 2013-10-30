////////////////////////////////////////////////////////////////////////
/*
     FILE: MetadataServices.java
  PURPOSE: Performs various static metadata related functions.
   AUTHOR: Peter Hollemans
     DATE: 2004/09/15
  CHANGES: 2004/09/22, PFH, added append() and getCommandLine()
           2004/09/27, PFH, added toArray(), append(Object,Object)
           2005/06/23, PFH, added toString()

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import java.lang.reflect.*;

/**
 * The <code>MetadataServices</code> class defines various static
 * methods relating to metadata manipulation.
 *
 * @author Peter Hollemans
 * @since 3.1.8
 */
public class MetadataServices {

  // Constants
  // ---------
  
  /** The composite attribute value splitting string. */
  public static final String SPLIT_STRING = "\n";

  ////////////////////////////////////////////////////////////

  /**
   * Creates a collapsed version of an attribute value.  The collapsed
   * version of the attribute has no repeated composite values.  For
   * example, the string "GOES-10\nGOES-10\nGOES-12" will be collapsed
   * to simply "GOES-10\nGOES-12".
   *
   * @param attValue the attribute value to collapse.
   *
   * @return the collapsed attribute value.
   */
  public static String collapse (
    String attValue
  ) {

    // Create attribute value array
    // ----------------------------
    String[] attValueArray = attValue.split (SPLIT_STRING);
    if (attValueArray.length == 1) return (attValue);

    // Store unique attribute values in a set
    // --------------------------------------
    Set valueSet = new LinkedHashSet();
    for (int i = 0; i < attValueArray.length; i++)
      valueSet.add (attValueArray[i]);

    // Construct unique value composite
    // --------------------------------
    String[] valueArray = (String[]) valueSet.toArray (new String[] {});
    String newValue = "";
    for (int i = 0; i < valueArray.length; i++) {
      newValue += valueArray[i];
      if (i < valueArray.length-1) newValue += SPLIT_STRING;
    } // for

    return (newValue);

  } // collapse

  ////////////////////////////////////////////////////////////

  /**
   * Creates a collapsed version of an attribute value.  The collapsed
   * version of the attribute has no repeated composite values.
   *
   * @param attValue the attribute value to collapse.
   *
   * @return the collapsed attribute value.
   *
   * @see #collapse(String)
   */
  public static Object collapse (
    Object attValue
  ) {

    // Check for string
    // ----------------
    if (attValue instanceof String) 
      return (collapse ((String) attValue));

    // Check for array
    // ---------------
    if (!attValue.getClass().isArray())
      return (attValue);

    // Store unique attribute values in a set
    // --------------------------------------
    Set valueSet = new LinkedHashSet();
    for (int i = 0; i < Array.getLength (attValue); i++)
      valueSet.add (Array.get (attValue, i));

    // Construct unique value composite
    // --------------------------------
    Object newArray = Array.newInstance (
      attValue.getClass().getComponentType(), valueSet.size());
    Iterator iter = valueSet.iterator();
    for (int i = 0; iter.hasNext(); i++) {
      Object value = iter.next();
      Array.set (newArray, i, value);
    } // for

    return (newArray);

  } // collapse

  ////////////////////////////////////////////////////////////

  /**
   * Creates a formatted version of an attribute value.  The formatted
   * version is collapsed and the splitting string replaced with a
   * user-specified string.
   *
   * @param attValue the attribute value to format.
   * @param split the string to use for splitting composite values.
   *
   * @return the formatted attribute value.
   */
  public static String format (
    String attValue,
    String split
  ) {
  
    return (collapse (attValue).replaceAll (SPLIT_STRING, split));

  } // format

  ////////////////////////////////////////////////////////////

  /**
   * Appends a new value to the end of an attribute.
   *
   * @param attValue the existing attribute value, may be null.
   * @param appendValue the attribute value to append.
   *
   * @return the new composite attribute value.
   */
  public static String append (
    String attValue,
    String appendValue
  ) {

    if (attValue == null) attValue = "";
    if (appendValue == null) appendValue = "";
    if (!attValue.equals ("") && !appendValue.equals ("")) 
      attValue += SPLIT_STRING;
    attValue += appendValue;

    return (attValue);

  } // append

  ////////////////////////////////////////////////////////////

  /**
   * Converts a wrapped Java primitive to a primitive array.
   *
   * @param value the object value to convert.
   *
   * @return a Java primitive array of length 1.
   *
   * @throws ClassNotFoundException if the value class is unknown.
   */
  public static Object toArray (
    Object value
  ) throws ClassNotFoundException {

    if (value instanceof Byte) 
      return (new byte[] { ((Byte)value).byteValue() });
    else if (value instanceof Short)
      return (new short[] { ((Short)value).shortValue() });
    else if (value instanceof Integer)
      return (new int[] { ((Integer)value).intValue() });
    else if (value instanceof Long)
      return (new long[] { ((Long)value).longValue() });
    else if (value instanceof Float)
      return (new float[] { ((Float)value).floatValue() });
    else if (value instanceof Double)
      return (new double[] { ((Double)value).doubleValue() });
    else
      throw new ClassNotFoundException ("Unsupported primitive wrapper");

  } // toArray

  ////////////////////////////////////////////////////////////

  /**
   * Appends a new value to the end of an attribute.
   *
   * @param attValue the existing attribute value, may be null.
   * @param appendValue the attribute value to append.
   *
   * @return the new composite attribute value.
   */
  public static Object append (
    Object attValue,
    Object appendValue
  ) {

    // Handle null attribute values
    // ----------------------------
    if (attValue == null && appendValue == null)
      throw new IllegalArgumentException ("Null attribute values");
    else if (attValue == null) return (appendValue);
    else if (appendValue == null) return (attValue);

    // Handle string attributes
    // ------------------------
    if (attValue instanceof String && appendValue instanceof String)
      return (append ((String) attValue, (String) appendValue));

    // Convert to arrays
    // -----------------
    try {
      if (!attValue.getClass().isArray())
        attValue = toArray (attValue);
      if (!appendValue.getClass().isArray())
        appendValue = toArray (appendValue);
    } // try
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException ("Failed converting to array");
    } // catch

    // Check classes
    // -------------
    Class attClass = attValue.getClass();
    Class appendClass = appendValue.getClass();
    if (!attClass.equals (appendClass)) 
      throw new IllegalArgumentException ("Incompatible attribute classes");

    // Append arrays
    // -------------
    int attLength = Array.getLength (attValue);
    int appendLength = Array.getLength (appendValue);
    Object newArray = Array.newInstance (
      attValue.getClass().getComponentType(), attLength + appendLength);
    System.arraycopy (attValue, 0, newArray, 0, attLength);
    System.arraycopy (appendValue, 0, newArray, attLength, appendLength);

    return (newArray);

  } // append

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a command line string from program name and arguments.
   *
   * @param prog the program name.
   * @param argv the array of arguments.
   *
   * @return the assembled command line string.
   */
  public static String getCommandLine (
    String prog,
    String[] argv
  ) {

    String line = prog;
    for (int i = 0; i < argv.length; i++)
      line += " " + argv[i];

    return (line);

  } // getCommandLine

  ////////////////////////////////////////////////////////////

  /** Converts an attribute value to a string. */
  public static String toString (
    Object attValue
  ) {

    if (attValue.getClass().isArray()) {
      StringBuffer buffer = new StringBuffer();
      int length = Array.getLength (attValue);
      for (int i = 0; i < length; i++) {
        buffer.append (Array.get (attValue, i).toString());
        if (i < length-1) buffer.append (" ");
      } // for
      return (buffer.toString());
    } // if
    else {
      return (attValue.toString());
    } // else

  } // toString

  ////////////////////////////////////////////////////////////

  private MetadataServices () { }

  ////////////////////////////////////////////////////////////

} // MetadataServices class

////////////////////////////////////////////////////////////////////////
