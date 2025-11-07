package org.openidentityplatform.openam.mcp.server.model;

import java.util.ArrayList;
import java.util.List;

public record Realm(String name,
                    boolean active,
                    String parentPath,
                    List<String> aliases) {
      public Realm(RealmDTO realmDTO) {
            this(realmDTO.name().equals("/") ? "root" : realmDTO.name(),
                    realmDTO.active(), realmDTO.parentPath(),
                    new ArrayList<>(realmDTO.aliases()));
      }
}