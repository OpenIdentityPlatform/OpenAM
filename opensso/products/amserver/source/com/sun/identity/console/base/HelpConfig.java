/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HelpConfig.java,v 1.3 2008/06/25 05:42:48 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.iplanet.jato.RequestManager;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.shared.debug.Debug;

/**
 * Helper class for online JavaHelp.
 */
public class HelpConfig {
    public static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);
    
    /** Context path to the JavaHelp helpset file */
    private static final String HELP_SET_FILE = "/html/en/help/app.hs";

    /** Help files' element and attribute names */
    private static final String ELEM_MAPREF = "mapref";
    private static final String ELEM_SUBHELPSET = "subhelpset";
    private static final String ELEM_MAPID = "mapID";
    private static final String ATTR_LOCATION = "location";
    private static final String ATTR_TARGET = "target";
    private static final String ATTR_URL = "url";
    
    private static HelpConfig instance = new HelpConfig();
    private static EntityResolver ignoreDtdResolver = new DefaultHandler() {
        public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException
        {
            if (systemId!=null) {
                systemId = systemId.trim();
                if (systemId.endsWith(".dtd") || systemId.endsWith(".DTD")) {
                    return new InputSource(new StringReader(""));
                }
            }
            try {
                return super.resolveEntity(publicId, systemId);
            } catch (Exception e) {
                if (!(e instanceof SAXException)) {
                    throw new SAXException(e);
                } else {
                    throw (SAXException)e;
                }
            }
        }
    };
    
    /**
     * Memory caches for help filenames and anchors with JavaHelp IDs as keys.
     */
    private Map helpFileNameMap = new HashMap();
    private Map helpAnchorMap = new HashMap();

    private String title = "";

    private DocumentBuilder builder;
    
    /** Creates a new instance of HelpConfig */
    private HelpConfig() {
        init();
    }
    
    private void init() {
        try {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            // Disable external DTD in xerces parser
            factory.setAttribute(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                Boolean.FALSE);
            builder = factory.newDocumentBuilder();
            // Disable external DTD in crimson parser
            builder.setEntityResolver(ignoreDtdResolver);
        } catch (ParserConfigurationException pce) {
            debug.error("HelpConfig.init : " , pce);
            return;
        } catch (IllegalArgumentException iae) {
            debug.error("HelpConfig.init : " , iae);
            return;
        } catch (Exception e) {
            debug.error("HelpConfig.init : " , e);
            return;
        }
        ServletContext context =
            RequestManager.getRequestContext().getServletContext();
        URL hs = null;
        try {
            hs = context.getResource(HELP_SET_FILE);
        } catch (MalformedURLException muex) {
            debug.error(
               "HelpConfig.init ServletContext path in incorrect form: " +
                HELP_SET_FILE);
            return;
        }
        if (hs != null) {
            parseHelpSetFile(hs);
        } else {
            debug.error(
              "HelpConfig.init Cannot find HelpSet file on context path: " +
                HELP_SET_FILE);
            return;
        }
    }
    
    private void parseHelpSetFile(URL hs) {
        Document hsDoc;
        try {
            InputStream hsIn = hs.openStream();
            hsDoc = builder.parse(hsIn);
        } catch (IOException ioe) {
            debug.error("HelpConfig.parseHelpSetFile URL=" + hs, ioe);
            return;
        } catch (SAXException saxe) {
            debug.error("HelpConfig.parseHelpSetFile URL=" + hs, saxe);
            return;
        } catch (IllegalArgumentException iae) {
            debug.error("HelpConfig.parseHelpSetFile URL=" + hs, iae);
            return;
        }

        // parse mapref's
        NodeList maprefs = hsDoc.getElementsByTagName(ELEM_MAPREF);
        if (maprefs != null) {
            for(int i=0; i<maprefs.getLength(); i++) {
                Node node = maprefs.item(i);
                if (node instanceof Element) {
                    String mapref = ((Element)node).getAttribute(ATTR_LOCATION);
                    try {
                        URL jhm = new URL(hs, mapref);
                        parseHelpMap(jhm);
                    } catch(MalformedURLException mue) {
                        debug.warning (
                            "HelpConfig.parseHelpSetFile Invalid mapref, " +
                             mapref, mue);
                        // continue on next
                    }
                } else {
                    debug.warning(
                        "HelpConfig.parseHelpSetFile HS file format error, " +
                        "mapref should be element.");
                    // continue anyway
                }
            }
        }
        
        // parse subhelpset's
        NodeList subhelpsets = hsDoc.getElementsByTagName(ELEM_SUBHELPSET);
        if (subhelpsets != null) {
            for(int i=0; i<subhelpsets.getLength(); i++) {
                Node node = subhelpsets.item(i);
                if (node instanceof Element) {
                    String subhelpset =
                        ((Element)node).getAttribute(ATTR_LOCATION);
                    try {
                        URL subhs = new URL(hs,  subhelpset);
                        parseHelpSetFile(subhs);
                    } catch(MalformedURLException mue) {
                        debug.warning(
                            "HelpConfig.parseHelpSetFile Invalid subhelpset, " +
                             subhelpset, mue);
                        // continue on next
                    }
                } else {
                    debug.warning(
                        "HelpConfig.parseHelpSetFile HS file format error, " +
                        "subhelpset should be element.");
                    // continue anyway
                }
            }
        }
    }
    
    private void parseHelpMap(URL jhm) {
        Document jhmDoc;
        try {
            InputStream jhmIn = jhm.openStream();
            jhmDoc = builder.parse(jhmIn);
        } catch (IOException ioe) {
            debug.error("HelpConfig.parseHelpMap", ioe);
            return;
        } catch (SAXException saxe) {
            debug.error("HelpConfig.parseHelpMap", saxe);
            return;
        } catch (IllegalArgumentException iae) {
            debug.error("HelpConfig.parseHelpMap", iae);
            return;
        }
        NodeList mapIDs = jhmDoc.getElementsByTagName(ELEM_MAPID);
        if (mapIDs != null) {
            for(int i=0; i<mapIDs.getLength(); i++) {
                Node node = mapIDs.item(i);
                if (node instanceof Element) {
                    Element mapID = (Element)node;
                    String target = mapID.getAttribute(ATTR_TARGET);
                    String url = mapID.getAttribute(ATTR_URL);
                    int hash = url.indexOf('#');
                    if (hash == -1) {
                        helpFileNameMap.put(target,url);
                    } else {
                        helpFileNameMap.put(target, url.substring(0,hash));
                        if (hash < url.length()-1) {
                            helpAnchorMap.put(target, url.substring(hash+1));
                        }
                    }
                } else {
                    debug.error(
                      "HelpConfig.parseHelpMap HelpMap file format error, " +
                      "mapID should be element.");
                }
            }
        }
    }
    
    public static HelpConfig getInstance() {
        return instance;
    }
    
    public String getHelpFileName(String helpID) {
        return (String)helpFileNameMap.get(helpID);
    }
    
    public String getHelpAnchor(String helpID) {
        return (String)helpAnchorMap.get(helpID);
    }
    
}
