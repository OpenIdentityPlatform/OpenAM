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
 * $Id: ServerEditGeneralViewBean.java,v 1.2 2008/06/25 05:43:17 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.service.model.ServerSiteModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import java.util.Set;

/**
 * Server Configuration, General Tab.
 */
public class ServerEditGeneralViewBean
    extends ServerEditViewBeanBase
{
    private static final String PARENT_SITE = "singleChoiceSite";
    private static final String DEFAULT_DISPLAY_URL =
        "/console/service/ServerEditGeneral.jsp";

    /**
     * Creates a clone server view bean.
     */
    public ServerEditGeneralViewBean() {
        super("ServerEditGeneral", DEFAULT_DISPLAY_URL);
    }
    
    protected String getPropertyXML() {
        return "com/sun/identity/console/propertyServerEditGeneral.xml";
    }

    /**
     * Displays the profile of a site.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String serverName = (String)getPageSessionAttribute(
            PG_ATTR_SERVER_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();
        try {
            getParentSites(serverName, model);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    private void getParentSites(String serverName, ServerSiteModel model)
        throws AMConsoleException {
        Set sites = model.getSiteNames();
        OptionList choices = createOptionList(sites);
        choices.add(0, 
            new Option(model.getLocalizedString("none.site"), ""));
        String parentSite = model.getServerSite(serverName);
        if (parentSite == null) {
            parentSite = "";
        }
        CCDropDownMenu cb = (CCDropDownMenu)getChild(PARENT_SITE);
        cb.resetStateData();
        cb.setValue(parentSite);
        cb.setOptions(choices);
    }

    /**
     * Handles create site request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        submitCycle = true;
        String serverName = (String)getPageSessionAttribute(
            PG_ATTR_SERVER_NAME);

        String parentSite =
            (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)) ?
            null : (String)getDisplayFieldValue(PARENT_SITE);
        ServerSiteModel model = (ServerSiteModel)getModel();

        try {
            model.modifyServer(serverName, parentSite, getAttributeValues());
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "serverconfig.updated");
        } catch (UnknownPropertyNameException e) {
            // ignore. this cannot happen because property in this page
            // is customized.
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    protected String removeParentSiteBlob(String xml) {
        String serverName = (String)getPageSessionAttribute(
            PG_ATTR_SERVER_NAME);
        if (serverName.equals(ServerConfiguration.DEFAULT_SERVER_CONFIG)) {
            int idx = xml.indexOf(PARENT_SITE);
            if (idx != -1){
                int start = xml.lastIndexOf(
                    "<section name=\"secSite\" defaultValue=", idx);
                int end = xml.indexOf("</section>", start);
                xml = xml.substring(0, start) + xml.substring(end +11);
            }
        }
        return xml;
    }
}
