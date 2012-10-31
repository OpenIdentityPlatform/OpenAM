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
 * $Id: RealmDiscoveryDescriptionAddViewBean.java,v 1.2 2008/06/25 05:49:42 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.sun.identity.console.service.model.SMDescriptionData;
import com.sun.identity.console.service.model.SMDiscoEntryData;

public class RealmDiscoveryDescriptionAddViewBean
    extends RealmDiscoveryDescriptionViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/realm/RealmDiscoveryDescriptionAdd.jsp";

    public RealmDiscoveryDescriptionAddViewBean() {
	super("RealmDiscoveryDescriptionAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getPageTitleText() {
        return "discovery.service.description.create.page.title";
    }

    protected void handleButton1Request(SMDescriptionData smData) {
	RealmResourceOfferingViewBeanBase vb =
	    (RealmResourceOfferingViewBeanBase)getReturnToViewBean();
	SMDiscoEntryData data = (SMDiscoEntryData)removePageSessionAttribute(
	    PG_SESSION_DISCO_ENTRY_DATA);
	data.descData.add(smData);
	setPageSessionAttribute(RealmResourceOfferingViewBeanBase.
	    PROPERTY_ATTRIBUTE, data);
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected SMDescriptionData getCurrentData() {
	return null;
    }
}
