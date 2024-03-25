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
      String debugMode = (debugProperty != null ? debugProperty : debugVariable);
      String debugLevel = (
        debugMode == null ? null :
        debugMode.matches ("true|fine") ? "FINE" :
        debugMode.equals ("finer") ? "FINER" : 
        null
      );

      // Detect what type of logging format we want.  A short format just
      // prints the logging level, message, and error if applicable.  The
      // long format also adds the time stamp and class.
      String logFormatProperty = System.getProperty ("cw.log.format");
      String logFormat = (
        logFormatProperty == null ? null :
        logFormatProperty.equals ("short") ? "[%4$s] %5$s%6$s%n" :
        logFormatProperty.equals ("long") ? "%1$tF %1$tT %4$s [%2$s] - %5$s%6$s%n" : 
        null
      );

      // Set up the log manager
      // ----------------------
      LogManager logManager = LogManager.getLogManager();
      try {
        logManager.reset();

        // Create a logging config injection that remaps the logging level
        // and the format upon request.
        Function<String, BiFunction<String, String, String>> mapper = key -> {

          BiFunction<String, String, String> function;

          if (key.equals ("noaa.coastwatch.level") && (debugLevel != null))
            function = (oldVal, newVal) -> debugLevel;
          else if (key.equals ("java.util.logging.SimpleFormatter.format") && (logFormat != null))
            function = (oldVal, newVal) -> logFormat;
          else 
            function = (oldVal, newVal) -> newVal;

          return (function);

        };
        logManager.updateConfiguration (stream, mapper);

      } // try
      catch (Exception e) {
        e.printStackTrace();
      } // catch
    } // if

  } // LoggingConfig const
} // LoggingConfig class

////////////////////////////////////////////////////////////////////////
