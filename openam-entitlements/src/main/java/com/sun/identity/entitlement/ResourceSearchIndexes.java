/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ResourceSearchIndexes.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 *
 * Portions copyright 2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement;

import java.util.HashSet;
import java.util.Set;

/**
 * This class encapsulates the result of resource splitting.
 */
public class ResourceSearchIndexes {
    private Set<String> hostIndexes = new HashSet();
    private Set<String> pathIndexes = new HashSet();
    private Set<String> parentPathIndexes = new HashSet();

    /**
     * Constructor.
     *
     * @param hostIndexes Set of host indexes.
     * @param pathIndexes Set of path indexes.
     * @param parentPathIndexes 
     */
    public ResourceSearchIndexes(
        Set<String> hostIndexes,
        Set<String> pathIndexes,
        Set<String> parentPathIndexes
    ) {
        if (hostIndexes != null) {
            this.hostIndexes.addAll(hostIndexes);
        }
        if (pathIndexes != null) {
            this.pathIndexes.addAll(pathIndexes);
        }
        if (parentPathIndexes != null) {
            this.parentPathIndexes = parentPathIndexes;
        }
    }

    /**
     * Appends indexes.
     *
     * @param other Other indexes
     */
    public void addAll(ResourceSearchIndexes other) {
        this.hostIndexes.addAll(other.hostIndexes);
        this.pathIndexes.addAll(other.pathIndexes);
        this.parentPathIndexes.addAll(other.parentPathIndexes);
    }

    /**
     * Returns host indexes.
     *
     * @return host indexes.
     */
    public Set<String> getHostIndexes() {
        return hostIndexes;
    }

    /**
     * Returns parent path indexes.
     *
     * @return parent path indexes.
     */
    public Set<String> getParentPathIndexes() {
        return parentPathIndexes;
    }

    /**
     * Returns path indexes.
     *
     * @return path indexes.
     */
    public Set<String> getPathIndexes() {
        return pathIndexes;
    }

    /**
     * @return Whether there are any indexes present.
     */
    public boolean isEmpty() {
        return hostIndexes.isEmpty() && parentPathIndexes.isEmpty() && pathIndexes.isEmpty();
    }

}
