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
 * $Id: AgentPropertyXMLBuilder.java,v 1.8 2008/10/02 16:31:28 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.property;

import com.iplanet.sso.SSOException;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.console.agentconfig.AgentTabManager;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.console.agentconfig.AgentsViewBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for building agent configuration XML.
 */
public class AgentPropertyXMLBuilder
    extends PropertyXMLBuilderBase
{
    private String agentType;
    private boolean bGroup;
    private boolean olderAgentType;
    private String tabName;

    private static final String DUMMY_SECTION = "blank.header";
    private static final String ATTR_NAME_PWD = "userpassword";
    private static final String ATTR_LOC_CONFIG =
        "com.sun.identity.agents.config.repository.location";
    
    /**
     * Constructor
     *
     * @param agentType Type of agent
     * @param bGroup <code>true</code> if this is for agent group.
     * @param olderAgentType <code>true</code> if agent is older ones like
     *        2.2 agent
     * @param tab Tab name.
     * @param model Model for getting localized string and user locale.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     */
    public AgentPropertyXMLBuilder(
        String agentType,
        boolean bGroup,
        boolean olderAgentType,
        String tab,
        AMModel model
    ) throws SMSException, SSOException {
        this.model = model;
        this.agentType = agentType;
        this.bGroup = bGroup;
        this.olderAgentType = olderAgentType;
        tabName = tab;
        
        if (tab == null) {
            AgentTabManager mgr = AgentTabManager.getInstance();
            tabName = mgr.getDefaultTab(agentType);
        } else {
            tabName = tab;
        }
        
        serviceName = IdConstants.AGENT_SERVICE;
        svcSchemaManager = new ServiceSchemaManager(
            serviceName, model.getUserSSOToken());
        getServiceResourceBundle();
        if (serviceBundle != null) {
            getAttributeSchemas(serviceName, olderAgentType);
        }
    }

    /**
     * Returns a XML for displaying attribute in property sheet.
     *
     * @param readonly Set of readonly attribute names.
     * @param choice Choice of type of configuration.
     * @throws SMSException if attribute schema cannot obtained.
     * @throws SSOException if single sign on token is invalid.
     * @throws AMConsoleException if there are no attribute to display.
     * @return XML for displaying attribute in property sheet.
     */
    public String getXML(Set readonly, String  choice)
        throws SMSException, SSOException, AMConsoleException {
        StringBuffer xml = new StringBuffer(1000);

        xml.append(getXMLDefinitionHeader()).append(START_TAG);
        AgentTabManager tabMgr = AgentTabManager.getInstance();
        List order = tabMgr.getSectionOrder(agentType, tabName);
        boolean bLocal = (choice != null) && choice.equals("local");
        
        if (bLocal || (order == null) || order.isEmpty()) {
            Set attrSchemas = (Set)mapTypeToAttributeSchema.get(DUMMY_SECTION);
            if (bLocal) {
                for (Iterator i = attrSchemas.iterator(); i.hasNext();) {
                    AttributeSchema as = (AttributeSchema) i.next();
                    if ((as.getName().equals(AgentsViewBean.DESCRIPTION))) {
                        i.remove();
                    }
                }
            }
            String display = model.getLocalizedString("blank.header");
            buildSchemaTypeXML(display, attrSchemas, xml, model,
                serviceBundle, readonly);
        } else {
            for (Iterator i = order.iterator(); i.hasNext(); ) {
                String sectionName = (String)i.next();
                Set attributeSchema = (Set)mapTypeToAttributeSchema.get(
                    sectionName);
                String display = model.getLocalizedString("section.label." +
                    agentType + "." + tabName + "." + sectionName);
                buildSchemaTypeXML(display, attributeSchema, xml,
                    model, serviceBundle, readonly);
            }
        }

        xml.append(END_TAG);
        
        if (!bGroup && !olderAgentType && tabMgr.isFirstTab(agentType, tabName)
        ) {
            String buff = xml.toString();
            int idx = buff.indexOf("<property ");
            return buff.substring(0, idx) + GROUP_XML + buff.substring(idx);
        } else {
            return xml.toString();
        }
   }
    
    private void getAttributeSchemas(String serviceName, boolean localized) 
        throws SMSException, SSOException {
        mapTypeToAttributeSchema = new HashMap();
        Set localProperty = null;
        
        if (localized && !bGroup) {
            localProperty= AgentConfiguration.getLocalPropertyNames(agentType);
        }
        
        Set attrSchemas = AgentConfiguration.getAgentAttributeSchemas(
            agentType);
        for (Iterator i = attrSchemas.iterator(); i.hasNext(); ) {
            AttributeSchema as = (AttributeSchema)i.next();
            String i18nKey = as.getI18NKey();
            if ((i18nKey == null) || (i18nKey.trim().length() == 0)) {
                i.remove();
            } else if (bGroup) {
                if (as.getName().equals(ATTR_NAME_PWD) ||
                    as.getName().equals(ATTR_LOC_CONFIG)
                ) {
                    i.remove();
                }
            } else if ((localProperty != null) && 
                !localProperty.contains(as.getName())) {
                i.remove();
            }
        }
        
        AgentTabManager mgr = AgentTabManager.getInstance();
        Map secToAttrNames = null;
        
        if (!localized) {
            secToAttrNames = (tabName == null) ?
            mgr.getAttributeNames(agentType, -1) :
            mgr.getAttributeNames(agentType, mgr.getTabId(agentType, tabName));
        }
        if ((secToAttrNames == null) || secToAttrNames.isEmpty()) {
            mapTypeToAttributeSchema.put(DUMMY_SECTION, attrSchemas);
        } else {
            for (Iterator i = secToAttrNames.keySet().iterator(); i.hasNext(); )
            {
                String sectionName = (String)i.next();
                Set attrNames = (Set)secToAttrNames.get(sectionName);
                Set set = getAttributeSchemas(attrSchemas, attrNames);
                if ((set != null) && !set.isEmpty()) {
                    mapTypeToAttributeSchema.put(sectionName, set);
                }
            }
        }
    }

    /**
     * Returns a set of attribute schemas for generating the XML.
     *
     * @return a set of attribute schemas for generating the XML.
     */
    public Set getAttributeSchemas() {
        Set set = new HashSet();
        for (Iterator i = mapTypeToAttributeSchema.keySet().iterator(); 
            i.hasNext(); 
        ) {
            String key = (String)i.next();
            set.addAll((Set)mapTypeToAttributeSchema.get(key));
        }
        return set;
    }

    private Set getAttributeSchemas(Set attrSchemas, Set selectable) {
        Set results = new HashSet(selectable.size() *2);

        if ((attrSchemas != null) && !attrSchemas.isEmpty()) {
            for (Iterator i = attrSchemas.iterator(); i.hasNext(); ) {
                AttributeSchema as = (AttributeSchema)i.next();
                String name = as.getName();
                if (selectable.contains(name)) {
                    String i18nKey = as.getI18NKey();
                    if ((i18nKey != null) && (i18nKey.trim().length() > 0)) {
                        results.add(as);
                    }
                }
            }
        }

        return results;
    }
    
    private static final String GROUP_XML =
        "<property>" +
        "<label name=\"lblagentgroup\" " +
        "defaultValue=\"label.agentgroup\" " +
        "labelFor=\"agentgroup\" />" +
        "<cc name=\"agentgroup\" " +
        "tagclass=\"com.sun.web.ui.taglib.html.CCDropDownMenuTag\" />" +
        "</property>";
}
