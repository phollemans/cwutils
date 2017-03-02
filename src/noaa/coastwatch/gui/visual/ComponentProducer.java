////////////////////////////////////////////////////////////////////////
/*

     File: ComponentProducer.java
   Author: Peter Hollemans
     Date: 2004/03/02

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
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.Component;

/**
 * The <code>ComponentProducer</code> interface defines the methods
 * required for objects that have associated components.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public interface ComponentProducer {

  /**
   * Gets the visual component used to display the object's
   * properties.
   */
  public Component getComponent ();

  /** 
   * Refreshes the component to reflect the producer object data
   * (optional operation).
   */
  public void refreshComponent ();

} // ComponentProducer interface

////////////////////////////////////////////////////////////////////////
