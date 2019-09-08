////////////////////////////////////////////////////////////////////////
/*

     File: ESRIShapefileReader.java
   Author: Peter Hollemans
     Date: 2005/03/18

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
package noaa.coastwatch.render.feature;

// Imports
// -------
import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader;
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException;
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.AbstractPolyShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.AbstractPointShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.AbstractMultiPointShape;
import org.nocrala.tools.gis.data.esri.shapefile.ValidationPreferences;
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType;
import org.nocrala.tools.gis.data.esri.shapefile.shape.PointData;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.feature.LineFeature;
import noaa.coastwatch.render.LineFeatureOverlay;
import noaa.coastwatch.render.feature.LineFeatureSource;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.feature.PolygonFeature;
import noaa.coastwatch.render.PolygonFeatureOverlay;
import noaa.coastwatch.render.feature.PolygonFeatureSource;
import noaa.coastwatch.util.EarthLocation;

/**
 * <p>The <code>ESRIShapefileReader</code> class reads geographic
 * features from ESRI shapefile data and presents the data as an
 * {@link EarthDataOverlay} object.  ESRI shapefile format files may
 * contain a number of different types of feature geometries,
 * including points, polylines, and polygons.  The reader {@link
 * #getOverlay} method returns an overlay that is appropriate to the
 * feature geometry.</p>
 *
 * <p>As of version 3.5.1, this class uses the Java ESRI Shape File Reader
 * library available from:
 * <pre>
 *   https://sourceforge.net/projects/javashapefilere/
 * </pre>
 * Additional information on ESRI shapefiles and a technical
 * description of the format may be obtained from the ESRI web site:
 * <pre>
 *   http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class ESRIShapefileReader {

  // Variables
  // ---------

  /** The overlay created from the shapefile. */
  private EarthDataOverlay overlay;

  /** The reader for shapefile data. */
  private ShapeFileReader reader;
  
  /** The input stream used for reading shapes. */
  private InputStream inputStream;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the overlay created from the shapefile.
   *
   * @return the overlay created from the shapes read.
   */
  public EarthDataOverlay getOverlay () { return (overlay); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new shapefile reader using a URL.
   *
   * @param shapeURL the URL for the shape file.
   */
  public ESRIShapefileReader (
    URL shapeURL
  ) throws IOException {

    try {
    
      // Create shape reader
      // -------------------
      inputStream = shapeURL.openStream();
      ValidationPreferences prefs = new ValidationPreferences();
      prefs.setAllowUnlimitedNumberOfPointsPerShape (true);
      reader = new ShapeFileReader (inputStream, prefs);

      // Create overlay based on prototype geometry
      // ------------------------------------------
      ShapeType shapeType = reader.getHeader().getShapeType();
      switch (shapeType) {

      case MULTIPOINT:
      case MULTIPOINT_M:
      case MULTIPOINT_Z:
      case POINT:
      case POINT_M:
      case POINT_Z:
        /*
        PlotSymbol plotSymbol = new CircleSymbol();
        plotSymbol.setSize (8);
        PointFeatureSymbol featureSymbol = new SimpleSymbol (plotSymbol);
        overlay = new PointFeatureOverlay (featureSymbol, new PointSource());
        */
        throw new IOException ("Point and multipoint shapefiles are not supported");

      case POLYGON:
      case POLYGON_M:
      case POLYGON_Z:
        overlay = new PolygonFeatureOverlay (Color.WHITE, new PolygonSource());
        break;

      case POLYLINE:
      case POLYLINE_M:
      case POLYLINE_Z:
        overlay = new LineFeatureOverlay (Color.WHITE, new LineSource());
        break;

      default:
        throw new IOException ("Unsupported shapefile type " + shapeType);

      } // switch

    } // try
    
    catch (IOException | InvalidShapeFileException e) {
      if (inputStream != null) inputStream.close();
      throw new IOException (e.getMessage());
    } // catch

  } // ESRIShapefileReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * The <code>LineSource</code> class accesses the polyline data in the
   * shapefile.
   */
  private class LineSource extends LineFeatureSource {

    /** The selected flag, true if select was called. */
    private boolean selected;

    ////////////////////////////////////////////////////////

    @Override
    protected void select () throws IOException {

      // Check if already selected
      // -------------------------
      if (selected) return;
      selected = true;

      try {

        // Iterate over each shape part and create line features
        // -----------------------------------------------------
        AbstractPolyShape polyShape;
        while ((polyShape = (AbstractPolyShape) reader.next()) != null) {

          int parts = polyShape.getNumberOfParts();

          for (int i = 0; i < parts; i++) {
            LineFeature line = new LineFeature();
            PointData[] points = polyShape.getPointsOfPart (i);
            for (int j = 0; j < points.length; j++)
              line.add (new EarthLocation (points[j].getY(), points[j].getX()));
            featureList.add (line);
          } // for

        } // while

      } // try

      catch (InvalidShapeFileException e) { throw new IOException (e.getMessage()); }

      finally {
        inputStream.close();
      } // finally

    } // select

    ////////////////////////////////////////////////////////

  } // LineSource class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>PolygonSource</code> class accesses the polygon data in the
   * shapefile.
   */
  private class PolygonSource extends PolygonFeatureSource {

    /** The selected flag, true if select was called. */
    private boolean selected;

    ////////////////////////////////////////////////////////

    @Override
    protected void select () throws IOException {

      // Check if already selected
      // -------------------------
      if (selected) return;
      selected = true;

      try {

        /**
         * The ESRI documentation for polygon shapefiles says this:
         *
         * "A polygon consists of one or more rings. A ring is a connected
         * sequence of four or more points that form a closed,
         * non-self-intersecting loop. A polygon may contain multiple
         * outer rings. The order of vertices or orientation for a ring
         * indicates which side of the ring is the interior of the polygon.
         * The neighborhood to the right of an observer walking along the
         * ring in vertex order is the neighborhood inside the polygon.
         * Vertices of rings defining holes in polygons are in a
         * counterclockwise direction. Vertices for a single, ringed polygon
         * are, therefore, always in clockwise order. The rings of a polygon
         * are referred to as its parts."
         *
         * So in this reading loop, we separate out the parts of an individual
         * polygon into polygon features and then terminate the list of parts
         * using a zero-length polygon.  This indicates to the rendering code
         * that the polygon parts should be rendered together.
         */

        // Iterate over each shape part and create polygon features
        // --------------------------------------------------------
        AbstractPolyShape polyShape;
        while ((polyShape = (AbstractPolyShape) reader.next()) != null) {

          int parts = polyShape.getNumberOfParts();

          for (int i = 0; i < parts; i++) {
            PolygonFeature polygon = new PolygonFeature (PolygonFeature.COUNTER_CLOCKWISE);
            PointData[] points = polyShape.getPointsOfPart (i);
            for (int j = 0; j < points.length; j++)
              polygon.add (new EarthLocation (points[j].getY(), points[j].getX()));
            polygonList.add (polygon);
          } // for

          polygonList.add (new PolygonFeature (PolygonFeature.COUNTER_CLOCKWISE));

        } // while

      } // try

      catch (InvalidShapeFileException e) { throw new IOException (e.getMessage()); }

      finally {
        inputStream.close();
      } // finally

    } // select

    ////////////////////////////////////////////////////////

  } // PolygonSource class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>PointSource</code> class accesses the point data in the
   * shapefile.
   */
  private class PointSource extends PointFeatureSource {

    /** The selected flag, true if select was called. */
    private boolean selected;

    ////////////////////////////////////////////////////////

    @Override
    protected void select () throws IOException {

      // Check if already selected
      // -------------------------
      if (selected) return;
      selected = true;

      try {

        ShapeType shapeType = reader.getHeader().getShapeType();
        switch (shapeType) {

        // Iterate over each multipoint shape and create point features
        // ------------------------------------------------------------
        case MULTIPOINT:
        case MULTIPOINT_M:
        case MULTIPOINT_Z:
          AbstractMultiPointShape multiPointShape;
          while ((multiPointShape = (AbstractMultiPointShape) reader.next()) != null) {
            PointData[] points = multiPointShape.getPoints();
            for (int i = 0; i < points.length; i++) {
              EarthLocation loc = new EarthLocation (points[i].getY(), points[i].getX());
              featureList.add (new PointFeature (loc));
            } // for
          } // while
          break;

        // Iterate over each point shape and create point features
        // -------------------------------------------------------
        case POINT:
        case POINT_M:
        case POINT_Z:
          AbstractPointShape pointShape;
          while ((pointShape = (AbstractPointShape) reader.next()) != null) {
            EarthLocation loc = new EarthLocation (pointShape.getY(), pointShape.getX());
            featureList.add (new PointFeature (loc));
          } // while

        default:
          throw new IOException ("Unexpected shape type " + shapeType);

        } // switch

      } // try
      
      catch (InvalidShapeFileException e) { throw new IOException (e.getMessage()); }
      
      finally {
        inputStream.close();
      } // finally

    } // select

    ////////////////////////////////////////////////////////

  } // PointSource

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    URL url = new File (argv[0]).toURI().toURL();
    ESRIShapefileReader reader = new ESRIShapefileReader (url);
    EarthDataOverlay overlay = reader.getOverlay();
    
    System.out.println ("overlay = " + overlay);

  } // main

  ////////////////////////////////////////////////////////////

} // ESRIShapefileReader class

////////////////////////////////////////////////////////////////////////

