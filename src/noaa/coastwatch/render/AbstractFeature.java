////////////////////////////////////////////////////////////////////////
/*
     FILE: AbstractFeature.java
  PURPOSE: Abstract class for basic functionality of each feature.
   AUTHOR: Peter Hollemans
     DATE: 2005/04/29
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.util.*;

/*
 * An <code>AbstractFeature</code> can be extended by any concrete
 * <code>Feature</code> class.  It provides a default implementation
 * for the attribute interface and other methods.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public abstract class AbstractFeature 
  implements Feature {

  // Variables
  // ---------

  /** The array of attribute values. */
  private Object[] attributeArray;

  /** The list of feature points. */
  private List points;

  ////////////////////////////////////////////////////////////

  public Object getAttribute (int index) { return (attributeArray[index]); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the attribute array.
   *
   * @param attributeArray the array of feature attributes.
   */
  protected void setAttributes (
    Object[] attributeArray
  ) {

    this.attributeArray = attributeArray;

  } // setAttributes

  ////////////////////////////////////////////////////////////
    
  /** Gets an iterator over the points in this feature. */
  public Iterator iterator () {  return (points.iterator()); }

  ////////////////////////////////////////////////////////////

  /** Creates a new feature with no attributes. */
  protected AbstractFeature () {

    points = new ArrayList();

  } // AbstractFeature constructor

  ////////////////////////////////////////////////////////////

  /** Adds a new point to this feature. */
  public void add (Object point) { points.add (point); }

  ////////////////////////////////////////////////////////////

  /** Gets a point from this feature. */
  public Object get (int index) { return (points.get (index)); }

  ////////////////////////////////////////////////////////////

  /** Gets the total number of points in this feature. */
  public int size () { return (points.size()); }

  ////////////////////////////////////////////////////////////

  /** Removes a point from this feature. */
  public Object remove (int index) { return (points.remove (index)); }

  ////////////////////////////////////////////////////////////

  /** Adds a number of points from another feature to this feature. */
  public void addAll (Feature feature) { 

    for (Iterator iter = feature.iterator(); iter.hasNext(); )
      points.add (iter.next());

  } // addAll

  ////////////////////////////////////////////////////////////

} // AbstractFeature class

////////////////////////////////////////////////////////////////////////
