////////////////////////////////////////////////////////////////////////
/*
     FILE: FontChooser.java
  PURPOSE: To show a font chooser panel.
   AUTHOR: Peter Hollemans
     DATE: 2004/02/27
  CHANGES: n/a
           
  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.visual;

// Imports
// -------
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import noaa.coastwatch.gui.GUIServices;
import noaa.coastwatch.gui.TestContainer;

/**
 * The <code>FontChooser</code> class is a panel that displays font 
 * family, style, and size controls to allow the user to select a
 * letter font.  When the user selects a font, a property change
 * event is fired whose property is given by
 * <code>FONT_PROPERTY</code>.
 *
 * @author Peter Hollemans
 * @since 3.1.7
 */
public class FontChooser
  extends JPanel {

  // Constants
  // ---------

  /** The font property. */
  public static String FONT_PROPERTY = "font_selection";

  /** The swatch size. */
  private static final int SWATCH_SIZE = 16;

  /** The available font families. */
  private static final String[] FAMILY_NAMES = 
    GraphicsEnvironment.getLocalGraphicsEnvironment().
    getAvailableFontFamilyNames();

  /** The minimum font size. */
  private static final int FONT_MIN = 5;

  /** The maximum font size. */
  private static final int FONT_MAX = 40;

  /** The font sample string. */
  private static final String FONT_SAMPLE = "Aa Bb Cc";

  // Variables
  // ---------

  /** The initial font chooser font. */
  private Font font;

  /** The font family combo box. */
  private JComboBox familyCombo;

  /** The bold checkbox. */
  private JCheckBox boldCheck;

  /** The italic checkbox. */
  private JCheckBox italicCheck;

  /** The point size spinner. */
  private JSpinner sizeSpinner;

  /** The sample font label shown in the sample panel. */
  private JLabel sampleLabel;

  ////////////////////////////////////////////////////////////

  /** Gets the index of the family of the specified font. */
  private int getFamilyIndex (
    Font font
  ) {

    String family = font.getFamily();
    for (int i = 0; i < FAMILY_NAMES.length; i++) {
      if (family.equalsIgnoreCase (FAMILY_NAMES[i])) return (i);
    } // for
    throw new IllegalArgumentException ("Unknown font family: " + family);

  } // getFamilyIndex

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new font chooser.  The dialog, plain style, 12 point
   * font is used initially.
   */
  public FontChooser () {

    this (null);

  } // FontChooser

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new font chooser with the specified initial font.
   *
   * @param font the initial font for the main font chooser. If null,
   * the dialog, plain style, 12 point font is used initially.
   */
  public FontChooser (
    Font font
  ) {

    super (new GridBagLayout());

    // Initialize
    // ----------
    if (font == null) font = Font.decode (null);

    // Create font family combo
    // ------------------------
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    GUIServices.setConstraints (gc, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Font family:"), gc);
    gc.insets = new Insets (2, 0, 2, 0);

    familyCombo = new JComboBox (FAMILY_NAMES);
    familyCombo.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          updateFont();
        } // actionPerformed
      });
    GUIServices.setConstraints (gc, 1, 0, 1, 1, GridBagConstraints.NONE,
      0, 0);
    this.add (familyCombo, gc);

    // Create bold checkbox
    // --------------------
    boldCheck = new JCheckBox ("Use bold style");
    boldCheck.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          updateFont();
        } // actionPerformed
      });
    GUIServices.setConstraints (gc, 0, 2, 2, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    this.add (boldCheck, gc);

    // Create italic checkbox
    // ----------------------
    italicCheck = new JCheckBox ("Use italic style");
    italicCheck.addActionListener (new ActionListener() {
        public void actionPerformed (ActionEvent event) {
          updateFont();
        } // actionPerformed
      });
    GUIServices.setConstraints (gc, 0, 3, 2, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    this.add (italicCheck, gc);

    // Create point size spinner
    // -------------------------
    GUIServices.setConstraints (gc, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    gc.insets = new Insets (2, 0, 2, 10);
    this.add (new JLabel ("Point size:"), gc);
    gc.insets = new Insets (2, 0, 2, 0);
   
    sizeSpinner = new JSpinner (new SpinnerNumberModel (12, 
      FONT_MIN, FONT_MAX, 1));
    sizeSpinner.addChangeListener (new ChangeListener() {
        public void stateChanged (ChangeEvent event) {
          updateFont();
        } // stateChanged
      });
    GUIServices.setConstraints (gc, 1, 1, 1, 1, GridBagConstraints.NONE, 
      0, 0);
    this.add (sizeSpinner, gc);

    // Create sample panel
    // -------------------
    JPanel samplePanel = new JPanel();
    samplePanel.setBorder (new TitledBorder (new EtchedBorder(), "Sample"));
    sampleLabel = new AntialiasedLabel (FONT_SAMPLE);
    sampleLabel.setPreferredSize (
      new Dimension (FONT_MAX*FONT_SAMPLE.length()*3/4, FONT_MAX));
    sampleLabel.setHorizontalAlignment (SwingConstants.CENTER);
    sampleLabel.setVerticalAlignment (SwingConstants.CENTER);
    samplePanel.add (sampleLabel);
    GUIServices.setConstraints (gc, 0, 4, 2, 1, GridBagConstraints.HORIZONTAL, 
      0, 0);
    this.add (samplePanel, gc);

    // Set initial font
    // ----------------
    setFontSelection (font);

  } // FontChooser

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the current font based on the family, face, and size
   * controllers.
   */
  private void updateFont () {

    int familyIndex = familyCombo.getSelectedIndex();
    boolean bold = boldCheck.isSelected();
    boolean italic = italicCheck.isSelected();
    int style;
    if (!bold && !italic) 
      style = Font.PLAIN;
    else {
      style = 0;
      if (bold) style += Font.BOLD;
      if (italic) style += Font.ITALIC;
    } // else
    int size = ((Integer) sizeSpinner.getValue()).intValue();
    setFontSelection (new Font (FAMILY_NAMES[familyIndex], style, size));

  } // updateFont

  ////////////////////////////////////////////////////////////

  /** Gets the font selected by this font chooser. */
  public Font getFontSelection () { return (font); }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the font selected by this font chooser.  The font may
   * be null, in which case this method has no effect.
   */
  public void setFontSelection (Font newFont) { 

    if (newFont != null && !newFont.equals (font)) {

       // Modify components
       // -----------------
       familyCombo.setSelectedIndex (getFamilyIndex (newFont));
       boldCheck.setSelected (newFont.isBold());
       italicCheck.setSelected (newFont.isItalic());
       sizeSpinner.setValue (new Integer (newFont.getSize()));

      // Update sample
      // -------------
      sampleLabel.setFont (newFont);

      // Set new font
      // ------------
      this.font = newFont;
      firePropertyChange (FONT_PROPERTY, null, newFont);

    } // if

  } // setFontSelection

  ////////////////////////////////////////////////////////////

  /** Allows standard JLabels to show antialiased text. */
  private class AntialiasedLabel 
    extends JLabel {

    public AntialiasedLabel (String text) { super (text); }

    /** Paints this label with antialiasing. */
    protected void paintComponent (Graphics g) {

      // Turn on antialiasing and pass to super
      // --------------------------------------
      ((Graphics2D) g).setRenderingHint (RenderingHints.KEY_ANTIALIASING, 
        RenderingHints.VALUE_ANTIALIAS_ON);
      super.paintComponent (g);

    } // paintComponent

  } // AntialiasedLabel

  ////////////////////////////////////////////////////////////
  
  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String argv[]) {
  
    JPanel panel = new FontChooser (Font.decode (argv[0]));
    noaa.coastwatch.gui.TestContainer.showFrame (panel);

  } // main

  ////////////////////////////////////////////////////////////

} // FontChooser

////////////////////////////////////////////////////////////////////////

