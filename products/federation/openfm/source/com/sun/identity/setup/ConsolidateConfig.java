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
 * $Id: ConsolidateConfig.java,v 1.2 2008/06/25 05:50:00 qcheng Exp $
 *
 */

package com.sun.identity.setup;

import com.sun.identity.common.SystemConfigurationUtil;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PropertyResourceBundle;

/**
 * Consolidate properties from two files into one.
 */
public class ConsolidateConfig {
    
    private ConsolidateConfig() {
    }
    
    public static void main(String[] args) {
        try {
            Map map1 = getPropertyMap(args[0]);
            Map map2 = getPropertyMap(args[1]);
            /*
             * in federation, we have com.sun.identity.common.serverMode
             * in amserver, we have com.iplanet.am.serverMode
             * they are referring to the same thing. Hence we need to
             * remove com.sun.identity.common.serverMode.
             */
            map2.remove(SystemConfigurationUtil.PROP_SERVER_MODE); 
            
            for (Iterator i = map2.keySet().iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                if (!map1.containsKey(key)) {
                    map1.put(key, map2.get(key));
                }
                
            }

            writeToFile(map1, args[2]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static Map getPropertyMap(String filename)
        throws Exception {
        Map map = new HashMap();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            PropertyResourceBundle bundle = new PropertyResourceBundle(fis);
            for (Enumeration e = bundle.getKeys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                map.put(key, bundle.getString(key));
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
        return map;
    }

    private static void writeToFile(Map map, String filename)
        throws FileNotFoundException, IOException
    {
        FileOutputStream fout = null;
        StringBuffer buff = new StringBuffer();
        
        for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            buff.append(entry.getKey())
                .append("=")
                .append(entry.getValue())
                .append("\n");
        }
        try {
            fout = new FileOutputStream(filename);
            fout.write(buff.toString().getBytes());
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
    }
}

