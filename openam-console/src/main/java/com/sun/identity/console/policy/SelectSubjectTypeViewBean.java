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
 * $Id: SelectSubjectTypeViewBean.java,v 1.2 2008/06/25 05:43:05 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMDisplayType;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.QueryResults;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.Syntax;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;

public class SelectSubjectTypeViewBean
    extends SelectTypeViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/SelectSubjectType.jsp";
    private static final String ATTR_SUBJECT_NAME = "tfSubjectName";
    private static final String ATTR_SUBJECT_TYPE = "radioSubjectType";
    public static final String CALLING_VIEW_BEAN =
        "SelectSubjectTypeViewBeanCallingVB";

    /**
     * Creates a view to prompt user for subject type before subject creation.
     */
    public SelectSubjectTypeViewBean() {
        super("SelectSubjectType", DEFAULT_DISPLAY_URL);
    }

    protected String getTypeOptionsChildName() {
        return ATTR_SUBJECT_TYPE;
    }

    protected OptionList getTypeOptions() {
        PolicyModel model = (PolicyModel)getModel();
        String curRealm = (String)getPageSessionAttribute(
           AMAdminConstants.CURRENT_REALM);
        QueryResults qr = model.getActiveSubjectTypes(curRealm);
        String strError = qr.getStrError();
        if ((strError != null) && (strError.length() > 0)) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                strError);
        }
        return createOptionList((Map)qr.getResults(), model.getUserLocale());
    }

    protected String getPropertyXMLFileName() {
        return "com/sun/identity/console/propertyPMSelectSubjectType.xml";
    }

    protected String getCallingViewBeanPgSessionName() {
        return CALLING_VIEW_BEAN;
    }

    /**
     * Handles next button request.
     *
     * @param event Request invocation event.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        PolicyModel model = (PolicyModel)getModel();
        String subjectType = (String)propertySheetModel.getValue(
            ATTR_SUBJECT_TYPE);

            setPageSessionAttribute(SubjectOpViewBeanBase.CALLING_VIEW_BEAN,
                (String)getPageSessionAttribute(CALLING_VIEW_BEAN));
            String realmName = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            String viewBeanURL = model.getSubjectViewBeanURL(
                realmName, subjectType);
            unlockPageTrailForSwapping();

            if ((viewBeanURL != null) && (viewBeanURL.trim().length() > 0)) {
                forwardToURL(viewBeanURL, subjectType, realmName);
            } else {
                forwardToViewBean(model, subjectType, realmName);
            }
    }

    private void forwardToURL(
        String url,
        String subjectType,
        String realmName
    ) {
        SubjectProxyViewBean vb = (SubjectProxyViewBean)getViewBean(
            SubjectProxyViewBean.class);
        passPgSessionMap(vb);
        vb.setURL(url, "add");
        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_SUBJECT_TYPE_NAME,
            subjectType);
        if ((realmName == null) || (realmName.trim().length() == 0)) {
            realmName = AMModelBase.getStartDN(
                getRequestContext().getRequest());
        }

        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_REALM_NAME, realmName);
        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_CACHED_ID,
            (String)getPageSessionAttribute(
                ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID));
        vb.setDisplayFieldValue(SubjectProxyViewBean.TF_OP, "add");
        vb.forwardTo(getRequestContext());
    }

    private void forwardToViewBean(
        PolicyModel model,
        String subjectType,
        String realmName
    ) {
        Syntax syntax = model.getSubjectSyntax(realmName, subjectType);
        int displaySyntax = AMDisplayType.getDisplaySyntax(syntax);
        SubjectAddViewBean vb = null;

        switch (displaySyntax) {
        case AMDisplayType.SYNTAX_TEXT:
        case AMDisplayType.SYNTAX_SINGLE_CHOICE:
        case AMDisplayType.SYNTAX_MULTIPLE_CHOICE:
            vb = (SubjectAddViewBean)getViewBean(SubjectAddViewBean.class);
            break;
        default:
            vb = (SubjectNoneAddViewBean)getViewBean(
                SubjectNoneAddViewBean.class);
            break;
        }
        setPageSessionAttribute(SubjectOpViewBeanBase.PG_SESSION_SUBJECT_TYPE,
            subjectType);

        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.selectSubjectType";
    }

    protected boolean startPageTrail() {
        return false;
    }

}
