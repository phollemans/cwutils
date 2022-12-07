////////////////////////////////////////////////////////////////////////
/*

     File: FloatScalingScheme.java
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
 * The <code>FloatScalingScheme</code> class implements a scale and offset
 * for scaling float data.  Float values are scaled as
 * <code>scaled = (raw - offset)*scale</code>.
 *
 * @author Peter Hollemans
 * @since 3.6.1
 */
@noaa.coastwatch.test.Testable
public class FloatScalingScheme implements ScalingScheme {

  /** The scaling factor for float data. */
  public float scale;

  /** The offset for float data. */
  public float offset;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new scaling instance.
   *
   * @param scale the scaling factor.
   * @param offset the offset value.
   */
  public FloatScalingScheme (float scale, float offset) {
  
    this.scale = scale;
    this.offset = offset;
  
  } // FloatScalingScheme

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (ScalingSchemeVisitor visitor) {

    visitor.visitFloatScalingScheme (this);

  } // accept
  
  ////////////////////////////////////////////////////////////

  /**
   * Scales raw float data.
   *
   * @param rawData the raw unscaled array to read.
   * @param scaledData the scaled array to write.
   */
  public void scaleFloatData (float[] rawData, float[] scaledData) {

    for (int i = 0; i < rawData.length; i++) {
      scaledData[i] = scaleFloat (rawData[i]);
    } // for

  } // scaleFloatData

  ////////////////////////////////////////////////////////////

  /**
   * Unscales float data.
   *
   * @param scaledData the scaled array to read.
   * @param rawData the unscaled array to write.
   */
  public void unscaleFloatData (float[] scaledData, float[] rawData) {

    for (int i = 0; i < scaledData.length; i++) {
      rawData[i] = unscaleFloat (scaledData[i]);
    } // for

  } // unscaleFloatData

  ////////////////////////////////////////////////////////////

  /**
   * Scales a raw float value.
   *
   * @param value the raw value to scale.
   *
   * @return the scaled value.
   */
  public float scaleFloat (float value) {

    float scaled = Float.isNaN (value) ? Float.NaN : (value - offset)*scale;
    return (scaled);
  
  } // scaleFloat

  ////////////////////////////////////////////////////////////

  /**
   * Unscales a float value.
   *
   * @param value the value to unscale.
   *
   * @return the raw unscaled value.
   */
  public float unscaleFloat (float value) {

    float unscaled = Float.isNaN (value) ? Float.NaN : (value/scale + offset);
    return (unscaled);

  } // unscaleFloat

  /////////////////////////////////////////////////////////////////

  @Override
  public boolean equals (Object obj) {

    boolean equal = false;
    if (obj instanceof FloatScalingScheme) {
      var scheme = (FloatScalingScheme) obj;
      equal = (this.scale == scheme.scale && this.offset == scheme.offset);
    } // if

    return (equal);

  } // equals

  /////////////////////////////////////////////////////////////////

  @Override
  public int hashCode() { return (Float.hashCode (scale)*1009 ^ Float.hashCode (offset)*1013); }

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (FloatScalingScheme.class);

    FloatScalingScheme scheme = new FloatScalingScheme (0.01f, 3000.0f);
    float scaledValue = 10.0f;
    float rawValue = 4000.0f;

    logger.test ("scaleFloat");
    assert (scheme.scaleFloat (rawValue) == scaledValue);
    logger.passed();

    logger.test ("unscaleFloat");
    assert (scheme.unscaleFloat (scaledValue) == rawValue);
    logger.passed();

    var otherScheme = new FloatScalingScheme (0.02f, 3000.0f);
    var sameScheme = new FloatScalingScheme (0.01f, 3000.0f);
    logger.test ("equals");
    assert (sameScheme.equals (scheme));
    assert (!otherScheme.equals (scheme));
    logger.passed();

    logger.test ("hashCode");
    assert (scheme.hashCode() == sameScheme.hashCode());
    logger.passed();

  } // main
  
  ////////////////////////////////////////////////////////////

} // FloatScalingScheme class

////////////////////////////////////////////////////////////////////////



