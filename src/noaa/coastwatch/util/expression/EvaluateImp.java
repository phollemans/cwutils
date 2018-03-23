////////////////////////////////////////////////////////////////////////
/*

     File: EvaluateImp.java
   Author: Peter Hollemans
     Date: 2017/11/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.expression;

/**
 * The <code>EvaluateImp</code> class defines an interface for all
 * classes that help evaluate mathematical expressions by returning the
 * variable values needed to perform the expression computation.  The values are
 * retrieved using the index corresponding to the variable, set up in the
 * expression parsing phase using a {@link ParseImp} object.  Methods in this
 * class take the form of <code>getXXXProperty(int)</code> where
 * <code>XXX</code> is the type specified by the
 * {@link ParseImp#typeOfVariable(String)} method, either Byte, Short, Integer,
 * Long, Float, or Double.
 *
 * @see ExpressionParser#init
 * @see ExpressionParser#parse
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface EvaluateImp {

  default public byte getByteProperty (int varIndex) { throw new UnsupportedOperationException(); }
  default public short getShortProperty (int varIndex) { throw new UnsupportedOperationException(); }
  default public int getIntegerProperty (int varIndex) { throw new UnsupportedOperationException(); }
  default public long getLongProperty (int varIndex) { throw new UnsupportedOperationException(); }
  default public float getFloatProperty (int varIndex) { throw new UnsupportedOperationException(); }
  default public double getDoubleProperty (int varIndex) { throw new UnsupportedOperationException(); }

} // EvaluateImp interface

////////////////////////////////////////////////////////////////////////
