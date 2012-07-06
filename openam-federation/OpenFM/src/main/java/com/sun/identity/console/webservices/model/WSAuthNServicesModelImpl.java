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
 * $Id: WSAuthNServicesModelImpl.java,v 1.2 2008/06/25 05:49:51 qcheng Exp $
 *
 */

package com.sun.identity.console.webservices.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.console.property.PropertyXMLBuilder;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public class WSAuthNServicesModelImpl
    extends AMServiceProfileModelImpl
    implements AMServiceProfileModel
{
    public static final String SERVICE_NAME =
	"sunIdentityServerAuthnService";
    public static final String ATTRIBUTE_NAME_HANDLERS = "MechanismHandlerList";
    public static final String KEY_PREFIX = "key=";
    public static final String CLASS_PREFIX = "class=";
    public static final String ACTION_PREFIX = "action=";

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public WSAuthNServicesModelImpl(
        HttpServletRequest req, 
        Map map
    ) throws AMConsoleException {
	super(req, SERVICE_NAME, map);
    }

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param serviceName Name of Service.
     * @param map of user information
     */
    public WSAuthNServicesModelImpl(
	HttpServletRequest req,
	String serviceName,
	Map map
    ) throws AMConsoleException {
	super(req, serviceName, map);
    }

    /**
     * Returns the XML for property sheet view component.
     *
     * @param realmName Name of Realm.
     * @param viewbeanClassName Class Name of View Bean.
     * @param serviceName Name of Service.
     * @return the XML for property sheet view component.
     * @throws AMConsoleException if XML cannot be created.
     */
    public String getPropertySheetXML(
        String realmName,
        String viewbeanClassName,
        String serviceName
    ) throws AMConsoleException {
	String xml = super.getPropertySheetXML(
	    realmName, viewbeanClassName, serviceName);
	return PropertyXMLBuilder.removeSubSection(
	    xml, ATTRIBUTE_NAME_HANDLERS, TBL_HANDLERS_XML);
    }

    private static String TBL_HANDLERS_XML =
	"<property span=\"true\"><cc name=\"MechanismHandlerList\" tagclass=\"com.sun.web.ui.taglib.table.CCActionTableTag\" > <attribute name=\"title\" value=\"webservices.authentication.service.table.handles.name\" /><attribute name=\"empty\" value=\"webservices.authentication.service.table.handles.noentries\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"selectionType\" value=\"multiple\" /><attribute name=\"selectionJavascript\" value=\"toggleTblButtonState('WSAuthNServices', 'WSAuthNServices.MechanismHandlerList', 'MechanismHandlerListCount', 'WSAuthNServices.tblHandlersButtonDelete', this)\" /><attribute name=\"showAdvancedSortingIcon\" value=\"false\" /><attribute name=\"showLowerActions\" value=\"false\" /><attribute name=\"showPaginationControls\" value=\"false\" /><attribute name=\"showPaginationIcon\" value=\"false\" /><attribute name=\"showSelectionIcons\" value=\"true\" /><attribute name=\"showSelectionSortIcon\" value=\"false\" /><attribute name=\"showSortingRow\" value=\"true\" /></cc></property>";
}
