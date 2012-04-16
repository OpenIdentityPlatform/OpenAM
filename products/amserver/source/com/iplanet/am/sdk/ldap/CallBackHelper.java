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
 * $Id: CallBackHelper.java,v 1.2 2008/06/25 05:41:25 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.ldap;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.sdk.AMCallBack;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.common.CallBackHelperBase;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.sso.SSOToken;

/**
 * This class has all the utility methods which determine the external pre-post
 * processing classes for (user, org, role, group) operations. It also provides
 * methods to execute the external implementations by performing a call back of
 * the methods corresponding to the operation in progress.
 */
public class CallBackHelper extends CallBackHelperBase {

    // Variable to store the imp instances for post processing
    private Hashtable callBackObjects = new Hashtable();

    // Operation Types
    public static final int CREATE = 1;

    public static final int DELETE = 2;

    public static final int MODIFY = 3;

    private AMCallBack instantiateClass(String className) {
        try {
            return ((AMCallBack) Class.forName(className).newInstance());
        } catch (ClassNotFoundException c) {
            debug.error("CallBackHelper.instantiateClass(): Unable to "
                    + "locate class " + className, c);
        } catch (Exception e) {
            debug.error("CallBackHelper.instantiateClass(): Unable to "
                    + "instantiate class " + className, e);
        }
        return null;
    }

    /**
     * Returns an instantiated call back object. If not already instantiated
     * trys to instantiate. If not successful a null value is returned.
     */
    private AMCallBack getCallBackObject(String className) {
        AMCallBack callBack = (AMCallBack) callBackObjects.get(className);
        if (callBack == null) { // Not yet instantiated. Instantiate now
            callBack = instantiateClass(className);
            if (callBack != null) {
                callBackObjects.put(className, callBack);
            }
        }
        return callBack;
    }

    public Map getAttributes(SSOToken token, String entryDN, Set attrNames,
            String orgDN) {

        if (!isExternalGetAttributesEnabled(orgDN)) {
            return null;
        }

        Set implSet = getPrePostImpls(orgDN);
        if (implSet != null && !implSet.isEmpty()) {
            Map attributes = new HashMap();
            Iterator itr = implSet.iterator();
            while (itr.hasNext()) {
                String className = (String) itr.next();
                AMCallBack impl = getCallBackObject(className);
                if (impl == null) {
                    continue;
                }
                Map implAttrs = impl.getAttributes(token, entryDN, attrNames);
                if (implAttrs != null && !implAttrs.isEmpty()) {
                    attributes = CommonUtils.mergeMaps(implAttrs, attributes);
                }
            }
            return attributes;
        }
        return null;
    }

    public Map preProcess(SSOToken token, String entryDN, String orgDN,
            Map oldAttrMap, Map newAttrMap, int operation, int objectType,
            boolean softDelete) throws AMException {
        Set implSet = getPrePostImpls(orgDN);
        if (implSet != null && !implSet.isEmpty()) {
            // Post processing impls present
            // Iterate through the Pre-Processing Impls and execute
            Iterator itr = implSet.iterator();
            while (itr.hasNext()) {
                String className = (String) itr.next();
                AMCallBack impl = getCallBackObject(className);
                if (impl == null) {
                    continue;
                }
                try {
                    Map map;
                    switch (operation) {
                    case CREATE:
                        map = impl.preProcessCreate(token, entryDN, newAttrMap,
                                objectType);
                        newAttrMap = ((map == null) ? newAttrMap : map);
                        break;
                    case MODIFY:
                        map = impl.preProcessModify(token, entryDN, oldAttrMap,
                                newAttrMap, objectType);
                        newAttrMap = ((map == null) ? newAttrMap : map);
                        break;
                    case DELETE:
                        impl.preProcessDelete(token, entryDN, oldAttrMap,
                                softDelete, objectType);
                        break;
                    }
                } catch (AMException ae) {
                    // Exception thrown by the external impl
                    debug.error("CallBackHelper.preProcess(): Preprocessing"
                            + "impl " + className
                            + " exception thrown by impl:", ae);
                    throw ae;
                }
            }
            return newAttrMap;
        }
        // At this point oldAttrSet should be returned only if newAttrSet is
        // not null as newAttrSet will be the latest one needed for updation
        return ((newAttrMap != null) ? newAttrMap : oldAttrMap);
    }

