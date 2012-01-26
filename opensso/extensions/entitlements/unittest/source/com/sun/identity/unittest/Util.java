/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Util.java,v 1.1 2009/02/27 16:11:36 veiming Exp $
 */

package com.sun.identity.unittest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 */
public class Util {
    private static Util instance = new Util();
    private Util() {
    }

    public static List<String> getFileContent(String fileName)
        throws IOException {
        File aFile = new File(fileName);
        List<String> list = new ArrayList<String>();
        BufferedReader input = new BufferedReader(new FileReader(aFile));
        try {
            String line = input.readLine();
            while (line != null) {
                list.add(line);
                line = input.readLine();
            }
        } finally {
            input.close();
        }
        return list;
    }

    public static List<String> getWebResource(String name)
        throws IOException {
        List<String> list = new ArrayList<String>();
        InputStream in = instance.getClass().getClassLoader()
            .getResourceAsStream(name);
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        try {
            String line = input.readLine();
            while (line != null) {
                list.add(line);
                line = input.readLine();
            }
        } finally {
            input.close();
        }
        return list;
    }

    public static List<JSONObject> toJSONObject(List<String> list, String objName)
        throws JSONException {
        List<JSONObject> objects = new ArrayList<JSONObject>();
        for (String s : list) {
            JSONObject json = new JSONObject(s);
            objects.add(json.optJSONObject(objName));
        }
        return objects;
    }
}
