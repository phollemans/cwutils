////////////////////////////////////////////////////////////////////////
/*
     FILE: ExpressionMaskOverlayPropertyChooser.java
  PURPOSE: Allows the user to edit overlay properties for expression masks.
   AUTHOR: Peter Hollemans
     DATE: 2006/11/02
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import noaa.coastwatch.render.*;

/** 
 * The <code>ExpressionMaskOverlayPropertyChooser</code> class is an
 * implementation of an <code>OverlayPropertyChooser</code> that
 * allows the user to edit the properties of an
 * <code>ExpressionMaskOverlay</code>.
 *
 * @author Peter Hollemans
 * @since 3.2.1
 */
public class ExpressionMaskOverlayPropertyChooser 
  extends GenericOverlayPropertyChooser {

  // Variables
  // ---------

  /** The text field foe expression input. */
  private JTextField field;

  ////////////////////////////////////////////////////////////

  /** Creates a new chooser panel. */
  public ExpressionMaskOverlayPropertyChooser (
    ExpressionMaskOverlay newOverlay
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
      ((ExpressionMaskOverlay) overlay).setExpression (field.getText());
    } // try
    catch (Exception e) {
      throw new IllegalStateException (e.getMessage());
    } // catch

  } // validate

  ////////////////////////////////////////////////////////////

} // ExpressionMaskOverlayPropertyChooser class

////////////////////////////////////////////////////////////////////////
