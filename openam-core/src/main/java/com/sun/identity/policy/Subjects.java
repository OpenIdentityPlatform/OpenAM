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
 * $Id: Subjects.java,v 1.4 2008/06/25 05:43:45 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.*;

import org.w3c.dom.*;

import com.iplanet.sso.*;
import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.am.util.Cache;
import com.sun.identity.policy.interfaces.Subject;

/**
 * The class <code>Subjects</code> provides methods to maintain
 * a collection of <code>Subject</code> objects that can be
 * applied to a policy. This class provides methods to add, replace
 * and remove <code>Subject</code> objects from this users collection.
 * The <code>Policy</code> object provides methods to set
 * <code>Subjects</code>, which identifies users to whom the
 * the policy applies.
 */
public class Subjects {

    private final int SUBJECTS_RESULT_CACHE_SIZE = 1000;
    private String name;
    private String description;
    private Map users = new HashMap();
    private Cache resultCache = new Cache(SUBJECTS_RESULT_CACHE_SIZE);
    private long resultTtl;

    /**
     * Constructor used by the <code>Policy</code> object
     * to get a default instance of the <code>Subjects</code>
     */
    protected Subjects() {
	this((String) null, (String) null);
    }

    /**
     * Constructor used by <code>Policy</code> to obtain
     * an instance of <code>Subjects</code> from the
     * XML document
     *
     * @param usersNode node that repersents the Subjects
     * @throws InvalidFormatException if the node passed in does not
     * conform to expected format
     * 
     * @throws InvalidNameException 
     * @throws NameNotFoundException 
     *         
     * @throws PolicyException if can not construct <code>Subjects</code>
     */
    protected Subjects(PolicyManager pm, Node usersNode)
	throws InvalidFormatException, InvalidNameException,
	NameNotFoundException, PolicyException 
    {
	// Check if the node name is PolicyManager.POLICY_SUBJECTS_NODE
	if (!usersNode.getNodeName().equalsIgnoreCase(
	    PolicyManager.POLICY_SUBJECTS_NODE)) {
	    if (PolicyManager.debug.warningEnabled()) {
		PolicyManager.debug.warning(
		    "invalid subjects xml blob given to construct subjects");
	    }
	    throw (new InvalidFormatException(ResBundleUtils.rbName,
		"invalid_xml_subjects_root_node", null, "",
		PolicyException.USER_COLLECTION));
	}

	// Get the subjects name
	if ((name = XMLUtils.getNodeAttributeValue(usersNode,
	    PolicyManager.NAME_ATTRIBUTE)) == null) {
	    name = "Subjects:" + ServiceTypeManager.generateRandomName();
	}

	// Get the description
	if ((description = XMLUtils.getNodeAttributeValue(usersNode,
	    PolicyManager.DESCRIPTION_ATTRIBUTE)) == null) {
	    description = "";
	}

	// Get SubjectTypeManager
	SubjectTypeManager stm = pm.getSubjectTypeManager();

	// Get individual subjects
	Iterator subjectNodes = XMLUtils.getChildNodes(
	    usersNode, PolicyManager.SUBJECT_POLICY).iterator();
	while (subjectNodes.hasNext()) {
	    Node subjectNode = (Node) subjectNodes.next();
	    String subjectType = XMLUtils.getNodeAttributeValue(
		subjectNode, PolicyManager.TYPE_ATTRIBUTE);
	    if (subjectType == null) {
		if (PolicyManager.debug.warningEnabled()) {
		    PolicyManager.debug.warning("subject type is null");
		}
		throw (new InvalidFormatException(
		    ResBundleUtils.rbName,
		    "invalid_xml_subjects_root_node", null, "",
		    PolicyException.USER_COLLECTION));
	    }

	    // Construct the subject object
	    Subject subject = stm.getSubject(subjectType);

	    // Get and set the values
                
	    NodeList attrValuePairNodes = subjectNode.getChildNodes();
	    int numAttrValuePairNodes = attrValuePairNodes.getLength();
	    for (int j = 0; j < numAttrValuePairNodes; j++) {
		Node attrValuePairNode = attrValuePairNodes.item(j);
		if (XMLUtils.getNamedChildNode(attrValuePairNode,
		    PolicyManager.ATTR_NODE, PolicyManager.NAME_ATTRIBUTE,
		    SUBJECT_VALUES_ATTR_NAME) != null) {
		    subject.setValues(XMLUtils.getAttributeValuePair(
			attrValuePairNode));
		}
	    }

	    // Get the friendly name given to subject
	    String subjectName = XMLUtils.getNodeAttributeValue(
		subjectNode, PolicyManager.NAME_ATTRIBUTE);

	    String exclusive = XMLUtils.getNodeAttributeValue(
		subjectNode, INCLUDE_TYPE);
	    // Add the subject to users collection
	    addSubject(subjectName, subject, EXCLUSIVE_TYPE.equals(exclusive));
	}

	// Get individual realmsubjects
	subjectNodes = XMLUtils.getChildNodes(
	    usersNode, PolicyManager.REALM_SUBJECT_POLICY).iterator();
	while (subjectNodes.hasNext()) {
	    Node subjectNode = (Node) subjectNodes.next();
	    // Get the friendly name given to subject
	    String subjectName = XMLUtils.getNodeAttributeValue(
		subjectNode, PolicyManager.NAME_ATTRIBUTE);

	    String exclusive = XMLUtils.getNodeAttributeValue(
		subjectNode, INCLUDE_TYPE);
	    // Add the subject to users collection
            addSubject(subjectName, stm.getSharedSubject(subjectName),
                    EXCLUSIVE_TYPE.equals(exclusive));
        }
    }

