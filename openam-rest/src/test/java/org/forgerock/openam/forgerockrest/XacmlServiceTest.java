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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest;

import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.authentication.service.protocol.RemoteHttpServletRequest;
import org.forgerock.openam.authentication.service.protocol.RemoteHttpServletResponse;
import org.forgerock.openam.forgerockrest.entitlements.StubPrivilege;
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Server;
import org.restlet.data.Disposition;
import org.restlet.data.Form;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.adapter.ServerCall;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.internal.ServletCall;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.INTERNAL_ERROR;
import static org.mockito.Mockito.*;
import static com.sun.identity.entitlement.xacml3.XACMLExportImport.ImportStep;

public class XacmlServiceTest {

    private static final ConcurrentMap<String, Object> REQUEST_ATTRIBUTES = new ConcurrentHashMap<String, Object>() {{
        put("realm", "/");
    }};

    private AdminTokenAction adminTokenAction;
    private XacmlService service;
    private XACMLExportImport importExport;
    private Debug debug;
    private Response response;
    private Request request;

    private Answer<SSOToken> ssoTokenAnswer = new Answer<SSOToken>() {
        @Override
        public SSOToken answer(InvocationOnMock invocation) throws Throwable {
            SSOToken ssoToken = mock(SSOToken.class);
            doReturn("my-uuid").when(ssoToken).getProperty(Constants.UNIVERSAL_IDENTIFIER);
            return ssoToken;
        }
    };
    private Form query;

    @BeforeMethod
    public void setup() throws Exception {
        this.importExport = mock(XACMLExportImport.class);
        this.debug = mock(Debug.class);
        this.adminTokenAction = mock(AdminTokenAction.class);
        doAnswer(ssoTokenAnswer).when(adminTokenAction).run();
        this.service = new XacmlServiceTestWrapper(importExport, adminTokenAction, this.debug, null, null);
        this.request = mock(Request.class);
        doReturn(REQUEST_ATTRIBUTES).when(request).getAttributes();
        this.response = mock(Response.class);
        service.setRequest(request);
        service.setResponse(response);
        query = new Form();
        service = spy(service);
        doReturn(query).when(service).getQuery();
    }

    @Test
    public void testImportXACML() throws Exception {
        //given
        Representation representation = mock(Representation.class);
        InputStream is = new ByteArrayInputStream("Hello World".getBytes());
        doReturn(is).when(representation).getStream();

        StubPrivilege privilege = new StubPrivilege();
        privilege.setName("fred");
        XACMLExportImport.ImportStep importStep = mock(XACMLExportImport.ImportStep.class);
        doReturn(XACMLExportImport.DiffStatus.ADD).when(importStep).getDiffStatus();
        doReturn(privilege).when(importStep).getPrivilege();

        List<ImportStep> steps = Arrays.asList(importStep);
        doReturn(steps).when(importExport).importXacml(eq("/"), eq(is), any(Subject.class), eq(false));

        //when
        Representation result = service.importXACML(representation);

        //then
        assertThat(result).isInstanceOf(JacksonRepresentation.class);
        Map<String, Object> resultMap = JsonValueBuilder.toJsonArray(result.getText()).get(0).asMap();
        assertThat(resultMap).contains(entry("status", "A"), entry("name", "fred"));
        verify(response).setStatus(Status.SUCCESS_OK);
    }

    @Test
    public void testImportXACMLDryRun() throws Exception {
        //given
        query.add("dryrun", "true");
        Representation representation = mock(Representation.class);
        InputStream is = new ByteArrayInputStream("Hello World".getBytes());
        doReturn(is).when(representation).getStream();

        StubPrivilege privilege = new StubPrivilege();
        privilege.setName("fred");
        XACMLExportImport.ImportStep importStep = mock(XACMLExportImport.ImportStep.class);
        doReturn(XACMLExportImport.DiffStatus.ADD).when(importStep).getDiffStatus();
        doReturn(privilege).when(importStep).getPrivilege();

        List<ImportStep> steps = Arrays.asList(importStep);
        doReturn(steps).when(importExport).importXacml(eq("/"), eq(is), any(Subject.class), eq(true));

        //when
        Representation result = service.importXACML(representation);

        //then
        assertThat(result).isInstanceOf(JacksonRepresentation.class);
        Map<String, Object> resultMap = JsonValueBuilder.toJsonArray(result.getText()).get(0).asMap();
        assertThat(resultMap).contains(entry("status", "A"), entry("name", "fred"));
        verify(response).setStatus(Status.SUCCESS_OK);
    }

