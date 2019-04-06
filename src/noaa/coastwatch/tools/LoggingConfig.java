////////////////////////////////////////////////////////////////////////
/*

     File: LoggingConfig.java
   Author: Peter Hollemans
     Date: 2019/02/19

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
// -------
import java.util.logging.LogManager;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.io.InputStream;

/**
 * The <code>LoggingConfig</code> class sets up the default logging configuration
 * used by the command line tools.  This class should be named on the Java
 * command line with the system property java.util.logging.config.class to
 * properly initialize the logging system.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class LoggingConfig {
  public LoggingConfig () {

    // Read the main log config from our custom file
    // ---------------------------------------------
    InputStream stream = ClassLoader.getSystemResourceAsStream ("logging.properties");

    if (stream != null) {

      // Detect if we want debugging info
      // --------------------------------
      String debugProperty = System.getProperty ("cw.debug");
      String debugVariable = System.getenv ("CW_DEBUG");
      boolean isDebug = (
        (debugProperty != null && debugProperty.equals ("true")) ||
        (debugVariable != null && debugVariable.equals ("true"))
      );

      // Set up the log manager
      // ----------------------
      LogManager logManager = LogManager.getLogManager();
      try {
        logManager.reset();

        // Inject a FINE log level if debugging requested
        // ----------------------------------------------
        Function<String, BiFunction<String, String, String>> mapper =
          (key) -> key.equals ("noaa.coastwatch.level") && isDebug
          ? ((oldVal, newVal) -> "FINE")
          : ((oldVal, newVal) -> newVal);

        // Read/update the config
        // ----------------------
        logManager.updateConfiguration (stream, mapper);

      } // try
      catch (Exception e) {
        e.printStackTrace();
      } // catch
    } // if

  } // LoggingConfig const
} // LoggingConfig class

////////////////////////////////////////////////////////////////////////
