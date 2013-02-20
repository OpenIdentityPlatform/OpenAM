/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CLIDefinitionGenerator.java,v 1.9 2008/06/25 05:42:24 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package org.forgerock.maven.plugins;

import com.sun.identity.cli.annotation.DefinitionClassInfo;
import com.sun.identity.cli.annotation.Macro;
import com.sun.identity.cli.annotation.ResourceStrings;
import com.sun.identity.cli.annotation.SubCommandInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author Peter Major
 * @goal generate-cli
 */
public class CLIDefinitionGenerator extends AbstractMojo {
    
    private static Map<String, String> mapLongToShortOptionName =
            new HashMap<String, String>();
    /**
     * A list of fully qualified classnames of CLI definitions.
     * @parameter
     */
    private List<String> definitions = new ArrayList<String>();
    /**
     * The directory where the generated properties files should be written to.
     * @parameter
     * @required
     */
    private String outputDir;
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project = new MavenProject();
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        for (String className : definitions) {
            try {
                Class clazz = Class.forName(className);
                Field pdtField = clazz.getDeclaredField("product");
                
                if (pdtField != null) {
                    DefinitionClassInfo classInfo = pdtField.getAnnotation(DefinitionClassInfo.class);
                    
                    PrintStream rbOut = createResourcePrintStream(outputDir, classInfo);
                    getCommonResourceStrings(rbOut, clazz);
                    rbOut.println("product-name=" + classInfo.productName());
                    getCommands(className, clazz, rbOut);
                    rbOut.close();
                } else {
                    throw new Exception("Incorrect Definiton, class=" + className + " missing product field");
                }
            } catch (Exception ex) {
                throw new MojoFailureException("An error occured while generating CLI resources", ex);
            }
        }
        Resource resource = new Resource();
        resource.setDirectory(outputDir);
        project.addResource(resource);
    }

    /**
     * Generates CLI Definition class.
     *
     * @param argv Array of annotated class names.
     */
    private void getCommonResourceStrings(PrintStream rbOut, Class clazz) throws Exception {
        Field field = clazz.getDeclaredField("resourcestrings");
        
        if (field != null) {
            ResourceStrings resStrings = field.getAnnotation(ResourceStrings.class);
            List<String> list = toList(resStrings.string());
            for (String resString : list) {
                rbOut.println(resString);
            }
        }
    }
    
    private PrintStream createResourcePrintStream(String resourceDir, DefinitionClassInfo classInfo)
            throws IOException {
        new File(resourceDir).mkdirs();
        String filePath = resourceDir + File.separator + classInfo.resourceBundle() + ".properties";
        getLog().error("FILE PATH: " + filePath);
        File rbFile = new File(filePath);
        rbFile.createNewFile();
        FileOutputStream rbStream = new FileOutputStream(rbFile);
        return new PrintStream(rbStream);
    }
    
    private void getCommands(String className, Class clazz, PrintStream rbOut) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        for (Field fld : fields) {
            SubCommandInfo info = fld.getAnnotation(SubCommandInfo.class);
            if (info != null) {
                if ((info.implClassName() == null) || (info.description() == null)) {
                    throw new Exception("Incorrect Definition, class=" + className + " field=" + fld.toString());
                }
                
                List<String> mandatoryOptions = toList(info.mandatoryOptions());
                List<String> optionalOptions = toList(info.optionalOptions());
                List<String> optionAliases = toList(info.optionAliases());
                
                if (info.macro() != null && info.macro().length() > 0) {
                    Field fldMarco = clazz.getDeclaredField(info.macro());
                    Macro macroInfo = fldMarco.getAnnotation(Macro.class);
                    appendToList(mandatoryOptions, macroInfo.mandatoryOptions());
                    appendToList(optionalOptions, macroInfo.optionalOptions());
                    appendToList(optionAliases, macroInfo.optionAliases());
                }
                
                validateOption(mandatoryOptions);
                validateOption(optionalOptions);
                
                String subcmdName = fld.getName().replace('_', '-');
                
                String resPrefix = "subcmd-" + subcmdName;
                String desc = info.description().replaceAll("&#124;", "|");
                rbOut.println(resPrefix + "=" + desc);
                createResourceForOptions(resPrefix + "-", mandatoryOptions, rbOut);
                createResourceForOptions(resPrefix + "-", optionalOptions, rbOut);
                addResourceStrings(toList(info.resourceStrings()), rbOut);
            }
        }
    }
    
    private void validateOption(List<String> options) throws MojoFailureException {
        for (String option : options) {
            int idx = option.indexOf('|');
            String longName = option.substring(0, idx);
            String shortName = option.substring(idx + 1, idx + 2);
            
            String test = mapLongToShortOptionName.get(longName);
            if (test == null) {
                mapLongToShortOptionName.put(longName, shortName);
            } else if (!test.equals(shortName)) {
                throw new MojoFailureException("Mismatched names: " + longName + "-> " + test + ", " + shortName);
            }
        }
    }
    
    private void addResourceStrings(List<String> res, PrintStream rbOut) {
        for (String s : res) {
            rbOut.println(s);
        }
    }
    
    private void createResourceForOptions(String prefix, List<String> options, PrintStream rbOut) {
        for (String option : options) {
            String opt = option.substring(0, option.indexOf('|'));
            String description = option.substring(option.lastIndexOf('|') + 1);
            int idxWeb = description.indexOf("<web>");
            if (idxWeb != -1) {
                rbOut.println(prefix + "__web__-" + opt + "=" + description.substring(idxWeb + 5));
                description = description.substring(0, idxWeb);
            }
            
            rbOut.println(prefix + opt + "=" + description);
        }
    }

    /**
     * Returns a list of string by adding string in an array to it.
     *
     * @param array Array of String.
     * @return a list of string.
     */
    private List<String> toList(String[] array) {
        List<String> ret = new ArrayList<String>();
        if (array != null && array.length > 0) {
            ret.addAll(Arrays.asList(array));
        }
        return ret;
    }

    /**
     * Adds string in an array to a list.
     *
     * @param list List of string where new string are to be added.
     * @param array Array of String.
     */
    private void appendToList(List<String> list, String[] array) {
        if (array != null && array.length > 0) {
            list.addAll(Arrays.asList(array));
        }
    }
}
