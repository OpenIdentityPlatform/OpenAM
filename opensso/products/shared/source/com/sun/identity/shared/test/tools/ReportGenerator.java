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
 * $Id: ReportGenerator.java,v 1.2 2008/06/25 05:53:06 qcheng Exp $
 *
 */

package com.sun.identity.shared.test.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * This class generates HTML report based on <code>testNG</code> result XML
 * file.
 */
public class ReportGenerator {
    private String dataDir;
    private String templateDir;
    private String outputDir;
    private List<Suite> suitesPassed = new ArrayList<Suite>();
    private List<Suite> suitesFailed = new ArrayList<Suite>();

    /**
     * Creates an instance of <code>ReportGenerator</code> object.
     *
     * @param dataDir Directory where code>testNG</code> result XML resides.
     * @param templateDir Directory where HTML templates reside.
     * @param outputDir Directory to place the HTML reports.
     */
    public ReportGenerator(
        String dataDir,
        String templateDir,
        String outputDir
    ) {
        this.dataDir = dataDir;
        this.templateDir = templateDir;
        this.outputDir = outputDir;
    }

    /**
     * Creates HTML report.
     *
     * @throws ParserConfigurationException if the <code>testNG</code>
     *         report XML is not well formed.
     * @throws SAXException if the <code>testNG</code> report XML is not 
     *         well formed.
     * @throws IOException if the <code>testNG</code> report XML is not 
     *         accessible.
     */
    public void createHTMLReports()
        throws ParserConfigurationException, SAXException, IOException
    {
        Set<File> directories = getDirectories();
        for (File f : directories) {
            Suite suite = new Suite(f);
            if (suite.passed()) {
                suitesPassed.add(suite);
            } else {
                suitesFailed.add(suite);
            }
        }
        genMainPage();
    }

    private void genMainPage()
        throws IOException {
        String templateIndex = getFileContent(templateDir + "/index.html");
        StringBuffer buff = new StringBuffer();

        for (Suite s : suitesFailed) {
            buff.append(s.toHTML());
            s.createHTMLReports(templateDir, outputDir);
        }
        for (Suite s : suitesPassed) {
            buff.append(s.toHTML());
            s.createHTMLReports(templateDir, outputDir);
        }
        writeToFile(templateIndex.replaceAll("<!-- entry -->", buff.toString()),
            outputDir + "/index.html");
    }

    private Set<File> getDirectories() {
        Set<File> directories = new TreeSet<File>();
        File oDir = new File(dataDir);
        File[] files = oDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (!f.getName().startsWith(".") && f.isDirectory()) {
                directories.add(f);
            }
        }
        return directories;
    }

    /**
     * Writes content to a file.
     *
     * @param content Content to be written.
     * @param fileName Name of file of which content is written to.
     * @throws IOException if content cannot be written.
     */
    public static void writeToFile(String content, String fileName)
        throws IOException {
        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
            fw.write(content, 0, content.length());
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    /**
     * Returns content of a file.
     *
     * @param fileName Name of the file.
     * @return Content of a file.
     * @throws IOException if connect cannot be read.
     */
    public static String getFileContent(String fileName)
        throws IOException {
        BufferedReader br = null;
        StringBuffer buff = new StringBuffer();
        try {
            br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            while (line != null) {
                buff.append(line).append("\n");
                line = br.readLine();
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return buff.toString();
    }

    public static void main(String[] args) {
        ReportGenerator gen = new ReportGenerator(args[0], args[1], args[2]);
        try {
            gen.createHTMLReports();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
