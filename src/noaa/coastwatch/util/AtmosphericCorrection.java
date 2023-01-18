/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2021 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.util;

/**
 * <p>The <code>AtmosphericCorrection</code> class performs a simplified
 * atmospheric correction algorithm to transform MODIS top-of-atmosphere
 * reflectance data to corrected reflectance data.  The correction accounts
 * for molecular (Rayleigh) scattering and gaseous absorption (water vapor,
 * ozone) but performs no aerosol correction.  No real-time input or ancillary
 * data is required.  See <i>Descloitres et al, The MODIS Rapid Response Project:
 * Near-Real-Time Processing for Fire Monitoring and Other Applications, MODIS
 * Science Team Meeting, Jul 7, 2004</i> for some details and example imagery.</p>
 *
 * <p>This routine was originally written by Jacques Descloitres for use with
 * the MODIS Rapid Response Project, NASA/GSFC/SSAI (see
 * http://rapidfire.sci.gsfc.nasa.gov).  It was converted from the C source
 * file tc_corr.c to Java and documented, but the algorithm remains the
 * same.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.0
 */
public class AtmosphericCorrection {

//  Description:
//
//    Simplified atmospheric correction algorithm that transforms MODIS
//    top-of-the-atmosphere level-1B radiance data into corrected reflectance
//    for Rapid Response applications.
//    Required ancillary data: coarse resolution DEM tbase.hdf
//
//  References and Credits:
//
//    Jacques Descloitres, MODIS Rapid Response Project, NASA/GSFC/SSAI
//    http://rapidfire.sci.gsfc.nasa.gov
//
//  Revision history:
//
//    Version 1.0   08/24/01
//    Version 1.1   01/25/02
//    Version 1.2   05/30/02
//    Version 1.3   09/06/02
//    Version 1.4   09/02/03
//    Version 1.4.1 01/22/04
//    Version 1.5   02/17/04
//    Version 1.5.1 03/08/07 (not run in RR production)
//    Version 1.5.2 06/07/07
//    Version 1.6   08/18/09 (Be sure to update PROCESS_VERSION_NUMBER also)
//                           Started with Version 1.5.2, version run in Rapid Response for several years,
//                           this had several differences from 1.4.2, the DRL version, including the addition
//                           of band 8 and a number of small, but perhaps important, computational changes
//                           1) removed most code within #ifdef DEBUG clauses
//                           2) left command line options for nearest, TOA, and sealevel; note that these options
//                              were in Version 1.4.2 but were not available from the command line
//                           3) changes by DRL to 1.4.2 to write scale factor and offset has already been
//                              incorporated into 1.5.2
//                           4) Added in the modifications from Chuanmin Hu & Brock Murch of Univ South Florida
//                              IMaRS to add bands 9-16.  The aO3 and taur0 parameters came "from SeaDAS codes"
//                              and "The H2O parameters can be assumed 0 for bands 8-16 because those bands were
//                              designed to avoid water vapor absorption."
//                           Disclaimer: the nearest, TOA, and sealevel options and bands 9-16 are not used
//                                       by Rapid Response so I cannot consider them tested/validated - JES
    
    
  private static final int NBANDS = 16;
  private static final double UO3 = 0.285;
  private static final double UH2O = 2.93;
  private static final double MAXAIRMASS = 18;
  private static final double SCALEHEIGHT = 8000;
  private static final double TAUSTEP4SPHALB = 0.0001;
  private static final int MAXNUMSPHALBVALUES = 4000;  // with no aerosol taur <= 0.4 in all bands everywhere

  private static final double[] AS0 = new double[] {
    0.33243832, 0.16285370, -0.30924818, -0.10324388, 0.11493334,
    -6.777104e-02, 1.577425e-03, -1.240906e-02, 3.241678e-02, -3.503695e-02
  };
  private static final double[] AS1 = new double[] {0.19666292, -5.439061e-02};
  private static final double[] AS2 = new double[] {0.14545937,-2.910845e-02};

  // Values for bands 9-16 below provided by B Murch and C Hu Univ South Florida
  // IMaRS, obtained from SEADAS.
  // For the moment I've retained the Jacques values for 1-8 but show the differing
  // SEADAS values in the commented out line.