    /**
     * Constructor to obtain an instance of <code>Subjects</code>
     * to hold collection of users represented as
     * <code>Subject</code>
     *
     * @param name name for the collection of <code>Subject</code>
     * @param description user friendly description for
     * the collection of <code>Subject</code>  
     */
    public Subjects(String name, String description) {
	this.name = (name == null) ? 
	    ("Subjects:" + ServiceTypeManager.generateRandomName()) : name;
	this.description = (description == null) ?
	    "" : description;
    }

    /**
     * Returns the name for the collection of users
     * represented as <code>Subject</code>
     *
     * @return name of the collection of subjects
     */
    public String getName() {
	return (name);
    }

    /**
     * Returns the description for the collection of users
     * represented as <code>Subject</code>
     *
     * @return description for the collection of subjects
     */
    public String getDescription() {
	return (description);
    }

    /**
     * Sets the name for this instance of the
     * <code>Subjects<code> which contains a collection
     * of users respresented as <code>Subject</code>.
     *
     * @param name for the collection of subjects
     */
    public void setName(String name) {
	this.name = (name == null) ?
	    ("Subjects:" + ServiceTypeManager.generateRandomName()) : name;
    }

    /**
     * Sets the description for this instance of the
     * <code>Subjects<code> which contains a collection
     * of users respresented as <code>Subject</code>.
     *
     * @param description description for the collection subjects
     */
    public void setDescription(String description) {
	this.description = (description == null) ?
	    "" : description;
    }

    /**
     * Returns the names of <code>Subject</code> objects
     * contained in this object.
     *
     * @return names of <code>Subject</code> contained in
     * this object
     */
    public Set getSubjectNames() {
	return (users.keySet());
    }

    /**
     * Returns the <code>Subject</code> object associated
     * with the given subject name.
     *
     * @param subjectName name of the subject object
     *
     * @return subject object corresponding to subject name
     *
     * @exception NameNotFoundException if a subject
     * with the given name is not present
     */
    public Subject getSubject(String subjectName)
	throws NameNotFoundException 
    {
	QualifiedSubject answer = (QualifiedSubject) users.get(subjectName);
	if (answer == null) {
	    String[] objs = { subjectName };
	    throw (new NameNotFoundException(ResBundleUtils.rbName,
		"name_not_present", objs,
		subjectName, PolicyException.USER_COLLECTION));
	
	}
	return answer.getSubject();
    }

