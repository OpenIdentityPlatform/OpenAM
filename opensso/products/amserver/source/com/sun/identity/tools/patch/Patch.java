/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
* $Id: Patch.java,v 1.4 2009/03/10 23:54:14 veiming Exp $
*/

package com.sun.identity.tools.patch;

import com.sun.identity.tools.manifest.Manifest;
import com.sun.identity.tools.bundles.CopyUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.Properties;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The <code>Patch</code> class provides methods to parse the
 * commandline arguments, execute them accordingly for the 
 * command line tool - ssopatch.
 * This class generates manifest files for comparison
 * between different version of a given war file. 
 */

public class Patch implements PatchGeneratorConstants{
    private String srcFilePath;
    private String destFilePath;
    private String src2FilePath;    
    private String stagingFilePath;
    
    private Manifest firstMan;
    private Manifest origMan;
    private Manifest secondMan;
    private boolean createMode = false;    
    private boolean compareMode = false;    
    private boolean mergeMode = false;
    private boolean overwriteMode = false;
    private boolean overrideMode = false;
     
    private File stagingArea;  
    private Properties srcManifest;
    private Properties src2Manifest;    
    private char wildCard;
    private static Locale locale;
    private static ResourceBundle rbMessages;

    private static Locale getLocale(String strLocale) {
        StringTokenizer st = new StringTokenizer(strLocale, "_");
        String lang = (st.hasMoreTokens()) ? st.nextToken() : "";
        String country = (st.hasMoreTokens()) ? st.nextToken() : "";
        String variant = (st.hasMoreTokens()) ? st.nextToken() : "";
        return new Locale(lang, country, variant);
    }

