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
 * $Id: AgentViewBean.java,v 1.2 2008/06/25 05:42:53 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.realm.model.RMRealmModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import java.util.Collections;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class AgentViewBean
    extends DMTypeBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/Agent.jsp";

    /**
     * Creates a authentication domains view bean.
     */
    public AgentViewBean() {
	super("Agent");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);
    }

    protected AMModel getModelInternal() {
	HttpServletRequest req = getRequestContext().getRequest();
	return new RMRealmModelImpl(req, getPageSessionAttributes());
    }

    protected Set getEntries() {
        return Collections.EMPTY_SET;
    }

    protected void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblDMAgents.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.dm.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE, "table.dm.button.delete");
        tblModel.setActionValue(TBL_COL_NAME, "table.dm.name.column.name");
    }

    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
	forwardTo();
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
	forwardTo();
    }

    /**
     * Deletes authentication domains.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
	throws ModelControlException
    {
	forwardTo();
    }

    /**
     * Handles edit realm request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
	throws ModelControlException
    {
        forwardTo();
    }

    /**
     * Handles worm hole sub realm request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataHrefRequest(RequestInvocationEvent event)
	throws ModelControlException 
    {
	forwardTo();
    }
}
