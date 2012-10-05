////////////////////////////////////////////////////////////////////////
/*
     FILE: THREDDSFileChooser.java
  PURPOSE: Allows the user to choose files from a THREDDS server.
   AUTHOR: X. Liu
     DATE: 2011/12/08
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2011, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.open;

// Imports
// -------
import java.beans.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import java.util.List;
import noaa.coastwatch.gui.*;

import java.net.URL;
import java.net.URLConnection;
import javax.swing.tree.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;
import org.xml.sax.*;
import noaa.coastwatch.util.*;

/** 
 * The <code>THREDDSFileChooser</code> class allows the user to choose
 * a file from a THREDDS catalog. 
 */
public class THREDDSFileChooser
  extends JPanel {

  // Constants
  // ---------

  /** The file selection property. */
  public static final String FILE_PROPERTY = "file";

  // Variables
  // ---------

  /** The server chooser used for selecting the network server. */
  private JComboBox serverBox;

  /** The file chooser used for selecting directories and files. */
  //private FileChooser fileChooser;

  /** The status field showing connection and listing status. */
  private JTextField statusField;
  
  private XMLTree xmlTree;
  
  private JScrollPane  treeHolder;

  ////////////////////////////////////////////////////////////

  /** 
   * Creates a new chooser.
   * 
   * @param serverList the initial list of {@link
   * ServerTableModel.Entry} objects to display.
   * listing.
   */
  public THREDDSFileChooser (
    List serverList
  ) {

    super (new BorderLayout());

    // Create server chooser
    // ---------------------
    String[] serverStrings = { "http://coastwatch.noaa.gov/thredds/catalog/chloraAquaMODISMaskedCWHDFCB05/catalog.xml", 
    		                "http://coastwatch.noaa.gov/thredds/catalog/chloraAquaMODISMaskedCWHDFSE05/catalog.xml", 
    		                "http://coastwatch.noaa.gov/thredds/catalog/chloraAquaMODISMaskedCWHDFNE05/catalog.xml", 
    		                "http://coastwatch.noaa.gov/thredds/catalog/chloraAquaMODISMaskedCWHDFWC05/catalog.xml", 
    		                "http://coastwatch.noaa.gov/thredds/catalog/chloraAquaMODISMaskedCWHDFGM05/catalog.xml" };

    serverBox = new JComboBox(serverStrings);
    serverBox.setSelectedIndex(4);
    serverBox.addActionListener(new ServerListener());
    JLabel serverLabel = new JLabel("Select a Catalog: ");
    JPanel serverPanel = new JPanel();
    serverPanel.add(serverLabel, BorderLayout.EAST);
    serverPanel.add(serverBox, BorderLayout.WEST);
    this.add (serverPanel, BorderLayout.NORTH);

  } // NetworkFileChooser constructor

  ////////////////////////////////////////////////////////////

  // Handles a new server connection request. */
  private class ServerListener implements ActionListener {
	//private Document doc;
	//private String servername;
    public void actionPerformed (ActionEvent event) {
    	
        String servername = (String)serverBox.getSelectedItem();
        final String servername2 = new String(servername);
        
        	Thread worker = new Thread(){
        		public void run () {
        		try{
        		final java.net.URL url = new URL(servername2);
        		if (url != null) {
                URLConnection connection = url.openConnection();
                InputStream is = connection.getInputStream();
                String content=new String(readBytes(is));
                final Document doc = getDocument(content);
                SwingUtilities.invokeLater (new Runnable() {
                public void run() { 
                	if(xmlTree == null){
                		xmlTree = new XMLTree(doc.getDocumentElement(), servername2);
                		xmlTree.addPropertyChangeListener (new FileListener());
                		treeHolder = new JScrollPane(xmlTree);
                		treeHolder.setPreferredSize(new Dimension(100,100));
                		treeHolder.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
                		treeHolder.setMinimumSize(new Dimension(100,100));
                		THREDDSFileChooser.this.add(treeHolder, BorderLayout.CENTER);
                		THREDDSFileChooser.this.validate();
                	}
                	else{
                		xmlTree.loadDocument(doc.getDocumentElement(), servername2);
                		THREDDSFileChooser.this.validate();
                	}
                }});//invokeLater
        		}//if
        		}//try
        		catch(Exception e){}
        		}//run
        	};//thredd
      //} // if
        	worker.run();
    } // propertyChange
  } // ServerListener class

  ////////////////////////////////////////////////////////////

  // Handles a file chooser change. */
  private class FileListener implements PropertyChangeListener {
    public void propertyChange (PropertyChangeEvent event) {

      String prop = event.getPropertyName();

      // Update status
      // -------------
      if (prop.equals ("File_Selected")) {
        THREDDSFileChooser.this.firePropertyChange (FILE_PROPERTY, null, event.getNewValue());
      } // else if

    } // propertyChange
  } // FileListener class
  
  /**
   *  Create a Document object with the given xml.
   *
   *  @param xml The xml.
   *  @return A new Document.
   *  @throws Exception When something goes wrong.
   */
  public Document getDocument(String xml) throws Exception {
      xml = xml.trim();
      DocumentBuilder builder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      if (xml.length() == 0) {
          return builder.newDocument();
      }
      //MyErrorHandler errorHandler = new MyErrorHandler();
      //builder.setErrorHandler(errorHandler);
      try {
          return builder.parse(new ByteArrayInputStream(xml.getBytes()));
      } catch (Exception exc) {
          //            System.err.println("OOps:" + xml);
          throw new IllegalStateException("Error parsing xml: "
                                          + exc.getMessage());
      }
      /*
      DOMParser parser = new DOMParser();
      parser.parse(
          new InputSource(new ByteArrayInputStream(xml.getBytes())));
      return  parser.getDocument();
      */
  }
  
  /**
   * Read the bytes in the given input stream.
   *
   * @param is The input stream
   *
   * @throws IOException On badness
   */
  public static byte[] readBytes(InputStream is)
          throws IOException {
      int    totalRead = 0;
      //byte[] content   = getByteBuffer();
      byte[] content   = new byte[1000000];
      try {
          while (true) {
              int howMany = is.read(content, totalRead,
                                    content.length - totalRead);
              if (howMany < 0) {
                  break;
              }
              if (howMany == 0) {
                  continue;
              }
              totalRead += howMany;
              if (totalRead >= content.length) {
                  byte[] tmp       = content;
                  int    newLength = ((content.length < 25000000)
                                      ? content.length * 2
                                      : content.length + 5000000);
                  content = new byte[newLength];
                  System.arraycopy(tmp, 0, content, 0, totalRead);
              }
          }
      } finally {
          try {
                  is.close();
              }
          catch (Exception exc) {}
      }
      byte[] results = new byte[totalRead];
      System.arraycopy(content, 0, results, 0, totalRead);
      //putByteBuffer(content);
      return results;
  }

  ////////////////////////////////////////////////////////////

} // THREDDSFileChooser class

////////////////////////////////////////////////////////////////////////
