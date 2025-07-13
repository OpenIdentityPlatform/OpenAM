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
 * Portions Copyrighted 2017-2025 3A Systems, LLC.
 */

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.LinkedHashMap;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.identity.common.configuration.MapValueParser;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.InvalidAttributeValueException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class to convert service configurations between XML and JSON
 * @since 13.0.0
 */
public class SmsJsonConverter {
    private final ServiceSchema schema;
    private final MapValueParser nameValueParser = new MapValueParser();
    private final Debug debug = Debug.getInstance("SmsJsonConverter");
    private final Map<String, AttributeSchemaConverter> attributeSchemaConverters = new HashMap<String, AttributeSchemaConverter>();

    private BiMap<String, String> attributeNameToResourceName;
    private BiMap<String, String> resourceNameToAttributeName;
    private Map<String, String> attributeNameToSection;
    private List<String> hiddenAttributeNames;
    private boolean initialised = false;

    @Inject
    public SmsJsonConverter(ServiceSchema schema) {
        this.schema = schema;
    }

    private synchronized void init() {
        if (initialised) {
            return;
        }
        attributeNameToResourceName = getAttributeNameToResourceName(schema);
        hiddenAttributeNames = getHiddenAttributeNames();

        for (Object attributeName : schema.getAttributeSchemaNames()) {
            AttributeSchemaConverter attributeSchemaConverter;

            final AttributeSchema attributeSchema = this.schema.getAttributeSchema((String) attributeName);
            final AttributeSchema.Syntax syntax = attributeSchema.getSyntax();

            attributeSchemaConverter = getAttributeSchemaValue(syntax);

            final String resourceName = attributeSchema.getResourceName();
            if (resourceName == null) {
                attributeSchemaConverters.put((String) attributeName, attributeSchemaConverter);
            } else {
                attributeSchemaConverters.put(resourceName, attributeSchemaConverter);
            }
        }

        resourceNameToAttributeName = attributeNameToResourceName.inverse();
        attributeNameToSection = getAttributeNameToSection();

        initialised = true;
    }

    private AttributeSchemaConverter getAttributeSchemaValue(AttributeSchema.Syntax syntax) {
        AttributeSchemaConverter attributeSchemaConverter;
        if (isBoolean(syntax)) {
            attributeSchemaConverter = new BooleanAttributeSchemaValue();
        } else if (isDouble(syntax)) {
            attributeSchemaConverter = new DoubleAttributeSchemaValue();
        } else if (isInteger(syntax)) {
            attributeSchemaConverter = new IntegerAttributeSchemaValue();
        } else if (isScript(syntax)) {
            attributeSchemaConverter = new ScriptAttributeSchemaValue();
        } else if (isPassword(syntax)) {
            attributeSchemaConverter = new PasswordAttributeSchemaValue();
        } else {
            attributeSchemaConverter = new StringAttributeSchemaValue();
        }
        return attributeSchemaConverter;
    }

    /**
     * Will validate the Map representation of the service configuration against the global serviceSchema and return a
     * corresponding JSON representation
     *
     * @param attributeValuePairs The schema attribute values.
     * @return Json representation of attributeValuePairs
     */
    public JsonValue toJson(Map<String, Set<String>> attributeValuePairs, boolean validate) {
        return toJson(null, attributeValuePairs, validate);
    }

    /**
     * Will validate the Map representation of the service configuration against the serviceSchema and return a
     * corresponding JSON representation
     *
     * @param attributeValuePairs The schema attribute values.
     * @param validate Should the attributes be validated.
     * @param parentJson The {@link JsonValue} to which the attributes should be added.
     * @return Json representation of attributeValuePairs
     */
    public JsonValue toJson(Map<String, Set<String>> attributeValuePairs, boolean validate, JsonValue parentJson) {
        return toJson(null, attributeValuePairs, validate, parentJson);
    }

    /**
     * Will validate the Map representation of the service configuration against the serviceSchema and return a
     * corresponding JSON representation
     *
     * @param realm The realm, or null if global.
     * @param attributeValuePairs The schema attribute values.
     * @param validate Should the attributes be validated.
     * @return Json representation of attributeValuePairs
     */
    public JsonValue toJson(String realm, Map<String, Set<String>> attributeValuePairs, boolean validate) {
        return toJson(realm, attributeValuePairs, validate, json(object()));
    }

