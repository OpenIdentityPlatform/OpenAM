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
 * Copyright 2016 ForgeRock AS.
 */
package com.sun.identity.install.tools.util;

import static org.assertj.core.api.Assertions.*;
import org.testng.annotations.Test;

/**
 * Test the RESTEndpoint class.
 */
public class RESTEndpointTest {

    @Test
    public void shouldTestSimplePath() {
        final String path = "<path>";

        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(path)
                .build();

        String toStringValue = endpoint.toString();
        String toPathValue = endpoint.getPath();

        assertThat(toStringValue).startsWith("path+params=" + path);
        assertThat(toPathValue).startsWith(path);
    }

    @Test
    public void shouldAppendTwoPaths() {
        final String path = "<path>";
        final String append = "<append>";

        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(path)
                .path(append)
                .build();

        String toStringValue = endpoint.toString();
        String toPathValue = endpoint.getPath();

        assertThat(toStringValue).startsWith("path+params=" + path + "/" + append);
        assertThat(toPathValue).startsWith(path + "/" + append);
    }

    @Test
    public void shouldAppendTwoPathsWithExtraSeparators() {
        final String path = "<path>";
        final String append = "<append>";

        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(path + "/")
                .path("/" + append)
                .build();

        String toStringValue = endpoint.toString();
        String toPathValue = endpoint.getPath();

        assertThat(toStringValue).startsWith("path+params=" + path + "/" + append);
        assertThat(toPathValue).startsWith(path + "/" + append);
    }

    @Test
    public void shouldAppendThreePathsWithExtraSeparators() {
        final String path = "<path>";
        final String append = "<append>";

        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path(path + "/")
                .path("/" + append + "/")
                .path("/" + append + "/")
                .build();

        String toPathValue = endpoint.getPath();

        assertThat(toPathValue).doesNotContain("//");
    }

    @Test
    public void shouldHandleMultipleParametersInCorrectOrder() {
        final String param1name = "param1";
        final String param1value = "value1";

        final String param2name = "param2";
        final String param2value = "value2";

        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .path("<path>")
                .path("<append>")
                .parameter(param1name, param1value)
                .parameter(param2name, param2value)
                .build();

        String toStringValue = endpoint.toString();
        String toPathValue = endpoint.getPath();

        assertThat(toStringValue).contains("?" + param1name + "=" + param1value);
        assertThat(toPathValue).contains("?" + param1name + "=" + param1value);
        assertThat(toStringValue).contains("&" + param2name + "=" + param2value);
        assertThat(toPathValue).contains("&" + param2name + "=" + param2value);

        // Ordering should be preserved with the parameters, even though it probably doesn't make a difference
        // in real life.
        assertThat(toStringValue.indexOf(param1name)).isLessThan(toStringValue.indexOf(param2name));
    }

    /**
     * Note that this actually checks the output of the toString function, which could be made to lie without effecting
     * the remainder of the functionality within the class.  However, using toString means we don't have to introduce
     * any other functions to access the http method.
     */
    @Test
    public void checkGetAndPost() {
        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder().build();

        // Check the default is POST
        String toStringValue = endpoint.toString();

        assertThat(toStringValue).contains("method: POST");
        assertThat(toStringValue).doesNotContain("method: GET");

        endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .get()
                .build();

        toStringValue = endpoint.toString();

        assertThat(toStringValue).contains("method: GET");
        assertThat(toStringValue).doesNotContain("method: POST");

        endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .post()
                .build();

        toStringValue = endpoint.toString();

        assertThat(toStringValue).contains("method: POST");
        assertThat(toStringValue).doesNotContain("method: GET");
    }

    @Test
    public void checkToStringHidesPasswordsInPostData() {
        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .build();

        // Check there is no post data
        String toStringValue = endpoint.toString();

        assertThat(toStringValue).doesNotContain(" post data ");

        String postData = "hello world";
        endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .postData(postData)
                .build();

        toStringValue = endpoint.toString();

        assertThat(toStringValue).contains(" post data " + postData);

        postData = "Password: secret";

        endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .postData(postData)
                .build();

        toStringValue = endpoint.toString();

        assertThat(toStringValue).doesNotContain(" post data " + postData);
        assertThat(toStringValue).contains("hidden");
    }

    @Test
    public void checkHeaders() {
        final String name1 = "<header-name1>";
        final String value1 = "<header-value1>";
        final String name2 = "<header-name2>";
        final String value2 = "<header-value2>";
        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .headers(name1, value1)
                .headers(name2, value2)
                .build();

        String toStringValue = endpoint.toString();

        int indexHeaders = toStringValue.indexOf("headers:");
        int indexName1 = toStringValue.indexOf(name1);
        int indexValue1 = toStringValue.indexOf(value1);
        int indexName2 = toStringValue.indexOf(name2);
        int indexValue2 = toStringValue.indexOf(value2);

        assertThat(indexHeaders).isNotEqualTo(-1);
        assertThat(indexName1).isNotEqualTo(-1);
        assertThat(indexValue1).isNotEqualTo(-1);
        assertThat(indexName2).isNotEqualTo(-1);
        assertThat(indexValue2).isNotEqualTo(-1);

        assertThat(indexName1).isGreaterThan(indexHeaders);
        assertThat(indexValue1).isGreaterThan(indexName1);
        assertThat(indexName2).isGreaterThan(indexName1);
        assertThat(indexName2).isGreaterThan(indexValue1);
        assertThat(indexValue2).isGreaterThan(indexName2);

        assertThat(toStringValue).contains(name1 + ": " + value1);
        assertThat(toStringValue).contains(name2 + ": " + value2);
    }

    @Test
    public void shouldHidePasswordsInHeaders() {
        final String name1 = "<header-name1>";
        final String value1 = "<header-value1>";
        final String name2 = "<userPasswordValue>";
        final String value2 = "should-not-appear";
        RESTEndpoint endpoint = new RESTEndpoint.RESTEndpointBuilder()
                .headers(name1, value1)
                .headers(name2, value2)
                .build();

        String toStringValue = endpoint.toString();

        assertThat(toStringValue).contains(name1);
        assertThat(toStringValue).contains(value1);
        assertThat(toStringValue).contains(name1 + ": " + value1);
        assertThat(toStringValue).contains(name2);
        assertThat(toStringValue).doesNotContain(value2);
    }
}
