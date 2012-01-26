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
 * $Id: TransientStateAccess.java,v 1.2 2008/06/25 05:51:24 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TransientStateAccess implements IStateAccess, InstallConstants {
    
    public TransientStateAccess() {
            this(null);
    }
    
    public TransientStateAccess(Map map) {
            if ((map != null) && (map.size() > 0)) {
                    setData(new HashMap(map));
            } else {
                    setData(new HashMap());
            }
    }
    
    public String getInstanceName() {
        return null; 
    }

    public Object get(String key) {
        return getData().get(key);
    }

    public void put(String key, Object value) {
        getData().put(key, value);
    }
    
    public void remove(String key) {
        getData().remove(key);
    }
    
    public Map getData() {
        return data;
    }
    
    public void putData(Map data) {
        data.putAll(data);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(LINE_SEP);
        sb.append("*** BEGIN PersistentStateAccess ***********");
        Iterator iter = getData().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            sb.append("      ");
            sb.append((String) me.getKey()).append(" = ");
            sb.append((String) me.getValue()).append(LINE_SEP);
        }
        sb.append("*** END PersistentStateAccess  ************");
        sb.append(LINE_SEP);
        return sb.toString();
    }
    
    public void clear() {
        getData().clear();
    }
    
    private void setData(HashMap data) {
        this.data = data;
    }

    private HashMap data;
}
