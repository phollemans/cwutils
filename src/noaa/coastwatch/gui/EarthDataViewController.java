////////////////////////////////////////////////////////////////////////
/*
     FILE: EarthDataViewController.java
  PURPOSE: Handles the interaction between choosers and the view panel.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/17
           2004/10/17, PFH, fixed bug in bitmask overlay navigation
           2005/02/13, PFH, added null check in performViewOperation()
           2005/05/22, PFH, added 1:1 view operation
           2006/10/20, PFH, added warning message before navigation
           2006/10/30, PFH, added legend panel
           2006/11/10, PFH, added view update on adjusting function
           2006/12/14, PFH, added navigation analysis panel
           2007/07/27, PFH, changed DrawingProxy to LightTable
           2007/07/30, PFH, changed BOX_MODE to BOX_ZOOM_MODE
           2007/11/05, PFH, fixed null object in updateNavigation()
           2014/11/11, PFH
           - Changes: Added call to variableChooser.dispose().
           - Issue: Memory was being held onto even after the dispose method
             was called, and we found that it was largely through the 
             VariableChooser instance.
           2015/02/27, PFH
           - Changes: Added call to compositeChooser.dispose()
           - Issue: Same as the last change, memory was being leaked.
 
  CoastWatch Software Library and Utilities
  Copyright 1998-2015, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import noaa.coastwatch.gui.AnnotationListChooser;
import noaa.coastwatch.gui.CompositeChooser;
import noaa.coastwatch.gui.EarthDataViewPanel;
import noaa.coastwatch.gui.EnhancementChooser;
import noaa.coastwatch.gui.LegendPanel;
import noaa.coastwatch.gui.LightTable;
import noaa.coastwatch.gui.NavigationChooser;
import noaa.coastwatch.gui.OverlayListChooser;
import noaa.coastwatch.gui.PaletteChooser;
import noaa.coastwatch.gui.SurveyListChooser;
import noaa.coastwatch.gui.VariableChooser;
import noaa.coastwatch.gui.ViewOperationChooser;
import noaa.coastwatch.gui.nav.NavigationAnalysisPanel;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.render.ColorComposite;
import noaa.coastwatch.render.ColorEnhancement;
import noaa.coastwatch.render.ColorEnhancementSettings;
import noaa.coastwatch.render.EarthDataOverlay;
import noaa.coastwatch.render.EarthDataView;
import noaa.coastwatch.render.EarthImageTransform;
import noaa.coastwatch.render.EnhancementFunction;
import noaa.coastwatch.render.GridContainerOverlay;
import noaa.coastwatch.render.ImageTransform;
import noaa.coastwatch.render.LinearEnhancement;
import noaa.coastwatch.render.Palette;
import noaa.coastwatch.render.PaletteFactory;
import noaa.coastwatch.tools.Preferences;
import noaa.coastwatch.tools.ResourceManager;
import noaa.coastwatch.util.BoxSurvey;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthDataSurvey;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.LineSurvey;
import noaa.coastwatch.util.PointSurvey;
import noaa.coastwatch.util.PolygonSurvey;
import noaa.coastwatch.util.Statistics;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * The <code>EarthDataViewController</code> class handles
 * interactions between an {@link EarthDataView} object and the
 * chooser objects used to manipulate its properties.  All
 * changes in the view overlays, palette, enhancement function,
 * zoom, etc. are handled by the controller.  The controller
 * methods may be used to access the property chooser panels in
 * order to arrange them in a layout manager.
 *
 * @see EarthDataView
 * @see EarthDataViewPanel
 * @see PaletteChooser
 * @see EnhancementChooser
 * @see VariableChooser
 * @see ViewOperationChooser
 * @see OverlayListChooser
 * @see SurveyListChooser
 * @see AnnotationListChooser
 * @see CompositeChooser
 * @see NavigationChooser
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class EarthDataViewController {

  // Constants
  // ---------

  /** 
   * The view interaction mode, used when the view operation chooser
   * is active, which is most of the time.
   */
  private static final int VIEW_MODE = 0;

  /** 
   * The survey interaction mode, used when a survey is being
   * performed.
   */
  private static final int SURVEY_MODE = 1;

  /** 
   * The annotation interaction mode, used when an annotation is being
   * performed.
   */
  private static final int ANNOTATION_MODE = 2;

  /** 
   * The navigation interaction mode, used when navigation
   * correction is being performed.
   */
  private static final int NAVIGATION_MODE = 3;

  /**
   * The navigation analysis mode, used when navigation
   * analysis is being performed.
   */
  private static final int NAV_ANALYSIS_MODE = 4;

  // Variables
  // ---------

  /** The color enhancement view. */
  private ColorEnhancement enhancementView;

  /** The color composite view. */
  private ColorComposite compositeView;

  /** 
   * The composite flag, true for color composite, false for color
   * enhancement.  By default, the controller starts up with a color
   * enhancement.
   */
  private boolean viewIsComposite;

  /** The main view panel. */
  private EarthDataViewPanel viewPanel;

  /** The legend panel for the view. */
  private LegendPanel legendPanel;

  /** The palette chooser. */
  private PaletteChooser paletteChooser;

  /** The enhancement chooser. */
  private EnhancementChooser enhancementChooser;

  /** The variable chooser. */
  private VariableChooser variableChooser;

  /** The overlay list chooser. */
  private OverlayListChooser overlayChooser;

  /** The survey list chooser. */
  private SurveyListChooser surveyChooser;

  /** The annotation list chooser. */
  private AnnotationListChooser annotationChooser;

  /** The map of variable name to enhancement settings. */
  private HashMap settingsMap;

  /** The reader object. */
  private EarthDataReader reader;

  /** The composite chooser. */
  private CompositeChooser compositeChooser;

  /** The navigation chooser. */
  private NavigationChooser navigationChooser;

  /** The light table for the view panel. */
  private LightTable lightTable;

  /** 
   * The user interaction mode.  This is normally set to the view
   * interaction mode, but may in some cases be changed to the survey
   * or other to perform drawing operations.
   */
  private int interactionMode;

  /** 
   * The listener for view operations.  This must be kept as a
   * variable so that we can later remove it from the list of
   * listeners when this class is being disposed.
   */
  private OperationListener operationListener;

  /** The flag for warning about navigation corrections. */
  private static boolean showWarning = true;

  /** The navigation analysis panel. */
  private NavigationAnalysisPanel navAnalysisPanel;

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the navigation analysis panel for this controller.
   *
   * @return the analysis panel.
   */
  public NavigationAnalysisPanel getNavAnalysisPanel () {

    return (navAnalysisPanel);

  } // getNavAnalysisPanel

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a list of tab component panels created by this controller.
   *
   * @return the component panels.
   */
  public List getTabComponentPanels () {

    List list = new LinkedList();
    list.add (enhancementChooser);
    list.add (paletteChooser);
    list.add (overlayChooser);
    list.add (surveyChooser);
    list.add (annotationChooser);
    list.add (compositeChooser);
    list.add (navigationChooser);
    return (list);

  } // getTabComponentPanels

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the variable chooser created by this controller.
   *
   * @return the variable chooser.
   */
  public VariableChooser getVariableChooser () { return (variableChooser); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the view panel created by this controller.
   *
   * @return the view panel.
   */
  public EarthDataViewPanel getViewPanel () { return (viewPanel); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the light table created by this controller.
   *
   * @return the light table.
   */
  public LightTable getLightTable () { return (lightTable); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the legend panel created by this controller.
   *
   * @return the legend panel.
   */
  public LegendPanel getLegendPanel () { return (legendPanel); }

  ////////////////////////////////////////////////////////////

  /** 
   * Saves the color enhancement palette and function settings.  The
   * settings are saved using the current variable name so that they
   * can later be restored when the view contains the same variable
   * again.  In this way, we don't need to have multiple views and can
   * reuse the same view for multiple variables.
   *
   * @param view the view to save.
   */
  private void saveEnhancementSettings (
    ColorEnhancement view
  ) {

    ColorEnhancementSettings settings = view.saveSettings();
    settingsMap.put (settings.getName(), settings);

  } // saveEnhancementSettings

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the default color enhancement settings for the specified
   * variable.
   */
  private ColorEnhancementSettings getDefaultSettings (
    String variableName
  ) {

    Statistics stats = reader.getStatistics (variableName);
    EnhancementFunction func = new LinearEnhancement (
      new double[] {Math.floor (stats.getMin()), 
      Math.ceil (stats.getMax())});
    Palette pal = PaletteFactory.create ("BW-Linear");

    return (new ColorEnhancementSettings (variableName, pal, func));

  } // getDefaultSettings

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the user-specified preferences based color enhancement
   * settings for the specified variable.  If there are no preferred
   * settings, the default settings are returned.
   */
  private ColorEnhancementSettings getPrefsSettings (
    String variableName
  ) {

    ColorEnhancementSettings prefsSettings = 
      ResourceManager.getPreferences().getEnhancement (variableName);
    if (prefsSettings != null) return (prefsSettings);
    else return (getDefaultSettings (variableName));

  } // getPrefsSettings

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the cached color enhancement settings for the specified
   * variable.
   */
  private ColorEnhancementSettings getCachedSettings (
    String variableName
  ) {

    return ((ColorEnhancementSettings) settingsMap.get (variableName));

  } // getCachedSettings

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the color enhancement settings for the specified
   * variable.
   */
  private ColorEnhancementSettings getEnhancementSettings (
    String variableName
  ) {

    ColorEnhancementSettings settings = getCachedSettings (variableName);
    if (settings != null) return (settings);
    else return (getPrefsSettings (variableName));

  } // getEnhancementSettings

  ////////////////////////////////////////////////////////////

  /** 
   * Restores the color enhancement palette and function settings
   * saved by a previous call to <code>saveEnhancementSettings</code>.
   * If no view settings are available, they are created from default
   * settings.
   *
   * @param view the view to restore.
   */
  private void restoreEnhancementSettings (
    ColorEnhancement view
  ) {

    // Retrieve settings
    // -----------------
    String variableName = view.getGrid().getName();
    ColorEnhancementSettings settings = getCachedSettings (variableName);
    ColorEnhancementSettings prefsSettings = getPrefsSettings (variableName);
    if (settings == null) {
      settings = prefsSettings;
      settingsMap.put (variableName, settings);
    } // if

    // Apply settings
    // --------------
    view.restoreSettings (settings);

    // Update enhancement chooser
    // --------------------------
    enhancementChooser.setRange (prefsSettings.getFunction().getRange());
    enhancementChooser.setStatistics (reader.getStatistics (variableName));
    enhancementChooser.setFunction (settings.getFunction());
    enhancementChooser.setPalette (settings.getPalette());

    // Update palette chooser
    // ----------------------
    paletteChooser.setPalette (settings.getPalette());

  } // restoreEnhancementSettings

  ////////////////////////////////////////////////////////////

  /** 
   * Creates an new controller using the specified reader.  The
   * initial view shows a color enhancement of the first variable in
   * the specified list.
   *
   * @param reader the reader to use.
   * @param variableList the list of variables names to make
   * available.  The only variables currently supported are
   * <code>Grid</code> data variables.
   *
   * @see noaa.coastwatch.util.Grid
   */
  public EarthDataViewController (
    EarthDataReader reader,
    List<String> variableList
  ) {

    // Set reader
    // ----------
    this.reader = reader;

    // Initialize view settings maps
    // -----------------------------
    settingsMap = new HashMap();
    
    // Create variable chooser
    // -----------------------
    variableChooser = new VariableChooser (variableList);
    variableChooser.addPropertyChangeListener (
      VariableChooser.VARIABLE_PROPERTY, new VariableListener());

    // Create enhancement chooser
    // --------------------------
    enhancementChooser = new EnhancementChooser();
    enhancementChooser.addPropertyChangeListener (
      EnhancementChooser.FUNCTION_PROPERTY, new EnhancementListener());

    // Create palette chooser
    // ----------------------
    paletteChooser = new PaletteChooser();
    paletteChooser.addPropertyChangeListener (
      PaletteChooser.PALETTE_PROPERTY, new PaletteListener());

    // Create view panel
    // -----------------
    try {
      enhancementView = new ColorEnhancement (
        reader.getInfo().getTransform(), 
        (Grid) reader.getVariable ((String) variableList.get (0)),
        PaletteFactory.create ("BW-Linear"),
        new LinearEnhancement (new double[] {-50, 50}));
    } // try
    catch (NoninvertibleTransformException e) {
      throw new RuntimeException ("Cannot initialize color enhancement");
    } // catch
    catch (IOException e) {
      throw new RuntimeException ("Cannot set initial variable");
    } // catch
    restoreEnhancementSettings (enhancementView);
    viewPanel = new EarthDataViewPanel (enhancementView);

    // Create legend panel
    // -------------------
    legendPanel = new LegendPanel (viewPanel.getView().getLegend());

    // Create overlay chooser
    // ----------------------
    overlayChooser = new OverlayListChooser (reader, variableList);
    OverlayListener overlayListener = new OverlayListener();
    overlayChooser.addPropertyChangeListener (
      OverlayListChooser.OVERLAY_LIST_PROPERTY, overlayListener);

    // Create survey chooser
    // ---------------------
    surveyChooser = new SurveyListChooser();
    surveyChooser.addPropertyChangeListener (
      SurveyListChooser.SURVEY_LIST_PROPERTY, overlayListener);
    surveyChooser.addSurveyActionListener (new SurveyListener());

    // Create annotation chooser
    // ---------------------
    annotationChooser = new AnnotationListChooser();
    annotationChooser.addPropertyChangeListener (
      AnnotationListChooser.ANNOTATION_LIST_PROPERTY, overlayListener);
    annotationChooser.addAnnotationActionListener (new AnnotationListener());

    // Create composite chooser
    // ------------------------
    compositeChooser = new CompositeChooser (variableList);
    compositeChooser.addPropertyChangeListener (new CompositeListener());

    // Create navigation chooser
    // -------------------------
    navigationChooser = new NavigationChooser (variableList);
    navigationChooser.addPropertyChangeListener (new NavigationListener());

    // Attach listener to operation chooser
    // ------------------------------------
    operationListener = new OperationListener();
    ViewOperationChooser.getInstance().addPropertyChangeListener (
      ViewOperationChooser.OPERATION_PROPERTY, operationListener);
    
    // Create light table
    // ------------------
    lightTable = new LightTable (viewPanel);
    lightTable.addChangeListener (new DrawListener()); 

    // Create navigation analysis panel
    // --------------------------------
    navAnalysisPanel = new NavigationAnalysisPanel (reader, variableList);
    navAnalysisPanel.addPropertyChangeListener (new NavAnalysisListener());

    // TODO: Do we need to make it so that navigation boxes have
    // overlays that show the box outlines?  That was the
    // original design, but using the navigation panel seems to
    // work OK without it.


  } // EarthDataViewController

  ////////////////////////////////////////////////////////////

  /** Disposes of any resources used by this controller. */
  public void dispose () {

    ViewOperationChooser.getInstance().removePropertyChangeListener (
      ViewOperationChooser.OPERATION_PROPERTY, operationListener);
    variableChooser.dispose();
    compositeChooser.dispose();

    // TODO: We found here that when all CDAT windows were closed, the variable
    // chooser was leaking memory (a lot of it).  When we fixed that, it turns
    // out that there are other GUI components leaking a small amount of memory,
    // for example the OverlayListChooser via its listeners.  For now we'll
    // leave it because the impact on overall memory is small, but it should be
    // improved in the future with a strategy for removing listeners
    // that hold onto references for objects that should be getting garbage
    // collected.

  } // dispose

  ////////////////////////////////////////////////////////////

  /** Updates the data reader navigation for the specified variables. */
  private void updateNavigation (
    List variableNames,
    AffineTransform affine
  ) {

    // Stop any current rendering
    // --------------------------
    viewPanel.stopRendering();

    // Give a warning to the user
    // --------------------------
    boolean doNavigation = true;
    if (showWarning) {
      int result = JOptionPane.showConfirmDialog (
        SwingUtilities.getWindowAncestor (viewPanel),
        "The navigation correction will now be applied by permanently\n" +
        "modifying the data file.  This operation can be undone by\n" +
        "selecting 'Reset correction to identity' and clicking 'Perform'.\n" +
        "Are you sure you want to modify the data file?  This warning\n" +
        "will not appear again during this session.",
        "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      doNavigation = (result == JOptionPane.YES_OPTION);
      showWarning = false;
    } // if

    // Update navigation in reader
    // ---------------------------
    boolean success = false;
    if (doNavigation) {
      try {
        reader.updateNavigation (variableNames, affine);
        success = true;
      } // try
      catch (Exception e) {
        JOptionPane.showMessageDialog (
          SwingUtilities.getWindowAncestor (viewPanel),
          "An error occurred writing the navigation correction:\n" +
          e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
      } // catch
    } // if

    // Update navigation in grids
    // --------------------------
    if (success) {

      // Get active grid from enhancement view
      // -------------------------------------
      List updateList = new ArrayList();
      Grid grid = enhancementView.getGrid();
      if (variableNames.contains (grid.getName())) {
        updateList.add (grid);
        enhancementView.invalidate();
      } // if

      // Get active grids from composite view
      // ------------------------------------
      if (viewIsComposite) {
        Grid[] grids = compositeView.getGrids();
        for (int i = 0; i < 3; i++) {
          if (variableNames.contains (grids[i].getName())) {
            updateList.add (grids[i]);
            compositeView.invalidate();
          } // if
        } // for
      } // if

      // Get active grids from overlays
      // ------------------------------
      List overlayList = getView().getOverlays();
      for (Iterator iter = overlayList.iterator(); iter.hasNext(); ) {
        EarthDataOverlay overlay = (EarthDataOverlay) iter.next();
        if (overlay instanceof GridContainerOverlay) {
          GridContainerOverlay container = (GridContainerOverlay) overlay;
          for (Grid containerGrid : container.getGridList()) {
            if (variableNames.contains (containerGrid.getName())) {
              updateList.add (containerGrid);
              overlay.invalidate();
            } // if
          } // for
        } // if
      } // for

      // Update grid navigation
      // ----------------------
      for (Iterator iter = updateList.iterator(); iter.hasNext(); ) {
        grid = (Grid) iter.next();
        AffineTransform oldAffine = grid.getNavigation();
        if (affine == null) grid.setNavigation (null);
        else {
          if (oldAffine.isIdentity()) grid.setNavigation (affine);
          else {
            AffineTransform newAffine = (AffineTransform) oldAffine.clone();
            newAffine.preConcatenate (affine);
            grid.setNavigation (newAffine);
          } // else
        } // else
      } // for

    } // if

    // Repaint panel
    // -------------
    viewPanel.repaint();

  } // updateNavigation

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the navigation analysis panel. */
  private class NavAnalysisListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      String property = event.getPropertyName();

      // Change navigation mode
      // ----------------------
      if (property.equals (NavigationAnalysisPanel.OPERATION_MODE_PROPERTY)) {
        int mode = ((Integer) event.getNewValue()).intValue();
        lightTable.setDrawingMode (mode);
        setInteractionMode (NAV_ANALYSIS_MODE);
        lightTable.setActive (true);
        viewPanel.setDefaultCursor (lightTable.getCursor());
      } // if

    } // propertyChange
  } // NavAnalysisListener class

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the navigation chooser. */
  private class NavigationListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      String property = event.getPropertyName();

      // Change navigation mode
      // ----------------------
      if (property.equals (NavigationChooser.NAVIGATION_MODE_PROPERTY)) {
        String mode = (String) event.getNewValue();
        if (mode.equals (NavigationChooser.TRANSLATION))
          lightTable.setDrawingMode (LightTable.IMAGE_TRANSLATE_MODE);
        else if (mode.equals (NavigationChooser.ROTATION))
          lightTable.setDrawingMode (LightTable.IMAGE_ROTATE_MODE);
        setInteractionMode (NAVIGATION_MODE);
        lightTable.setActive (true);
        viewPanel.setDefaultCursor (lightTable.getCursor());
      } // if

      // Set manual correction
      // ---------------------
      else if (property.equals (NavigationChooser.AFFINE_PROPERTY)) {
        updateNavigation (navigationChooser.getVariables(), 
          (AffineTransform) event.getNewValue());
      } // else if

    } // propertyChange
  } // NavigationListener class

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the composite chooser. */
  private class CompositeListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      String property = event.getPropertyName();
      boolean needsRepaint = false;
      
      // Change view type
      // ----------------
      if (property.equals (CompositeChooser.COMPOSITE_MODE_PROPERTY)) {
        boolean isComposite = compositeChooser.getCompositeMode();

        // Create composite view
        // ---------------------
        if (isComposite) {
          try {
            String redVariable = compositeChooser.getRedComponent();
            String greenVariable = compositeChooser.getGreenComponent();
            String blueVariable = compositeChooser.getBlueComponent();
            Grid[] grids = new Grid[] {
              (Grid) reader.getVariable (redVariable),
              (Grid) reader.getVariable (greenVariable),
              (Grid) reader.getVariable (blueVariable)
            };
            EnhancementFunction[] funcs = new EnhancementFunction[] {
              getEnhancementSettings (redVariable).getFunction(),
              getEnhancementSettings (greenVariable).getFunction(),
              getEnhancementSettings (blueVariable).getFunction(),
            };
            compositeView = new ColorComposite (
              reader.getInfo().getTransform(), grids, funcs);
            compositeView.setProperties (enhancementView);
          } // try
          catch (Exception e) { return; }
        } // if

        // Create enhancement view
        // -----------------------
        else {
          enhancementView.setProperties (compositeView);
        } // else

        // Change view mode
        // ----------------
        if (isComposite) {
          viewPanel.setView (compositeView);
          legendPanel.setEnabled (false);
        } // if
        else {
          compositeView = null;
          viewPanel.setView (enhancementView);
          legendPanel.setLegend (enhancementView.getLegend());
          legendPanel.setEnabled (true);
        } // else
        viewIsComposite = isComposite;
        needsRepaint = true;

      } // if

      // Change composite variable
      // -------------------------
      else if (viewIsComposite) {
        String var = null;
        int index = -1;
        if (property.equals (CompositeChooser.RED_COMPONENT_PROPERTY)) {
          var = compositeChooser.getRedComponent();
          index = ColorComposite.RED;
        } // if
        else if (property.equals (CompositeChooser.GREEN_COMPONENT_PROPERTY)) {
          var = compositeChooser.getGreenComponent();
          index = ColorComposite.GREEN;
        } // else if
        else if (property.equals (CompositeChooser.BLUE_COMPONENT_PROPERTY)) {
          var = compositeChooser.getBlueComponent();
          index = ColorComposite.BLUE;
        } // else if
        if (var != null) {
          try {
            viewPanel.stopRendering();
            Grid[] grids = compositeView.getGrids();
            grids[index] = (Grid) reader.getVariable (var);
            compositeView.setGrids (grids);
            EnhancementFunction[] funcs = compositeView.getFunctions();
            funcs[index] = getEnhancementSettings (var).getFunction();
            compositeView.setFunctions (funcs);
            needsRepaint = true;
          } // try
          catch (IOException e) { return; }
        } // if
      } // else if

      // Repaint view panel
      // ------------------
      if (needsRepaint) {
        viewPanel.repaint();
        legendPanel.repaint();
      } // if
      
    } // propertyChange
  } // CompositeListener class

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the variable chooser. */
  private class VariableListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      // Check current variable name
      // ---------------------------
      String newName = (String) event.getNewValue();
      if (enhancementView.getGrid().getName().equals (newName)) return;

      // Update view
      // -----------
      if (!viewIsComposite) viewPanel.stopRendering();
      Grid grid;
      try { grid = (Grid) reader.getVariable (newName); }
      catch (IOException e) { 
        throw new RuntimeException ("Cannot set variable " + newName + ": " +
          e.getMessage());
      } // catch
      enhancementView.setGrid (grid);
      restoreEnhancementSettings (enhancementView);
      if (!viewIsComposite) viewPanel.repaint();
      legendPanel.setLegend (enhancementView.getLegend());
      legendPanel.repaint();

    } // propertyChange
  } // VariableListener class

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the palette chooser. */
  private class PaletteListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      // Check current palette
      // ---------------------
      Palette palette = (Palette) event.getNewValue();
      if (enhancementView.getPalette().equals (palette)) return;

      // Update enhancement chooser
      // --------------------------
      enhancementChooser.setPalette (palette);

      // Update view
      // -----------
      if (!viewIsComposite) viewPanel.stopRendering();
      enhancementView.setPalette (palette);
      saveEnhancementSettings (enhancementView);
      if (!viewIsComposite)  viewPanel.repaint();
      legendPanel.setLegend (enhancementView.getLegend());
      legendPanel.repaint();

    } // propertyChange
  } // PaletteListener class

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the enhancement chooser. */
  private class EnhancementListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {
      
      // Check current function
      // ----------------------
      EnhancementFunction func = (EnhancementFunction) event.getNewValue();
      if (enhancementView.getFunction().equals (func)) return;

      // Update adjusting enhancement view
      // ---------------------------------
      boolean needsRepaint = false;
      if (enhancementChooser.getValueIsAdjusting()) {
        if (!viewIsComposite) {
          viewPanel.stopRendering();
          enhancementView.setAdjustingFunction (func);
          needsRepaint = true;
        } // if
      } // if

      // Update normal enhancement view
      // ------------------------------
      else {
        if (!viewIsComposite) viewPanel.stopRendering();
        enhancementView.setFunction (func);
        saveEnhancementSettings (enhancementView);

        // Update composite view
        // ---------------------
        if (viewIsComposite) {
          String var = variableChooser.getVariable();
          Grid[] grids = compositeView.getGrids();
          int index = -1;
          for (int i = 0; i < 3; i++)
            if (grids[i].getName().equals (var)) {index = i ; break; }
          if (index != -1) {
            EnhancementFunction[] funcs = compositeView.getFunctions();
            funcs[index] = func;
            viewPanel.stopRendering();
            compositeView.setFunctions (funcs);
            needsRepaint = true;
          } // if
        } // if
        else {
          needsRepaint = true;
        } // else

      } // else

      // Repaint view panel
      // ------------------
      if (needsRepaint) {
        viewPanel.repaint();
        legendPanel.setLegend (enhancementView.getLegend());
        legendPanel.repaint();
      } // if

    } // propertyChange
  } // EnhancementListener class

  ////////////////////////////////////////////////////////////

  /** Gets the current earth data view. */
  private EarthDataView getView() { 

    if (viewIsComposite) return (compositeView);
    else return (enhancementView);

  } // getView
  
  ////////////////////////////////////////////////////////////

  /** 
   * Gets the overlay list chooser created by this controller.
   *
   * @return the overlay list chooser.
   */
  public OverlayListChooser getOverlayChooser() { return (overlayChooser); }
  
  ////////////////////////////////////////////////////////////

  /** 
   * Gets the palette chooser created by this controller.
   *
   * @return the palette chooser.
   */
  public PaletteChooser getPaletteChooser() { return (paletteChooser); }
  
  ////////////////////////////////////////////////////////////

  /** 
   * Gets the enhancement chooser created by this controller.
   *
   * @return the enhancement chooser.
   */
  public EnhancementChooser getEnhancementChooser() { return (enhancementChooser); }

  ////////////////////////////////////////////////////////////

  /** Gets the current earth data view navigation. */
  private AffineTransform getNavigation() { 

    if (viewIsComposite) return (compositeView.getGrids()[0].getNavigation());
    else return (enhancementView.getGrid().getNavigation());

  } // getNavigation

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by overlay choosers. */
  private class OverlayListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {
      
      // Get values
      // ----------
      EarthDataOverlay oldOverlay = (EarthDataOverlay) event.getOldValue();
      EarthDataOverlay newOverlay = (EarthDataOverlay) event.getNewValue();
      EarthDataView view = getView();

      // Stop any rendering
      // ------------------
      viewPanel.stopRendering();

      // Remove old overlay from view
      // ----------------------------
      if (oldOverlay != null) {
        view.removeOverlay (oldOverlay);
      } // if

      // Add overlay to view
      // -------------------
      if (newOverlay != null) {
        if (!view.containsOverlay (newOverlay))
          view.addOverlay (newOverlay);
        else
          view.setChanged();
      } // else if

      // Repaint view panel
      // ------------------
      viewPanel.repaint();

    } // propertyChange
  } // OverlayListener class

  ////////////////////////////////////////////////////////////

  /** Resets the interaction mode. */
  public void resetInteraction () {

    ViewOperationChooser.getInstance().deactivate();
    surveyChooser.deactivate();
    annotationChooser.deactivate(); 
    navigationChooser.deactivate();
    navAnalysisPanel.deactivate();
    lightTable.setActive (false);
    viewPanel.setDefaultCursor (Cursor.getDefaultCursor());

  } // resetInteraction

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the current interaction mode.  The choosers are updated
   * accordingly. 
   */
  private void setInteractionMode (
    int interactionMode
  ) {

    this.interactionMode = interactionMode;
    switch (interactionMode) {
    case SURVEY_MODE:
      ViewOperationChooser.getInstance().deactivate();
      annotationChooser.deactivate(); 
      navigationChooser.deactivate();
      navAnalysisPanel.deactivate();
      break;
    case VIEW_MODE:
      surveyChooser.deactivate(); 
      annotationChooser.deactivate(); 
      navigationChooser.deactivate();
      navAnalysisPanel.deactivate();
      break;
    case ANNOTATION_MODE:
      ViewOperationChooser.getInstance().deactivate();
      surveyChooser.deactivate(); 
      navigationChooser.deactivate();
      navAnalysisPanel.deactivate();
      break;
    case NAVIGATION_MODE:
      ViewOperationChooser.getInstance().deactivate();
      surveyChooser.deactivate(); 
      annotationChooser.deactivate(); 
      navAnalysisPanel.deactivate();
      break;
    case NAV_ANALYSIS_MODE:
      ViewOperationChooser.getInstance().deactivate();
      surveyChooser.deactivate(); 
      annotationChooser.deactivate(); 
      navigationChooser.deactivate();
      break;
    default:
      throw new IllegalStateException();
    } // switch

  } // setInteractionMode

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the survey chooser. */
  private class SurveyListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Set light table
      // ---------------
      if (command.equals (SurveyListChooser.POINT_COMMAND))
        lightTable.setDrawingMode (LightTable.POINT_MODE);
      else if (command.equals (SurveyListChooser.LINE_COMMAND))
        lightTable.setDrawingMode (LightTable.LINE_MODE);
      else if (command.equals (SurveyListChooser.BOX_COMMAND))
        lightTable.setDrawingMode (LightTable.BOX_MODE);
      else if (command.equals (SurveyListChooser.POLYGON_COMMAND))
        lightTable.setDrawingMode (LightTable.POLYLINE_MODE);
      else {
        throw new IllegalArgumentException (
          "Unknown survey command " + command);
      } // else

      // Setup for survey
      // ----------------
      setInteractionMode (SURVEY_MODE);
      lightTable.setActive (true);
      viewPanel.setDefaultCursor (lightTable.getCursor());

    } // actionPerformed
  } // SurveyListener class

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the annotation chooser. */
  private class AnnotationListener implements ActionListener {
    public void actionPerformed (ActionEvent event) {

      String command = event.getActionCommand();

      // Set light table
      // ---------------
      if (command.equals (AnnotationListChooser.LINE_COMMAND))
        lightTable.setDrawingMode (LightTable.LINE_MODE);
      else if (command.equals (AnnotationListChooser.POLYLINE_COMMAND))
        lightTable.setDrawingMode (LightTable.POLYLINE_MODE);
      else if (command.equals (AnnotationListChooser.BOX_COMMAND))
        lightTable.setDrawingMode (LightTable.BOX_MODE);
      else if (command.equals (AnnotationListChooser.POLYGON_COMMAND))
        lightTable.setDrawingMode (LightTable.POLYLINE_MODE);
      else if (command.equals (AnnotationListChooser.CIRCLE_COMMAND))
        lightTable.setDrawingMode (LightTable.CIRCLE_MODE);
      else if (command.equals (AnnotationListChooser.CURVE_COMMAND))
        lightTable.setDrawingMode (LightTable.POLYLINE_MODE);
      else if (command.equals (AnnotationListChooser.TEXT_COMMAND))
        lightTable.setDrawingMode (LightTable.POINT_MODE);
      else {
        throw new IllegalArgumentException (
          "Unknown annotation command " + command);
      } // else

      // Setup for annotation
      // --------------------
      setInteractionMode (ANNOTATION_MODE);
      lightTable.setActive (true);
      Cursor cursor;
      if (command.equals (AnnotationListChooser.TEXT_COMMAND))
        cursor = Cursor.getPredefinedCursor (Cursor.TEXT_CURSOR);
      else
        cursor = lightTable.getCursor();
      viewPanel.setDefaultCursor (cursor);

    } // actionPerformed
  } // AnnotationListener class

  ////////////////////////////////////////////////////////////

  /** Handles change events generated by the operation chooser. */
  private class OperationListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      // Get view operation
      // ------------------
      String operation = (String) event.getNewValue();

      // Find out if view is currently showing
      // -------------------------------------
      boolean showing = viewPanel.isShowing();

      // Perform proxy-related operations
      // --------------------------------
      if (operation.equals (ViewOperationChooser.ZOOM)) {
        setInteractionMode (VIEW_MODE);
        lightTable.setDrawingMode (LightTable.BOX_ZOOM_MODE);
        lightTable.setActive (true);
        viewPanel.setDefaultCursor (lightTable.getCursor());
      } // if
      else if (operation.equals (ViewOperationChooser.PAN)) {
        setInteractionMode (VIEW_MODE);
        lightTable.setDrawingMode (LightTable.IMAGE_MODE);
        lightTable.setActive (true);
        viewPanel.setDefaultCursor (lightTable.getCursor());
      } // else if
      else if (operation.equals (ViewOperationChooser.RECENTER)) {
        setInteractionMode (VIEW_MODE);
        lightTable.setDrawingMode (LightTable.POINT_MODE);
        lightTable.setActive (true);
        viewPanel.setDefaultCursor (lightTable.getCursor());
      } // else if

      // Perform single-click operations
      // -------------------------------
      else if (showing && operation.equals (ViewOperationChooser.MAGNIFY)) {
        viewPanel.magnify (2);
        viewPanel.repaint();
      } // if
      else if (showing && operation.equals (ViewOperationChooser.SHRINK)) {
        viewPanel.magnify (0.5);
        viewPanel.repaint();
      } // else if
      else if (showing && operation.equals (ViewOperationChooser.ONE_TO_ONE)) {
        viewPanel.unityMagnify();
        viewPanel.repaint();
      } // else if
      else if (showing && operation.equals (ViewOperationChooser.FIT)) {
        viewPanel.fitReset();
        viewPanel.repaint();
      } // else if
      else if (showing && operation.equals (ViewOperationChooser.RESET)) {
        viewPanel.reset();
        viewPanel.repaint();
      } // else if

    } // propertyChange
  } // OperationListener class

  ////////////////////////////////////////////////////////////

  /** Performs a view operation using the specified shape. */
  private void performViewOperation (
    Shape s
  ) {

    // Get view operation
    // ------------------
    String operation = ViewOperationChooser.getInstance().getOperation();
    if (operation == null) return;
    
    // Perform zoom
    // ------------
    if (operation.equals (ViewOperationChooser.ZOOM)) {
      Rectangle rect = ((Rectangle2D) s).getBounds();
      if (rect.width == 0 || rect.height == 0) return;
      viewPanel.magnify (rect);
      viewPanel.repaint();
    } // if

    // Perform pan
    // -----------
    else if (operation.equals (ViewOperationChooser.PAN)) {
      Line2D line = (Line2D) s;
      Point2D p1 = line.getP1();
      Point2D p2 = line.getP2();
      if (p1.equals (p2)) return;
      Dimension dims = viewPanel.getSize();
      Point2D center = new Point2D.Double (dims.width/2, dims.height/2);
      center.setLocation (
        center.getX() + (p1.getX() - p2.getX()),
        center.getY() + (p1.getY() - p2.getY())
      );
      viewPanel.setCenter (center);
      viewPanel.repaint();
    } // else if
    
    // Perform recenter
    // ----------------
    else if (operation.equals (ViewOperationChooser.RECENTER)) {
      Line2D line = (Line2D) s;
      viewPanel.setCenter (line.getP1());
      viewPanel.repaint();
    } // else if

  } // performViewOperation

  ////////////////////////////////////////////////////////////

  /** Performs a survey using the specified shape. */
  private void performSurvey (
    Shape s
  ) {

    // TODO: When surveys are being performed, we need to change the
    // cursor to an hourglass.  Since the survey is currently being
    // performed in the event processing thread, that means we would
    // have to move it to a new worker thread and start it "in the
    // background", while changing the cursor in the event processing
    // thread.

    String surveyCommand = surveyChooser.getSurveyCommand();
    ImageTransform imageTrans = getView().getTransform().getImageTransform();
    EarthTransform earthTrans = getView().getTransform().getEarthTransform();
    DataVariable var = enhancementView.getGrid();
    EarthDataSurvey survey = null;

    // Survey point
    // ------------
    if (surveyCommand.equals (SurveyListChooser.POINT_COMMAND)) {
      Line2D line = (Line2D) s;
      DataLocation loc = 
        imageTrans.transform (viewPanel.translate (line.getP1())).round();
      survey = new PointSurvey (var, earthTrans, loc);
    } // if

    // Survey line
    // -----------
    else if (surveyCommand.equals (SurveyListChooser.LINE_COMMAND)) {
      Line2D line = (Line2D) s;
      DataLocation start = 
        imageTrans.transform (viewPanel.translate (line.getP1())).round();
      DataLocation end = 
        imageTrans.transform (viewPanel.translate (line.getP2())).round();
      if (start.equals (end))
        survey = new PointSurvey (var, earthTrans, start);
      else
        survey = new LineSurvey (var, earthTrans, start, end);
    } // else if 

    // Survey box
    // ----------
    else if (surveyCommand.equals (SurveyListChooser.BOX_COMMAND)) { 
      Rectangle rect = ((Rectangle2D) s).getBounds();
      Point startPoint = new Point (rect.x, rect.y);
      DataLocation start = 
        imageTrans.transform (viewPanel.translate (startPoint)).round();
      Point endPoint = new Point (rect.x + rect.width, 
        rect.y + rect.height);
      DataLocation end = 
        imageTrans.transform (viewPanel.translate (endPoint)).round();
      if (start.get(0) > end.get(0)) {
        DataLocation temp = start;
        start = end;
        end = temp;
      } // if
      if (start.equals (end))
        survey = new PointSurvey (var, earthTrans, start);
      else
        survey = new BoxSurvey (var, earthTrans, start, end);
    } // else if

    // Survey polygon
    // --------------
    else if (surveyCommand.equals (SurveyListChooser.POLYGON_COMMAND)) { 
      AffineTransform affine = viewPanel.getAffine();
      GeneralPath path = (GeneralPath) s;
      path.closePath();
      Shape shape = affine.createTransformedShape (path);
      if (new Area (shape).isEmpty()) {
        Rectangle2D bounds = shape.getBounds2D();
        DataLocation start = new DataLocation (bounds.getX(), 
          bounds.getY()).round();
        survey = new PointSurvey (var, earthTrans, start);
      } // if
      else
        survey = new PolygonSurvey (var, earthTrans, shape);
    } // else if

    // Submit survey to chooser
    // ------------------------
    surveyChooser.addSurvey (survey);

  } // performSurvey

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a Bezier curve given a general path of polyline
   * segments.
   *
   * @param polyline the general path of polyline segments.
   *
   * @return a new Bezier curve that follows the polyline.
   */
  private GeneralPath createBezier (
    GeneralPath polyline
  ) {

    // Get iterator for polyline
    // -------------------------
    PathIterator iterator = polyline.getPathIterator (null);
    float[] coords = new float[6];

    // Initialize curve points
    // -----------------------
    Point2D.Float p1 = new Point2D.Float();
    Point2D.Float p2 = new Point2D.Float();
    Point2D.Float p3 = new Point2D.Float();
    int type = iterator.currentSegment (coords);
    if (type != PathIterator.SEG_MOVETO)
      throw new IllegalArgumentException ("Path has no initial MOVETO");
    p1.setLocation (coords[0], coords[1]);

    iterator.next();
    type = iterator.currentSegment (coords);
    if (type != PathIterator.SEG_LINETO)
      throw new IllegalArgumentException ("Path may only contain LINETO");
    p2.setLocation (coords[0], coords[1]);

    // Initialize curve
    // ----------------
    GeneralPath curve = new GeneralPath();
    curve.moveTo (p1.x, p1.y);
    curve.lineTo ((p1.x + p2.x)/2, (p1.y + p2.y)/2);
    
    // Loop over each point to create curve
    // ------------------------------------
    iterator.next();
    for ( ; !iterator.isDone(); iterator.next()) {

      // Get next point
      // --------------
      type = iterator.currentSegment (coords);
      if (type != PathIterator.SEG_LINETO)
        throw new IllegalArgumentException ("Path may only contain LINETO");
      p3.setLocation (coords[0], coords[1]);

      // Draw curve
      // ----------
      curve.curveTo (
        p1.x*0.25f + p2.x*0.75f, p1.y*0.25f + p2.y*0.75f,
        p2.x*0.75f + p3.x*0.25f, p2.y*0.75f + p3.y*0.25f,
        (p2.x + p3.x)/2, (p2.y + p3.y)/2
      );

      // Rotate points
      // -------------
      Point2D.Float tmp = p1;
      p1 = p2;
      p2 = p3;
      p3 = tmp;

    } // for

    // Finish curve
    // ------------
    curve.lineTo (p2.x, p2.y);

    return (curve);

  } // createBezier

  ////////////////////////////////////////////////////////////

  /** Performs an annotation using the specified shape. */
  private void performAnnotation (
    Shape s
  ) {

    String command = annotationChooser.getAnnotationCommand();

    // Check for empty shape
    // ---------------------
    if (!command.equals (AnnotationListChooser.TEXT_COMMAND)) {
      Rectangle2D bounds = s.getBounds2D();
      if (bounds.getWidth() == 0 && bounds.getHeight() == 0) return;
    } // if

    // Close path if polygon
    // ---------------------
    if (command.equals (AnnotationListChooser.POLYGON_COMMAND))
      ((GeneralPath) s).closePath();

    // Create Bezier if curve
    // ----------------------
    else if (command.equals (AnnotationListChooser.CURVE_COMMAND))
      s = createBezier ((GeneralPath) s);

    // Transform shape to data coordinates
    // -----------------------------------
    AffineTransform affine = viewPanel.getAffine();
    Shape newShape = affine.createTransformedShape (s);

    annotationChooser.addAnnotation (newShape);

  } // performAnnotation

  ////////////////////////////////////////////////////////////

  /** Performs navigation correction using the specified shape. */
  private void performNavigation (
    Shape s
  ) {

    // Get navigation endpoints
    // ------------------------
    Line2D line = (Line2D) s;
    Point2D p1 = line.getP1();
    Point2D p2 = line.getP2();
    if (p1.equals (p2)) return;
    ImageTransform imageTrans = getView().getTransform().getImageTransform();
    AffineTransform nav = getNavigation();
    DataLocation loc1 = 
      imageTrans.transform (viewPanel.translate (p1)).transform (nav);
    DataLocation loc2 = 
      imageTrans.transform (viewPanel.translate (p2)).transform (nav);

    // Derive translation correction
    // -----------------------------
    String mode = navigationChooser.getMode();
    AffineTransform affine = null;
    if (mode.equals (NavigationChooser.TRANSLATION)) {
      affine = AffineTransform.getTranslateInstance (
        loc1.get(0) - loc2.get(0), loc1.get(1) - loc2.get(1));
    } // if

    // Derive rotation correction
    // --------------------------
    else if (mode.equals (NavigationChooser.ROTATION)) {
      Dimension size = viewPanel.getSize();
      Point pc = new Point (size.width/2, size.height/2);
      DataLocation centerLoc = 
        imageTrans.transform (viewPanel.translate (pc)).transform (nav);
      double baseAngle = Math.atan2 (-(loc1.get(0) - centerLoc.get(0)),
        loc1.get(1) - centerLoc.get(1));
      double pointAngle = Math.atan2 (-(loc2.get(0) - centerLoc.get(0)),
        loc2.get(1) - centerLoc.get(1));
      double angle = baseAngle - pointAngle;
      affine = AffineTransform.getRotateInstance (angle, centerLoc.get(0),
        centerLoc.get(1));
    } // else if

    // Update navigation
    // -----------------
    viewPanel.setImageAffine (null);
    updateNavigation (navigationChooser.getVariables(), affine);

  } // performNavigation

  ////////////////////////////////////////////////////////////

  /** Performs navigation analysis using the specified shape. */
  private void performNavAnalysis (
    Shape s
  ) {

    AffineTransform affine = viewPanel.getAffine();
    Line2D line = (Line2D) s;
    Line2D.Double newLine = new Line2D.Double (
      affine.transform (line.getP1(), null), 
      affine.transform (line.getP2(), null)
    );
    navAnalysisPanel.performOperation (newLine);

  } // performNavAnalysis

  ////////////////////////////////////////////////////////////

  /** Handles events generated by the light table. */
  private class DrawListener implements ChangeListener {
    public void stateChanged (ChangeEvent event) {

      // Check if panel is showing
      // -------------------------
      if (!viewPanel.isShowing()) return;

      // Get drawing shape
      // -----------------
      Shape s = lightTable.getShape();

      // Perform operation
      // -----------------
      switch (interactionMode) {
      case VIEW_MODE: performViewOperation (s); break;
      case SURVEY_MODE: performSurvey (s); break;
      case ANNOTATION_MODE: performAnnotation (s); break;
      case NAVIGATION_MODE: performNavigation (s); break;
      case NAV_ANALYSIS_MODE: performNavAnalysis (s); break;
      default: throw new IllegalStateException();
      } // if

    } // stateChanged
  } // DrawListener class

  ////////////////////////////////////////////////////////////

} // EarthDataViewController

////////////////////////////////////////////////////////////////////////
