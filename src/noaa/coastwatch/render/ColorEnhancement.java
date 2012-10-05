////////////////////////////////////////////////////////////////////////
/*
     FILE: ColorEnhancement.java
  PURPOSE: A class to set up a graphical view of Earth data using a
           color palette.
   AUTHOR: Peter Hollemans
     DATE: 2002/07/21
  CHANGES: 2002/07/28, PFH, converted to location classes
           2002/09/04, PFH, rearranged
           2002/10/20, PFH, optimized getByte, prepare
           2002/10/22, PFH, added normalize
           2003/04/19, PFH, added rendering progress mode
           2003/07/21, PFH, added getColorModel
           2003/09/13, PFH, moved Statistics out of DataVariable
           2004/02/20, PFH
             - updated to use hasCoordianteCaches() in prepare()
             - change updateColorModel() to recreate image on palette change
           2004/03/11, PFH, modified to improve performance
           2004/05/19, PFH, added saveSettings(), restoreSettings()
           2004/05/28, PFH, added handling for stopRendering flag
           2004/06/09, PFH, modified setGrid() to invalidate row/col caches
           2004/10/17, PFH, modified to use invalidate()
           2005/05/30, PFH, changed DataColorScale to pass name and units
           2005/10/12, PFH, added check for NaN norm value in getByte()
           2006/11/10, PFH, added setAdjustingFunction()
           2012/08/14, PFH, added legend caching

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
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.util.trans.*;

/**
 * A color enhancement is an Earth data view that creates an image
 * based on a grid variable, color palette, and enhancement function.
 * The variable data is mapped to a normalized range of [0..1] using
 * the enhancement function, then the color palette is applied.  For
 * example, if the data is visible surface albedo in percent, the
 * palette is a black-to-white grayscale ramp, and the enhancement is
 * a linear enhancement set to map 0.0 to 0 and 30.0 to 1, then the
 * color enhancement will produce an image that has close to black
 * colors for low albedo values and close to white for albedo values
 * near 30 percent.  Values falling below the [0..1] range of the
 * enhancement function are mapped to the missing color, by default
 * black.  Values falling above the [0..1] range are mapped to the
 * last palette color.
 */
