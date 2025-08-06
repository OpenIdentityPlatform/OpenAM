/*
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
 * $Id: UMChangeUserPasswordViewBean.java,v 1.6 2009/12/12 01:34:11 babysunil Exp $
 *
 * Portions Copyrighted 2012-2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.console.user;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.StaticTextField;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.CloseWindowViewBean;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.idm.EntityEditViewBean;
import com.sun.identity.console.realm.RMRealmViewBeanBase;
import com.sun.identity.console.user.model.UMChangeUserPasswordModel;
import com.sun.identity.console.user.model.UMChangeUserPasswordModelImpl;
import com.sun.identity.shared.encode.Hash;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCPassword;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import org.forgerock.openam.ldap.LDAPUtils;

import java.text.MessageFormat;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

public class UMChangeUserPasswordViewBean
    extends RMRealmViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/user/UMChangeUserPassword.jsp";

    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String PAGETITLE = "pgtitle";
    private static final String ATTR_PASSWORD = "tfPassword";
    private static final String REENTER_PASSWORD = "tfConfirmPassword";
    private static final String ATTR_OLD_PASSWORD = "tfOldPassword";
    private static final String FORM_TOKEN = "frmToken";
    private static boolean oldapicall = true;

    private CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    private boolean submitCycle = false;

    public UMChangeUserPasswordViewBean() {
        super("UMChangeUserPassword");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
            createPageTitleModel();
            createPropertyModel();
            registerChildren();
        }
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.close");
    }

    protected void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyUserChangePassword.xml"));
        propertySheetModel.clear();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PAGETITLE, CCPageTitle.class);
        ptModel.registerChildren(this);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(FORM_TOKEN, StaticTextField.class);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(FORM_TOKEN)) {
            view = new StaticTextField(this, FORM_TOKEN, getFormToken());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private String getFormToken() {
        SSOToken userToken = getModel().getUserSSOToken();
        String value = "";
        if (userToken != null) {
            value = Hash.hash(userToken.getTokenID().toString());
        }
        return value;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        UMChangeUserPasswordModel model = (UMChangeUserPasswordModel) getModel();
        String userId = (String) getPageSessionAttribute(EntityEditViewBean.UNIVERSAL_ID);
        if (userId == null) {
            userId = model.getUserName();
        }
        String loggedinUser = model.getUserName();

        //check if the enabled flag is set to true and if the user being edited
        //is same as logged in user
        if (model.isOldPasswordRequired()
                && LDAPUtils.rdnValueFromDn(userId).equalsIgnoreCase(LDAPUtils.rdnValueFromDn(loggedinUser))) {
            oldapicall = false;
        } else {
            CCPassword pwdtag = (CCPassword) getChild(ATTR_OLD_PASSWORD);
            pwdtag.setDisabled(true);
            oldapicall = true;
        }
        String[] arg = {AMFormatUtils.getIdentityDisplayName(model, userId)};
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString("page.title.user.change.password"),
            (Object[]) arg));
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new UMChangeUserPasswordModelImpl(
            req, getPageSessionAttributes());
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles save password options request.
     *
     * @param event Request invocation event.
     * @throws ModelControlException if action table model state cannot be 
     *               restored.
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        HttpServletRequest req = event.getRequestContext().getRequest();
        String formToken = req.getParameter(FORM_TOKEN);
        if (formToken == null || formToken.isEmpty() || !formToken.equals(getFormToken())) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", "Invalid form token");
            forwardTo();
            return;
        }
        UMChangeUserPasswordModel model = (UMChangeUserPasswordModel) getModel();
        String userId = (String) getPageSessionAttribute(EntityEditViewBean.UNIVERSAL_ID);
        if (userId == null) {
            userId = model.getUserName();
        }
        String pwd = (String) propertySheetModel.getValue(ATTR_PASSWORD);
        String reenter = (String) propertySheetModel.getValue(REENTER_PASSWORD);
        String oldPwd = (String) propertySheetModel.getValue(ATTR_OLD_PASSWORD);
        pwd = pwd.trim();
        reenter = reenter.trim();
        if (pwd.length() == 0) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "user.change.password.missing.password.message");
        } else if (!pwd.equals(reenter)) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "user.change.password.mismatch.password.message");
        } else {
            if (!oldapicall) {
                if (oldPwd.length() == 0) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        "user.change.password.missing.old.password.message");
                } else {
                    try {
                        //change password after validating old password
                        model.changePwd(userId, oldPwd, pwd);
                        setInlineAlertMessage(CCAlert.TYPE_INFO, 
                            "message.information",
                            "user.change.password.modified.password.message");
                    } catch (AMConsoleException e) {
                        setInlineAlertMessage(CCAlert.TYPE_ERROR, 
                            "message.error", e.getMessage());
                    }
                }

            } else {
                try {
                    model.changePassword(userId, pwd);
                    setInlineAlertMessage(CCAlert.TYPE_INFO,
                        "message.information",
                        "user.change.password.modified.password.message");
                } catch (AMConsoleException e) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                }
            }
        }

        forwardTo();
    }

    /**
     * Handles close browser window request.
     *
     * @param event Request invocation event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        CloseWindowViewBean vb = (CloseWindowViewBean)getViewBean(
            CloseWindowViewBean.class);
        vb.forwardTo(getRequestContext());
    }
}
