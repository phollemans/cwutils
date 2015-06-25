////////////////////////////////////////////////////////////////////////
/*
     FILE: PreferencesChooser.java
  PURPOSE: Allows the user to choose preferences.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/21
  CHANGES: 2005/02/03, PFH
           - divided into private classes
           - added general section
           - added enhancement functions
           - added showDialog()
           2005/10/09, PFH, added variableChange=null check in applyChanges()
           2006/03/15, PFH, modified to use GUIServices.getIconButton()
           2006/11/03, PFH, added units and list icons
           2006/11/08, PFH, added help button
           2012/12/04, PFH, updated to use getSelectedValuesList()
           2015/05/15, PFH
          - Changes: Added support for heap and cache size preferences.
          - Issue: We needed some way to give the user the ability to
            change the Java VM heap size and tile cache size without
            having to edit batch or script files.

  CoastWatch Software Library and Utilities
  Copyright 1998-2015, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.render.ColorEnhancementSettings;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.EnhancementFunctionFactory;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.LogEnhancement;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.render.StepEnhancement;
import noaa.coastwatch.tools.Preferences;
import noaa.coastwatch.tools.ResourceManager;

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

  // Constants
  // ---------

  /** The category name for general settings. */
  private static final String GENERAL_CATEGORY = "General";

  /** The category name for enhancement settings. */
  private static final String ENHANCEMENT_CATEGORY = "Enhancement";

  /** The icon for general settings. */
  private static final Icon GENERAL_ICON = 
    GUIServices.getIcon ("prefs.general");

  /** The icon for enhancement settings. */
  private static final Icon ENHANCEMENT_ICON = 
    GUIServices.getIcon ("prefs.enhancement");

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
  
    // Initialize
    // ----------
    super (new BorderLayout (5, 5));
    this.prefs = (Preferences) prefs.clone();
    
    // Create cards
    // ------------
    final CardLayout cardLayout = new CardLayout();
    final JPanel cards = new JPanel (cardLayout);
    add (cards, BorderLayout.CENTER);
    generalPrefsChooser = new GeneralPreferencesChooser();
    cards.add (generalPrefsChooser, GENERAL_CATEGORY);
    enhancePrefsChooser = new EnhancementPreferencesChooser();
    cards.add (enhancePrefsChooser, ENHANCEMENT_CATEGORY);

    // Create list
    // -----------
    final JList list = new JList (new String[] {GENERAL_CATEGORY, 
      ENHANCEMENT_CATEGORY});
    list.setSelectionMode (ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex (0);
    list.addListSelectionListener (new ListSelectionListener () {
        public void valueChanged (ListSelectionEvent e) {
          cardLayout.show (cards, (String) list.getSelectedValue());
        } // valueChanged
      });
    list.setCellRenderer (new PrefsCellRenderer());    
    JScrollPane scrollPane = new JScrollPane (list);
    Dimension viewSize = list.getPreferredScrollableViewportSize();
    int width = viewSize.width + viewSize.height/list.getVisibleRowCount();
    list.setFixedCellWidth (width);
    add (scrollPane, BorderLayout.LINE_START);

  } // PreferencesChooser constructor

  ////////////////////////////////////////////////////////////

  /** Renders list cells for the preferences category list. */
  private class PrefsCellRenderer 
    extends JLabel 
    implements ListCellRenderer {

    public Component getListCellRendererComponent (
      JList list,
      Object value,
      int index,
      boolean isSelected,
      boolean cellHasFocus
    ) {

      String str = value.toString();
      setText (str);
      Icon icon;
      if (str.equals (GENERAL_CATEGORY)) icon = GENERAL_ICON;
      else if (str.equals (ENHANCEMENT_CATEGORY)) icon = ENHANCEMENT_ICON;
      else throw new IllegalStateException ("Unknown preferences category");
      setIcon (icon);
      if (isSelected) {
        setBackground (list.getSelectionBackground());
        setForeground (list.getSelectionForeground());
      } // if
      else {
        setBackground (list.getBackground());
        setForeground (list.getForeground());
      } // else
      setEnabled (list.isEnabled());
      setFont (list.getFont());
      setOpaque (true);

      return (this);

    } // getListCellRendererComponent

  } // PrefsCellRenderer

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
      setBorder (new TitledBorder (new EtchedBorder(), "General Preferences"));

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
      add (new JLabel ("Mb"), gc);

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
      add (new JLabel ("Mb"), gc);

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

  } // GeneralPreferencesChooser

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

    // Variables
    // ---------

    /** The list of variables with color enhancement settings. */
    private JList variableList;
    
    /** The variable list data model. */
    private DefaultListModel variableModel;
    
    /** The field for typing in new variable names. */
    private JTextField variableField;
  
    /** The add button. */
    private JButton addButton;
  
    /** The remove button. */
    private JButton removeButton;
  
    /** The list of available palettes. */
    private JList paletteList;
  
    /** The field for typing in the minimum value. */
    private JTextField minField;
  
    /** The field for typing in the maximum value. */
    private JTextField maxField;
  
    /** The radio button for selecting units from data. */
    private JRadioButton dataUnitsRadio;

    /** The radio button for converting to user units. */
    private JRadioButton convertUnitsRadio;

    /** The combo for selecting units value. */
    private JComboBox unitsCombo;
  
    /** The panel used for enhancement settings. */
    private JPanel settingsPanel;
  
    /** The last changed text field, or null for none. */
    public JTextField fieldChanged;
  
    /** The variable that was last changed. */
    private String variableChanged;
    
    /** The spinner for step enhancement step count. */
    private JSpinner stepsSpinner;

    /** The combo box for function type. */
    private JComboBox functionCombo;

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
      setBorder (new TitledBorder (new EtchedBorder(), 
        "Enhancement Preferences"));
  
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
      variableModel = new DefaultListModel();
      for (Iterator iter = prefs.getEnhancementVariables().iterator(); 
        iter.hasNext(); )
        variableModel.addElement (iter.next());
      variableList = new JList (variableModel);
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
      addButton.setToolTipText ("Add");
      variableControlPanel.add (addButton, gc);

      // Create remove button
      // --------------------
      removeButton = GUIServices.getIconButton ("list.remove");
      GUIServices.setSquare (removeButton);
      removeButton.addActionListener (new ActionListener () {
          public void actionPerformed (ActionEvent event) {
            removeEntry();
          } // actionPerformed
        });
      removeButton.setEnabled (false);
      removeButton.setToolTipText ("Remove");
      variableControlPanel.add (removeButton, gc);

      // Create settings panel
      // ---------------------
      settingsPanel = new JPanel (new BorderLayout());
      GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.BOTH, 
        1, 1);
      gc.insets = new Insets (0, 2, 0, 2);
      this.add (settingsPanel, gc);
  
      // Create palette list
      // --------------------
      paletteList = new JList (PaletteFactory.getPredefined().toArray());
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

      unitsCombo = new JComboBox (COMMON_UNITS);
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
      functionCombo = new JComboBox (new Object[] {
        FUNCTION_LINEAR, FUNCTION_STEP, FUNCTION_LOG});
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
          prefs.getEnhancement ((String) variableList.getSelectedValue());

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
          prefs.getEnhancement ((String) variableList.getSelectedValue());

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
        variableChanged = (String) variableList.getSelectedValue();
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
        Object[] selectedValues = variableList.getSelectedValuesList().toArray();
        removeButton.setEnabled (selectedValues.length != 0);
  
        // Update preferences
        // ------------------
        listenersDisabled = true;
        if (selectedValues.length == 1) {
          GUIServices.setContainerEnabled (settingsPanel, true);
          String varName = (String) selectedValues[0];
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
              new Integer (((StepEnhancement)func).getSteps()));
            stepsSpinner.setEnabled (true);
          } // if
          else if (func instanceof LinearEnhancement) {
            functionCombo.setSelectedItem (FUNCTION_LINEAR);
            stepsSpinner.setValue (new Integer (10));
            stepsSpinner.setEnabled (false);
          } // else if
          else if (func instanceof LogEnhancement) {
            functionCombo.setSelectedItem (FUNCTION_LOG);
            stepsSpinner.setValue (new Integer (10));
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
          stepsSpinner.setValue (new Integer (10));
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
          prefs.getEnhancement ((String) variableList.getSelectedValue());
        String paletteName = (String) paletteList.getSelectedValue();
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
  
      // Remove selected items
      // ---------------------
      Object[] values = variableList.getSelectedValuesList().toArray();
      for (int i = 0; i < values.length; i++) {
        String varName = (String) values[i];
        prefs.removeEnhancement (varName);
        prefs.removeUnits (varName);
        variableModel.removeElement (varName);
      } // for
      prefsChanged = true;
  
    } // removeEntry

  } // EnhancementPreferencesChooser

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
  
