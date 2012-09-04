/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: XMLParser.java,v 1.4 2008/06/25 05:41:41 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.util;

import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * <p>
 * XMLParser provides a way for applications to handle a hook into applications
 * and applications and it's server.
 * </p>
 */

public class XMLParser {
    private Hashtable elemmap = new Hashtable();

    private boolean useGenericClass = false;
    private Map groupContainer;

    /*
     * static public void main(String argv[]) throws Exception { XMLParser wp =
     * new XMLParser(true); GenericNode n = (GenericNode) wp.parse(new
     * FileInputStream(argv[0])); System.out.println("FINAL:"+n.toString()); }
     */

    ParseOutput walkTree(Node nd) throws XMLException {
        Vector elements = new Vector();
        String pcdata = null;
        Hashtable atts = new Hashtable();
        NamedNodeMap nd_map = nd.getAttributes();
        if (nd_map != null) {
            for (int i = 0; i < nd_map.getLength(); i++) {
                Node att = nd_map.item(i);
                atts.put(att.getNodeName(), XMLUtils.unescapeSpecialCharacters(
                    att.getNodeValue()));
            }
        }
        for (Node ch = nd.getFirstChild(); ch != null; ch = ch.getNextSibling())
        {
            switch (ch.getNodeType()) {
            case Node.ELEMENT_NODE:
                elements.addElement(walkTree(ch));
                break;
            case Node.TEXT_NODE:
                String tmp = stripWhitespaces(XMLUtils.unescapeSpecialCharacters(
                    ch.getNodeValue()));
                if (tmp != null && tmp.length() != 0)
                    pcdata = tmp;
                break;
            default:
            // System.out.println("DBG: Ignoring :"+ch.getNodeType());
            }

        }
        // lookup hash
        String po_name = (String) elemmap.get(nd.getNodeName());
        ParseOutput po;
        if (po_name == null) {
            if (useGenericClass)
                po = new GenericNode();
            else
                throw new XMLException("No class registered for"
                        + nd.getNodeName());
        } else {
            try {
                po = (ParseOutput) Class.forName(po_name).newInstance();
            } catch (Exception ex) {
                StringBuilder buf = new StringBuilder();
                buf.append("Got Exception while creating class instance of ");
                buf.append(nd.getNodeName());
                buf.append(" :");
                buf.append(ex.toString());
                throw new XMLException(buf.toString());
            }
        }
        po.process(this, nd.getNodeName(), elements, atts, pcdata);
        return po;
    }

    String stripWhitespaces(String s) {
        return s.trim();
    }

    /**
     * <p>
     * The application have to implement Parseoutput interface when use
     * constructor without parameter.
     * </p>
     */
    public XMLParser() {
    }

    /**
     * <p>
     * Use default GenericNode as node type if usegeneric is true.
     * </p>
     * 
     * @param usegeneric
     */
    public XMLParser(boolean usegeneric, Map groupContainer) {
        useGenericClass = usegeneric;
        this.groupContainer = groupContainer;
    }

    /**
     * Parses an XML document from a String variable.
     */
    public Object parse(String s) throws XMLException {
        ByteArrayInputStream bin = null;
        String st = stripWhitespaces(s);
        try {
            bin = new ByteArrayInputStream(st.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new XMLException("Encoding not supported:" + ex.toString());
        }
        return parse(bin);
    }

    /**
     * <p>
     * Parse an XML document.
     * </p>
     */
    public Object parse(InputStream xmlin) throws XMLException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new XMLException("DBG:Got ParserConfigurationException:"
                    + e.toString());
        }

        Document doc = null;
        try {
            doc = db.parse(xmlin);
        } catch (SAXParseException e) {
            throw new XMLException("DBG:Got SAXParseException:" + e.toString()
                    + "line:" + e.getLineNumber() + " col :"
                    + e.getColumnNumber());
        } catch (SAXException e) {
            throw new XMLException("DBG:Got SAXException:" + e.toString());
        } catch (IOException ex) {
            throw new XMLException("DBG: Got IOException:" + ex.toString());
        }

        Element elem = doc.getDocumentElement();
        return (walkTree(elem));

    }

    /**
     * <p>
     * Register a call back function.
     * 
     * @param elemname
     *            is the tag name of this node
     * @param classname
     *            is the call back function
     */
    public void register(String elemname, String classname) {
        // TODO : add error checking : if classname exists, null, etc
        if (elemmap == null)
            elemmap = new Hashtable();
        elemmap.put(elemname, classname);
    }

    public Map getGroupContainer() {
        return groupContainer;
    }
}
