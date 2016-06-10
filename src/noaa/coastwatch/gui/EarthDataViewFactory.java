////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataViewFactory.java
  PURPOSE: Creates preview panels for earth data.
   AUTHOR: Peter Hollemans
     DATE: 2005/07/29
  CHANGES: 2007/03/30, PFH, added check for failed grid subset read

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.GridSubsetReader;
import noaa.coastwatch.io.NCReader;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.ColorEnhancementSettings;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.tools.Preferences;
import noaa.coastwatch.tools.ResourceManager;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;

/**
 * The <code>EarthDataViewFactory</code> uses an {@link
 * EarthDataReader} and variable name to create an {@link
 * EarthDataView} for displaying a view of the variable data.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class EarthDataViewFactory {

  // Variables
  // ---------
  
  /** The maximum grid subset size in rows or columns. */
  private static int subsetSize = 256;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the grid subset size.  If the {@link EarthDataReader} passed
   * to {@link #create} is capable of reading grid subsets via the
   * {@link GridSubsetReader} interface, the specified grid subset
   * dimension will be used to limit the total size of grid data read.
   * The default subset size is set at 256, which is appropriate for
   * previewing grid data.
   *
   * @param subsetSize the grid subset size.
   */
  public void setSubsetSize (
    int subsetSize
  ) {

    this.subsetSize = subsetSize;

  } // setSubsetSize

 ////////////////////////////////////////////////////////////

  /**
   * Creates a new data view.
   *
   * @param reader the reader to use for variable data.
   * @param varName the variable name to view.
   *
   * @return a new view of the data variable, or null on error.
   */
  public static EarthDataView create (
    EarthDataReader reader,
    String varName
  ) {

    try {

      // Get earth transform
      // -------------------
      EarthTransform trans = reader.getInfo().getTransform();

      // Get subset grid
      // ---------------
      Grid grid;
      if (reader instanceof GridSubsetReader) {
        int[] dims = reader.getPreview (varName).getDimensions();
        int strideValue = Math.max (
          (int) Math.ceil ((float)dims[Grid.ROWS]/subsetSize),
          (int) Math.ceil ((float)dims[Grid.COLS]/subsetSize)
        );
        int[] start = new int[] {0, 0};
        int[] stride = new int[] {strideValue, strideValue};
        int[] length = new int[] {dims[Grid.ROWS]/strideValue, 
          dims[Grid.COLS]/strideValue};
        try {
          grid = ((GridSubsetReader) reader).getGridSubset (varName, start, 
            stride, length);

          // TODO: What happens here if the transform is not a map
          // projection?  We're currently limited in our implementations
          // of grid subset readers, so we're OK for now.  But in the
          // future, we may need a more general mechanism here.
          
          if (trans instanceof MapProjection)
            trans = ((MapProjection) trans).getSubset (start, stride, length);
        } // try

        // Fall back on full size grid
        // ---------------------------
        catch (Exception e) { 
          if (reader instanceof NCReader && !((NCReader) reader).isNetwork())
            grid = (Grid) reader.getVariable (varName);
          else
            throw new RuntimeException();
        } // catch

      } // if

      // Get normal grid
      // ---------------
      else {
        grid = (Grid) reader.getVariable (varName);
      } // else

      // Get enhancement settings
      // ------------------------
      ColorEnhancementSettings settings = 
        ResourceManager.getPreferences().getEnhancement (varName);
      if (settings == null) {
        Statistics stats = grid.getStatistics (0.01);
        EnhancementFunction func = new LinearEnhancement (
          new double[] {Math.floor (stats.getMin()), 
          Math.ceil (stats.getMax())});
        Palette pal = PaletteFactory.create ("BW-Linear");
        settings = new ColorEnhancementSettings (varName, pal, func);
      } // if

      // Create view
      // -----------
      ColorEnhancement view = new ColorEnhancement (trans, grid, 
        settings.getPalette(), settings.getFunction());

      return (view);

    } // try

    catch (Exception e) { 

      System.out.println ("Sending back null view");
      e.printStackTrace (System.out);

      return (null); 
    } // catch

  } // create

  ////////////////////////////////////////////////////////////

} // EarthDataViewFactory class

////////////////////////////////////////////////////////////////////////
