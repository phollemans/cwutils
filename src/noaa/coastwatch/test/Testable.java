////////////////////////////////////////////////////////////////////////
/*
     FILE: Testable.java
  PURPOSE: Marker annotation for testable classes.
   AUTHOR: Peter Hollemans
     DATE: 2014/03/20
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.test;

// Imports
// -------
import java.lang.annotation.*;

////////////////////////////////////////////////////////////////////////

/**
 * The Testable annotation can be used by any class that has a main
 * method that should be called to test the class methods.  The main
 * method should throw an error if the class fails testing.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Testable { }

////////////////////////////////////////////////////////////////////////
