/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: GenUtils.java,v 1.1 2008/11/22 02:19:58 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.utils;

import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;

public class GenUtils {
    
    public static ArrayList resolveStringArrayListDuplicates(
        ArrayList inList
    ){
        Stack stack = new Stack();
        ArrayList cleanedList = new ArrayList();
        for (int y = 0; y < inList.size(); y++) {
            String ppid = (String)inList.get(y);
            if (stack.search(ppid) < 0) {
                cleanedList.add(ppid);
                stack.push(ppid);
            }
        }
        return cleanedList;
    }
    
    public static ArrayList seperateStringComponents(String inputStr) {
        ArrayList ret = new ArrayList();
        StringTokenizer stok = new StringTokenizer(inputStr,",");
        while (stok.hasMoreTokens()) {
            String comp = stok.nextToken();
            ret.add(comp.trim());
        }
        return ret;
    }
    
    public static ArrayList seperateStringComponents(
        String inputStr,String tok
    ) {
        ArrayList ret = new ArrayList();
        StringTokenizer stok = new StringTokenizer(inputStr,tok);
        while (stok.hasMoreTokens()) {
            String comp = stok.nextToken();
            ret.add(comp.trim());
        }
        return ret;
    }
    
    public static String getStringDelimited(
        String inputStr,
        String lhDelim,
        String rhDelim
    ) {
        int lhindx = inputStr.indexOf(lhDelim);
        if (lhindx == -1) {
            return "";
        }
        int rhindx = inputStr.indexOf(rhDelim);
        if (rhindx == -1) {
            return "";
        }
        int lhStart = lhindx+lhDelim.length();
        if (lhStart > rhindx) {
            return "";
        }
        if (rhindx > inputStr.length()) {
            return "";
        }
        String retString = inputStr.substring(lhStart,rhindx);
        return retString;
    }
    
    public static String getStringDelimited(
        String inputStr,
        String lhDelim,
        String rhDelim,
        int fromIndx
    ) {
        String netStr = inputStr.substring(fromIndx);
        return getStringDelimited(inputStr,lhDelim,rhDelim);
    }
}
