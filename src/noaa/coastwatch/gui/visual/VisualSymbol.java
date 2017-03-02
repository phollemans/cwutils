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
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import noaa.coastwatch.gui.visual.AbstractVisualObject;
import noaa.coastwatch.render.PlotSymbolFactory;

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
  private static final int ICON_SIZE = 14;

  // Variables
  // ---------

  /** The symbol component combo box. */
  private JComboBox symbolCombo;

  ////////////////////////////////////////////////////////////

  /** Creates a new visual symbol object using the specified symbol. */
  public VisualSymbol (
    PlotSymbol symbol
  ) {                     

    // Create combo box
    // ----------------
    symbolCombo = new JComboBox<PlotSymbol>();
    PlotSymbolFactory.getSymbolNames()
      .forEachRemaining (name -> symbolCombo.addItem (PlotSymbolFactory.create (name)));
    symbolCombo.setSelectedItem (symbol);
    symbolCombo.addActionListener (event -> firePropertyChange());
    symbolCombo.setRenderer (new SymbolRenderer());

  } // VisualSymbol constructor

  ////////////////////////////////////////////////////////////

  /** Renders the symbols as symbol swatches. */
  private class SymbolRenderer
    extends JLabel
    implements ListCellRenderer<PlotSymbol> {

    ////////////////////////////////////////////////////////

    /** Creates a new opaque symbol renderer. */
    public SymbolRenderer () {

      setOpaque (true);

    } // SymbolRenderer constructor

    ////////////////////////////////////////////////////////

    /** Sets this label to show a symbol swatch icon. */
    public Component getListCellRendererComponent (
      JList list,
      PlotSymbol value,
      int index,
      boolean isSelected,
      boolean cellHasFocus
    ) {

      setIcon (new SymbolSwatch (value, ICON_SIZE));
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

  @Override
  public Component getComponent () { return (symbolCombo); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getValue () { return (symbolCombo.getSelectedItem()); }

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new JPanel();
    String symbolName = PlotSymbolFactory.getSymbolNames().next();
    PlotSymbol symbol = PlotSymbolFactory.create (symbolName);
    Component comp =  new VisualSymbol (symbol).getComponent();
    panel.add (comp);
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // VisualSymbol class

////////////////////////////////////////////////////////////////////////