    /**
     * Will validate the Map representation of the service configuration against the serviceSchema and return a
     * corresponding JSON representation
     *
     * @param realm The realm, or null if global.
     * @param attributeValuePairs The schema attribute values.
     * @param validate Should the attributes be validated.
     * @param parentJson The {@link JsonValue} to which the attributes should be added.
     * @return Json representation of attributeValuePairs
     */
    public JsonValue toJson(String realm, Map<String, Set<String>> attributeValuePairs, boolean validate,
            JsonValue parentJson) {

        if (!initialised) {
            init();
        }

        boolean validAttributes = true;
        if (validate) {
            try {
                if (realm == null) {
                    validAttributes = schema.validateAttributes(attributeValuePairs);
                } else {
                    validAttributes = schema.validateAttributes(attributeValuePairs, realm);
                }
            } catch (SMSException e) {
                debug.error("schema validation threw an exception while validating the attributes: realm=" + realm +
                        " attributes: " + attributeValuePairs, e);
                throw new JsonException("Unable to validate attributes", e);
            }
        }

        if (validAttributes) {
            for (String attributeName : attributeValuePairs.keySet()) {
                String jsonResourceName = attributeNameToResourceName.get(attributeName);

                String name;
                if (jsonResourceName != null) {
                    name = jsonResourceName;
                } else {
                    name = attributeName;
                }

                AttributeSchema attributeSchema = schema.getAttributeSchema(attributeName);

                if (shouldBeIgnored(attributeName)) {
                    continue;
                }

                AttributeSchema.Type type = attributeSchema.getType();
                final Set<String> object = attributeValuePairs.get(attributeName);

                Object jsonAttributeValue = null;

                if (type == null) {
                    throw new JsonException("Type not defined.");
                }

                AttributeSchemaConverter attributeSchemaConverter = attributeSchemaConverters.get(name);

                if (isASingleValue(type)) {
                    if (!object.isEmpty()) {
                        jsonAttributeValue = attributeSchemaConverter.toJson(object.iterator().next());
                    }
                } else if (containsMultipleValues(type)) {
                    if (isAMap(attributeSchema.getUIType())) {
                        Map<String, Object> map = new HashMap<String, Object>();

                        Iterator<String> itr = object.iterator();
                        while (itr.hasNext()) {
                            Pair<String, String> entry = nameValueParser.parse(itr.next());
                            if(entry != null) {
                                map.put(entry.getFirst(), attributeSchemaConverter.toJson(entry.getSecond()));
                            }
                        }
                        jsonAttributeValue = map;
                    } else {
                        List<Object> list = new ArrayList<Object>();

                        Iterator<String> itr = object.iterator();
                        while (itr.hasNext()) {
                            list.add(attributeSchemaConverter.toJson(itr.next()));
                        }
                        jsonAttributeValue = list;
                    }
                }

                String sectionName = attributeNameToSection.get(attributeName);
                if (sectionName != null) {
                    parentJson.putPermissive(new JsonPointer("/" + sectionName + "/" + name), jsonAttributeValue);
                } else {
                    parentJson.put(name, jsonAttributeValue);
                }
            }
        } else {
            throw new JsonException("Invalid attributes");
        }
        return parentJson;
    }

    private boolean isAMap(AttributeSchema.UIType type) {
        return AttributeSchema.UIType.MAPLIST.equals(type)
                || AttributeSchema.UIType.GLOBALMAPLIST.equals(type);
    }

    private boolean containsMultipleValues(AttributeSchema.Type type) {
        return type.equals(AttributeSchema.Type.LIST) || type.equals(AttributeSchema.Type.MULTIPLE_CHOICE);
    }

    private boolean isASingleValue(AttributeSchema.Type type) {
        return type.equals(AttributeSchema.Type.SINGLE) || type.equals(AttributeSchema.Type.SIGNATURE) || type
                .equals(AttributeSchema.Type.VALIDATOR) || type.equals(AttributeSchema.Type
                .SINGLE_CHOICE);
    }

    private boolean isDate(AttributeSchema.Syntax syntax) {
        return syntax.equals(AttributeSchema.Syntax.DATE);
    }

    private boolean isBoolean(AttributeSchema.Syntax syntax) {
        return syntax.equals(AttributeSchema.Syntax.BOOLEAN);
    }

    private boolean isInteger(AttributeSchema.Syntax syntax) {
        return syntax.equals(AttributeSchema.Syntax.NUMBER) || syntax.equals(AttributeSchema.Syntax.NUMBER_RANGE)
                || syntax.equals(AttributeSchema.Syntax.NUMERIC) || syntax.equals(AttributeSchema.Syntax.PERCENT);
    }

    private boolean isDouble(AttributeSchema.Syntax syntax) {
        return syntax.equals(AttributeSchema.Syntax.DECIMAL) || syntax.equals(AttributeSchema.Syntax
                .DECIMAL_NUMBER) || syntax.equals(AttributeSchema.Syntax.DECIMAL_RANGE);
    }

