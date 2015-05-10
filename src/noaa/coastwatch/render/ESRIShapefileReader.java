////////////////////////////////////////////////////////////////////////
/*
     FILE: ESRIShapefileReader.java
  PURPOSE: Creates an overlay based on ESRI shapefile contents.
   AUTHOR: Peter Hollemans
     DATE: 2005/03/18
  CHANGES: 2005/05/26, PFH, changed vectorList to featureList
           2012/08/30, PFH, fixed toURL() deprecation

  CoastWatch Software Library and Utilities
  Copyright 1998-2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render;

// Imports
// -------
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryCollectionIterator;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.LineFeature;
import noaa.coastwatch.render.LineFeatureOverlay;
import noaa.coastwatch.render.LineFeatureSource;
import noaa.coastwatch.render.PointFeature;
import noaa.coastwatch.render.PointFeatureSource;
import noaa.coastwatch.render.PolygonFeature;
import noaa.coastwatch.render.PolygonFeatureOverlay;
import noaa.coastwatch.render.PolygonFeatureSource;
import noaa.coastwatch.util.EarthLocation;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureResults;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;

import com.vividsolutions.jts.geom.Polygon;
/**
 * <p>The <code>ESRIShapefileReader</code> class reads geographic
 * features from ESRI shapefile data and presents the data as an
 * {@link EarthDataOverlay} object.  ESRI shapefile format files may
 * contain a number of different types of feature geometries,
 * including points, polylines, and polygons.  The reader {@link
 * #getOverlay} method returns an overlay that is appropriate to the
 * feature geometry.</p>
 *
 * <p>This class uses the GeoTools version 2 library for accessing
 * shapefile data.  GeoTools is available from:
 * <pre>
 *   http://www.geotools.org
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

  /** The feature source used to read shapefile data. */
  private FeatureSource source;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the overlay created from the shapefile.  The same instance
   * of the overlay is returned each time.
   */
  public EarthDataOverlay getOverlay () { return (overlay); }

  ////////////////////////////////////////////////////////////

  /** Creates a new reader using a URL. */
  public ESRIShapefileReader (
    URL shapeURL
  ) throws IOException {

    // Create feature source
    // ---------------------
    ShapefileDataStore store = new ShapefileDataStore (shapeURL);
    String typeName = store.getTypeNames()[0];
    source = store.getFeatureSource (typeName);

    // Get prototype shape geometry
    // ----------------------------
    FeatureReader reader = source.getFeatures().reader();
    Geometry geom = null;
    while (geom == null && reader.hasNext()) {
      Feature feature;
      try { feature = reader.next(); }
      catch (IllegalAttributeException e) { continue; }
      Geometry featureGeom = feature.getDefaultGeometry();
      if (featureGeom != null) geom = featureGeom;
    } // while
    reader.close();

    // Check for null prototype geometry
    // ---------------------------------
    if (geom == null)
      throw new IOException ("Shapefile feature geometry is Null");

    // Create overlay based on prototype geometry
    // ------------------------------------------
    if (geom instanceof Point || geom instanceof MultiPoint) {


      /*
      PlotSymbol plotSymbol = new CircleSymbol();
      plotSymbol.setSize (8);
      PointFeatureSymbol featureSymbol = new SimpleSymbol (plotSymbol);
      overlay = new PointFeatureOverlay (featureSymbol, new PointSource());
      */


      throw new IOException ("Point data shapefiles not currently supported");


    } // if
    else if (geom instanceof LineString || geom instanceof MultiLineString) {
      overlay = new LineFeatureOverlay (Color.WHITE, new LineSource());
    } // if
    else if (geom instanceof Polygon || geom instanceof MultiPolygon) {
      overlay = new PolygonFeatureOverlay (Color.WHITE, new PolygonSource());
    } // else if

  } // ESRIShapefileReader constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Iterator over all basic geometries in a feature reader.  Basic
   * geometries are those that are not collections.  If a collection
   * is encountered, it is iterated over until all the basic
   * non-collection geometries are extracted.
   */
  private class BasicGeometryIterator 
    implements Iterator {

    /** The reader used for features. */
    private FeatureReader reader;
    
    /** The next available basic geometry. */
    private Geometry next;

    /** The current collection iterator, if any. */
    private GeometryCollectionIterator collectionIter;

    ////////////////////////////////////////////////////////

    /** Gets the next geometry, or null if none available. */
    private Geometry getNext () throws IOException {

      // Loop until we get the next
      // --------------------------
      Geometry thisNext = null;
      while ((reader.hasNext() || (collectionIter != null && 
        collectionIter.hasNext())) && thisNext == null) {

        // Check collection iterator
        // -------------------------
        if (collectionIter != null) {
          while (collectionIter.hasNext() && thisNext == null) {
            Geometry geom = (Geometry) collectionIter.next();
            if (!(geom instanceof GeometryCollection))
              thisNext = geom;
          } // while
        } // if
        
        // Get next from reader
        // --------------------
        if (thisNext == null) {
          if (reader.hasNext()) {
            Geometry geom;
            try { geom = reader.next().getDefaultGeometry(); }
            catch (IllegalAttributeException e) { continue; }
            if (geom instanceof GeometryCollection) {
              collectionIter = 
                new GeometryCollectionIterator ((GeometryCollection) geom); 
            } // if
            else {
              collectionIter = null;
              thisNext = geom;
            } // else
          } // if
        } // if

      } // while

      return (thisNext);

    } // getNext

    ////////////////////////////////////////////////////////

    /** Create a new iterator from features in the reader. */
    public BasicGeometryIterator (
      FeatureReader reader
    ) {

      this.reader = reader;
      try { next = getNext(); }
      catch (IOException e) { next = null; }

    } // BasicGeometryIterator constructor

    ////////////////////////////////////////////////////////

    /** Returns true if there are more basic geometries. */
    public boolean hasNext () { return (next != null); }
  
    ////////////////////////////////////////////////////////
    
    /** Returns the next available basic geometry. */
    public Object next () { 

      Geometry oldNext = next;
      try { next = getNext(); }
      catch (IOException e) { next = null; }
      return (oldNext);

    } // next

    ////////////////////////////////////////////////////////

    /** Throws an error since this operation is not supported. */
    public void remove () { throw new UnsupportedOperationException(); }

    ////////////////////////////////////////////////////////

  } // BasicGeometryIterator

  ////////////////////////////////////////////////////////////

  /**
   * The <code>LineSource</code> class uses the enclosing reader to
   * provide line data to an overlay. 
   */
  public class LineSource
    extends LineFeatureSource {

    /** The selected flag, true if select was called. */
    private boolean selected;

    ////////////////////////////////////////////////////////

    protected void select () throws IOException {

      // Check if already selected
      // -------------------------
      if (selected) return;
      selected = true;

      // Get lines
      // ---------
      FeatureReader reader = source.getFeatures().reader();
      BasicGeometryIterator iter = new BasicGeometryIterator (reader);
      while (iter.hasNext()) {
        Geometry geom = (Geometry) iter.next();
        Coordinate[] coords = geom.getCoordinates();
        LineFeature line = new LineFeature();
        for (int i = 0; i < coords.length; i++)
          line.add (new EarthLocation (coords[i].y, coords[i].x));
        featureList.add (line);
      } // while
      reader.close();

    } // select

    ////////////////////////////////////////////////////////

  } // LineSource

  ////////////////////////////////////////////////////////////

  /**
   * The <code>PolygonSource</code> class uses the enclosing reader to
   * provide polygon data to an overlay. 
   */
  public class PolygonSource
    extends PolygonFeatureSource {

    /** The selected flag, true if select was called. */
    private boolean selected;

    ////////////////////////////////////////////////////////

    protected void select () throws java.io.IOException {

      // Check if already selected
      // -------------------------
      if (selected) return;
      selected = true;

      // Get vectors
      // -----------
      FeatureReader reader = source.getFeatures().reader();
      BasicGeometryIterator iter = new BasicGeometryIterator (reader);
      while (iter.hasNext()) {
        Geometry geom = (Geometry) iter.next();
        Coordinate[] coords = geom.getCoordinates();
        PolygonFeature polygon = 
          new PolygonFeature (PolygonFeature.COUNTER_CLOCKWISE);
        for (int i = 0; i < coords.length; i++)
          polygon.add (new EarthLocation (coords[i].y, coords[i].x));
        polygonList.add (polygon);
      } // while
      reader.close();

    } // select

    ////////////////////////////////////////////////////////

  } // PolygonSource

  ////////////////////////////////////////////////////////////

  /**
   * The <code>PointSource</code> class uses the enclosing reader to
   * provide point data to an overlay. 
   */
  public class PointSource
    extends PointFeatureSource {

    /** The selected flag, true if select was called. */
    private boolean selected;

    ////////////////////////////////////////////////////////

    protected void select () throws java.io.IOException {

      // Check if already selected
      // -------------------------
      if (selected) return;
      selected = true;

      // Get points
      // ----------
      FeatureReader reader = source.getFeatures().reader();
      BasicGeometryIterator iter = new BasicGeometryIterator (reader);
      while (iter.hasNext()) {
        Geometry geom = (Geometry) iter.next();
        Coordinate[] coords = geom.getCoordinates();
        EarthLocation loc = new EarthLocation (coords[0].y, coords[0].x);
        featureList.add (new PointFeature (loc));
      } // while
      reader.close();

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

    ESRIShapefileReader reader = 
      new ESRIShapefileReader (new File (argv[0]).toURI().toURL());
    EarthDataOverlay overlay = reader.getOverlay();

  } // main

  ////////////////////////////////////////////////////////////

} // ESRIShapefileReader class

////////////////////////////////////////////////////////////////////////

