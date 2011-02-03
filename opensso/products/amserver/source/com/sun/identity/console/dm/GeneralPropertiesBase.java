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
 * $Id: GeneralPropertiesBase.java,v 1.2 2008/06/25 05:42:54 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HREF;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.dm.model.DMModel;
import com.sun.identity.console.idm.EntityEditViewBean;
import com.sun.identity.console.property.PropertyTemplate;
import javax.servlet.http.HttpServletRequest;

/**
 * This class can be used by all property pages.
 */
public abstract class GeneralPropertiesBase 
    extends AMPrimaryMastHeadViewBean 
{
    public GeneralPropertiesBase(String name) {
	super(name);
	registerChild(AMAdminConstants.DYN_LINK_COMPONENT_NAME, HREF.class);
    }


    public void handleDynLinkRequest(RequestInvocationEvent event) {
        HttpServletRequest req = getRequestContext().getRequest();

        String url = req.getParameter(
            PropertyTemplate.PARAM_PROPERTIES_VIEW_BEAN_URL);

	String curProfile = (String)getPageSessionAttribute(
	    AMAdminConstants.CURRENT_PROFILE);
	DMModel model = (DMModel)getModel();
	String universalId = model.getUniversalId(curProfile);
	setPageSessionAttribute(EntityEditViewBean.UNIVERSAL_ID, universalId);

        AMPostViewBean vb = (AMPostViewBean)getViewBean(AMPostViewBean.class);
        passPgSessionMap(vb);
        vb.setTargetViewBeanURL(url);
        vb.forwardTo(getRequestContext());
    }
}
