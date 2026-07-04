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
 * The <code>ManagedLayeredPane</code> class is a layered pane that
 * manages component bounds using simple named positions.
 *
 * <p>Components may still be assigned to normal Swing layers using
 * the inherited layered pane constraints.  This class adds a separate
 * set of position constraints that place each managed component within
 * the pane content area.  A managed component may fill the content
 * area, align to one edge or corner using its preferred size, or fill
 * the bottom edge using its preferred height.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.1
 * @serial exclude
 */
public class ManagedLayeredPane extends JLayeredPane {

  private static final Logger LOGGER = Logger.getLogger (ManagedLayeredPane.class.getName());    

  /** The managed component positions. */
  public enum Position {

    /** Fill the full pane content area. */
    FULL,

    /** Center the component along the north edge. */
    NORTH,

    /** Center the component along the east edge. */
    EAST,

    /** Center the component along the south edge. */
    SOUTH,

    /** Fill the full width along the south edge using the preferred height. */
    SOUTH_FULL,

    /** Center the component along the west edge. */
    WEST,

    /** Align the component to the north-east corner. */
    NORTH_EAST,

    /** Align the component to the north-west corner. */
    NORTH_WEST,

    /** Align the component to the south-east corner. */
    SOUTH_EAST,

    /** Align the component to the south-west corner. */
    SOUTH_WEST
  };

  private Map<Component, Constraints> constraintsMap;

  ////////////////////////////////////////////////////////////

  /** Holds a set of constraints for a managed component. */
  public static class Constraints {

    /** The component position. */
    public Position pos;

    /** The component insets, or null for no insets. */
    public Insets insets;

    /**
     * Creates a new constraints object.
     *
     * @param pos the component position.
     * @param insets the component insets, or null for no insets.
     */
    public Constraints (Position pos, Insets insets) { this.pos = pos; this.insets = insets; }
  };

  ////////////////////////////////////////////////////////////

  /**
   * Updates the bounds of the managed components.
   *
   * <p>Each managed component is placed within the pane content area after
   * subtracting the pane insets.  Except for {@link Position#FULL} and
   * {@link Position#SOUTH_FULL}, component bounds use the component preferred
   * size.  Position insets offset components from the matching content edge;
   * null insets are treated as zero on all sides.</p>
   */
  public void updateBounds() {

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

  /** Creates a new managed layered pane. */
  public ManagedLayeredPane () {

    constraintsMap = new HashMap<>();
    this.addComponentListener (new ComponentAdapter() {
      public void componentResized (ComponentEvent e) { updateBounds(); }
      public void componentShown (ComponentEvent e) { updateBounds(); }
    });

  } // ManagedLayeredPane

  ////////////////////////////////////////////////////////////

  /**
   * Sets the position constraints for a component already added to this pane.
   *
   * <p>If the component is not a child of this pane, no constraints are
   * changed.</p>
   *
   * @param component the managed component.
   * @param constraints the position constraints.
   */
  public void setComponentConstraints (Component component, Constraints constraints) {

    if (component.getParent() == this)
      constraintsMap.put (component, constraints);

  } // setComponentContraints

  ////////////////////////////////////////////////////////////

  /**
   * Sets the position constraints for a component already added to this pane.
   *
   * @param component the managed component.
   * @param pos the component position.
   * @param insets the component insets, or null for no insets.
   */
  public void setComponentConstraints (Component component, Position pos, Insets insets) {

    setComponentConstraints (component, new Constraints (pos, insets));

  } // setComponentConstraints

  ////////////////////////////////////////////////////////////

  /**
   * Adds a component to a Swing layer and manages it at the specified position.
   *
   * @param comp the component to add.
   * @param constraints the Swing layered pane constraints.
   * @param pos the component position.
   */
  public void add (Component comp, Object constraints, Position pos) {

    add (comp, constraints, pos, null);

  } // add

  ////////////////////////////////////////////////////////////

  /**
   * Adds a component to a Swing layer and manages it at the specified position.
   *
   * @param comp the component to add.
   * @param constraints the Swing layered pane constraints.
   * @param pos the component position.
   * @param insets the component insets, or null for no insets.
   */
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
