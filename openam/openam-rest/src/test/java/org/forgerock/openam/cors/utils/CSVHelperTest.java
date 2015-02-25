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
package org.forgerock.openam.cors.utils;

import java.util.ArrayList;
import java.util.List;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;

public class CSVHelperTest {

    private CSVHelper csvHelper = new CSVHelper();

    @Test
    public void shouldConvertBackAndForth() {
        //given
        String list = "one,two,three,four";

        //when
        List<String> myList = csvHelper.csvStringToList(list, false);
        String result = csvHelper.listToCSVString(myList);

        //then
        assertEquals(result, list);
    }

    @Test
    public void shouldReturnEmptyStringFromNullList() {
        //given
        List<String> myList = null;

        //when
        String result = csvHelper.listToCSVString(myList);

        //then
        assertNotNull(result);
        assertEquals(result, "");
    }

    @Test
    public void shouldReturnSingleEmptyForNoCommaString() {
        //given
        String one = "one";

        //when
        List<String> myList = csvHelper.csvStringToList(one, false);

        //then
        assertEquals(myList.size(), 1);
        assertTrue(myList.contains("one"));
    }

    @Test
    public void shouldReturnEmptyStringFromEmptyList() {
        //given
        List<String> myList = new ArrayList<String>();

        //when
        String result = csvHelper.listToCSVString(myList);

        //then
        assertNotNull(result);
        assertEquals(result, "");
    }

    @Test
    public void shouldReturnCommaSeperatedListInString() {
        //given
        List<String> myList = new ArrayList<String>();
        myList.add("one");
        myList.add("two");

        //when
        String result = csvHelper.listToCSVString(myList);

        //then
        assertNotNull(result);
        assertEquals(result, "one,two");
    }

    @Test
    public void shouldReturnListOfElements() {
        //given
        String myElements = "one, two, three, four";

        //when
        List<String> myList = csvHelper.csvStringToList(myElements, false);

        //then
        assertNotNull(myList);
        assertEquals(myList.size(), 4);
        assertTrue(myList.contains("one"));
        assertTrue(myList.contains("two"));
        assertTrue(myList.contains("three"));
        assertTrue(myList.contains("four"));
    }

    @Test
    public void shouldNotLowerCaseListOfElements() {
        //given
        String myElements = "one, TWO, thRee, four";

        //when
        List<String> myList = csvHelper.csvStringToList(myElements, false);

        //then
        assertNotNull(myList);
        assertEquals(myList.size(), 4);
        assertTrue(myList.contains("one"));
        assertTrue(myList.contains("TWO"));
        assertTrue(myList.contains("thRee"));
        assertTrue(myList.contains("four"));
    }

    @Test
    public void shouldLowerCaseListOfElements() {
        String myElements = "one,TWO, thRee,four";

        //when
        List<String> myList = csvHelper.csvStringToList(myElements, true);

        //then
        assertNotNull(myList);
        assertEquals(myList.size(), 4);
        assertTrue(myList.contains("one"));
        assertTrue(myList.contains("two"));
        assertTrue(myList.contains("three"));
        assertTrue(myList.contains("four"));
    }

    @Test
    public void shouldReturnListOfElementsRegardlessOfSpaces() {
        //given
        String myElements = "one,         two, three,    four";

        //when
        List<String> myList = csvHelper.csvStringToList(myElements, false);

        //then
        assertNotNull(myList);
        assertEquals(myList.size(), 4);
        assertTrue(myList.contains("one"));
        assertTrue(myList.contains("two"));
        assertTrue(myList.contains("three"));
        assertTrue(myList.contains("four"));
    }

    @Test
    public void shouldReturnEmptyListFromNullString() {
        //given
        String myElements = null;

        //when
        List<String> myList = csvHelper.csvStringToList(myElements, false);

        //then
        assertNotNull(myList);
        assertEquals(myList.size(), 0);
    }

}
