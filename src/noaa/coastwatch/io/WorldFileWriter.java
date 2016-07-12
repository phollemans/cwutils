////////////////////////////////////////////////////////////////////////
/*
     FILE: WorldFileWriter.java
  PURPOSE: Writes georeference world files.
   AUTHOR: Peter Hollemans
     DATE: 2005/05/30
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;

/**
 * The <code>WorldFileWriter</code> class writes ASCII text world
 * files that store image to map coordinate transformation parameters
 * as a set of floating point values.  World files contain the
 * following lines:
 * <ul>
 *   <li>Line 1: x-dimension of a pixel in map units</li>
 *   <li>Line 2: rotation parameter</li>
 *   <li>Line 3: rotation parameter</li>
 *   <li>Line 4: NEGATIVE of y-dimension of a pixel in map units</li>
 *   <li>Line 5: x-coordinate of center of upper left pixel</li>
 *   <li>Line 6: y-coordinate of center of upper left pixel</li>
 * </ul>
 * More information on world files and the naming conventions for
 * world files may be found at ESRI's support center:
 * <ul>
 *   <li><a href="http://support.esri.com/index.cfm?fa=knowledgebase.techarticles.articleShow&d=17489">FAQ:  What is the format of the world file used for georeferencing images?</a></li>
 *   <li><a href="http://support.esri.com/index.cfm?fa=knowledgebase.techArticles.articleShow&d=23082">Object:  [O-Image] Supported image formats for ArcIMS 4.0, 4.0.1 and 9.0</a></li>
 * </ul>
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class WorldFileWriter {

  // Variables
  // ---------

  /** The output stream to write to. */
  private OutputStream output;

  /** The earth image transform for coordinate transform data. */
  private EarthImageTransform trans;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new writer using the specified output stream.
   * 
   * @param output the output stream for writing.
   * @param trans the earth image transform for earth location metadata.
   * 
   * @throws IllegalArgumentException if the earth transform is not a
   * map projection.
   */
  public WorldFileWriter (
    OutputStream output,
    EarthImageTransform trans
  ) throws IOException {

    // Check for map projection
    // ------------------------
    if (!(trans.getEarthTransform() instanceof MapProjection)) 
      throw new IllegalArgumentException (
        "Earth transform must be a map projection");

    // Set variables
    // -------------
    this.output = output;
    this.trans = trans;

  } // WorldFileWriter

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a world file to the output stream.
   *
   * @throws IOException if an error occurred writing to the output
   * stream.
   */
  public void write () throws IOException {

    // Get transforms
    // --------------
    EarthTransform earthTrans = trans.getEarthTransform();
    ImageTransform imageTrans = trans.getImageTransform();
    MapProjection map = (MapProjection) earthTrans;

    // Get transform parameters
    // ------------------------
    AffineTransform imageAffine;
    try { 
      imageAffine = imageTrans.getAffine().createInverse(); 
    } // try
    catch (NoninvertibleTransformException e) { 
      throw new IOException ("Cannot invert image affine transform");
    } // catch
    AffineTransform mapAffine = map.getAffine();
    AffineTransform modelAffine = (AffineTransform) mapAffine.clone();
    modelAffine.concatenate (imageAffine);
    double[] matrix = new double[6];
    modelAffine.getMatrix (matrix);

    // Create file
    // -----------
    PrintStream stream = new PrintStream (output);
    stream.println (matrix[0]);
    stream.println (matrix[1]);
    stream.println (matrix[2]);
    stream.println (matrix[3]);
    stream.println (matrix[4]);
    stream.println (matrix[5]);
    stream.close();

  } // write

  ////////////////////////////////////////////////////////////

} // WorldFileWriter class

////////////////////////////////////////////////////////////////////////
