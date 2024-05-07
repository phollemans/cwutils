////////////////////////////////////////////////////////////////////////
/*

     File: SurveyListChooser.java
   Author: Peter Hollemans
     Date: 2004/03/26

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

// TODO: There should be a way to save survey data.
//       o For lines: Save plot data to PDF file.
//                    Save line data to lat/lon/value ASCII file.
//                    Save results to text file.
//       o For boxes: Save plot data to PDF file.
//                    Save results to text file.
//       Also, it may be useful to save the survey overlay itself as
//       lat/lon locations via a group save.

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.JEditorPane;
import javax.swing.UIManager;

import noaa.coastwatch.gui.AbstractOverlayListPanel;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.SurveyPlotFactory;
import noaa.coastwatch.gui.TabComponent;
import noaa.coastwatch.render.SurveyOverlay;
import noaa.coastwatch.util.EarthDataSurvey;
import noaa.coastwatch.util.HTMLReportFormatter;

/**
 * <p>The <code>SurveyListChooser</code> class is a panel that
 * allows the user to manipulate a list of {@link
 * EarthDataSurvey} objects.  The user may add a new point, line,
 * or box survey, edit the survey color, name and linestyle, and
 * change the survey layer.</p>
 *
 * <p>The chooser signals a change in the survey list by firing a
 * property change event whose property name is given by
 * <code>SURVEY_LIST_PROPERTY</code>.  See the {@link
 * AbstractOverlayListPanel} class for details on how the property
 * change events should be interpreted.</p>
 *
 * <p>Surveys require that extra information be provided from the user
 * object.  The chooser signals that it requires input for a survey by
 * firing an action event whose action command specifies the type of
 * input required as <code>POINT_COMMAND</code>,
 * <code>LINE_COMMAND</code>, or <code>BOX_COMMAND</code>.  The user
 * object should perform some operation to obtain the survey input
 * information, and then pass it to the <code>addSurvey()</code>
 * method.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class SurveyListChooser
  extends JPanel
  implements TabComponent, RequestHandler {

  // Constants
  // ---------

  /** The survey list property. */
  public static final String SURVEY_LIST_PROPERTY = 
    AbstractOverlayListPanel.OVERLAY_PROPERTY;

  /** The point survey command. */
  public static final String POINT_COMMAND = "Point";

  /** The line survey command. */
  public static final String LINE_COMMAND = "Line";

  /** The box survey command. */
  public static final String BOX_COMMAND = "Rectangle";

  /** The polygon survey command. */
  public static final String POLYGON_COMMAND = "Polygon";

  /** The survey list tooltip. */
  private static final String SURVEY_LIST_TOOLTIP = "Data Surveys";

  /** The survey list panel title. */
  private static final String SURVEY_TITLE = "Survey";

  // Variables
  // ---------

  /** The survey list panel. */
  private SurveyListPanel listPanel;

  /** The list of action listeners. */
  private List<ActionListener> actionListeners;

  /** The last survey command executed. */
  private String surveyCommand;

  /** The panel used for survey results. */
  private JEditorPane resultsArea;

  /** The plot area used for survey plot results. */
  private JPanel plotPanel;

  /** The map of surveys to plot panels. */
  private Map surveyPlotMap;

  private DispatchTable dispatch;

  ////////////////////////////////////////////////////////////

  /**
   * 
   * @since 3.8.1
   */
  public static List<AbstractButton> getToolBarButtons () {

    var className = SurveyListChooser.class.getName();

    var button = new DropDownButton (GUIServices.createAction (className, "perform_survey", "Survey", "layers.survey", "Perform data survey"));
    button.addItem (GUIServices.createAction (className, "survey_point", "Single point", "survey.menu.point", null));
    button.addItem (GUIServices.createAction (className, "survey_line", "Line", "survey.menu.line", null));
    button.addItem (GUIServices.createAction (className, "survey_box", "Rectangular area", "survey.menu.box", null));
    button.addItem (GUIServices.createAction (className, "survey_polygon", "Polygon area", "survey.menu.polygon", null));

    return (List.of (button));

  } // getToolBarButtons

  ////////////////////////////////////////////////////////////

  private void startSurveyMode (String command) {

    surveyCommand = command;    
    var event = new ActionEvent (this, 0, surveyCommand);  
    for (var listener : actionListeners) listener.actionPerformed (event);

  } // startSurveyMode

  ////////////////////////////////////////////////////////////

  @Override
  public void handleRequest (Request request) { dispatch.handleRequest (request); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean canHandleRequest (Request request) { return (dispatch.canHandleRequest (request)); }

  ////////////////////////////////////////////////////////////

  /** Gets the last survey command executed. */
  public String getSurveyCommand () { return (surveyCommand); }

  ////////////////////////////////////////////////////////////

  /** 
   * Adds the specified listener for receiving survey input action
   * commands. 
   */
  public void addSurveyActionListener (
    ActionListener listener
  ) {

    actionListeners.add (listener);

  } // addSurveyActionListener

  ////////////////////////////////////////////////////////////

  /** Creates a new survey list chooser. */
  public SurveyListChooser () {

    super (new GridLayout (2, 1));

    // Create main panel
    // -----------------
    listPanel = new SurveyListPanel();
    listPanel.addPropertyChangeListener (SurveyListPanel.SELECTION_PROPERTY,
      new PropertyChangeListener() {
        public void propertyChange (PropertyChangeEvent event) {
          SurveyOverlay overlay = (SurveyOverlay) event.getNewValue();
          if (overlay == null)
            clearSurvey();
          else
            showSurvey (overlay.getSurvey());
        } // propertyChange
      });
    this.add (listPanel);

    // Create results panel
    // --------------------
    JTabbedPane resultsPanel = new JTabbedPane();
    this.add (resultsPanel);

    // Create results area
    // -------------------
    this.resultsArea = new JEditorPane();
    resultsArea.setContentType ("text/html");
    resultsArea.putClientProperty (JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    resultsArea.setEditable (false);
    JScrollPane scrollPane = new JScrollPane (resultsArea);
    resultsPanel.add ("Results", scrollPane);

    // Create plot panel
    // -----------------
    surveyPlotMap = new HashMap();
    plotPanel = new JPanel (new BorderLayout());
    resultsPanel.add ("Plot", plotPanel);

    // Create list of survey action listeners
    // --------------------------------------
    actionListeners = new ArrayList<>();

    dispatch = new DispatchTable (SurveyListChooser.class.getName());
    dispatch.addDispatch ("survey_point", () -> startSurveyMode (POINT_COMMAND));
    dispatch.addDispatch ("survey_line", () -> startSurveyMode (LINE_COMMAND));
    dispatch.addDispatch ("survey_box", () -> startSurveyMode (BOX_COMMAND));
    dispatch.addDispatch ("survey_polygon", () -> startSurveyMode (POLYGON_COMMAND));

  } // SurveyListChooser constructor

  ////////////////////////////////////////////////////////////

  @Override
  public String getToolTip () { return (SURVEY_LIST_TOOLTIP); }

  ////////////////////////////////////////////////////////////

  @Override
  public String getTitle () { return (null); }

  ////////////////////////////////////////////////////////////

  @Override
  public Icon getIcon () { return (GUIServices.getIcon ("survey.tab")); }

  ////////////////////////////////////////////////////////////

  /** Redirects overlay property listeners to the list panel. */
  public void addPropertyChangeListener (
    String propertyName,
    PropertyChangeListener listener
  ) {

    if (propertyName.equals (AbstractOverlayListPanel.OVERLAY_PROPERTY))
      listPanel.addPropertyChangeListener (propertyName, listener);
    else
      super.addPropertyChangeListener (propertyName, listener);

  } // addPropertyChangeListener

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a new survey.  A survey overlay is created for the survey
   * and the overlay added to the list.
   *
   * @param survey the new data survey to add.
   */
  public void addSurvey (
    EarthDataSurvey survey
  ) {

    // Add plot panel to map
    // ---------------------
    JPanel newPlotPanel = SurveyPlotFactory.create (survey);
    surveyPlotMap.put (survey, newPlotPanel);

    // Add survey overlay to list
    // --------------------------
    SurveyOverlay overlay = new SurveyOverlay (survey, new Color (255, 84, 207));
    overlay.setDropShadow (true);
    overlay.setStroke (new BasicStroke (2));
    String className = survey.getClass().getName();
    var surveyType = className.replaceAll (".*\\.([A-Za-z]*)Survey", "$1");
    if (surveyType.equals ("Box")) surveyType = "Rectangle";
    overlay.setName (surveyType + listPanel.getOverlayCount (survey.getClass()));
    listPanel.addOverlay (overlay);

  } // addSurvey

  ////////////////////////////////////////////////////////////

  /** Deactivates the survey so that no survey is selected. */
  public void deactivate () { } //listPanel.hidden.setSelected (true); }

  ////////////////////////////////////////////////////////////

  /** Clears the survey results. */
  private void clearSurvey () {

    // Clear results text
    // ------------------
    resultsArea.setText ("");

    // Clear statistics plot
    // ---------------------
    plotPanel.removeAll();
    if (plotPanel.isShowing()) {
      plotPanel.revalidate();
      plotPanel.repaint();
    } // if

  } // clearSurvey

  ////////////////////////////////////////////////////////////

  /** Shows the survey results for the specified survey. */
  private void showSurvey (
    EarthDataSurvey survey
  ) {

    // TODO: It would be more clear which survey is currently selected
    // if the survey overlay itself changed.  For example, small black
    // squares on the endpoints of the survey shape.

    // Set results text
    // ----------------
    var formatter = HTMLReportFormatter.create();
    formatter.setSpacing (2);
    survey.getResults (formatter);
    resultsArea.setText (formatter.getContent());
    resultsArea.setCaretPosition (0);


    // resultsArea.setText (survey.getResults());
    // resultsArea.setCaretPosition (0);


    // Set statistics plot
    // -------------------
    plotPanel.removeAll();
    plotPanel.add ((JPanel) surveyPlotMap.get (survey), BorderLayout.CENTER);
    if (plotPanel.isShowing()) {
      plotPanel.revalidate();
      plotPanel.repaint();
    } // if

  } // showSurvey

  ////////////////////////////////////////////////////////////
  
  /** Implements survey list buttons and title. */
  private class SurveyListPanel 
    extends AbstractOverlayListPanel {

    // Variables
    // ---------

    /** The invisible button used for turning off the button group. */
    public JToggleButton hidden;

    ////////////////////////////////////////////////////////

    /** Creates a new survey list panel. */
    public SurveyListPanel () {

      setBaseLayer (1000);

    } // SurveyListPanel constructor

    ////////////////////////////////////////////////////////

    /** Creates the overlay list add buttons. */
    protected List getAddButtons () {

//       // Create button list and listener
//       // -------------------------------
//       List buttons = new LinkedList();
//       ActionListener buttonListener = new SurveyListener();

//       // Create buttons
//       // --------------
//       ButtonGroup group = new ButtonGroup();
// //      JToggleButton button;
//       JButton button;

// //      button = GUIServices.getIconToggle ("survey.point");
//       button = GUIServices.getIconButton ("survey.point");
//       button.setActionCommand (POINT_COMMAND);
//       button.addActionListener (buttonListener);
//       button.setToolTipText (POINT_COMMAND);
//       buttons.add (button);
// //      group.add (button);

// //      button = GUIServices.getIconToggle ("survey.line");
//       button = GUIServices.getIconButton ("survey.line");
//       button.setActionCommand (LINE_COMMAND);
//       button.addActionListener (buttonListener);
//       button.setToolTipText (LINE_COMMAND);
//       buttons.add (button);
// //      group.add (button);

// //      button = GUIServices.getIconToggle ("survey.box");
//       button = GUIServices.getIconButton ("survey.box");
//       button.setActionCommand (BOX_COMMAND);
//       button.addActionListener (buttonListener);
//       button.setToolTipText (BOX_COMMAND);
//       buttons.add (button);
// //      group.add (button);

// //      button = GUIServices.getIconToggle ("survey.polygon");
//       button = GUIServices.getIconButton ("survey.polygon");
//       button.setActionCommand (POLYGON_COMMAND);
//       button.addActionListener (buttonListener);
//       button.setToolTipText (POLYGON_COMMAND);
//       buttons.add (button);
// //      group.add (button);

//       hidden = new JToggleButton();
//       group.add (hidden);

//       return (buttons);

      return (null);

    } // getAddButtons

    ////////////////////////////////////////////////////////

    /** Gets the survey list title. */
    protected String getTitle () { return (SURVEY_TITLE); }

    ////////////////////////////////////////////////////////

    /** Gets the survey button panel title. */
    protected String getButtonTitle () { return (SURVEY_TITLE + " Mode"); }

    ////////////////////////////////////////////////////////

    /** Handles survey add events. */
    private class SurveyListener implements ActionListener {
      public void actionPerformed (ActionEvent event) {

        surveyCommand = event.getActionCommand();
        for (Iterator iter = actionListeners.iterator(); iter.hasNext(); )
          ((ActionListener) iter.next()).actionPerformed (event);

      } // actionPerformed
    } // SurveyListener class

    ////////////////////////////////////////////////////////

  } // SurveyListPanel class

  ////////////////////////////////////////////////////////////

} // SurveyListChooser class

////////////////////////////////////////////////////////////////////////
