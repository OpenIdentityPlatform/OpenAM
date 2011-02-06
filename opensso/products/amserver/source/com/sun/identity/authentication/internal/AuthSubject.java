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
 * $Id: AuthSubject.java,v 1.2 2008/06/25 05:41:53 qcheng Exp $
 *
 */

package com.sun.identity.authentication.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class AuthSubject implements Serializable {

    private Set principals;

    private boolean readOnly;

    public AuthSubject() {
        principals = new HashSet();
        readOnly = false;
    }

    public Set getPrincipals() {
        if (readOnly) {
            HashSet p = new HashSet();
            p.addAll(principals);
            return (p);
        }
        return (principals);
    }
}