    private void getProperties() {
        String propFilePath = System.getProperty(PROPERTIES_FILE);
        Properties sysProp = new Properties();
        if (propFilePath != null) {
            File propFile = new File(propFilePath);
            if (propFile.exists() && propFile.isFile()) {
                FileInputStream fin = null;
                try {
                    fin = new FileInputStream(propFile);
                    sysProp.load(fin);
                } catch (IOException ex){
                    System.out.println("Error occured when reading file "
                        + propFilePath);
                    System.exit(1);
                } finally {
                    try {
                        if (fin != null) {
                            fin.close();
                        }
                    } catch (IOException ex) {
                        // fin wasn't open
                    }
                }
            }
        }
        
        srcFilePath = (String)options.get(OPTION_SRC_FILE_PATH);
        destFilePath = (String)options.get(OPTION_DEST_FILE_PATH);    
        src2FilePath = (String)options.get(OPTION_SRC2_FILE_PATH);
        stagingFilePath = (String)options.get(OPTION_STAGING_FILE_PATH);
        wildCard = System.getProperty(WILDCARD_CHAR,
            sysProp.getProperty(WILDCARD_CHAR,DEFAULT_WILDCARD_CHAR)).charAt(0);
   
        if (srcFilePath == null) {
            System.out.println(rbMessages.getString("exception-no-src"));   
            printUsage();
            System.exit(1);            
        } 
        if ((destFilePath != null) && (src2FilePath != null)) {
            System.out.println(rbMessages.getString("exception-bad-args"));   
            printUsage();
            System.exit(1);   
        }  
        
        // if manifest file name passed in we are in create mode        
        if (destFilePath != null) {
            createMode = true;
        }  
        // if second source file name passed in we are in compare mode        
        if (src2FilePath != null) {
            compareMode = true;
        } else {
            src2FilePath = srcFilePath;
        }          
        if (stagingFilePath != null) {
            mergeMode = true;
        }


        overwriteMode = isOptionSet(OPTION_OVERWRITE);
        overrideMode = isOptionSet(OPTION_OVERRIDE);

        firstMan = new Manifest();        
        firstMan.setDefaultProperties();
        origMan = new Manifest();        
        origMan.setDefaultProperties();                    
        secondMan = new Manifest();        
        secondMan.setDefaultProperties();                              
    }
 
    
    private void compareManifest(Manifest man1, Manifest man2, Manifest orig, 
            String stagingPath ) {
        int diffCount = 0;  
        int customCount = 0;

        try {
            String idSrc = man1.getProperty("identifier");
            String idSrc2 = man2.getProperty("identifier");
            
            if (stagingPath != null) {
                stagingArea = new File(stagingFilePath);
                if (stagingArea.exists() && (overwriteMode == false)) {
                    System.out.println(
                            rbMessages.getString("exception-path-exists"));
                    System.exit(1);
                } else {
                    stagingArea.mkdirs();
                }

            }
            if ((idSrc != null) && (idSrc2 != null)) {
                System.out.print(rbMessages.getString("patch-compare"));
                if (man1.srcFile == null) {
                    System.out.print("Internal");
                } else {
                    System.out.print(man1.srcFile.getPath());
                }
                System.out.print(" (");
                System.out.print(idSrc);
                System.out.print(") ");
                System.out.print(rbMessages.getString("patch-against"));
                if (man2.srcFile == null) {
                    System.out.print("Internal");
                } else {
                    System.out.print(man2.srcFile.getPath());
                }
                System.out.print(" (");
                System.out.print(idSrc2);
                System.out.print(")\n");
            }            
                        
            for (Enumeration keysEnum = man2.getPropertyNames();
                    keysEnum.hasMoreElements();) {
                String key = (String) keysEnum.nextElement();
                String hashValue = man2.getProperty(key);
                String srcValue = man1.getProperty(key);
                                
                // preload with the value from the first manifest
                String origValue = man1.getProperty(key);
                if (orig != null) {
                    origValue = orig.getProperty(key);
                }
                if ((key.equals("identifier")) || 
                        (key.equals("META-INF/MANIFEST.MF"))) {
                    man1.removeProperty(key);
                    continue;
                }
                if (key.equals("META-INF/OpenSSO.manifest")) {
                    if (mergeMode) {
                        File destFile = new File(stagingArea, key);
                        JarFile src2War = new JarFile(man2.srcFile);
                        InputStream fileIn = src2War.getInputStream(
                                src2War.getEntry(key));

                        CopyUtils.copyFileFromJar(fileIn, destFile,
                                overwriteMode); 
                    }
                    man1.removeProperty(key);                    
                    continue;
                }
                
                if ((srcValue == null) && (origValue == null)) {
                    // new file in the new war file
                                     
                    if (mergeMode) {
                        File destFile = new File(stagingArea, key);
                        JarFile src2War = new JarFile(man2.srcFile);
                        InputStream fileIn = src2War.getInputStream(
                                src2War.getEntry(key));

                        CopyUtils.copyFileFromJar(fileIn, destFile, 
                                overwriteMode);                    
                    } else {
                        diffCount++;                        
                        System.out.print(rbMessages.getString("patch-new"));
                        System.out.print("(");                                                              
                        System.out.print(key);
                        System.out.print(")\n");                         
                    }
                } else if ((srcValue == null) && (origValue != null)) {
                    // file was removed by customer
                    diffCount++;                    
                    System.out.print(rbMessages.getString("patch-missing"));
                    System.out.print("(");                                      
                    System.out.print(key);
                    System.out.print(")\n");             
                } else if ((srcValue != null) && (origValue == null)) {
                    // file not in original manifest but is in old war and
                    // new war.  Keep the new one and warn the customer.
                    diffCount++;

                    System.out.print(rbMessages.getString("patch-not-in-manifest"));
                    man1.removeProperty(key);                    
                    if (mergeMode) {
                        File destFile = new File(stagingArea, key);
                        JarFile src2War = new JarFile(man2.srcFile);
                        InputStream fileIn = src2War.getInputStream(
                                src2War.getEntry(key));

                        CopyUtils.copyFileFromJar(fileIn, destFile, 
                                overwriteMode);
                        System.out.print(rbMessages.getString("patch-from-new"));                     
                    }
                    System.out.print("(");                                        
                    System.out.print(key);
                    System.out.print(")\n");                     
                } else if ((srcValue.equals(origValue))) {
                    // File was not changed in original
                    man1.removeProperty(key);
                    
                    if (mergeMode) {
                        File destFile = new File(stagingArea, key);
                        JarFile src2War = new JarFile(man2.srcFile);
                        InputStream fileIn = src2War.getInputStream(
                                src2War.getEntry(key));

                        CopyUtils.copyFileFromJar(fileIn, destFile, 
                                overwriteMode);
                    }
                    if (!hashValue.equals(srcValue)) {
                        // new war has updated file
                        diffCount++;
                        if (!mergeMode) {
                            System.out.print(rbMessages.getString("patch-modified"));
                            System.out.print("(");                    
                            System.out.print(key);
                            System.out.print(")\n");
                        }
                    }
                } else if ((origValue.equals(hashValue)) && 
                        (!origValue.equals(srcValue))) {
                    // customer did modify the original
                    // so keep the customizations
                    diffCount++;
                    customCount++;
                    man1.removeProperty(key);
  
                    System.out.print(rbMessages.getString("patch-customized"));

                    if (mergeMode) {
                        System.out.print(rbMessages.getString("patch-from-orig"));
                        
                        File destFile = new File(stagingArea, key);
                        JarFile src2War = new JarFile(man1.srcFile);
                        InputStream fileIn = src2War.getInputStream(
                                src2War.getEntry(key));

                        CopyUtils.copyFileFromJar(fileIn, destFile, 
                                overwriteMode);
                    }
                    System.out.print("(");
                    System.out.print(key);
                    System.out.print(")\n");                                      
                } else {
                    // file has changed but may have been customized
                    // copy the new file and warn user
                    diffCount++;
                    customCount++;
                    man1.removeProperty(key);
  
                    System.out.print(rbMessages.getString("patch-need-custom"));
                    
                    if (mergeMode) {
                        System.out.print(rbMessages.getString("patch-from-new"));                     
                        
                        File destFile = new File(stagingArea, key);
                        JarFile src2War = new JarFile(man2.srcFile);
                        InputStream fileIn = src2War.getInputStream(
                                src2War.getEntry(key));
                        CopyUtils.copyFileFromJar(fileIn, destFile, 
                                overwriteMode);
                    } 
                    System.out.print("(");                    
                    System.out.print(key);
                    System.out.print(")\n");                    
                }
            }    
            if (orig != null) {
                // At the end, the srcManifest will contain only those
                // items that were not found in the src2Manifest
                for (Enumeration keysEnum = man1.getPropertyNames();
                        keysEnum.hasMoreElements();) {
                    String key = (String) keysEnum.nextElement();
                    String srcValue = man1.getProperty(key);
                    String origValue = orig.getProperty(key);
                    
                    if ((key != null) && (!key.equals(
                            "META-INF/OpenSSO.manifest"))) {
                        // file exists in original war but not in new war
                        // check to see if it was in original manifest
                        // if yes, then we removed in the patch, so don't copy
                        // if no, then this was a file added by user                                       
                        if ((origValue != null) && (key.equals(origValue))) {
                            // was found in original manifest so removed from new
                            continue;
                        }
                        if (origValue == null) {
                            // file was added by customer
                            System.out.print(rbMessages.getString(
                                    "patch-added"));
                        } else {
                            // file was customized but removed in new verwion
                            System.out.print(rbMessages.getString(
                                    "patch-custom-rm"));
                        }
                        if (mergeMode) {
                            System.out.print(rbMessages.getString(
                                    "patch-from-orig"));

                            File destFile = new File(stagingArea, key);
                            JarFile src2War = new JarFile(man1.srcFile);
                            InputStream fileIn = src2War.getInputStream(
                                    src2War.getEntry(key));

                            CopyUtils.copyFileFromJar(fileIn, destFile,
                                    overwriteMode);
                        }
                        System.out.print("(");
                        System.out.print(key);
                        System.out.print(")\n");
                        diffCount++;
                    }
                }
            }
            
            
            if (diffCount == 0) {
                System.out.println(rbMessages.getString("patch-identical"));
            } else {
                System.out.print(rbMessages.getString("patch-diff"));
                System.out.println(diffCount);
                System.out.print(rbMessages.getString("patch-custom"));
                System.out.println(customCount);                
            }            
        } catch (Exception ex) {
            System.out.println(rbMessages.getString("exception-read-error"));
            System.exit(1);
        }
    }
            
   
    private boolean createManifest(Manifest man, String srcFilePath, String destFilePath) {
        return man.createManifest(srcFilePath, destFilePath, null, true, true);
    }
    
    
    public void processPatch() {
        try {
            
            if (stagingFilePath != null) {
                stagingArea = new File(stagingFilePath);
                if (stagingArea.exists() && (overwriteMode == false)) {
                    System.out.println(
                            rbMessages.getString("exception-path-exists"));
                    System.exit(1);    
                }
            }         
            
            // Here we need to generate a manifest for the original war file
            System.out.print(rbMessages.getString("patch-generating"));
            System.out.print(srcFilePath);
            System.out.print("\n");
            if (!createManifest(firstMan, srcFilePath, null)) {
                System.out.println(rbMessages.getString("exception-no-create"));
                System.exit(1);             
            }


            // Grab the original Manifest file stored inside the war file
            File srcFile = new File(srcFilePath);
            JarFile srcWar = new JarFile(srcFile);
           
            origMan.setProperties(PatchGeneratorUtils.getManifest(srcWar,
                    DEFAULT_MANIFEST_FILE, wildCard));   

            String id = origMan.getProperty("identifier");                        
            if (id == null) {
                System.out.println(rbMessages.getString("exception-no-manifest"));
                System.exit(1);               
            }
                      
            if (compareMode == true) {
                // we are going to compare two different war files
                // so generate the manifest for the second war file

                File src2File = new File(src2FilePath);
                JarFile src2War = new JarFile(src2File);
                
                secondMan.setProperties(PatchGeneratorUtils.getManifest(src2War,
                        DEFAULT_MANIFEST_FILE, wildCard));

                String id2 = secondMan.getProperty("identifier");
                if (id2 == null) {
                    System.out.println(rbMessages.getString("exception-no-manifest"));
                    System.exit(1);
                }
                System.out.print(rbMessages.getString("patch-original"));
                System.out.print(id);
                System.out.print("\n"); 
                System.out.print(rbMessages.getString("patch-new"));
                System.out.print(id2);
                System.out.print("\n");

                // need to check to make sure revisions are compatible
                //   ie.  Can patch Enterprise 8.0 to Enterprise 8.0 Update x
                //        Can't patch Nightly build to Enterprise 8.0 Update x
                String[] result = id.split("\\s|\\(|\\)|\\.");
                String[] result2 = id2.split("\\s|\\(|\\)|\\.");

                // Versions have the following format:
                //  Enterprise a.b Build z(datestamp)
                //  Enterprise a.b Update c Build z(datestamp)
                //  Express Build z(datestamp)
                //  (datestamp)

                boolean is_compatible = false;
                int vers = 0, rev = 0, build = 0, update = 0;
                long date = 0;
                int vers2 = 0, rev2 = 0, build2 = 0, update2 = 0;
                long date2 = 0;

                date = Long.parseLong(result[result.length - 1]);
                if (result[0].equals("Enterprise")) {
                    // Enterprise/Express build - grab the version compenents
                    if (result.length > 5) {
                        vers = Integer.parseInt(result[1]);
                        rev = Integer.parseInt(result[2]);
                        if (result[3].equals("Build")) {
                            build = Integer.parseInt(result[4]);
                        } else if (result[3].equals("Update")) {
                            update = Integer.parseInt(result[4]);
                        }
                    }
                } else if (result[0].equals("Express")) {
                    if (result.length >= 3) {
                        if (result[1].equals("Build")) {
                            if (result[2].matches("[0-9]*[a-z]") == false) {
                                build = Integer.parseInt(result[2]);
                            } else {
                                String[] express_bld = result[2].split("[a-z]");
                                build = Integer.parseInt(express_bld[0]);
                            }
                        }
                    }
                }
                
                date2 = Long.parseLong(result2[result2.length - 1]);
                if (result2[0].equals("Enterprise")) {
                    // Enterprise/Express build - grab the version compenents
                    if (result2.length > 5) {
                        vers2 = Integer.parseInt(result2[1]);
                        rev2 = Integer.parseInt(result2[2]);
                        if (result2[3].equals("Build")) {
                            build2 = Integer.parseInt(result2[4]);
                        } else if (result2[3].equals("Update")) {
                            update2 = Integer.parseInt(result2[4]);
                        }
                    }
                } else if (result2[0].equals("Express")) {
                    if (result2.length >= 3) {
                        if (result2[1].equals("Build")) {
                            if (result2[2].matches("[0-9]*[a-z]") == false) {
                                build2 = Integer.parseInt(result2[2]);
                            } else {
                                String[] express_bld = result2[2].split("[a-z]");
                                build2 = Integer.parseInt(express_bld[0]);
                            }
                        }
                    }
                }
                if (result[0].equals(result2[0])) {
                    // both are Enterprise
                    if (vers == vers2 && rev == rev2) {
                        // same revision
                        if ((date <= date2) && ((build <= build2) || (update <= update2))) {
                            is_compatible = true;
                        }
                    }
                }

                if (is_compatible) {
                    System.out.println(rbMessages.getString("patch-supported"));
                } else {
                    System.out.println(rbMessages.getString("patch-not-supported"));
                    if (!overrideMode) {
                        System.out.println(rbMessages.getString("patch-override"));
                        System.exit(1);
                    }
                }             
                
                System.out.print(rbMessages.getString("patch-generating"));
                System.out.print(src2FilePath);
                System.out.print("\n");
                if (!createManifest(secondMan, src2FilePath, null)) {
                    System.out.println(rbMessages.getString("exception-no-create"));
                    System.exit(1);
                }
                compareManifest(firstMan, secondMan, origMan, stagingFilePath);
            } else {
                // comparing one file current contents against original manifest
                // output differences to stdout
                compareManifest(origMan, firstMan, null, null); 
            }                      

        } catch (Exception ex) {
            System.out.println(rbMessages.getString("exception-read-error"));
            System.exit(1);
        }       
    }
    
    
    /* For manifest creation:
     *     -D"file.src.path=test/opensso.war" \
     *          -D"file.dest.path=META-INF/OpenSSO.manifest"
     * 
     * For comparison to see what has changed
     *      -D"file.src.path=test/opensso.war"
     * 
     * For comparison of two versions of OpenSSO
     *     -D"file.src.path=test/opensso.war" \
     *          -D"file.src2.path=test/new_opensso.war"
     * 
     * To create a staging area containing the latest bits 
     *      -D"file.src.path=test/opensso.war" \
     *          -D"file.src2.path=test/new_opensso.war" \
     *          -D"file.staging.path="test/staging"
     * 
     */
    public static void main(String[] args) {
        Patch patch = new Patch();
        getOptions(args);
        patch.getProperties();
  
        // if the destination was passed in, then we are in create mode
        if (patch.createMode) {
            patch.createManifest(patch.firstMan, patch.srcFilePath,
                patch.destFilePath);
        } else {
           patch.processPatch(); 
        }
        
    }
    
