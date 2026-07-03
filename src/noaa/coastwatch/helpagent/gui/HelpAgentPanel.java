/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JTextPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.Style;

import noaa.coastwatch.helpagent.HelpAgentDefaults;

/**
 * A Swing panel that provides a chat UI for the CoastWatch Help Agent HTTP service.
 *
 * <p>The panel creates and maintains a server-side chat session, streams assistant
 * responses using Server-Sent Events (SSE), and renders user/assistant/system
 * messages. The panel is designed for embedding in desktop tools
 * such as CDAT, but can also be launched as a standalone test UI using
 * {@link #main(String[])}.</p>
 *
 * @author Peter Hollemans
 * @since 4.2.0
 * @serial exclude
 */
public class HelpAgentPanel extends JPanel {

  // private final JTextPane transcriptPane;
  // private final JScrollPane transcriptScrollPane;
  // private final JTextArea inputArea;


  ////////////////////////////////////////////////////////////

  /** Creates a new help agent panel using the default service settings. */
  public HelpAgentPanel() {

    // this (
    //   HelpAgentDefaults.DEFAULT_URL,
    //   HelpAgentDefaults.DEFAULT_STORE,
    //   HelpAgentDefaults.DEFAULT_MODEL
    // );

  } // HelpAgentPanel constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new help agent panel.
   *
   * @param serviceUrl the help agent service URL.
   * @param storeName the file-search store name.
   * @param model the model name.
   */
  public HelpAgentPanel (
    String serviceUrl, 
    String storeName, 
    String model
  ) {

    // super (new BorderLayout(8, 8));
    // setBorder (new EmptyBorder (8, 8, 8, 8));

    // transcriptPane = new JTextPane();
    // transcriptPane.setEditable (false);
    // transcriptScrollPane = new JScrollPane (transcriptPane);
    // transcriptScrollPane.setVerticalScrollBarPolicy (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    // var transcriptScrollPaneContainer = new JPanel (new BorderLayout());
    // transcriptScrollPaneContainer.setBorder (BorderFactory.createTitledBorder ("Chat Transcript"));
    // transcriptScrollPaneContainer.add (transcriptScrollPane);
    // this.add (transcriptScrollPaneContainer, BorderLayout.CENTER);

    // inputArea = new JTextArea (4, 60);
    // inputArea.setLineWrap (true);
    // inputArea.setWrapStyleWord (true);
    // var inputScrollPane = new JScrollPane (inputArea);
    // inputScrollPane.setVerticalScrollBarPolicy (ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    // var inputPanel = new JPanel (new BorderLayout());
    // inputPanel.setBorder (BorderFactory.createTitledBorder ("Chat Input"));
    // inputPanel.add (inputScrollPane, BorderLayout.CENTER);

    // var buttonBox = Box.createVerticalBox();
    // var sendButton = new JButton ("⬆ Send");
    // sendButton.addActionListener (event -> submitQuestion());
    // buttonBox.add (Box.createVerticalGlue());
    // buttonBox.add (sendButton);
    // inputPanel.add (buttonBox, BorderLayout.EAST);
    // this.add (inputPanel, BorderLayout.SOUTH);












  } // HelpAgentPanel constructor

  ////////////////////////////////////////////////////////////

  private void submitQuestion() {






  } // submitQuestion

  ////////////////////////////////////////////////////////////

  /**
   * Tests the help agent panel.
   *
   * @param args the command line arguments.
   */
  public static void main (String[] args) {

    SwingUtilities.invokeLater(
      () -> {
        JFrame frame = new JFrame ("CoastWatch Utilities Help Agent");
        frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.setContentPane (new HelpAgentPanel());
        frame.setSize (800, 600);
        frame.setLocationRelativeTo (null);
        frame.setVisible (true);
      }
    );

  } // main

  ////////////////////////////////////////////////////////////

} // HelpAgentPanel
