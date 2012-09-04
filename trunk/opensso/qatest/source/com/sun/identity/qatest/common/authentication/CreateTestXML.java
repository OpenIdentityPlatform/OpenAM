/* The contents of this file are subject to the terms
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
 * $Id: CreateTestXML.java,v 1.8 2009/06/02 17:10:59 cmwesley Exp $ 
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.common.authentication;

import com.sun.identity.qatest.common.TestCommon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * <code>CreateTestXML</code> is a helper class to create the XML file for the
 * form based validation for each of the forms.
 * This is xml used by the <code>WebTest</code> to verify the test.
 */
public class CreateTestXML extends TestCommon {

    private String testURL;
    private String testLogoutURL;
    private String baseDirectory;
    
    /**
     * Default constructor
     */
    public CreateTestXML() 
    throws Exception {
        super("authentication");
        testURL = getLoginURL("/");
        testLogoutURL = protocol + ":" + "//" + host + ":" + port +
                        uri + "/UI/Logout";

        baseDirectory = getTestBase();
    }
    
    /**
     * Creates the service based form Login XML
     * @param testMap - a Map object containing test related data.
     * @param testNegative - a boolean indicating if the current test is a
     * negative test.
     * @return the file name of the generated XML file.
     */
    public String createServiceXML(Map testMap, boolean testNegative)
    throws Exception {
        String fileName = "";
        String users = (String)testMap.get("users");
        String successMsg = (String)testMap.get("successMsg");
        String loginService = (String)testMap.get("servicename");
        String strIdentifier = (String)testMap.get("uniqueIdentifier");

        fileName = baseDirectory + strIdentifier + ".xml";
        PrintWriter out = new PrintWriter(new BufferedWriter
                (new FileWriter(fileName)));
        out.write("<url href=\"" + testURL + "?service=" +
                loginService);
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        StringTokenizer testUsers = new StringTokenizer(users,"|");
        List<String> testUserList = new ArrayList<String>();

        while (testUsers.hasMoreTokens()) {
            testUserList.add(testUsers.nextToken());
        }
        int totalforms = testUserList.size();
        int formcount = 0;
        for (String testUserName: testUserList) {
            formcount = formcount + 1;
            String tuser;
            String tpass;
            int uLength = testUserName.length();
            int uIndex = testUserName.indexOf(":");
            tuser= testUserName.substring(0, uIndex);
            tpass = testUserName.substring(uIndex + 1, uLength);
            if (testNegative) {
                 tpass = tpass + "fail";
            }
            out.write("<form name=\"Login\" buttonName=\"IDButton\">");
            out.write(System.getProperty("line.separator"));
            out.write("<input name=\"IDToken1\" value=\"" + tuser + "\"/>");
            out.write(System.getProperty("line.separator"));
            out.write("<input name=\"IDToken2\" value=\"" + tpass + "\"/>");
            out.write(System.getProperty("line.separator"));
            if (formcount == totalforms) {
                if (!testNegative) {
                    out.write("<result text=\"" + successMsg + "\"/>");
                } else {
                    out.write("<result text=\"" + successMsg + "\"/>");
                }
                out.write(System.getProperty("line.separator"));
                out.write("</form>");
                out.write(System.getProperty("line.separator"));
                out.write("</url>");
                out.write(System.getProperty("line.separator"));
            } else {
                out.write("</form>");
                out.write(System.getProperty("line.separator"));
            }
        }
        out.flush();
        out.close();
        
        return fileName;
    }

    /**
     * Creates the form login XML
     * @param testMap contains test related data
     * @return xml file name
     */
    public String createAuthXML(Map testMap)
    throws Exception {
        String fileName = "";
        String users = (String)testMap.get("users");
        String redirectURL = (String)testMap.get("redirectURL");
        String strIdentifier = (String)testMap.get("uniqueIdentifier");
        String successMsg = (String)testMap.get("successMsg");

        fileName = baseDirectory + strIdentifier + ".xml";

        PrintWriter out =
                new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        out.write("<url href=\"" + redirectURL);
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        String[] testUsers = users.split("\\|");
        int formCount = 0;
        for (String testUser: testUsers) {
            formCount++;
            int tokenIndex = testUser.indexOf(":");
            String userName = testUser.substring(0, tokenIndex);
            String password = testUser.substring(tokenIndex + 1,
                    testUser.length());
            out.write("<form name=\"Login\" IDButton=\"\">");
            out.write(System.getProperty("line.separator"));
            out.write("<input name=\"IDToken1\" value=\"" + userName + "\"/>");
            out.write(System.getProperty("line.separator"));
            out.write("<input name=\"IDToken2\" value=\"" + password + "\"/>");
            out.write(System.getProperty("line.separator"));
            if (formCount == testUsers.length) {
                out.write("<result text=\"" + successMsg + "\"/>");
                out.write(System.getProperty("line.separator"));
                out.write("</form>");
                out.write(System.getProperty("line.separator"));
                out.write("</url>");
                out.write(System.getProperty("line.separator"));
            } else {
                out.write("</form>");
                out.write(System.getProperty("line.separator"));
            }
        }

        out.flush();
        out.close();
        return fileName;
    }
    
    /**
     * Create the required XML files for the Account Lockout/warning tests
     * @param testMap - a Map object containing the properties to generate the
     * XML file
     * @param isWarning - a boolean indicating if the XML file should generate
     * an account lockout warning.  This should be set to true if a warning
     * should be generated.
     * @return a String containing the file name of the generated XML file.
     */
    public String createLockoutXML(Map testMap, boolean isWarning)
    throws Exception {
        String fileName = "";
        String userName = (String)testMap.get("Loginuser");
        String password = (String)testMap.get("Loginpassword");
        password = password + "tofail";
        String attempts = (String)testMap.get("Loginattempts");
        String Passmsg = (String)testMap.get("Passmsg");
        String loginurl = (String)testMap.get("loginurl");
        int ilockattempts = Integer.parseInt(attempts);
        if (!isWarning) {
            fileName = baseDirectory  + "accountlock.xml";
        } else {
            fileName = baseDirectory +  "accountwarning.xml";
        }
        PrintWriter out = new PrintWriter(new BufferedWriter
                (new FileWriter(fileName)));
        out.write("<url href=\"" + loginurl);
        out.write("\">");
        out.write(System.getProperty("line.separator"));
        int formcount = 0;
        for (int i=0; i < ilockattempts ; i ++) {
            formcount = formcount + 1;
            out.write("<form name=\"Login\" IDButton=\"\" >");
            out.write(System.getProperty("line.separator"));
            out.write("<input name=\"IDToken1\" value=\"" + userName + "\" />");
            out.write(System.getProperty("line.separator"));
            out.write("<input name=\"IDToken2\" value=\"" + password + "\" />");
            out.write(System.getProperty("line.separator"));
            if(formcount == ilockattempts){
                out.write("<result text=\"" + Passmsg + "\" />");
                out.write(System.getProperty("line.separator"));
            }
            out.write("</form>");
            out.write(System.getProperty("line.separator"));
            out.write(" <form anchorpattern=\"/UI/Login?\" />");
            out.write(System.getProperty("line.separator"));
        }
        out.write("</url>");
        out.write(System.getProperty("line.separator"));
        out.flush();
        out.close();
        
        return fileName;
    }
}
