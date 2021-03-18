////////////////////////////////////////////////////////////////////////
/*

     File: DoubleScalingScheme.java
   Author: Peter Hollemans
     Date: 2021/03/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2021 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.chunk;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The <code>DoubleScalingScheme</code> class implements a scale and offset
 * for scaling double data.  Double values are scaled as
 * <code>scaled = (raw - offset)*scale</code>.
 *
 * @author Peter Hollemans
 * @since 3.6.1
 */
@noaa.coastwatch.test.Testable
public class DoubleScalingScheme implements ScalingScheme {

  /** The scaling factor for double data. */
  public double scale;

  /** The offset for double data. */
  public double offset;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new scaling instance.
   *
   * @param scale the scaling factor.
   * @param offset the offset value.
   */
  public DoubleScalingScheme (double scale, double offset) {
  
    this.scale = scale;
    this.offset = offset;
  
  } // DoubleScalingScheme

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (ScalingSchemeVisitor visitor) {

    visitor.visitDoubleScalingScheme (this);

  } // accept
  
  ////////////////////////////////////////////////////////////

  /**
   * Scales raw double data.
   *
   * @param rawData the raw unscaled array to read.
   * @param scaledData the scaled array to write.
   */
  public void scaleDoubleData (double[] rawData, double[] scaledData) {

    for (int i = 0; i < rawData.length; i++) {
      scaledData[i] = scaleDouble (rawData[i]);
    } // for

  } // scaleDoubleData

  ////////////////////////////////////////////////////////////

  /**
   * Unscales double data.
   *
   * @param scaledData the scaled array to read.
   * @param rawData the raw unscaled array to write.
   */
  public void unscaleDoubleData (double[] scaledData, double[] rawData) {

    for (int i = 0; i < scaledData.length; i++) {
      rawData[i] = unscaleDouble (scaledData[i]);
    } // for

  } // unscaleDoubleData

  ////////////////////////////////////////////////////////////

  /**
   * Scales a raw double value.
   *
   * @param value the raw value to scale.
   *
   * @return the scaled value.
   */
  public double scaleDouble (double value) {

    double scaled = Double.isNaN (value) ? Double.NaN : (value - offset)*scale;
    return (scaled);
  
  } // scaleDouble

  ////////////////////////////////////////////////////////////

  /**
   * Unscales a double value.
   *
   * @param value the value to unscale.
   *
   * @return the unscaled value.
   */
  public double unscaleDouble (double value) {

    double unscaled = Double.isNaN (value) ? Double.NaN : (value/scale + offset);
    return (unscaled);

  } // unscaleDouble

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (DoubleScalingScheme.class);

    DoubleScalingScheme scheme = new DoubleScalingScheme (0.01, 3000.0);
    double scaledValue = 10.0;
    double rawValue = 4000.0;

    logger.test ("scaleDouble");
    assert (scheme.scaleDouble (rawValue) == scaledValue);
    logger.passed();

    logger.test ("unscaleDouble");
    assert (scheme.unscaleDouble (scaledValue) == rawValue);
    logger.passed();

  } // main
  
  ////////////////////////////////////////////////////////////

} // DoubleScalingScheme class

////////////////////////////////////////////////////////////////////////




