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
 * $Id: XMLDocument.java,v 1.3 2008/06/25 05:51:31 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util.xml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Represents a simple XML document in memory that may be edited and stored.
 * This implementation relies exlusively on the syntactic correctness of the
 * underlying document such as balancing of quotes and delimiters etc. If an 
 * XML document meets these requirements, it can be used to instantiate this 
 * class and can then be edited using the public methods available in this 
 * class as well as in <code>XMLElement</code> class.
 * <p>
 * The in-memroy XML representation does not include any meta information such
 * as <code>DOCTYPE</code> tags, processing instructions, or any commets. Such
 * tags are filtered out before the final in-memory representation of the XML
 * document is constructed. However, when this document is saved, these meta
 * information tags are re-inserted in the appropriate places so as to preserve
 * the original format of the document in all respects possible. Even white
 * spaces are preserved as far as possible.
 * </p>
 * <p>
 * This implementation uses an adhoc scanner/parser and does not rely on any
 * third party libraries.
 * </p>
 */
public class XMLDocument implements IXMLUtilsConstants {

    /**
     * Creates an instance of XMLDocument using the specified <code>File</code>
     * object. No checking is done to ensure the availablity and readability of
     * the file passed in as the argument. It is expected that the caller has
     * completed such checks and taken the necessary backups before creating
     * this instance.
     * 
     * @param file
     *            representing the XML document on file system.
     * @throws Exception
     *             in case an error occurse during the parsing of this 
     *             document.
     */
    public XMLDocument(File file) throws Exception {
        setDocumentFile(file);
        setParser(new XMLParser());
        initDocument(file);
    }

    /**
     * Returns the root element for the given XML document. This element
     * represents the entire XML document in memory and can be used to traverse
     * and edit various portions of the document.
     * 
     * @return the root element of the XML tree
     */
    public XMLElement getRootElement() {
        return root;
    }

    /**
     * A factory method used for the creation of new XML elements that can be
     * added to this XML document at a later stage. When this method is called,
     * a new <code>XMLElement</code> object is returned to the caller.
     * However, this newly created element is still not attached to the 
     * document anywhere and it is the responsiblity of the caller to attach it
     * in the appropriate location.
     * 
     * @param name
     *            the name of the new element to be created
     * @return the newly created element that can be added to the document
     * @throws Exception
     *             in case the creation of the new element fails due to any
     *             reason
     */
    public XMLElement newElement(String name) throws Exception {
        return newElement(name, null, null);
    }

    /**
     * A factory method used for the creation of new XML elements that can be
     * added to this XML document at a later stage. When this method is called,
     * a new <code>XMLElement</code> object is returned to the caller.
     * However, this newly created element is still not attached to the 
     * document anywhere and it is the responsiblity of the caller to attach it
     * in the appropriate location. Further, the element returned from this 
     * method is a collapsed element that is contained within a single bounded
     * token.
     * 
     * @param name
     *            the name of the new element to be created
     * @return the newly created element that can be added to the document
     * @throws Exception
     *             in case the creation of the new element fails due to any
     *             reason
     */
    public XMLElement newCollapsedElement(String name) throws Exception {
        return newElement(name, null, null, true);
    }

    /**
     * A factory method used for the creation of new XML elements that can be
     * added to this XML document at a later stage. When this method is called,
     * a new <code>XMLElement</code> object is returned to the caller.
     * However, this newly created element is still not attached to the 
     * document anywhere and it is the responsiblity of the caller to attach it
     * in the appropriate location. Note that the supplied parameter
     * <code>xmlFragement</code> must be a valid well-formed xml element.
     * 
     * @param xmlFragment
     *            the xml fragment which will be parsed into an element
     * @return the newly created element that can be added to the document
     * @throws Exception
     *             in case the creation of the new element fails due to any
     *             reason
     */
    public XMLElement newElementFromXMLFragment(String xmlFragment)
            throws Exception {
        StringReader reader = new StringReader(xmlFragment);
        ArrayList newTokens = getParser().parse(reader);
        ArrayList filteredTokens = getFilteredTokens(newTokens);
        ArrayList elements = getElements(filteredTokens);

        if (elements == null || elements.size() != 1) {
            throw new Exception("Failed to parse fragment into new element");
        }

        return (XMLElement) elements.get(0);
    }

