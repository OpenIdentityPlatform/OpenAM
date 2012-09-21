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
 * $Id: NumericViewCondition.java,v 1.1 2009/08/09 06:04:20 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.model.NumericConditionType.Operator;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.NumericAttributeCondition;
import java.io.Serializable;

public class NumericViewCondition
    extends ViewCondition
    implements Serializable {

    private float value = 0;
    private Operator operator;

    public EntitlementCondition getEntitlementCondition() {
        NumericAttributeCondition nac = new NumericAttributeCondition();
        nac.setDisplayType(getConditionType().getName());
        NumericConditionType lct = (NumericConditionType)getConditionType();
        nac.setValue(getValue());
        nac.setOperator(NumericAttributeCondition.Operator.valueOf(getOperator().toString()));
        nac.setAttributeName(lct.getAttribute());

        return nac;
    }

    @Override
    public String toString() {
        return getTitle() + ": {" +  getOperator().getTitle() + " " + getValue() + "}";
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }
}
