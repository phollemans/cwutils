////////////////////////////////////////////////////////////////////////
/*

     File: UsageInfo.java
   Author: Peter Hollemans
     Date: 2019/04/01

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
import java.util.Formatter;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;

/**
 * The <code>UsageInfo</code> class holds information about how to call
 * a command line tool: its main function, options, and parameters.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class UsageInfo {

  // Variables
  // ---------

  /** The name of the command. */
  private String command;
  
  /** The function of the command. */
  private String func;

  /** The map of group numbers to maps of parameter names and descriptons. */
  private Map<Integer, Map <String, String>> paramMap;

  /** A map of option names to descriptions. */
  private Map<String, String> optionMap;

  ////////////////////////////////////////////////////////////

  public UsageInfo (String command) {

    this.command = command;
    paramMap = new TreeMap<>();
    optionMap = new LinkedHashMap<>();

  } // UsageInfo constructor

  ////////////////////////////////////////////////////////////

  public void func (String func) { this.func = func; }
  public void param (String param, String desc) { param (param, desc, 1); }
  public void param (String param, String desc, int group) {
    Map<String, String> map = paramMap.get (group);
    if (map == null) { map = new LinkedHashMap<>(); paramMap.put (group, map); }
    map.put (param, desc);
  } // param
  public void option (String option, String desc) { optionMap.put (option, desc); }
  public void section (String section) { optionMap.put (section, null); }

  ////////////////////////////////////////////////////////////

  @Override
  public String toString() {
  
    StringBuilder builder = new StringBuilder();
    Formatter formatter = new Formatter (builder);

    // Find first column size
    // ----------------------
    int maxColumnSize = 0;
    for (int group : paramMap.keySet()) {
      for (String key : paramMap.get (group).keySet()) maxColumnSize = Math.max (key.length(), maxColumnSize);
    } // for
    for (String key : optionMap.keySet()) maxColumnSize = Math.max (key.length(), maxColumnSize);
    String columnFormat = String.format ("  %%-%ds  %%s\n", maxColumnSize);

    // Format synopsis
    // ---------------
    boolean isFirstLine = true;
    for (int group : paramMap.keySet()) {
      if (isFirstLine) {
        formatter.format ("Usage:  %s", command);
        isFirstLine = false;
      } // if
      else
        formatter.format ("        %s", command);
      if (optionMap.size() != 0) builder.append (" [OPTIONS]");
      paramMap.get (group).forEach ((name, desc) -> {
        formatter.format (" %s", name);
      });
      builder.append ("\n");
    } // for
    formatter.format ("%s\n", func);

    // Format parameters
    // -----------------
    if (paramMap.size() != 0) {
      formatter.format ("\nMain parameters:\n");
      Set<String> paramPrinted = new HashSet<>();
      paramMap.forEach ((group, map) -> {
        map.forEach ((name, desc) -> {
          if (!paramPrinted.contains (name)) {
            formatter.format (columnFormat, name, desc);
            paramPrinted.add (name);
          } // if
        });
      });
    } // if

    // Format options
    // --------------
    if (optionMap.size() != 0) {
      formatter.format ("\nOptions:\n");
      optionMap.forEach ((name, desc) -> {
        if (desc == null)
          formatter.format ("\n  %s:\n", name);
        else
          formatter.format (columnFormat, name, desc);
      });
    } // if

    return (builder.toString());

  } // toString

  ////////////////////////////////////////////////////////////

} // UsageInfo class

////////////////////////////////////////////////////////////////////////
