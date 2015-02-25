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
 * $Id: AttributeValues.java,v 1.10 2009/10/09 23:14:26 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;

/**
 * This class provides utilities for process attribute values. The attribute
 * values are passed into the CLI via properties file.
 */
public class AttributeValues {
    private AttributeValues() {
    }

    /**
     * Returns a map of attribute name to set of values.
     *
     * @param mgr Command Manager object.
     * @param fileName Name of file that contains the attribute values data.
     * @param listAttributeValues list of attribute values in the format.
     *        <code>&lt;attribute-name&gt;=&lt;attribute-value&gt;</code>.
     * @return service attribute values.
     * @throws CLIException if the file contains data of incorrect format.
     */
    public static Map parse(
        CommandManager mgr,
        String fileName,
        List listAttributeValues
    ) throws CLIException {
        Map results = null;
        if (fileName != null) {
            results = parse(mgr, fileName);
        }

        if ((listAttributeValues != null) && !listAttributeValues.isEmpty()) {
            if (results != null) {
                results.putAll(parse(mgr, listAttributeValues));
            } else {
                results = parse(mgr, listAttributeValues);
            }
        }

        return (results == null) ? new HashMap() : results;
    }

    /**
     * Returns a map of attribute name to set of values.
     *
     * @param mgr Command Manager object.
     * @param listAttributeValues list of attribute values in the format.
     *        <code>&lt;attribute-name&gt;=&lt;attribute-value&gt;</code>.
     * @return service attribute values.
     * @throws CLIException if the file contains data of incorrect format.
     */
    public static Map parse(CommandManager mgr, List listAttributeValues) 
        throws CLIException {
        Map attrValues = 
            new HashMap();

        if ((listAttributeValues != null) && !listAttributeValues.isEmpty()) {
            for (Iterator i = listAttributeValues.iterator(); i.hasNext(); ) {
                String s = (String)i.next();
                boolean retry = true;
                int idx = 0;
                
                while (retry) {
                    idx = s.indexOf('=', idx+1);
                    if (idx == -1) {
                        throw createIncorrectFormatException(mgr, s);
                    }
                    retry = (s.charAt(idx-1) == '\\');
                }

                String attrName = s.substring(0, idx);
                String attrValue = s.substring(idx+1);
                
                if (!attrName.startsWith("#")) {
                    attrName = attrName.trim();
                    attrValue = attrValue.trim();
                    attrName = stripEscapeChars(attrName);
                    
                    Set set = (Set)attrValues.get(attrName);
                    if (set == null) {
                        set = new HashSet();
                        attrValues.put(attrName, set);
                    }
                    set.add(attrValue);
                }
            }
        }
        return attrValues;
    }

