////////////////////////////////////////////////////////////////////////
/*

     File: ResamplingDiagnostic.java
   Author: Peter Hollemans
     Date: 2019/03/12

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util;

// Imports
// -------
import java.util.List;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Collectors;

import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.ResamplingMap;
import noaa.coastwatch.util.ResamplingMapFactory;
import noaa.coastwatch.util.ResamplingSourceImp;
import noaa.coastwatch.util.trans.Datum;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

import java.util.logging.Logger;
import java.util.Arrays;

/**
 * A <code>ResamplingDiagnostic</code> generates statistics on the difference
 * between a resampling map and the ideal resampling between a source and
 * destination transform.  The diagnostic is designed to be injected into
 * the reampling operation using the Proxy pattern.  The diagnostic
 * catches calls made to a {@link ResamplingMapFactory} and stores information
 * about the resampling for later analysis.  To perform a diagnostic of the
 * resampling, create a new diagnostic object using the resampling map factory
 * that is going to be used in an actual resampling operation, and then pass
 * the diagnostic object to the resampling instead of the actual factory.
 * The diagnositc will accumulate data about the resampling.  Then call
 * {@link #complete} to finish the diagnostic.  The diagnostic results are
 * obtained from {@link #getDistErrorStats} and {@link getOmegaStats}.
 *
 * @see ResamplingMap
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class ResamplingDiagnostic implements ResamplingMapFactory {

  private static final Logger LOGGER = Logger.getLogger (ResamplingDiagnostic.class.getName());

  // Variables
  // ---------

  /** The source transform for resampling. */
  private EarthTransform sourceTrans;
  
  /** The implementation to use for source locations. */
  private ResamplingSourceImp sourceImp;

  /** The destination transform for resampling. */
  private EarthTransform destTrans;

  /** The list of diagnostic info values. */
  private List<DiagnosticInfo> diagnosticInfoList;

  /** The resampling factory that we are being a proxy for. */
  private ResamplingMapFactory mapFactory;

  /** The diagnostic sampling factor in the range [0..1]. */
  private double factor;

  /** The summary statistics for the distance. */
  private DoubleSummaryStatistics distStats;

  /** The summary statistics for the distance error. */
  private DoubleSummaryStatistics distErrorStats;

  /** The summary statistics for the omega values. */
  private DoubleSummaryStatistics omegaStats;

  /** The flag to perform a datum shift between source and destination. */
  private boolean isDatumShiftNeeded;

  ////////////////////////////////////////////////////////////

  /**
   * Holds an individual diagnostic information value. Each value represents the data
   * for a single remapped location from the source to destination transform.
   * The data is generated from an instance of a remapping, and used in
   * generating statistics.
   */
  public static class DiagnosticInfo {

    /** The destination coordinates. */
    public int[] destCoords;
    
    /** The corresponding source coordinates. */
    public int[] sourceCoords;

    /** The center earth location of the destination pixel. */
    public EarthLocation destEarthLoc;

    /**
     * The distance in kilometers from center of destination pixel to
     * center of optimal source pixel.  The optimal source pixel is the
     * one that has the least distance possible of all pixels in the
     * source transform.
     */
    public double optimalDist;

    /** The optimal distance source coordinates. */
    public int[] optimalSourceCoords;

    /**
     * The actual distance in kilometers from the center of the destination
     * pixel to center of source pixel, as was specified by the remapping.
     */
    public double actualDist;

    /**
     * Determines if the pixel chosen is the optimal pixel based on the source
     * and optimal source coordinates.
     *
     * @return true if the pixel is the optimal, or false if not.
     */
    public boolean isOptimal() {
    
      return (
        sourceCoords[ROW] == optimalSourceCoords[ROW] &&
        sourceCoords[COL] == optimalSourceCoords[COL]
      );
      
    } // isOptimal

    public double getDistance() { return (actualDist); }

    public double getDistanceError() { return (actualDist - optimalDist); }

    /**
     * Gets the omega normalized error index, defined as 1 - (dist-opt)/(dist+opt)
     * where dist is the remapped pixel distance and opt is the optimal pixel
     * distance.  Omega is a normalized error, whose value is 1 when the pixel
     * chosen for the remapping is the optimal pixel, or &lt; 1 otherwise.  If
     * both the distance and optimal distance are zero, omega is 1.
     *
     * @return the omega value.
     */
    public double getOmega() {

      double omega;
      if (actualDist == optimalDist)
        omega = 1;
      else
        omega = 1 - (actualDist - optimalDist) / (actualDist + optimalDist);

      return (omega);
      
    } // getOmega

  } // DiagnosticInfo class

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new diagnostic object.
   *
   * @param sourceTrans the source transform for resampling.
   * @param sourceImp the source-specific object to use for resampling queries.
   * @param destTrans the destination transform for resampling.
   * @param mapFactory the map factory being used to create resampling maps.
   * @param factor the diagnostic sampling factor in the range (0..1].  The
   * pixel remapping will be sampled according to the factor where 0 is none
   * of the pixels, and 1 is all of the pixels.  A factor of 0.01 will sample
   * 1% of the pixels, which is recommended.
   */
  public ResamplingDiagnostic (
    EarthTransform sourceTrans,
    ResamplingSourceImp sourceImp,
    EarthTransform destTrans,
    ResamplingMapFactory mapFactory,
    double factor
  ) {
  
    diagnosticInfoList = new ArrayList<>();
    this.sourceTrans = sourceTrans;
    this.sourceImp = sourceImp;
    this.destTrans = destTrans;
    this.mapFactory = mapFactory;
    this.factor = factor;
    this.isDatumShiftNeeded = !sourceTrans.getDatum().equals (destTrans.getDatum());

  } // ResamplingDiagnostic constructor

  ////////////////////////////////////////////////////////////

  @Override
  public ResamplingMap create (
    int[] start,
    int[] length
  ) {

    ResamplingMap map = mapFactory.create (start, length);
    if (map != null) {

      int[] destCoords = new int[2];
      int[] sourceCoords = new int[2];

      // We want:
      // width/N x height/N = width x height x factor
      // ==> 1/N^2 = factor
      // N = sqrt (1/factor)
      if (factor == 0 || factor > 1)
        throw new RuntimeException ("Invalid diagnostic sampling factor " + factor);
      int stride = (int) Math.sqrt (1/factor);

      int samples = 0;
      
      for (int i = 0; i < length[ROW]; i += stride) {
        destCoords[ROW] = i + start[ROW];
        for (int j = 0; j < length[COL]; j += stride) {

          // Map dest to source
          // ------------------
          destCoords[COL] = j + start[COL];
          boolean isValid = map.map (destCoords, sourceCoords);
          if (isValid) {

            // Create new diagnostic info
            // --------------------------
            DiagnosticInfo info = new DiagnosticInfo();
            info.destCoords = (int[]) destCoords.clone();
            info.sourceCoords = (int[]) sourceCoords.clone();
            synchronized (diagnosticInfoList) { diagnosticInfoList.add (info); }
            samples++;

          } // isValid
      
        } // for
      } // for

      LOGGER.fine ("Accumulated " + samples + " diagnostic samples at start = " + Arrays.toString (start));

    } // if
    
    return (map);

  } // create

  ////////////////////////////////////////////////////////////

  /**
   * Completes the diagnostic by finding the optimal resampled pixels in
   * the source transform and generating statistics on the actual versus
   * optimal resampling.
   */
  public void complete () {
  
    LOGGER.fine ("Running diagnostic on " + diagnosticInfoList.size() + " samples");

    // Fill in remaining diagnostic sample values
    // ------------------------------------------
    DataLocation sourceLoc = new DataLocation (2);
    DataLocation destLoc = new DataLocation (2);
    EarthLocation sourceEarthLoc = new EarthLocation();
    Datum sourceDatum = sourceTrans.getDatum();
    diagnosticInfoList.forEach (info -> {
      destLoc.setCoords (info.destCoords);
      info.destEarthLoc = destTrans.transform (destLoc);
      if (isDatumShiftNeeded) info.destEarthLoc.shiftDatum (sourceDatum);
      sourceLoc.setCoords (info.sourceCoords);
      sourceTrans.transform (sourceLoc, sourceEarthLoc);
      info.actualDist = info.destEarthLoc.distance (sourceEarthLoc);
      info.optimalDist = Double.MAX_VALUE;
      info.optimalSourceCoords = new int[2];
    });

    // Loop over all samples
    // ---------------------
    int[] dims = sourceTrans.getDimensions();
    int[] sourceCoords = new int[2];

    int window = sourceImp.getWindowSize();
    int radius = (window-1)/2;
    int[] windowStart = new int[2];
    int[] windowEnd = new int[2];

    int infoIndex = 0;
    int infoCount = diagnosticInfoList.size();

    for (DiagnosticInfo info : diagnosticInfoList) {

      if ((infoIndex+1)%(infoCount/4) == 0)
        LOGGER.fine ("Diagnostic " + ((infoIndex+1)/(infoCount/4) * 25) + "% complete");

      // Set the window to search
      // ------------------------
      for (int k = 0; k < 2; k++) {
        windowStart[k] = info.sourceCoords[k] - radius;
        if (windowStart[k] < 0) windowStart[k] = 0;
        windowEnd[k] = info.sourceCoords[k] + radius;
        if (windowEnd[k] > dims[k]-1) windowEnd[k] = dims[k]-1;
      } // for

      // Search within window for closest location
      // -----------------------------------------
      for (sourceCoords[ROW] = windowStart[ROW]; sourceCoords[ROW] <= windowEnd[ROW]; sourceCoords[ROW]++) {
        for (sourceCoords[COL] = windowStart[COL]; sourceCoords[COL] <= windowEnd[COL]; sourceCoords[COL]++) {
          
          sourceLoc.setCoords (sourceCoords);
          if (sourceImp.isValidLocation (sourceLoc)) {

            sourceTrans.transform (sourceLoc, sourceEarthLoc);
            if (sourceEarthLoc.isValid()) {
              double dist = info.destEarthLoc.distanceProxy (sourceEarthLoc);
              if (dist < info.optimalDist) {
                info.optimalDist = dist;
                info.optimalSourceCoords[ROW] = sourceCoords[ROW];
                info.optimalSourceCoords[COL] = sourceCoords[COL];
              } // if
            } // if

          } // if

        } // for
      } // for

      infoIndex++;

    } // for

    // Filter out any diagnostic points with invalid data
    // --------------------------------------------------
    diagnosticInfoList.removeIf (info -> info.optimalDist == Double.MAX_VALUE ||
      Double.isNaN (info.actualDist));

    // Complete computation of optimal distances and check
    // ---------------------------------------------------
    boolean isNegativeError = false;
    for (DiagnosticInfo info : diagnosticInfoList) {
      info.optimalDist = EarthLocation.distanceProxyToDistance (info.optimalDist);
      if (info.optimalDist > info.actualDist) {
        LOGGER.warning ("Optimal dist > actual resampled dist at dest coords " +
          Arrays.toString (info.destCoords));
      } // if
    } // for

    // Compute statistics
    // ------------------
    distStats = diagnosticInfoList.parallelStream()
      .collect (Collectors.summarizingDouble (DiagnosticInfo::getDistance));
    distErrorStats = diagnosticInfoList.parallelStream()
      .collect (Collectors.summarizingDouble (DiagnosticInfo::getDistanceError));
    omegaStats = diagnosticInfoList.parallelStream()
      .collect (Collectors.summarizingDouble (DiagnosticInfo::getOmega));

  } // complete
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the statistics for the distance in kilometers from the center of the
   * destination pixel to the center of mapped source pixel.
   *
   * @return the statistics object.
   */
  public DoubleSummaryStatistics getDistStats() { return (distStats); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the statistics for (dist - opt) over all diagnostic samples,
   * where dist is the distance in kilometers from the center of the
   * destination pixel to the center of mapped source pixel, and opt is the
   * distance in kilometers from the center of the destination pixel to the
   * center of the optimal source pixel -- ie: the one that has the least
   * distance possible to the destination pixel of all pixels in the
   * source transform.
   *
   * @return the statistics object.
   */
  public DoubleSummaryStatistics getDistErrorStats() { return (distErrorStats); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the statistics for the omega normalized error index over all diagnostic
   * samples. Omega is defined as 1 - (dist-opt)/(dist+opt) where dist and opt
   * are as defined in {@link getDistErrorStats}.  Omega is a normalized error,
   * whose value is 1 when the pixel chosen for the remapping is the optimal
   * pixel, or &lt; 1 otherwise.  If both the distance and optimal distance are
   * zero, omega is 1.
   *
   * @return the statistics object.
   */
  public DoubleSummaryStatistics getOmegaStats() { return (omegaStats); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the diagnostic sample count, that is the number of remapped pixels
   * used to generate statistics.
   *
   * @return the sample count.
   */
  public int getSampleCount() { return (diagnosticInfoList.size()); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the suboptimal pixel count, that is the number of remapped pixels
   * that did not match their optimal pixel.
   *
   * @return the suboptimal pixel count.
   */
  public int getSuboptimalCount() {

    int count = 0;
    for (DiagnosticInfo info : diagnosticInfoList) {
      if (!info.isOptimal()) count++;
    } // for

    return (count);
    
  } // getSuboptimalCount

  ////////////////////////////////////////////////////////////

  /**
   * Gets a list of the suboptimal diagnositic info objects.  These are the
   * diagnostic samples for which the actual mapped pixel did not match
   * the optimal pixel from the source transform.
   *
   * @return the list of suboptimal diagnositic info objects.
   *
   * @see #getSuboptimalCount
   */
  public List<DiagnosticInfo> getSuboptimalDiagnosticList() {

    List<DiagnosticInfo> suboptimalDiagnosticInfoList = new ArrayList<>();
    for (DiagnosticInfo info : diagnosticInfoList) {
      if (!info.isOptimal()) suboptimalDiagnosticInfoList.add (info);
    } // for

    return (suboptimalDiagnosticInfoList);
    
  } // getSuboptimalDiagnosticList

  ////////////////////////////////////////////////////////////

} // ResamplingDiagnostic class

////////////////////////////////////////////////////////////////////////

