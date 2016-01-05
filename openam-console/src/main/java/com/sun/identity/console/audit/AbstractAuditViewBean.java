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
import static com.sun.identity.console.base.model.AMAdminConstants.*;
import static com.sun.identity.console.base.model.AMPropertySheetModel.*;
import static com.sun.web.ui.view.alert.CCAlert.*;
import static java.util.Collections.singletonList;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.audit.model.AbstractAuditModel;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.base.model.SMSubConfig;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.table.CCActionTable;
import org.forgerock.openam.utils.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract Audit configuration UI view bean.
 *
 * @since 13.0.0
 */
public abstract class AbstractAuditViewBean extends AMServiceProfileViewBeanBase {

    private final String viewBeanPath;
    private boolean populatedSubConfigTable;

    /**
     * Create a new {@code AbstractAuditViewBean}.
     *
     * @param name Name of the view bean.
     * @param url Path to the view bean.
     */
    public AbstractAuditViewBean(String name, String url) {
        super(name, url, AUDIT_SERVICE);
        viewBeanPath = "../audit/" + name;
    }

    /**
     * Get the view bean responsible for selecting the event handler to create.
     *
     * @return The view bean.
     */
    protected abstract ViewBean getSelectViewBean();

    /**
     * Get the view bean responsible for editing the event handler to create.
     *
     * @return The view bean.
     */
    protected abstract ViewBean getEditViewBean();

    @Override
    protected void initialize(String serviceName) {
        HttpServletRequest req = RequestManager.getRequestContext().getRequest();
        String location = req.getParameter("Location");
        if (isNotBlank(location)) {
            setPageSessionAttribute(CURRENT_REALM, hexToString(location));
        }
        super.initialize(serviceName);
    }

    @Override
    protected View createChild(String name) {
        if (!populatedSubConfigTable && name.equals(TBL_SUB_CONFIG)) {
            populatedSubConfigTable = true;
            SerializedField szCache = (SerializedField) getChild(SZ_CACHE);
            populateTableModel(szCache.<List<SMSubConfig>>getSerializedObj());
        }
        return super.createChild(name);
    }

    @Override
    public void forwardTo(RequestContext reqContext) throws NavigationException {
        initialize(AUDIT_SERVICE);
        super.forwardTo(reqContext);
    }

    @Override
    public void beginDisplay(DisplayEvent event) throws ModelControlException {
        super.beginDisplay(event);

        AbstractAuditModel model = (AbstractAuditModel) getModel();
        if (!submitCycle) {
            try {
                populateTableModel(model.getEventHandlerConfigurations());
                resetButtonState(TBL_SUB_CONFIG_BUTTON_DELETE);
                disableButton(TBL_SUB_CONFIG_BUTTON_ADD, false);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(TYPE_ERROR, ERROR_MESSAGE, e.getMessage());
            }
        }
    }

    @Override
    protected String getPropertySheetXML(AMServiceProfileModel model) throws AMConsoleException {
        String realm = isGlobalService() ? "/" : (String) getPageSessionAttribute(CURRENT_REALM);
        return model.getPropertySheetXML(realm, getName(), getClass().getName());
    }

    /**
     * @return {@code true} if service is displaying global config.
     */
    abstract boolean isGlobalService();

    private void populateTableModel(List<SMSubConfig> subConfigs) {
        CCActionTable tbl = (CCActionTable) getChild(TBL_SUB_CONFIG);
        CCActionTableModel tblModel = (CCActionTableModel) tbl.getModel();
        tblModel.clearAll();

        if (CollectionUtils.isEmpty(subConfigs)) {
            return;
        }
        SerializedField szCache = (SerializedField) getChild(SZ_CACHE);
        List<SMSubConfig> cache = new ArrayList<>(subConfigs.size());
        boolean firstEntry = true;

        for (SMSubConfig conf : subConfigs) {
            if (firstEntry) {
                firstEntry = false;
            } else {
                tblModel.appendRow();
            }
            tblModel.setValue(TBL_SUB_CONFIG_DATA_NAME, conf.getName());
            tblModel.setValue(TBL_SUB_CONFIG_HREF_NAME, conf.getName());
            tblModel.setValue(TBL_SUB_CONFIG_DATA_TYPE, conf.getType());
            cache.add(conf);
        }
        szCache.setValue(cache);
    }

    /**
     * Called on request from the UI to add a new event handler.
     *
     * @param event Request Invocation Event.
     */
    @SuppressWarnings("unused")
    public void handleTblSubConfigButtonAddRequest(RequestInvocationEvent event) {
        ViewBean vb = getSelectViewBean();
        setPageSessionAttribute(PG_SESSION_PROFILE_VIEWBEANS, (Serializable) singletonList(viewBeanPath));
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Called on request from the UI to delete an event handler.
     *
     * @param event Request Invocation Event.
     */
    @SuppressWarnings("unused")
    public void handleTblSubConfigButtonDeleteRequest(RequestInvocationEvent event) throws ModelControlException {
        submitCycle = true;
        CCActionTable tbl = (CCActionTable) getChild(TBL_SUB_CONFIG);
        tbl.restoreStateData();
        CCActionTableModel tblModel = (CCActionTableModel) tbl.getModel();
        Integer[] selected = tblModel.getSelectedRows();
        SerializedField szCache = (SerializedField) getChild(SZ_CACHE);
        List list = szCache.getSerializedObj();
        Set<String> names = new HashSet<>(selected.length * 2);

        for (Integer index : selected) {
            SMSubConfig sc = (SMSubConfig) list.get(index);
            names.add(sc.getName());
        }

        try {
            AbstractAuditModel model = (AbstractAuditModel) getModel();
            model.deleteEventHandles(names);

            if (selected.length == 1) {
                setInlineAlertMessage(TYPE_INFO, INFORMATION_MESSAGE, "event.handler.message.deleted");
            } else {
                setInlineAlertMessage(TYPE_INFO, INFORMATION_MESSAGE, "event.handler.message.deleted.plural");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(TYPE_ERROR, ERROR_MESSAGE, e.getMessage());
        }

        submitCycle = false;
        forwardTo();
    }

    /**
     * Called on request from the UI to edit an event handler.
     *
     * @param event Request Invocation Event.
     */
    @SuppressWarnings("unused")
    public void handleTblSubConfigHrefNameRequest(RequestInvocationEvent event) {
        String auditHandler = (String) getDisplayFieldValue(TBL_SUB_CONFIG_HREF_NAME);
        setPageSessionAttribute(AUDIT_HANDLER_NAME, auditHandler);
        setPageSessionAttribute(SERVICE_NAME, serviceName);
        setPageSessionAttribute(PG_SESSION_PROFILE_VIEWBEANS, (Serializable) singletonList(viewBeanPath));
        ViewBean vb = getEditViewBean();
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Called on request from the UI to return to the previous page.
     *
     * @param event Request Invocation Event.
     */
    @SuppressWarnings("unused")
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        try {
            String name = (String) getPageSessionAttribute(SAVE_VB_NAME);
            ViewBean vb = getViewBean(Class.forName(name));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            debug.warning("AbstractAuditViewBean.handleButton3Request:", e);
        }
    }

    @Override
    protected String getBreadCrumbDisplayName() {
        AMModel model = getModel();
        String serviceName = (String) getPageSessionAttribute(SERVICE_NAME);
        Object[] arg = {model.getLocalizedServiceName(serviceName)};
        return MessageFormat.format(model.getLocalizedString("breadcrumbs.services.edit"), arg);
    }

    @Override
    protected boolean startPageTrail() {
        return false;
    }
}
