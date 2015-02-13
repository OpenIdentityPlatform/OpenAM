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

package org.forgerock.openam.cts.adapters;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdGenerator;
import org.forgerock.openam.tokens.Converter;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;
import org.forgerock.util.Reject;

import com.google.inject.assistedinject.Assisted;

/**
 * A TokenAdapter that can adapt Java bean-compliant POJOs that have been annotated with the annotations in
 * org.forgerock.openam.tokens.
 * @param <T> The type of the POJO.
 */
public class JavaBeanAdapter<T> implements TokenAdapter<T> {

    private final Class<T> beanClass;
    private final TokenIdGenerator idGenerator;
    private TokenType tokenType;
    private final List<FieldDetails> fields = new ArrayList<FieldDetails>();
    private final Map<String, FieldDetails> fieldsMap = new HashMap<String, FieldDetails>();
    private FieldDetails idField;

    @Inject
    public JavaBeanAdapter(@Assisted Class<T> beanClass, TokenIdGenerator idGenerator) {
        Type type = beanClass.getAnnotation(Type.class);
        if (type == null || type.value() == null) {
            throw new IllegalArgumentException("Token class does not declare token type: " + beanClass.getName());
        }
        this.beanClass = beanClass;
        this.idGenerator = idGenerator;
        initialise();
    }

