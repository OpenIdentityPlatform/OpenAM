/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: PrivilegeResource.java,v 1.5 2009/12/15 00:44:19 veiming Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.entitlement.rest;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.entitlement.xacml3.SearchFilterFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import com.sun.identity.rest.ResourceBase;
import com.sun.identity.rest.RestException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * REST for privilege management
 */
@Path("/1/entitlement/privilege")
public class PrivilegeResource extends ResourceBase {
    public static final String RESULT = "results";

    @GET
    @Produces("application/json")
    public String privileges(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("filter") List filters
    ) {
        try {
            Subject caller = getCaller(request);
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, caller);
            Set<String> privilegeNames = pm.searchNames(
                    getFilters(filters));
            JSONObject jo = new JSONObject();
            jo.put(RESULT, privilegeNames);
            return createResponseJSONString(200, headers, jo);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("PrivilegeResource.privileges", e);
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            PrivilegeManager.debug.error("PrivilegeResource.privileges", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.error("PrivilegeResource.privileges", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    @POST
    @Produces("application/json")
    public String createPrivilege(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @FormParam("realm") @DefaultValue("/") String realm,
        @FormParam("privilege.json") String jsonString
    ) {
        try {
            Subject caller = getCaller(request);
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, caller);
            Privilege privilege = Privilege.getNewInstance(jsonString);
            pm.add(privilege);
            return createResponseJSONString(201, headers, "Created");
        } catch (JSONException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.createPrivilege", e);
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.createPrivilege", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.createPrivilege", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    @PUT
    @Produces("application/json")
    @Path("/{name}")
    public String modifyPrivilege(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @FormParam("realm") @DefaultValue("/") String realm,
        @FormParam("privilege.json") String jsonString,
        @PathParam("name") String name
    ) {
        try {
            Subject caller = getCaller(request);
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, caller);
            Privilege privilege = Privilege.getNewInstance(jsonString);
            pm.modify(privilege);
            return createResponseJSONString(200, headers, "OK");
        } catch (JSONException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.modify", e);
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.modify", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.modify", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{name}")
    public String privilege(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @PathParam("name") String name
    ) {
        try {
            Subject caller = getCaller(request);
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, caller);
            Privilege privilege = pm.findByName(name);
            JSONObject jo = new JSONObject();
            jo.put(RESULT, privilege.toMinimalJSONObject());
            return createResponseJSONString(200, headers, jo);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("PrivilegeResource.privilege", e);
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            PrivilegeManager.debug.error("PrivilegeResource.privilege", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.error("PrivilegeResource.privilege", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    @DELETE
    @Produces("application/json")
    @Path("/{name}")
    public String deletePrivilege(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @PathParam("name") String name
    ) {
        try {
            Subject caller = getCaller(request);
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, caller);
            pm.remove(name);
            return createResponseJSONString(200, headers, "OK");
        } catch (JSONException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.deletePrivilege", e);
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.deletePrivilege", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.error(
                "PrivilegeResource.deletePrivilege", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    private Set<SearchFilter> getFilters(List<String> filters)
        throws EntitlementException {
        if ((filters == null) || filters.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        Set<SearchFilter> results = new HashSet<SearchFilter>();
        for (String f : filters) {
            SearchFilter sf = getEqualSearchFilter(f);
            if (sf == null) {
                sf = getNumericSearchFilter(f, true);
                if (sf == null) {
                    sf = getNumericSearchFilter(f, false);
                }
            }
            if (sf != null) {
                results.add(sf);
            }
        }

        return results;
    }

    private SearchFilter getEqualSearchFilter(String f)
        throws EntitlementException {
        SearchFilter sf = null;
        int idx = f.indexOf('=');

        if (idx != -1) {
            String attrName = f.substring(0, idx);
            if ((attrName.equals(Privilege.LAST_MODIFIED_DATE_ATTRIBUTE)) ||
                (attrName.equals(Privilege.CREATION_DATE_ATTRIBUTE))) {
                try {
                    sf = new SearchFilter(SearchFilterFactory.getSearchAttribute(attrName),
                        Long.parseLong(f.substring(idx + 1)),
                        SearchFilter.Operator.EQUALS_OPERATOR);
                } catch (NumberFormatException e) {
                    String[] param = {f};
                    throw new EntitlementException(328, param);
                }
            } else {
                sf = new SearchFilter(SearchFilterFactory.getSearchAttribute(attrName), f.substring(idx + 1));
            }
        }
        return sf;
    }

    private SearchFilter getNumericSearchFilter(String f, boolean greaterThan)
        throws EntitlementException {
        SearchFilter sf = null;
        int idx = (greaterThan) ? f.indexOf('>') : f.indexOf('<');

        if (idx != -1) {
            String attrName = f.substring(0, idx);
            if ((attrName.equals(Privilege.LAST_MODIFIED_DATE_ATTRIBUTE)) ||
                (attrName.equals(Privilege.CREATION_DATE_ATTRIBUTE))) {
                try {
                    sf = new SearchFilter(SearchFilterFactory.getSearchAttribute(attrName),
                        Long.parseLong(f.substring(idx + 1)),
                        SearchFilter.Operator.EQUALS_OPERATOR);
                } catch (NumberFormatException e) {
                    String[] param = {f};
                    throw new EntitlementException(328, param);
                }
            } else {
                String[] param = {f};
                throw new EntitlementException(328, param);
            }
        }
        return sf;
    }
}
