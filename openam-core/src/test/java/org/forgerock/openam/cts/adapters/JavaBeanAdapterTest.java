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
 */

package org.forgerock.openam.cts.adapters;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.MapToJsonBytesConverter;
import org.forgerock.openam.tokens.MapToJsonStringConverter;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.name.Names;

public class JavaBeanAdapterTest extends GuiceTestCase {

    public static final String GUICED_VALUE = "tokenDetail";
    private JavaBeanAdapter<DummyBean> adapter;

    @Override
    public void configure(Binder binder) {
        binder.bind(ObjectMapper.class).annotatedWith(Names.named(CoreTokenConstants.OBJECT_MAPPER)).to(ObjectMapper.class);
        binder.bind(String.class).annotatedWith(Names.named(GuicedBean.GUICED_NAME)).toInstance(GUICED_VALUE);
    }

    @BeforeMethod
    public void setup() throws Exception {
        adapter = new JavaBeanAdapter<DummyBean>(DummyBean.class, new TokenIdFactory(null));
    }

    @Test
    public void testRoundTrip() throws Exception {
        //Given
        DummyBean b = new DummyBean();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("fred", Arrays.asList("one", "two"));
        b.setComplexField(map);
        b.setCounter(50);
        GregorianCalendar date = new GregorianCalendar(2014, 10, 30);
        b.setDate(date);
        b.setId("abc123");
        b.setName("my token");

        //When
        Token t = adapter.toToken(b);

        //Then
        assertThat(t.getAttributeNames()).containsOnly(
                CoreTokenField.BLOB,
                CoreTokenField.TOKEN_ID,
                CoreTokenField.STRING_ONE,
                CoreTokenField.INTEGER_ONE,
                CoreTokenField.DATE_ONE,
                CoreTokenField.TOKEN_TYPE
        );
        assertThat(t.getTokenId()).isEqualTo("abc123");
        assertThat(t.<Calendar>getAttribute(CoreTokenField.DATE_ONE).getTimeInMillis()).isEqualTo(date.getTimeInMillis());
        assertThat(t.<Integer>getAttribute(CoreTokenField.INTEGER_ONE)).isEqualTo(50);
        assertThat(t.<String>getAttribute(CoreTokenField.STRING_ONE)).isEqualTo("my token");
        assertThat(t.<byte[]>getAttribute(CoreTokenField.BLOB)).isEqualTo("{\"fred\":[\"one\",\"two\"]}".getBytes(Charset.forName("UTF-8")));

        //When
        DummyBean roundTrip = adapter.fromToken(t);

        //Then
        assertThat(roundTrip).isEqualTo(b);
    }

