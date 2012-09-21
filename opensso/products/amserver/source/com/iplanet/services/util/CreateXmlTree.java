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
 * $Id: CreateXmlTree.java,v 1.3 2008/06/25 05:41:41 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.util;

import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.debug.Debug;

public class CreateXmlTree {

    private static DocumentBuilderFactory factory;

    private static DocumentBuilder db = null;

    static {
        Debug debug = Debug.getInstance("amXML");
        factory = DocumentBuilderFactory.newInstance();
        try {
            db = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            debug.error("XML Parser Configuration Error:", pce);
        }
    }

    /**
     * creates a single xml node and appends that node to the input xml
     * construct.
     */
    public static void createSingleNode(String key, String val, 
            StringBuffer xml) 
    {
        Document doc = db.newDocument();
        Element item = doc.createElement(key);
        item.appendChild(doc.createTextNode(val));
        xml.append(item.toString());
    }

    /**
     * creates a single xml node and return that node.
     */
    public static String createSingleNode(String key, String val) {
        Document doc = db.newDocument();
        Element item = doc.createElement(key);
        item.appendChild(doc.createTextNode(val));
        return (item.toString());
    }

    /**
     * creates a multiple xml nodes and appends those nodes to the input xml
     * construct.
     */
    public static void createMultiNodes(String key, Enumeration e,
            StringBuffer xml) {
        while (e.hasMoreElements()) {
            createSingleNode(key, (String) e.nextElement(), xml);
        }
    }

    /**
     * Parses attribute value, replaces " and ' with XML parser acceptable
     * strings.
     */

    public static String parseAttValue(String s) {
        if ((s == null) || (s.equals(""))) {
            return s;
        }
        char dquote = '\"';
        char quote = '\'';
        int i;

        StringBuilder sb = new StringBuilder(s);

        while ((i = s.indexOf(quote)) != -1) {
            sb.replace(i, i + 1, "&apos;");
            s = sb.toString();
        }

        while ((i = s.indexOf(dquote)) != -1) {
            sb.replace(i, i + 1, "&quot;");
            s = sb.toString();
        }

        return (s);
    }
}
