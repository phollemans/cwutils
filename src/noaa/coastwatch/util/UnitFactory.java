////////////////////////////////////////////////////////////////////////
/*

     File: UnitFactory.java
   Author: Peter Hollemans
     Date: 2005/06/06

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
package noaa.coastwatch.util;

// Imports
// --------
import ucar.units.Unit;
import ucar.units.UnitDB;
import ucar.units.UnitDBManager;
import ucar.units.UnitFormat;
import ucar.units.UnitFormatManager;
import ucar.units.UnitName;
import ucar.units.UnknownUnit;

/**
 * The <code>UnitFactory</code> class creates Unidata UDUNITS style
 * <code>Unit</code> objects from unit specification strings.  This is
 * simply a convenience factory that uses the <code>ucar.units</code>
 * package.  A number of aliases are also added to the parser to
 * handle legacy CoastWatch unit specifications.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public class UnitFactory {

  // Variables
  // ---------

  /** The units formatter for parsing units strings. */
  private static UnitFormat format;

  ////////////////////////////////////////////////////////////

  static {

    try {

      // Get units formatter
      // -------------------
      format = UnitFormatManager.instance();

      // Add aliases
      // -----------
      UnitDB unitDB = UnitDBManager.instance();
      String[][] aliases = new String[][] {
        {"celsius", "temp_deg_c"},
        {"percent", "albedo*100%"}
      };
      for (int i = 0; i < aliases.length; i++) {
        Unit unit = format.parse (aliases[i][0]);
        Unit alias = unit.clone (UnitName.newUnitName (aliases[i][1]));
        unitDB.addUnit (alias);
      } // for

    } catch (Exception e) {
      throw new RuntimeException (e);
    } // catch

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * Creates a unit based on a unit specifier string.
   * 
   * @param unitSpec the unit specifier string.
   *
   * @return the new unit.
   *
   * @throws IllegalArgumentException if the unit specifier does not
   * resolve to a valid unit.
   */
  public static Unit create (
    String unitSpec
  ) {

    Unit unit;
    try { unit = format.parse (unitSpec); }
    catch (Exception e) { unit = null; }
    if (unit == null || unit instanceof UnknownUnit) {
      throw new IllegalArgumentException ("Unknown unit specifier '" +
        unitSpec + "'");
    } // if
    return (unit);

  } // create

  ////////////////////////////////////////////////////////////

} // UnitFactory class

///////////////////////////////////////////////////////////////////////
