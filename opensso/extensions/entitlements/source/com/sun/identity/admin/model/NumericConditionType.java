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
 * $Id: NumericConditionType.java,v 1.3 2009/08/11 18:04:57 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.NumericAttributeCondition;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;

public class NumericConditionType
        extends ConditionType
        implements Serializable {

    private static final String DEFAULT_TEMPLATE = "/admin/facelet/template/condition-numeric.xhtml";
    private static final String DEFAULT_ICON = "../image/limit.png";

    public List<Operator> getOperators() {
        return operators;
    }

    public Operator getOperator() {
        return operators.get(0);
    }

    public void setOperators(List<Operator> operators) {
        this.operators = operators;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public enum Operator {
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        EQUAL;

        public String getToString() {
            return toString();
        }

        public String getTitle() {
            Resources r = new Resources();
            String title = r.getString(this, "title." + toString());
            if (title == null) {
                title = toString();
            }

            return title;
        }
    }

    private List<Operator> operators = new ArrayList<Operator>();
    private String attribute;

    public NumericConditionType() {
        setTemplate(DEFAULT_TEMPLATE);
        setConditionIconUri(DEFAULT_ICON);
    }

    public ViewCondition newViewCondition() {
        NumericViewCondition nvc = new NumericViewCondition();
        nvc.setConditionType(this);

        nvc.setOperator(getOperator());

        return nvc;
    }

    public ViewCondition newViewCondition(EntitlementCondition ec, ConditionFactory conditionTypeFactory) {
        assert (ec instanceof NumericAttributeCondition);
        NumericAttributeCondition nac = (NumericAttributeCondition) ec;

        NumericViewCondition nvc = (NumericViewCondition) newViewCondition();
        nvc.setValue(nac.getValue());

        Operator op = Operator.valueOf(nac.getOperator().toString());
        if (!operators.contains(op)) {
            op = getOperator();
        }
        nvc.setOperator(op);

        return nvc;
    }

    public List<SelectItem> getOperatorItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (Operator o: operators) {
            SelectItem si = new SelectItem(o, o.getTitle());
            items.add(si);
        }

        return items;
    }
}

