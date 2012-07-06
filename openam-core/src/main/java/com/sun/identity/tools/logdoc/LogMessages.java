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
 * $Id: LogMessages.java,v 1.2 2008/06/25 05:44:12 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.tools.logdoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Log Message IDs. It parses a Document Object Model of message IDs xml and
 * generates the HTML page.
 */
public class LogMessages {
    private String name;
    private String prefix;
    private List<LogMessage> messages = new ArrayList<LogMessage>();

    private LogMessages() {
    }

    LogMessages(String fileName, Element root)
        throws ParserConfigurationException, SAXException, IOException {
        name = getDisplayName(fileName);
        prefix = root.getAttribute("prefix");

        NodeList children = root.getElementsByTagName("logmessage");
        int sz = children.getLength();

        for (int i = 0; i < sz; i++) {
            messages.add(new LogMessage((Element)children.item(i)));

        }
    }

    String getName() {
        return name;
    }

    void generateHTMLFile()
        throws IOException {
        String page = LogHtmlTemplate.page.replaceAll("@NAME@", name);
        page = page.replaceAll("@ID_OPTIONS@", getOptions());
        page = page.replaceAll("@messages@", getMessagesHtml());
        LogMessagesFormatter.writeToFile(page, name + ".html");
    }

    private String getOptions() {
        StringBuilder buff = new StringBuilder();
        for (LogMessage m : messages) {
            buff.append(LogHtmlTemplate.option.replaceAll("@id@",
                prefix + "-" + m.getID()));
        }
        return buff.toString();
    }

    private String getMessagesHtml() {
        StringBuilder buff = new StringBuilder();
        for (LogMessage m : messages) {
            buff.append(m.getHTML(prefix));
        }
        return buff.toString();
    }

    private String getDisplayName(String fileName) {
        int idx = fileName.lastIndexOf('/');
        if (idx != -1) {
            fileName = fileName.substring(idx+1);
        }
        idx = fileName.indexOf(".xml");
        if (idx != -1) {
            fileName = fileName.substring(0, idx);
        }
        return fileName;
    }

    class LogMessage {
        private String name;
        private String id;
        private String description;
        private String level;
        private List<String> datainfos = new ArrayList<String>();
        private List<String> triggers = new ArrayList<String>();
        private List<String> actions = new ArrayList<String>();

        LogMessage(Element elm) {
            name = elm.getAttribute("name");
            id = elm.getAttribute("id");
            description = elm.getAttribute("description");
            level = elm.getAttribute("loglevel");

            if (level.startsWith("LL_")) {
                level = level.substring(3);
            }

            getItems(elm, "datainfo", datainfos);
            getItems(elm, "triggers", triggers);
            getItems(elm, "actions", actions);
        }

        String getID() {
            return id;
        }

        String getHTML(String prefix) {
            String html = LogHtmlTemplate.logmessage.replaceAll("@id@",
                prefix + "-" + id);
            html = html.replaceAll("@level@", level);
            html = html.replaceAll("@description@", description);
            html = html.replaceAll("@datainfos@", listToString(datainfos));
            html = html.replaceAll("@triggers@", listToString(triggers));
            html = html.replaceAll("@actions@", listToString(actions));
            return html;
        }

        private String listToString(List<String> list) {
            if (list.isEmpty()) {
                return "&nbsp;";
            }

            StringBuilder buff = new StringBuilder();
            for (String s : list) {
                buff.append(s).append("<br />");
            }
            return buff.toString();
        }

        private void getItems(Element elm, String name, List<String> list) {
            NodeList children = elm.getElementsByTagName(name);
            if (children.getLength() > 0) {
                Element node = (Element)children.item(0);
                NodeList iter = node.getElementsByTagName("item");
                int sz = iter.getLength();

                for (int i = 0; i < sz; i++) {
                    Element x = (Element)iter.item(i);
                    list.add(x.getFirstChild().getNodeValue());
                }
            }
        }
    }
}

