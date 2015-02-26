/**
 * Copyright 2013 ForgeRock, Inc.
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
package com.sun.identity.sm.ldap;

import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

import static org.testng.Assert.assertTrue;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TimedActionTests {
    @Test
    public void shouldTimeTask() {
        // Given
        final List<Long> times = new LinkedList<Long>();
            TimedAction action = new TimedAction(){
            @Override
            public void action() {
                times.add(System.currentTimeMillis());
            }
        };

        // When
        action.go();

        // Then
        assertTrue(times.size() > 0);
    }
}
