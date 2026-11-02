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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.xacml.v3.rest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import javax.security.auth.Subject;

import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.xacml.v3.DiffStatus;
import org.forgerock.openam.xacml.v3.ImportStep;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.ByteStreams;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

public class XacmlServiceTest {

    private XACMLExportImport importExport;
    private AdminTokenAction adminTokenAction;

    @BeforeMethod
    public void setUp() throws Exception {
        importExport = mock(XACMLExportImport.class);
        adminTokenAction = mock(AdminTokenAction.class);
        SSOToken adminToken = mock(SSOToken.class);
        given(adminToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("my-uuid");
        given(adminTokenAction.run()).willReturn(adminToken);
    }

    private XacmlService service(boolean permitted) {
        return new XacmlServiceTestWrapper(importExport, adminTokenAction, mock(Debug.class), mock(RestLog.class),
                mock(DelegationEvaluator.class), permitted);
    }

    private static Realm realm(String path) {
        Realm realm = mock(Realm.class);
        given(realm.asPath()).willReturn(path);
        return realm;
    }

    private static Context context(String realmPath) {
        return new RealmContext(new RootContext(), realm(realmPath));
    }

    private static Request post(String uri, String contentType, String body) throws URISyntaxException {
        Request request = new Request().setMethod("POST").setUri(uri);
        if (contentType != null) {
            request.getHeaders().put("Content-Type", contentType);
        }
        if (body != null) {
            request.setEntity(body.getBytes(StandardCharsets.UTF_8));
        }
        return request;
    }

    private static Request get(String uri) throws URISyntaxException {
        return new Request().setMethod("GET").setUri(uri);
    }

    private static ImportStep step(DiffStatus status, String name, String type) {
        ImportStep step = mock(ImportStep.class);
        given(step.getDiffStatus()).willReturn(status);
        given(step.getName()).willReturn(name);
        given(step.getType()).willReturn(type);
        return step;
    }

    private static String read(InputStream stream) throws IOException {
        return new String(ByteStreams.toByteArray(stream), StandardCharsets.UTF_8);
    }

    private static void assertXmlError(Response response, int code) throws IOException {
        assertThat(response.getStatus().getCode()).isEqualTo(code);
        ContentTypeHeader type = ContentTypeHeader.valueOf(response);
        assertThat(type.getType()).isEqualTo("application/xml");
        assertThat(type.getCharset()).isEqualTo(StandardCharsets.UTF_8);
        assertThat(response.getEntity().getString()).contains("<error>").contains("<code>" + code + "</code>");
    }

    @Test
    public void importReturnsTheStepsAsJson() throws Exception {
        List<ImportStep> steps = Arrays.asList(step(DiffStatus.ADD, "policy1", "privilege"),
                step(DiffStatus.UPDATE, "iPlanetAMWebAgentService", "application"),
                step(DiffStatus.UPDATE, "URL", "resourceType"));
        given(importExport.importXacml(eq("/"), any(InputStream.class), any(Subject.class), eq(false)))
                .willReturn(steps);

        Response response = service(true).importXACML(context("/"),
                post("http://localhost/xacml/policies", "application/xml", "<PolicySet/>"));

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/json");
        assertThat((List<Object>) response.getEntity().getJson()).containsExactly(
                object(field("status", "A"), field("name", "policy1"), field("type", "privilege")),
                object(field("status", "U"), field("name", "iPlanetAMWebAgentService"),
                        field("type", "application")),
                object(field("status", "U"), field("name", "URL"), field("type", "resourceType")));
    }

    @Test
    public void importReadsDryrunFromTheQuery() throws Exception {
        List<ImportStep> steps = singletonList(step(DiffStatus.ADD, "p", "privilege"));
        given(importExport.importXacml(any(), any(), any(), anyBoolean())).willReturn(steps);

        service(true).importXACML(context("/sub"),
                post("http://localhost/xacml/policies?dryrun=true", "application/xml", "<PolicySet/>"));

        verify(importExport).importXacml(eq("/sub"), any(InputStream.class), any(Subject.class), eq(true));
    }

    @Test
    public void importPassesTheBodyOfAFormEncodedPost() throws Exception {
        ArgumentCaptor<InputStream> body = ArgumentCaptor.forClass(InputStream.class);
        List<ImportStep> steps = singletonList(step(DiffStatus.ADD, "p", "privilege"));
        given(importExport.importXacml(any(), body.capture(), any(), anyBoolean())).willReturn(steps);

        Response response = service(true).importXACML(context("/"),
                post("http://localhost/xacml/policies?dryrun=true",
                        "application/x-www-form-urlencoded; charset=UTF-8", "<PolicySet a=\"b&c\"/>"));

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(read(body.getValue())).isEqualTo("<PolicySet a=\"b&c\"/>");
        verify(importExport).importXacml(eq("/"), any(), any(), eq(true));
    }

    @Test
    public void importPassesAnEmptyStreamForAnEmptyBody() throws Exception {
        ArgumentCaptor<InputStream> body = ArgumentCaptor.forClass(InputStream.class);
        given(importExport.importXacml(any(), body.capture(), any(), anyBoolean())).willReturn(emptyList());

        service(true).importXACML(context("/"), post("http://localhost/xacml/policies", "application/xml", null));

        assertThat(body.getValue()).isNotNull();
        assertThat(body.getValue().read()).isEqualTo(-1);
    }

    @Test
    public void importWithoutPoliciesAnswers400Xml() throws Exception {
        given(importExport.importXacml(any(), any(), any(), anyBoolean())).willReturn(emptyList());

        Response response = service(true).importXACML(context("/"),
                post("http://localhost/xacml/policies", "application/xml", "<PolicySet/>"));

        assertXmlError(response, 400);
        assertThat(response.getEntity().getString()).contains("No policies found in XACML document");
    }

    @Test
    public void importEntitlementFailureAnswers400Xml() throws Exception {
        given(importExport.importXacml(any(), any(), any(), anyBoolean()))
                .willThrow(new EntitlementException(EntitlementException.JSON_PARSE_ERROR));

        Response response = service(true).importXACML(context("/"),
                post("http://localhost/xacml/policies", "application/xml", "<PolicySet/>"));

        assertXmlError(response, 400);
        assertThat(response.getEntity().getString()).contains("JSON Exception.");
    }

    @Test
    public void importRuntimeFailureAnswers500Xml() throws Exception {
        given(importExport.importXacml(any(), any(), any(), anyBoolean()))
                .willThrow(new IllegalStateException("boom"));

        assertXmlError(service(true).importXACML(context("/"),
                post("http://localhost/xacml/policies", "application/xml", "<PolicySet/>")), 500);
    }

    /** Master's no-token import answers 500 this way: the message lookup of a code without a bundle key throws. */
    @Test
    public void importMessageLookupFailureAnswers500Xml() throws Exception {
        EntitlementException failure = mock(EntitlementException.class);
        given(failure.getLocalizedMessage(any(Locale.class)))
                .willThrow(new MissingResourceException("no key", "b", "500"));
        given(importExport.importXacml(any(), any(), any(), anyBoolean())).willThrow(failure);

        assertXmlError(service(true).importXACML(context("/"),
                post("http://localhost/xacml/policies", "application/xml", "<PolicySet/>")), 500);
    }

    @Test
    public void importWithoutPermissionAnswers403Xml() throws Exception {
        Response response = service(false).importXACML(context("/"),
                post("http://localhost/xacml/policies", "application/xml", "<PolicySet/>"));

        assertXmlError(response, 403);
        verify(importExport, never()).importXacml(any(), any(), any(), anyBoolean());
    }

    @Test
    public void exportReturnsAnXacmlAttachment() throws Exception {
        given(importExport.exportXACML(eq("/"), any(Subject.class), any(List.class))).willReturn(new PolicySet());

        Response response = service(true).exportXACML(context("/"), get("http://localhost/xacml/policies"));

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/xacml+xml; version=3.0");
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=realm-policies.xml");
        assertThat(response.getEntity().getString()).contains("<ns2:PolicySet")
                .contains("xmlns:ns2=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\"");
    }

    @Test
    public void exportNamesTheAttachmentAfterTheRealm() throws Exception {
        given(importExport.exportXACML(any(), any(Subject.class), any(List.class))).willReturn(new PolicySet());

        assertThat(service(true).exportXACML(context("/sub"), get("http://localhost/xacml/policies"))
                .getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=sub-realm-policies.xml");
        assertThat(service(true).exportXACML(context("/sub/subsub"), get("http://localhost/xacml/policies"))
                .getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=sub-subsub-realm-policies.xml");
    }

    @Test
    public void exportPassesEveryFilterValueInOrder() throws Exception {
        ArgumentCaptor<List> filters = ArgumentCaptor.forClass(List.class);
        given(importExport.exportXACML(eq("/sub"), any(Subject.class), filters.capture()))
                .willReturn(new PolicySet());

        service(true).exportXACML(context("/sub"),
                get("http://localhost/xacml/policies?filter=test1&filter=test2"));

        assertThat(filters.getValue()).containsExactly("test1", "test2");
    }

    @Test
    public void exportEntitlementFailureAnswers500Xml() throws Exception {
        given(importExport.exportXACML(any(), any(Subject.class), any(List.class)))
                .willThrow(new EntitlementException(EntitlementException.JSON_PARSE_ERROR));

        Response response = service(true).exportXACML(context("/"), get("http://localhost/xacml/policies"));

        assertXmlError(response, 500);
        assertThat(response.getEntity().getString()).contains("JSON Exception.");
    }

    @Test
    public void exportWithoutPermissionAnswers403Xml() throws Exception {
        assertXmlError(service(false).exportXACML(context("/"), get("http://localhost/xacml/policies")), 403);
        verify(importExport, never()).exportXACML(any(), any(), any());
    }

    @Test
    public void permissionCheckGrantedIsAudited() throws Exception {
        RestLog restLog = mock(RestLog.class);
        DelegationEvaluator evaluator = mock(DelegationEvaluator.class);
        XacmlService xacmlService = new XacmlService(importExport, adminTokenAction, mock(Debug.class), restLog,
                evaluator);
        SSOToken adminToken = mock(SSOToken.class);
        DelegationPermission delegationPermission = mock(DelegationPermission.class);
        given(evaluator.isAllowed(adminToken, delegationPermission, Collections.EMPTY_MAP)).willReturn(true);

        boolean result = xacmlService.checkPermission(delegationPermission, adminToken, "blah");

        assertThat(result).isTrue();
        verify(restLog).auditAccessGranted(anyString(), anyString(), anyString(), any(SSOToken.class));
    }

    @Test
    public void permissionCheckDeniedIsAudited() throws Exception {
        RestLog restLog = mock(RestLog.class);
        DelegationEvaluator evaluator = mock(DelegationEvaluator.class);
        XacmlService xacmlService = new XacmlService(importExport, adminTokenAction, mock(Debug.class), restLog,
                evaluator);
        SSOToken adminToken = mock(SSOToken.class);
        DelegationPermission delegationPermission = mock(DelegationPermission.class);
        given(evaluator.isAllowed(adminToken, delegationPermission, Collections.EMPTY_MAP)).willReturn(false);

        boolean result = xacmlService.checkPermission(delegationPermission, adminToken, "blah");

        assertThat(result).isFalse();
        verify(restLog).auditAccessDenied(anyString(), anyString(), anyString(), any(SSOToken.class));
    }
}
