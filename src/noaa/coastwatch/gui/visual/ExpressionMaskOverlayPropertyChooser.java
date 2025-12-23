////////////////////////////////////////////////////////////////////////
/*

     File: ExpressionMaskOverlayPropertyChooser.java
   Author: Peter Hollemans
     Date: 2006/11/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2006 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import javax.swing.JTextField;
import noaa.coastwatch.gui.visual.GenericOverlayPropertyChooser;
import noaa.coastwatch.gui.visual.OverlayPropertyChooser;
import noaa.coastwatch.render.JavaExpressionMaskOverlay;

/** 
 * The <code>ExpressionMaskOverlayPropertyChooser</code> class is an
 * implementation of an <code>OverlayPropertyChooser</code> that
 * allows the user to edit the properties of an
 * <code>JavaExpressionMaskOverlay</code>.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class ExpressionMaskOverlayPropertyChooser 
  extends GenericOverlayPropertyChooser {

  // Variables
  // ---------

  /** The text field for expression input. */
  private JTextField field;

  ////////////////////////////////////////////////////////////

  @Override
  protected String getTitle () { return ("Expression Mask"); }

  ////////////////////////////////////////////////////////////

  /** Creates a new chooser panel. */
  public ExpressionMaskOverlayPropertyChooser (
    JavaExpressionMaskOverlay newOverlay
  ) {

    super (newOverlay);

    // Modify the expression field
    // ---------------------------
    field = (JTextField) getComponent ("expression");
    field.setColumns (20);

    // Remove any listeners from VisualString
    // --------------------------------------
    ActionListener[] aListen = field.getActionListeners();
    for (int i = 0; i < aListen.length; i++) {
      if (aListen[i].getClass().getName().indexOf ("VisualString") != -1)
        field.removeActionListener (aListen[i]);
    } // for
    FocusListener[] fListen = field.getFocusListeners();
    for (int i = 0; i < fListen.length; i++) {
      if (fListen[i].getClass().getName().indexOf ("VisualString") != -1)
        field.removeFocusListener (fListen[i]);
    } // for

    // Add in our own action listener
    // ------------------------------
    field.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          firePropertyChange (OVERLAY_PROPERTY, null, overlay);
        } // actionPerformed
      });

  } // ExpressionMaskOverlayPropertyChooser constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Validates the expression text field for this chooser. 
   */
  protected void validateInput () {

    try {
      ((JavaExpressionMaskOverlay) overlay).setExpression (field.getText());
    } // try
    catch (Exception e) {
      throw new IllegalStateException (e.getMessage());
    } // catch

  } // validate

  ////////////////////////////////////////////////////////////

} // ExpressionMaskOverlayPropertyChooser class

////////////////////////////////////////////////////////////////////////
