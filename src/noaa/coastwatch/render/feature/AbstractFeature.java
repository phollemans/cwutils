////////////////////////////////////////////////////////////////////////
/*

     File: AbstractFeature.java
   Author: Peter Hollemans
     Date: 2005/04/29

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
  protected List<EarthLocation> points;

  /** The precalculated hash code. */
  private int hash;

  /** The hashed flag, true if the hash code is valid. */
  private boolean isHashed = false;

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
    
  @Override
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

  @Override
  public int getAttributeCount() { return (attributeArray.length); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean equals (Object obj) {
  
    boolean isEqual = true;

    // Check object type
    // -----------------
    if (!(obj instanceof AbstractFeature)) {
      isEqual = false;
    } // if
    
    else {
      AbstractFeature feature = (AbstractFeature) obj;

      // Check points
      // ------------
      if (this.size() != feature.size()) {
        isEqual = false;
      } // if
      else {
        for (int i = 0; i < this.size(); i++) {
          if (!this.get (i).equals (feature.get (i))) {
            isEqual = false;
            break;
          } // if
        } // for
      } // else

      // Check attribute values
      // ----------------------
      if (isEqual) {
        if (this.getAttributeCount() != feature.getAttributeCount()) {
          isEqual = false;
        } // if
        else {
          for (int i = 0; i < this.getAttributeCount(); i++) {
            Object attValue = getAttribute (i);
            Object otherAttValue = feature.getAttribute (i);
            if (!(attValue == null && otherAttValue == null)) {   // both values are null
              if (
                (attValue != null && otherAttValue == null) ||    // one value is null
                (attValue == null && otherAttValue != null) ||    // other value is null
                (!attValue.equals (otherAttValue))                // values are non-null but unequal
              ) {
                isEqual = false;
                break;
              } // if
            } // if
          } // for
        } // else
      } // if
    
    } // else
    
    return (isEqual);

  } // equals

  ////////////////////////////////////////////////////////////

  @Override
  public int hashCode () {

    // Get cached hash code
    // --------------------
    if (isHashed) return (hash);

    // Calculate hash code
    // -------------------
    hash = 0;
    for (int i = 0; i < this.size(); i++) {
      EarthLocation loc = get (i);
      long bits = Double.doubleToLongBits (loc.lat);
      hash = hash ^ ((int) (bits^(bits >>> 32)));
      bits = Double.doubleToLongBits (loc.lon);
      hash = hash ^ ((int) (bits^(bits >>> 32)));
    } // for

    for (int i = 0; i < getAttributeCount(); i++) {
      Object attValue = getAttribute (i);
      if (attValue != null) hash = hash ^ attValue.hashCode();
    } // for

    // Set flag and return
    // -------------------
    isHashed = true;
    return (hash);

  } // hashCode

  ////////////////////////////////////////////////////////////

} // AbstractFeature class

////////////////////////////////////////////////////////////////////////
