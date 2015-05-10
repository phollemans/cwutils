////////////////////////////////////////////////////////////////////////
/*
     FILE: SubsetOptionPanel.java
  PURPOSE: Allows the user to choose subset options for data export.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/03
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
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.Box;
import javax.swing.ButtonGroup;
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
import noaa.coastwatch.util.EarthLocation;

/** 
 * The <code>SubsetOptionPanel</code> class allows the user to choose
 * from a set of data export subset options.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class SubsetOptionPanel
  extends JPanel {

  // Constants
  // ---------

  /** The subset type for all data. */
  public static final int SUBSET_ALL = 0;

  /** The subset type for current data. */
  public static final int SUBSET_CURRENT = 1;

  /** The subset type for image coordinates. */
  public static final int SUBSET_IMAGE = 2;

  /** The subset type for geographic coordinates. */
  public static final int SUBSET_GEOGRAPHIC = 3;

  // Variables
  // ---------

  /** The radio button for all data. */
  private JRadioButton allRadio;

  /** The radio button for current view data. */
  private JRadioButton currentRadio;

  /** The radio button for image coordinates. */
  private JRadioButton imageRadio;

  /** The radio button for geographic coordinates. */
  private JRadioButton geographicRadio;

  /** The panel holding controls for image subsets. */
  private JPanel imagePanel;

  /** The panel holding controls for geographic subsets. */
  private JPanel geographicPanel;

  /** The text field for image upper-left row. */
  private JTextField rowField;

  /** The text field for image upper-left column. */
  private JTextField colField;

  /** The text field for image rows. */
  private JTextField rowsField;

  /** The text field for image columns. */
  private JTextField colsField;

  /** The text field for geographic upper-left latitude. */
  private JTextField ulLatField;

  /** The text field for geographic upper-left longitude. */
  private JTextField ulLonField;

  /** The text field for geographic lower-right latitude. */
  private JTextField lrLatField;

  /** The text field for geographic lower-right longitude. */
  private JTextField lrLonField;

  ////////////////////////////////////////////////////////////

  /** Creates the image coordinates panel. */
  private JPanel createImagePanel () {

    // Initialize
    // ----------
    JPanel imagePanel = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (0, 0, 0, 4);

    // Add upper-left controls
    // -----------------------
    GUIServices.setConstraints (gc, 0, 0, 4, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    imagePanel.add (new JLabel ("Upper-left corner (pixels):"), gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    imagePanel.add (new JLabel ("Row"), gc);
    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 4, 0);
    rowField = new JTextField();
    imagePanel.add (rowField, gc);

    GUIServices.setConstraints (gc, 2, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    imagePanel.add (new JLabel ("Column"), gc);
    GUIServices.setConstraints (gc, 3, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 4, 0);
    colField = new JTextField();
    imagePanel.add (colField, gc);
    
    // Add dimensions controls
    // -----------------------
    GUIServices.setConstraints (gc, 0, 2, 4, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    imagePanel.add (new JLabel ("Dimensions (pixels):"), gc);

    GUIServices.setConstraints (gc, 0, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    imagePanel.add (new JLabel ("Rows"), gc);
    GUIServices.setConstraints (gc, 1, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 4, 0);
    rowsField = new JTextField();
    imagePanel.add (rowsField, gc);

    GUIServices.setConstraints (gc, 2, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    imagePanel.add (new JLabel ("Columns"), gc);
    GUIServices.setConstraints (gc, 3, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 4, 0);
    colsField = new JTextField();
    imagePanel.add (colsField, gc);

    return (imagePanel);

  } // createImagePanel

  ////////////////////////////////////////////////////////////

  /** Creates the geographic coordinates panel. */
  private JPanel createGeographicPanel () {

    // Initialize
    // ----------
    JPanel geographicPanel = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (0, 0, 0, 4);

    // Add upper-left controls
    // -----------------------
    GUIServices.setConstraints (gc, 0, 0, 4, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    geographicPanel.add (new JLabel ("Upper-left corner (degrees):"), gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    geographicPanel.add (new JLabel ("Latitude"), gc);
    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 4, 0);
    ulLatField = new JTextField();
    geographicPanel.add (ulLatField, gc);

    GUIServices.setConstraints (gc, 2, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    geographicPanel.add (new JLabel ("Longitude"), gc);
    GUIServices.setConstraints (gc, 3, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 4, 0);
    ulLonField = new JTextField();
    geographicPanel.add (ulLonField, gc);
    
    // Add dimensions controls
    // -----------------------
    GUIServices.setConstraints (gc, 0, 2, 4, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    geographicPanel.add (new JLabel ("Lower-right corner (degrees):"), gc);

    GUIServices.setConstraints (gc, 0, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    geographicPanel.add (new JLabel ("Latitude"), gc);

    GUIServices.setConstraints (gc, 1, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 4, 0);
    lrLatField = new JTextField();
    geographicPanel.add (lrLatField, gc);

    GUIServices.setConstraints (gc, 2, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    geographicPanel.add (new JLabel ("Longitude"), gc);

    GUIServices.setConstraints (gc, 3, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 4, 0);
    lrLonField = new JTextField();
    geographicPanel.add (lrLonField, gc);

    return (geographicPanel);

  } // createGeographicPanel

  ////////////////////////////////////////////////////////////

  /** Gets the subset type. */
  public int getSubsetType () {

    int type;
    if (allRadio.isSelected()) type = SUBSET_ALL;
    else if (currentRadio.isSelected()) type = SUBSET_CURRENT;
    else if (imageRadio.isSelected()) type = SUBSET_IMAGE;
    else if (geographicRadio.isSelected()) type = SUBSET_GEOGRAPHIC;
    else throw new IllegalStateException ("No subset type selected");

    return (type);

  } // getSubsetType

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the image coordinates.
   *
   * @param upperLeft the upper-left data image coordinate as <code>[row,
   * column]</code> (modified).
   * @param dimensions the data image dimensions as <code>[rows,
   * columns]</code> (modified).
   */
  public void getImageCoords (
    int[] upperLeft,
    int[] dimensions
  ) {

    upperLeft[0] = Integer.parseInt (rowField.getText());
    upperLeft[1] = Integer.parseInt (colField.getText());
    dimensions[0] = Integer.parseInt (rowsField.getText());
    dimensions[1] = Integer.parseInt (colsField.getText());

  } // getImageCoordinates

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the geographic coordinates.
   *
   * @param upperLeft the upper-left geographic coordinate (modified).
   * @param lowerRight the lower-right geographic coordinate (modified).
   */
  public void getGeographicCoords (
    EarthLocation upperLeft,
    EarthLocation lowerRight
  ) {

    upperLeft.lat = Double.parseDouble (ulLatField.getText());
    upperLeft.lon = Double.parseDouble (ulLonField.getText());
    lowerRight.lat = Double.parseDouble (lrLatField.getText());
    lowerRight.lon = Double.parseDouble (lrLonField.getText());

  } // getGeographicCoordinates

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new subset option panel.
   *
   * @param upperLeft the upper-left data coordinate as <code>[row,
   * column]</code>.
   * @param dimensions the data dimensions as
   * <code>[rows, columns]</code>.
   * @param bounds the data bounds as
   * <code>[upper-left, lower-right]</code>.
   */
  public SubsetOptionPanel (
    int[] upperLeft,                            
    int[] dimensions,
    EarthLocation[] bounds
  ) {

    // Initialize
    // ----------
    super (new GridBagLayout());
    setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), "Subset Options"),
      new EmptyBorder (4, 4, 4, 4)
    ));
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 0);
    ButtonGroup group = new ButtonGroup();

    // Add all radio button
    // --------------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    allRadio = new JRadioButton ("Use full dimensions", true);
    group.add (allRadio);
    this.add (allRadio, gc);

    // Add current radio button
    // ------------------------
    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    currentRadio = new JRadioButton ("Subset using current view");
    group.add (currentRadio);
    this.add (currentRadio, gc);

    // Add image coordinates radio button
    // ----------------------------------
    GUIServices.setConstraints (gc, 0, 2, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    imageRadio = new JRadioButton ("Subset using image coordinates");
    group.add (imageRadio);
    this.add (imageRadio, gc);
    imageRadio.addItemListener (new ItemListener() {
        public void itemStateChanged (ItemEvent e) {
          GUIServices.setContainerEnabled (imagePanel, 
            imageRadio.isSelected());
        } // itemStateChanged
      });

    GUIServices.setConstraints (gc, 0, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    imagePanel = createImagePanel();
    rowField.setText (Integer.toString (upperLeft[0]));
    colField.setText (Integer.toString (upperLeft[1]));
    rowsField.setText (Integer.toString (dimensions[0]));
    colsField.setText (Integer.toString (dimensions[1]));
    GUIServices.setContainerEnabled (imagePanel, false);
    JPanel imageContainerPanel = new JPanel (new BorderLayout());
    imageContainerPanel.add (Box.createHorizontalStrut (20), 
      BorderLayout.WEST);
    imageContainerPanel.add (imagePanel, BorderLayout.CENTER);
    this.add (imageContainerPanel, gc);

    // Add geographic coordinates radio button
    // ---------------------------------------
    GUIServices.setConstraints (gc, 0, 4, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    geographicRadio = 
      new JRadioButton ("Subset using geographic coordinates");
    group.add (geographicRadio);
    this.add (geographicRadio, gc);
    geographicRadio.addItemListener (new ItemListener() {
        public void itemStateChanged (ItemEvent e) {
          GUIServices.setContainerEnabled (geographicPanel, 
            geographicRadio.isSelected());
        } // itemStateChanged
      });

    GUIServices.setConstraints (gc, 0, 5, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    geographicPanel = createGeographicPanel();
    ulLatField.setText (Double.toString (bounds[0].lat));
    ulLonField.setText (Double.toString (bounds[0].lon));
    lrLatField.setText (Double.toString (bounds[1].lat));
    lrLonField.setText (Double.toString (bounds[1].lon));
    GUIServices.setContainerEnabled (geographicPanel, false);
    JPanel geographicContainerPanel = new JPanel (new BorderLayout());
    geographicContainerPanel.add (Box.createHorizontalStrut (20), 
      BorderLayout.WEST);
    geographicContainerPanel.add (geographicPanel, BorderLayout.CENTER);
    this.add (geographicContainerPanel, gc);

  } // SubsetOptionPanel constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    JPanel panel = new SubsetOptionPanel (new int[] {0, 0}, 
      new int[] {1024, 1024},
      new EarthLocation[] {new EarthLocation (50, -130), 
      new EarthLocation (30, -100)});
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // SubsetOptionPanel class

////////////////////////////////////////////////////////////////////////
