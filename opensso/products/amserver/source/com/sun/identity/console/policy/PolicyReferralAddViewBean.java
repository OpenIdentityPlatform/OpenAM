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
 * $Id: PolicyReferralAddViewBean.java,v 1.2 2008/06/25 05:43:04 qcheng Exp $
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

public class PolicyReferralAddViewBean
    extends PolicyReferralViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PolicyReferralAdd.jsp";

    /**
     * Creates a policy creation view bean.
     */
    public PolicyReferralAddViewBean() {
        super("PolicyReferralAdd", DEFAULT_DISPLAY_URL);
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    protected String getPropertyXMLFileName(boolean readOnly) {
        return "com/sun/identity/console/propertyPMPolicyReferral.xml";
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        backTrail();
        forwardToPolicyViewBean();
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
                forwardTo();
            } else {
                CachedPolicy cachedPolicy = getCachedPolicy();
                PolicyModel model = (PolicyModel)getModel();
    
                String name = cachedPolicy.getPolicy().getName();
                if (name.equals(model.getLocalizedString("policy.create.name"))
                ) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        model.getLocalizedString("policy.name.change"));
                    forwardTo();
                } else {
                    try {
                        model.createPolicy(currentRealm,
                            cachedPolicy.getPolicy());
                        backTrail();
                        forwardToPolicyViewBean();
                    } catch (AMConsoleException e) {
                        setInlineAlertMessage(CCAlert.TYPE_ERROR,
                            "message.error", e.getMessage());
                        forwardTo();
                    }
                }
            }
        } catch (AMConsoleException e) {
            redirectToStartURL();
        }
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.addReferralPolicy";
    }

    protected boolean startPageTrail() {
        return false;
    }

    protected boolean isProfilePage() {
        return false;
    }
}
