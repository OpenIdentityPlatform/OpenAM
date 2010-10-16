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
 * $Id: UserIDGenerator.java,v 1.2 2008/06/25 05:42:06 qcheng Exp $
 *
 */



package com.sun.identity.authentication.spi;

import java.util.Map;
import java.util.Set;

/**
 * An application implements a <code>UserIDGenerator</code> interface and
 * registers itself to the Core Authentication service so that authentication 
 * modules can retrieve a list of auto-generated user IDs. The method that each 
 * individual module implements <code>AMLoginModule</code> can be used to 
 * retrieve such list is <code>getNewUserIDs()</code>. For example in
 * self-registration module, when an end-user tries to register a user ID that
 * is not valid, the module then can display a list of alternate user IDs
 * that the end-user could be used to complete the registration.
 *
 * @supported.all.api
 */
public interface UserIDGenerator {
    /**
     * Generates a set of user IDs. Optionally, the specified parameters,
     * <code>orgName</code> and attributes, could be used to generate the user
     * IDs. The parameter <code>num</code> refers to the maximum number of user
     * IDs returned. It is possible that the size of the returned
     * <code>Set</code> is smaller than the parameter <code>num</code>.
     * 
     * @param orgName the DN of the organization.
     * @param attributes the keys in the <code>Map</code> contains the
     *                   attribute names and their corresponding values in
     *                   the <code>Map</code> is a <code>Set</code> that
     *                   contains the values for the attribute.
     * @param num the maximum number of returned user IDs; 0 means there
     *            is no limit.
     * @return a set of auto-generated user IDs.
     */
    public Set generateUserIDs(String orgName, Map attributes, int num);
    
}
