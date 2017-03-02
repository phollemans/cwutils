////////////////////////////////////////////////////////////////////////
/*

     File: cwtoolstest.java
   Author: Melanie Wright
     Date: 2009/03/29

  CoastWatch Software Library and Utilities
  Copyright (c) 2009 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.tools;

// Imports
// --------
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import noaa.coastwatch.io.IOServices;

/**
 * The <code>cwtoolstest</code> program goes through every command 
 * line tool and tests the options.
 *
 * @author Melanie Wright
 * @since 3.2.3
 */
public class cwtoolstest {

  // Constants
  // ---------
  
  /** The directory to find test data in. */
  private static final String TEST_DATA_DIR = "test";

  /** The name of a test file available. */
  private static final String TEST_FILE1 = "2009_054_1004_n18_wj.hdf";

  /** The name of a second test file available. */
  private static final String TEST_FILE2 = "2009_045_0958_n18_wj.hdf";

  // Variables
  // ---------
  
  /** The absolute path to the test data with trailing slash. */
  private final String absolutePathToTestData;
  
  /** The stream that catches stderr content. */
  private ByteArrayOutputStream errorStream;
  
  /** The stream that catches stdout content. */
  private ByteArrayOutputStream outputStream;

  /** The map of tool name to test count. */
  private Map<String,Integer> testCount;

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String[] argv) {
    
    // Create a test object
    // --------------------
    cwtoolstest test = null;
    try { test = new cwtoolstest(); }
    catch (IOException e) {
      System.err.println ("Unable to instantiate test class: " + e);
    }  // catch
    
    // Run the tests
    // -------------
    test.runTests();
    
  } // main

  ////////////////////////////////////////////////////////////
  
  /** Constructs a new instance of this class. */
  public cwtoolstest () throws IOException {
    
    // Set absolute path to test files
    // -------------------------------
    String testFilePath = IOServices.getFilePath (
        cwtoolstest.class, TEST_DATA_DIR + "/" + TEST_FILE1);
    absolutePathToTestData = testFilePath.substring (
        0, testFilePath.length() - TEST_FILE1.length());
    
    // Create test count map
    // ---------------------
    testCount = new HashMap<String,Integer>();

  } // cwtoolstest constructor
  
  ////////////////////////////////////////////////////////////
  
  /** Runs the tests. */
  public void runTests () {

    // Save system streams
    // -------------------
    PrintStream sysErr = System.err;
    PrintStream sysOut = System.out;
    SecurityManager sysSecure = System.getSecurityManager();

    // Run each test
    // -------------
    try {
      
      // Redirect error and output streams
      // ---------------------------------
      this.errorStream = new ByteArrayOutputStream();
      this.outputStream = new ByteArrayOutputStream();
      System.setErr (new PrintStream (errorStream));
      System.setOut (new PrintStream (outputStream));

      // Create manager to catch System.exit()
      // -------------------------------------
      TestSecurityManager testSecurityManager = new TestSecurityManager();
      ClassLoader classLoader = getClass().getClassLoader();
      URL policy = classLoader.getResource ("toolstest.policy");
      System.setProperty ("java.security.policy", policy.toString());
      System.setSecurityManager (testSecurityManager);

      // Test with no parameters (expect fail)
      // -------------------------------------
      sysOut.println (runTest ("cwangles", true, new String[] {}));
      sysOut.println (runTest ("cwautonav", true, new String[] {}));
      sysOut.println (runTest ("cwcomposite", true, new String[] {}));
      sysOut.println (runTest ("cwcoverage", true, new String[] {}));
      sysOut.println (runTest ("cwdownload", true, new String[] {}));
      sysOut.println (runTest ("cwexport", true, new String[] {}));
      sysOut.println (runTest ("cwgraphics", true, new String[] {}));
      sysOut.println (runTest ("cwimport", true, new String[] {}));
      sysOut.println (runTest ("cwinfo", true, new String[] {}));
      sysOut.println (runTest ("cwmath", true, new String[] {}));
      sysOut.println (runTest ("cwnavigate", true, new String[] {}));
      sysOut.println (runTest ("cwregister", true, new String[] {}));
      sysOut.println (runTest ("cwrender", true, new String[] {}));
      sysOut.println (runTest ("cwsample", true, new String[] {}));
      sysOut.println (runTest ("cwstats", true, new String[] {}));
      sysOut.println (runTest ("hdatt", true, new String[] {}));

      // Run angles tests
      // ----------------
      sysOut.println (runTest ("cwangles", false, new String[] {"--version"}));
      sysOut.println (runTest ("cwangles", true, new String[] {"--location"}));

      String fileLocation = getTestFile (TEST_FILE1, "cwangles");
      sysOut.println (runTest ("cwangles", false, new String[] {
        "--location",
        fileLocation
      }));
      
      sysOut.println (runTest ("cwangles", true, new String[] {
        "--location",
        fileLocation
      }));

      // Run composite tests
      // -------------------
      sysOut.println (runTest ("cwcomposite", false, new String[] {
        "-v",
        "--method", "latest", 
        getTestFile (TEST_FILE1, "cwcomposite"),
        getTestFile (TEST_FILE2, "cwcomposite"),
        getOutputName ("cwcomposite", "hdf", true)
      }));

      // Run coverage tests
      // ------------------
      sysOut.println (runTest ("cwcoverage", false, new String[] {
        "-v",  
        "--labels", "ER/SR", 
        getTestFile (TEST_FILE1, "cwcoverage"),
        getTestFile (TEST_FILE2, "cwcoverage"),
        getOutputName ("cwcoverage", "pdf", true)
      }));

      // Run download tests
      // ------------------
      sysOut.println (runTest ("cwdownload", false, new String[] {
        "--satellite", "noaa-16", 
        "--scenetime", "day", 
        "--test", 
        "--region", "(er|sr)", 
        "--station", "wi", 
        "--dir", TEST_DATA_DIR,
        "psbcw1.nesdis.noaa.gov"
      }));
      
      // Run info tests
      // --------------
      sysOut.println (runTest ("cwinfo", false, new String[] {
        getTestFile (TEST_FILE1, "cwinfo")
      }));

      // TODO Run more tests
      // -------------------
      // sysOut.println (runTest ("cwautonav", false, new String[]{}));
      // sysOut.println (runTest ("cwexport", false, new String[] {"--verbose", "--match", "avhrr_ch1", getTestFile(TEST_FILE1, "cwexport"), getOutputName("cwexport", "ch1", true)}));
      // sysOut.println (runTest ("cwgraphics", false, new String[]{"-v", "--land", "0", "--grid", "0", "--coast", "1", "--political", "1", "--variable", "geography", getTestFile(TEST_FILE1, "cwgraphics")}));
      // sysOut.println (runTest ("cwimport", false, new String[]{}));
      // sysOut.println (runTest ("cwmath", false, new String[]{}));
      // sysOut.println (runTest ("cwnavigate", false, new String[]{}));
      // sysOut.println (runTest ("cwregister", false, new String[]{}));
      // sysOut.println (runTest ("cwrender", false, new String[]{}));
      // sysOut.println (runTest ("cwsample", false, new String[]{}));
      // sysOut.println (runTest ("cwstats", false, new String[]{}));
      // sysOut.println (runTest ("hdatt", false, new String[]{}));

    } // try
    
    // Propagate the exception
    // -----------------------
    catch (IOException e) {
      throw new RuntimeException (e);
    } // catch
    
    finally {
      
      // Set environment back to normal
      // ------------------------------
      if (sysErr != null) { System.setErr (sysErr); }
      if (sysOut != null) { System.setOut (sysOut); }
      if (sysSecure != null) { System.setSecurityManager (sysSecure); }

    } // finally
    
  } // runTests

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a new test file working copy based on an existing file.
   *
   * @param testFile the name of the test file to copy.
   * @param tool the tool being used for testing.
   * 
   * @return the newly created test file name.
   *
   * @throws IOException if an error occurred copying the file
   * data.
   */
  private String getTestFile (
    String testFile, 
    String tool
  ) throws IOException {

    String source = absolutePathToTestData + testFile;
    String dest = absolutePathToTestData + "test-" + tool + "-" + testFile;
    copyFile (source, dest);
    return (dest);

  } // getTestFile

  ////////////////////////////////////////////////////////////
  
  /**
   * Creates a copy of a file.
   * 
   * @param source the source file.
   * @param dest the destination file to create.
   *
   * @throws IOException if an error occurred copying the file
   * data.
   */
  private static void copyFile (
    String source, 
    String dest
  ) throws IOException {
    
    File inputFile = new File (source);
    File outputFile = new File (dest);
    FileInputStream in = null;
    FileOutputStream out = null;
    
    try {
      
      // Create I/O streams
      // ------------------
      in = new FileInputStream (inputFile);
      out = new FileOutputStream (outputFile);

      // Copy file contents in 256 kb chunks
      // -----------------------------------
      byte[] buffer = new byte[256*1024];
      int bytesRead;
      while ((bytesRead = in.read (buffer)) != -1)
        out.write (buffer, 0, bytesRead); 
      
    } // try
    
    finally {

      // Close the I/O streams
      // ---------------------
      if (in != null) {
        try { in.close(); }
        catch (IOException e) { e.printStackTrace(); }
      } // if
      if (out != null) {
        try { out.close(); }
        catch (IOException e) { e.printStackTrace(); }
      } // if
      
    } // finally
    
  } // copyFile

  ////////////////////////////////////////////////////////////

  /**
   * Creates a file name for the output of the specified tool in
   * the test data directory.
   * 
   * @param tool the tool being used for testing. 
   * @param extension the output file extension, not including
   * the trailing period.
   * @param delete the delete flag, true to delete any output
   * file that may exist already with the new output file name.
   *
   * @return the new output file name to use.
   *
   * @throws IOException if the output file already existed, the
   * delete flag was true, and there was some error deleting the
   * file.
   */
  private String getOutputName (
    String tool, 
    String extension,
    boolean delete
  ) throws IOException {

    // Create file name
    // ----------------
    String newName = absolutePathToTestData + "test-" + tool + "-output." + extension;

    // Delete any existing file
    // ------------------------
    if (delete) {
      File file = new File (newName);
      if (file.exists()) {
        boolean deleted;
        try { deleted = file.delete(); }
        catch (SecurityException e) { throw new IOException (e); }
        if (!deleted) throw new IOException ("File delete failed");
      } // if
    } // if    

    return (newName);
    
  } // getOutputName

  ////////////////////////////////////////////////////////////

  /**
   * Runs the main method for a tool with the specified
   * arguments. 
   * 
   * @param tool the class name of the tool to run
   * (noaa.coastwatch.tool is automatically prepended).
   * @param expectError true if the method call is expected to
   * fail or false otherwise.  If true, the test result is
   * labelled as a PASS.
   * @param args the arguments to call the main method with.
   * 
   * @return the result object populated based on the results of
   * the method call.
   * 
   * @throws ClassNotFoundException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   * @throws IOException
   */
  private TestResult runTest (
    String tool, 
    boolean expectError,
    String... args
  ) {
    
    boolean passed = false;
    String message = null;

    // Get test number
    // ---------------
    Integer testNumber = testCount.get (tool);
    if (testNumber == null) testNumber = 1;
    testCount.put (tool, testNumber + 1);
 
    // Get class main method
    // ---------------------
    Method method;
    try {
      Class toolClass = Class.forName ("noaa.coastwatch.tools." + tool);
      method = toolClass.getMethod ("main", String[].class);
    } // try
    catch (Exception e) {
      e.printStackTrace();
      return (new TestResult (tool, testNumber, expectError, args, false,
        "Error getting main method: " + e, errorStream.toString(),
        outputStream.toString()));
    } // catch

    // Call main method
    // ----------------
    try {
      method.invoke (null, new Object[] { (String[]) args });
      passed = !expectError;
      message = "exitStatus = ?";
    } // try
    
    // Catch exit exception
    // --------------------
    catch (SystemExitException see) {
      passed = (expectError ? 
        see.getExitStatus() != 0 :
        see.getExitStatus() == 0
      );
      message = "exitStatus = " + see.getExitStatus();
    } // catch

    // Catch invocation exception
    // --------------------------
    catch (InvocationTargetException e) {
      if (e.getCause() instanceof SystemExitException) {
        SystemExitException see = (SystemExitException) e.getCause();
        passed = (expectError ? 
          see.getExitStatus() != 0 :
          see.getExitStatus() == 0
        );
        message = "exitStatus = " + see.getExitStatus();
      } // if 
      else {
        e.printStackTrace();
        message = "main (" + args + ") threw exception: " + e;
      } // else
    } // catch

    // Catch regular exception
    // -----------------------
    catch (Exception e) {
      e.printStackTrace();
      message = "main (" + args + ") threw exception: " + e;
    } // catch
    
    // Construct a result to return
    // ----------------------------
    TestResult result = new TestResult (tool, testNumber, expectError, args, 
      passed, message, errorStream.toString(), outputStream.toString());

    // Clear error streams
    // -------------------
    errorStream.reset();
    outputStream.reset();
    
    return (result);
    
  } // runTest
  
  ////////////////////////////////////////////////////////////

  /** Holds information on the results of a test run. */
  private class TestResult {
    
    // Variables
    // ---------
    
    /** The name of the tool being tested. */
    private final String toolName;

    /** The sequential test number for the tool. */
    private final int testNumber;
    
    /** The flag for whether or not to expect failure. */
    private final boolean expectError;
    
    /** The arguments the main method was called with. */
    private final String[] args;
    
    /** The pass flag, true if the test passed or false otherwise. */
    private final boolean passed;
    
    /** The message resulting from the test run. */
    private final String message;
    
    /** The String captured from System.err.println() during execution. */
    private final String errorString;
    
    /** The String captured from System.out.println() during execution. */
    private final String outputString;

    ////////////////////////////////////////////////////////////
    
    /**
     * Creates a new results object.
     * 
     * @param toolName the name of the tool being tested.
     * @param testNumber the sequential test number for the tool.
     * @param expectError flag for whether or not to expect
     * failure.
     * @param args the arguments the main method was called with.
     * @param passed true if the test passed, false if it failed.
     * @param message the message resulting from the test run.
     * @param errorString string captured from standard error
     * during execution.
     * @param outputString string captured from standard out
     * during execution.
     */
    public TestResult (
      String toolName, 
      int testNumber,
      boolean expectError, 
      String[] args,
      boolean passed, 
      String message, 
      String errorString, 
      String outputString
    ) {
      
      this.toolName = toolName;
      this.testNumber = testNumber;
      this.expectError = expectError;
      this.args = args;
      this.passed = passed;
      this.message = message;
      this.errorString = errorString;
      this.outputString = outputString;
      
    } // TestResult constructor
    
    ////////////////////////////////////////////////////////////

    /** Gets the name of the tool being tested. */
    public String getToolName () { return (toolName); }

    ////////////////////////////////////////////////////////////
    
    /** Returns true if the test was expected to fail, false otherwise. */
    public boolean expectError () { return (expectError); }

    ////////////////////////////////////////////////////////////
    
    /** Gets the args the main method was called with. */
    public String[] getArgs () { return (args); } 

    ////////////////////////////////////////////////////////////
    
    /** Returns true if the test passed, false if it failed. */
    public boolean getPassed () { return (passed); }

    ////////////////////////////////////////////////////////////
    
    /** Gets the message resulting from the test run. */
    public String getMessage () { return (message); }

    ////////////////////////////////////////////////////////////
    
    public String toString () {

      // Format simple passed string
      // ---------------------------
      if (passed) {
        return ("[PASSED] " + toolName + " test #" + testNumber);
      } // if

      // Format failed string
      // --------------------
      else {
        StringBuffer argsBuf = new StringBuffer();
        for (String str : args) {
          argsBuf.append (str);
          argsBuf.append (" ");
        } // for
        String argsStr = argsBuf.toString().trim();
        return ("[FAILED] " + toolName + " test #" + testNumber +
          ", args = {" + argsStr + "}\n" +
          errorString);
      } // else
      
    } // toString
    
    ////////////////////////////////////////////////////////////
    
  } // TestResult class

  ////////////////////////////////////////////////////////////
  
  /** Manages exit calls by throwing a SystemExitException. */
  class TestSecurityManager 
    extends SecurityManager {
    public void checkExit (int status) {
      
      throw new SystemExitException (status);
      
    } // checkExit
  } // TestSecurityManager class

  ////////////////////////////////////////////////////////////
  
  /** Holds an exit status code. */
  public class SystemExitException 
    extends RuntimeException {

    // Variables
    // ---------
    /** The exit status code. */
    private final int exitStatus;

    ////////////////////////////////////////////////////////////
    
    /** Create a new exception with the specifid exit status. */
    public SystemExitException (int exitStatus) {

      this.exitStatus = exitStatus;

    } // SystemExitException constructor

    ////////////////////////////////////////////////////////////
    
    /** Gets the exit status. */
    public int getExitStatus () { return (exitStatus); }

    ////////////////////////////////////////////////////////////
    
  } // SystemExitException class
  
  ////////////////////////////////////////////////////////////
  
} // cwtoolstest class

////////////////////////////////////////////////////////////////////////