    @Test
    public void testImportXACMLIOException() throws Exception {
        //given
        Representation representation = mock(Representation.class);
        doThrow(new IOException()).when(representation).getStream();

        try {
            //when
            service.importXACML(representation);

            //then
            fail("Expect exception");
        } catch (ResourceException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    public void testImportXACMLNoPolicies() throws Exception {
        //given
        Representation representation = mock(Representation.class);
        InputStream is = new ByteArrayInputStream("Hello World".getBytes());
        doReturn(is).when(representation).getStream();
        doReturn(Collections.emptyList()).when(importExport).importXacml(eq("/"), eq(is), any(Subject.class), eq(false));

        try {
            //when
            service.importXACML(representation);

            //then
            fail("Expect exception");
        } catch (ResourceException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(BAD_REQUEST);
            assertThat(e.getMessage()).isEqualTo("No policies found in XACML document");
        }
    }

    @Test
    public void testImportXACMLImportFailure() throws Exception {
        //given
        Representation representation = mock(Representation.class);
        InputStream is = new ByteArrayInputStream("Hello World".getBytes());
        doReturn(is).when(representation).getStream();
        EntitlementException failure = new EntitlementException(EntitlementException.JSON_PARSE_ERROR);
        doThrow(failure).when(importExport).importXacml(eq("/"), eq(is), any(Subject.class), eq(false));

        try {
            //when
            service.importXACML(representation);

            //then
            fail("Expect exception");
        } catch (ResourceException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(BAD_REQUEST);
            assertThat(e.getMessage()).isEqualTo("JSON Exception.");
        }
    }

    @Test
    public void testExportXACML() throws Exception {
        //given
        query.add(XacmlService.QUERY_PARAM_STRING, "test1");
        query.add(XacmlService.QUERY_PARAM_STRING, "test2");
        PolicySet policySet = new PolicySet();
        doReturn(policySet).when(importExport).exportXACML(eq("/"), any(Subject.class), any(List.class));

        //when
        Representation result = service.exportXACML();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.write(baos);
        String xml = new String(baos.toByteArray(), "UTF-8");

        //then
        assertThat(xml).contains("<PolicySet");
        assertThat(xml).contains("xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\"");
        verify(response).setStatus(Status.SUCCESS_OK);
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(importExport).exportXACML(eq("/"), any(Subject.class), listCaptor.capture());
        assertThat(listCaptor.getValue()).containsExactly("test1", "test2");
    }

    @Test
    public void testDispositionOfRootRealmExport() throws Exception {
        //given
        query.add(XacmlService.QUERY_PARAM_STRING, "test1");
        query.add(XacmlService.QUERY_PARAM_STRING, "test2");
        PolicySet policySet = new PolicySet();
        doReturn(policySet).when(importExport).exportXACML(eq("/"), any(Subject.class), any(List.class));

        //when
        Representation result = service.exportXACML("/");
        Disposition disposition = result.getDisposition();

        assertThat(disposition.getFilename()).isEqualTo("realm-policies.xml");
        assertThat(disposition.getType()).isEqualTo(disposition.TYPE_ATTACHMENT);
    }

    @Test
    public void testDispositionOfSubRealmExport() throws Exception {
        //given
        query.add(XacmlService.QUERY_PARAM_STRING, "test1");
        query.add(XacmlService.QUERY_PARAM_STRING, "test2");
        PolicySet policySet = new PolicySet();
        doReturn(policySet).when(importExport).exportXACML(eq("/"), any(Subject.class), any(List.class));

        //when
        Representation result = service.exportXACML("/sub");
        Disposition disposition = result.getDisposition();

        assertThat(disposition.getFilename()).isEqualTo("sub-realm-policies.xml");
        assertThat(disposition.getType()).isEqualTo(disposition.TYPE_ATTACHMENT);
    }

    @Test
    public void testDispositionOfSubSubRealmExport() throws Exception {
        //given
        query.add(XacmlService.QUERY_PARAM_STRING, "test1");
        query.add(XacmlService.QUERY_PARAM_STRING, "test2");
        PolicySet policySet = new PolicySet();
        doReturn(policySet).when(importExport).exportXACML(eq("/"), any(Subject.class), any(List.class));

        //when
        Representation result = service.exportXACML("/sub1/sub2");
        Disposition disposition = result.getDisposition();

        assertThat(disposition.getFilename()).isEqualTo("sub1-sub2-realm-policies.xml");
        assertThat(disposition.getType()).isEqualTo(disposition.TYPE_ATTACHMENT);
    }

    @Test
    public void testExportXACMLEntitlementException() throws Exception {
        //given
        EntitlementException ee = new EntitlementException(EntitlementException.JSON_PARSE_ERROR);
        doThrow(ee).when(importExport).exportXACML(eq("/"), any(Subject.class), any(List.class));

        try {
            //when
            Representation result = service.exportXACML();

            //then
            fail("Expect exception");
        } catch (ResourceException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(INTERNAL_ERROR);
            assertThat(e.getMessage()).isEqualTo("JSON Exception.");
        }
    }
}
