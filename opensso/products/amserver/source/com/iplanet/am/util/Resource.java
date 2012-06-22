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
 * $Id: Resource.java,v 1.3 2008/06/25 05:41:28 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/**
 * This utility is used to read any resource such as dtd file or xml file from a
 * jar file or from a file system we use java class delegation to the class
 * loader to achieve this
 * 
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.xml.Resource}
 */
public final class Resource {

    /**
     * Reads the resource from a class loader.
     * 
     * @param fileName
     *            resource to be read.
     * @param cl
     *            class which delegates the classloader functionality.
     * @return resource value.
     */
    public static String read(String fileName, Class cl) {
        String data = "";
        try {
            InputStream in = cl.getResourceAsStream(fileName);

            // may be absoulte file path is given
            if (in == null) {
                try {
                    // works well if the user has given absolute path
                    in = new FileInputStream(fileName);
                } catch (FileNotFoundException e) {
                    // works well if the user has given the relative path to the
                    // location of class file
                    String directoryURL = cl.getProtectionDomain()
                            .getCodeSource().getLocation().toString();
                    String fileURL = directoryURL + fileName;
                    URL url = new URL(fileURL);
                    in = url.openStream();
                }
            }// if
            data = Resource.read(new InputStreamReader(in));
            in.close();
        } catch (Exception e) {
        }// try/catch
        return data;
    }// read()

    /**
     * This method reads the resource from a reader
     */

    public static String read(Reader aReader) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader bReader = new BufferedReader(aReader);
            char[] data = new char[2048];
            int count = 0;
            while ((count = bReader.read(data)) != -1) {
                sb.append(data, 0, count);
            }// while loop
            bReader.close();
            aReader.close();
        } catch (IOException e) {
        }// try/catch
        return sb.toString();
    }// read()

    /**
     * This method reads the resource from the classloader which load this class
     */

    public static String read(String fileName) {
        return read(fileName, Resource.class);
    }// read()

    public static boolean save(String aFileName, String aContent) {
        try {
            FileWriter fOut = new FileWriter(aFileName);
            fOut.write(aContent);
            fOut.flush();
            fOut.close();
            return true;
        } catch (Exception e) {
            return false;
        }// try/catch
    }// save()

    public static void main(String[] args) {
        System.out.println(Resource.read(args[0]));
    }// main()
}// class Read