    /**
     * Returns the <code>Subject</code> object associated
     * with the given subject name.
     *
     * @param subjectName name of the subject object
     *
     * @return <code>Subject</code> object corresponding to subject name
     *
     */
    Subject fetchSubject(String subjectName) {
	QualifiedSubject answer = (QualifiedSubject) users.get(subjectName);
        return (answer != null) ? answer.getSubject() : null;
    }

    /**
     * Adds a <code>Subject</code> object to the this instance
     * of user collection. Since the name is not provided it
     * will be dynamically assigned such that it is unique within
     * this instance of the user collection. However if a subject 
     * entry with the same name already exists in the user collection
     * <code>NameAlreadyExistsException</code> will be thrown.
     * The subject is added as a normal (non exclusive) subject.
     * So, policy will apply to members of the subject.
     *
     * @param subject instance of the subject object added to this
     * collection
     *
     * @exception NameAlreadyExistsException throw if a 
     * subject object is present with the same name 
     */
    public void addSubject(Subject subject)
	throws NameAlreadyExistsException 
    {
	addSubject(null, subject, false);
    }

    /**
     * Adds a <code>Subject</code> object to the this instance
     * of user collection. If another subject with the same name
     * already exists in the user collection
     * <code>NameAlreadyExistsException</code> will be thrown.
     * The subject is added as a normal (non exclusive) subject.
     * So, policy will apply to members of the subject.
     *
     * @param subjectName name for the subject instance
     * @param subject instance of the subject object added to this
     * collection
     *
     * @exception NameAlreadyExistsException if a 
     * subject object is present with the same name 
     */
    public void addSubject(String subjectName, Subject subject)
        throws NameAlreadyExistsException 
    {
        addSubject(subjectName, subject, false);
    }

    /**
     * Adds a <code>Subject</code> object to the this instance
     * of user collection. If another subject with the same name
     * already exists in the user collection
     * <code>NameAlreadyExistsException</code> will be thrown.
     *
     * @param subjectName name for the subject instance
     * @param subject instance of the subject object added to this
     * collection
     *
     * @param exclusive boolean flag indicating whether the subject 
     *        is to be exclusive subject. If subject is exclusive, 
     *        policy applies to users who are not members of the 
     *        subject. Otherwise, policy applies to members of the subject.
     *
     * @exception NameAlreadyExistsException if a 
     * subject object is present with the same name 
     */
    public void addSubject(String subjectName, Subject subject, boolean
        exclusive)
        throws NameAlreadyExistsException 
    {
	if (subjectName == null) {
	    subjectName = "Subject:" +
		ServiceTypeManager.generateRandomName();
	}
	if (users.containsKey(subjectName)) { 
	    String[] objs = { subjectName };
	    throw (new NameAlreadyExistsException(ResBundleUtils.rbName,
		"name_already_present", objs,
		subjectName, PolicyException.USER_COLLECTION));
	}
	users.put(subjectName, new QualifiedSubject(subject, exclusive));
    }


    /**
     * Replaces an existing subject object having the same name
     * with the new one. If a <code>Subject</code> with the given
     * name does not exist, <code>NameNotFoundException</code>
     * will be thrown.
     * The subject is replaced as a normal (non exclusive) subject.
     * So, policy will apply to members of the subject.
     *
     * @param subjectName name for the subject instance
     * @param subject instance of the subject object that will
     * replace another subject object having the given name
     *
     * @exception NameNotFoundException if a subject instance
     * with the given name is not present
     */
    public void replaceSubject(String subjectName, Subject subject)
        throws NameNotFoundException 
    {
        replaceSubject(subjectName, subject, false);
    }

