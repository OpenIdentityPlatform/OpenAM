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
 * $Id: LogMessageID.java,v 1.6 2008/08/27 22:08:38 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.messageid;

import com.sun.identity.log.spi.Debug;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Log Message ID is a unique identifier for each log message.
 */
public class LogMessageID {
    private Level logLevel;
    private String prefix;
    private int id;
    private String name;
    private String description;
    private int dataInfo;

    private static Map mapLogLevel = new HashMap();

    static {
        mapLogLevel.put("LL_CONFIG", Level.CONFIG);
        mapLogLevel.put("LL_FINE", Level.FINE);
        mapLogLevel.put("LL_FINER", Level.FINER);
        mapLogLevel.put("LL_FINEST", Level.FINEST);
        mapLogLevel.put("LL_INFO", Level.INFO);
        mapLogLevel.put("LL_SEVERE", Level.SEVERE);
        mapLogLevel.put("LL_ALL", Level.ALL);
    }

    /**
     * Constructs a log message ID instance.
     *
     * @param logLevel Log level.
     * @param prefix Prefix of this log.
     * @param id Unique Identification number.
     * @param name Unique name.
     * @param description Description of this log.
     * @param dataInfo Information on the data logged.
     */
    public LogMessageID(
        Level logLevel,
        String prefix,
        int id,
        String name,
        String description,
        int dataInfo
    ) {
        this.logLevel = logLevel;
        this.prefix = prefix;
        this.id = id;
        this.name = name;
        this.description = description;
        this.dataInfo = dataInfo;
    }

    /**
     * Returns log level.
     *
     * @return log level.
     */
    public Level getLogLevel() {
        return logLevel;
    }

    /**
     * Returns prefix.
     *
     * @return prefix.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns name.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns id.
     *
     * @return name.
     */
    public int getID() {
        return id;
    }

    /**
     * Returns description.
     *
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns number of entries in the data column.
     *
     * @return number of entries in the data column.
     */
    public int getNumberOfEntriesInDataColumn() {
        return dataInfo;
    }

    static LogMessageID createInstance(String prefix, Node node) {
        LogMessageID messageID = null;

        if ((node != null) &&  (node.getNodeType() == Node.ELEMENT_NODE)) {
            String nodeName = node.getNodeName();

            if (nodeName.equals(LogMessageConstants.XML_LOG_MESSAGE_TAG_NAME)){
                String name = ((Element)node).getAttribute(
                    LogMessageConstants.XML_ATTRNAME_LOG_MESSAGE_NAME);
                String id = ((Element)node).getAttribute(
                    LogMessageConstants.XML_ATTRNAME_LOG_MESSAGE_ID);
                String logLevel = ((Element)node).getAttribute(
                    LogMessageConstants.XML_ATTRNAME_LOG_LEVEL);
                String description = ((Element)node).getAttribute(
                    LogMessageConstants.XML_ATTRNAME_LOG_MESSAGE_DESCRIPTION);

                if ((name.length() > 0) && (id.length() > 0)) {
                    try {
                        messageID = new LogMessageID(
                            (Level)mapLogLevel.get(logLevel), prefix,
                            Integer.parseInt(id), name, description,
                            getArrayCount(node,
                                LogMessageConstants.XML_DATAINFO_TAG_NAME));
                    } catch (NumberFormatException e) {
                        Debug.error("LogMessageID.createInstance", e);        
                    }
                } else {
                    Debug.error("LogMessageID.createInstance: " +
                 "unable to create log message ID because its name is missing");
                }
            }
        }

        return messageID;
    }

    private static int getArrayCount(Node node, String tagName) {
        int count = 0;
        Node target = getNodeOfName(node, tagName);

        if (target != null) {
            NodeList items = target.getChildNodes();
            int sz = items.getLength();

            for (int i = 0; i < sz; i++) {
                Node item = items.item(i);
                if (item.getNodeName().equals("item")) {
                    count++;
                }
            }
        }

        return count;
    }

    private static Node getNodeOfName(Node node, String tagName) {
        Node target = null;
        NodeList nodelist = node.getChildNodes();

        if (nodelist != null) {
            int sz = nodelist.getLength();

            for (int i = 0; (i < sz) && (target == null); i++) {
                Node item = nodelist.item(i);
                if (item.getNodeName().equals(tagName)) {
                    target = item;
                }
            }
        }

        return target;
    }
}
