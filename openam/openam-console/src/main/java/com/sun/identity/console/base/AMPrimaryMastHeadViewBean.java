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
 * $Id: AMPrimaryMastHeadViewBean.java,v 1.11 2009/08/18 22:38:10 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.console.base;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBean;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HREF;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMCommonNameGenerator;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.idm.EndUserViewBean;
import com.sun.identity.console.idm.EntitiesViewBean;
import com.sun.web.ui.model.CCBreadCrumbsModel;
import com.sun.web.ui.model.CCMastheadModel;
import com.sun.web.ui.model.CCNavNodeInterface;
import com.sun.web.ui.model.CCTabsModel;
import com.sun.web.ui.view.breadcrumb.CCBreadCrumbs;
import com.sun.web.ui.view.masthead.CCPrimaryMasthead;
import com.sun.web.ui.view.tabs.CCNodeEventHandlerInterface;
import com.sun.web.ui.view.tabs.CCTabs;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.text.MessageFormat;
import javax.servlet.http.HttpServletRequest;
import org.owasp.esapi.ESAPI;

/**
 * This is the base class for all view beans that have primary
 * <code>masthead</code> in Console.
 */
public abstract class AMPrimaryMastHeadViewBean
    extends AMViewBeanBase
    implements CCNodeEventHandlerInterface
{
    public static final String PG_SESSION_PAGE_TRAIL_ID = "pageTrailID";
    protected static final String PG_SESSION_TAB_ID = "primaryMastHeadTabID";
    protected static Set retainPageSessionsBtwTabs = new HashSet();
    private static final String BREAD_CRUMB = "breadCrumb";
    private static final String BREAD_CRUMB_HREF = "breadCrumbHref";


    static {
        retainPageSessionsBtwTabs.add(PG_SESSION_TAB_ID);
        retainPageSessionsBtwTabs.add(AMAdminConstants.CURRENT_REALM);
        retainPageSessionsBtwTabs.add(AMAdminConstants.CURRENT_ORG);
        retainPageSessionsBtwTabs.add(AMAdminConstants.PREVIOUS_REALM);
        retainPageSessionsBtwTabs.add(EntitiesViewBean.PG_SESSION_ENTITY_TYPE);
    }

    public static final String MH_COMMON = "mhCommon";
    public static final String HDR_COMMON = "hdrCommon";
    public static final String TAB_COMMON = "tabCommon";

    protected CCTabsModel tabModel = null;

    /**
     * Creates an instance of base view bean object.
     *
     * @param name Name of page.
     */
    public AMPrimaryMastHeadViewBean(String name) {
        super(name);
    }

    public void setRequestContext(RequestContext rc) {
        super.setRequestContext(rc);
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        if (realmName != null) {
            createTabModel();
        }
    }

    public void forwardTo(RequestContext rc) {
        if (!handleRealmNameInTabSwitch(rc)) {
            super.forwardTo(rc);
        }
    }
    
    protected boolean handleRealmNameInTabSwitch(RequestContext rc) {
        boolean forwarded = false;
        // Need to default realm name if it is not even set at this point.
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        if (realmName == null) {
            String startDN = AMModelBase.getStartDN(rc.getRequest());
            setPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE, startDN);
        }
        createTabModel();

        DelegationConfig dConfig = DelegationConfig.getInstance();
        //check to see if class is in the noneeddealwith bean
        if (!dConfig.isUncontrolledViewBean(getClass().getName())) {
            if ((tabModel == null) || (tabModel.getNodeCount() == 0)) {
                EndUserViewBean vb = (EndUserViewBean) getViewBean(EndUserViewBean.class);
                vb.forwardTo(rc);
                forwarded = true;
           }
        }
        return forwarded;
    }

    /**
     * Registers user interface components used by this view bean.
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(MH_COMMON, CCPrimaryMasthead.class);
        registerChild(BREAD_CRUMB, CCBreadCrumbs.class);
        registerChild(BREAD_CRUMB_HREF, HREF.class);
    }

    /**
     * Creates user interface components used by this view bean.
     *
     * @param name of component
     * @return child component
     */
    protected View createChild(String name) {
        View view = null;

        if (name.equals(TAB_COMMON)) {
            view = new CCTabs(this, tabModel, name);
        } else if (name.equals(MH_COMMON)) {
            CCPrimaryMasthead mh = new CCPrimaryMasthead(
                this, createMastheadModel(), name);
            mh.setLogoutCommand(AMLogoutCommand.class);
            mh.setUserName(ESAPI.encoder().encodeForHTML(getUserDisplayName()));
            view = mh;
        } else if (name.equals(BREAD_CRUMB)) {
            view = createBreadCrumb(name);
        } else if (name.equals(BREAD_CRUMB_HREF)) {
            view = new HREF(this, name, null);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        beginDisplay(event, true);
    }

    // this allows dervived class to delay the setting of selected tab node.
    protected void beginDisplay(DisplayEvent event, boolean setSelectedTabNode)
        throws ModelControlException {
        super.beginDisplay(event);
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        if (realmName == null) {
            String startDN = AMModelBase.getStartDN(
                getRequestContext().getRequest());
            setPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE, startDN);
        }
        if (setSelectedTabNode) {
            setSelectedTabNode(realmName);
        }
    }

    protected void setSelectedTabNode(String realmName) {
        HttpServletRequest req = getRequestContext().getRequest();
        String strID = (String)getPageSessionAttribute(getTrackingTabIDName());
        int id = 1;

        if ((strID == null) || (strID.trim().length() == 0)) {
            strID = req.getParameter(getTrackingTabIDName());
            setPageSessionAttribute(getTrackingTabIDName(), strID);
        }

        if ((strID == null) || (strID.trim().length() == 0)) {
            id = getDefaultTabId(realmName, req);
        } else {
            try {
                id = Integer.parseInt(strID);
            } catch (NumberFormatException e) {
                AMModelBase.debug.error(
                    "AMPrimaryMastHeadVB.setSelectedTabNode", e);
            }
        }

        tabModel.clear();
        setPageSessionAttribute("CCTabs.SelectedTabId", Integer.toString(id));
        tabModel.setSelectedNode(id);
    }
    
    protected int getDefaultTabId(String realmName, HttpServletRequest req) {
        return AMViewConfig.getInstance().getDefaultTabId(realmName, req);
    } 

    private CCMastheadModel createMastheadModel() {
        CCMastheadModel mm = new CCMastheadModel();
 
        AMModel model = getModel();
        String consoleDirectory = model.getConsoleDirectory();

        /*
         * set the logo; can be different for each realm based on 
         * the console jsp directory attribute.
         */
        String logo = 
            "../" + consoleDirectory + "/images/PrimaryProductName.png";

        mm.setSrc(logo);
        mm.setWidth("");
        mm.setHeight("");
        mm.setVersionProductNameSrc(logo);

        /*
         * enable some of the masthead display features...
         * turn off the date, does not provide anything valid and is
         * issue for localization
         */
        mm.setShowDate(false);
        mm.setShowServer(true);
        mm.setShowUserRole(true);
        mm.setVersionFileName("help/version.html");
        
        return mm;
    }

    private String getUserDisplayName() {
        String name = "";
        AMModel model = getModel();
        if (model != null) {
            String userId = model.getUserName();
            AMCommonNameGenerator gen = AMCommonNameGenerator.getInstance();
            name = gen.generateCommonName(userId, model);
        }
        return name;
    }

    /**********************************************************************
     *
     * code to handle tab related functionality
     *
     **********************************************************************/

    protected void createTabModel() {
        if (tabModel == null) {
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_PROFILE);
            if (realmName != null) {
                AMViewConfig amconfig = AMViewConfig.getInstance();
                tabModel = amconfig.getTabsModel(
                    realmName, getRequestContext().getRequest());
                registerChild(TAB_COMMON, CCTabs.class);
            }
        }
    }

    /**
     * Handles tab selected event.
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        try {
            AMViewBeanBase vb = getTabNodeAssociatedViewBean(null, nodeID);
            passPgSessionMapEx(vb);
            vb.resetView();
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            AMModelBase.debug.error("AMPrimaryMastHeadViewBean.nodeClicked", e);
            forwardTo();
        }
    }

    /**
     * Pass session attribute map to other view bean.
     *
     * @param other view bean
     */
    public void passPgSessionMapEx(ViewBean other) {
        /*
         * This method does not carry over page session attributes
         * from on tab set to another.
         */
        Map attributes = getPageSessionAttributes();
        for (Iterator i = retainPageSessionsBtwTabs.iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            other.setPageSessionAttribute(key,
                (Serializable)attributes.get(key));
        }

        other.setPageSessionAttribute(getTrackingTabIDName(),
            (Serializable)attributes.get(getTrackingTabIDName()));
    }

    /**
     * Returns view bean for a tab.
     *
     * @param tabSetName Name of tab set.
     * @param nodeID Node ID.
     * @return view bean for a tab.
     * @throws AMConsoleException if view bean cannot be found.
     */
    protected AMViewBeanBase getTabNodeAssociatedViewBean(
        String tabSetName,
        int nodeID
    ) throws AMConsoleException {
        AMViewBeanBase v = null;
        AMViewConfig amconfig = AMViewConfig.getInstance();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        if (curRealm == null) {
            curRealm = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        AMPrimaryMastHeadViewBean vb = (tabSetName == null) ?
            (AMPrimaryMastHeadViewBean)amconfig.getTabViewBean(
                this, curRealm, getModel(), nodeID, getChildNodeId(nodeID)) :
            (AMPrimaryMastHeadViewBean)amconfig.getTabViewBean(
                this, curRealm, getModel(), tabSetName, nodeID,
                    getChildNodeId(nodeID));

        setPageSessionAttribute(
            vb.getTrackingTabIDName(), Integer.toString(nodeID));
        return vb;
    }

    private int getChildNodeId(int nodeID) {
        int childNodeId = -1;
        CCNavNodeInterface node = tabModel.getNodeById(nodeID);
        if (node != null) {
            List children = node.getChildren();

            if ((children != null) && !children.isEmpty()) {
                CCNavNodeInterface c = (CCNavNodeInterface)children.get(0);
                childNodeId = c.getId();
            }
        }
        return childNodeId;
    }

    protected String getTrackingTabIDName() {
        return "opensso.SelectedTabId";
    }
    
    protected boolean isRootRealm(String realm, String startDN) {
        boolean isRoot = false;
        if ((realm == null) ||
            realm.length() == 0 ||
            realm.equals("/") ||
            realm.equals(startDN))
        {
            isRoot = true;
        }
        return isRoot;
    }

    private PageTrail getPageTrail() {
        String pageTrailID = getPageTrailID();
        PageTrailManager mgr = PageTrailManager.getInstance();
        AMModel model = getModel();
        return mgr.getTrail(model.getUserSSOToken(), pageTrailID);
    }

    private String getPageTrailID() {
        String pageTrailID = (String)getPageSessionAttribute(
            PG_SESSION_PAGE_TRAIL_ID);
        if (pageTrailID == null) {
            HttpServletRequest req = getRequestContext().getRequest();
            pageTrailID = req.getParameter(PG_SESSION_PAGE_TRAIL_ID);
            setPageSessionAttribute(PG_SESSION_PAGE_TRAIL_ID, pageTrailID);
        }
        return pageTrailID;
    }

    protected void initPageTrail()
        throws AMConsoleException
    {
        Object initPageTrail = getPageSessionAttribute(
            AMAdminConstants.PG_SESSION_INIT_PAGETRAIL);
        boolean swap = (initPageTrail != null) && initPageTrail.equals("2");
        if (swap) {
            initPageTrail = null;
        }

        if (initPageTrail == null) {
            lockPageTrail();
            AMModel model = getModel();
            String pageTrailID = getPageTrailID();
            PageTrailManager mgr = PageTrailManager.getInstance();
            PageTrail trail = null;

            if (pageTrailID != null) {
                trail = mgr.getTrail(model.getUserSSOToken(), pageTrailID);
            }

            if ((trail == null) && !startPageTrail()) {
                throw new AMConsoleException("unable to get page trail");
            }

            if (trail == null) {
                trail = new PageTrail();
                pageTrailID = mgr.registerTrail(model.getUserSSOToken(), trail);
                setPageSessionAttribute(PG_SESSION_PAGE_TRAIL_ID, pageTrailID);
            }

            String displayName = getBreadCrumbDisplayName();
            if (displayName != null) {
                if (swap) {
                    trail.swap(displayName, getClass().getName(),
                        getPageSessionAttributes());
                } else if (startPageTrail()) {
                    trail.set(displayName, getClass().getName(),
                        getPageSessionAttributes());
                } else {
                    trail.add(displayName, getClass().getName(),
                        getPageSessionAttributes());
                }
            }
        }
    }

    protected void setPageTrail(String displayName, String viewBeanClassName) {
        PageTrail trail = getPageTrail();
        trail.set(displayName, viewBeanClassName, getPageSessionAttributes());
    }

    protected void addPageTrail(String displayName, String viewBeanClassName) {
        PageTrail trail = getPageTrail();
        trail.add(displayName, viewBeanClassName, getPageSessionAttributes());
    }

    protected PageTrail.Marker backTrail() {
        PageTrail trail = getPageTrail();
        return (trail != null) ? trail.pop() : null;
    }

    private CCBreadCrumbs createBreadCrumb(String name) {
        PageTrail trail = getPageTrail();
        CCBreadCrumbsModel model = null;

        if (trail != null) {
            List markers = trail.getMarkers();
            int size = markers.size();
            if (size >= 1) {
                PageTrail.Marker marker =
                    (PageTrail.Marker)markers.get(size -1);
                model = new CCBreadCrumbsModel(marker.getDisplayName());
                model.setUseGrayBg("true");
                size--;
                for (int i = 0; i < size; i++) {
                    marker = (PageTrail.Marker)markers.get(i);
                    model.appendRow();
                    model.setValue(
                        CCBreadCrumbsModel.LABEL, marker.getDisplayName());
                    model.setValue(
                        CCBreadCrumbsModel.COMMANDFIELD, BREAD_CRUMB_HREF);
                    model.setValue(
                        CCBreadCrumbsModel.HREF_VALUE, Integer.toString(i));
                }
            } else {
                model = new CCBreadCrumbsModel("dummy");
            }
        } else {
            model = new CCBreadCrumbsModel("dummy");
        }

        return new CCBreadCrumbs(this, model, name);
    }

    protected String getBreadCrumbDisplayName() {
        return null;
    }

    protected boolean startPageTrail() {
        return true;
    }

    public void unlockPageTrail() {
        removePageSessionAttribute(AMAdminConstants.PG_SESSION_INIT_PAGETRAIL);
    }

    public void unlockPageTrailForSwapping() {
        setPageSessionAttribute(
            AMAdminConstants.PG_SESSION_INIT_PAGETRAIL, "2");
    }

    public void lockPageTrail() {
        setPageSessionAttribute(
            AMAdminConstants.PG_SESSION_INIT_PAGETRAIL, "1");
    }

    public void handleBreadCrumbHrefRequest(RequestInvocationEvent event) {
        String idx = (String)getDisplayFieldValue(BREAD_CRUMB_HREF);
        PageTrail trail = getPageTrail();

        if (trail != null) {
            try {
                PageTrail.Marker marker = trail.backTo(Integer.parseInt(idx));
                try {
                    Class clazz = Class.forName(marker.getViewBeanClassName());
                    AMPrimaryMastHeadViewBean vb = (AMPrimaryMastHeadViewBean)
                        getViewBean(clazz);
                    passPgSessionMap(
                        vb, marker.getPageSessionAttributeValues());
                    vb.forwardTo(getRequestContext());
                } catch (ClassNotFoundException e) {
                    forwardTo();
                }
            } catch (AMConsoleException e) {
                AMAdminFrameViewBean vb = (AMAdminFrameViewBean)getViewBean(
                    AMAdminFrameViewBean.class);
                vb.forwardTo(getRequestContext());
            }
        } else {
            AMAdminFrameViewBean vb = (AMAdminFrameViewBean)getViewBean(
                AMAdminFrameViewBean.class);
            vb.forwardTo(getRequestContext());
        }
    }
}
