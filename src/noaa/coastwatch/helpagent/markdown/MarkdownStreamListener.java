/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent.markdown;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.OrderedList;
import java.util.List;
import java.util.function.Consumer;
import noaa.coastwatch.helpagent.ChatAgentClient;
import noaa.coastwatch.helpagent.markdown.MarkdownRenderer.NodeType;

// Testing
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Buffers streamed Markdown and renders only complete subtree units.
 *
 * @author Peter Hollemans
 * @since 4.2.0
 */
public class MarkdownStreamListener implements ChatAgentClient.StreamListener {

  private static final Logger LOGGER = Logger.getLogger (MarkdownStreamListener.class.getName());

  private final StringBuilder buffer;
  private final MarkdownRenderer renderer;
  private final MarkdownSink sink;
  private int lastRenderedIndex;
  private boolean failed;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new markdown stream listener.
   *
   * @param sink the sink used for rendered markdown
   */
  public MarkdownStreamListener (
    MarkdownSink sink
  ) {

    this.buffer = new StringBuilder();
    this.renderer = new MarkdownRenderer();
    this.sink = sink;
    this.lastRenderedIndex = -1;
    this.failed = false;

  } // MarkdownStreamListener ctor

  ////////////////////////////////////////////////////////////

  @Override
  public void onStatus (
    String text
  ) {

    LOGGER.fine (String.format ("Received STATUS text: '%s'", text.replace ("\n", "\\n")));

    if (!text.isEmpty()) {
      sink.startParagraph();
      sink.text (text);
      sink.endParagraph();
    } // if

  } // onStatus

  ////////////////////////////////////////////////////////////

  @Override
  public void onError (
    String text
  ) {

    LOGGER.fine (String.format ("Received ERROR text: '%s'", text.replace ("\n", "\\n")));

    if (text.isEmpty()) text = "Received unknown error.";
    sink.error (text);
    failed = true;

  } // onError

  ////////////////////////////////////////////////////////////

  @Override
  public void onToken (
    String text
  ) {

    LOGGER.fine (String.format ("Received TOKEN text: '%s'", text.replace ("\n", "\\n")));

    if (failed) {
      LOGGER.fine ("Ignoring TOKEN, failed flag is set");
      return;
    } // if

    if (text == null || text.isEmpty()) return;
    buffer.append (text);
    flush (false);

  } // onToken

  ////////////////////////////////////////////////////////////

  @Override
  public void onDone () {

    LOGGER.fine (String.format ("Received DONE signal"));

    if (failed) {
      LOGGER.fine ("Ignoring DONE, failed flag is set");
      return;
    } // if

    flush (true);

  } // onDone

  ////////////////////////////////////////////////////////////

