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
 * $Id: FSAccountUtils.java,v 1.3 2008/06/25 05:46:40 qcheng Exp $
 *
 */

package com.sun.identity.federation.accountmgmt;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * The utility class contains the helper functions used in 
 * account management.
 */
public class FSAccountUtils {
    
    /**
     * Attribute name to store Account's federation information key
     */    
    public static final String USER_FED_INFO_KEY_ATTR = 
        "iplanet-am-user-federation-info-key";
    
    /**
     * Attribute name to store Account's federation information.
     */    
    public static final String USER_FED_INFO_ATTR = 
        "iplanet-am-user-federation-info";

    private static final String FED_INFO_DELIM = "|";
    
    /**
     * Parses federation information string and put corresponding parts in
     * object fields.
     *
     * @param fedInfoString - String containg federation information.
     * @return Account federation information object.
     * @throws FSAccountMgmtException if <code>fedInfoString</code> cannot be
     *         parsed.
     */
    public static FSAccountFedInfo stringToObject(String fedInfoString)
        throws FSAccountMgmtException {
        FSAccountFedInfo fedInfoObject = null;
        StringTokenizer str = new StringTokenizer(fedInfoString, 
            FED_INFO_DELIM);
        String token;

        fedInfoObject = new FSAccountFedInfo();
        try {
            token = str.nextToken();
            fedInfoObject.setProviderID(token); 
            NameIdentifier localNI = null;
            NameIdentifier remoteNI = null;     
            // Local Name Identifier fields.
            token = str.nextToken();
            if (!token.equalsIgnoreCase("null")) {
                String localName = token;
                String localNameQualifier = "";
                String localNameFormat = "";

                token = str.nextToken();
                if (!token.equalsIgnoreCase("null")) {
                    localNameQualifier = token;
                } 
                
                token = str.nextToken();
                if (!token.equalsIgnoreCase("null")) {
                    localNameFormat = token;
                } 
                try {
                    localNI = new NameIdentifier(localName, 
                        localNameQualifier, localNameFormat);
                } catch (SAMLException se) {
                    FSUtils.debug.error("FSAccountUtils.stringToObject(): "
                        + "SAMLException: ", se);
                    throw new FSAccountMgmtException(se.getMessage());
                }
            } else {
                // just ignore two tokens.
                token = str.nextToken();
                token = str.nextToken();
            }
            fedInfoObject.setLocalNameIdentifier(localNI);
            
            // Remote Name Identifier fields.
            token = str.nextToken();
            if (!token.equalsIgnoreCase("null")) {
                String remoteName = token;
                String remoteNameQualifier = "";
                String remoteNameFormat = "";

                token = str.nextToken();
                if (!token.equalsIgnoreCase("null")) {
                    remoteNameQualifier = token;
                } 
                
                token = str.nextToken();
                if (!token.equalsIgnoreCase("null")) {
                    remoteNameFormat = token;
                } 
                try {
                    remoteNI = new NameIdentifier(remoteName, 
                        remoteNameQualifier, remoteNameFormat);
                } catch (SAMLException se) {
                    FSUtils.debug.error("FSAccountUtils.stringToObject(): "
                        + "SAMLException: ", se);
                    throw new FSAccountMgmtException(se.getMessage());
                }
            } else {
                // just ignore two tokens.
                token = str.nextToken();
                token = str.nextToken();
            }
            fedInfoObject.setRemoteNameIdentifier(remoteNI);
            
            token = str.nextToken();
            if (token.equalsIgnoreCase("IDPRole")) {
                fedInfoObject.setRole(true);
            } else if (token.equalsIgnoreCase("SPRole")) {
                fedInfoObject.setRole(false);
            } else {
                FSUtils.debug.error("FSAccountUtils.stringToObject():" +
                    " You have modified IDP/SP Role" +
                    " in iDS :: set it to IDPRole/SPRole ");
                throw new FSAccountMgmtException(
                    IFSConstants.INVALID_ACT_FED_INFO_IN_IDS, null);
            }
            
            token = str.nextToken();
            if (token.equalsIgnoreCase("Active")) {
                fedInfoObject.activateFedStatus();
            } else if (token.equalsIgnoreCase("InActive")) {
                fedInfoObject.deActivateFedStatus();
            } else {
                FSUtils.debug.error("FSAccountUtils.stringToObject():" +
                    " You have modified Active/InActive in iDS ");
                throw new FSAccountMgmtException(
                    IFSConstants.INVALID_ACT_FED_INFO_IN_IDS, null);
            }
            if (str.hasMoreTokens()) {
                token = str.nextToken();
                if (token != null && 
                    token.equalsIgnoreCase(IFSConstants.AFFILIATED)) 
                {
                    fedInfoObject.setAffiliation(true);
                }
            }
        } catch (NoSuchElementException nsee) {
            FSUtils.debug.error(
                "FSAccountUtils.stringToObject() : NoSuchElementException: ",
                nsee);
            throw new FSAccountMgmtException(nsee.getMessage());
        }
        return fedInfoObject;
    }
    
