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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.federation.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ForwardPathValidatorTest {

    @DataProvider
    public Object[][] safeForwardPaths() {
        return new Object[][] {
            {"/idpSSOInit.jsp"},
            {"/saml2/jsp/idpSSOInit.jsp?metaAlias=/idp&spEntityID=x"},
            {"/saml2/jsp/idpSSOInit.jsp?metaAlias=%2Fidp&goto=%2F..%2FWEB-INF"}, // query is not a path
            {"/console/base/AMAdminFrame"},
            {"/console/my%20page.jsp"},       // one legitimate encoding layer
            {"/console/a+b.jsp"},             // '+' is literal in a path
            {"/web-info/page.jsp"},           // only the real WEB-INF directory is reserved
            {"/a/b..c/d"},                    // ".." inside a segment is an ordinary name
            {"/console//base/AMAdminFrame"},  // collapses to an ordinary path
            {"/./console/base/AMAdminFrame"},
            {"/console/base;jsessionid=1/AMAdminFrame"},
            {"/XUI/#login/"},                 // the container drops the fragment: /XUI/
        };
    }

    @DataProvider
    public Object[][] unsafeForwardPaths() {
        return new Object[][] {
            {null},
            {""},
            {"idpSSOInit.jsp"},               // relative
            {"/WEB-INF/web.xml"},
            {"/web-inf/classes/x.properties"},
            {"/META-INF/MANIFEST.MF"},
            {"/foo/../WEB-INF/web.xml"},
            {"/foo/../../etc/passwd"},
            {"/foo/..;/WEB-INF/web.xml"},
            {"\\WEB-INF\\web.xml"},
            {"/foo\\..\\WEB-INF"},
            {"/foo/..?x=1"},
            {"/foo\0.jsp"},
            // The container decodes the dispatcher path once more before it
            // normalises it, so an encoded traversal must be refused as well.
            {"/x/%2e%2e/%2e%2e/WEB-INF/web.xml"},
            {"/x/%2E%2E/WEB-INF/web.xml"},
            {"/x/..%2f..%2fWEB-INF/web.xml"},
            {"/x/%2e%2e%2fWEB-INF/web.xml"},
            {"/x/..%5c..%5cWEB-INF/web.xml"},
            {"/x/..%3b/WEB-INF/web.xml"},
            {"/%57EB-INF/web.xml"},
            {"/%77eb-inf/web.xml"},
            {"/x/%00.jsp"},
            {"/x/%0d%0a.jsp"},
            // A second encoding layer is never a legitimate in-app path, and
            // a container that decodes twice would resolve it to a traversal.
            {"/x/%252e%252e/%252e%252e/WEB-INF/web.xml"},
            {"/x/%2525.jsp"},
            // A malformed escape makes the container throw; refuse it up front.
            {"/x/%zz.jsp"},
            {"/x/%2"},
            {"/x/100%.jsp"},
            // The container collapses "//", "/./" and ";params" before mapping,
            // so the reserved-directory check has to see the collapsed path.
            {"//WEB-INF/web.xml"},
            {"/./WEB-INF/web.xml"},
            {"/WEB-INF;x/web.xml"},
            {"/;/WEB-INF/web.xml"},
            {"/%2e/WEB-INF/web.xml"},
            {"/WEB-INF/"},
            {"/WEB-INF/./web.xml"},
            // request.getRequestDispatcher() drops a fragment before mapping.
            {"/WEB-INF#/x"},
            {"/WEB-INF/web.xml#x"},
            {"/x#/../WEB-INF/web.xml"},       // a ServletContext dispatcher keeps the fragment
            // An escape that decodes to a URL delimiter is never a plain in-app path.
            {"/WEB-INF%3fx/web.xml"},
            {"/WEB-INF%23/web.xml"},
            {"/x/\u007f.jsp"},
            {"/x/%7f.jsp"},
            {"/x/%c0%ae%c0%ae/WEB-INF/web.xml"},  // overlong UTF-8 is not a path
        };
    }

    @Test(dataProvider = "safeForwardPaths")
    public void acceptsOrdinaryInAppPaths(String path) {
        assertThat(ForwardPathValidator.isSafeForwardPath(path)).as(path).isTrue();
    }

    @Test(dataProvider = "unsafeForwardPaths")
    public void rejectsTraversalAndReservedDirectories(String path) {
        assertThat(ForwardPathValidator.isSafeForwardPath(path)).as(path).isFalse();
    }

    @DataProvider
    public Object[][] safeMetaAliases() {
        return new Object[][] {
            {"/idp"},
            {"/sp"},
            {"/myrealm/sp"},
            {"/my-realm/my_sp.1"},
            {"/my%20realm/sp"},               // raw request URI form of "/my realm/sp"
        };
    }

    @DataProvider
    public Object[][] unsafeMetaAliases() {
        return new Object[][] {
            {null},
            {""},
            {"/../../WEB-INF/web.xml"},
            {"/idp/../../WEB-INF/web.xml"},
            {"\\..\\..\\WEB-INF\\web.xml"},
            {"/idp\0"},
            {"/idp?x=/../WEB-INF"},
            {"/%2e%2e/%2e%2e/WEB-INF/web.xml"},
            {"/idp/..%2f..%2fWEB-INF/web.xml"},
            {"/%252e%252e/%252e%252e/WEB-INF/web.xml"},
            {"/idp%00"},
            {"/idp%zz"},
            {"/idp#/../../WEB-INF/web.xml"},
            {"/idp/%c0%ae%c0%ae/WEB-INF/web.xml"},
        };
    }

    @Test(dataProvider = "safeMetaAliases")
    public void acceptsOrdinaryMetaAliases(String metaAlias) {
        assertThat(ForwardPathValidator.isSafeMetaAlias(metaAlias)).as(metaAlias).isTrue();
    }

    @Test(dataProvider = "unsafeMetaAliases")
    public void rejectsMetaAliasesThatEscapeTheHandlerPath(String metaAlias) {
        assertThat(ForwardPathValidator.isSafeMetaAlias(metaAlias)).as(metaAlias).isFalse();
    }
}
