/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XMLElement.java,v 1.2 2008/06/25 05:51:31 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Represents a simple XML element that may have given attributes, child
 * elements or a string value. Note that if the delete method is called on an
 * element, the element will disallow any further method invocations by 
 * throwing IllegalStateExceptions.
 */
public class XMLElement implements IXMLUtilsConstants {

    /**
     * Deletes this element from the master document. Once an element has been
     * deleted, any further method invocations on this element will result in
     * IllegalStateException being generated.
     * 
     * @throws Exception
     *             if the delete operation fails
     */
    public void delete() throws Exception {
        failIfDeleted();
        XMLElement parent = getParent();
        if (parent == null) {
            throw new Exception("Deletion of root element not permitted");
        }
        int startTokenIndex = getStartToken().getTokenIndex();
        int endTokenIndex = startTokenIndex;
        if (getEndToken() != null) {
            endTokenIndex = getEndToken().getTokenIndex();
        }
        getDocument().deleteTokens(startTokenIndex, endTokenIndex);

        parent.removeChildElement(this);
        markDeleted();
    }

    /**
     * Returns the value of the attribute with the given name as present in the
     * current element. Note that this value may be null if the given attribute
     * is not already present in this document. Also note that even though the
     * attributes are defined with leading and trailing double quotes, the 
     * value returned by this method will be devoid of these quotes.
     * 
     * @param name
     *            the name of the attribute whoes value must be returned.
     * @return the value of given named attribute, or null if the attribute is
     *         not present.
     */
    public String getAttributeValue(String name) {
        return getAttributeValue(name, true);
    }

    /**
     * Adds the given child element at the given index.
     * 
     * @param child
     * @param index
     * @throws Exception
     */
    public void addChildElementAt(XMLElement child, int index) throws Exception
    {
        addChildElementAt(child, index, false);
    }

    /**
     * Adds the given child element at the specified index. If the index is out
     * of bounds, this method will throw an exception.
     * 
     * @param child
     *            the element to be added as a child of this element
     * @param index
     *            the index at which the child element has to be added
     * @throws Exception
     *             if the addition operation fails
     */
    public void addChildElementAt(XMLElement child, int index,
            boolean addAfterNewLine) throws Exception {
        failIfDeleted();
        boolean addOuterWhitespace = true;
        if (getEndToken() == null) {
            insertEndToken();
            addOuterWhitespace = false;
        }
        ArrayList childElements = getChildElements();
        if (childElements == null) {
            childElements = new ArrayList();
            addOuterWhitespace = false;
        }

        if (index == -1) {
            index = childElements.size();
            addOuterWhitespace = false;
        }

        BoundedToken addAfterToken = null;
        if (index == 0) {
            addAfterToken = getStartToken();
        } else if (index <= childElements.size()) {
            XMLElement lastChildElement = (XMLElement) childElements
                    .get(index - 1);
            if (lastChildElement.getEndToken() != null) {
                addAfterToken = lastChildElement.getEndToken();
            } else {
                addAfterToken = lastChildElement.getStartToken();
            }
        }

        if (addAfterToken != null) {
            getDocument().addXMLElementAfterTokenIndex(
                    addAfterToken.getTokenIndex(), child, addAfterNewLine,
                    addOuterWhitespace);
        } else {
            throw new Exception("Failed to add child element: no such index: "
                    + index + ", element: " + this);
        }
        childElements.add(index, child);
        setChildElements(childElements);
    }

    private void insertEndToken() throws Exception {
        String startTokenString = getStartToken().getTokenString();
        if (startTokenString.endsWith("/>")) {
            String newStartTokenString = startTokenString.substring(0,
                    startTokenString.length() - 2)
                    + ">";
            getStartToken().setTokenString(newStartTokenString);
            setEndToken(new BoundedToken("</" + getName() + ">"));
            getDocument().insertEndTokenForElement(this);
        } else {
            throw new Exception("Failed to open collapsed element for update: "
                    + this);
        }
    }

    /**
     * Appends the given child element to the end of the list of existing child
     * elements.
     * 
     * @param child
     * @param addAfterNewLine
     * @throws Exception
     */
    public void addChildElement(XMLElement child, boolean addAfterNewLine)
            throws Exception {
        addChildElementAt(child, -1, addAfterNewLine);
    }

