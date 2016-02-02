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
* Copyright 2014-2016 ForgeRock AS.
*/
package org.forgerock.openam.entitlement.rest.wrappers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.util.Set;
import javax.security.auth.Subject;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.Reject;

/**
 * Wrapper for the Jsonification of the Application class.
 *
 * Taking an instance of an {@link com.sun.identity.entitlement.Application} this class exposes the necessary
 * parts of that class to the Jackson {@link ObjectMapper} via annotations.
 *
 * You cannot set the complex object {@link ApplicationType} in a single simple operation as
 * it requires verification of the subject's ability to access the referenced ApplicationType.
 *
 * As such, a second call to this wrapper must be made after the initial parsing of the JSON to
 * populate the ApplicationType having first determined the {@link Subject} which is
 * requesting this operation.
 */
public class ApplicationWrapper implements Comparable<ApplicationWrapper> {

    @JsonIgnore
    private final Application application;

    @JsonIgnore
    private final ApplicationTypeManagerWrapper applicationTypeManagerWrapper;

    @JsonIgnore
    private static final Debug debug = Debug.getInstance("frRest");

    /**
     * Necessary default constructor for Json
     */
    public ApplicationWrapper() {
        application = new Application();
        applicationTypeManagerWrapper = new ApplicationTypeManagerWrapper();
    }

    public ApplicationWrapper(Application application, ApplicationTypeManagerWrapper appTypeManagerWrapper) {
        this.application = application;
        this.applicationTypeManagerWrapper = appTypeManagerWrapper;
    }

    @JsonIgnore
    public Application getApplication() {
        return application;
    }

    @JsonProperty("name")
    public void setName(String name) {
        application.setName(name);
    }

    @JsonProperty("name")
    public String getName() {
        return application.getName();
    }

    @JsonProperty("displayName")
    public void setDisplayName(String id) {
        application.setDisplayName(id);
    }

    @JsonProperty("displayName")
    public String getDisplayName() {
        return application.getDisplayName();
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        application.setDescription(description);
    }

    @JsonProperty("description")
    public String getDescription() {
        return application.getDescription();
    }

    /**
     * Retrieves the {@link ApplicationType} specified by the request, ensuring that the {@link Subject} supplied
     * has the authorization. Returns true if the ApplicationType was set successfully, false otherwise.
     *
     * @param mySubject The user's Subject
     * @param applicationTypeName The ApplicationType name to look up - the returning ApplicationType will be used
     * @return True if we set an ApplicationType on this Application
     */
    @JsonIgnore
    public boolean setApplicationType(Subject mySubject, String applicationTypeName) {
        final ApplicationType appType = applicationTypeManagerWrapper.getApplicationType(mySubject, applicationTypeName);

        if (appType != null) {
            application.setApplicationType(appType);
            return true;
        }

        return false;
    }

    @JsonProperty("applicationType")
    public String getApplicationType() {
        return application.getApplicationType().getName();
    }

    @JsonProperty("conditions")
    public void setConditions(Set<String> conditions) {
        application.setConditions(conditions);
    }

    @JsonProperty("conditions")
    public Set<String> getConditions() {
        return application.getConditions();
    }

    @JsonProperty("subjects")
    public void setSubjects(Set<String> subjects) {
        application.setSubjects(subjects);
    }

    @JsonProperty("subjects")
    public Set<String> getSubjects() {
        return application.getSubjects();
    }

    @JsonProperty("resourceTypeUuids")
    public void setResourceTypeUuids(final Set<String> resourceTypeUuids) {
        application.addAllResourceTypeUuids(resourceTypeUuids);
    }

    @JsonProperty("resourceTypeUuids")
    public Set<String> getResourceTypeUuids() {
        return application.getResourceTypeUuids();
    }

    @JsonProperty("entitlementCombiner")
    public void setEntitlementCombiner(String name) {
        Reject.ifNull(name);
        application.setEntitlementCombinerName(EntitlementUtils.getEntitlementCombiner(name));
    }

    @JsonProperty("entitlementCombiner")
    public String getEntitlementCombiner() {
        return application.getEntitlementCombiner().getName();
    }

