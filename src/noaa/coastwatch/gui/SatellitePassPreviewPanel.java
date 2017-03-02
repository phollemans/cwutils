////////////////////////////////////////////////////////////////////////
/*

     File: SatellitePassPreviewPanel.java
   Author: Peter Hollemans
     Date: 2003/01/20

  CoastWatch Software Library and Utilities
  Copyright (c) 2003 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.ImageViewPanel;
import noaa.coastwatch.util.SatellitePassInfo;

/**
 * The satellite pass preview panel displays a satellite pass data
 * preview image.  The image may be manipulated by using the tools to
 * zoom and pan.
 *
 * @see SatellitePassInfo 
 *
 * @author Peter Hollemans
 * @since 3.1.3
 */
public class SatellitePassPreviewPanel
  extends JPanel {

  // Constants
  // ---------
  /** The view commands. */
  private static final String ZOOMIN_COMMAND = "Magnify";
  private static final String ZOOMOUT_COMMAND = "Shrink";
  private static final String ZOOM_COMMAND = "Zoom";
  private static final String PAN_COMMAND = "Pan";
  private static final String RESET_COMMAND = "Reset";

  /** The foregound color. */
  private static final Color FOREGROUND_COLOR = new Color (160, 160, 160);

  /** The background color. */
  private static final Color BACKGROUND_COLOR = Color.BLACK;

  // Variables
  // ---------
  /** The image view panel. */
  private ImageViewPanel viewPanel;

  /** The button group for tool buttons. */
  private ButtonGroup toolButtonGroup;

  /** The current pass. */
  private SatellitePassInfo pass;

  ////////////////////////////////////////////////////////////

  /** Gets the currently displayed pass. */
  public SatellitePassInfo getPass () { return (pass); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the active satellite pass used to display a pass preview. 
   * 
   * @param pass the satellite pass information, or null for no pass
   * preview information.
   */
  public void setPass (
    SatellitePassInfo pass
  ) { 

    // Set current pass
    // ----------------
    this.pass = pass;

    // Get preview image
    // -----------------
    Image image = null;
    if (pass != null) {
      try { 
        image = Toolkit.getDefaultToolkit().createImage (
          new URL (pass.getPreviewURL()));
      } catch (MalformedURLException e) { }
    } // if

    // Set preview image
    // -----------------
    viewPanel.setImage (image);

  } // setPass

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new empty satellite pass preview panel.
   */
  public SatellitePassPreviewPanel () {

    // Initialize
    // ----------
    super (new BorderLayout());
    this.pass = null;

    // Create view panel
    // -----------------
    viewPanel = new ImageViewPanel();
    add (viewPanel, BorderLayout.CENTER);
    viewPanel.setBackground (BACKGROUND_COLOR);
    viewPanel.setForeground (FOREGROUND_COLOR);

    // Create tool bar
    // ---------------
    JToolBar toolBar = new JToolBar (JToolBar.VERTICAL);
    toolBar.setFloatable (false);
    add (toolBar, BorderLayout.EAST);

    JButton button;
    JToggleButton toggleButton;
    ActionListener toolListener = new ChangeView();
    toolButtonGroup = new ButtonGroup();

    button = GUIServices.getIconButton ("view.magnify");
    button.setToolTipText (ZOOMIN_COMMAND);
    button.getModel().setActionCommand (ZOOMIN_COMMAND);
    button.addActionListener (toolListener);
    toolBar.add (button);

    button = GUIServices.getIconButton ("view.shrink");
    button.setToolTipText (ZOOMOUT_COMMAND);
    button.getModel().setActionCommand (ZOOMOUT_COMMAND);
    button.addActionListener (toolListener);
    toolBar.add (button);

    toggleButton = GUIServices.getIconToggle ("view.zoom");
    toggleButton.setToolTipText (ZOOM_COMMAND);
    toggleButton.getModel().setActionCommand (ZOOM_COMMAND);
    toggleButton.addActionListener (toolListener);
    toolButtonGroup.add (toggleButton);
    toolBar.add (toggleButton);

    toggleButton = GUIServices.getIconToggle ("view.pan");
    toggleButton.setToolTipText (PAN_COMMAND);
    toggleButton.getModel().setActionCommand (PAN_COMMAND);
    toggleButton.addActionListener (toolListener);
    toolButtonGroup.add (toggleButton);
    toolBar.add (toggleButton);

    button = GUIServices.getIconButton ("view.reset");
    button.setToolTipText (RESET_COMMAND);
    button.getModel().setActionCommand (RESET_COMMAND);
    button.addActionListener (toolListener);
    toolBar.add (button);

  } // SatellitePassPreviewPanel constructor

  ////////////////////////////////////////////////////////////

  /** Responds to a tool view command. */
  private class ChangeView 
    extends AbstractAction {

    public void actionPerformed (ActionEvent event) {
      String command = event.getActionCommand();

      // Zoom out
      // --------
      if (command.equals (ZOOMOUT_COMMAND)) { 
        viewPanel.magnify (0.5); 
      } // if

      // Zoom in
      // -------
      else if (command.equals (ZOOMIN_COMMAND)) { 
        viewPanel.magnify (2); 
      } // if

      // Turn on box zoom mode
      // ---------------------
      else if (command.equals (ZOOM_COMMAND)) { 
        if (((JToggleButton) event.getSource()).isSelected ()) {
          viewPanel.setViewMode (ImageViewPanel.ZOOM_MODE);
        } // if
      } // else if

      // Turn on pan mode
      // ----------------
      else if (command.equals (PAN_COMMAND)) { 
        if (((JToggleButton) event.getSource()).isSelected ()) {
          viewPanel.setViewMode (ImageViewPanel.PAN_MODE);
        } // if
      } // else if

      // Reset view
      // ----------
      else if (command.equals (RESET_COMMAND)) { 
        viewPanel.reset(); 
      } // if

    } // actionPerformed

  } // ChangeView class

  ////////////////////////////////////////////////////////////

} // SatellitePassPreviewPanel class

////////////////////////////////////////////////////////////////////////
