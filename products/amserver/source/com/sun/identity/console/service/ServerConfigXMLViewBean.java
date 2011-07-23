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
 * $Id: ServerConfigXMLViewBean.java,v 1.7 2008/08/29 18:14:52 veiming Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.util.Crypt;
import com.sun.identity.common.configuration.ServerConfigXML;
import com.sun.identity.common.configuration.ServerConfigXML.DirUserObject;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.service.model.ServerSiteModel;
import com.sun.identity.console.service.model.ServerSiteModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.view.tabs.CCTabs;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * This view bean manages server configuration XML.
 */
public class ServerConfigXMLViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String DEFAULT_DISPLAY_URL =
        "/console/service/ServerConfigXML.jsp";
    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";
    private static final String SZ_CACHE_SERVER = "szCache1";
    private static final String SZ_CACHE_USER = "szCache2";
        
    private static final String TBL_SERVERS = "tblServers";
    
    private static final String TF_SERVER_MIN_POOL = "tfserverminpool";
    private static final String TF_SERVER_MAX_POOL = "tfservermaxpool";

    private static final String TBL_SERVERS_COL_HOST =
        "tblServerConfigXMLServerColHost";
    private static final String TBL_SERVERS_COL_NAME =
        "tblServerConfigXMLServerColName";
    private static final String TBL_SERVERS_COL_PORT =
        "tblServerConfigXMLServerColPort";
    private static final String TBL_SERVERS_COL_TYPE =
        "tblServerConfigXMLServerColType";
    private static final String TBL_SERVERS_NAME_DATA =
        "tblServerConfigXMLNameDataServer";
    private static final String TBL_SERVERS_HOST_DATA =
        "tblServerConfigXMLHostDataServer";
    private static final String TBL_SERVERS_PORT_DATA =
        "tblServerConfigXMLServerDataPort";
    private static final String TBL_SERVERS_TYPE_DATA =
        "tblServerConfigXMLServerDataType";
    
    private static final String TBL_USERS = "tblUsers";
    private static final String TF_USER_MIN_POOL = "tfuserminpool";
    private static final String TF_USER_MAX_POOL = "tfusermaxpool";

    private static final String TBL_USERS_COL_NAME =
        "tblServerConfigXMLUserColName";
    private static final String TBL_USERS_COL_HOST =
        "tblServerConfigXMLUserColHost";
    private static final String TBL_USERS_COL_PORT =
        "tblServerConfigXMLUserColPort";
    private static final String TBL_USERS_COL_TYPE =
        "tblServerConfigXMLUserColType";
    private static final String TBL_USERS_NAME_DATA =
        "tblServerConfigXMLNameDataUser";
    private static final String TBL_USERS_HOST_DATA =
        "tblServerConfigXMLHostDataUser";
    private static final String TBL_USERS_PORT_DATA =
        "tblServerConfigXMLUserDataPort";
    private static final String TBL_USERS_TYPE_DATA =
        "tblServerConfigXMLUserDataType";
    private static final String TF_SERVER_BIND_DN = "tfbinddn";
    private static final String TF_SERVER_BIND_PWD = "tfbindpwd";
    private static final String TF_USER_ROOTSUFFIX = "tfamsdkrootsuffix";
    private static final String TF_USER_PROXY_PWD = "tfproxypwd";
    private static final String TF_USER_ADMIN_PWD = "tfadminpwd";

    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private CCActionTableModel tblServerModel;
    private CCActionTableModel tblUserModel;
    private boolean submitCycle;
    private boolean bAMSDKEnabled;

    /**
     * Creates a server config XML view bean.
     */
    public ServerConfigXMLViewBean() {
        super("ServerConfigXML");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    protected void initialize() {
        if (!initialized) {
            String serverName = (String)getPageSessionAttribute(
                ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
            if (serverName != null) {
                super.initialize();
                
                bAMSDKEnabled = getModel().isAMSDKEnabled();
                createPageTitleModel();
                createTabModel();
                createPropertyModel(serverName);
                createTableModel();
                registerChildren();
                initialized = true;
            }
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(SZ_CACHE_SERVER, SerializedField.class);
        registerChild(SZ_CACHE_USER, SerializedField.class);
        ptModel.registerChildren(this);
        registerChild(PGTITLE_THREE_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        registerChild(TBL_SERVERS, CCActionTable.class);
        registerChild(TBL_USERS, CCActionTable.class);
        tblServerModel.registerChildren(this);
        tblUserModel.registerChildren(this);
        propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(TBL_SERVERS)) {
            SerializedField szCache = (SerializedField)getChild(
                SZ_CACHE_SERVER);
            populateServerTableModel((List)szCache.getSerializedObj());
            view = new CCActionTable(this, tblServerModel, name);
        } else if (name.equals(TBL_USERS)) {
            SerializedField szCache = (SerializedField)getChild(
                SZ_CACHE_USER);
            populateUserTableModel((List)szCache.getSerializedObj());
            view = new CCActionTable(this, tblUserModel, name);
        } else if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (tblServerModel.isChildSupported(name)) {
            view = tblServerModel.createChild(this, name);
        } else if (tblUserModel.isChildSupported(name)) {
            view = tblUserModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private void createTableModel() {
        tblServerModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblServerConfigXMLServers.xml"));
        tblServerModel.setTitleLabel("label.items");
        tblServerModel.setActionValue(TBL_SERVERS_COL_NAME,
            "amconfig.serverconfig.xml.server.table.column.name");
        tblServerModel.setActionValue(TBL_SERVERS_COL_HOST,
            "amconfig.serverconfig.xml.server.table.column.host");
        tblServerModel.setActionValue(TBL_SERVERS_COL_PORT,
            "amconfig.serverconfig.xml.server.table.column.port");
        tblServerModel.setActionValue(TBL_SERVERS_COL_TYPE,
            "amconfig.serverconfig.xml.server.table.column.type");
        propertySheetModel.setModel(TBL_SERVERS, tblServerModel);

        tblUserModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblServerConfigXMLUsers.xml"));
        tblUserModel.setTitleLabel("label.items");
        tblUserModel.setActionValue(TBL_USERS_COL_NAME,
            "amconfig.serverconfig.xml.user.table.column.name");
        tblUserModel.setActionValue(TBL_USERS_COL_HOST,
            "amconfig.serverconfig.xml.user.table.column.host");
        tblUserModel.setActionValue(TBL_USERS_COL_PORT,
            "amconfig.serverconfig.xml.user.table.column.port");
        tblUserModel.setActionValue(TBL_USERS_COL_TYPE,
            "amconfig.serverconfig.xml.user.table.column.type");
        propertySheetModel.setModel(TBL_USERS, tblUserModel);
    }

    
    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new ServerSiteModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Displays the profile of a site.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String serverName = (String)getPageSessionAttribute(
            ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();
        ptModel.setPageTitleText(model.getEditServerPageTitle(serverName));
        try {
            setConfigProperties(serverName, model);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    private void setConfigProperties(String serverName, ServerSiteModel model)
        throws AMConsoleException {
        if (!submitCycle) {
            ServerConfigXML xmlObj = model.getServerConfigObject(serverName);
            ServerConfigXML.ServerGroup defaultServerGroup = 
                xmlObj.getDefaultServerGroup();
            ServerConfigXML.ServerGroup smsServerGroup = 
                xmlObj.getSMSServerGroup();

            propertySheetModel.setValue(TF_USER_MIN_POOL, 
                Integer.toString(defaultServerGroup.minPool));
            propertySheetModel.setValue(TF_USER_MAX_POOL, 
                Integer.toString(defaultServerGroup.maxPool));
            
            propertySheetModel.setValue(TF_SERVER_MIN_POOL, 
                Integer.toString(smsServerGroup.minPool));
            propertySheetModel.setValue(TF_SERVER_MAX_POOL, 
                Integer.toString(smsServerGroup.maxPool));
            
            List bindInfo = smsServerGroup.dsUsers;
            if (!bindInfo.isEmpty()) {
                ServerConfigXML.DirUserObject bind = 
                    (ServerConfigXML.DirUserObject)bindInfo.iterator().next();
                propertySheetModel.setValue(TF_SERVER_BIND_DN, bind.dn);
                propertySheetModel.setValue(TF_SERVER_BIND_PWD, 
                    AMPropertySheetModel.passwordRandom);
            }
            
            if (bAMSDKEnabled) {
                propertySheetModel.setValue(TF_USER_ROOTSUFFIX,
                    defaultServerGroup.dsBaseDN);
                for (Iterator i = defaultServerGroup.dsUsers.iterator(); 
                    i.hasNext(); ) {
                    DirUserObject o = (DirUserObject)i.next();
                    if (o.type.equals("proxy")) {
                        propertySheetModel.setValue(TF_USER_PROXY_PWD,
                            AMPropertySheetModel.passwordRandom);
                    } else if (o.type.equals("admin")) {
                        propertySheetModel.setValue(TF_USER_ADMIN_PWD,
                            AMPropertySheetModel.passwordRandom);
                    }
                }
            }
        
            populateUserTableModel(defaultServerGroup.hosts);
            populateServerTableModel(smsServerGroup.hosts);
        }
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    protected void createTabModel() {
        if (tabModel == null) {
            AMViewConfig amconfig = AMViewConfig.getInstance();
            tabModel = amconfig.getTabsModel(ServerEditViewBeanBase.TAB_NAME,
                "/", getRequestContext().getRequest());

            registerChild(TAB_COMMON, CCTabs.class);
        }
    }

    private void createPropertyModel(String serverName) {
        String fileName = (bAMSDKEnabled) ?
            "com/sun/identity/console/propertyServerConfigXMLWithAMSDK.xml" :
            "com/sun/identity/console/propertyServerConfigXML.xml";
        String xml = AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(fileName));
        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear(); 
    }
    
    private void populateServerTableModel(List entries) {
        tblServerModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE_SERVER);
        int counter = 0;

        if ((entries != null) && !entries.isEmpty()) {
            for (Iterator i = entries.iterator(); i.hasNext(); counter++) {
                if (counter > 0) {
                    tblServerModel.appendRow();
                }

                ServerConfigXML.ServerObject entry =
                    (ServerConfigXML.ServerObject) i.next();
                tblServerModel.setValue(TBL_SERVERS_NAME_DATA, entry.name);
                tblServerModel.setValue(TBL_SERVERS_HOST_DATA, entry.host);
                tblServerModel.setValue(TBL_SERVERS_PORT_DATA, entry.port);
                tblServerModel.setValue(TBL_SERVERS_TYPE_DATA, entry.type);
                tblServerModel.setSelectionVisible(counter, true);
            }
        }
        szCache.setValue(entries);
    }
    
    private void populateUserTableModel(List entries) {
        if (bAMSDKEnabled) {
            tblUserModel.clearAll();
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE_USER);
            int counter = 0;

            if ((entries != null) && !entries.isEmpty()) {
                for (Iterator i = entries.iterator(); i.hasNext(); counter++) {
                    if (counter > 0) {
                        tblUserModel.appendRow();
                    }
                
                    ServerConfigXML.ServerObject entry =
                        (ServerConfigXML.ServerObject)i.next();
                    tblUserModel.setValue(TBL_USERS_NAME_DATA, entry.name);
                    tblUserModel.setValue(TBL_USERS_HOST_DATA, entry.host);
                    tblUserModel.setValue(TBL_USERS_PORT_DATA, entry.port);
                    tblUserModel.setValue(TBL_USERS_TYPE_DATA, entry.type);
                    tblUserModel.setSelectionVisible(counter, true);
                }
            }
            szCache.setValue(entries);
        }
    }
    
    /**
     * Handles save request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        submitCycle = true;
        String svrMinPool = (String)getDisplayFieldValue(TF_SERVER_MIN_POOL);
        String svrMaxPool = (String)getDisplayFieldValue(TF_SERVER_MAX_POOL);
        String bindDN = (String)getDisplayFieldValue(TF_SERVER_BIND_DN);
        
        String serverName = (String)getPageSessionAttribute(
            ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();
        
        try {
            ServerConfigXML xmlObj = model.getServerConfigObject(serverName);
            ServerConfigXML.ServerGroup smsServerGroup = 
                xmlObj.getSMSServerGroup();
            
            if (bAMSDKEnabled) {
                String userMinPool = (String)
                    getDisplayFieldValue(TF_USER_MIN_POOL);
                String userMaxPool = (String)
                    getDisplayFieldValue(TF_USER_MAX_POOL);
                ServerConfigXML.ServerGroup defaultServerGroup = 
                    xmlObj.getDefaultServerGroup();

                defaultServerGroup.minPool = Integer.parseInt(userMinPool);
                defaultServerGroup.maxPool = Integer.parseInt(userMaxPool);
                
                defaultServerGroup.dsBaseDN = ((String)getDisplayFieldValue(
                    TF_USER_ROOTSUFFIX)).trim();
               
                for (Iterator i = defaultServerGroup.dsUsers.iterator(); 
                    i.hasNext(); ) {
                    DirUserObject o = (DirUserObject)i.next();
                    if (o.type.equals("proxy")) {
                        o.dn = "cn=puser,ou=DSAME Users," +
                            defaultServerGroup.dsBaseDN;
                        String pwd = (String)getDisplayFieldValue(
                            TF_USER_PROXY_PWD);
                        if (!pwd.equals(AMPropertySheetModel.passwordRandom)) {
                            o.password = Crypt.encode(pwd);
                        }
                    } else if (o.type.equals("admin")) {
                        o.dn = "cn=dsameuser,ou=DSAME Users," +
                            defaultServerGroup.dsBaseDN;
                        String pwd = (String)getDisplayFieldValue(
                            TF_USER_ADMIN_PWD);
                        if (!pwd.equals(AMPropertySheetModel.passwordRandom)) {
                            o.password = Crypt.encode(pwd);
                        }
                    }
                }
            }

            smsServerGroup.minPool = Integer.parseInt(svrMinPool);
            smsServerGroup.maxPool = Integer.parseInt(svrMaxPool);
            List userGroup = smsServerGroup.dsUsers;
            ServerConfigXML.DirUserObject bind = (ServerConfigXML.DirUserObject)
                userGroup.iterator().next();
            bind.dn = bindDN;
        
            String bindPwd = (String)getDisplayFieldValue(TF_SERVER_BIND_PWD);
            if (!bindPwd.equals(AMPropertySheetModel.passwordRandom)) {
                bind.password = Crypt.encode(bindPwd);
            }
            
            model.setServerConfigXML(serverName, xmlObj.toXML());
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                model.getLocalizedString("serverconfig.updated"));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        } catch (NumberFormatException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                model.getLocalizedString("exception.thread.pool.no.integer"));
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
     * Handles remove server group entry request.
     *
     * @param event Request invocation event
     */
    public void handleTblServerConfigXMLUserButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        String serverName = (String)getPageSessionAttribute(
            ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();
        try {
            ServerConfigXML xmlObj = model.getServerConfigObject(serverName);
            ServerConfigXML.ServerGroup defaultServerGroup = 
                xmlObj.getDefaultServerGroup();
            
            CCActionTable table = (CCActionTable)getChild(TBL_USERS);
            table.restoreStateData();
            Integer[] selected = tblUserModel.getSelectedRows();
            
            if (selected.length >= defaultServerGroup.hosts.size()) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    model.getLocalizedString(
                    "exception.cannot,delete.all.servers"));
            } else {
                for (int i = selected.length -1; i >= 0; --i) {
                    defaultServerGroup.hosts.remove(selected[i].intValue());
                }
                model.setServerConfigXML(serverName, xmlObj.toXML());
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "serverconfig.updated");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        
        forwardTo();
    }
    
    /**
     * Handles add server group entry request.
     *
     * @param event Request invocation event
     */
    public void handleTblServerConfigXMLServerButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        ServerConfigXMLAddServerViewBean vb = 
            (ServerConfigXMLAddServerViewBean)getViewBean(
            ServerConfigXMLAddServerViewBean.class);
        unlockPageTrail();
        vb.setPageSessionAttribute(
            ServerConfigXMLAddServerViewBean.PG_ATTR_SERVER_GROUP_TYPE, "sms");
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Handles add server group entry request.
     *
     * @param event Request invocation event
     */
    public void handleTblServerConfigXMLUserButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        ServerConfigXMLAddServerViewBean vb = 
            (ServerConfigXMLAddServerViewBean)getViewBean(
            ServerConfigXMLAddServerViewBean.class);
        unlockPageTrail();
        vb.setPageSessionAttribute(
            ServerConfigXMLAddServerViewBean.PG_ATTR_SERVER_GROUP_TYPE, 
            DSConfigMgr.DEFAULT);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    /**
     * Handles remove server group entry request.
     *
     * @param event Request invocation event
     */
    public void handleTblServerConfigXMLServerButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        String serverName = (String)getPageSessionAttribute(
            ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
        ServerSiteModel model = (ServerSiteModel)getModel();
        try {
            ServerConfigXML xmlObj = model.getServerConfigObject(serverName);
            ServerConfigXML.ServerGroup smsServerGroup = 
                xmlObj.getSMSServerGroup();
            
            CCActionTable table = (CCActionTable)getChild(TBL_SERVERS);
            table.restoreStateData();
            Integer[] selected = tblServerModel.getSelectedRows();
            
            if (selected.length >= smsServerGroup.hosts.size()) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    model.getLocalizedString(
                    "exception.cannot,delete.all.servers"));
            } else {
                for (int i = selected.length -1; i >= 0; --i) {
                    smsServerGroup.hosts.remove(selected[i].intValue());
                }
                model.setServerConfigXML(serverName, xmlObj.toXML());
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "serverconfig.updated");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        
        forwardTo();
    }
    
    /**
     * Handles return to home page request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
        throws ModelControlException {
        returnToHomePage();
    }
    
    /**
     * Handles tab selected event. 
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        try {
            AMViewBeanBase vb = getTabNodeAssociatedViewBean(
                "cscGeneral", nodeID);

            String tmp = (String)getPageSessionAttribute(
                AMAdminConstants.PREVIOUS_TAB_ID);
            vb.setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, tmp);
            unlockPageTrailForSwapping();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            debug.error("ServerEditGeneralViewBean.nodeClicked", e);
            forwardTo();
        }
    }

    private void returnToHomePage() {
        backTrail();
        ServerSiteViewBean vb = (ServerSiteViewBean)getViewBean(
            ServerSiteViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.editserver";
    }

    protected boolean startPageTrail() {
        return false;
    }
    
    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.serversite.config");
    }

    protected String removeParentSiteBlob(String xml) {
        //General Tab View Bean will overwrite this.
        return xml;
    }

    protected String getTrackingTabIDName() {
        return ServerEditViewBeanBase.TAB_TRACKER;
    }
}
