/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.util.Map;
import java.util.HashMap;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

import noaa.coastwatch.gui.GUIServices;

import java.util.logging.Logger;

/**
 * <p>The <code>OnScreenViewOperationChooser</code> class provides an
 * on screen display style set of buttons that the user can use to 
 * select view transform operations.</p>
 *
 * <p>The chooser signals a change in the selected operation by
 * firing a <code>PropertyChangeEvent</code> whose property name is
 * <code>OnScreenViewOperationChooser.OPERATION_PROPERTY</code>, and new value
 * contains an operation name from the constants in this class.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class OnScreenViewOperationChooser extends TranslucentPanel {

  private static final Logger LOGGER = Logger.getLogger (OnScreenViewOperationChooser.class.getName());    

  public static final String OPERATION_PROPERTY = "operation";

  public enum Mode {
  	MAGNIFY,
  	SHRINK,
  	ONE_TO_ONE,
  	ZOOM,
  	PAN,
  	RESET,
  	FIT,
    CLOSE
  };

  private Mode lastOperation;
  private Map<Mode, ModeInfo> infoMap;

  ////////////////////////////////////////////////////////////

  private static class ModeInfo {    
    String tip;
    String icon;
    ModeInfo (String icon, String tip) { this.tip = tip; this.icon = icon; }
  } // ModeInfo

  ////////////////////////////////////////////////////////////

  private void setHelperInComponents (OpacityHelper helper) {

    for (var component : getComponents()) {
      if (component instanceof TranslucentComponent) {
        ((TranslucentComponent) component).setHelper (helper);
      } // if
    } // for

  } // setHelperInComponents

  ////////////////////////////////////////////////////////////

  @Override
  public void setHelper (OpacityHelper helper) { 

    super.setHelper (helper);
    setHelperInComponents (helper);

  } // setHelper

  ////////////////////////////////////////////////////////////

  public OnScreenViewOperationChooser () { this (BoxLayout.Y_AXIS, false); }

  ////////////////////////////////////////////////////////////

  private JLabel createSeparatorLabel (int axis, boolean large) {

    var separatorType = axis == BoxLayout.Y_AXIS ? 
      IconFactory.Purpose.SEPARATOR_HORIZONTAL : 
      IconFactory.Purpose.SEPARATOR_VERTICAL;
    var icon = IconFactory.getInstance().createIcon (separatorType, IconFactory.Mode.NORMAL, large ? 32 : 16);
    var label = new TranslucentLabel (icon);
    label.setHorizontalAlignment (SwingConstants.CENTER);
    var back = this.getBackground();
    var line = new Color (back.getRed()*4, back.getGreen()*4, back.getBlue()*4, 192);
    label.setForeground (line);
    label.setAlignmentX (0.5f);
    label.setAlignmentY (0.5f);

    return (label);

  } // createSeparatorLabel

  ////////////////////////////////////////////////////////////

  private JButton createButton (Mode mode, boolean large, ActionListener listener) {

    var ext = large ? ".large" : "";

    var defaultIcon = GUIServices.getIcon (infoMap.get (mode).icon + ext);
    Icon rolloverIcon, pressedIcon;
    try {
      rolloverIcon = GUIServices.getIcon (infoMap.get (mode).icon + ext + ".rollover");
      pressedIcon = GUIServices.getIcon (infoMap.get (mode).icon + ext + ".pressed");
    } // try 
    catch (Exception e) {
      rolloverIcon = null;
      pressedIcon = null;
    } // catch
    var button = GUIServices.createTranslucentButton (defaultIcon, rolloverIcon, pressedIcon);
    button.getModel().setActionCommand (mode.name());
    button.addActionListener (listener);
    button.setToolTipText (infoMap.get (mode).tip);

    if (mode == Mode.RESET) {
      if (large) button.setBorder (BorderFactory.createEmptyBorder (8, 8, 16, 8));  
      else button.setBorder (BorderFactory.createEmptyBorder (4, 8, 8, 8));  
    } // if
    else {
      if (large) button.setBorder (BorderFactory.createEmptyBorder (12, 8, 12, 8));
      else button.setBorder (BorderFactory.createEmptyBorder (6, 8, 6, 8));
    } // else

    button.setAlignmentX (0.5f);
    button.setAlignmentY (0.5f);

    return (button);

  } // createButton

  ////////////////////////////////////////////////////////////

  public void reconfigure (
    int axis,
    boolean large,
    Runnable closeAction,
    Mode[] modes 
  ) {

    setLayout (new BoxLayout (this, axis));
    removeAll();

    setDiameter (large ? 32 : 24);

    int strutSize = large ? 12 : 12;
    this.add (axis == BoxLayout.Y_AXIS ? Box.createVerticalStrut (strutSize) : Box.createHorizontalStrut (strutSize));

    for (int i = 0; i < modes.length; i++) {
      if (modes[i] == null) 
        this.add (createSeparatorLabel (axis, large));
      else
        this.add (createButton (modes[i], large, event -> buttonClickEvent (event)));
    } // for

    if (closeAction != null) {
      this.add (createSeparatorLabel (axis, large));
      this.add (createButton (Mode.CLOSE, large, event -> closeAction.run()));
    } // if

    this.add (axis == BoxLayout.Y_AXIS ? Box.createVerticalStrut (strutSize) : Box.createHorizontalStrut (strutSize));

    var helper = getHelper();
    if (helper != null) setHelperInComponents (helper);

  } // reconfigure

  ////////////////////////////////////////////////////////////

  public Mode[] defaultModes() { 

    return (new Mode[] {
      Mode.RESET,    
      null,
      Mode.MAGNIFY,
      Mode.SHRINK,
      null,
      Mode.ZOOM,
      Mode.ONE_TO_ONE,
      Mode.FIT
    });

  } // defaultModes

  ////////////////////////////////////////////////////////////

  public Mode[] subsetModes() { 

    return (new Mode[] {
      Mode.RESET,
      null,
      Mode.MAGNIFY,
      Mode.SHRINK      
    });

  } // subsetModes

  ////////////////////////////////////////////////////////////

  public OnScreenViewOperationChooser (int axis, boolean large) {

    infoMap = new HashMap<>();
    infoMap.put (Mode.RESET, new ModeInfo ("view.reset.onscreen", "Fit image to window"));
    infoMap.put (Mode.MAGNIFY, new ModeInfo ("view.magnify.onscreen", "Zoom in"));
    infoMap.put (Mode.SHRINK, new ModeInfo ("view.shrink.onscreen", "Zoom out"));
    infoMap.put (Mode.ONE_TO_ONE, new ModeInfo ("view.one_to_one.onscreen", "Zoom to actual size"));
    infoMap.put (Mode.ZOOM, new ModeInfo ("view.zoom.onscreen", "Zoom to selection"));
    infoMap.put (Mode.FIT, new ModeInfo ("view.fit.onscreen", "Fill window"));
    infoMap.put (Mode.CLOSE, new ModeInfo ("view.close.onscreen", "Exit fullscreen mode"));

    reconfigure (axis, large, null, defaultModes());

  } // OnScreenViewOperationChooser

  ////////////////////////////////////////////////////////////

  public Mode getViewOperation () { return (lastOperation); }

  ////////////////////////////////////////////////////////////

  public void performViewOperation (Mode operation) {

		lastOperation = operation;
		firePropertyChange (OPERATION_PROPERTY, null, lastOperation);

  } // performViewOperation

  ////////////////////////////////////////////////////////////

  private void buttonClickEvent (ActionEvent event) {

		var operation = Mode.valueOf (event.getActionCommand());
		performViewOperation (operation);

	} // buttonClickEvent

  ////////////////////////////////////////////////////////////

} // ViewOperationChooser class

