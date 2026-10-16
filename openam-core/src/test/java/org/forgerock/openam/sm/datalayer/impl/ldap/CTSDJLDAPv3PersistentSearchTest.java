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
* Copyright 2016 ForgeRock AS.
* Portions Copyright 2026 3A Systems, LLC.
*/
package org.forgerock.openam.sm.datalayer.impl.ldap;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.opendj.ldap.LdapException.newLdapException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.HashSet;

import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.PersistentSearchRequestControl;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Responses;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.spi.LdapPromises;
import org.forgerock.util.promise.PromiseImpl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CTSDJLDAPv3PersistentSearchTest {

    CTSDJLDAPv3PersistentSearch pSearch;

    ConnectionFactory mockConnectionFactory;

    @BeforeMethod
    public void theSetup() { //you need this
        mockConnectionFactory = mock(ConnectionFactory.class);

        pSearch = new CTSDJLDAPv3PersistentSearch(3000, DN.rootDN(), Filter.alwaysFalse(),
                SearchScope.WHOLE_SUBTREE, mockConnectionFactory);
    }

    @AfterMethod
    public void tearDown() {
        pSearch.stopQuery();
    }

    @Test
    public void shouldNotInformListenersConnectionDownOnInitialStart() throws Exception {
        //given
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);
        pSearch.addContinuousQueryListener(mockListener);
        givenDirectoryAcceptsPersistentSearch(PromiseImpl.<Result, LdapException>create());

        //when
        pSearch.startQuery();

        //then
        verify(mockListener, never()).connectionLost();
    }

    @Test
    public void shouldInformListenersConnectionDownWhenSearchFails() throws Exception {
        //given
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);
        ContinuousQueryListener mockListener2 = mock(ContinuousQueryListener.class);
        pSearch.addContinuousQueryListener(mockListener);
        pSearch.addContinuousQueryListener(mockListener2);
        PromiseImpl<Result, LdapException> searchResult = PromiseImpl.create();
        givenDirectoryAcceptsPersistentSearch(searchResult);
        pSearch.startQuery();

        //when
        searchResult.handleException(newLdapException(ResultCode.UNAVAILABLE));

        //then
        verify(mockListener, times(1)).connectionLost();
        verify(mockListener2, times(1)).connectionLost();
    }

    @Test
    public void shouldRestartSearchWhenSearchFails() throws Exception {
        //given
        pSearch = new CTSDJLDAPv3PersistentSearch(1000, DN.rootDN(), Filter.alwaysFalse(),
                SearchScope.WHOLE_SUBTREE, mockConnectionFactory);
        pSearch.addContinuousQueryListener(mock(ContinuousQueryListener.class));
        PromiseImpl<Result, LdapException> searchResult = PromiseImpl.create();
        givenDirectoryAcceptsPersistentSearch(searchResult);
        pSearch.startQuery();

        //when
        searchResult.handleException(newLdapException(ResultCode.UNAVAILABLE));

        //then - every restart gets the same failed search, so the count keeps growing
        verify(mockConnectionFactory, timeout(5000).atLeast(2)).create();
    }

    @Test
    public void shouldRestartSearchWhenDirectoryRefusesSearch() throws Exception {
        //given
        pSearch = new CTSDJLDAPv3PersistentSearch(1000, DN.rootDN(), Filter.alwaysFalse(),
                SearchScope.WHOLE_SUBTREE, mockConnectionFactory);
        pSearch.addContinuousQueryListener(mock(ContinuousQueryListener.class));
        PromiseImpl<Result, LdapException> searchResult = PromiseImpl.create();
        givenDirectoryAcceptsPersistentSearch(searchResult);
        pSearch.startQuery();

        //when
        searchResult.handleException(newLdapException(ResultCode.INSUFFICIENT_ACCESS_RIGHTS));

        //then - the access rights may be fixed without restarting AM, so the search keeps being restarted,
        //also after the restarted search is refused again
        verify(mockConnectionFactory, timeout(5000).atLeast(3)).create();
    }

    @Test
    public void shouldInformListenersWhenSearchIsReestablished() throws Exception {
        //given
        pSearch = new CTSDJLDAPv3PersistentSearch(1000, DN.rootDN(), Filter.alwaysFalse(),
                SearchScope.WHOLE_SUBTREE, mockConnectionFactory);
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);
        pSearch.addContinuousQueryListener(mockListener);
        PromiseImpl<Result, LdapException> failedSearch = PromiseImpl.create();
        givenDirectoryAcceptsPersistentSearch(failedSearch, PromiseImpl.<Result, LdapException>create());
        pSearch.startQuery();

        //when
        failedSearch.handleException(newLdapException(ResultCode.UNAVAILABLE));

        //then - one call when the search fails, one when it is re-established
        verify(mockConnectionFactory, timeout(5000).times(2)).create();
        verify(mockListener, timeout(5000).times(2)).connectionLost();
    }

    @Test
    public void shouldNotRestartSearchWhenStopped() throws Exception {
        //given
        pSearch = new CTSDJLDAPv3PersistentSearch(1000, DN.rootDN(), Filter.alwaysFalse(),
                SearchScope.WHOLE_SUBTREE, mockConnectionFactory);
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);
        pSearch.addContinuousQueryListener(mockListener);
        PromiseImpl<Result, LdapException> searchResult = PromiseImpl.create();
        givenDirectoryAcceptsPersistentSearch(searchResult);
        pSearch.startQuery();
        pSearch.stopQuery();

        //when
        searchResult.handleException(newLdapException(ResultCode.CLIENT_SIDE_USER_CANCELLED));

        //then
        verify(mockListener, never()).connectionLost();
        verify(mockConnectionFactory, after(2000).times(1)).create();
    }

    private void givenDirectoryAcceptsPersistentSearch(PromiseImpl<Result, LdapException> searchResult)
            throws Exception {
        givenDirectoryAcceptsPersistentSearch(searchResult, searchResult);
    }

    private void givenDirectoryAcceptsPersistentSearch(PromiseImpl<Result, LdapException> firstSearchResult,
            PromiseImpl<Result, LdapException> laterSearchResult) throws Exception {
        Connection connection = mock(Connection.class);
        given(mockConnectionFactory.create()).willReturn(connection);
        given(connection.searchSingleEntry(any(SearchRequest.class))).willReturn(
                Responses.newSearchResultEntry("").addAttribute("supportedControl", PersistentSearchRequestControl.OID));
        given(connection.searchAsync(any(SearchRequest.class), any(), any(SearchResultHandler.class)))
                .willReturn(LdapPromises.asPromise(firstSearchResult), LdapPromises.asPromise(laterSearchResult));
    }

    @Test
    public void shouldStopAllListenersAndStopSearch() {
        //given
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);
        ContinuousQueryListener mockListener2 = mock(ContinuousQueryListener.class);
        pSearch.addContinuousQueryListener(mockListener);
        pSearch.addContinuousQueryListener(mockListener2);

        //when
        pSearch.stopQuery();

        //then
        assertThat(pSearch.hasListeners()).isFalse();
        assertThat(pSearch.isShutdown()).isTrue();
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldNotAllowAccessToAddListener() {
        //given
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);

        //when
        pSearch.addListener(mockListener, new HashSet());

        //then - caught by exception
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldNotAllowAccessToRemoveListener() {
        //given
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);

        //when
        pSearch.removeListener(mockListener);

        //then - caught by exception
    }

    @Test
    public void shouldAddContinuousQueryListener() {
        //given
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);

        assertThat(pSearch.hasListeners()).isFalse();

        //when
        pSearch.addContinuousQueryListener(mockListener);

        //then
        assertThat(pSearch.hasListeners()).isTrue();
    }

    @Test
    public void shouldRemoveContinuousQueryListener() {
        //given
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);

        assertThat(pSearch.hasListeners()).isFalse();

        //when
        pSearch.addContinuousQueryListener(mockListener);
        assertThat(pSearch.hasListeners()).isTrue();
        pSearch.removeContinuousQueryListener(mockListener);

        //then
        assertThat(pSearch.hasListeners()).isFalse();

    }
}
