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
 * and vice-versa, apply the orientation at the center of the data coordinates
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
 * The image to data coordinate transform is the inverse.
 *
 * @see ImageTransform#ImageTransform(Dimension,Dimension,DataLocation,double,AffineTransform)
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@noaa.coastwatch.test.Testable
public class OrientationAffineFactory {

  private static final Logger LOGGER = Logger.getLogger (OrientationAffineFactory.class.getName());

  /** The classification of pole orientation detected near the image center. */
  public static enum PoleOrientation {
    NORTH_POLE,
    SOUTH_POLE,
    UNDETERMINED
  } // PoleOrientation enum

  /** The minimum normalized dot product magnitude for a radial north test. */
  private static final double POLE_DOT_PRODUCT = 0.7;

  ////////////////////////////////////////////////////////////

  /**
   * Determines whether a transform appears to be centered on the north pole,
   * south pole, or neither.
   *
   * <p>The test samples four points surrounding the image center and compares
   * the local north vector at each point to the vector from the sample point to
   * the image center. For a north-pole-centered view, local north should point
   * consistently toward the center. For a south-pole-centered view, local north
   * should point consistently away from the center.</p>
   *
   * <p>The returned classification is conservative. If any sample point fails
   * to produce a usable north vector, or if the vectors are not consistently
   * radial relative to the center, the result is {@link
   * PoleOrientation#UNDETERMINED}.</p>
   *
   * @param trans the transform to examine.
   *
   * @return the detected pole orientation classification.
   */
  static public PoleOrientation getPoleOrientation (
    EarthTransform2D trans
  ) {

    PoleOrientation orientation = PoleOrientation.UNDETERMINED;

    try {

      int[] dims = trans.getDimensions();
      double centerRow = (dims[Grid.ROWS]-1)/2.0;
      double centerCol = (dims[Grid.COLS]-1)/2.0;
      double rowOffset = Math.max (1, (dims[Grid.ROWS]-1)*0.25);
      double colOffset = Math.max (1, (dims[Grid.COLS]-1)*0.25);

      var candidateList = List.of (
        new DataLocation (centerRow - rowOffset, centerCol),
        new DataLocation (centerRow + rowOffset, centerCol),
        new DataLocation (centerRow, centerCol - colOffset),
        new DataLocation (centerRow, centerCol + colOffset)
      );

      boolean allTowardCenter = true;
      boolean allAwayFromCenter = true;
      var north = new double[] {0, 0};

      for (var dataLoc : candidateList) {

        // Compute the local north vector in row/column coordinates
        var earthLoc = trans.transform (dataLoc);
        trans.getWorldAxes (earthLoc, north, new double[] {0, 0});

        // Compare local north to the radial vector from the sample to center
        double dRow = centerRow - dataLoc.get (Grid.ROWS);
        double dCol = centerCol - dataLoc.get (Grid.COLS);
        double radialMag = Math.sqrt (dRow*dRow + dCol*dCol);
        double northMag = Math.sqrt (
          north[Grid.ROWS]*north[Grid.ROWS] +
          north[Grid.COLS]*north[Grid.COLS]
        );
        if (radialMag < 1e-6 || northMag < 1e-6) {
          allTowardCenter = false;
          allAwayFromCenter = false;
          break;
        } // if

        double dot = (
          north[Grid.ROWS]*dRow +
          north[Grid.COLS]*dCol
        ) / (northMag*radialMag);

        LOGGER.fine ("Pole orientation candidate data location = " + dataLoc +
          ", north = " + Arrays.toString (north) + ", dot = " + dot);

        if (dot < POLE_DOT_PRODUCT) allTowardCenter = false;
        if (dot > -POLE_DOT_PRODUCT) allAwayFromCenter = false;

      } // for

      if (allTowardCenter) orientation = PoleOrientation.NORTH_POLE;
      else if (allAwayFromCenter) orientation = PoleOrientation.SOUTH_POLE;

    } // try
    catch (Exception e) { }

    return (orientation);

  } // getPoleOrientation

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

      // Compute the type of pole we have if any
      var pole = getPoleOrientation (trans);

      // TODO: Should we really check the full list of candidates here and come to
      // some consensus, rather than just poicking the first one that didn't 
      // produce a zero output?  See Codex conversation on polar stereographic 
      // rotation.

      // Get the north and east vectors using a list of candidate locations
      // for testing
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

      // Compute z term of the cross product NxE which takes the form:

