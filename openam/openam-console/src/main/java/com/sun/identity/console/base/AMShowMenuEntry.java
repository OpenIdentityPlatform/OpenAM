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
 * $Id: AMShowMenuEntry.java,v 1.2 2008/06/25 05:42:47 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.sun.identity.console.base.model.AMConsoleException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class AMShowMenuEntry {
    private String id;
    private String label;
    private String viewbean;

    public AMShowMenuEntry(Node root)
        throws AMConsoleException
    {
        NamedNodeMap attrs = root.getAttributes();
        if (attrs == null) {
            throw new AMConsoleException(
                "AMShowMenuEntry.<init> incorrect XML format");
        }

        id = setAttribute(attrs, "id",
            "AMShowMenuEntry.<init> missing id attribute");
        viewbean = setAttribute(attrs, "viewbean",
            "AMShowMenuEntry.<init> missing viewbean attribute");
        label = setAttribute(attrs, "label",
            "AMShowMenuEntry.<init> missing label attribute");
    }

    public String getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getViewBean() {
        return viewbean;
    }

    private String setAttribute(
        NamedNodeMap attrs,
        String attrName,
        String exceptionMsg
    ) throws AMConsoleException {
        Node nodeID = attrs.getNamedItem(attrName);
        if (nodeID == null) {
            throw new AMConsoleException(exceptionMsg);
        }
        String value = nodeID.getNodeValue();
        value = value.trim();

        if (value.length() == 0) {
            throw new AMConsoleException(exceptionMsg);
        }

        return value;
    }
}
