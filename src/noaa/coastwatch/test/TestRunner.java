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
import java.util.TreeMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import noaa.coastwatch.test.TestLogger;

////////////////////////////////////////////////////////////////////////

/**
 * The TestableTester class contains a single static method main() that 
 * runs the unit tests for all classes that registered for testing.
 *
 * @author Peter Hollemans
 * @since 3.3.1
 */
public class TestRunner {

  // Variables
  // ---------
  
  /** The list of registered classes for testing. */
  private List<Class> registeredClasses;

  /** The singleton instance of this class. */
  private static TestRunner instance = new TestRunner();

  ////////////////////////////////////////////////////////////

  private TestRunner () {

    registeredClasses = new ArrayList<Class>();
  
  } // TestRunner constructor

  ////////////////////////////////////////////////////////////

  /** Gets the singleton instance of this class. */
  public static TestRunner getInstance() { return (instance); }

  ////////////////////////////////////////////////////////////

  /**
   * Registers a class for testing.
   * 
   * @param classObj the class for testing.
   */
  public void register (Class classObj) {
  
    registeredClasses.add (classObj);
  
  } // register

  ////////////////////////////////////////////////////////////

  /** Runs the tests for the classes registered for testing. */
  public void runTests () throws Exception {
  
    // Check assertions
    // ----------------
    boolean assertionsEnabled = false;
    assert (assertionsEnabled = true);
    if (!assertionsEnabled) throw new RuntimeException ("Assertions not enabled");

    // Sort classes by name
    // --------------------
    /**
     * We do this step because we want to guarantee a consistent order
     * for the unit tests so that they run in groups by package.
     */
    TreeMap<String,Class> classNameMap = new TreeMap<String, Class>();
    for (Class classObj : registeredClasses)
      classNameMap.put (classObj.getName(), classObj);
    Set<String> classNames = classNameMap.keySet();
    System.out.println ("Found " + classNames.size() + " class(es) registered for testing:");
    for (String name : classNames) System.out.println ("  " + name);
    System.out.println();

    // Run tests
    // ---------
    TestLogger logger = TestLogger.getInstance();
    Class[] parameterArray = new Class[1];
    parameterArray[0] = String[].class;
    boolean allTestsPassed = true;
    for (Class testClass : classNameMap.values()) {
      Method mainMethod = testClass.getMethod ("main", parameterArray);
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
      System.out.print ("All tests ");
      logger.passed();
    } // if
    else {
      System.out.print ("One or more tests ");
      logger.failed();
    } // else
  
  } // runTests

  ////////////////////////////////////////////////////////////

  /** Runs the main method. */
  public static void main (String[] argv) throws Exception {

    TestRunner runner = TestRunner.getInstance();
    for (String name : argv[0].split ("\\p{Space}+")) runner.register (Class.forName (name));
    runner.runTests();
    
  } // main

  ////////////////////////////////////////////////////////////

} // TestRunner class

////////////////////////////////////////////////////////////////////////