    /**
     * Prints  the usage for using the patch generation utility
     *
     */
    private static void printUsage(){
        System.out.println(rbMessages.getString("usage"));
    }

    private static Map options = new HashMap();
    private static Map longShortNameMapping = new HashMap();
    private static Set unary = new HashSet();

    static {
        longShortNameMapping.put(OPTION_HELP, "-?");
        longShortNameMapping.put(OPTION_SRC_FILE_PATH, "-o");
        longShortNameMapping.put(OPTION_DEST_FILE_PATH, "-m");
        longShortNameMapping.put(OPTION_SRC2_FILE_PATH, "-c");
        longShortNameMapping.put(OPTION_STAGING_FILE_PATH, "-s");
        longShortNameMapping.put(OPTION_LOCALE, "-l");
        longShortNameMapping.put(OPTION_OVERRIDE, "-r");
        longShortNameMapping.put(OPTION_OVERWRITE, "-w");

        unary.add(OPTION_HELP);
        unary.add(OPTION_OVERRIDE);
        unary.add(OPTION_OVERWRITE);
    }

    private static void getOptions(String[] args) {
        String currentOpt = null;
        String invalidOpt = null;
        boolean incorrectFormat = false;
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.startsWith("--")) {
                if (!longShortNameMapping.keySet().contains(s)) {
                    invalidOpt = s;
                }
                if (currentOpt != null) {
                    options.put(currentOpt, "");
                }
                currentOpt = s;
            } else if (s.startsWith("-")) {
                String longName = getLongName(s);
                if (longName == null) {
                    invalidOpt = s;
                }
                if (currentOpt != null) {
                    options.put(currentOpt, "");
                }
                currentOpt = longName;
            } else {
                if ((currentOpt == null) || unary.contains(currentOpt)) {
                    incorrectFormat = true;
                } else {
                    options.put(currentOpt, s);
                    currentOpt = null;
                }
            }
        }

        if (currentOpt != null) {
            if (unary.contains(currentOpt)) {
                options.put(currentOpt, "");
            } else {
                incorrectFormat = true;
            }
        }

        String strLocale = (String)options.remove(OPTION_LOCALE);
        if ((strLocale != null) && (strLocale.length() > 0)) {
            locale = getLocale(strLocale);
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }

        try {
            rbMessages = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, locale);
        } catch (MissingResourceException e) {
            System.out.print("Resource file not found.\n");
            System.exit(1);
        }

        boolean bHelp = isOptionSet(OPTION_HELP);

        if (invalidOpt != null) {
            Object[] a = {invalidOpt};
            System.out.print(MessageFormat.format(rbMessages.getString(
                "invalid-option"), a));
            System.out.println();
            printUsage();
            System.exit(1);
        }
        if (incorrectFormat || (bHelp && (options.size() > 1))) {
            System.out.print(rbMessages.getString("incorrect-format"));
            System.out.println();
            printUsage();
            System.exit(1);
        }
        if (bHelp) {
            System.out.println();
            printUsage();
            System.exit(0);
        }
    }

    private static boolean isOptionSet(String optName) {
        String str = (String)options.get(optName);
        return (str != null);
    }

    private static String getLongName(String s) {
        for (Iterator i = longShortNameMapping.keySet().iterator(); i.hasNext();
        ) {
            String longName = (String)i.next();
            String shortName = (String)longShortNameMapping.get(longName);
            if (s.equals(shortName)) {
                return longName;
            }
        }
        return null;
    }
    
}
