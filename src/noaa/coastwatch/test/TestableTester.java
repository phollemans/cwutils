////////////////////////////////////////////////////////////////////////
/*
     FILE: TestableTester.java
  PURPOSE: Runs tests on testable classes.
   AUTHOR: Peter Hollemans
     DATE: 2014/03/23
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
import java.lang.reflect.*;
import java.util.*;
import org.reflections.*;

////////////////////////////////////////////////////////////////////////

/**
 * The TestableTester class contains a single static method main() that 
 * runs the unit tests for all classes that declare the Testable annotation.
 * Classes need not register in any way for testing, rather the Testable
 * annotation is detected via reflection.
 *
 * @see Testable
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class TestableTester {

  ////////////////////////////////////////////////////////////

  /** Runs the main method. */
  public static void main (String[] argv) throws Exception {

    // Check assertions
    // ----------------
    boolean assertionsEnabled = false;
    assert (assertionsEnabled = true);
    if (!assertionsEnabled) throw new RuntimeException ("Assertions not enabled");
    
    // Get all testable classes
    // ------------------------
    Reflections reflections = new Reflections ("noaa.coastwatch");
    Set<Class<?>> testableClassSet =
      reflections.getTypesAnnotatedWith (noaa.coastwatch.test.Testable.class);

    // Run tests
    // ---------
    Class[] parameterArray = new Class[1];
    parameterArray[0] = String[].class;
    for (Class testableClass : testableClassSet) {
      System.out.println ("***** " + testableClass + " *****");
      Method mainMethod = testableClass.getMethod ("main", parameterArray);
      try { mainMethod.invoke (null, (Object) null); }
      catch (InvocationTargetException e) {
        System.out.println();
        e.getCause().printStackTrace (System.out);
      } // catch
      System.out.println();
    } // for

  } // main

  ////////////////////////////////////////////////////////////

} // TestableTester class

////////////////////////////////////////////////////////////////////////
