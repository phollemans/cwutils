////////////////////////////////////////////////////////////////////////
/*
     FILE: TestableTester.java
  PURPOSE: Runs tests on testable classes.
   AUTHOR: Peter Hollemans
     DATE: 2014/03/23
  CHANGES: 2015/05/07, PFH
           - Changes: Added class sorting by name.
           - Issues: The order that classes were being tested was random,
             so it was difficult to locate a test reliably from one run to
             another, so we added a sort of class names before the testing
             to resolve this.
           2016/01/19, PFH
           - Changes: Updated to new TestLogger class.
           - Issues: With more classes being unit tested, it was difficult to
             see the pass/fail status of each test.  The singleton class
             TestLogger now handles unit test output, and prints messages 
             in color using a standard message layout.
 
  CoastWatch Software Library and Utilities
  Copyright 2014-2016, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.test;

// Imports
// -------
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import noaa.coastwatch.test.Testable;
import noaa.coastwatch.test.TestLogger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;

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
  
    // Sort classes by name
    // --------------------
    /**
     * We do this step because we want to guarantee a consistent order
     * for the unit tests so that they run in groups by package.
     */
    TreeMap<String,Class<?>> testableClassMap = new TreeMap<String, Class<?>>();
    for (Class testableClass : testableClassSet)
      testableClassMap.put (testableClass.getName(), testableClass);
      
    // Run tests
    // ---------
    TestLogger logger = TestLogger.getInstance();
    Class[] parameterArray = new Class[1];
    parameterArray[0] = String[].class;
    boolean allTestsPassed = true;
    for (Class testableClass : testableClassMap.values()) {
      Method mainMethod = testableClass.getMethod ("main", parameterArray);
      try { mainMethod.invoke (null, (Object) null); }
      catch (InvocationTargetException e) {
        logger.failed();
        Throwable cause = e.getCause();
        logger.error (cause.getClass().getName());
        for (StackTraceElement element : cause.getStackTrace()) {
          logger.error ("  at " + element);
        } // for
        allTestsPassed = false;
      } // catch
      catch (Exception e) {
        System.out.println ("Got an Exception while testing");
        System.out.println (e);
      } // catch
      System.out.println();
    } // for

    // Print final conclusion
    // ----------------------
    if (allTestsPassed) {
      System.out.print ("All tests passed ");
      logger.passed();
    } // if
    else {
      System.out.print ("One or more tests ");
      logger.failed();
    } // else

  } // main

  ////////////////////////////////////////////////////////////

} // TestableTester class

////////////////////////////////////////////////////////////////////////