    /**
     * Process the annotations on the bean class, and throw exceptions for invalid configuration.
     */
    private void initialise() {
        this.tokenType = beanClass.getAnnotation(Type.class).value();
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(beanClass);
        } catch (IntrospectionException e) {
            throw new IllegalStateException("Could not introspect type " + beanClass.getName(), e);
        }
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            if (pd.getReadMethod() != null && pd.getWriteMethod() != null) {
                Method readMethod = pd.getReadMethod();
                Field f = readMethod.getAnnotation(Field.class);
                Method writeMethod = pd.getWriteMethod();
                if (f == null) {
                    f = writeMethod.getAnnotation(Field.class);
                }
                if (f == null) {
                    try {
                        java.lang.reflect.Field field = beanClass.getDeclaredField(pd.getName());
                        f = field.getAnnotation(Field.class);
                    } catch (NoSuchFieldException e) {
                        // fine - field isn't for storage in CTS.
                    }
                }
                if (f != null) {
                    CoreTokenField tokenField = f.field();
                    Class<?> attributeType = tokenField.getAttributeType();
                    Class<?> beanFieldType = readMethod.getReturnType();
                    Class<? extends Converter> converterType = f.converter();
                    if (converterType.equals(Converter.IdentityConverter.class) && !beanFieldType.equals(attributeType)) {
                        throw new IllegalStateException("Field " + pd.getDisplayName() + " does not have a compatible type" +
                                "and does not declare a converter");
                    }
                    validateConverterType(attributeType, beanFieldType, converterType);
                    Converter converter = InjectorHolder.getInstance(converterType);
                    boolean generated = f.generated();
                    FieldDetails field = new FieldDetails(tokenField, readMethod, writeMethod, converter, generated);
                    if (tokenField == CoreTokenField.TOKEN_ID) {
                        idField = field;
                    } else {
                        if (generated) {
                            throw new IllegalStateException("Non-id values cannot be generated: " + f.toString());
                        }
                        fields.add(field);
                        fieldsMap.put(pd.getName(), field);
                    }
                }
            }
        }
        if (idField == null) {
            throw new IllegalStateException("The bean class does not declare an ID field");
        }
    }

    /**
     * Checks that the converter can convert the source type to the token attribute type.
     */
    private void validateConverterType(Class<?> attributeType, Class<?> beanFieldType, Class<? extends Converter> converterType) {
        for (java.lang.reflect.Type t : converterType.getGenericInterfaces()) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                if (pt.getRawType().equals(Converter.class)) {
                    Class<?> from;
                    if (pt.getActualTypeArguments()[0] instanceof Class) {
                        from = (Class<?>) pt.getActualTypeArguments()[0];
                    } else if (pt.getActualTypeArguments()[0] instanceof ParameterizedType) {
                        from = (Class<?>) ((ParameterizedType) pt.getActualTypeArguments()[0]).getRawType();
                    } else {
                        throw new IllegalStateException("Can't work out what the converter type is");
                    }
                    java.lang.reflect.Type to = pt.getActualTypeArguments()[1];
                    if (!(isMatchingClassType(attributeType, to) || isMatchingArrayType(attributeType, to)) ||
                            !from.isAssignableFrom(beanFieldType)) {
                        throw new IllegalStateException("Incompatible converter types (" + from.getName() +
                                " -> " + to.toString() + "). Expected " + beanFieldType.getName() + " -> " +
                                attributeType.getName());
                    }
                }
            }
        }
    }

    private boolean isMatchingClassType(Class<?> attributeType, java.lang.reflect.Type to) {
        return !attributeType.isArray() && to instanceof Class && ((Class) to).isAssignableFrom(attributeType);
    }

    private boolean isMatchingArrayType(Class<?> attributeType, java.lang.reflect.Type to) {
        return attributeType.isArray() &&
                (to instanceof GenericArrayType &&
                ((GenericArrayType) to).getGenericComponentType().equals(attributeType.getComponentType())) ||
                (to instanceof Class && ((Class) to).isArray() &&
                        ((Class) to).getComponentType().equals(attributeType.getComponentType()));
    }

    @Override
    public Token toToken(T o) {
        Reject.ifTrue(o == null, "Object must not be null");
        String tokenId = (String) idField.read(o);
        if (tokenId == null && idField.generated) {
            tokenId = idGenerator.generateTokenId(null);
            idField.write(tokenId, o);
        } else if (tokenId == null) {
            throw new IllegalArgumentException("ID field is not generated and is null");
        }
        Token token = new Token(tokenId, tokenType);
        for (FieldDetails details : fields) {
            Object value = details.read(o);
            if (value != null) {
                token.setAttribute(details.tokenField, value);
            }
        }
        return token;
    }

    @Override
    public T fromToken(Token token) {
        Reject.ifTrue(token == null, "Object must not be null");
        if (token.getType() != tokenType) {
            throw new IllegalArgumentException("Wrong token type (" + token.getType().name() + ") - expecting " +
                    tokenType.name());
        }
        try {
            T bean = null;
            for (Constructor<?> constructor : beanClass.getConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    bean = (T) constructor.newInstance();
                    break;
                }
            }
            if (bean == null) {
                bean = InjectorHolder.getInstance(beanClass);
            }

            for (FieldDetails details : fields) {
                details.write(token.getValue(details.tokenField), bean);
            }
            idField.write(token.getValue(idField.tokenField), bean);

            return bean;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot construct bean class " + beanClass.getName(), e);
        }
    }

    /**
     * Use the bean mappings that have been parsed to turn a query keyed by bean property names into a query keyed by
     * token property names.
     * @param beanQuery The query keyed by bean property names.
     * @return The transformed query keyed by token property names.
     */
    public TokenFilter toTokenQuery(Map<String, Object> beanQuery, TokenFilter.Type type) {
        TokenFilterBuilder.FilterAttributeBuilder builder = new TokenFilterBuilder().type(type);
        if (type == TokenFilter.Type.AND) {
            builder.withAttribute(CoreTokenField.TOKEN_TYPE, tokenType);
        }
        for (Map.Entry entry : beanQuery.entrySet()) {
            builder.withAttribute(fieldsMap.get(entry.getKey()).tokenField, entry.getValue());
        }
        return builder.build();
    }

    private static class FieldDetails {
        private final CoreTokenField tokenField;
        private final Method readMethod;
        private final Method writeMethod;
        private final Converter converter;
        private final boolean generated;

        FieldDetails(CoreTokenField tokenField, Method readMethod, Method writeMethod,
                Converter converter, boolean generated) {
            this.tokenField = tokenField;
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
            this.converter = converter;
            this.generated = generated;
        }

        Object read(Object o) {
            try {
                return converter.convertFrom(readMethod.invoke(o));
            } catch (Exception e) {
                throw new IllegalStateException("Should be able to call getter", e);
            }
        }

        void write(Object value, Object bean) {
            try {
                writeMethod.invoke(bean, converter.convertBack(value));
            } catch (Exception e) {
                throw new IllegalStateException("Should be able to call setter", e);
            }
        }
    }
}
