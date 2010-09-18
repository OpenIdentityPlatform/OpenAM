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
 * $Id: EntityDiscoveryDescriptionEditViewBean.java,v 1.2 2008/06/25 05:49:41 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.sun.identity.console.base.model.AMConsoleException;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.service.model.SMDescriptionData;
import com.sun.identity.console.service.model.SMDiscoEntryData;

public class EntityDiscoveryDescriptionEditViewBean
    extends EntityDiscoveryDescriptionViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/idm/EntityDiscoveryDescriptionEdit.jsp";
    protected static final String PGATTR_INDEX = "descriptionTblIndex";
    private boolean populateValues = false;

    public EntityDiscoveryDescriptionEditViewBean() {
	super("EntityDiscoveryDescriptionEdit", DEFAULT_DISPLAY_URL);
    }

    public void populateValues(String index) {
	setPageSessionAttribute(PGATTR_INDEX, index);
	populateValues = true;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);

	if (populateValues) {
	    SMDescriptionData data = getCurrentData();
	    setValues(data);
	}
    }

    protected SMDescriptionData getCurrentData() {
	int currentIdx = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));
	SMDiscoEntryData data = (SMDiscoEntryData)getPageSessionAttribute(
	    PG_SESSION_DISCO_ENTRY_DATA);
	return (SMDescriptionData)data.descData.get(currentIdx);
    }

    protected void createPageTitleModel() {
        create3ButtonPageTitle();
    }

    protected String getPageTitleText() {
	return "discovery.service.description.edit.page.title";
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("policy.table.title.subjects");
    }

    protected void handleButton1Request(SMDescriptionData smData)
	throws AMConsoleException
    {
	EntityResourceOfferingViewBeanBase vb =
	    (EntityResourceOfferingViewBeanBase)getReturnToViewBean();
	SMDiscoEntryData data = (SMDiscoEntryData)getPageSessionAttribute(
	    PG_SESSION_DISCO_ENTRY_DATA);
	int index = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));
	data.descData.set(index, smData);
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
	populateValues = true;
	forwardTo();
    }
										
    protected void createPropertyModel() {
	DelegationConfig dConfig = DelegationConfig.getInstance();
	boolean canModify = dConfig.hasPermission("/", null,
	    AMAdminConstants.PERMISSION_MODIFY,
	    getRequestContext().getRequest(), getClass().getName());
										
	String xmlFile = (canModify) ?
	    "com/sun/identity/console/propertySMDiscoveryDescription.xml" :
	"com/sun/identity/console/propertySMDiscoveryDescription_Readonly.xml";
										
	propertySheetModel = new AMPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(xmlFile));
	propertySheetModel.clear();
    }
}
