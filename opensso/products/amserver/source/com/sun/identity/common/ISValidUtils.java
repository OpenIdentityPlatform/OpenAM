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
 * $Id: ISValidUtils.java,v 1.2 2008/06/25 05:42:26 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.Iterator;

import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 * This class provides common validation routines.
 */
public class ISValidUtils {

    private static NamespaceContext nsc = new NamespaceContext() {
        public String getNamespaceURI(String pPrefix) {
            return "";
        }

        public String getPrefix(String pNamespaceURI) {
            return "";
        }

        public Iterator getPrefixes(String pNamespaceURI) {
            return null;
        }
    };

    /**
     * Returns true if <code>QName</code> is valid for a given local part.
     * 
     * @param localPart
     *            local part entry for object <code>QName</code>
     * @return true if <code>QName</code> is valid.
     */
    public static boolean isValidQName(String localPart) {
        QName parseqn = DatatypeConverter.parseQName(localPart, nsc);
        return parseqn != null;
    }
}