    /**
     * Appends the given child element to the end of the list of existing child
     * elements.
     * 
     * @param child
     *            the child element to append
     * @throws Exception
     *             if the addition operation fails.
     */
    public void addChildElement(XMLElement child) throws Exception {
        addChildElementAt(child, -1, false);
    }

    /**
     * Returns a list of named chiled elements of this element.
     * 
     * @param name
     *            the name of the child elements which must be returned
     * @return the child elements with the given names.
     */
    public ArrayList getNamedChildElements(String name) {
        failIfDeleted();
        ArrayList result = new ArrayList();
        ArrayList childElements = getChildElements();
        if (childElements != null && childElements.size() > 0) {
            for (int i = 0; i < childElements.size(); i++) {
                XMLElement nextChild = (XMLElement) childElements.get(i);
                if (nextChild.getName().equals(name)) {
                    result.add(nextChild);
                }
            }
        }

        return result;
    }

    /**
     * Updates the value of this element. This method will fail with an
     * exception if the current element does not store any value.
     * 
     * @param value
     *            to be updated
     * @throws Exception
     *             if the update operation fails
     */
    public void updateValue(String value) throws Exception {
        failIfDeleted();
        ArrayList childElements = getChildElements();
        if (childElements != null && childElements.size() > 0) {
            throw new Exception("Invalid operation: update value on: " + this);
        }

        if (getValueToken() == null) {
            getDocument().addValueTokenForElement(this, "");
        }

        getValueToken().setValue(value);
        setValue(value);
    }

    /**
     * Updates the value of the an attribute identified by the given name. If
     * this attribute is not present, the value is added to the attribute list
     * at the end of the already present attributes. Note that it is not
     * possible to add an attribute in the middle of the attribute list if
     * already certain other attributes are present. One way to work around 
     * this limitation is to remove all attributes from this element and then
     * add them in the required order as necessary.
     * 
     * @param name
     *            the name of the attribute to be updated or added
     * @param value
     *            the value of the attirbute
     * @throws Exception
     *             if the update was not successful
     */
    public void updateAttribute(String name, String value) {
        updateAttribute(name, value, true);
    }

    /**
     * Removes any attribute with the given name from this element. The return
     * value indicates the number of attributes that were removed. If the give
     * attribute is not present, the return value is <code>0</code>.
     * 
     * @param name
     *            the name of the attribute that must be removed.
     * @return the number of attributes that were removed
     */
    public int removeAttribute(String name) {
        failIfDeleted();
        ArrayList attributes = getAttributes();
        int count = 0;
        if (attributes != null && attributes.size() > 0) {
            Iterator it = attributes.iterator();
            while (it.hasNext()) {
                XMLElementAttribute attr = (XMLElementAttribute) it.next();
                if (attr.getName().equals(name)) {
                    it.remove();
                    count++;
                }
            }
        }

        if (count > 0) {
            setAttributes(attributes);
            updateAttributeStrings();
        }

        return count;
    }

    /**
     * Returns an indented string representation of this element and any
     * contained child elements.
     * 
     * @return a string representation of this element along with its sub-tree
     */
    public String toString() {
        return toIndentedString(0);
    }

    /**
     * Returns an xml fragment that represents this element and any contained
     * child elements.
     * 
     * @return an xml fragment representing this element
     */
    public String toXMLString() {
        int beginTokenIndex = getStartToken().getTokenIndex();
        int endTokenIndex = beginTokenIndex;
        if (getEndToken() != null) {
            endTokenIndex = getEndToken().getTokenIndex();
        }
        return getDocument().toXMLFragment(beginTokenIndex, endTokenIndex);
    }

    /**
     * Returns the value stored in this element.
     * 
     * @return the value stored in this element
     */
    public String getValue() {
        failIfDeleted();
        return value;
    }

    /**
     * Returns the name of this element
     * 
     * @return the name of this element
     */
    public String getName() {
        failIfDeleted();
        return name;
    }

    /**
     * Returns an ordered collection of child elements of this element
     * 
     * @return the collection of child elements in the order they are defined
     */
    public ArrayList getChildElements() {
        failIfDeleted();
        return childElements;
    }

