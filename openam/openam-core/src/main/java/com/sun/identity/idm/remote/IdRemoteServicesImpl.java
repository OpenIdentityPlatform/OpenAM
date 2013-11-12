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
 * $Id: IdRemoteServicesImpl.java,v 1.23 2010/01/06 01:58:26 veiming Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.idm.remote;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.dpro.session.Session;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdServices;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.jaxrpc.SMSJAXRPCObject;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.Callback;

/*
 * Class that implements the remote services that are needed for IdRepo.
 */
public class IdRemoteServicesImpl implements IdServices {

    // TODO: Use a different JAX-RPC interface
    protected static final String SDK_SERVICE = "DirectoryManagerIF";

    public static final String AMSR_RESULTS = "__results";

    public static final String AMSR_CODE = "__errorCode";

    public static final String AMSR_ATTRS = "__attrs";

    private SOAPClient client;

    private static Debug debug = Debug.getInstance("amIdmClient");

    private static boolean sendRestrictionContext;

    private static IdServices instance;
    
    protected IdRemoteServicesImpl() {
        /*
         * Here we set sendRestrictionContext by checking a property
         * in agent configuration file and also if the server is
         * sending back the right version string indicating that it
         * supports cookie hijacking.
         */
        String euc = SystemProperties.get(Constants.IS_ENABLE_UNIQUE_COOKIE);
        if ((euc != null) && (euc.length() > 0)) {
            sendRestrictionContext = Boolean.valueOf(euc).booleanValue();
            if (debug.messageEnabled()) {
                debug.message("IdRemoteServicesImpl.<init>: " +
                    Constants.IS_ENABLE_UNIQUE_COOKIE +
                    " = " +sendRestrictionContext);
            }
        }
        
        if (sendRestrictionContext) {
            SMSJAXRPCObject smsObj = new SMSJAXRPCObject();
            SSOToken appToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            try {
                Map map = smsObj.read(appToken,
                    "o=" + SMSJAXRPCObject.AMJAXRPCVERSIONSTR);
                euc = (String)map.get(SMSJAXRPCObject.AMJAXRPCVERSIONSTR);
                if ((euc != null) && (euc.length() > 0)) {
                    int version = Integer.valueOf(euc).intValue();
                    sendRestrictionContext = (version > 9);
                }
            } catch (NumberFormatException e) {
                debug.warning("IdRemoteServicesImpl.<init>.", e);
            } catch (SSOException e) {
                debug.warning("IdRemoteServicesImpl.<init>.", e);
            } catch (SMSException e) {
                debug.warning("IdRemoteServicesImpl.<init>.", e);
            }
        }

        if (debug.messageEnabled()) {
            debug.message(
                "IdRemoteServicesImpl.<init>: sendRestrictionContext = " +
                    sendRestrictionContext);
       }
       
       // Initialize JAX-RPC SOAP client
       client = new SOAPClient(SDK_SERVICE);
   }

    protected static Debug getDebug() {
        return debug;
    }

    protected static synchronized IdServices getInstance() {
        if (instance == null) {
            getDebug().message("IdRemoteServicesImpl.getInstance(): "
                    + "Creating new Instance of IdRemoteServicesImpl()");
            instance = new IdRemoteServicesImpl();
        }
        return instance;
    }

    protected void processException(Exception exception) 
        throws SSOException, IdRepoException {
        
        if (exception instanceof SSOException) {                  
            throw (SSOException) exception;                              
        } else if (exception instanceof IdRepoException) { 
            throw (IdRepoException) exception;
        } else {
            if (debug.errorEnabled()) {
                getDebug().error(
                    "IdRemoteServicesImpl.processException(): " +
                    "caught remote/un-known exception - ", exception);
            }
            throw new IdRepoException(AMSDKBundle.getString("1000"), "1000");
        }                               
    }
    
    /**
     * Returns <code>true</code> if the data store has successfully
     * authenticated the identity with the provided credentials. In case the
     * data store requires additional credentials, the list would be returned
     * via the <code>IdRepoException</code> exception.
     * 
     * @param orgName
     *            realm name to which the identity would be authenticated
     * @param credentials
     *            Array of callback objects containing information such as
     *            username and password.
     * 
     * @return <code>true</code> if data store authenticates the identity;
     *         else <code>false</code>
     */
    public boolean authenticate(String orgName, Callback[] credentials) {
        if (getDebug().messageEnabled()) {
            getDebug().message("IdRemoteServicesImpl.authenticate(): "
                    + " Not supported for remote clients");
        }

        // Not supported for remote
        return false;
    }

