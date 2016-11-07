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

package org.forgerock.openam.cts.api;

import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.Option;

/**
 * Options are intended to provide guidance to the CTS as to how it should perform the requested
 * operation. Typically this will be passed through to the backing implementation and handled in an
 * implementation specific way.
 * <p>
 * Options should be considered optional, in that the backing implementation may not be able to
 * support them and should signal this accordingly.
 * <p>
 * Options by their very nature need to be generic in what they represent. For example the
 * {@link #OPTIMISTIC_CONCURRENCY_CHECK_OPTION} is a concept which is then implemented in the
 * backing implementation in a specific way. Therefore the Option should be generic, with the value
 * it contains, specific.
 *
 * @since 14.0.0
 */
public final class CTSOptions {

    private CTSOptions() {
    }

    /**
     * Signals the CTS to perform an optimistic concurrency check before making a change to a CTS
     * token.
     *
     * <p>The value of the option will be used to base the concurrency check assertion on.</p>
     *
     * <p>Applicable for use with update and delete CTS operations.</p>
     *
     * @see org.forgerock.openam.cts.impl.ETagAssertionCTSOptionFunction
     */
    public static final Option<String> OPTIMISTIC_CONCURRENCY_CHECK_OPTION = Option.of(String.class, null);

    /**
     * Signals the CTS to perform a read of a CTS token on delete.
     *
     * <p>The value of the option will be used to determine if the token is read on the delete
     * operation.</p>
     *
     * <p>Applicable for use only with the delete CTS operation.</p>
     *
     * @see org.forgerock.openam.cts.impl.DeletePreReadOptionFunction
     */
    public static final Option<CoreTokenField[]> PRE_DELETE_READ_OPTION = Option.of(CoreTokenField[].class, null);
}
