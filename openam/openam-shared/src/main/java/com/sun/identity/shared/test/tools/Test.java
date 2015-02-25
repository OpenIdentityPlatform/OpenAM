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
 * $Id: Test.java,v 1.2 2008/06/25 05:53:06 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.shared.test.tools;

import com.sun.identity.shared.xml.XMLUtils;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class is the test object in the report generating tool setup.
 * It is responsible for capturing the attributes of a test and then
 * generate HTML markup accordingly.
 */
public class Test {
    private String name;
    private int numberOfTests;
    private int numberOfFailures;
    private int numberOfErrors;
    private float timeTaken;
    private List<TestCase> testcases = new ArrayList<TestCase>();

    /**
     * Creates an instance of <code>Test</code>.
     *
     * @throws ParserConfigurationException if the <code>testNG</code>
     *         report XML is not well formed.
     * @throws SAXException if the <code>testNG</code> report XML is not 
     *         well formed.
     * @throws IOException if the <code>testNG</code> report XML is not 
     *         accessible.
     */
    public Test(File f)
        throws ParserConfigurationException, SAXException, IOException {
        parseXML(f);
    }

    /**
     * Returns <code>true</code> if the testcase succeeded.
     *
     * @return <code>true</code> if the testcase succeeded.
     */
    public boolean passed() {
        return getSkippedTestCases().isEmpty() &&
            getFailedTestCases().isEmpty();
    }

    /**
     * Returns a list of passed testcases.
     *
     * @return list of passed testcases.
     */
    public List<TestCase> getPassedTestCases() {
        List<TestCase> passed = new ArrayList<TestCase>();
        for (TestCase tc : testcases) {
            if (tc.passed()) {
                passed.add(tc);
            }
        }
        return passed;
    }

    /**
     * Returns a list of skipped testcases.
     *
     * @return list of skipped testcases.
     */
    public List<TestCase> getSkippedTestCases() {
        List<TestCase> skipped = new ArrayList<TestCase>();
        for (TestCase tc : testcases) {
            if (tc.skipped()) {
                skipped.add(tc);
            }
        }
        return skipped;
    }

    /**
     * Returns a list of failed testcases.
     *
     * @return list of failed testcases.
     */
    public List<TestCase> getFailedTestCases() {
        List<TestCase> failed = new ArrayList<TestCase>();
        for (TestCase tc : testcases) {
            if (tc.failed()) {
                failed.add(tc);
            }
        }
        return failed;
    }

    /**
     * Returns HTML markup for this object.
     *
     * @return HTML markup for this object.
     */
    public String toHTML() {
        List<TestCase> failed = getFailedTestCases();
        List<TestCase> passed = getPassedTestCases();
        List<TestCase> skipped = getSkippedTestCases();
        StringBuffer buff = new StringBuffer();

        for (TestCase tc : failed) {
            buff.append(tc.toHTML());
        }

        for (TestCase tc : passed) {
            buff.append(tc.toHTML());
        }

        for (TestCase tc : skipped) {
            buff.append(tc.toHTML());
        }

        Object[] params = {name, buff.toString()};
        return MessageFormat.format(HTMLConstants.TEST_TABLE, params);
    }

    private void parseXML(File f)
        throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilder builder = XMLUtils.getSafeDocumentBuilder(false);
        Document doc =  builder.parse(f);

        Element topElement = doc.getDocumentElement();
        name = topElement.getAttribute("name");
        numberOfTests = Integer.parseInt(topElement.getAttribute("tests"));
        numberOfFailures = Integer.parseInt(topElement.getAttribute(
            "failures"));
        numberOfErrors = Integer.parseInt(topElement.getAttribute("errors"));
        timeTaken = Float.parseFloat(topElement.getAttribute("time"));

        NodeList childElements = topElement.getChildNodes();
        int numChildElements = childElements.getLength();

        for (int i = 0; i < numChildElements; i++) {
            Node node = childElements.item(i);
            if ((node != null) &&  (node.getNodeType() == Node.ELEMENT_NODE)) {
                String elementName = node.getNodeName();

                if (elementName.equals("testcase")) {
                    testcases.add(new TestCase(node));
                }
            }
        }
    }
}