    /**
     * Replaces an existing subject object having the same name
     * with the new one. If a <code>Subject</code> with the given
     * name does not exist, <code>NameNotFoundException</code>
     * will be thrown.
     *
     * @param subjectName name for the subject instance
     * @param subject instance of the subject object that will
     *          replace another subject object having the given name
     *
     * @param exclusive boolean flag indicating whether the subject 
     *        is to be exclusive subject. If subject is exclusive, 
     *        policy applies to users who are not members of the 
     *        subject. Otherwise, policy applies to members of the subject.
     *
     * @exception NameNotFoundException if a subject instance
     * with the given name is not present
     */
    public void replaceSubject(String subjectName, Subject subject, 
        boolean exclusive) throws NameNotFoundException 
    {
	if (!users.containsKey(subjectName)) {
	    String[] objs = { subjectName };
	    throw (new NameNotFoundException(ResBundleUtils.rbName,
		"name_not_present", objs,
		subjectName, PolicyException.USER_COLLECTION));
	}
	users.put(subjectName, new QualifiedSubject(subject, exclusive));
    }

    /**
     * Removes the <code>Subject</code> object identified by
     * the subject name. If a subject instance with the given
     * name does not exist, the method will return silently.
     *
     * @param subjectName name of the subject instance that
     * will be removed from the user collection
     * @return the subject that was just removed
     */
    public Subject removeSubject(String subjectName) {
        Subject subject = null;
        QualifiedSubject qualifiedSubject 
                = (QualifiedSubject)users.remove(subjectName);
        if (qualifiedSubject != null) {
            subject = qualifiedSubject.getSubject();
        }
        return subject;
    }
 
    /**
     * Removes the <code>Subject</code> object identified by
     * object's <code>equals</code> method. If a subject instance
     * does not exist, the method will return silently.
     *
     * @param subject subject object that
     * will be removed from the user collection
     * @return the subject that was just removed
     */
    public Subject removeSubject(Subject subject) {
        Subject s = null;
	String subjectName = getSubjectName(subject);
	if (subjectName != null) {
            QualifiedSubject qualifiedSubject 
                    = (QualifiedSubject)users.remove(subjectName);
            if (qualifiedSubject != null) {
                s = qualifiedSubject.getSubject();
            }
	}
        return s;
    }

    /**
     * Checks if the subject is exclusive. 
     * If subject is exclusive, policy applies to users who are not members of
     * the subject. Otherwise, policy applies to members of the subject.
     *
     * @param subjectName name of the subject 
     * @return <code>true</code> if the subject is exclusive, <code>false</code>
     *        otherwise
     * @throws NameNotFoundException if the subject with the given subjectName 
     *         does not exist in the policy
     *
     *
     */
    public boolean isSubjectExclusive(String subjectName) 
            throws NameNotFoundException {
	if (!users.containsKey(subjectName)) {
	    String[] objs = { subjectName };
	    throw (new NameNotFoundException(ResBundleUtils.rbName,
		"name_not_present", objs,
		subjectName, PolicyException.USER_COLLECTION));
	} else {
            return ((QualifiedSubject)users.get(subjectName)).isExclusive();
        }
    }

    /**
     * Checks if the subject is a reference to a <code>Subject</code>
     * defined at the realm. 
     *
     * @param subjectName name of the subject 
     * @return <code>true</code> if the subject is a reference to
     * a <code>Subject</code> definet at the realm, <code>false</code>
     *        otherwise
     * @throws NameNotFoundException if the subject with the given subjectName 
     *         does not exist in the policy
     *
     *
     */
    public boolean isRealmSubject(String subjectName) 
            throws NameNotFoundException {
	if (!users.containsKey(subjectName)) {
	    String[] objs = { subjectName };
	    throw (new NameNotFoundException(ResBundleUtils.rbName,
		"name_not_present", objs,
		subjectName, PolicyException.USER_COLLECTION));
	} else {
            return ((QualifiedSubject)users.get(subjectName)).isRealmSubject();
        }
    }

    /**
     * Returns the name associated with the given subject object.
     * It uses the <code>equals</code> method on the subject
     * to determine equality. If a subject instance that matches
     * the given subject object is not present, the method
     * returns <code>null</code>.
     *
     * @param subject subject object for which this method will
     * return its associated name
     *
     * @return user friendly name given to the subject object;
     * <code>null</code> if not present
     */
    public String getSubjectName(Subject subject) {
	String answer = null;
	Iterator items = users.keySet().iterator();
	while (items.hasNext()) {
	    String subjectName = (String) items.next();
            QualifiedSubject qs = (QualifiedSubject) users.get(subjectName);
	    if (qs.getSubject().equals(subject)) {
		answer = subjectName;
		break;
	    }
	}
	return (answer);
    }

