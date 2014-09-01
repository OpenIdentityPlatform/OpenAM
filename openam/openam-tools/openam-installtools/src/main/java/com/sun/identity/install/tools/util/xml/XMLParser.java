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
 * $Id: XMLParser.java,v 1.2 2008/06/25 05:51:32 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;

/**
 * This is a simple parser that can be used to parse and create in-memory
 * DOM-like representation of an xml document. The logic for this parser is
 * based on an adhoc implementation which requires that the XML document be 
 * well formed syntactically and does not do any further validation of any 
 * type.
 */
public class XMLParser implements IXMLUtilsConstants {

    /**
     * This method returns an ordered colloection of tokens that can together
     * represent the entire XML document. This method is used as the first 
     * phase parsing or scanning of the XML document.
     * 
     * @param isreader
     *            the reader attached to the source of the document.
     * 
     * @return an ordered collection of <code>Token</code>s that together
     *         represent the entire XML document.
     * 
     * @throws Exception
     *             if the parse was not successful
     */
    ArrayList parse(Reader isreader) throws Exception {
        ArrayList result = new ArrayList();
        StringBuffer buff = new StringBuffer();
        int boundCount = 0;
        boolean inComment = false;
        boolean inCompoundToken = false;
        boolean inQuotes = false;
        int length;
        char[] cbuf = new char[1];
        BufferedReader in = new BufferedReader(isreader);
        while ((length = in.read(cbuf, 0, 1)) != -1) {
            char ch = cbuf[0];
            if (ch == '"') {
                if ((buff.length() != 0) && buff.toString().endsWith("\\")) {
                    buff.append(ch);
                    continue;
                } else {
                    inQuotes = !inQuotes;
                    buff.append(ch);
                    continue;
                }
            }
            if (inQuotes) {
                buff.append(ch);
                continue;
            }
            if (ch == '<') {
                if (inComment) {
                    buff.append(ch);
                } else if (boundCount == 0) {
                    if (buff.length() > 0) {
                        result.add(getXMLTokenFactory().getToken(
                                buff.toString()));
                        buff.delete(0, buff.length());
                    }
                    buff.append(ch);
                    boundCount++;
                } else if (boundCount > 0) {
                    buff.append(ch);
                    boundCount++;
                } else {
                    throw new Exception("Invalid boundCount: " + boundCount);
                }
            } else if (ch == '>') {
                if (buff.length() >= 1) {
                    if (!inComment) {
                        buff.append(ch);
                        boundCount--;
                        if (boundCount == 0) {
                            result.add(getXMLTokenFactory().getToken(
                                    buff.toString()));
                            buff.delete(0, buff.length());
                        }
                        continue;
                    }
                    if (buff.length() > 1
                            && buff.substring(buff.length() - 2, buff.length())
                                    .equals("--")) {
                        buff.append(ch);
                        if (inComment) {
                            boundCount--;
                            inComment = false;
                        }
                        if (boundCount == 0) {
                            result.add(getXMLTokenFactory().getToken(
                                    buff.toString()));
                            buff.delete(0, buff.length());
                        }
                    } else {
                        buff.append(ch);
                    }
                } else {
                    throw new Exception("Char > in begining of token");
                }
            } else if (ch == '-') {
                if (buff.length() > 2 && buff.toString().startsWith("<!-")) {
                    if (!inComment) {
                        inComment = true;
                    }
                }
                buff.append(ch);
            } else {
                buff.append(ch);
            }
        }
        if (inQuotes) {
            throw new Exception("unbalanced quote encountered");
        }
        if (boundCount > 0) {
            throw new Exception("unbalanced token encountered: boundCount = "
                    + boundCount);
        } else if (boundCount < 0) {
            throw new Exception(" invalid boundCount: " + boundCount);
        }
        if (buff.length() > 0) {
            result.add(getXMLTokenFactory().getToken(buff.toString()));
        }
        in.close();
        return result;
    }

    /**
     * This method is used as the second phase parser to create an ordered
     * collection of attributes from a given attribute string.
     * 
     * @param attributeString
     *            the attribute string fragment to be parsed
     * @return an ordered collection of attributes
     * 
     * @throws Exception
     *             if the parse was not successful
     */
    ArrayList parseAttributes(String attributeString) throws Exception {
        ArrayList result = null;
        if (attributeString != null && attributeString.trim().length() > 0) {
            result = new ArrayList();
            boolean inQuotes = false;
            boolean inName = true;
            boolean inValue = false;
            StringBuffer buff = new StringBuffer();
            String name = null;
            String value = null;
            for (int i = 0; i < attributeString.length(); i++) {
                char ch = attributeString.charAt(i);
                if (ch == '"') {
                    if ((buff.length() != 0) && buff.toString().endsWith("\\"))
                    {
                        buff.append(ch);
                        continue;
                    } else {
                        inQuotes = !inQuotes;
                        buff.append(ch);
                        continue;
                    }
                }
                if (inQuotes) {
                    buff.append(ch);
                    continue;
                }
                if (ch == ' ') {
                    if (inValue) {
                        value = buff.toString().trim();
                        buff.delete(0, buff.length());
                        if (name == null || name.trim().length() == 0) {
                            throw new Exception("Failed to parse attribute: "
                                    + attributeString);
                        }
                        result.add(new XMLElementAttribute(name, value));
                        name = null;
                        value = null;
                        inValue = false;
                        inName = true;
                        continue;
                    }
                    if (inName) {
                        continue;
                    }
                }
                if (ch == '=') {
                    if (inName) {
                        name = buff.toString();
                        buff.delete(0, buff.length());
                        inName = false;
                        inValue = true;
                        continue;
                    }
                }
                buff.append(ch);
            }
            if (buff.length() > 0) {
                if (!inValue) {
                    throw new Exception("Failed to parse attributes: "
                            + attributeString);
                }
                if (name == null || name.trim().length() == 0) {
                    throw new Exception("Failed to parse attribute: "
                            + attributeString);
                }
                value = buff.toString().trim();
                result.add(new XMLElementAttribute(name, value));
            }
        }
        return result;
    }

    XMLParser() {
        setXMLTokenFactory(new XMLTokenFactory());
    }

    int getNextTokenIndex() {
        return this.getXMLTokenFactory().getNextTokenIndex();
    }

    private void setXMLTokenFactory(XMLTokenFactory xmlTokenFactory) {
        this.xmlTokenFactory = xmlTokenFactory;
    }

    private XMLTokenFactory getXMLTokenFactory() {
        return xmlTokenFactory;
    }

    private XMLTokenFactory xmlTokenFactory;
}
