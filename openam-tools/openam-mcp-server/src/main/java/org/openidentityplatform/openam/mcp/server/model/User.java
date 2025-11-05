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
