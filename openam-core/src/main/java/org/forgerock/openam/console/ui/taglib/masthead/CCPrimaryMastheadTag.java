/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */  
package org.forgerock.openam.console.ui.taglib.masthead;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.jato.util.NonSyncStringBuffer;
import com.sun.identity.shared.Constants;
import javax.servlet.jsp.JspException;

/**
 *
 * @author Peter Major
 */
public class CCPrimaryMastheadTag extends com.sun.web.ui.taglib.masthead.CCPrimaryMastheadTag {

    private String CONTEXT_ROOT = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

    @Override
    protected void appendHelpLink(NonSyncStringBuffer buffer) throws JspException {
        return;
    }
    
    @Override
    protected void appendGeneralLinks(NonSyncStringBuffer buffer) throws JspException {
    	//TODO migrate to CCHrefTag
    	String switchToXUI = "<a href=\""+CONTEXT_ROOT.concat("/XUI/")+"\" class=\"MstLnkLft\" title=\"Switch to XUI\"  target=\"_top\">Switch to XUI</a>";
    	buffer.append(switchToXUI);
    	super.appendGeneralLinks(buffer);
    }
}
