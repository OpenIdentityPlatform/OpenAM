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
 * $Id: AMEntity.java,v 1.4 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * This interface provides methods to manage entities. The entities that can be
 * managed by these interfaces are configured in the <code> DAI </code> service
 * of Access Manager. This service will group the name of the entity and the
 * creation templates, search template, primary LDAP <code>objectclass</code>
 * and the status attribute (if any). This grouping is used to determine what
 * object is being managed. <code>AMEntity</code> objects can be obtained by
 * using <code>AMStoreConnection</code>. A handle to this object can be
 * obtained by using the DN of the object.
 * 
 * <PRE>
 * 
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); if
 * (amsc.doesEntryExist(uDN)) { AMEntity entity = amsc.getEntity(uDN); }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMEntity {

    /**
     * Returns the distinguished name of the entry.
     * 
     * @return distinguished name.
     */
    public String getDN();

    /**
     * Returns the parent distinguished name of the entry.
     * 
     * @return distinguished name.
     */
    public String getParentDN();

    /**
     * Returns true if the entry exists in the directory or not. First a syntax
     * check is done on the distinguished name string corresponding to the
     * entry. If the distinguished name syntax is valid, a directory call will
     * be made to check for the existence of the entry.
     * <p>
     * 
     * <B>NOTE:</B> This method internally invokes a call to the directory to
     * verify the existence of the entry. There could be a performance overhead.
     * Hence, please use your discretion while using this method.
     * 
     * @return false if the entry does not have a valid DN syntax or if the
     *         entry does not exists in the Directory.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean isExists() throws SSOException;

    /**
     * Returns Map of all attributes. Map key is the attribute name and value is
     * the attribute value.
     * 
     * @return Map of all attributes.
     * @throws AMException
     *             if an error is encountered when trying to read attributes
     *             from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Map getAttributes() throws AMException, SSOException;

    /**
     * Returns Map of specified attributes. Map key is the attribute name and
     * value is the attribute value.
     * 
     * @param attributeNames
     *            The Set of attribute names.
     * @return Map of specified attributes.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Map getAttributes(Set attributeNames) throws AMException,
            SSOException;

    /**
     * Sets attribute values in this <code>AMObject</code>. Note that this
     * method sets or replaces the attribute value with the new value supplied.
     * Also, the attributes changed by this method are not committed to the LDAP
     * data store unless the method {@link AMObject#store store()} is called
     * explicitly.
     * 
     * @param attributes
     *            map of attribute name to a set of attribute values. Each of
     *            the attribute value must be a string value.
     * @throws AMException
     *             if an error is encountered when trying to set/replace
     *             attributes from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setAttributes(Map attributes) throws AMException, SSOException;

    /**
     * Removes attributes in this <code>AMObject</code>. The attributes are
     * removed from the LDAP data store.
     * 
     * @param attributes
     *            The Set of attribute names.
     * @throws AMException
     *             if an error is encountered when trying to remove attributes
     *             from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeAttributes(Set attributes) throws AMException,
            SSOException;

    /**
     * Activates the entity (if a status attribute is defined for this entity).
     * If a status attribute is not defined then this method returns without
     * doing anything.
     * 
     * @throws AMException
     *             if an error is encountered when trying to activate the
     *             managed object.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void activate() throws AMException, SSOException;

    /**
     * Deactivates the entity (if a status attribute is defined for this
     * entity). If a status attribute is not defined then this method returns
     * without doing anything.
     * 
     * @throws AMException
     *             if an error is encountered when trying to deactivate the
     *             managed object.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deactivate() throws AMException, SSOException;

    /**
     * Returns true if the entity is activated. If the entity does not have a
     * status attribute, then this method returns true, rather that throw an
     * exception.
     * 
     * @return true if the entity is activated.
     * @throws AMException
     *             if an error is encountered when trying to get the status
     *             attribute from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean isActivated() throws AMException, SSOException;

    /**
     * Deletes the object.
     * 
     * @see #delete(boolean)
     * @see #purge(boolean, int)
     * @throws AMException
     *             if an error is encountered when trying to delete entry from
     *             the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void delete() throws AMException, SSOException;

    /**
     * Deletes object(s). This method takes a boolean parameter, if its value is
     * true, will remove the object and any objects under it, otherwise, will
     * try to remove the object only. Two notes on recursive delete. First, be
     * aware of the PERFORMANCE hit when large amount of child objects present.
     * In the soft-delete mode, this method will mark the following objects for
     * deletion: <code>Organization, Group, User</code>
     * <code>purge()</code>
     * should be used to physically delete this object.
     * 
     * @see #purge(boolean, int)
     * 
     * @param recursive
     *            if true delete the object and any objects under it, otherwise,
     *            delete the object only.
     * @throws AMException
     *             if an error is encountered when trying to delete entry from
     *             the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void delete(boolean recursive) throws AMException, SSOException;

    /**
     * Gets the object's organization distinguished name. NOTE: Obtaining an
     * organization distinguished name involves considerable overhead. Hence
     * after obtaining the organization distinguished name, each object saves
     * this information. Consecutives method calls on this object fetch the
     * value stored in the object. Creating a new <code>AMEntity</code>
     * instance every time to obtain the organization distinguished name is not
     * recommended.
     * 
     * @return The object's organization DN.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store or the object does not have
     *             organization distinguished name.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public String getOrganizationDN() throws AMException, SSOException;

    /**
     * Stores the change to directory server. This method should be called after
     * doing <code>setAttributes</code> so that the changes that are made can
     * be permanently committed to the LDAP data store.
     * 
     * @throws AMException
     *             if an error is encountered when trying to save the attributes
     *             to the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void store() throws AMException, SSOException;

    /**
     * Purges entry from data store. It will physically delete the entry from
     * the data store. This method will override the soft-delete option, which
     * the method <code>delete()</code> will not. There is a big PERFORMANCE
     * hit if this method is used to delete a large organization in the
     * recursive mode.
     * 
     * @see #delete()
     * @param recursive
     *            true to recursively delete the whole subtree.
     * @param graceperiod
     *            If set to an integer greater than -1, it will verify if the
     *            object was last modified at least that many days ago before
     *            physically deleting it. Pre/Post Callback plugins as
     *            registered in the Administration Service, will be called upon
     *            object deletion. If any of the pre-callback classes throw an
     *            exception, then the operation is aborted.
     * @throws AMException
     *             if there is an internal error in the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void purge(boolean recursive, int graceperiod) throws AMException,
            SSOException;

}
