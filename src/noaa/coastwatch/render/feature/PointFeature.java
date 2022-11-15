////////////////////////////////////////////////////////////////////////
/*

     File: PointFeature.java
   Author: Peter Hollemans
     Date: 2005/05/03

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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.render.feature.AbstractFeature;
import noaa.coastwatch.util.EarthLocation;

/*
 * A <code>PointFeature</code> is a single geographic point with
 * attributes.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class PointFeature 
  extends AbstractFeature {

  // Variables
  // ---------
  
  /** The single geographic point. */
  private EarthLocation point;

  ////////////////////////////////////////////////////////////
    
  /** Gets an iterator over the one point in this feature. */
  public Iterator iterator () { 

    return (Arrays.asList (new Object[] {point}).iterator());

  } // iterator

  ////////////////////////////////////////////////////////////

  /** Gets the earth location point. */
  public EarthLocation getPoint () { return (point); }

  ////////////////////////////////////////////////////////////

  /** Sets the earth location point. */
  public void setPoint (EarthLocation point) { this.point = point; }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new point feature with attributes. 
   * 
   * @param point thae point feature location.
   * @param attributeArray the array of feature attributes, or null for no
   * attributes.
   */
  public PointFeature (
    EarthLocation point,
    Object[] attributeArray
  ) {

    super (null);
    this.point = point;
    setAttributes (attributeArray);

  } // PointFeature constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new point feature with no attributes. 
   * 
   * @param point thae point feature location.
   */
  public PointFeature (
    EarthLocation point
  ) {

    super (null);
    this.point = point;

  } // PointFeature constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void add (EarthLocation point) { throw new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

  @Override
  public EarthLocation get (
    int index
  ) {
  
    if (index == 0) return (point);
    else throw (new IndexOutOfBoundsException());
  
  } // get

  ////////////////////////////////////////////////////////////

  @Override
  public int size () { return (1); }

  ////////////////////////////////////////////////////////////

  @Override
  public EarthLocation remove (int index) { throw new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

  @Override
  public void addAll (Feature feature) { new UnsupportedOperationException(); }

  ////////////////////////////////////////////////////////////

} // PointFeature class

////////////////////////////////////////////////////////////////////////
