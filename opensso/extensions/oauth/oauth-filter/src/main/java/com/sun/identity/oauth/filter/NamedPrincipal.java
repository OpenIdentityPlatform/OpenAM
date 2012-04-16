/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at:
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 *
 * See the License for the specific language governing permission and
 * limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at opensso/legal/CDDLv1.0.txt. If applicable,
 * add the following below the CDDL Header, with the fields enclosed by
 * brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * $Id: NamedPrincipal.java,v 1.1 2009/05/26 22:17:45 pbryan Exp $
 */

package com.sun.identity.oauth.filter;

import java.security.Principal;

/**
 * @author Paul C. Bryan <pbryan@sun.com>
 */
class NamedPrincipal implements Principal {

    private String name;

    public NamedPrincipal(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object another) {
        return (another != null && another instanceof NamedPrincipal
        && ((NamedPrincipal)another).name.equals(name));
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String getName() {
        return name;
    }
}