    private boolean isScript(AttributeSchema.Syntax syntax) {
        return syntax.equals(AttributeSchema.Syntax.SCRIPT);
    }

    private boolean isPassword(AttributeSchema.Syntax syntax) {
        return syntax.equals(AttributeSchema.Syntax.PASSWORD);
    }

    /**
     * Will validate the Json representation of the service configuration against the global serviceSchema,
     * and return a corresponding Map representation.
     *
     * @param jsonValue The request body.
     * @return Map representation of jsonValue
     */
    public Map<String, Set<String>> fromJson(JsonValue jsonValue) throws JsonException, BadRequestException {
        return fromJson(null, jsonValue);
    }

    /**
     * Will validate the Json representation of the service configuration against the serviceSchema for a realm,
     * and return a corresponding Map representation.
     *
     * @param jsonValue The request body.
     * @param realm The realm, or null if global.
     * @return Map representation of jsonValue
     */
    public Map<String, Set<String>> fromJson(String realm, JsonValue jsonValue) throws JsonException, BadRequestException {
        if (!initialised) {
            init();
        }

        Map<String, Set<String>> result = new HashMap<>();
        if (jsonValue == null || jsonValue.isNull()) {
            return result;
        }
        Map<String, Object> translatedAttributeValuePairs = getTranslatedAttributeValuePairs(jsonValue.asMap());

        for (String attributeName : translatedAttributeValuePairs.keySet()) {

            // Ignore _id field used to name resource when creating
            if (ResourceResponse.FIELD_CONTENT_ID.equals(attributeName)) {
                continue;
            }

            if (shouldBeIgnored(attributeName)) {
                continue;
            }

            if(shouldNotBeUpdated(attributeName)) {
                throw new BadRequestException("Invalid attribute, '" + attributeName + "', specified");
            }


            final Object attributeValue = translatedAttributeValuePairs.get(attributeName);
            Set<String> value = new HashSet<>();

            if (attributeValue instanceof HashMap) {
                final HashMap<String, Object> attributeMap = (HashMap<String, Object>) attributeValue;
                for (String name : attributeMap.keySet()) {
                    value.add("[" + name + "]=" + convertJsonToString(attributeName, attributeMap.get(name)));
                }
            } else if (attributeValue instanceof List) {
                List<Object> attributeArray = (ArrayList<Object>) attributeValue;
                for (Object val : attributeArray) {
                    value.add(convertJsonToString(attributeName, val));
                }
            } else if (attributeValue != null) {
                value.add(convertJsonToString(attributeName, attributeValue));
            }

            if (!value.isEmpty() || !isPassword(schema.getAttributeSchema(attributeName).getSyntax())) {
                result.put(attributeName, value);
            }
        }

        try {
            if (result.isEmpty() ||
                    (realm == null && schema.validateAttributes(result)) ||
                    (realm != null && schema.validateAttributes(result, realm))) {
                return result;
            } else {
                throw new JsonException("Invalid attributes");
            }
        } catch (InvalidAttributeValueException e) {
            throw new BadRequestException(e.getLocalizedMessage(), e);
        } catch (SMSException e) {
            throw new JsonException("Unable to validate attributes", e);
        }
    }

    private boolean shouldBeIgnored(String attributeName) {
        final AttributeSchema attributeSchema = schema.getAttributeSchema(attributeName);
        return (attributeSchema == null || StringUtils.isBlank(attributeSchema.getI18NKey())) || attributeName.equals
                ("_type") || hiddenAttributeNames.contains(attributeName);
    }

    private boolean shouldNotBeUpdated(String attributeName) {
        final AttributeSchema attributeSchema = schema.getAttributeSchema(attributeName);
        return attributeSchema == null || hiddenAttributeNames.contains(attributeName);
    }

    private AttributeSchemaConverter getAttributeConverter(String attributeName) {
        return attributeNameToResourceName.get(attributeName) == null ?
                attributeSchemaConverters.get(attributeName) : attributeSchemaConverters.get(attributeNameToResourceName.get(attributeName));
    }

    /**
     * Checks each attribute name in the json to see whether it is actually a resource name and therefore needs to be
     * translated back to the original attribute name
     *
     * @param attributeValuePairs The untranslated list of attribute names to values
     * @return The attribute name to value pairs with all their original attribute names
     */
    private Map<String, Object> getTranslatedAttributeValuePairs(Map<String, Object> attributeValuePairs) {
        Map<String, Object> translatedAttributeValuePairs = new HashMap<String, Object>();

        for (String attributeName : attributeValuePairs.keySet()) {
            if (!isASectionName(attributeName)) {
                String translatedAttributeName = resourceNameToAttributeName.get(attributeName);

                if (translatedAttributeName != null) {
                    translatedAttributeValuePairs.put(translatedAttributeName, attributeValuePairs.get(attributeName));
                } else {
                    translatedAttributeValuePairs.put(attributeName, attributeValuePairs.get(attributeName));
                }
            } else {
                translatedAttributeValuePairs.putAll(getTranslatedAttributeValuePairs((Map<String, Object>)
                        attributeValuePairs.get(attributeName)));
            }
        }
        return translatedAttributeValuePairs;
    }

