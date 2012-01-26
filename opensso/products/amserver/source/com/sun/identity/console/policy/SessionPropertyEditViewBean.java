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
 * $Id: SessionPropertyEditViewBean.java,v 1.2 2008/06/25 05:43:06 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import java.util.Set;

public class SessionPropertyEditViewBean
    extends SessionPropertyOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SessionPropertyEdit.jsp";
    static final String PROPERTY_NAME = "propertyEntryName";
    private boolean submitCycle = false;

    /**
     * Creates a realm creation view bean.
     */
    public SessionPropertyEditViewBean() {
        super("SessionPropertyEdit");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        if (!submitCycle) {
            Map mapValues = (Map)getPageSessionAttribute(
                SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
            String propertyName = (String)getPageSessionAttribute(
                PROPERTY_NAME);
            Set propertyValues = (Set)mapValues.get(propertyName);
            setValues(propertyName, propertyValues);
        }
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.cancel");
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
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
        submitCycle = true;
        try {
            Map map = getValues();
            String name = (String)map.get(ATTR_NAME);
            Set values = (Set)map.get(ATTR_VALUES);

            Map mapValues = (Map)getPageSessionAttribute(
                SessionPropertyConditionHelper.PG_SESSION_PROPERTY_VALUES);
            String propertyName = (String)getPageSessionAttribute(
                PROPERTY_NAME);
            boolean okToEdit = true;

            if (!name.equals(propertyName)) {
                Set check = (Set)mapValues.get(name);
                if (check != null) {
                    okToEdit = false;
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.condition.session.property.name.already.exists.message"
                    );
                    forwardTo();
                } else {
                    mapValues.remove(propertyName);
                }
            } else {
                mapValues.remove(propertyName);
            }

            if (okToEdit) {
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
