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

import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFUtils;
import com.linuxense.javadbf.DBFDataType;
import java.math.BigDecimal;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;

import noaa.coastwatch.render.feature.LineFeature;
import noaa.coastwatch.render.feature.LineFeatureSource;
import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.PointFeatureSource;
import noaa.coastwatch.render.feature.PolygonFeature;
import noaa.coastwatch.render.feature.PolygonFeatureSource;
import noaa.coastwatch.render.feature.Attribute;

import noaa.coastwatch.render.SimpleSymbol;
import noaa.coastwatch.render.PlotSymbolFactory;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.PointFeatureOverlay;
import noaa.coastwatch.render.LineFeatureOverlay;
import noaa.coastwatch.render.PolygonFeatureOverlay;

import noaa.coastwatch.util.EarthLocation;

import java.util.logging.Logger;
import java.util.logging.Level;

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

  private static final Logger LOGGER = Logger.getLogger (ESRIShapefileReader.class.getName());

  // Variables
  // ---------

  /** The overlay created from the shapefile. */
  private EarthDataOverlay overlay;

  /** The reader for shapefile data. */
  private ShapeFileReader reader;
  
  /** The input stream used for reading shapes. */
  private InputStream inputStream;

  /** The database file reader or null for none. */
  private DBFReader dbfReader;

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

    AttributedFeatureSource featureSource;

    try {

      // First we create the shape reader from a URL.  We set up the 
      // preferences to allow an unlimited number of points per shape,
      // otherwise it's limited to 10000.  The documentation recommends
      // against this in case of corrupt files with a large number of points
      // which can cause an out of memory error.

      inputStream = shapeURL.openStream();
      ValidationPreferences prefs = new ValidationPreferences();
      prefs.setAllowUnlimitedNumberOfPointsPerShape (true);
      reader = new ShapeFileReader (inputStream, prefs);

      // Look for the shape type for this file.

      ShapeType shapeType = reader.getHeader().getShapeType();
      LOGGER.fine ("Found shapefile type = " + shapeType);

      switch (shapeType) {

      // In the case of a point file, we create a PointFeatureOverlay
      // with a PointFeatureSource from this reader.  We assign an initial
      // default symbol (which defaults to no fill and black outline color).

      case MULTIPOINT:
      case MULTIPOINT_M:
      case MULTIPOINT_Z:
      case POINT:
      case POINT_M:
      case POINT_Z:
        SimpleSymbol symbol = new SimpleSymbol (PlotSymbolFactory.create ("Circle"));
        var pointSource = new PointSource();
        overlay = new PointFeatureOverlay (symbol, pointSource);
        featureSource = pointSource;
        break;

      // In the case of a polygon file, we create a PolygonFeatureOverlay
      // with a PolygonFeatureSource from this reader.  We assign an initial
      // white line drawing colour and no fill colour.

      case POLYGON:
      case POLYGON_M:
      case POLYGON_Z:
        var polygonSource = new PolygonSource();
        overlay = new PolygonFeatureOverlay (Color.WHITE, polygonSource);
        featureSource = polygonSource;
        break;

      // In the case of a polyline file, we create a LineFeatureOverlay
      // with a LineFeatureSource from this reader.  We assign an initial
      // white line drawing colour.

      case POLYLINE:
      case POLYLINE_M:
      case POLYLINE_Z:
        var lineSource = new LineSource();
        overlay = new LineFeatureOverlay (Color.WHITE, lineSource);
        featureSource = lineSource;
        break;

      default:
        throw new IOException ("Unsupported shapefile type " + shapeType);

      } // switch

    } // try
    
    catch (IOException | InvalidShapeFileException e) {
      if (inputStream != null) inputStream.close();
      throw new IOException (e.getMessage());
    } // catch

    // Now try to open a DBF file if it exists.  We use the data in the DBF
    // records in two ways: (i) the AbstractFeatureSouce objects created 
    // by this class hold the attribute names and types from the
    // DBF, and (ii) the AbstractFeature objects produced from the source
    // objects hold the attribute values.

    var fileName = shapeURL.toString();
    int ext = fileName.indexOf (".shp");
    if (ext != -1) {
      String dbfFile = fileName.substring (0, ext) + ".dbf";
      try {

        var dbfUrl = new URL (dbfFile);
        var dbfStream = dbfUrl.openStream();
        dbfReader = new DBFReader (dbfStream);
        LOGGER.fine ("Found DBF file at " + dbfFile);

        int fields = dbfReader.getFieldCount();
        var attList = new ArrayList<Attribute>();
        LOGGER.fine ("Found " + fields + " attribute field(s)");
        for (int i = 0; i < fields; i++) {
          DBFField field = dbfReader.getField (i);
          var name = field.getName();
          var type = field.getType();
          var att = new Attribute (name, getJavaType (type), "");
          LOGGER.fine ("att = " + att.toDebugString());
          attList.add (att);
        } // for
        featureSource.updateAttributes (attList);

      } // try
      catch (Exception e) {
        LOGGER.log (Level.FINE, "Error accessing DBF for shapefile", e);
      } // catch

    } // if

  } // ESRIShapefileReader constructor

  ////////////////////////////////////////////////////////////

  /**
   * Converts a DBF type into a Java type.  The conversion was copied
   * from the DBFReader Javadoc page.
   * 
   * @param type the DBF type.
   * 
   * @return the Java type
   *
   * @throws RuntimeException if the DBF type could not be converted.
   */
  private Class getJavaType (DBFDataType type) {

    // Note that here, any BigDecimal types are reported as Double.  Therefore
    // in other places that read the attribute values, BigDecimal values need
    // to be converted to Double values.

    Class javaType = null;
    switch (type.getCharCode()) {
    case 'C': javaType = String.class; break;
    // case 'N': javaType = BigDecimal.class; break;
    // case 'F': javaType = BigDecimal.class; break;
    case 'N': javaType = Double.class; break;
    case 'F': javaType = Double.class; break;
    case 'L': javaType = Boolean.class; break;
    case 'D': javaType = Date.class; break;
    // case 'Y': javaType = BigDecimal.class; break;
    case 'Y': javaType = Double.class; break;
    case 'I': javaType = Integer.class; break;
    case 'T': javaType = Date.class; break;
    case '@': javaType = Date.class; break;
    case 'V': javaType = String.class; break;
    case 'O': javaType = Double.class; break;
    case 'M': javaType = String.class; break;
    case 'B': javaType = byte[].class; break;
    case 'G': javaType = byte[].class; break;
    case 'P': javaType = byte[].class; break;
    case 'Q': javaType = byte[].class; break;
    default: throw new RuntimeException ("Illegal DBF type: " + type);
    } // switch

    return (javaType);

  } // getJavaType

  ////////////////////////////////////////////////////////////

  private interface AttributedFeatureSource {
    default void updateAttributes (List<Attribute> attList) { }
  }  // AttributedFeatureSource

  ////////////////////////////////////////////////////////////

  /**
   * The <code>LineSource</code> class accesses the polyline data in the
   * shapefile.
   */
  private class LineSource extends LineFeatureSource 
    implements AttributedFeatureSource {

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
        int totalShapes = 0;
        int totalParts = 0;
        int totalPoints = 0;
        while ((polyShape = (AbstractPolyShape) reader.next()) != null) {

          totalShapes++;
          int parts = polyShape.getNumberOfParts();
          totalParts += parts;

          for (int i = 0; i < parts; i++) {
            LineFeature line = new LineFeature();
            PointData[] points = polyShape.getPointsOfPart (i);
            totalPoints += points.length;
            for (int j = 0; j < points.length; j++)
              line.add (new EarthLocation (points[j].getY(), points[j].getX()));
            featureList.add (line);
          } // for

        } // while

        LOGGER.fine ("Read polyline data with " + totalShapes + " shapes, " + 
          totalParts + " parts, " + totalPoints + " points");

      } // try

      catch (InvalidShapeFileException e) { throw new IOException (e.getMessage()); }

      finally {
        inputStream.close();
        if (dbfReader != null) dbfReader.close();
      } // finally

    } // select

    ////////////////////////////////////////////////////////

  } // LineSource class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>PolygonSource</code> class accesses the polygon data in the
   * shapefile.
   */
  private class PolygonSource extends PolygonFeatureSource
    implements AttributedFeatureSource {

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
        int totalShapes = 0;
        int totalParts = 0;
        int totalPoints = 0;
        while ((polyShape = (AbstractPolyShape) reader.next()) != null) {

          totalShapes++;
          int parts = polyShape.getNumberOfParts();
          totalParts += parts;

          for (int i = 0; i < parts; i++) {
            PolygonFeature polygon = new PolygonFeature (PolygonFeature.COUNTER_CLOCKWISE);
            PointData[] points = polyShape.getPointsOfPart (i);
            totalPoints += points.length;
            for (int j = 0; j < points.length; j++)
              polygon.add (new EarthLocation (points[j].getY(), points[j].getX()));
            polygonList.add (polygon);
          } // for

          polygonList.add (new PolygonFeature (PolygonFeature.COUNTER_CLOCKWISE));

        } // while

        LOGGER.fine ("Read polygon data with " + totalShapes + " shapes, " + 
          totalParts + " parts, " + totalPoints + " points");

      } // try

      catch (InvalidShapeFileException e) { throw new IOException (e.getMessage()); }

      finally {
        inputStream.close();
        if (dbfReader != null) dbfReader.close();
      } // finally

    } // select

    ////////////////////////////////////////////////////////

  } // PolygonSource class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>PointSource</code> class accesses the point data in the
   * shapefile.
   */
  private class PointSource extends PointFeatureSource
    implements AttributedFeatureSource {

    /** The selected flag, true if select was called. */
    private boolean selected;

    ////////////////////////////////////////////////////////

    public void updateAttributes (List<Attribute> attList) { setAttributes (attList); }

    ////////////////////////////////////////////////////////

    @Override
    protected void select () throws IOException {

      // Check if already selected
      // -------------------------
      if (selected) return;
      selected = true;

      try {

        int totalShapes = 0;
        int totalPoints = 0;

        ShapeType shapeType = reader.getHeader().getShapeType();
        switch (shapeType) {

        // Iterate over each multipoint shape and create point features
        // ------------------------------------------------------------
        case MULTIPOINT:
        case MULTIPOINT_M:
        case MULTIPOINT_Z:
          AbstractMultiPointShape multiPointShape;
          while ((multiPointShape = (AbstractMultiPointShape) reader.next()) != null) {

            totalShapes++;
            PointData[] points = multiPointShape.getPoints();
            totalPoints += points.length;
            for (int i = 0; i < points.length; i++) {
              EarthLocation loc = new EarthLocation (points[i].getY(), points[i].getX());
              featureList.add (new PointFeature (loc));
            } // for

            // TODO: In this case, should we read the DBF file?  Each record
            // in the DBF file pertains to a shape in the shapefile, but
            // we separate out the points of a single multipoint shape into
            // separate features.  And in our model, attributes are attached
            // to each feature, not a collection of features. 

          } // while
          break;

        // Iterate over each point shape and create point features
        // -------------------------------------------------------
        case POINT:
        case POINT_M:
        case POINT_Z:
          AbstractPointShape pointShape;
          while ((pointShape = (AbstractPointShape) reader.next()) != null) {

            totalShapes++;
            totalPoints++;

            Object[] attValues = null;
            if (dbfReader != null) {
              attValues = dbfReader.nextRecord();              
              if (attValues == null) throw new IOException ("Error reading DBF data at record " + totalShapes);
              for (int i = 0; i < attValues.length; i++) {
                if (attValues[i] instanceof BigDecimal) 
                  attValues[i] = ((BigDecimal) attValues[i]).doubleValue();
              } // for
              LOGGER.finer ("Point feature " + totalShapes + " atts = " + Arrays.toString (attValues));
            } // if
            
            EarthLocation loc = new EarthLocation (pointShape.getY(), pointShape.getX());
            var feature = new PointFeature (loc, attValues);
            featureList.add (feature);

          } // while
          break;

        default:
          throw new IOException ("Unexpected shape type " + shapeType);

        } // switch

        LOGGER.fine ("Read point data with " + totalShapes + " shapes, " + totalPoints + " points");

      } // try
      
      catch (InvalidShapeFileException e) { throw new IOException (e.getMessage()); }
      
      finally {
        inputStream.close();
        if (dbfReader != null) dbfReader.close();
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

