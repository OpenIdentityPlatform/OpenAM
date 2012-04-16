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
 * $Id: SSOTokenID.java,v 1.3 2008/08/15 01:05:20 veiming Exp $
 *
 */

package com.iplanet.sso;

/**
 * The <code>SSOTokenID</code> is an interface that is used to identify a single
 * sign on token object. It contains a random string and the name of the server.
 * The random string in the <code>SSOTokenID</code> is unique on a given server.
 * 
 * @see com.iplanet.sso.SSOToken
 * @supported.all.api
 */
public interface SSOTokenID {
    /**
     * Returns the encrypted Single Sign On token string.
     * 
     * @return An encrypted Single Sign On token string
     */
    String toString();

    /**
     * Returns <code>true</code> if current object is equals to
     * <code>object</code>. This are the conditions
     * <ul>
     * <li><code>object</code> is not null</li>
     * <li>this instance and <code>object</code> have the same random string.
     * <li>this instance and <code>object</code> have the same server name.
     * </ul>
     * 
     * @param object Object for comparison.
     * @return <code>true</code> if current object is equals to
     *         <code>object</code>.
     */
    boolean equals(Object object);

    /**
     * Returns a hash code for this object.
     * 
     * @return a hash code value for this object.
     */
    int hashCode();

}