  private static final double[] AH2O = new double[] {
    0.000406601, 0.0015933, 0, 1.78644e-05, 0.00296457, 0.000617252, 0.000996563, 0.00222253,
    0.00094005, 0.000563288, 0, 0, 0, 0, 0, 0
  };

  private static final double[] BH2O = new double[] {
    0.812659, 0.832931, 1, 0.8677850, 0.806816 , 0.944958, 0.78812, 0.791204,
    0.900564, 0.942907, 0, 0, 0, 0, 0, 0
  };

  /*const double aO3[NBANDS]={ 0.0711,    0.00313, 0.0104,     0.0930,   0, 0, 0, 0.00244, 0.00383, 0.0225, 0.0663, 0.0836, 0.0485, 0.0395, 0.0119, 0.00263};*/
  private static final double[] AO3 = new double[] {
    0.0433461, 0.0, 0.0178299, 0.0853012, 0, 0, 0, 0.0813531,
    0, 0, 0.0663, 0.0836, 0.0485, 0.0395, 0.0119, 0.00263
  };

  /*const double taur0[NBANDS] = { 0.0507,  0.0164,  0.1915,  0.0948,  0.0036,  0.0012,  0.0004,  0.3109, 0.2375, 0.1596, 0.1131, 0.0994, 0.0446, 0.0416, 0.0286, 0.0155};*/
  private static final double[] TAUR0 = new double[] {
    0.04350, 0.01582, 0.16176, 0.09740, 0.00369, 0.00132, 0.00033, 0.05373,
    0.01561 ,0.00129, 0.1131, 0.0994, 0.0446, 0.0416, 0.0286, 0.0155
  };

  private static final double[] SPHALB_A = new double[] {
    -0.57721566,  0.99999193, -0.24991055,
     0.05519968, -0.00976004,  0.00107857
  };

  private static double[] sphalb0;

  private static AtmosphericCorrection instance;

  ////////////////////////////////////////////////////////////

  static {

    sphalb0 = new double[MAXNUMSPHALBVALUES];
    sphalb0[0] = 0.0;
    for (int j = 1; j < MAXNUMSPHALBVALUES; j++)
      sphalb0[j] = csalbr (j * TAUSTEP4SPHALB);

  } // static

  ////////////////////////////////////////////////////////////

