////////////////////////////////////////////////////////////////////////
/*

     File: EarthDataViewFactory.java
   Author: Peter Hollemans
     Date: 2005/07/29

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
package noaa.coastwatch.gui;

// Imports
// -------

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.GridSubsetReader;
import noaa.coastwatch.io.NCReader;
import noaa.coastwatch.io.IOServices;
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
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.VariableStatisticsGenerator;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>EarthDataViewFactory</code> uses an {@link
 * EarthDataReader} and variable name to create an {@link
 * EarthDataView} for displaying a view of the variable data.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class EarthDataViewFactory {

  private static final Logger LOGGER = Logger.getLogger (EarthDataViewFactory.class.getName());

  // The maximum grid subset size in rows or columns.
  private int subsetSize = 256;

  // We hold onto a cache of settings so that we don't need to recalculate
  // the statistics each time we're asked for a variable with the same name
  // from the same reader.
  private Map<String, Map<String, ColorEnhancementSettings>> settingsMap = new HashMap<>();

  // The singleton instance of this class.
  private static EarthDataViewFactory instance;

  ////////////////////////////////////////////////////////////

  /**
   * Gets a shared instance of this class.
   * 
   * @return the shared instance.
   * 
   * @since 3.8.1
   */
  public static EarthDataViewFactory getInstance() {

    if (instance == null) instance = new EarthDataViewFactory();
    return (instance);

  } // getInstance

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
   * Recalls a set of enhancement settings if they were previously
   * stored during this session.
   * 
   * @param reader earth data reader to recall the settings.
   * @param varName the variable name to recall the settings.
   * 
   * @return the settings or null if the settings were not previously
   * stored.
   * 
   * @since 3.8.1
   */
  private ColorEnhancementSettings recallSettings (
    EarthDataReader reader,
    String varName
  ) { 

    ColorEnhancementSettings settings = null;
    var map = settingsMap.get (reader.getSource());
    if (map != null) {
      settings = map.get (varName);
    } // if

    return (settings);

  } // recallSettings

 ////////////////////////////////////////////////////////////

  /**
   * Stores a set of enhancement settings for the current session.
   * 
   * @param reader earth data reader to store the settings.
   * @param varName the variable name to store the settings.
   * @param settings the settings to store.
   * 
   * @since 3.8.1
   */
  private void storeSettings (
    EarthDataReader reader,
    String varName,
    ColorEnhancementSettings settings
  ) { 

    var source = reader.getSource();
    var map = settingsMap.get (source);
    if (map == null) {
      map = new HashMap<>();
      settingsMap.put (source, map);
    } // if
    map.put (varName, settings);

  } // storeSettings

 ////////////////////////////////////////////////////////////

  /**
   * Creates a new data view.
   *
   * @param reader the reader to use for variable data.
   * @param varName the variable name to view.
   *
   * @return a new view of the data variable, or null on error.
   */
  public EarthDataView create (
    EarthDataReader reader,
    String varName
  ) {

    ColorEnhancement view = null;

    try {

      // Get earth transform
      // -------------------
      EarthTransform trans = reader.getInfo().getTransform();

      // Get subset grid
      // ---------------
      Grid grid;
      if (reader instanceof GridSubsetReader && trans instanceof MapProjection) {
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
          grid = ((GridSubsetReader) reader).getGridSubset (varName, start, stride, length);
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

      // Try getting the enhancement settings from the user preferences,
      // using either the variable name or the stripped variable name
      // (in case of a group name).
      var settings = ResourceManager.getPreferences().getEnhancement (varName);
      if (settings == null) {
        String baseVarName = IOServices.stripGroup (varName);
        if (!baseVarName.equals (varName)) settings = 
          ResourceManager.getPreferences().getEnhancement (baseVarName);
      } // if

      // If the settings aren't found in the user preferences, look in the 
      // stored preferences in this factory instance.
      if (settings == null) {
        settings = recallSettings (reader, varName);
      } // if

      // If the settings aren't found in either the user preferences or in
      // the stored settings, as a last resort we compute statistics to 
      // create a reasonable view.


// TODO: Should we try to detect a log distribution here, rather than using 
// a linear scale?


      if (settings == null) {
        DataLocationConstraints lc = new DataLocationConstraints();
        lc.fraction = 0.01;
        Statistics stats = VariableStatisticsGenerator.getInstance().generate (grid, lc);


      // LOGGER.fine ("Creating earth data view with " + palette.getName() + 
      //   " palette and " + function.describe() + " function with range " + 
      //   Arrays.toString (function.getRange()));

        
        EnhancementFunction func = new LinearEnhancement (
          new double[] {Math.floor (stats.getMin()), Math.ceil (stats.getMax())}
        );
        Palette pal = PaletteFactory.create ("BW-Linear");
        settings = new ColorEnhancementSettings (varName, pal, func);
        storeSettings (reader, varName, settings);
      } // if

      var palette = settings.getPalette();
      var function = settings.getFunction();
      LOGGER.fine ("Creating earth data view with " + palette.getName() + 
        " palette and " + function.describe() + " function with range " + 
        Arrays.toString (function.getRange()));

      // Create a view using the settings we just created or recalled.
      view = new ColorEnhancement (trans, grid, settings.getPalette(), settings.getFunction());

    } // try

    catch (Exception e) { 
      LOGGER.log (Level.WARNING, "Failed to create view from reader and variable", e);
    } // catch

    return (view);

  } // create

  ////////////////////////////////////////////////////////////

} // EarthDataViewFactory class

////////////////////////////////////////////////////////////////////////
