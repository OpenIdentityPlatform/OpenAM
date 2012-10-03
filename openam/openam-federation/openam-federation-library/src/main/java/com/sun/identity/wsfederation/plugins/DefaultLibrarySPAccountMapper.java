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
 * $Id: DefaultLibrarySPAccountMapper.java,v 1.6 2009/10/28 23:58:59 exu Exp $
 *
 */


package com.sun.identity.wsfederation.plugins;

import java.security.PrivateKey;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;

import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectStatement;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml2.common.SAML2Constants;

import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.profile.RequestSecurityTokenResponse;
import com.sun.identity.wsfederation.profile.SAML11RequestedSecurityToken;

/**
 * This class <code>DefaultLibrarySPAccountMapper</code> is the default 
 * implementation of the <code>SPAccountMapper</code> that is used
 * to map the <code>SAML</code> protocol objects to the user accounts.
 * at the <code>ServiceProvider</code> side of WSFederation plugin.
 * Custom implementations may extend from this class to override some
 * of these implementations if they choose to do so.
 */
public class DefaultLibrarySPAccountMapper extends DefaultAccountMapper 
       implements SPAccountMapper {

    private PrivateKey decryptionKey = null;

     /**
      * Default constructor
      */
     public DefaultLibrarySPAccountMapper() {
         debug.message("DefaultLibrarySPAccountMapper.constructor: ");
         role = SP;
     }

    /**
     * Returns the user's disntinguished name or the universal ID for the 
     * corresponding  <code>SAML</code> <code>Assertion</code>. This method
     * will be invoked by the <code>WS-Federation</code> framework while 
     * processing the <code>Assertion</code> and retrieves the identity  
     * information. The implementation of this method checks for
     * the user for the corresponding name identifier in the assertion.
     *
     * @param rstr Request Security Token Response.
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param realm realm or the organization name that may be used to find
     *        the user information.
     * @return user's disntinguished name or the universal ID.
     * @exception WSFederationException if any failure.
     */
    public String getIdentity(
        RequestSecurityTokenResponse rstr,
        String hostEntityID,
        String realm
    ) throws WSFederationException {

        if(rstr == null) {
           throw new WSFederationException(bundle.getString(
                 "nullRstr"));
        }

        if(hostEntityID == null) {
           throw new WSFederationException(bundle.getString(
                 "nullHostEntityID"));
        }
        
        if(realm == null) {
           throw new WSFederationException(bundle.getString(
                 "nullRealm"));
        }

        SAML11RequestedSecurityToken rst 
            = (SAML11RequestedSecurityToken)rstr.getRequestedSecurityToken();

        Subject subject = null;
        Assertion assertion = rst.getAssertion();
        Iterator iter = assertion.getStatement().iterator();
        while (iter.hasNext()) {
            Statement statement = (Statement)iter.next();
            if (statement.getStatementType() ==
                Statement.AUTHENTICATION_STATEMENT) {
                subject = ((SubjectStatement)statement).getSubject();
                break;
            }
	}
        NameIdentifier nameID = subject.getNameIdentifier();
 
        String userID = null;
        String format = nameID.getFormat();
        
        String remoteEntityID = 
            WSFederationUtils.getMetaManager().getEntityByTokenIssuerName(
                realm, assertion.getIssuer());
        if(debug.messageEnabled()) {
            debug.message(
                "DefaultLibrarySPAccountMapper.getIdentity(Assertion):" +
                " realm = " + realm + " hostEntityID = " + hostEntityID);  
        }

        try {
            userID = dsProvider.getUserID(realm, getSearchParameters(nameID, 
                realm, hostEntityID, remoteEntityID));
        } catch(DataStoreProviderException dse) {
            debug.error(
               "DefaultLibrarySPAccountMapper.getIdentity(Assertion): " +
               "DataStoreProviderException", dse);
            throw new WSFederationException(dse);
        }

        return userID;
    }

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     * @param realm realm to check the dynamical profile creation attributes.
     * @return true if dynamical profile creation or ignore profile is enabled,
     * false otherwise.
     */
    protected boolean isDynamicalOrIgnoredProfile(String realm) {
        return true;
    }

    /**
     * Returns the attribute name.
     */
    private Set getAttribute(
                AttributeStatement statement,
                String attributeName,
                String realm,
                String hostEntityID)
    {
        if (debug.messageEnabled()) {
            debug.message(
                "DefaultLibrarySPAccountMapper.getAttribute: attribute" +
                "Name =" + attributeName);
        }

        List list = statement.getAttribute();

        for(Iterator iter=list.iterator(); iter.hasNext();) {
            Attribute attribute = (Attribute)iter.next();
            if(!attributeName.equalsIgnoreCase(attribute.getAttributeName())) {
               continue;
            }

            List values = null;
            try {
                values = attribute.getAttributeValue();
            }
            catch (SAMLException se)
            {
                // Just ignore it and carry on - getAttributeValue doesn't
                // really throw an exception - it just says it does
            }
            if(values == null || values.size() == 0) {
               return null;
            }
            Set set = new HashSet();
            set.addAll(values); 
            return set; 
        }
        return null;
    }
}
