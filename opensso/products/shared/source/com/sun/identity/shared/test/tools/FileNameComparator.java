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
 * $Id: FileNameComparator.java,v 1.2 2008/06/25 05:53:06 qcheng Exp $
 *
 */

package com.sun.identity.shared.test.tools;

import java.io.File;
import java.util.Comparator;

/**
 * Comparator to order test case.
 */
public class FileNameComparator implements Comparator<File> {
    /**
     * Returns 0 if <code>f1.getPath()</code> equals <code>f2.getPath()</code>; 
     * 1 if <code>f1.getPath()</code> is greater than <code>f2.getPath()</code>;
     * and -1 if <code>f1.getPath()</code> is smaller than 
     * <code>f2.getPath()</code>;
     * 
     * @param f1 File #1.
     * @param f2 File #2.
     * @return the comparison of <code>f1</code> and <code>f2</code> based on
     *         path name.
     */
    public int compare(File f1, File f2) {
        return f1.getPath().compareTo(f2.getPath());
    }
}
