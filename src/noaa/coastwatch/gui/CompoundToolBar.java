////////////////////////////////////////////////////////////////////////
/*

     File: CompoundToolBar.java
   Author: Peter Hollemans
     Date: 2004/05/10

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
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.FlowLayout;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.BorderFactory;

import java.util.ArrayList;
import java.util.List;

import noaa.coastwatch.gui.FileOperationChooser;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;
import noaa.coastwatch.gui.ViewOperationChooser;

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

  private List<JComponent> componentList;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new compound toolbar from the specified toolbars.
   *
   * @param toolbars the toolbars to combine.
   * @param separators the list of separator flags, true to put a separator
   * after the toolbar, or false to put a space (not applicable to the last
   * toolbar).
   * @param sameSize the same size flag, true to make all components
   * the same size as the maximum size component.
   */
  public CompoundToolBar (
    JToolBar[] toolbars,
    boolean[] separators,
    boolean sameSize
  ) {

    // Initialize
    // ----------
    this.setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
    this.setBorder (BorderFactory.createEmptyBorder (2, 5, 2, 5));

    // Add components
    // --------------
    componentList = new ArrayList<>();
    for (int i = 0; i < toolbars.length; i++) {
      while (toolbars[i].getComponentCount() != 0) {
        JComponent comp = (JComponent) toolbars[i].getComponentAtIndex (0);
        comp.setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
        this.add (comp);
        this.add (Box.createHorizontalStrut (2));
        componentList.add (comp);
      } // while
      if (i < toolbars.length-1) {
        if (separators[i]) this.addSeparator (new Dimension (20, 20));
        else this.add (Box.createHorizontalGlue());
      } // if
    } // for
    this.add (Box.createHorizontalGlue());

    // Make same size
    // --------------
    if (sameSize) GUIServices.setSameSize (componentList);

  } // CompoundToolBar constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the button sizes of this toolbar after a button content change.
   * 
   * @since 3.8.1
   */
  public void updateButtonSize () { GUIServices.setSameSize (componentList); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

    FileOperationChooser fileChooser = FileOperationChooser.getInstance();
    ViewOperationChooser viewChooser = ViewOperationChooser.getInstance();
    CompoundToolBar compound = new CompoundToolBar (
      new JToolBar[] {fileChooser, viewChooser}, new boolean[] {true, false}, true);
    compound.setFloatable (false);
    noaa.coastwatch.gui.TestContainer.showFrame (compound);

  } // main

  ////////////////////////////////////////////////////////////

} // CompoundToolBar class

////////////////////////////////////////////////////////////////////////
