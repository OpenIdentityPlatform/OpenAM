/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.console.task;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.task.model.SocialAuthNModel;
import com.sun.identity.console.task.model.SocialAuthNModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.SortedSet;

/**
 * JATO view bean for configuring social login providers (Google, Facebook etc) via OpenID Connect or some other
 * mechanism. Assumes that all mechanisms are loosely based on OIDC -- i.e., OAuth2 with some form of identity.
 *
 * @since 12.0.0
 */
public class ConfigureSocialAuthNViewBean extends AMPrimaryMastHeadViewBean {

    public static final String DEFAULT_DISPLAY_URL = "/console/task/ConfigureSocialAuthN.jsp";
    private static final String PROPERTY_DIR = "com/sun/identity/console/";
    private static final String KNOWN_PROVIDER_PROPERTIES =
            PROPERTY_DIR + "propertyConfigureSocialAuthNKnownProvider.xml";
    private static final String UNKNOWN_PROVIDER_PROPERTIES =
            PROPERTY_DIR + "propertyConfigureSocialAuthNUnknownProvider.xml";

    private static final String TITLE_MESSAGE = "configure.social.authentication.title.message";

    private static final String PAGETITLE = "pgtitle";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private static final String REALM_FIELD = "tfRealm";
    private static final String REDIRECT_URL_FIELD = "tfRedirectUrl";
    private static final String PROVIDER_ATTR = "provider";

    private final CCPageTitleModel ptModel;
    private final AMPropertySheetModel propertySheetModel;

    public ConfigureSocialAuthNViewBean() {
        super("ConfigureSocialAuthN");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        this.ptModel = createPageTitleModel();
        this.propertySheetModel = createPropertyModel();
        registerChildren();
    }

    @Override
    public void forwardTo(RequestContext rc) {
        super.forwardTo(rc);
    }


    @Override
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new SocialAuthNModelImpl(req, getPageSessionAttributes());
    }

    @Override
    protected void registerChildren() {
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PAGETITLE, CCPageTitle.class);
        super.registerChildren();
    }

    @Override
    protected View createChild(String name) {
        View view;

        if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private CCPageTitleModel createPageTitleModel() {
        CCPageTitleModel model = new CCPageTitleModel(getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        model.setValue("button1", "button.create");
        model.setValue("button2", "button.cancel");

        return model;
    }

    private AMPropertySheetModel createPropertyModel() {
        // NB: at this point we cannot access the RequestContext so do not know if the provider is known or not.
        // Default to unknown and then switch in the beginDisplay method
        AMPropertySheetModel model = new AMPropertySheetModel(openPropertySheetXml(UNKNOWN_PROVIDER_PROPERTIES));
        model.clear();
        return model;
    }

    @Override
    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        HttpServletRequest req = getRequestContext().getRequest();

        try {
            super.beginDisplay(event);
            SocialAuthNModel model = (SocialAuthNModel) getModel();

            // If provider is known then use the simplified property sheet
            if (model.isKnownProvider()) {
                propertySheetModel.setDocument(openPropertySheetXml(KNOWN_PROVIDER_PROPERTIES));
                final String titleMessage = model.getLocalizedString(TITLE_MESSAGE);
                ptModel.setPageTitleText(MessageFormat.format(titleMessage, model.getProviderDisplayName()));
                ptModel.setPageTitleHelpMessage(model.getLocalizedProviderHelpMessage());
            }

            SortedSet<String> realms = model.getRealms();

            CCDropDownMenu menuRealm = (CCDropDownMenu) getChild(REALM_FIELD);
            menuRealm.setOptions(createOptionList(realms));

            String realm = req.getParameter("realm");
            if (realm != null && !realm.trim().isEmpty()) {
                setDisplayFieldValue(REALM_FIELD, realm);
            }

            setDisplayFieldValue(REDIRECT_URL_FIELD, model.getDefaultRedirectUrl());

            String provider = req.getParameter("type");
            if (provider != null && !provider.trim().isEmpty()) {
                setPageSessionAttribute(PROVIDER_ATTR, provider.trim());
            }

        } catch (AMConsoleException ex) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", ex.getMessage());
        }
    }

    private InputStream openPropertySheetXml(final String file) {
        return getClass().getClassLoader().getResourceAsStream(file);
    }
}
