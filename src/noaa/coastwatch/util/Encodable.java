////////////////////////////////////////////////////////////////////////
/*
     FILE: Encodable.java
  PURPOSE: Interface to set the functionality of all encodable subclasses.
   AUTHOR: Peter Hollemans
     DATE: 2002/06/13
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2002, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

/**
 * An encodable object is one whose representation can be encoded in a
 * data structure or array, for example as a number of integers or
 * floating point values.  The idea is that an encodable object may
 * have its encoding written to a file or data stream which will later
 * be used to recreate the object.  Normally this procedure is only
 * necessary if the encoding is more compact or less complicated than
 * the information used in the original object constructor.  To allow
 * for maximum flexibility, the encoding is specified using an object
 * whose structure must be fully documented by the implementing class.
 */
public interface Encodable {

  ////////////////////////////////////////////////////////////

  /**
   * Gets an encoded representation of this object.
   *
   * @return the object encoding.
   *
   * @see #useEncoding
   */
  public Object getEncoding ();

  ////////////////////////////////////////////////////////////

  /**
   * Uses an encoded representation of this object to recreate the
   * object contents.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public void useEncoding (
    Object obj
  );

  ////////////////////////////////////////////////////////////

} // Encodable class

////////////////////////////////////////////////////////////////////////
