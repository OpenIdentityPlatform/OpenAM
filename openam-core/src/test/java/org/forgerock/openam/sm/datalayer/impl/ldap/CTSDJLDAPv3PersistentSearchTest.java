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
*/
package org.forgerock.openam.sm.datalayer.impl.ldap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;

import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
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

    @Test
    public void shouldInformListenersConnectionDown() {
        //given
        ContinuousQueryListener mockListener = mock(ContinuousQueryListener.class);
        ContinuousQueryListener mockListener2 = mock(ContinuousQueryListener.class);
        pSearch.addContinuousQueryListener(mockListener);
        pSearch.addContinuousQueryListener(mockListener2);

        //when
        pSearch.clearCaches();

        //then
        verify(mockListener, times(1)).connectionLost();
        verify(mockListener2, times(1)).connectionLost();
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
