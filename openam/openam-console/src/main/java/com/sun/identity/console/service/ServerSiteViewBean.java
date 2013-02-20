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
 * $Id: ServerSiteViewBean.java,v 1.3 2008/07/07 20:39:20 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.service.model.ServerSiteModel;
import com.sun.identity.console.service.model.ServerSiteModelImpl;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.html.CCButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/**
 * Servers and Sites Management main page.
 */
public class ServerSiteViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String DEFAULT_DISPLAY_URL =
        "/console/service/ServerSite.jsp";
    private static final String BTN_DEFAULT_SERVER = "btnDefaultSettings";
    
    private static final String TBL_SERVERS = "tblServer";
    private static final String TBL_SERVER_BUTTON_ADD = "tblServerButtonAdd";
    private static final String TBL_SERVER_BUTTON_CLONE =
        "tblServerButtonClone";
    private static final String TBL_SERVER_BUTTON_DELETE =
        "tblServerButtonDelete";
    private static final String TBL_COL_SERVER_NAME = "tblColServerName";
    private static final String TBL_DATA_SERVER_ACTION_HREF =
        "tblDataServerActionHref";
    private static final String TBL_DATA_SERVER_NAME = "tblDataServerName";
    private static final String TBL_COL_SITE = "tblColSite";
    private static final String TBL_DATA_SITE = "tblDataSite";

    private static final String TBL_SITES = "tblSite";
    private static final String TBL_SITE_BUTTON_ADD = "tblSiteButtonAdd";
    private static final String TBL_SITE_BUTTON_DELETE =
        "tblSiteButtonDelete";
    private static final String TBL_COL_SITE_NAME = "tblColSiteName";
    private static final String TBL_DATA_SITE_ACTION_HREF =
        "tblDataSiteActionHref";
    private static final String TBL_DATA_SITE_NAME = "tblDataSiteName";
    private static final String TBL_COL_SITE_URL = "tblColSiteURL";
    private static final String TBL_DATA_SITE_URL = "tblDataSiteURL";
    private static final String TBL_COL_SITE_SERVERS = "tblColSiteServers";
    private static final String TBL_DATA_SITE_SERVERS = "tblDataSiteServers";

    private static final String SZ_CACHE_1 = "szCache1";
    private static final String SZ_CACHE_SERVER = "szCacheServer";
    private static final String SZ_CACHE_SITE = "szCacheSite";
    private static final String PAGETITLE = "pgtitle";

    private CCActionTableModel tblServerModel;
    private CCActionTableModel tblSiteModel;
    private CCPageTitleModel ptModel;

    /**
     * Creates a servers and sites view bean.
     */
    public ServerSiteViewBean() {
        super("ServerSite");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createTableModel();
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/simplePageTitle.xml"));
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(SZ_CACHE_1, SerializedField.class);
        registerChild(SZ_CACHE_SERVER, SerializedField.class);
        registerChild(SZ_CACHE_SITE, SerializedField.class);
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_SERVERS, CCActionTable.class);
        registerChild(TBL_SITES, CCActionTable.class);
        registerChild(BTN_DEFAULT_SERVER, CCButton.class);
        ptModel.registerChildren(this);
        tblServerModel.registerChildren(this);
        tblSiteModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(TBL_SERVERS)) {
            SerializedField szCache = (SerializedField)getChild(
                SZ_CACHE_SERVER);
            populateServerTableModel((Map)szCache.getSerializedObj());
            view = new CCActionTable(this, tblServerModel, name);
        } else if (name.equals(TBL_SITES)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE_SITE);
            populateSiteTableModel((Map)szCache.getSerializedObj());
            view = new CCActionTable(this, tblSiteModel, name);
        } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblSiteModel.isChildSupported(name)) {
            view = tblSiteModel.createChild(this, name);
        } else if (tblServerModel.isChildSupported(name)) {
            view = tblServerModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    /**
     * Displays servers and sites information.
     *
     * @param event Display Event.
     * @throws ModelControlException if unable to initialize model.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        resetButtonState(TBL_SERVER_BUTTON_DELETE);
        resetButtonState(TBL_SERVER_BUTTON_CLONE);
        resetButtonState(TBL_SITE_BUTTON_DELETE);
        getSiteNames();
        getServerNames();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new ServerSiteModelImpl(req, getPageSessionAttributes());
    }

    private void createTableModel() {
        tblSiteModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblSite.xml"));
        tblSiteModel.setTitleLabel("label.items");
        tblSiteModel.setActionValue(TBL_SITE_BUTTON_ADD,
            "table.site.button.new");
        tblSiteModel.setActionValue(TBL_SITE_BUTTON_DELETE,
            "table.site.button.delete");
        tblSiteModel.setActionValue(TBL_COL_SITE_NAME,
            "table.site.name.column.name");
        tblSiteModel.setActionValue(TBL_COL_SITE_URL,
            "table.site.url.column.name");
        tblSiteModel.setActionValue(TBL_COL_SITE_SERVERS,
            "table.site.servers.column.name");

        tblServerModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblServer.xml"));
        tblServerModel.setTitleLabel("label.items");
        tblServerModel.setActionValue(TBL_SERVER_BUTTON_ADD,
            "table.server.button.new");
        tblServerModel.setActionValue(TBL_SERVER_BUTTON_DELETE,
            "table.server.button.delete");
        tblServerModel.setActionValue(TBL_SERVER_BUTTON_CLONE,
            "table.server.button.clone");
        tblServerModel.setActionValue(TBL_COL_SERVER_NAME,
            "table.server.name.column.name");
        tblServerModel.setActionValue(TBL_COL_SITE,
            "table.server.site.column.name");
    }

    private void getSiteNames() {
        ServerSiteModel model = (ServerSiteModel)getModel();
        try {
            Set sites = model.getSiteNames();
            Map map = new HashMap(sites.size() *2);

            for (Iterator i = sites.iterator(); i.hasNext(); ) {
                String site = (String)i.next();
                String[] params = new String[2];
                params[0] = model.getSitePrimaryURL(site);
                
                Set assignedServers = model.getSiteServers(site);
                if ((assignedServers != null) && !assignedServers.isEmpty()) {
                    Set set = new TreeSet();
                    set.addAll(assignedServers);
                    StringBuilder buff = new StringBuilder();
                    for (Iterator j = set.iterator(); j.hasNext(); ) {
                        buff.append((String)j.next()).append("<br />");
                    }
                    params[1] = buff.toString();
                } else {
                    params[1] = "";
                }
                
                map.put(site, params);
            }
            populateSiteTableModel(map);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    private void getServerNames() {
        ServerSiteModel model = (ServerSiteModel)getModel();
        try {
            Set servers = model.getServerNames();
            Map map = new HashMap(servers.size() *2);

            for (Iterator i = servers.iterator(); i.hasNext(); ) {
                String server = (String)i.next();
                map.put(server, model.getServerSite(server));
            }
            populateServerTableModel(map);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    private void populateSiteTableModel(Map siteToURL) {
        tblSiteModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE_1);
        SerializedField szCacheSite = (SerializedField)getChild(SZ_CACHE_SITE);
        ServerSiteModel model = (ServerSiteModel)getModel();
        tblServerModel.setMaxRows(model.getPageSize());
        HashMap cacheSite = new HashMap();
        ArrayList cache = new ArrayList();

        if ((siteToURL != null) && !siteToURL.isEmpty()) {
            int counter = 0;

            for (Iterator iter = siteToURL.keySet().iterator();
                iter.hasNext(); counter++
            ) {
                if (counter > 0) {
                    tblSiteModel.appendRow();
                }
                String name = (String)iter.next();
                String[] params = (String[])siteToURL.get(name);
                String url = params[0];
                String assigned = params[1];
                tblSiteModel.setValue(TBL_DATA_SITE_ACTION_HREF,
                    stringToHex(name));
                tblSiteModel.setValue(TBL_DATA_SITE_NAME, name); 
                tblSiteModel.setValue(TBL_DATA_SITE_URL, url);
                tblSiteModel.setValue(TBL_DATA_SITE_SERVERS, assigned);
                tblSiteModel.setSelectionVisible(counter, true);
                cacheSite.put(name, params);
                cache.add(name);
            }
            szCacheSite.setValue(cacheSite);
            szCache.setValue(cache);
        } else {
            szCache.setValue(null);
            szCacheSite.setValue(null);
        }
    }

    private void populateServerTableModel(Map serverToSite) {
        tblServerModel.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        SerializedField szCacheServer = (SerializedField)getChild(
            SZ_CACHE_SERVER);
        ServerSiteModel model = (ServerSiteModel)getModel();
        tblServerModel.setMaxRows(model.getPageSize());
        int counter = 0;
        HashMap cacheServer = new HashMap();
        ArrayList cache = new ArrayList();
        if ((serverToSite != null) && !serverToSite.isEmpty()) {
            for (Iterator iter = serverToSite.keySet().iterator();
                iter.hasNext(); counter++
            ) {
                if (counter > 0) {
                    tblServerModel.appendRow();
                }
                String name = (String)iter.next();
                String siteName = (String)serverToSite.get(name);
                tblServerModel.setValue(TBL_DATA_SERVER_ACTION_HREF, name);
                tblServerModel.setValue(TBL_DATA_SERVER_NAME, name); 
                tblServerModel.setValue(TBL_DATA_SITE, siteName);
                tblServerModel.setSelectionVisible(counter, true);
                cacheServer.put(name, siteName);
                cache.add(name);
            }
            szCache.setValue(cache);
            szCacheServer.setValue(cacheServer);
        } else {
            szCache.setValue(null);
        }
    }

    /**
     * Forwards request to site creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSiteButtonAddRequest(RequestInvocationEvent event) {
        SiteAddViewBean vb = (SiteAddViewBean)getViewBean(
            SiteAddViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to server clone view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblServerButtonCloneRequest(RequestInvocationEvent event)
        throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_SERVERS);
        table.restoreStateData();
        Integer[] selected = tblServerModel.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List list = (List)szCache.getSerializedObj();
        String serverName = (String)list.get(selected[0].intValue());

        ServerCloneViewBean vb = (ServerCloneViewBean)getViewBean(
            ServerCloneViewBean.class);
        unlockPageTrail();
        setPageSessionAttribute(ServerCloneViewBean.PG_ATTR_SERVER_NAME,
            serverName);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to server creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblServerButtonAddRequest(RequestInvocationEvent event) {
        ServerAddViewBean vb = (ServerAddViewBean)getViewBean(
            ServerAddViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Deletes server.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblServerButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SERVERS);
        table.restoreStateData();
        Integer[] selected = tblServerModel.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List list = (List)szCache.getSerializedObj();
        Set names = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            String name = (String)list.get(selected[i].intValue());
            names.add(name);
        }

        try {
            ServerSiteModel model = (ServerSiteModel)getModel();
            model.deleteServers(names);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "serverconfig.message.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "serverconfig.message.deleted.pural");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    /**
     * Deletes site.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblSiteButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SITES);
        table.restoreStateData();
        Integer[] selected = tblSiteModel.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE_1);
        List list = (List)szCache.getSerializedObj();
        Set names = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            String name = (String)list.get(selected[i].intValue());
            names.add(name);
        }

        try {
            ServerSiteModel model = (ServerSiteModel)getModel();
            model.deleteSites(names);
            
            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "siteconfig.message.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "siteconfig.message.deleted.pural");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles edit site request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataSiteActionHrefRequest(RequestInvocationEvent event)
        throws ModelControlException {
        String siteName = hexToString((String)getDisplayFieldValue(
            TBL_DATA_SITE_ACTION_HREF));
        setPageSessionAttribute(SiteEditViewBean.PG_ATTR_SITE_NAME, siteName);
        SiteEditViewBean vb = (SiteEditViewBean)getViewBean(
            SiteEditViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    /**
     * Handles edit server request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataServerActionHrefRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        String serverName = (String)getDisplayFieldValue(
            TBL_DATA_SERVER_ACTION_HREF);
        forwardToServerProfilePage(serverName);
    }
    
    private void forwardToServerProfilePage(String serverName) {
        setPageSessionAttribute(ServerEditGeneralViewBean.PG_ATTR_SERVER_NAME, 
            serverName);

        String tabIdx = (String)getPageSessionAttribute(
            ServerEditGeneralViewBean.TAB_TRACKER);
        int idxTab = 421;

        if (tabIdx != null) {
            idxTab = Integer.parseInt(tabIdx);
        }
        AMViewConfig config = AMViewConfig.getInstance();

        try {
            AMViewBeanBase vb = config.getTabViewBean(this, "/", getModel(),
                ServerEditGeneralViewBean.TAB_NAME, idxTab, -1);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    public void handleBtnDefaultSettingsRequest(RequestInvocationEvent event) {
        forwardToServerProfilePage(ServerConfiguration.DEFAULT_SERVER_CONFIG);
    }

    

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.server.config";
    }
}
