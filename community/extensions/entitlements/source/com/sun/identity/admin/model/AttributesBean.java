/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AttributesBean.java,v 1.8 2009/06/29 13:48:01 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.ListFormatter;
import com.sun.identity.admin.handler.AttributesHandler;
import com.sun.identity.entitlement.ResourceAttribute;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class AttributesBean implements Serializable {
    private List<ViewAttribute> viewAttributes = new ArrayList<ViewAttribute>();
    private AttributesHandler attributesHandler;

    public enum AttributeType {
        STATIC,
        USER;

        public String getToString() {
            return toString();
        }
    }

    public AttributesBean() {
        reset();
    }

    public abstract Set<ResourceAttribute> toResourceAttributesSet();

    public void reset() {
        viewAttributes.clear();
    }


    public abstract ViewAttribute newViewAttribute();

    public abstract AttributeType getAttributeType();

    public List<ViewAttribute> getViewAttributes() {
        return viewAttributes;
    }

    public AttributesHandler getAttributesHandler() {
        return attributesHandler;
    }

    public void setAttributesHandler(AttributesHandler attributesHandler) {
        this.attributesHandler = attributesHandler;
    }

    public String getToString() {
        return new ListFormatter(viewAttributes).toString();
    }

    public String getToFormattedString() {
        return new ListFormatter(viewAttributes).toFormattedString();
    }

    public static String getToFormattedString(List<ViewAttribute> vas) {
        return new ListFormatter(vas).toFormattedString();
    }


    private static String getListToFormattedString(List list) {
        StringBuffer b = new StringBuffer();

        for (Iterator<Resource> i = list.iterator(); i.hasNext();) {
            b.append(i.next());
            if (i.hasNext()) {
                b.append("\n");
            }

        }

        return b.toString();
    }
}