    /**
     * Checks if two <code>Subjects</code> are identical.
     * Two subjects (or user collections) are identical only
     * if both have the same set of <code>Subject</code> objects.
     *
     * @param o object againt which this subjects object
     * will be checked for equality
     *
     * @return <code>true</code> if all the subjects match;
     * <code>false</code> otherwise
     */
    public boolean equals(Object o) {
	if (o instanceof Subjects) {
	    Subjects s = (Subjects) o;
            if (s.users.size() == this.users.size()) {
                Iterator subjects = users.entrySet().iterator();
                while (subjects.hasNext()) {
                    Object ss = ((Map.Entry) subjects.next()).getValue();
                    if (!s.users.containsValue(ss)) {
                        return (false);
                    }
                }
                return (true);
            }
	}
	return (false);
    }

    /**
     * Returns a new copy of this object with the identical
     * set of user collections (subjects).
     *
     * @return a copy of this object with identical values
     */
    public Object clone() {
	Subjects answer = null;
	try {
	    answer = (Subjects) super.clone();
	} catch (CloneNotSupportedException se) {
            answer = new Subjects();
        }
	answer.name = name;
	answer.description = description;
	answer.users = new HashMap();
	Iterator items = users.keySet().iterator();
	while (items.hasNext()) {
	    String item = (String)items.next();
	    QualifiedSubject qualifiedSubject 
                    = (QualifiedSubject) users.get(item);
	    answer.users.put(item, 
                    new QualifiedSubject( 
                    (Subject)(qualifiedSubject.getSubject().clone()),
                    qualifiedSubject.isExclusive()));
	}
	return (answer);
    }

    /**
     * Checks if the given user (using <code>SSOToken</code>) belongs
     * to any of the subjects contained in this user collection (subjects).
     * In the current implementation it is sufficient if the user
     * belongs to one of the subject objects, however in the future it
     * can be extended to have complex logical operations.
     *
     * @param token single-sign-on token of the user
     * @return <code>true</code> if the user is memeber of one
     * of the subjects; <code>false</code> otherwise.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if an error occured while
     * checking if the user is a member of one of subjects
     */
    public boolean isMember(SSOToken token) throws SSOException,
            PolicyException {

        boolean member = false;
        long currentTime = System.currentTimeMillis();
        long[]  cachedResult = null;
        cachedResult = (long[])resultCache.get(token.getTokenID().toString());
        if (cachedResult == null) {
            cachedResult = new long[2];
        }
        if ( (currentTime - cachedResult[0] ) < resultTtl ) {
            member = (cachedResult[1] == 1) ? true: false;
	    if (PolicyManager.debug.messageEnabled()) {
		PolicyManager.debug.message("Subjects.isMember():getting "
		    +"subject evaluation results from resultCache of policy");
	    }
        } else {
            Iterator items = users.entrySet().iterator();
            while (items.hasNext()) {
                QualifiedSubject qualifiedSubject 
                        = (QualifiedSubject)((Map.Entry) items.next())
                        .getValue();
                if (qualifiedSubject.subject.isMember(token)) {
                    if (!qualifiedSubject.exclusive) {
                        member = true;
                        break;
                    }
                } else {
                    if (qualifiedSubject.exclusive) {
                        member = true;
                        break;
                    }
                }

            }
            long memberLong = member ? 1: 0;
            cachedResult[0] = currentTime;
            cachedResult[1] = memberLong;

            //Boolean b = member ? Boolean.TRUE : Boolean.FALSE;
            resultCache.put(token.getTokenID().toString(), cachedResult);

        }
	return member;
    }

    /**
     * Returns XML string representation of the subject
     * (user collection) object.
     *
     * @return xml string representation of this object
     */
    public String toString() {
	return (toXML());
    }

