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
 * $Id: ResourceName.java,v 1.2 2008/06/25 05:43:47 qcheng Exp $
 *
 */

package com.sun.identity.policy.interfaces;

import java.util.Map;
import java.util.Set;
import com.sun.identity.policy.ResourceMatch;
import com.sun.identity.policy.PolicyException;

/**
 * The interface <code>ResourceName</code> provides
 * methods to determine the hierarchy of resource names.
 * It provides methods to compare resources, get sub resources etc.
 * Also it provides an interface to determine the service
 * type to which it be used. Service developers could
 * provide an implementation of this interface that will
 * determine its hierarchy during policy evaluation and
 * also its display in the GUI. A class that implements
 * this interface must have a empty constructor.
 * @supported.all.api
 */
public interface ResourceName {

    /**
     * Returns the service type names for which the resource name
     * object can be used.
     *
     * @return service type names for which the resource
     * comparator can be used
     */
    public Set getServiceTypeNames();

    /**
     * Initializes the resource name with configuration information,
     * usually set by the administrators
     *
     * @param configParams configuration parameters as a map.
     * The keys of the map are the configuration parameters. 
     * Each key is corresponding to one <code>String</code> value
     * which specifies the configuration parameter value.
     */
    public void initialize(Map configParams);

    /**
     * Compares two resources.
     *
     * @param origRes name of the resource which will be compared
     * @param compRes name of the resource which will be compared with
     * @param wildcardCompare flag for wildcard comparison
     *
     * @return returns <code>ResourceMatch</code> that
     * specifies if the resources are exact match, or
     * otherwise.
     * <ul>
     * <li><code>ResourceMatch.NO_MATCH</code> means two resources do not match
     * <li><code>ResourceMatch.EXACT_MATCH</code> means two resources match
     * <li><code>ResourceMatch.SUB_RESOURCE_MATCH</code> means
     *     <code>compRes</code> is the sub resource of the <code>origRes</code>
     * <li><code>ResourceMatch.SUPER_RESOURCE_MATCH</code> means
     *     <code>compRes</code> is the super resource of the
     *     <code>origRes</code>
     * <li><code>ResourceMatch.WILDCARD_MATCH</code> means two resources match
     *     with respect to the wildcard
     * </ul>
     */
    public ResourceMatch compare(
        String origRes, String compRes, boolean wildcardCompare);
    
    /**
     * Appends sub-resource to super-resource.
     *
     * @param superResource name of the super-resource to be appended to.
     * @param subResource name of the sub-resource to be appended.
     *
     * @return returns the combination resource.
     */
    public String append(String superResource, String subResource);

    /**
     * Gets sub-resource from an original resource minus
     * a super resource. This is the complementary method of
     * append().
     *
     * @param res name of the original resource consisting of
     * the second parameter <code>superRes</code> and the returned value
     * @param superRes name of the super-resource which the first
     * parameter begins with.
     *
     * @return returns the sub-resource which the first parameter
     * ends with. If the first parameter does not begin with the
     * the first parameter, then the return value is null.
     */
    public String getSubResource(String res, String superRes);

    /**
     * Gets the canonicalized form of a resource string
     * 
     * @param res the resource string to be canonicalized
     * @return the resource string in its canonicalized form.
     * @throws PolicyException if resource string is invalid
     */
    public String canonicalize(String res) throws PolicyException;


    /******* this method will be removed after the demo ********/
    /**
     * Method to split a resource into the smallest necessary
     * sub resource units
     *
     * @param res name of the resource to be split
     *
     * @return returns the array of sub-resources, with the first
     * element being what the original resource begins with, and
     * the last one being what it ends with
     */
    public String[] split(String res);

}
