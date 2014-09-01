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
 * $Id: NameIDInfoKey.java,v 1.2 2008/06/25 05:47:45 qcheng Exp $
 *
 */


package com.sun.identity.saml2.common;

import java.util.StringTokenizer;

/**
 * This class <code>NameIDInfoKey</code> represents the user account
 * federation information key: sun-fm-saml2-nameid-info-key.
 * This multiple-valued attribute is used for searching purpose, in case 
 * of ldap datastore, an equality index need to be created for this 
 * attribute for better search performance.
 *       Value format for this attribute:
 *         <hosted_entity_id>|<remote_entity_id>|<idp_nameid>
 *       where:
 *       <hosted_entity_id>    : entity id for this hosted entity
 *       <remote_entity_id>    : entity id for the remote entity
 *       <idp_nameid>          : name identifier for the IDP
 *       for example:
 *         http://www.sp1.com|http://www.idp1.com|vPQyHXLnSWLAVh2BoI3gdUrhanC1
 * 
 */
public class NameIDInfoKey {

    private static final String DELIM = "|";
    private String _hostEntityID = null;
    private String _remoteEntityID = null;
    private String _nameIDValue = null;

    /**
     * Private contstructor.
     */
    private NameIDInfoKey() {}

    /**
     * Constructor
     * @param nameIDValue randomly generated name identifier value.
     * @param hostEntityID <code>EntityID</code> of the hosted provider. 
     * @param remoteEntityID <code>EntityID</code> of the remote provider.
     * @exception SAML2Exception if any of the parameter values are null.
     */
    public NameIDInfoKey (
        String nameIDValue, 
        String hostEntityID, 
        String remoteEntityID
    ) throws SAML2Exception {

        if(nameIDValue == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullNameIDValue"));
        }

        if(hostEntityID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullHostEntityID"));
        }

        if(remoteEntityID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullRemoteEntityID"));
        }
        
        _nameIDValue = nameIDValue;
        _hostEntityID = hostEntityID;
        _remoteEntityID = remoteEntityID;
    }

    /**
     * Returns the <code>NameID</code> value.
     * @return the value of the <code>NameID</code>.
     */
    public String getNameIDValue() {
        return _nameIDValue;
    }

    /**
     * Returns the <code>EntityID</code> of the hosted provider.
     * @return the <code>EntityID</code> of the hosted provider.
     */
    public String getHostEntityID() {
        return _hostEntityID;
    }

    /**
     * Returns the <code>EntityID</code> of the remote provider.
     * @return the <code>EntityID</code> of the remote provider.
     */
    public String getRemoteEntityID() {
        return _remoteEntityID;
    }

    /**
     * Returns the account federation information key value string that is 
     * stored in the repository.
     * @return the value of the <code>NameIDInfoKey</code> that is
     *         stored in the repository.
     */
    public String toValueString() {
        StringBuffer sb = new StringBuffer();
        sb.append(_hostEntityID)
          .append(DELIM)
          .append(_remoteEntityID)
          .append(DELIM)
          .append(_nameIDValue);

        return sb.toString(); 
    }

    /** 
     * Returns the <code>NameIDInfoKey</code> by parsing the string value.
     * @return the <code>NameIDInfoKey</code>
     * @exception SAML2Exception if the parsing fails.
     */ 
    public static NameIDInfoKey parse(String infoKey) throws SAML2Exception {

        if(infoKey == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullNameIDInfoKey")); 
        }
        
        StringTokenizer st = new StringTokenizer(infoKey, DELIM);
        if(st.countTokens() != 3) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                "inValidNameIDInfoKey"));
        }

        String hostID = st.nextToken();
        String remoteID = st.nextToken();
        String nameID = st.nextToken();
        return new NameIDInfoKey(nameID, hostID, remoteID);
    }
}