    /**
     * A factory method used for the creation of new XML elements that can be
     * added to this XML document at a later stage. When this method is called,
     * a new <code>XMLElement</code> object is returned to the caller.
     * However, this newly created element is still not attached to the 
     * document anywhere and it is the responsiblity of the caller to attach it
     * in the appropriate location.
     * 
     * @param name
     *            the name of the new element to be created
     * @param value
     *            the value of the new element to be created
     * @return the newly created element that can be added to the document
     * @throws Exception
     *             in case the creation of the new element fails due to any
     *             reason
     */
    public XMLElement newElement(String name, String value) throws Exception {
        return newElement(name, value, null);
    }

    /**
     * Stores the in-memory XML data to the file system.
     * 
     * @throws Exception
     *             If the save operation did not succeed.
     */
    public void store() throws Exception {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(getDocumentFile()));
            ArrayList rawTokens = getRawTokens();
            for (int i = 0; i < rawTokens.size(); i++) {
                writer.write(((Token) rawTokens.get(i)).toString());
            }
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Sets the number of spaces used for denoting one indent level. The 
     * default indent level is set to <code>4</code> spaces. However, this can
     * be changed by calling this method anytime. This value comes into effect
     * only when any new element is added to the XML document.
     * 
     * @param spaces
     *            the number of spaces used to denote one level of indentation
     */
    public void setIndentDepth(int spaces) {
        indentDepth = spaces;
    }

    /**
     * Sets a flag that is used by the document to indent value tokens when
     * adding child elements that have value. The default behavior is to indent
     * value tokens, but that can be changed to no indent by calling this
     * method.
     * 
     */
    public void setNoValueIndent() {
        indentValueTokenFlag = false;
    }

    /**
     * Sets a flag that is used by the document to indent value tokens when
     * adding child elements that have value. The default behavior is to indent
     * value tokens, but changed by calling <code>setNoValueIndent</code>
     * method, it can be reset back to its original state by calling this
     * method.
     * 
     */
    public void setValueIndent() {
        indentValueTokenFlag = true;
    }

    /**
     * Returns the DOCTYPE string associated with the first DOCTYPE element
     * present in this document. This method may return null if no DOCTYPE 
     * token is already present in the document.
     * 
     * @return
     */
    public String getDoctypeString() {
        String result = null;
        DoctypeToken dctoken = getDoctypeToken();
        if (dctoken != null) {
            result = dctoken.getDoctypeString();
        }
        return result;
    }

    /**
     * This methods provides a means to update the DOCTYPE element of the
     * document with a new value as supplied in the argument. This method will
     * throw an Exception if the given document does not contain a predefined
     * DOCTYPE element.
     * 
     * @param newDoctypeString
     * @throws Exception
     */
    public void updatedDoctypeString(String newDoctypeString) throws Exception 
    {
        DoctypeToken dctoken = getDoctypeToken();
        if (dctoken != null) {
            dctoken.updateDoctypeString(newDoctypeString);
        } else {
            throw new Exception("FAILED to update DOCTYPE - no such element");
        }
    }

    /**
     * Adds a value token for the given element with the given value.
     * 
     * @param element
     * @param value
     * @throws Exception
     */
    void addValueTokenForElement(XMLElement element, String value)
            throws Exception {
        UnboundedToken valueToken = newValueToken(value);
        ArrayList rawTokens = getRawTokens();
        int startTokenIndex = element.getStartToken().getTokenIndex();
        int indentLevel = getIndentLevel(startTokenIndex);
        WhiteSpaceToken valueIndentToken = new WhiteSpaceToken(NEW_LINE
                + getIndentStringForIndentLevel(indentLevel + 1));
        boolean added = false;
        for (int i = 0; i < rawTokens.size(); i++) {
            Token nextToken = (Token) rawTokens.get(i);
            if (nextToken.getTokenIndex() == startTokenIndex) {
                int insertIndex = i + 1;
                valueIndentToken.setTokenIndex(
                        getParser().getNextTokenIndex());
                rawTokens.add(insertIndex, valueIndentToken);
                insertIndex++;
                rawTokens.add(insertIndex, valueToken);
                added = true;
                break;
            }
        }

        if (!added) {
            throw new Exception("Failed to add value token");
        }
        element.setValueToken(valueToken);
    }

