////////////////////////////////////////////////////////////////////////
/*

     File: Testable.java
   Author: Peter Hollemans
     Date: 2014/03/20

  CoastWatch Software Library and Utilities
  Copyright (c) 2014 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.test;

// Imports
// -------
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
