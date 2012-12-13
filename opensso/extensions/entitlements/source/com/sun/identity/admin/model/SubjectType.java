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
 * $Id: SubjectType.java,v 1.11 2009/08/10 14:22:16 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import com.sun.identity.entitlement.EntitlementSubject;
import java.io.Serializable;

public abstract class SubjectType implements Serializable {
    private String name;
    private String template;
    private String subjectIconUri;
    private boolean expression;

    public abstract ViewSubject newViewSubject();
    public abstract ViewSubject newViewSubject(EntitlementSubject es, SubjectFactory stf);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isExpression() {
        return expression;
    }

    public void setExpression(boolean expression) {
        this.expression = expression;
    }

    public String getSubjectIconUri() {
        return subjectIconUri;
    }

    public void setSubjectIconUri(String subjectIconUri) {
        this.subjectIconUri = subjectIconUri;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public String getTitle() {
        Resources r = new Resources();
        String title = r.getString(this, "title");
        if (title == null) {
            title = getName();
        }
        return title;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SubjectType)) {
            return false;
        }
        SubjectType st = (SubjectType)other;

        return st.getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}