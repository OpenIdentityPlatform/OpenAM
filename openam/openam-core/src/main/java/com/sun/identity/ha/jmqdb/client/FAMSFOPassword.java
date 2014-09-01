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
 * $Id: FAMSFOPassword.java,v 1.2 2008/06/25 05:43:28 qcheng Exp $
 *
 */


package com.sun.identity.ha.jmqdb.client;

import java.io.*;
import java.util.*;

@Deprecated
public class FAMSFOPassword {

    private static final String RESOURCE_BUNDLE = "amSessionDB";
    private static final String HELP = "--help";
    private static final String S_HELP = "-h";
    private static final String ENCRYPT = "--encrypt";
    private static final String S_ENCRYPT = "-e";
    private static final String PASSWORDFILE = "--passwordfile";
    private static final String S_PASSWORDFILE = "-f";
    private static ResourceBundle bundle = null;

    static {
        try {            
            bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE,
                                              Locale.getDefault());
        }
        catch (MissingResourceException mre) {
            System.err.println("Cannot get the resource bundle.");
            System.exit(1);
	}
    }
    
    FAMSFOPassword() {
    }

    private void saveEncPasswordToFile(String passwordfile,
                                       String encPassword) {
        
        try {
            FileOutputStream fos = new FileOutputStream(passwordfile);
            BufferedWriter writer = 
                new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
            PrintWriter statsFile = new PrintWriter(writer);
            statsFile.println(encPassword);
            statsFile.flush();
            statsFile.close();
            setFilePermission(passwordfile, "600");
        } catch (Exception e) {
            System.err.println(bundle.getString("error-saving-passwd"));
            System.exit(1);
        }
    }

    private void setFilePermission(String filename, String perm) 
        throws IOException {        
        System.out.println("os.name="+
                           System.getProperty("os.name"));
        // Do "chmod" only if it is on UNIX/Linux platform
        if (System.getProperty("path.separator").equals(":")) {        
            Runtime.getRuntime().exec("chmod "+perm+" "+filename);
        }
    }
    
    private void process(String[] args) throws Exception {

        // Check the initial arguments
        if ((args.length == 0) || args[0].equals(HELP) ||
            (args[0].equals(S_HELP))) {            
            System.err.println(bundle.getString("amsfopasswd-usage"));
            System.exit(1);
        } else if (!args[0].equals(HELP) && 
                   !args[0].equals(S_HELP) &&
                   !args[0].equals(ENCRYPT) && 
                   !args[0].equals(S_ENCRYPT) &&
                   !args[0].equals(PASSWORDFILE) && 
                   !args[0].equals(S_PASSWORDFILE)) {
            // Invalid subcommand
            System.err.println(bundle.getString("invalid-option"));
            System.err.println(bundle.getString("amsfopasswd-usage"));
            System.exit(1);
        } else if (args.length != 4) {
            // Illegal number of arguments
            System.err.println(bundle.getString("illegal-args"));
            System.err.println(bundle.getString("amsfopasswd-usage"));
            System.exit(1);
        }

        // Encrypt the password nad save it to the file
        String filename = null;
        String cleartext = null;
        
        if (args[0].equals(S_ENCRYPT) || 
            args[0].equals(ENCRYPT)) {
            cleartext = args[1];
            if (args[2].equals(PASSWORDFILE) ||
                args[2].equals(S_PASSWORDFILE)) {
                filename = args[3];
            } else {
                System.err.println(bundle.getString("amsfopasswd-usage"));
                System.exit(1);                    
            }
        }
        if (args[0].equals(PASSWORDFILE) ||
            args[0].equals(S_PASSWORDFILE)) {
                filename = args[1];
                if (args[2].equals(ENCRYPT) ||
                    args[2].equals(S_ENCRYPT)) {
                    cleartext = args[3];
                } else {
                    System.err.println(bundle.getString("amsfopasswd-usage"));
                    System.exit(1);                    
                }
        }
        
        String encPassword = 
            CryptUtil.encrypt(CryptUtil.DEFAULT_PBE_PWD,
                                  cleartext);
        saveEncPasswordToFile(filename, encPassword);
    }
    
    static public ResourceBundle getResourceBundle() {
        
        return bundle;
    }
    
    static String readEncPasswordFromFile(String passwordfile) 
        throws Exception {

        String line =null;
        BufferedReader in =null;
        try{
            in = new BufferedReader(new FileReader(passwordfile));
            if(in.ready())  {
                line = in.readLine();
            } 
            return line;
        } catch(IOException e){
            System.out.println("Could not open file " + e.getMessage());
        } finally {
            if (in !=null ) {
                try {
                    in.close();
                }
                catch (Exception e) {
                    System.out.println("Unable to close the file: "+
                                       e.getMessage());
                }
            }
        }
        return null;
    }

    static public void main(String args[]) {
    
        FAMSFOPassword pwdGen = new FAMSFOPassword();        
        ResourceBundle rb = FAMSFOPassword.getResourceBundle();
                
        try {
            pwdGen.process(args);
            System.out.println(rb.getString("successful"));
            System.exit(0);
        } catch (Exception eex) {
            System.err.println(bundle.getString("fail") + " " +
                               eex.getLocalizedMessage());
            System.exit(1);
        }
    }
    
    
}
