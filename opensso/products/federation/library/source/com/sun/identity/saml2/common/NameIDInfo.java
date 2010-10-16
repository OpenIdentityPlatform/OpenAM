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
 * $Id: NameIDInfo.java,v 1.3 2008/06/25 05:47:45 qcheng Exp $
 *
 */


package com.sun.identity.saml2.common;

import java.util.StringTokenizer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.AssertionFactory;

/**
 * This class <code>NameIDInfo</code> represents the user account
 * federation information stored in the repository.
 * The name of attribute name is sun-fm-saml2-nameid-info.
 * This multiple-valued attribute is used to store all information 
 * related to the name identifier, such as IDP, SP entity id, role, etc.
 * Value format for this attribute:
 *  <hosted_entity_id>|<remote_entity_id>|<idp_nameid>|<idp_nameid_qualifier>|
 *  <idp_nameid_format>|<sp_nameid>|<sp_nameid_qualifier>|<hosted_entity_role>|
 * <is_affiliation>
 *       where:
 *       <hosted_entity_id>    : entity id for this hosted entity
 *       <remote_entity_id>    : entity id for the remote entity
 *       <idp_nameid>          : name identifier for the IDP
 *       <idp_nameid_qualifier>: nameid qualifier for the IDP
 *       <idp_nameid_format>   : nameid format for the IDP
 *       <sp_nameid>           : name identifier for the SP/Affiliation
 *       <sp_nameid_qualifier> : nameid qualifier for the SP/Affiliation
 *       <hosted_entity_role>  : value of SPRole, IDPRole or DualRole.
 *       <is_affiliation>      : true for affiliation, false otherwise 
 *       for example:
 *       http://www.sp.com|http://www.idp.com|
 *       vPQyHXLnSWLAVh2BoI3gdUrhanC1|http://www.idp.com|
 *       urn:oasis:names:tc:SAML:2.0:nameid-format:persistent|
 *       g6lD46kMqDGSsFPawoFrw4iNf86C|http://www.sp.com|SPRole|false
 */
public class NameIDInfo {

    private static final String DELIM = "|";
    private static final String NULL = "null";
    private String _hostEntityID = null;
    private String _remoteEntityID = null;
    private String _nameIDValue = null;
    private String _nameQualifier = null;
    private String _format = null;
    private String _spNameIDValue = null;
    private String _spNameQualifier = null;
    private String _role = null;
    private boolean _isAffiliation = false;
    private NameID _nameID = null;

    /**
     * Private contstructor.
     */
    private NameIDInfo() {}

    /**
     * Constructor
     * @param hostEntityID <code>EntityID</code> of the hosted entity. 
     * @param remoteEntityID <code>EntityID</code> of the remote entity.
     * @param nameID <code>NameID</code> object.
     * @param hostEntityRole the role of the hosted entity.
     * @param isAffiliation true if this account federation information
     *                      is an affilation based federation. 
     * @exception SAML2Exception if any of the parameter values are null.
     */
    public NameIDInfo (
        String hostEntityID, 
        String remoteEntityID,
        NameID nameID,
        String hostEntityRole,
        boolean isAffiliation
    ) throws SAML2Exception {

        if(nameID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullNameID"));
        }

        if(hostEntityID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullHostEntityID"));
        }

        if(remoteEntityID == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullRemoteEntityID"));
        }

        if(hostEntityRole == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullEntityRole"));
        }
        
        _hostEntityID = hostEntityID;
        _remoteEntityID = remoteEntityID;
        _nameIDValue = nameID.getValue();
        _nameQualifier = nameID.getNameQualifier();
        _format = nameID.getFormat();
        _spNameIDValue = nameID.getSPProvidedID();  
        _spNameQualifier = nameID.getSPNameQualifier();
        _role = hostEntityRole;
        _isAffiliation = isAffiliation;
        _nameID = nameID;
      
    }

    /**
     * Returns the <code>NameID</code> object.
     * @return the <code>NameID</code> object.
     */
    public NameID getNameID() {
        return _nameID;    
    }

    /**
     * Returns the <code>NameIDInfoKey</code>
     * @return the <code>NameID</code> object.
     * @exception SAML2Exception if any failure.
     */
    public NameIDInfoKey getNameIDInfoKey() throws SAML2Exception {
        return new NameIDInfoKey(_nameIDValue, _hostEntityID, _remoteEntityID);
    }

