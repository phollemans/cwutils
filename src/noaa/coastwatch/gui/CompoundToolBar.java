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
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JToolBar;
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

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) {

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
