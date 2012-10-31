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
 * $Id: CreateSAML2MetaDataViewBean.java,v 1.6 2008/12/02 22:20:13 asyhuang Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.federation.model.CreateMetaDataModel;
import com.sun.identity.console.federation.model.CreateMetaDataModelImpl;
import com.sun.identity.workflow.MetaTemplateParameters;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.editablelist.CCEditableList;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

public class CreateSAML2MetaDataViewBean
        extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/CreateSAML2MetaData.jsp";
    private static final String PROPERTIES = "propertyAttributes";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String AFFI_MEMBERS = "affimembers";
    private static final String PROTO_SAMLv2 = "samlv2";
    private static final String PROTO_IDFF = "idff";
    private static final String PROTO_WSFED = "wsfed";


    private CCPageTitleModel ptModel;
    private AMPropertySheetModel psModel;
    private String protocol;

    /**
     * Creates a authentication domains view bean.
     */
    public CreateSAML2MetaDataViewBean() {
        super("CreateSAML2MetaData");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            protocol = (String) getPageSessionAttribute("protocol");
            if ((protocol == null) || (protocol.trim().length() == 0)) {
                HttpServletRequest req = getRequestContext().getRequest();
                protocol = req.getParameter("p");
            }
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
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        psModel.registerChildren(this);
        ptModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE_TWO_BTNS)) {
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
        throws ModelControlException
    {
        super.beginDisplay(event);
        setPageSessionAttribute("protocol", protocol);
        AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
        ps.init();
        populateRealmData();
        psModel.setModel(AFFI_MEMBERS, new CCEditableListModel());
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        if (protocol.equals(PROTO_SAMLv2)) {
            ptModel.setPageTitleText("metadata.saml2.create.title");
        } else if (protocol.equals(PROTO_IDFF)) {
            ptModel.setPageTitleText("metadata.idff.create.title");
        } else {
            ptModel.setPageTitleText("metadata.wsfed.create.title");
        }
        ptModel.setValue("button1", "button.create");
        ptModel.setValue("button2", "button.cancel");
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new CreateMetaDataModelImpl(req, getPageSessionAttributes());
    }

    private void createPropertyModel() {
        if (protocol == null) {
            protocol = (String)getPageSessionAttribute("protocol");
        }        
        String xml;
        if (protocol.equals(PROTO_SAMLv2)) {
            xml =  "com/sun/identity/console/propertyCreateSAML2Entity.xml";
        } else if(protocol.equals(PROTO_IDFF)) {
            xml =  "com/sun/identity/console/propertyCreateIDFFEntity.xml";
        } else {
            xml =  "com/sun/identity/console/propertyCreateWSFedEntity.xml";
        }
        psModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xml));
        psModel.setModel(AFFI_MEMBERS, new CCEditableListModel());
        psModel.clear();
    }

    private void populateRealmData() {
        CreateMetaDataModel model = (CreateMetaDataModel)getModel();
        try{
            Set realmNames = new TreeSet(model.getRealmNames("/", "*"));
            CCDropDownMenu menu =
                (CCDropDownMenu)getChild("singleChoiceRealm");
            OptionList list = new OptionList();
            for (Iterator i = realmNames.iterator(); i.hasNext();) {
                String name = (String)i.next();
                list.add(getPath(name), name);
            }
            menu.setOptions(list);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles save button request.
     * save
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
	       setPageSessionAttribute(getTrackingTabIDName(),
	           AMAdminConstants.FED_TAB_ID);
        setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID,
            getTrackingTabIDName());
        CreateMetaDataModel model = (CreateMetaDataModel)getModel();
        try {
            String realm = getDisplayFieldStringValue("singleChoiceRealm");
            String entityId = getDisplayFieldStringValue("tfEntityId");

            if ((realm == null) || (realm.trim().length() == 0)) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    model.getLocalizedString(
                    "samlv2.create.provider.missing.realm"));
                forwardTo();
            } else if ((entityId == null) || (entityId.trim().length() == 0)) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    model.getLocalizedString(
                    "samlv2.create.provider.missing.entityId"));
                forwardTo();
            } else {
                Map map = getWorkflowParamMap(realm, model);
                if (protocol.equals(PROTO_SAMLv2)) {
                    model.createSAMLv2Provider(realm, entityId, map);
                } else if (protocol.equals(PROTO_IDFF)) {
                    model.createIDFFProvider(realm, entityId, map);
                } else {
                    model.createWSFedProvider(realm, entityId, map);
                }

                backTrail();
                FederationViewBean vb = (FederationViewBean) getViewBean(
                    FederationViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    private Map getWorkflowParamMap(String realm, CreateMetaDataModel model)
        throws AMConsoleException {
        Map map = new HashMap();
        boolean hasRole = false;
        hasRole |= addStringToMap(map, MetaTemplateParameters.P_IDP,
            "tfidentityprovider", realm);
        addStringToMap(map, MetaTemplateParameters.P_IDP_S_CERT,
            "tfidpscertalias");
        addStringToMap(map, MetaTemplateParameters.P_IDP_E_CERT,
            "tfidpecertalias");

        hasRole |= addStringToMap(map, MetaTemplateParameters.P_SP,
            "tfserviceprovider", realm);
        addStringToMap(map, MetaTemplateParameters.P_SP_S_CERT,
            "tfspscertalias");
        addStringToMap(map, MetaTemplateParameters.P_SP_E_CERT,
            "tfspecertalias");

        if ((protocol.equals(PROTO_SAMLv2))) {
            hasRole |= addStringToMap(map,
                MetaTemplateParameters.P_ATTR_QUERY_PROVIDER,
                "tfattrqueryprovider", realm);
            addStringToMap(map,
                MetaTemplateParameters.P_ATTR_QUERY_PROVIDER_S_CERT,
                "tfattrqscertalias");
            addStringToMap(map,
                MetaTemplateParameters.P_ATTR_QUERY_PROVIDER_E_CERT,
                "tfattrqecertalias");

            hasRole |= addStringToMap(map,
                MetaTemplateParameters.P_ATTR_AUTHORITY,
                "tfattrauthority", realm);
            addStringToMap(map, MetaTemplateParameters.P_ATTR_AUTHORITY_S_CERT,
                "tfattrascertalias");
            addStringToMap(map, MetaTemplateParameters.P_ATTR_AUTHORITY_E_CERT,
                "tfattraecertalias");

            hasRole |= addStringToMap(map,
                MetaTemplateParameters.P_AUTHN_AUTHORITY,
                "tfauthnauthority", realm);
            addStringToMap(map, MetaTemplateParameters.P_AUTHN_AUTHORITY_S_CERT,
                "tfauthnascertalias");
            addStringToMap(map, MetaTemplateParameters.P_AUTHN_AUTHORITY_E_CERT,
                "tfauthnaecertalias");

            hasRole |= addStringToMap(map, MetaTemplateParameters.P_PEP,
                "tfxacmlpep", realm);
            addStringToMap(map, MetaTemplateParameters.P_PEP_S_CERT,
                "tfxacmlpepscertalias");
            addStringToMap(map, MetaTemplateParameters.P_PEP_E_CERT,
                "tfxacmlpepecertalias");

            hasRole |= addStringToMap(map, MetaTemplateParameters.P_PDP,
                "tfxacmlpdp", realm);
            addStringToMap(map, MetaTemplateParameters.P_PDP_S_CERT,
                "tfxacmlpdpscertalias");
            addStringToMap(map, MetaTemplateParameters.P_PDP_E_CERT,
                "tfxacmlpdpecertalias");
            boolean bAffiliation = addStringToMap(map,
                MetaTemplateParameters.P_AFFILIATION, "tfaffiliation", realm);
            hasRole |= bAffiliation;

            addStringToMap(map, MetaTemplateParameters.P_AFFI_S_CERT,
                "tfaffiscertalias");
            addStringToMap(map, MetaTemplateParameters.P_AFFI_E_CERT,
                "tfaffiecertalias");
            String owner = this.getDisplayFieldStringValue(MetaTemplateParameters.P_AFFI_OWNERID);
            if (owner != null && owner.length() > 0) {
                addStringToMap(map, MetaTemplateParameters.P_AFFI_OWNERID,
                        "affiOwnerID");
            } else if (bAffiliation) {
                throw new AMConsoleException(
                        model.getLocalizedString(
                        "samlv2.create.provider.missing.affiliation.owner"));
            }


            CCEditableList eList = (CCEditableList) getChild(AFFI_MEMBERS);
            eList.restoreStateData();
            Set affiMembers = getValues(eList.getModel().getOptionList());
            if ((affiMembers != null) && !affiMembers.isEmpty()) {
                List list = new ArrayList();
                list.addAll(affiMembers);
                map.put(MetaTemplateParameters.P_AFFI_MEMBERS, list);
            } else if (bAffiliation) {
                throw new AMConsoleException(
                    model.getLocalizedString(
                    "samlv2.create.provider.missing.affiliation.members"));
            }

        } else if (protocol.equals(PROTO_IDFF)) {
            boolean bAffiliation = addStringToMap(map,
                MetaTemplateParameters.P_AFFILIATION, "tfaffiliation", realm);
            hasRole |= bAffiliation;

            addStringToMap(map, MetaTemplateParameters.P_AFFI_S_CERT,
                "tfaffiscertalias");
            addStringToMap(map, MetaTemplateParameters.P_AFFI_E_CERT,
                "tfaffiecertalias");
            String owner = this.getDisplayFieldStringValue(MetaTemplateParameters.P_AFFI_OWNERID);
            if (owner != null && owner.length() > 0) {
                addStringToMap(map, MetaTemplateParameters.P_AFFI_OWNERID,
                        "affiOwnerID");
            } else if (bAffiliation) {
                throw new AMConsoleException(
                        model.getLocalizedString(
                        "samlv2.create.provider.missing.affiliation.owner"));
            }

            CCEditableList eList = (CCEditableList) getChild(AFFI_MEMBERS);
            eList.restoreStateData();
            Set affiMembers = getValues(eList.getModel().getOptionList());
            if ((affiMembers != null) && !affiMembers.isEmpty()) {
                List list = new ArrayList();
                list.addAll(affiMembers);
                map.put(MetaTemplateParameters.P_AFFI_MEMBERS, list);
            } else if (bAffiliation) {
                throw new AMConsoleException(
                    model.getLocalizedString(
                    "samlv2.create.provider.missing.affiliation.members"));
            }
        }
        
        if (!hasRole) {
            throw new AMConsoleException(
                model.getLocalizedString(
                "samlv2.create.provider.missing.roles"));
        }
        return map;
    }

    private boolean addStringToMap(
        Map map,
        String key,
        String name,
        String realm
    ) {
        String val = this.getDisplayFieldStringValue(name);
        if ((val != null) && (val.trim().length() > 0)) {
            while (val.startsWith("/")) {
                val = val.substring(1);
            }
            val = (realm.endsWith("/") ) ? realm + val : realm + "/" + val;
            map.put(key, val);
            return true;
        } else {
            return false;
        }
    }
    private boolean addStringToMap(Map map, String key, String name) {
        String val = this.getDisplayFieldStringValue(name);
        if ((val != null) && (val.trim().length() > 0)) {
            map.put(key, val);
            return true;
        } else {
            return false;
        }
    }


    /**
     * Handles page cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        setPageSessionAttribute(getTrackingTabIDName(),
            AMAdminConstants.FED_TAB_ID);
        setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID,
            getTrackingTabIDName());
        FederationViewBean vb = (FederationViewBean)
            getViewBean(FederationViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public String endPropertyAttributesDisplay(
        ChildContentDisplayEvent event
    ) {
        String html = event.getContent();
        int idx = html.indexOf("singleChoiceRealm");
        idx = html.lastIndexOf("<tr>", idx);
        html = html.substring(0, idx) + "</table>\n" +
        "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" title=\"\">\n"+
            html.substring(idx);
        return html;
    }
}
