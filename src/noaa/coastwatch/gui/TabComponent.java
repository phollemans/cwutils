////////////////////////////////////////////////////////////////////////
/*

     File: TabComponent.java
   Author: Peter Hollemans
     Date: 2004/02/17

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui;

// Imports
// -------
import javax.swing.Icon;

/**
 * The <code>TabComponent</code> interface is used to specify that
 * components used in tabs must supply various descriptive elements,
 * such as title, icon, and tool tip.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public interface TabComponent {

  /** 
   * Gets the tab title.
   *
   * @return the title to be used if the tab is to have a text label
   * as a title, or null if no title is required.
   */
  public String getTitle ();

  /** 
   * Gets the tab icon.
   *
   * @return the icon to be used if the tab is to have an icon beside
   * the title label, or null if no icon is required.
   */
  public Icon getIcon ();

  /** 
   * Gets the tab tooltip.
   *
   * @return the tooltip text if the tab is to have an appearing tooltip
   * when the mouse hovers over the tab, or null if no tooltip is
   * required.
   */
  public String getToolTip ();

} // TabComponent interface

////////////////////////////////////////////////////////////////////////
