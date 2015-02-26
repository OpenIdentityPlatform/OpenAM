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
 * $Id: IpRangeConditionType.java,v 1.4 2009/08/13 16:55:04 farble1670 Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.model.IpRangeViewCondition.IpAddress;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.IPCondition;
import java.io.Serializable;

public class IpRangeConditionType
    extends ConditionType
    implements Serializable {
    public ViewCondition newViewCondition() {
        ViewCondition vc = new IpRangeViewCondition();
        vc.setConditionType(this);

        return vc;
    }
    
    public ViewCondition newViewCondition(EntitlementCondition ec, ConditionFactory conditionTypeFactory) {
        assert(ec instanceof IPCondition);
        IPCondition ipc = (IPCondition)ec;

        IpRangeViewCondition ipvc = (IpRangeViewCondition)newViewCondition();
        ipvc.setStartIp(new IpAddress(ipc.getStartIp()));
        ipvc.setEndIp(new IpAddress(ipc.getEndIp()));

        return ipvc;
    }
}
