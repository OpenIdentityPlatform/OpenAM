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
 * $Id: Failure.java,v 1.2 2008/06/25 05:53:06 qcheng Exp $
 *
 */

package com.sun.identity.shared.test.tools;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is the failure object in the report generating tool setup.
 */
public class Failure {
    private String type;
    private String message;
    private String stackTrace;

    /**
     * Creates an instance of <code>Failure</code> object.
     *
     * @param node Failure Document Object Model Node.
     */
    public Failure(Node node) {
        parseNode(node);
    }

    /**
     * Returns stack trace of the failure.
     *
     * @return stack trace of the failure.
     */
    public String getStackTrace() {
        return stackTrace;
    }

    private void parseNode(Node node) {
        Element elt = (Element)node;
        type = elt.getAttribute("node");
        message = elt.getAttribute("message");

        NodeList children = node.getChildNodes();
        Node n = children.item(0);
        stackTrace = n.getNodeValue();
    }
}
