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
package noaa.coastwatch.render.feature;

// Imports
// -------
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.render.feature.Feature;
import noaa.coastwatch.util.EarthLocation;

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
  private List<EarthLocation> points;

  ////////////////////////////////////////////////////////////

  @Override
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
  public Iterator<EarthLocation> iterator () {  return (points.iterator()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new feature with no attributes.
   *
   * @param points the list of points to use for this feature, or null to
   * not create a list of points.  In this case, the child class is responsible
   * for the feature points.
   */
  protected AbstractFeature (
    List<EarthLocation> points
  ) {

    if (points != null)
      this.points = new ArrayList<EarthLocation> (points);

  } // AbstractFeature constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new feature with no attributes.  The list of points is
   * created and initialized to be empty.
   */
  protected AbstractFeature () {

    points = new ArrayList<EarthLocation>();

  } // AbstractFeature constructor

  ////////////////////////////////////////////////////////////

  /** Adds a new point to this feature. */
  public void add (EarthLocation point) { points.add (point); }

  ////////////////////////////////////////////////////////////

  /** Gets a point from this feature. */
  public EarthLocation get (int index) { return (points.get (index)); }

  ////////////////////////////////////////////////////////////

  /** Gets the total number of points in this feature. */
  public int size () { return (points.size()); }

  ////////////////////////////////////////////////////////////

  /** Removes a point from this feature. */
  public EarthLocation remove (int index) { return (points.remove (index)); }

  ////////////////////////////////////////////////////////////

  /** Adds a number of points from another feature to this feature. */
  public void addAll (Feature feature) { 

    for (Iterator<EarthLocation> iter = feature.iterator(); iter.hasNext(); )
      points.add (iter.next());

  } // addAll

  ////////////////////////////////////////////////////////////

} // AbstractFeature class

////////////////////////////////////////////////////////////////////////
