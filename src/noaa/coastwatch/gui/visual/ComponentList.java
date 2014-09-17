////////////////////////////////////////////////////////////////////////
/*
     FILE: ComponentList.java
  PURPOSE: Shows a list of components similar to a JList.
   AUTHOR: Peter Hollemans
     DATE: 2004/03/02
  CHANGES: 2006/03/11, PFH, modified for Mac Command key
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

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
import java.util.*;
import java.util.List;
import noaa.coastwatch.gui.*;

/** 
 * The <code>ComponentList</code> class is similar to a Swing
 * <code>JList</code> but with a simplified model and operations.  It
 * lays out a list of <code>ComponentProducer</code> objects from top
 * to bottom and allows them to be selected, removed, added, and so
 * on.  A change in the list selection is signaled using a property
 * change event given by <code>SELECTION_PROPERTY</code>.<p>
 *
 * The list may be placed inside a scrollable window.  The scrolling
 * behaviour works under the assumption that all components in the
 * list have the same height.  The user may set the number of visible
 * components using the <code>setVisibleRowCount()</code> method.<p>
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class ComponentList
  extends JPanel 
  implements Scrollable {

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
  private List producerList;

  /** The map of components to producers. */
  private Map componentMap;

  /** The hash set of selections. */
  private SortedSet selectionSet;

  /** The last component index that was clicked. */
  private Integer lastIndex;

  /** The selected component background. */
  private Color selectionBackground;

  /** The default component background. */
  private Color defaultBackground;

  /** The visible row count. */
  private int visibleRowCount = 8;

  ////////////////////////////////////////////////////////////

  /** Creates a new empty component list. */
  public ComponentList () {

    super (new GridBagLayout());

    // Initialize
    // ----------
    producerList = new LinkedList();
    selectionSet = new TreeSet();
    componentMap = new HashMap();

    // Add selection listener
    // ----------------------
    addMouseListener (new SelectionListener());

    // Set colors
    // ----------
    selectionBackground = (Color) UIManager.get ("List.selectionBackground");
    defaultBackground = (Color) UIManager.get ("Label.background");

  } // ComponentList constructor

  ////////////////////////////////////////////////////////////

  /** Clears the list of all components. */
  public void clear () {

    producerList.clear();
    componentMap.clear();
    reset();

  } // clear

  ////////////////////////////////////////////////////////////

  /** Adds a new component to the end of the list. */
  public void addElement (
    ComponentProducer producer
  ) { 

    addElement (producerList.size(), producer);

  } // addElement

  ////////////////////////////////////////////////////////////

  /** Adds a new component at the specified index in the list. */
  public void addElement (
    int index,
    ComponentProducer producer
  ) {

    producerList.add (index, producer);
    componentMap.put (producer.getComponent(), producer);
    reset();

  } // addElement

  ////////////////////////////////////////////////////////////

  /** Removes the specified component from the list. */
  public boolean removeElement (
    ComponentProducer producer
  ) { 

    boolean removed = producerList.remove (producer);
    if (removed) {
      componentMap.remove (producer.getComponent());
      reset();
    } // if
    return (removed);

  } // removeElement

  ////////////////////////////////////////////////////////////

  /** Removes the component at the specified index. */
  public ComponentProducer removeElement (
    int index
  ) {

    ComponentProducer producer = 
      (ComponentProducer) producerList.remove (index);
    if (producer != null) {
      componentMap.remove (producer.getComponent());
      reset();
    } // if
    return (producer);

  } // removeElement

  ////////////////////////////////////////////////////////////

  /** Moves the component between the specified indices. */
  public void moveElement (
    int source,
    int dest
  ) {

    producerList.add (dest, producerList.remove (source));
    reset();

  } // moveElement

  ////////////////////////////////////////////////////////////

  /** Gets the number of components in the list. */
  public int getElements () { return (producerList.size()); }

  ////////////////////////////////////////////////////////////
  
  /** Gets the currently selected indices. */
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
   */
  public void setSelectionInterval (
    int start, 
    int end
  ) {

    checkBounds (start);
    checkBounds (end);
    selectionSet.clear();
    for (int i = start; i <= end; i++)
      selectionSet.add (new Integer (i));
    lastIndex = new Integer (start);
    updateBackgrounds();

  } // setSelectionInterval

  ////////////////////////////////////////////////////////////

  /** Gets the component at the specified index. */
  public ComponentProducer getElement (
    int index
  ) {

    return ((ComponentProducer) producerList.get (index));

  } // getElement

  ////////////////////////////////////////////////////////////

  /** Returns an iterator over components in the list. */
  public Iterator iterator () { return (producerList.iterator()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Checks if the index is within the allowed bounds, and throws an
   * exception if not.
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
    for (Iterator iter = producerList.iterator(); iter.hasNext();) 
      this.add (((ComponentProducer) iter.next()).getComponent(), gc);

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
      Component component = 
        ((ComponentProducer) producerList.get (i)).getComponent();
      if (selectionSet.contains (new Integer (i)))
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

      // Get click count
      // ---------------
      int clicks = e.getClickCount();

      // Get component index
      // -------------------
      Component component = findComponentAt (e.getPoint());
      Integer index = 
        new Integer (producerList.indexOf (componentMap.get (component)));
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
            selectionSet.add (new Integer (i));
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

  /** Gets the number of visible rows in this list. */
  public int getVisibleRowCount () { return (visibleRowCount); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the height of each component.  The maximum height of any of
   * the components is returned.
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

  /** Gets the scrollable viewport size. */
  public Dimension getPreferredScrollableViewportSize () {

     Dimension size = getPreferredSize();
     if (size.width == 0) size.width = 256;
     if (size.height == 0) size.height = 16*visibleRowCount;
     else size.height = visibleRowCount * getRowHeight();
     return (size);

  } // getPreferredScrollableViewportSize

  ////////////////////////////////////////////////////////////

  /** Gets the scrollable block increment. */
  public int getScrollableBlockIncrement (
    Rectangle visibleRect, 
    int orientation, 
    int direction
  ) {

    return (visibleRowCount * getRowHeight());

  } // getScrollableBlockIncrement

  ////////////////////////////////////////////////////////////

  /** Returns false as this list is independent of the viewport height. */
  public boolean getScrollableTracksViewportHeight() { return (false); }

  ////////////////////////////////////////////////////////////

  /** Returns true as this list expands to the viewport width. */
  public boolean getScrollableTracksViewportWidth() { return (true); }

  ////////////////////////////////////////////////////////////

  /** Gets the scrollable unit increment. */
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

    ComponentList list = new ComponentList();
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
