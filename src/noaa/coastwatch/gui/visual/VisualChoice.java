////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualChoice.java
  PURPOSE: Defines a visual interface for a choice of one value maong
           many values.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/24
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;

/**
 * The <code>VisualChoice</code> class represents a value choice as a
 * combo box.  When the combo box selection is modified, the value is
 * changed.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class VisualChoice 
  extends AbstractVisualObject {

  // Variables
  // ---------

  /** The combo box component. */
  private JComboBox combo;

  ////////////////////////////////////////////////////////////

  /** Creates a new visual choice object using the specified value. */
  public VisualChoice (
    Object value
  ) {                     

    // Create combo box
    // ----------------
    combo = new JComboBox (new Object[] {value});
    combo.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          firePropertyChange();
        } // actionPerformed
      });

  } // VisualChoice constructor

  ////////////////////////////////////////////////////////////

  /** Gets the field used to represent the string. */
  public Component getComponent () { return (combo); }

  ////////////////////////////////////////////////////////////

  /** Gets the choice value value. */
  public Object getValue () { return (combo.getSelectedItem()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets restrictions on the allowed choice values.
   * 
   * @param restrict the <code>List</code> of values that are allowed.
   */
  public void setRestrictions (
    Object restrict
  ) {

    Object selected = combo.getSelectedItem();
    DefaultComboBoxModel model = 
      new DefaultComboBoxModel (((List)restrict).toArray());
    model.setSelectedItem (selected);
    combo.setModel (model);

  } // setRestrictions

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    List choices = new ArrayList();
    choices.add ("hello");
    choices.add ("goodbye");
    choices.add ("world");
    VisualChoice visual = new VisualChoice (choices.get (1));
    visual.setRestrictions (choices);
    panel.add (visual.getComponent());
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualChoice class

////////////////////////////////////////////////////////////////////////
