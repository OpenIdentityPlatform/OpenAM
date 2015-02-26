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
 * $Id: AndConditionType.java,v 1.7 2009/08/09 06:04:20 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.entitlement.AndCondition;
import com.sun.identity.entitlement.EntitlementCondition;
import java.io.Serializable;

public class AndConditionType
        extends ConditionType
        implements Serializable {

    public ViewCondition newViewCondition() {
        ViewCondition vc = new AndViewCondition();
        vc.setConditionType(this);

        return vc;
    }

    public ViewCondition newViewCondition(EntitlementCondition ec, ConditionFactory conditionTypeFactory) {
        assert (ec instanceof AndCondition);
        AndCondition ac = (AndCondition) ec;

        AndViewCondition avc = (AndViewCondition) newViewCondition();

        if (ac.getEConditions() != null) {
            for (EntitlementCondition childEc : ac.getEConditions()) {
                if (childEc != null) {
                    ViewCondition vc = conditionTypeFactory.getViewCondition(childEc);
                    avc.addViewCondition(vc);
                }
            }
        }

        return avc;
    }
}
