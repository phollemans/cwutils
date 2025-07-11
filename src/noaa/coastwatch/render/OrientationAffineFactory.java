////////////////////////////////////////////////////////////////////////
/*

     File: OrientationAffineFactory.java
   Author: Peter Hollemans
     Date: 2014/02/23

  CoastWatch Software Library and Utilities
  Copyright (c) 2014 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.render;

// Import
// ------

import java.awt.geom.AffineTransform;
import java.util.List;

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

import java.util.Arrays;
import java.util.logging.Logger;

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

  private static final Logger LOGGER = Logger.getLogger (OrientationAffineFactory.class.getName());

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
      double[] zero = new double[] {0, 0}; 
      double[] north = new double[] {0, 0};
      double[] east = new double[] {0, 0};
      int[] dims = trans.getDimensions();
      var candidateList = List.of (
        new DataLocation ((dims[Grid.ROWS]-1)/2.0, (dims[Grid.COLS]-1)/2.0),
        new DataLocation ((dims[Grid.ROWS]-1)*0.25, (dims[Grid.COLS]-1)/2.0),
        new DataLocation ((dims[Grid.ROWS]-1)*0.75, (dims[Grid.COLS]-1)/2.0),
        new DataLocation ((dims[Grid.ROWS]-1)/2.0, (dims[Grid.COLS]-1)*0.25),
        new DataLocation ((dims[Grid.ROWS]-1)/2.0, (dims[Grid.COLS]-1)*0.75)
      );
      int candidateIndex = 0;
      DataLocation dataLoc = null;
      EarthLocation earthLoc = null;
      while (candidateIndex < candidateList.size() && (Arrays.equals (north, zero) || Arrays.equals (east, zero))) {
        dataLoc = candidateList.get (candidateIndex);
        earthLoc = trans.transform (dataLoc);
        trans.getWorldAxes (earthLoc, north, east);
        candidateIndex++;
      } // while
      LOGGER.fine ("Orientation candidate data location = " + dataLoc +
        ", earth location = " + earthLoc);
      LOGGER.fine ("World axes north = " + Arrays.toString (north) +
        ", east = " + Arrays.toString (east));

      // Compute cross product term
      // --------------------------
      /**
       * The cross product here is N x E, which takes the form:
       *
       *               | i  j  k  |
       *     N x E =   | ni nj nk |
       *               | ei ej ek |
       *
       * The z component of the cross product is therefore ni*ej - nj*ei.
       */
      double z3 = north[Grid.ROWS]*east[Grid.COLS] - north[Grid.COLS]*east[Grid.ROWS];
      
      // Compute affine
      // --------------
      /**
       * These next tests correct the orientation so that the north and east
       * vectors point up and right on the screen:
       *
       *    N
       *    ^    (negative row direction)
       *    |
       *    |
       *    +-----> E     (positive column direction)
       *
       * The correct orientation results in N x E being into the screen
       * (negative value of the z-component).
       *
       * Before correction we may have the following cases:
       * 
       * z3 < 0 (ie: z axis of NxE into the screen, rotation required):
       *
       *            N     +-----> E    N <-----+     E  
       *            ^     |                    |     ^
       *            |     |                    |     |
       *            |     V                    V     |
       *    E <-----+     N                    E     +-----> N
       *
       * z3 > 0 (ie: z axis of NxE out of the screen, axis flip required):
       * 
       *            E     +-----> N    E <-----+     N  
       *            ^     |                    |     ^
       *            |     |                    |     |
       *            |     V                    V     |
       *    N <-----+     E                    N     +-----> E
       *
       */
      if (z3 < 0) {
        /**
         * In this case, the z-axis is into the screen, which is correct.  We
         * may simply need a rotation if the north axis is pointing downward
         * (positive rows direction).
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
        else if (east[Grid.COLS] < 0)
          affine = AffineTransform.getScaleInstance (1, -1);
        else {

          // Should never have this case!  We return null.
        
        }// else
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
    assert (orient.getType() == AffineTransform.TYPE_FLIP);

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