      /**
       * 
       *                | i  j  k  |
       *      N x E =   | ni nj nk |
       *                | ei ej ek |
       * 
       *  The z component of the cross product is therefore ni*ej - nj*ei.
       * 
       */
      double z3 = north[Grid.ROWS]*east[Grid.COLS] - north[Grid.COLS]*east[Grid.ROWS];
      
      // Compute the affine needed for proper orientation

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
        if (north[Grid.ROWS] > 0 && pole == PoleOrientation.UNDETERMINED)
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

    // A north polar stereographic view centered away from the pole should
    // rotate so that the local north/east frame appears in the expected
    // screen orientation.
    MapProjection map = new PolarStereographicProjection (
      SpheroidConstants.SPHEROID_SEMI_MAJOR[12],
      SpheroidConstants.SPHEROID_SEMI_MINOR[12],
      new int[] {512, 512},
      new java.awt.geom.AffineTransform(),
      0, Math.toRadians (90), 0, 0);
    map = map.getModified (new EarthLocation (60, 180), new double[] {1, 1});
    AffineTransform orient = OrientationAffineFactory.create (map);
    assert (orient.getType() == AffineTransform.TYPE_QUADRANT_ROTATION);

    // The same north polar stereographic view centered on the prime meridian
    // should already be correctly oriented and require no transform.
    map = map.getModified (new EarthLocation (60, 0), new double[] {1, 1});
    orient = OrientationAffineFactory.create (map);
    assert (orient.getType() == AffineTransform.TYPE_IDENTITY);
    
    // A geographic projection written with one axis reversed should be
    // detected as needing a flip for display.
    map = MapProjectionFactory.getInstance().create (GCTP.GEO, 0,
      new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, GCTP.WGS84,
      new int[] {512, 512}, new EarthLocation (0, 0),
      new double[] {-0.05, 0.05});
    orient = OrientationAffineFactory.create (map);
    assert (orient.getType() == AffineTransform.TYPE_FLIP);

    // Reversing the opposite axis should also produce a display flip.
    map = MapProjectionFactory.getInstance().create (GCTP.GEO, 0,
      new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, GCTP.WGS84,
      new int[] {512, 512}, new EarthLocation (0, 0),
      new double[] {0.05, -0.05});
    orient = OrientationAffineFactory.create (map);
    assert (orient.getType() == AffineTransform.TYPE_FLIP);

    // A null swath projection should not yield any usable orientation.
    SwathProjection.setNullMode (true);
    SwathProjection swath = new SwathProjection (null);
    SwathProjection.setNullMode (false);
    orient = OrientationAffineFactory.create (swath);
    assert (orient == null);
    logger.passed();

    logger.test ("getPoleOrientation");

    // A north polar stereographic view centered on the pole should be
    // recognized by north vectors that point radially toward the center.
    map = new PolarStereographicProjection (
      SpheroidConstants.SPHEROID_SEMI_MAJOR[12],
      SpheroidConstants.SPHEROID_SEMI_MINOR[12],
      new int[] {512, 512},
      new java.awt.geom.AffineTransform(),
      0, Math.toRadians (90), 0, 0);
    map = map.getModified (new EarthLocation (90, 0), new double[] {1, 1});
    assert (OrientationAffineFactory.getPoleOrientation (map) ==
      PoleOrientation.NORTH_POLE);

    // A south polar stereographic view centered on the pole should be
    // recognized by north vectors that point radially away from the center.
    map = new PolarStereographicProjection (
      SpheroidConstants.SPHEROID_SEMI_MAJOR[12],
      SpheroidConstants.SPHEROID_SEMI_MINOR[12],
      new int[] {512, 512},
      new java.awt.geom.AffineTransform(),
      0, Math.toRadians (-90), 0, 0);
    map = map.getModified (new EarthLocation (-90, 0), new double[] {1, 1});
    assert (OrientationAffineFactory.getPoleOrientation (map) ==
      PoleOrientation.SOUTH_POLE);

    // A normal geographic projection should not exhibit the radial north
    // vector pattern associated with a pole-centered view.
    map = MapProjectionFactory.getInstance().create (GCTP.GEO, 0,
      new double[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, GCTP.WGS84,
      new int[] {512, 512}, new EarthLocation (0, 0),
      new double[] {-0.05, 0.05});
    assert (OrientationAffineFactory.getPoleOrientation (map) ==
      PoleOrientation.UNDETERMINED);
    
    logger.passed();
    
  } // main

  ////////////////////////////////////////////////////////////

} // OrientationAffineFactory class

////////////////////////////////////////////////////////////////////////
