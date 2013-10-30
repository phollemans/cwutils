////////////////////////////////////////////////////////////////////////
/*
     FILE: RenderOptionPanel.java
  PURPOSE: Allows the user to choose options for rendered images.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/03
  CHANGES: 2006/11/15, PFH, added more options

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import noaa.coastwatch.gui.*;

/** 
 * The <code>RenderOptionPanel</code> class allows the user to choose
 * from a set of image rendering options.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class RenderOptionPanel
  extends JPanel {

  // Variables
  // ---------

  /** The checkbox for plot legends. */
  private JCheckBox nolegendsCheck;
  
  /** The checkbox for font and line smoothing. */
  private JCheckBox nosmoothCheck;

  /** The checkbox for limiting image colors. */
  private JCheckBox colorCheck;

  /** The spinner for the number of colors to limit images to. */
  private JSpinner colorSpinner;

  /** The world file check box. */
  private JCheckBox worldCheck;

  /** The checkbox for TIFF compression. */
  private JCheckBox compressCheck;

  /** The combo box for TIFF compression algorithm. */
  private JComboBox compressCombo;

  /** The line of controls for color limits. */
  private JPanel colorLine;

  ////////////////////////////////////////////////////////////

  /** Gets the legends flag. */
  public boolean getLegends () { 

    if (nolegendsCheck == null) return (false);
    return (!nolegendsCheck.isSelected());

  } // getLegends

  ////////////////////////////////////////////////////////////

  /** Gets the smooth flag. */
  public boolean getSmooth () { 
    
    if (nosmoothCheck == null) return (false);
    return (!nosmoothCheck.isSelected());

  } // getSmooth

  ////////////////////////////////////////////////////////////

  /** Gets the world file flag. */
  public boolean getWorld () { 

    if (worldCheck == null) return (false);
    return (worldCheck.isEnabled() && worldCheck.isSelected()); 

  } // getWorld

  ////////////////////////////////////////////////////////////

  /** Gets the number of colors or 0 for unlimited. */
  public int getColors () { 

    if (colorCheck == null) return (0);
    return ((colorCheck.isEnabled() && colorCheck.isSelected()) ? 
      ((Integer) colorSpinner.getValue()).intValue() : 0); 

  } // getColors

  ////////////////////////////////////////////////////////////

  /** Gets the TIFF compression type. */
  public String getCompress () { 

    if (compressCheck == null) return (null);
    return (compressCheck.isSelected() ? 
      ((String) compressCombo.getSelectedItem()).toLowerCase() : "none");

  } // getCompress

  ////////////////////////////////////////////////////////////

  /** Adds the components as a line to this panel. */
  private JPanel addLine (
    Component[] components
  ) {

    JPanel line = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 2));
    for (int i = 0; i < components.length; i++)
      line.add (components[i]);
    line.setAlignmentX (0);
    this.add (line);

    return (line);

  } // addLine

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new option panel with the specified options
   * 
   * @param showLegends if true, shows the legends checkbox.
   * @param showSmooth if true, shows the smoothing checkbox.
   * @param showWorld if true, shows the world file checkbox.
   * @param showColor if true, shows the color index checkbox and
   * spinner.
   * @param showCompress if true, shows the TIFF compression checkbox
   * and combo.
   */
  public RenderOptionPanel (
    boolean showLegends,
    boolean showSmooth,
    boolean showWorld,
    boolean showColor,
    boolean showCompress
  ) {

    // Initialize
    // ----------
    setLayout (new BoxLayout (this, BoxLayout.Y_AXIS));
    setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), "Rendering Options"),
      new EmptyBorder (4, 4, 4, 4)
    ));
    setBorder (new TitledBorder (new EtchedBorder(), "Rendering Options"));

    // Add legend check box
    // --------------------
    if (showLegends) {
      nolegendsCheck = new JCheckBox ("Data image only, no legends", 
        false);
      nolegendsCheck.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent event) {
            if (worldCheck != null) 
              worldCheck.setEnabled (nolegendsCheck.isSelected());
          } // actionPerformed
        });
      addLine (new Component[] {nolegendsCheck});
    } // if

    // Add world file checkbox
    // -----------------------
    if (showLegends && showWorld) {
      worldCheck = new JCheckBox ("Write world file ( .wld )");
      worldCheck.setEnabled (false);
      addLine (new Component[] {Box.createHorizontalStrut (10), worldCheck});
    } // if

    // Add smoothing checkbox
    // ----------------------
    if (showSmooth) {
      nosmoothCheck = new JCheckBox ("No smoothing of fonts and lines", false);
      nosmoothCheck.addActionListener (new ActionListener() {
          public void actionPerformed (ActionEvent event) {
            if (colorLine != null) {
              GUIServices.setContainerEnabled (colorLine, 
                nosmoothCheck.isSelected());
            } // if
          } // actionPerformed
        });
      addLine (new Component[] {nosmoothCheck});
    } // if

    // Add color limiting controls
    // ---------------------------
    if (showColor) {
      colorCheck = new JCheckBox ("Limit image to");
      colorSpinner = new JSpinner (new SpinnerNumberModel (256, 2, 256, 1)); 
      JLabel colorLabel = new JLabel ("colors");
      Component[] comps = (showSmooth ?
        new Component[] {Box.createHorizontalStrut (10), colorCheck, 
        colorSpinner, colorLabel} :
        new Component[] {colorCheck, colorSpinner, colorLabel}
      );
      colorLine = addLine (comps);
      if (showSmooth) GUIServices.setContainerEnabled (colorLine, false);
    } // if
    
    // Add TIFF compression
    // --------------------
    if (showCompress) {
      compressCheck = new JCheckBox ("Use TIFF compression algorithm");
      compressCombo = new JComboBox (new String[] {"Deflate", "Pack"});
      addLine (new Component[] {compressCheck, compressCombo});
    } // if

  } // RenderOptionPanel constructor

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) {

    JPanel panel = new RenderOptionPanel (true, true, true, true, true);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // RenderOptionPanel class

////////////////////////////////////////////////////////////////////////
