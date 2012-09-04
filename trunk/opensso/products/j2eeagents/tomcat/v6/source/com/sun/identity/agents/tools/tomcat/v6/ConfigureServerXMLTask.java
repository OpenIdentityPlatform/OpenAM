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
 * $Id: ConfigureServerXMLTask.java,v 1.2 2008/11/28 12:36:21 saueree Exp $
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


public class ConfigureServerXMLTask extends ServerXMLBase implements ITask {
    public static final String LOC_TSK_MSG_CONFIGURE_SERVER_XML_EXECUTE =
    	"TSK_MSG_CONFIGURE_SERVER_XML_EXECUTE";
    public static final String LOC_TSK_MSG_CONFIGURE_SERVER_XML_ROLLBACK =
    	"TSK_MSG_CONFIGURE_SERVER_XML_ROLLBACK";

    public boolean execute(
        String name,
        IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = false;

        try {
            XMLDocument xmlDoc = new XMLDocument(new File(
                        getServerXMLFile(stateAccess)));
            xmlDoc.setNoValueIndent();

            // add the Agent realm and Mbean descriptor
            // status = configureServerLifecycleListener(
            //        xmlDoc,
            //        stateAccess);

                status = configureRealm(
                        xmlDoc,
                        stateAccess);
            
        } catch (Exception ex) {
            Debug.log(
                "ConfigureServerXMLTask.configureServerLifecycleListener(): "
                + ex.getMessage(),
                ex);
            status = false;
        }

        return status;
    }

    private boolean configureServerLifecycleListener(
        XMLDocument xmlDoc,
        IStateAccess stateAccess) {
        boolean result = true;
        boolean match = false;
        XMLElement listener = null;

        try {
            ArrayList listenerElements = xmlDoc.getRootElement()
                                               .getNamedChildElements(
                    ELEMENT_LISTENER);
            String classNameVal = null;
            Iterator listenerIterator = listenerElements.iterator();

            while (listenerIterator.hasNext()) {
                listener = (XMLElement) listenerIterator.next();
                classNameVal = listener.getAttributeValue(
                        ATTR_NAME_CLASSNAME);

                if ((classNameVal != null)
                        && (classNameVal.trim()
                                            .length() != 0)) {
                    if (classNameVal.equalsIgnoreCase(
                                ATTR_VALUE_SERVER_LIFECYCLE_CLASSNAME)) {
                        match = true;

                        break;
                    }
                }
            } // end while

            if (match) {
                // Save the old value to be reinstated during unconfigure
                boolean descriptorPresent = false;
                String attrDesc = listener.getAttributeValue(
                        ATTR_NAME_DESCRIPTORS);

                if (attrDesc != null) {
                    if (attrDesc.equalsIgnoreCase(
                                ATTR_VAL_AGENT_MBEAN_DESCRIPTOR)) {
                        stateAccess.put(
                            ATTR_NAME_DESCRIPTORS,
                            attrDesc); // save

                        // old
                        // data
                        Debug.log(
                            "ConfigureServerXMLTask." +
                            "configureServerLifecycleListener: Descriptor "
                            + attrDesc
                            + " present! Replacing with Agent descriptor");
                    } else {
                        Debug.log(
                            "ConfigureServerXMLTask." +
                            "configureServerLifecycleListener: " +
                            "Agent Descriptor " + attrDesc
                            + " already present!");
                        descriptorPresent = true;
                    }
                } else {
                    Debug.log(
                        "ConfigureServerXMLTask." +
                        "configureServerLifecycleListener: " +
                        "Descriptor not present");
                }

                if (!descriptorPresent) {
                    listener.updateAttribute(
                        ATTR_NAME_DESCRIPTORS,
                        ATTR_VAL_AGENT_MBEAN_DESCRIPTOR);
                }

                stateAccess.put(
                    KEY_SERVER_LIFECYCLE_ELEM_EXISTS,
                    STR_VALUE_TRUE);
            } else {
                ArrayList serviceElements = xmlDoc.getRootElement()
                                                  .getNamedChildElements(
                        ELEMENT_SERVICE);

                if ((serviceElements != null)
                        && (serviceElements.size() > 0)) {
                    XMLElement serverLifeCycleElement = xmlDoc.newElement(
                            ELEMENT_LISTENER);

                    serverLifeCycleElement.updateAttribute(
                        ATTR_NAME_CLASSNAME,
                        ATTR_VALUE_SERVER_LIFECYCLE_CLASSNAME);
                    serverLifeCycleElement.updateAttribute(
                        ATTR_NAME_DESCRIPTORS,
                        ATTR_VAL_AGENT_MBEAN_DESCRIPTOR);

                    xmlDoc.getRootElement()
                          .addChildElementAt(
                        serverLifeCycleElement,
                        0);

                    stateAccess.put(
                        KEY_SERVER_LIFECYCLE_ELEM_EXISTS,
                        STR_VALUE_FALSE);
                } else {
                    Debug.log(
                        "ConfigureServerXMLTask." +
                        "ConfigureServerLifecycleListener: No service " +
                        "element found! " + "Mbean descriptor not added");
                }
            }

            XMLElement lifeCycleElement = xmlDoc.newElement(ELEMENT_LISTENER);
            lifeCycleElement.updateAttribute(ATTR_NAME_CLASSNAME, ATTR_VALUE_LIFECYCLE_CLASSNAME);
            xmlDoc.getRootElement().addChildElementAt(lifeCycleElement, 0);
        } catch (Exception ex) {
            Debug.log(
                "ConfigureServerXMLTask.configureServerLifecycleListener(): "
                + ex.getMessage(),
                ex);
            result = false;
        }

        return result;
    }