public class ColorEnhancement 
  extends EarthDataView {

  // Variables
  // ---------

  /** The number of standard data colors in a color map. */
  private int colors = 255;

  /** The index of the missing color. */
  private int missing = 255;

  /** The data grid variable for enhancement. */
  private Grid grid;

  /** The color palette to use for enhancement. */
  private Palette pal;

  /** The data enhancement function. */
  private EnhancementFunction func;
 
  /** The color model to use for indexed images. */
  private IndexColorModel colorModel;

  /** The color to use for invalid or below range values. */
  private Color missingColor;

  /** The adjusting function, or null for none. */
  private EnhancementFunction adjFunc;

  /** The (cached) legend to use for data annotation. */
  private Legend legend;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the number of colors in color model.
   *
   * @see #setColors
   */
  public int getColors () { return (colors+1); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the number of colors in the color model.
   *
   * @param colors the number of colors in the model, up to 256
   * which is the default.  The actual number of colors for data
   * will be one less than the number of colors in the model, and
   * the last color will be used for missing data.
   */
  public void setColors (
    int colors
  ) {

    this.colors = colors-1;
    this.missing = colors-1;
    updateColorModel();

  } // setColors

  ////////////////////////////////////////////////////////////

  /** Gets the index color model used for images. */
  public IndexColorModel getColorModel () { return (colorModel); }

  ////////////////////////////////////////////////////////////

  /** Gets the data grid variable. */
  public Grid getGrid () { return (grid); }

  ////////////////////////////////////////////////////////////

  /** Gets the enhancement color palette. */
  public Palette getPalette () { return (pal); }

  ////////////////////////////////////////////////////////////

  /** Gets the enhancement function. */
  public EnhancementFunction getFunction () { return (func); }

  ////////////////////////////////////////////////////////////

  /** Sets the data grid variable. */
  public void setGrid (Grid grid) { 

    this.grid = grid;
    this.legend = null;
    invalidate();

  } // setGrid

  ////////////////////////////////////////////////////////////

  /** Sets the enhancement color palette. */
  public void setPalette (Palette pal) { 

    this.pal = pal;
    this.legend = null;
    updateColorModel();

  } // setPalette

  ////////////////////////////////////////////////////////////

  /** Sets the missing value color. */
  public void setMissingColor (Color missingColor) {

    this.missingColor = missingColor;
    this.legend = null;
    updateColorModel();

  } // setMissingColor

  ////////////////////////////////////////////////////////////

  /**
   * Sets an adjusting function.  This may be used when the function
   * is being adjusted interactively, and the view should reflect the
   * new function as well as possible.  The view image data is not
   * recomputed, rather the color index model is adjusted to
   * approximate what the view would look like with the new function.
   * The next call to {@link #setFunction} will set the view back to
   * normal.
   *
   * @param adjFunc the adjusting function.
   */
  public void setAdjustingFunction (
    EnhancementFunction adjFunc
  ) {

    // Check image
    // -----------
    if (image == null) return;

    // Remove cached legend
    // --------------------
    this.legend = null;

    // Create new color arrays
    // -----------------------
    int mapSize = colorModel.getMapSize();
    byte[] r = new byte[mapSize];
    byte[] g = new byte[mapSize];
    byte[] b = new byte[mapSize];

    // Fill new color arrays
    // ---------------------
    for (int i = 0; i < colors; i++) {
      double value = func.getInverse (((double) i)/(colors-1));
      int colorIndex = getByte (value, adjFunc) & 0xff;
      r[i] = (byte) colorModel.getRed (colorIndex);
      g[i] = (byte) colorModel.getGreen (colorIndex);
      b[i] = (byte) colorModel.getBlue (colorIndex);
    } // for
    r[missing] = (byte) colorModel.getRed (missing);
    g[missing] = (byte) colorModel.getGreen (missing);
    b[missing] = (byte) colorModel.getBlue (missing);

    // Convert image to new color model
    // --------------------------------
    IndexColorModel adjModel = new IndexColorModel (8, mapSize, r, g, b);
    image = new BufferedImage (adjModel, image.getRaster(), false, null);
    this.adjFunc = adjFunc;
    changed = true;

  } // setAdjustingFunction

  ////////////////////////////////////////////////////////////
  
  /** Sets the enhancement function. */
  public void setFunction (EnhancementFunction func) { 

    this.func = func;
    this.adjFunc = null;
    this.legend = null;
    image = null;
    changed = true;

  } // setFunction

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new color enhancement from the specified parameters.
   * 
   * @param trans the view Earth transform.
   * @param grid the grid variable to use for this enhancement.
   * @param pal the indexed color palette for color enhancement.
   * @param func the enhancement function.
   *
   * @throws NoninvertibleTransformException if the resulting image 
   * transform is not invertible.
   */
  public ColorEnhancement (
    EarthTransform trans,
    Grid grid,
    Palette pal,
    EnhancementFunction func
  ) throws NoninvertibleTransformException {

    // Initialize variables
    // --------------------
    super (grid.getDimensions(), trans);
    this.grid = grid;
    this.pal = pal;
    this.func = func;

    // Create color model
    // ------------------
    missingColor = Color.BLACK;
    updateColorModel();

  } // ColorEnhancement constructor

  ////////////////////////////////////////////////////////////

  /**
   * Updates the index color model and number of palette colors.  If
   * there is an image available, it is converted to the new color
   * model.
   */
  private void updateColorModel () {

    // Create color model
    // ------------------
    Palette remapped = pal.remap (colors);
    remapped.add (missingColor);
    colorModel = remapped.getModel();

    // Convert image to new color model
    // --------------------------------
    if (image != null) {
      image = new BufferedImage (colorModel, image.getRaster(), false, null);
      changed = true;
    } // if

  } // updateColorModel

  ////////////////////////////////////////////////////////////

  /**
   * Gets the byte value for a data value.  The byte value represents
   * an index into the indexed color model.
   *
   * @param value the data value.
   * @param func the enhancement function.
   *
   * @return a byte value based on the enhancement function.
   * If the data value is invalid or less than the enhancement
   * function range, <code>colors</code> is returned to represent
   * the missing value.  If the data value is greater than the
   * function range, it is clipped at the maximum color.
   */
  private byte getByte (
    double value,
    EnhancementFunction func
  ) {

    if (Double.isNaN (value)) return ((byte) missing);
    double norm = func.getValue (value);
    if (Double.isNaN (norm) || norm < 0) return ((byte) missing);
    else if (norm > 1) norm = 1;
    return ((byte) Math.round (norm * (colors-1)));

  } // getByte

  ////////////////////////////////////////////////////////////

  protected void prepare (
    Graphics2D g
  ) {

    // TODO: This method needs to have some underlying optimization to
    // work more quickly.  For example, when reading data from a
    // cached grid, we only need to get the data rows and columns that
    // will be rendered in the image, rather than all the rows in a
    // certain grid tile.  This method might be able to give a "hint"
    // to the cached grid as to what it actually must read, based on
    // the cached row and column coordinates.
    //
    // Another optimization would be to reuse byte data from the old
    // image in the new image.  If the old row and column caches were
    // kept, they could be used to obtain data from the old image to
    // insert into the new image.  This would work best in an image
    // pan situation, where no scale change has occurred.

    // Create new image
    // ----------------
    image = new BufferedImage (imageDims.width, imageDims.height,
      BufferedImage.TYPE_BYTE_INDEXED, colorModel);

    // Fill with missing color
    // -----------------------
    if (progress) {
      Graphics2D imageGraphics = image.createGraphics();
      imageGraphics.setColor (missingColor);
      imageGraphics.fillRect (0, 0, imageDims.width, imageDims.height);
      imageGraphics.dispose();
    } // if

    // Get raster
    // ----------
    WritableRaster raster = image.getRaster();

    // Create coordinate caches
    // ------------------------
    if (!hasCoordinateCaches()) {
      int type = grid.getNavigation().getType();
      if (type == AffineTransform.TYPE_IDENTITY ||
        (type ^ AffineTransform.TYPE_TRANSLATION) == 0)
        computeCaches (grid);
    } // if

    // Set update lines
    // ----------------
    int updateLines = (int) (imageDims.height * UPDATE_FRACTION);
    int lines = 0;

    // Create byte data row
    // --------------------
    byte[] byteRow = new byte[imageDims.width];

    // Render using image transform
    // ----------------------------
    if (!hasCoordinateCaches()) {
      Point point = new Point();
      ImageTransform imageTrans = trans.getImageTransform();
      for (point.y = 0; point.y < imageDims.height; point.y++) {

        // Render line
        // -----------
        for (point.x = 0; point.x < imageDims.width; point.x++) {
          byteRow[point.x] = getByte (grid.getValue (
            imageTrans.transform (point)), func);
        } // for
        raster.setDataElements (0, point.y, imageDims.width, 1, byteRow);

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
          byte byteValue = 0;
          for (int x = 0; x < imageDims.width; x++) {
            if (colCache[x] != lastGridCol) {
              byteValue = getByte (grid.getValue (rowCache[y], colCache[x]), 
                func);
              lastGridCol = colCache[x];
            } // if
            byteRow[x] = byteValue;
          } // for
          lastGridRow = rowCache[y];
        } // if
        raster.setDataElements (0, y, imageDims.width, 1, byteRow);

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
   * Gets a data color scale legend for annotation of this color
   * enhancement.
   *
   * @return the data color scale legend.  The legend is constructed
   * with the default font and colors.
   *
   * @see DataColorScale
   */
  public Legend getLegend () { 

    if (this.legend == null) {
      EnhancementFunction legendFunc = (adjFunc != null ? adjFunc : func);
      this.legend = new DataColorScale (legendFunc, pal, grid.getName(),
        grid.getUnits());
    } // if
    
    return (this.legend);

  } // getLegend

  ////////////////////////////////////////////////////////////

  /**
   * Normalizes the enhancement function for this view using the
   * visible grid value statistics.
   *
   * @param units the number of standard deviation units above and
   * below the mean for the data range.
   */
  public void normalize (
    double units
  ) {

    // Calculate statistics
    // --------------------
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
    func.normalize (stats, units);
    this.legend = null;
    
  } // normalize

  ////////////////////////////////////////////////////////////

  /** Saves and returns the current settings. */
  public ColorEnhancementSettings saveSettings () {

    return (new ColorEnhancementSettings (grid.getName(), getPalette(), 
      getFunction()));

  } // saveSettings

  ////////////////////////////////////////////////////////////

  /** 
   * Restores the previously saved settings. 
   *
   * @param settings the settings to restore.  Only the palette and
   * function are used -- the variable name is igored.
   * 
   */
  public void restoreSettings (
    ColorEnhancementSettings settings
  ) {

    setPalette (settings.getPalette());
    setFunction (settings.getFunction());

  } // restoreSettings

  ////////////////////////////////////////////////////////////

} // ColorEnhancement class

////////////////////////////////////////////////////////////////////////
