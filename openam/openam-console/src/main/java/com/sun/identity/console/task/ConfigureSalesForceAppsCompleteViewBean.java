/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConfigureSalesForceAppsCompleteViewBean.java,v 1.3 2009/07/28 17:45:40 babysunil Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock Inc.
 */
package com.sun.identity.console.task;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.task.model.TaskModel;
import com.sun.identity.console.task.model.TaskModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Create register product UI.
 */
public class ConfigureSalesForceAppsCompleteViewBean
        extends AMPrimaryMastHeadViewBean {

    public static final String DEFAULT_DISPLAY_URL =
            "/console/task/ConfigureSalesForceAppsComplete.jsp";
    protected static final String PROPERTIES = "propertyAttributes";
    private AMPropertySheetModel psModel;
    private CCPageTitleModel ptModel;
    private static final String PGTITLE_ONE_BTNS =
            "pgtitleOneBtns";
    private static final String ENTITY_ID = "entityId";
    private boolean initialized;

    /**
     * Creates a salesforce complete view bean.
     */
    public ConfigureSalesForceAppsCompleteViewBean() {
        super("ConfigureSalesForceAppsComplete");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            initialized = true;
            createPropertyModel();
            createPageTitleModel();
            registerChildren();
            super.initialize();
        }
        super.registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTIES, AMPropertySheet.class);
        registerChild(PGTITLE_ONE_BTNS, CCPageTitle.class);
        psModel.registerChildren(this);
        ptModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE_ONE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTIES)) {
            view = new AMPropertySheet(this, psModel, name);
        } else if ((psModel != null) && psModel.isChildSupported(name)) {
            view = psModel.createChild(this, name, getModel());
        } else if ((ptModel != null) && ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    public void beginDisplay(DisplayEvent event)
            throws ModelControlException {
        try {
            super.beginDisplay(event);
            HttpServletRequest req = getRequestContext().getRequest();
            String realm = req.getParameter("realm");
            String idp = req.getParameter("idp");
            String attrMapp = req.getParameter("attrMapp");
            String spEntityId = req.getParameter(ENTITY_ID);
            setPageSessionAttribute(ENTITY_ID, spEntityId);
            setPageSessionAttribute("entityRealm", realm);
            TaskModel model = (TaskModel) getModelInternal();
            Map values = model.getConfigureSalesForceAppsURLs(realm, idp, attrMapp);

            String domainId = getModel().getLocalizedString("salesforce.link");           
            String msg = "<ul>";
            String orgMsg = getModel().getLocalizedString(
                        "configure.salesforce.apps.complete.urllist");
            msg += "<li>";
            msg += MessageFormat.format(orgMsg, domainId, domainId);
                msg += "</li>";
            msg += "</ul>";

            values.put("urllist", returnEmptySetIfValueIsNull(msg));
            AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
            ps.setAttributeValues(values, model);
   
        } catch (AMConsoleException ex) {
            Logger.getLogger(ConfigureSalesForceAppsCompleteViewBean.class.getName()).log(Level.SEVERE, null, ex);
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    ex.getMessage());
        }
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", "button.finish");
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new TaskModelImpl(req, getPageSessionAttributes());
    }

    private void createPropertyModel() {
        psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyConfigureSalesForceAppsComplete.xml"));
        psModel.setValue("buttonDownloadCert",
                "configure.salesforce.apps.complete.certificate.download");
        psModel.clear();
    }

    /**
     * Handles finish button request.
     * 
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
            throws ModelControlException {
        String acsUrl = getDisplayFieldStringValue("SalesforceLoginURL");
        try {
            if ((acsUrl != null) && (acsUrl.length() > 0)) {
                String realm = (String) getPageSessionAttribute("entityRealm");
                String entityId = (String) getPageSessionAttribute(ENTITY_ID);
                TaskModel model = (TaskModel) getModelInternal();
                model.setAcsUrl(realm, entityId, acsUrl);
                HomeViewBean vb = (HomeViewBean) getViewBean(HomeViewBean.class);
                backTrail();
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } else if ((acsUrl == null) || !(acsUrl.length() > 0)) {
                ConfigureSalesForceAppsFinishWarningViewBean vb =
                        (ConfigureSalesForceAppsFinishWarningViewBean) getViewBean(
                        ConfigureSalesForceAppsFinishWarningViewBean.class);
                vb.forwardTo(getRequestContext());
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }

    }

    /**
     * Handles Verification Certificate download button request.
     * Sends Verification Certificate to the ServletResponse output stream.  
     * @param event Request invocation event
     */
    public void handleButtonDownloadCertRequest(RequestInvocationEvent event)
            throws ModelControlException {
        RequestContext reqContext = event.getRequestContext();
        HttpServletResponse resp = reqContext.getResponse();
        String cert = (String) psModel.getValue("PubKey");

        ServletOutputStream op = null;
        try {
            int length = 0;
            op = resp.getOutputStream();

            //  Set the response
            resp.setContentType("application/octet-stream");
            resp.setContentLength(cert.length());
            resp.setHeader("Content-Disposition",
                    "attachment; filename=\"" + "OpenSSOCert.txt" + "\"");

            //  Stream to the requester.
            int BUFSIZE = cert.length();
            byte[] bbuf = new byte[BUFSIZE];
            InputStream is = new ByteArrayInputStream(cert.getBytes());
            DataInputStream in = new DataInputStream(is);

            while ((in != null) && ((length = in.read(bbuf)) != -1)) {
                op.write(bbuf, 0, length);
            }

            in.close();
            op.flush();
        } catch (IOException ex) {
            debug.error("ConfigureSalesForceAppsCompleteViewBean.uploadCert", ex);
            setInlineAlertMessage(CCAlert.TYPE_ERROR,
                    "configure.salesforce.apps.complete.certificate.download.error",
                    ex.getMessage());
        } finally {
            if (op != null) {
                try {
                    op.close();
                } catch (IOException ex) {
                    debug.error("ConfigureSalesForceAppsCompleteViewBean.uploadCert", ex);
                    setInlineAlertMessage(CCAlert.TYPE_ERROR,
                            "configure.SalesForce.apps.complete.certificate.download.error",
                            ex.getMessage());
                }
            }
        }
    }

    private Set returnEmptySetIfValueIsNull(String str) {
        Set set = Collections.EMPTY_SET;
        if (str != null) {
            set = new HashSet(2);
            set.add(str);
        }
        return set;
    }
}
