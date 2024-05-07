/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.tools;

import java.util.List;
import java.util.ArrayList;

/**
 * <p>The tools utility lists all the CoastWatch Utilities tools and their
 * function.</p>
 *
 * <!-- START MAN PAGE -->
 *
 * <h2>Name</h2>
 * <p> 
 *   <!-- START NAME -->
 *   cwtools - lists all tools in the CoastWatch Utilities.
 *   <!-- END NAME -->
 * </p>
 *
 * <!-- END MAN PAGE -->
 *
 * @author Peter Hollemans
 * @since 3.8.0
 */
public final class cwtools {

  ////////////////////////////////////////////////////////////

  /**
   * Performs the main function.
   *
   * @param argv the list of command line parameters.  
   */
  public static void main (String argv[]) {

    var usageList = List.of (
      cdat.getUsage(),
      cwangles.getUsage(),
      cwautonav.getUsage(),
      cwcomposite.getUsage(),
      cwcoverage.getUsage(),
      cwexport.getUsage(),
      cwgraphics.getUsage(),
      cwimport.getUsage(),
      cwinfo.getUsage(),
      cwmaster.getUsage(),
      cwmath.getUsage(),
      cwnavigate.getUsage(),
//      cwregister.getUsage(),
      cwregister2.getUsage(),
      cwrender.getUsage(),
      cwsample.getUsage(),
      cwscript.getUsage(),
      cwstats.getUsage(),
      cwtccorrect.getUsage(),
      hdatt.getUsage()
    );
    System.out.println ("The CoastWatch Utilities contains the following tools:\n");
    for (var usage : usageList) {
      System.out.println (usage.getName() + " - " + usage.getFunc());
    } // for
    System.out.println (
      "\n" +
      "For documentation, see the user's guide, tool manual pages, or type -h after\n" + 
      "the tool name for a synopsis.\n"
    );

  } // main

  ////////////////////////////////////////////////////////////

  private cwtools () { }

  ////////////////////////////////////////////////////////////

} // cwtools class 
