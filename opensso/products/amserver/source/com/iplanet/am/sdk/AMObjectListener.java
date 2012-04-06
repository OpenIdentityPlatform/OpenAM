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
 * $Id: AMObjectListener.java,v 1.3 2008/06/25 05:41:21 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.EventListener;
import java.util.Map;
import java.util.Set;

/**
 * The purpose of this interface is to allow AM SDK plugin implementors to
 * return changes about AM Entities
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public interface AMObjectListener extends EventListener {

    /**
     * This callback method is called by the Identity Repository plugin when
     * backend datastore triggers a notification event
     * 
     * @param name
     *            unique name of the object that has changed
     * @param type
     *            type of change i.e., ADD, DELETE, MODIFY
     * @param configMap
     *            Map of configuration information which the AM SDK framework
     *            passes to the plugin, and which the plugin should return as
     *            is. This information helps the framework to map the object to
     *            the universal identifier used as the cache key.
     */
    public void objectChanged(String name, int type, Map configMap);

    /**
     * This callback method is called by the Identity Repository plugin when
     * backend datastore triggers a notification event that results in multiple
     * objects being modified. This callback signifies a change to specific
     * attributes of all members of the organization. This callback should be
     * used to notify modifications of "dynamic" or "virtual" attributes.
     * 
     * @param parentNames
     *            name of the parent whoes "dynamic" attributes attributes have
     *            been modified, affecting all its children
     * @param type
     *            type of change i.e., ADD, DELETE, MODIFY
     * @param attrNames
     *            attribute names that have been modified
     * @param configMap
     *            Map of configuration information which the AM SDK framework
     *            passes to the plugin, and which the plugin should return as
     *            is. This information helps the framework to map the object to
     *            the universal identifier used as the cache key.
     */
    public void objectsChanged(String parentNames, int type, Set attrNames,
            Map configMap);

    /**
     * This callback method is called by the Identity Repository plugin when
     * backend datastore triggers a permission change notification event. This
     * callback signifies a change in the permissions associated with an
     * organization. A change in the permission can impack the permissios of all
     * entities in an organization and all cached entries for that organization
     * will be cleared.
     * 
     * @param orgName
     *            unique name of the organization whoes permissions has changed
     * @param configMap
     *            Map of configuration information which the AM SDK framework
     *            passes to the plugin, and which the plugin should return as
     *            is. This information helps the framework to map the object to
     *            the universal identifier used as the cache key.
     */
    public void permissionsChanged(String orgName, Map configMap);

    /**
     * This callback notifies the listener that all object should be marked as
     * "changed" or "dirty". This callback is only used in the case when IdRepo
     * plugin looses the connection to data store and does not know what could
     * have changed in the repository.
     */
    public void allObjectsChanged();

    public void setConfigMap(Map cmap);

    public Map getConfigMap();

    public static final int ADD = AMEvent.OBJECT_ADDED;

    public static final int DELETE = AMEvent.OBJECT_REMOVED;

    public static final int MODIFY = AMEvent.OBJECT_CHANGED;

    public static final int RENAMED = AMEvent.OBJECT_CHANGED;
}
