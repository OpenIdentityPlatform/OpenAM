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
 * $Id: IOTest.java,v 1.1 2009/08/19 05:41:03 veiming Exp $
 */

package com.sun.identity.test;

import com.sun.identity.unittest.Util;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class IOTest {
    private static final String RESOURCE_NAME = "eUnitTest.data";
    private static final String FILENAME =
        System.getProperty("java.io.tmpdir") + "/" + "eUnitTest.txt";
    private static final String JSON_FILENAME =
        System.getProperty("java.io.tmpdir") + "/" + "eJSONUnitTest.txt";
    private static final String JSON_OBJECT = "jobject";

    private static final String LINE_1 = "Line1";
    private static final String LINE_2 = "Line2";
    private final MockObject mock1 = new MockObject("one");
    private final MockObject mock2 = new MockObject("two");

    @BeforeClass
    public void setup() throws IOException, JSONException {
        createFile();
        createJSONFile();
    }

    private void createFile() 
        throws IOException {
        File f = new File(FILENAME);
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(f)));
        out.print(LINE_1 + "\n" + LINE_2);
        out.flush();
        out.close();
    }

    private void createJSONFile()
        throws IOException, JSONException {
        File f = new File(JSON_FILENAME);
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(f)));
        JSONObject json1 = new JSONObject(mock1);
        JSONObject store = new JSONObject();
        store.putOpt(JSON_OBJECT, json1);
        out.print(store.toString() + "\n");

        JSONObject json2 = new JSONObject(mock2);
        store = new JSONObject();
        store.putOpt(JSON_OBJECT, json2);
        out.print(store.toString());
        out.flush();
        out.close();
    }


    @AfterClass
    public void cleanup() {
        File f = new File(FILENAME);
        f.delete();
        f = new File(JSON_FILENAME);
        f.delete();

    }

    @Test
    public void readFile() throws Exception {
        List<String> content = Util.getFileContent(FILENAME);
        if (!content.get(0).equals(LINE_1)) {
            throw new Exception("IOTest.readFile failed. Line 1 is incorrect");
        }
        if (!content.get(1).equals(LINE_2)) {
            throw new Exception("IOTest.readFile failed. Line 2 is incorrect");
        }
    }

    @Test
    public void readJSONFile() throws Exception {
        List<String> content = Util.getFileContent(JSON_FILENAME);
        List<JSONObject> objects = Util.toJSONObject(content, JSON_OBJECT);
        JSONObject json1 = objects.get(0);
        if (!json1.optString("str").equals(mock1.getStr())) {
            throw new Exception(
                "IOTest.readJSONFile failed. object 1 is incorrect");
        }
        JSONObject json2 = objects.get(1);
        if (!json2.optString("str").equals(mock2.getStr())) {
            throw new Exception(
                "IOTest.readJSONFile failed. object 2 is incorrect");
        }
    }

    @Test
    public void readResource() throws Exception {
        List<String> content = Util.getWebResource(RESOURCE_NAME);
        if (!content.get(0).equals(LINE_1)) {
            throw new Exception("IOTest.readFile failed. Line 1 is incorrect");
        }
        if (!content.get(1).equals(LINE_2)) {
            throw new Exception("IOTest.readFile failed. Line 2 is incorrect");
        }
    }

    public class MockObject {
        private String str;

        public MockObject(String s) {
            str = s;
        }

        public void setStr(String s) {
            str = s;
        }

        public String getStr() {
            return str;
        }

    }
}