    /**
     * Adds the given XMLElement after the token whoes index matches the given
     * lastTokenIndex value. If the flag addAfterNewLine is true, a new line is
     * added before the addition of the new element to this document.
     * 
     * @param lastTokenIndex
     * @param element
     * @param addAfterNewLine
     * @throws Exception
     */
    void addXMLElementAfterTokenIndex(int lastTokenIndex, XMLElement element,
            boolean addAfterNewLine) throws Exception {
        addXMLElementAfterTokenIndex(lastTokenIndex, element, addAfterNewLine,
                true);
    }

    /**
     * Adds the given XMLElement after the token whoes index matches the given
     * lastTokenIndex value. If the flag addAfterNewLine is true, a new line is
     * added before the addition of the new element to this document.
     * 
     * @param lastTokenIndex
     * @param element
     * @param addAfterNewLine
     * @param addOuterWhitespace
     * @throws Exception
     */
    void addXMLElementAfterTokenIndex(int lastTokenIndex, XMLElement element,
            boolean addAfterNewLine, boolean addOuterWhitespace)
            throws Exception {
        ArrayList rawTokens = getRawTokens();
        String outerIndentString = "";
        String indentIncrementString = "";
        if (addAfterNewLine) {
            outerIndentString = getIndentStringForIndentLevel(getIndentLevel(
                    lastTokenIndex));
            indentIncrementString = getIndentIncrementString();
        }

        boolean outerIndent = true;
        if (!addOuterWhitespace) {
            outerIndent = false;
        }

        ArrayList newTokens = element.getCollapsedTokens(outerIndentString,
                indentIncrementString, indentValueToken(), outerIndent);

        boolean added = false;
        for (int i = 0; i < rawTokens.size(); i++) {
            Token nextToken = (Token) rawTokens.get(i);
            if (nextToken.getTokenIndex() == lastTokenIndex) {
                int lastIndexPosition = i;
                while (nextToken instanceof WhiteSpaceToken) {
                    lastIndexPosition++;
                    nextToken = (Token) rawTokens.get(i);
                }
                int insertIndex = lastIndexPosition + 1;
                if (addAfterNewLine) {
                    WhiteSpaceToken wstoken = new WhiteSpaceToken(NEW_LINE);
                    wstoken.setTokenIndex(getParser().getNextTokenIndex());
                    rawTokens.add(insertIndex++, wstoken);
                }
                for (int j = 0; j < newTokens.size(); j++) {
                    Token newToken = (Token) newTokens.get(j);
                    rawTokens.add(insertIndex + j, newToken);
                }
                added = true;
                break;
            }
        }
        if (!added) {
            throw new Exception("Parent element not found: index "
                    + lastTokenIndex);
        }
        setRawTokens(rawTokens);
    }

    /**
     * Inserts the ending token for a given element which was initially added 
     * or parsed into a collapsed element.
     * 
     * @param element
     * @throws Exception
     */
    void insertEndTokenForElement(XMLElement element) throws Exception {
        int startTokenIndex = element.getStartToken().getTokenIndex();
        int indentLevel = getIndentLevel(startTokenIndex);
        String indentString = NEW_LINE
                + getIndentStringForIndentLevel(indentLevel - 1);
        WhiteSpaceToken indentToken = new WhiteSpaceToken(indentString);
        indentToken.setTokenIndex(getParser().getNextTokenIndex());
        Token endToken = element.getEndToken();
        endToken.setTokenIndex(getParser().getNextTokenIndex());
        ArrayList rawTokens = getRawTokens();
        boolean added = false;
        for (int i = 0; i < rawTokens.size(); i++) {
            Token nextToken = (Token) rawTokens.get(i);
            if (nextToken.getTokenIndex() == startTokenIndex) {
                rawTokens.add(i + 1, indentToken);
                rawTokens.add(i + 2, endToken);
                added = true;
                break;
            }
        }
        if (!added) {
            throw new Exception("Failed to add end token for element: "
                    + element);
        }
    }

