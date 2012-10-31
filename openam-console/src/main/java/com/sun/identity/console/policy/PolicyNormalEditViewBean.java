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
 * $Id: PolicyNormalEditViewBean.java,v 1.2 2008/06/25 05:43:03 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;

public class PolicyNormalEditViewBean
    extends PolicyNormalViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PolicyNormalEdit.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    /**
     * Creates a policy creation view bean.
     */
    public PolicyNormalEditViewBean() {
        super("PolicyNormalEdit", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
        ptModel.setPageTitleText("page.title.policy.edit");
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        PolicyModel model = (PolicyModel)getModel();
        String cacheID = (String)getPageSessionAttribute(
            ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID);

        try {
            CachedPolicy cachedPolicy = model.getCachedPolicy(cacheID);
            String policyName = cachedPolicy.getTrackPolicyName();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);

            try {
                cacheID = model.cachePolicy(curRealm, policyName);
                setPageSessionAttribute(
                    PolicyOpViewBeanBase.PG_SESSION_POLICY_CACHE_ID, cacheID);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }

            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyNormalEditViewBean.handleButton2Request", e);
            redirectToStartURL();
        }
    }

    /**
     * Handles create policy request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        String currentRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        try {
            if (reconstructPolicy()) {
                // error message set in PolicyOpViewBeanBase
            } else {
                CachedPolicy cachedPolicy = getCachedPolicy();
                PolicyModel model = (PolicyModel)getModel();
    
                try {
                    model.replacePolicy(currentRealm, cachedPolicy.getPolicy());
                    setInlineAlertMessage(CCAlert.TYPE_INFO,
                        "message.information", "policy.modified.message");
                    cachedPolicy.setPolicyModified(false);
                } catch (AMConsoleException e) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                }
            }

            forwardTo();
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyNormalEditViewBean.handleButton1Request", e);
            redirectToStartURL();
        }
    }

    protected String getBreadCrumbDisplayName() {
        try {
            PolicyModel model = (PolicyModel)getModel();
            CachedPolicy cachedPolicy = getCachedPolicy();
            String[] arg = {cachedPolicy.getTrackPolicyName()};
            return MessageFormat.format(
                model.getLocalizedString("breadcrumbs.editPolicy"), 
                (Object[])arg);
        } catch (AMConsoleException e) {
            debug.warning(
                "PolicyNormalEditViewBean.getBreadCrumbDisplayName", e);
            return "";
        }
    }

    protected boolean startPageTrail() {
        return false;
    }

    protected boolean isProfilePage() {
        return true;
    }
}
