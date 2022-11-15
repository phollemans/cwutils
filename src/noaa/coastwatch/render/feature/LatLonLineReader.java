/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.render.feature;

import java.awt.Color;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

import java.util.ArrayList;

import java.net.URI;
import java.net.URL;

import noaa.coastwatch.render.LineFeatureOverlay;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.feature.LineFeature;
import noaa.coastwatch.render.feature.LineFeatureSource;

import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.Datum;
import noaa.coastwatch.util.trans.DatumFactory;
import noaa.coastwatch.util.trans.SpheroidConstants;

import noaa.coastwatch.io.SimpleParser;

import java.util.logging.Logger;

/**
 * <p>The <code>LatLonLineReader</code> class reads sets of latitude/longitude 
 * locations to create line features and presents the data as a
 * {@link noaa.coastwatch.render.EarthDataOverlay} object.  If the lat/lon data
 * contains out of range values (eg: -999), then the line data is broken up
 * into multiple segments of continuous line data using the out of range
 * locations as endpoints.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.0
 */
public class LatLonLineReader {

  private static final Logger LOGGER = Logger.getLogger (LatLonLineReader.class.getName());

  /** The overlay created from the line data. */
  private EarthDataOverlay overlay;
  
  /** The file used for reading line data. */
  private String filename;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the overlay created from the line data.
   *
   * @return the overlay created from the lines read.
   */
  public EarthDataOverlay getOverlay () { return (overlay); }

  ////////////////////////////////////////////////////////////

  /**
   * Performs a test parsing of a list of latitude/longitude locations 
   * in a text file.
   * 
   * @param filename the name of the text file to read.
   * 
   * @throws IOException if an error occurred parsing the locations, or no
   * valid locations were found.
   */
  public static void testLocationData (
    String filename
  ) throws IOException { 

    int valid = 0;

    try (
      var stream = new FileInputStream (new File (filename));
      var reader = new InputStreamReader (stream);
      var buffer = new BufferedReader (reader);      
    ) {
      var parser = new SimpleParser (buffer);
      do {
        double lat = parser.getNumber();
        if (lat < -90 || lat > 90) lat = Double.NaN;
        double lon = parser.getNumber();
        if (lon < -360 || lon > 360) lon = Double.NaN;
        if (!Double.isNaN (lat) && !Double.isNaN (lon)) valid++;
      } while (!parser.eof());
    } // try

    if (valid == 0) throw new IOException ("No valid lat/lon locations found");

  } // testLocationData

  ////////////////////////////////////////////////////////////

  /**
   * <p>Reads a list of latitude/longitude locations in a text file, 
   * for example:</p>
   * 
   * <pre>
   *   30.0 -150.0
   *   31.0 -152.0
   *   ...
   * </pre>
   * 
   * <p>Any locations with lat/lon values out of range are marked 
   * so that {@link EarthLocation#isValid} returns false.</p>
   * 
   * @param filename the name of the text file to read.
   * @param datum the datum to use for creating the location points, or null
   * to use WGS84.
   * 
   * @return the list of points read.
   * 
   * @throws IOException if an error occurred parsing the locations.
   */
  public static List<EarthLocation> readLocationData (
    String filename,
    Datum datum
  ) throws IOException { 

    var locations = new ArrayList<EarthLocation>();
    if (datum == null) datum = DatumFactory.create (SpheroidConstants.WGS84);
    try (
      var stream = new FileInputStream (new File (filename));
      var reader = new InputStreamReader (stream);
      var buffer = new BufferedReader (reader);      
    ) {
      var parser = new SimpleParser (buffer);
      do {
        double lat = parser.getNumber();
        if (lat < -90 || lat > 90) lat = Double.NaN;
        double lon = parser.getNumber();
        if (lon < -360 || lon > 360) lon = Double.NaN;
        locations.add (new EarthLocation (lat, lon, datum));
      } while (!parser.eof());
    } // try

    return (locations);

  } // readLocationData

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new lat/lon line reader from data in a file.
   *
   * @param filename the filename for the lat/lon line file.
   */
  public LatLonLineReader (
    String filename
  ) throws IOException {

    var file = new File (filename);
    if (!file.canRead()) throw new IOException ("Cannot read " + filename);
    testLocationData (filename);

    this.filename = filename;
    this.overlay = new LineFeatureOverlay (Color.WHITE, new LineSource());

  } // LatLonLineReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * The <code>LineSource</code> class accesses the data in the
   * lat/lon point file.
   */
  private class LineSource extends LineFeatureSource {

    /** The selected flag, true if select was called. */
    private boolean selected;

    ////////////////////////////////////////////////////////

    @Override
    protected void select () throws IOException {

      if (!selected) {

        selected = true;

        var locationData = readLocationData (filename, null);
        var line = new LineFeature();
        for (var loc : locationData) {
          if (!loc.isValid() && line.size() != 0) {
            featureList.add (line);
            line = new LineFeature();
          } // if
          else {
            line.add (loc);
          } // else
        } // for
        if (line.size() != 0) featureList.add (line);

      } // if

    } // select

    ////////////////////////////////////////////////////////

  } // LineSource class

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    var filename = argv[0];
    var reader = new LatLonLineReader (filename);
    var overlay = reader.getOverlay();
    
    System.out.println ("overlay = " + overlay);

  } // main

  ////////////////////////////////////////////////////////////

} // LatLonLineReader class

