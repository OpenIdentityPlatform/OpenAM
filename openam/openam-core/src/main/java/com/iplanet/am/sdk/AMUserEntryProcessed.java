/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMUserEntryProcessed.java,v 1.2 2008/06/25 05:41:23 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;

import com.iplanet.sso.SSOToken;

/**
 * Interface that needs to be implemented by external applications inorder to do
 * some special processing for user creations/deletions/modifications. The
 * implementation module is pluggable and is configurable via
 * <code>AMConfig.properties</code>. The property to set is
 * <code>com.iplanet.am.sdk.userEntryProcessingImpl</code>. <BR>
 * NOTE: All these interface methods will be invoked whenever a user entry is
 * created/modified/deleted using Sun Java System Access Manager SDK.
 * 
 * @deprecated This interface has been deprecated. Please use
 *             {@link AMCallBack AMCallBack}
 * 
 */
public interface AMUserEntryProcessed {

    /**
     * Method which gets invoked whenever a user is created
     * 
     * @param token
     *            the single sign on token.
     * @param userDN
     *            the DN of the user being added
     * @param attributes
     *            a map consisting of attribute names and a set of values for
     *            each of them
     */
    public void processUserAdd(SSOToken token, String userDN, Map attributes);

    /**
     * Method which gets invoked whenever a user entry is modified
     * 
     * @param token
     *            the single sign on token.
     * @param userDN
     *            the DN of the user being modified
     * @param oldAttributes
     *            a map consisting of attribute names and a set of values for
     *            each of them before modification
     * @param newAttributes
     *            a map consisting of attribute names and a set of values for
     *            each of them after modification
     */
    public void processUserModify(SSOToken token, String userDN,
            Map oldAttributes, Map newAttributes);

    /**
     * Method which gets invoked whenever a user entry is deleted
     * 
     * @param token
     *            the single sign on token.
     * @param userDN
     *            the DN of the user being deleted
     * @param attributes
     *            a map consisting of attribute names and a set of values for
     *            each of them
     */
    public void processUserDelete(SSOToken token, 
            String userDN, Map attributes);
}
