////////////////////////////////////////////////////////////////////////
/*
     FILE: ComponentProducer.java
  PURPOSE: Defines an interface for objects that have a corresponding
           component.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/02
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.*;

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
