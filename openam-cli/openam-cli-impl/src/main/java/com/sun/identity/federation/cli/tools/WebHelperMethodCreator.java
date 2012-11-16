/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WebHelperMethodCreator.java,v 1.3 2008/06/25 05:49:54 qcheng Exp $
 *
 */

package com.sun.identity.federation.cli.tools;

import com.sun.identity.cli.CLIConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class is responsible for generating the method for each CLI sub
 * command.
 */
public class WebHelperMethodCreator {
    private String scmdName;
    private String description;
    private String methodName;
    private List<String> formalParameters = new ArrayList<String>();
    private Map<String, String> nameToDescription =
        new HashMap<String, String>();
    private Map<String, String> nameToShortName = new HashMap<String, String>();
    private Set<String> textAreaUI = new HashSet<String>();
    private Set<String> checkboxUI = new HashSet<String>();
    private Set<String> unaryOptionNames = new HashSet<String>();
    private Set<String> singleOptionNames = new HashSet<String>();
    private Set<String> aliases = new HashSet<String>();
    
    WebHelperMethodCreator(
        String cmdName,
        String description,
        List<String> mandatory, 
        List<String> optional,
        List<String> aliases
    ) {
        scmdName = cmdName.replaceAll("_", "-");
        methodName = getMethodName(cmdName);
        this.description = description;
        validateOptions(mandatory);
        validateOptions(optional);
        registerAliases(aliases);
    }
        
    private static String getMethodName(String name) {
        int idx = name.indexOf('_');
        while (idx != -1) {
            name = name.substring(0, idx) +
                name.substring(idx+1, idx+2).toUpperCase() +
                name.substring(idx+2);
            idx = name.indexOf('_');
        }
        return name;
    }
    
    private void validateOptions(List<String> options) {
        for (Iterator i = options.iterator(); i.hasNext(); ) {
            String token = (String)i.next();
            StringTokenizer t = new StringTokenizer(token, "|");
            String name = t.nextToken();
            String shortName = t.nextToken();
            String type = t.nextToken();
            boolean unary = type.equals(CLIConstants.FLAG_UNARY);
            boolean single = type.equals(CLIConstants.FLAG_SINGLE);
            
            if (t.countTokens() == 2) {
                String webUI = t.nextToken();
                if (webUI.equals(CLIConstants.FLAG_WEB_UI_TEXTAREA)) {
                    textAreaUI.add(name);
                } else if (webUI.equals(CLIConstants.FLAG_WEB_UI_CHECKBOX)) {
                    checkboxUI.add(name);
                }
            }
            
            String description = t.nextToken();
            int idxWeb = description.indexOf("<web>");
            if (idxWeb != -1) {
                nameToDescription.put(name, description.substring(idxWeb +5));
            } else {
                nameToDescription.put(name, description);
            }
            nameToShortName.put(name, shortName);
            if (unary) {
                unaryOptionNames.add(name);
            } else if (single) {
                singleOptionNames.add(name);
            }
            
            formalParameters.add(name);
        }
    }
    
    private void registerAliases(List<String> list) {
        for (String opt : list) {
            StringTokenizer st = new StringTokenizer(opt, "|");
            st.nextToken();
            aliases.add(st.nextToken());
        }
    }
    
    public void genMethod(StringBuffer buff) {
        createJavaDoc(buff);
        buff.append("    public HtmlPage ")
            .append(methodName)
            .append("(\n");
        createFormalParameters(buff);
        buff.append("    ) throws Exception {\n");
        createImplementations(buff);
        buff.append("    }\n");
    }
    
    private void createJavaDoc(StringBuffer buff) {
        buff.append("\n    /**\n");
        buff.append("     * ")
            .append(description)
            .append("\n     *\n");
        buff.append(
            "     * @param webClient HTML Unit Web Client object.\n");
        for (Iterator i = formalParameters.iterator(); i.hasNext(); ) {
            String opt = (String)i.next();
            if (!aliases.contains(opt) && !isAuthField(opt) && !isIgnored(opt)){
                buff.append("     * @param ")
                    .append(opt)
                    .append(" ")
                    .append((String)nameToDescription.get(opt))
                    .append("\n");
            }
        }
        buff.append("     */\n");
    }
    