    // TODO: Remove this. Use the Maps interface only
    public AttrSet preProcess(SSOToken token, String entryDN, String orgDN,
            AttrSet oldAttrSet, AttrSet newAttrSet, int operation,
            int objectType, boolean softDelete) throws AMException {
        Set implSet = getPrePostImpls(orgDN);
        if (implSet != null && !implSet.isEmpty()) {
            // Post processing impls present
            // Iterate through the Pre-Processing Impls and execute
            Iterator itr = implSet.iterator();
            Map newAttrMap = CommonUtils.attrSetToMap(newAttrSet);
            Map oldAttrMap = CommonUtils.attrSetToMap(oldAttrSet);

            while (itr.hasNext()) {
                String className = (String) itr.next();
                AMCallBack impl = getCallBackObject(className);
                if (impl == null) {
                    continue;
                }
                try {
                    Map map;
                    switch (operation) {
                    case CREATE:
                        map = impl.preProcessCreate(token, entryDN, newAttrMap,
                                objectType);
                        newAttrMap = ((map == null) ? newAttrMap : map);
                        break;
                    case MODIFY:
                        map = impl.preProcessModify(token, entryDN, oldAttrMap,
                                newAttrMap, objectType);
                        newAttrMap = ((map == null) ? newAttrMap : map);
                        break;
                    case DELETE:
                        impl.preProcessDelete(token, entryDN, oldAttrMap,
                                softDelete, objectType);
                        break;
                    }
                } catch (AMException ae) {
                    // Exception thrown by the external impl
                    debug.error("CallBackHelper.preProcess(): Preprocessing"
                            + "impl " + className
                            + " exception thrown by impl:", ae);
                    throw ae;
                }
            }
            return CommonUtils.mapToAttrSet(newAttrMap);
        }
        // At this point oldAttrSet should be returned only if newAttrSet is
        // not null as newAttrSet will be the latest one needed for updation
        return ((newAttrSet != null) ? newAttrSet : oldAttrSet);
    }

    // TODO: Remove this. Use the Maps interface only
    public void postProcess(SSOToken token, String entryDN, String orgDN,
            AttrSet oldAttrSet, AttrSet newAttrSet, int operation,
            int objectType, boolean softDelete) throws AMException {
        // Use the external impls instantiated at the time of pre-processing
        Set implSet = getPrePostImpls(orgDN);
        if ((implSet != null) && (!implSet.isEmpty())) {
            Map newAttrMap = CommonUtils.attrSetToMap(newAttrSet);
            Map oldAttrMap = CommonUtils.attrSetToMap(oldAttrSet);
            // Iterate through the Pre-Processing Impls and execute
            Iterator itr = implSet.iterator();
            while (itr.hasNext()) {
                String className = (String) itr.next();
                AMCallBack impl = getCallBackObject(className);
                if (impl == null) {
                    continue;
                }
                try {
                    switch (operation) {
                    case CREATE:
                        impl.postProcessCreate(token, entryDN, newAttrMap,
                                objectType);
                        break;
                    case MODIFY:
                        impl.postProcessModify(token, entryDN, oldAttrMap,
                                newAttrMap, objectType);
                        break;
                    case DELETE:
                        impl.postProcessDelete(token, entryDN, oldAttrMap,
                                softDelete, objectType);
                        break;
                    }
                } catch (AMException ae) {
                    // Exception thrown by the external impl
                    debug.error("CallBackHelper.postProcess(): Preprocessing"
                            + "impl " + impl.getClass().getName()
                            + " exception thrown: ", ae);
                }
            }
        }
    }

