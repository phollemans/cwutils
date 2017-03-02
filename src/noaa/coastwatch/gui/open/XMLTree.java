////////////////////////////////////////////////////////////////////////
/*

     File: XMLTree.java
   Author: X. Liu
     Date: 2011/12/08

  CoastWatch Software Library and Utilities
  Copyright (c) 2011 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.open;

//Imports
//-------
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A JTree that displays an xml document.
 *
 * @author Xiaoming Liu
 * @since 3.3.0
 */
public class XMLTree extends JTree {


    /** xml attribute */
    public static final String ATTR_NAME = "name";

    /** xml attribute */
    public static final String ATTR_LABEL = "label";

    /** the null string */
    public static final String NULL_STRING = null;


    /** Icon used for the leafs of the jtree */
    static ImageIcon leafIcon;

    /** The jtree root */
    private XmlTreeNode treeRoot;

    /** The tree model */
    private DefaultTreeModel treeModel;


    /** A map from dom element to tree node */
    private Hashtable elementToNode = new Hashtable();

    /**
     * Collection of xml tag names that are not to be added to the jtree
     */
    private Hashtable tagsToNotProcessButRecurse = null;

    /** If true then add all of the attributes of each node as jtree children nodes */
    private boolean includeAttributes = false;

    /** If set then only tags in this list are recursed */
    private Hashtable tagsToRecurse = null;

    /** If set then only tags in this list are processed */
    private Hashtable tagsToProcess = null;

    /** Don't process the children trees of tags in this list */
    private Hashtable tagsToNotRecurse = null;

    /** Don't process the tags in this list */
    private Hashtable tagsToNotProcess = null;

    /** Define the name of the xml attribute that should be used for the label for certain tags */
    private Hashtable tagNameToLabelAttr = null;

    /** You can specify that the label for a certain tag is gotten from a child of the tag */
    private Hashtable tagNameToLabelChild = null;

    /**
     *  The useTagNameAsLabel property.
     */
    private boolean useTagNameAsLabel;

