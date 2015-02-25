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
 * $Id: RealmPropertiesBase.java,v 1.3 2008/06/25 05:43:11 qcheng Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.sm.SMSSchema;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.tabs.CCTabs;
import java.text.MessageFormat;

public abstract class RealmPropertiesBase
    extends RMRealmViewBeanBase
{
    public RealmPropertiesBase(String name) {
        super(name);
    }
    
    public CCPageTitleModel ptModel;

    /**
     * Handles tab selected event. 
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        AMViewConfig amconfig = AMViewConfig.getInstance();

        try {
            AMViewBeanBase vb = getTabNodeAssociatedViewBean("realms", nodeID);

            String tmp = (String)getPageSessionAttribute(
                AMAdminConstants.PREVIOUS_REALM);
            vb.setPageSessionAttribute(AMAdminConstants.PREVIOUS_REALM, tmp);
            
            tmp = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            vb.setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, tmp);

            tmp = (String)getPageSessionAttribute(
                AMAdminConstants.PREVIOUS_TAB_ID);
            vb.setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, tmp);
            unlockPageTrailForSwapping();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            debug.error("RealmPropertiesBase.nodeClicked", e);
            forwardTo();
        }
    }

    protected void createTabModel() {
        if (tabModel == null) {
            AMViewConfig amconfig = AMViewConfig.getInstance();
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_PROFILE);
            if (realmName != null) {
                tabModel = amconfig.getTabsModel("realms", realmName,
                    getRequestContext().getRequest());
                registerChild(TAB_COMMON, CCTabs.class);
            }
        }
    }

    protected void forwardToRealmView(RequestInvocationEvent event) {   
        // reset the current realm to be the realm that was being viewed before
        // the profile page was opened.
        String tmp = 
            (String)getPageSessionAttribute(AMAdminConstants.PREVIOUS_REALM);
        setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, tmp);

        // reset the tab selected to the realm view
        tmp = (String)getPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID);
        setPageSessionAttribute(getTrackingTabIDName(), tmp);

        // and now forward on to the realm page...
        RMRealmViewBean vb = (RMRealmViewBean)getViewBean(
            RMRealmViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /*
    * Get the name of the current realm and add it to the title of the 
    * properties page. 
    */
    protected void setPageTitle(AMModel model, String title) {
        String realm = 
            (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        
        AMModel m = getModel();
        String startDN = m.getStartDN();
        if (isRootRealm(realm, startDN)) {
            realm = AMFormatUtils.DNToName(m, m.getStartDSDN());
        }
        int index = realm.lastIndexOf('/');
        if (index != -1) {
            realm = realm.substring(index + 1);
        }
        // unescape the value to display '/' character
        String[] tmp = { SMSSchema.unescapeName(realm) };
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString(title), (Object[])tmp));
    }

    protected String getBreadCrumbDisplayName() {
        String realm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        AMModel model = (AMModel)getModel();
        
        String path = null;
        if ((realm != null) && (realm.trim().length() > 0)) {
            int idx = realm.lastIndexOf('/');
            if ((idx != -1) && (idx < (realm.length() -1))) {
                path = realm.substring(idx+1);
            }
        }
        if (path == null) {
            model.getStartDSDN();
            path = AMFormatUtils.DNToName(model, model.getStartDSDN());
        }
        
        // unescape the value to display '/' character
        String[] arg = {SMSSchema.unescapeName(path)};
        return MessageFormat.format(
            model.getLocalizedString("breadcrumbs.editRealm"), (Object[])arg);
    }

    protected boolean startPageTrail() {
        return false;
    }

    /**
     * Handles "back to" page request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        forwardToRealmView(event);
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.back.realms");
    }
}                                                                     
