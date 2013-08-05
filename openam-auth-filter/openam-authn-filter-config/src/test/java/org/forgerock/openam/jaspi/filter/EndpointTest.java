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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.filter;

import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class EndpointTest {

    @Test
    public void shouldEquals() {

        //Given
        Endpoint endpointOne = new Endpoint("URI", "HTTP_METHOD");
        Endpoint endpointTwo = new Endpoint("URI", "HTTP_METHOD", "QUERY_PARAM", "VALUE");
        Endpoint endpointThree = new Endpoint("URI", "HTTP_METHOD", "QUERY_PARAM", "VALUE", "VALUE2");

        //When
        boolean oneEqualsTwo = endpointOne.equals(endpointTwo);
        boolean oneEqualsThree = endpointOne.equals(endpointThree);
        boolean twoEqualsOne = endpointTwo.equals(endpointOne);
        boolean twoEqualsThree = endpointTwo.equals(endpointThree);
        boolean threeEqualsOne = endpointThree.equals(endpointOne);
        boolean threeEqualsTwo = endpointThree.equals(endpointTwo);

        //Then
        assertTrue(oneEqualsTwo);
        assertTrue(oneEqualsThree);
        assertTrue(twoEqualsOne);
        assertTrue(twoEqualsThree);
        assertTrue(threeEqualsOne);
        assertTrue(threeEqualsTwo);
    }

    @Test
    public void shouldHashCodesEqual() {

        //Given
        Endpoint endpointOne = new Endpoint("URI", "HTTP_METHOD");
        Endpoint endpointTwo = new Endpoint("URI", "HTTP_METHOD", "QUERY_PARAM", "VALUE");
        Endpoint endpointThree = new Endpoint("URI", "HTTP_METHOD", "QUERY_PARAM", "VALUE", "VALUE2");

        //When
        int oneHashCode = endpointOne.hashCode();
        int twoHashCode = endpointTwo.hashCode();
        int threeHashCode = endpointThree.hashCode();

        //Then
        assertEquals(oneHashCode, twoHashCode);
        assertEquals(oneHashCode, threeHashCode);
        assertEquals(twoHashCode, threeHashCode);
    }

    @Test
    public void shouldGetQueryParamMatchers() {

        //Given
        Endpoint endpoint = new Endpoint("URI", "HTTP_METHOD", "QUERY_PARAM", "VALUE", "VALUE2");

        //When
        Set<QueryParameterMatcher> queryParamMatchers = endpoint.getQueryParamMatchers();

        //Then
        assertEquals(queryParamMatchers.size(), 2);
    }

    @Test
    public void shouldGetEmptyQueryParamMatchersWhenOnlyQueryParamNameSet() {

        //Given
        Endpoint endpoint = new Endpoint("/URI_C", "POST", "QUERY_PARAM");

        //When
        Set<QueryParameterMatcher> queryParamMatchers = endpoint.getQueryParamMatchers();

        //Then
        assertTrue(queryParamMatchers.isEmpty());
    }
}