    private boolean configureRealm(
        XMLDocument xmlDoc,
        IStateAccess stateAccess) {
        boolean result = false;
        XMLElement serviceElement = null;
        XMLElement engineElement = null;
        XMLElement engineChild = null;

        try {
            XMLElement realmElement = xmlDoc.newCollapsedElement(
                    ELEMENT_REALM);
            realmElement.updateAttribute(
                ATTR_NAME_AGENT_REALM_CLASSNAME,
                ATTR_VAL_AGENT_REALM_CLASSNAME);
            realmElement.updateAttribute(
                ATTR_NAME_AGENT_REALM_DEBUG,
                ATTR_VAL_AGENT_REALM_DEBUG);

            ArrayList elements = xmlDoc.getRootElement()
                                       .getChildElements();

            for (int i = 0; i < elements.size(); i++) {
                serviceElement = (XMLElement) elements.get(i);

                if (serviceElement.getName()
                                      .equalsIgnoreCase(ELEMENT_SERVICE)) {
                    int count = 0;
                    ArrayList serviceElements = serviceElement
                        .getChildElements();

                    for (count = 0; count < serviceElements.size();
                            count++) {
                        engineElement = (XMLElement) serviceElements.get(
                                count);

                        if (engineElement.getName()
                                             .equalsIgnoreCase(
                                    ELEMENT_ENGINE)) {
                            boolean oldRealm = false;
                            boolean realmExists = false;
                            int realmIndex = 0;

                            ArrayList engineElements = engineElement
                                .getChildElements();

                            for (int index = 0;
                                    index < engineElements.size();
                                    index++) {
                                engineChild = (XMLElement) engineElements
                                    .get(index);

                                // Check if there is a <Realm> as with new
                                // tomcat install
                                if (engineChild.getName().equalsIgnoreCase(
                                            ELEMENT_REALM)) {
                                    if (engineChild.getAttributeValue(
                                    		ATTR_NAME_CLASSNAME)
                                    		.equalsIgnoreCase(
                                            ATTR_VAL_AGENT_REALM_CLASSNAME)) {
                                        Debug.log(
                                            "ConfigureServerXMLtask." +
                                            "configureRealm(): "
                                            + "Agent Realm already exists.");
                                        realmExists = true;

                                        break;
                                    } else {
                                        Debug.log(
                                            "ConfigureServerXMLtask." +
                                            "configureRealm(): "
                                            + "There is a match for an " +
                                            "existing element <Realm> !");
                                        oldRealm = true;
                                        realmIndex = index;

                                        break;
                                    }
                                }
                            }

                            if (oldRealm) {
                                stateAccess.put(
                                    serviceElement.getAttributeValue(
                                        "name"),
                                    engineChild.toXMLString());
                                	// save old data

                                engineChild.delete();
                                result = true;
                            }

                            if (!realmExists) {
                                engineElement.addChildElementAt(
                                    realmElement,
                                    realmIndex,
                                    true);
                            }

                            result = true;

                            break;
                        }
                    }

                    if (count == serviceElements.size()) {
                        Debug.log(
                            "Agent was not configured for Service "
                            + serviceElement.getName()
                            + " since no engine element was found " +
                            "for the service");
                    }
                }
            }

            xmlDoc.store();
        } catch (Exception ex) {
            Debug.log(
                "ConfigureServerXMLTask.configureServerLifecycleListener(): "
                + "encountered exception " + ex.getMessage(),
                ex);
            result = false;
        }

        return result;
    }

    public LocalizedMessage getExecutionMessage(
        IStateAccess stateAccess,
        Map properties) {
        String serverXMLFile = getServerXMLFile(stateAccess);
        Object[] args = { serverXMLFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_SERVER_XML_EXECUTE,
                STR_TOMCAT_GROUP,
                args);

        return message;
    }

    public LocalizedMessage getRollBackMessage(
        IStateAccess stateAccess,
        Map properties) {
        String serverXMLFile = getServerXMLFile(stateAccess);
        Object[] args = { serverXMLFile };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CONFIGURE_SERVER_XML_ROLLBACK,
                STR_TOMCAT_GROUP,
                args);

        return message;
    }

    public boolean rollBack(
        String name,
        IStateAccess stateAccess,
        Map properties) throws InstallException {
        boolean status = false;

        status = super.unconfigureServerXML(stateAccess);

        return status;
    }
}
