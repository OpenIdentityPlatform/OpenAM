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
 * $Id: ResponseProviderEditViewBean.java,v 1.2 2008/06/25 05:43:04 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.policy.NameAlreadyExistsException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.Map;

public class ResponseProviderEditViewBean
    extends ResponseProviderOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/ResponseProviderEdit.jsp";
    public static final String EDIT_RESPONSEPROVIDER_NAME =
        "editResponseProviderName";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    ResponseProviderEditViewBean(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    /**
     * Creates a policy creation view bean.
     */
    public ResponseProviderEditViewBean() {
        super("ResponseProviderEdit", DEFAULT_DISPLAY_URL);
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
            RESPONSEPROVIDER_TYPE_NAME);
        String title = model.getLocalizedString(
            "page.title.policy.responseprovider.edit");
        String[] param = {i18nName};
        ptModel.setPageTitleText(MessageFormat.format(title, (Object[])param));
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
        try {
            handleButton1Request(getCachedPolicy());
        } catch (AMConsoleException e) {
            debug.warning(
                "ResponseProviderEditViewBean.handleButton1Request", e);
            redirectToStartURL();
        }
    }

    private void handleButton1Request(CachedPolicy cachedPolicy) 
        throws ModelControlException
    {
        ResponseProvider deleted = null;
        submitCycle = true;
        String origName = (String)getPageSessionAttribute(
            EDIT_RESPONSEPROVIDER_NAME);
        Policy policy = cachedPolicy.getPolicy();

        try {
            ResponseProvider provider = createResponseProvider();
            if (provider != null) {
                String name = (String)propertySheetModel.getValue(
                    RESPONSEPROVIDER_NAME);

                if (origName.equals(name)) {
                    policy.replaceResponseProvider(name, provider);
                } else {
                    deleted = policy.removeResponseProvider(origName);
                    policy.addResponseProvider(name, provider);
                }

                deleted = null;
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.responseProvider.updated");
                cachedPolicy.setPolicyModified(true);
            }
        } catch (NameAlreadyExistsException e) {
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
                    policy.addResponseProvider(origName, deleted);
                } catch (NameAlreadyExistsException e) {
                    debug.warning(
                        "ResponseProviderEditViewBean.handleButton1Request",e);
                }
            }
        }
        forwardTo();
    }

    protected Map getDefaultValues() {
        Map values = null;
        String providerName = (String)getPageSessionAttribute(
            ResponseProviderOpViewBeanBase.PG_SESSION_PROVIDER_NAME);

        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            ResponseProvider provider = policy.getResponseProvider(
                providerName);
            values = provider.getProperties();
        } catch (NameNotFoundException e) {
            debug.warning("ResponseProviderEditViewBean.getDefaultValues",e);
        } catch (AMConsoleException e) {
            debug.warning("ResponseProviderEditViewBean.getDefaultValues",e);
        }
        return values;
    }

    protected boolean hasValues() {
        return true;
    }

    protected boolean isCreateViewBean() {
        return false;
    }

    protected String getBreadCrumbDisplayName() {
        PolicyModel model = (PolicyModel)getModel();
        String origName = (String)getPageSessionAttribute(
            EDIT_RESPONSEPROVIDER_NAME);
        String[] arg = {origName};
        return MessageFormat.format(
            model.getLocalizedString("breadcrumbs.editResponseProvider"), 
            (Object[])arg);
    }

    protected boolean startPageTrail() {
      return false;
    }

}
