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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.entitlement.utils.indextree.nodecontext;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for MapSearchContext.
 *
 * @author andrew.forrest@forgerock.com
 */
public class MapSearchContextTest {

    @Test
    public void exerciseSearchContext() {
        SearchContext context = new MapSearchContext();
        assertFalse(context.has(ContextKey.LEVEL_REACHED));

        context.add(ContextKey.LEVEL_REACHED, Boolean.TRUE);
        assertTrue(context.has(ContextKey.LEVEL_REACHED));
        assertNotNull(context.get(ContextKey.LEVEL_REACHED));
        assertEquals(context.get(ContextKey.LEVEL_REACHED), Boolean.TRUE);

        context.remove(ContextKey.LEVEL_REACHED);
        assertFalse(context.has(ContextKey.LEVEL_REACHED));
    }

}
