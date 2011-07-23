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
 * $Id: GetUserReq.java,v 1.2 2008/06/25 05:52:27 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMStoreConnection;
import java.util.HashSet;
import java.util.Set;

abstract class GetUserReq extends SearchReq {

    protected Set userDNs = new HashSet();
    protected boolean DNsOnly = true;

    /**
     * Constructs a new GetUserReq.
     *
     * @param targetDN 
     */
    GetUserReq(String targetDN) {
        super(targetDN);
    }

    /**
     * sets the value for DNsOnly which tells the process() method to get only
     * the DNs or all the information.
     *
     * @param DNsOnly  if true only DN's , if false all the information.
     */
    void setDNsOnly(boolean DNsOnly) {
        this.DNsOnly = DNsOnly;
    }

    /**
     * gets the value of DNsOnly which tells the process() method to get only
     * the DNs or all the information.
     *
     * @return DNsOnly if true get only DN's, if false get all the information.
     */
    boolean isDNsOnly() {
        return DNsOnly;
    }

    /**
     * adds the user dn to Set userDNs which holds all the user dn's.
     *
     * @param userDN  the DN of a user
     */
    void addUserDNs(String userDN) {
        userDNs.add(userDN);
    }

    /**
     * gets the userDNs set which contains all the users DN's whose information
     * should be retrieved.
     *
     * @return   userDNs which contains all the user DN's .
     */
    Set getUserDNs() {
        return userDNs;
    }

    abstract void process(AMStoreConnection dpConnection)
        throws AdminException;
}
