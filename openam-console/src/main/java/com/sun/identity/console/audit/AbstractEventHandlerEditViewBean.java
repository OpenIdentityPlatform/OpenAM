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
 * Copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.console.audit;

import static com.sun.identity.console.audit.AuditConsoleConstants.*;
import static com.sun.identity.console.base.AMServiceProfile.PG_SESSION_PROFILE_VIEWBEANS;
import static com.sun.identity.console.base.model.AMAdminConstants.CURRENT_REALM;
import static com.sun.web.ui.view.alert.CCAlert.*;
import static java.text.MessageFormat.format;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.audit.model.AbstractAuditModel;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

import java.util.List;
import java.util.Map;

/**
 * Abstract view bean for editing a new Audit Event Handler.
 *
 * @since 13.0.0
 */
public abstract class AbstractEventHandlerEditViewBean extends AMPrimaryMastHeadViewBean {

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private boolean submitCycle;

    protected String serviceName;

    /**
     * Create a new {@code AbstractEventHandlerEditViewBean}.
     *
     * @param name Name of the view bean.
     * @param url Path to the view bean.
     */
    public AbstractEventHandlerEditViewBean(String name, String url) {
        super(name);
        setDefaultDisplayURL(url);
    }

    @Override
    protected void initialize() {
        if (initialized || isEmpty((String) getPageSessionAttribute(AUDIT_HANDLER_NAME))) {
            return;
        }
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
        initialized = true;
    }

    @Override
    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PAGE_TITLE_TWO_BUTTONS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(SZ_CACHE, SerializedField.class);
    }

    @Override
    protected View createChild(String name) {
        View view;

        if (name.equals(PAGE_TITLE_TWO_BUTTONS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(getClass().getClassLoader().getResourceAsStream
                ("com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.cancel");
    }

    private void createPropertyModel() {
        try {
            AbstractAuditModel model = (AbstractAuditModel) getModel();
            String subConfigName = (String) getPageSessionAttribute(AUDIT_HANDLER_NAME);
            String realm = isGlobalService() ? "/" : (String) getPageSessionAttribute(CURRENT_REALM);
            propertySheetModel = new AMPropertySheetModel(
                    model.getEditEventHandlerPropertyXML(realm, subConfigName, getClass().getName()));
            propertySheetModel.clear();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(TYPE_ERROR, ERROR_MESSAGE, e.getMessage());
        }
    }

    /**
     * @return {@code true} if service is displaying global config.
     */
    abstract boolean isGlobalService();

    @Override
    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        super.beginDisplay(event);
        AbstractAuditModel model = (AbstractAuditModel) getModel();
        String eventHandlerName = (String) getPageSessionAttribute(AUDIT_HANDLER_NAME);

        if (!submitCycle) {
            AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTY_ATTRIBUTE);
            propertySheetModel.clear();

            try {
                ps.setAttributeValues(model.getEventHandlerAttributeValues(eventHandlerName), model);
            } catch (AMConsoleException a) {
                setInlineAlertMessage(TYPE_WARNING, WARNING_MESSAGE, "noproperties.message");
            }
        }

        ptModel.setPageTitleText(format(model.getLocalizedString("event.handler.page.title.edit"), eventHandlerName));
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    @SuppressWarnings("unused")
    public void handleButton3Request(RequestInvocationEvent event) {
        backToProfileViewBean();
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    @SuppressWarnings("unused")
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles next button request.
     *
     * @param event Request invocation event.
     */
    @SuppressWarnings("unused")
    public void handleButton1Request(RequestInvocationEvent event) throws ModelControlException {
        submitCycle = true;
        AbstractAuditModel model = (AbstractAuditModel) getModel();
        AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTY_ATTRIBUTE);
        String subConfigName = (String) getPageSessionAttribute(AUDIT_HANDLER_NAME);

        try {
            Map orig = model.getEventHandlerAttributeValues(subConfigName);
            Map values = ps.getAttributeValues(orig, true, true, model);
            model.setEventHandlerAttributeValues(subConfigName, values);
            backToProfileViewBean();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(TYPE_ERROR, ERROR_MESSAGE, e.getMessage());
            forwardTo();
        }
    }

    private void backToProfileViewBean() {
        String url = (String) ((List) getPageSessionAttribute(PG_SESSION_PROFILE_VIEWBEANS)).get(0);
        AMPostViewBean vb = (AMPostViewBean) getViewBean(AMPostViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.setTargetViewBeanURL(url);
        vb.forwardTo(getRequestContext());
    }

    @Override
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.subconfig.edit";
    }

    @Override
    protected boolean startPageTrail() {
        return false;
    }

}