    /**
     * Deletes the tokens from the token whoes index matches with startIndex
     * upto the token whoes index matches with endIndex. Both these tokens are
     * included in the deletion as well.
     * 
     * @param startIndex
     * @param endIndex
     * @throws Exception
     */
    void deleteTokens(int startIndex, int endIndex) throws Exception {
        ArrayList updatedRawTokens = new ArrayList();
        updatedRawTokens.addAll(getRawTokens());
        Iterator it = updatedRawTokens.iterator();
        boolean delete = false;
        int deleteCount = 0;
        Token lastToken = null;
        while (it.hasNext()) {
            Token nextToken = (Token) it.next();
            if (!delete) {
                if (nextToken.getTokenIndex() == startIndex) {
                    if (lastToken != null
                            && lastToken instanceof WhiteSpaceToken) {
                        lastToken.markDeleted();
                    }
                    nextToken.markDeleted();
                    deleteCount++;
                    if (startIndex != endIndex) {
                        delete = true;
                    }
                }
            } else {
                deleteCount++;
                nextToken.markDeleted();
                if (nextToken.getTokenIndex() == endIndex) {
                    delete = false;
                    break;
                }
            }
            lastToken = nextToken;
        }

        if (delete == true) {
            throw new Exception("Failed to find last token: index " + 
                    endIndex);
        }

        if (startIndex != endIndex && deleteCount < 2) {
            throw new Exception("Failed to delete tokens for range: "
                    + startIndex + "-" + endIndex);
        }

        if (startIndex == endIndex && deleteCount != 1) {
            throw new Exception("Failed to delete token at index: "
                    + startIndex + ", delete count: " + deleteCount);
        }

        setRawTokens(updatedRawTokens);
    }

    /**
     * Returns an xml fragment that represents this element and any contained
     * child elements.
     * 
     * @param beginTokenIndex
     *            the index of the token where the string begins
     * @param endTokenIndex
     *            the index of the token where the string ends
     * @return an xml fragment representing this element
     */
    String toXMLFragment(int beginTokenIndex, int endTokenIndex) {
        StringBuffer buff = new StringBuffer();
        ArrayList rawTokens = this.getRawTokens();
        Iterator it = rawTokens.iterator();
        int index = 0;
        boolean inRange = false;
        while (it.hasNext()) {
            Token nextToken = (Token) it.next();
            if (!inRange) {
                if (nextToken.getTokenIndex() == beginTokenIndex) {
                    inRange = true;
                }
            }

            if (inRange) {
                buff.append(nextToken.toString());
                if (nextToken.getTokenIndex() == endTokenIndex) {
                    inRange = false;
                }
            }
        }

        return buff.toString();
    }

    /**
     * Factory method for creation a new value token with the given value.
     * 
     * @param value
     * @return
     * @throws Exception
     */
    private UnboundedToken newValueToken(String value) throws Exception {
        if (value == null) {
            value = "";
        }
        UnboundedToken valueToken = new UnboundedToken(value);
        valueToken.setTokenIndex(getParser().getNextTokenIndex());
        return valueToken;
    }

    /**
     * Factory method for creating a new element with the given name, given
     * value and given attributes. The value and the attributes may be null.
     * 
     * @param name
     * @param value
     * @param attributes
     * @return
     * @throws Exception
     */
    private XMLElement newElement(String name, String value,
            ArrayList attributes) throws Exception {
        return newElement(name, value, attributes, false);
    }

    /**
     * Factory method for creating a new element with the given name, given
     * value and given attributes. The value and the attributes may be null. If
     * the boolean argument <code>collapsed</code> is set to true, the element
     * will have a single token for start and end marks. In this case if a 
     * value is specified, it will result in the throwing of an exception to 
     * indicate an invalid request.
     * 
     * @param name
     * @param value
     * @param attributes
     * @param collapsed
     * @return
     * @throws Exception
     */
    private XMLElement newElement(String name, String value,
            ArrayList attributes, boolean collapsed) throws Exception {
        XMLElement result = new XMLElement(this, name);
        String startTokenString = null;
        if (collapsed) {
            startTokenString = "<" + name + "/>";
        } else {
            startTokenString = "<" + name + ">";
        }
        BoundedToken startToken = new BoundedToken(startTokenString);
        startToken.setTokenIndex(getParser().getNextTokenIndex());
        result.setStartToken(startToken);
        if (value != null && value.trim().length() > 0) {
            if (collapsed) {
                throw new Exception(
                        "Cannot add a collapsed element with specified value");
            }
            UnboundedToken valueToken = new UnboundedToken(value);
            valueToken.setTokenIndex(getParser().getNextTokenIndex());
            result.setValueToken(valueToken);
        }
        if (!collapsed) {
            BoundedToken endToken = new BoundedToken("</" + name + ">");
            endToken.setTokenIndex(getParser().getNextTokenIndex());
            result.setEndToken(endToken);
        }
        if (attributes != null && attributes.size() > 0) {
            Iterator it = attributes.iterator();
            while (it.hasNext()) {
                XMLElementAttribute attr = (XMLElementAttribute) it.next();
                result.updateAttribute(attr.getName(), attr.getValue());
            }
        }
        return result;
    }