    /**
     * Returns a string representing the path of this XMLElement in the current
     * document.
     * 
     * @return the path string
     */
    public String getPath() {
        ArrayList parents = new ArrayList();
        XMLElement entry = this;
        while (entry != null) {
            parents.add(0, entry);
            entry = entry.getParent();
        }

        StringBuffer buff = new StringBuffer();
        Iterator it = parents.iterator();
        while (it.hasNext()) {
            buff.append(((XMLElement) it.next()).getName());
            if (it.hasNext()) {
                buff.append("/");
            }
        }

        return buff.toString();
    }

    /**
     * Constructor
     * 
     * @param document
     * @param name
     */
    XMLElement(XMLDocument document, String name) {
        this(document, name, null, null);
    }

    /**
     * Constructor
     * 
     * @param document
     * @param name
     * @param value
     */
    XMLElement(XMLDocument document, String name, String value) {
        this(document, name, null, value);
    }

    /**
     * Constructor
     * 
     * @param document
     * @param name
     * @param childElements
     */
    XMLElement(XMLDocument document, String name, ArrayList childElements) {
        this(document, name, childElements, null);
    }

    /**
     * Returns a list of tokens in a collapsed manner that can later be 
     * inserted into the main document.
     * 
     * @param outerIndentString
     * @param indentIncrementString
     * @param indentValueToken
     * @param outerIndent
     * @return
     */
    ArrayList getCollapsedTokens(String outerIndentString,
            String indentIncrementString, boolean indentValueToken,
            boolean outerIndent) {
        ArrayList result = new ArrayList();
        String topIndentString = "";
        String valueIndentString = "";
        if (!outerIndentString.equals("")) {
            topIndentString = NEW_LINE + outerIndentString;
            valueIndentString = NEW_LINE + outerIndentString
                    + indentIncrementString;
        }
        if (outerIndent) {
            result.add(new WhiteSpaceToken(topIndentString));
        } else {
            result.add(new WhiteSpaceToken(outerIndentString));
        }
        result.add(getStartToken());
        if (getValueToken() != null) {
            if (indentValueToken) {
                result.add(new WhiteSpaceToken(valueIndentString));
            }
            result.add(getValueToken());
            if (indentValueToken) {
                result.add(new WhiteSpaceToken(topIndentString));
            }
        } else {
            ArrayList childElements = getChildElements();
            if (childElements != null && childElements.size() > 0) {
                for (int i = 0; i < childElements.size(); i++) {
                    XMLElement nextChild = (XMLElement) childElements.get(i);
                    result.addAll(nextChild.getCollapsedTokens(
                            outerIndentString + indentIncrementString,
                            indentIncrementString, indentValueToken, true));
                }
            }
            if (outerIndent) {
                result.add(new WhiteSpaceToken(topIndentString));
            }
        }
        if (getEndToken() != null) {
            result.add(getEndToken());
        }
        return result;
    }

    /**
     * Returns an integer indicating the indent level of this element given its
     * parent's indent level.
     * 
     * @param tokenIndex
     * @param parentIndentLevel
     * @return
     */
    int getIndentLevelForToken(int tokenIndex, int parentIndentLevel) {
        int indentLevel = -1;
        if (getStartToken().getTokenIndex() == tokenIndex) {
            if (getEndToken() != null) {
                indentLevel = parentIndentLevel + 1;
            } else {
                indentLevel = parentIndentLevel;
            }
        } else if (getEndToken() != null
                && getEndToken().getTokenIndex() == tokenIndex) {
            indentLevel = parentIndentLevel;
        } else if (getEndToken() == null) {
            indentLevel = parentIndentLevel;
        } else {
            ArrayList childElements = getChildElements();
            if (childElements != null && childElements.size() > 0) {
                for (int i = 0; i < childElements.size(); i++) {
                    XMLElement child = (XMLElement) childElements.get(i);
                    int level = child.getIndentLevelForToken(tokenIndex,
                            parentIndentLevel + 1);
                    if (level != -1) {
                        indentLevel = level;
                        break;
                    }
                }
            } else {
                indentLevel = -1;
            }
        }

        return indentLevel;
    }