    @JsonProperty("searchIndex")
    public void setSearchIndex(String classname)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        if (classname == null || classname.isEmpty()) {
            return;
        }

        try {
            application.setSearchIndex(Class.forName(classname));
        } catch (ClassNotFoundException e) {
            debug.warning("SearchIndex class not found.", e);
            throw e;
        } catch (InstantiationException e) {
            debug.warning("SearchIndex class unable to instantiate.", e);
            throw e;
        } catch (IllegalAccessException e) {
            debug.warning("SearchIndex class was illegally accessed.", e);
            throw e;
        }
    }

    @JsonProperty("searchIndex")
    public String getSearchIndex() {
        return application.getSearchIndex() == null ? null : application.getSearchIndex().getClass().getCanonicalName();
    }

    @JsonProperty("saveIndex")
    public void setSaveIndex(String classname)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        if (classname == null || classname.isEmpty()) {
            return;
        }

        try {
            application.setSaveIndex(Class.forName(classname));
        } catch (ClassNotFoundException e) {
            debug.warning("SaveIndex class not found.", e);
            throw e;
        } catch (InstantiationException e) {
            debug.warning("SaveIndex class unable to instantiate.", e);
            throw e;
        } catch (IllegalAccessException e) {
            debug.warning("SaveIndex class was illegally accessed.", e);
            throw e;
        }
    }

    @JsonProperty("saveIndex")
    public String getSaveIndex() {
        return application.getSaveIndex() == null ? null : application.getSaveIndex().getClass().getCanonicalName();
    }

    @JsonProperty("resourceComparator")
    public void setResourceComparator(String classname)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        if (classname == null || classname.isEmpty()) {
            return;
        }

        try {
            application.setResourceComparator(Class.forName(classname));
        } catch (ClassNotFoundException e) {
            debug.warning("ResourceComparator class not found.", e);
            throw e;
        } catch (InstantiationException e) {
            debug.warning("ResourceComparator class unable to instantiate.", e);
            throw e;
        } catch (IllegalAccessException e) {
            debug.warning("ResourceComparator class was illegally accessed.", e);
            throw e;
        }
    }

    @JsonProperty("resourceComparator")
    public String getResourceComparator() {
        return application.getResourceComparator(false) == null ? null :
                application.getResourceComparator(false).getClass().getCanonicalName();
    }

    @JsonProperty("attributeNames")
    public void setAttributeNames(Set<String> attributeNames) {
        application.setAttributeNames(attributeNames);
    }

    @JsonProperty("attributeNames")
    public Set<String> getAttributeNames() {
        return application.getAttributeNames();
    }

    @JsonProperty("createdBy")
    public void setCreatedBy(String createdBy) {
        application.setCreatedBy(createdBy);
    }

    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return application.getCreatedBy();
    }

    @JsonProperty("lastModifiedBy")
    public void setLastModifiedBy(String lastModifiedBy) {
        application.setLastModifiedBy(lastModifiedBy);
    }

    @JsonProperty("lastModifiedBy")
    public String getLastModifiedBy() {
        return application.getLastModifiedBy();
    }

    @JsonProperty("creationDate")
    public void setCreationDate(long creationDate) {
        application.setCreationDate(creationDate);
    }

    @JsonProperty("creationDate")
    public long getCreationDate() {
        return application.getCreationDate();
    }

    @JsonProperty("lastModifiedDate")
    public void setLastModifiedDate(long lastModifiedDate) {
        application.setLastModifiedDate(lastModifiedDate);
    }

    @JsonProperty("lastModifiedDate")
    public long getLastModifiedDate() {
        return application.getLastModifiedDate();
    }

    @JsonProperty("editable")
    public boolean isEditable() {
        return application.isEditable();
    }

    public JsonValue toJsonValue() throws EntitlementException {
        try {
            final ObjectMapper mapper = JsonValueBuilder.getObjectMapper();
            return JsonValueBuilder.toJsonValue(mapper.writeValueAsString(this));
        } catch (IOException e) {
            throw new EntitlementException(EntitlementException.INVALID_APPLICATION_CLASS);
        }
    }

    @Override
    public int compareTo(ApplicationWrapper that) {
        return this.getName().compareTo(that.getName());
    }

}
