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
 * $Id: ConditionType.java,v 1.13 2009/08/10 14:22:15 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import com.sun.identity.entitlement.EntitlementCondition;

public abstract class ConditionType {
    private String name;
    private String template;
    private String conditionIconUri;
    private boolean expression = false;

    public abstract ViewCondition newViewCondition();
    public abstract ViewCondition newViewCondition(EntitlementCondition ec, ConditionFactory conditionTypeFactory);

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

    public String getConditionIconUri() {
        return conditionIconUri;
    }

    public void setConditionIconUri(String conditionIconUri) {
        this.conditionIconUri = conditionIconUri;
    }

    public boolean isExpression() {
        return expression;
    }

    public void setExpression(boolean expression) {
        this.expression = expression;
    }

    public String getTitle() {
        Resources r = new Resources();
        String title = r.getString(this, "title");
        if (title == null) {
            title = r.getString(this, "title." + getName());
        }
        if (title == null) {
            title = getName();
        }
        return title;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
