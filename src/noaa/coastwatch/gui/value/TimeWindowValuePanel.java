////////////////////////////////////////////////////////////////////////
/*

     File: TimeWindowValuePanel.java
   Author: Peter Hollemans
     Date: 2017/03/17

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.value;

// Imports
// --------
import java.util.Date;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.FlowLayout;

import noaa.coastwatch.gui.value.ValuePanel;
import noaa.coastwatch.gui.value.LongValuePanel;
import noaa.coastwatch.gui.value.DateValuePanel;
import noaa.coastwatch.render.feature.TimeWindow;

// Testing
import javax.swing.JButton;

/**
 * A <code>TimeWindowValuePanel</code> holds a {@link TimeWindow} value and 
 * allows the user to change it.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class TimeWindowValuePanel
  extends ValuePanel<TimeWindow> {

  // Constants
  // ---------
  
  /** The units for time length. */
  private enum Units {
    SECONDS,
    MINUTES,
    HOURS,
    DAYS;
    @Override
    public String toString() {
      String value = super.toString();
      value = value.toLowerCase();
      return (value);
    } // toString    
  } // Units

  // Variables
  // ---------
  
  /** The time window value held by this object. */
  private TimeWindow timeWindowValue;

  /** The date panel for the central date. */
  private DateValuePanel datePanel;
  
  /** The window size panel. */
  private LongValuePanel sizePanel;
  
  /** The combo box with units for the size value (minutes, hours, days). */
  private JComboBox unitsCombo;

  /** The flag that denotes we are in the middle of a panel reconfiguration. */
  private boolean isReconfiguring;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new value panel.  The initial value is set to the epoch
   * start with a 0 minute window.
   */
  public TimeWindowValuePanel () {

    setLayout (new FlowLayout (FlowLayout.LEFT, 2, 0));
    
    // Add window size panel
    // ---------------------
    this.add (new JLabel ("within"));
    sizePanel = new LongValuePanel();
    sizePanel.setTextLength (5);
    sizePanel.addPropertyChangeListener (LongValuePanel.VALUE_PROPERTY,
      event -> { if (!isReconfiguring) windowChanged(); });
    this.add (sizePanel);
    unitsCombo = new JComboBox<Units> (Units.values());
    unitsCombo.addActionListener (event -> { if (!isReconfiguring) windowChanged(); });
    this.add (unitsCombo);
    
    // Add central date panel
    // ----------------------
    this.add (new JLabel ("of"));
    datePanel = new DateValuePanel();
    datePanel.addPropertyChangeListener (DateValuePanel.VALUE_PROPERTY,
      event -> { if (!isReconfiguring) windowChanged(); });
    this.add (datePanel);
    
    setValue (new TimeWindow (new Date (0), 0));

  } // TimeWindowValuePanel constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void setValue (TimeWindow value) {
  
    timeWindowValue = value;
    isReconfiguring = true;

    // Set central date
    // ----------------
    datePanel.setValue (value.getCentralDate());
    
    // Set window size with units
    // --------------------------
    long windowSize = value.getWindowSize();
    long seconds = windowSize/1000;
    if (seconds < 60 || seconds%60 != 0) {
      unitsCombo.setSelectedItem (Units.SECONDS);
      sizePanel.setValue (seconds);
    } // if
    else {
      long minutes = seconds/60;
      if (minutes < 60 || minutes%60 != 0) {
        unitsCombo.setSelectedItem (Units.MINUTES);
        sizePanel.setValue (minutes);
      } // if
      else {
        long hours = minutes/60;
        if (hours < 24 || hours%24 != 0) {
          unitsCombo.setSelectedItem (Units.HOURS);
          sizePanel.setValue (hours);
        } // if
        else {
          long days = hours/24;
          unitsCombo.setSelectedItem (Units.DAYS);
          sizePanel.setValue (days);
        } // else
      } // else
    } // else

    isReconfiguring = false;
    
  } // setValue
  
  ////////////////////////////////////////////////////////////

  @Override
  public TimeWindow getValue() { return (timeWindowValue); }
  
  ////////////////////////////////////////////////////////////
  
  /** Updates the time window and sends a signal of the change if needed. */
  public void windowChanged () {

    // Get the displayed time window value
    // -----------------------------------
    long windowSize = sizePanel.getValue();
    Units units = (Units) unitsCombo.getSelectedItem();
    switch (units) {
    case SECONDS: windowSize *= 1000; break;
    case MINUTES: windowSize *= 1000*60; break;
    case HOURS: windowSize *= 1000*60*60; break;
    case DAYS: windowSize *= 1000*60*60*24; break;
    } // switch
    TimeWindow newTimeWindowValue = new TimeWindow (datePanel.getValue(), windowSize);

    // Send signal if different
    // ------------------------
    if (!newTimeWindowValue.equals (timeWindowValue)) {
      timeWindowValue = newTimeWindowValue;
      signalValueChanged();
    } // if
  
  } // windowChanged

  ////////////////////////////////////////////////////////////
  
  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    TimeWindowValuePanel panel = new TimeWindowValuePanel();
    panel.addPropertyChangeListener (VALUE_PROPERTY, event -> {
      System.out.println ("[" + new Date().getTime() + "] event.getNewValue() = " + event.getNewValue());
    });
    panel.add (new JButton ("OK"));

    JButton reset = new JButton ("Reset");
    reset.addActionListener (event -> panel.setValue (new TimeWindow (new Date (0), 0)));
    panel.add (reset);

    JButton button;
    button = new JButton ("61 Sec");
    button.addActionListener (event -> panel.setValue (new TimeWindow (new Date (0), 61*1000)));
    panel.add (button);

    button = new JButton ("61 Min");
    button.addActionListener (event -> panel.setValue (new TimeWindow (new Date (0), 61*60*1000)));
    panel.add (button);

    button = new JButton ("25 Hours");
    button.addActionListener (event -> panel.setValue (new TimeWindow (new Date (0), 25*60*60*1000)));
    panel.add (button);

    button = new JButton ("48 Hours");
    button.addActionListener (event -> panel.setValue (new TimeWindow (new Date (0), 48*60*60*1000)));
    panel.add (button);

    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // TimeWindowValuePanel class

////////////////////////////////////////////////////////////////////////