    /**
     * Returns a map of attribute name to set of values.
     *
     * @param mgr Command Manager object.
     * @param fileName Name of file that contains the attribute values data.
     * @return service attribute values.
     * @throws CLIException if the file contains data of incorrect format.
     */
    public static Map parse(CommandManager mgr, String fileName)
        throws CLIException {
        BufferedReader in = null;
        Map attrValues = new HashMap();

        try {
            in = new BufferedReader(new FileReader(fileName));
            String line = in.readLine();
            while (line != null) {
                line = line.trim();
                if ((line.length() > 0) && !line.startsWith("#")) {
                    boolean retry = true;
                    int idx = 0;

                    while (retry) {
                        idx = line.indexOf('=', idx+1);
                        if (idx == -1) {
                            throw createIncorrectFormatException(mgr, line);
                        }
                        retry = (line.charAt(idx-1) == '\\');
                    }

                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx+1).trim();
                    
                    key = stripEscapeChars(key);
                    Set values = (Set)attrValues.get(key);
                    if (values == null) {
                        values = new HashSet();
                        attrValues.put(key, values);
                    }
                    values.add(hexToString(value));
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } finally {
            if (in !=null ) {
                try {
                    in.close();
                } catch (IOException e) {
                    //ignore cannot close input stream
                }
            }
        }
        return attrValues;
    }
    
    private static String stripEscapeChars(String key) {
        StringBuilder buff = new StringBuilder();
        int idx = key.indexOf('\\');
        
        while (idx != -1) {
            buff.append(key.substring(0, idx));
            key = key.substring(idx +1);
            idx = key.indexOf('\\');
        }
        
        buff.append(key);
        return buff.toString();
    }

    /**
     * Returns a set of values.
     *
     * @param fileName Name of file that contains the values data.
     * @return values.
     * @throws CLIException if the file contains data of incorrect format.
     */
    public static List parseValues(String fileName)
        throws CLIException
    {
        BufferedReader in = null;
        List values = new ArrayList();

        try {
            in = new BufferedReader(new FileReader(fileName));
            String line = in.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0) {
                    values.add(line);
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } finally {
            if (in !=null ) {
                try {
                    in.close();
                } catch (IOException e) {
                    //ignore cannot close input stream
                }
            }
        }
        return values;
    }

    /**
     * Merage two attribute values map.
     *
     * @param map1 Map of String of Set of String.
     * @param map2 Map of String of Set of String.
     * @param multipleAttributesMap map of attribute name to <code>true</code>
     *        if the attribute type is multiple.
     * @param bAdd <code>true</code> to add the values of <code>map2</code>
     *        <code>map1</code>. <code>false</code> to remove values of
     *        <code>map2</code> from <code>map1</code>.
     * @return <code>true</code. is <code>map1</code> is altered.
     */
    public static boolean mergeAttributeValues(
        Map map1,
        Map map2,
        Map<String, Boolean> multipleAttributesMap,
        boolean bAdd
    ) {
        boolean modified = false;
        for (Iterator i = map2.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            Set orig = (Set)map1.get(key);

            Boolean b = multipleAttributesMap.get(key);
            boolean multipleTyped =  ((b != null) && b.booleanValue());

            if (!multipleTyped) {
                map1.put(key, (Set)map2.get(key));
                modified = true;
            } else if ((orig != null) && !orig.isEmpty()) {
                modified = (bAdd) ? orig.addAll((Set)map2.get(key)) :
                    orig.removeAll((Set)map2.get(key));
            } else if (bAdd) {
                map1.put(key, (Set)map2.get(key));
                modified = true;
            }
        }
        return modified;
    }


    /**
     * Merage two attribute values map.
     *
     * @param map1 Map of String of Set of String.
     * @param map2 Map of String of Set of String.
     * @param bAdd <code>true</code> to add the values of <code>map2</code>
     *        <code>map1</code>. <code>false</code> to remove values of
     *        <code>map2</code> from <code>map1</code>.
     * @return <code>true</code. is <code>map1</code> is altered.
     */
    public static boolean mergeAttributeValues(
        Map map1, 
        Map map2,
        boolean bAdd
    ) {
        boolean modified = false;
        for (Iterator i = map2.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            Set orig = (Set)map1.get(key);
            if ((orig != null) && !orig.isEmpty()) {
                modified = (bAdd) ? orig.addAll((Set)map2.get(key)) :
                    orig.removeAll((Set)map2.get(key));
            } else if (bAdd) {
                map1.put(key, (Set)map2.get(key));
                modified = true;
            }
        }
        return modified;
    }

    public static CLIException createIncorrectFormatException(
        CommandManager mgr,
        String line
    ) {
        ResourceBundle rb = mgr.getResourceBundle();
        String[] param = {line};
        String msg = MessageFormat.format(rb.getString(
            "exception-incorrect-data-format"), (Object[])param);
        return new CLIException(msg,ExitCodes.INCORRECT_DATA_FORMAT);
    }
    
    private static String hexToString(String str) {
        StringBuilder buff = new StringBuilder();
        int idx = str.indexOf("\\u");
        while (idx != -1) {
            boolean done = false;
            if (idx > 0) {
                if (str.charAt(idx -1) == '\\') {
                    buff.append(str.substring(0, idx-1))
                        .append(str.substring(idx, idx+2));
                    str = str.substring(idx+2);
                    done = true;
                }
            }

            if (!done) {
                buff.append(str.substring(0, idx))
                    .append(
                    (char)Integer.parseInt(str.substring(idx+2, idx+6), 16));
                str = str.substring(idx+6);
            }
            idx = str.indexOf("\\u");
        }

        buff.append(str);
        return buff.toString();
    }

}
