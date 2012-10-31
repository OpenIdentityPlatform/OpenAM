/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AgentDumpViewBean.java,v 1.1 2008/12/10 18:25:14 farble1670 Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.agentconfig;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.agentconfig.model.AgentDumpModel;
import com.sun.identity.console.agentconfig.model.AgentDumpModelImpl;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;

/**
 * Servers and Sites Management main page.
 */
public class AgentDumpViewBean extends AMPrimaryMastHeadViewBean
{
    private static final String DEFAULT_DISPLAY_URL =
            "/console/agentconfig/AgentDump.jsp";
    private static final String PGTITLE_ONE_BTN = "pgtitleOneBtn";
    private static final String STATICTEXT_VALUES = "txtValues";

    static final String PG_ATTR_CONFIG_PAGE = "pgAttrConfigPage";

    private CCPageTitleModel ptModel;

    /**
     * Creates a agent configurtion inheritance setting view bean.
     */
    public AgentDumpViewBean() {
        super("AgentDump");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String universalId =
                    (String)getPageSessionAttribute(
                        AgentProfileViewBean.UNIVERSAL_ID
                    );
            if (universalId != null) {
                super.initialize();
                createPageTitleModel();
                registerChildren();
                initialized = true;
            }
        }
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE_ONE_BTN)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                    getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/oneBtnPageTitle.xml"
                    )
                );
        ptModel.setValue("button1", getBackButtonLabel());
    }


    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new AgentDumpModelImpl(req, getPageSessionAttributes());
    }


    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
    }

    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        try {
            super.beginDisplay(event);
            setTitle();
            String universalId =
                    (String)getPageSessionAttribute(
                        AgentProfileViewBean.UNIVERSAL_ID
                    );
            AgentDumpModel model = (AgentDumpModel)getModel();
            Map values = model.getAttributeValues(universalId);

            CCStaticTextField valuesText =
                    (CCStaticTextField)getChild(STATICTEXT_VALUES);
            valuesText.setValue(getFormattedAttributes(values));
        } catch (AMConsoleException amce) {
            setInlineAlertMessage(
                    CCAlert.TYPE_ERROR, "message.error", amce.getMessage()
            );
        }
    }

    protected void setTitle() {
        AgentDumpModel model = (AgentDumpModel)getModel();
        String universalId =
                (String)getPageSessionAttribute(
                    AgentProfileViewBean.UNIVERSAL_ID
                );

        try {
            String title = model.getLocalizedString("page.title.agent.dump");
            String displayName = model.getDisplayName(universalId);
            Object[] param = { displayName };
            ptModel.setPageTitleText(MessageFormat.format(title, param));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(
                    CCAlert.TYPE_ERROR, "message.error", e.getMessage()
            );
        }
    }

    private static String getFormattedAttributes(Map<String,Set<String>> values) {
        StringBuilder b = new StringBuilder();
        for (String key: values.keySet()) {
            Set<String> vals = values.get(key);
            if (vals == null || vals.isEmpty()) {
                b.append(key);
                b.append("=\n");
            } else {
                for (String val: vals) {
                    b.append(key);
                    b.append('=');
                    b.append(val);
                    b.append('\n');
                }
            }
        }

        return b.toString();
    }

    public void handleButton1Request(RequestInvocationEvent event)
            throws ModelControlException {
        try {
            AMViewBeanBase vb =
                    (AMViewBeanBase)getViewBean(
                        Class.forName((String)getPageSessionAttribute(
                            PG_ATTR_CONFIG_PAGE
                        )
                    ));
            removePageSessionAttribute(PG_ATTR_CONFIG_PAGE);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            setInlineAlertMessage(
                    CCAlert.TYPE_ERROR, "message.error", e.getMessage()
            );
        }
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.agent.config");
    }
}
