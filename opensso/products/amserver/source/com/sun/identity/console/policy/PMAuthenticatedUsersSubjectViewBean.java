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
 * $Id: PMAuthenticatedUsersSubjectViewBean.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import javax.servlet.http.HttpServletRequest;

public class PMAuthenticatedUsersSubjectViewBean
    extends PolicySubjectPluginViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PMAuthenticatedUsersSubject.jsp";

    public PMAuthenticatedUsersSubjectViewBean() {
        super("PMAuthenticatedUsersSubject", DEFAULT_DISPLAY_URL);
    }

    public void handleTfOpRequest(RequestInvocationEvent event) {
        String op = (String)getDisplayFieldValue(TF_OP);
        HttpServletRequest req = getRequestContext().getRequest();
        setPageSessionAttribute(AMAdminConstants.CURRENT_REALM,
            req.getParameter(TF_REALM_NAME));
        setPageSessionAttribute(ProfileViewBeanBase.PG_SESSION_POLICY_CACHE_ID,
            req.getParameter(TF_CACHED_ID));
                                                                                
        SubjectOpViewBeanBase vb = op.equals("edit") ?
            (SubjectOpViewBeanBase)getViewBean(
                PMAuthenticatedUsersSubjectEditViewBean.class) :
            (SubjectOpViewBeanBase)getViewBean(
                PMAuthenticatedUsersSubjectAddViewBean.class);
        setPageSessionAttribute(SubjectOpViewBeanBase.PG_SESSION_SUBJECT_NAME,
            req.getParameter(TF_SUBJECT_NAME));
        setPageSessionAttribute(SubjectOpViewBeanBase.PG_SESSION_SUBJECT_TYPE,
            req.getParameter(TF_SUBJECT_TYPE_NAME));
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
}
