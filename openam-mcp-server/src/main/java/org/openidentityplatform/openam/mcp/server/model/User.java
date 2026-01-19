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
 * Copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.mcp.server.model;

import java.util.List;

public record User(String userName,
                   String familyName,
                   String givenName,
                   String name,
                   String mail,
                   String phone
) {
    public User(UserDTO userDTO) {
        this(userDTO.userName(),
                singleValue(userDTO.sn()),
                singleValue(userDTO.givenName()),
                singleValue(userDTO.cn()),
                singleValue(userDTO.mail()),
                singleValue(userDTO.telephoneNumber()));
    }

    private static String singleValue(List<String> vals) {
        if (vals == null || vals.isEmpty()) {
            return null;
        }
        return vals.iterator().next();
    }
}
