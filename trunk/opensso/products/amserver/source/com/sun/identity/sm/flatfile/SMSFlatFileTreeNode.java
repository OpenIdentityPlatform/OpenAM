/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SMSFlatFileTreeNode.java,v 1.4 2008/06/25 05:44:09 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.flatfile;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.SMSException;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


/**
 * This is a node in the directory tree that the 
 * <code>SMSEnhancedFlatFileObject</code> class holds to track the 
 * directory structure of the data.
 */
public class SMSFlatFileTreeNode {
    private String distinguishedName;
    private String id;
    private String name;
    private SMSFlatFileTreeNode parentNode;
    private Set children = new HashSet();
    
    private static final String XML_DIRECTIVES = 
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n\n" +
        "<!DOCTYPE FlatFileTree\n" +
        "     PUBLIC \"=//Service Management Services (SMS) 1.0 DTD//EN\"\n"
        + "    \"jar://com/sun/identity/sm/flatfile/smsflatfile.dtd\">\n\n";
    private static final String VERSION = "1.0";
    private static final String XML_VERSION_ATTR_NAME = "version";
    private static final String XML_ROOT_ELEMENT = "FlatFileTree";
    private static final String XML_NODE_ELEMENT = "FlatFileNode";
    private static final String XML_ROOT_START_TAG = 
        "<" + XML_ROOT_ELEMENT + " " + XML_VERSION_ATTR_NAME + "=\"" + 
        VERSION + "\">";
    private static final String XML_ROOT_END_TAG = 
        "</" + XML_ROOT_ELEMENT + ">";
    private static final String XML_NODE_DN_ATTR_NAME = "distinguishedName";
    private static final String XML_NODE_START_TAG = 
        "<" + XML_NODE_ELEMENT + " " + XML_NODE_DN_ATTR_NAME + "=\"{0}\">";
    private static final String XML_NODE_END_TAG = 
        "</" + XML_NODE_ELEMENT + ">";

    
    /**
     * Creates a new instance of SMSFlatFileTreeNode 
     */
    public SMSFlatFileTreeNode(String dn) {
        distinguishedName = dn;
        id = dn.toLowerCase();
        int idx = dn.indexOf(',');
        name = (idx != -1) ? dn.substring(0, idx) : dn;
    }
    
    /**
     * Returns parent node.
     *
     * @return parent node.
     */
    public SMSFlatFileTreeNode getParentNode() {
        return parentNode;
    }
    
    /**
     * Adds child.
     *
     * @param child Child node.
     * @return <code>true</code> if child is added to the tree.
     * @throws SMSException if the dn of the child is not prefix with
     *         this node.
     */
    public boolean addChild(SMSFlatFileTreeNode child) {
        boolean added = false;
        String childDN = child.id;
        String parentDN = "," + id;
        
        if (childDN.endsWith(parentDN)) {
            String rdn = child.distinguishedName.substring(
                0, (child.distinguishedName.length() - parentDN.length()));

            if (rdn.indexOf(',') == -1) {
                children.add(child);
                child.parentNode = this;
                added = true;
            } else {
                for (Iterator i = children.iterator(); (i.hasNext()) && !added;
                ) {
                    SMSFlatFileTreeNode c = (SMSFlatFileTreeNode)i.next();
                    added = c.addChild(child);
                }
            }
        }

        return added;
    }
    
    /**
     * Removes a child node from the tree.
     *
     * @param node Node of to be removed.
     * @param baseDir Base directory where data files are stored.
     */
    public boolean removeChild(SMSFlatFileTreeNode node, String baseDir) {
        boolean removed = false;
        for (Iterator i = children.iterator(); (i.hasNext()) && !removed; ) {
            SMSFlatFileTreeNode c = (SMSFlatFileTreeNode)i.next();
            if (c.equals(node)) {
                i.remove();
                node.clear(baseDir);
                removed = true;
            }
        }
        return removed;
    }
    
