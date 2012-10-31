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
 * $Id: PMAuthenticatedUsersSubjectEditViewBean.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.sun.identity.console.policy.model.PolicyModel;
import java.util.Collections;
import java.util.Set;

public class PMAuthenticatedUsersSubjectEditViewBean
    extends SubjectEditViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PMAuthenticatedUsersSubjectEdit.jsp";

    /**
     * Creates a policy creation view bean.
     */
    public PMAuthenticatedUsersSubjectEditViewBean() {
        super("PMAuthenticatedUsersSubjectEdit", DEFAULT_DISPLAY_URL);
    }

    protected String getPropertyXMLFileName(boolean readonly) {
        return
           "com/sun/identity/console/propertyPMPMAuthenticatedUsersSubject.xml";
    }

    protected Set getDefaultValues(PolicyModel model) {
        return Collections.EMPTY_SET;
    }

    protected boolean hasValues() {
        return false;
    }

    public boolean beginChildDisplay(ChildDisplayEvent event) {
        // do nothing, shortcircuit the implementation from parent class.
        return true;
    }
}
