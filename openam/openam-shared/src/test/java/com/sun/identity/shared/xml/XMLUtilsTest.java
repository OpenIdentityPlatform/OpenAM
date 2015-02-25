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
package com.sun.identity.shared.xml;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.*;

public class XMLUtilsTest {

    @DataProvider(name = "invalid")
    public String[][] getInvalidData() {
        return new String[][]{
            {"\u0000hello\u0008\u000bworld\u001f"},
            {"\u0001hello\u0007\ufffeworld\uffff"},
            {"\u0002hello\u0006\u000cworld\u001e"},
            {"\u0003hello\u0005\u000eworld\u001d"},
            {"\u0000h\u0001e\u0002l\u0003l\u0004o\u0005w\u0006o\u0007r\u0008l\u000bd\u000c"}
        };
    }

    @Test(dataProvider = "invalid")
    public void invalidCharactersAreCorrectlyRemoved(String invalid) {
        assertThat(XMLUtils.removeInvalidXMLChars(invalid)).isEqualTo("helloworld");
    }

    @DataProvider(name = "escaping")
    public String[][] getEscapeData() {
        return new String[][]{
            {"<hello>", "&lt;hello&gt;"},
            {"<a>&lt;</a>", "&lt;a&gt;&amp;lt;&lt;/a&gt;"},
            {"\"world\"", "&quot;world&quot;"},
            {"'world'", "&apos;world&apos;"},
            {"\nhello\n", "&#xA;hello&#xA;"},
            {"\u0000<\u0001hello&world\u0002>", "&lt;hello&amp;world&gt;"}
        };
    }

    @Test(dataProvider = "escaping")
    public void specialCharactersAreCorrectlyEscaped(String content, String escaped) {
        assertThat(XMLUtils.escapeSpecialCharacters(content)).isEqualTo(escaped);
    }
}
