////////////////////////////////////////////////////////////////////////
/*
     FILE: SatellitePassInfo.java
  PURPOSE: A class to hold satellite pass information.
   AUTHOR: Peter Hollemans
     DATE: 2003/01/13
  CHANGES: 2003/03/26, PFH, modified to use new ServerQuery
           2004/08/31, PFH, modified to use new SatelliteDataInfo parent

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.awt.*;
import java.util.*;
import java.io.*;
import java.text.*;
import java.net.*;
import noaa.coastwatch.render.*;
import noaa.coastwatch.net.*;

/**
 * A satellite pass info object holds information about one pass
 * of a satellite over the Earth.
 */
public class SatellitePassInfo
  extends SatelliteDataInfo {

  // Variables
  // ---------
  /** The pass identification string. */
  private String passID;

  /** The pass scene time: 'day', 'night', or 'day/night'. */
  private String sceneTime;

  /** The pass orbit type: 'ascending' or 'descending'. */
  private String orbitType;

  /** The pass size in lines and samples. */
  private int lines, samples;

  /** The ground station the captured the pass. */
  private String groundStation;

  /** The polygon delineating the pass coverage. */
  private LineFeature coveragePolygon;

  /** The pass center point. */
  private EarthLocation center;

  /** The URL used to retrieve a preview image of the pass. */
  private String previewURL;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the pass info date from the server query.
   *
   * @param query the server query results.
   * @param result the server query result index.
   * 
   * @return the date or null if no date could be correctly parsed.
   */
  private static Date extractDate (
    ServerQuery query,
    int result
  ) {

    // Create date
    // -----------
    String passDate = query.getValue (result, "date");
    String startTime = query.getValue (result, "time");
    Date date;
    try {
      SimpleDateFormat dateFormat = new SimpleDateFormat (
        "yyyy-MM-dd HH:mm:ss z");
      date = dateFormat.parse (passDate + " " + startTime + " GMT");
    } catch (Exception e) { date = null; }

    return (date);

  } // extractDate

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new satellite pass info object using the specified ID
   * and query results.
   * 
   * @param passID the satellite pass identifier used for the query.
   * @param query the server query results.
   * @param result the server query result index.
   */
  public SatellitePassInfo (
    String passID,
    ServerQuery query,
    int result
  ) {

    // Call the super class constructor
    // --------------------------------
    super (
      query.getValue (result, "satellite"),
      query.getValue (result, "sensor"),
      extractDate (query, result),
      null, 
      query.getURL(),
      ""
    );

    // Create string fields
    // --------------------
    this.passID = passID;
    sceneTime = query.getValue (result, "scene_time");
    orbitType = query.getValue (result, "orbit_type");
    groundStation = query.getValue (result, "description");
    previewURL = query.getValue (result, "preview_url");

    // Get dimensions
    // --------------
    try {
      lines = Integer.parseInt (query.getValue (result, "dim_rows"));
      samples = Integer.parseInt (query.getValue (result, "dim_cols"));
    } catch (Exception e) { lines = samples = 0; }        

    // Check GCP values
    // ----------------
    String gcpLats = query.getValue (result, "gcp_lats");
    String gcpLons = query.getValue (result, "gcp_lons");
    if (gcpLats.equals ("") || gcpLons.equals ("")) {
      coveragePolygon = null;
      center = new EarthLocation (Double.NaN, Double.NaN);
    } // if

    // Use GCP values
    // --------------
    else {

      // Create Earth locations
      // ----------------------
      gcpLats = gcpLats.replaceAll ("[\\{\\}]", "");
      gcpLons = gcpLons.replaceAll ("[\\{\\}]", "");
      String[] gcpLatVals = gcpLats.split (",");
      String[] gcpLonVals = gcpLons.split (",");
      int gcpMeshPoints = (int) Math.round (Math.sqrt (gcpLatVals.length));
      EarthLocation gcpPoints[] = new EarthLocation[
        gcpMeshPoints*gcpMeshPoints];
      for (int i = 0; i < gcpPoints.length; i++) {
        gcpPoints[i] = new EarthLocation (Double.parseDouble (gcpLatVals[i]),
          Double.parseDouble (gcpLonVals[i]));
      } // for

      // Create coverage polygon
      // -----------------------
      coveragePolygon = new LineFeature();
      for (int i = 0; i < gcpMeshPoints; i++)
        coveragePolygon.add (gcpPoints[i]);
      for (int i = gcpMeshPoints*2-1; i <= gcpPoints.length-1;
        i += gcpMeshPoints)
        coveragePolygon.add (gcpPoints[i]);
      for (int i = gcpPoints.length - 2; i >= gcpPoints.length - 
        gcpMeshPoints; i--)
        coveragePolygon.add (gcpPoints[i]);
      for (int i = gcpPoints.length - gcpMeshPoints*2; i >= 0;
        i -= gcpMeshPoints) 
        coveragePolygon.add (gcpPoints[i]);

      // Save center point
      // -----------------
      center = gcpPoints[gcpPoints.length/2];

    } // else
 
  } // SatellitePassInfo constructor

  ////////////////////////////////////////////////////////////

  /** Gets the pass center point. */
  public EarthLocation getCenter () { return ((EarthLocation)center.clone()); }

  ////////////////////////////////////////////////////////////

  /** Gets the pass identifier. */
  public String getPassID () { return (passID); }

  ////////////////////////////////////////////////////////////

  /** Gets the orbit type: 'ascending' or 'descending'. */
  public String getOrbitType () { return (orbitType); }
  
  ////////////////////////////////////////////////////////////

  /** Gets the pass dimensions as [lines, samples]. */
  public int[] getDimensions () { return (new int[] {lines, samples}); }

  ////////////////////////////////////////////////////////////

  /** Gets the pass ground capture station. */
  public String getGroundStation () { return (groundStation); }

  ////////////////////////////////////////////////////////////

  /** Gets the pass coverage polygon. */
  public LineFeature getCoveragePolygon () { return (coveragePolygon); }

  ////////////////////////////////////////////////////////////

  /** Gets the pass preview URL. */
  public String getPreviewURL () { return (previewURL); }

  ////////////////////////////////////////////////////////////

  /** Gets the pass scene time: 'day', 'night', or 'day/night'. */
  public String getSceneTime () { return (sceneTime); }

  ////////////////////////////////////////////////////////////

  public String getSceneTime (
    DataLocation upperLeft,
    DataLocation lowerRight
  ) {

    return (getSceneTime());

  } // getSceneTime

  ////////////////////////////////////////////////////////////

  public String getSceneTime (
    int[] dims
  ) {

    return (getSceneTime());

  } // getSceneTime

  ////////////////////////////////////////////////////////////

} // SatellitePassInfo

////////////////////////////////////////////////////////////////////////