    /** where we came from */
    private String baseUrlPath;

    
    /**
     * ctor
     *
     * @param xmlRoot The root of the xml dom tree
     * @param basePath Where the xml came from
     *
     */
    public XMLTree(Element xmlRoot, String basePath) {
        setToolTipText(" ");
        setMultipleSelect(false);
        setShowsRootHandles(true);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                treeClick(event);
            }
        });
        loadDocument(xmlRoot, basePath);
    }
    
    /**
     * loadDocument
     *
     * @param xmlRoot The root of the xml dom tree
     * @param basePath Where the xml came from
     *
     */
    public void loadDocument(Element xmlRoot, String basePath) {
        baseUrlPath         = basePath;
        List tags = new ArrayList();
        tags.add("dataset");
        tags.add("catalogRef");
        tags.add("collection");
        tags.add("documentation");
        tags.add("docparent");
        addTagsToProcess(tags);
        treeRoot        = new XmlTreeNode(null, "");
        treeModel       = new DefaultTreeModel(treeRoot);
        setModel(treeModel);
        setRootVisible(false);
        if (xmlRoot != null) {
            treeRoot.removeAllChildren();
            process(treeRoot, xmlRoot);
            treeModel.nodeStructureChanged(treeRoot);
            for (Enumeration children = treeRoot.children();
                        children.hasMoreElements(); ) {
                    DefaultMutableTreeNode child =
                        (DefaultMutableTreeNode) children.nextElement();
                    expandPath(new TreePath(new Object[] { treeRoot,
                            child }));
            }
        }
    }
    


    /**
     *  Gets called when the tree is clicked.
     *
     * @param event Mouse event
     */
    protected void treeClick(MouseEvent event) {
        XmlTreeNode node = getXmlTreeNodeAt(event.getX(), event.getY());
        if (node == null) {
            return;
        }
        Element element = node.getXmlNode();
        if (element == null) {
            return;
        }
        if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() > 1) {
        	String s = element.getAttribute("name");
        	String path = baseUrlPath.replaceFirst("catalog", "dodsC");
        	String ss = path.replaceFirst("catalog.xml",s);
        	firePropertyChange ("File_Selected", null, ss);
        }
    }


    /**
     *  Define the set of tags who we should process
     *
     * @param tags List of tag names
     */
    public void addTagsToProcess(List tags) {
        tagsToProcess = addToTable(tagsToProcess, tags);
    }
    

    /**
     *  Should we show the given xml Element
     *
     * @param xmlNode
     * @return Should we look at this node and turn it into a jtree node
     */
    protected boolean shouldProcess(Element xmlNode) {
        String tagName = getLocalName(xmlNode);
        if (tagsToProcess != null) {
            if (tagsToProcess.get(tagName) == null) {
                return false;
            }
        }

        if (tagsToNotProcess != null) {
            return (tagsToNotProcess.get(getLocalName(xmlNode))
                    == null);
        }
        return true;
    }


    /**
     *  Walk the xml tree at the given xmlNode and create the JTree
     *
     * @param parentTreeNode The parent jtree node
     * @param xmlNode The xml node to process
     */
    protected void process(XmlTreeNode parentTreeNode, Element xmlNode) {
        XmlTreeNode childTreeNode = null;
        if (shouldProcess(xmlNode)) {
            String label = getLabel(xmlNode);
            childTreeNode = new XmlTreeNode(xmlNode, label);
            elementToNode.put(xmlNode, childTreeNode);
            parentTreeNode.add(childTreeNode);
            if (includeAttributes) {
                NamedNodeMap           attrs     = xmlNode.getAttributes();
                DefaultMutableTreeNode attrsNode = null;
                for (int i = 0; i < attrs.getLength(); i++) {
                    Attr attr = (Attr) attrs.item(i);
                    if (i == 0) {
                        attrsNode = new DefaultMutableTreeNode("Attributes");
                        childTreeNode.add(attrsNode);
                    }
                    attrsNode.add(
                        new DefaultMutableTreeNode(
                            attr.getNodeName() + "=" + attr.getNodeValue()));
                }
            }
        }


        if (shouldRecurse(xmlNode)) {
            NodeList children = getElements(xmlNode);
            if (childTreeNode == null) {
                childTreeNode = parentTreeNode;
            }
            for (int i = 0; i < children.getLength(); i++) {
                Element childXmlNode = (Element) children.item(i);
                process(childTreeNode, childXmlNode);
            }
        }
    }

    /**
     *  Should we recursiely descend the children of the given xml Element
     *
     * @param xmlNode The xml node
     * @return    Should we recurse down
     */
    protected boolean shouldRecurse(Element xmlNode) {
        String tagName = getLocalName(xmlNode);

        if (tagsToNotProcessButRecurse != null) {
            if (tagsToNotProcessButRecurse.get(tagName) != null) {
                return true;
            }
        }

        if (tagsToRecurse != null) {
            if (tagsToRecurse.get(tagName) == null) {
                return false;
            }
        }

        if (tagsToNotRecurse != null) {
            return (tagsToNotRecurse.get(getLocalName(xmlNode))
                    == null);
        }
        return true;

    }

    /**
     *  Return the String used for the JTree node.
     *  This first looks in the tagNameToLabelAttr hashtable
     *  for an attribute name to fetch the label. If not found
     *  we try the attributes "label" and "name".
     *
     * @param n The node
     * @return Its label
     */
    public String getLabel(Element n) {
        String label = null;

        if (useTagNameAsLabel) {
            return getLocalName(n);
        }

        if (tagNameToLabelAttr != null) {
            String attrName =
                (String) tagNameToLabelAttr.get(getLocalName(n));
            if (attrName != null) {
                label = getAttribute(n, attrName, NULL_STRING);
            }
        }

        if (tagNameToLabelChild != null) {
            String childTag =
                (String) tagNameToLabelChild.get(getLocalName(n));
            if (childTag != null) {
                Element child = getElement(n, childTag);
                if (child != null) {
                    label = getChildText(child);
                }
            }
        }

        if (label == null) {
            label = getAttribute(n, ATTR_LABEL, NULL_STRING);
        }

        if (label == null) {
            label = getAttribute(n, ATTR_NAME, NULL_STRING);
        }

        if (label == null) {
            label = getLocalName(n);
        }


        return label;
    }
    
    
    /**
     *  Return the xml tree node located at the given position
     *
     * @param x x
     * @param y y
     * @return The node or null
     */
    public XmlTreeNode getXmlTreeNodeAt(int x, int y) {
        return getXmlTreeNodeAtPath(getPathForLocation(x, y));
    }

    /**
     *  Return the xml tree node located at the given position
     *
     * @param path The tree path
     * @return The node or null
     */
    protected XmlTreeNode getXmlTreeNodeAtPath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        if ( !(last instanceof XmlTreeNode)) {
            return null;
        }
        return (XmlTreeNode) last;
    }

    /**
     * Set tree select mode
     *
     * @param v Do multiples?
     */
    public void setMultipleSelect(boolean v) {
        getSelectionModel().setSelectionMode(v
                                             ? TreeSelectionModel
                                             .DISCONTIGUOUS_TREE_SELECTION
                                             : TreeSelectionModel
                                             .SINGLE_TREE_SELECTION);
    }
    
    /**
     * Get the non qualified tag name
     *
     * @param element element
     *
     * @return tag name
     */
    public String getLocalName(Node element) {
        String localName = element.getLocalName();
        if (localName != null) {
            return localName;
        }
        String name = element.getNodeName();
        int    idx  = name.indexOf(":");
        if (idx >= 0) {
            name = name.substring(idx + 1);
        }
        return name;
    }
    
    /**
     *  Get the given name-d attribute from the given element.
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @param dflt The default value.
     *  @return The attribute value or the dflt if not found.
     */
    public String getAttribute(Node element, String name,
                                      String dflt) {
        if (element == null) {
            return dflt;
        }
        return getAttribute(element.getAttributes(), name, dflt);
    }
        
    
    /**
     *  Get the given name-d attribute from the given attrs map.
     *  If not found then return the dflt argument.
     *
     *  @param attrs The xml attribute map.
     *  @param name The name of the attribute.
     *  @param dflt The default value
     *  @return The attribute valueif found, else the dflt argument.
     */
    public String getAttribute(NamedNodeMap attrs, String name,
                                      String dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        return ((n == null)
                ? dflt
                : n.getNodeValue());
    }
    
    /**
     *  Concatenates the node values (grom getNodeValue) of the  children of the given parent Node.
     *
     *  @param parent The xml node to search its chidlren.
     *  @return The text values contained by the children of the given parent.
     */
    public String getChildText(Node parent) {
        if (parent == null) {
            return null;
        }
        NodeList     children = parent.getChildNodes();
        StringBuffer sb       = new StringBuffer();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ((child.getNodeType() == Node.TEXT_NODE)
                    || (child.getNodeType() == Node.CDATA_SECTION_NODE)) {
                sb.append(child.getNodeValue());
            }
        }
        return sb.toString();
    }

    
    /**
     *  Get the first  Element children of the given parent Element with the  given tagName.
     *
     *  @param parent The xml node to search its children.
     *  @param tagName The tag to match.
     *  @return The first Element child that matches the given tag name.
     */
    public Element getElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Object o = children.item(i);
            if (o instanceof Element) {
                Element e = (Element) o;
                if (isTag(e, tagName)) {
                    return e;
                }
            }
        }
        return null;
    }
    
    /**
     *  Get all children of the given parent Element who are instances of
     *  the Element class.
     *
     *  @param parent The xml node to search its chidlren.
     *  @return All Element children of the given parent.
     */
    public NodeList getElements(Element parent) {
        return getElements(parent, new XmlNodeList());
    }
    
    /**
     *  Get all Element children of the given parent Element with the
     *  given tagName.
     *
     *  @param parent The xml node to search its children.
     *  @param tagName The tag to match.
     *  @return The Element children of the given parent node whose tags match the given tagName.
     */
    public XmlNodeList getElements(Element parent, String tagName) {
        XmlNodeList nodeList = new XmlNodeList();
        NodeList    children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Object o = children.item(i);
            if (o instanceof Element) {
                Element e = (Element) o;
                if ((tagName == null) || isTag(e, tagName)) {
                    nodeList.add(e);
                }
            }
        }
        return nodeList;
    }
    
    /**
     *  Get all children of the given parent Element who are instances of
     *  the Element class.
     *
     *  @param parent The xml node to search its chidlren.
     * @param nodeList list to add to
     *  @return All Element children of the given parent.
     */
    public static NodeList getElements(Element parent, XmlNodeList nodeList) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Object o = children.item(i);
            if (o instanceof Element) {
                nodeList.add(o);
            }
        }
        return nodeList;
    }
    
    /**
     * Checks if the tag name of the given  node matches the given name.
     * If the given name is fully qualified (e.g., namespace:tagname) then
     * check if it matches the full name of the node.
     * If the node name is fully qualified and the name isn't then strip off
     * the namespace of the node and compare
     * else just compare the 2
     *
     *
     * @param node the xml node
     * @param name name
     *
     * @return is non qualified tag name the same
     */
    public boolean isTag(Node node, String name) {
        if (name == null) {
            return true;
        }
        String nodeName = node.getNodeName();
        if (name.indexOf(":") >= 0) {
            return nodeName.equals(name);
        }
        if (nodeName.indexOf(":") >= 0) {
            return equals(getLocalName(node), name);
        }
        return equals(nodeName, name);
    }
    
    /**
     *  adds the objects in the given tags list into the given hashtable.
     *
     * @param ht The hashtable
     * @param tags List of tags to add
     * @return the ht argument (or the newly constructed ht of the ht arg is null)
     */
    private Hashtable addToTable(Hashtable ht, List tags) {
        if (ht == null) {
            ht = new Hashtable();
        }
        for (int i = 0; i < tags.size(); i++) {
            ht.put(tags.get(i), tags.get(i));
        }
        return ht;
    }
    
    public boolean equals(Object o1, Object o2) {
        if ((o1 != null) && (o2 != null)) {
            return o1.equals(o2);
        }
        return ((o1 == null) && (o2 == null));
    }

    
    /**
     * Class XmlTreeNode
     */
    public class XmlTreeNode extends DefaultMutableTreeNode {

        /** Corresponding xml node */
        Element xmlNode;

        /**
         * ctor
         *
         * @param node The xml node
         * @param name The label to use
         *
         */
        public XmlTreeNode(Element node, String name) {
        	super(name);
            this.xmlNode      = node;
        }

        /**
         * ctor
         *
         * @param node The xml node
         */
        public XmlTreeNode(Element node) {
        	this(node, "");
        }

        /**
         * Get the node
         *
         * @return The xml node
         */
        public Element getXmlNode() {
            return xmlNode;
        }

    }
    
    
    /**
     * Class XmlNodeList
     */
    public class XmlNodeList extends ArrayList implements NodeList {

        /**
         *  Default ctor.
         */
        public XmlNodeList() {}

        /**
         *  Return the length of the list.
         *  @return The length.
         */
        public int getLength() {
            return size();
        }

        /**
         *  Return the index'th item.
         *  @param index The list index.
         *  @return The Node.
         */
        public Node item(int index) {
            return (Node) get(index);
        }
    }

}
