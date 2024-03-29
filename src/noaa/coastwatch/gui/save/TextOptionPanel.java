////////////////////////////////////////////////////////////////////////
/*

     File: TextOptionPanel.java
   Author: Peter Hollemans
     Date: 2004/05/04

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
package noaa.coastwatch.gui.save;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;

/** 
 * The <code>TextOptionPanel</code> class allows the user to choose
 * from a set of text options for data export.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class TextOptionPanel
  extends JPanel {

  // Constants
  // ---------

  /** The coordinate order for (latitude, longitude). */
  public static final int ORDER_LATLON = 0;

  /** The coordinate order for (longitude, latitude). */
  public static final int ORDER_LONLAT = 1;

  // Variables
  // ---------

  /** The check box for dimension header. */
  private JCheckBox headerCheck;

  /** The check box for geographic coordinates. */
  private JCheckBox coordsCheck;

  /** The radio button for lat/lon coordinate order. */
  private JRadioButton latlonRadio;

  /** The radio button for lon/lat coordinate order. */
  private JRadioButton lonlatRadio;

  /** The panel used for geographic coordinate order controls. */
  private JPanel coordsPanel;

  /** The missing value field. */
  private JTextField missingField;

  ////////////////////////////////////////////////////////////

  /** Gets the dimension header flag. */
  public boolean getHeader () { return (headerCheck.isSelected()); }

  ////////////////////////////////////////////////////////////

  /** Gets the geographic coordinates flag. */
  public boolean getCoords () { return (coordsCheck.isSelected()); }

  ////////////////////////////////////////////////////////////

  /** Gets the geographic coordinate order. */
  public int getCoordOrder () { 

    int type;
    if (latlonRadio.isSelected()) type = ORDER_LATLON;
    else if (lonlatRadio.isSelected()) type = ORDER_LONLAT;
    else throw new IllegalStateException ("No coordinate order selected");

    return (type);

  } // getCoordOrder

  ////////////////////////////////////////////////////////////

  /** Gets the missing value. */
  public double getMissing () { 

    String text = missingField.getText();
    if (text.equalsIgnoreCase ("NaN")) return (Double.NaN);
    else return (Double.parseDouble (text));

  } // getMissing

  ////////////////////////////////////////////////////////////

  /** Creates the geographic coordinates panel. */
  private JPanel createCoordsPanel () {

    // Initialize
    // ----------
    JPanel coordsPanel = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    ButtonGroup group = new ButtonGroup();

    // Add radio buttons
    // -----------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    latlonRadio = new JRadioButton ("Write as (latitude, longitude)", true);
    group.add (latlonRadio);
    coordsPanel.add (latlonRadio, gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    lonlatRadio = new JRadioButton ("Write as (longitude, latitude)", false);
    group.add (lonlatRadio);
    coordsPanel.add (lonlatRadio, gc);

    return (coordsPanel);

  } // createCoordsPanel

  ////////////////////////////////////////////////////////////

  /** Creates a new text option panel. */
  public TextOptionPanel () {

    // Initialize
    // ----------
    super (new GridBagLayout());
    setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), "Text Options"),
      new EmptyBorder (4, 4, 4, 4)
    ));
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 0);

    // Add dimension header checkbox
    // -----------------------------
    GUIServices.setConstraints (gc, 0, 0, 2, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    headerCheck = new JCheckBox ("Write 1-line dimension headers", false);
    this.add (headerCheck, gc);

    // Add geographic dimensions checkbox
    // ----------------------------------
    GUIServices.setConstraints (gc, 0, 1, 2, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    coordsCheck = new JCheckBox ("Include geographic coordinates", true);
    this.add (coordsCheck, gc);
    coordsCheck.addItemListener (new ItemListener() {
        public void itemStateChanged (ItemEvent e) {
          GUIServices.setContainerEnabled (coordsPanel, 
            coordsCheck.isSelected());
        } // itemStateChanged
      });

    GUIServices.setConstraints (gc, 0, 2, 2, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    coordsPanel = createCoordsPanel();
    JPanel coordsContainerPanel = new JPanel (new BorderLayout());
    coordsContainerPanel.add (Box.createHorizontalStrut (20), 
      BorderLayout.WEST);
    coordsContainerPanel.add (coordsPanel, BorderLayout.CENTER);
    this.add (coordsContainerPanel, gc);

    // Add missing value field
    // -----------------------
    GUIServices.setConstraints (gc, 0, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Missing value:"), gc);

    GUIServices.setConstraints (gc, 1, 3, 1, 1, 
      GridBagConstraints.NONE, 0, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    missingField = new JTextField (12);
    missingField.setText ("NaN");
    this.add (missingField, gc);

  } // TextOptionPanel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    JPanel panel = new TextOptionPanel();
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // TextOptionPanel class

////////////////////////////////////////////////////////////////////////
