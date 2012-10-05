////////////////////////////////////////////////////////////////////////
/*
     FILE: UnitFactory.java
  PURPOSE: Creates unit objects.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/06
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// --------
import ucar.units.*;

/**
 * The <code>UnitFactory</code> class create Unidata UDUNITS style
 * <code>Unit</code> objects from unit specification strings.  This is
 * simply a convenience factory that uses the <code>ucar.units</code>
 * package.  A number of aliases are also added to the parser to
 * handle legacy CoastWatch unit specifications.
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