  public static AtmosphericCorrection getInstance() {
  
    if (instance == null) instance = new AtmosphericCorrection();
    return (instance);
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Corrects a set of MODIS band reflectance values for atmospheric effects.
   *
   * @param width the data width in pixels.
   * @param height the data height in pixels.
   * @param bandNumbers the array of MODIS band numbers for correction in the
   * range [1..16], or 0 if a band is included in the reflectance data but
   * should not be corrected.
   * @param bandReflect the MODIS band reflectance values as
   * [bands][width*height].  The bands should be in the same order as the
   * band numbers array.  Reflectance values are modified in-place, or set
   * to Float.NaN if no correction could be applied.
   * @param solarZenith the solar zenith angles in degrees.
   * @param solarAzimuth the solar azimuth angles in degrees, or null if the
   * relative azimuth is supplied.
   * @param satZenith the satellite zenith angles in degrees.
   * @param satAzimuth the satellite azimuth values in degrees, or null if
   * the relative azimuth is supplied.
   * @param relAzimuth the relative azimuth angle between solar and satellite
   * views, or null if the solar and satellite azimuth angles are supplied.
   * @param elev the elevation of the data in meters above sea level, or 0
   * for ocean data.
   */
  public void correct (
    int width,
    int height,
    int[] bandNumbers,
    float[][] bandReflect,
    float[] solarZenith,
    float[] solarAzimuth,
    float[] satZenith,
    float[] satAzimuth,
    float[] relAzimuth,
    float[] elev
  ) {
  
    int[] bandIndices = new int[bandNumbers.length];
    for (int i = 0; i < bandNumbers.length; i++)
      bandIndices[i] = bandNumbers[i] - 1;

    correct_reflectance (width, height, bandIndices.length, bandIndices, bandReflect,
      solarZenith, solarAzimuth, satZenith, satAzimuth, relAzimuth, elev);
  
  } // correct

  ////////////////////////////////////////////////////////////

  private AtmosphericCorrection () { }

  ////////////////////////////////////////////////////////////

  private void correct_reflectance (
    int nx,
    int ny,
    int nbands,
    int[] indband,
    float[][] reflectance,
    float[] solzen,
    float[] solazi,
    float[] senzen,
    float[] senazi,
    float[] relazi,
    float[] alt
  ) {

    float[] height = alt;

    double[] rhoray = new double[NBANDS];
    double[] sphalb = new double[NBANDS];
    double[] TtotraytH2O = new double[NBANDS];
    double[] tOG = new double[NBANDS];

    double[] temp_taur = new double[NBANDS];
    double[] temp_trup = new double[NBANDS];
    double[] temp_trdown = new double[NBANDS];

    boolean[] process = new boolean[NBANDS];
    for (int ib = 0; ib < NBANDS; ib++) process[ib] = false;
    for (int ib = 0; ib < nbands; ib++) {
      int iband = indband[ib];
      if ((iband < 0) || (iband >= NBANDS)) continue;
      process[iband] = true;
    } // for

    int count = nx*ny;
    if (relazi == null) {
      relazi = new float[count];
      for (int idx = 0; idx < count; idx++)
        relazi[idx] = solazi[idx] - senazi[idx];
    } // if

    for (int idx = 0; idx < count; idx++) {

      double mus = Math.cos (Math.toRadians (solzen[idx]));
      double muv = Math.cos (Math.toRadians (senzen[idx]));
      double phi = relazi[idx];

      getatmvariables (mus, muv, phi, height[idx], process,
        sphalb, rhoray, TtotraytH2O, tOG, temp_taur, temp_trup, temp_trdown);

      for (int iband = 0; iband < nbands; iband++) {
        int ib = indband[iband];
        if ((reflectance[iband][idx] >= 0) && (reflectance[iband][idx] < 2.0))
          reflectance[iband][idx] = (float) correctedrefl (reflectance[iband][idx], TtotraytH2O[ib], tOG[ib], rhoray[ib], sphalb[ib]);
        else
          reflectance[iband][idx] = Float.NaN;
      } // for
      
    } // for

  } // correct_reflectance

  ////////////////////////////////////////////////////////////

  private static double csalbr (
    double tau
  ) {

    return (3*tau - fintexp3 (tau) * (4 + 2*tau) + 2*Math.exp (-tau)) / (4 + 3*tau);

  } // csalbr

  ////////////////////////////////////////////////////////////

  private static double fintexp1 (
    double tau
  ) {
  
    double xx = SPHALB_A[0];
    double xftau = 1.0;
    for (int i = 1; i < 6; i++) {
      xftau *= tau;
      xx += SPHALB_A[i] * xftau;
    } // for

    return (xx - Math.log (tau));

  } // fintexp1

  ////////////////////////////////////////////////////////////

  private static double fintexp3 (
    double tau
  ) {

    return (Math.exp (-tau) * (1.0 - tau) + tau * tau * fintexp1 (tau)) / 2.0;

  } // fintexp3

  ////////////////////////////////////////////////////////////

  //  phi:     IN  azimuthal difference between sun and observation in degree
  //               (phi=0 in backscattering direction)
  //  mus:     IN  cosine of the sun zenith angle
  //  muv:     IN  cosine of the observation zenith angle
  //  taur:    IN  molecular optical depth
  //  rhoray: OUT  molecular path reflectance
  //  trup:   OUT
  //  trdown: OUT
  //  process: IN
  //
  //  constant xdep: depolarization factor (0.0279)
  //   xfd = (1-xdep/(2-xdep)) / (1 + 2*xdep/(2-xdep)) = 2 * (1 - xdep) / (2 + xdep) = 0.958725775

  private void chand (
    double phi,
    double muv,
    double mus,
    double[] taur,
    double[] rhoray,
    double[] trup,
    double[] trdown,
    boolean[] process
  ) {
    
    double xfd = 0.958725775;
    double xbeta2 = 0.5;

    double phios = phi + 180;
    double xcos1 = 1.0;
    double xcos2 = Math.cos (Math.toRadians (phios));
    double xcos3 = Math.cos (Math.toRadians (2*phios));
    double xph1 = 1 + (3*mus*mus - 1) * (3*muv*muv - 1) * xfd / 8;
    double xph2 = - xfd * xbeta2 * 1.5 * mus * muv * Math.sqrt (1 - mus*mus) * Math.sqrt (1 - muv*muv);
    double xph3 =   xfd * xbeta2 * 0.375 * (1 - mus*mus) * (1 - muv*muv);

    double[] pl = new double[5];
    pl[0] = 1.0;
    pl[1] = mus + muv;
    pl[2] = mus * muv;
    pl[3] = mus * mus + muv * muv;
    pl[4] = mus * mus * muv * muv;

    double fs01 = 0, fs02 = 0;
    for (int i = 0; i < 5; i++) {
      fs01 += (pl[i] * AS0[i]);
      fs02 += (pl[i] * AS0[5 + i]);
    } // for

    for (int ib = 0; ib < NBANDS; ib++) {
      if (process[ib]) {
        double xlntaur = Math.log (taur[ib]);
        double fs0 = fs01 + fs02 * xlntaur;
        double fs1 = AS1[0] + xlntaur * AS1[1];
        double fs2 = AS2[0] + xlntaur * AS2[1];
        trdown[ib] = Math.exp (-taur[ib]/mus);
        trup[ib] = Math.exp (-taur[ib]/muv);
        double xitm1 = (1 - trdown[ib] * trup[ib]) / 4 / (mus + muv);
        double xitm2 = (1 - trdown[ib]) * (1 - trup[ib]);
        double xitot1 = xph1 * (xitm1 + xitm2 * fs0);
        double xitot2 = xph2 * (xitm1 + xitm2 * fs1);
        double xitot3 = xph3 * (xitm1 + xitm2 * fs2);
        rhoray[ib] = xitot1 * xcos1 + xitot2 * xcos2 * 2 + xitot3 * xcos3 * 2;
      } // if
    } // for

  } // chand

  ////////////////////////////////////////////////////////////

  private int getatmvariables (
    double mus,
    double muv,
    double phi,
    double height,
    boolean[] process,
    double[] sphalb,
    double[] rhoray,
    double[] TtotraytH2O,
    double[] tOG,
    double[] temp_taur,
    double[] temp_trup,
    double[] temp_trdown
  ) {
   
    double[] taur = temp_taur;
    double[] trup = temp_trup;
    double[] trdown = temp_trdown;

    double m = 1.0 / mus + 1.0 / muv;
    if (m > MAXAIRMASS) return (-1);

    double psurfratio = Math.exp (-height / SCALEHEIGHT);
    for (int ib = 0; ib < NBANDS; ib++) {
      if (process[ib]) taur[ib] = TAUR0[ib] * psurfratio;
    } // for

    chand (phi, muv, mus, taur, rhoray, trup, trdown, process);

    for (int ib = 0; ib < NBANDS; ib++) {

      if (!process[ib]) continue;

      if (taur[ib] / TAUSTEP4SPHALB >= MAXNUMSPHALBVALUES) {
        sphalb[ib] = -1.0;
        /* Use sphalb as flag to indicate atm variables are not computed successfully */
        continue;
      } // if

      sphalb[ib] = sphalb0[(int)(taur[ib] / TAUSTEP4SPHALB + 0.5)];
      double Ttotrayu = ((2 / 3.0 + muv) + (2 / 3.0 - muv) * trup[ib]) / (4 / 3.0 + taur[ib]);
      double Ttotrayd = ((2 / 3.0 + mus) + (2 / 3.0 - mus) * trdown[ib]) / (4 / 3.0 + taur[ib]);
      double tO3 = 1, tO2 = 1, tH2O = 1;
      if (AO3[ib] != 0) tO3 = Math.exp (-m * UO3 * AO3[ib]);
      if (BH2O[ib] != 0) tH2O = Math.exp (-(AH2O[ib]*(Math.pow((m * UH2O), BH2O[ib]))));
      ////////////////////////////////////////////////////
      //      t02 = exp(-m * aO2);
      ////////////////////////////////////////////////////
      TtotraytH2O[ib] = Ttotrayu * Ttotrayd * tH2O;
      tOG[ib] = tO3 * tO2;

    } // for

    return (0);

  } // getatmvariables

  ////////////////////////////////////////////////////////////

  private double correctedrefl (
    double refl,
    double TtotraytH2O,
    double tOG,
    double rhoray,
    double sphalb
  ) {

      double corr_refl = (refl / tOG - rhoray) / TtotraytH2O;
      corr_refl /= (1.0 + corr_refl * sphalb);
      return (corr_refl);

  } // correctedrefl
  
  ////////////////////////////////////////////////////////////

} // AtmosphericCorrection class
