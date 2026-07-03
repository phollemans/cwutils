/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent.markdown;

import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TableSeparator;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Testing
import java.util.logging.Logger;

/**
 * Parses Markdown text and renders it to a {@link MarkdownSink} object 
 * by parsing the Markdown syntax tree.
 *
 * @author Peter Hollemans
 * @since 4.2.0
 */
public class MarkdownRenderer {

  private static final Logger LOGGER = Logger.getLogger (MarkdownRenderer.class.getName());

  private final Parser parser;

  ////////////////////////////////////////////////////////////

  enum NodeType {

    DOCUMENT, 
    HEADING,
    PARAGRAPH,
    TEXT,
    CODE,
    EMPHASIS,
    STRONG_EMPHASIS,
    SOFT_LINE_BREAK,
    HARD_LINE_BREAK,
    FENCED_CODE_BLOCK,
    THEMATIC_BREAK,
    TABLE_BLOCK,
    TABLE_HEAD,
    TABLE_SEPARATOR,
    TABLE_BODY,
    TABLE_ROW,
    TABLE_CELL,
    BULLET_LIST,
    ORDERED_LIST,
    BULLET_LIST_ITEM,
    ORDERED_LIST_ITEM,
    OTHER;

    public static NodeType of (
      Node node
    ) {

      if (node instanceof Document) return (DOCUMENT);
      else if (node instanceof Heading) return (HEADING);
      else if (node instanceof Paragraph) return (PARAGRAPH);
      else if (node instanceof Text) return (TEXT);
      else if (node instanceof Code) return (CODE);
      else if (node instanceof Emphasis) return (EMPHASIS);
      else if (node instanceof StrongEmphasis) return (STRONG_EMPHASIS);
      else if (node instanceof SoftLineBreak) return (SOFT_LINE_BREAK);
      else if (node instanceof HardLineBreak) return (HARD_LINE_BREAK);
      else if (node instanceof FencedCodeBlock) return (FENCED_CODE_BLOCK);
      else if (node instanceof ThematicBreak) return (THEMATIC_BREAK);
      else if (node instanceof TableBlock) return (TABLE_BLOCK);
      else if (node instanceof TableHead) return (TABLE_HEAD);
      else if (node instanceof TableSeparator) return (TABLE_SEPARATOR);
      else if (node instanceof TableBody) return (TABLE_BODY);
      else if (node instanceof TableRow) return (TABLE_ROW);
      else if (node instanceof TableCell) return (TABLE_CELL);
      else if (node instanceof BulletList) return (BULLET_LIST);
      else if (node instanceof OrderedList) return (ORDERED_LIST);
      else if (node instanceof BulletListItem) return (BULLET_LIST_ITEM);
      else if (node instanceof OrderedListItem) return (ORDERED_LIST_ITEM);
      else return (OTHER);

    } // of

    public boolean isContent () {

      switch (this) {

        case HEADING:
        case PARAGRAPH:
        case FENCED_CODE_BLOCK:
        case TABLE_ROW:
        case THEMATIC_BREAK:
          return (true);

        default:
          return (false);

      } // switch

    } // isContent

  } // NodeType

  ////////////////////////////////////////////////////////////


  /**
   * Creates a new markdown renderer.
   */
  public MarkdownRenderer () {

    MutableDataSet options = new MutableDataSet();
    options.set (Parser.EXTENSIONS, List.of (TablesExtension.create()));
    parser = Parser.builder (options).build();

  } // MarkdownRenderer ctor

  ////////////////////////////////////////////////////////////

  /**
   * Parses Markdown and gets the top-level block nodes.
   *
   * @param markdown the Markdown text to parse
   *
   * @return the top-level block nodes
   */
  public Node parseDocument (
    String markdown
  ) {

    Node document = parser.parse (markdown == null ? "" : markdown);
    return (document);

  } // parseDocument

  ////////////////////////////////////////////////////////////