  /**
   * Traverses a node subtree in preorder and applies a visitor to each node.
   *
   * @param node the root of the subtree to traverse
   * @param visitor the callback applied to each visited node
   */
  private static void preorderTraversal (
    Node node,
    Consumer<Node> visitor
  ) {

    visitor.accept (node);
    for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
      preorderTraversal (child, visitor);
    } // for

  } // preorderTraversal

  ////////////////////////////////////////////////////////////

  /** 
   * Updates the Markdown text for problems with code blocks.  Sometimes
   * the model outputs the code fence marks in unexpected and non-standard
   * places.  This corrects those isssues.
   * 
   * @param markdown the original Markdown text from the model.
   * 
   * @return the revised and corrected Markdown text.
   */
  private static String sanitizeFencedCodeBlocks (
    String markdown
  ) {

    StringBuilder result = new StringBuilder();
    String[] lines = markdown.split ("\n", -1);

    boolean inFence = false;
    String fence = null;
    String fenceIndent = "";

    for (String line : lines) {

      String trimmed = line.stripLeading();

      // Outside a fenced code block, just copy the line and detect whether
      // this line starts a fenced block.
      if (!inFence) {

        if (trimmed.startsWith ("```")) {
          inFence = true;
          fence = "```";
          fenceIndent = "";
          result.append (line).append ('\n');
        } // if
        else if (trimmed.startsWith ("~~~")) {
          inFence = true;
          fence = "~~~";
          fenceIndent = "";
          result.append (line).append ('\n');
        } // else if

        // Malformed opening fence appended to paragraph text, for example:
        //
        //   3. Run the script:```bash
        //
        // Flexmark treats the fence marker as literal paragraph text because a fenced
        // code block must start at the beginning of a line. Split it into:
        //
        //   3. Run the script:
        //      ```bash
        //
        // so the fence starts a real code block. If the paragraph line starts a list
        // item, indent the fence to the list-item continuation column so the code block
        // remains inside the list item.
        else {
          int backtick = trimmed.indexOf ("```");
          int tilde = trimmed.indexOf ("~~~");
          int fenceIndex = -1;

          if (backtick > 0 && (tilde < 0 || backtick < tilde)) {
            fenceIndex = backtick;
            fence = "```";
          } // if
          else if (tilde > 0) {
            fenceIndex = tilde;
            fence = "~~~";
          } // else if

          if (fenceIndex > 0) {
            int leading = line.length() - trimmed.length();
            int split = leading + fenceIndex;
            String before = line.substring (0, split).stripTrailing();
            String after = line.substring (split);

            String continuation = "";
            var matcher = java.util.regex.Pattern
              .compile ("^(\\s*(?:\\d+[.)]|[-+*])\\s+)")
              .matcher (line);
            if (matcher.find()) continuation = " ".repeat (matcher.group (1).length());

            result.append (before).append ('\n');
            result.append (continuation).append (after).append ('\n');

            inFence = true;
            fenceIndent = continuation;
          } // if
          else {
            result.append (line).append ('\n');
          } // else
        } // else

      } // if

      // Inside a fenced code block, look for either a valid closing fence on
      // its own line or a malformed closing fence appended to code text.
      else {

        String trailing = line.stripTrailing();

        // Proper closing fence on its own line, for example:
        //
        //   ```
        //
        // This is already valid Markdown, so copy the line unchanged and leave the
        // fenced-code state.
        if (trimmed.equals (fence)) {
          inFence = false;
          fence = null;
          result.append (fenceIndent).append (line).append ('\n');
          fenceIndent = "";
        } // if

        // Malformed closing fence followed by Markdown on the same line, for example:
        //
        //   ```### This is a new Markdown header
        //
        // Flexmark treats this as code-block text rather than as the end of the fenced
        // block followed by a heading. Split it into:
        //
        //   ```
        //   ### This is a new Markdown header
        //
        // so the following Markdown is parsed normally.
        else if (trimmed.startsWith (fence)) {
          int leading = line.length() - trimmed.length();
          String suffix = trimmed.substring (fence.length());

          result.append (fenceIndent).append (line.substring (0, leading)).append (fence).append ('\n');

          if (!suffix.isEmpty()) {
            result.append (line.substring (0, leading)).append (suffix).append ('\n');
          } // if

          inFence = false;
          fence = null;
          fenceIndent = "";
        } // else if

        // Malformed closing fence appended to the final code line, for example:
        //
        //   printf ("Hello world\n");```
        //
        // Flexmark keeps the closing fence as part of the code text. Split it into:
        //
        //   printf ("Hello world\n");
        //   ```
        //
        // so the fenced block closes cleanly.
        else if (trailing.endsWith (fence)) {
          int split = trailing.lastIndexOf (fence);
          String content = trailing.substring (0, split);
          String suffix = line.substring (trailing.length());

          result.append (fenceIndent).append (content).append (suffix).append ('\n');
          result.append (fenceIndent).append (fence).append ('\n');

          inFence = false;
          fence = null;
          fenceIndent = "";
        } // else if

        // Ordinary code line inside the fenced block
        else {
          result.append (fenceIndent).append (line).append ('\n');
        } // else

      } // else

    } // for

    return (result.toString());

  } // sanitizeFencedCodeBlocks

  ////////////////////////////////////////////////////////////

  /**
   * Flushes any newly completed renderable units to the Markdown sink based
   * on the current buffer text.
   *
   * @param isDone true if the stream has completed
   */
  void flush (boolean isDone) {

    // Sanitize the streamed Markdown before parsing so malformed fenced-code
    // endings from the model do not consume the rest of the document.
    String markdown = sanitizeFencedCodeBlocks (buffer.toString());

    // Parse the tree and traverse to get a new set of content nodes 
    Node doc = renderer.parseDocument (markdown);
    List<Node> traversal = new ArrayList<>();
    Consumer<Node> visit = node -> { if (NodeType.of (node).isContent()) traversal.add (node); };
    preorderTraversal (doc, visit);

    // If we're done, target to render the remaining content
    int targetIndex;
    if (isDone) {
      targetIndex = traversal.size() - 1;
    } // if

    // Otherwise, render only up to the second last content node and assume
    // that the last content node is incomplete
    else {
      targetIndex = traversal.size() - 2;
    } // else

    // If we have a valid target index, render from the previous to this 
    // target
    if (lastRenderedIndex < targetIndex) {
      var prevNode = lastRenderedIndex == -1 ? null : traversal.get (lastRenderedIndex);
      var targetNode = traversal.get (targetIndex);
      renderer.renderTraversal (prevNode, targetNode, sink);
      lastRenderedIndex = targetIndex;
    } // if

  } // flush

  ////////////////////////////////////////////////////////////

  private static int depth (Node node) {
    
    int depth = 0;
    Node parent = node.getParent();
    while (parent != null) {
      parent = parent.getParent();
      depth++;
    } // while
    return (depth);

  } // depth

  ////////////////////////////////////////////////////////////

  /**
   * Runs a test program that renders Markdown input.
   *
   * @param args the command line arguments.
   *
   * @throws Exception if an error occurs reading or rendering input.
   */
  public static void main (String[] args) throws Exception {

    var markdownFile = args[0];
    var blocksFile = args.length > 1 ? args[1] : null;

    {
      System.out.println ("===== Traversing document nodes =====");
      String text = Files.readString (Path.of (markdownFile));
      var renderer = new MarkdownRenderer();
      var doc = renderer.parseDocument (text);
      var textParents = new java.util.HashSet<String>();
      Consumer<Node> visit = node -> {
        int depth = depth (node);
        if (node instanceof com.vladsch.flexmark.ast.Text) { 
          textParents.add (node.getParent().getClass().getName()); 
        } // if
        for (int i = 0; i < depth; i++) System.out.print ("  ");
        System.out.println ("depth: " + depth + ", node: " + node);
      };
      preorderTraversal (doc, visit);
      System.out.println ("Text parents:");
      for (var name : textParents) System.out.println (name);
    }

    { 
      var sink = new TerminalSink (System.out, true);
      var listener = new MarkdownStreamListener (sink);
      System.out.println ("===== Test streaming line by line =====");
      try (var reader = new BufferedReader (new FileReader (markdownFile))) {
        String line;
        while ((line = reader.readLine()) != null) {
          listener.onToken (line + "\n");
        } // while
        listener.onDone();
      } // try
    }

    {

      System.out.println ("===== Test streaming by chunk =====");

      var sink = new TerminalSink (System.out, true);
      var listener = new MarkdownStreamListener (sink);

      var blockSizes = new ArrayList<Integer>();
      if (blocksFile != null) {
        try (var blockReader = new BufferedReader (new FileReader (blocksFile))) {
          String line;
          while ((line = blockReader.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty()) continue;
            blockSizes.add (Integer.parseInt (line));
          } // while
        } // try
        System.out.println ("Got blocks " + Arrays.toString (blockSizes.toArray()));
      } // if

      try (var reader = new BufferedReader (new FileReader (markdownFile))) {
        int blockSize = blockSizes.size() == 0 ? 64 : blockSizes.remove (0);
        char[] buffer = new char[blockSize];
        int count;
        while ((count = reader.read (buffer)) != -1) {
          listener.onToken (new String (buffer, 0, count));
          blockSize = blockSizes.size() == 0 ? 64 : blockSizes.remove (0);
          buffer = new char[blockSize];
        } // while
        listener.onDone();
      } // try

    } 

  } // main

  ////////////////////////////////////////////////////////////

} // MarkdownStreamListener
