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
 * $Id: CreateAGZip.java,v 1.2 2008/08/20 21:39:26 qcheng Exp $
 *
 */

package com.sun.identity.fedlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *  * Creates Assertion Generator.
 *   */
public class CreateAGZip {
    private Map fedConfigTagSwap;
    private List fedConfigTagSwapOrder;
    private Map fedletBits;
    private Map jarExtracts;

    /**
     * Default constructor
     */
    public CreateAGZip() {
        fedConfigTagSwap = new HashMap();
        fedConfigTagSwap.put("@CONFIGURATION_PROVIDER_CLASS@",
            "com.sun.identity.plugin.configuration.impl.FedletConfigurationImpl");
        fedConfigTagSwap.put("@DATASTORE_PROVIDER_CLASS@",
            "com.sun.identity.plugin.datastore.impl.FedletDataStoreProvider");
        fedConfigTagSwap.put("@LOG_PROVIDER_CLASS@",
            "com.sun.identity.plugin.log.impl.FedletLogger");
        fedConfigTagSwap.put("@SESSION_PROVIDER_CLASS@",
            "com.sun.identity.plugin.session.impl.FedletSessionProvider");
        fedConfigTagSwap.put("@XML_SIGNATURE_PROVIDER@",
            "com.sun.identity.saml.xmlsig.AMSignatureProvider");
        fedConfigTagSwap.put("@XMLSIG_KEY_PROVIDER@",
            "com.sun.identity.saml.xmlsig.JKSKeyProvider");
        fedConfigTagSwap.put("%BASE_DIR%%SERVER_URI%", "@FEDLET_HOME@");
        fedConfigTagSwap.put("%BASE_DIR%", "@FEDLET_HOME@");
        fedConfigTagSwap.put("com.sun.identity.common.serverMode=true",
            "com.sun.identity.common.serverMode=false");
        fedConfigTagSwap.put("@SERVER_PROTO@", "http");
        fedConfigTagSwap.put("@SERVER_HOST@", "example.identity.sun.com");
        fedConfigTagSwap.put("@SERVER_PORT@", "80");
        fedConfigTagSwap.put("/@SERVER_URI@", "/fedlet");

        fedConfigTagSwapOrder = new ArrayList();
        fedConfigTagSwapOrder.add("@CONFIGURATION_PROVIDER_CLASS@");
        fedConfigTagSwapOrder.add("@DATASTORE_PROVIDER_CLASS@");
        fedConfigTagSwapOrder.add("@LOG_PROVIDER_CLASS@");
        fedConfigTagSwapOrder.add("@SESSION_PROVIDER_CLASS@");
        fedConfigTagSwapOrder.add("@XML_SIGNATURE_PROVIDER@");
        fedConfigTagSwapOrder.add("@XMLSIG_KEY_PROVIDER@");
        fedConfigTagSwapOrder.add("%BASE_DIR%%SERVER_URI%");
        fedConfigTagSwapOrder.add("%BASE_DIR%");
        fedConfigTagSwapOrder.add("com.sun.identity.common.serverMode=true");
        fedConfigTagSwapOrder.add("@SERVER_PROTO@");
        fedConfigTagSwapOrder.add("@SERVER_HOST@");
        fedConfigTagSwapOrder.add("@SERVER_PORT@");
        fedConfigTagSwapOrder.add("/@SERVER_URI@");
    }

    public void createAGZip(String bitsFile, String jarExtractFile,
        String warDir, String fedletZipFile, String readmeFile) 
        throws IOException, FileNotFoundException {
       
       
        initMaps(bitsFile); 

        File tempFile = new File(fedletZipFile);
        String workDir = tempFile.getParentFile().toString() + 
            File.separator + ".fedletzip";
        File dir = new File(workDir);
        if (!dir.getParentFile().exists())  {
            dir.getParentFile().mkdirs();
        }
        dir.mkdir();
        String fedletWarDir = workDir + File.separator + "war";
        dir = new File(fedletWarDir);
        dir.mkdir();

        // create fedlet.war
        copyBits(warDir, readmeFile, fedletWarDir);
        
        createWar(workDir);

	// copy README file        
        copyFile(readmeFile, workDir + "/README");
        
        // create Fedlet ZIP
        createZip(workDir, fedletZipFile);
    }
    
