////////////////////////////////////////////////////////////////////////
/*

     File: PreferencesChooser.java
   Author: Peter Hollemans
     Date: 2004/05/21

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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.font.TextLayout;

import java.util.Iterator;
import java.util.List;

import java.io.File;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.FileTransferHandler;
import noaa.coastwatch.render.ColorEnhancementSettings;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.EnhancementFunctionFactory;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.LogEnhancement;
import noaa.coastwatch.render.GammaEnhancement;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.render.StepEnhancement;
import noaa.coastwatch.render.IconElement;
import noaa.coastwatch.render.IconElementFactory;
import noaa.coastwatch.render.GraphicsServices;
import noaa.coastwatch.tools.Preferences;
import noaa.coastwatch.tools.ResourceManager;

import java.util.logging.Logger;

/**
 * The <code>PreferencesChooser</code> class is a panel that displays
 * a <code>Preferences</code> object and allows the preferences to be
 * manipulated.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class PreferencesChooser
  extends JPanel {

  private static final Logger LOGGER = Logger.getLogger (PreferencesChooser.class.getName());

  // Constants
  // ---------

  /** The category name for general settings. */
  private static final String GENERAL_CATEGORY = "General";

  /** The category name for enhancement settings. */
  private static final String ENHANCEMENT_CATEGORY = "Enhancement";

  /** The category name for export settings. */
  private static final String EXPORT_CATEGORY = "Export";

  /** The constant for no units conversion. */
  private static final String NO_UNITS_PREF = "FROM DATA";

  /** Common units of measure. */
  private static final String[] COMMON_UNITS = new String[] {
    "Select units ...",
    "-- TEMPERATURE --",
    "celsius",
    "kelvin",
    "fahrenheit",
    "-- CONCENTRATION --",
    "mg m^-3",
    "kg m^-3",
    "-- SPEED --", 
    "m/s",
    "km/h",
    "mi/h",
    "kt",
    "-- ANGLE --",
    "degrees",
    "radians",
    "-- PRESSURE --",
    "Pa",
    "mmHg",
    "atm",
    "-- TIME --",
    "sec",
    "min",
    "hr",
    "days",
    "months",
    "years",
    "-- DISTANCE --",
    "cm",
    "m",
    "km",
    "in",
    "ft",
    "mi",
    "nmi"
  };

  // Variables
  // ---------
  
  /** The preferences object displayed by this panel. */
  private Preferences prefs;

  /** The change flag, true if the original preferences have been changed. */
  private boolean prefsChanged;

  /** The enhancement preferences panel. */
  private EnhancementPreferencesChooser enhancePrefsChooser;

  /** The general preferences chooser. */
  private GeneralPreferencesChooser generalPrefsChooser;

  /** The file chooser to use for logos. */
  private static JFileChooser fileChooser;

  // The category selected in the preferences chooser.
  private static String selectedCategory = GENERAL_CATEGORY;

  // The card layout and cards that show each chooser under a toggle button.
  private CardLayout cardLayout;
  private JPanel cards;

  ////////////////////////////////////////////////////////////

  static {

    fileChooser = new JFileChooser();
    SimpleFileFilter imageFilter = new SimpleFileFilter (
      new String[] {"png", "gif", "jpg", "tif", "bmp"}, "Image files");
    fileChooser.addChoosableFileFilter (imageFilter);
    fileChooser.setDialogTitle ("Select logo image");
    fileChooser.setDialogType (JFileChooser.OPEN_DIALOG);
    fileChooser.setApproveButtonText ("OK");
    fileChooser.setFileFilter (imageFilter);

  } // static

  ////////////////////////////////////////////////////////////

  private void categorySelectEvent (String category) {
    cardLayout.show (cards, category);
    selectedCategory = category;
  } // categorySelectEvent

  ////////////////////////////////////////////////////////////
  
  /** 
   * Creates a new enhancement preferences chooser with the
   * specified preferences.
   * 
   * @param prefs the initial preference settings.
   */
  public PreferencesChooser (
    Preferences prefs
  ) {
  
    super (new BorderLayout (5, 5));
    this.prefs = (Preferences) prefs.clone();

    // Start by creating a card layout that will be controlled by the
    // toggle buttons on a toolbar.
    cardLayout = new CardLayout();
    cards = new JPanel (cardLayout);
    add (cards, BorderLayout.CENTER);
    generalPrefsChooser = new GeneralPreferencesChooser();
    cards.add (generalPrefsChooser, GENERAL_CATEGORY);
    enhancePrefsChooser = new EnhancementPreferencesChooser();
    cards.add (enhancePrefsChooser, ENHANCEMENT_CATEGORY);
    var exportPrefsChooser = new ExportPreferencesChooser();
    cards.add (exportPrefsChooser, EXPORT_CATEGORY);
    cardLayout.show (cards, selectedCategory);

    // Now create the toggle buttons so that they switch the card layout 
    // component.
    JPanel topPanel = new JPanel (new BorderLayout());
    topPanel.add (new JSeparator (JSeparator.HORIZONTAL), BorderLayout.SOUTH);
    add (topPanel, BorderLayout.NORTH);

    var toolbar = new JToolBar();

    toolbar.setLayout (new BoxLayout (toolbar, BoxLayout.X_AXIS));
    toolbar.add (Box.createHorizontalGlue());

    toolbar.setFloatable (false);
    topPanel.add (toolbar, BorderLayout.CENTER);
    var group = new ButtonGroup();
    JToggleButton button;

    button = new JToggleButton (GENERAL_CATEGORY, GUIServices.getIcon ("prefs.tab.general"));
    button.setSelected (selectedCategory.equals (GENERAL_CATEGORY));
    group.add (button);
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (event -> categorySelectEvent (GENERAL_CATEGORY));
    toolbar.add (button);

    button = new JToggleButton (ENHANCEMENT_CATEGORY, GUIServices.getIcon ("prefs.tab.enhancement"));
    button.setSelected (selectedCategory.equals (ENHANCEMENT_CATEGORY));
    group.add (button);
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (event -> categorySelectEvent (ENHANCEMENT_CATEGORY));
    toolbar.add (button);

    button = new JToggleButton (EXPORT_CATEGORY, GUIServices.getIcon ("prefs.tab.export"));
    button.setSelected (selectedCategory.equals (EXPORT_CATEGORY));
    group.add (button);
    button.setHorizontalTextPosition (SwingConstants.CENTER);
    button.setVerticalTextPosition (SwingConstants.BOTTOM);
    button.setIconTextGap (0);
    button.addActionListener (event -> categorySelectEvent (EXPORT_CATEGORY));
    toolbar.add (button);

    toolbar.add (Box.createHorizontalGlue());

  } // PreferencesChooser constructor

  ////////////////////////////////////////////////////////////

  /**
   * Shows a preferences chooser in a dialog and blocks.  If the user
   * presses OK, then the dialog is disposed and the new preferences
   * are returned.  If the user presses Cancel or no preferences were
   * changed, the dialog is disposed and null is returned.
   *
   * @param component the parent component for the dialog.
   * @param title the title for the dialog.
   * @param initialPrefs the initial preferences to show.
   * 
   * @return the new preferences, or null if the user pressed cancel
   * or did not make any changes.
   */
  public static Preferences showDialog (
    Component component,
    String title, 
    Preferences initialPrefs
  ) { 

    // Create chooser panel
    // --------------------
    final PreferencesChooser chooser = new PreferencesChooser (initialPrefs);

    // Create dialog actions
    // ---------------------
    final Preferences[] retval = new Preferences[] {null};
    Action okAction = GUIServices.createAction ("OK", new Runnable() {
        public void run () {
          if (chooser.getPrefsChanged())
            retval[0] = chooser.getPreferences();
        } // run
      });
    Action cancelAction = GUIServices.createAction ("Cancel", null);

    // Create and show dialog
    // ----------------------
    Component[] controls = new Component[] {
      GUIServices.getHelpButton (PreferencesChooser.class),
      Box.createHorizontalGlue()
    };
    JDialog prefsDialog = GUIServices.createDialog (
      component, title, true, chooser, controls,
      new Action[] {okAction, cancelAction}, null, true);
    prefsDialog.setVisible (true);

    // Return final value
    // ------------------
    return (retval[0]);

  } // showDialog

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the preferences changed flag.
   *
   * @return true if the preferences have been changed since the
   * initialization, or false if not.
   */
  public boolean getPrefsChanged () { 

    generalPrefsChooser.applyChanges();
    enhancePrefsChooser.applyChanges();
    return (prefsChanged);

  } // getPrefsChanged

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the current preferences.
   * 
   * @return the current preferences object.
   */
  public Preferences getPreferences () {

    generalPrefsChooser.applyChanges();
    enhancePrefsChooser.applyChanges();
    return (prefs);

  } // getPreferences

  ////////////////////////////////////////////////////////////

  /**
   * The <code>GeneralPreferencesChooser</code> lets the user change
   * just the preferences relating to general settings.
   */
  private class GeneralPreferencesChooser
    extends JPanel {

    // Variables
    // ---------
    
    /** The text field for heap size. */
    JTextField heapField;

    /** The text field for tile cache size. */
    JTextField cacheField;

    ////////////////////////////////////////////////////////

    /**
     * Applies any changes that have been made but not applied to the
     * to the current preferences.
     *
     * @since 3.3.1
     */
    public void applyChanges () {

      // Get new heap size
      // -----------------
      try {
        int newHeapSize = Integer.parseInt (heapField.getText());
        if (newHeapSize != prefs.getHeapSize()) {
          prefs.setHeapSize (newHeapSize);
          prefsChanged = true;
        } // if
      } // try
      catch (NumberFormatException e) {
        heapField.setText (Integer.toString (prefs.getHeapSize()));
      } // catch
      
      // Get new tile cache size
      // -----------------------
      try {
        int newCacheSize = Integer.parseInt (cacheField.getText());
        if (newCacheSize != prefs.getCacheSize()) {
          prefs.setCacheSize (newCacheSize);
          prefsChanged = true;
        } // if
      } // try
      catch (NumberFormatException e) {
        cacheField.setText (Integer.toString (prefs.getCacheSize()));
      } // catch
      
    } // applyChanges

    ////////////////////////////////////////////////////////
  
    /** 
     * Creates a new general preferences chooser.
     */
    public GeneralPreferencesChooser () {
  
      // Initialize
      // ----------
      super (new GridBagLayout());

      GridBagConstraints gc = new GridBagConstraints();
      gc.insets = new Insets (2, 0, 2, 0);
      gc.anchor = GridBagConstraints.WEST;

      // Add memory options
      // ------------------
      int row = -1;
      GUIServices.setConstraints (gc, 0, ++row, 3, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      add (new JLabel ("Memory limits (software restart required for changes to take effect):"), gc);

      GUIServices.setConstraints (gc, 0, ++row, 1, 1, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 15, 2, 10);
      add (new JLabel ("Maximum heap size:"), gc);

      heapField = new JTextField (8);
      heapField.setText (Integer.toString (prefs.getHeapSize()));
      heapField.setEditable (true);
      FocusAdapter focusAdapter = new FocusAdapter() {
        public void focusLost (FocusEvent event) {
          applyChanges();
        } // focusLost
      };
      heapField.addFocusListener (focusAdapter);

      GUIServices.setConstraints (gc, 1, row, 1, 1, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 0, 2, 0);
      add (heapField, gc);

      GUIServices.setConstraints (gc, 2, row, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      add (new JLabel (" Mb"), gc);

      GUIServices.setConstraints (gc, 0, ++row, 1, 1, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 15, 2, 10);
      add (new JLabel ("Tile cache size:"), gc);

      cacheField = new JTextField (8);
      cacheField.setText (Integer.toString (prefs.getCacheSize()));
      cacheField.setEditable (true);
      cacheField.addFocusListener (focusAdapter);

      GUIServices.setConstraints (gc, 1, row, 1, 1, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 0, 2, 0);
      add (cacheField, gc);

      GUIServices.setConstraints (gc, 2, row, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      add (new JLabel (" Mb"), gc);

      // Add geographic coordinate selection
      // -----------------------------------
      GUIServices.setConstraints (gc, 0, ++row, 3, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      gc.insets = new Insets (5, 0, 5, 0);
      add (new JLabel ("Geographic coordinates:"), gc);
      
      final JRadioButton decimalRadio = new JRadioButton (
        "Display as decimal degrees", prefs.getEarthLocDegrees());
      ButtonGroup group = new ButtonGroup();
      group.add (decimalRadio);
      decimalRadio.addItemListener (new ItemListener () {
          public void itemStateChanged (ItemEvent e) {
            prefs.setEarthLocDegrees (e.getStateChange() == ItemEvent.SELECTED);
            prefsChanged = true;
          } // itemStateChanged
        });
      GUIServices.setConstraints (gc, 0, ++row, 3, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      gc.insets = new Insets (2, 10, 2, 0);
      add (decimalRadio, gc);

      JRadioButton dmsRadio = new JRadioButton (
        "Display as degrees/minutes/seconds", !prefs.getEarthLocDegrees());
      group.add (dmsRadio);
      GUIServices.setConstraints (gc, 0, ++row, 3, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      add (dmsRadio, gc);

      GUIServices.setConstraints (gc, 0, ++row, 3, 1, GridBagConstraints.HORIZONTAL, 1, 1);
      add (new JLabel(), gc);

    } // GeneralPreferencesChooser constructor

    ////////////////////////////////////////////////////////

  } // GeneralPreferencesChooser class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>EnhancementPreferencesChooser</code> lets the user
   * change just the preferences relating to default data
   * enhancements.
   */
  private class EnhancementPreferencesChooser
    extends JPanel {

    // Constants
    // ---------

    /** The enhancement function type linear. */
    private static final String FUNCTION_LINEAR = "Linear";

    /** The enhancement function type step. */
    private static final String FUNCTION_STEP = "Step";
    
    /** The enhancement function type log base 10. */
    private static final String FUNCTION_LOG = "Log10";

    /** The enhancement function type gamma. */
    private static final String FUNCTION_GAMMA = "Gamma";

    // Variables
    // ---------

    /** The list of variables with color enhancement settings. */
    private JList<String> variableList;
    
    /** The variable list data model. */
    private DefaultListModel<String> variableModel;
    
    /** The field for typing in new variable names. */
    private JTextField variableField;
  
    /** The add button. */
    private JButton addButton;
  
    /** The remove button. */
    private JButton removeButton;
  
    /** The list of available palettes. */
    private JList<String> paletteList;
  
    /** The field for typing in the minimum value. */
    private JTextField minField;
  
    /** The field for typing in the maximum value. */
    private JTextField maxField;
  
    /** The radio button for selecting units from data. */
    private JRadioButton dataUnitsRadio;

    /** The radio button for converting to user units. */
    private JRadioButton convertUnitsRadio;

    /** The combo for selecting units value. */
    private JComboBox<String> unitsCombo;
  
    /** The panel used for enhancement settings. */
    private JPanel settingsPanel;
  
    /** The last changed text field, or null for none. */
    public JTextField fieldChanged;
  
    /** The variable that was last changed. */
    private String variableChanged;
    
    /** The spinner for step enhancement step count. */
    private JSpinner stepsSpinner;

    /** The combo box for function type. */
    private JComboBox<String> functionCombo;

    /** The flag indicating that event listeners are disabled. */
    private boolean listenersDisabled;

    ////////////////////////////////////////////////////////
  
    /** 
     * Creates a new enhancement preferences chooser.
     */
    public EnhancementPreferencesChooser () {
  
      // Initialize
      // ----------
      super (new GridBagLayout());
  
      // Create variable panel
      // ---------------------
      JPanel variablePanel = new JPanel (new BorderLayout());
      GridBagConstraints gc = new GridBagConstraints();
      gc.insets = new Insets (0, 2, 0, 2);
      GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.BOTH, 
        1, 1);
      this.add (variablePanel, gc);
  
      // Create variable list
      // --------------------
      variableModel = new DefaultListModel<>();
      variableModel.addAll (prefs.getEnhancementVariables());
      variableList = new JList<> (variableModel);
      variableList.addListSelectionListener (new VariableListListener());
      variablePanel.add (new JLabel ("Variable:"), BorderLayout.NORTH);
      variablePanel.add (new JScrollPane (variableList), BorderLayout.CENTER);
  
      // Create variable controls
      // ------------------------
      JPanel variableControlPanel = new JPanel (new GridBagLayout());
      variablePanel.add (variableControlPanel, BorderLayout.SOUTH);
  
      GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
        GridBagConstraints.HORIZONTAL, 1, 0);
      gc.insets = new Insets (2, 2, 2, 2);
      variableField = new JTextField (12);
      variableField.setEditable (true);
      variableField.addActionListener (new ActionListener () {
          public void actionPerformed (ActionEvent event) {
            addButton.doClick();
          } // actionPerformed
        });
      variableControlPanel.add (variableField, gc);

      // Create add button
      // -----------------
      GUIServices.setConstraints (gc, GridBagConstraints.RELATIVE, 0, 1, 1, 
        GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 0, 2, 0);
      addButton = GUIServices.getIconButton ("list.add");
      GUIServices.setSquare (addButton);
      addButton.addActionListener (new ActionListener () {
          public void actionPerformed (ActionEvent event) {
            addEntry();
          } // actionPerformed
        });
      addButton.setToolTipText ("Add new variable to list");
      variableControlPanel.add (addButton, gc);

      // Create remove button
      // --------------------
      removeButton = GUIServices.getIconButton ("list.delete");
      GUIServices.setSquare (removeButton);
      removeButton.addActionListener (new ActionListener () {
          public void actionPerformed (ActionEvent event) {
            removeEntry();
          } // actionPerformed
        });
      removeButton.setEnabled (false);
      removeButton.setToolTipText ("Remove variable from list");
      variableControlPanel.add (removeButton, gc);

      // Create restore button
      var restoreButton = GUIServices.getIconButton ("list.restore");
      GUIServices.setSquare (restoreButton);
      restoreButton.addActionListener (event -> restoreEntries());
      restoreButton.setToolTipText ("Restore default variable preferences");
      variableControlPanel.add (restoreButton, gc);

      // Create settings panel
      // ---------------------
      settingsPanel = new JPanel (new BorderLayout());
      GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.BOTH, 
        1, 1);
      gc.insets = new Insets (0, 10, 0, 2);
      this.add (settingsPanel, gc);
  
      // Create palette list
      // --------------------
      paletteList = new JList<> (PaletteFactory.getPredefined().toArray (new String[0]));
      paletteList.addListSelectionListener (new PaletteListListener());
      paletteList.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
      settingsPanel.add (new JLabel ("Palette:"), BorderLayout.NORTH);
      settingsPanel.add (new JScrollPane (paletteList), BorderLayout.CENTER);
  
      // Create function controls
      // ---------------------
      JPanel functionControlPanel = new JPanel (new GridBagLayout());
      settingsPanel.add (functionControlPanel, BorderLayout.SOUTH);
  
      GridBagConstraints gcSub = new GridBagConstraints();
      gcSub.anchor = GridBagConstraints.WEST;

      GUIServices.setConstraints (gcSub, 0, 0, 2, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 0);
      functionControlPanel.add (new JLabel ("Units:"), gcSub);

      GUIServices.setConstraints (gcSub, 0, 1, 2, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 0);
      JPanel dataLine = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 0));
      functionControlPanel.add (dataLine, gcSub);

      dataUnitsRadio = new JRadioButton ("Use units from data source");
      ButtonGroup group = new ButtonGroup();
      dataUnitsRadio.addActionListener (new ActionListener () {
          public void actionPerformed (ActionEvent event) {
            unitsCombo.setEnabled (false);
            updateUnits();
          } // actionPerformed
        });
      group.add (dataUnitsRadio);
      dataLine.add (dataUnitsRadio);

      GUIServices.setConstraints (gcSub, 0, 2, 2, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 0);
      JPanel convertLine = new JPanel (new FlowLayout (FlowLayout.LEFT, 5, 0));
      functionControlPanel.add (convertLine, gcSub);

      convertUnitsRadio = new JRadioButton ("Display in units of");
      convertUnitsRadio.addActionListener (new ActionListener () {
          public void actionPerformed (ActionEvent event) {
            unitsCombo.setEnabled (true);
            updateUnits();
          } // actionPerformed
        });
      group.add (convertUnitsRadio);
      convertLine.add (convertUnitsRadio);

      unitsCombo = new JComboBox<> (COMMON_UNITS);
      unitsCombo.setEditable (true);
      unitsCombo.addItemListener (new UnitsComboListener());
      convertLine.add (unitsCombo);

      gcSub.insets = new Insets (2, 0, 2, 0);
      GUIServices.setConstraints (gcSub, 0, 3, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 10);
      functionControlPanel.add (new JLabel ("Minimum:"), gcSub);
      minField = new JTextField (8);
      minField.setEditable (true);
      FieldDocumentListener fieldDocListener = new FieldDocumentListener();
      minField.getDocument().addDocumentListener (fieldDocListener);
      FieldFocusListener fieldFocusListener = new FieldFocusListener();
      minField.addFocusListener (fieldFocusListener);
      GUIServices.setConstraints (gcSub, 1, 3, 1, 1, 
        GridBagConstraints.NONE, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 0);
      functionControlPanel.add (minField, gcSub);
  
      GUIServices.setConstraints (gcSub, 0, 4, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 10);
      functionControlPanel.add (new JLabel ("Maximum:"), gcSub);
      maxField = new JTextField (8);
      maxField.setEditable (true);
      maxField.getDocument().addDocumentListener (fieldDocListener);
      maxField.addFocusListener (fieldFocusListener);
      GUIServices.setConstraints (gcSub, 1, 4, 1, 1, 
        GridBagConstraints.NONE, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 0);
      functionControlPanel.add (maxField, gcSub);
  
      GUIServices.setConstraints (gcSub, 0, 5, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 10);
      functionControlPanel.add (new JLabel ("Function:"), gcSub);
      functionCombo = new JComboBox (new String[] {
        FUNCTION_LINEAR, FUNCTION_STEP, FUNCTION_LOG, FUNCTION_GAMMA});
      functionCombo.addActionListener (new FunctionComboListener());
      GUIServices.setConstraints (gcSub, 1, 5, 1, 1, 
        GridBagConstraints.NONE, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 0);
      functionControlPanel.add (functionCombo, gcSub);

      GUIServices.setConstraints (gcSub, 0, 6, 1, 1, 
        GridBagConstraints.HORIZONTAL, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 10);
      functionControlPanel.add (new JLabel ("Steps:"), gcSub);
      stepsSpinner = new JSpinner (new SpinnerNumberModel (10, 
        1, 100, 1));
      stepsSpinner.addChangeListener (new StepsSpinnerListener());
      GUIServices.setConstraints (gcSub, 1, 6, 1, 1, 
        GridBagConstraints.NONE, 0, 0);
      gcSub.insets = new Insets (2, 0, 2, 0);
      functionControlPanel.add (stepsSpinner, gcSub);

      GUIServices.setConstraints (gcSub, 2, 0, 1, 1, 
        GridBagConstraints.HORIZONTAL, 1, 0);
      functionControlPanel.add (new JLabel(), gcSub);

      // Make settings initially inactive
      // --------------------------------
      GUIServices.setContainerEnabled (settingsPanel, false);

    } // EnhancementPreferencesChooser constructor

    ////////////////////////////////////////////////////////

    /** Handles steps spinner change events. */
    private class StepsSpinnerListener implements ChangeListener {
      public void stateChanged (ChangeEvent event) {

        // Check if listeners are disabled
        // -------------------------------
        if (listenersDisabled) return;

        // Get current settings
        // --------------------
        ColorEnhancementSettings settings = 
          prefs.getEnhancement (variableList.getSelectedValue());

        // Convert to new function
        // -----------------------
        String functionType = "step" + stepsSpinner.getValue().toString();
        settings.setFunction (EnhancementFunctionFactory.convert (
          settings.getFunction(), functionType));
        prefsChanged = true;

      } // stateChanged
    } // StepsSpinnerListener class

    ////////////////////////////////////////////////////////

    /** Updates the units preferences based on the chooser controls. */
    private void updateUnits () {
                              
      // Remove units
      // ------------
      int index = unitsCombo.getSelectedIndex();
      String item = ((String) unitsCombo.getSelectedItem()).trim();
      if (dataUnitsRadio.isSelected() || index == 0 || item.equals ("")) {
        prefs.removeUnits (variableChanged);
      } // if

      // Add units
      // ---------
      else {
        prefs.setUnits (variableChanged, item);
      } // else

      prefsChanged = true;

    } // updateUnits

    ////////////////////////////////////////////////////////

    /** Handles units combo events. */
    private class UnitsComboListener implements ItemListener {
      public void itemStateChanged (ItemEvent event) {

        // Check if need to react
        // ----------------------
        if (listenersDisabled) return;
        if (event.getStateChange() == ItemEvent.DESELECTED) return;

        // Check item
        // ----------
        String item = (String) unitsCombo.getSelectedItem();
        if (item.startsWith ("-- ")) {
          unitsCombo.setSelectedIndex (0);
          return;
        } // if

        // Set new units
        // -------------
        updateUnits();

      } // actionPerformed
    } // UnitsComboListener class

    ////////////////////////////////////////////////////////

    /** Handles function combo events. */
    private class FunctionComboListener implements ActionListener {
      public void actionPerformed (ActionEvent event) {

        // Check if listeners are disabled
        // -------------------------------
        if (listenersDisabled) return;

        // Get current settings
        // --------------------
        ColorEnhancementSettings settings = 
          prefs.getEnhancement (variableList.getSelectedValue());

        // Get new function type
        // ---------------------
        String selected = (String) functionCombo.getSelectedItem();
        String functionType = null;
        if (selected == FUNCTION_LINEAR) 
          functionType = "linear";
        else if (selected == FUNCTION_LOG) 
          functionType = "log";
        else if (selected == FUNCTION_STEP) 
          functionType = "step" + stepsSpinner.getValue().toString();
        else if (selected == FUNCTION_GAMMA)
          functionType = "gamma";

        // Convert to new function
        // -----------------------
        EnhancementFunction currentFunc = settings.getFunction();
        EnhancementFunction newFunc;
        try { 
          newFunc = EnhancementFunctionFactory.convert (currentFunc, 
            functionType);
        } // try
        catch (Exception e) {
          String errorMessage = 
            "An error occurred modifying the function type:\n" +
            e.toString() + "\n" + 
            "Please correct the problem and try again.";
          JOptionPane.showMessageDialog (PreferencesChooser.this, 
            errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
          listenersDisabled = true;
          if (currentFunc instanceof StepEnhancement)
            functionCombo.setSelectedItem (FUNCTION_STEP);
          else if (currentFunc instanceof LinearEnhancement)
            functionCombo.setSelectedItem (FUNCTION_LINEAR);
          else if (currentFunc instanceof LogEnhancement)
            functionCombo.setSelectedItem (FUNCTION_LOG);
          else if (currentFunc instanceof GammaEnhancement)
            functionCombo.setSelectedItem (FUNCTION_GAMMA);
          listenersDisabled = false;
          return;
        } // catch
        settings.setFunction (newFunc);
        prefsChanged = true;

        // Enable/disable steps spinner
        // ----------------------------
        stepsSpinner.setEnabled (selected == FUNCTION_STEP);

      } // actionPerformed
    } // FunctionComboListener class

    ////////////////////////////////////////////////////////

    /**
     * Applies any changes that have been made but not applied to the
     * to the current preferences.
     */
    public void applyChanges () {

      // Check for any changes
      // ---------------------
      if (fieldChanged == null || variableChanged == null) return;

      // Get existing enhancement range
      // ------------------------------
      ColorEnhancementSettings settings = 
        prefs.getEnhancement (variableChanged);
      double[] oldRange = settings.getFunction().getRange();
        
      // Modify range
      // ------------
      double[] newRange = (double[]) oldRange.clone();
      int index = (fieldChanged == minField ? 0 : 1);
      try {
        newRange[index] = Double.parseDouble (fieldChanged.getText());
        try {
          settings.getFunction().setRange (newRange); 
          prefsChanged = true;
        } // try
        catch (Exception e) {
          fieldChanged.setText (Double.toString (oldRange[index]));
        } // catch
      } // try
      catch (NumberFormatException e) { 
        fieldChanged.setText (Double.toString (oldRange[index]));
      } // catch
      
    } // applyChanges

    ////////////////////////////////////////////////////////
  
    /** Handles a change in focus in min/max fields. */
    private class FieldFocusListener extends FocusAdapter {
      public void focusLost (FocusEvent event) {
        applyChanges();
      } // focusLost
    } // FieldFocusListener class
  
    ////////////////////////////////////////////////////////
  
    /** Handles min/max field change events. */
    private class FieldDocumentListener implements DocumentListener {
      public void changedUpdate (DocumentEvent e) { }
      public void insertUpdate (DocumentEvent e) { 
        Document doc = e.getDocument();
        if (doc == minField.getDocument()) fieldChanged = minField;
        else if (doc == maxField.getDocument()) fieldChanged = maxField;
        variableChanged = variableList.getSelectedValue();
      } // insertUpdate
      public void removeUpdate (DocumentEvent e) { insertUpdate (e); }
    } // FieldDocumentListener class
  
    ////////////////////////////////////////////////////////
  
    /** Handles variable list selection events. */
    private class VariableListListener implements ListSelectionListener {
      public void valueChanged (ListSelectionEvent event) {
  
        // Check if list selection is adjusting
        // ------------------------------------
        if (variableList.getValueIsAdjusting()) return;
  
        // Update remove button
        // --------------------
        var selectedValues = variableList.getSelectedValuesList().toArray (new String[0]);
        removeButton.setEnabled (selectedValues.length != 0);
  
        // Update preferences
        // ------------------
        listenersDisabled = true;
        if (selectedValues.length == 1) {
          GUIServices.setContainerEnabled (settingsPanel, true);
          String varName = selectedValues[0];
          ColorEnhancementSettings settings = prefs.getEnhancement (varName);
          paletteList.setSelectedValue (settings.getPalette().getName(), true);
          double[] range = settings.getFunction().getRange();
          minField.setText (Double.toString (range[0]));
          maxField.setText (Double.toString (range[1]));
          String units = prefs.getUnits (varName);
          if (units == null) {
            dataUnitsRadio.setSelected (true);
            unitsCombo.setSelectedIndex (0);
            unitsCombo.setEnabled (false);
          } // if
          else {
            convertUnitsRadio.setSelected (true);
            unitsCombo.setSelectedItem (units);
            unitsCombo.setEnabled (true);
          } // else
          EnhancementFunction func = settings.getFunction();
          if (func instanceof StepEnhancement) {
            functionCombo.setSelectedItem (FUNCTION_STEP);
            stepsSpinner.setValue (
              Integer.valueOf (((StepEnhancement)func).getSteps()));
            stepsSpinner.setEnabled (true);
          } // if
          else if (func instanceof LinearEnhancement) {
            functionCombo.setSelectedItem (FUNCTION_LINEAR);
            stepsSpinner.setValue (Integer.valueOf (10));
            stepsSpinner.setEnabled (false);
          } // else if
          else if (func instanceof LogEnhancement) {
            functionCombo.setSelectedItem (FUNCTION_LOG);
            stepsSpinner.setValue (Integer.valueOf (10));
            stepsSpinner.setEnabled (false);
          } // else if
          else if (func instanceof GammaEnhancement) {
            functionCombo.setSelectedItem (FUNCTION_GAMMA);
            stepsSpinner.setValue (Integer.valueOf (10));
            stepsSpinner.setEnabled (false);
          } // else if
        } // if
        else {
          GUIServices.setContainerEnabled (settingsPanel, false);
          paletteList.clearSelection();
          minField.setText ("");
          maxField.setText ("");
          dataUnitsRadio.setSelected (true);
          unitsCombo.setSelectedIndex (0);
          functionCombo.setSelectedItem (FUNCTION_LINEAR);
          stepsSpinner.setValue (Integer.valueOf (10));
        } // else
        listenersDisabled = false;
  
      } // valueChanged
    } // VariableListListener class
  
    ////////////////////////////////////////////////////////
  
    /** Handles palette list selection events. */
    private class PaletteListListener implements ListSelectionListener {
      public void valueChanged (ListSelectionEvent event) {
  
        // Check if listeners are disabled
        // -------------------------------
        if (listenersDisabled) return;

        // Update preferences
        // ------------------
        ColorEnhancementSettings settings =
          prefs.getEnhancement (variableList.getSelectedValue());
        String paletteName = paletteList.getSelectedValue();
        if (paletteName != null) {
          settings.setPalette (PaletteFactory.create (paletteName));
          prefsChanged = true;
        } // if
  
      } // valueChanged
    } // PaletteListListener class
  
    ////////////////////////////////////////////////////////
  
    /** 
     * Adds an entry to the variable list using the text field contents.
     * If the text field contains an empty strings, no operation is
     * performed.
     */
    private void addEntry () {
  
      // Get and check variable name
      // ---------------------------
      String variableName = variableField.getText();
      if (variableName.equals ("")) return;
      if (variableModel.contains (variableName)) return;
  
      // Create settings
      // ---------------
      ColorEnhancementSettings settings = new ColorEnhancementSettings (
        variableName, PaletteFactory.create ("BW-Linear"), 
        new LinearEnhancement (new double[] {0, 0}));
      prefs.setEnhancement (settings);
      prefsChanged = true;
  
      // Add entry to list
      // -----------------
      variableModel.addElement (variableName);
      variableList.ensureIndexIsVisible (variableModel.getSize() - 1);
      variableList.setSelectedIndex (variableModel.getSize() - 1);
  
      // Clear text field
      // ----------------
      variableField.setText ("");
  
    } // addEntry
  
    ////////////////////////////////////////////////////////
  
    /** 
     * Removes one or more entries from the list using the highlighted
     * list entries.
     */
    private void removeEntry () {
  
      for (var varName : variableList.getSelectedValuesList()) {
        prefs.removeEnhancement (varName);
        prefs.removeUnits (varName);
        variableModel.removeElement (varName);
      } // for
      prefsChanged = true;
  
    } // removeEntry

    ////////////////////////////////////////////////////////
  
    /** Restores the variable list entries from the default preferences. */
    private void restoreEntries () {
  
      String question = 
        "The default variable preferences will be restored.\n" +
        "This will override any existing variable settings.\n" + 
        "Are you sure?\n";
      int result = JOptionPane.showConfirmDialog (
        PreferencesChooser.this, question, "Confirmation", 
        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      if (result == JOptionPane.YES_OPTION) {

        LOGGER.fine ("Restoring variable list from default preferences");

        try {
          var defaultPrefs = ResourceManager.getDefaultPreferences();
          for (var varName : prefs.getEnhancementVariables()) {
            prefs.removeUnits (varName);
            prefs.removeEnhancement (varName);
          } // for
          for (var varName : defaultPrefs.getEnhancementVariables()) {
            var units = defaultPrefs.getUnits (varName);
            if (units != null) prefs.setUnits (varName, units);
            prefs.setEnhancement (defaultPrefs.getEnhancement (varName));
          } // for
          variableModel.clear();
          variableModel.addAll (defaultPrefs.getEnhancementVariables());
          prefsChanged = true;
        } // try 
        catch (Exception e) { 
          String message = "Error restoring default variable preferences:\n" + e;
          JOptionPane.showMessageDialog (PreferencesChooser.this, message, "Error", JOptionPane.ERROR_MESSAGE);
        } // catch

      } // if

    } // restoreEntries

  } // EnhancementPreferencesChooser class

  ////////////////////////////////////////////////////////////

  /**
   * The <code>ExportPreferencesChooser</code> lets the user change
   * just the preferences relating to export settings.
   * 
   * @since 3.8.1
   */
  private class ExportPreferencesChooser extends JPanel {

    IconElement logoIcon;
    JRadioButton predefinedRadio;
    JRadioButton customRadio;
    JComboBox<String> logoCombo;
    JPanel logoPanel;
    JTextField customField;
    JButton fileSelectButton;
    FileTransferHandler dropHandler;

    ////////////////////////////////////////////////////////

    private String logoSetting () {
      var logo = predefinedRadio.isSelected() ? 
        (String) logoCombo.getSelectedItem() : 
        customField.getText();
      return (logo);
    } // logoSetting

    private void applyChanges() { 
      var logo = logoSetting();
      prefs.setLogo (logo);
      prefsChanged = true;
    } // applyChanges

    private void updateEnabled() { 
      boolean predefined = predefinedRadio.isSelected();
      logoCombo.setEnabled (predefined);
      customField.setEnabled (!predefined);
      fileSelectButton.setEnabled (!predefined);
    } // updateEnabled

    private void radioButtonEvent () {
      updateEnabled();
      updateLogo();
    } // radioButtonEvent

    private void updateLogo() {
      var logo = logoSetting();
      reloadLogo (logo);
      logoPanel.repaint();
      applyChanges();
    } // updateLogo

    private boolean reloadLogo (String logo) {
      boolean success;
      try { 
        logoIcon = IconElementFactory.getInstance().create (logo); 
        success = true;
      } // try
      catch (Exception e) {
        logoIcon = null;
        // JOptionPane.showMessageDialog (ExportPreferencesChooser.this,
        //   "Error creating logo from image:\n" + e.toString(),
        //   "Error", JOptionPane.ERROR_MESSAGE);
        success = false;

      } // catch
//      if (success) updateLogo();
      return (success);
    } // reloadLogo

    private void logoComboEvent() {
      updateLogo();
    } // logoComboEvent

    private void fileSelectEvent () {
      int returnVal = fileChooser.showDialog (this, null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        customField.setText (fileChooser.getSelectedFile().getPath());
        updateLogo();
      } // if
    } // fileSelectEvent

    private void acceptCustomEvent () {
      updateLogo();
    } // acceptCustomEvent

    private void fileDropEvent (File file) {
      customField.setText (file.getPath());
      if (customRadio.isSelected()) acceptCustomEvent();
      else customRadio.doClick();
    } // fileDropEvent

    ////////////////////////////////////////////////////////
  
    /** Creates a new export preferences chooser. */
    public ExportPreferencesChooser () {
  
      super (new GridBagLayout());

      GridBagConstraints gc = new GridBagConstraints();
      gc.insets = new Insets (2, 0, 2, 0);
      gc.anchor = GridBagConstraints.NORTHWEST;

      int row = -1;
      GUIServices.setConstraints (gc, 0, ++row, 3, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      gc.insets = new Insets (5, 0, 5, 0);
      add (new JLabel ("Plot legend logo:"), gc);
      
      // Start by getting the current logo setting.  If there's no setting,
      // then this preferences instance was from before the export update.
      // Select a default logo in that case.  

      var predefinedNames = IconElementFactory.getInstance().getResourceNames();
      var logo = prefs.getLogo();
      if (logo == null) logo = IconElementFactory.getInstance().getDefaultIcon();
      boolean predefined = predefinedNames.contains (logo);
      try { logoIcon = IconElementFactory.getInstance().create (logo); }
      catch (Exception e) { }

      ButtonGroup group = new ButtonGroup();

      // Add the predefined and custom logo controls eith their event
      // handlers.

      predefinedRadio = new JRadioButton ("Use predefined:", predefined);
      group.add (predefinedRadio);
      predefinedRadio.addItemListener (event -> radioButtonEvent());
      GUIServices.setConstraints (gc, 0, ++row, 1, 1, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 10, 2, 0);
      add (predefinedRadio, gc);

      logoCombo = new JComboBox<String>();
      predefinedNames.forEach (logoCombo::addItem);
      if (predefined) logoCombo.setSelectedItem (logo);
      logoCombo.addActionListener (event -> logoComboEvent());
      GUIServices.setConstraints (gc, 1, row, 1, 1, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 5, 2, 0);
      add (logoCombo, gc);

      customRadio = new JRadioButton ("Use custom:", !predefined);
      group.add (customRadio);
      GUIServices.setConstraints (gc, 0, ++row, 1, 1, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 10, 2, 0);
      add (customRadio, gc);

      customField = new JTextField (20);
      if (!predefined) customField.setText (logo);
      customField.setEditable (true);
      customField.addFocusListener (new FocusAdapter() {
        public void focusLost (FocusEvent event) { acceptCustomEvent(); }
      });
      customField.addActionListener (event -> acceptCustomEvent());
      dropHandler = new FileTransferHandler (() -> fileDropEvent (dropHandler.getFile()));
      customField.setTransferHandler (dropHandler);

      GUIServices.setConstraints (gc, 1, row, 1, 1, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (2, 5, 2, 0);
      add (customField, gc);

      fileSelectButton = GUIServices.getIconButton ("select.file");
      GUIServices.setSquare (fileSelectButton);
      fileSelectButton.addActionListener (event -> fileSelectEvent());

      GUIServices.setConstraints (gc, 2, row, 1, 1, GridBagConstraints.NONE, 1, 0);
      add (fileSelectButton, gc);

      // Add the logo preview panel.  This will show a logo if there's a valid
      // one, or a message if the logo is invalid, ie: the user selected an
      // image file that couldn't be used, or the custom logo no longer exists.

      logoPanel = new LogoPanel();
      logoPanel.setTransferHandler (dropHandler);
      logoPanel.setPreferredSize (new Dimension (80, 80));

      GUIServices.setConstraints (gc, 4, 0, 1, 4, GridBagConstraints.NONE, 0, 0);
      gc.insets = new Insets (5, 0, 5, 0);
      add (logoPanel, gc);

      GUIServices.setConstraints (gc, 0, ++row, 3, 1, GridBagConstraints.HORIZONTAL, 1, 1);
      add (new JLabel(), gc);

      updateEnabled();

    } // ExportPreferencesChooser constructor

    ////////////////////////////////////////////////////////

    private class LogoPanel extends JPanel {

      int SPACE_SIZE = 5;
      Stroke DEFAULT_STROKE = new BasicStroke (1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
      Color FILL = new Color (237, 238, 207);

      public void paintComponent (Graphics g) {
        super.paintComponent (g);
        var g2d = (Graphics2D) g;

        // First draw a square frame in the foreground color, filled with the
        // legend background color.
        var size = getSize();
        int frameWidth = size.height - 1;
        int frameHeight = frameWidth;
        var frameOrigin = new Point (
          (size.width - frameWidth)/2, 
          (size.height - frameWidth)/2
        );
        g2d.setColor (FILL);
        g2d.fillRect (frameOrigin.x, frameOrigin.y, frameWidth, frameHeight);
        g2d.setColor (getForeground());
        g2d.setStroke (DEFAULT_STROKE);
        GraphicsServices.drawRect (g2d, new Rectangle (frameOrigin.x, frameOrigin.y, 
          frameWidth, frameHeight));

        // If the logo is null, then we have no preview to present and we
        // draw some text lines.
        if (logoIcon == null) {

          var renderContext = g2d.getFontRenderContext();
          var font = g2d.getFont();
          g2d.setColor (getForeground());

          var firstLine = "No preview";
          var secondLine = "available";

          var layout = new TextLayout (firstLine, font, renderContext);
          var textBounds = layout.getBounds().getBounds();
          var textHeight = (int) Math.ceil (layout.getAscent() + layout.getDescent());
          g2d.drawString (firstLine, 
            frameOrigin.x + (frameWidth - textBounds.width)/2, 
            frameOrigin.y + (frameHeight - textHeight)/2 + textHeight/2);

          layout = new TextLayout (secondLine, font, renderContext);
          textBounds = layout.getBounds().getBounds();
          g2d.drawString (secondLine, 
            frameOrigin.x + (frameWidth - textBounds.width)/2, 
            frameOrigin.y + (frameHeight + textHeight)/2 + textHeight/2);

        } // if

        // If the logo is available, then we render it inside the frame with
        // a background.
        else {
          int logoSize = frameWidth - SPACE_SIZE*4;
          if (logoSize > 0) {
            logoIcon.setPreferredSize (new Dimension (logoSize, logoSize));
            var bounds = logoIcon.getBounds (g2d);
            logoIcon.setPosition (new Point (frameOrigin.x + SPACE_SIZE*2, frameOrigin.y + SPACE_SIZE*2));          
            logoIcon.setShadow (true);
            logoIcon.render (g2d, null, FILL);
          } // if
        } // else

      } // paintComponent
    } // LogoPanel class

    ////////////////////////////////////////////////////////

  } // ExportPreferencesChooser class

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
    
    JPanel panel = new PreferencesChooser (ResourceManager.getPreferences());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);
    
  } // main
  
  ////////////////////////////////////////////////////////////
  
} //  PreferencesChooser
  
////////////////////////////////////////////////////////////////////////
  
