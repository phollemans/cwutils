/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.io;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.Instant;

import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.dataset.CoordinateAxisTimeHelper;
import ucar.nc2.time.Calendar;

import noaa.coastwatch.io.EarthGridSet;
import noaa.coastwatch.io.EarthGridSet.Axis;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform2D;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.EarthDataInfo;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Creates {@link EarthGridSet} objects from user-specified data.
 * 
 * @author Peter Hollemans
 * @since 4.0.1
 */
public class EarthGridSetFactory {

  private static final Logger LOGGER = Logger.getLogger (EarthGridSetFactory.class.getName());

  ////////////////////////////////////////////////////////////

  /**
   * Creates a grid set from a single input source.  The source should contain
   * 2D grids with other axes, for example time and height.
   *
   * @param source the source data, possibly a URL or a local file name.
   * 
   * @return the grid set.
   * 
   * @throws IOException if there was an error accessing the source.
   */
  public static EarthGridSet createFromSingleSource (String source) throws IOException {

    // Open the source and get the coordinate systems
    var reader = EarthDataReaderFactory.create (source);
    var systems = reader.getCoordinateSystems();
    LOGGER.fine ("Found " + systems.size() + " coordinate systems");

    // Create a list of variables and axes
    var variableList = new ArrayList<String>();
    var variableAxes = new LinkedHashMap<String, List<Axis>>();
    var geoAxisTypes = List.of (AxisType.GeoX, AxisType.GeoY, AxisType.Lat, AxisType.Lon);

    // Fill in the variable names if there were no coordinate systems
    if (systems.size() == 0) {
      variableList.addAll (reader.getAllGrids());
      for (var varName : variableList) variableAxes.put (varName, new ArrayList<Axis>());
    } // if

    // For each system, add to the mapping of variable name to axes lists
    else {
      for (var system : systems) {
        var systemVars = reader.getVariablesForSystem (system);
        LOGGER.fine ("Processing coordinate system for variables " + Arrays.toString (systemVars.toArray()));
        variableList.addAll (systemVars);
        var coordAxes = system.getCoordinateAxes();
        var axisList = new ArrayList<Axis>();
        for (var coordAxis : coordAxes) {
          if (coordAxis.getRank() == 1) {
            var coordAxis1D = (CoordinateAxis1D) coordAxis;
            if (!geoAxisTypes.contains (coordAxis1D.getAxisType())) {
              Axis axis;

              // Create a time axis
              if (coordAxis1D.getAxisType().equals (AxisType.Time)) {
                var dateList = new ArrayList<Instant>();
                var helper = new CoordinateAxisTimeHelper (Calendar.getDefault(), coordAxis1D.getUnitsString());
                double[] offsets = coordAxis1D.getCoordValues();
                for (var offset : offsets) {
                  var date = helper.makeCalendarDateFromOffset (offset);
                  dateList.add (Instant.ofEpochMilli (date.getMillis()));
                } // for
                axis = new Axis<Instant> (
                  coordAxis1D.getShortName(),
                  coordAxis1D.getAxisType().toString(),
                  "milliseconds since 1970-01-01T00:00:00Z",
                  dateList,
                  Instant.class
                );
              } // if

              // Create a regular axis
              else {
                axis = new Axis<Double> (
                  coordAxis1D.getShortName(),
                  coordAxis1D.getAxisType().toString(),
                  coordAxis1D.getUnitsString(),
                  Arrays.stream (coordAxis1D.getCoordValues()).boxed().collect (Collectors.toList()),
                  Double.class
                );
              } // else

              axisList.add (axis);
              LOGGER.fine ("Found axis " + axis + " of size " + axis.getSize());

            } // if
          } // if
        } // for
        if (!axisList.isEmpty()) {
          for (var varName : systemVars) {
            variableAxes.put (varName, axisList);
          } // for
        } // if
      } // for
    } // else

    // Get the earth transform for the file
    var trans = reader.getInfo().getTransform();

    // Create a grid set using this information
    var gridSet = new EarthGridSet () {

      @Override
      public List<String> getVariables() { return (variableList); }

      @Override
      public List<Axis> getAxes (String varName) { return (variableAxes.get (varName)); }

      @Override
      public Grid accessGrid (String varName, List<Integer> axisIndexList) {

        LOGGER.fine ("Retrieving grid for " + varName + " with axes " + 
          Arrays.toString (axisIndexList.toArray()));

        var axes = getAxes (varName);
        var gridVarName = varName;

        // For grids with multiple axes, append the indices to the variable
        if (axes.size() != 0) {
          gridVarName += "_";
          for (int i = 0; i < axes.size(); i++) {
            var axisCode = axes.get (i).getAxisType().substring (0, 1);
            if (axisCode.equals ("G")) axisCode = "Z";
            gridVarName += "_" + axisCode + axisIndexList.get (i);
          } // for
          gridVarName += "_x_x";
        } // if

        // Get the grid and return
        Grid grid;
        LOGGER.fine ("Retrieving grid " + gridVarName);
        try { grid = (Grid) reader.getVariable (gridVarName); }
        catch (IOException e) { throw new RuntimeException ("Error getting grid", e); }
        return (grid);

      } // accessGrid

      @Override
      public void releaseGrid (Grid grid) { grid.dispose(); }

      @Override
      public EarthTransform2D getTransform (String varName) { return ((EarthTransform2D) trans); }

      @Override
      public String getOrigin() { return (reader.getInfo().getOrigin()); }

      @Override
      public String getSource() { return (reader.getInfo().getSource()); }

      @Override
      public void dispose() {


        // What to release here?


      } // dispose

    };

    return (gridSet);

  } // create

