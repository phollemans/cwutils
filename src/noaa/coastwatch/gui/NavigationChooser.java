////////////////////////////////////////////////////////////////////////
/*
     FILE: NavigationChooser.java
  PURPOSE: Allows the modification of navigation corrections.
   AUTHOR: Peter Hollemans
     DATE: 2004/06/07
  CHANGES: 2006/03/15, PFH, modified to use GUIServices.getIconToggle() and
             JToolBar for the navigation mode buttons
           2012/12/04, PFH, updated to use getSelectedValuesList()

  CoastWatch Software Library and Utilities
  Copyright 2004-2012, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.io.*;
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import java.util.List;
import noaa.coastwatch.render.*;
import noaa.coastwatch.util.*;
import noaa.coastwatch.io.*;

/**
 * The <code>NavigationChooser</code> class allows the user to modify
 * the navigation affine transform for a set of Earth data
 * variables.  The user may use a visual correction mode, or a manual
 * correction mode.<p>
 *
 * The chooser signals a change in the visual navigation correction
 * mode by firing a <code>PropertyChangeEvent</code> whose property
 * name is <code>NAVIGATION_MODE_PROPERTY</code> and value is
 * <code>TRANSLATION</code> or <code>ROTATION</code>.  The user object
 * should perform a correction of the indicated type.<p>
 *
 * The chooser signals a manual navigation correction by firing a
 * <code>PropertyChangeEvent</code> whose property name is
 * <code>AFFINE_PROPERTY</code> and value is the affine transform to
 * apply.  If the affine transform to apply is null, then the
 * navigation transform for the user-specified list of variables
 * should be reset.<p>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class NavigationChooser
  extends JPanel
  implements TabComponent {

  // Constants
  // ---------

  /** The navigation mode property. */
  public static final String NAVIGATION_MODE_PROPERTY = "navigationMode";

  /** The navigation affine property. */
  public static final String AFFINE_PROPERTY = "affine";

  /** The translation navigation mode. */
  public static final String TRANSLATION = "Translation";
  
  /** The rotation navigation mode. */
  public static final String ROTATION = "Rotation";

  /** The navigation tooltip. */
  private static final String NAVIGATION_TOOLTIP = "Navigation Correction";

  // Variables
  // ---------    

  /** The translation mode toggle button. */
  private JToggleButton translationButton;

  /** The rotation mode toggle button. */
  private JToggleButton rotationButton;

  /** The invisible button used for turning off the button group. */
  private JToggleButton hidden;

  /** The affine parameter fields. */
  private JTextField[] affineFields;

  /** The list of variable names to apply the transform to. */
  private JList variableList;

  /** The button group for mode buttons. */
  private ButtonGroup modeGroup;

  /** The reset radio button. */
  private JRadioButton resetRadio;

  /** The panel for translation parameters. */
  private JPanel translationPanel;

  /** The translation radio button. */
  private JRadioButton translationRadio;

  /** The panel for rotation parameters. */
  private JPanel rotationPanel;

  /** The rotation radio button. */
  private JRadioButton rotationRadio;

  /** The panel for generic affine parameters. */
  private JPanel affinePanel;

  /** The affine radio button. */
  private JRadioButton affineRadio;

  /** The translation rows text field. */
  private JTextField rowsField;

  /** The translation columns text field. */
  private JTextField colsField;

  /** The rotation center row text field. */
  private JTextField centerRowField;

  /** The rotation center column text field. */
  private JTextField centerColField;

  /** The rotation angle text field. */
  private JTextField angleField;

  ////////////////////////////////////////////////////////////

  /** Gets the navigation mode, or null if no mode is selected. */
  public String getMode () { 

    ButtonModel model = modeGroup.getSelection();
    if (model != null)
      return (model.getActionCommand());
    else
      return (null);

  } // getMode

  ////////////////////////////////////////////////////////////

  /** Gets the list of selected variable names. */
  public List getVariables () { 

    return (variableList.getSelectedValuesList());

  } // getVariables

  ////////////////////////////////////////////////////////////

  /** 
   * Deactivates the navigation so that no navigation mode is
   * selected. 
   */
  public void deactivate () { hidden.setSelected (true); }

  ////////////////////////////////////////////////////////////

  /**  
   * Creates a new navigation chooser panel.
   *
   * @param variableNames the list of variable names to make available.
   */  
  public NavigationChooser (
    List variableNames
  ) {

    // Initialize
    // ----------
    super (new GridBagLayout());

    // Create visual correction panel
    // ------------------------------
    JToolBar visualPanel = new JToolBar();
    visualPanel.setFloatable (false);
    visualPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Visual Correction"));
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    this.add (visualPanel, gc);
    
    // Add mode buttons
    // ----------------
    ActionListener modeListener = new NavigationModeListener();
    modeGroup = new ButtonGroup();
    translationButton = GUIServices.getIconToggle ("correction.translate");
    translationButton.setActionCommand (TRANSLATION);
    translationButton.addActionListener (modeListener);
    translationButton.setToolTipText (TRANSLATION);
    modeGroup.add (translationButton);
    visualPanel.add (translationButton);

    rotationButton = GUIServices.getIconToggle ("correction.rotate");
    rotationButton.setActionCommand (ROTATION);
    rotationButton.addActionListener (modeListener);
    rotationButton.setToolTipText (ROTATION);
    modeGroup.add (rotationButton);
    visualPanel.add (rotationButton);

    hidden = new JToggleButton();
    modeGroup.add (hidden);

    // Create manual correction panel
    // ------------------------------
    JPanel manualPanel = new JPanel (new GridBagLayout());
    manualPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Manual Correction"));
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    this.add (manualPanel, gc);

    // Add reset option
    // ----------------
    ButtonGroup manualGroup = new ButtonGroup();
    resetRadio = new JRadioButton ("Reset correction to identity");
    manualGroup.add (resetRadio);
    GUIServices.setConstraints (gc, 0, 0, 2, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    manualPanel.add (resetRadio, gc);

    // Add translation option
    // ----------------------
    translationRadio = new JRadioButton ("Apply translation transform:");
    translationRadio.addItemListener (new ItemListener() {
        public void itemStateChanged (ItemEvent e) {
          GUIServices.setContainerEnabled (translationPanel, 
            translationRadio.isSelected());
        } // itemStateChanged
      });
    manualGroup.add (translationRadio);
    GUIServices.setConstraints (gc, 0, 1, 2, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    manualPanel.add (translationRadio, gc);

    // Add translation panel
    // ---------------------
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.NONE,
      0, 0);
    manualPanel.add (Box.createHorizontalStrut (20), gc);
    translationPanel = new JPanel (new GridBagLayout());
    GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    manualPanel.add (translationPanel, gc);

    // Add translation fields
    // ----------------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.NONE,
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    translationPanel.add (new JLabel ("Rows"), gc);
    rowsField = new JTextField (12);
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    translationPanel.add (rowsField, gc);
    
    GUIServices.setConstraints (gc, 2, 0, 1, 1, GridBagConstraints.NONE,
      0, 0);
    translationPanel.add (new JLabel ("Columns"), gc);
    colsField = new JTextField (12);
    GUIServices.setConstraints (gc, 3, 0, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    translationPanel.add (colsField, gc);
    gc.insets = new Insets (0,0,0,0);

    // Add rotation option
    // -------------------
    rotationRadio = new JRadioButton ("Apply rotation transform:");
    rotationRadio.addItemListener (new ItemListener() {
        public void itemStateChanged (ItemEvent e) {
          GUIServices.setContainerEnabled (rotationPanel, 
            rotationRadio.isSelected());
        } // itemStateChanged
      });
    manualGroup.add (rotationRadio);
    GUIServices.setConstraints (gc, 0, 3, 2, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    manualPanel.add (rotationRadio, gc);

    // Add rotation panel
    // ---------------------
    GUIServices.setConstraints (gc, 0, 4, 1, 1, GridBagConstraints.NONE,
      0, 0);
    manualPanel.add (Box.createHorizontalStrut (20), gc);
    rotationPanel = new JPanel (new GridBagLayout());
    GUIServices.setConstraints (gc, 1, 4, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    manualPanel.add (rotationPanel, gc);

    // Add rotation fields
    // -------------------
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.NONE,
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    rotationPanel.add (new JLabel ("Center row"), gc);
    centerRowField = new JTextField (12);
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    rotationPanel.add (centerRowField, gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.NONE,
      0, 0);
    rotationPanel.add (new JLabel ("Center column"), gc);
    centerColField = new JTextField (12);
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    rotationPanel.add (centerColField, gc);

    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.NONE,
      0, 0);
    rotationPanel.add (new JLabel ("Angle (degrees)"), gc);
    angleField = new JTextField (12);
    GUIServices.setConstraints (gc, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    rotationPanel.add (angleField, gc);
    gc.insets = new Insets (0,0,0,0);

    // Add affine option
    // -----------------
    affineRadio = new JRadioButton ("Apply general affine transform:");
    affineRadio.addItemListener (new ItemListener() {
        public void itemStateChanged (ItemEvent e) {
          GUIServices.setContainerEnabled (affinePanel, 
            affineRadio.isSelected());
        } // itemStateChanged
      });
    manualGroup.add (affineRadio);
    GUIServices.setConstraints (gc, 0, 5, 2, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    manualPanel.add (affineRadio, gc);

    // Add affine panel
    // ----------------
    GUIServices.setConstraints (gc, 0, 6, 1, 1, GridBagConstraints.NONE,
      0, 0);
    manualPanel.add (Box.createHorizontalStrut (20), gc);
    affinePanel = new JPanel (new GridBagLayout());
    GUIServices.setConstraints (gc, 1, 6, 1, 1, GridBagConstraints.HORIZONTAL,
      1, 0);
    manualPanel.add (affinePanel, gc);

    // Add affine fields
    // -----------------
    affineFields = new JTextField[6];
    int index = 0;
    Insets labelInsets = new Insets (2, 0, 2, 10);
    Insets fieldInsets = new Insets (2, 0, 2, 2);
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 2; j++) {
        JLabel label = new JLabel (Character.toString ((char) ('a' + index)));
        GUIServices.setConstraints (gc, i*2, j, 1, 1, 
          GridBagConstraints.HORIZONTAL, 0, 0);
        gc.insets = labelInsets;
        affinePanel.add (label, gc);
        affineFields[index] = new JTextField (1);
        GUIServices.setConstraints (gc, i*2+1, j, 1, 1, 
          GridBagConstraints.HORIZONTAL, 1, 0);
        gc.insets = fieldInsets;
        affinePanel.add (affineFields[index], gc);
        index++;
      } // for
    } // for
    gc.insets = new Insets (0,0,0,0);

    // Add button panel
    // ----------------
    JPanel buttonPanel = new JPanel (new GridLayout (1, 0, 2, 2));
    GUIServices.setConstraints (gc, 0, 7, 2, 1, GridBagConstraints.NONE, 0, 0);
    gc.insets = new Insets (2, 2, 2, 2);
    manualPanel.add (buttonPanel, gc);
    gc.insets = new Insets (0,0,0,0);

    // Add buttons
    // -----------
    JButton performButton = new JButton ("Perform");
    performButton.addActionListener (new ManualButtonListener());
    buttonPanel.add (performButton);

    // Create list panel
    // -----------------
    JPanel listPanel = new JPanel (new BorderLayout());
    listPanel.setBorder (new TitledBorder (new EtchedBorder(), 
      "Correction Variables"));
    GUIServices.setConstraints (gc, 0, 2, 1, 1, GridBagConstraints.BOTH, 1, 1);
    this.add (listPanel, gc);

    // Add variable list
    // -----------------
    variableList = new JList (variableNames.toArray());
    variableList.setSelectionInterval (0, variableNames.size()-1);
    listPanel.add (new JScrollPane (variableList), BorderLayout.CENTER);

    // Set initial manual selection
    // ----------------------------
    resetRadio.setSelected (true);
    GUIServices.setContainerEnabled (translationPanel, false);
    GUIServices.setContainerEnabled (rotationPanel, false);
    GUIServices.setContainerEnabled (affinePanel, false);

  } // NavigationChooser constructor

  ////////////////////////////////////////////////////////////

  /** Handles manual button events. */
  private class ManualButtonListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      AffineTransform affine = null;

      try {

        // Translation correction
        // ----------------------
        if (translationRadio.isSelected()) {
          double rows = Double.parseDouble (rowsField.getText());
          double cols = Double.parseDouble (colsField.getText());
          affine = AffineTransform.getTranslateInstance (rows, cols);
        } // if

        // Rotation correction
        // -------------------
        else if (rotationRadio.isSelected()) {
          double centerRow = Double.parseDouble (centerRowField.getText());
          double centerCol = Double.parseDouble (centerColField.getText());
          double angle = Double.parseDouble (angleField.getText());
          affine = AffineTransform.getRotateInstance (Math.toRadians (angle), 
            centerRow, centerCol);
        } // if

        // Affine correction
        // -----------------
        else if (affineRadio.isSelected()) {
          double[] matrix = new double[6];
          for (int i = 0; i < 6; i++) 
            matrix[i] = Double.parseDouble (affineFields[i].getText());
          affine = new AffineTransform (matrix);
        } // if

      } // try
      catch (NumberFormatException e) {
        JOptionPane.showMessageDialog (NavigationChooser.this, 
          "An error has been detected in the correction parameters:\n" + 
          e.toString() +"\nPlease correct the problem and try again.", 
          "Error", JOptionPane.ERROR_MESSAGE);
        return;
      } // catch

      // Fire change event
      // -----------------
      firePropertyChange (AFFINE_PROPERTY, null, affine);

    } // actionPerformed
  } // ManualButtonListener class

  ////////////////////////////////////////////////////////////

  /** Handles navigation mode events. */
  private class NavigationModeListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String mode = event.getActionCommand();
      firePropertyChange (NAVIGATION_MODE_PROPERTY, null, mode);

    } // actionPerformed
  } // NavigationModeListener class

  ////////////////////////////////////////////////////////////

  /** Gets the navigation chooser tab icon. */
  public Icon getIcon () {

    return (GUIServices.getIcon ("correction.tab"));

  } // getIcon

  ////////////////////////////////////////////////////////////

  /** Gets the navigation chooser tooltip. */
  public String getToolTip () { return (NAVIGATION_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  /** Gets the navigation chooser title. */
  public String getTitle () { return (null); }

  ////////////////////////////////////////////////////////////

} // NavigationChooser class

////////////////////////////////////////////////////////////////////////
