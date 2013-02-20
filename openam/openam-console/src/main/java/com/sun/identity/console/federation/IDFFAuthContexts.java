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
 * $Id: IDFFAuthContexts.java,v 1.3 2008/06/25 05:49:36 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * This is collections of authentication contexts. This object can be serialized
 * between view beans forwarding.
 */
public class IDFFAuthContexts
    implements Serializable {
    private Map collections = new HashMap();
    
    /**
     * Default Constructor.
     */
    public IDFFAuthContexts() {
    }
    
    
    public List toSPAuthContextInfo(){
        List list = new ArrayList();
        Set entries = collections.entrySet();
        Iterator iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            IDFFAuthContext idffAuthContextObj =
                (IDFFAuthContext) entry.getValue();           
            String str = "context=" + idffAuthContextObj.name + "|level=" +
                idffAuthContextObj.level ;
            if(idffAuthContextObj.supported.equals("true")){
                list.add(str);
            }
        }
       
        return list;
    }
    
    public List toIDPAuthContextInfo(){        
        List list = new ArrayList();
        Set entries = collections.entrySet();
        Iterator iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();            
            IDFFAuthContext idffAuthContextObj =
                (IDFFAuthContext)entry.getValue();
            String str = "context=" + idffAuthContextObj.name + 
                "|key=" +  idffAuthContextObj.key +
                "|value=" +  idffAuthContextObj.value +
                "|level=" +  idffAuthContextObj.level ;
            if(idffAuthContextObj.supported.equals("true")){
                list.add(str);
            }
        }       
        return list;
    }
    /**
     * Returns true is collection is empty.
     *
     * @return true is collection is empty.
     */
    public boolean isEmpty() {
        return collections.isEmpty();
    }
    
    /**
     * Returns number of entries in collection.
     *
     * @return number of entries in collection.
     */
    public int size() {
        return collections.size();
    }
    
    public Map getCollections(){
        return collections;
    }
    
    
    /**
     * Adds IDFFAuthContext to the collection
     *
     * @param name Name of entry.
     * @param supported true if this entry is supported.
     * @param level Level of entry.
     */
    public void put(String name,  String supported, String level) {
        IDFFAuthContext c = new IDFFAuthContext();
        c.name = name;
        c.supported = supported;
        c.key = null;
        c.value = null;
        c.level = level;
        collections.put(name, c);
    }
    
    /**
     * Adds IDFFAuthContext to the collection.
     *
     * @param name Name of entry.
     * @param supported true if this entry is supported.
     * @param key Key of entry.
     * @param value Value of entry.
     * @param level Level of entry.
     */
    public void put(
        String name,
        String supported,
        String key,
        String value,
        String level
    ) {
        IDFFAuthContext c = new IDFFAuthContext();
        c.name = name;
        c.supported = supported;
        c.key = key;
        c.value = value;
        c.level = level;
        collections.put(name, c);
    }

    /**
     * Returns IDFFAuthContext in the collection.
     *
     * @param name Name of entry to return.
     * @return IDFFAuthContext in the collection.
     */
    public IDFFAuthContext get(String name) {
        return (IDFFAuthContext)collections.get(name);
    }
    
    public class IDFFAuthContext
        implements Serializable {
        public String name;
        public String supported;
        public String key;
        public String value;
        public String level;
        
        public IDFFAuthContext() {
        }
       
        public String toString(){
            String str = "context=" + name 
                + "|support=" +  supported 
                + "|key=" +  key + "|value="
                +  value + "|level=" +  level ;
            return str;
        }
        
    }
}
