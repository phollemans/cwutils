////////////////////////////////////////////////////////////////////////
/*
     FILE: BinaryOptionPanel.java
  PURPOSE: Allows the user to choose binary options for data export.
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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import noaa.coastwatch.gui.*;

/** 
 * The <code>BinaryOptionPanel</code> class allows the user to choose
 * from a set of data export binary options.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class BinaryOptionPanel
  extends JPanel {

  // Constants
  // ---------

  /** The data type for 8-bit unsigned integers. */
  public static final int TYPE_UBYTE = 0;

  /** The data type for 16-bit signed integers. */
  public static final int TYPE_SHORT = 1;

  /** The data type for 32-bit floating-point. */
  public static final int TYPE_FLOAT = 2;

  /** The data type strings. */
  private static final String[] DATA_TYPES = new String[] {
    "8-bit unsigned int",
    "16-bit signed int",
    "32-bit float"
  };

  /** The byte order for host. */
  public static final int ORDER_HOST = 0;

  /** The byte order for most-significant-byte first. */
  public static final int ORDER_MSB = 1;

  /** The byte order for least-significant-byte first. */
  public static final int ORDER_LSB = 2;

  /** The byte order strings. */
  private static final String[] BYTE_ORDERS = new String[] {
    "Host",
    "MSB",
    "LSB"
  };

  /** The scaling type for min/max range. */
  public static final int SCALING_RANGE = 0;

  /** The scaling type for factor/offset. */
  public static final int SCALING_FACTOR = 1;

  // Variables
  // ---------

  /** The text field for scaling factor. */
  private JTextField scaleField;

  /** The text field for add offset. */
  private JTextField offsetField;

  /** The text field for missing value. */
  private JTextField missingField;

  /** The text field for minimum value. */
  private JTextField minField;

  /** The text field for maximum value. */
  private JTextField maxField;

  /** The check box for dimension header. */
  private JCheckBox headerCheck;

  /** The combo box for data type. */
  private JComboBox dataTypeCombo;

  /** The radio button for current scaling. */
  private JRadioButton rangeRadio;

  /** The radio button for scaling factor/offset. */
  private JRadioButton scaleRadio;

  /** The combo box for byte order. */
  private JComboBox byteOrderCombo;

  /** The panel for scale factor/offset controls. */
  private JPanel scalePanel;

  /** The panel for range min/max controls. */
  private JPanel rangePanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the scaling factor and offset.
   * 
   * @param scaling the scaling factor and offset as <code>[factor,
   * offset]</code> (modified).
   */
  public void getScaling (
    double[] scaling
  ) {

    scaling[0] = Double.parseDouble (scaleField.getText());
    scaling[1] = Double.parseDouble (offsetField.getText());

  } // getScaling

  ////////////////////////////////////////////////////////////

  /** Gets the missing value. */
  public double getMissing () { 

    String text = missingField.getText();
    if (text.equalsIgnoreCase ("NaN")) return (Double.NaN);
    else return (Double.parseDouble (text));

  } // getMissing

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the range.
   * 
   * @param range the range as <code>[min,max]</code> (modified).
   */
  public void getRange (
    double[] range
  ) {

    range[0] = Double.parseDouble (minField.getText());
    range[1] = Double.parseDouble (maxField.getText());

  } // getRange

  ////////////////////////////////////////////////////////////

  /** Gets the dimension header flag. */
  public boolean getHeader () { return (headerCheck.isSelected()); }

  ////////////////////////////////////////////////////////////

  /** Gets the data type. */
  public int getDataType () { return (dataTypeCombo.getSelectedIndex()); }

  ////////////////////////////////////////////////////////////

  /** Gets the byte order. */
  public int getByteOrder () { return (byteOrderCombo.getSelectedIndex()); }

  ////////////////////////////////////////////////////////////

  /** Gets the scaling type. */
  public int getScalingType () { 

    int type;
    if (rangeRadio.isSelected()) type = SCALING_RANGE;
    else if (scaleRadio.isSelected()) type = SCALING_FACTOR;
    else throw new IllegalStateException ("No scaling type selected");

    return (type);

  } // getScalingType

  ////////////////////////////////////////////////////////////

  /** Creates the scale factor/offset panel. */
  private JPanel createScalePanel () {

    // Initialize
    // ----------
    JPanel scalePanel = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;

    // Add scale field
    // ---------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (0, 0, 0, 10);
    scalePanel.add (new JLabel ("Scale factor:"), gc);

    GUIServices.setConstraints (gc, 1, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (0, 0, 0, 0);
    scaleField = new JTextField (12);
    scalePanel.add (scaleField, gc);

    // Add offset field
    // ----------------
    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (0, 0, 0, 10);
    scalePanel.add (new JLabel ("Add offset:"), gc);

    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (0, 0, 0, 0);
    offsetField = new JTextField (12);
    scalePanel.add (offsetField, gc);

    return (scalePanel);

  } // createScalePanel

  ////////////////////////////////////////////////////////////

  /** Creates the range min/max panel. */
  private JPanel createRangePanel () {

    // Initialize
    // ----------
    JPanel rangePanel = new JPanel (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;

    // Add minimum field
    // -----------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (0, 0, 0, 10);
    rangePanel.add (new JLabel ("Minimum value:"), gc);

    GUIServices.setConstraints (gc, 1, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (0, 0, 0, 0);
    minField = new JTextField (12);
    rangePanel.add (minField, gc);

    // Add maximum field
    // -----------------
    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (0, 0, 0, 10);
    rangePanel.add (new JLabel ("Maximum value:"), gc);

    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    gc.insets = new Insets (0, 0, 0, 0);
    maxField = new JTextField (12);
    rangePanel.add (maxField, gc);

    return (rangePanel);

  } // createRangePanel

  ////////////////////////////////////////////////////////////

  /** Creates a new binary option panel. */
  public BinaryOptionPanel () {

    // Initialize
    // ----------
    super (new GridBagLayout());
    setBorder (new CompoundBorder (
      new TitledBorder (new EtchedBorder(), "Binary Options"),
      new EmptyBorder (4, 4, 4, 4)
    ));
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 0);
    ButtonGroup group = new ButtonGroup();

    // Add dimension header checkbox
    // -----------------------------
    GUIServices.setConstraints (gc, 0, 0, 2, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    headerCheck = new JCheckBox ("Write 72-bit dimension headers", false);
    this.add (headerCheck, gc);

    // Add data type combo
    // -------------------
    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Data type:"), gc);

    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.NONE, 0, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    dataTypeCombo = new JComboBox (DATA_TYPES);
    dataTypeCombo.addItemListener (new DataTypeListener());
    this.add (dataTypeCombo, gc);

    // Add byte order combo
    // --------------------
    GUIServices.setConstraints (gc, 0, 2, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Byte order:"), gc);

    GUIServices.setConstraints (gc, 1, 2, 1, 1, 
      GridBagConstraints.NONE, 0, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    byteOrderCombo = new JComboBox (BYTE_ORDERS);
    this.add (byteOrderCombo, gc);

    // Add missing field
    // -----------------
    GUIServices.setConstraints (gc, 0, 3, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Missing value:"), gc);

    GUIServices.setConstraints (gc, 1, 3, 1, 1, 
      GridBagConstraints.NONE, 0, 0);
    gc.insets = new Insets (2, 0, 2, 0);
    missingField = new JTextField (12);
    this.add (missingField, gc);

    // Add scale radio button
    // ----------------------
    GUIServices.setConstraints (gc, 0, 4, 2, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    scaleRadio = new JRadioButton ("Scale using factor and offset", true);
    group.add (scaleRadio);
    this.add (scaleRadio, gc);
    scaleRadio.addItemListener (new ItemListener() {
        public void itemStateChanged (ItemEvent e) {
          GUIServices.setContainerEnabled (scalePanel, 
            scaleRadio.isSelected());
        } // itemStateChanged
      });

    GUIServices.setConstraints (gc, 0, 5, 2, 1, 
      GridBagConstraints.NONE, 0, 0);
    scalePanel = createScalePanel();
    JPanel scaleContainerPanel = new JPanel (new BorderLayout());
    scaleContainerPanel.add (Box.createHorizontalStrut (20), 
      BorderLayout.WEST);
    scaleContainerPanel.add (scalePanel, BorderLayout.CENTER);
    this.add (scaleContainerPanel, gc);

    // Add range radio button
    // ----------------------
    GUIServices.setConstraints (gc, 0, 6, 2, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    rangeRadio = new JRadioButton ("Scale using range", false);
    group.add (rangeRadio);
    this.add (rangeRadio, gc);
    rangeRadio.addItemListener (new ItemListener() {
        public void itemStateChanged (ItemEvent e) {
          GUIServices.setContainerEnabled (rangePanel, 
            rangeRadio.isSelected());
        } // itemStateChanged
      });

    GUIServices.setConstraints (gc, 0, 7, 2, 1, 
      GridBagConstraints.NONE, 0, 0);
    rangePanel = createRangePanel();
    JPanel rangeContainerPanel = new JPanel (new BorderLayout());
    rangeContainerPanel.add (Box.createHorizontalStrut (20), 
      BorderLayout.WEST);
    rangeContainerPanel.add (rangePanel, BorderLayout.CENTER);
    GUIServices.setContainerEnabled (rangePanel, false);
    this.add (rangeContainerPanel, gc);

    // Set initial data type
    // ---------------------
    dataTypeCombo.setSelectedIndex (1);

  } // BinaryOptionPanel constructor

  ////////////////////////////////////////////////////////////

  /** Handles a change in data type. */
  private class DataTypeListener implements ItemListener {
    public void itemStateChanged (ItemEvent e) {

      // Modify text fields
      // ------------------
      int type = dataTypeCombo.getSelectedIndex();
      switch (type) {
      case TYPE_UBYTE:
        scaleField.setText ("0.1");
        offsetField.setText ("0");
        missingField.setText ("0");
        break;
      case TYPE_SHORT:
        scaleField.setText ("0.01");
        offsetField.setText ("0");
        missingField.setText ("-32768");
        break;
      case TYPE_FLOAT:
        missingField.setText ("NaN");
        break;
      } // switch

      // Enable/disable scaling and range
      // --------------------------------
      boolean isEnabled = (type != TYPE_FLOAT);
      scaleRadio.setEnabled (isEnabled);
      GUIServices.setContainerEnabled (scalePanel, 
        isEnabled && scaleRadio.isSelected());
      rangeRadio.setEnabled (isEnabled);
      GUIServices.setContainerEnabled (rangePanel, 
        isEnabled && rangeRadio.isSelected());

    } // itemStateChanged
  } // DataTypeListener class

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argv) {

    JPanel panel = new BinaryOptionPanel();
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // BinaryOptionPanel class

////////////////////////////////////////////////////////////////////////
