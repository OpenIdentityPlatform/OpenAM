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
 * $Id: ServerXMLBase.java,v 1.2 2008/11/28 12:36:22 saueree Exp $
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.agents.tools.tomcat.v6;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.xml.XMLDocument;
import com.sun.identity.install.tools.util.xml.XMLElement;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;


public class ServerXMLBase implements IConstants, IConfigKeys {
    public ServerXMLBase() {
    }

    public boolean unconfigureServerXML(IStateAccess stateAccess) {
        boolean status = false;

        try {
            XMLDocument xmlDoc = new XMLDocument(new File(
                        getServerXMLFile(stateAccess)));
            xmlDoc.setValueIndent();

            // remove the realm
            status = unconfigureRealm(
                    xmlDoc,
                    stateAccess);
            xmlDoc.store();
        } catch (Exception e) {
            Debug.log(
                "ServerXMLBase.unconfigureServerXML(): "
                + " encountered exception " + e.getMessage());
        }

        return status;
    }

    private boolean unconfigureRealm(
        XMLDocument xmlDoc,
        IStateAccess stateAccess) {
        boolean result = false;
        XMLElement serviceElement = null;
        XMLElement engineElement = null;
        XMLElement engineChild = null;

        try {
            ArrayList elements = xmlDoc.getRootElement()
                                       .getChildElements();

            for (int i = 0; i < elements.size(); i++) {
                serviceElement = (XMLElement) elements.get(i);

                if (serviceElement.getName()
                                      .equalsIgnoreCase(ELEMENT_SERVICE)) {
                    int count = 0;
                    int index = 0;
                    ArrayList serviceElements = serviceElement
                        .getChildElements();

                    for (count = 0; count < serviceElements.size();
                            count++) {
                        engineElement = (XMLElement) serviceElements.get(
                                count);

                        if (engineElement.getName()
                                             .equalsIgnoreCase(
                                    ELEMENT_ENGINE)) {
                            boolean realmExists = false;

                            ArrayList engineElements = engineElement
                                .getChildElements();

                            for (index = 0; index < engineElements.size();
                                    index++) {
                                engineChild = (XMLElement) engineElements
                                    .get(index);

                                if (engineChild.getName()
                                            .equalsIgnoreCase(
                                            ELEMENT_REALM)) {
                                    if (engineChild.getAttributeValue(
                                    		ATTR_NAME_CLASSNAME)
                                            .equalsIgnoreCase(
                                            ATTR_VAL_AGENT_REALM_CLASSNAME))
                                    {
                                        Debug.log(
                                            "ServerXMLBase.unconfigureRealm():"
                                            + " Found and removing " +
                                            "Agent Realm!");

                                        engineChild.delete();

                                        realmExists = true;

                                        break;
                                    }
                                }
                            }

                            if (realmExists) {
                                String previousRealm = (String) stateAccess
                                    .get(
                                        serviceElement.getAttributeValue(
                                            "name"));

                                if ((previousRealm != null)
                                        && (previousRealm.length() > 0)) {
                                    Debug.log(
                                        "ServerXMLBase.unconfigureRealm(): "
                                        + "restoring previous realm "
                                        + previousRealm);

                                    XMLElement previousRealmElem = xmlDoc
                                        .newElementFromXMLFragment(
                                            previousRealm);
                                    engineElement.addChildElementAt(
                                        previousRealmElem,
                                        index);
                                }

                                result = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Debug.log(
                "ServerXMLbase.unconfigureRealm(): encountered exception "
                + ex.getMessage(),
                ex);
            result = false;
        }

        return result;
    }

    protected String getServerXMLFile(IStateAccess stateAccess) {
        return (String) stateAccess.get(STR_KEY_TOMCAT_SERVER_XML_FILE);
    }
}