    /**
     * Returns the indent level for the given token.
     * 
     * @param tokenIndex
     * @return
     */
    private int getIndentLevel(int tokenIndex) {
        return getRootElement().getIndentLevelForToken(tokenIndex, 0);
    }

    /**
     * Returns a whitespace string which represents the indentation to be used
     * for a given indentLevel value.
     * 
     * @param indentLevel
     * @return
     */
    private String getIndentStringForIndentLevel(int indentLevel) {
        StringBuffer buff = new StringBuffer("");
        String indentIncrementString = getIndentIncrementString();
        for (int i = 0; i < indentLevel; i++) {
            buff.append(indentIncrementString);
        }

        return buff.toString();
    }

    /**
     * Returns a string with the number of spaces corresponding to the indent
     * depth set for this document.
     * 
     * @return
     */
    private String getIndentIncrementString() {
        StringBuffer buff = new StringBuffer("");
        for (int i = 0; i < getIndentDepth(); i++) {
            buff.append(' ');
        }

        return buff.toString();
    }

    /**
     * Initializes the document with the given File object.
     * 
     * @param file
     * @throws Exception
     */
    private void initDocument(File file) throws Exception {
        setRawTokens(getParser().parse(
                new InputStreamReader(new FileInputStream(file))));
        initXMLTree();

        ArrayList rawTokens = getRawTokens();
        Iterator it = rawTokens.iterator();
        while (it.hasNext()) {
            Token nextToken = (Token) it.next();
            if (nextToken instanceof DoctypeToken) {
                setDoctypeTokenIndex(nextToken.getTokenIndex());
                break;
            }
        }
    }

