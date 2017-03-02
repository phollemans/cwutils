////////////////////////////////////////////////////////////////////////
/*
     FILE: PointFeature.java
  PURPOSE: Contains one feature point with attributes.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/03
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
   * @param attributeArray the array of feature attributes.
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

} // PointFeature class

////////////////////////////////////////////////////////////////////////
