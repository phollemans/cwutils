////////////////////////////////////////////////////////////////////////
/*

     File: DateValuePanel.java
   Author: Peter Hollemans
     Date: 2017/03/15

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerModel;
import javax.swing.JSpinner;
import java.awt.FlowLayout;

import noaa.coastwatch.gui.value.ValuePanel;

// Testing
import javax.swing.JButton;

/**
 * A <code>DateValuePanel</code> holds a Date value and allows the user
 * to change it.
 *
 * @author Peter Hollemans
 * @since 3.3.2
 */
public class DateValuePanel
  extends ValuePanel<Date> {

  // Constants
  // ---------

  /** The date format for display and parsing. */
  private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss 'UTC '";

  // Variables
  // ---------
  
  /** The date value held by this object. */
  private Date dateValue;

  /** The date spinner to display the value. */
  private JSpinner dateSpinner;

  /** The flag that denotes we are in the middle of a panel reconfiguration. */
  private boolean isReconfiguring;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new value panel.  The initial value is set to the epoch
   * start. 
   */
  public DateValuePanel () {

    setLayout (new FlowLayout (FlowLayout.LEFT, 2, 0));

    // Create date spinner
    // -------------------
    SpinnerModel dateModel = new SpinnerDateModel();
    dateSpinner = new JSpinner (dateModel);
    JSpinner.DateEditor editor = new JSpinner.DateEditor (dateSpinner, DATE_FORMAT);
    SimpleDateFormat dateFormat = editor.getFormat();
    dateFormat.setTimeZone (TimeZone.getTimeZone ("GMT+0"));
    dateSpinner.setEditor (editor);
    dateSpinner.addChangeListener (listener -> {if (!isReconfiguring) dateChanged();});
    setValue (new Date (0));
    this.add (dateSpinner);

  } // DateValuePanel constructor

  ////////////////////////////////////////////////////////////

  @Override
  public void setValue (Date value) {
  
    dateValue = value;
    isReconfiguring = true;
    dateSpinner.setValue (value);
    isReconfiguring = false;
    
  } // setValue
  
  ////////////////////////////////////////////////////////////

  @Override
  public Date getValue() { return (dateValue); }
  
  ////////////////////////////////////////////////////////////
  
  /** Updates the date and sends a signal of the change if needed. */
  public void dateChanged () {

    // Check if we have a change in value
    // ----------------------------------
    Date newDateValue = (Date) dateSpinner.getValue();
    if (!newDateValue.equals (dateValue)) {
      dateValue = newDateValue;
      signalValueChanged();
    } // if
  
  } // dateChanged

  ////////////////////////////////////////////////////////////
  
  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    DateValuePanel panel = new DateValuePanel();
    panel.addPropertyChangeListener (VALUE_PROPERTY, event -> {
      System.out.println ("[" + new Date().getTime() + "] event.getNewValue() = " + event.getNewValue());
    });
    panel.add (new JButton ("OK"));
    JButton reset = new JButton ("Reset");
    reset.addActionListener (event -> panel.setValue (new Date (0)));
    panel.add (reset);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // DateValuePanel class

////////////////////////////////////////////////////////////////////////

