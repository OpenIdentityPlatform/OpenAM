/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FSAccountFedInfoKey.java,v 1.3 2008/06/25 05:46:39 qcheng Exp $
 *
 */

package com.sun.identity.federation.accountmgmt;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;

/**
 * This class handles the user account's federation information key.
 */
public class FSAccountFedInfoKey {

    /**
     * Specifies provider's (SP/IDP) nameSpace.
     */
    private String nameSpace = "";
    
    /**
     * Contains Opaque Handle sent to other side in federation process.
     */
    private String name = "";
    
    /**
     * Default Constructor.
     */
    public FSAccountFedInfoKey() {
    }

    /**
     * Constructor.
     * @param nameSpace Specifies provider's (SP/IDP) nameSpace.
     * @param name Contains Opaque Handle sent/received 
     *  in federation process.
     * @exception FSAccountMgmtException if illegal argument passed.
     */
    public FSAccountFedInfoKey(
        String nameSpace, 
        String name)
        throws FSAccountMgmtException
    {
        if ((nameSpace == null) || (nameSpace.length() <= 0)) {
            FSUtils.debug.error(
                "FSAccountFedInfoKey: Invalid Argument: nameSpace is " +
                nameSpace);
            throw new FSAccountMgmtException(
                IFSConstants.NULL_NAME_SPACE, null);
        }
        
        if ((name == null) || (name.length() <= 0)) {
            FSUtils.debug.error(
                "FSAccountFedInfoKey: Invalid Argument: name is " + name);
            throw new FSAccountMgmtException(
                IFSConstants.NULL_PROVIDER_ID, null);
        }

        this.nameSpace = nameSpace;
        this.name = name;

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAccountFedInfoKey : nameSpace :: " + this.nameSpace +
                ", name :: " + this.name);
        }
    }

    /**
     * Gets provider's (SP/IDP) nameSpace.
     * @return provider's nameSpace
     */
    public String getNameSpace() {
        return this.nameSpace;
    }
    
    /**
     * Sets provider's nameSpace.
     * @param nameSpace - provider's nameSpace
     */
    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }
    
    /**
     * Gets opaque handle sent/received.
     * @return opaque handle sent/received
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Sets opaque handle sent/received.
     * @param name opaque handle sent/received
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the namespace and name in String.
     * @return provider's name space and name, seprated with pipe
     */
    public String toString() {
       return nameSpace + "|" + name; 
    }
}
