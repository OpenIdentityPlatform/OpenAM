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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.indextree;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.ServiceManagementDAO;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.forgerock.openam.core.DNWrapper;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for IndexTresServiceImpl.
 */
public class IndexTreeServiceImplTest {

    private static final String ROOT_SUFFIX = "dc=openam,dc=openidentityplatform,dc=org";

    private static final String REALM = "/test-realm";
    private static final String REALM_DN = "ou=test-realm," + ROOT_SUFFIX;
    private static final String SERVICE_DN = "ou=default,ou=OrganizationConfig,ou=1.0," +
            "ou=sunEntitlementIndexes,ou=services," + REALM_DN;

    private static final String REALM2 = "/some-other-test-realm";
    private static final String REALM_DN2 = "ou=some-other-test-realm," + ROOT_SUFFIX;
    private static final String SERVICE_DN2 = "ou=default,ou=OrganizationConfig,ou=1.0," +
            "ou=sunEntitlementIndexes,ou=services," + REALM_DN2;

    private static final String FILTER = "(sunserviceID=indexes)";

    private IndexTreeServiceImpl treeService;
    private IndexChangeManager manager;
    private PrivilegedAction<SSOToken> privilegedAction;
    private ServiceManagementDAO serviceManagementDAO;
    private ShutdownManager shutdownManager;
    private DNWrapper dnMapper;
    private SSOToken ssoToken;

    private Set<String> excludes;

    @BeforeMethod
    public void setUp() throws LdapException {
        // Create mock objects.
        manager = mock(IndexChangeManager.class);
        privilegedAction = mock(MockPrivilegedAction.class);
        serviceManagementDAO = mock(ServiceManagementDAO.class);
        dnMapper = mock(DNWrapper.class);
        shutdownManager = mock(ShutdownManager.class);
        ssoToken = mock(SSOToken.class);
        excludes = Collections.emptySet();

        treeService = new IndexTreeServiceImpl(
                manager, privilegedAction, serviceManagementDAO, dnMapper, shutdownManager);

        verify(shutdownManager).addShutdownListener(treeService);
        verify(manager).registerObserver(treeService);
    }

    /**
     * Carries out two searches for the same realm. Demonstrates that DAO search is only
     * invoked once, after which the results are cached within the tree structure.
     */
    @Test
    public void treeSearchSingleRealm() throws Exception {
        // Path indexes to return from the DAO search.
        List<SMSDataEntry> pathIndexes = new ArrayList<SMSDataEntry>();
        pathIndexes.add(new SMSDataEntry("{dn:somedn,attributeValues:{pathindex:[\"http://www.test.com\"]}}"));
        pathIndexes.add(new SMSDataEntry("{dn:somedn,attributeValues:{pathindex:[\"http://*.test.com\"]}}"));
        pathIndexes.add(new SMSDataEntry("{dn:somedn,attributeValues:{pathindex:[\"http://www.example.com/*\"]}}"));
        pathIndexes.add(new SMSDataEntry("{dn:somedn,attributeValues:{pathindex:[\"*\"]}}"));

        // Set up mock objects for a single search.
        when(dnMapper.orgNameToDN(REALM)).thenReturn(REALM_DN);
        when(privilegedAction.run()).thenReturn(ssoToken);
        when(serviceManagementDAO.checkIfEntryExists(SERVICE_DN, ssoToken)).thenReturn(true);
        when(serviceManagementDAO.search(ssoToken, SERVICE_DN, FILTER, 0, 0, false, false, excludes))
                .thenReturn(pathIndexes.iterator());

        // Execute the actual search for www.test.com url.
        Set<String> results = treeService.searchTree("http://www.test.com", REALM);

        // Verify the results.
        verify(dnMapper).orgNameToDN(REALM);
        verify(privilegedAction).run();
        verify(serviceManagementDAO).checkIfEntryExists(SERVICE_DN, ssoToken);
        verify(serviceManagementDAO).search(ssoToken, SERVICE_DN, FILTER, 0, 0, false, false, excludes);

        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test.com");
        expectedResults.add("http://*.test.com");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        // Execute the actual search for www.example.com url.
        results = treeService.searchTree("http://www.example.com/home", REALM);

        // Verify the results.
        verifyNoMoreInteractions(dnMapper, privilegedAction, serviceManagementDAO);

        expectedResults = new HashSet<String>();
        expectedResults.add("http://www.example.com/*");
        expectedResults.add("*");
        assertEquals(expectedResults, results);
    }