    /**
     * Returns XML string representation of the subject
     * (user collection) object.
     *
     * @return xml string representation of this object
     */
    protected String toXML() {
	StringBuilder sb = new StringBuilder(100);
	sb.append("\n").append(SUBJECTS_ELEMENT_BEGIN)
	    .append(XMLUtils.escapeSpecialCharacters(name))
            .append(SUBJECTS_DESCRIPTION)
	    .append(XMLUtils.escapeSpecialCharacters(description))
            .append("\">");
	Iterator items = users.keySet().iterator();
	while (items.hasNext()) {
	    String subjectName = (String) items.next();
	    QualifiedSubject qualifiedSubject 
                    = (QualifiedSubject) users.get(subjectName);
            boolean realmSubject = qualifiedSubject.isRealmSubject();
            if (realmSubject) {
                sb.append("\n").append(REALM_SUBJECT_ELEMENT)
                        .append(XMLUtils.escapeSpecialCharacters(subjectName))
                        .append("\" ")
                        .append(INCLUDE_TYPE).append("=\"")
                        .append(qualifiedSubject.isExclusive() 
                                ? EXCLUSIVE_TYPE : INCLUSIVE_TYPE)
                        .append(REALM_SUBJECT_ELEMENT_END);
            } else {
                Subject subject = qualifiedSubject.getSubject();
                sb.append("\n").append(SUBJECT_ELEMENT)
                    .append(XMLUtils.escapeSpecialCharacters(subjectName))
                    .append(SUBJECT_TYPE)
                    .append(XMLUtils.escapeSpecialCharacters(
                            SubjectTypeManager.subjectTypeName(subject)))
                    .append("\" ")
                    .append(INCLUDE_TYPE).append("=\"")
                    .append(qualifiedSubject.isExclusive() 
                            ? EXCLUSIVE_TYPE : INCLUSIVE_TYPE)
                    .append("\">");
                // Add attribute values pairs
                Set v = subject.getValues();
                if ((v != null) && !v.isEmpty()) {
                    sb.append("\n").append(ATTR_VALUE_BEGIN);
                    Iterator values = v.iterator();
                    while (values.hasNext()) {
                        sb.append(VALUE_BEGIN)
                            .append(XMLUtils.escapeSpecialCharacters(
                                    (String) values.next()))
                            .append(VALUE_END);
                    }
                    sb.append("\n").append(ATTR_VALUE_END);
                }
                sb.append("\n").append(SUBJECT_ELEMENT_END);
            }
	}
	sb.append("\n").append(SUBJECTS_ELEMENT_END);
	return (sb.toString());		
    }

    /**
     * Sets ttl for Subjects result.
     * That is once isMember() is invoked, the result is cached for ttl time.
     * For subsequent requests of isMember() for the same ssoToken, result is 
     * simply returned from cache.
     * We maintain an Cache of isMember() results keyed by tokenId.
     * @param ttl Subjects result ttl in milliseconds
     */
    void setResultTtl(long ttl) {
        this.resultTtl = ttl;
    }

    /**
     * Returns ttl for Subjects result.
     * @return Subjects result ttl in milliseconds
     */
    long getResultTtl() {
        return resultTtl;
    }

    /**
     * Returns ttl for Subjects result for the given sso token.
     * @param token sso token for which to compute subjects result ttl
     * @return Subjects result ttl in milliseconds for the given sso token
     */
    long getResultTtl(SSOToken token) {
        long ttl = Long.MAX_VALUE;
        long[]  cachedResult 
                = (long[])resultCache.get(token.getTokenID().toString());
        if (cachedResult != null) {
            ttl = cachedResult[0] + resultTtl;
        }
        return ttl;
    }

    void setPolicyConfig(Map policyConfig) throws PolicyException {
        resultCache = new Cache(SUBJECTS_RESULT_CACHE_SIZE);
        Iterator sIter = users.keySet().iterator();
        while ( sIter.hasNext() ) {
            QualifiedSubject qualifiedSubject 
                    = (QualifiedSubject) users.get(sIter.next());
            qualifiedSubject.getSubject().initialize(policyConfig);
        }
        setResultTtl(PolicyConfig.getSubjectsResultTtl(policyConfig));
    }

