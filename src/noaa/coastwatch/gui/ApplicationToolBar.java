/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.util.List;
import java.util.ArrayList;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JToolBar;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.Action;
import javax.swing.AbstractButton;
import javax.swing.SwingConstants;

import java.util.logging.Logger;

/**
 *
 * 
 * 
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class ApplicationToolBar extends JToolBar {

  private static final Logger LOGGER = Logger.getLogger (ApplicationToolBar.class.getName());    

  private List<ActionListener> actionListenerList;
  private List<AbstractButton> buttonList;

  ////////////////////////////////////////////////////////////

  public ApplicationToolBar (
    List<List<AbstractButton>> buttonGroupList
  ) {

    this.setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
    this.setBorder (BorderFactory.createEmptyBorder (2, 2, 2, 2));
    actionListenerList = new ArrayList<>();
    buttonList = new ArrayList<>();

    var lastGroup = buttonGroupList.get (buttonGroupList.size()-1);
    for (var buttonGroup : buttonGroupList) {
      addButtonGroup (buttonGroup);
      buttonList.addAll (buttonGroup);
      if (buttonGroup != lastGroup) add (Box.createHorizontalGlue());
    } // for

    GUIServices.setSameSize (buttonList);

  } // ApplicationToolBar

  ////////////////////////////////////////////////////////////

  private void addButtonGroup (List<AbstractButton> buttonGroup) {

    var lastButton = buttonGroup.get (buttonGroup.size()-1);
    for (var button : buttonGroup) {
      button.setHorizontalTextPosition (SwingConstants.CENTER);
      button.setVerticalTextPosition (SwingConstants.BOTTOM);
      button.setIconTextGap (0);
      button.setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
//      if (GUIServices.IS_AQUA) button.setBorderPainted (false);
      button.addActionListener (event -> buttonClickEvent (event));
      this.add (button);
      if (button != lastButton) this.add (Box.createHorizontalStrut (5));
    } // for
 
  } // addButtonGroup

  ////////////////////////////////////////////////////////////

  private void buttonClickEvent (ActionEvent event) {

    actionListenerList.forEach (listener -> listener.actionPerformed (event));

  } // buttonClickEvent

  ////////////////////////////////////////////////////////////

  public void addActionListener (ActionListener listener) {

    actionListenerList.add (listener);

  }  // addActionListener

  ////////////////////////////////////////////////////////////

  public void setShowText (boolean show) {

    for (var button : buttonList) {
      var action = button.getAction();
      if (action != null) {
        var text = show ? (String) action.getValue (Action.NAME) : "";
        button.setText (text);
      } // if
    } // for

    GUIServices.setSameSize (buttonList);

  } // setShowText

  ////////////////////////////////////////////////////////////

} // ApplicationToolBar class
