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
 * $Id: ReferralEditViewBean.java,v 1.3 2009/12/01 21:23:32 veiming Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.NameAlreadyExistsException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.interfaces.Referral;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.Set;

public class ReferralEditViewBean
    extends ReferralOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/ReferralEdit.jsp";
    public static final String EDIT_REFERRAL_NAME = "editReferralName";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    /**
     * Creates a policy creation view bean.
     */
    public ReferralEditViewBean() {
        super("ReferralEdit", DEFAULT_DISPLAY_URL);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        PolicyModel model = (PolicyModel)getModel();
        String i18nName = (String)propertySheetModel.getValue(
            REFERRAL_TYPE_NAME);
        String title = model.getLocalizedString(
            "page.title.policy.referral.edit");
        String[] param = {i18nName};
        ptModel.setPageTitleText(MessageFormat.format(title, (Object[])param));
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        return (readonly) ?
            "com/sun/identity/console/propertyPMReferralEdit_Readonly.xml" :
            "com/sun/identity/console/propertyPMReferralEdit.xml";
    }

    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles edit policy request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        try {
            handleButton1Request(getCachedPolicy());
        } catch (AMConsoleException e) {
            redirectToStartURL();
        }
    }

    private void handleButton1Request(CachedPolicy cachedPolicy)
        throws ModelControlException
    {
        Referral deleted = null;
        String origName = (String)getPageSessionAttribute(EDIT_REFERRAL_NAME);
        Policy policy = cachedPolicy.getPolicy();

        try {
            Referral referral = createReferral();
            if (referral != null) {
                String name = (String)propertySheetModel.getValue(
                    REFERRAL_NAME);

                if (origName.equals(name)) {
                    policy.replaceReferral(name, referral);
                } else {
                    deleted = policy.removeReferral(origName);
                    policy.addReferral(name, referral);
                }

                deleted = null;
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.referral.updated");
                cachedPolicy.setPolicyModified(true);
            }
        } catch (NameAlreadyExistsException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (InvalidNameException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (NameNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        } finally {
            if (deleted != null) {
                try {
                    policy.addReferral(origName, deleted);
                } catch (NameAlreadyExistsException e) {
                    debug.warning(
                        "ReferralEditViewBean.handleButton1Request",e);
                } catch (InvalidNameException e) {
                    debug.warning(
                        "ReferralEditViewBean.handleButton1Request",e);
                }
            }
        }
        forwardTo();
    }

    protected Set getDefaultValues() {
        Set values = null;
        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            String referralName = (String)getPageSessionAttribute(
                PG_SESSION_REFERRAL_NAME);

            Referral referral = policy.getReferral(referralName);
            values = referral.getValues();
        } catch (NameNotFoundException e) {
            debug.warning("ReferralEditViewBean.getDefaultValues", e);
        } catch (AMConsoleException e) {
            debug.warning("ReferralEditViewBean.getDefaultValues", e);
        }
        return values;
    }

    protected String getBreadCrumbDisplayName() {
        PolicyModel model = (PolicyModel)getModel();
        String origName = (String)getPageSessionAttribute(EDIT_REFERRAL_NAME);
        String[] arg = {origName};
        return MessageFormat.format(
            model.getLocalizedString("breadcrumbs.editReferral"),(Object[])arg);
    }

    protected boolean startPageTrail() {
      return false;
    }
}
