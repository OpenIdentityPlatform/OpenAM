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
 * $Id: DelegationPropertiesViewBean.java,v 1.2 2008/06/25 05:42:51 qcheng Exp $
 *
 */

package com.sun.identity.console.delegation;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationModel;
import com.sun.identity.console.delegation.model.DelegationModelImpl;
import com.sun.identity.console.property.PrivilegeXMLBuilder;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.delegation.DelegationPrivilege;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class DelegationPropertiesViewBean
    extends RealmPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/delegation/DelegationProperties.jsp";
    public static final String CURRENT_IDENTITY = "currentIdentity";

    private AMPropertySheetModel psModel;
    private CCPageTitleModel ptModel;

    private static final String PAGETITLE = "pgtitle";
    protected static final String DELEGATION_PROPERTIES =
        "DelegationProperties";
    private boolean submitCycle;
    private boolean hasPrivileges;

    /**
     * Creates a authentication domains view bean.
     */
    public DelegationPropertiesViewBean() {
        super("DelegationProperties");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            if (curRealm != null) {
                super.initialize();
                initialized = true;
                createPageTitleModel();
                createPropertyModel();
                registerChildren();
            }
        }
    }

    protected void registerChildren() {
        registerChild(DELEGATION_PROPERTIES, AMPropertySheet.class);
        psModel.registerChildren(this);
        ptModel.registerChildren(this);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(DELEGATION_PROPERTIES)) {
            view = new AMPropertySheet(this, psModel, name);
        } else if (psModel.isChildSupported(name)) {
            view = psModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        DelegationModel model = (DelegationModel)getModel();

        if (model != null) {
            if (!submitCycle && hasPrivileges) {
                AMPropertySheet ps =
                    (AMPropertySheet)getChild(DELEGATION_PROPERTIES);
                psModel.clear();
                ps.setAttributeValues(getPrivileges(model), model);
            }
            setPageTitle(model);
        }

        if (!hasPrivileges) {
            disableButton("button1", true);
            disableButton("button2", true);
        }
    }

    private Map getPrivileges(DelegationModel model) {
        Map map = null; 
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String uid = (String)getPageSessionAttribute(CURRENT_IDENTITY);

        try {
            Set privileges = model.getPrivileges(curRealm, uid);

            if ((privileges != null) && !privileges.isEmpty()) {
                map = new HashMap(privileges.size() *2);

                for (Iterator iter = privileges.iterator(); iter.hasNext(); ) {
                    DelegationPrivilege p = (DelegationPrivilege)iter.next();
                    Set val = new HashSet(2);
                    val.add(Boolean.TRUE.toString());
                    map.put(p.getName(), val);
                }
            }
        } catch (AMConsoleException a) {
            setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                "nopermissions.message");
        }

        return (map == null) ? Collections.EMPTY_MAP : map;
    }

    /*
    * Get the name of the current realm and add it to the title of the 
    * properties page. 
    * First attempt to get the value from the 
    */
    private void setPageTitle(DelegationModel model) {
        String[] tmp = { getDisplayName() };
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString(
                "page.title.realms.properties"), (Object[])tmp));
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new DelegationModelImpl(req, getPageSessionAttributes());
    }

    private void createPropertyModel() {
        PrivilegeXMLBuilder builder = PrivilegeXMLBuilder.getInstance();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        String xml = builder.getXML(curRealm, getModel());

        if (xml.length() > 0) {
            psModel = new AMPropertySheetModel(xml);
            psModel.clear();
            hasPrivileges = true;
        } else {
            // This happens when we cannot get privileges from SM.
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertyBlank.xml"));
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "delegation.no.privileges");
        }
    }

    /**
     * Handles save button request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        DelegationModel model = (DelegationModel)getModel();
        String uid = (String)getPageSessionAttribute(CURRENT_IDENTITY);
        AMPropertySheet ps = (AMPropertySheet)getChild(DELEGATION_PROPERTIES);
        String realm = 
            (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);

        try {
            PrivilegeXMLBuilder builder = PrivilegeXMLBuilder.getInstance();
            Set privileges = builder.getAllPrivileges(realm, model);
            Map values = ps.getAttributeValues(privileges);
            model.setPrivileges(realm, uid, values);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "delegation.privilege.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles "back to" page request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        DelegationViewBean vb = (DelegationViewBean)
            getViewBean(DelegationViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }


    protected String getBackButtonLabel() {
        String[] arg = {
            getModel().getLocalizedString("table.delegation.summary") };

        return MessageFormat.format(
            getModel().getLocalizedString("back.button"), (Object[])arg);
    }

    protected String getBreadCrumbDisplayName() {
        String message = "breadcrumbs.realm.pivilege.editPrivilege";
        DelegationModel model = (DelegationModel)getModel();

        String[] tmp = { getDisplayName() };
        return MessageFormat.format(
            model.getLocalizedString(message), (Object[])tmp);
    }

    protected boolean startPageTrail() {
        return false;
    }

    /**
     * Retrieve the name from the current object and convert it to a
     * displayable format.
     */
    private String getDisplayName() {
        String displayName = "";
        DelegationModel model = (DelegationModel)getModel();
        String uid = (String)getPageSessionAttribute(CURRENT_IDENTITY);
        try {
            AMIdentity entity = IdUtils.getIdentity(
                model.getUserSSOToken(), uid);
            displayName = AMFormatUtils.getIdentityDisplayName(model, entity);
        } catch (IdRepoException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
        }
        return displayName;
    }
}
