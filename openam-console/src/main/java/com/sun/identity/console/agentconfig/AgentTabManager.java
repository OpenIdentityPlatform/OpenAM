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
 * $Id: AgentTabManager.java,v 1.2 2008/06/25 05:42:44 qcheng Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.sun.identity.console.base.model.AMModelBase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Agent Tab Manager.
 */
public class AgentTabManager {
    private static AgentTabManager instance = new AgentTabManager();
    private Map tabs = new TreeMap();
    private Map tabOrder = new HashMap();
    private Map sectionOrder = new HashMap();
    
    private AgentTabManager() {
        initInstance();
    }
    
    /**
     * Returns an instance of agent tab manager.
     *
     * @return an instance of agent tab manager.
     */
    public static AgentTabManager getInstance() {
        return instance;
    }
    
    /**
     * Returns map of section name to a set of attribute names of a given tab.
     *
     * @param agentType Type of agent.
     * @param tabId Tab Id.
     * @return map of section name to a set of attribute names of a given tab.
     */
    public Map getAttributeNames(String agentType, int tabId) {
        Map results = null;
        List list = getTabs(agentType);
        
        if ((list != null) && !list.isEmpty()) {
            String tabName = (tabId == -1) ? 
                (String)list.iterator().next() : getTabName(agentType, tabId);
            Map map = (Map)tabs.get(agentType);
            if (map != null) {
                results = (Map)map.get(tabName);
            }
        }
        return results;
    }
    
    
    /**
     * Returns tabs  of a given agent type.
     *
     * @param agentType Type of agent.
     * @return tabs of a given agent type.
     */
    public List getTabs(String agentType) {
        return (List)tabOrder.get(agentType);
    }
    
    /**
     * Returns default tab Id of a given agent type.
     *
     * @param agentType Type of agent.
     * @return default tab Id of a given agent type.
     */
    public String getDefaultTab(String agentType) {
        List list = (List)tabOrder.get(agentType);
        return (list != null) ? (String)list.get(0) : null;
    }
    
    /**
     * Returns order of sections of a given tab.
     *
     * @param agentType Type of agent.
     * @param tab Tab Id.
     * @return order of sections of a given agent type.
     */
    public List getSectionOrder(String agentType, String tab) {
        if (tab != null) {
            Map map = (Map)sectionOrder.get(agentType);
            return (map != null) ? (List)map.get(tab) : null;
        } else {
            return null;
        }
    }
    
    /**
     * Returns <code>true</code> if the tab is the first one.
     *
     * @param agentType Type of agent.
     * @param tab Name of tab
     * @return <code>true</code> if the tab is the first one.
     */
    public boolean isFirstTab(String agentType, String tab) {
        return (tab == null) || (tab.equals(getDefaultTab(agentType)));
    }
    
    private void initInstance() {
        Map tabProperties = getTabProperties();
        for (Iterator i = tabProperties.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            Set values = (Set)tabProperties.get(key);
            for (Iterator j = values.iterator(); j.hasNext(); ) {
                String value = (String)j.next();
                addTabEntry(key, value);
            }
        }
        
        ResourceBundle rbOrder = ResourceBundle.getBundle("agenttaborder");
        for (Enumeration e = rbOrder.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String value = rbOrder.getString(key);
            int idx = key.indexOf('.');
            String agentType = key.substring(0, idx);
            String marker = key.substring(idx+1);
            
            if (marker.equals("tab")) {
                List list = new ArrayList();
                StringTokenizer st = new StringTokenizer(value, " ");
                while (st.hasMoreTokens()) {
                    list.add(st.nextToken().trim());
                }
                tabOrder.put(agentType, list);
            } else if (marker.equals("section")) {
                Map map = new HashMap();
                sectionOrder.put(agentType, map);
                
                StringTokenizer st = new StringTokenizer(value, " ");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();
                    int idx1 = token.indexOf('.');
                    String tab = token.substring(0, idx1);
                    String section = token.substring(idx1+1);
                    
                    List list = (List)map.get(tab);
                    if (list == null) {
                        list = new ArrayList();
                        map.put(tab, list);
                    }
                    list.add(section);
                }
            }
            
        }
    }
    
    private void addTabEntry(String key, String value) {
        StringTokenizer st = new StringTokenizer(key, ".");
        if (st.countTokens() == 3) {
            String agentType = st.nextToken();
            String tab = st.nextToken();
            String section = st.nextToken();
            
            Map mapAgentType = (Map)tabs.get(agentType);
            if (mapAgentType == null) {
                mapAgentType = new TreeMap();
                tabs.put(agentType, mapAgentType);
            }
            
            Map mapTab = (Map)mapAgentType.get(tab);
            if (mapTab == null) {
                mapTab = new TreeMap();
                mapAgentType.put(tab, mapTab);
            }
            
            Set mapSection = (Set)mapTab.get(section);
            if (mapSection == null) {
                mapSection = new TreeSet();
                mapTab.put(section, mapSection);
            }
            
            mapSection.add(value);
        }
    }
    
    private Map getTabProperties() {
        InputStream is =getClass().getClassLoader().getResourceAsStream(
            "agenttab.properties");
        Map map = new HashMap();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("#") && (line.trim().length() > 0)) {
                    int idx = line.indexOf('=');
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx+1).trim();
                    
                    Set set = (Set)map.get(key);
                    if (set == null) {
                        set = new HashSet();
                        map.put(key, set);
                    }
                    set.add(value);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            AMModelBase.debug.error("AgentTabManager.getTabProperties", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    // do nothing
                }
            }
        }
        return map;
    }
    
    /**
     * Returns tab name.
     *
     * @param tabIdx Tab Id.
     * @return tab name.
     */
    public String getTabName(String agentType, int tabIdx) {
        String sIdx = Integer.toString(tabIdx);
        sIdx = sIdx.substring(GenericAgentProfileViewBean.TAB_PREFIX.length());
        int idx = Integer.parseInt(sIdx);
        List list = (List)tabOrder.get(agentType);
        return (String)list.get(idx);
    }

    /**
     * Returns tab Id.
     *
     * @param tabName Tab name.
     * @return tab Id.
     * */
    public int getTabId(String agentType, String tabName) {
        int idx = -1;
        List list = (List)tabOrder.get(agentType);
        
        for (int i = 0; i < list.size() && (idx == -1); i++ ) {
            String name = (String)list.get(i);
            if (name.equals(tabName)) {
                idx = Integer.parseInt(
                    GenericAgentProfileViewBean.TAB_PREFIX + i);
            }
        }
        return idx;
    }
}
