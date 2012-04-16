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
 * $Id: getEncoding.java,v 1.2 2008/06/25 05:41:28 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import java.io.UnsupportedEncodingException;
import java.util.TreeMap;

public class getEncoding {

    static TreeMap java2Http;

    static {

        java2Http = new TreeMap();// Maps Http codeset into java codeset

        java2Http.put("5601", "euc-kr");
        java2Http.put("646", "iso-8859-1");

        java2Http.put("big5", "big5");

        java2Http.put("cns11643", "euc-tw");
        java2Http.put("cp1252", "windows-1252");

        java2Http.put("eucjp", "euc-jp");
        java2Http.put("gbk", "gbk");
        java2Http.put("johab", "Johab");

        java2Http.put("iso646-us", "iso-8859-1");
        java2Http.put("iso8859_1", "iso-8859-1");
        java2Http.put("iso8859-1", "iso-8859-1");
        java2Http.put("iso8859-2", "iso-8859-2");
        java2Http.put("iso8859-5", "iso-8859-5");
        java2Http.put("iso8859-6", "iso-8859-6");
        java2Http.put("iso8859-7", "iso-8859-7");
        java2Http.put("iso8859-8", "iso-8859-8");
        java2Http.put("iso8859-9", "iso-8859-9");
        java2Http.put("iso8859-11", "iso-8859-11");
        java2Http.put("iso8859-15", "iso-8859-15");

        java2Http.put("pck", "shift_jis");

        java2Http.put("sjis", "shift_jis");
        java2Http.put("tis620.2533", "tis-620");
        java2Http.put("utf8", "utf-8");

    }

    public static void main(String args[]) {

        String enc = System.getProperty("file.encoding");

        String test = "ABC";
        try {
            test.getBytes(enc);
        } catch (UnsupportedEncodingException ex) {
            enc = "iso8859_1";
        }
        if (args.length == 0 || (!args[0].equals("-http"))) {
            System.out.println("file encoding = " + enc);
            System.exit(0);
        }

        String httpEnc = (String) java2Http.get(enc.toLowerCase());
        if (httpEnc == null || httpEnc.length() == 0)
            httpEnc = enc;
        System.out.println("file encoding = " + httpEnc);
    }

}
