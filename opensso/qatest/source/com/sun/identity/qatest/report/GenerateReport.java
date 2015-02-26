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
 * $Id: GenerateReport.java,v 1.1 2009/09/24 21:38:02 sridharev Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.report;

import com.sun.identity.qatest.common.TestCommon;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.Scanner;

/**
 * This class generates a new customized index.html in the
 * report directory with the all the links to the debugs, properties,
 * server config detials, copied the default testng generated report to
 * report.html 
 * 
 * This class is called from xml/opensso-common.xml.
 */
public class GenerateReport extends TestCommon {

    private String strReportDir;
    private String modName;
    private String moduleoutput;
    private String serverConfig;
    private String container;
    private String jdkversion;
    private String osVersion;
    private String fileseperator = System.getProperty("file.separator");

    /**
     *  Constructor for the Generating QATest Report
     * 
     */
    public GenerateReport(String reportDir, String moduleName)
            throws Exception {
        super("GenerateReport");
        try {
            strReportDir = reportDir;
            modName = moduleName;
            moduleoutput = modName + ".output";
            serverConfig = getServerInfoFile(reportDir);
            String strGetConfig = reportDir + fileseperator + serverConfig;
            scanOpenSSOConfig(strGetConfig);

            //Handle the report to include the debug and log files
            modifyReport(strReportDir);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void modifyReport(String strRepDir) {
        try {
            String toRead = strRepDir + fileseperator + "index.html";
            String copyOrg = strRepDir + fileseperator + "report.html";
            File fileOrg = new File(toRead);
            if (fileOrg.exists()) {
                fileOrg.renameTo(new File(copyOrg));
                FileWriter fstream = new FileWriter(new File(strRepDir,
                        "index.html"), true);
                BufferedWriter bout = new BufferedWriter(fstream);
                FileReader fReader = new FileReader(new File(copyOrg));
                BufferedReader buf = new BufferedReader(fReader);
                String strLine = null;
                while ((strLine = buf.readLine()) != null) {
                    if (strLine.contains("</body>")) {
                        int len = strLine.length();
                        int index = strLine.indexOf("</body>");
                        String s1 = strLine.substring(0, index);
                        String s2 = strLine.substring(index, len);
                        bout.write(s1);
                        bout.write(newline);
                        writeDebugInfo(bout);
                        bout.write(newline);
                        bout.write(s2);
                        bout.write(newline);
                    } else if (strLine.startsWith("<head>")) {
                        strLine = strLine.replace("<title>Test results",
                                "<title>OpenSSO Automated Test results");
                        bout.write(strLine);
                        bout.write(newline);
                    } else if (strLine.endsWith("</html")) {
                        int idx = strLine.indexOf("</html");
                        String temp = strLine.substring(0, idx);
                        bout.write(temp);
                        bout.write(newline);
                    } else if (strLine.startsWith("</head><body>")) {
                        int len = strLine.length();
                        int index = strLine.indexOf("<body>");
                        String s1 = strLine.substring(0, index);
                        String s2 = strLine.substring(index, len);
                        bout.write(strLine);
                        bout.write(newline);
                        bout.write("<h2>");
                        bout.write(newline);
                        bout.write(" <p align=\"center\"><img alt=\"\" "); 
                        bout.write("src=\"http://www.forgerock.com/images/" +
                                "boxes/half-top-box-openam.png\"");
                        bout.write(" style=\"width: 435px; height: 139px;\"><br>");
                        bout.write("</p><h2>");
                        bout.write(newline);
                        bout.write("<hr style=\"width: 100%; height: 2px;\"><h2>");
                        bout.write(newline);
                        bout.write("<p align=\"center\"><br></p>");
                        bout.write(newline);
                    } else {
                        bout.write(strLine);
                        bout.write(newline);
                    }
                }
                bout.flush();
                bout.close();
            } else {
                log(Level.FINE, "modifyReport", "qatest report " +
                        "index file does not exist in the report directory. Hence" +
                        "no report is modified.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void writeDebugInfo(BufferedWriter bw) {

        try {
            bw.write("<p>");
            bw.write(newline);
            bw.write("<h3><p align ='left'> Test Execution Details: : " +
                    "</p></h3>");
            bw.write(newline);
            bw.write("<table border='1' width='100%' class='main-page'>" + "<tr><th>Module</th><th>Server Configuration</th>" + "<th>Runtime Outputs</th> " + "<th>Properties and XML Files</th>" + "<th>TestBase Env</th></tr>");
            bw.write(newline);
            bw.write("<tr align='center' class='invocation-passed'>" + "<td><font size=\"4\">" + modName + "</font></td>" + "<td><font size=\"4\">" + "<div style=\"text-align: " + "justify;\">" + "<a href=./" + serverConfig + "> " + "OpenSSO Server Environment </a><br>" + "<div style=\"text-align: justify;\">" + "Container : " + container + "<br>" + "<div style=\"text-align: justify;\">" + " Operating System : " + osVersion + "<br>" + " Java Version : " + jdkversion + "<br>" + "</font></td>" + "<td><font size=\"4\">" + "<div style=\"text-align: justify;\">" + "<a href=./" + moduleoutput + ">QATEST Module Output " + "</a><br>" + "<div style=\"text-align: justify;\">" + "<a href=./logs>QATEST Client Logs </a><br>" + "<div style=\"text-align: justify;\">" + "<a href=./debug>OpenSSO Client Debug Logs</a><br>" + "<div style=\"text-align: justify;\">" + "<a href=./emailable-report.html> Emailable Report </a><br>" + "</font></td><td><font size=\"4\">" + "<div style=\"text-align: justify;\">" + "<a href=./properties> Properties </a><br>" + "<div style=\"text-align: justify;\">" + "<a href=./xml/xml/testng> TestNG XML </a><br>" + "<div style=\"text-align: justify;\">" + "<a href=./xml> TestCase Dynamic XMLs </a><br>" + "</td><td><font size=\"4\">" + "<div style=\"text-align: justify;\">" + "<a href=./test_env.txt>QATEST Client Environment</a>" + "</font></td>");
            bw.write(newline);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
    }

    private String getServerInfoFile(String dir) {
        String infoFile = "";
        try {
            File nFile = new File(dir + fileseperator);
            if (nFile.isDirectory()) {
                String[] dlist = nFile.list();
                for (int i = 0; i < dlist.length; i++) {
                    String repname = dlist[i];
                    if (repname.startsWith("http") && repname.endsWith(".html")) {
                        infoFile = repname;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return infoFile;
    }

    private void scanOpenSSOConfig(String strFile) {
        try {
            Scanner sc = new Scanner(new File(strFile));
            int count = 10;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.contains("Container")) {
                    String temp = "";
                    for (int i = 0; i < count; i++) {
                        temp = sc.nextLine();
                    }
                    container = temp.trim();
                } else if (line.contains("Operating System")) {
                    String temp = "";
                    for (int i = 0; i < count; i++) {
                        temp = sc.nextLine();
                    }
                    String temp1 = sc.nextLine().trim();
                    osVersion = temp.trim() + temp1;
                } else if (line.contains("Java Version")) {
                    String temp = "";
                    for (int i = 0; i < count; i++) {
                        temp = sc.nextLine();
                    }
                    jdkversion = temp.trim();
                }
            }
            sc.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String args[]) {
        try {
            GenerateReport gp = new GenerateReport(args[0], args[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
