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
 * $Id: TimePolicyModelImpl.java,v 1.3 2008/06/25 05:43:08 qcheng Exp $
 *
 */

package com.sun.identity.console.policy.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public class TimePolicyModelImpl
    extends PolicyModelImpl
    implements PolicyModel
{
    private static List propertyNames = new ArrayList();

    static {
        propertyNames.add("StartDate");
        propertyNames.add("EndDate");
    }

    /**
     * Creates a simple model using default resource bundle. 
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public TimePolicyModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    /**
     * Returns property names of a condition.
     *
     * @param realmName Name of Realm.
     * @param conditionType Name of condition name.
     * @return property names of a condition.
     */
    public List getConditionPropertyNames(
        String realmName,
        String conditionType
    ) {
        return propertyNames;
    }
}
