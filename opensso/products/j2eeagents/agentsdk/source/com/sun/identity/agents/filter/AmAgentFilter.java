/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmAgentFilter.java,v 1.2 2008/06/25 05:51:42 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

/**
 * The entry point of the agent filter 
 */
public class AmAgentFilter extends AmAgentBaseFilter {

    /* (non-Javadoc)
     * @see com.sun.identity.agents.filter.AmAgentBaseFilter#getDefaultFilterMode()
     */
    protected AmFilterMode getDefaultFilterMode() {
        return AmFilterMode.MODE_ALL;
    }

    /* (non-Javadoc)
     * @see com.sun.identity.agents.filter.AmAgentBaseFilter#getAllowedFilterModes()
     */
    protected AmFilterMode[] getAllowedFilterModes() {
        return ALLOWED_MODES; 
    }

    private static final AmFilterMode[] ALLOWED_MODES = new AmFilterMode[] { 
            AmFilterMode.MODE_NONE, AmFilterMode.MODE_SSO_ONLY, 
            AmFilterMode.MODE_J2EE_POLICY, AmFilterMode.MODE_URL_POLICY, 
            AmFilterMode.MODE_ALL
    };
}
