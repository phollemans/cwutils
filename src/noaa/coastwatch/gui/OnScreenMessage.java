/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;

import noaa.coastwatch.gui.GUIServices;

import java.util.logging.Logger;

/**
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class OnScreenMessage extends TranslucentPanel {

  private static final Logger LOGGER = Logger.getLogger (OnScreenMessage.class.getName());    

  ////////////////////////////////////////////////////////////

  public OnScreenMessage (String message, Runnable closeAction) {

    super (BoxLayout.X_AXIS);

    this.add (Box.createHorizontalStrut (5));

    var closeButton = GUIServices.createOnScreenStyleButton (16, IconFactory.Purpose.CLOSE_CIRCLE);
    closeButton.setForeground (Color.WHITE);
    closeButton.addActionListener (event -> {
      this.setVisible (false);
      if (closeAction != null) closeAction.run();
    });
    closeButton.setBorder (BorderFactory.createEmptyBorder (6, 8, 6, 8));
    this.add (closeButton);

    var label = new JLabel (message);
    label.setForeground (Color.WHITE);
    this.add (label);

    this.add (Box.createHorizontalStrut (20));

  } // OnScreenMessage

  ////////////////////////////////////////////////////////////

} // OnScreenMessage class

