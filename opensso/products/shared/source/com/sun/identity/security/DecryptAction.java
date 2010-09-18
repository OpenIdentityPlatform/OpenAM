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
 * $Id: DecryptAction.java,v 1.4 2008/08/19 19:14:55 veiming Exp $
 *
 */

package com.sun.identity.security;

import java.security.PrivilegedAction;

import com.iplanet.am.util.AMPasswordUtil;

/**
 * 
 * The class is used to perform privileged operation with
 * <code>AccessController.doPrivileged()
 * </code> when using
 * <code> com.iplanet.am.util.AMPasswordUtil</code> to decrypt passwords. Ths
 * class implements the interface <code>
 * PrivilegedAction </code> with a
 * non-default constructor. This class should be used in order to perform the
 * privileged operation of
 * <code> com.iplanet.am.util.AMPasswordUtil.decrypt()</code>.
 * 
 * <PRE>
 * 
 * This line of code: String encStr =
 * com.iplanet.am.util.AMPasswordUtil.decrypt(str); should be replaced with:
 * String encStr = (String) AccessController.doPrivileged( new
 * DecryptAction(str)); If this is not done and Java security permissions check
 * is enabled, then the operation will fail and return a null everytime.
 * 
 * Note: Java security permissions check for OpenSSO can be enabled by
 * setting the property <code>com.sun.identity.security.checkcaller</code> to
 * true in AMConfig properties file.
 * 
 * </PRE>
 *
 * @supported.all.api
 */
public class DecryptAction implements PrivilegedAction {
    protected String value;

    /**
     * Non default constructor to be used when a <code>doPrivileged()</code>
     * is performed for the decryption operations.
     * 
     * @param svalue
     *            Value of string to be encoded/decoded
     * 
     */
    public DecryptAction(String svalue) {
        super();
        value = svalue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.security.PrivilegedAction#run()
     */
    public Object run() {
        return AMPasswordUtil.decrypt(value);
    }

}
