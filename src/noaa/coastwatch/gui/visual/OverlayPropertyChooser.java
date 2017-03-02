////////////////////////////////////////////////////////////////////////
/*
     FILE: OverlayPropertyChooser.java
  PURPOSE: Sets up the methods of all overlay property choosers.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/12
  CHANGES: 2006/11/02, PFH, added validate()
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import noaa.coastwatch.render.EarthDataOverlay;

/** 
 * The <code>OverlayPropertyChooser</code> class is a panel that
 * holds an overlay value and allows the user to retrieve the
 * value.  The panel contents must be setup by the child class.
 * When a change to the overlay is made, the child must fire a
 * property change event whose property is given by the
 * <code>OVERLAY_PROPERTY</code> constant.  Child classes must
 * implement the <code>getTitle()</code> method to return the
 * overlay property chooser title shown in the titled border.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public abstract class OverlayPropertyChooser<T extends EarthDataOverlay>
  extends JPanel {

  // Constants
  // ---------

  /** The overlay property. */
  public static String OVERLAY_PROPERTY = "overlay";

  // Variables
  // ---------

  /** The overlay to use for choosing properties. */
  protected T overlay;

  ////////////////////////////////////////////////////////////

  /** Creates a new overlay property chooser panel. */
  protected OverlayPropertyChooser (
    T newOverlay
  ) {

    // Initialize
    // ----------
    this.overlay = newOverlay;

    // Set titled border
    // -----------------
    String title = getTitle() + " Overlay Properties";
    this.setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), title),
      new EmptyBorder (4, 4, 4, 4)
    ));
    
  } // OverlayPropertyChooser

  ////////////////////////////////////////////////////////////

  /**
   * Gets the title that will be used to annotate the properties
   * panel.  The properties panel will be titled "XXX Overlay
   * Properties" where "XXX" is the string returned by this method.
   */
  protected abstract String getTitle();

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the overlay stored by this chooser panel.
   *
   * @return the overlay shown by this chooser.
   *
   * @throws IllegalStateException if the chooser controls are
   * not in a valid state.
   */
  public T getOverlay() {

    validateInput();
    return (overlay); 

  } // getOverlay

  ////////////////////////////////////////////////////////////

  /** 
   * Checks the chooser settings for validity.  Some overlay
   * choosers may have a complex set of controls that will not
   * always be in a valid state.  This method is called by the
   * {@link #getOverlay} method to ensure that the chooser
   * controls are set properly.<p>
   *
   * The default implementation of this method does nothing.  It
   * is up to the child class to override this method and perform
   * actual validation.
   *
   * @throws IllegalStateException if the chooser controls are
   * not in a valid state.
   */
  protected void validateInput () { }

  ////////////////////////////////////////////////////////////

} // OverlayPropertyChooser class

////////////////////////////////////////////////////////////////////////