  ////////////////////////////////////////////////////////////

  /**
   * Creates a grid set from multiple input sources.  The sources should all
   * have the same earth transform and different time stamps.
   *
   * @param sourceList the list of source datasets.
   * 
   * @return the grid set.
   * 
   * @throws IOException if there was an error accessing the source.
   */
  public static EarthGridSet createFromTimeSeries (List<String> sourceList) throws IOException {

    var gridNames = new TreeSet<String>();
    var infoList = new ArrayList<EarthDataInfo>();
    var readerMap = new HashMap<String, EarthDataReader>();

    // Loop over each source and gather information
    EarthTransform firstTrans = null;
    for (String source : sourceList) {
      var reader = EarthDataReaderFactory.create (source);
      readerMap.put (source, reader);
      var info = reader.getInfo();
      infoList.add (info);
      EarthTransform thisTrans = info.getTransform();
      if (firstTrans == null) firstTrans = thisTrans;
      else {
        if (!thisTrans.equals (firstTrans))
          throw (new RuntimeException ("Found earth transform mismatch between " + source + " and " + sourceList.get (0)));
      } // else
      gridNames.addAll (reader.getAllGrids());
    } // for
    var trans = firstTrans;

    // Eliminate any grid names that are not in all sources
    for (var reader : readerMap.values()) {
      var gridsInReader = reader.getAllGrids();
      gridNames.retainAll (gridsInReader);
    } // for
    if (gridNames.isEmpty())
      throw new RuntimeException ("No single variable name found in all input sources");
    var variableList = new ArrayList<String> (gridNames);

    // Sort the info items by date and create an axis of date values
    var entryList = new ArrayList<Entry<String, EarthDataInfo>>();
    for (int i = 0; i < sourceList.size(); i++)
      entryList.add (new SimpleEntry<> (sourceList.get (i), infoList.get (i)));
    entryList.sort (Comparator.comparing (Entry::getValue, Comparator.comparing (EarthDataInfo::getDate)));
    var dateList = new ArrayList<Instant>();
    for (var entry : entryList)
      dateList.add (entry.getValue().getDate().toInstant());
    var timeAxis = new Axis<Instant> (
      "time",
      AxisType.Time.toString(),
      "milliseconds since 1970-01-01T00:00:00Z",
      dateList,
      Instant.class
    );
    var axisList = new ArrayList<Axis>();
    axisList.add (timeAxis);
    LOGGER.fine ("Found axis " + timeAxis + " of size " + timeAxis.getSize());

    // Create a grid set using this information
    var gridSet = new EarthGridSet () {

      @Override
      public List<String> getVariables() { return (variableList); }

      @Override
      public List<Axis> getAxes (String varName) { return (axisList); }

      @Override
      public Grid accessGrid (String varName, List<Integer> axisIndexList) {

        LOGGER.fine ("Retrieving grid for " + varName + " with axes " + 
          Arrays.toString (axisIndexList.toArray()));

        // Get the grid and return
        int timeIndex = axisIndexList.get (0);
        var reader = readerMap.get (entryList.get (timeIndex).getKey());
        Grid grid;
        LOGGER.fine ("Retrieving grid " + varName);
        try { grid = (Grid) reader.getVariable (varName); }
        catch (IOException e) { throw new RuntimeException ("Error getting grid", e); }
        return (grid);

      } // accessGrid

      @Override
      public void releaseGrid (Grid grid) { grid.dispose(); }


      // TODO: Could we better handle opening / closing readers here?


      @Override
      public EarthTransform2D getTransform (String varName) { return ((EarthTransform2D) trans); }

      @Override
      public String getOrigin() { return (infoList.get (0).getOrigin()); }

      @Override
      public String getSource() { return (infoList.get (0).getSource()); }

      @Override
      public void dispose() {


        // What to release here?


      } // dispose

    };

    return (gridSet);

  } // create

  ////////////////////////////////////////////////////////////

} // EarthGridSetFactory class
