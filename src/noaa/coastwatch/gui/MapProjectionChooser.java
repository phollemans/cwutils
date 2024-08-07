////////////////////////////////////////////////////////////////////////
/*

     File: MapProjectionChooser.java
   Author: Peter Hollemans
     Date: 2002/12/03

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.GCTP;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.DatumFactory;
import noaa.coastwatch.util.trans.MapProjection;
import noaa.coastwatch.util.trans.MapProjectionFactory;

/**
 * The map projection chooser allows the display and selection of map
 * projection parameters.
 *
 * @see noaa.coastwatch.util.GCTP
 * @see noaa.coastwatch.util.trans.MapProjection
 * @see noaa.coastwatch.util.trans.MapProjectionFactory
 *
 * @author Peter Hollemans
 * @since 3.1.2
 */
public class MapProjectionChooser
  extends JPanel {

  // Constants
  // ---------
  /** The number of region fields in total. */
  private final static int REGION_FIELDS = 6;

  /** The indices of various region fields. */
  private final static int ROWS_FIELD = 0;
  private final static int COLS_FIELD = 1;
  private final static int CENTERLAT_FIELD = 2;
  private final static int CENTERLON_FIELD = 3;
  private final static int PIXELHEIGHT_FIELD = 4;
  private final static int PIXELWIDTH_FIELD = 5;

  /** The decimal format for doubles. */
  private static final DecimalFormat DBLFMT = new DecimalFormat ("0.######");

  // Variables
  // ---------    

  /** The region text fields. */
  private JTextField[] regionFields;

  /** The pixel size linked button. */
  private JCheckBox linkButton;

  /** The parameters panel. */
  private JPanel parametersPanel;

  /** The projection system combo box. */
  private JComboBox systemCombo;  

  /** The projection spheroid combo box. */
  private JComboBox spheroidCombo;

  /** The zone text field. */
  private JTextField zoneField;

  /** The parameters text fields. */
  private JTextField[] parameterFields;

  /** The hash set of ignored parameters. */
  private static HashSet ignoreSet;

  /** The pixel size label. */
  private JLabel pixelSizeLabel;

  /** 
   * The last spheroid selected.  This is used when the projection
   * system is changed to one that does not support a generic
   * spheroid.  The spheroid is saved and then recalled again when a
   * spheroid-capable projection system is selected.
   */
  private Integer lastSpheroid;

  /** The mapping of spheroid names to code values. */
  private static HashMap<String, Integer> spheroidCodeMap;

  ////////////////////////////////////////////////////////////

  static {

    // Build list of ignored GCTP parameter names
    // ------------------------------------------
    ignoreSet = new HashSet();
    ignoreSet.add ("SMajor");
    ignoreSet.add ("SMinor");
    ignoreSet.add ("Sphere");
    ignoreSet.add ("FE");
    ignoreSet.add ("FN");

    // Build map of spheroid names to codes
    // ------------------------------------
    spheroidCodeMap = new HashMap<String, Integer>();
    for (int spheroid = 0; spheroid < GCTP.MAX_SPHEROIDS; spheroid++)
      spheroidCodeMap.put (GCTP.SPHEROID_NAMES[spheroid], spheroid);
    
  } // static

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected projection system. */
  public int getSystem () { return (systemCombo.getSelectedIndex()); }

  ////////////////////////////////////////////////////////////

  /** Sets the projection system. */
  public void setSystem (
    int system
  ) {

    // Check system code
    // -----------------
    if (system < 0 || system > GCTP.MAX_PROJECTIONS-1) return;

    // Set system
    // ----------
    systemCombo.setSelectedIndex (system);

  } // setSystem

  ////////////////////////////////////////////////////////////

  /** Updates the projection system parameters. */
  private void updateSystemParameters () {

    // Remove elements
    // ---------------
    parametersPanel.removeAll();
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, 0, GridBagConstraints.RELATIVE, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);

    // Add zone
    // --------
    int system = getSystem();
    boolean needZone = (system == GCTP.SPCS || system == GCTP.UTM);
    if (needZone) { 
      parametersPanel.add (new JLabel ("Zone:"), gc);
      zoneField = new JTextField();
      parametersPanel.add (zoneField, gc);
    } // if
    else zoneField = null;

    // Loop over each parameter
    // ------------------------
    GCTP.Requirements require = GCTP.getRequirements(system)[0];
    for (int i = 0; i < GCTP.Requirements.MAX_PARAMETERS; i++) {

      // Check if parameter is required and not ignored
      // ----------------------------------------------
      if (require.isRequired (i) && 
        !ignoreSet.contains (require.getShortDescription (i))) {

        // Add parameter fields
        // --------------------
        String desc = require.getDescription (i);
        String units = require.getUnits (i);
        String labelText = desc + (!units.equals ("") ? " (" + units + ")" :
          "") + ":";
        parametersPanel.add (new JLabel (labelText), gc);
        parameterFields[i] = new JTextField();
        parametersPanel.add (parameterFields[i], gc);

      } // if
      else parameterFields[i] = null;
    } // for

    GUIServices.setConstraints (gc, 0, GridBagConstraints.RELATIVE, 1, 1, 
      GridBagConstraints.BOTH, 1, 1);
    parametersPanel.add (new JLabel(), gc);

    // Set pixel size label
    // --------------------
    if (system == GCTP.GEO) pixelSizeLabel.setText ("Pixel size (degrees):");
    else pixelSizeLabel.setText ("Pixel size (kilometers):");

    // Perform layout
    // --------------
    validate();

  } // updateSystemParameters

  ////////////////////////////////////////////////////////////

  /**
   * Gets the currently selected spheroid.
   *
   * @return the index of the currently selected spheroid, or -1
   * if no spheroid is selected.
   */
  public int getSpheroid () {
    
    int spheroid;
    if (spheroidCombo.getItemCount() == 0) spheroid = -1;
    else {
      String item = (String) spheroidCombo.getSelectedItem();
      spheroid = spheroidCodeMap.get (item);
    } // else
    
    return (spheroid);
    
  } // getSpheroid

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the projection spheroid.
   *
   * @throws IllegalArgumentException if the spheroid code is out of range,
   * or invalid for the current projection system.
   */
  public void setSpheroid (
    int spheroid
  ) {

    // Check spheroid code
    // -------------------
    if (spheroid < 0 || spheroid > GCTP.MAX_SPHEROIDS-1)
      throw new IllegalArgumentException();
    if (!GCTP.isSupportedSpheroid (spheroid, getSystem()))
      throw new IllegalArgumentException();

    // Set spheroid
    // ------------
    spheroidCombo.setSelectedItem (GCTP.SPHEROID_NAMES[spheroid]);

  } // setSpheroid

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected zone. */
  public int getZone () throws NumberFormatException { 

    if (zoneField == null) return (0);
    int zone = Integer.parseInt (zoneField.getText());
    return (zone);

  } // getZone

  ////////////////////////////////////////////////////////////

  /** Sets the projection zone. */
  public void setZone (
    int zone
  ) {

    if (zoneField != null) {
      zoneField.setText (Integer.toString (zone));
    } // if

  } // setZone

  ////////////////////////////////////////////////////////////

  /** Gets the current projection parameters. */
  public double[] getParameters () throws NumberFormatException {

    // Get requirements
    // ----------------
    GCTP.Requirements require = GCTP.getRequirements (getSystem())[0];

    // Get parameters
    // --------------
    double[] parameters = new double[GCTP.Requirements.MAX_PARAMETERS];
    for (int i = 0; i < GCTP.Requirements.MAX_PARAMETERS; i++) {

      // Set zero parameter
      // ------------------
      if (parameterFields[i] == null) parameters[i] = 0;

      // Set non-zero parameter with angle conversion
      // --------------------------------------------
      else {
        parameters[i] = Double.parseDouble (parameterFields[i].getText());
        if (require.getUnits(i).equals ("degrees")) {
          parameters[i] = GCTP.pack_angle (parameters[i]);
        } // if
      } // else

    } // for

    return (parameters);

  } // getParameters

  ////////////////////////////////////////////////////////////

  /** Sets the projection parameters. */
  public void setParameters (
    double[] parameters
  ) {

    // Set parameters
    // --------------
    GCTP.Requirements require = GCTP.getRequirements (getSystem())[0];
    for (int i = 0; i < parameters.length; i++) {
      if (parameterFields[i] != null) {
        String text;
        if (require.getUnits(i).equals ("degrees"))
          text = DBLFMT.format (GCTP.unpack_angle (parameters[i]));
        else
          text = DBLFMT.format (parameters[i]);        
        parameterFields[i].setText (text);
      } // if
    } // for

  } // setParameters

  ////////////////////////////////////////////////////////////

  /** Gets the current dimensions. */
  public int[] getDimensions () throws NumberFormatException { 

    int[] dims = new int[] {
      Integer.parseInt (regionFields[ROWS_FIELD].getText()),
      Integer.parseInt (regionFields[COLS_FIELD].getText())
    };
    return (dims);

  } // getDimensions

  ////////////////////////////////////////////////////////////

  /** Sets the dimensions. */
  public void setDimensions (
    int[] dims
  ) {

    regionFields[ROWS_FIELD].setText (Integer.toString (dims[Grid.ROWS]));
    regionFields[COLS_FIELD].setText (Integer.toString (dims[Grid.COLS]));

  } // setDimensions

  ////////////////////////////////////////////////////////////

  /** Gets the current center earth location. */
  public EarthLocation getCenter () throws NumberFormatException { 

    EarthLocation center = new EarthLocation (
      Double.parseDouble (regionFields[CENTERLAT_FIELD].getText()),
      Double.parseDouble (regionFields[CENTERLON_FIELD].getText()),
      DatumFactory.create (getSpheroid())
    );
    return (center);

  } // getCenter

  ////////////////////////////////////////////////////////////

  /** Sets the projection center earth location. */
  public void setCenter (
    EarthLocation center
  ) {

    regionFields[CENTERLAT_FIELD].setText (DBLFMT.format (center.lat));
    regionFields[CENTERLON_FIELD].setText (DBLFMT.format (center.lon));

  } // setCenter

  ////////////////////////////////////////////////////////////

  /** Gets the current pixel dimensions. */
  public double[] getPixelDimensions () throws NumberFormatException { 

    double factor = (getSystem() == GCTP.GEO ? 1 : 1e3);
    double[] pixelDims = new double[] {
      Double.parseDouble (regionFields[PIXELHEIGHT_FIELD].getText())*factor,
      Double.parseDouble (regionFields[PIXELWIDTH_FIELD].getText())*factor
    };
    return (pixelDims);

  } // getPixelDimensions

  ////////////////////////////////////////////////////////////

  /** Sets the projection pixel dimensions. */
  public void setPixelDimensions (
    double[] pixelDims
  ) {

    double factor = (getSystem() == GCTP.GEO ? 1 : 1e3);
    regionFields[PIXELHEIGHT_FIELD].setText (DBLFMT.format (
      pixelDims[0]/factor));
    regionFields[PIXELWIDTH_FIELD].setText (DBLFMT.format (
      pixelDims[1]/factor));
    if (pixelDims[0] == pixelDims[1]) linkButton.setSelected (true);

  } // setPixelDimensions

  ////////////////////////////////////////////////////////////

  /** Gets the currently selected map projection. */
  public MapProjection getMapProjection () 
    throws NumberFormatException, NoninvertibleTransformException { 

    return (MapProjectionFactory.getInstance().create (
      getSystem(), getZone(), getParameters(), getSpheroid(), 
      getDimensions(), getCenter(), getPixelDimensions()));

  } // getMapProjection

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the projection parameters from the specified projection and
   * dimensions.
   *
   * @param proj the map projection.
   * @param dims the region dimensions.
   */
  public void setMapProjection (
    MapProjection proj,
    int[] dims
  ) {

    setSystem (proj.getSystem());
    setSpheroid (proj.getSpheroid());
    setZone (proj.getZone());
    setParameters (proj.getParameters());
    setDimensions (dims);
    setCenter (proj.transform (new DataLocation ((dims[Grid.ROWS]-1)/2.0,
      (dims[Grid.COLS]-1)/2.0)));
    setPixelDimensions (proj.getPixelDimensions());

  } // setMapProjection

  ////////////////////////////////////////////////////////////

  /** Handles the link button change. */
  private class LinkChanged
    implements ItemListener {
    public void itemStateChanged (ItemEvent event) {

      // Set pixel width text
      // --------------------
      if (linkButton.isSelected()) {
        regionFields[PIXELWIDTH_FIELD].setText (
          regionFields[PIXELHEIGHT_FIELD].getText());
      } // if
 
    } // itemStateChanged
  } // LinkChanged class

  ////////////////////////////////////////////////////////////

  /** Handles the change in pixel size width or height. */
  private class PixelSizeChanged 
    implements DocumentListener {

    // Variables
    // ---------
    /** The updating flag. */
    private boolean updating;

    ////////////////////////////////////////////////////////

    public void changedUpdate (DocumentEvent e) { }
    public void insertUpdate (DocumentEvent e) { updateLinked (e); }
    public void removeUpdate (DocumentEvent e) { updateLinked (e); }

    ////////////////////////////////////////////////////////

    /** Checks that the pixel dimensions are linked correctly. */

    private void updateLinked (
      DocumentEvent event
    ) {

      // Check fields are linked
      // -----------------------
      if (!linkButton.isSelected()) return;

      // Check for updating
      // ------------------
      if (updating) return;

      // Get fields
      // ----------
      JTextField field, other;
      Document doc = event.getDocument();
      if (doc == regionFields[PIXELHEIGHT_FIELD].getDocument()) {
        field = regionFields[PIXELHEIGHT_FIELD];
        other = regionFields[PIXELWIDTH_FIELD];
      } // if
      else {
        field = regionFields[PIXELWIDTH_FIELD];
        other = regionFields[PIXELHEIGHT_FIELD];
      } // else

      // Update other field
      // ------------------
      updating = true;
      other.setText (field.getText());
      updating = false;

    } // updateLinked

    ////////////////////////////////////////////////////////

  } // PixelSizeChanged class

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the list of spheroids for the current projection system.
   * Not all spheroids are supported by each projection.
   */
  private void updateSpheroidList () {
  
    int currentSpheroid = getSpheroid();
  
    // Populate the list from scratch
    // ------------------------------
    int system = getSystem();
    spheroidCombo.removeAllItems();
    for (int spheroid = 0; spheroid < GCTP.MAX_SPHEROIDS; spheroid++) {
      if (GCTP.isSupportedSpheroid (spheroid, system))
        spheroidCombo.addItem (GCTP.SPHEROID_NAMES[spheroid]);
    } // for

    // Restore the last spheroid
    // -------------------------
    if (currentSpheroid != -1) {

      if (spheroidCombo.getItemCount() != GCTP.MAX_SPHEROIDS) {
        if (lastSpheroid == null)
          lastSpheroid = Integer.valueOf (currentSpheroid);
      } // if
      else {
        if (lastSpheroid != null) {
          setSpheroid (lastSpheroid.intValue());
          lastSpheroid = null;
        } // if
        else {
          setSpheroid (currentSpheroid);
        } // else
      } // else

    } // if

  } // updateSpheroidList

  ////////////////////////////////////////////////////////////

  /** Handles a projection system change. */
  private class SystemChanged extends AbstractAction {
    public void actionPerformed (ActionEvent e) {

      updateSystemParameters();
      updateSpheroidList();

    } // actionPerformed
  } // SystemChanged class

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new map projection chooser.
   *
   * @param proj the initial map projection.
   * @param dims the initial dimensions as [rows, columns].
   */
  public MapProjectionChooser (
    MapProjection proj,
    int[] dims
  ) {

    // Call super
    // ----------
    super (new GridBagLayout());

    // Create projection panel
    // -----------------------
    JPanel projPanel = new JPanel (new GridBagLayout());
    projPanel.setBorder (new TitledBorder (new EtchedBorder(), "Projection"));
    GridBagConstraints gc = new GridBagConstraints();
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.BOTH, 1, 0);
    this.add (projPanel, gc);

    JPanel fixedPanel = new JPanel (new GridLayout (5, 1));
    fixedPanel.add (new JLabel ("System:"));
    systemCombo = new JComboBox (GCTP.PROJECTION_NAMES);
    systemCombo.addActionListener (new SystemChanged());
    fixedPanel.add (systemCombo);
    fixedPanel.add (new JLabel ("Spheroid:"));
    spheroidCombo = new JComboBox();
    fixedPanel.add (spheroidCombo);
    fixedPanel.add (new JLabel ("Parameters:"));
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    projPanel.add (fixedPanel, gc);

    parametersPanel = new JPanel (new GridBagLayout());
    JScrollPane scrollPane = new JScrollPane (parametersPanel);
    int height = (int) new JLabel ("Test").getPreferredSize().getHeight();
    scrollPane.getViewport().setPreferredSize (new Dimension (10, height*6));
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.BOTH, 1, 1);
    projPanel.add (scrollPane, gc);
    parameterFields = new JTextField[GCTP.Requirements.MAX_PARAMETERS];

    // Create region panel
    // ---------------------
    JPanel regionPanel = new JPanel (new GridBagLayout());
    regionPanel.setBorder (new TitledBorder (new EtchedBorder(), "Region"));
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.BOTH, 1, 0);
    this.add (regionPanel, gc);
    regionFields = new JTextField[REGION_FIELDS];

    GUIServices.setConstraints (gc, 0, 0, 2, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (new JLabel ("Dimensions (pixels):"), gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (new JLabel ("Rows"), gc);

    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (new JLabel ("Columns"), gc);

    regionFields[ROWS_FIELD] = new JTextField();
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (regionFields[ROWS_FIELD], gc);

    regionFields[COLS_FIELD] = new JTextField();
    GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (regionFields[COLS_FIELD], gc);

    GUIServices.setConstraints (gc, 0, 3, 2, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (new JLabel ("Center location (degrees):"), gc);

    GUIServices.setConstraints (gc, 0, 4, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (new JLabel ("Latitude"), gc);

    GUIServices.setConstraints (gc, 1, 4, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (new JLabel ("Longitude"), gc);

    regionFields[CENTERLAT_FIELD] = new JTextField();
    GUIServices.setConstraints (gc, 0, 5, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (regionFields[CENTERLAT_FIELD], gc);

    regionFields[CENTERLON_FIELD] = new JTextField();
    GUIServices.setConstraints (gc, 1, 5, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (regionFields[CENTERLON_FIELD], gc);

    GUIServices.setConstraints (gc, 0, 6, 2, 1, GridBagConstraints.BOTH, 1, 1);
    pixelSizeLabel = new JLabel ("Pixel size:");
    regionPanel.add (pixelSizeLabel, gc);

    GUIServices.setConstraints (gc, 0, 7, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (new JLabel ("Height"), gc);

    GUIServices.setConstraints (gc, 1, 7, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (new JLabel ("Width"), gc);

    regionFields[PIXELHEIGHT_FIELD] = new JTextField();
    DocumentListener pixel = new PixelSizeChanged();
    regionFields[PIXELHEIGHT_FIELD].getDocument().addDocumentListener (pixel);
    GUIServices.setConstraints (gc, 0, 8, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (regionFields[PIXELHEIGHT_FIELD], gc);

    regionFields[PIXELWIDTH_FIELD] = new JTextField();
    regionFields[PIXELWIDTH_FIELD].getDocument().addDocumentListener (pixel);
    GUIServices.setConstraints (gc, 1, 8, 1, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (regionFields[PIXELWIDTH_FIELD], gc);

    linkButton = new JCheckBox ("Use square pixels");
    linkButton.addItemListener (new LinkChanged());
    GUIServices.setConstraints (gc, 0, 9, 2, 1, GridBagConstraints.BOTH, 1, 1);
    regionPanel.add (linkButton, gc);

    // Set map projection
    // ------------------
    setMapProjection (proj, dims);

  } // MapProjectionChooser constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a dialog showing the projection chooser panel and OK/Cancel
   * buttons.
   *
   * @param frame the parent frame for the dialog.
   * @param title the dialog window title.
   *
   * @return true if the user pressed OK, false otherwise.
   */
  public boolean showDialog (
    Frame frame,
    String title
  ) {

    // Create map projection chooser dialog
    // ------------------------------------
    final JOptionPane optionPane = new JOptionPane (this,
      JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
    final JDialog dialog = new JDialog (frame, title, true);
    dialog.setContentPane (optionPane);

    // Add input check to OK option
    // ----------------------------
    dialog.setDefaultCloseOperation (JDialog.DO_NOTHING_ON_CLOSE);
    optionPane.addPropertyChangeListener (
      new PropertyChangeListener() {
        public void propertyChange (PropertyChangeEvent e) {
          String prop = e.getPropertyName();
          if (dialog.isVisible() && (e.getSource() == optionPane)
            && (prop.equals (JOptionPane.VALUE_PROPERTY) ||
            prop.equals (JOptionPane.INPUT_VALUE_PROPERTY))) {

            // Get option value
            // ----------------
            int value = ((Integer)optionPane.getValue()).intValue();

            // Check for input error
            // ---------------------
            if (value == JOptionPane.OK_OPTION) {
              try { 
                MapProjection newProj = getMapProjection(); 
                dialog.setVisible (false);
              } // try
              catch (Exception error) {            
                JOptionPane.showMessageDialog (dialog, 
                  "A problem occurred parsing the projection parameters.\n" + 
                  error.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                optionPane.setValue (Integer.valueOf (JOptionPane.DEFAULT_OPTION));
                return;
              } // catch
            } // if
          
            // Close dialog
            // ------------
            else if (value == JOptionPane.CANCEL_OPTION) {
              dialog.setVisible (false);
            } // else if

          } // if

        } // propertyChange
      } // PropertyChangeListener class
    );

    // Pack and set location
    // ---------------------
    dialog.pack();
    if (frame != null) {
      Rectangle frameRect = frame.getBounds();
      Rectangle dialogRect = dialog.getBounds();
      int x = frameRect.x + frameRect.width/2 - dialogRect.width/2;
      if (x < 0) x = 0;
      int y = frameRect.y + frameRect.height/2 - dialogRect.height/2;
      if (y < 0) y = 0;
      dialog.setLocation (x, y);
    } // if     
    dialog.setVisible (true);

    // Return value
    // ------------
    int value = ((Integer)optionPane.getValue()).intValue();
    return (value == JOptionPane.OK_OPTION);

  } // showDialog

  ////////////////////////////////////////////////////////////

} // MapProjectionChooser class

////////////////////////////////////////////////////////////////////////
