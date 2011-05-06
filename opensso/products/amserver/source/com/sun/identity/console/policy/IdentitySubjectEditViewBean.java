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
 * $Id: IdentitySubjectEditViewBean.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.sso.SSOToken;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.console.policy.model.IdentitySubjectModel;
import com.sun.identity.console.policy.model.IdentitySubjectModelImpl;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdUtils;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class IdentitySubjectEditViewBean
    extends SubjectEditViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/IdentitySubjectEdit.jsp";
    private static final String FILTER_TYPE = "tfType";
    private static final String ENTITY_TYPE = "searchEntityType";

    /**
     * Creates a policy creation view bean.
     */
    public IdentitySubjectEditViewBean() {
        super("IdentitySubjectEdit", DEFAULT_DISPLAY_URL);
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        return (readonly) ?
            "com/sun/identity/console/propertyPMIdentitySubject_Readonly.xml" :
            "com/sun/identity/console/propertyPMIdentitySubject.xml";
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        IdentitySubjectModel model = (IdentitySubjectModel)getModel();
        String realmName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        Set values = null;
        if (bFilter) {
            Set defaultValue = getValues();
            if (defaultValue != null) {
                values = getAMIdentity(model, defaultValue);
            }
        } else {
            values = getAMIdentity(model, getDefaultValues(model));
        }

        super.beginDisplay(event);

        if (canModify) {
            Map supportedEntityTypes = model.getSupportedEntityTypes(realmName);
            CCDropDownMenu menu = (CCDropDownMenu)getChild(FILTER_TYPE);
            OptionList entityTypes = createOptionList(supportedEntityTypes);
            entityTypes.add(0, "policy.subject.select.identity.type", "");
            menu.setOptions(entityTypes);
            menu.setValue("");

            CCAddRemove child = (CCAddRemove)getChild(
                VALUES_MULTIPLE_CHOICE_VALUE);
            child.restoreStateData();
            OptionList selected = addRemoveModel.getSelectedOptionList();
            child.resetStateData();
            addRemoveModel.setAvailableOptionList(createOptionList(
                getPossibleValues(model, realmName, values)));
            if (submitCycle) {
                addRemoveModel.setSelectedOptionList(selected);
            } else {
                addRemoveModel.setSelectedOptionList(createOptionList(values));
            }
        } else {
            propertySheetModel.setValue(VALUES_MULTIPLE_CHOICE_VALUE,
                AMAdminUtils.getString(getDefaultValues(), ",", false));
        }
    }

    private Set getPossibleValues(
        IdentitySubjectModel model,
        String realmName,
        Set values
    ) {
        Set possibleValues = null;
        String searchEntityType = (String)getPageSessionAttribute(ENTITY_TYPE);
        if ((searchEntityType != null) && (searchEntityType.length() > 0)) {
            String pattern = (String)propertySheetModel.getValue(FILTER);

            try {
                IdSearchResults results = model.getEntityNames(
                    realmName, searchEntityType, pattern);
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
                
                // remove the system users which should not be displayed.
                    Set hiddenUsers = model.getSpecialUsers(realmName);
                    possibleValues.removeAll(hiddenUsers);  

                if ((possibleValues != null) && !possibleValues.isEmpty()) {
                    if (submitCycle) {
                        CCAddRemove child = (CCAddRemove)getChild(
                            VALUES_MULTIPLE_CHOICE_VALUE);
                        Set selected = getValues(
                            addRemoveModel.getSelectedOptionList());
                        if ((selected != null) && !selected.isEmpty()) {
                            Set amids = getAMIdentity(model, selected);
                            possibleValues.removeAll(amids);
                        }
                    } else if (values != null) {
                        possibleValues.removeAll(values);
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

    protected Set getDefaultValues(IdentitySubjectModel model) {
        Set values = null;
        String subjectName = (String)getPageSessionAttribute(
            SubjectOpViewBeanBase.PG_SESSION_SUBJECT_NAME);
         try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            Subject subject = policy.getSubject(subjectName);
            values = subject.getValues();
         } catch (NameNotFoundException e) {
            debug.warning("IdentitySubjectEditViewBean.getDefaultValues", e);
         } catch (AMConsoleException e) {
            debug.warning("IdentitySubjectEditViewBean.getDefaultValues", e);
        }
        return (values != null) ? values : Collections.EMPTY_SET;
    }

    private Set getAMIdentity(IdentitySubjectModel model, Set ids) {
        Set values = Collections.EMPTY_SET;
        if ((ids != null) && !ids.isEmpty()) {
            values = new HashSet(ids.size()*2);
            SSOToken token = model.getUserSSOToken();

            for (Iterator i = ids.iterator(); i.hasNext(); ) {
                String id = (String)i.next();
                try {
                    AMIdentity amid = IdUtils.getIdentity(token, id);
                    values.add(amid);
                } catch (IdRepoException e) {
                    debug.warning(
                        "IdentitySubjectEditViewBean.getAMIdentity", e);
                }
            }
        }
        return values;
    }

    protected Set getValues(String subjectType)
        throws ModelControlException {
        CCAddRemove child = (CCAddRemove)getChild(
            VALUES_MULTIPLE_CHOICE_VALUE);
        child.restoreStateData();
        Set values = getValues(addRemoveModel.getSelectedOptionList());

        if ((values == null) || values.isEmpty()) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                "policy.missing.subject.value");
            values = null;
        }

        return values;
    }

    protected void setAddRemoveModel()
        throws ModelControlException {
        // NO-OP
    }

    protected OptionList createOptionList(Set values) {
        OptionList optList = new OptionList();

        if ((values != null) && !values.isEmpty()) {
            for (Iterator iter = values.iterator(); iter.hasNext(); ) {
                AMIdentity identity = (AMIdentity)iter.next();
                optList.add(
                    PolicyUtils.getDNDisplayString(identity.getName()),
                    IdUtils.getUniversalId(identity));
            }
        }

        return optList;
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new IdentitySubjectModelImpl(req, getPageSessionAttributes());
    }

    /**
     * Handles filter results request.
     *
     * @param event Request invocation event.
     */
    public void handleBtnFilterRequest(RequestInvocationEvent event) {
        CCDropDownMenu menu = (CCDropDownMenu)getChild(FILTER_TYPE);
        setPageSessionAttribute(ENTITY_TYPE, (String)menu.getValue());
        super.handleBtnFilterRequest(event);
    }

    public boolean beginChildDisplay(ChildDisplayEvent event) {
        // do nothing, shortcircuit the implementation from parent class.
        return true;
    }

    protected Set getValidValues() {
        // Do not go to plugins to get valid values
        return Collections.EMPTY_SET;
    }
}
