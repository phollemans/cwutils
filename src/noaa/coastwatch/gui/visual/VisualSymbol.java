////////////////////////////////////////////////////////////////////////
/*
     FILE: VisualSymbol.java
  PURPOSE: Defines a visual interface for a plot symbol.
   AUTHOR: Peter Hollemans
     DATE: 2008/06/20
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2008, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import jahuwaldt.plot.PlotSymbol;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;
import noaa.coastwatch.render.PlotSymbolFactory;

import jahuwaldt.plot.PlotSymbol;
/**
 * The <code>VisualSymbol</code> class represents a plot symbol
 * as a combo box with an icon of the symbol.
 *
 * @author Peter Hollemans
 * @since 3.2.3
 */
public class VisualSymbol 
  extends AbstractVisualObject {

  // Constants 
  // ---------

  /** The size of the symbol icon. */
  private static final int ICON_SIZE = 16;

  // Variables
  // ---------

  /** The set of possible symbols. */
  private static List<PlotSymbol> symbolList;

  /** The symbol component combo box. */
  private JComboBox symbolCombo;

  ////////////////////////////////////////////////////////////

  static {

    // Create array of plot symbols
    // ----------------------------
    symbolList = new ArrayList<PlotSymbol>();
    for (Iterator<String> iter = PlotSymbolFactory.getSymbolNames();
      iter.hasNext();) {
      symbolList.add (PlotSymbolFactory.create (iter.next()));
    } // for

  } // static

  ////////////////////////////////////////////////////////////

  /** Creates a new visual symbol object using the specified symbol. */
  public VisualSymbol (
    PlotSymbol symbol
  ) {                     

    // Create combo box
    // ----------------
    symbolCombo = new JComboBox (symbolList.toArray());
    symbolCombo.setSelectedItem (symbol);
    //    symbolCombo.addActionListener (new SymbolComboListener());
    symbolCombo.setRenderer (new SymbolRenderer());

  } // VisualSymbol constructor

  ////////////////////////////////////////////////////////////

  /** Renders the symbols as symbol swatches. */
  private class SymbolRenderer
    extends JLabel
    implements ListCellRenderer {

    ////////////////////////////////////////////////////////

    /** Creates a new opaque symbol renderer. */
    public SymbolRenderer () {

      setOpaque (true);

    } // SymbolRenderer constructor

    ////////////////////////////////////////////////////////

    /** Sets this label to show a stroke swatch icon. */
    public Component getListCellRendererComponent (
      JList list,
      Object value,
      int index,
      boolean isSelected,
      boolean cellHasFocus
    ) {

      /*
      setIcon (new SymbolSwatch (getBasicStroke (2, (float[]) value),
        SWATCH_SIZE*3, SWATCH_SIZE));
      */
      if (isSelected) {
        setBackground (list.getSelectionBackground());
        setForeground (list.getSelectionForeground());
      } // if
      else {
        setBackground (list.getBackground());
        setForeground (list.getForeground());
      }  // else
      return (this);

    } // getListCellRendererComponent

    ////////////////////////////////////////////////////////

  } // SymbolRenderer class

  ////////////////////////////////////////////////////////////

  /** Gets the combo box used to represent the symbol. */
  public Component getComponent () { return (symbolCombo); }

  ////////////////////////////////////////////////////////////

  /** Gets the symbol value. */
  public Object getValue () { return (symbolCombo.getSelectedItem()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    /*
    JPanel panel = new JPanel();
    Stroke stroke = StrokeChooser.getBasicStroke (2, 
      StrokeChooser.DASH_PATTERNS[1]);
    Component comp =  new VisualSymbol (stroke).getComponent();
    panel.add (comp);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);
    */

  } // main

  ////////////////////////////////////////////////////////////

} // VisualSymbol class

////////////////////////////////////////////////////////////////////////
