////////////////////////////////////////////////////////////////////////
/*
     FILE: OrientationAffineFactory.java
  PURPOSE: Analyses earth transforms to determine need for a transform.
   AUTHOR: Peter Hollemans
     DATE: 2014/02/23
  CHANGES: 2016/01/19, PFH
           - Changes: Updated to new logging API.

  CoastWatch Software Library and Utilities
  Copyright 2014-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Import
// ------
import java.awt.geom.AffineTransform;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;
import noaa.coastwatch.util.trans.PolarStereographicProjection;
import noaa.coastwatch.util.trans.SpheroidConstants;
import noaa.coastwatch.util.trans.SwathProjection;

// Testing
import noaa.coastwatch.test.TestLogger;

////////////////////////////////////////////////////////////////////////

/**
 * The OrientationAffineFactory constructs affine transform objects to
 * correctly orient an EarthTransform so that it appears in the on-screen image
 * the way a data user would naturally expect to see it.  For example,
 * data users tend to expect north to be at the top of the image, and
 * east to be at the right, as if looking at a rectangular 2D area of the
 * globe.  In that case, the normal coordinate system would be one such 
 * that N_hat x E_hat (the north-pointing unit vector crossed with the 
 * east-pointing unit vector), under a right-handed coordinate system, 
 * would point into the screen.  The affine constructed will be such
 * that the image coordinate system's basis vectors will be rotated if
 * needed such that the cross product N_hat x E_hat points into the screen,
 * _and_ in the case of scan projections (for example a pushbroom scanner)
 * will be oriented with N_hat pointing towards the top of the image.
 *
 * To use the affine to transform data coordinates to image coordinates
 * and vice-versa, apply the orientation at the center of the data coordiantes
 * in the data coordinate frame:
 *
 * Data coordinates to image coordinates:
 * <ol>
 *   <li>Translate center of data coordinates to origin in data
 *   coordinate frame (-Cr, -Cc) where Cr is the center row and Cc
 *   is the center column coordinate.</li>
 *   <li>Apply orientation affine.</li>
 *   <li>Translate back to center in data coordinate frame
 *   (+Cr, +Cc).</li>
 *   <li>Transform data coordinates to image coordinates as normal.</li>
 * </ol>
 *
 * The image to data coordindate transform is the inverse.
 *
 * @see ImageTransform#ImageTransform(Dimension,Dimension,DataLocation,double,AffineTransform)
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class OrientationAffineFactory {

  ////////////////////////////////////////////////////////////

  /**
   * Creates an affine transform that orients a data coordinate system
   * for viewing.
   *
   * @param trans the transform to create an orientation for.
   * 
   * @return an orientation affine, or null if one could not be determined.
   */
  static public AffineTransform create (
    EarthTransform2D trans
  ) {

    AffineTransform affine = null;

    try {

      // Get north and east vectors
      // --------------------------
      double[] north = new double[2];
      double[] east = new double[2];
      int[] dims = trans.getDimensions();
      DataLocation dataLoc = new DataLocation ((dims[Grid.ROWS]-1)/2.0,
        (dims[Grid.COLS]-1)/2.0);
      EarthLocation earthLoc = trans.transform (dataLoc);
      trans.getWorldAxes (earthLoc, north, east);

      // Compute cross product term
      // --------------------------
      double z3 = north[Grid.ROWS]*east[Grid.COLS] - north[Grid.COLS]*east[Grid.ROWS];
      
      // Compute affine
      // --------------
      if (z3 < 0) {
        /**
         * In this case, the z-axis is into the screen, which is correct.  We
         * may simply need a rotation.
         */
        if (north[Grid.ROWS] > 0)
          affine = AffineTransform.getScaleInstance (-1, -1);
        else
          affine = new AffineTransform();
      } // if
      else if (z3 > 0) {
        /**
         * In this case, the z-axis is out of the screen, which is not correct.
         * We need to flip one of the basis vectors.
         */
        if (north[Grid.ROWS] > 0)
          affine = AffineTransform.getScaleInstance (-1, 1);
        else
          affine = new AffineTransform();
      } // else if
      else {
        /**
         * We should never have this case -- so there is something wrong with
         * the input vectors.  We should return null.
         */
      } // else
    
    } // try
    catch (Exception e) { }
    
    return (affine);
  
  } // create
  
  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (OrientationAffineFactory.class);

    logger.test ("create");
    MapProjection map = new PolarStereographicProjection (
      SpheroidConstants.SPHEROID_SEMI_MAJOR[12],
      SpheroidConstants.SPHEROID_SEMI_MINOR[12],
      new int[] {512, 512},
      new java.awt.geom.AffineTransform(),
      0, Math.toRadians (90), 0, 0);
    map = map.getModified (new EarthLocation (60, 180), new double[] {1, 1});
    AffineTransform orient = OrientationAffineFactory.create (map);
    assert (orient.getType() == AffineTransform.TYPE_QUADRANT_ROTATION);

    map = map.getModified (new EarthLocation (60, 0), new double[] {1, 1});
    orient = OrientationAffineFactory.create (map);
    assert (orient.getType() == AffineTransform.TYPE_IDENTITY);
    
    map = MapProjectionFactory.getInstance().create (GCTP.GEO, 0,
      new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, GCTP.WGS84,
      new int[] {512, 512}, new EarthLocation (0, 0),
      new double[] {-0.05, 0.05});
    orient = OrientationAffineFactory.create (map);
    assert (orient.getType() == AffineTransform.TYPE_FLIP);

    map = MapProjectionFactory.getInstance().create (GCTP.GEO, 0,
      new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, GCTP.WGS84,
      new int[] {512, 512}, new EarthLocation (0, 0),
      new double[] {0.05, -0.05});
    orient = OrientationAffineFactory.create (map);
    assert (orient.getType() == AffineTransform.TYPE_IDENTITY);

    SwathProjection.setNullMode (true);
    SwathProjection swath = new SwathProjection (null);
    SwathProjection.setNullMode (false);
    orient = OrientationAffineFactory.create (swath);
    assert (orient == null);
    
    logger.passed();
    
  } // main

  ////////////////////////////////////////////////////////////

} // OrientationAffineFactory class

////////////////////////////////////////////////////////////////////////
