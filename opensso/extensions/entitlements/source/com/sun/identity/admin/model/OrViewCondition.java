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
 * $Id: OrViewCondition.java,v 1.10 2009/08/09 06:04:20 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.OrCondition;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class OrViewCondition
        extends ContainerViewCondition
        implements Serializable {

    public OrViewCondition() {
        super();
    }

    public EntitlementCondition getEntitlementCondition() {
        OrCondition oc = new OrCondition();
        oc.setDisplayType(getConditionType().getName());
        Set<EntitlementCondition> ecs = new HashSet<EntitlementCondition>();

        for (ViewCondition vc : getViewConditions()) {
            EntitlementCondition ec = vc.getEntitlementCondition();
            if (ec != null) {
                ecs.add(ec);
            }
        }

        if (ecs.size() > 0) {
            oc.setEConditions(ecs);
            return oc;
        } else {
            return null;
        }
    }
}
