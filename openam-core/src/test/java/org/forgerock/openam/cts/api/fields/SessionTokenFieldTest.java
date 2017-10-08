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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.api.fields;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;

import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iplanet.dpro.session.service.InternalSession;

public class SessionTokenFieldTest {
    /**
     * Ensures that the latestAccessTime field is not renamed in the JSON, as the SessionTokenField class is used to
     * optimise cases where only that field is changed.
     */
    @Test
    public void shouldFailIfLastAccessTimeFieldIsRemoved() throws NoSuchFieldException {
        String fieldName = SessionTokenField.LATEST_ACCESS_TIME.getInternalSessionFieldName();
        Annotation annotation = InternalSession.class.getDeclaredField("latestAccessTimeInSeconds").getAnnotations()[0];
        assertThat(((JsonProperty) annotation).value()).isEqualTo(fieldName);
    }
}