    private boolean isASectionName(String attributeName) {
        return attributeNameToSection.containsValue(attributeName);
    }

    protected List<String> getHiddenAttributeNames() {
        ArrayList<String> hiddenAttributeNames = null;

        try {
            InputStream resource = getClass().getClassLoader().getResourceAsStream("amConsoleConfig.xml");
            Document doc = XMLUtils.getSafeDocumentBuilder(false).parse(resource);
            NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
                    "//consoleconfig/servicesconfig/consoleservice/@realmEnableHideAttrName", doc,
                    XPathConstants.NODESET);
            String rawList = nodes.item(0).getNodeValue();
            hiddenAttributeNames = new ArrayList<>(Arrays.asList(rawList.split(",")));
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hiddenAttributeNames;
    }

    protected Map<String, String> getAttributeNameToSection() {
        Map<String, String> result = new LinkedHashMap();
        String serviceSectionFilename = schema.getName() != null ? schema.getName() : schema.getServiceName();
        serviceSectionFilename = serviceSectionFilename + ".section.properties";

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(serviceSectionFilename);

        if (inputStream != null) {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                while ((line = reader.readLine()) != null) {
                    if (!(line.matches("^\\#.*") || line.isEmpty())) {
                        String[] attributeValue = line.split("=");
                        final String sectionName = attributeValue[0];
                        result.put(attributeValue[1], sectionName);
                    }
                }
            } catch (IOException e) {
                if (debug.errorEnabled()) {
                    debug.error("Error reading section properties file", e);
                }
            }
        }
        return result;
    }

    private BiMap<String, String> getAttributeNameToResourceName(ServiceSchema schema) {
        HashBiMap<String, String> result = HashBiMap.create();

        for (String attributeName : (Set<String>) schema.getAttributeSchemaNames()) {
            final String resourceName = schema.getAttributeSchema(attributeName).getResourceName();
            if (resourceName != null) {
                result.put(attributeName, resourceName);
            }
        }
        return result;
    }

    private String convertJsonToString(String attributeName, Object value) throws BadRequestException {
        AttributeSchemaConverter converter = getAttributeConverter(attributeName);
        try {
            return converter.fromJson(value);
        } catch (ClassCastException cce) {
            throw new BadRequestException("Invalid attribute value syntax: '" + value + "'", cce);
        }
    }

    private static interface AttributeSchemaConverter {
        Object toJson(String value);
        String fromJson(Object json);
    }

    private static class StringAttributeSchemaValue implements AttributeSchemaConverter {
        @Override
        public Object toJson(String value) {
            return value;
        }

        @Override
        public String fromJson(Object json) {
            return (String) json;
        }
    }

    private static class PasswordAttributeSchemaValue implements AttributeSchemaConverter {
        @Override
        public Object toJson(String value) {
            return null;
        }

        @Override
        public String fromJson(Object json) {
            return (String) json;
        }
    }

    private static class BooleanAttributeSchemaValue implements AttributeSchemaConverter {
        @Override
        public Object toJson(String value) {
            return Boolean.parseBoolean(value);
        }

        @Override
        public String fromJson(Object json) {
            return Boolean.toString((Boolean) json);
        }
    }

    private static class DoubleAttributeSchemaValue implements AttributeSchemaConverter {
        @Override
        public Object toJson(String value) {
            return Double.parseDouble(value);
        }

        @Override
        public String fromJson(Object json) {
            return Double.toString((Double) json);
        }
    }

    private static class IntegerAttributeSchemaValue implements AttributeSchemaConverter {
        @Override
        public Object toJson(String value) {
            return Integer.parseInt(value);
        }

        @Override
        public String fromJson(Object json) {
            return Integer.toString((Integer) json);
        }
    }

    private static class ScriptAttributeSchemaValue implements AttributeSchemaConverter {
        @Override
        public Object toJson(String value) {
            try {
                return Base64.encode(value.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("Script encoding failed", e);
            }
        }

        @Override
        public String fromJson(Object json) {
            String decodedValue = Base64.decodeAsUTF8String((String)json);
            return decodedValue == null ? "" : decodedValue;

        }
    }
}
