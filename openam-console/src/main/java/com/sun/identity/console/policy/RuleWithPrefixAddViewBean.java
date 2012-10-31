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
 * $Id: RuleWithPrefixAddViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;

public class RuleWithPrefixAddViewBean
    extends RuleAddViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/RuleWithPrefixAdd.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    /**
     * Creates a policy creation view bean.
     */
    public RuleWithPrefixAddViewBean() {
        super("RuleWithPrefixAdd", DEFAULT_DISPLAY_URL);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        getManagedResources();
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        return (!isReferralPolicy()) ?
            "com/sun/identity/console/propertyPMRuleWithPrefixAdd.xml" :
            "com/sun/identity/console/propertyPMRuleWithPrefixAddNoAction.xml";
    }
}
