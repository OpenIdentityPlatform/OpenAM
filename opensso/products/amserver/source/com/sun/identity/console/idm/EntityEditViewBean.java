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
 * $Id: EntityEditViewBean.java,v 1.11 2009/01/28 05:34:57 ww203982 Exp $
 *
 */ 
        
package com.sun.identity.console.idm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMPostViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.idm.model.EntitiesModel;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.model.CCNavNode;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.model.CCTabsModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.shared.ldap.LDAPDN; 

public class EntityEditViewBean
    extends EntityOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/idm/EntityEdit.jsp";
    public static final String UNIVERSAL_ID = "universalId";
    public static final String PG_SESSION_ENTITY_TAB = "entityTab";
    public static final String PG_SESSION_MEMBER_TYPE = "memberType";
    public static final String TAB_PROFILE_COMP = "tabProfile";
    static final String PROPERTY_UUID = "tfUUID";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    public static final int TAB_PROFILE = 0;
    public static final int TAB_SERVICES= 1;

    protected String identityDisplayName;

    EntityEditViewBean(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    public EntityEditViewBean() {
        super("EntityEdit", DEFAULT_DISPLAY_URL);
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
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    protected String getBackButtonLabel() {
        return getBackButtonLabel("policy.table.title.subjects");
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        EntitiesModel model = (EntitiesModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Map localized = model.getSupportedEntityTypes(curRealm);

        String type = (String)getPageSessionAttribute(ENTITY_TYPE);
        String i18nName = (String)localized.get(type);
        String title = model.getLocalizedString(
            "page.title.entities.edit");

        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        propertySheetModel.setValue(PROPERTY_UUID, universalId);

        // identityDisplayName also used be EndUserViewBean
        identityDisplayName = 
           AMFormatUtils.getIdentityDisplayName(model, universalId);
        Object[] param = { i18nName, identityDisplayName };
        ptModel.setPageTitleText(MessageFormat.format(title, param));
        
        checkForAttributesToDisplay(type);

        if (hasNoAttributeToDisplay) {
            disableSaveAndResetButton();
        }
    }

    protected void checkForAttributesToDisplay(String type) {
        if (this.getClass().equals(EntityEditViewBean.class)) {
            EntitiesModel model = (EntitiesModel)getModel();
            String agentType = (String)getPageSessionAttribute(
                ENTITY_AGENT_TYPE);
            String serviceName = model.getServiceNameForIdType(type, 
                agentType);
            hasNoAttributeToDisplay =
                (serviceName == null) || (serviceName.trim().length() == 0);
        }
    }

    protected void disableSaveAndResetButton() {
        disableButton("button1", true);
        disableButton("button2", true);
    }

    protected boolean createPropertyModel() {
        boolean created = super.createPropertyModel();

        if (created) {
            String type = (String)getPageSessionAttribute(ENTITY_TYPE);
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            EntitiesModel model = (EntitiesModel)getModel();
            tabModel = new CCTabsModel();
            tabModel.addNode(new CCNavNode(TAB_PROFILE, "profile.tab", "", ""));

            if (model.canAssignService(curRealm, type)) {
                tabModel.addNode(
                    new CCNavNode(TAB_SERVICES, "services.tab", "", ""));
            }

            try {
                Set memberOfs = model.getIdTypeMemberOf(curRealm, type);
                for (Iterator iter = memberOfs.iterator(); iter.hasNext(); ) {
                    IdType t = (IdType)iter.next();
                    tabModel.addNode(
                        new CCNavNode(t.hashCode(), t.getName(), "", ""));
                }

                Set beMemberOfs = model.getIdTypeBeMemberOf(curRealm, type);
                if ((beMemberOfs !=null) && !beMemberOfs.isEmpty()) {
                    for (Iterator i = beMemberOfs.iterator(); i.hasNext(); ) {
                        IdType t = (IdType)i.next();
                        tabModel.addNode(
                            new CCNavNode(t.hashCode(), t.getName(), "", ""));
                    }
                }
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
            tabModel.setSelectedNode(TAB_PROFILE);
        }

        return created;
    }

    protected void setDefaultValues(String type)
        throws AMConsoleException {
        if (propertySheetModel != null) {
            EntitiesModel model = (EntitiesModel)getModel();
            String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);

            if (!submitCycle) {
                propertySheetModel.clear();

                try {
                    Map attrValues = model.getAttributeValues(
                        universalId, false);
                    AMPropertySheet prop = (AMPropertySheet)getChild(
                        PROPERTY_ATTRIBUTE);
                    prop.setAttributeValues(attrValues, model);
                } catch (AMConsoleException e) {
                    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
                }
            }

            String[] uuid = {universalId};
            propertySheetModel.setValues(PROPERTY_UUID, uuid, model);
        }
    }

    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        EntitiesModel model = (EntitiesModel)getModel();
        AMPropertySheet prop = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        try {
            Map oldValues = model.getAttributeValues(universalId, false);
            Map values = prop.getAttributeValues(oldValues, true, model);
            String curRealm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            model.modifyEntity(curRealm, universalId, values);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "message.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    protected boolean isCreateViewBean() {
        return false;
    }

    /**
     * Handles tab selected event.
     *
     * @param event Request Invocation Event.
     * @param nodeID Selected Node ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        EntityEditViewBean vb = null;
        IdType idType = null;
        EntitiesModel model = (EntitiesModel)getModel();
        unlockPageTrailForSwapping();

        setPageSessionAttribute(
            getTrackingTabIDName(), Integer.toString(nodeID)); 
        if (nodeID == TAB_PROFILE) {
            vb = (EntityEditViewBean)getViewBean(EntityEditViewBean.class);
            forwardToOtherEntityViewBean(vb, idType);
        } else if (nodeID == TAB_SERVICES) {
            vb = (EntityServicesViewBean)getViewBean(
                EntityServicesViewBean.class);
            forwardToOtherEntityViewBean(vb, idType);
        } else {
            handleMembersViewForwarding(nodeID, model);
        }
    }
    
    protected int getDefaultTabId(String realmName, HttpServletRequest req) {
        return TAB_PROFILE;
    } 

    private void forwardToOtherEntityViewBean(
        EntityEditViewBean vb,
        IdType idType
    ) {
        if (vb == null) {
            forwardTo();
        } else {
            String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
            if (idType != null) {
                setPageSessionAttribute(PG_SESSION_MEMBER_TYPE,
                    idType.getName());
            }
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }

    private void handleMembersViewForwarding(int nodeID, EntitiesModel model) {
        String type = (String)getPageSessionAttribute(ENTITY_TYPE);
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);

        try {
            Set memberOfs = model.getIdTypeMemberOf(curRealm, type);
            IdType idType = getIdType(nodeID);

            if (idType != null) {
                /*
                 * 20050328 Dennis
                 * bug 6246346
                 */
                String viewURL = null;
                if (memberOfs.contains(idType)) {
                    viewURL = "../idm/EntityMembership";
                } else {
                    viewURL = (model.canAddMember(
                        curRealm, type, idType.getName())) ?
                        "../idm/EntityMembers" :
                        "../idm/EntityMembersFilteredIdentity";
                }
                AMPostViewBean vb = (AMPostViewBean)getViewBean(
                    AMPostViewBean.class);
                setPageSessionAttribute(PG_SESSION_MEMBER_TYPE,
                    idType.getName());
                passPgSessionMap(vb);
                vb.setTargetViewBeanURL(viewURL);
                vb.forwardTo(getRequestContext());
            } else {
                forwardTo();
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    protected IdType getIdType(int hashCode) {
        IdType idType = null;
        EntitiesModel model = (EntitiesModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Map types = model.getSupportedEntityTypes(curRealm);

        for (Iterator iter = types.keySet().iterator();
            iter.hasNext() && (idType == null);
        ) {
            try {
                IdType type = IdUtils.getType((String)iter.next());
                if (type.hashCode() == hashCode) {
                    idType = type;
                }
            } catch (IdRepoException e) {
                // display message
            }
        }

        return idType;
    }

    protected String getTrackingTabIDName() {
        return PG_SESSION_ENTITY_TAB;
    }

    protected OptionList getOptionListForEntities(Collection entities) {
        OptionList optList = new OptionList();

        if ((entities != null) && !entities.isEmpty()) {
            EntitiesModel model = (EntitiesModel)getModel();
            Map lookup = new HashMap(entities.size() *2);
            Set unsorted = new HashSet(entities.size() *2);

            for (Iterator iter = entities.iterator(); iter.hasNext(); ) {
                AMIdentity entity = (AMIdentity)iter.next();
                String name = AMFormatUtils.getIdentityDisplayName(
                    model, entity);
                String universalId = IdUtils.getUniversalId(entity);
                lookup.put(universalId, name);
                unsorted.add(name);
            }

            List list = AMFormatUtils.sortItems(
                unsorted, model.getUserLocale());
            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                String name = (String)iter.next();
                String id= null;
                String tmp = null;
                for (Iterator it = lookup.keySet().iterator(); it.hasNext(); ) {
                   id = (String)it.next();
                   if (lookup.get(id).equals(name)) {
                       String[] comps = LDAPDN.explodeDN(id, true);
                       tmp = name + "(" + comps[0] + ")"; 
                       optList.add(tmp, id);
                }
            }
        }
      }

        return optList;
    }

    protected List getEntityDisplayNames(Collection entities) {
        List displayNames = null;
        if ((entities != null) && !entities.isEmpty()) {
            EntitiesModel model = (EntitiesModel)getModel();
            Set names = new HashSet(entities.size() *2);

            for (Iterator iter = entities.iterator(); iter.hasNext(); ) {
                AMIdentity entity = (AMIdentity)iter.next();
                names.add(AMFormatUtils.getIdentityDisplayName(
                    model, entity));
            }
            displayNames = AMFormatUtils.sortItems(names,
                model.getUserLocale());
        }

        return (displayNames != null) ? displayNames : Collections.EMPTY_LIST;
    }

    protected Set getEntitiesID(Set entities) {
        Set ids = new HashSet(entities.size() *2);
        for (Iterator iter = entities.iterator(); iter.hasNext(); ) {
            AMIdentity entity = (AMIdentity)iter.next();
            ids.add(IdUtils.getUniversalId(entity));
        }
        return ids;
    }

    protected String getBreadCrumbDisplayName() {
        EntitiesModel model = (EntitiesModel)getModel();
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        String[] arg ={AMFormatUtils.getIdentityDisplayName(
            model, universalId)};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.editentities"), (Object[])arg);
    }

    /**
     * Handles "back to" page request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        removePageSessionAttribute(getTrackingTabIDName());
        setPageSessionAttribute(
            getTrackingTabIDName(), Integer.toString(TAB_PROFILE));
        forwardToEntitiesViewBean();
    }

    protected boolean startPageTrail() {
        return false;
    }
}
