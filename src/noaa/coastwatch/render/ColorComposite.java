////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorComposite.java
  PURPOSE: A class to set up a graphical view of Earth data using
           red/green/blue combination of three data variables.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/21
  CHANGES: 2002/07/28, PFH, converted to location classes
           2002/09/05, PFH, added comments and rearranged
           2002/10/10, PFH, changed to use generic enhancement functions
           2002/10/20, PFH, optimized getRGB, prepare
           2002/10/22, PFH, added normalize
           2003/04/19, PFH, added rendering progress mode
           2003/09/13, PFH, moved Statistics out of DataVariable
           2004/03/11, PFH, modified to improve performance
           2004/05/25, PFH, changed getGrid() to getGrids()
           2004/05/28, PFH, added handling for stopRendering flag
           2004/06/09, PFH, modified setGrids() to invalidate row/col caches
           2004/10/17, PFH, modified to use invalidate()

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * A color composite is a data view that creates an image based on
 * three data grid variables, one each for red, green, and
 * blue.  Each grid is associated with an enhancement function.  The
 * composite is created by normalizing each variable to the range
 * [0..1] using the enhancement functions, then mapping the normalized
 * values to byte data in the range [0..255].  Each byte is then used
 * as either the red, green, or blue component of a 24-bit color
 * value.  This is repeated for each pixel to form the overall
 * composite image.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class ColorComposite 
  extends EarthDataView {

  // Constants
  // ---------
  /** The red component. */
  public final static int RED = 0;

  /** The green component. */
  public final static int GREEN = 1;

  /** The blue component. */
  public final static int BLUE = 2;

  // Variables
  // ---------
  /** The data grid variables for the composite. */
  private Grid[] grids;

  /** The data enhancement functions. */
  private EnhancementFunction[] funcs;
 
  ////////////////////////////////////////////////////////////

  /** Gets the data grid variables. */
  public Grid[] getGrids () { return ((Grid[]) grids.clone()); }

  ////////////////////////////////////////////////////////////

  /** Gets the enhancement functions. */
  public EnhancementFunction[] getFunctions () { 
    return ((EnhancementFunction[]) funcs.clone()); 
  } // getFunctions

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the data grid variables.
   *
   * @param grids the array of three grid variables.  The order of
   * grid variables is [red, green, blue].  Note that the grids must
   * all have the same dimensions.
   */
  public void setGrids (Grid[] grids) { 

    this.grids = (Grid[]) grids.clone();
    invalidate();

  } // setGrids

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the enhancement functions.
   *
   * @param funcs the array of three enhancement functions.  The order of
   * functions is the same as the order of grids: [red, green, blue].
   */
  public void setFunctions (EnhancementFunction[] funcs) { 

    this.funcs = (EnhancementFunction[]) funcs.clone(); 
    image = null;
    changed = true;

  } // setFunctions

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new color composite from the specified parameters.
   * 
   * @param trans the view Earth transform.
   * @param grids the grid variables to use for this composite as
   * [red, green, blue].  Note that the grids must all have the same
   * dimensions.
   * @param funcs the enhancement functions as [red, green,
   * blue].
   *
   * @throws NoninvertibleTransformException if the resulting image 
   * transform is not invertible.
   */
  public ColorComposite (
    EarthTransform trans,
    Grid[] grids,
    EnhancementFunction[] funcs
  ) throws NoninvertibleTransformException {

    // Initialize variables
    // --------------------
    super (grids[0].getDimensions(), trans);
    setGrids (grids);
    setFunctions (funcs);

  } // ColorComposite constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the RGB value for a set of data values.
   *
   * @param values the data values.
   *
   * @return an RGB value based on the data values and enhancement 
   * functions.
   */
  private int getRGB (
    double[] values
  ) {

    int rgbVal = 0xff000000;
    for (int i = 0; i < 3; i++) {
      double norm;
      if (Double.isNaN (values[i])) norm = 0;
      else norm = funcs[i].getValue(values[i]);
      if (norm < 0) norm = 0;
      else if (norm > 1) norm = 1;
      int intVal = (int) (norm * 255);
      rgbVal |= intVal << ((2-i)*8);
    } // if
    return (rgbVal);

  } // getRGB

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g
  ) {

    // Create new image
    // ----------------
    image = new BufferedImage (imageDims.width, imageDims.height,
      BufferedImage.TYPE_INT_RGB);

    // Create coordinate caches
    // ------------------------
    if (rowCache == null) {
      int type = grids[0].getNavigation().getType();
      if (type == AffineTransform.TYPE_IDENTITY ||
        (type ^ AffineTransform.TYPE_TRANSLATION) == 0)
        computeCaches (grids[0]);
    } // if

    // Set update lines
    // ----------------
    int updateLines = (int) (imageDims.height * UPDATE_FRACTION);
    int lines = 0;

    // Create RGB row and values array
    // -------------------------------
    int[] rgbRow = new int[imageDims.width];
    double[] values = new double[3];

    // Render using image transform
    // ----------------------------
    if (!hasCoordinateCaches()) {
      ImageTransform imageTrans = trans.getImageTransform();
      Point point = new Point();
      for (point.y = 0; point.y < imageDims.height; point.y++) {

        // Render line
        // -----------
        for (point.x = 0; point.x < imageDims.width; point.x++) {
          DataLocation loc = imageTrans.transform (point);
          values[0] = grids[0].getValue (loc);
          values[1] = grids[1].getValue (loc);
          values[2] = grids[2].getValue (loc);
          rgbRow[point.x] = getRGB (values);
        } // for
        image.setRGB (0, point.y, imageDims.width, 1, rgbRow, 0, 
          imageDims.width);

        // Show rendering progress
        // -----------------------
        if (progress) {
          lines++;
          if (lines >= updateLines) {
            g.drawImage (image, 0, 0, null);
            lines = 0;
          } // if
        } // if

        // Detect rendering stop
        // ---------------------
        if (stopRendering) return;

      } // for
    } // if

    // Render using cached coordinates
    // -------------------------------
    else {
      int lastGridRow = Integer.MIN_VALUE;
      for (int y = 0; y < imageDims.height; y++) {

        // Render line
        // -----------
        if (rowCache[y] != lastGridRow) {
          int lastGridCol = Integer.MIN_VALUE;
          int rgbValue = 0;
          for (int x = 0; x < imageDims.width; x++) {
            if (colCache[x] != lastGridCol) {
              values[0] = grids[0].getValue (rowCache[y], colCache[x]);
              values[1] = grids[1].getValue (rowCache[y], colCache[x]);
              values[2] = grids[2].getValue (rowCache[y], colCache[x]);
              rgbValue = getRGB (values);
              lastGridCol = colCache[x];
            } // if
            rgbRow[x] = rgbValue;
          } // for
          lastGridRow = rowCache[y];
        } // if
        image.setRGB (0, y, imageDims.width, 1, rgbRow, 0, imageDims.width);

        // Show rendering progress
        // -----------------------
        if (progress) {
          lines++;
          if (lines >= updateLines) {
            g.drawImage (image, 0, 0, null);
            lines = 0;
          } // if
        } // if

        // Detect rendering stop
        // ---------------------
        if (stopRendering) return;

      } // for
    } // else
 
  } // prepare

  ////////////////////////////////////////////////////////////

  /**
   * Normalizes the enhancement function for this view using the
   * visible grid value statistics.
   *
   * @param component the component to enhance, either <code>RED</code>,
   * <code>GREEN</code>, or <code>BLUE</code>.  
   * @param units the number of standard deviation units above and
   * below the mean for the data range.
   */
  public void normalize (
    int component,
    double units
  ) {

    // Calculate statistics
    // --------------------
    Grid grid = grids[component];
    DataLocation[] corners = getCorners();
    Statistics stats = grid.getStatistics (corners[0],
      corners[1], 0.01);

    // Check for zero valid
    // --------------------    
    if (stats.getValid() == 0) {
      throw new ArithmeticException ("Variable '" + grid.getName() +
        "' has no valid data values, cannot normalize");
    } // if 

    // Modify function
    // ---------------
    funcs[component].normalize (stats, units);

  } // normalize

  ////////////////////////////////////////////////////////////

} // ColorComposite class

////////////////////////////////////////////////////////////////////////
