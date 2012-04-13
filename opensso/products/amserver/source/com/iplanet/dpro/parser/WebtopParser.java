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
 * $Id: WebtopParser.java,v 1.3 2008/06/25 05:41:29 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.parser;


import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

/**
 * XMLParser provides a way for applications to handle a hook into
 * applications and applications and its server.
 *
 * @supported.all.api
 */
public class WebtopParser {
    private Hashtable elemmap = new Hashtable();
    private boolean useGenericClass = false;

    static public void main(String argv[]) throws Exception {
        WebtopParser wp = new WebtopParser(true);
        GenericNode n = (GenericNode) wp.parse(new FileInputStream(argv[0]));
        System.out.println("FINAL:"+n.toString());
    }

    /**
     * Parses and processes a Node.
     *
     * @param nd The Node to parse.
     * @return the parsed object for the node.
     * @throws Exception if node cannot be parsed or <code>ParseOutput</code>
     *         object cannot be instantiated.
     */
    ParseOutput walkTree(Node nd)
        throws Exception
    {
        Vector elements = new Vector();
        Vector retelements;
        String pcdata = null;
        Hashtable atts = new Hashtable();
        NamedNodeMap nd_map = nd.getAttributes();
        if (nd_map != null) {
            for (int i=0; i < nd_map.getLength(); i++) {
                Node att = nd_map.item(i);
                atts.put(att.getNodeName(), att.getNodeValue());
            }
        }
        for (Node ch = nd.getFirstChild();
        ch != null; ch = ch.getNextSibling()) {
            switch (ch.getNodeType()) {
                case Node.ELEMENT_NODE :
                    elements.addElement(walkTree(ch));
                    break;
                case Node.TEXT_NODE :
                    String tmp  = stripWhitespaces(ch.getNodeValue());
                    if (tmp != null && tmp.length() !=0) {
                        pcdata = tmp;
                    }
                    break;
                default :
            }
        }
        // lookup hash
        String po_name = (String) elemmap.get(nd.getNodeName());
        ParseOutput po;
        if (po_name == null) {
            if (useGenericClass) {
                po = (ParseOutput) new GenericNode();
            } else {
                throw new Exception("No class registered for"+nd.getNodeName());
            }
        } else {
            try {
                po = (ParseOutput) Class.forName(po_name).newInstance();
            } catch(Exception ex) {
                StringBuilder buf = new StringBuilder();
                buf.append("Got Exception while creating class instance of ");
                buf.append(nd.getNodeName());
                buf.append(" :");
                buf.append(ex.toString());
                throw new Exception(buf.toString());
            }
        }
        po.process(nd.getNodeName(), elements, atts, pcdata);
        return po;
    }

    /**
     * Removes leading and trailing whitespace.
     *
     * @param s String from which to remove whitespace.
     * @return String with leading and trailing whitespace removed.
     */
    String stripWhitespaces(String s) {
        return s.trim();
    }

    /**
     * Constructs a <code>WebtopParser</code> instance.
     */
    public WebtopParser() {
    }

    /**
     * Sets whether to use the default <code>GenericNode</code> as the node
     * type.
     *
     * @param usegeneric <code>true</code> if <code>GenericNode</code> type is
     *        to be used.
     */
    public WebtopParser(boolean usegeneric) {
        useGenericClass = usegeneric;
    }

    /**
     * Parses an XML document from a String variable.
     *
     * @param s The XML document.
     * @throws Exception if there are unsupported encoding issues.
     */
    public Object parse(String s) throws Exception {
        ByteArrayInputStream bin = null;
        String st = stripWhitespaces(s);
        try {
            bin = new ByteArrayInputStream(st.getBytes("UTF-8"));
        } catch(UnsupportedEncodingException ex) {
            throw new Exception("Encoding not supported:" + ex.toString());
        }
        return parse(bin);
    }

    /**
     * Parses an XML document.
     *
     * @param xmlin the XML document.
     * @return the ParseOutput object from walking and processing the node.
     * @throws Exception if there are IO or XML parsing exceptions.
     */
    public Object parse(InputStream xmlin)
        throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new Exception("DBG:Got ParserConfigurationException:" +
                e.toString());
        }

        Document doc = null;
        try {
            doc = db.parse(xmlin);
        } catch(SAXParseException e) {
            throw new Exception("DBG:Got SAXParseException:" +
                e.toString() + "line:" +e.getLineNumber() +
                " col :" + e.getColumnNumber());
        } catch (SAXException e) {
            throw new Exception("DBG:Got SAXException:" +e.toString());
        } catch (IOException ex) {
            throw new Exception("DBG: Got IOException:" + ex.toString());
        }

        Element elem = doc.getDocumentElement();
        return(walkTree(elem));
    }

    /**
     * Registers a call back function.
     *
     * @param elemname The tag name of this node.
     * @param classname The call back function.
     */
    public void register(String elemname, String classname) {
        if (elemmap == null) {
            elemmap = new Hashtable();
        }
        elemmap.put(elemname, classname);
    }
}