    /**
     * Returns the number of <code>Subject</code> elements in this
     * </code>Subjects</code> object
     *
     * @return the number of <code>Subject</code> elements in this
     *           </code>Subjects<code> object
     */
    int size() {
        return users.size();
    }

    /**
     * Checks whether subject result is in the cache for the sso token
     *
     * @param token sso token
     * @return  <code>true</code> if subject result for the sso token is in
     *           cache, else <code>false</code>
     * @throws SSOException if the token is invalid
     *       
     */
    boolean isSubjectResultCached(SSOToken token)  throws SSOException {
        return (resultCache.get(token.getTokenID().toString())
                != null);
    }

    // Private variables to construct the XML document
    private static String SUBJECTS_ELEMENT_BEGIN = "<Subjects name=\"";
    private static String SUBJECTS_DESCRIPTION = "\" description=\"";
    private static String SUBJECTS_ELEMENT_END = "</Subjects>";
    private static String SUBJECT_ELEMENT = "<Subject name=\"";
    private static String INCLUDE_TYPE = "includeType";
    private static String SUBJECT_TYPE = "\" type=\"";
    private static String SUBJECT_ELEMENT_END = "</Subject>";
    private static String ATTR_VALUE_BEGIN =
	"<AttributeValuePair><Attribute name=\"Values\"/>";
    private static String VALUE_BEGIN = "<Value>";
    private static String VALUE_END = "</Value>";
    private static String ATTR_VALUE_END = "</AttributeValuePair>";
    private static String SUBJECT_VALUES_ATTR_NAME = "Values";
    private static String INCLUSIVE_TYPE = "inclusive";
    private static String EXCLUSIVE_TYPE = "exclusive";
    private static String REALM_SUBJECT_ELEMENT = "<RealmSubject name=\"";
    private static String REALM_SUBJECT_ELEMENT_END = "\"></RealmSubject>";



    /**
     * Class that represents a <code>Subject</code> and whether it is
     * <code>exclusive</code>. An <code>exclusive</code> 
     * <code>Subject</code> treats users 
     * who do not match the name values specified in the <code>Subject</code>
     * as members of the <code>Subject</code> 
     */
    private static class QualifiedSubject {

        Subject subject;
        boolean exclusive = false;

        /**
         * Constructs a <code>QualifiedSubject</code>
         * @param subject <Subject> to initialize this object with 
         * @param exclusive <code>exclusive</code> state of this object
         */
        QualifiedSubject(Subject subject, boolean exclusive) {
            this.subject = subject;
            this.exclusive = exclusive;
        }

        /**
         * Returns <code>Subject</code> this object was initialized with
         * @return <code>Subject</code> this object was initialized with
         */
        Subject getSubject() {
            return subject;
        }

        /**
         * Returns <code>exclusive</code> state of this object
         * @return <code>exclusive</code> state of this object
         */
        boolean isExclusive() {
                return exclusive;
        }

        /**
         * Checks if this object is a realm scoped <code>Subject</code>
         * @return <code>true</code> if this object is a realm scoped
         * <code>Subject</code>, otherwise <code>false</code> 
         */
        boolean isRealmSubject() {
            return subject instanceof SharedSubject;
        }

    }

    /**
     * Clears the cached membership evaluation results corresponding
     * to the <code>tokenIdString</code>. This is triggered through
     * <code>PolicySSOTokenListener</code>, <code>PolicyCache</code>
     * and <code>Policy</code> when session property
     * of a logged in user is changed
     *
     * @param tokenIdString sessionId of the user whose session property changed
     */
    void clearSubjectResultCache(String tokenIdString) throws PolicyException {
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message(
                    "Subjects.clearSubjectResultCache(tokenIdString): "
                    + " clearing cached subject evaluation result for "
                    + " tokenId XXXXX");
        }
        resultCache.remove(tokenIdString);
    }

}
