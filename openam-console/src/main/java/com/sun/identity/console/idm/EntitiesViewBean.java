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
 * $Id: EntitiesViewBean.java,v 1.14 2009/12/11 23:25:19 veiming Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.HasEntitiesTabs;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.console.idm.model.EntitiesModelImpl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.shared.ldap.LDAPDN;

public class EntitiesViewBean
    extends RealmPropertiesBase
    implements HasEntitiesTabs
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/Entities.jsp";
    public static final String PG_SESSION_ENTITY_TYPE = "entitytype";

    private static final String TF_FILTER = "tfFilter";
    private static final String BTN_SEARCH = "btnSearch";

    private static final String TBL_SEARCH = "tblSearch";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_DATA_UNIVERSALNAME = "tblDataUniversalName";
    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";
    private static final String TBL_COL_ID = "tblColId";
    private static final String TBL_DATA_ID = "tblDataId";

    private static final String PAGETITLE = "pgtitle";
    private static final String DEFAULT_ID_TYPE = "user";
    static final String ATTR_NAME_AGENT_TYPE = "Type=";

    private CCActionTableModel tblModel = null;
    private CCPageTitleModel ptModel;
    private boolean tblModelPopulated = false;
    private static Class wspProfileViewBeanClass;
    private static Class wscProfileViewBeanClass;
    private static boolean supportWSSecurityUI;

    static {
        try {
            wspProfileViewBeanClass = Class.forName(
                "com.sun.identity.console.idm.WebServiceProviderEditViewBean");
            wscProfileViewBeanClass = Class.forName(
                "com.sun.identity.console.idm.WebServiceClientEditViewBean");
            supportWSSecurityUI = true;
        } catch (ClassNotFoundException e) {
            //ignored. This means that this is not a openfm console.
        }
    }

    /**
     * Creates a policy view bean.
     */
    public EntitiesViewBean() {
        super("Entities");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createTableModel();
        registerChildren();
    }

    public void resetView() {
        super.resetView();
        tblModelPopulated = false;
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        szCache.setValue(null);
        tblModel.clearAll();
        setDisplayFieldValue(TF_FILTER, "*");
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(TF_FILTER, CCTextField.class);
        registerChild(BTN_SEARCH, CCButton.class);
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_SEARCH, CCActionTable.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(TBL_SEARCH)) {
            populateTableModelEx();
            view = new CCActionTable(this, tblModel, name);
       } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblModel.isChildSupported(name)) {
            view = tblModel.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event, false);
        resetButtonState(TBL_BUTTON_DELETE);

        EntitiesModel model = (EntitiesModel)getModel();

        CCButton b = (CCButton)getChild(TBL_BUTTON_ADD);
        String curRealm = (String)getPageSessionAttribute(AMAdminConstants.CURRENT_REALM);
        if (model.repoExists(curRealm)) {
            b.setDisabled(false);
        } else {
            b.setDisabled(true);
        }

        String[] param = {getDisplayIDType()};
        ptModel.setPageTitleText(MessageFormat.format(
            model.getLocalizedString("page.title.entities"), (Object[])param));
        tblModel.setTitle(MessageFormat.format(
            model.getLocalizedString("table.entities.title.name"), (Object[])param));
        tblModel.setTitleLabel(MessageFormat.format(
            model.getLocalizedString("table.entities.title.name"), (Object[])param));
        tblModel.setSummary(MessageFormat.format(
            model.getLocalizedString("table.entities.summary"),(Object[]) param));

        getEntityNames();
        addEntitiesTab();
        tabModel.setSelectedNode(7);
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", 
            getBackButtonLabel("page.title.back.realms"));
    }

    private void addEntitiesTab() {
        EntitiesModel model = (EntitiesModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        AMViewConfig config = AMViewConfig.getInstance();
        config.addEntityTabs(tabModel, curRealm, model);
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new EntitiesModelImpl(req, getPageSessionAttributes());
    }

    private void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblIDMEntities.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.entities.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
            "table.entities.button.delete");
        tblModel.setActionValue(TBL_COL_NAME,
            "table.entities.name.column.name");
        tblModel.setActionValue(TBL_COL_ID,
            "table.entities.name.column.id"); 
    }

    private void getEntityNames() {
        EntitiesModel model = (EntitiesModel)getModel();
        String filter = ((String)getDisplayFieldValue(TF_FILTER));

        if ((filter == null) || (filter.length() == 0)) {
            filter = "*";
            setDisplayFieldValue(TF_FILTER, "*");
        } else {
            filter = filter.trim();
        }

        try {
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);

            IdSearchResults results = model.getEntityNames(
                curRealm, getDisplayIDType(), filter);
            int errorCode = results.getErrorCode();

            switch (errorCode) {
            case IdSearchResults.SIZE_LIMIT_EXCEEDED:
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    "message.sizelimit.exceeded");
                break;
            case IdSearchResults.TIME_LIMIT_EXCEEDED:
                setInlineAlertMessage(CCAlert.TYPE_WARNING, "message.warning",
                    "message.timelimit.exceeded");
                break;
            }
            populateTableModel(results.getSearchResults());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            // disable the add button if there was an error 
            CCButton b = (CCButton)getChild(TBL_BUTTON_ADD);
            b.setDisabled(true);
        }
    }

    private String getDisplayIDType() {
        String idType = (String)getPageSessionAttribute(PG_SESSION_ENTITY_TYPE);

        if ((idType == null) || (idType.length() == 0)) {
            setPageSessionAttribute(PG_SESSION_ENTITY_TYPE, DEFAULT_ID_TYPE);
            idType = DEFAULT_ID_TYPE;
        }

        return idType;
    }

    private void populateTableModelEx() {
        if (!tblModelPopulated) {
            tblModelPopulated = true;
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            List cache = (List)szCache.getSerializedObj();

            if ((cache != null) && !cache.isEmpty()) {
                EntitiesModel model = (EntitiesModel)getModel();
                SSOToken ssoToken = model.getUserSSOToken();
                List list = new ArrayList(cache.size());

                for (Iterator iter = cache.iterator(); iter.hasNext(); ) {
                    String id = (String)iter.next();
                    try {
                        list.add(IdUtils.getIdentity(ssoToken, id));
                    } catch (IdRepoException e) {
                        //ignore since ID is not found.
                    }
                }
                populateTableModel(list);
            }
        }
    }

    private void populateTableModel(Collection entityNames) {
        tblModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

        if ((entityNames != null) && !entityNames.isEmpty()) {
            // set the paging size
            EntitiesModel model = (EntitiesModel)getModel();
            tblModel.setMaxRows(model.getPageSize());

            // remove the special users from the set of enitities to display
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            Set specialUsers = model.getSpecialUsers(curRealm);
            entityNames.removeAll(specialUsers);

            // get the current logged in user
            AMIdentity curUser = null;
            try {
                curUser = IdUtils.getIdentity(
                    model.getUserSSOToken(), model.getUniversalID());
            } catch (IdRepoException idr) {
                // do nothing
            }
            
            int counter = 0;
            boolean firstEntry = true;

            List cache = new ArrayList(entityNames.size());
            for (Iterator iter = entityNames.iterator(); iter.hasNext(); ) {
                AMIdentity entity = (AMIdentity)iter.next();

                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModel.appendRow();
                }
                String name = AMFormatUtils.getIdentityDisplayName(
                    model, entity);
                
                // hide the checkbox if this is the current user or amadmin
                boolean showCheckbox =
                    ((curUser != null) && (!curUser.equals(entity))) &&
                    !model.isAmadminUser(entity);
                tblModel.setSelectionVisible(counter++, showCheckbox);

                String universalId = IdUtils.getUniversalId(entity);
                tblModel.setValue(TBL_DATA_NAME, name);
                String[] comps = LDAPDN.explodeDN(universalId, true);
                tblModel.setValue(TBL_DATA_ID, comps[0]);
                tblModel.setValue(TBL_DATA_UNIVERSALNAME, universalId);
                tblModel.setValue(TBL_DATA_ACTION_HREF,
                    stringToHex(universalId));
                cache.add(universalId);
            }
            szCache.setValue((Serializable)cache);
        } else {                         
            szCache.setValue(null);
        }
    }

    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
        EntityAddViewBean vb = (EntityAddViewBean)getViewBean(
            EntityAddViewBean.class);
        setPageSessionAttribute(EntityAddViewBean.ENTITY_TYPE,
            (String)getPageSessionAttribute(PG_SESSION_ENTITY_TYPE));
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to edit policy view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
        EntitiesModel model = (EntitiesModel)getModel();
        String universalId = hexToString((String)getDisplayFieldValue(
            TBL_DATA_ACTION_HREF));
        setPageSessionAttribute(EntityEditViewBean.UNIVERSAL_ID, universalId);

        try {
            AMIdentity amid = IdUtils.getIdentity(
                model.getUserSSOToken(), universalId);
            String idType = amid.getType().getName();
            AMViewBeanBase vb = (supportWSSecurityUI) ?
                getEntityEditViewBean(amid) :
                (AMViewBeanBase)getViewBean(EntityEditViewBean.class);

            setPageSessionAttribute(EntityOpViewBeanBase.ENTITY_NAME,
                amid.getName());
            setPageSessionAttribute(EntityOpViewBeanBase.ENTITY_TYPE, idType);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (SSOException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        } catch (IdRepoException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getErrorString(e));
            forwardTo();
        }
    }

    private AMViewBeanBase getEntityEditViewBean(AMIdentity amid) 
        throws SSOException, IdRepoException
    {
        Map attrValues = amid.getAttributes();
        Set deviceKeyValue = (Set)attrValues.get(
            "sunIdentityServerDeviceKeyValue");
        String agentType = null;
        if ((deviceKeyValue != null) && !deviceKeyValue.isEmpty()) {
            for (Iterator i = deviceKeyValue.iterator();
                i.hasNext() && (agentType == null);
            ) {
                String val = (String)i.next();
                if (val.startsWith(ATTR_NAME_AGENT_TYPE)) {
                    agentType = val.substring(ATTR_NAME_AGENT_TYPE.length());
                }
            }
        }

        AMViewBeanBase vb;
        if ((agentType != null) && agentType.equalsIgnoreCase("WSC")) {
            vb = (AMViewBeanBase)getViewBean(wscProfileViewBeanClass);
        } else if ((agentType != null) && agentType.equalsIgnoreCase("WSP")) {
            vb = (AMViewBeanBase)getViewBean(wspProfileViewBeanClass);
        } else {
            vb = (AMViewBeanBase)getViewBean(EntityEditViewBean.class);
        }
        return vb;
    }

    /**
     * Deletes policies.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();

        Integer[] selected = tblModel.getSelectedRows();
        Set names = new HashSet(selected.length *2);
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List cache = (List)szCache.getSerializedObj();

        for (int i = 0; i < selected.length; i++) {
            names.add((String)cache.get(selected[i].intValue()));
        }

        try {
            EntitiesModel model = (EntitiesModel)getModel();
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String idType = (String)getPageSessionAttribute(
                PG_SESSION_ENTITY_TYPE);
            model.deleteEntities(curRealm, names);

            if (selected.length == 1) {
                Object[] param = {model.getLocalizedString(idType)};
                String msg = model.getLocalizedString(
                    "entities.message.deleted");
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    MessageFormat.format(msg, param));
            } else {
                Object[] param = {model.getLocalizedString(idType)};
                String msg = model.getLocalizedString(
                    "entities.message.deleted.pural");
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    MessageFormat.format(msg, param));
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles 'Back to' button request. In this case, it takes you back
     * to the realm view.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        backTrail();
        forwardToRealmView(event);
    }
}
