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
 * $Id: FSAuthDomainsOpViewBeanBase.java,v 1.2 2008/06/25 05:49:34 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.federation.model.FSAuthDomainsModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public abstract class FSAuthDomainsOpViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    protected CCPageTitleModel ptModel;

    /**
     * Creates an authentication domains view bean.
     *
     * @param name Name of view
     */
    public FSAuthDomainsOpViewBeanBase(String name) {          
	super(name);
    }

    protected void registerChildren() {           
	ptModel.registerChildren(this);
	super.registerChildren();
    }

    protected View createChild(String name) {         
	View view = null;
        if (ptModel.isChildSupported(name)) {
	    view = ptModel.createChild(this, name);
	} else {
	    view = super.createChild(name);
	}
	return view;
    }

    protected AMModel getModelInternal() {           
	HttpServletRequest req = getRequestContext().getRequest();
	return new FSAuthDomainsModelImpl(req, getPageSessionAttributes());
    }

    static Map getProviderDisplayNames(AMModel model, Collection values) { 
	Map map = null;
	if ((values != null) && (!values.isEmpty())) {
	    map = new HashMap(values.size() *2);
	    for (Iterator iter = values.iterator(); iter.hasNext(); ) {
		String name = (String)iter.next();
		int idx = name.lastIndexOf('|');
		if (idx != -1) {
		    String type = name.substring(idx+1);
		    map.put(name,
			name.substring(0, idx) + " " +
			    model.getLocalizedString(type + ".label"));
		} else {
		    map.put(name, name);
		}
	    }
	}        
	return (map == null) ? Collections.EMPTY_MAP : map;
    }

    protected void forwardToAuthDomainView(RequestInvocationEvent event) {         
        FederationViewBean vb = 
            (FederationViewBean) getViewBean(FederationViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
}
