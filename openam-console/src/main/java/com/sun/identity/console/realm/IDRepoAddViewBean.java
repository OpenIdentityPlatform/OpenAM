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
 * $Id: IDRepoAddViewBean.java,v 1.3 2009/11/19 23:45:59 asyhuang Exp $
 *
 */

package com.sun.identity.console.realm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.realm.model.IDRepoModel;
import com.sun.identity.idm.common.IdRepoUtils;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;

public class IDRepoAddViewBean
    extends IDRepoOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/realm/IDRepoAdd.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    IDRepoAddViewBean(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    public IDRepoAddViewBean() {
        super("IDRepoAdd", DEFAULT_DISPLAY_URL);
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
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.finish");
        ptModel.setValue("button3", "button.cancel");
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        
        IDRepoModel model = (IDRepoModel)getModel();
        String i18nName = (String)propertySheetModel.getValue(
            IDREPO_TYPE_NAME);
        String title = model.getLocalizedString(
            "page.title.realm.idrepo.create");
        String[] param = {i18nName};
        ptModel.setPageTitleText(MessageFormat.format(title, (Object[])param));
    }

    /**
    * Sets the default values viewed in the create page for new 
    * data stores. 
    */
    protected void setDefaultValues(String type) {
        IDRepoModel model = (IDRepoModel)getModel();
        Map defaultValues = model.getDefaultAttributeValues(type);
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        ps.setAttributeValues(defaultValues, model);
    }

    public void handleButton1Request(RequestInvocationEvent event) {
        IDRepoSelectTypeViewBean vb = (IDRepoSelectTypeViewBean)getViewBean(
            IDRepoSelectTypeViewBean.class);
        unlockPageTrailForSwapping();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        IDRepoModel model = (IDRepoModel)getModel();
        AMPropertySheet prop = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        String idRepoType = (String)propertySheetModel.getValue(IDREPO_TYPE);
        String idRepoName = (String)propertySheetModel.getValue(IDREPO_NAME);
        Map defaultValues = model.getDefaultAttributeValues(idRepoType);

        if (idRepoName.trim().length() > 0) {
            try {
                boolean LoadSchema = false;
                Map values = prop.getAttributeValues(defaultValues.keySet());
                String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);

                Set set = (HashSet) values.get("idRepoLoadSchema");
                if( set != null){
                    Iterator i = set.iterator();
                    if(i.hasNext()){
                        String loadingSchemaFlag = (String) i.next();
                        if(loadingSchemaFlag.equals("true") &&
                            IdRepoUtils.hasIdRepoSchema(idRepoType)){
                            LoadSchema=true;
                        }
                    }
                }

                model.createIDRepo(realmName, idRepoName, idRepoType, values);
                               
                if(LoadSchema==true){
                    ServletContext servletCtx = event.getRequestContext().getServletContext();
                    model.loadIdRepoSchema(idRepoName,realmName,servletCtx);
                }
                
                forwardToIDRepoViewBean();
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "idrepo.missing.idRepoName");
            forwardTo();
        }
    }

    protected boolean isCreateViewBean() {
        return true;
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.realm.idrepo.addIdRepo";
    }

    protected boolean startPageTrail() {
        return false;
    }
}