    /**
     * Returns string equalivent of <code>FSAccountFedInfo</code> object.
     *
     * @return Account Federation information.
     * @param fedInfoObject federation info as an object.
     * @exception FSAccountMgmtException if <code>fedInfoObject</code> cannot
     *            be converted to string.
     */
    public static String objectToInfoString(FSAccountFedInfo fedInfoObject)
        throws FSAccountMgmtException {
        StringBuffer fedInfoSB = new StringBuffer(1000);
        
        fedInfoSB.append(FED_INFO_DELIM);
        fedInfoSB.append(fedInfoObject.getProviderID());
        
        NameIdentifier lni = fedInfoObject.getLocalNameIdentifier();
        NameIdentifier rni = fedInfoObject.getRemoteNameIdentifier();
        
        if (lni == null && rni == null) {
            FSUtils.debug.error("FSAccountUtils.objectToInfoString(): " +
                "both NameIdentifiers are null");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_NAME_IDENTIFIER, null);
        }
        
        if (lni != null) {
            fedInfoSB.append(FED_INFO_DELIM);
            String name = lni.getName();
            if (name != null && name.length() > 0 ){
                fedInfoSB.append(name);
            } else {
                FSUtils.debug.error(
                    "FSAccountUtils.objectToInfoString(): local Name is null");
                throw new FSAccountMgmtException(
                    IFSConstants.NULL_NAME, null);
            }
            
            fedInfoSB.append(FED_INFO_DELIM);
            String nameQual = lni.getNameQualifier();
            if (nameQual != null && nameQual.length() > 0 ){
                fedInfoSB.append(nameQual);
            } else {
                fedInfoSB.append("null");
            }

            fedInfoSB.append(FED_INFO_DELIM);
            String nameFormat = lni.getFormat();
            if (nameFormat != null && nameFormat.length() > 0 ){
                fedInfoSB.append(nameFormat);
            } else {
                fedInfoSB.append("null");
            }
        } else {
            fedInfoSB.append(FED_INFO_DELIM);
            fedInfoSB.append("null");

            fedInfoSB.append(FED_INFO_DELIM);
            fedInfoSB.append("null");

            fedInfoSB.append(FED_INFO_DELIM);
            fedInfoSB.append("null");
        }
        
        if (rni != null) {
            fedInfoSB.append(FED_INFO_DELIM);
            String name = rni.getName();
            if (name != null && name.length() > 0 ){
                fedInfoSB.append(name);
            } else {
                FSUtils.debug.error(
                    "FSAccountUtils.objectToInfoString(): remote Name is null");
                throw new FSAccountMgmtException(
                    IFSConstants.NULL_NAME,null);
            }
            
            fedInfoSB.append(FED_INFO_DELIM);
            String nameQual = rni.getNameQualifier();
            if (nameQual != null && nameQual.length() > 0 ){
                fedInfoSB.append(nameQual);
            } else {
                fedInfoSB.append("null");
            }

            fedInfoSB.append(FED_INFO_DELIM);
            String nameFormat = rni.getFormat();
            if (nameFormat != null && nameFormat.length() > 0 ){
                fedInfoSB.append(nameFormat);
            } else {
                fedInfoSB.append("null");
            }
        } else {
            fedInfoSB.append(FED_INFO_DELIM)
                .append("null").append(FED_INFO_DELIM)
                .append("null").append(FED_INFO_DELIM)
                .append("null");
        }
        