    /**
     * First carries out a two searches for test-realm. This demonstrates the use of the cached tree after the initial
     * search. Secondly carries out two further searches for some-other-test-realm. This demonstrates that a new search
     * is carried out for the new realm and that a separate cached tree is created.
     */
    @Test
    public void treeSearchMultipleRealms() throws Exception {
        // Path indexes to return from the DAO search for realm test-realm.
        List<SMSDataEntry> pathIndexes = new ArrayList<SMSDataEntry>();
        pathIndexes.add(new SMSDataEntry("{dn:somedn,attributeValues:{pathindex:[\"http://*.test.com\"]}}"));
        pathIndexes.add(new SMSDataEntry("{dn:somedn,attributeValues:{pathindex:[\"*\"]}}"));

        // Set up mock objects for search against test-realm.
        when(dnMapper.orgNameToDN(REALM)).thenReturn(REALM_DN);
        when(privilegedAction.run()).thenReturn(ssoToken);
        when(serviceManagementDAO.checkIfEntryExists(SERVICE_DN, ssoToken)).thenReturn(true);
        when(serviceManagementDAO.search(ssoToken, SERVICE_DN, FILTER, 0, 0, false, false, excludes))
                .thenReturn(pathIndexes.iterator());

        // Execute the actual search for www.test.com url.
        Set<String> results = treeService.searchTree("http://www.test.com", REALM);

        // Verify the results.
        verify(dnMapper).orgNameToDN(REALM);
        verify(privilegedAction).run();
        verify(serviceManagementDAO).checkIfEntryExists(SERVICE_DN, ssoToken);
        verify(serviceManagementDAO).search(ssoToken, SERVICE_DN, FILTER, 0, 0, false, false, excludes);

        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://*.test.com");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        // Execute the actual search for www.example.com url.
        results = treeService.searchTree("http://www.test.com", REALM);

        // Verify the results - cached tree is now used as opposed to carrying out another search.
        verifyNoMoreInteractions(dnMapper, privilegedAction, serviceManagementDAO);
        assertEquals(expectedResults, results);

        // Now for a second realm.

        // Path indexes to return from the DAO search for some-other-test-realm.
        pathIndexes = new ArrayList<SMSDataEntry>();
        pathIndexes.add(new SMSDataEntry("{dn:somedn,attributeValues:{pathindex:[\"http://www.example.com/*\"]}}"));

        // Set up mock objects for a search against some-other-test-realm.
        when(dnMapper.orgNameToDN(REALM2)).thenReturn(REALM_DN2);
        when(privilegedAction.run()).thenReturn(ssoToken);                                                              
        when(serviceManagementDAO.checkIfEntryExists(SERVICE_DN2, ssoToken)).thenReturn(true);
        when(serviceManagementDAO.search(ssoToken, SERVICE_DN2, FILTER, 0, 0, false, false, excludes))
                .thenReturn(pathIndexes.iterator());

        // Execute the actual search for www.test.com url.
        results = treeService.searchTree("http://www.example.com/home", REALM2);

        // Verify the results - additional search carried out for new realm.
        verify(dnMapper).orgNameToDN(REALM2);
        verify(privilegedAction, times(2)).run();
        verify(serviceManagementDAO).checkIfEntryExists(SERVICE_DN2, ssoToken);
        verify(serviceManagementDAO).search(ssoToken, SERVICE_DN2, FILTER, 0, 0, false, false, excludes);

        expectedResults = new HashSet<String>();
        expectedResults.add("http://www.example.com/*");
        assertEquals(expectedResults, results);

        // Execute the actual search for www.example.com url.
        results = treeService.searchTree("http://www.example.com/home", REALM2);

        // Verify the results - cached tree is now used as opposed to carrying out another search.
        verifyNoMoreInteractions(dnMapper, privilegedAction, serviceManagementDAO);
        assertEquals(expectedResults, results);
    }

    /**
     * Verifies that a search which returns no results, results in an empty set being returned.
     */
    @Test
    public void noSearchResultsReturnsEmptyList() throws Exception {
        // Path indexes to return from the DAO search.
        List<SMSDataEntry> emptyIndexes = Collections.emptyList();

        // Set up mock objects for a single search.
        when(dnMapper.orgNameToDN(REALM)).thenReturn(REALM_DN);
        when(privilegedAction.run()).thenReturn(ssoToken);
        when(serviceManagementDAO.checkIfEntryExists(SERVICE_DN, ssoToken)).thenReturn(true);
        // Returns empty list of results.
        when(serviceManagementDAO.search(ssoToken, SERVICE_DN, FILTER, 0, 0, false, false, excludes))
                .thenReturn(emptyIndexes.iterator());

        // Execute the actual search for www.test.com url.
        Set<String> results = treeService.searchTree("http://www.test.com", REALM);

        // Verify the results.
        verify(dnMapper).orgNameToDN(REALM);
        verify(privilegedAction).run();
        verify(serviceManagementDAO).checkIfEntryExists(SERVICE_DN, ssoToken);
        verify(serviceManagementDAO).search(ssoToken, SERVICE_DN, FILTER, 0, 0, false, false, excludes);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Verifies that a missing DN, results in an empty set being returned.
     */
    @Test
    public void missingDNReturnsEmptyList() throws Exception {
        // Set up mock objects for a single search.
        when(dnMapper.orgNameToDN(REALM)).thenReturn(REALM_DN);
        when(privilegedAction.run()).thenReturn(ssoToken);
        // State that the entry doesn't exist.
        when(serviceManagementDAO.checkIfEntryExists(SERVICE_DN, ssoToken)).thenReturn(false);

        // Execute the actual search for www.test.com url.
        Set<String> results = treeService.searchTree("http://www.test.com", REALM);

        // Verify the results.
        verify(dnMapper).orgNameToDN(REALM);
        verify(privilegedAction).run();
        verify(serviceManagementDAO).checkIfEntryExists(SERVICE_DN, ssoToken);
        verifyNoMoreInteractions(serviceManagementDAO);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }


    /**
     * Verify that shutdown causes any clean up, including the connection being closed.
     */
    @Test
    public void shutdownCleanUp() {

    }

    // Type marker interface.
    private static interface MockPrivilegedAction extends PrivilegedAction<SSOToken> {
    }


}
