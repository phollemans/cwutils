////////////////////////////////////////////////////////////////////////
/*

     File: VariableStatisticsGenerator.java
   Author: Peter Hollemans
     Date: 2017/04/05

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
package noaa.coastwatch.util;

// Imports
// -------
import noaa.coastwatch.util.DataLocationConstraints;
import noaa.coastwatch.util.DataLocationIterator;
import noaa.coastwatch.util.DataLocationIteratorFactory;
import noaa.coastwatch.util.DataVariableIterator;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.Statistics;

/**
 * The <code>VariableStatisticsGenerator</code> class creates a 
 * {@link Statistics} object using a data variable and set of constraints.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class VariableStatisticsGenerator {

  // Variables
  // ---------

  /** The instance of this factory. */
  private static VariableStatisticsGenerator instance;

  ////////////////////////////////////////////////////////////

  /** Gets an instance of this factory. */
  public static VariableStatisticsGenerator getInstance () {

    if (instance == null) instance = new VariableStatisticsGenerator();
    return (instance);

  } // getInstance

  ////////////////////////////////////////////////////////////

  private VariableStatisticsGenerator () { }

  ////////////////////////////////////////////////////////////

  /**
   * Generates a statistics object for a variable given location
   * constraints.
   *
   * @param var the variable to get data from.
   * @param constraints the data location bounds and sparseness contraints.  If
   * the start and end bounds are not set, the contraint dims field is 
   * automatically populated from the variable dimensions.
   *
   * @return the data statistics computed over the locations in the variable,
   * subject to the location constraints.
   *
   * @throws IllegalArgumentException if inconsistencies are found in the
   * constraints.
   */
  public Statistics generate (
    DataVariable var,
    DataLocationConstraints constraints
  ) {

    if (constraints.start == null || constraints.end == null)
      constraints.dims = var.getDimensions();
    DataLocationIterator locationIter = DataLocationIteratorFactory.getInstance().create (constraints);
    DataVariableIterator varIter = new DataVariableIterator (var, locationIter);
    Statistics stats = new Statistics (varIter);

    return (stats);
    
  } // generate

  ////////////////////////////////////////////////////////////

} // VariableStatisticsGenerator class

////////////////////////////////////////////////////////////////////////
