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
 * $Id: LogFileFilter.java,v 1.3 2008/06/25 05:43:41 qcheng Exp $
 *
 */

package com.sun.identity.log.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.StringTokenizer;

/**
 *  Class that implements the FileNameFilter and is used by the VerifierList
 *  to get the list of keyfiles and associated logfiles based on a filter
 *  criteria.
 */

public class LogFileFilter implements FilenameFilter {
    
    private String filter = null;
    
    /**
     *  Constructor that takes the filter as input to create the File Filter
     *
     *  @param filter filter to create the File Filter.
     */
    public LogFileFilter(String filter) {
        this.filter = filter;
    }
    
    /**
     *  Implementation of the accept method that creates a filter
     *
     *  @param  dir the path to the location of the files.
     *  @param  name the name to be used.
     *  @return true if filter is created successfully.
     */
    
    public boolean accept(File dir, String name) {
        String filename = name;
        
        /* interpret null filter as satisfying all conditions */
        if ((filter == null) || (filter.length() == 0)) {
            return true;
        }
        
        /* tokenize the filter with the wildcard character */
        StringTokenizer st = new StringTokenizer(filter, "*");

        /*
         * check if first character is a wildcard character
         * if not then initial substring should match
         */
        if (filter.charAt(0) != '*') {
            int index = 0;
            if ((index = filename.indexOf(st.nextToken())) != 0) {
                return false;
            }
            filename = filename.substring(index);
        }
        
        while (st.hasMoreElements()) {
            String token = st.nextToken();
            int idx = 0;
            if ((idx = filename.indexOf(token)) == -1) {
                return false;
            }
            filename = filename.substring(idx + token.length());
        }
        
        if (!filter.endsWith("*") && (filename.length() != 0)) {
            return false;
        }
        return true;
    }
}
