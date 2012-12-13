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
 * $Id: TestCase.java,v 1.2 2008/06/25 05:53:06 qcheng Exp $
 *
 */

package com.sun.identity.shared.test.tools;

import java.text.MessageFormat;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is the testcase object in the report generating tool setup.
 * It is responsible for capturing the attributes of a testcase and then
 * generate HTML markup accordingly.
 */
public class TestCase {
    private String name;
    private String classname;
    private float timeTaken;
    private Failure failure;
    private boolean skipped;

    /**
     * Creates an instance of <code>TestCase</code>
     */
    public TestCase(Node node) {
        parseNode(node);
    }
    
    /**
     * Returns <code>true</code> if the testcase succeeded.
     *
     * @return <code>true</code> if the testcase succeeded.
     */
    public boolean passed() {
        return !failed() && !skipped();
    }

    /**
     * Returns <code>true</code> if the testcase failed.
     *
     * @return <code>true</code> if the testcase failed.
     */
    public boolean failed() {
        return (failure != null);
    }

    /**
     * Returns <code>true</code> if the testcase is skipped.
     *
     * @return <code>true</code> if the testcase is skipped.
     */
    public boolean skipped() {
        return skipped;
    }

    /**
     * Returns HTML markup for this object.
     *
     * @return HTML markup for this object.
     */
    public String toHTML() {
        String htmlID = "tblTestPassed";
        String status = "Succeeded";
        if (failed()) {
            htmlID = "tblTestFailed";
            status = "Failed";
        } else if (skipped()) {
            htmlID = "tblTestSkipped";
            status = "Skipped";
        }

        StringBuffer buff = new StringBuffer();
        buff.append("<tr>");
        Object[] params = {htmlID, name};
        buff.append(MessageFormat.format(HTMLConstants.TBL_ENTRY, params));
        params[1] = status;
        buff.append(MessageFormat.format(HTMLConstants.TBL_ENTRY, params));
        params[1] = Float.toString(timeTaken);
        buff.append(MessageFormat.format(HTMLConstants.TBL_NUM_ENTRY, params));
        params[1] = (failure != null) ?
            "<pre>" + failure.getStackTrace() + "</pre>" : "&nbsp;";
        buff.append(MessageFormat.format(HTMLConstants.TBL_ENTRY, params));
        buff.append("</tr>");

        return buff.toString();

    }

    private void parseNode(Node node) {
        Element elt = (Element)node;
        name = elt.getAttribute("name");
        classname = elt.getAttribute("classname");
        timeTaken = Float.parseFloat(elt.getAttribute("time"));

        NodeList childElements = node.getChildNodes();
        int numChildElements = childElements.getLength();

        for (int i = 0; i < numChildElements; i++) {
            Node n = childElements.item(i);
            if ((n != null) &&  (n.getNodeType() == Node.ELEMENT_NODE)) {
                String elementName = n.getNodeName();

                if (elementName.equals("failure")) {
                    failure = new Failure(n);
                } else if (elementName.equals("skipped")) {
                    skipped = true;
                }
            }
        }
    }
}