    private void clear(String baseDir) {
        parentNode = null;
        File file = new File(getAttributeFilename(baseDir));
        if (file.exists()) {
            file.delete();                    
        }
        
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            SMSFlatFileTreeNode c = (SMSFlatFileTreeNode)i.next();
            c.clear(baseDir);
        }
    }
    
    /**
     * Returns the attribute file name of a node.
     *
     * @param dn Distinguished Name of the node.
     * @param baseDir Base Directory.
     * @return the attribute file name of a node.
     */
    public String getAttributeFilename(String dn, String baseDir) {
        SMSFlatFileTreeNode child = getChild(dn);
        return (child != null) ? child.getAttributeFilename(baseDir) : null;
    }

    /**
     * Returns the attribute file name of this node.
     *
     * @param baseDir Base Directory.
     * @return the attribute file name of a node.
     */
    public String getAttributeFilename(String baseDir) {
        String hash = Hash.hash(distinguishedName);
        hash = hash.replace('/', '_');
        return baseDir + File.separator + hash;
    }

    /**
     * Returns <code>true</code> is a node of a given <code>dn</code> exists.
     *
     * @param baseDir Base Directory.
     * @param dn Distinguished name of the node.
     * @return <code>true</code> is a node of a given <code>dn</code> exists.
     */
    public boolean isExists(String baseDir, String dn) {
        String hash = Hash.hash(dn);
        hash = hash.replace('/', '_');
        File file = new File(baseDir + File.separator + hash);
        return file.exists() || isNodeExists(dn);
    }
    
    private boolean isNodeExists(String dn) {
        boolean exist = distinguishedName.equalsIgnoreCase(dn);
        if (!exist) {
             for (Iterator i = children.iterator(); (i.hasNext() && !exist);) {
                 SMSFlatFileTreeNode c = (SMSFlatFileTreeNode)i.next();
                 exist = c.isNodeExists(dn);
             }
         }
        return exist;
    }

    /**
     * Returns the node of a given distinguished name.
     *
     * @param dn Distinguished Name of the node.
     * @return the node of a given distinguished name.
     */
     public SMSFlatFileTreeNode getChild(String dn) {
         SMSFlatFileTreeNode child = null;
         
         if (distinguishedName.equalsIgnoreCase(dn)) {
             child = this;
         } else {
             for (Iterator i = children.iterator(); 
                (i.hasNext()) && (child == null);
             ) {
                 SMSFlatFileTreeNode c = (SMSFlatFileTreeNode)i.next();
                 child = c.getChild(dn);
             }
         }

         return child;
     }
     
     /**
      * Searches for nodes that matches with a given filter.
      *
      * @param filter Search filter.
      * @param recursive <code>true</code> to perform search recursively.
      * @return a set of node of class, <code>SMSFlatFileTreeNode</code>.
      */
     public Set searchChildren(NodeNameFilter filter, boolean recursive) {
         Set set = new HashSet();
         getChildren(recursive, set);
         Set results = new HashSet();
         for (Iterator i = set.iterator(); i.hasNext(); ) {
            SMSFlatFileTreeNode node = (SMSFlatFileTreeNode)i.next();
            if ((filter == null) || filter.accept(node.getName())) {
                results.add(node);
            }
         }
         return results;
     }
     
     private void getChildren(boolean recursive, Set results) {
         for (Iterator i = children.iterator(); i.hasNext(); ) {
             SMSFlatFileTreeNode c = (SMSFlatFileTreeNode)i.next();
             results.add(c);
             if (recursive) {
                 c.getChildren(true, results);
             }
         }
     }

     /**
      * Returns distinguished name.
      **
      * @return distinguished name.
      */
     public String getDN() {
        return distinguishedName;
     }

     /**
      * Returns name.
      **
      * @return name.
      */     
     public String getName() {
        return name;
     }

     /**
      * Returns <code>true</code> if this node is identical to other.
      *
      * @param o Other node.
      * @return <code>true</code> if this node is identical to other.
      */
     public boolean equals(Object o) {
         boolean same = false;
         if (o instanceof SMSFlatFileTreeNode) {
             same = ((SMSFlatFileTreeNode)o).id.equals(id);
         }
         return same;
     }

     /**
      * Returns hash code.
      *
      * @return hash code.
      */
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns XML String of this tree.
     *
     * @return XML String of this tree.
     */
    public String toXML() {
        StringBuilder xml = new StringBuilder();
        if (parentNode == null) {
            xml.append(XML_DIRECTIVES)
               .append(XML_ROOT_START_TAG)
               .append("\n");
        }
        
        Object[] dn = {distinguishedName};
        xml.append(MessageFormat.format(XML_NODE_START_TAG, dn))
           .append("\n");
        
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            SMSFlatFileTreeNode c = (SMSFlatFileTreeNode)i.next();
            xml.append(c.toXML());
        }
        
        xml.append(XML_NODE_END_TAG)
           .append("\n");
        
        if (parentNode == null) {
            xml.append(XML_ROOT_END_TAG)
               .append("\n");
        }
        return xml.toString();
    }
    
    /**
     * Creates directory tree from a XML.
     *
     * @param xml XML string.
     * @param debug Debugger.
     * @return the root node of the tree.
     */
    public static SMSFlatFileTreeNode createTree(String xml, Debug debug)
        throws Exception
    {
        SMSFlatFileTreeNode root = null;
        Document doc = XMLUtils.toDOMDocument(xml, debug);
        Node rootNode = XMLUtils.getRootNode(doc, XML_ROOT_ELEMENT);

        if (rootNode != null) {
            root = createNode(
                XMLUtils.getChildNode(rootNode, XML_NODE_ELEMENT));
        }
        
        return root;
    }
    
    private static SMSFlatFileTreeNode createNode(Node parentNode)
        throws SMSException
    {
        SMSFlatFileTreeNode node = new SMSFlatFileTreeNode(
            XMLUtils.getNodeAttributeValue(parentNode, 
                XML_NODE_DN_ATTR_NAME));
        Set children = XMLUtils.getChildNodes(parentNode, XML_NODE_ELEMENT);
  
        if ((children != null) && !children.isEmpty()) {
            for (Iterator i = children.iterator(); i.hasNext(); ) {
                Node n = (Node)i.next();
                SMSFlatFileTreeNode child = createNode(n);
                node.addChild(child);
            }
        }
        return node;
    }
}