    @Test
    public void testGenerateId() throws Exception {
        //Given
        DummyBean b = new DummyBean();

        //When
        Token t = adapter.toToken(b);

        //Then
        assertThat(b.getId()).matches("[0-9a-f-]{36}");
        assertThat(b.getId()).isEqualTo(t.getTokenId());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNonGeneratedNullId() throws Exception {
        // Given
        JavaBeanAdapter<GuicedBean> guicedBeanAdapter = new JavaBeanAdapter<GuicedBean>(GuicedBean.class, null);

        GuicedBean b = new GuicedBean("123");

        // When
        guicedBeanAdapter.toToken(b);
    }

    @Test
    public void guicedTokenBean() throws Exception {
        // Given
        JavaBeanAdapter<GuicedBean> guicedBeanAdapter = new JavaBeanAdapter<GuicedBean>(GuicedBean.class, null);

        Token token = new Token("abc123", TokenType.GENERIC);
        token.setAttribute(CoreTokenField.STRING_ONE, "fred");

        // When
        GuicedBean bean = guicedBeanAdapter.fromToken(token);

        // Then
        assertThat(bean.guicedValue).isEqualTo(GUICED_VALUE);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void wrongTokenTypeFails() throws Exception {
        // Given
        Token token = new Token("abc123", TokenType.REST);

        // When
        adapter.fromToken(token);

        // Then exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullFailsFromToken() throws Exception {
        // Given

        // When
        adapter.fromToken(null);

        // Then exception
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullFailsToToken() throws Exception {
        // Given

        // When
        adapter.toToken(null);

        // Then exception
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void initialiseFailsWrongConverter() throws Exception {
        // When
        new JavaBeanAdapter<WrongConverterBean>(WrongConverterBean.class, null);

        // Then exception
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void initialiseFailsInvalidType() throws Exception {
        // When
        new JavaBeanAdapter<InvalidTypeBean>(InvalidTypeBean.class, null);

        // Then exception
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void initialiseFailsCannotGenerateNonId() throws Exception {
        // When
        new JavaBeanAdapter<CannotGenerateNonIdBean>(CannotGenerateNonIdBean.class, null);

        // Then exception
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void initialiseFailsNoId() throws Exception {
        // When
        new JavaBeanAdapter<NoIdBean>(NoIdBean.class, null);

        // Then exception
    }

    @Type(TokenType.GENERIC)
    public static class DummyBean {
        private Map<String, Object> complexField;
        private String id;
        @Field(field = CoreTokenField.STRING_ONE)
        private String name;
        private Integer counter;
        private Calendar date;
        private Long unusedField;

        @Field(field = CoreTokenField.BLOB, converter = MapToJsonBytesConverter.class)
        public Map<String, Object> getComplexField() {
            return complexField;
        }

        public void setComplexField(Map<String, Object> complexField) {
            this.complexField = complexField;
        }

        @Field(field = CoreTokenField.TOKEN_ID, generated = true)
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Field(field = CoreTokenField.STRING_ONE)
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Field(field = CoreTokenField.INTEGER_ONE)
        public Integer getCounter() {
            return counter;
        }

        public void setCounter(Integer counter) {
            this.counter = counter;
        }

        public Calendar getDate() {
            return date;
        }

        @Field(field = CoreTokenField.DATE_ONE)
        public void setDate(Calendar date) {
            this.date = date;
        }

        @Override
        public boolean equals(Object o) {
            DummyBean b = (DummyBean) o;
            return id.equals(b.id) &&
                    counter.equals(b.counter) &&
                    name.equals(b.name) &&
                    complexField.equals(b.complexField) &&
                    date.getTimeInMillis() == b.date.getTimeInMillis();
        }

        public Long getUnusedField() {
            return unusedField;
        }

        public void setUnusedField(Long unusedField) {
            this.unusedField = unusedField;
        }
    }

    @Type(TokenType.GENERIC)
    public static class WrongConverterBean {
        @Field(field = CoreTokenField.BLOB, converter = MapToJsonStringConverter.class)
        private Map<String, Object> complexField;
        @Field(field = CoreTokenField.TOKEN_ID)
        private String id;

        public Map<String, Object> getComplexField() {
            return complexField;
        }
        public void setComplexField(Map<String, Object> complexField) {
            this.complexField = complexField;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
    }

    @Type(TokenType.GENERIC)
    public static class CannotGenerateNonIdBean {
        @Field(field = CoreTokenField.STRING_ONE, generated = true)
        private String myString;
        @Field(field = CoreTokenField.TOKEN_ID)
        private String id;

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getMyString() {
            return myString;
        }
        public void setMyString(String myString) {
            this.myString = myString;
        }
    }

    @Type(TokenType.GENERIC)
    public static class NoIdBean {
        @Field(field = CoreTokenField.STRING_ONE)
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Type(TokenType.GENERIC)
    public static class InvalidTypeBean {
        @Field(field = CoreTokenField.BLOB)
        private Map<String, Object> complexField;
        @Field(field = CoreTokenField.TOKEN_ID)
        private String id;

        public Map<String, Object> getComplexField() {
            return complexField;
        }
        public void setComplexField(Map<String, Object> complexField) {
            this.complexField = complexField;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
    }

    @Type(TokenType.GENERIC)
    public static class GuicedBean {
        public static final String GUICED_NAME = "Token Detail";
        private final String guicedValue;
        @Field(field = CoreTokenField.TOKEN_ID)
        private String id;
        @Field(field = CoreTokenField.STRING_ONE)
        private String name;

        @Inject
        public GuicedBean(@Named(GUICED_NAME) String guicedValue) {
            this.guicedValue = guicedValue;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
    }
}