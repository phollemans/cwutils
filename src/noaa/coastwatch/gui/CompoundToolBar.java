////////////////////////////////////////////////////////////////////////
/*
     FILE: CompoundToolBar.java
  PURPOSE: Combines two or more toolbars into one.
   AUTHOR: Peter Hollemans
     DATE: 2004/05/10
  CHANGES: 2006/06/29, PFH, modified to use GUIServices.setSameSize()

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.util.*;
import java.util.List;
import java.awt.*;
import javax.swing.*;

/**
 * The <code>CompoundToolBar</code> class combines two or more
 * toolbars into one by "stealing" the buttons from the other toolbars
 * for itself.  Components are laid out left-to-right and separators
 * are placed between component groups.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class CompoundToolBar
  extends JToolBar {

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new compound toolbar from the specified toolbars.
   *
   * @param toolbars the toolbars to combine.
   * @param sameSize the same size flag, true to make all components
   * the same size as the maximum size component.
   */
  public CompoundToolBar (
    JToolBar[] toolbars,
    boolean sameSize
  ) {

    // Initialize
    // ----------
    setLayout (new FlowLayout (FlowLayout.LEFT, 2, 2));

    // Add components
    // --------------
    List componentList = new ArrayList();
    for (int i = 0; i < toolbars.length; i++) {
      while (toolbars[i].getComponentCount() != 0) {
        JComponent comp = (JComponent) toolbars[i].getComponentAtIndex (0);
        this.add (comp);
        componentList.add (comp);
      } // while
      if (i != toolbars.length-1) this.addSeparator();
    } // for

    // Make same size
    // --------------
    if (sameSize) GUIServices.setSameSize (componentList);

  } // CompoundToolBar constructor

  ////////////////////////////////////////////////////////////

  /** Tests this class. */
  public static void main (String[] argc) {

    FileOperationChooser fileChooser = FileOperationChooser.getInstance();
    ViewOperationChooser viewChooser = ViewOperationChooser.getInstance();
    CompoundToolBar compound = new CompoundToolBar (
      new JToolBar[] {fileChooser, viewChooser}, true);
    compound.setFloatable (false);
    noaa.coastwatch.gui.TestContainer.showFrame (compound);

  } // main

  ////////////////////////////////////////////////////////////

} // CompoundToolBar class

////////////////////////////////////////////////////////////////////////
