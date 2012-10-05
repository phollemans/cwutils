////////////////////////////////////////////////////////////////////////
/*
     FILE: OpendapURLFilter.java
  PURPOSE: Identifies and filter OPeNDAP URLs.
   AUTHOR: Peter Hollemans
     DATE: 2005/06/30
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

/**
 * The <code>OpendapURLFilter</code> class filters OPeNDAP URL
 * strings.  The filter rejects any URLs that are not considered to be
 * OPeNDAP-specific, and modifies OPeNDAP URLs that end with a
 * recognized OPeNDAP service extension.
 */
public class OpendapURLFilter implements StringFilter {

  /** Filters the input string. */
  public String filter (String input) {

    // Eliminate non-OPeNDAP URLs
    // --------------------------
    if (input == null) return (null);


//    if (input.toLowerCase().indexOf ("nph-") == -1) return (null);



    // TODO: The above filter doesn't work if the web master has
    // done a tricky script alias so that "nph-" never appears as
    // part of an OPeNDAP URL.

    // Remove known extensions
    // -----------------------
    String output = input.replaceFirst (
      "^(.*)\\.(das|dds|dods|asc|ascii|html|info|ver)$", "$1");

    return (output);

  } // filter

} // OpendapURLFilter class

////////////////////////////////////////////////////////////////////////
