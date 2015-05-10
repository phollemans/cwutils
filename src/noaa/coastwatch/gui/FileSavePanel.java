////////////////////////////////////////////////////////////////////////
/*
     FILE: FileSavePanel.java
  PURPOSE: Selects a file for saving.
   AUTHOR: Peter Hollemans
     DATE: 2006/12/19
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui;

// Imports
// -------
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import noaa.coastwatch.gui.GUIServices;

/** 
 * The <code>FileSavePanel</code> displays a file name for saving and
 * allows the user to choose a new file name.  The functionality is
 * similar to a file chooser, but the panel is much smaller and is
 * designed to be combined in a panel with other controls, such as
 * save options and formats.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class FileSavePanel 
  extends JPanel {

  // Constants
  // ---------

  /** The standard width of file name and paths. */
  private static final int NAME_WIDTH = 20;

  // Variables
  // ---------

  /** The text field for the save file name. */
  private JTextField saveField;

  /** The file chooser instance for this panel. */
  private JFileChooser fileChooser;

  /** The label for directory name. */
  private JLabel whereLabel;

  /** The file for saving. */
  private File saveFile;

  ////////////////////////////////////////////////////////////

  /** Shows the file selection dialog. */
  private void showFileSelection () {

    // Set active file
    // ---------------
    fileChooser.setSelectedFile (getSaveFile());

    // Get user selected file
    // ----------------------
    int returnVal = fileChooser.showDialog (this, null);
    if (returnVal == JFileChooser.APPROVE_OPTION)
      setSaveFile (fileChooser.getSelectedFile());

  } // showFileSelection

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the current save file.
   *
   * @return the currently displayed save file.
   */
  public File getSaveFile () {

    return (new File (saveFile.getParentFile(), saveField.getText()));

  } // getSaveFile

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the save file. 
   * 
   * @param file the new file to set as the save file.
   */
  public void setSaveFile (
    File file
  ) {
  
    // Store file
    // ----------
    saveFile = file;

    // Set name field
    // --------------
    saveField.setText (file.getName());
    if (saveField.isVisible())
      saveField.setCaretPosition (saveField.getText().length());

    // Set directory label
    // -------------------
    String where = file.getParent();
    if (where == null) where = "";
    if (where.length() > NAME_WIDTH) {
      where = "..." + where.substring (where.length() - NAME_WIDTH, 
        where.length());
    } // if    
    whereLabel.setText (where);

  } // setSaveFile

  ////////////////////////////////////////////////////////////

  /** Creates a new save panel with default file chooser. */
  public FileSavePanel () { this (null); }

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new save panel.
   *
   * @param fileChooser the file chooser to use when the user wants to
   * select a file, or null to create a default one.
   */
  public FileSavePanel (
    JFileChooser fileChooser
  ) {

    // Set file chooser
    // ----------------
    if (fileChooser == null) {
      fileChooser = new JFileChooser();
      fileChooser.setDialogTitle ("Select");
      fileChooser.setDialogType (JFileChooser.SAVE_DIALOG);
      fileChooser.setApproveButtonText ("OK");
    } // if
    this.fileChooser = fileChooser;

    // Create panel
    // ------------
    setLayout (new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.anchor = GridBagConstraints.WEST;
    gc.insets = new Insets (2, 0, 2, 5);

    GUIServices.setConstraints (gc, 0, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    this.add (new JLabel ("Save as:"), gc);

    saveField = new JTextField (NAME_WIDTH);
    GUIServices.setConstraints (gc, 1, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    this.add (saveField, gc);

    JButton selectButton = new JButton ("Select...");
    selectButton.addActionListener (new ActionListener () {
        public void actionPerformed (ActionEvent event) {
          showFileSelection();
        } // actionPerformed
      });
    GUIServices.setConstraints (gc, 2, 0, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    this.add (selectButton, gc);

    GUIServices.setConstraints (gc, 0, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 0, 0);
    this.add (new JLabel ("Where:"), gc);
    
    whereLabel = new JLabel();
    GUIServices.setConstraints (gc, 1, 1, 1, 1, 
      GridBagConstraints.HORIZONTAL, 1, 0);
    this.add (whereLabel, gc);

    // Set initial file
    // ----------------
    setSaveFile (new File (System.getProperty ("user.dir"), "Untitled.txt"));

  } // FileSavePanel constructor

  ////////////////////////////////////////////////////////////

} // FileSavePanel

////////////////////////////////////////////////////////////////////////
