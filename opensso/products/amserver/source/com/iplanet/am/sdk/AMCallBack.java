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
 * $Id: AMCallBack.java,v 1.3 2008/06/25 05:41:19 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOToken;

/**
 * A Plugin Class that needs to be extended by external applications in-order to
 * do some special pre/post-processing for creation/deletion and modification
 * operations for User, Organization, Roles and Groups. The implementation
 * classes/module(s) are pluggable and are configurable through the Organization
 * attribute: <code>iplanet-am-admin-console-pre-post-processing-modules</code>
 * of the <code>iPlanetAMAdminConsoleService</code> service.
 * 
 * <p>
 * For call backs pertaining to Organizations and Organizational Units, the
 * parent organization's configuration (
 * <code>iPlanetAMAdminConsoleService</code>
 * Organization configuration) will be used to obtain the plugin modules.
 * 
 * <p>
 * The call backs will be made at the time of performing one of the
 * corresponding User/Organization/Role/Group operations (create/modify/delete
 * and attribute fetch) by the Sun Java System Access Manager SDK. Applications
 * that need to perform special pre/post processing for one or more of the above
 * operations, should extend the class and override the corresponding methods.
 * 
 * <p>
 * The API's for pre call back provide a mechanism to inspect the attributes
 * being modified and also modify the values appropriately if required. Care
 * should be taken while performing such modifications, so that it will not
 * affect other plugins which are dependent on the same attributes.
 * 
 * <p>
 * <b>Note:</b>
 * <ul>
 * <p>
 * <li> When more than one plugin modules are configured at a particular
 * Organization level, the call backs for each of the plugins will occur one
 * after the other. Also, note that the order in which plugins are called back
 * is cannot pre-determined in any way.
 * 
 * <p>
 * <li> Since the methods of this class will be invoked by the Identity Server
 * SDK and will control the flow of SDK, extreme caution should be taken while
 * overriding these methods to avoid performance bottle necks.
 * 
 * <p>
 * <li> The exceptions thrown by the pre-processing methods of this class will
 * be treated as a failure of external processing and the operation in progress
 * will be halted by the SDK. The exception thrown should include a proper user
 * specific localized error message which can be propagated back to the
 * application using the SDK. The locale of the user should to be determined
 * using token of the authenticated user while constructing such a localized
 * message.
 * </ul>
 * 
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public class AMCallBack {

    protected AMCallBack() {
    }

    /**
     * This method provides a mechanism for applications to obtain attributes
     * external to the Access Manager data store.
     * 
     * This callback gets invoked when any of the {@link AMObject#getAttributes 
     * AMObject.getAttributes()} methods are called. When multiple plugins
     * override this method, then attributes returned from each of them will be
     * merged and returned. When the <code>getAttribute()</code> method that
     * request specific attributes the call backs are made only for those
     * attributes that are not found in the Access Manager's data store. If the
     * <code>getAttributes()</code> which do not request any specific
     * attributes is called, the call back will take place after obtaining all
     * the attributes for the corresponding entry from the Access Manager's data
     * store. <br>
     * <b>NOTE:</b>
     * <ul>
     * <li> This callback is not enabled by default. In order for this call back
     * to be invoked, the organizational attribute:
     * <code>iplanet-am-admin-console-external-attribute-fetch-enabled</code>
     * of the <code>iPlanetAMAdminConsoleService</code> service should be set
     * to <code>enabled</code>
     * <li> Overriding this method would cause significant performance impact.
     * Hence, extreme caution should be taken while overriding this method to
     * avoid processing overhead.
     * <li> The attributes returned by the plugins will not be cached by SDK.
     * Hence, in order to avoid performance overheads, it is recommended that
     * plugin's maintain a local cache of frequently attributes. Also, the cache
     * needs to be in sync with any modifications made to those attributes.
     * </ul>
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being added
     * @param attrNames
     *            names of attributes that need to retrieved. If null, all
     *            attributes should be returned.
     * 
     * @return a Map of attributes, where the key is the attribute name and the
     *         value is a Set of values. This map of attributes will be copied
     *         to the original map retrieved from the Access Manager data store
     *         and will be returned to the caller.
     */
    public Map getAttributes(SSOToken token, String entryDN, Set attrNames) {
        return null;
    }

    /**
     * Method which gets invoked before a create operation is performed.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being added
     * @param attributes
     *            a map consisting of attribute names and a set of values for
     *            each of them. This map of attributes can be inspected,
     *            modified and sent back. Note, caution should be taken while
     *            performing modifications to avoid changing attributes that are
     *            used by Access Manager. If no modifications need to done,
     *            either the original map or null value can be returned.
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#USER AMObject.USER}
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#FILTERED_ROLE AMObject.FILTERED_ROLE}
     *            <li> {@link AMObject#ORGANIZATION AMObject.ORGANIZATION}
     *            <li> {@link AMObject#ORGANIZATIONAL_UNIT 
     *            AMObject.ORGANIZATIONAL_UNIT}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#DYNAMIC_GROUP AMObject.DYNAMIC_GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * @return Map of updated values for <code>attributes<code> or null. If this
     *         returned map is not null, then this map will be used while
     *         performing the operation. 
     * @throws AMPreCallBackException if an 
     *         error that occurs during pre processing. The SDK will not proceed
     *         with the create operation, if any one of the implementation
     *         classes throws an exception. A user specific localized message
     *         should be sent as part of the exception message. The specific
     *         messages can be added to <code>amProfile.properties</code> file.
     */
    public Map preProcessCreate(SSOToken token, String entryDN, Map attributes,
            int objectType) throws AMPreCallBackException {
        return attributes;
    }

    /**
     * Method which gets invoked before a modify operation is performed.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being modified
     * @param oldAttributes
     *            a map consisting of attribute names and a set of values for
     *            each of them before modification
     * @param newAttributes
     *            a map consisting of attribute names and a set of values for
     *            each of them after modification. This map of attributes can be
     *            inspected, modified and sent back. Note, caution should be
     *            taken while performing modifications to avoid changing
     *            attributes that are used by Access Manager. If no
     *            modifications need to done, either the original map or null
     *            value can be returned.
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#USER AMObject.USER}
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#FILTERED_ROLE AMObject.FILTERED_ROLE}
     *            <li> {@link AMObject#ORGANIZATION AMObject.ORGANIZATION}
     *            <li> {@link AMObject#ORGANIZATIONAL_UNIT 
     *            AMObject.ORGANIZATIONAL_UNIT}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#DYNAMIC_GROUP AMObject.DYNAMIC_GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * @return Map of updated values for <code>newAttributes</code> or null.
     *         If this returned map is not null, then this map will be used
     *         while performing the operation.
     * 
     * @throws AMPreCallBackException
     *             if an error occurs pre processing. The SDK will not proceed
     *             with the modify operation, if any one of the implementation
     *             classes throws an exception. A user specific localized
     *             message should be sent as part of the exception message. The
     *             specific messages can be added to
     *             <code>amProfile.properties</code> file.
     */
    public Map preProcessModify(SSOToken token, String entryDN,
            Map oldAttributes, Map newAttributes, int objectType)
            throws AMPreCallBackException {
        return newAttributes;
    }

    /**
     * Method which gets invoked before an entry is deleted. The deletion type
     * configured in Sun Java System Access Manager is also passed as a
     * parameter to this method.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being deleted
     * @param attributes
     *            a map consisting of attribute names and a set of values for
     *            each of them.
     * @param softDeleteEnabled
     *            if true soft delete will be performed Otherwise hard delete
     *            will be performed.
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#USER AMObject.USER}
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#FILTERED_ROLE AMObject.FILTERED_ROLE}
     *            <li> {@link AMObject#ORGANIZATION AMObject.ORGANIZATION}
     *            <li> {@link AMObject#ORGANIZATIONAL_UNIT 
     *            AMObject.ORGANIZATIONAL_UNIT}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#DYNAMIC_GROUP AMObject.DYNAMIC_GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * @throws AMPreCallBackException
     *             if an error occurs during entry delete pre-processing. The
     *             SDK will not proceed with the delete operation, if any one of
     *             the implementation classes throws an exception. A user
     *             specific localized message should be sent as part of the
     *             exception message. The specific messages can be added to
     *             <code>amProfile.properties</code> file.
     */
    public void preProcessDelete(SSOToken token, String entryDN,
            Map attributes, boolean softDeleteEnabled, int objectType)
            throws AMPreCallBackException {
    }

    /**
     * Method which gets invoked after a entry create operation is performed.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being added
     * @param attributes
     *            a map consisting of attribute names and a set of values for
     *            each of them
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#USER AMObject.USER}
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#FILTERED_ROLE AMObject.FILTERED_ROLE}
     *            <li> {@link AMObject#ORGANIZATION AMObject.ORGANIZATION}
     *            <li> {@link AMObject#ORGANIZATIONAL_UNIT 
     *            AMObject.ORGANIZATIONAL_UNIT}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#DYNAMIC_GROUP AMObject.DYNAMIC_GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * 
     * @throws AMPostCallBackException
     *             if an error occurs during post processing. A user specific
     *             localized message should be sent as part of the exception
     *             message. The specific messages can be added to
     *             <code>amProfile.properties</code> file.
     */
    public void postProcessCreate(SSOToken token, String entryDN,
            Map attributes, int objectType) throws AMPostCallBackException {
    }

    /**
     * Method which gets invoked after a entry is modified
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being modified
     * @param oldAttributes
     *            a map consisting of attribute names and a set of values for
     *            each of them before modification
     * @param newAttributes
     *            a map consisting of attribute names and a set of values for
     *            each of them after modification
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#USER AMObject.USER}
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#FILTERED_ROLE AMObject.FILTERED_ROLE}
     *            <li> {@link AMObject#ORGANIZATION AMObject.ORGANIZATION}
     *            <li> {@link AMObject#ORGANIZATIONAL_UNIT 
     *            AMObject.ORGANIZATIONAL_UNIT}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#DYNAMIC_GROUP AMObject.DYNAMIC_GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * 
     * @throws AMPostCallBackException
     *             if an error occurs during post processing. A user specific
     *             localized message should be sent as part of the exception
     *             message. The specific messages can be added to
     *             <code>amProfile.properties</code> file.
     */
    public void postProcessModify(SSOToken token, String entryDN,
            Map oldAttributes, Map newAttributes, int objectType)
            throws AMPostCallBackException {
    }

    /**
     * Method which gets invoked after a entry entry is deleted. The deletion
     * type configured in Sun Java System Access Manager is also passed as a
     * parameter to this method.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being deleted
     * @param attributes
     *            a map consisting of attribute names and a set of values for
     *            each of them
     * @param softDelete
     *            If true, this implies that the object is just being marked for
     *            deletion, if false, then it implies that the object is being
     *            removed from the data store.
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#USER AMObject.USER}
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#FILTERED_ROLE AMObject.FILTERED_ROLE}
     *            <li> {@link AMObject#ORGANIZATION AMObject.ORGANIZATION}
     *            <li> {@link AMObject#ORGANIZATIONAL_UNIT 
     *            AMObject.ORGANIZATIONAL_UNIT}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#DYNAMIC_GROUP AMObject.DYNAMIC_GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * @throws AMPostCallBackException
     *             if an error occurs during post processing. A user specific
     *             localized message should be sent as part of the exception
     *             message. The specific messages can be added to
     *             <code>amProfile.properties</code> file.
     */
    public void postProcessDelete(SSOToken token, String entryDN,
            Map attributes, boolean softDelete, int objectType)
            throws AMPostCallBackException {
    }

    /**
     * Method which gets called before users are added to a role/group.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being added
     * @param members
     *            a set consisting of user DN's. This set of members can be
     *            inspected, modified (users can be added/removed) and sent
     *            back. If no modifications need to done, either the original
     *            set or null value can be returned.
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * 
     * @return Set of updated values for <code>members<code> or null. If null
     * value or empty set is returned, no users will be added. Hence, if no
     * modification is being performed to the original set, it needs to be 
     * back.
     *
     * @throws AMPreCallBackException if an 
     * error occurs during pre processing. The SDK will not proceed with
     * the adding users to role/group operation, if any one of the 
     * implementation classes throws an exception. A user specific localized 
     * message should be sent as part of the exception message. The specific 
     * messages can be added to
     * <code>amProfile.properties</code> file.
     */
    public Set preProcessAddUser(SSOToken token, String entryDN, Set members,
            int objectType) throws AMPreCallBackException {
        return members;
    }

    /**
     * Method which gets invoked after users are added to a role/group.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being added
     * @param members
     *            a Set consisting of user DN's which represent the users added
     *            to the role/group.
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * 
     * @throws AMPostCallBackException
     *             if an error occurs during post processing. A user specific
     *             localized message should be sent as part of the exception
     *             message. The specific messages can be added to
     *             <code>amProfile.properties</code> file.
     */
    public void postProcessAddUser(SSOToken token, String entryDN, Set members,
            int objectType) throws AMPostCallBackException {
    }

    /**
     * Method which gets called before users are removed from a role/group.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being added
     * @param members
     *            a set consisting of user DN's. This set of members can be
     *            inspected, modified (users can be added/removed) and sent
     *            back. If no modifications need to done, either the original
     *            set or null value can be returned.
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * 
     * @return Set of updated values for <code>members<code> or null. If null
     * value or empty set is returned, no users will be removed. Hence, if no
     * modification is being performed to the original set, it needs to be 
     * back.
     *
     * @throws AMPreCallBackException if an 
     * error that occurs during pre processing. The SDK will not proceed with
     * the removing users from role/group operation, if any one of the 
     * implementation classes throws an exception. A user specific localized 
     * message should be sent as part of the exception message. The specific 
     * messages can be added to <code>amProfile.properties</code> file.
     */
    public Set preProcessRemoveUser(SSOToken token, String entryDN,
            Set members, int objectType) throws AMPreCallBackException {
        return members;
    }

    /**
     * Method which gets invoked after users are removed from a role/group.
     * 
     * @param token
     *            the <code>SSOToken</code>
     * @param entryDN
     *            the DN of the entry being added
     * @param members
     *            a Set consisting of user DN's which represent the users added
     *            to the role/group.
     * @param objectType
     *            represents the type of entry on which the operation is being
     *            performed. Types could be:
     *            <ul>
     *            <li> {@link AMObject#ROLE AMObject.ROLE}
     *            <li> {@link AMObject#GROUP AMObject.GROUP}
     *            <li> {@link AMObject#ASSIGNABLE_DYNAMIC_GROUP 
     *            AMObject.ASSIGNABLE_DYNAMIC_GROUP}
     *            </ul>
     * 
     * @throws AMPostCallBackException
     *             if an error occurs during post processing. A user specific
     *             localized message should be sent as part of the exception
     *             message. The specific messages can be added to
     *             <code>amProfile.properties</code> file.
     */
    public void postProcessRemoveUser(SSOToken token, String entryDN,
            Set members, int objectType) throws AMPostCallBackException {
    }
}
