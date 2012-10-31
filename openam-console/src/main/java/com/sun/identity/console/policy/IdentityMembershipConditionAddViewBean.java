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
 * $Id: IdentityMembershipConditionAddViewBean.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.console.policy.model.IdentitySubjectModel;
import com.sun.identity.console.policy.model.IdentitySubjectModelImpl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.html.CCTextField;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Default implementation of Identity Member Condition Add View Bean.
 */
public class IdentityMembershipConditionAddViewBean
    extends ConditionAddViewBean {
    private static final String FILTER = "tfFilter";
    private static final String FILTER_TYPE = "tfType";
    private static final String ENTITY_TYPE = "searchEntityType";
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/IdentityMembershipConditionAdd.jsp";
    private IdentityMembershipHelper helper = 
        IdentityMembershipHelper.getInstance();
    private boolean bFilter;

    /**
     * Creates an instance of
     * <code>IdentityMembershipConditionAddViewBean</code>.
     */
    public IdentityMembershipConditionAddViewBean() {
        super("IdentityMembershipConditionAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        /*
         * readonly is not used because it only applies to edit view bean.
         * this is parameter is required because we have a generic method
         * for both add and edit view bean.
         */
        return AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyPMIdentityMembershipCondition.xml"
            ));
    }
    
    protected String getMissingValuesMessage() {
        return "policy.condition.missing.identity.membership.message";
    }

    protected void setPropertiesValues(PolicyModel model, Map values) {
        CCAddRemove child = (CCAddRemove)getChild(VALUES_MULTIPLE_CHOICE_VALUE);
        helper.setSelectedIdentities(child, model.getUserSSOToken(), 
            model.getUserLocale(), this, values);
    }

    protected Map getConditionValues(
        PolicyModel model,
        String realmName,
        String conditionType
    ) {
        CCAddRemove child = (CCAddRemove)getChild(
            VALUES_MULTIPLE_CHOICE_VALUE);
        return helper.getSelectedIdentities(child);
    }
    
    protected IdentitySubjectModel getSubjectModel() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new IdentitySubjectModelImpl(req, getPageSessionAttributes());
    }

    protected View createChild(String name) {
        View view = null;

        if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
            if (name.equals(FILTER)) {
                ((CCTextField)view).setValue("*");
            }
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    /**
     * Sets the values to UI model.
     *
     * @param event Display Event.
     * @throws ModelControlException if the default UI model is not accessible.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        IdentitySubjectModel subjectModel = getSubjectModel();
        PolicyModel model = (PolicyModel)getModel();
        super.beginDisplay(event);
        CCDropDownMenu menu = (CCDropDownMenu)getChild(FILTER_TYPE);
        Map supportedEntityTypes = model.getSupportedEntityTypes(realmName);
        OptionList entityTypes = createOptionList(supportedEntityTypes);
        entityTypes.add(0, "policy.subject.select.identity.type", "");
        menu.setOptions(entityTypes);
        menu.setValue("");
        
        // initialize the available/selected component
        CCAddRemove child = (CCAddRemove)getChild(
            VALUES_MULTIPLE_CHOICE_VALUE);
        child.restoreStateData();
        OptionList selected = addRemoveModel.getSelectedOptionList();
        OptionList possible = helper.createOptionList(
            model.getUserSSOToken(), model.getUserLocale(), this,
            getPossibleValues(subjectModel, realmName));
        child.resetStateData();
        addRemoveModel.setAvailableOptionList(possible);
    }
    
    /**
     * Returns a set of supported AMIdentity objects for a realm.
     */
    private Set getPossibleValues(IdentitySubjectModel model, String realmName){
        Set possibleValues = null;
        String entityType = (String)getPageSessionAttribute(ENTITY_TYPE);
        if ((entityType != null) && (entityType.length() > 0)) {
            String pattern = (String)propertySheetModel.getValue(FILTER);

            try {
                IdSearchResults results = model.getEntityNames(
                    realmName, entityType, pattern);
                int errorCode = results.getErrorCode();

                switch (errorCode) {
                case IdSearchResults.SIZE_LIMIT_EXCEEDED:
                    setInlineAlertMessage(CCAlert.TYPE_WARNING,
                        "message.warning", "message.sizelimit.exceeded");
                    break;
                case IdSearchResults.TIME_LIMIT_EXCEEDED:
                    setInlineAlertMessage(CCAlert.TYPE_WARNING,
                        "message.warning", "message.timelimit.exceeded");
                    break;
                }

                possibleValues = results.getSearchResults();
                if ((possibleValues != null) && !possibleValues.isEmpty()) {     
                    // remove the system users which should not be displayed.
                    Set hiddenUsers = model.getSpecialUsers(realmName);
                    possibleValues.removeAll(hiddenUsers);

                    // remove the identities that are already selected
                    Set selected = getValues(
                        addRemoveModel.getSelectedOptionList());
                    if ((selected != null) && !selected.isEmpty()) {
                        Set amids = helper.getAMIdentity(
                            model.getUserSSOToken(), selected);
                        possibleValues.removeAll(amids);
                    }
                }
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }

        return (possibleValues != null) ?
            possibleValues : Collections.EMPTY_SET;
    }
    
    /**
     * Handles filter results request.
     *
     * @param event Request invocation event.
     */
    public void handleBtnFilterRequest(RequestInvocationEvent event) {
        CCDropDownMenu menu = (CCDropDownMenu)getChild(FILTER_TYPE);
        setPageSessionAttribute(ENTITY_TYPE, (String)menu.getValue());
        bFilter = true; 
        submitCycle = true;
        forwardTo(); 
    }
}
