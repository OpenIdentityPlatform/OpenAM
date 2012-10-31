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
 * $Id: SessionPropertyAddViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class SessionPropertyAddViewBean
    extends SessionPropertyOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SessionPropertyAdd.jsp";

    /**
     * Creates a realm creation view bean.
     */
    public SessionPropertyAddViewBean() {
        super("SessionPropertyAdd");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardToCallingViewBean();
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        try {
            Map map = getValues();
            String name = (String)map.get(ATTR_NAME);
            Set values = (Set)map.get(ATTR_VALUES);

            Map mapValues = (Map)getPageSessionAttribute(
                SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
            boolean okToAdd = true;

            if (mapValues == null) {
                mapValues = new HashMap();
                setPageSessionAttribute(
                    SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES,
                    (HashMap)mapValues);
            } else {
                Set check = (Set)mapValues.get(name);
                if (check != null) {
                    okToAdd = false;
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.condition.session.property.name.already.exists.message"
                    );
                    forwardTo();
                }
            }

            if (okToAdd) {
                mapValues.put(name, values);
                forwardToCallingViewBean();
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }
}
