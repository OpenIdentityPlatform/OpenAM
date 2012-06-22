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
 * $Id: NodeNameFilter.java,v 1.2 2008/06/25 05:44:08 qcheng Exp $
 *
 */

package com.sun.identity.sm.flatfile;

import java.util.regex.Pattern;

/**
 * Simple class that looks for subentries with name matching the 
 * filter. Only wildcard '*' character is supported in the filter.
 */
public class NodeNameFilter {
    // Pattern to match
    Pattern pattern;

    /**
     * Creates an instance of <code>NodeNameFilter</code>.
     *
     * @param filter Filter pattern.
     */
    public NodeNameFilter(String filter) { 
        if ((filter != null) && (filter.length() != 0) &&
            !filter.equals("*")
        ) {
            // Replace "*" with ".*"
            int idx = filter.indexOf('*');
            while (idx != -1) {
                filter = filter.substring(0, idx) + ".*" +
                    filter.substring(idx + 1);
                idx = filter.indexOf('*', idx + 2);
            }
            pattern = Pattern.compile(filter.toLowerCase());
        }
    }

    /**
     * Returns <code>true</code> if <code>name</code> matches the filter.
     *
     * @param name Name of comparison.
     * @return <code>true</code> if <code>name</code> matches the filter.
     */
    public boolean accept(String name) {
        return (pattern == null) ? true :
            (pattern.matcher(name.toLowerCase()).matches());
    }
}
