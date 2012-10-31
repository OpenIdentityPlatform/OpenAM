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
 * $Id: SAMLv2GeneralViewBean.java,v 1.3 2008/06/25 05:49:37 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.web.ui.view.alert.CCAlert;
import javax.servlet.http.HttpServletRequest;

public class SAMLv2GeneralViewBean extends SAMLv2Base {
    
    public static final String DEFAULT_DISPLAY_URL =
	"/console/federation/SAMLv2General.jsp";

    public SAMLv2GeneralViewBean() {
	super("SAMLv2General");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	SAMLv2Model samlModel = (SAMLv2Model)getModel();
        super.beginDisplay(event);
        setDisplayFieldValue(samlModel.TF_NAME, entityName);
    }

    protected void createPropertyModel() {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertySAMLv2General.xml"));

	psModel.clear();
    }
     
    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {            
        forwardTo();
    }
    

}
