package org.forgerock.openam.utils;

import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class JsonValueBuilderTest {

    @BeforeClass
    public void setUp() {

    }

    @Test
    public void shouldCreateJsonValue() {

        //Given
        List<String> list = new ArrayList<String>();
        list.add("LIST_VALUE_1");
        list.add("LIST_VALUE_2");
        list.add("LIST_VALUE_3");

        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("MAP_KEY_1", "MAP_VALUE_1");
        map.put("MAP_KEY_2", "MAP_VALUE_2");
        map.put("MAP_KEY_3", "MAP_VALUE_3");

        Map<String, String> mapParam = new LinkedHashMap<String, String>();
        mapParam.put("MAP_PARAM_KEY_1", "MAP_PARAM_VALUE_1");
        mapParam.put("MAP_PARAM_KEY_2", "MAP_PARAM_VALUE_2");

        JsonValue jsonValueParam1 = new JsonValue(mapParam);

        List<String> listParam = new ArrayList<String>();
        listParam.add("LIST_PARAM_VALUE_1");
        listParam.add("LIST_PARAM_VALUE_2");

        JsonValue jsonValueParam2 = new JsonValue(listParam);

        //When
        JsonValue jsonValue = JsonValueBuilder.jsonValue()
                .put("KEY1", 1)
                .put("KEY2", 4L)
                .put("KEY3", 34.56)
                .put("KEY4", list)
                .put("KEY5", map)
                .put("KEY6", "VALUE")
                .put("KEY7", jsonValueParam1)
                .put("KEY8", jsonValueParam2)
                .array("KEY9")
                .add("KEY9_VALUE_1")
                .addLast("KEY9_VALUE_2")
                .build();

        //Then
        JsonValue expectedJsonValue = getExpectedJsonValue();
        assertEquals(jsonValue.toString(), expectedJsonValue.toString());
    }

    private JsonValue getExpectedJsonValue() {

        Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
        rootMap.put("KEY1", 1);
        rootMap.put("KEY2", 4);
        rootMap.put("KEY3", 34.56);
        List<String> key4List = new ArrayList<String>();
        key4List.add("LIST_VALUE_1");
        key4List.add("LIST_VALUE_2");
        key4List.add("LIST_VALUE_3");
        rootMap.put("KEY4", key4List);
        Map<String, Object> key5Map = new LinkedHashMap<String, Object>();
        key5Map.put("MAP_KEY_1", "MAP_VALUE_1");
        key5Map.put("MAP_KEY_2", "MAP_VALUE_2");
        key5Map.put("MAP_KEY_3", "MAP_VALUE_3");
        rootMap.put("KEY5", key5Map);
        rootMap.put("KEY6", "VALUE");
        Map<String, Object> key7Map = new LinkedHashMap<String, Object>();
        key7Map.put("MAP_PARAM_KEY_1", "MAP_PARAM_VALUE_1");
        key7Map.put("MAP_PARAM_KEY_2", "MAP_PARAM_VALUE_2");
        rootMap.put("KEY7", key7Map);
        List<String> key8List = new ArrayList<String>();
        key8List.add("LIST_PARAM_VALUE_1");
        key8List.add("LIST_PARAM_VALUE_2");
        rootMap.put("KEY8", key8List);
        List<String> key9List = new ArrayList<String>();
        key9List.add("KEY9_VALUE_1");
        key9List.add("KEY9_VALUE_2");
        rootMap.put("KEY9", key9List);

        return new JsonValue(rootMap);
    }

    @Test
    public void shouldConvertJsonStringToJsonValue() throws IOException {

        //Given
        JsonValue expectedJsonValue = getExpectedJsonValue();

        //When
        JsonValue jsonValue = JsonValueBuilder.toJsonValue(expectedJsonValue.toString());

        //Then
        assertEquals(jsonValue.toString(), expectedJsonValue.toString());
    }
}
