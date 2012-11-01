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
 * $Id: LogMessagesFormatter.java,v 1.2 2008/06/25 05:44:12 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.tools.logdoc;

import com.sun.identity.shared.xml.XMLUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Generate html page for log message IDs. It takes an array of directories
 * which contains the log message IDs xml files; parses them; and generate
 * the HTML page.
 */
public class LogMessagesFormatter {
    private static FilenameFilter xmlFilter;
    static String htmlDir = System.getProperty("opensso.log.html.dir");
    private List<LogMessages> logMessages = new ArrayList<LogMessages>();

    static {
        xmlFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        };
    }

    private LogMessagesFormatter() {
    }

    private LogMessagesFormatter(Set<String> files)
        throws ParserConfigurationException, SAXException, IOException {
        for (String fileName : files ) {
            parseXML(fileName);
        }
        generateCSSFiles();
        generateHTMLFiles();
    }

    private void generateCSSFiles()
        throws IOException {
        writeToFile(LogHtmlTemplate.css_ie5win, "css_ie5win.css");
        writeToFile(LogHtmlTemplate.css_ie6up, "css_ie6up.css");
        writeToFile(LogHtmlTemplate.css_ns4sol, "css_ns4sol.css");
        writeToFile(LogHtmlTemplate.css_ns4win, "css_ns4win.css");
        writeToFile(LogHtmlTemplate.css_ns6up, "css_ns6up.css");
        writeToFile(LogHtmlTemplate.stylesheetjs, "stylesheet.js");
        writeToFile(LogHtmlTemplate.browserjs, "browserVersion.js");
    }

    private void generateHTMLFiles()
        throws IOException {
        StringBuilder buff = new StringBuilder();
        for (LogMessages lm : logMessages) {
            String htmlpage = lm.getName() + ".html";
            String link = LogHtmlTemplate.indexLink.replaceAll(
                "@htmlpage@", htmlpage);
            link = link.replaceAll("@name@", lm.getName());
            buff.append(link);
            lm.generateHTMLFile();
        }

        String indexPage = LogHtmlTemplate.indexPage.replaceAll("@indices@",
            buff.toString());
        writeToFile(indexPage, "index.html");
    }

    static void writeToFile(String content, String fileName) 
        throws IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(
                htmlDir + "/" + fileName));
            out.write(content);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void parseXML(String fileName) 
        throws ParserConfigurationException, SAXException, IOException {
        String xml = stripDocType(fileName);
        ByteArrayInputStream bis = null;

        try {
            bis = new ByteArrayInputStream(xml.getBytes());
            DocumentBuilder db = XMLUtils.getSafeDocumentBuilder(false);
            Document dom = db.parse(bis);

            Element rootElm = dom.getDocumentElement();
            logMessages.add(new LogMessages(fileName, rootElm));
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }

    private static String stripDocType(String fileName)
        throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(new
                File(fileName))); 
            StringBuilder buff = new StringBuilder();
            String line = reader.readLine();

            while (line != null) {
                buff.append(line);
                line = reader.readLine();
            }
            return buff.toString().replaceAll("<!DOCTYPE .*?>", "");
        } finally {
            if (reader != null){
                reader.close();
            }
        }

    }

    /**
     * Takes an array of directories where message IDs xml resides.
     *
     * @param args Array of directories where message IDs xml resides.
     */
    public static void main(String[] args) {
        try {
            Set<String> files = new TreeSet<String>();
            for (int i = 0; i < args.length; i++) {
                getXMLFiles(args[i], files);
            }
            new LogMessagesFormatter(files);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getXMLFiles(String dir, Set<String> files)
        throws IOException {
        File directory = new File(dir);
        String[] children = directory.list(xmlFilter);
        if (children != null) {
            for (int i= 0; i < children.length; i++) {
                files.add(dir + "/" + children[i]);
            }
        }
    }
}