    /**
     * Returns an indented representation of the tree starting at this element.
     * The given index is to be used as the indent level for this element.
     * 
     * @param index
     * @return
     */
    String toIndentedString(int index) {
        failIfDeleted();
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < index; i++) {
            buff.append(" ");
        }

        return buff.toString()
                + "["
                + getName()
                + ":"
                + getValue()
                + ":"
                + (getChildElements() == null ? "0" : String
                        .valueOf(getChildElements().size())) + ":" + "attr="
                + getAttributes() + ":" + getStartToken().getTokenIndex()
                + "]\n" + getChildElementsIndentedString(index + 1);
    }

    /**
     * Sets the given attributes for this element.
     * 
     * @param attributes
     */
    void setAttributes(ArrayList attributes) {
        this.attributes = attributes;
    }

    /**
     * Sets the starting token for this element.
     * 
     * @param startToken
     */
    void setStartToken(BoundedToken startToken) {
        this.startToken = startToken;
    }

    /**
     * Sets the ending token for this element. The ending token may not always
     * be set if the element is closed in the starting token itself.
     * 
     * @param endToken
     */
    void setEndToken(BoundedToken endToken) {
        this.endToken = endToken;
    }

    /**
     * Sets the value token for this element. The value token may not always be
     * set such as when this element has no value or contains child elements.
     * 
     * @param valueToken
     */
    void setValueToken(UnboundedToken valueToken) {
        this.valueToken = valueToken;
    }

    /**
     * Adds the given child elements to this element.
     * 
     * @param childElements
     */
    void setChildElements(ArrayList childElements) {
        if (childElements != null && childElements.size() > 0) {
            for (int i = 0; i < childElements.size(); i++) {
                XMLElement nextChild = (XMLElement) childElements.get(i);
                nextChild.setParent(this);
            }
        }
        this.childElements = childElements;
    }

    /**
     * Returns the ending token of this element. May be null.
     * 
     * @return
     */
    BoundedToken getEndToken() {
        return endToken;
    }

    /**
     * Returns the starting token for this element. May be null if this element
     * has not completely been initialized.
     * 
     * @return
     */
    BoundedToken getStartToken() {
        return startToken;
    }

    /**
     * Returns the value token for this element. May be null.
     * 
     * @return
     */
    UnboundedToken getValueToken() {
        return valueToken;
    }

    /**
     * Returns the attributes for this particular element.
     * 
     * @return
     */
    private ArrayList getAttributes() {
        return attributes;
    }

    /**
     * Returns the value of the given named attribute. The boolean flag when 
     * set to true will result in the value being devoid of the leading and 
     * trailing quotes.
     * 
     * @param name
     * @param stripQuotes
     * @return
     */
    private String getAttributeValue(String name, boolean stripQuotes) {
        failIfDeleted();
        String value = null;
        ArrayList attributes = getAttributes();
        if (attributes != null && attributes.size() > 0) {
            for (int i = 0; i < attributes.size(); i++) {
                XMLElementAttribute attr = (XMLElementAttribute) attributes
                        .get(i);
                if (attr.getName().trim().equals(name)) {
                    if (stripQuotes) {
                        value = attr.getValue();
                        if (value.charAt(0) == '"') {
                            value = value.substring(1);
                        }
                        if (value.charAt(value.length() - 1) == '"') {
                            value = value.substring(0, value.length() - 1);
                        }
                    } else {
                        value = attr.getValue();
                    }
                    break;
                }
            }
        }

        return value;
    }

    /**
     * Updates the value of the given attribute. If the attribute is not
     * present, it is added to the list of attributes for this element. The
     * boolean flag when set to true will result in the addition of leading 
     * and trailing double quotes for the value of the attribute.
     * 
     * @param name
     * @param value
     * @param addQuotes
     */
    private void updateAttribute(String name, String value, boolean addQuotes) 
    {
        failIfDeleted();
        ArrayList attributes = getAttributes();
        if (attributes == null) {
            attributes = new ArrayList();
        }
        boolean found = false;
        for (int i = 0; i < attributes.size(); i++) {
            XMLElementAttribute attr = (XMLElementAttribute) attributes.get(i);
            if (attr.getName().equals(name)) {
                if (addQuotes) {
                    attr.setValue("\"" + value + "\"");
                } else {
                    attr.setValue(value);
                }
                found = true;
            }
        }

        if (!found) {
            String attrValue = null;
            if (value.startsWith("\"") && value.endsWith("\"")) {
                attrValue = value;
            } else {
                attrValue = "\"" + value + "\"";
            }
            attributes.add(new XMLElementAttribute(name, attrValue));
        }

        setAttributes(attributes);
        updateAttributeStrings();
    }

    /**
     * Returns an indented string representation of the child elements of this
     * element.
     * 
     * @param index
     * @return
     */
    private String getChildElementsIndentedString(int index) {
        StringBuffer buff = new StringBuffer();
        ArrayList childElements = getChildElements();
        if (childElements != null) {
            for (int i = 0; i < childElements.size(); i++) {
                buff.append(((XMLElement) childElements.get(i))
                        .toIndentedString(index));
                buff.append("\n");
            }
        }

        return buff.toString();
    }

    /**
     * Constructor
     * 
     * @param document
     * @param name
     * @param childElements
     * @param value
     */
    private XMLElement(XMLDocument document, String name,
            ArrayList childElements, String value) {
        setDocument(document);
        setName(name);
        setChildElements(childElements);
        setValue(value);
    }

    /**
     * Updates the attribute string of the element's start and end tokens.
     */
    private void updateAttributeStrings() {
        if (getEndToken() != null) {
            getEndToken().removeAttributeString();
        }
        StringBuffer buff = new StringBuffer();
        ArrayList attributes = getAttributes();
        if (attributes != null && attributes.size() > 0) {
            for (int i = 0; i < attributes.size(); i++) {
                XMLElementAttribute attr = (XMLElementAttribute) attributes
                        .get(i);
                buff.append(attr.getName()).append("=").append(attr.getValue())
                        .append(" ");
            }
        }
        getStartToken().updateAttributeString(buff.toString());
    }

    /**
     * Sets the name of this element.
     * 
     * @param name
     */
    private void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the value of this element.
     * 
     * @param value
     */
    private void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the reference of the containing document.
     * 
     * @return
     */
    private XMLDocument getDocument() {
        return document;
    }

    /**
     * Sets the reference of the containing document.
     * 
     * @param document
     */
    private void setDocument(XMLDocument document) {
        this.document = document;
    }

    /**
     * Marks the given element deleted.
     * 
     */
    private void markDeleted() {
        ArrayList childElements = getChildElements();
        if (childElements != null && childElements.size() > 0) {
            for (int i = 0; i < childElements.size(); i++) {
                XMLElement nextChild = (XMLElement) childElements.get(i);
                nextChild.markDeleted();
            }
        }
        isDeleted = true;
    }

    /**
     * Returns true if the element is marked deleted, false otherwise.
     * 
     * @return
     */
    private boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Fails if this element has been deleted previously by throwing an
     * IllegalStateException.
     * 
     */
    private void failIfDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException(
                    "Operation failed: element is deleted");
        }
    }

    /**
     * Removes the given child element from this element.
     * 
     * @param child
     * @throws Exception
     */
    private void removeChildElement(XMLElement child) throws Exception {
        boolean removed = false;
        ArrayList childElements = getChildElements();
        if (childElements != null && childElements.size() > 0) {
            int childStartTokenIndex = child.getStartToken().getTokenIndex();
            Iterator it = childElements.iterator();
            while (it.hasNext()) {
                XMLElement nextChild = (XMLElement) it.next();
                if (nextChild.getStartToken().getTokenIndex() == 
                    childStartTokenIndex) {
                    it.remove();
                    removed = true;
                    break;
                }
            }
        }

        if (!removed) {
            throw new Exception("Failed to remove child element: " + child
                    + " from: " + this);
        }
    }

    /**
     * Adds the parent reference to this element.
     * 
     * @param parent
     */
    private void setParent(XMLElement parent) {
        this.parent = parent;
    }

    /**
     * Returns the reference of the parent of this document.
     * 
     * @return
     */
    private XMLElement getParent() {
        return parent;
    }

    private XMLDocument document;

    private String name;

    private ArrayList attributes;

    private ArrayList childElements;

    private String value;

    private BoundedToken startToken;

    private BoundedToken endToken;

    private UnboundedToken valueToken;

    private XMLElement parent;

    private boolean isDeleted;
}
