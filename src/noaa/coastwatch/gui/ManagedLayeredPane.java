/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.util.Map;
import java.util.HashMap;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
import javax.swing.JLayeredPane;

import java.util.logging.Logger;

/**
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class ManagedLayeredPane extends JLayeredPane {

  private static final Logger LOGGER = Logger.getLogger (ManagedLayeredPane.class.getName());    

  public enum Position {
    FULL,
    NORTH,
    EAST,
    SOUTH,
    SOUTH_FULL,
    WEST,
    NORTH_EAST,
    NORTH_WEST,
    SOUTH_EAST,
    SOUTH_WEST
  };

  private Map<Component, Constraints> constraintsMap;

  ////////////////////////////////////////////////////////////

  /** Holds a set of contraints for a managed component. */
  public static class Constraints {
    public Position pos;
    public Insets insets;
    public Constraints (Position pos, Insets insets) { this.pos = pos; this.insets = insets; }
  };

  ////////////////////////////////////////////////////////////

  private void updateBounds() {

    var paneDims = this.getSize();
    var paneinsets = this.getInsets();

    var contentRect = new Rectangle (
      paneinsets.left,
      paneinsets.top, 
      paneDims.width - (paneinsets.left + paneinsets.right),
      paneDims.height - (paneinsets.top + paneinsets.bottom)
    );

    for (var component : this.getComponents()) {
      var constraints = constraintsMap.get (component);
      if (constraints != null && constraints.pos != null) {
        var insets = constraints.insets;
        if (insets == null) insets = new Insets (0, 0, 0, 0);

        var size = component.getPreferredSize();
        Rectangle bounds;
        switch (constraints.pos) {

        case FULL:
          bounds = contentRect;
          break;

        case NORTH:
          bounds = new Rectangle (
            contentRect.x + contentRect.width/2 - size.width/2,
            contentRect.y + insets.top, 
            size.width, 
            size.height
          );
          break;

        case NORTH_EAST:
          bounds = new Rectangle (
            contentRect.x + contentRect.width - size.width - insets.right,
            contentRect.y + insets.top, 
            size.width, 
            size.height
          );
          break;

        case NORTH_WEST:
          bounds = new Rectangle (
            contentRect.x + insets.left,
            contentRect.y + insets.top, 
            size.width, 
            size.height
          );
          break;

        case SOUTH:
          bounds = new Rectangle (
            contentRect.x + contentRect.width/2 - size.width/2,
            contentRect.y + contentRect.height - size.height - insets.bottom, 
            size.width, 
            size.height
          );
          break;

        case SOUTH_FULL:
          bounds = new Rectangle (
            contentRect.x,
            contentRect.y + contentRect.height - size.height - insets.bottom, 
            contentRect.width, 
            size.height
          );
          break;

        case SOUTH_EAST:
          bounds = new Rectangle (
            contentRect.x + contentRect.width - size.width - insets.right,
            contentRect.y + contentRect.height - size.height - insets.bottom, 
            size.width, 
            size.height
          );
          break;

        case SOUTH_WEST:
          bounds = new Rectangle (
            contentRect.x + insets.left,
            contentRect.y + contentRect.height - size.height - insets.bottom, 
            size.width, 
            size.height
          );
          break;

        case WEST:
          bounds = new Rectangle (
            contentRect.x + insets.left,
            contentRect.y + contentRect.height/2 - size.height/2,
            size.width, 
            size.height
          );
          break;

        case EAST:
          bounds = new Rectangle (
            contentRect.x + contentRect.width - size.width - insets.right,
            contentRect.y + contentRect.height/2 - size.height/2,
            size.width, 
            size.height
          );
          break;

        default: throw new IllegalStateException ("Unhandled case for position " + constraints.pos);

        } // switch 
        component.setBounds (bounds);

      } // if
    } // for

    this.validate();

  } // updateBounds

  ////////////////////////////////////////////////////////////

  public ManagedLayeredPane () {

    constraintsMap = new HashMap<>();
    this.addComponentListener (new ComponentAdapter() {
      public void componentResized (ComponentEvent e) { updateBounds(); }
      public void componentShown (ComponentEvent e) { updateBounds(); }
    });

  } // ManagedLayeredPane

  ////////////////////////////////////////////////////////////

  public void setComponentConstraints (Component component, Constraints constraints) {

    if (component.getParent() == this)
      constraintsMap.put (component, constraints);

  } // setComponentContraints

  ////////////////////////////////////////////////////////////

  public void setComponentConstraints (Component component, Position pos, Insets insets) {

    setComponentConstraints (component, new Constraints (pos, insets));

  } // setComponentConstraints

  ////////////////////////////////////////////////////////////

  public void add (Component comp, Object constraints, Position pos) {

    add (comp, constraints, pos, null);

  } // add

  ////////////////////////////////////////////////////////////

  public void add (Component comp, Object constraints, Position pos, Insets insets) {

    add (comp, constraints);
    setComponentConstraints (comp, new Constraints (pos, insets));
    updateBounds();

  } // add

  ////////////////////////////////////////////////////////////

  @Override
  public void remove (Component component) {

    super.remove (component);
    constraintsMap.remove (component);

  } // remove

  ////////////////////////////////////////////////////////////

} // ManagedLayeredPane class

