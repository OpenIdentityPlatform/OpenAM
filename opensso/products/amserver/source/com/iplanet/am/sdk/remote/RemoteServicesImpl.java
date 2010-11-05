/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RemoteServicesImpl.java,v 1.10 2009/07/02 20:26:16 hengming Exp $
 *
 */

package com.iplanet.am.sdk.remote;

import com.iplanet.am.sdk.AMEntryExistsException;
import com.iplanet.am.sdk.AMEventManagerException;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.sdk.AMObjectListener;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.common.CallBackHelperBase;
import com.iplanet.am.sdk.common.IComplianceServices;
import com.iplanet.am.sdk.common.IDCTreeServices;
import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.am.sdk.common.MiscUtils;
import com.iplanet.dpro.session.Session;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.SearchControl;
import com.iplanet.ums.SortKey;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class RemoteServicesImpl implements IDirectoryServices {
    protected static final String AM_SDK_DEBUG_FILE = "amProfile_Client";

    protected static final String SDK_SERVICE = "DirectoryManagerIF";

    protected static final String IDREPO_SERVICE = "IdRepoServiceIF";

    protected static final String AMSR_COUNT = "__count";

    protected static final String AMSR_RESULTS = "__results";

    protected static final String AMSR_CODE = "__errorCode";

    protected static final String AMSR_ATTRS = "__attrs";

    protected static Debug debug = Debug.getInstance(AM_SDK_DEBUG_FILE);

    private static RemoteServicesImpl instance;

    // Protected Members
    private SOAPClient client;

    protected IDCTreeServices dcTreeServicesImpl;

    protected IComplianceServices complianceServicesImpl;

    protected CallBackHelperBase callBackHelperBase;

    protected static Debug getDebug() {
        return debug;
    }

    public RemoteServicesImpl() {
        client = new SOAPClient(SDK_SERVICE);
        dcTreeServicesImpl = new DCTreeServicesImpl(client);
        complianceServicesImpl = new ComplianceServicesImpl(client);
        callBackHelperBase = new CallBackHelperBase();
    }

    protected SOAPClient getSOAPClient() {
        return client;
    }

    protected static synchronized IDirectoryServices getInstance() {
        if (instance == null) {
            getDebug().message("RemoteServicesImpl.getInstance(): " 
                    + "Creating a new Instance of RemoteServicesImpl()");
            instance = new RemoteServicesImpl();
        }
        return instance;
    }

    public IDCTreeServices getDCTreeServicesImpl() {
        return dcTreeServicesImpl;
    }

    public IComplianceServices getComplianceServicesImpl() {
        return complianceServicesImpl;
    }

    /**
     * Checks if the entry exists in the directory.
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            The DN of the entry that needs to be checked
     * @return true if the entryDN exists in the directory, false otherwise
     */
    public boolean doesEntryExists(SSOToken token, String entryDN) {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN };
            Boolean res = ((Boolean) client.send(client.encodeMessage(
                    "doesEntryExists", objs), 
                    Session.getLBCookie(tokenID), null));
            return res.booleanValue();
        } catch (RemoteException rex) {
            return false;
        } catch (Exception ex) {
            return false;
        }

    }

    /**
     * Gets the type of the object given its DN.
     * 
     * @param token
     *            token a valid SSOToken
     * @param dn
     *            DN of the object whose type is to be known.
     * 
     * @throws AMException
     *             if the data store is unavailable or if the object type is
     *             unknown
     * @throws SSOException
     *             if ssoToken is invalid or expired.
     */
    public int getObjectType(SSOToken token, String dn) throws AMException,
            SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, dn };
            Integer res = ((Integer) client.send(client.encodeMessage(
                    "getObjectType", objs), 
                    Session.getLBCookie(tokenID), null));
            return res.intValue();
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getObjectType: dn=" + dn +
                    ";  AMRemoteException caught exception=", amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getObjectType: dn=" + dn +
                    ";  AMRemoteException caught exception=", rex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                "RemoteServicesImpl.getObjectType: dn=" + dn +
                ";  caught SSOException=", ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getObjectType: dn=" + dn +
                    ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.remote.RemoteServicesImplIF#
     *      getObjectType(java.lang.String, java.lang.String, Map)
     */
    public int getObjectType(SSOToken token, String dn, Map cachedAttributes)
            throws AMException, SSOException {
        return (getObjectType(token, dn));
    }

    /**
     * Gets the attributes for this entryDN from the corresponding DC Tree node.
     * The attributes are fetched only for Organization entries in DC tree mode.
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            dn of the entry
     * @param attrNames
     *            attributes name
     * @param byteValues
     *            <code>true</code> if in bytes
     * @param objectType
     *            the object type of entryDN.
     * @return an AttrSet of values or null if not found
     * @throws AMException
     *             if error encountered in fetching the DC node attributes.
     */
    public Map getDCTreeAttributes(SSOToken token, String entryDN,
            Set attrNames, boolean byteValues, int objectType)
            throws AMException, SSOException {
        // Object []
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    attrNames, Boolean.valueOf(byteValues), 
                    new Integer(objectType)};
            return ((Map) client.send(client.encodeMessage(
                    "getDCTreeAttributes", objs), 
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getDCTreeAttributes: entryDN=" +
                    entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getDCTreeAttributes: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.getDCTreeAttributes: caught " +
                    "SSOException=", ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getDCTreeAttributes: entryDN="
                    + entryDN + ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    public Map getAttributes(SSOToken token, String entryDN, int profileType)
            throws AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    new Integer(profileType) };
            Map map = (Map) client.send(client.encodeMessage("getAttributes1",
                    objs), Session.getLBCookie(tokenID), null);
            AMHashMap res = new AMHashMap();
            res.copy(map);
            return res;

        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributes: entryDN=" +
                    entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributes: caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributes: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributes: entryDN=" +
                    entryDN + ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            int profileType) throws AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    attrNames, new Integer(profileType) };
            Map map = (Map) client.send(client.encodeMessage("getAttributes2",
                    objs), Session.getLBCookie(tokenID), null);
            AMHashMap res = new AMHashMap();
            res.copy(map);
            return res;
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributes 2: entryDN=" +
                    entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributes: caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributes: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributes2: entryDN=" +
                    entryDN + ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    // Note: This API will not be implemented the Cached impl of the interface.
    public Map getAttributesFromDS(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException, SSOException {
        return getAttributes(token, entryDN, attrNames, profileType);
    }

    public Map getAttributesByteValues(SSOToken token, String entryDN,
            int profileType) throws AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN, new Integer(profileType) };
            return ((Map) client.send(client.encodeMessage(
                    "getAttributesByteValues1", objs), 
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributesByteValues: entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.getAttributesByteValues: " 
                    + "caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributesByteValues: caught "
                    + "SSOException=", ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributesByteValues: entryDN="
                    + entryDN + ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    public Map getAttributesByteValues(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    attrNames, new Integer(profileType) };
            return ((Map) client.send(client.encodeMessage(
                    "getAttributesByteValues2", objs), 
                    Session.getLBCookie(tokenID), null));

        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributesByteValues2: entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.getAttributesByteValues: " 
                    + "caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error("RemoteServicesImpl.getAttributesByteValues: " 
                    + "caught SSOException=", ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributesByteValues2: entryDN="
                    + entryDN + ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Gets all attributes corresponding to the entryDN. This method obtains the
     * DC Tree node attributes and also performs compliance related verification
     * checks in compliance mode. Note: In compliance mode you can skip the
     * compliance checks by setting ignoreCompliance to "false".
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the DN of the entry whose attributes need to retrieved
     * @param ignoreCompliance
     *            a boolean value specificying if compliance related entries
     *            need to ignored or not. Ignored if true.
     * @return a Map containing attribute names as keys and Set of values
     *         corresponding to each key.
     * @throws AMException
     *             if an error is encountered in fetching the attributes
     */
    public Map getAttributes(SSOToken token, String entryDN,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMException, SSOException {

        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    Boolean.valueOf(ignoreCompliance), 
                    Boolean.valueOf(byteValues),
                    new Integer(profileType) };
            Map map = (Map) client.send(client.encodeMessage("getAttributes3",
                    objs), Session.getLBCookie(tokenID), null);
            AMHashMap res = new AMHashMap();
            res.copy(map);
            return res;
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributes 3: entryDN=" +
                    entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributes: caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributes: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributes3: entryDN=" +
                    entryDN + ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Gets the specific attributes corresponding to the entryDN. This method
     * obtains the DC Tree node attributes and also performs compliance related
     * verification checks in compliance mode. Note: In compliance mode you can
     * skip the compliance checks by setting ignoreCompliance to "false".
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the DN of the entry whose attributes need to retrieved
     * @param attrNames
     *            a Set of names of the attributes that need to be retrieved.
     *            The attrNames should not be null.
     * @param ignoreCompliance
     *            a boolean value specificying if compliance related entries
     *            need to ignored or not. Ignored if true.
     * @return a Map containing attribute names as keys and Set of values
     *         corresponding to each key.
     * @throws AMException
     *             if an error is encountered in fetching the attributes
     */
    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            boolean ignoreCompliance, boolean byteValues, int profileType)
            throws AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    attrNames, Boolean.valueOf(ignoreCompliance),
                    Boolean.valueOf(byteValues), new Integer(profileType) };
            Map map = (Map) client.send(client.encodeMessage("getAttributes4",
                    objs), Session.getLBCookie(tokenID), null);
            AMHashMap res = new AMHashMap();
            res.copy(map);
            return res;
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributes 4: entryDN=" +
                    entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributes: caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.getAttributes: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributes4: entryDN=" +
                    entryDN + ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }
    }

    public String getOrgSearchFilter(String entryDN) {
        try {
            Object[] objs = { entryDN };
            return ((String) client.send(client.encodeMessage(
                    "getOrgSearchFilter", objs), null, null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getOrgSearchFilter: entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            return ("");
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getOrgSearchFilter: caught exception=",
                    rex);
            return ("");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getOrgSearchFilter: entryDN=" + entryDN +
                    ";  caught exception=", ex);
            }
            return ("");
        }

    }

    /**
     * Gets the Organization DN for the specified entryDN. If the entry itself
     * is an org, then same DN is returned.
     * <p>
     * <b>NOTE:</b> This method will involve serveral directory searches, hence
     * be cautious of Performance hit
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the entry whose parent Organization is to be obtained
     * @return the DN String of the parent Organization
     * @throws AMException
     *             if an error occured while obtaining the parent Organization
     */
    public String getOrganizationDN(SSOToken token, String entryDN)
            throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN };
            return ((String) client.send(client.encodeMessage(
                    "getOrganizationDN", objs), 
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getOrganizationDN: entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getOrganizationDN: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error("RemoteServicesImpl.getOrganizationDN: caught " 
                    + "SSOException=", ssoe);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getOrganizationDN: entryDN=" +
                     entryDN +  ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Gets the Organization DN for the specified entryDN. If the entry itself
     * is an org, then same DN is returned.
     * 
     * @param token
     *            a valid SSOToken
     * @param entryDN
     *            the entry whose parent Organization is to be obtained
     * @param childDN
     *            the immediate entry whose parent Organization is to be
     *            obtained
     * @return the DN String of the parent Organization
     * @throws AMException
     *             if an error occured while obtaining the parent Organization
     */
    public String verifyAndGetOrgDN(SSOToken token, String entryDN,
            String childDN) throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN, childDN };
            return ((String) client.send(client.encodeMessage(
                    "verifyAndGetOrgDN", objs), 
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.verifyAndGetOrgDN: entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.verifyAndGetOrgDN: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error("RemoteServicesImpl.verifyAndGetOrgDN: caught " 
                    + "SSOException=", ssoe);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.verifyAndGetOrgDN: entryDN=" +
                     entryDN +  ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Returns attributes from an external data store.
     * 
     * @param token
     *            Single sign on token of user
     * @param entryDN
     *            DN of the entry user is trying to read
     * @param attrNames
     *            Set of attributes to be read
     * @param profileType
     *            Integer determining the type of profile being read
     * @return A Map of attribute-value pairs
     * @throws AMException
     *             if an error occurs when trying to read external datastore
     */
    public Map getExternalAttributes(SSOToken token, String entryDN,
            Set attrNames, int profileType) throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    attrNames, new Integer(profileType) };
            return ((Map) client.send(client.encodeMessage(
                    "getExternalAttributes", objs), 
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getExternalAttributes: entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getExternalAttributes: caught " 
                    + "exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getExternalAttributes: entryDN=" +
                     entryDN +  ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Adds or remove static group DN to or from member attribute
     * 'iplanet-am-static-group-dn'
     * 
     * @param token
     *            SSOToken
     * @param members
     *            set of user DN's
     * @param staticGroupDN
     *            DN of the static group
     * @param toAdd
     *            true to add, false to remove
     * @throws AMException
     *             if there is an internal problem with AM Store.
     */
    public void updateUserAttribute(SSOToken token, Set members,
            String staticGroupDN, boolean toAdd) throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, members,
                    staticGroupDN, Boolean.valueOf(toAdd) };
            client.send(client.encodeMessage("updateUserAttribute", objs),
                    Session.getLBCookie(tokenID), null);
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.updateUserAttr: staticGroupDN="
                    + staticGroupDN +
                    ";  AMRemoteException caught exception=", amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.updateUserAttribute: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.updateUserAttribute: staticGroupDN="
                    + staticGroupDN +  ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Create an entry in the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryName
     *            name of the entry (naming value), e.g. "sun.com", "manager"
     * @param objectType
     *            Profile Type, ORGANIZATION, AMObject.ROLE, AMObject.USER, etc.
     * @param parentDN
     *            the parent DN
     * @param attributes
     *            the initial attribute set for creation
     */
    public void createEntry(SSOToken token, String entryName, int objectType,
            String parentDN, Map attributes) throws AMEntryExistsException,
            AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryName,
                    new Integer(objectType), parentDN, attributes };
            client.send(client.encodeMessage("createEntry", objs), 
        	    Session.getLBCookie(tokenID), null);

        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.createEntry: entryName="
                    + entryName + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.createEntry: caught " + "exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.createEntry: entryName=" +
                     entryName+  ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Remove an entry from the directory.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            dn of the profile to be removed
     * @param objectType
     *            profile type
     * @param recursive
     *            if true, remove all sub entries & the object
     * @param softDelete
     *            Used to let pre/post callback plugins know that this delete is
     *            either a soft delete (marked for deletion) or a purge/hard
     *            delete itself, otherwise, remove the object only
     */
    public void removeEntry(SSOToken token, String entryDN, int objectType,
            boolean recursive, boolean softDelete) throws AMException,
            SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    new Integer(objectType), Boolean.valueOf(recursive),
                    Boolean.valueOf(softDelete) };
            client.send(client.encodeMessage("removeEntry", objs), 
        	    Session.getLBCookie(tokenID), null);
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.removeEntry: entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.removeEntry: caught " + "exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.removeEntry: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.removeEntry: entryDN=" +
                     entryDN +  ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Remove group admin role
     * 
     * @param token
     *            SSOToken of the caller
     * @param dn
     *            group DN
     * @param recursive
     *            true to delete all admin roles for all sub groups or sub
     *            people container
     */
    public void removeAdminRole(SSOToken token, String dn, boolean recursive)
            throws SSOException, AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, dn, Boolean.valueOf(recursive) };
            client.send(client.encodeMessage("removeAdminRole", objs), 
        	    Session.getLBCookie(tokenID),
        	    null);
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.removeAdminRole: dn="
                    + dn + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.removeAdminRole: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.removeAdminRole: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.removeAdminRole: dn=" +
                     dn  + ";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Searches the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the entry to start the search with
     * @param searchFilter
     *            search filter
     * @param searchScope
     *            search scope, BASE, ONELEVEL or SUBTREE
     * @return Set set of matching DNs
     */
    public Set search(SSOToken token, String entryDN, String searchFilter,
            int searchScope) throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    searchFilter, new Integer(searchScope) };
            return ((Set) client.send(client.encodeMessage("search1", objs),
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.search: entryDN" + entryDN
                    + ";  AMRemoteException caught exception=", amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.search: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.search: entryDN=" +
                     entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }
    }

    /**
     * Search the Directory
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the entry to start the search with
     * @param searchFilter
     *            search filter
     * @param searchControl
     *            search control defining the VLV indexes and search scope
     * @param attrNames
     *            attributes name
     * @return Set of matching DNs
     */
    public AMSearchResults search(SSOToken token, String entryDN,
            String searchFilter, SearchControl searchControl,
            String attrNames[]) throws AMException {
        try {
            SortKey[] keys = searchControl.getSortKeys();
            LinkedList sortKeys = new LinkedList();
            for (int i = 0; (keys != null) && (i < keys.length); i++) {
                if (keys[i].reverse) {
                    sortKeys.add("true:" + keys[i].attributeName);
                } else {
                    // Using "fals" instead of "false" so that it
                    // has 4 characters as "true", hence easy to
                    // reconstruct SortKey
                    sortKeys.add("fals:" + keys[i].attributeName);
                }
            }

            int[] vlvRange = searchControl.getVLVRange();
            if (vlvRange == null) {
                vlvRange = new int[3];
            }
            Set attrNamesSet = MiscUtils.stringArrayToSet(attrNames);
        	String tokenID = token.getTokenID().toString();
            Object[] objs = {
            		tokenID,
                    entryDN,
                    searchFilter,
                    sortKeys,
                    new Integer(vlvRange[0]),
                    new Integer(vlvRange[1]),
                    new Integer(vlvRange[2]),
                    searchControl.getVLVJumpTo(),
                    new Integer(searchControl.getTimeOut()),
                    new Integer(searchControl.getMaxResults()),
                    new Integer(searchControl.getSearchScope()),
                    Boolean.valueOf(
                        searchControl.isGetAllReturnAttributesEnabled()),
                    attrNamesSet };
            Map results = (Map) client.send(client.encodeMessage("search3",
                    objs), Session.getLBCookie(tokenID), null);

            String cString = (String) results.remove(AMSR_COUNT);
            Set dns = (Set) results.remove(AMSR_RESULTS);
            String eString = (String) results.remove(AMSR_CODE);
            int count = 0, errorCode = 0;
            try {
                count = Integer.parseInt(cString);
                errorCode = Integer.parseInt(eString);
            } catch (NumberFormatException nfe) {
                getDebug().error(
                        "RemoteServicesImpl.search: caught number "
                                + "format error", nfe);
            }
            return (new AMSearchResults(count, dns, errorCode, results));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.search2 : entryDN" + entryDN +
                    ";  AMRemoteException caught exception=", amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.search: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.search2 : entryDN=" +
                     entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }
    }

    /**
     * Get members for roles, dynamic group or static group
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the role or group
     * @param objectType
     *            objectType of the target object, AMObject.ROLE or
     *            AMObject.GROUP
     * @return Set Member DNs
     */
    public Set getMembers(SSOToken token, String entryDN, int objectType)
            throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN, new Integer(objectType) };
            return ((Set) client.send(client.encodeMessage("getMembers", objs),
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getMembers : entryDN" + entryDN
                    + ";  AMRemoteException caught exception=", amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getMembers: caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getMembers : entryDN=" +
                     entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Renames an entry. Currently used for only user renaming
     * 
     * @param token
     *            the sso token
     * @param objectType
     *            the type of entry
     * @param entryDN
     *            the entry DN
     * @param newName
     *            the new name (i.e., if RDN is cn=John, the value passed should
     *            be "John"
     * @param deleteOldName
     *            if true the old name is deleted otherwise it is retained.
     * @return new <code>DN</code> of the renamed entry
     * @throws AMException
     *             if the operation was not successful
     */
    public String renameEntry(SSOToken token, int objectType, String entryDN,
            String newName, boolean deleteOldName) throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = {tokenID, new Integer(objectType), entryDN, newName,
                    Boolean.valueOf(deleteOldName) };
            return ((String) client.send(client.encodeMessage("renameEntry",
                    objs), Session.getLBCookie(tokenID), null));

        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.renameEntry : entryDN" + entryDN
                    + ";  AMRemoteException caught exception=", amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.renameEntry: caught " + "exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.renameEntry : entryDN=" +
                     entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Method Set the attributes of an entry.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param objectType
     *            profile type
     * @param stringAttributes
     *            attributes to be set
     * @param byteAttributes
     *            attributes to be set
     * @param isAdd
     *            <code>true</code> add to existing value;
     *            otherwise replace existing value
     */
    public void setAttributes(SSOToken token, String entryDN, int objectType,
            Map stringAttributes, Map byteAttributes, boolean isAdd)
            throws AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    new Integer(objectType), stringAttributes, byteAttributes,
                    Boolean.valueOf(isAdd) };
            client.send(client.encodeMessage("setAttributes", objs), 
        	    Session.getLBCookie(tokenID), null);
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.setAttributes : entryDN" +
                    entryDN + ";  AMRemoteException caught exception=" ,
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.setAttributes: caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.setAttributes: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.setAttributes : entryDN=" +
                     entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Changes user password.
     * 
     * @param token Single sign on token
     * @param entryDN DN of the profile whose template is to be set
     * @param attrName password attribute name
     * @param oldPassword old password
     * @param newPassword new password
     * @throws AMException if an error occurs when changing user password
     * @throws SSOException If user's single sign on token is invalid.
     */
    public void changePassword(SSOToken token, String entryDN, String attrName,
        String oldPassword, String newPassword)
        throws AMException, SSOException {

        try {
            String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN, attrName, oldPassword,
                newPassword };
            client.send(client.encodeMessage("changePassword", objs), 
        	    Session.getLBCookie(tokenID), null);
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.changePassword : entryDN" +
                    entryDN + ";  AMRemoteException caught exception=" ,
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                "RemoteServicesImpl.changePassword: caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                "RemoteServicesImpl.changePassword: caught SSOException=",
                ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.changePassword : entryDN=" +
                     entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Returns an array containing the dynamic group's scope, base dn, and
     * filter.
     */
    public String[] getGroupFilterAndScope(SSOToken token, String entryDN,
            int profileType) throws SSOException, AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN, new Integer(profileType) };
            LinkedList list = (LinkedList) client.send(client.encodeMessage(
                    "getGroupFilterAndScope", objs), 
                    Session.getLBCookie(tokenID), null);
            String[] array = new String[list.size()];
            list.toArray(array);
            return array;
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getGroupFilterAndScope : entryDN"
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.getGroupFilterAndScope: " 
                    + "caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.getGroupFilterAndScope: caught "
                    + "SSOException=", ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getGroupFilterAndScope : entryDN="
                    + entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Sets the filter for a dynamic group in the datastore.
     * 
     * @param token
     * @param entryDN
     * @param filter
     * @throws AMException
     * @throws SSOException
     */
    public void setGroupFilter(SSOToken token, String entryDN, String filter)
            throws AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN, filter };
            client.send(client.encodeMessage("setGroupFilter", objs), 
        	    Session.getLBCookie(tokenID), null);
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.setGroupFilter : entryDN" +
                    entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.setGroupFilter: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.setGroupFilter: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.setGroupFilter : entryDN="
                    + entryDN +";  caught exception=", ex);
            }
            getDebug().error(
                    "RemoteServicesImpl.setGroupFilter: caught exception=", ex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Modify member ship for role or static group
     * 
     * @param token
     *            SSOToken
     * @param members
     *            Set of member DN to be operated
     * @param target
     *            DN of the target object to add the member
     * @param type
     *            type of the target object, AMObject.ROLE or AMObject.GROUP
     * @param operation
     *            type of operation, ADD_MEMBER or REMOVE_MEMBER
     */
    public void modifyMemberShip(SSOToken token, Set members, String target,
            int type, int operation) throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, members, target,
                    new Integer(type), new Integer(operation) };
            client.send(client.encodeMessage("modifyMemberShip", objs), 
        	    Session.getLBCookie(tokenID), null);

        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.modifyMemberShip : target"
                    + target + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.modifyMemberShip: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.modifyMemberShip : target="
                    + target +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Get registered services for an organization
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the org
     * @return Set set of service names
     */
    public Set getRegisteredServiceNames(SSOToken token, String entryDN)
            throws AMException {
        try {
            Object[] objs = { null, entryDN };
            return ((Set) client.send(client.encodeMessage(
                    "getRegisteredServiceNames", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getRegisteredServiceNames : entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getRegisteredServiceNames: caught "
                    + "exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getRegisteredServiceNames : entryDN="
                    + entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Register a service for an org or org unit policy to a profile
     * 
     * @param token
     *            token
     * @param orgDN
     *            DN of the org
     * @param serviceName
     *            Service Name
     */
    public void registerService(SSOToken token, String orgDN, String 
            serviceName) throws AMException, SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, orgDN, serviceName };
            client.send(client.encodeMessage("registerService", objs), 
        	    Session.getLBCookie(tokenID), null);
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.registerService : orgDN="
                    + orgDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.registerService: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error(
                    "RemoteServicesImpl.registerService: caught SSOException=",
                    ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.registerService : orgDN="
                    + orgDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Un register service for a AMro profile.
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose service is to be removed
     * @param objectType
     *            profile type
     * @param serviceName
     *            Service Name
     * @param type
     *            Template type
     */
    public void unRegisterService(SSOToken token, String entryDN,
            int objectType, String serviceName, int type) throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    new Integer(objectType), serviceName, new Integer(type) };
            client.send(client.encodeMessage("unRegisterService", objs), 
        	    Session.getLBCookie(tokenID), null);
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.unRegisterService : entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.unRegisterService: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.unRegisterService : entryDN="
                    + entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Get the AMTemplate DN (COSTemplateDN)
     * 
     * @param token
     *            SSOToken
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param serviceName
     *            Service Name
     * @param type
     *            the template type, AMTemplate.DYNAMIC_TEMPLATE
     * @return String DN of the AMTemplate
     */
    public String getAMTemplateDN(SSOToken token, String entryDN,
            int objectType, String serviceName, int type) throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    new Integer(objectType), serviceName, new Integer(type) };
            return ((String) client.send(client.encodeMessage(
                    "getAMTemplateDN", objs), 
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAMTemplateDN : entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getAMTemplateDN: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAMTemplateDN : entryDN="
                    + entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Create an AMTemplate (COSTemplate)
     * 
     * @param token
     *            token
     * @param entryDN
     *            DN of the profile whose template is to be set
     * @param objectType
     *            object type
     * @param serviceName
     *            Service Name
     * @param attributes
     *            attributes to be set
     * @param priority
     *            template priority
     * @return String DN of the newly created template
     */
    public String createAMTemplate(SSOToken token, String entryDN,
            int objectType, String serviceName, Map attributes, int priority)
            throws AMException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID, entryDN,
                    new Integer(objectType), serviceName, attributes,
                    new Integer(priority) };
            return ((String) client.send(client.encodeMessage(
                    "createAMTemplate", objs), 
                    Session.getLBCookie(tokenID), null));

        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.createAMTemplate : entryDN="
                    + entryDN + ";  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.createAMTemplate: caught exception=",
                    rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.createAMTemplate : entryDN="
                    + entryDN +";  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    /**
     * Gets the naming attribute after reading it from the corresponding
     * creation template. If not found, a default value will be used
     */
    public String getNamingAttribute(int objectType, String orgDN) {
        try {
            Object[] objs = { new Integer(objectType), orgDN };
            return ((String) client.send(client.encodeMessage("getNamingAttr",
                    objs), null, null));
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getNamingAttr: caught exception=", rex);
            return null;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getNamingAttribute : orgDN="
                    + orgDN+";  caught exception=", ex);
            }
            return null;
        }

    }

    /**
     * Get the name of the creation template to use for specified object type.
     */
    public String getCreationTemplateName(int objectType) {
        try {
            Object[] objs = { new Integer(objectType) };
            return ((String) client.send(client.encodeMessage(
                    "getCreationTemplateName", objs), null, null));
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.getCreationTemplateName: " 
                    + "caught exception=", rex);
            return null;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getCreationTemplateName : "
                    + "  caught exception=", ex);
            }
            return null;
        }

    }

    public String getObjectClass(int objectType) {
        try {
            Object[] objs = { new Integer(objectType) };
            return ((String) client.send(client.encodeMessage(
                    "getObjectClassFromDS", objs), null, null));
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.getObjectClassFromDS: " 
                    + "caught exception=", rex);
            return null;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getObjectClass : "
                    + "  caught exception=", ex);
            }
            return null;
        }

    }

    /**
     * Returns the set of attributes (both optional and required) needed for an
     * objectclass based on the LDAP schema
     * 
     * @param objectclass
     * @return Set of the attributes for the  object class
     */
    public Set getAttributesForSchema(String objectclass) {
        try {
            Object[] objs = { objectclass };
            return ((Set) client.send(client.encodeMessage(
                    "getAttributesForSchema", objs), null, null));
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.getAttributesForSchema: " +
                    "caught exception=", rex);
            return Collections.EMPTY_SET;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getAttributesForSchema : "
                    + "  caught exception=", ex);
            }
            return Collections.EMPTY_SET;
        }

    }

    public String getSearchFilterFromTemplate(int objectType, String orgDN,
            String searchTemplateName) {
        try {
            Object[] objs = { new Integer(objectType), orgDN };
            return ((String) client.send(client.encodeMessage(
                    "getSearchFilterFromTemplate", objs), null, null));
        } catch (RemoteException rex) {
            getDebug().error(
                    "RemoteServicesImpl.getSearchFilterFromTemplate: caught "
                            + "exception=", rex);
            return null;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getSearchFilterFromTemplate : orgDN="
                    + orgDN+";  caught exception=", ex);
            }
            return null;
        }

    }

    public Set getTopLevelContainers(SSOToken token) throws AMException,
            SSOException {
        try {
        	String tokenID = token.getTokenID().toString();
            Object[] objs = { tokenID };
            return ((Set) client.send(client.encodeMessage(
                    "getTopLevelContainers", objs), 
                    Session.getLBCookie(tokenID), null));
        } catch (AMRemoteException amrex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getTopLevelContainers : "
                    + "  AMRemoteException caught exception=",
                    amrex);
            }
            throw convertException(amrex);
        } catch (RemoteException rex) {
            getDebug().error("RemoteServicesImpl.getTopLevelContainers: " 
                    + "caught exception=", rex);
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        } catch (SSOException ssoe) {
            getDebug().error("RemoteServicesImpl.getTopLevelContainers: caught "
                    + "SSOException=", ssoe);
            throw ssoe;
        } catch (Exception ex) {
            if (getDebug().messageEnabled()) {
                getDebug().message(
                    "RemoteServicesImpl.getTopLevelContainers : "
                    + "  caught exception=", ex);
            }
            throw new AMException(AMSDKBundle.getString("1000"), "1000");
        }

    }

    protected static AMException convertException(AMRemoteException amrx) {
        return new AMException(amrx.getMessage(), amrx.getErrorCode(), amrx
                .getMessageArgs());
    }

    public void addListener(SSOToken token, AMObjectListener listener,
            Map configMap) throws AMEventManagerException {
        EventListener.getInstance().addListener(token, listener);
    }

}
