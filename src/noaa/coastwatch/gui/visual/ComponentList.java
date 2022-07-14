////////////////////////////////////////////////////////////////////////
/*

     File: ComponentList.java
   Author: Peter Hollemans
     Date: 2004/03/02

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;

import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.visual.ComponentProducer;
import noaa.coastwatch.gui.visual.VisualOverlay;
import noaa.coastwatch.render.CoastOverlay;
import noaa.coastwatch.render.LatLonOverlay;

/** 
 * <p>The <code>ComponentList</code> class is similar to a Swing
 * <code>JList</code> but with a simplified model and operations.  It
 * lays out a list of <code>ComponentProducer</code> objects from top
 * to bottom and allows them to be selected, removed, added, and so
 * on.  A change in the list selection is signaled using a property
 * change event given by <code>SELECTION_PROPERTY</code>.</p>
 *
 * <p>The list may be placed inside a scrollable window.  The scrolling
 * behaviour works under the assumption that all components in the
 * list have the same height.  The user may set the number of visible
 * components using the <code>setVisibleRowCount()</code> method.</p>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ComponentList<E extends ComponentProducer>
  extends JPanel 
  implements Scrollable, Iterable<E> {

  // Constants
  // ---------

  /** 
   * The selection property event used to signal changes in the
   * selected elements.
   */
  public static final String SELECTION_PROPERTY = "selection";

  // Variables
  // ---------

  /** The list of component producers. */
  private List<E> producerList;

  /** The map of components to producers. */
  private Map<Component,E> componentMap;

  /** The hash set of selections. */
  private SortedSet<Integer> selectionSet;

  /** The last component index that was clicked. */
  private Integer lastIndex;

  /** The selected component background. */
  private Color selectionBackground;

  /** The default component background. */
  private Color defaultBackground;

  /** The visible row count. */
  private int visibleRowCount = 8;

  /** The selectable flag, true if rows can be selected or false if not. */
  private boolean isSelectable = true;

  ////////////////////////////////////////////////////////////

  /** Creates a new empty component list. */
  public ComponentList() {

    super (new GridBagLayout());

    // Initialize
    // ----------
    producerList = new LinkedList<E>();
    selectionSet = new TreeSet<Integer>();
    componentMap = new HashMap<Component, E>();

    // Add selection listener
    // ----------------------
    addMouseListener (new SelectionListener());

    // Set colors
    // ----------
    selectionBackground = (Color) UIManager.get ("List.selectionBackground");
    defaultBackground = (Color) UIManager.get ("Label.background");

  } // ComponentList constructor

  ////////////////////////////////////////////////////////////

  /**
   * Sets the selectable flag.  When on, the list rows are selectable 
   * and change their background colour when selected.  When off, rows
   * are not selectable and keep their default background color even when
   * clicked.
   *
   * @param selectableFlag the selectable flag, true if rows should be 
   * selectable or false if not.
   */
  public void setSelectable (
    boolean selectableFlag
  ) {
  
    this.isSelectable = selectableFlag;
   
  } // setSelectable

  ////////////////////////////////////////////////////////////

  /** Clears the list of all components. */
  public void clear () {

    producerList.clear();
    componentMap.clear();
    reset();

  } // clear

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a new component to the end of the list.
   * 
   * @param producer the component producer to add to the list.
   */
  public void addElement (
    E producer
  ) { 

    addElement (producerList.size(), producer);

  } // addElement

  ////////////////////////////////////////////////////////////

  /** 
   * Adds a new component at the specified index in the list.
   *
   * @param index the index to add the component at.  The component is 
   * inserted at the index and all component indices after the inserted
   * index are incremented.
   * @param producer the component producer to add to the list.
   */
  public void addElement (
    int index,
    E producer
  ) {

    producerList.add (index, producer);
    componentMap.put (producer.getComponent(), producer);
    reset();

  } // addElement

  ////////////////////////////////////////////////////////////

  /** 
   * Removes the specified component from the list.
   * 
   * @param producer the component producer to remove.
   * 
   * @return true if the component was removed, false otherwise.
   */
  public boolean removeElement (
    E producer
  ) { 

    boolean removed = producerList.remove (producer);
    if (removed) {
      componentMap.remove (producer.getComponent());
      reset();
    } // if
    return (removed);

  } // removeElement

  ////////////////////////////////////////////////////////////

  /** 
   * Removes the component at the specified index.
   *
   * @param index the index of the component to remove.
   *
   * @return the removed component producer.
   */
  public E removeElement (
    int index
  ) {

    E producer = producerList.remove (index);
    if (producer != null) {
      componentMap.remove (producer.getComponent());
      reset();
    } // if
    return (producer);

  } // removeElement

  ////////////////////////////////////////////////////////////

  /** 
   * Moves the component between the specified indices.
   *
   * @param source the source index for the component to move.
   * @param dest the destination index to insert a new component.  The
   * destination index is relative to the indices of elements after the 
   * removal operation has happened.
   */
  public void moveElement (
    int source,
    int dest
  ) {

    producerList.add (dest, producerList.remove (source));
    reset();

  } // moveElement

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the number of components in the list.
   *
   * @return the number of components.
   */
  public int getElements () { return (producerList.size()); }

  ////////////////////////////////////////////////////////////
  
  /** 
   * Gets the currently selected indices.
   *
   * @return the array of selected indices, possible zero length if no
   * rows are selected.
   */
  public int[] getSelectedIndices () {

    int[] indices = new int[selectionSet.size()];
    Iterator iter = selectionSet.iterator();
    for (int i = 0; i < indices.length; i++)
      indices[i] = ((Integer) iter.next()).intValue();

    return (indices);

  } // getSelectedIndices

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the selection indices between the specified start and end
   * inclusive.
   *
   * @param start the starting index for the selection.
   * @param end the ending index for selection.
   * 
   * @throws IndexOutOfBoundsException if either index is out of bounds.
   */
  public void setSelectionInterval (
    int start, 
    int end
  ) {

    if (isSelectable) {
      checkBounds (start);
      checkBounds (end);
      selectionSet.clear();
      for (int i = start; i <= end; i++)
        selectionSet.add (Integer.valueOf (i));
      lastIndex = Integer.valueOf (start);
      updateBackgrounds();
    } // if
    
  } // setSelectionInterval

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the component at the specified index.
   *
   * @param index to index of the component producer to retrieve.
   *
   * @return the component producer at the specified index.
   *
   * @throws IndexOutOfBoundsException if the index is out of range.
   */
  public E getElement (
    int index
  ) {

    return (producerList.get (index));

  } // getElement

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an iterator over the component producers in the list.
   *
   * @return the iterator over component producers.
   */
  public Iterator<E> iterator () { return (producerList.iterator()); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the index of a component producer in the list.
   *
   * @param producer the component producer to search for.
   *
   * @return the index of the first occurrence of the producer,
   * or -1 if this list does not contain the producer.
   */
  public int indexOf (
    E producer
  ) {
  
    return (producerList.indexOf (producer));
   
  } // indexOf

  ////////////////////////////////////////////////////////////

  /** 
   * Checks if an index is within bounds.
   *
   * @param index the index to check.
   *
   * @throws IndexOutOfBoundsException if the index is out of bounds.
   */
  private void checkBounds (
    int index
  ) {

    if (index < 0)
      throw (new IndexOutOfBoundsException ("index < 0"));
    else if (index > producerList.size()-1)
      throw (new IndexOutOfBoundsException ("index > size()-1"));

  } // checkBounds

  ////////////////////////////////////////////////////////////

  /** 
   * Resets the list by removing all selections, updating the
   * layout, and setting the background colors. 
   */
  private void reset () {

    selectionSet.clear();
    lastIndex = null;
    updateLayout();
    updateBackgrounds();

  } // reset

  ////////////////////////////////////////////////////////////

  /** Updates the internal layout of components. */
  private void updateLayout () {

    // Remove all components
    // ---------------------
    this.removeAll();

    // Setup constraints
    // -----------------
    GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.weightx = 1;
    gc.fill = GridBagConstraints.HORIZONTAL;

    // Add each component
    // ------------------
    this.forEach (prod -> this.add (prod.getComponent(), gc));

    // Add final space-filling component
    // ---------------------------------
    gc.weighty = 1;
    gc.fill = GridBagConstraints.BOTH;
    this.add (Box.createGlue(), gc);

    // Revalidate the layout if needed
    // -------------------------------
    if (isShowing()) { 
      revalidate(); 
      repaint();
    } // if

  } // updateLayout

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the component backgrounds based on the current selection.
   * A selection property change event is also fired, since this
   * method is only ever called when the selection is changed.
   */
  private void updateBackgrounds() {

    for (int i = 0; i < producerList.size(); i++) {
      Component component = producerList.get (i).getComponent();
      if (selectionSet.contains (Integer.valueOf (i)))
        component.setBackground (selectionBackground);
      else
        component.setBackground (defaultBackground);
    } // for
    firePropertyChange (SELECTION_PROPERTY, null, null);

  } // updateBackground

  ////////////////////////////////////////////////////////////

  /** Handles click events for changes in the list selection. */
  private class SelectionListener extends MouseInputAdapter {
    public void	mouseClicked (MouseEvent e) {

      if (isSelectable) {

        // Get click count
        // ---------------
        int clicks = e.getClickCount();

        // Get component index
        // -------------------
        Component component = findComponentAt (e.getPoint());
        Integer index = 
          Integer.valueOf (producerList.indexOf (componentMap.get (component)));
        if (index.intValue() == -1) return;

        // Respond to single click
        // -----------------------
        if (clicks == 1) {

          // Add index to selection
          // ----------------------
          if (GUIServices.IS_MAC ? e.isMetaDown() : e.isControlDown()) {
            if (selectionSet.contains (index))
              selectionSet.remove (index);
            else
              selectionSet.add (index);
            lastIndex = index;
          } // if

          // Add index range to selection
          // ----------------------------
          else if (e.isShiftDown() && lastIndex != null) {
            int start = Math.min (lastIndex.intValue(), index.intValue());
            int end = Math.max (lastIndex.intValue(), index.intValue());
            selectionSet.clear();
            for (int i = start; i <= end; i++)
              selectionSet.add (Integer.valueOf (i));
          } // else if

          // Create new selection
          // --------------------
          else {
            selectionSet.clear();
            selectionSet.add (index);
            lastIndex = index;
          } // else

          // Update backgrounds
          // ------------------
          updateBackgrounds();

        } // if

      } // if

    } // mouseClicked
  } // SelectionListener class

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the number of visible rows in this list.  If the list is
   * inside a scroll window, the preferred size of the viewport is set
   * to display the specified number of rows.  By default, the visible
   * row count is 8.
   *
   * @param visibleRowCount the desired number of visible rows.
   */
  public void setVisibleRowCount (
    int visibleRowCount
  ) {

    this.visibleRowCount = visibleRowCount;

  } // setVisibleRowCount

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the visible rows in this list.
   *
   * @return the number of visible rows in the component list.
   */
  public int getVisibleRowCount () { return (visibleRowCount); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the row height.
   *
   * @return the row height, calculated as the maximum height of any of 
   * the components.
   */
  private int getRowHeight () {

    int maxHeight = 0;
    for (Iterator iter = producerList.iterator(); iter.hasNext(); ) {
      Component component = ((ComponentProducer) iter.next()).getComponent();
      int height = component.getPreferredSize().height;
      if (height > maxHeight) maxHeight = height;
    } // for

    return (maxHeight);

  } // getRowHeight

  ////////////////////////////////////////////////////////////

  @Override
  public Dimension getPreferredScrollableViewportSize () {

     Dimension size = getPreferredSize();
     if (size.width == 0) size.width = 256;
     if (size.height == 0) size.height = 16*visibleRowCount;
     else size.height = visibleRowCount * getRowHeight();
     return (size);

  } // getPreferredScrollableViewportSize

  ////////////////////////////////////////////////////////////

  @Override
  public int getScrollableBlockIncrement (
    Rectangle visibleRect, 
    int orientation, 
    int direction
  ) {

    return (visibleRowCount * getRowHeight());

  } // getScrollableBlockIncrement

  ////////////////////////////////////////////////////////////

  @Override
  public boolean getScrollableTracksViewportHeight() {

    // We return false here as this list is independent of the
    // viewport height.
  
    return (false);
    
  } // getScrollableTracksViewportHeight

  ////////////////////////////////////////////////////////////

  @Override
  public boolean getScrollableTracksViewportWidth() {
  
    // We return true here as this list expands to the viewport width.
  
    return (true);
    
  } // getScrollableTracksViewportWidth

  ////////////////////////////////////////////////////////////

  @Override
  public int getScrollableUnitIncrement (
    Rectangle visibleRect, 
    int orientation, 
    int direction
  ) {

    return (getRowHeight());

  } // getScrollableUnitIncrement

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    ComponentList<VisualOverlay> list = new ComponentList<VisualOverlay>();
    list.addElement (new VisualOverlay (
      new noaa.coastwatch.render.LatLonOverlay (Color.WHITE)));
    list.addElement (new VisualOverlay (
      new noaa.coastwatch.render.CoastOverlay (Color.WHITE)));
    list.addElement (new VisualOverlay (
      new noaa.coastwatch.render.LatLonOverlay (Color.WHITE)));
    list.addElement (new VisualOverlay (
      new noaa.coastwatch.render.CoastOverlay (Color.WHITE)));
    list.addElement (new VisualOverlay (
      new noaa.coastwatch.render.LatLonOverlay (Color.WHITE)));
    list.addElement (new VisualOverlay (
      new noaa.coastwatch.render.CoastOverlay (Color.WHITE)));
    noaa.coastwatch.gui.TestContainer.showFrame (list);

  } // main

  ////////////////////////////////////////////////////////////

} // ComponentList class

////////////////////////////////////////////////////////////////////////
