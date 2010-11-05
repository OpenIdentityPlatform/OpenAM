/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: StringSet.java,v 1.2 2009/10/14 17:42:05 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.util;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * A collection of strings that contains no duplicate values.
 *
 * @author Paul C. Bryan
 */
public class StringSet extends LinkedHashSet<String>
{
    /**
     * Creates a new, empty string set.
     */
    public StringSet() {
        super();
    }

    /**
     * Creates a new string set with the same elements as the specified
     * string collection.
     *
     * @param c the collection of strings whose elements are to be placed into this set.
     */
    public StringSet(Collection<String> c) {
        super();
        addAll(c);
    }
}

