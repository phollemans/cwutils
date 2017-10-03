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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.lang.reflect.Method;
import java.net.URL;
import java.text.BreakIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

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
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import noaa.coastwatch.gui.HTMLPanel;
import noaa.coastwatch.gui.PanelOutputStream;
import noaa.coastwatch.tools.ToolServices;

/**
 * The GUI services class defines various static methods relating
 * to graphical user interfaces.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class GUIServices {

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

  /** The Aqua boolean, true if we are running on a Mac with Aqua look. */
  public static final boolean IS_AQUA = 
    UIManager.getLookAndFeel().getID().equals ("Aqua");

  /** The GTK boolean, true if we are running the GTK look. */
  public static final boolean IS_GTK = 
    UIManager.getLookAndFeel().getID().equals ("GTK");

  /** The insets to use for icon-only button margins. */
  public static final Insets ICON_INSETS = new Insets (2, 2, 2, 2);
  
  /** The window size keys for storing/recalling window sizes. */
  public static final String WINDOW_WIDTH_KEY = "window.width";
  public static final String WINDOW_HEIGHT_KEY = "window.height";

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

  /** The hlpe index URL to use for help panels. */
  private static URL helpIndex;
  
  /** The local clipboard. */
  private static Clipboard clipboard;

  ////////////////////////////////////////////////////////////

  /** Loads the icon properties file. */
  static {

    // Get static properties
    // ---------------------
    iconProperties = getPropertiesFromResource (ICON_PROPERTIES_FILE);
    helpProperties = getPropertiesFromResource (HELP_PROPERTIES_FILE);
    clipboard = new Clipboard ("CDAT Clipboard");

    // Get current directory
    // ---------------------
    userDir = getPlatformDefaultDirectory();

  } // static

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
   * Creates a dialog with similar layout to dialogs produced by
   * <code>JOptionPane</code>.  The dialog has a main message area,
   * and a set of buttons.
   *
   * @param parent the dialog parent component.
   * @param title the dialog title string.
   * @param modal true if the dialog should be model, false if not.
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
    final JDialog dialog = new JDialog (
      JOptionPane.getFrameForComponent (parent), title, modal);
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


 // TODO: This doesn't work well, see note in applyAquaButtonTreatment
 //         if (IS_AQUA) button.setForeground (Color.WHITE);


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
    
    return (new ImageIcon (resource));

  } // getIcon

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
      throw new IllegalArgumentException ("Cannot find help file for class " + 
        className);
    } // if
    final URL resource = helpClass.getResource (helpFile);
    if (resource == null) {
      throw new IllegalArgumentException ("Cannot find resource for file " +
        helpFile);
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
          HTMLPanel helpPanel = new HTMLPanel (helpIndex, false);
          helpPanel.setPreferredSize (ToolServices.HELP_DIALOG_SIZE);
          helpPanel.setPage (resource);
          Window window = SwingUtilities.getWindowAncestor ((Component) 
            event.getSource());
          helpPanel.showDialog (window, "Help");
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
   * Adds a listener that executes when a file is opened on a Mac via
   * the Finder (for example by double-clicking).  This requires a
   * call to the <code>MacGUIServices</code> class via reflection so
   * that it will run correctly no matter the platform.
   *
   * @param listener the listener to call.
   *
   * @throws RuntimeException if called on a non-Mac platform.
   *
   * @see noaa.coastwatch.gui.MacGUIServices
   */
  public static void addMacOpenFileListener (
    ActionListener listener
  ) {

    try {

      // Get the add method
      // ------------------
      Class services = ClassLoader.getSystemClassLoader().loadClass (
        "noaa.coastwatch.gui.MacGUIServices");
      Class[] classes = {ActionListener.class};
      Method addMethod = services.getDeclaredMethod ("addOpenFileListener", 
        classes);

      // Add the listener
      // ----------------
      if (addMethod != null) {
        Object[] args = {listener};
        addMethod.invoke (services, args);
      } // if
      
    } catch (NoClassDefFoundError e1) {
      throw new RuntimeException ("Not running on a Mac platform");
    } catch (Exception e2) {
      throw new RuntimeException (e2.getMessage());
    } // catch

  } // addMacOpenFileListener

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
   * Gets the local clipboard.
   *
   * @return the clipboard.
   */
  public static Clipboard getCDATClipboard() {

    return (clipboard);

  } // getCDATClipBoard

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
   * @param targetClass the class to associate with the window size.
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

  private GUIServices () { }

  ////////////////////////////////////////////////////////////

} // GUIServices class

////////////////////////////////////////////////////////////////////////
