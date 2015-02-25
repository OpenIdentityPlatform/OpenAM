/*
 * Copyright 2014 ForgeRock, AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */

package org.forgerock.openam.license;

import java.util.Iterator;
import java.util.List;

/**
 * Represents a set of licenses that must be accepted in order for the product to be installed.
 *
 * @since 12.0.0
 */
public class LicenseSet implements Iterable<License> {
    private final List<License> licenses;

    /**
     * Initialises the license set with the given list of required licenses.
     *
     * @param licenses the licenses that are required.
     * @throws NullPointerException if the list of licenses is null.
     */
    public LicenseSet(List<License> licenses) {
        if (licenses == null) {
            throw new NullPointerException("Null license set");
        }
        this.licenses = licenses;
    }

    /**
     * Returns the list of licenses to be accepted.
     *
     * @return the list of licenses.
     */
    public List<License> getLicenses() {
        return licenses;
    }

    /**
     * Accepts all licenses in the license set.
     */
    public void acceptAll() {
        for (License license : licenses) {
            license.accept();
        }
    }

    /**
     * Checks whether all of the licenses in the license set have been accepted.
     * @return true if all licenses have been accepted, otherwise false.
     */
    public boolean isAccepted() {
        for (License license : licenses) {
            if (!license.isAccepted()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return an iterator over the required licenses, in order.
     */
    public Iterator<License> iterator() {
        return licenses.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof LicenseSet)) {
            return false;
        }

        LicenseSet that = (LicenseSet) o;

        return licenses.equals(that.licenses);
    }

    @Override
    public int hashCode() {
        return licenses.hashCode();
    }
}
