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
package com.sun.identity.plugin.log.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public class FedletLoggerTest {

    @Test
    public void everyParameterGoesOnItsOwnBracedLine() {
        assertThat(FedletLogger.formatMessage("LOGIN_SUCCESS", new String[] {"alice", "sp"}, "sess"))
                .isEqualTo("LOGIN_SUCCESS\n{alice}\n{sp}\n{sess}");
    }

    /** A line break inside a parameter must not leave the braces and start a line of its own. */
    @Test
    public void aLineBreakInAParameterIsWrittenAsAnEscape() {
        assertThat(FedletLogger.formatMessage("LOGIN_FAILED", new String[] {"alice\r\nSEVERE: forged\rx\ny"}, null))
                .isEqualTo("LOGIN_FAILED\n{alice\\r\\nSEVERE: forged\\rx\\ny}");
    }

    @Test
    public void aMessageWithoutParametersIsTheMessageId() {
        assertThat(FedletLogger.formatMessage("LOGIN_SUCCESS", null, null)).isEqualTo("LOGIN_SUCCESS");
        assertThat(FedletLogger.formatMessage("LOGIN_SUCCESS", new String[0], "sess")).isEqualTo("LOGIN_SUCCESS");
    }
}