    public void initMaps(String bitsFile) 
    throws FileNotFoundException, IOException {
        fedletBits = new HashMap();
        Properties prop = new Properties();
        FileInputStream fis = new FileInputStream(new File(bitsFile));
        prop.load(fis);
        for (Enumeration e = prop.propertyNames(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            fedletBits.put(k, prop.getProperty(k));
        }

    }
    
    private void copyBits(String warDir, String readmeFile, String workDir)
        throws IOException {

        for (Iterator i = fedletBits.keySet().iterator(); i.hasNext(); ) {
            String file = (String)i.next();
            String target = (String)fedletBits.get(file);
            if ((target == null) || (target.trim().length() == 0)) {
                target = file;
            }
            copyFile(warDir + File.separator + file, 
                workDir + File.separator + target);
        }
	
    }

    private void extractJars(String warDir, String workDir)
        throws IOException {
        for (Iterator i = jarExtracts.keySet().iterator(); i.hasNext(); ) {
            JarInputStream jis = null;
            JarOutputStream zipOutputStream = null;

            try {
                String fileName = (String) i.next();
                Set pkgNames = (Set) jarExtracts.get(fileName);
                InputStream is = new FileInputStream(
                    new File(warDir + fileName));
                jis = new JarInputStream(is);
                File outFile = new File(workDir + fileName);
                if (!outFile.getParentFile().exists()) {
                    outFile.getParentFile().mkdirs();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(
                    outFile);
                zipOutputStream = new JarOutputStream(fileOutputStream);
                JarEntry entry = jis.getNextJarEntry();

                while (entry != null) {
                    int size = (new Long(entry.getSize())).intValue();
                    if (size > 0) {
                        String name = entry.getName();
                        boolean bExtract = false;
                        for (Iterator j = pkgNames.iterator();
                            (j.hasNext() && !bExtract);
                        ) {
                            bExtract = name.startsWith((String)j.next());
                        }
                        if (bExtract) {
                            zipOutputStream.putNextEntry(entry);
                            byte[] b = new byte[size];
                            int tobeRead = size;
                            while (true) {
                                int readSize = 
                                    jis.read(b, size - tobeRead, tobeRead);
                                if (readSize == tobeRead) {
                                    break;
                                } else {
                                    tobeRead -= readSize;
                                }
                            }
                            zipOutputStream.write(b);
                        }
                    }
                    entry = jis.getNextJarEntry();
                }
            } finally {
                try {
                    if (jis != null) {
                        jis.close();
                    }
                    if (zipOutputStream != null) {
                        zipOutputStream.close();
                    }
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
    }

    private void copyFile(String source, String dest) 
        throws IOException {
        File test = new File(dest);
        File parent = test.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        FileOutputStream fos = null;
        InputStream src = null;
        try {
            src = new FileInputStream(new File(source));
            if (src != null) {
                fos = new FileOutputStream(dest);
                int length = 0;
                byte[] bytes = new byte[1024];
                while ((length = src.read(bytes)) != -1) {
                    fos.write(bytes, 0, length);
                }
            } else {
                Object[] param = {source};
                throw new IOException("File " + source + " not found.");
            }
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (src != null) {
                    src.close();
                }
            } catch (IOException ex) {
                //ignore
            }
        }
    }

    private void createCOTProperties(String confDir) throws IOException {
        
        String content =
            "cot-name=fedletcot\n" +
            "sun-fm-cot-status=Active\n" +
            "sun-fm-trusted-providers=IDP_ENTITY_ID, FEDLET_ENTITY_ID\n" +
            "sun-fm-saml2-readerservice-url=\n" + 
            "sun-fm-saml2-writerservice-url=\n";
        writeToFile(confDir + File.separator + "fedlet.cot-template", content);
    }
    
    private void validateParameters(String bitsFile, String jarExtractFile, 
        String warDir, String fedletZipFile, String readmeFile)
        throws IOException {
        if (!isFileExists(bitsFile)) {
            throw new IOException("File " + bitsFile + " not found.");
        }
        if (!isFileExists(jarExtractFile)) {
            throw new IOException("File " + jarExtractFile + " not found.");
        }
        if (!isDirExists(warDir)) {
            throw new IOException("Directory " + warDir + " not found.");
        }
        if (isFileExists(fedletZipFile)) {
            throw new IOException("Fedlet file "  + fedletZipFile + 
                " already exists");
        }
        if (!isFileExists(readmeFile)) {
            throw new IOException("File "  + readmeFile + " not found.");
        }
    }
    
    private boolean isFileExists(String name) throws IOException {
        return isExists(name, true);
    }
    
    private boolean isDirExists(String name) throws IOException {
        return isExists(name, false);
    }
    
    private boolean isExists(String name, boolean needFile) throws IOException {
        File file = new File(name);
        if (file.exists()) {
            if (needFile && file.isDirectory()) {
                throw new IOException(name + " is a directory, not a file");
            } else if (!needFile && file.isFile()) {
                throw new IOException(name + " is a file, not a directory");
            } else {
               return true;
            }
        } else {
            return false;
        }
    }
    
    

    private void createFederationConfigProperties(
        String warDir, String workDir) throws IOException {

        File configFile = new File(warDir + File.separator +
            "WEB-INF" + File.separator + "fedlet" + File.separator +
            "FederationConfig.properties");
        FileInputStream fis = new FileInputStream(configFile);
        StringBuffer sb = new StringBuffer(10000);
        byte[] content = new byte[1000];
        int lenRead;
        while (true) {
            lenRead = fis.read(content);
            if (lenRead == -1) {
                break;
            }
            sb.append(new String(content, 0, lenRead));
        }
        String prop = sb.toString();
        for (Iterator i = fedConfigTagSwapOrder.iterator(); i.hasNext();) {
            String k = (String) i.next();
            String v = (String) fedConfigTagSwap.get(k);
            prop = prop.replaceAll(k, v);
        }
        writeToFile(workDir + File.separator + "FederationConfig.properties", 
            prop);
    }

    private static void writeToFile(String fileName, String content)
        throws IOException {
        FileWriter fout = null;
        try {
            fout = new FileWriter(fileName);
            fout.write(content);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception ex) {
                    //No handling requried
                }
            }
        }
    }

    private void createWar(String workDir)
        throws IOException {
        JarOutputStream out = null;
        String warDir = workDir + "/war";
        int lenWorkDir = warDir.length() +1;
        List files = getAllFiles(warDir, true);
        String jarName = workDir + "/AG.war";

        try {
            out = new JarOutputStream(new FileOutputStream(jarName));
            byte[] buf = new byte[1024];

            for (Iterator i = files.iterator(); i.hasNext();) {
                String fname = (String) i.next();
                FileInputStream in = new FileInputStream(fname);
                out.putNextEntry(new JarEntry(fname.substring(lenWorkDir)));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }

            deleteAllFiles(warDir, files);
            (new File(warDir)).delete();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
            // ignore
            }
        }
    }

    private String createZip(String workDir, String zipName)
        throws IOException {
        int lenWorkDir = workDir.length() +1;

        ZipOutputStream out = null;
        try {
            List files = getAllFiles(workDir, true);
            out = new ZipOutputStream(new FileOutputStream(zipName));
            byte[] buf = new byte[1024];

            for (Iterator i = files.iterator(); i.hasNext(); )  {
                String fname = (String) i.next();
                FileInputStream in = new FileInputStream(fname);
                out.putNextEntry(new ZipEntry(fname.substring(lenWorkDir)));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                // Complete the entry
                out.closeEntry();
                in.close();
            }

            deleteAllFiles(workDir, files);
            (new File(workDir)).delete();
            return zipName;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    private void deleteAllFiles(String workDir, List files) {
        for (Iterator i = files.iterator(); i.hasNext(); ) {
            String fname = (String)i.next();
            (new File(fname)).delete();
        }

        List dirs = getAllFiles(workDir, false);
        for (int i = dirs.size() -1; i >= 0; --i) {
            String dirName = (String)dirs.get(i);
            File test = new File(dirName);
            if (test.isDirectory()) {
                test.delete();
            }
        }
    }

    private List getAllFiles(String dir, boolean bFileOnly) {
        List list = new ArrayList();
        File directory = new File(dir);
        String[] children = directory.list();

        for (int i = 0; i < children.length; i++) {
            String child = dir + "/" + children[i];
            File f = new File(child);
            if (f.isDirectory()) {
                if (!bFileOnly) {
                    list.add(f.getAbsolutePath());
                }
                list.addAll(getAllFiles(f.getAbsolutePath(), bFileOnly));
            } else {
                list.add(f.getAbsolutePath());
            }
        }

        return list;
    }
    
    /**
     * Main program to create Assertion Generator ZIP
     * The program takes following parameters in order:
     * 1. a file containing the list of filenames to be extract from opensso.war
     * 2. a file containing the list of jars to be extracted
     * 3. file point to the opensso.war
     * 4. name with full path to write Assertion Generator ZIP
     * 5. README file
     */
    public static void main(String[] args) {
        if ((args == null) || (args.length != 5)) {
            System.err.println("Invalid parameters.");
            System.err.println("Expected parameters (in this order) : " +
                "<bits_filename> <jar_extract_filename> <war_directory> " +
                "<assertion_generation_zip_filename> <readme_filename>");
        }
        try {
            CreateAGZip instance = new CreateAGZip();
            instance.createAGZip(args[0], args[1], args[2], args[3],
                args[4]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
