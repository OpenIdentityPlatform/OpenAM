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
 * $Id: DefaultIDPAttributeMapper.java,v 1.4 2008/08/29 02:29:17 superpat7 Exp $
 *
 */


package com.sun.identity.wsfederation.plugins;

import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.wsfederation.common.WSFederationException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.wsfederation.common.WSFederationConstants;

/**
 * This class <code>DefaultAttributeMapper</code> implements the
 * <code>IDPAttributeMapper</code> to return the SAML <code>Attribute</code>
 * objects that may be inserted in the SAML Assertion.
 * This IDP attribute mapper reads the attribute map configuration defined
 * in the hosted IDP configuration and construct the SAML
 * <code>Attribute</code> objects. If the mapped values are not present in
 * the data store, this will try to read from the Single sign-on token.
 */
public class DefaultIDPAttributeMapper extends DefaultAttributeMapper 
     implements IDPAttributeMapper {

    /**
     * Constructor
     */
    public DefaultIDPAttributeMapper() {
        debug.message("DefaultIDPAttributeMapper.Constructor");
        role = IDP;
    }

    /**
     * Returns list of SAML <code>Attribute</code> objects for the 
     * IDP framework to insert into the generated <code>Assertion</code>. 
     * @param session Single sign-on session.
     * @param hostEntityID <code>EntityID</code> of the hosted entity.
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @param realm name of the realm.
     * @exception WSFederationException if any failure.
     */
    public List getAttributes(
        Object session,
        String hostEntityID,
        String remoteEntityID,
        String realm 
    ) throws WSFederationException {
 
        if(hostEntityID == null) {
           throw new WSFederationException(bundle.getString(
                 "nullHostEntityID"));
        }

        if(realm == null) {
           throw new WSFederationException(bundle.getString(
                 "nullRealm"));
        }
       
        if(session == null) {
           throw new WSFederationException(bundle.getString(
                 "nullSSOToken"));
        }

        try {
            if(!SessionManager.getProvider().isValid(session)) {
               if(debug.warningEnabled()) {
                  debug.warning("DefaultIDPAttributeMapper.getAttributes: " +
                  "Invalid session");
               }
               return null;
            }

            Map configMap = getConfigAttributeMap(realm, hostEntityID);
            if(configMap == null || configMap.isEmpty()) {
               if(debug.messageEnabled()) {
                  debug.message("DefaultIDPAttributeMapper.getAttributes:" +
                  "Configuration map is not defined.");
               }
               return null;
            }

            List attributes = new ArrayList();
            
            Set localAttributes = new HashSet();
            localAttributes.addAll(configMap.values());
            Map valueMap = null;

            try {
                valueMap = dsProvider.getAttributes(
                     SessionManager.getProvider().getPrincipalName(session),
                     localAttributes); 
            } catch (DataStoreProviderException dse) {
                if(debug.warningEnabled()) {
                   debug.warning("DefaultIDPAttributeMapper.getAttributes: "+
                   "Datastore exception", dse);
                }
                //continue to check in ssotoken.
            }

            Iterator iter = configMap.keySet().iterator();
            while(iter.hasNext()) {
                String samlAttribute = (String)iter.next();
                String localAttribute = (String)configMap.get(samlAttribute);
                String[] localAttributeValues = null;
                if(valueMap != null && !valueMap.isEmpty()) {
                   Set values = (Set)valueMap.get(localAttribute); 
                   if(values == null || values.isEmpty()) {
                      if(debug.messageEnabled()) {
                         debug.message("DefaultIDPAttributeMapper.getAttribute:"
                         + " user profile does not have value for " + 
                         localAttribute + " but is going to check ssotoken:");
                      }
                      localAttributeValues = SessionManager.
                          getProvider().getProperty(session, localAttribute);
                        if (localAttributeValues != null &&
                          localAttributeValues.length == 0) {
                          localAttributeValues = null;
                      }
                   } else {
                      localAttributeValues = (String[])
                          values.toArray(new String[values.size()]);
                   }
                } 

                if(localAttributeValues == null) {
                   if(debug.messageEnabled()) {
                      debug.message("DefaultIDPAttributeMapper.getAttribute:"
                      + " user does not have " + localAttribute);
                   }
                   continue;
                }

                attributes.add(
                    getSAMLAttribute(samlAttribute, localAttributeValues));
            }
            return attributes;      
        } catch (WSFederationException sme) {
            debug.error("DefaultIDPAttribute.getAttributes: " +
            "SAML Exception", sme);
            throw new WSFederationException(sme);

        } catch (SessionException se) {
            debug.error("DefaultIDPAttribute.getAttributes: " +
            "SessionException", se);
            throw new WSFederationException(se);
        }

    }

    /**
     * Returns the SAML <code>Attribute</code> object.
     * @param name attribute name.
     * @param values attribute values.
     * @exception WSFederationException if any failure.
     */
    protected Attribute getSAMLAttribute(String name, String[] values)
      throws WSFederationException {
        if(name == null) {
            throw new WSFederationException(bundle.getString(
                "nullInput"));
        }

        List list = new ArrayList();
        if(values != null) {
            for (int i=0; i<values.length; i++) {
                // Make the AttributeValue element 'by hand', since Attribute 
                // constructor below is expecting a list of AttributeValue 
                // elements
                String attrValueString = SAMLUtils.makeStartElementTagXML(
                    "AttributeValue", true, true)
                    + (XMLUtils.escapeSpecialCharacters(values[i]))
                    + SAMLUtils.makeEndElementTagXML("AttributeValue",true);
                list.add(XMLUtils.toDOMDocument(attrValueString,
                                    SAMLUtils.debug).getDocumentElement());
            }
        }
        Attribute attribute = null;
        try {
            attribute = new Attribute(name, WSFederationConstants.CLAIMS_URI, 
                list);
        } catch (SAMLException se) {
            throw new WSFederationException(se);
        }
        return attribute;
    }
}