    public void postProcess(SSOToken token, String entryDN, String orgDN,
            Map oldAttrMap, Map newAttrMap, int operation, int objectType,
            boolean softDelete) throws AMException {
        // Use the external impls instantiated at the time of pre-processing
        Set implSet = getPrePostImpls(orgDN);
        if ((implSet != null) && (!implSet.isEmpty())) {
            // Iterate through the Pre-Processing Impls and execute
            Iterator itr = implSet.iterator();
            while (itr.hasNext()) {
                String className = (String) itr.next();
                AMCallBack impl = getCallBackObject(className);
                if (impl == null) {
                    continue;
                }
                try {
                    switch (operation) {
                    case CREATE:
                        impl.postProcessCreate(token, entryDN, newAttrMap,
                                objectType);
                        break;
                    case MODIFY:
                        impl.postProcessModify(token, entryDN, oldAttrMap,
                                newAttrMap, objectType);
                        break;
                    case DELETE:
                        impl.postProcessDelete(token, entryDN, oldAttrMap,
                                softDelete, objectType);
                        break;
                    }
                } catch (AMException ae) {
                    // Exception thrown by the external impl
                    debug.error("CallBackHelper.postProcess(): Preprocessing"
                            + "impl " + impl.getClass().getName()
                            + " exception thrown: ", ae);
                }
            }
        }
    }

    /**
     * Special method for pre processing memberShip modification for roles &
     * groups.
     */
    public Set preProcessModifyMemberShip(SSOToken token, String entryDN,
            String orgDN, Set members, int operation, int objectType)
            throws AMException {
        Set implSet = getPrePostImpls(orgDN);
        if (implSet != null && !implSet.isEmpty()) {
            // Post processing impls present
            // Iterate through the PrePost-Processing plugins and execute
            Iterator itr = implSet.iterator();
            while (itr.hasNext()) {
                String className = (String) itr.next();
                AMCallBack impl = getCallBackObject(className);
                if (impl == null) {
                    continue;
                }
                try {
                    switch (operation) {
                    case DirectoryServicesImpl.ADD_MEMBER:
                        members = impl.preProcessAddUser(token, entryDN,
                                members, objectType);
                        break;
                    case DirectoryServicesImpl.REMOVE_MEMBER:
                        members = impl.preProcessRemoveUser(token, entryDN,
                                members, objectType);
                        break;
                    }
                } catch (AMException ae) {
                    // Exception thrown by the external impl
                    debug.error("CallBackHelper.preProcessModifyMemberShip():"
                            + " Preprocessing impl " + className
                            + " exception " + "thrown by impl:", ae);
                    throw ae;
                }
            }
        }
        return members;
    }

    /**
     * Special method for post processing memberShip modification for roles &
     * groups.
     */
    public void postProcessModifyMemberShip(SSOToken token, String entryDN,
            String orgDN, Set members, int operation, int objectType)
            throws AMException {
        // Use the external impls instantiated at the time of pre-processing
        Set implSet = getPrePostImpls(orgDN);
        if ((implSet != null) && (!implSet.isEmpty())) {
            // Iterate through the PrePost-Processing plugins and execute
            Iterator itr = implSet.iterator();
            while (itr.hasNext()) {
                String className = (String) itr.next();
                AMCallBack impl = getCallBackObject(className);
                if (impl == null) {
                    continue;
                }
                try {
                    switch (operation) {
                    case DirectoryServicesImpl.ADD_MEMBER:
                        impl.postProcessAddUser(token, entryDN, members,
                                objectType);
                        break;
                    case DirectoryServicesImpl.REMOVE_MEMBER:
                        impl.postProcessRemoveUser(token, entryDN, members,
                                objectType);
                        break;
                    }
                } catch (AMException ae) {
                    // Exception thrown by the external impl
                    debug.error("CallBackHelper.postProcessModifyMemberShip()"
                            + ": Preprocessing impl " 
                            + impl.getClass().getName()
                            + " exception thrown: ", ae);
                }
            }
        }
    }
}
