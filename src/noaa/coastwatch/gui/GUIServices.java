////////////////////////////////////////////////////////////////////////
/*

     File: GUIServices.java
   Author: Peter Hollemans
     Date: 2002/10/09

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Toolkit;
import java.awt.GraphicsEnvironment;
import java.awt.AlphaComposite;
import java.awt.Rectangle;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.awt.image.ImageFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URI;
import java.text.BreakIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;
import java.util.Map;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JFrame;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import noaa.coastwatch.gui.IconFactory;
import noaa.coastwatch.gui.IconFactory.Purpose;
import noaa.coastwatch.gui.IconFactory.Mode;
import noaa.coastwatch.gui.HTMLPanel;
import noaa.coastwatch.gui.PanelOutputStream;
import noaa.coastwatch.tools.ToolServices;

import com.install4j.api.launcher.StartupNotification;

import java.util.logging.Logger;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The GUI services class defines various static methods relating
 * to graphical user interfaces.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
@noaa.coastwatch.test.Testable
public class GUIServices {

  private static final Logger LOGGER = Logger.getLogger (GUIServices.class.getName());  

  // Constants
  // ---------

  /** The properties file of icon purpose to icon file. */
  private static final String ICON_PROPERTIES_FILE = "icon.properties";

  /** The properties file of class name to help file. */
  private static final String HELP_PROPERTIES_FILE = "help.properties";

  /** The Mac boolean, true if we are running on a Mac. **/
  public static final boolean IS_MAC = 
    System.getProperty ("os.name").toLowerCase().indexOf ("mac") != -1;

  /** The Windows boolean, true if we are running on Windows. **/
  public static final boolean IS_WIN = 
    System.getProperty ("os.name").toLowerCase().indexOf ("win") != -1;

  /** The Linux boolean, true if we are running on Linux. **/
  public static final boolean IS_LINUX = 
    System.getProperty ("os.name").toLowerCase().indexOf ("linux") != -1;

  /** The Aqua boolean, true if we are running on a Mac with Aqua look. */
  public static final boolean IS_AQUA = 
    UIManager.getLookAndFeel().getID().equals ("Aqua");

  /** The GTK boolean, true if we are running the GTK look. */
  public static final boolean IS_GTK = 
    UIManager.getLookAndFeel().getID().equals ("GTK");

  /** The insets to use for icon-only button margins. */
  public static final Insets ICON_INSETS = new Insets (2, 2, 2, 2);
  
  /** The preferences key for application window width. */
  public static final String WINDOW_WIDTH_KEY = "window.width";

  /** The preferences key for applicationwindow height. */
  public static final String WINDOW_HEIGHT_KEY = "window.height";

  /** The preferences key for recently opened files. */
  public static final String RECENT_FILES_KEY = "recent.files";

  // Variables
  // ---------

  /** The properties mapping icon purpose to icon file. */
  private static Properties iconProperties;

  /** The properties mapping class name to help file. */
  private static Properties helpProperties;

  /** The error dialog flag, true if the error dialog is showing. */
  private static boolean errorShowing;

  /** The error dialog. */
  private static JDialog errorDialog;

  /** The output stream for standard error messages. */
  private static PanelOutputStream panelStream;

  /** The current directory for file chooser. */
  private static File userDir;

  /** The help index URL to use for help panels. */
  private static URL helpIndex;
  
  /** The currently active full screen window on Mac. */
  private static Window fullScreenMacWindow;

  ////////////////////////////////////////////////////////////

  /** Loads the icon properties file. */
  static {

    // Get static properties
    // ---------------------
    iconProperties = getPropertiesFromResource (ICON_PROPERTIES_FILE);
    helpProperties = getPropertiesFromResource (HELP_PROPERTIES_FILE);

    // Get current directory
    // ---------------------
    userDir = getPlatformDefaultDirectory();

  } // static

  ////////////////////////////////////////////////////////////

  /**
   * 
   * 
   * @since 3.8.1
   */
  public static Action createAction (
    String className,
    String command,
    String text,
    String iconPurpose,
    String desc
  ) {

    var action = new AbstractAction (text, getIcon (iconPurpose)) {
      public void actionPerformed (ActionEvent e) { }
    };
    action.putValue (Action.ACTION_COMMAND_KEY, className + ":" + command);
    if (desc != null) action.putValue (Action.SHORT_DESCRIPTION, desc);

    return (action);

  } // createAction

  ////////////////////////////////////////////////////////////

  /**
   * 
   * 
   * @since 3.8.1
   */
  public static void centerOnScreen (JFrame frame) {

    var device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    var config = device.getDefaultConfiguration();
    var bounds = config.getBounds();
    var insets = Toolkit.getDefaultToolkit().getScreenInsets (config);

    bounds.x += insets.left;
    bounds.y += insets.top;
    bounds.width -= (insets.left + insets.right);
    bounds.height -= (insets.top + insets.bottom);

    var size = frame.getSize();
    var x = bounds.x + bounds.width/2 - size.width/2;
    var y = bounds.y + bounds.height/2 - size.height/2;
    frame.setLocation (x, y);

  } // centerOnScreen

  ////////////////////////////////////////////////////////////

  /** 
   * Initializes the environment prior to creating any GUI components.
   * 
   * @since 3.7.1
   */
  public static void initializeLaf () {

    var laf = System.getProperty ("swing.defaultlaf", "");
    if (laf.indexOf ("flatlaf") != -1) {
      UIManager.put ("TabbedPane.tabType", "card");
      UIManager.put ("EditorPane.inactiveBackground", UIManager.get ("EditorPane.background"));
    } // if

  } // initializeLaf

  ////////////////////////////////////////////////////////////

  /**
   * Creates a button with an on screen display type of style and translucent
   * properties.
   * 
   * @param defaultIcon the icon to use.
   * @param rolloverIcon the rollover icon to use or null.
   * @param pressedIcon the pressed icon to use or null.
   * 
   * @return the button.
   *
   * @since 3.8.1
   */
  public static TranslucentButton createTranslucentButton (
    Icon defaultIcon,
    Icon rolloverIcon,
    Icon pressedIcon
  ) {

    var button = new TranslucentButton (defaultIcon);
    if (rolloverIcon != null) button.setRolloverIcon (rolloverIcon);
    if (pressedIcon != null) button.setPressedIcon (pressedIcon);

    button.setOpaque (false);
    button.setContentAreaFilled (false);
    button.setBorderPainted (false);
    button.setFocusPainted (false);

    return (button);

  } // createTranslucentButton

  ////////////////////////////////////////////////////////////

  private static class OperatorImageFilter extends RGBImageFilter {

    private BiConsumer<int[],int[]> operator;

    public OperatorImageFilter (BiConsumer<int[],int[]> operator) { 
      canFilterIndexColorModel = true; 
      this.operator = operator;
    } // OperatorImageFilter

    private void decodeRGB (int rgb, int[] pixel) {
      pixel[0] = (rgb & 0x00ff0000) >> 16;
      pixel[1] = (rgb & 0x0000ff00) >> 8;
      pixel[2] = (rgb & 0x000000ff);
      pixel[3] = (rgb & 0xff000000) >> 24;
    } // decodeRGB

    private int encodeRGB (int[] pixel) {
      int rgb = (pixel[3] << 24) | (pixel[0] << 16) | (pixel[1] << 8) | pixel[2];
      return (rgb);      
    } // encodeRGB

    public int filterRGB (int x, int y, int rgb) {

      int[] pixelIn = new int[4];
      int[] pixelOut = new int[4];
      decodeRGB (rgb, pixelIn);
      operator.accept (pixelIn, pixelOut);
      return (encodeRGB (pixelOut));

    } // filterRGB

  } // OperatorImageFilter class

  ////////////////////////////////////////////////////////////

  private static ImageFilter createImageFilter (
    BiConsumer<int[], int[]> operator
  ) {

    return (new OperatorImageFilter (operator));

  } // createImageFilter

  ////////////////////////////////////////////////////////////

  public static Image createModifiedImage (
    Image image, 
    BiConsumer<int[],int[]> operator
  ) {

    var filter = new OperatorImageFilter (operator);
    var modifiedImage = Toolkit.getDefaultToolkit().createImage (
      new FilteredImageSource (image.getSource(), filter));

    return (modifiedImage);

  } // createModifiedImage

  ////////////////////////////////////////////////////////////

  /**
   * Creates a button with an on screen display type of style.
   * 
   * @param size the width and height of the button.
   * @param purpose the button purpose.
   * 
   * @return the button.
   *
   * @since 3.8.1
   */
  public static JButton createOnScreenStyleButton (
    int size, 
    Purpose purpose
  ) {

    var factory = IconFactory.getInstance();
    var button = new JButton (factory.createIcon (purpose, Mode.NORMAL, size));
    button.setRolloverIcon (factory.createIcon (purpose, Mode.HOVER, size));
    button.setPressedIcon (factory.createIcon (purpose, Mode.PRESSED, size));
    button.setOpaque (false);
    button.setContentAreaFilled (false);
    button.setBorderPainted (false);
    button.setFocusPainted (false);

    return (button);

  } // createOnScreenStyleButton

  ////////////////////////////////////////////////////////////

  /**
   * Creates a darkening function to be use with a composite.
   * 
   * @param f the darkening factor.
   * 
   * @return the darkening function.
   * 
   * @since 3.8.1
   * 
   * @see #createComposite
   */
  public static BiFunction<int[], int[], Boolean> createDarkenFunction (double f) {

    BiFunction<int[], int[], Boolean> func = (src,dest) -> {

      var write = false;

      if (src[3] != 0) {
        float srcAlpha = src[3] / 255.0f;
        for (int i = 0; i < 3; i++) {
          src[i] = (int) Math.round (src[i]*f);
          int value = (int) Math.round (src[i]*srcAlpha + dest[i]*(1-srcAlpha));
          if (value < 0) value = 0;
          else if (value > 255) value = 255;
          dest[i] = value;
        } // for
        write = true;
      } // if

      return (write);

    };

    return (func);

  } // createDarkenFunction

  ////////////////////////////////////////////////////////////

  /**
   * Creates a composite that uses a function to specify the mapping from
   * source to destination pixels.
   * 
   * @param function a function that takes (srcPixels, destPixels) and 
   * overwrites destPixels with the output composite pixel values.  The function
   * returns true if successful and the destPixels values should be written
   * to the output, or false if not.  The source and destination pixels are
   * stored as int[] arrays that contain [red, green, blue, alpha] in the 
   * range of 0-255.
   * 
   * @return the composite object.
   * 
   * @since 3.8.1
   */
  public static Composite createComposite (
    BiFunction<int[], int[], Boolean> function
  ) {

    var composite = new Composite() {

      @Override
      public CompositeContext createContext (ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {

        var context = new CompositeContext() {

          @Override
          public void dispose () { }

          @Override
          public void compose (Raster src, Raster dstIn, WritableRaster dstOut) {

            int[] srcPixels = new int[4]; 
            int[] dstPixels = new int[4];

            for (int x = 0; x < src.getWidth(); x++) {
              for (int y = 0; y < src.getHeight(); y++) {
                src.getPixel (x, y, srcPixels);
                dstIn.getPixel (x, y, dstPixels);
                var success = function.apply (srcPixels, dstPixels);
                if (success) dstOut.setPixel (x, y, dstPixels);
              } // for
            } // for

          } // compose

        };
        return (context);
      } // createContext

    };

    return (composite);

  } // createComposite

  ////////////////////////////////////////////////////////////

  /**
   * Creates a label with text and URL that opens a browser window.
   * 
   * @param text the text for the label.
   * @param url the URL to open when the label is clicked.
   * 
   * @return the hyperlink label.
   * 
   * @since 3.8.1
   */
  public static JLabel createLinkLabel (
    String text,
    String url
  ) {

    var html = "<html><a href=\"" + url + "\">" + text + "</a></html>";
    var label = new JLabel (html);
    label.setCursor (Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));
    label.setToolTipText (url);
    label.addMouseListener (new MouseAdapter() {
      @Override
      public void mouseClicked (MouseEvent event) {
        try { Desktop.getDesktop().browse (new URI (url)); }
        catch (Exception e) {
          throw new RuntimeException (e);
        } // catch
      } // mouseClicked     
    });

    return (label);

  } // createLinkLabel

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an about component appropriate for an about dialog box.
   * 
   * @param tool the tool or program name.
   *
   * @return the about component.
   * 
   * @since 3.8.1
   */
  public static Component getAboutComponent (
    String tool
  ) {

    String os = 
      System.getProperty ("os.name") + " " + 
      System.getProperty ("os.version") + " " + 
      System.getProperty ("os.arch");
    String jvm = System.getProperty ("java.version") + " on " + os;

    var items = List.of (
      "Program",
      "Package",
      "Version",
      "Java version",
      "Website",
      "Author",
      "Support",
      "Copyright"
    );
    var itemCount = items.size();
    var values = List.of (
      tool,
      ToolServices.PACKAGE,
      ToolServices.getVersion(),
      jvm,
      ToolServices.WEBSITE,
      ToolServices.AUTHOR,
      ToolServices.SUPPORT,
      ToolServices.COPYRIGHT
    );

    var panel = Box.createHorizontalBox();
    var itemBox = Box.createVerticalBox();
    panel.add (itemBox);
    panel.add (Box.createHorizontalStrut (10));
    var valueBox = Box.createVerticalBox();
    panel.add (valueBox);

    var bold = UIManager.getFont ("Label.font").deriveFont (Font.BOLD);

    for (int i = 0; i < itemCount; i++) {
      var item = items.get (i);
      var itemLabel = new JLabel (item + ":");
      itemLabel.setFont (bold);
      itemBox.add (itemLabel);
      var value = values.get (i);
      valueBox.add (item.equals ("Website") ? 
        createLinkLabel (value.substring (value.lastIndexOf ("/")+1, value.length()), value) : 
        new JLabel (value)
      );
    } // for

    return (panel);

  } // getAboutComponent

  ////////////////////////////////////////////////////////////

  /**
   * Gets the platform default directory for opening new files.
   *
   * @return the platform default directory or null if not available.
   */
  public static File getPlatformDefaultDirectory () {
  
    File dir = null;
    try {
      String dirProperty = (IS_MAC || IS_WIN ? "user.home" : "user.dir");
      dir = new File (dirProperty);
    } // try
    catch (SecurityException e) {
      /*
       * If we get here, then we must be running as an applet
       * because generally there's no other reason that we can't
       * have access to the local filesystem.  If we're running
       * as an applet, we're not likely to call the
       * getFileChooser() method anyway, so we just leave
       * the userDir as null.
       */
    } // catch
  
    return (dir);

  } // getPlatformDefaultDirectory

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a set of properties from a resource file.
   * 
   * @param resource the resource file to load.
   *
   * @return the new properties loaded.
   *
   * @throws RuntimeException if an error occurred finding the
   * resource or loading the properties.
   */
  private static Properties getPropertiesFromResource (
    String resource
  ) {

    // Get properties resource
    // -----------------------
    InputStream stream = GUIServices.class.getResourceAsStream (resource);
    if (stream == null) {
      throw new RuntimeException ("Cannot find resource '" + resource + "'");
    } // if
    
    // Load properties from resource
    // -----------------------------
    Properties props = new Properties();
    try {
      props.load (stream);
      stream.close();   
    } // try
    catch (IOException e) {
      throw new RuntimeException ("Error loading properties: " + 
        e.getMessage());
    } // catch

    return (props);

  } // getPropertiesFromResource

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the values in a <code>GridBagConstraints</code> object.
   *
   * @param c the contraints object.
   * @param gridx the grid x position.
   * @param gridy the grid y position.
   * @param gridwidth the number of grid cells wide.
   * @param gridheight the number of grid cells high.
   * @param fill the fill mode.
   * @param weightx the weight in the x direction.
   * @param weighty the weight in the y direction.
   */
  public static void setConstraints (
    GridBagConstraints c,
    int gridx,
    int gridy,
    int gridwidth,
    int gridheight,
    int fill,
    double weightx,
    double weighty
  ) {                           

    c.gridx = gridx;
    c.gridy = gridy;
    c.gridheight = gridheight;
    c.gridwidth = gridwidth;
    c.fill = fill;
    c.weightx = weightx;
    c.weighty = weighty;

  } // setConstraints

  ////////////////////////////////////////////////////////////
  
  /** 
   * This is a class to handle the text foreground color changes that
   * happen under the Mac Aqua look and feel that aren't implemented by 
   * default.
   */
  private static class AquaButtonListener
    extends MouseAdapter
    implements FocusListener {
  
    /** The flag indicating if we are currently being pressed. */
    boolean isPressed = false;

    public void mousePressed (MouseEvent event) {
      isPressed = true;
      event.getComponent().setForeground (Color.WHITE);
    } // mousePressed

    public void mouseReleased (MouseEvent event) {
      isPressed = false;
      event.getComponent().setForeground (UIManager.getColor ("Button.foreground"));
    } // mouseReleased

    public void mouseEntered (MouseEvent event) {
      if (isPressed) {
        event.getComponent().setForeground (Color.WHITE);
      } // if
    } // mouseEntered

    public void mouseExited (MouseEvent event) {
      if (isPressed) {
        event.getComponent().setForeground (UIManager.getColor ("Button.foreground"));
      } // if
    } // mouseExited

    public void focusLost (FocusEvent event) {
      event.getComponent().setForeground (UIManager.getColor ("Button.foreground"));
    } // focusLost

    public void focusGained (FocusEvent event) { }

  } // AquaButtonListener class

  ////////////////////////////////////////////////////////////

  /**
   * Adds a listener to the button to enhance the behaviour under Aqua.
   *
   * @param button the button to modify.
   */
  private static void applyAquaButtonTreatment (
    JButton button
  ) {
  
  // TODO: This doesn't actually work correctly (see the CDAT file open
  // dialog for an issue), so for now we disable it until we can do more
  // testing.

/*
    AquaButtonListener listener = new AquaButtonListener();
    button.addMouseListener (listener);
    button.addFocusListener (listener);
*/

  } // applyAquaButtonTreatment

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a button with text label using an action.  In some look and feels, this returns
   * a slightly modified button that is more consistent with the operating
   * system UI.
   *
   * @param action the action for the button.
   *
   * @return the button created using the action.
   */
  public static JButton getTextButton (
    Action action
  ) {
  
    JButton button = new JButton (action);
    if (IS_AQUA) applyAquaButtonTreatment (button);

    return (button);
    
  } // getTextButton

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a button with text label.  In some look and feels, this returns
   * a slightly modified button that is more consistent with the operating
   * system UI.
   *
   * @param text the text label.
   *
   * @return the button with text label.
   */
  public static JButton getTextButton (
    String text
  ) {
  
    JButton button = new JButton (text);
    if (IS_AQUA) applyAquaButtonTreatment (button);

    return (button);
    
  } // getTextButton

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a button with icon.
   *
   * @param purpose the icon purpose.
   *
   * @return the button with image icon appropriate for the specified
   * purpose.
   *
   * @throws IllegalArgumentException if the icon purpose has no known
   * icon, or the icon resource cannot be found.
   */
  public static JButton getIconButton (
    String purpose
  ) {

    JButton button = new JButton (getIcon (purpose));
    return (button);
  
  } // getIconButton

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a toggle button with icon.
   *
   * @param purpose the icon purpose.
   *
   * @return the toggle button with image icon appropriate for the
   * specified purpose.
   *
   * @throws IllegalArgumentException if the icon purpose has no known
   * icon, or the icon resource cannot be found.
   */
  public static JToggleButton getIconToggle (
    String purpose
  ) {

    JToggleButton button = new JToggleButton (getIcon (purpose));
    return (button);
  
  } // getIconToggle

  ////////////////////////////////////////////////////////////

  /**
   * Sets a component's preferred, min, and max size to be square.
   * The current minimum preferred dimension is used for both
   * dimensions.
   *
   * @param comp the component to modify.
   */
  public static void setSquare (
    JComponent comp
  ) {

    Dimension dim = comp.getPreferredSize();
    if (dim.width < dim.height) dim.height = dim.width;
    else dim.width = dim.height;
    comp.setPreferredSize (dim);
    comp.setMinimumSize (dim);
    comp.setMaximumSize (dim);

  } // setSquare

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the enabled flag on all components in a container.  If the
   * container contains othe containers, they are recursively set.
   * 
   * @param container the container to modify.
   * @param isEnabled the enabled flag, true to enable or false to
   * disable.
   */
  public static void setContainerEnabled (
    Container container, 
    boolean isEnabled
  ) {

    Component componentArray[] = container.getComponents();
    for (int i = 0; i < componentArray.length; i++) {
      if (componentArray[i] instanceof JScrollPane) {
        JScrollPane scrollPane = (JScrollPane) componentArray[i];
        scrollPane.setEnabled (isEnabled);
        scrollPane.getHorizontalScrollBar().setEnabled (isEnabled);
        scrollPane.getVerticalScrollBar().setEnabled (isEnabled);
        scrollPane.getViewport().setEnabled (isEnabled);
        scrollPane.getViewport().getView().setEnabled (isEnabled);
      } // if
      else if (componentArray[i] instanceof JComboBox ||
        componentArray[i] instanceof JSpinner) {
        componentArray[i].setEnabled (isEnabled);
      } // else if
      else if (componentArray[i] instanceof Container && 
        ((Container) componentArray[i]).getComponentCount() != 0) {
        setContainerEnabled ((Container) componentArray[i], isEnabled);
      } // if
      else {
        componentArray[i].setEnabled (isEnabled);
      } // else
    } // for

  } // setContainerEnabled

  ////////////////////////////////////////////////////////////

  /** 
   * Invokes the runnable using the
   * <code>SwingUtilities.invokeAndWait()</code> method but discards
   * any exceptions thrown.
   *
   * @param doRun the object to invoke.
   */
  public static void invokeAndWait (
    Runnable doRun
  ) {

    try { SwingUtilities.invokeAndWait (doRun); }
    catch (Exception e) { }

  } // invokeAndWait

  ////////////////////////////////////////////////////////////

  /**
   * Shows the specified frame in the AWT event dispatching thread.
   *
   * @param frame the frame to show.
   *
   * @since 3.4.1
   */
  public static void showFrame (
    Frame frame
  ) {

    SwingUtilities.invokeLater (new Runnable () {
      public void run () {
        frame.setVisible (true);
      } // run
    });

  } // showFrame

  ////////////////////////////////////////////////////////////

  /**
   * Creates a dialog with similar layout to dialogs produced by
   * <code>JOptionPane</code>.  The dialog has a main message area,
   * and a set of buttons.
   *
   * @param parent the dialog parent component.
   * @param title the dialog title string.
   * @param modal true if the dialog should be modal (ie: blocking input in all
   * other windows), false if not.
   * @param component the main dialog component.
   * @param controls the list of custom controls to be placed to the left
   * of the action buttons, or null for no custom controls.
   * @param actions the list of actions for dialog buttons.  The first
   * action is taken to be the default for the dialog.
   * @param hideAction the list of hide flags, true if the
   * corresponding action should cause the dialog to be hidden, or
   * null for all actions to hide the dialog.  
   * @param doDispose the dispose flag, true to dispose rather than
   * hide when a button is clicked.
   *
   * @return the dialog created.
   */
  public static JDialog createDialog (
    Component parent, 
    String title, 
    boolean modal,
    Component component,
    Component[] controls,
    Action[] actions,
    boolean[] hideAction,
    final boolean doDispose
  ) {

    // Initialize
    // ----------
    Window parentWindow = (parent instanceof Window ? (Window)parent : JOptionPane.getFrameForComponent (parent));
    var modality = (modal ?  Dialog.DEFAULT_MODALITY_TYPE :  Dialog.ModalityType.MODELESS);
    final JDialog dialog = new JDialog (parentWindow, title, modality);
    Container contentPane = dialog.getContentPane();

    // Create dialog panel
    // -------------------
    JPanel dialogPanel = new JPanel (new BorderLayout());
    dialogPanel.setBorder ((Border) UIManager.get ("OptionPane.border"));
    contentPane.add (dialogPanel, BorderLayout.CENTER);
    
    // Add message area
    // ----------------
    JPanel messageArea = new JPanel (new BorderLayout());
    messageArea.setBorder (
      (Border) UIManager.get ("OptionPane.messageAreaBorder"));
    dialogPanel.add (messageArea, BorderLayout.CENTER);
    messageArea.add (component, BorderLayout.CENTER);

    // Create button box
    // -----------------
    Box buttonBox = Box.createHorizontalBox();
    buttonBox.setBorder (
      (Border) UIManager.get ("OptionPane.buttonAreaBorder"));
    dialogPanel.add (buttonBox, BorderLayout.SOUTH);

    // Add custom controls or spacer
    // -----------------------------
    if (controls != null) {
      for (int i = 0; i < controls.length; i++) {
        buttonBox.add (controls[i]);
        buttonBox.add (Box.createRigidArea (new Dimension (5, 0)));
      } // for
    } // if
    else {
      buttonBox.add (Box.createHorizontalGlue());
    } // else

    // Create buttons
    // --------------
    ActionListener hideListener = new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          if (doDispose) dialog.dispose();
          else dialog.setVisible (false);
        } // actionPerformed
      };
    List buttonList = new ArrayList();

    boolean reverseOrder = IS_AQUA || IS_GTK;
    for (int i = 0; i < actions.length; i++) {
      int j = (reverseOrder ? actions.length-1-i : i);
      JButton button = new JButton (actions[j]);
      if (j == 0) {
        dialog.getRootPane().setDefaultButton (button);



        // This is the latest attempt to modify a dialog button text color 
        // under the Aqua look and feel on MacOS to be more natural.
        if (IS_AQUA) {

          button.getModel().addChangeListener (event -> {
            var model = button.getModel();
            if (model.isEnabled()) {
              if (!model.isPressed() || !model.isArmed()) button.setForeground (Color.WHITE);
              else button.setForeground (UIManager.getColor ("Button.foreground"));
            } // if
          });

          dialog.addWindowFocusListener (new java.awt.event.WindowFocusListener() {

            public void windowLostFocus (java.awt.event.WindowEvent event) {
              button.setForeground (UIManager.getColor ("Button.foreground"));
            } // windowLostFocus

            public void windowGainedFocus (java.awt.event.WindowEvent e) {
              var model = button.getModel();
              if (model.isEnabled()) {
                if (!model.isPressed()) button.setForeground (Color.WHITE);
                else button.setForeground (UIManager.getColor ("Button.foreground"));
              } // if            
            } // windowGainedFocus

          });

          // button.getModel().addChangeListener (event -> {
          //   var text = "";
          //   var model = button.getModel();
          //   if (model.isArmed()) text += " ARMED";
          //   if (model.isEnabled()) text += " ENABLED";
          //   if (model.isPressed()) text += " PRESSED";
          //   if (model.isRollover()) text += " ROLLOVER";
          //   if (model.isSelected()) text += " SELECTED";
          //   text = text.trim();
          //   LOGGER.fine ("Default button state changed to '" + text + "'");
          // });

        } // if




      } // if
      else if (IS_AQUA) applyAquaButtonTreatment (button);
      if (hideAction == null || hideAction[j])
        button.addActionListener (hideListener);
      buttonBox.add (button);
      if (i < actions.length-1)
        buttonBox.add (Box.createRigidArea (new Dimension (5, 0)));
      buttonList.add (button);
    } // for
    setSameSize (buttonList);

    // Pack and position
    // -----------------
    dialog.pack();
    dialog.setLocationRelativeTo (parent);
    if (doDispose) dialog.setDefaultCloseOperation (JDialog.DISPOSE_ON_CLOSE);
    
    return (dialog);

  } // createDialog

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a line-wrapped version of a string.
   *
   * @param input the string to line-wrap.
   * @param columns the number of columns to wrap at.
   * 
   * @return the line-wrapped version of the string.
   */
  public static String lineWrap (
    String input,
    int columns
  ) {

    // Check for null input
    // --------------------
    if (input == null) return (null);

    // Create line iterator
    // --------------------
    BreakIterator iter = BreakIterator.getLineInstance();
    iter.setText (input);

    // Create string buffers
    // ---------------------
    StringBuffer buffer = new StringBuffer();
    StringBuffer line = new StringBuffer();

    // Iterate over each word
    // ----------------------
    int start = iter.first();
    int end = iter.next();
    while (end != BreakIterator.DONE) {

      // Append the line to the buffer if long enough
      // --------------------------------------------
      int length = line.length();
      if (length != 0 && (length + (end-start)) > columns) {
        buffer.append (line);
        buffer.append ("\n");
        line.delete (0, length);
      } // if

      // Append the word to the current line
      // -----------------------------------
      line.append (input.substring (start, end));
      start = end;
      end = iter.next();

    } // while
    if (line.length() != 0) buffer.append (line);

    return (buffer.toString());
    
  } // lineWrap

  ////////////////////////////////////////////////////////////

  /**
   * Truncates a string to a maximum length by embedding an ellipsis in the
   * middle.  This is useful for long file names for example.
   *
   * @param input the input string to truncate.
   * @param maxLength the maximum length of the output string.
   *
   * @return the input string, truncated to the specified max length and 
   * ellipsis inserted if needed.  If the input is less than or equal to 
   * the maximum length, no truncation is done an the input string is
   * returned as-is.
   */
  public static String ellipsisString (
    String input,
    int maxLength
  ) {
  
    int length = input.length();
    if (length > maxLength) {
      String head = input.substring (0, maxLength/2-2);
      String tail = input.substring (length - maxLength/2-1, length);
      input = head + "..." + tail;
    } // if

    return (input);

  } // ellipsisString

  ////////////////////////////////////////////////////////////

  /**
   * Creates a dialog that displays standard error messages to the
   * user in a graphical window.  The dialog uses a {@link
   * PanelOutputStream} object to redirect standard error messages to
   * a scrolling panel.  The {@link java.lang.System} standard error
   * object is replaced with a new one that outputs to the dialog, so
   * this method should only be called once in the lifecycle of a GUI
   * program.  The dialog is set up so that if any errors are caught,
   * the dialog becomes visible and shows the error, and then allows
   * the user to close the program.
   *
   * @param parent the dialog parent component.
   * @param title the dialog title string.
   * @param userText the instructions to give to the user
   * concerning the error message, or null for none.
   *
   * @return the new standard error dialog.
   *
   * @throws IllegalStateException if the error dialog has already
   * been created.
   */
  public static JDialog createErrorDialog (
    final Component parent, 
    String title,
    String userText
  ) {

    // Check if we are already created
    // -------------------------------
    if (errorDialog != null)
      throw new IllegalStateException ("Error dialog already created");

    // Create and set error stream
    // ---------------------------
    panelStream = new PanelOutputStream();
    System.setErr (new PrintStream (panelStream, true));

    // Create message box
    // ------------------
    Box messageBox = new Box (BoxLayout.Y_AXIS);
    String messageText = 
      "The application may be in an unsable state.  You may choose to " +
      "end the application now, or ignore the message and continue.";
    String[] textArray = 
      ((userText == null ? "" : lineWrap (userText, 80) + "\n \n") +
      (lineWrap (messageText, 80))).split ("\n");
    for (int i = 0; i < textArray.length; i++)
      messageBox.add (new JLabel (textArray[i]));

    // Create dialog
    // -------------
    JPanel messagePanel = panelStream.getPanel();
    JPanel dialogPanel = new JPanel (new BorderLayout (5, 5));
    dialogPanel.add (new JLabel ("An unexpected warning or error " +
      "message has been generated:"), BorderLayout.PAGE_START);
    dialogPanel.add (messagePanel, BorderLayout.CENTER);
    dialogPanel.add (messageBox, BorderLayout.PAGE_END);
    Action endNowAction = GUIServices.createAction ("End Now", 
      new Runnable () {
        public void run () { System.exit (1); }
      });
    Action continueAction = GUIServices.createAction ("Continue",
      new Runnable () {
        public void run () { 
          errorShowing = false;
          panelStream.getTextArea().setText (null);
        } // run
      });
    errorDialog = createDialog (parent, title, true, dialogPanel, 
      null, new Action[] {endNowAction, continueAction}, null, false);
    errorDialog.setDefaultCloseOperation (JDialog.DO_NOTHING_ON_CLOSE);
    errorShowing = false;

    // Make dialog grab focus if lost
    // ------------------------------
    errorDialog.addWindowFocusListener (new WindowAdapter() {
        public void windowLostFocus (WindowEvent e) { 
          if (errorShowing) {
            errorDialog.setVisible (false);
            errorDialog.setVisible (true);
          } // if
        }
      });

    // Setup text area font and size
    // -----------------------------
    JTextArea textArea = panelStream.getTextArea();
    Font font = textArea.getFont();
    Font monoFont = new Font ("Monospaced", font.getStyle(), font.getSize());
    textArea.setFont (monoFont);
    textArea.setRows (15);
    textArea.setColumns (80);
    textArea.setLineWrap (true);
    errorDialog.pack();

    // Add document listener to show dialog
    // ------------------------------------
    DocumentListener listener = new DocumentListener () {
        public void insertUpdate (DocumentEvent e) { 
          if (!errorShowing) {
            errorShowing = true;
            SwingUtilities.invokeLater (new Runnable () {
                public void run () { 
                  errorDialog.setLocationRelativeTo (parent);
                  errorDialog.setVisible (true);
                }// run
              });
          } // if
        } // insertUpdate
        public void removeUpdate (DocumentEvent e) {}
        public void changedUpdate (DocumentEvent e) {}
      };
    textArea.getDocument().addDocumentListener (listener);

    return (errorDialog);

  } // createErrorDialog

  ////////////////////////////////////////////////////////////

  /** 
   * Creates an action with the specified name and listener.
   * 
   * @param name the action name.
   * @param runnable the runnable to invoke when the action is
   * performed, or null for no operation.
   *
   * @return the action created.
   */
  public static Action createAction (
    String name,
    Runnable runnable
  ) { 

    final Runnable fRunnable = runnable;
    Action action = new AbstractAction (name) {
        public void actionPerformed (ActionEvent event) {
          if (fRunnable != null) fRunnable.run();
        } // actionPerformed
      };
    return (action);

  } // createAction

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a modal message dialog with no user input options.  The
   * dialog may not be closed by the user.
   *
   * @param parent the parent component.
   * @param title the dialog title.
   * @param message the message to display.
   *
   * @return the dialog created.
   */
  public static JDialog createMessageDialog (
    Component parent,
    String title,
    String message
  ) {
    
    // Create panel
    // ------------
    JPanel messagePanel = new JPanel();
    messagePanel.setBorder ((Border) UIManager.get ("OptionPane.border"));

    // Add label
    // ---------
    JLabel label = new JLabel (message,
      (Icon) UIManager.get ("OptionPane.informationIcon"),
      SwingConstants.TRAILING);
    label.setIconTextGap (10);
    messagePanel.add (label);

    // Create dialog
    // -------------
    JDialog messageDialog = new JDialog (
      JOptionPane.getFrameForComponent (parent), title, true);
    messageDialog.getContentPane().add (messagePanel);
    messageDialog.pack();
    messageDialog.setLocationRelativeTo (parent);
    messageDialog.setDefaultCloseOperation (JDialog.DO_NOTHING_ON_CLOSE);    
    
    return (messageDialog);

  } // createMessageDialog

  ////////////////////////////////////////////////////////////

  /** 
   * Gets an icon for the specified purpose.
   *
   * @param purpose the icon purpose.
   *
   * @return the image icon appropriate for the specified purpose.
   *
   * @throws IllegalArgumentException if the icon purpose has no known
   * icon, or the icon resource cannot be found.
   */
  public static ImageIcon getIcon (
    String purpose
  ) {

    // Get icon resource
    // -----------------
    String iconFile = iconProperties.getProperty (purpose);
    if (iconFile == null) {
      throw new IllegalArgumentException ("Cannot find icon for purpose " + 
        purpose);
    } // if
    URL resource = GUIServices.class.getResource (iconFile);
    if (resource == null) {
      throw new IllegalArgumentException ("Cannot find resource for icon " +
        iconFile);
    } // if

    // If we're not on Mac, try looking for another icon image with the @2x
    // naming convention.  If we find one, create an adaptive image for the
    // icon.  Otherwise icons on Windows and Linux look terrible on scaled
    // displays.
    ImageIcon icon = null;
    if (IS_MAC) {
      icon = new ImageIcon (resource);
    } // if
    else {
      var path = resource.toString();
      int index = path.lastIndexOf ('.');
      var base = path.substring (0, index);
      var ext = path.substring (index+1);
      var path2x = base + "@2x." + ext;
      try {
        var image2x = ImageIO.read (new URL (path2x));
        var image = ImageIO.read (resource);
        var adaptive = new AdaptiveImage (0, image, image2x);
        icon = new ImageIcon (adaptive);
      } // try
      catch (Exception e) {
        icon = new ImageIcon (resource);
      } // catch
    } // else

    return (icon);

  } // getIcon

  ////////////////////////////////////////////////////////////

  /**
   * The <code>AdaptiveImage</code> class handles the supply of icon images
   * of different resolutions for Windows and Linux when display scaling is
   * being used.  For example a base 24x24 icon image in Windows running at a
   * scaling of 125% needs a 30x30 image to be created.  This is done by
   * downscaling from the 48x48 image supplied for the Mac retina resolution.
   * This is only done in Windows and Linux because the Mac JVM already handles
   * display scaling by looking for the *@2x.png files.  Without this, upscaled
   * 24x24 icons look terrible on Windows and Linux displays.
   */
  private static class AdaptiveImage extends BaseMultiResolutionImage {
  
    private String previousDims = "";
    private Image previousVariant = null;
    
    public AdaptiveImage (int baseImageIndex, Image... resolutionVariants) {
      super (baseImageIndex, resolutionVariants);
    } // AdaptiveImage

    @Override
    public Image getResolutionVariant (double destImageWidth, double destImageHeight) {

      var variant = super.getResolutionVariant (destImageWidth, destImageHeight);
      if (variant.getWidth (null) != destImageWidth || variant.getHeight (null) != destImageHeight) {
        var dims = destImageWidth + "x" + destImageHeight;
        if (dims.equals (previousDims)) {
          variant = previousVariant;
        } // if
        else {
          var scaled = variant.getScaledInstance ((int) destImageWidth, (int) destImageHeight, Image.SCALE_AREA_AVERAGING);
          while (scaled.getWidth (null) == -1) {
            Thread.onSpinWait();
          } // while
          variant = scaled;
          previousDims = dims;
          previousVariant = variant;
        } // if
      } // if

      return (variant);
      
    } // getResolutionVariant
  
  } // AdaptiveImage class

  ////////////////////////////////////////////////////////////

  /**
   * Gets a help button for the specified class.
   *
   * @param helpClass the class to get a help button for.
   *
   * @return a help button, which when pressed will show help for
   * the specified class.
   *
   * @throws IllegalArgumentException if the class has no known
   * help resource, or the help resource cannot be found.
   */
  public static JButton getHelpButton (
    Class helpClass
  ) {

    // Find resource for help
    // ----------------------
    String className = helpClass.getName();
    String helpFile = helpProperties.getProperty (className);
    if (helpFile == null) {
      throw new IllegalArgumentException ("Cannot find help file for class " + className);
    } // if
//    final URL resource = helpClass.getResource (helpFile);
    URL resource = ClassLoader.getSystemResource (helpFile); 
    if (resource == null) {
      throw new IllegalArgumentException ("Cannot find resource for file " + helpFile);
    } // if

    // Create button
    // -------------
    JButton helpButton;
    if (IS_AQUA) {
      helpButton = new JButton();
      helpButton.putClientProperty ("JButton.buttonType", "help");
    } // if
    else { 
      helpButton = new JButton ("Help");
      helpButton.setIcon (getIcon ("menu.support"));
    } // else    
    helpButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {

          try {
            Desktop.getDesktop().browse (resource.toURI());
          } // try
          catch (Exception e) {
            throw new RuntimeException ("Error opening the help page: " + e.toString());
          } // catch

          // HTMLPanel helpPanel = new HTMLPanel (helpIndex, false);
          // helpPanel.setPreferredSize (ToolServices.HELP_DIALOG_SIZE);
          // helpPanel.setPage (resource);
          // Window window = SwingUtilities.getWindowAncestor ((Component) event.getSource());
          // helpPanel.showDialog (window, "Help");

        } // actionPerformed
      });

    return (helpButton);

  } // getHelpButton

  ////////////////////////////////////////////////////////////

  /**
   * Sets the active help index file for subsequent calls to
   * {@link #getHelpButton}.  HTML panels will go to the
   * specified index when the Home button is clicked.
   *
   * @param url the help index URL to use for the help system.
   */
  public static void setHelpIndex (
    URL url
  ) {

    helpIndex = url;

  } // setHelpIndex

  ////////////////////////////////////////////////////////////

  /** 
   * Gets a file chooser that tracks the user's selected directory.
   * Objects should use this method if they are going to display a
   * file chooser, so that the user always sees the same directory
   * that they were last in.
   *
   * @return a file chooser to use for opening or saving files.
   */
  public static JFileChooser getFileChooser () {

    // Create chooser and set directory
    // --------------------------------
    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setCurrentDirectory (userDir);

    // Add change listener for directory
    // ---------------------------------
    fileChooser.addPropertyChangeListener (
      JFileChooser.DIRECTORY_CHANGED_PROPERTY, new PropertyChangeListener () {
        public void propertyChange (PropertyChangeEvent event) {
          userDir = fileChooser.getCurrentDirectory();
        } // propertyChange
      });

    return (fileChooser);

  } // getFileChooser

  ////////////////////////////////////////////////////////////

  /**
   * Handles a startup event by invoking an action listener.
   *
   * @since 3.4.1
   */
  private static class StartupListener implements StartupNotification.Listener {

    private ActionListener action;
    
    public StartupListener (ActionListener action) { this.action = action; }
    
    @Override
    public void startupPerformed (String parameters) {

      if (parameters != null && !parameters.isEmpty()) {
        String[] paramArray = parameters.split ("\" \"");
        String file = paramArray[0].replaceAll ("\"", "");
        action.actionPerformed (new ActionEvent (this, 0, file));
      } // if
  
    } // startupPerformed
    
  } // StartupListener class

  ////////////////////////////////////////////////////////////

  /**
   * Adds a listener that executes when a file is double-clicked on
   * Mac or Windows.
   *
   * @param listener the listener to call.
   *
   * @since 3.4.1
   */
  public static void addOpenFileListener (
    ActionListener listener
  ) {

    StartupNotification.registerStartupListener (new StartupListener (listener));

  } // addOpenFileListener

  ////////////////////////////////////////////////////////////

  /**
   * Sets a list of <code>JComponent</code> objects to be the
   * same size as that of the maximum sized component.  This is
   * useful for a row of buttons which may have different length
   * labels and thus would be sized differently by a layout
   * manager, but would look better all one size.  Good for tool
   * bars and button boxes.
   *
   * @param componentList the list of components.
   */
  public static void setSameSize (
    List componentList
  ) {

    // Get maximum size
    // ----------------
    Dimension maxSize = new Dimension (0, 0);
    for (Iterator iter = componentList.iterator(); iter.hasNext();) {
      JComponent component = (JComponent) iter.next();
      Dimension size = component.getMinimumSize();
      if (size.width > maxSize.width) maxSize.width = size.width;
      if (size.height > maxSize.height) maxSize.height = size.height;
    } // for

    // Set preferred size
    // ------------------
    for (Iterator iter = componentList.iterator(); iter.hasNext();) {
      JComponent component = (JComponent) iter.next();
      component.setPreferredSize (maxSize);
    } // for

  } // setSameSize

  ////////////////////////////////////////////////////////////

  /**
   * Get the list of recently opened files for the target class.
   *
   * @param targetClass the class for the recent files.
   *
   * @return the list of recnetly opened file names, possibly empty.
   *
   * @since 3.4.0
   */
  public static List<String> getRecentlyOpenedFiles (
    Class targetClass
  ) {

    Preferences prefs = Preferences.userNodeForPackage (targetClass);
    String recentFiles = prefs.get (RECENT_FILES_KEY, null);
    List<String> recentFilesList;
    if (recentFiles == null)
      recentFilesList = new ArrayList<>();
    else
      recentFilesList = new ArrayList (Arrays.asList (recentFiles.split ("\n")));

    return (recentFilesList);

  } // getRecentlyOpenedFiles
  
  ////////////////////////////////////////////////////////////

  /**
   * Adds the specified file name to the list of recently opened files for the
   * target class.
   *
   * @param file the file name to add.
   * @param targetClass the class for the recent files.
   * @param maxFiles the maximum number of files to store.
   *
   * @since 3.4.0
   */
  public static void addFileToRecentlyOpened (
    String file,
    Class targetClass,
    int maxFiles
  ) {

    // Recall recent files
    // -------------------
    List<String> recentFilesList = getRecentlyOpenedFiles (targetClass);

    // Add file to list
    // ----------------
    int index = recentFilesList.indexOf (file);
    if (index == -1)
      recentFilesList.add (file);
    else {
      recentFilesList.remove (index);
      recentFilesList.add (file);
    } // else

    // Trim list length
    // ----------------
    if (recentFilesList.size() > maxFiles)
      recentFilesList = recentFilesList.subList (recentFilesList.size() - maxFiles, recentFilesList.size());

    setRecentlyOpenedFiles (recentFilesList, targetClass);

  } // addFileToRecentlyOpened
  
  ////////////////////////////////////////////////////////////

  /**
   * Sets the list of recently opened files for the target class.
   *
   * @param recentFilesList the new list of recently opened files, possibly
   * empty.
   * @param targetClass the class for the recent files.
   *
   * @since 3.4.0
   */
  public static void setRecentlyOpenedFiles (
    List<String> recentFilesList,
    Class targetClass
  ) {

    Preferences prefs = Preferences.userNodeForPackage (targetClass);

    // Remove recent files list
    // ------------------------
    if (recentFilesList.size() == 0) {
      prefs.remove (RECENT_FILES_KEY);
    } //
    
    // Update recent files list
    // ------------------------
    else {
      String recentFiles = "";
      for (int i = 0; i < recentFilesList.size(); i++) {
        recentFiles += recentFilesList.get (i);
        if (i != recentFilesList.size()-1)
          recentFiles += "\n";
      } // for
      prefs.put (RECENT_FILES_KEY, recentFiles);
    } // else
    
  } // setRecentlyOpenedFiles

  ////////////////////////////////////////////////////////////

  /**
   * Stores the specified window size for the target class.  Storing a window
   * size is useful for maintaining a window size across application 
   * invocations.
   *
   * @param windowSize the window size to store.
   * @param targetClass the class to associate with the window size.
   *
   * @see #recallWindowSizeForClass
   * @since 3.3.1
   */
  public static void storeWindowSizeForClass (
    Dimension windowSize,
    Class targetClass
  ) {

    Preferences prefs = Preferences.userNodeForPackage (targetClass);
    prefs.putInt (WINDOW_WIDTH_KEY, windowSize.width);
    prefs.putInt (WINDOW_HEIGHT_KEY, windowSize.height);
    
  } // storeWindowSizeForClass

  ////////////////////////////////////////////////////////////
  
  /**
   * Recalls the specified window size for the target class.  Recalling a window
   * size is useful for maintaining a window size across application 
   * invokations.
   *
   * @param targetClass the class to recall the window size.
   *
   * @return the windows size or null if one was not found for the target class.
   *
   * @see #storeWindowSizeForClass
   * @since 3.3.1
   */
  public static Dimension recallWindowSizeForClass (
    Class targetClass
  ) {

    Preferences prefs = Preferences.userNodeForPackage (targetClass);
    int width = prefs.getInt (WINDOW_WIDTH_KEY, -1);
    int height = prefs.getInt (WINDOW_HEIGHT_KEY, -1);
    Dimension windowSize = null;
    if (width != -1 && height != -1)
      windowSize = new Dimension (width, height);

    return (windowSize);
    
  } // recallWindowSizeForClass

  ////////////////////////////////////////////////////////////

  /**
   * Stores a boolean value for the target class.
   *
   * @param value the value to store.
   * @param key the key to use for storing the boolen value.
   * @param targetClass the class to associate with the value.
   *
   * @since 3.4.0
   */
  public static void storeBooleanSettingForClass (
    boolean value,
    String key,
    Class targetClass
  ) {

    Preferences prefs = Preferences.userNodeForPackage (targetClass);
    prefs.putBoolean (key, value);

  } // storeBooleanSettingForClass
  
  ////////////////////////////////////////////////////////////

  /**
   * Recalls a boolean value for the target class.
   *
   * @param def the default value for the boolean if not found.
   * @param key the key to use for recalling the boolean value.
   * @param targetClass the class to recall the boolean value.
   *
   * @return the boolean value, or the default value if none was found.
   *
   * @since 3.4.0
   */
  public static boolean recallBooleanSettingForClass (
    boolean def,
    String key,
    Class targetClass
  ) {

    Preferences prefs = Preferences.userNodeForPackage (targetClass);
    return (prefs.getBoolean (key, def));

  } // recallBooleanSettingForClass

  ////////////////////////////////////////////////////////////

  /**
   * Stores a string value for the target class.
   *
   * @param value the value to store.
   * @param key the key to use for storing the string value, may not be null.
   * @param targetClass the class to associate with the value.
   *
   * @since 3.6.0
   */
  public static void storeStringSettingForClass (
    String value,
    String key,
    Class targetClass
  ) {

    Preferences prefs = Preferences.userNodeForPackage (targetClass);
    prefs.put (key, value);

  } // storeStringSettingForClass
  
  ////////////////////////////////////////////////////////////

  /**
   * Recalls a string value for the target class.
   *
   * @param def the default value for the string if not found.
   * @param key the key to use for recalling the string value.
   * @param targetClass the class to recall the string value.
   *
   * @return the string value, or the default value if none was found.
   *
   * @since 3.6.0
   */
  public static String recallStringSettingForClass (
    String def,
    String key,
    Class targetClass
  ) {

    Preferences prefs = Preferences.userNodeForPackage (targetClass);
    return (prefs.get (key, def));

  } // recallStringSettingForClass

  ////////////////////////////////////////////////////////////

  /**
   * Gets the width of a label.
   * 
   * @param len the length of text in the label.
   * 
   * @return the width of the label in pixels.
   * 
   * @since 3.7.1
   */
  public static int getLabelWidth (int len) {

    var buf = new StringBuffer();
    for (int i = 0; i < len; i++) buf.append ((char) ('a' + (i%26)));
    return ((int) new JLabel (buf.toString()).getPreferredSize().getWidth());

  } // getLabelWidth

  ////////////////////////////////////////////////////////////

  /**
   * Gets the height of a label.
   * 
   * @return the height of the label in pixels.
   * 
   * @since 3.7.1
   */
  public static int getLabelHeight () {

    return ((int) new JLabel ("abcdef").getPreferredSize().getHeight());

  } // getLabelHeight

  ////////////////////////////////////////////////////////////

  // These next two methods get direct access to the native OSX fullscreen 
  // mode.  We added these due to issues with entering full screen mode on
  // MacOS under Java 17 on Macbook Pro 16" 2019 hardware which just seems to
  // show a black screen when entering full screen mode usinfg the default 
  // Java desktop methods that work properly on Linux and Windows.  For these
  // methods, see https://stackoverflow.com/questions/11570356/jframe-in-full-screen-java

  // Note that if these methods are actually used, the following option must
  // be added to the VM options: --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED
  
  private static void enableOSXFullscreen (Window window) {

    try {
      Class util = Class.forName ("com.apple.eawt.FullScreenUtilities");
      Class params[] = new Class[] {Window.class, Boolean.TYPE};
      Method method = util.getMethod ("setWindowCanFullScreen", params);
      method.invoke (util, window, true);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
      throw new RuntimeException (e);
    } // catch

  } // enableOSXFullscreen

  public static void toggleOSXFullscreen (Window window) {

    try {
      Class appClass = Class.forName ("com.apple.eawt.Application");
      Class params[] = new Class[]{};
      Method getApplication = appClass.getMethod ("getApplication", params);
      Object application = getApplication.invoke (appClass);
      Method requestToggleFulLScreen = application.getClass().getMethod ("requestToggleFullScreen", Window.class);
      requestToggleFulLScreen.invoke (application, window);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
      throw new RuntimeException (e);
    } // catch

  } // toggleOSXFullscreen

  ////////////////////////////////////////////////////////////

  /**
   * Determines if a window is currently the full screen window.
   * 
   * @param window the window to check for full screen.
   * 
   * @return true if the windoew is full screen or false if not.
   * 
   * @since 3.8.1
   */
  public static boolean isFullScreen (Window window) {

    boolean full = false;

    if (IS_MAC && fullScreenMacWindow != null) {
      full = (window == fullScreenMacWindow);
    } // if
    else {
      var device = window.getGraphicsConfiguration().getDevice();
      Window fullScreenWindow = device.getFullScreenWindow();
      full = (fullScreenWindow != null && window == fullScreenWindow);
    } // else

    return (full);

  } // isFullScreen

  ////////////////////////////////////////////////////////////

  /**
   * Enters full screen mode for a window.  The window must not already
   * be in full screen mode.
   * 
   * @param window the window to enter full screen.
   * 
   * @see #isFullScreen
   * 
   * @since 3.8.1
   */
  public static void enterFullScreen (Window window) {

    if (IS_MAC) {
      if (fullScreenMacWindow != null) 
        throw new IllegalStateException ("Already in full screen mode");
      enableOSXFullscreen (window);
      toggleOSXFullscreen (window);
      fullScreenMacWindow = window;
    } // if
    else {
      var device = window.getGraphicsConfiguration().getDevice();
      if (device.getFullScreenWindow() != null) 
        throw new IllegalStateException ("Already in full screen mode");
      device.setFullScreenWindow (window);
    } // else

  } // enterFullScreen

  ////////////////////////////////////////////////////////////

  /**
   * Exits full screen mode for a window.  The window must be in full 
   * screen mode.
   * 
   * @param window the window to exit full screen.
   * 
   * @see #isFullScreen
   * 
   * @since 3.8.1
   */
  public static void exitFullScreen (Window window) {

    if (IS_MAC) {
      if (window != fullScreenMacWindow) 
        throw new IllegalStateException ("Specified window is not in full screen mode");
      enableOSXFullscreen (window);
      toggleOSXFullscreen (window);
      fullScreenMacWindow = null;
    } // if
    else {
      var device = window.getGraphicsConfiguration().getDevice();
      var fullScreenWindow = device.getFullScreenWindow();
      if (window != fullScreenWindow) 
        throw new IllegalStateException ("Specified window is not in full screen mode");
      device.setFullScreenWindow (null);
    } // exitFullScreen

  } // enterFullScreen

  ////////////////////////////////////////////////////////////

  private GUIServices () { }

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   *
   * @throws Exception if an error occurred.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (GUIServices.class);

    logger.test ("setRecentlyOpenedFiles, getRecentlyOpenedFiles");

    List<String> files = new ArrayList<>();
    setRecentlyOpenedFiles (files, GUIServices.class);
    files = getRecentlyOpenedFiles (GUIServices.class);
    assert (files.size() == 0);

    files = Arrays.asList ("one", "two", "three");
    setRecentlyOpenedFiles (files, GUIServices.class);
    files = getRecentlyOpenedFiles (GUIServices.class);
    assert (files.size() == 3);
    assert (files.get (0).equals ("one"));
    assert (files.get (1).equals ("two"));
    assert (files.get (2).equals ("three"));
    
    files = new ArrayList<>();
    setRecentlyOpenedFiles (files, GUIServices.class);
    files = getRecentlyOpenedFiles (GUIServices.class);
    assert (files.size() == 0);

    logger.passed();

    logger.test ("addFileToRecentlyOpened");
    
    for (int i = 0; i < 5; i++) {
      String file = "file" + i;
      addFileToRecentlyOpened (file, GUIServices.class, 5);
      files = getRecentlyOpenedFiles (GUIServices.class);
      assert (files.size() == (i+1));
      assert (files.indexOf (file) == files.size()-1);
    } // for

    addFileToRecentlyOpened ("file3", GUIServices.class, 5);
    files = getRecentlyOpenedFiles (GUIServices.class);
    assert (files.size() == 5);
    assert (files.indexOf ("file0") == 0);
    assert (files.indexOf ("file1") == 1);
    assert (files.indexOf ("file2") == 2);
    assert (files.indexOf ("file4") == 3);
    assert (files.indexOf ("file3") == 4);

    addFileToRecentlyOpened ("file10", GUIServices.class, 5);
    files = getRecentlyOpenedFiles (GUIServices.class);
    assert (files.size() == 5);
    assert (files.indexOf ("file10") == 4);
    assert (files.indexOf ("file0") == -1);

    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // GUIServices class

////////////////////////////////////////////////////////////////////////
