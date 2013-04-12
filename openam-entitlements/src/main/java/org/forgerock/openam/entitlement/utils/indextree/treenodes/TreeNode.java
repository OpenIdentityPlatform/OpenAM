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
package org.forgerock.openam.entitlement.utils.indextree.treenodes;

import org.forgerock.openam.entitlement.utils.indextree.nodecontext.SearchContext;

/**
 * A representation of a node with a tree structure.
 * <p/>
 * A tree node can have one parent and many children. Children are represented by single references to one another. So
 * a parent has a single reference to its first child and then corresponding children can be accessed via a child's
 * sibling reference.
 * <p/>
 * A tree node represents a single character within an index rule. An index rule is the path in the tree up to an end
 * point. Each tree node is responsible for determining whether it has interest in a passed resource character. For
 * instance a tree node may represent a wildcard and therefore may have interest in a set of characters. Implementations
 * of this interface determine the concrete behaviour.
 * <p/>
 * Tree nodes have a notion of end points. An end point denotes the final character of an index rule that was added to
 * the tree structure. Therefore the path of an end point is an index rule. For instance, if two index rules "abcd" and
 * "abcdef" were added to the tree, there would be no way to determine that the path to tree node "d" represented an
 * index rule. The use of end points allow for this.
 *
 * @author apforrest
 */
public interface TreeNode {

    /**
     * Evaluates whether this tree node has interest in the passed character.
     *
     * @param value
     *         The passed character.
     * @param context
     *         The shared search context.
     * @return Whether there is interest in the passed character..
     */
    public boolean hasInterestIn(char value, SearchContext context);

    /**
     * @return The absolute path of the tree node.
     */
    public String getFullPath();

    /**
     * Creates a string representation of the tree from this node.
     * This makes a traversal down through all child nodes.
     *
     * @param includeEndPoint
     *         Whether the end point values should be included.
     * @return String representation of the tree node and its children.
     */
    public String toString(boolean includeEndPoint);

    /**
     * @return The tree node value.
     */
    public char getNodeValue();

    /**
     * @return Whether this is a tree leaf node.
     */
    public boolean isLeafNode();

    /**
     * @return Whether this is a root tree node.
     */
    public boolean isRoot();

    /**
     * A wildcard node can match more than one simultaneous characters of a given
     * resource URL and potentially from a set of different characters.
     *
     * @return Whether this node is considered to represent a wildcard.
     */
    public boolean isWildcard();

    /**
     * Mark this tree node as having an end point.
     */
    public void markEndPoint();

    /**
     * Remove the mark that this tree node has an end point.
     */
    public void removeEndPoint();

    /**
     * @return Whether this tree node has an end point.
     */
    public boolean isEndPoint();


    /**
     * @return The parent tree node.
     */
    public TreeNode getParent();

    /**
     * Sets the parent tree node.
     *
     * @param parent
     *         The parent tree node.
     */
    public void setParent(TreeNode parent);

    /**
     * @return Whether this tree node has a parent reference.
     */
    public boolean hasParent();

    /**
     * @return The child tree node.
     */
    public TreeNode getChild();

    /**
     * Sets the child tree node.
     *
     * @param child
     *         The child tree node.
     */
    public void setChild(TreeNode child);

    /**
     * @return Whether this tree node has a child reference.
     */
    public boolean hasChild();

    /**
     * @return The sibling tree node.
     */
    public TreeNode getSibling();

    /**
     * Sets the sibling tree node.
     *
     * @param sibling
     *         The sibling tree node.
     */
    public void setSibling(TreeNode sibling);

    /**
     * @return Whether this tree node has a sibling reference.
     */
    public boolean hasSibling();

}
