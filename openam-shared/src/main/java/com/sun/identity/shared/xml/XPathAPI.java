/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.shared.xml;

import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

import org.forgerock.openam.utils.PerThreadCache;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * <code>XPathAPI</code> class provides the convenience function for XPath API
 * that is a subset of Xalan XPathAPI but uses JAXP internally.
 */
public class XPathAPI {

   static private final int CACHE_SIZE =
      SystemPropertiesManager.getAsInt(Constants.XPATHFACTORY_CACHE_SIZE, 1024);

   static private final PerThreadCache<XPathFactory, RuntimeException> xpathFactoryCache =
      new PerThreadCache<XPathFactory, RuntimeException>(CACHE_SIZE) {
          @Override
          protected XPathFactory initialValue() {
              return XPathFactory.newInstance();
          }
   };

   private XPathAPI() {
   }

  /**
   * Use an XPath string to select a single node.
   * Namespace prefix is resolved using the document node.
   *
   * @param doc The node to start searching from with the embedded context.
   * @param str XPath string.
   * @return The first node found that matches the XPath, or null.
   *
   * @throws XPathException
   */
   public static Node selectSingleNode(Node doc, String str) throws XPathException {
       return selectSingleNode(doc, str, doc);
   }

  /**
   * Use an XPath string to select a single node using a provided element namespace.
   *
   * @param doc The node to start searching from.
   * @param str XPath string.
   * @param nsNode Node where namespace prefix in XPath is resolved from.
   * @return The first node found that matches the XPath, or null.
   *
   * @throws XPathException
   */
   public static Node selectSingleNode(Node doc, String str, Node nsNode) throws XPathException {
       SimpleNamespaceContext nsctx = new SimpleNamespaceContext(nsNode);
       return selectSingleNode(doc, str, nsctx);
   }

  /**
   * Use an XPath string to select a single node using a passed in namespace context.
   *
   * @param doc The node to start searching from.
   * @param str XPath string.
   * @param nsctx Namespace where the prefix in XPath is resolved from.
   * @return The first node found that matches the XPath, or null.
   *
   * @throws XPathException
   */
   public static Node selectSingleNode(Node doc, String str, NamespaceContext nsctx) throws XPathException {
       NodeList nl = selectNodeList(doc, str, nsctx);
       if (nl.getLength()==0) {
          return null;
       }
       return nl.item(0);
   }

  /**
   * Use an XPath string to select a nodelist.
   * Namespace prefix is resolved using the document node.
   *
   * @param doc The node to start searching from.
   * @param str XPath string.
   * @return a NodeList of the matched result.
   *
   * @throws XPathException
   */
   public static NodeList selectNodeList(Node doc, String str) throws XPathException {
       return selectNodeList(doc, str, doc);
   }

  /**
   * Use an XPath string to select a nodelist using a node namespace.
   *
   * @param doc The node to start searching from.
   * @param str XPath string.
   * @param nsNode Node where namespace prefix in XPath is resolved from.
   * @return a NodeList of the matched result.
   *
   * @throws XPathException
   */
   public static NodeList selectNodeList(Node doc, String str, Node nsNode) throws XPathException {
       SimpleNamespaceContext nsctx = new SimpleNamespaceContext(nsNode);
       return selectNodeList(doc, str, nsctx);
   }

  /**
   * Use an XPath string to select a nodelist
   * Namespace prefix is resolved using the the specified context.
   *
   * @param doc The node to start searching from.
   * @param str XPath string.
   * @param nsctx Namespace where the prefix in XPath is resolved from.
   * @return a NodeList.
   *
   * @throws XPathException
   */
   public static NodeList selectNodeList(Node doc, String str, NamespaceContext nsctx) throws XPathException {
       XPathFactory xpf = xpathFactoryCache.getInstanceForCurrentThread();
       XPath xpath = xpf.newXPath();
       xpath.setNamespaceContext(nsctx);
       XPathExpression expr = xpath.compile(str);
       return (NodeList) expr.evaluate(doc,XPathConstants.NODESET);
   }

   /**
    * SimpleNamespaceContext implements just enough NamespaceContext functionality
    * for XPath.
    */
   static class SimpleNamespaceContext implements NamespaceContext {

       private Map<String, String> pfxmap = new HashMap<String, String>();

       private Map<String, List<String>> uri2pfxmap = new HashMap<String, List<String>>();

       public SimpleNamespaceContext(Node node) {
          pfxmap.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
          pfxmap.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
          addNamespaces(node);
       }

       public String getNamespaceURI(String prefix) {
          if (prefix == null) {
             throw new IllegalArgumentException();
          } else if (pfxmap.containsKey(prefix)) {
             return pfxmap.get(prefix);
          }
          return "";
       }

       public String getPrefix(String namespaceUri) {
          if (namespaceUri == null) {
             throw new IllegalArgumentException();
          }
          for (Map.Entry<String,String> entry : pfxmap.entrySet()) {
              if (entry.getValue().equals(namespaceUri)) {
                return entry.getKey();
              }
          }
          return null;
       }

       public Iterator<String> getPrefixes(String namespaceUri) {
          List<String> prefixes;
          if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
             prefixes = Collections.singletonList(XMLConstants.XML_NS_PREFIX);
          } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceUri)) {
             prefixes = Collections.singletonList(XMLConstants.XMLNS_ATTRIBUTE);
          } else {
             prefixes = uri2pfxmap.get(namespaceUri);
             if (prefixes == null) {
                prefixes = Collections.EMPTY_LIST;
             }
          }
          return prefixes.iterator();
       }

       private void addNamespaces(Node element) {
           if (element.getParentNode() != null) {
               addNamespaces(element.getParentNode());
           }
           if (element.getNodeType() == Node.DOCUMENT_NODE) {
              element = ((Document)element).getDocumentElement();
           }
           if (element instanceof Element) {
               Element el = (Element)element;
               NamedNodeMap nmap = el.getAttributes();
               for (int x = 0; x < nmap.getLength(); x++) {
                   Attr attr = (Attr)nmap.item(x);
                   if ("xmlns".equals(attr.getPrefix())) {
                       String name = attr.getLocalName();
                       String nsUri = attr.getValue();
                       pfxmap.put(name, nsUri);
                       List<String> list = uri2pfxmap.get(nsUri);
                       if (list == null) {
                          list = new ArrayList<String>();
                          uri2pfxmap.put(nsUri,list);
                       }
                       list.add(name);
                   }
               }
           }
       }
   }
}
