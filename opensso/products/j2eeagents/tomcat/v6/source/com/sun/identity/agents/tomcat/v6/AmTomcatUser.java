/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: AmTomcatUser.java,v 1.2 2008/11/28 12:35:32 saueree Exp $
 */


package com.sun.identity.agents.tomcat.v6;

import java.security.Principal;


/**
 * The <code> AmTomcatUser</code> represents an authenticated Tomcat user and
 * stores the <code>AmRealmUser</code> internally.
 */
public class AmTomcatUser implements Principal {
    private String _user;

    /**
     * Constructor declaration
     *
     *
     * @param AmRealmUser
     *
     * @see com.sun.identity.agents.realm.AmRealmUser
     */
    public AmTomcatUser(String userName) {
        _user = userName;
    }

    /**
     * Method getName
     *
     *
     * @return name
     *
     */
    public String getName() {
        return _user;
    }

    /**
     * Method getName
     *
     *
     * @return name
     *
     */
    public boolean equals(Object another) {
        return _user.equalsIgnoreCase((String) another);
    }

    /**
     * Method hashCode
     *
     *
     * @return
     *
     */
    public int hashCode() {
        int result = _user.hashCode();

        return result;
    }

    /**
     * Method toString
     *
     *
     * @return User name
     *
     */
    public String toString() {
        return getName();
    }
}