  private static void preorderTraversalWithPredicate (
    Node node,
    Predicate<Node> visitorWithPredicate
  ) {

    boolean descend = visitorWithPredicate.test (node);
    if (descend) {
      for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
        preorderTraversalWithPredicate (child, visitorWithPredicate);
      } // for
    } // if

  } // preorderTraversalWithPredicate

  ////////////////////////////////////////////////////////////

  private static int indexOfNode (List<Node> list, Node node) {

    int index = -1;
    for (int i = 0; i < list.size(); i++) {
      if (list.get (i) == node) {
        index = i;
        break;
      } // if
    } // for

    return (index);

  } // indexOfNode

  ////////////////////////////////////////////////////////////

  /**
   * Renders a slice of a preorder traversal specified by the previously 
   * rendered node and the new target node.  The traversal rendering takes into
   * account content parents by opening and closing the parent nodes as 
   * needed.
   *
   * @param prevNode the previous node that was already rendered or null if
   * no previous node was rendered
   * @param targetNode the target node to render up to
   * @param sink the sink that receives rendering events
   */
  public void renderTraversal (
    Node prevNode,
    Node targetNode,
    MarkdownSink sink
  ) {

    var root = targetNode;
    while (root.getParent() != null) root = root.getParent();

    List<Node> traversal = new ArrayList<>();

    Predicate<Node> visitor = node -> { 
      traversal.add (node);
      boolean isContent = NodeType.of (node).isContent();
      return (!isContent);
    };

    preorderTraversalWithPredicate (root, visitor);

    int startIndex = prevNode == null ? 0 : indexOfNode (traversal, prevNode) + 1;
    int endIndex = indexOfNode (traversal, targetNode);

    for (int i = startIndex; i <= endIndex; i++) {

      var node = traversal.get (i);
      boolean isContent = NodeType.of (node).isContent();

      if (!isContent) {
        openParent (node, sink);
      } // if

      else {

        renderContent (node, sink);
        var walkNode = node;
        while (walkNode.getNext() == null && walkNode.getParent() != null) {
          closeParent (walkNode.getParent(), sink);
          walkNode = walkNode.getParent();
        } // while

      } // else

    } // for

  } // renderTraversal

  ////////////////////////////////////////////////////////////

  /**
   * Renders a content node to the supplied sink.
   *
   * @param node the node to render
   * @param sink the sink that receives rendering events
   */
  public void renderContent (
    Node node,
    MarkdownSink sink
  ) {

    LOGGER.fine ("Rendering content of type " + NodeType.of (node));

    AstVisitor visitor = new AstVisitor (sink);
    visitor.visit (node);

  } // renderContent

  ////////////////////////////////////////////////////////////

  /**
   * Computes the displayed index for an ordered list item by starting with
   * the ordered list start number and then adding the number of ordered-list
   * item siblings that precede this item.
   *
   * @param item the ordered list item to inspect
   *
   * @return the 1-based item number as it should be rendered
   */
  private static int orderedListItemIndex (
    OrderedListItem item
  ) {

    int startNumber = 1;
    if (item.getParent() instanceof OrderedList) {
      startNumber = ((OrderedList) item.getParent()).getStartNumber();
    } // if

    int siblingOffset = 0;
    for (Node sibling = item.getPrevious(); sibling != null; sibling = sibling.getPrevious()) {
      if (sibling instanceof OrderedListItem) siblingOffset++;
    } // for

    return (startNumber + siblingOffset);

  } // orderedListItemIndex

  ////////////////////////////////////////////////////////////

  /**
   * Opens a parent node by emitting the corresponding start event
   * to the markdown sink.
   *
   * @param node the parent node to open
   * @param sink the sink that receives rendering events
   *
   * @throws IllegalArgumentException if the node is not a parent node
   * @throws IllegalStateException if the node type is not handled
   */
  public void openParent (
    Node node,
    MarkdownSink sink
  ) {

    var type = NodeType.of (node);

    LOGGER.fine ("Opening parent of type " + type);

    if (type.isContent()) throw new IllegalArgumentException ("Open parent called on content node");

    switch (type) {
      case DOCUMENT: sink.startDocument(); break;
      case BULLET_LIST: sink.startBulletList(); break;
      case ORDERED_LIST: 
        sink.startOrderedList (((OrderedList) node).getStartNumber()); 
        break;
      case BULLET_LIST_ITEM: sink.startBulletListItem(); break;
      case ORDERED_LIST_ITEM:
        sink.startOrderedListItem (orderedListItemIndex ((OrderedListItem) node));
        break;
      case TABLE_BLOCK: sink.startTable(); break;
      case TABLE_HEAD: sink.startTableHeader(); break;
      case TABLE_BODY: sink.startTableBody(); break;
      case TABLE_SEPARATOR: sink.startTableSeparator(); break;
      case OTHER: break;
      default: throw new IllegalStateException ("Unhandled node type: " + type);
    } // switch

  } // openParent

  ////////////////////////////////////////////////////////////

  /**
   * Closes a previously opened parent node by emitting the corresponding end
   * event to the markdown sink.
   *
   * @param node the parent node to close
   * @param sink the sink that receives rendering events
   *
   * @throws IllegalArgumentException if the node is not a parent node
   * @throws IllegalStateException if the node type is not handled
   */
  public void closeParent (
    Node node,
    MarkdownSink sink
  ) {

    var type = NodeType.of (node);

    LOGGER.fine ("Closing parent of type " + type);

    if (type.isContent()) throw new IllegalArgumentException ("Close parent called on content node");

    switch (type) {
      case DOCUMENT: sink.endDocument(); break;
      case BULLET_LIST: sink.endBulletList(); break;
      case ORDERED_LIST: sink.endOrderedList(); break;
      case BULLET_LIST_ITEM: sink.endBulletListItem(); break;
      case ORDERED_LIST_ITEM: sink.endOrderedListItem(); break;
      case TABLE_BLOCK: sink.endTable(); break;
      case TABLE_HEAD: sink.endTableHeader(); break;
      case TABLE_BODY: sink.endTableBody(); break;
      case TABLE_SEPARATOR: sink.endTableSeparator(); break;
      case OTHER: break;
      default: throw new IllegalStateException ("Unhandled node type: " + type);
    } // switch

  } // closeParent

  ////////////////////////////////////////////////////////////

  /**
   * Visits markdown AST nodes and emits sink events.
   */
  private static class AstVisitor {

    private final MarkdownSink sink;
    private final NodeVisitor visitor;

    ////////////////////////////////////////////////////////////

    /**
     * Creates a new AST visitor.
     *
     * @param sink the sink for rendered output
     */
    public AstVisitor (
      MarkdownSink sink
    ) {

      this.sink = sink;

      visitor = new NodeVisitor (
        new VisitHandler<> (Heading.class, this::visit),
        new VisitHandler<> (Paragraph.class, this::visit),
        new VisitHandler<> (Text.class, this::visit),
        new VisitHandler<> (Code.class, this::visit),
        new VisitHandler<> (Emphasis.class, this::visit),
        new VisitHandler<> (StrongEmphasis.class, this::visit),
        new VisitHandler<> (SoftLineBreak.class, this::visit),
        new VisitHandler<> (HardLineBreak.class, this::visit),
        new VisitHandler<> (FencedCodeBlock.class, this::visit),
        new VisitHandler<> (ThematicBreak.class, this::visit),
        // new VisitHandler<> (TableBlock.class, this::visit),
        // new VisitHandler<> (TableHead.class, this::visit),
        new VisitHandler<> (TableRow.class, this::visit),
        new VisitHandler<> (TableCell.class, this::visit)
      );

    } // AstVisitor ctor

    ////////////////////////////////////////////////////////////

    /**
     * Visits a node.
     *
     * @param node the node to visit
     */
    public void visit (
      Node node
    ) {

      visitor.visit (node);

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      Heading heading
    ) {

      sink.startHeading (heading.getLevel());
      visitChildren (heading);
      sink.endHeading (heading.getLevel());

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      Paragraph paragraph
    ) {

      sink.startParagraph ();
      visitChildren (paragraph);
      sink.endParagraph ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      Text text
    ) {

      sink.text (text.getChars().unescape());

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      Code code
    ) {

      sink.inlineCode (code.getText().toString());

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      Emphasis emphasis
    ) {

      sink.startEmphasis ();
      visitChildren (emphasis);
      sink.endEmphasis ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      StrongEmphasis emphasis
    ) {

      sink.startStrong ();
      visitChildren (emphasis);
      sink.endStrong ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      SoftLineBreak lineBreak
    ) {

      sink.lineBreak ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      HardLineBreak lineBreak
    ) {

      sink.lineBreak ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      FencedCodeBlock codeBlock
    ) {

      sink.startCodeBlock (codeBlock.getInfo().toString().trim());
      sink.codeBlockText (codeBlock.getContentChars().normalizeEOL().toString());
      sink.endCodeBlock ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      ThematicBreak breakNode
    ) {

      sink.thematicBreak ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      TableBlock table
    ) {

      sink.startTable ();
      visitChildren (table);
      sink.endTable ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      TableHead head
    ) {

      visitChildren (head);

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      TableRow row
    ) {

      sink.startTableRow ();
      visitChildren (row);
      sink.endTableRow ();

    } // visit

    ////////////////////////////////////////////////////////////

    private void visit (
      TableCell cell
    ) {

      boolean isHeader = (cell.getParent() != null && cell.getParent().getParent() instanceof TableHead);
      sink.startTableCell (isHeader);
      visitChildren (cell);
      sink.endTableCell ();

    } // visit

    ////////////////////////////////////////////////////////////

    /**
     * Visits the children of a parent node.
     *
     * @param parent the parent node
     */
    private void visitChildren (
      Node parent
    ) {

      for (Node child = parent.getFirstChild(); child != null; child = child.getNext()) {
        visitor.visit (child);
      } // for

    } // visitChildren

    ////////////////////////////////////////////////////////////

  } // AstVisitor

  ////////////////////////////////////////////////////////////

} // MarkdownRenderer
