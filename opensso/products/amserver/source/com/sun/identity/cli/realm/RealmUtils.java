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
 * $Id: RealmUtils.java,v 1.2 2008/06/25 05:42:16 qcheng Exp $
 *
 */

package com.sun.identity.cli.realm;


/**
 * Realm related utilites.
 */
public class RealmUtils {
    private RealmUtils() {
    }

    /**
     * Returns parent realm from a given fully qualified realm.
     *
     * @param path Fully qualified realm.
     * @return parent realm.
     */
    public static String getParentRealm(String path) {
        String parent = "/";
        path = normalizeRealm(path);

        if ((path != null) && (path.length() > 0)) {
            int idx = path.lastIndexOf('/');
            if (idx > 0) {
                parent = path.substring(0, idx);
            }
        }

        return parent;
    }

    /**
     * Returns child realm from a given fully qualified realm.
     *
     * @param path Fully qualified realm.
     * @return child realm.
     */
    public static String getChildRealm(String path) {
        String child = "/";
        path = normalizeRealm(path);
        if ((path != null) && (path.length() > 0)) {
            int idx = path.lastIndexOf('/');
            if (idx != -1) {
                child = path.substring(idx+1);
            }
        }

        return child;
    }

    private static String normalizeRealm(String path) {
        if (path != null) {
            path = path.trim();
            if (path.length() > 0) {
                while (path.indexOf("//") != -1) {
                    path = path.replaceAll("//", "/");
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() -1);
                }
            }
        }
        return path.trim();
    }
}