    /**
     * Returns the <code>NameID</code> value.
     * @return the value of the <code>NameID</code>.
     */
    public String getNameIDValue() {
        return _nameIDValue;
    }

    /**
     * Returns the value of the <code>NameQualifier</code>.
     * @return the value of the <code>NameQualifier</code>.
     */
    public String getNameQualifier() {
        return _nameQualifier;
    }

    /**
     * Returns the value of the <code>NameID</code> format.
     * @return the value of the <code>NameID</code> format.
     */
    public String getFormat() {
        return _format;
    }

    /**
     * Returns the Service Provider provided  <code>NameID</code> value.
     * @return the value of the Service Provider provided <code>NameID</code>.
     */
    public String getSPNameIDValue() {
        return _spNameIDValue;
    }

    /**
     * Returns the Service Provider provided <code>NameID</code>'s
     * Name Qualifier.
     * @return the value of Service Provider provided <code>NameID</code>'s
     * Name Qualifier.
     */
    public String getSPNameQualifier() {
        return _spNameQualifier;
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
     * Returns the role of the host entity. 
     * @return the role of the host entity. 
     */
    public String getHostEntityRole() {
        return _role;
    }

    /**
     * Checks if this is an affiliation based federation.
     * @return true if this is an affiliation based federation.
     */
    public boolean isAffiliation() {
        return _isAffiliation; 
    }

    /**
     * Returns the account federation information key value string that is 
     * stored in the repository.
     * @return the value of the <code>NameIDInfo</code> that is
     *         stored in the repository.
     */
    public String toValueString() {

        StringBuffer sb = new StringBuffer();
        sb.append(_hostEntityID)
          .append(DELIM)
          .append(_remoteEntityID)
          .append(DELIM)
          .append(_nameIDValue)
          .append(DELIM);

        if(_nameQualifier != null && _nameQualifier.length() != 0) {
            sb.append(_nameQualifier);
        } else {
            sb.append(NULL); 
        }
        sb.append(DELIM);

        if(_format != null && _format.length() != 0) {
           sb.append(_format);
        } else {
           sb.append(NULL);
        }
        sb.append(DELIM);

        if(_spNameIDValue != null && _spNameIDValue.length() != 0) {
           sb.append(_spNameIDValue);
        } else {
           sb.append(NULL);
        }
        sb.append(DELIM);

        if(_spNameQualifier != null && _spNameQualifier.length() != 0) {
           sb.append(_spNameQualifier);
        } else {
           sb.append(NULL);
        }
        sb.append(DELIM);

        if(_role != null && _role.length() != 0) {
           sb.append(_role);
        } else {
           sb.append(NULL);
        }
        sb.append(DELIM)
          .append(Boolean.toString(_isAffiliation)); 

        return sb.toString(); 
    }

    /** 
     * Returns the <code>NameIDInfo</code> by parsing the string value.
     * @return the <code>NameIDInfo</code>
     * @exception SAML2Exception if the parsing fails.
     */ 
    public static NameIDInfo parse(String info) throws SAML2Exception {

        if(info == null) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                 "nullNameIDInfo")); 
        }
        
        StringTokenizer st = new StringTokenizer(info, DELIM);
        if(st.countTokens() != 9) {
           throw new SAML2Exception(SAML2Utils.bundle.getString(
                "inValidNameIDInfo"));
        }

        String hostEntityID = st.nextToken(); 
        String remoteEntityID = st.nextToken();
        String nameIDValue = st.nextToken();
        String nameQualifier = st.nextToken();
        String format = st.nextToken();
        String spNameIDValue = st.nextToken();
        String spNameQualifier = st.nextToken();
        String role = st.nextToken();
        boolean isAffiliation = Boolean.valueOf(st.nextToken()).booleanValue(); 

        NameID nameID = AssertionFactory.getInstance().createNameID();
        nameID.setValue(nameIDValue);

        if(nameQualifier != null && !NULL.equals(nameQualifier)) { 
           nameID.setNameQualifier(nameQualifier);
        }

        if(spNameIDValue != null && !NULL.equals(spNameIDValue)) {
           nameID.setSPProvidedID(spNameIDValue);
        }

        if(spNameQualifier != null && !NULL.equals(spNameQualifier)) {
           nameID.setSPNameQualifier(spNameQualifier);
        }

        if(format != null && !NULL.equals(format)) {
           nameID.setFormat(format);
        }

        return new NameIDInfo(hostEntityID, remoteEntityID, 
            nameID, role, isAffiliation);

    }
}

