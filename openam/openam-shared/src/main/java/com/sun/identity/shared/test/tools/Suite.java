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
 * $Id: Suite.java,v 1.2 2008/06/25 05:53:06 qcheng Exp $
 *
 */

package com.sun.identity.shared.test.tools;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * This class is the suite object in the report generating tool setup.
 * It is responsible for capturing the attributes of a suite and then
 * generate HTML markup accordingly.
 */
public class Suite {
    private String name;
    private List<Test> tests = new ArrayList<Test>();

    /**
     * Creates an instance of <code>Suite</code>.
     *
     * @throws ParserConfigurationException if the <code>testNG</code>
     *         report XML is not well formed.
     * @throws SAXException if the <code>testNG</code> report XML is not 
     *         well formed.
     * @throws IOException if the <code>testNG</code> report XML is not 
     *         accessible.
     */
    public Suite(File f)
        throws ParserConfigurationException, SAXException, IOException {
        name = f.getName();
        getXMLFiles(f);
    }

    /**
     * Returns a list of <code>Test</code> object in this suite.
     *
     * @return list of <code>Test</code> object in this suite.
     */
    public List<Test> getTests() {
        return tests;
    }

    /**
     * Returns a list of passed <code>Test</code> object in this suite.
     *
     * @return list of passed <code>Test</code> object in this suite.
     */
    public boolean passed() {
        boolean passed = true;
        for (Iterator i = tests.iterator(); i.hasNext() && passed; ) {
            Test t = (Test)i.next();
            passed = t.passed();
        }
        return passed;
    }

    /**
     * Creates HTML report.
     *
     * @param templateDir Template directory.
     * @param outputDir Output directory where reports are to be created 
     *        under.
     * @throws IOException if report cannot be created.
     */
    public void createHTMLReports(String templateDir, String outputDir)
        throws IOException
    {
        String templateHTML = ReportGenerator.getFileContent(
            templateDir + "/suite.html");
        String dirName = outputDir + "/" + name;
        File dir = new File(dirName);
        dir.mkdir();
        StringBuffer buff = new StringBuffer();
        
        for (Test t : tests) {
            buff.append(t.toHTML());
        }

        templateHTML = templateHTML.replaceAll("@Test@", buff.toString());
        templateHTML = templateHTML.replaceAll("@SuiteName@", name);
        ReportGenerator.writeToFile(templateHTML, dirName + "/index.html");
    }

    private int getNumberOfPasses() {
        int result = 0;
        for (Test t : tests) {
            result += t.getPassedTestCases().size();
        }
        return result;
    }

    private int getNumberOfFailed() {
        int result = 0;
        for (Test t : tests) {
            result += t.getFailedTestCases().size();
        }
        return result;
    }

    private int getNumberOfSkipped() {
        int result = 0;
        for (Test t : tests) {
            result += t.getSkippedTestCases().size();
        }
        return result;
    }

    /**
     * Returns HTML markup for this object.
     *
     * @return HTML markup for this object.
     */
    public String toHTML() {
        StringBuffer buff = new StringBuffer();
        boolean passed = passed();
        String titleTag = (passed) ? "tblMainPassedSuite" :"tblMainFailedSuite";
        String countTag = (passed) ? "tblMainPassed" : "tblMainFailed";

        String urlName = name + "/index.html";
        Object[] args = {urlName, name};
        String hrefName = MessageFormat.format(HTMLConstants.HREF, args);

        Object[] params = {titleTag, hrefName};
        buff.append("<tr>");
        buff.append(MessageFormat.format(HTMLConstants.TBL_ENTRY, params));
        params[0] = countTag;
        params[1] = Integer.toString(getNumberOfPasses());
        buff.append(MessageFormat.format(HTMLConstants.TBL_ENTRY, params));
        params[1] = Integer.toString(getNumberOfFailed());
        buff.append(MessageFormat.format(HTMLConstants.TBL_ENTRY, params));
        params[1] = Integer.toString(getNumberOfSkipped());
        buff.append(MessageFormat.format(HTMLConstants.TBL_ENTRY, params));
        buff.append("</tr>");
        return buff.toString();
    }

    private void getXMLFiles(File suite)
        throws ParserConfigurationException, SAXException, IOException {
        Set<File> xmlFiles = new TreeSet<File>(new FileNameComparator());
        File[] files = suite.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = f.getName();
            if (name.endsWith(".xml") && !name.equals("testng-failed.xml")) {
                xmlFiles.add(f);
            }
        }

        for (File f : xmlFiles) {
            tests.add(new Test(f));
        }
    }
}
