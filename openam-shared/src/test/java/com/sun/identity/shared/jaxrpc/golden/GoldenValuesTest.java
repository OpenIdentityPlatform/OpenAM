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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.shared.jaxrpc.golden;

import static org.assertj.core.api.Assertions.assertThat;

import com.iplanet.sso.SSOException;
import java.util.HashSet;
import java.util.List;
import org.testng.annotations.Test;

public class GoldenValuesTest {

    @Test
    public void describeIsStable() {
        assertThat(GoldenValues.describe(new Object[] {"arg1", null, new HashSet<>(List.of("set3"))}))
                .isEqualTo("[java.lang.String:arg1, null, java.util.HashSet:[set3]]");
    }

    @Test
    public void bundleExceptionsConstruct() {
        // SSOException resolves its message in the constructor: an unknown key throws MissingResourceException.
        assertThat(GoldenValues.exception(SSOException.class)).isExactlyInstanceOf(SSOException.class);
    }
}
