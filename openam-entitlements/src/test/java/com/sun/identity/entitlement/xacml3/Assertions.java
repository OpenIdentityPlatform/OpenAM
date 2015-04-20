/*
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
 *
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ResourceAttribute;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Collection of assertion methods for unit tests.
 */
public final class Assertions {

    public static void assertAllPrivilegesEquivalent(Collection<Privilege> first, Collection<Privilege> second) {
        assertThat(first.size()).isEqualTo(second.size());

        Set<Privilege> sortedFirst = new TreeSet<Privilege>(new PrivilegeComparator());
        sortedFirst.addAll(first);

        Set<Privilege> sortedSecond = new TreeSet<Privilege>(new PrivilegeComparator());
        sortedSecond.addAll(second);

        Iterator<Privilege> firstIterator = sortedFirst.iterator();
        Iterator<Privilege> secondIterator = sortedSecond.iterator();
        while (firstIterator.hasNext()) {
            assertPrivilegesEquivalent(firstIterator.next(), secondIterator.next());
        }
    }

    public static void assertPrivilegesEquivalent(Privilege first, Privilege second) {
        if (first == null || second == null) {
            assertThat(first).isEqualTo(second);
            return;
        }
        assertThat(first.getName()).isEqualTo(second.getName());
        assertThat(first.getDescription()).isEqualTo(second.getDescription());
        assertThat(first.getCreatedBy()).isEqualTo(second.getCreatedBy());
        assertThat(first.getLastModifiedBy()).isEqualTo(second.getLastModifiedBy());
        assertThat(first.getCreationDate()).isEqualTo(second.getCreationDate());
        assertThat(first.getLastModifiedDate()).isEqualTo(second.getLastModifiedDate());
        assertThat(first.isActive()).isEqualTo(second.isActive());

        assertEntitlementsEquivalent(first.getEntitlement(), second.getEntitlement());
        assertEntitlementConditionsEquivalent(first.getCondition(), second.getCondition());
        assertEntitlementSubjectsEquivalent(first.getSubject(), second.getSubject());
        assertAllResourceAttributesEquivalent(first.getResourceAttributes(), second.getResourceAttributes());
        // TODO: Check application indexes?
    }

    public static void assertEntitlementsEquivalent(Entitlement first, Entitlement second) {
        if (first == null || second == null) {
            assertThat(first).isEqualTo(second);
            return;
        }
        assertThat(first.getName()).isEqualTo(second.getName());
        assertThat(first.getApplicationName()).isEqualTo(second.getApplicationName());
        assertThat(first.getResourceNames()).isEqualTo(second.getResourceNames());
        assertThat(first.getActionValues()).isEqualTo(second.getActionValues());
        assertThat(first.getAdvices()).isEqualTo(second.getAdvices());
        assertThat(first.getAttributes()).isEqualTo(second.getAttributes());

    }

    public static void assertEntitlementConditionsEquivalent(EntitlementCondition first, EntitlementCondition second) {
        if (first == null || second == null) {
            assertThat(first).isEqualTo(second);
            return;
        }
        assertThat(first.getDisplayType()).isEqualTo(second.getDisplayType());
        assertThat(first.getState()).isEqualTo(second.getState());
    }

    private static void assertEntitlementSubjectsEquivalent(EntitlementSubject first, EntitlementSubject second) {
        if (first == null || second == null) {
            assertThat(first).isEqualTo(second);
            return;
        }
        assertThat(first.isIdentity()).isEqualTo(second.isIdentity());
        assertThat(first.getRequiredAttributeNames()).isEqualTo(second.getRequiredAttributeNames());
        assertThat(first.getSearchIndexAttributes()).isEqualTo(second.getSearchIndexAttributes());
    }

    public static void assertAllResourceAttributesEquivalent(
            Collection<ResourceAttribute> first, Collection<ResourceAttribute> second) {

        assertThat(first.size()).isEqualTo(second.size());

        Set<ResourceAttribute> sortedFirst = new TreeSet<ResourceAttribute>(new ResourceAttributeComparator());
        sortedFirst.addAll(first);

        Set<ResourceAttribute> sortedSecond = new TreeSet<ResourceAttribute>(new ResourceAttributeComparator());
        sortedSecond.addAll(second);

        Iterator<ResourceAttribute> firstIterator = sortedFirst.iterator();
        Iterator<ResourceAttribute> secondIterator = sortedSecond.iterator();

        while (firstIterator.hasNext()) {
            assertResourceAttributesEquivalent(firstIterator.next(), secondIterator.next());
        }
    }

    public static void assertResourceAttributesEquivalent(ResourceAttribute first, ResourceAttribute second) {
        if (first == null || second == null) {
            assertThat(first).isEqualTo(second);
            return;
        }
        assertThat(first.getPropertyName()).isEqualTo(second.getPropertyName());
        assertThat(first.getPropertyValues()).isEqualTo(second.getPropertyValues());
        assertThat(first.getPResponseProviderName()).isEqualTo(second.getPResponseProviderName());
    }

    public static void assertReferralPrivilegesEquivalent(ReferralPrivilege first, ReferralPrivilege second) {
        if (first == null || second == null) {
            assertThat(first).isEqualTo(second);
            return;
        }
        assertThat(first.getName()).isEqualTo(second.getName());
        assertThat(first.getDescription()).isEqualTo(second.getDescription());
        assertThat(first.getCreatedBy()).isEqualTo(second.getCreatedBy());
        assertThat(first.getLastModifiedBy()).isEqualTo(second.getLastModifiedBy());
        assertThat(first.getCreationDate()).isEqualTo(second.getCreationDate());
        assertThat(first.getLastModifiedDate()).isEqualTo(second.getLastModifiedDate());
        assertThat(first.isActive()).isEqualTo(second.isActive());

        assertThat(first.getRealms()).isEqualTo(second.getRealms());
        assertThat(first.getMapApplNameToResources()).isEqualTo(second.getMapApplNameToResources());
        assertThat(first.getOriginalMapApplNameToResources()).isEqualTo(second.getOriginalMapApplNameToResources());
    }

    static class ResourceAttributeComparator implements Comparator<ResourceAttribute> {

        @Override
        public int compare(ResourceAttribute first, ResourceAttribute second) {
            return first.getState().compareTo(second.getState());
        }
    }

    static class PrivilegeComparator implements Comparator<Privilege> {

        @Override
        public int compare(Privilege first, Privilege second) {
            return first.getName().compareTo(second.getName());
        }
    }
}
