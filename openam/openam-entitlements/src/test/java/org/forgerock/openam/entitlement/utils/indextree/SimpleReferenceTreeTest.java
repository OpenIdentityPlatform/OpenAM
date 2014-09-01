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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.entitlement.utils.indextree;

import org.forgerock.openam.entitlement.utils.indextree.nodefactory.TreeNodeFactory;
import org.forgerock.openam.entitlement.utils.indextree.treenodes.TreeNode;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for SimpleReferenceTree.
 *
 * @author andrew.forrest@forgerock.com
 */
public class SimpleReferenceTreeTest {

    private IndexRuleTree tree;

    @Before
    public void setUp() {
        tree = new SimpleReferenceTree();
    }

    /**
     * Processes some basic searches against the tree which contains simple rules.
     */
    @Test
    public void simpleRuleTree() {
        List<String> rules = new ArrayList<String>();
        rules.add("http://www.example.com");
        rules.add("http://www.test.com");
        rules.add("http://www.helloworld.com");
        tree.addIndexRules(rules);

        Set<String> results = tree.searchTree("http://www.example.com");
        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.example.com");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test.com");
        expectedResults.clear();
        expectedResults.add("http://www.test.com");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.someotherurl.com");
        assertTrue(results.isEmpty());
    }

    /**
     * Processes searches against the tree which contains rules with multilevel wildcards.
     */
    @Test
    public void multiLevelWildcardRules() {
        tree.addIndexRule("http://www.endurl.com/*");
        tree.addIndexRule("http://www.middleurl.com/*/home");
        tree.addIndexRule("http://www.substringurl.com/a*b/");
        tree.addIndexRule("*");

        Set<String> results = tree.searchTree("http://www.endurl.com");
        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.endurl.com/");
        expectedResults.clear();
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.endurl.com/home");
        expectedResults.clear();
        expectedResults.add("http://www.endurl.com/*");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.endurl.com/a/b/c/d");
        expectedResults.clear();
        expectedResults.add("http://www.endurl.com/*");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.middleurl.com/home");
        expectedResults.clear();
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.middleurl.com/abc");
        expectedResults.clear();
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.middleurl.com//home");
        expectedResults.clear();
        expectedResults.add("http://www.middleurl.com/*/home");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.middleurl.com/abc/home");
        expectedResults.clear();
        expectedResults.add("http://www.middleurl.com/*/home");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.middleurl.com/a/b/c/home");
        expectedResults.clear();
        expectedResults.add("http://www.middleurl.com/*/home");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.substringurl.com/");
        expectedResults.clear();
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.substringurl.com/ab/");
        expectedResults.clear();
        expectedResults.add("http://www.substringurl.com/a*b/");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.substringurl.com/ahellob/");
        expectedResults.clear();
        expectedResults.add("http://www.substringurl.com/a*b/");
        expectedResults.add("*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.substringurl.com/a/c/d/e/b/");
        expectedResults.clear();
        expectedResults.add("http://www.substringurl.com/a*b/");
        expectedResults.add("*");
        assertEquals(expectedResults, results);
    }

    /**
     * Processes searches against the tree which contains rules with single-level wildcards.
     */
    @Test
    public void singleLevelWildcardRules() {
        tree.addIndexRule("http://www.endurl.com/^");
        tree.addIndexRule("http://www.middleurl.com/^/home");
        tree.addIndexRule("http://www.substringurl.com/a^b/");
        tree.addIndexRule("^");

        Set<String> results = tree.searchTree("http://www.endurl.com");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.endurl.com/");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.endurl.com/home");
        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.endurl.com/^");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.endurl.com/a/b/c/d");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.middleurl.com/home");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.middleurl.com/abc");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.middleurl.com//home");
        expectedResults.clear();
        expectedResults.add("http://www.middleurl.com/^/home");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.middleurl.com/abc/home");
        expectedResults.clear();
        expectedResults.add("http://www.middleurl.com/^/home");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.middleurl.com/a/b/c/home");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.substringurl.com/");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.substringurl.com/ab/");
        expectedResults.clear();
        expectedResults.add("http://www.substringurl.com/a^b/");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.substringurl.com/ahellob/");
        expectedResults.clear();
        expectedResults.add("http://www.substringurl.com/a^b/");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.substringurl.com/a/c/d/e/b/");
        assertTrue(results.isEmpty());

        results = tree.searchTree("www.someurl.com");
        expectedResults.clear();
        expectedResults.add("^");
        assertEquals(expectedResults, results);
    }

    /**
     * Validates the behaviour of multilevel wildcards in the context question marks.
     */
    @Test
    public void queryStringMultiLevelWildcardRules() {
        tree.addIndexRule("http://www.test1.com/?");
        tree.addIndexRule("http://www.test2.com/*?");
        tree.addIndexRule("http://www.test3.com/?*");
        tree.addIndexRule("http://www.test4.com/*?*");

        Set<String> results = tree.searchTree("http://www.test1.com/?");
        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test1.com/?");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com/?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test2.com/abc?");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com/*?");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com/a/b/c?");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com/*?");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test3.com/?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test3.com/?abc");
        expectedResults.clear();
        expectedResults.add("http://www.test3.com/?*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test3.com/?a/b/c");
        expectedResults.clear();
        expectedResults.add("http://www.test3.com/?*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test4.com/?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/?abc");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/abc?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/abc?def");
        expectedResults.clear();
        expectedResults.add("http://www.test4.com/*?*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test4.com/a/b/c?d/e/f");
        expectedResults.clear();
        expectedResults.add("http://www.test4.com/*?*");
        assertEquals(expectedResults, results);
    }

    /**
     * Validates the behaviour of single-level wildcards in the context question marks.
     */
    @Test
    public void queryStringSingleLevelWildcardRules() {
        tree.addIndexRule("http://www.test1.com/?");
        tree.addIndexRule("http://www.test2.com/^?");
        tree.addIndexRule("http://www.test3.com/?^");
        tree.addIndexRule("http://www.test4.com/^?^");

        Set<String> results = tree.searchTree("http://www.test1.com/?");
        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test1.com/?");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com/?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test2.com/abc?");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com/^?");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com/a/b/c?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test3.com/?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test3.com/?abc");
        expectedResults.clear();
        expectedResults.add("http://www.test3.com/?^");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test3.com/?a/b/c");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/?abc");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/abc?");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/abc?def");
        expectedResults.clear();
        expectedResults.add("http://www.test4.com/^?^");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test4.com/a/b/c?d/e/f");
        assertTrue(results.isEmpty());
    }

    /**
     * Validates the behaviour of multilevel wildcards in the context bookmarks.
     */
    @Test
    public void bookmarkMultiLevelWildcardRules() {
        tree.addIndexRule("http://www.test1.com/#");
        tree.addIndexRule("http://www.test2.com/*#");
        tree.addIndexRule("http://www.test3.com/#*");
        tree.addIndexRule("http://www.test4.com/*#*");

        Set<String> results = tree.searchTree("http://www.test1.com/#");
        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test1.com/#");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com/#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test2.com/abc#");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com/*#");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com/a/b/c#");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com/*#");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test3.com/#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test3.com/#abc");
        expectedResults.clear();
        expectedResults.add("http://www.test3.com/#*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test3.com/#a/b/c");
        expectedResults.clear();
        expectedResults.add("http://www.test3.com/#*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test4.com/#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/#abc");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/abc#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/abc#def");
        expectedResults.clear();
        expectedResults.add("http://www.test4.com/*#*");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test4.com/a/b/c#d/e/f");
        expectedResults.clear();
        expectedResults.add("http://www.test4.com/*#*");
        assertEquals(expectedResults, results);
    }

    /**
     * Validates the behaviour of single-level wildcards in the context bookmarks.
     */
    @Test
    public void bookmarkSingleLevelWildcardRules() {
        tree.addIndexRule("http://www.test1.com/#");
        tree.addIndexRule("http://www.test2.com/^#");
        tree.addIndexRule("http://www.test3.com/#^");
        tree.addIndexRule("http://www.test4.com/^#^");

        Set<String> results = tree.searchTree("http://www.test1.com/#");
        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test1.com/#");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com/#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test2.com/abc#");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com/^#");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com/a/b/c#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test3.com/#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test3.com/#abc");
        expectedResults.clear();
        expectedResults.add("http://www.test3.com/#^");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test3.com/#a/b/c");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/#abc");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/abc#");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test4.com/abc#def");
        expectedResults.clear();
        expectedResults.add("http://www.test4.com/^#^");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test4.com/a/b/c#d/e/f");
        assertTrue(results.isEmpty());
    }

    @Test
    public void verifyUsageOfCustomFactory() {
        String path = "abcdefghijklmnopqrstuvwxyz";

        // Record mock interactions.
        TreeNodeFactory factory = mock(TreeNodeFactory.class);
        when(factory.getRootNode()).thenReturn(mock(TreeNode.class));

        for (char pathChar : path.toCharArray()) {
            when(factory.getTreeNode(pathChar)).thenReturn(mock(TreeNode.class));
        }

        // Create actual tree.
        tree = new SimpleReferenceTree(factory);
        tree.addIndexRule(path);

        // Verify factory usage.
        verify(factory).getRootNode();
        for (char pathChar : path.toCharArray()) {
            verify(factory).getTreeNode(pathChar);
        }
    }

    @Test
    public void removalOfRules() {
        tree.addIndexRule("http://www.test1.com");
        tree.addIndexRule("http://www.test2.com");
        tree.addIndexRule("http://www.test1.com");

        Set<String> results = tree.searchTree("http://www.test1.com");
        Set<String> expectedResults = new HashSet<String>();
        expectedResults.add("http://www.test1.com");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test3.com");
        assertTrue(results.isEmpty());

        // Now remove tree entry.
        tree.removeIndexRule("http://www.test1.com");

        results = tree.searchTree("http://www.test1.com");
        expectedResults.clear();
        expectedResults.add("http://www.test1.com");
        assertEquals(expectedResults, results);

        results = tree.searchTree("http://www.test2.com");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com");
        assertEquals(expectedResults, results);

        // Now remove tree entry the other entry.
        tree.removeIndexRule("http://www.test1.com");

        results = tree.searchTree("http://www.test1.com");
        assertTrue(results.isEmpty());

        results = tree.searchTree("http://www.test2.com");
        expectedResults.clear();
        expectedResults.add("http://www.test2.com");
        assertEquals(expectedResults, results);
    }

    @Test
    public void printTreeString() {
        tree.addIndexRule("abc");
        tree.addIndexRule("http://www.example.org");
        tree.addIndexRule("http://www.test.com");
        tree.addIndexRule("http://www.test.com/abc");

        String expectedTreeString = "http://www.test.com/abc\n           example.org\nabc";
        assertEquals(expectedTreeString, tree.toString());
    }

}
