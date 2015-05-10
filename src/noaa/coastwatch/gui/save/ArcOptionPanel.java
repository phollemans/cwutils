////////////////////////////////////////////////////////////////////////
/*
     FILE: ArcOptionPanel.java
  PURPOSE: Allows the user to choose ArcGIS options for data export.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/04
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;

/** 
 * The <code>ArcOptionPanel</code> class allows the user to choose
 * from a set of ArcGIS options for data export.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ArcOptionPanel
  extends JPanel {

  // Variables
  // ---------

  /** The check box for dimension header. */
  private JCheckBox headerCheck;

  ////////////////////////////////////////////////////////////

  /** Gets the dimension header flag. */
  public boolean getHeader () { return (headerCheck.isSelected()); }

  ////////////////////////////////////////////////////////////

  /** Creates a new text option panel. */
  public ArcOptionPanel () {

    // Initialize
    // ----------
    super (new GridBagLayout());
    setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), "ArcGIS Options"),
      new EmptyBorder (4, 4, 4, 4)
    ));
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 0);

    // Add dimension header checkbox
    // -----------------------------
    GUIServices.setConstraints (gc, 0, 0, 2, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    headerCheck = new JCheckBox ("Write header file ( .hdr )", true);
    this.add (headerCheck, gc);

  } // ArcOptionPanel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    JPanel panel = new ArcOptionPanel();
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // ArcOptionPanel class

////////////////////////////////////////////////////////////////////////