    /**
     * Returns the DOCTYPE token associated with this document. May return
     * <code>null</code> if no such token is present.
     * 
     * @return
     */
    private DoctypeToken getDoctypeToken() {
        DoctypeToken result = null;
        int doctypeTokenIndex = getDoctypeTokenIndex();
        if (doctypeTokenIndex != -1) {
            ArrayList rawTokens = getRawTokens();
            Iterator it = rawTokens.iterator();
            while (it.hasNext()) {
                Token nextToken = (Token) it.next();
                if (nextToken.getTokenIndex() == doctypeTokenIndex) {
                    result = (DoctypeToken) nextToken;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Creats an in-memory XML tree based on the parsed tokens in this 
     * document.
     * 
     * @throws Exception
     */
    private void initXMLTree() throws Exception {
        updateFilteredTokens();
        ArrayList elements = getElements(getFilteredTokens());
        if (elements.size() > 1) {
            throw new Exception("More than one root elements encountered");
        }
        setRootElement((XMLElement) elements.get(0));
    }

    /**
     * Returns a list of tokens that do not contain any whitespace tokens.
     * 
     * @param rawTokens
     * @return filtered tokens
     */
    private ArrayList getFilteredTokens(ArrayList rawTokens) {
        ArrayList filteredTokens = new ArrayList();
        Iterator it = rawTokens.iterator();

        while (it.hasNext()) {
            Token nextToken = (Token) it.next();
            if (!nextToken.isDeleted()) {
                if (nextToken instanceof BoundedToken
                        || nextToken instanceof UnboundedToken) {
                    filteredTokens.add(nextToken);
                }
            }
        }
        return filteredTokens;
    }

    /**
     * Updates the tokens to create a set of filtered tokens that make up the
     * various XML elements etc for final creation of in-memory XML
     * representation.
     */
    private void updateFilteredTokens() {
        setFilteredTokens(getFilteredTokens(getRawTokens()));
    }

    /**
     * Returns the attributes from the given attribute string.
     * 
     * @param attributeString
     * @return
     * @throws Exception
     */
    private ArrayList getAttributes(String attributeString) throws Exception {
        return getAttributes(attributeString, null);
    }

    /**
     * Returns the attributes from the given attribute strings of the starting
     * and the ending tokens of any element.
     * 
     * @param attributeStringBegin
     * @param attributeStringEnd
     * @return
     * @throws Exception
     */
    private ArrayList getAttributes(String attributeStringBegin,
            String attributeStringEnd) throws Exception {
        String attributeString = null;
        if (attributeStringBegin != null
                && attributeStringBegin.trim().length() > 0) {
            attributeString = attributeStringBegin;
        }

        if (attributeStringEnd != null
                && attributeStringEnd.trim().length() > 0) {
            attributeString += attributeStringEnd;
        }

        return getParser().parseAttributes(attributeString);
    }

    /**
     * Walks through the filtered token set to create an in-memory
     * representation of the XML document.
     * 
     * @param tokenList
     * @return
     * @throws Exception
     */
    private ArrayList getElements(ArrayList tokenList) throws Exception {
        ArrayList result = null;
        if (tokenList != null && tokenList.size() > 0) {
            result = new ArrayList();
            int count = 0;
            while (count < tokenList.size()) {
                String elementName = null;
                String elementValue = null;
                ArrayList attributeList = null;
                ArrayList childElements = null;

                XMLElement element = null;

                Token firstToken = (Token) tokenList.get(count);
                if (!(firstToken instanceof BoundedToken)) {
                    throw new Exception("First token not bounded: "
                            + firstToken.toDebugString());
                }
                BoundedToken token = (BoundedToken) firstToken;
                elementName = token.getName();
                String attributeString = token.getAttributeString();

                if (token.elementComplete()) {
                    element = new XMLElement(this, elementName);
                    element.setStartToken(token);
                    element.setAttributes(getAttributes(token
                            .getAttributeString()));
                    result.add(element);
                    count++;
                } else {
                    if (tokenList.size() > count + 1) {
                        Token secondToken = (Token) tokenList.get(count + 1);
                        if (secondToken instanceof BoundedToken) {
                            BoundedToken secondBoundedToken = (BoundedToken) 
                                secondToken;
                            if (!secondBoundedToken.elementStart()) {
	                            if (secondBoundedToken.getName()
	                                    .equals(elementName)) {
	                                if (secondBoundedToken.elementEnd()) {
	                                    element = new XMLElement(this, 
	                                            elementName);
	                                    element.setStartToken(token);
	                                    element.setEndToken(secondBoundedToken);
	                                    element.setAttributes(getAttributes(token
	                                            .getAttributeString(),
	                                            secondBoundedToken
	                                                    .getAttributeString()));
	                                    result.add(element);
	                                    count = count + 2;
	                                } else {
	                                    throw new Exception("Malformed element: "
	                                            + secondBoundedToken
	                                                    .toDebugString());
	                                }
	                            }
                            }
                        }
                    }
                }

                if (element == null && tokenList.size() >= count + 3) {
                    Token midToken = (Token) tokenList.get(count + 1);
                    UnboundedToken ubToken = null;
                    if (midToken instanceof UnboundedToken) {
                        ubToken = (UnboundedToken) midToken;
                        elementValue = ubToken.getValue();
                    }

                    if (elementValue != null) {
                        Token thirdToken = (Token) tokenList.get(count + 2);
                        if (!(thirdToken instanceof BoundedToken)) {
                            throw new Exception("Malformed token encountered: "
                                    + thirdToken.toDebugString());
                        }

                        BoundedToken thirdBoundedToken = (BoundedToken) 
                            thirdToken;
                        if (!thirdBoundedToken.getName().equals(elementName)) {
                            throw new Exception("Malformed token encountered: "
                                    + thirdToken.toDebugString());
                        }

                        if (!thirdBoundedToken.elementEnd()) {
                            throw new Exception("Malformed token encountered: "
                                    + thirdToken.toDebugString());
                        }

                        element = new XMLElement(this, elementName,
                                elementValue);
                        element.setStartToken(token);
                        element.setValueToken(ubToken);
                        element.setEndToken(thirdBoundedToken);
                        element.setAttributes(getAttributes(token
                                .getAttributeString(), thirdBoundedToken
                                .getAttributeString()));
                        result.add(element);
                        count = count + 3;
                    }
                }

                if (element == null) {
                    ArrayList innerTokens = new ArrayList();
                    int boundCount = 1;
                    for (int i = count + 1; i < tokenList.size(); i++) {
                        Token nextToken = (Token) tokenList.get(i);
                        if (nextToken instanceof BoundedToken) {
                            BoundedToken nextBoundedToken = (BoundedToken) 
                                nextToken;
                            if (nextBoundedToken.getName().equals(elementName))
                            {
                                if (nextBoundedToken.elementEnd()) {
                                    if(!nextBoundedToken.elementStart()) {
                                        boundCount--;
                                        if (boundCount == 0) {
                                            childElements = getElements(
                                                    innerTokens);
                                            element = new XMLElement(this,
                                                    elementName, childElements);
                                            element.setStartToken(token);
                                            element.setEndToken(nextBoundedToken);
                                            element.setAttributes(getAttributes(
                                                    token.getAttributeString(),
                                                    nextBoundedToken
                                                        .getAttributeString()));
                                            result.add(element);
                                            count = i + 1;
                                            break;
                                        } else {
                                            innerTokens.add(nextToken);
                                        }
                                    } else {
                                        innerTokens.add(nextToken);
                                    }
                                } else {
                                    boundCount++;
                                    innerTokens.add(nextToken);
                                }
                            } else {
                                innerTokens.add(nextToken);
                            }
                        } else {
                            innerTokens.add(nextToken);
                        }
                    }
                    if (element == null) {
                        throw new Exception("Element not terminated: "
                                + elementName + ", innerTokens = "
                                + innerTokens);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the raw tokens for this document.
     * 
     * @return
     */
    private ArrayList getRawTokens() {
        return rawTokens;
    }

    /**
     * Sets the raw tokens for this document.
     * 
     * @param rawTokens
     */
    private void setRawTokens(ArrayList rawTokens) {
        this.rawTokens = rawTokens;
    }

    /**
     * Sets the filtered tokens for this document.
     * 
     * @param filteredTokens
     */
    private void setFilteredTokens(ArrayList filteredTokens) {
        this.filteredTokens = filteredTokens;
    }

    /**
     * Returns the filted tokens for this document.
     * 
     * @return
     */
    private ArrayList getFilteredTokens() {
        return filteredTokens;
    }

    /**
     * Sets the root element of this document.
     * 
     * @param root
     */
    private void setRootElement(XMLElement root) {
        this.root = root;
    }

    /**
     * Sets the parser to be used with this document.
     * 
     * @param parser
     */
    private void setParser(XMLParser parser) {
        this.parser = parser;
    }

    /**
     * Returns the parser that is used with this document.
     * 
     * @return
     */
    private XMLParser getParser() {
        return parser;
    }

    /**
     * Sets the file object which is the source of this document.
     * 
     * @param file
     */
    private void setDocumentFile(File file) {
        documentFile = file;
    }

    /**
     * Returns the file object that is the source of this document.
     * 
     * @return
     */
    private File getDocumentFile() {
        return documentFile;
    }

    /**
     * Returns the preffered indentation depth of this document.
     * 
     * @return
     */
    private int getIndentDepth() {
        return indentDepth;
    }

    /**
     * Returns true if the value token should be indented while adding new
     * elements to this document, false otherwise.
     * 
     * @return
     */
    private boolean indentValueToken() {
        return indentValueTokenFlag;
    }

    /**
     * Sets the document type token index
     * 
     * @param index
     */
    private void setDoctypeTokenIndex(int index) {
        doctypeTokenIndex = index;
    }

    /**
     * Returns the document type token index. May return <code>-1</code> if no
     * document type token was found.
     * 
     * @return
     */
    private int getDoctypeTokenIndex() {
        return doctypeTokenIndex;
    }

    private File documentFile;

    private ArrayList rawTokens;

    private ArrayList filteredTokens;

    private XMLElement root;

    private XMLParser parser;

    private int indentDepth = 4;

    private boolean indentValueTokenFlag = true;

    private int doctypeTokenIndex = -1;
}