        fedInfoSB.append(FED_INFO_DELIM);
        if (fedInfoObject.isRoleIDP()) {
            fedInfoSB.append("IDPRole");
        } else {
            fedInfoSB.append("SPRole");
        }

        fedInfoSB.append(FED_INFO_DELIM);
        if (fedInfoObject.isFedStatusActive()) {
            fedInfoSB.append("Active");
        } else {
            fedInfoSB.append("InActive");
        }
        
        fedInfoSB.append(FED_INFO_DELIM);
        if(fedInfoObject.getAffiliation()) {
           fedInfoSB.append(IFSConstants.AFFILIATED);
           fedInfoSB.append(FED_INFO_DELIM);
        }
        return fedInfoSB.toString();
    }
    
    /**
     * Returns nameSpace and name of account federation information key. 
     *
     * @return string equalivent of account federation information key.
     * @param fedInfoKeyObject Account federation information key.
     * @exception FSAccountMgmtException if the namespace and/or name 
     *             in the fedInfoKeyObject are null. 
     */
    public static String objectToKeyString(FSAccountFedInfoKey fedInfoKeyObject)
        throws FSAccountMgmtException {
        StringBuffer attrValueSB = new StringBuffer(300);
        String nameSpace = fedInfoKeyObject.getNameSpace();
        if ((nameSpace == null) || (nameSpace.length() <= 0)){
            FSUtils.debug.error("FSAccountUtils.objectToKeyString():" +
                "Invalid Argument : nameSpace is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_NAME_SPACE, null);
        }
        
        String name = fedInfoKeyObject.getName();
        if ((name == null) || (name.length() <= 0)){
            FSUtils.debug.error("FSAccountUtils.objectToKeyString():" +
                "Invalid Argument : name is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_NAME, null);
        }
        
        attrValueSB.append(FED_INFO_DELIM)
                   .append(nameSpace)
                   .append(FED_INFO_DELIM)
                   .append(name)
                   .append(FED_INFO_DELIM);
        String fedKeyValue = attrValueSB.toString();
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSAccountUtils.objectToKeyString(): Value: " + fedKeyValue);
        }
        return fedKeyValue;
    }
    
    /**
     * Creates filter.
     * @param providerID provider id
     * @return filter
     */    
    static String createFilter(String providerID) {
        return FED_INFO_DELIM + providerID + FED_INFO_DELIM;
    }

    /**
     * Creates filter.
     * @param providerID provider id
     * @param nameID name id 
     * @return filter
     */    
    static String createFilter(String providerID, String nameID) {
        String str= FED_INFO_DELIM + providerID + FED_INFO_DELIM 
            + nameID + FED_INFO_DELIM;
        return str;      
    }
    
    /**
     * Creates filter. 
     * @param fedInfoKey federation info key
     * @return filter
     * @exception FSAccountMgmtException if the namespace and/or name in 
     *     the fedInfoKey are null. 
     */    
    static String createFilter(
        FSAccountFedInfoKey fedInfoKey)
        throws FSAccountMgmtException
    {
        String nameSpace = fedInfoKey.getNameSpace();
        if ((nameSpace == null) || (nameSpace.length() <= 0)){
            FSUtils.debug.error("FSAccountUtils.createFilter():" +
                "Invalid Argument : nameSpace is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_NAME_SPACE, null);
        }
        
        String name = fedInfoKey.getName();
        if ((name == null) || (name.length() <= 0)){
            FSUtils.debug.error("FSAccountUtils.createFilter():" +
                "Invalid Argument : name is NULL");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_NAME, null);
        }
        
        return FED_INFO_DELIM + nameSpace + FED_INFO_DELIM
            + name + FED_INFO_DELIM;
    }
}
