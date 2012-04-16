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
 * $Id: MergeProperties.java,v 1.6 2008/06/25 05:44:03 qcheng Exp $
 *
 */

package com.sun.identity.setup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Merge two properties file into one.
 */
public class MergeProperties {
    private MergeProperties() {
    }

    private MergeProperties(String origProp, String prependProp, String outfile)
        throws IOException
    {
        StringBuffer buff = new StringBuffer();
        Map p1 = getProperties(origProp);
        Map p2 = getProperties(prependProp);
        Set p1Keys = (Set)p1.keySet();
        Set p2Keys = (Set)p2.keySet();

        for (Iterator i = p1Keys.iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            String val = ((String)p1.get(key)).trim();
            if (p2Keys.contains(key)) {
                if (val.length() == 0) {
                    val = ((String)p2.get(key)).trim();
                } else {
                    val += " " + ((String)p2.get(key)).trim();
                }
            }
            buff.append(key)
                .append("=")
                .append(val)
                .append("\n");
        }

        for (Iterator i = p2Keys.iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            String val = (String)p2.get(key);
            if (!p1Keys.contains(key)) {
                buff.append(key)
                    .append("=")
                    .append(val)
                    .append("\n");
            }
        }

        writeToFile(outfile, buff);
    }

    private Map getProperties(String propertyName) {
        Map results = new HashMap();
        ResourceBundle res = ResourceBundle.getBundle(propertyName);
        for (Enumeration e = res.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            results.put(key, res.getString(key));
        }
        return results;
    }

    private void writeToFile(String filename, StringBuffer content)
        throws IOException
    {
        int idx = 0;
        while (idx != -1) {
            idx = content.indexOf("\\", idx);
            if (idx != -1) {
                content.insert(idx, '\\');
                idx += 2;
            }
        }

        if (filename != null) {
            File fileHandle = new File(filename);
            FileWriter out = null;
            try {
                out = new FileWriter(filename);
                out.write(content.toString());
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
     * Merges two properties files into one. The first argument is
     * the original properties file; the second is the one to prepend;
     * and the third is the output filename.
     * <p>
     * E.g.
     * <code>key1=x (from original properties file)</code> and
     * <code>key1=y (from the other properties file)</code> will result in
     * <code>key1=x y</code>.
     */
    public static void main(String[] args) {
        try {
            new MergeProperties(args[0], args[1], args[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