    public AMIdentity create(SSOToken token, IdType type, String name,
            Map attrMap, String amOrgName) throws IdRepoException, SSOException
    {
        String univid = null;
        
        try {            
            Object[] objs = { getTokenString(token), type.getName(),
                    name, attrMap, amOrgName };
            univid = (String) client.send(client.encodeMessage(
                    "create_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (Exception ex) {
            processException(ex);
        }
        
        return IdUtils.getIdentity(token, univid);
    }

    public void delete(SSOToken token, IdType type, String name,
            String orgName, String amsdkDN) throws IdRepoException,
            SSOException {
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, orgName, amsdkDN };
            client.send(client.encodeMessage("delete_idrepo", objs), 
        	    Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (Exception ex) {
            processException(ex);
        }
    }

    public Map getAttributes(SSOToken token, IdType type, String name, Set attrNames, String amOrgName, String amsdkDN,
            boolean isString) throws IdRepoException, SSOException {
        if (isString) {
            Map<String, Set<String>> res = null;

            try {
                Object[] objs = {getTokenString(token), type.getName(), name, attrNames, amOrgName, amsdkDN};
                res = (Map<String, Set<String>>) client.send(client.encodeMessage("getAttributes1_idrepo", objs),
                        Session.getLBCookie(token.getTokenID().toString()), null);
                if (res != null) {
                    Map<String, Set<String>> res2 = new AMHashMap();
                    for (Map.Entry<String, Set<String>> entry : res.entrySet()) {
                        String attr = entry.getKey();
                        Set<String> set = entry.getValue();
                        set = XMLUtils.decodeAttributeSet(set);
                        res2.put(attr, set);
                    }
                    res = res2;
                }
            } catch (Exception ex) {
                processException(ex);
            }

            return res;
        } else {
            return getBinaryAttributes(token, type, name, attrNames, amOrgName, amsdkDN);
        }
    }

    private Map<String, byte[][]> getBinaryAttributes(SSOToken token, IdType type, String name, Set<String> attrNames,
            String amOrgName, String amsdkDN) throws IdRepoException, SSOException {

        Map<String, byte[][]> ret = new AMHashMap(true);
        try {
            Object[] objs = {getTokenString(token), type.getName(), name, attrNames, amOrgName, amsdkDN};
            Map<String, Set<String>> encodedAttributes = (Map<String, Set<String>>) client.send(
                    client.encodeMessage("getBinaryAttributes_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);
            if (encodedAttributes != null) {

                for (Map.Entry<String, Set<String>> entry : encodedAttributes.entrySet()) {
                    String attrName = entry.getKey();
                    Set<String> stringValues = XMLUtils.decodeAttributeSet(entry.getValue());
                    byte[][] values = new byte[stringValues.size()][];
                    int counter = 0;
                    for (String value : stringValues) {
                        values[counter++] = Base64.decode(value);
                    }
                    ret.put(attrName, values);
                }
            }
        } catch (Exception ex) {
            processException(ex);
        }
        return ret;
    }

    public Map getAttributes(SSOToken token, IdType type, String name,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException {
        
        Map res = null;
        
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, amOrgName, amsdkDN };
            res = ((Map) client.send(client.encodeMessage(
                    "getAttributes2_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));
            if (res != null) {
                Map res2 = new AMHashMap();
                Iterator it = res.keySet().iterator();
                while (it.hasNext()) {
                    Object attr = it.next();
                    Set set = (Set)res.get(attr);
                    set = XMLUtils.decodeAttributeSet(set);
                    res2.put(attr, set);
                }
                res = res2;
            }
        } catch (Exception ex) {
            processException(ex);
        }
        
        return res;
    }

    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException {
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, attrNames, amOrgName, amsdkDN };
            client.send(client.encodeMessage("removeAttributes_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (Exception ex) {
            processException(ex);
        }
    }

    public IdSearchResults search(SSOToken token, IdType type, String pattern,
            IdSearchControl ctrl, String amOrgName) throws IdRepoException,
            SSOException {
        IdSearchOpModifier modifier = ctrl.getSearchModifier();
        Map avMap = ctrl.getSearchModifierMap();
        int filterOp;
        if (modifier.equals(IdSearchOpModifier.AND)) {
            filterOp = IdRepo.AND_MOD;
        } else {
            filterOp = IdRepo.OR_MOD;
        }
        
        Map idResults = null;
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    pattern, new Integer(ctrl.getTimeOut()),
                    new Integer(ctrl.getMaxResults()),
                    ctrl.getReturnAttributes(),
                    Boolean.valueOf(ctrl.isGetAllReturnAttributesEnabled()),
                    new Integer(filterOp), avMap,
                    Boolean.valueOf(ctrl.isRecursive()), amOrgName };
            idResults = ((Map) client.send(client.encodeMessage(
                    "search2_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (Exception ex) {
            processException(ex);
        }

        return mapToIdSearchResults(token, type, amOrgName, idResults);
    }

    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd, String amOrgName, String amsdkDN,
            boolean isString) throws IdRepoException, SSOException {
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, attributes, Boolean.valueOf(isAdd), amOrgName, 
                    amsdkDN, Boolean.valueOf(isString) };
            client.send(client.encodeMessage("setAttributes2_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);

        } catch (Exception ex) {
            processException(ex);
        }
    }

    public void changePassword(SSOToken token, IdType type, String name,
        String oldPassword, String newPassword, String amOrgName,
        String amsdkDN) throws IdRepoException, SSOException {

        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, oldPassword, newPassword, amOrgName, amsdkDN };
            client.send(client.encodeMessage("changePassword_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);

        } catch (Exception ex) {
            processException(ex);
        }
    }

    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException {
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, serviceName, stype.getType(), attrMap, amOrgName,
                    amsdkDN };
            client.send(client.encodeMessage("assignService_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);

        } catch (Exception ex) {
            processException(ex);
        }

    }

    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServiceNamesAndOCs, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException {
        
        Set resultSet = null;
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, mapOfServiceNamesAndOCs, amOrgName, amsdkDN };
            resultSet = ((Set) client.send(client.encodeMessage(
                    "getAssignedServices_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));

        } catch (Exception ex) {
            processException(ex);
        }
        
        return resultSet;
    }

    public Map getServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set attrNames, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException {
        
        Map resultMap = null;
        try {
            if (debug.messageEnabled()) {
                debug.message("IdRemoteServicesImpl.getServiceAttributes  type="
                    + type + ";  name="  + name + ";  serviceName="
                    + serviceName + ";  attrNames=" + attrNames
                    + ";  amOrgName=" + amOrgName
                    + ";  amsdkDN=" + amsdkDN);
            }

            Object[] objs = { getTokenString(token), type.getName(),
                    name, serviceName, attrNames, amOrgName, amsdkDN };
            resultMap = ((Map) client.send(client.encodeMessage(
                    "getServiceAttributes_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));

        } catch (Exception ex) {
            processException(ex);
        }
        
        return resultMap;
    }

    public Map getBinaryServiceAttributes(SSOToken token, IdType type,
            String name, String serviceName, Set attrNames, String amOrgName,
            String amsdkDN) throws IdRepoException, SSOException {

        Map resultMap = null;
        try {
            if (debug.messageEnabled()) {
                debug.message("IdRemoteServicesImpl.getBinaryServiceAttributes  "
                    + "type="+ type + ";  name="  + name + ";  serviceName="
                    + serviceName + ";  attrNames=" + attrNames
                    + ";  amOrgName=" + amOrgName
                    + ";  amsdkDN=" + amsdkDN);
            }

            Object[] objs = { token.getTokenID().toString(), type.getName(),
                   name, serviceName, attrNames, amOrgName, amsdkDN};
            resultMap = ((Map)client.send(client.encodeMessage(
                    "getBinaryServiceAttributes_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));

        } catch (RemoteException rex) {
            getDebug().error(
                "IdRemoteServicesImpl.getBinaryServiceAttributes_idrepo: " +
                "caught exception=", rex);
            throw new IdRepoException(AMSDKBundle.getString("1000"), "1000");
        } catch (Exception ex) {
            processException(ex);
        }

        return resultMap;
    }

    /**
     * Non-javadoc, non-public methods
     * Get the service attributes of the name identity. Traverse to the global
     * configuration if necessary until all attributes are found or reached
     * the global area whichever occurs first.
     *
     * @param token is the sso token of the person performing this operation.
     * @param type is the identity type of the name parameter.
     * @param name is the identity we are interested in.
     * @param serviceName is the service we are interested in
     * @param attrNames are the name of the attributes wer are interested in.
     * @param amOrgName is the orgname.
     * @param amsdkDN is the amsdkDN.
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException if user's single sign on token is invalid.
     */
    public Map getServiceAttributesAscending(SSOToken token, IdType type,
            String name, String serviceName, Set attrNames, String amOrgName,
            String amsdkDN) throws IdRepoException, SSOException {
        
        Map resultMap = null;
        try {
            if (debug.messageEnabled()) {
                debug.message("IdRemoteServicesImpl."
                    + "getServiceAttributesAscending type=" + type
                    + ";  name="  + name + ";  serviceName=" + serviceName
                    + ";  attrNames=" + attrNames + ";  amOrgName="
                    + amOrgName + ";  amsdkDN=" + amsdkDN);
            }

            Object[] objs = { getTokenString(token), type.getName(),
                   name, serviceName, attrNames, amOrgName, amsdkDN};
            resultMap = ((Map)client.send(
                    client.encodeMessage(
                        "getServiceAttributesAscending_idrepo", objs),
                        Session.getLBCookie(
                            token.getTokenID().toString()), null));

        } catch (Exception ex) {
            processException(ex);
        }
        
        return resultMap;
    }


    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException {
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, serviceName, attrMap, amOrgName, amsdkDN };
            client.send(client.encodeMessage("unassignService_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);

        } catch (Exception ex) {
            processException(ex);
        }
    }

    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap,
            String amOrgName, String amsdkDN) throws IdRepoException,
            SSOException {
        try {
            if (getDebug().messageEnabled()) {
                getDebug().message("IdRemoteServicesImpl.modifyService_idrepo:"
                    + " name =" +  name + ";  type=" + type +
                    ";  serviceName=" + serviceName + ";  stype=" + stype +
                    ";  attrMap=" + attrMap + ";  amOrgName=" + amOrgName +
                    ";  amsdkDN=" + amsdkDN);
            }
            Object[] objs = { getTokenString(token), type.getName(),
                    name, serviceName, stype.getType(), attrMap, amOrgName,
                    amsdkDN };
            client.send(client.encodeMessage("modifyService_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);

        } catch (Exception ex) {
            processException(ex);
        }
    }

    public Set getMembers(SSOToken token, IdType type, String name,
            String amOrgName, IdType membersType, String amsdkDN)
            throws IdRepoException, SSOException {
        Set idResults = null;
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, amOrgName, membersType.getName(), amsdkDN };
            Set res = (Set) client.send(client.encodeMessage(
                    "getMembers_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null);
            idResults = new HashSet();
            if (res != null) {
                Iterator it = res.iterator();
                while (it.hasNext()) {
                    String univid = (String) it.next();
                    AMIdentity id = IdUtils.getIdentity(token, univid);
                    idResults.add(id);
                }
            }
            
        } catch (Exception ex) {
            processException(ex);
        }
        
        return idResults;
    }

    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType, String amOrgName, String amsdkDN)
            throws IdRepoException, SSOException {
        
        Set idResults = null;
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, membershipType.getName(), amOrgName, amsdkDN };
            Set res = (Set) client.send(client.encodeMessage(
                    "getMemberships_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null);
            
            idResults = new HashSet();
            
            if (res != null) {
                Iterator it = res.iterator();
                while (it.hasNext()) {
                    String univid = (String) it.next();
                    AMIdentity id = IdUtils.getIdentity(token, univid);
                    idResults.add(id);
                }
            }            

        } catch (Exception ex) {
            processException(ex);
        }
        
        return idResults;
    }

    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation, String amOrgName)
            throws IdRepoException, SSOException {
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, members, membersType.getName(),
                    new Integer(operation), amOrgName };
            client.send(client.encodeMessage("modifyMemberShip_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);

        } catch (Exception ex) {
            processException(ex);
        }
    }

    public Set getSupportedOperations(SSOToken token, IdType type,
            String amOrgName) throws IdRepoException, SSOException {
        
        Set results = null;
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    amOrgName };
            Set ops = (Set) client.send(client.encodeMessage(
                    "getSupportedOperations_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null);
            results = new HashSet();
            if (ops != null) {
                Iterator it = ops.iterator();
                while (it.hasNext()) {
                    String op = (String) it.next();
                    IdOperation idop = new IdOperation(op);
                    results.add(idop);
                }
            }
            
        } catch (Exception ex) {
            processException(ex);
        }
        
        return results;
    }

    public Set getSupportedTypes(SSOToken token, String amOrgName)
            throws IdRepoException, SSOException {
        
        Set results = null;
        try {
            Object[] objs = { getTokenString(token), amOrgName };
            Set types = (Set) client.send(client.encodeMessage(
                    "getSupportedTypes_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null);
            results = new HashSet();
            if (types != null) {
                Iterator it = types.iterator();
                while (it.hasNext()) {
                    String currType = (String) it.next();
                    IdType thisType = IdUtils.getType(currType);
                    results.add(thisType);
                }
            }
            
        } catch (Exception ex) {
            processException(ex);
        }
        
        return results;
    }

    public boolean isExists(SSOToken token, IdType type, String name,
        String amOrgName) throws SSOException, IdRepoException
    {
        Boolean isExists = null;
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, amOrgName};
            isExists =
                ((Boolean) client
                    .send(client.encodeMessage("isExists_idrepo", objs),
                            Session.getLBCookie(token.getTokenID().toString()), 
                            null));
            
        } catch (Exception ex) {
            processException(ex);
        }
        
        return isExists.booleanValue(); 
    }

    public boolean isActive(SSOToken token, IdType type, String name,
            String amOrgName, String amsdkDN) throws SSOException,
            IdRepoException {
        
        Boolean isActive = null;
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, amOrgName, amsdkDN };
            isActive = ((Boolean) client.send(client.encodeMessage(
                    "isActive_idrepo", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));
            
        } catch (Exception ex) {
            processException(ex);
        }
        
        return isActive.booleanValue();
    }

    public void setActiveStatus(SSOToken token, IdType type, String name,
        String amOrgName, String amsdkDN, boolean active) throws SSOException,
        IdRepoException {
        
        try {
            Object[] objs = { getTokenString(token), type.getName(),
                    name, amOrgName, amsdkDN, Boolean.valueOf(active)};
            client.send(
                    client.encodeMessage("setActiveStatus_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null);

        } catch (Exception ex) {
            processException(ex);
        }
    }

    public void clearIdRepoPlugins(String orgName, String serviceComponent,
        int type) {
        // Nothing to do!
    }

    public void clearIdRepoPlugins() {
        // Nothing to do!
    }

    public void reloadIdRepoServiceSchema() {
        // Do Nothing !!
    }

    public void reinitialize() {
        // Do Nothing !!
    }

    public Set getFullyQualifiedNames(SSOToken token, IdType type,
        String name, String org) throws IdRepoException, SSOException {
        Set answer = null;
        try {
            Object[] objs = { getTokenString(token),
                type.getName(), name, org };
            Set set = (Set) client.send(client.encodeMessage(
                "getFullyQualifiedNames_idrepo", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null);
            if (set != null) {
                // Convert to CaseInsensitiveHashSet
                answer = new CaseInsensitiveHashSet(set);
            }
        } catch (Exception ex) {
            if (debug.warningEnabled()) {
                getDebug().warning(
                    "IdRemoteServicesImpl.getFullyQualifiedNames_idrepo: " +
                         "caught exception=", ex);
            }
            if (ex instanceof IdRepoException) {
                throw ((IdRepoException) ex);
            }
            throw new IdRepoException(AMSDKBundle.getString("1000"), "1000");
        }
        return (answer);
    }

    private IdSearchResults mapToIdSearchResults(SSOToken token, IdType type,
            String orgName, Map m) throws IdRepoException {
        IdSearchResults results = new IdSearchResults(type, orgName);
        if (m != null) {
            Set idSet = (Set) m.get(AMSR_RESULTS);
            Map attrMaps = (Map) m.get(AMSR_ATTRS);
            Integer err = (Integer) m.get(AMSR_CODE);

            if (idSet != null) {
                Iterator it = idSet.iterator();
                while (it.hasNext()) {
                    String idStr = (String) it.next();
                    AMIdentity id = IdUtils.getIdentity(token, idStr);
                    CaseInsensitiveHashMap resultAttrMap =
                        new CaseInsensitiveHashMap();
                    Map attrMap = (Map)attrMaps.get(idStr);
                    for(Iterator iter = attrMap.keySet().iterator();
                        iter.hasNext();) {
                        String attrName = (String)iter.next();
                        Set values = (Set)attrMap.get(attrName);
                        values = XMLUtils.decodeAttributeSet(values);
                        resultAttrMap.put(attrName, values);
                    }
                    results.addResult(id, resultAttrMap);
                }
            }
            if (err != null) {
                results.setErrorCode(err.intValue());
            }
        }
        return results;
    }

    private String getTokenString(SSOToken token) {
        if (!sendRestrictionContext) {
            return token.getTokenID().toString(); 
        }
        SSOToken appToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        return token.getTokenID().toString() + " " +
            appToken.getTokenID().toString();
    }

    
    public IdSearchResults getSpecialIdentities(
        SSOToken token,
        IdType type,
        String orgName
    ) throws IdRepoException, SSOException {
        Map res = null;

        try {
            Object[] objs = {getTokenString(token), type.getName(), orgName};
            res = ((Map)client.send(client.encodeMessage(
                    "getSpecialIdentities_idrepo", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null));

        } catch (Exception ex) {
            processException(ex);
        }

        return mapToIdSearchResults(token, type, orgName, res);
    }
}
