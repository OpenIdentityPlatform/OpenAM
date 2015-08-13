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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.oauth2.resources.labels;

import java.util.Set;
import org.forgerock.json.JsonValue;

import static org.forgerock.json.JsonValue.*;

/**
 * A bean representing a resource set label.
 */
public class ResourceSetLabel {

    private final String id;
    private String name;
    private final LabelType type;
    private final Set<String> resourceSetIds;

    public ResourceSetLabel(String id, String name, LabelType type, Set<String> resourceSetIds) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.resourceSetIds = resourceSetIds;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LabelType getType() {
        return type;
    }

    public Set<String> getResourceSetIds() {
        return resourceSetIds;
    }

    public void addResourceSetId(String resourceSetId) {
        resourceSetIds.add(resourceSetId);
    }

    public void removeResourceSetId(String resourceSetId) {
        resourceSetIds.remove(resourceSetId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceSetLabel that = (ResourceSetLabel) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (type != that.type) return false;
        return !(resourceSetIds != null ? !resourceSetIds.equals(that.resourceSetIds) : that.resourceSetIds != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (resourceSetIds != null ? resourceSetIds.hashCode() : 0);
        return result;
    }

    public JsonValue asJson() {
        JsonValue resourceSetLabel = json(object(
                field("_id", id),
                field("name", name),
                field("type", type)
        ));
        return resourceSetLabel;
    }
}
