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
 * Copyright 2015 ForgeRock AS.
 */
package com.sun.identity.console.audit;

import static com.sun.identity.console.audit.AuditConsoleConstants.*;
import static com.sun.identity.console.base.AMServiceProfile.PG_SESSION_PROFILE_VIEWBEANS;
import static com.sun.web.ui.view.alert.CCAlert.TYPE_ERROR;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.audit.model.AbstractAuditModel;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

import java.util.List;
import java.util.Set;

/**
 * Abstract view bean for selecting a new Audit Event Handler.
 *
 * @since 13.0.0
 */
public abstract class AbstractEventHandlerSelectViewBean extends AMPrimaryMastHeadViewBean {

    private static final String RB_EVENT_HANDLER = "rbEventHandler";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;

    /**
     * Create a new {@code AbstractEventHandlerEditViewBean}.
     *
     * @param name Name of the view bean.
     * @param url Path to the view bean.
     */
    public AbstractEventHandlerSelectViewBean(String name, String url) {
        super(name);
        setDefaultDisplayURL(url);
    }

    /**
     * Get the view bean responsible for creating a new event handler.
     *
     * @return The view bean.
     */
    protected abstract ViewBean getAddViewBean();

    @Override
    protected void initialize() {
        if (initialized) {
            return;
        }
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
        initialized = true;
    }

    @Override
    protected void registerChildren() {
        ptModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
        registerChild(PAGE_TITLE_TWO_BUTTONS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        super.registerChildren();
    }

    @Override
    protected View createChild(String name) {
        View view = null;

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
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream("com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.next");
        ptModel.setValue("button2", "button.cancel");
    }

    private void createPropertyModel() {
        propertySheetModel = new AMPropertySheetModel(getClass().getClassLoader()
                .getResourceAsStream("com/sun/identity/console/propertyAuditConfigSelect.xml"));
        propertySheetModel.clear();
    }

    @Override
    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        super.beginDisplay(event);
        try {
            AbstractAuditModel model = (AbstractAuditModel) getModel();
            Set<String> auditHandlerTypes = model.getEventHandlerTypeNames();

            OptionList optionList = new OptionList();
            for (String type : auditHandlerTypes) {
                optionList.add(type, type);
            }

            CCRadioButton rb = (CCRadioButton) getChild(RB_EVENT_HANDLER);
            rb.setOptions(optionList);
            rb.setValue(optionList.getValue(0));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(TYPE_ERROR, ERROR_MESSAGE, e.getMessage());
        }
    }

    /**
     * Handles Cancel request.
     *
     * @param event Request invocation event
     */
    @SuppressWarnings("unused")
    public void handleButton2Request(RequestInvocationEvent event) {
        String url = (String) ((List) getPageSessionAttribute(PG_SESSION_PROFILE_VIEWBEANS)).get(0);
        AMPostViewBean vb = (AMPostViewBean) getViewBean(AMPostViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.setTargetViewBeanURL(url);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles Next request.
     *
     * @param event Request invocation event
     */
    @SuppressWarnings("unused")
    public void handleButton1Request(RequestInvocationEvent event) throws ModelControlException {
        String subSchema = (String) getDisplayFieldValue(RB_EVENT_HANDLER);
        setPageSessionAttribute(AUDIT_HANDLER_TYPE, subSchema);
        ViewBean vb = getAddViewBean();
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.services.subschema.select";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
