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
 * $Id: PolicyWizardAdvancedTabIndex.java,v 1.2 2009/06/04 11:49:16 veiming Exp $
 */

package com.sun.identity.admin.model;

import java.util.HashMap;
import java.util.Map;

public enum PolicyWizardAdvancedTabIndex {

    ACTIONS(0),
    CONDITIONS(1),
    USER_ATTRIBUTES(2),
    RESOURCE_ATTRIBUTES(3);

    private final int tabIndex;

    private static final Map<Integer,PolicyWizardAdvancedTabIndex> intValues = new HashMap<Integer,PolicyWizardAdvancedTabIndex>() {
        {
            put(ACTIONS.toInt(), ACTIONS);
            put(CONDITIONS.toInt(), CONDITIONS);
            put(USER_ATTRIBUTES.toInt(), USER_ATTRIBUTES);
            put(RESOURCE_ATTRIBUTES.toInt(), RESOURCE_ATTRIBUTES);
        }
    };

    PolicyWizardAdvancedTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public int toInt() {
        return tabIndex;
    }

    public static PolicyWizardAdvancedTabIndex valueOf(int i) {
        return intValues.get(Integer.valueOf(i));
    }
}
