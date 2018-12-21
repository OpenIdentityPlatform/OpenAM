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
 * $Id: CaseInsensitiveKey.java,v 1.2 2008/06/25 05:42:25 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.common;

import java.io.Serializable;

/**
 * String wrapper that returns a case insensitive hash code useful for case
 * insensitive hashing in HashSet and HashMap.
 */
public class CaseInsensitiveKey implements Serializable  {

private static final long serialVersionUID = 1L;
	String mKey;

    public CaseInsensitiveKey(String key) {
        mKey = key;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CaseInsensitiveKey) {
            return mKey.equalsIgnoreCase(((CaseInsensitiveKey) o).toString());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mKey.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return mKey;
    }

}
