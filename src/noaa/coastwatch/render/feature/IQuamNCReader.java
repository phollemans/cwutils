////////////////////////////////////////////////////////////////////////
/*
     FILE: IQuamNCReader.java
  PURPOSE: To provide SST quality monitoring point data from the iQuam system.
   AUTHOR: Peter Hollemans
     DATE: 2016/06/20
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.render.feature;

// Imports
// -------

import java.io.IOException;

import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Attribute;

import noaa.coastwatch.render.feature.PointFeature;
import noaa.coastwatch.render.feature.PointFeatureSource;

import noaa.coastwatch.util.EarthArea;
import noaa.coastwatch.util.EarthLocation;

/**
 * The iQuam data reader reads NOAA iQuam (in-situ SST quality monitoring)
 * system data files and presents the data as point features.  Data files
 * are obtained from:
 * <blockquote>
 *   http://www.star.nesdis.noaa.gov/sod/sst/iquam/v2/index.html
 * </blockquote>
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class IQuamNCReader
  extends PointFeatureSource {

  ////////////////////////////////////////////////////////

  @Override
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

  ////////////////////////////////////////////////////////////

} // BinnedGSHHSReader class

////////////////////////////////////////////////////////////////////////