    private void createFormalParameters(StringBuffer buff) {
        buff.append("        WebClient webClient");
        for (Iterator i = formalParameters.iterator(); i.hasNext(); ) {
            String opt = (String)i.next();
            if (!aliases.contains(opt) && !isAuthField(opt) && !isIgnored(opt)){
                if (isTextareaUI(opt)) {
                    buff.append(",\n        String ").append(opt);
                } else if (isCheckboxUI(opt)) {
                    buff.append(",\n        boolean ").append(opt);
                } else if (singleOptionNames.contains(opt)) {
                    buff.append(",\n        String ").append(opt);
                } else {
                    buff.append(",\n        List ").append(opt);
                }
            }
        }
        buff.append("\n");
    }
    
    private void createImplementations(StringBuffer buff) {
        buff.append("        URL cmdUrl = new URL(amadmUrl + \"" + 
            scmdName + "\");\n");
        buff.append(
            "        HtmlPage page = (HtmlPage)webClient.getPage(cmdUrl);\n");
        buff.append(
            "        HtmlForm form = (HtmlForm)page.getForms().get(0);\n\n");
        
        for (Iterator i = formalParameters.iterator(); i.hasNext(); ) {
            String opt = (String)i.next();
            
            if (!aliases.contains(opt) && !isAuthField(opt) && !isIgnored(opt)){
                if (isTextareaUI(opt)) {
                    buff.append(
                        TEXTAREA_TEMPLATE.replaceAll("@opt@", opt))
                        .append("\n");
                } else if (isCheckboxUI(opt)) {
                    buff.append(
                        BOOLEAN_TEMPLATE.replaceAll("@opt@", opt))
                        .append("\n");
                } else if (singleOptionNames.contains(opt)) {
                    buff.append(
                        STRING_TEMPLATE.replaceAll("@opt@", opt))
                        .append("\n");
                } else {
                    buff.append(
                        LIST_TEMPLATE.replaceAll("@opt@", opt))
                        .append("\n");
                }
            }
        }
        buff.append("        return (HtmlPage)form.submit();\n");
    }
    
    private boolean isAuthField(String opt) {
        return opt.equals("adminid") || opt.equals("password");
    }
    
    private boolean isIgnored(String opt) {
        return opt.equals("continue") || opt.equals("outfile") ||
            opt.equals("password-file") ||
            (opt.equals("datafile") &&
                formalParameters.contains("attributevalues")) ||
            (opt.equals("datafile") &&
                formalParameters.contains("choicevalues"));
    }
    
    private boolean isTextareaUI(String opt) {
        String shortName = (String)nameToShortName.get(opt);
        return !shortName.equals(shortName.toLowerCase()) ||
            textAreaUI.contains(opt);
    }

    private boolean isCheckboxUI(String opt) {
        return unaryOptionNames.contains(opt) || checkboxUI.contains(opt);
    }

    private static final String TEXTAREA_TEMPLATE = 
        "        if (@opt@ != null) {\n" +
        "            HtmlTextArea ta@opt@ = " +
        "(HtmlTextArea)form.getTextAreasByName(\"@opt@\").get(0);\n" +
        "            ta@opt@.setText(@opt@);\n" +
        "        }\n";
    
    private static final String STRING_TEMPLATE = 
        "        if (@opt@ != null) {\n" +
        "            HtmlTextInput txt@opt@ = " +
        "(HtmlTextInput)form.getInputByName(\"@opt@\");\n" +
        "            txt@opt@.setValueAttribute(@opt@);\n" +
        "        }\n";
    private static final String BOOLEAN_TEMPLATE = 
        "        HtmlCheckBoxInput cb@opt@ = " +
        "(HtmlCheckBoxInput)form.getInputByName(\"@opt@\");\n" +
        "        cb@opt@.setChecked(@opt@);\n";
    
    private static final String LIST_TEMPLATE =
        "        if (@opt@ != null) {\n" +
        "            HtmlSelect sl@opt@= " +
        "(HtmlSelect)form.getSelectByName(\"@opt@\");\n" +
        "            String[] fakeOptions = new String[@opt@.size()];\n" +
        "            int cnt = 0;\n" +
        "            for (Iterator i = @opt@.iterator(); i.hasNext(); ) {\n" +
        "                fakeOptions[cnt++] = (String)i.next();\n" +
        "            }\n" +
        "            sl@opt@.fakeSelectedAttribute(fakeOptions);\n" +
        "        }\n";
}
