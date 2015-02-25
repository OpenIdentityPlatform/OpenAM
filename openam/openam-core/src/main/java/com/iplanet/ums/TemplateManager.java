/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TemplateManager.java,v 1.4 2008/06/25 05:41:46 qcheng Exp $
 *
 */

package com.iplanet.ums;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.Vector;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.util.I18n;

/**
 * The class manages a set of templates. The set of templates can be used to
 * define what attributes to use when creating an object, or what attributes to
 * retrieve for a particular object.
 * 
 * <p>
 * Example:
 * <p>
 * 
 * <pre>
 * TemplateManager mgr = TemplateManager.getTemplateManager();
 * 
 * Guid guid = new Guid(&quot;o=vortex.com&quot;);
 * 
 * CreationTemplate t1 = mgr.getCreationTemplate(&quot;BasicUser&quot;, guid,
 *         TemplateManager.SCOPE_ANCESTORS);
 * 
 * CreationTemplate t2 = mgr.getCreationTemplate(User.class, guid,
 *         TemplateManager.SCOPE_ANCESTORS);
 * 
 * SearchTemplate t3 = mgr.getSearchTemplate(&quot;BasicUserSearch&quot;, guid,
 *         TemplateManager.SCOPE_ANCESTORS);
 * </pre>
 * 
 * @see com.iplanet.ums.Template
 * @see com.iplanet.ums.CreationTemplate
 * @see com.iplanet.ums.SearchTemplate
 * @supported.api
 */
public class TemplateManager implements java.io.Serializable {

    /**
     * Search scope for determining how to get the configuration data. This will
     * get the configuration data at the organization level specified.
     * 
     * @supported.api
     */
    public static final int SCOPE_ORG = 0;

    /**
     * Search scope for determining how to get the configuration data. This will
     * get the configuration data from the nearest ancestor containing the
     * configuration data.
     * 
     * @supported.api
     */
    public static final int SCOPE_ANCESTORS = 1;

    /**
     * Search scope for determining how to get the configuration data. This will
     * get the configuration data at the organization level, and if not found,
     * then at the root level.
     * 
     * @supported.api
     */
    public static final int SCOPE_TOP = 2;

    private static final String TEMPLATE_NAME = "name";

    private static final String TEMPLATE_JAVACLASS = "javaclass";

    private static final String TEMPLATE_OPTIONAL = "optional";

    private static final String TEMPLATE_REQUIRED = "required";

    private static final String TEMPLATE_VALIDATED = "validated";

    private static final String TEMPLATE_NAMINGATTRIBUTE = "namingattribute";

    private static final String TEMPLATE_SEARCH_FILTER = "searchfilter";

    private static final String SCHEMA2_SEARCH_FILTER = 
        "inetDomainSearchFilter";

    private static final String TEMPLATE_ATTRS = "attrs";

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Default constructor that registers a default class resolver and accesses
     * template configuration information.
     * 
     * @throws UMSException
     *             if an exception occurs registering the resolver or accessing
     *             configuration data.
     */
    protected TemplateManager() throws UMSException {
        // Register com.iplanet.ums.DefaultClassResolver to resolve
        // a set of attributes to a Java class.
        addClassResolver(new DefaultClassResolver());
        // Register com.iplanet.ums.GroupResolver that can distinguish
        // between regular dynamic groups and assignable ones
        addClassResolver(new GroupResolver());
        try {
            m_configManager = ConfigManagerUMS.getConfigManager();
        } catch (ConfigManagerException e) {
            throw new UMSException(e.getMessage());
        }
    }

    /**
     * Clients can only obtain a reference through this method.
     * 
     * @return the one and only instance
     * @throws UMSException
     *             if an exception occurs while getting an instance of a
     *             template manager.
     * 
     * @supported.api
     */
    public static synchronized TemplateManager getTemplateManager()
            throws UMSException {
        if (m_mgr == null) {
            m_mgr = new TemplateManager();
        }
        return m_mgr;
    }

    /**
     * Registers a class that can resolve a set of attributes to a Java class.
     * The last class registered is the first to be called in the resolution
     * chain.
     * 
     * @param resolver
     *            a class that can produce a Java class instance from an ID and
     *            a set of attributes
     */
    public void addClassResolver(IClassResolver resolver) {
        m_resolvers.addElement(resolver);
    }

    /**
     * Unregisters a class that can resolve a set of attributes to a Java class.
     * 
     * @param resolver
     *            A class that can produce a Java class instance from an ID and
     *            a set of attributes
     */
    public void removeClassResolver(IClassResolver resolver) {
        m_resolvers.remove(resolver);
    }

    /**
     * Given a class, gets the default creation template for the object. This
     * will traverse the tree all the way to the root until the CreationTemplate
     * is found.
     * 
     * @param cls Class (instance of) to be queried for the template.
     * @param orgGuid GUID of the Organization where the config data is stored.
     * @return Creation template for the class or <code>null</code> if the
     *         class is not known or no template is registered for the class.
     * @throws UMSException if an exception occurs while getting the creation
     *         template.
     * @supported.api
     */
    public CreationTemplate getCreationTemplate(Class cls, Guid orgGuid)
            throws UMSException {

        return getCreationTemplate(cls, orgGuid, SCOPE_ANCESTORS);
    }

    /**
     * Returns default creation template of a given class.
     * 
     * @param cls Class (instance of) to be queried for the template.
     * @param orgGuid GUID of the Organization where the config data is stored.
     * @param scope Search scope for determining how to get the configuration
     *        data
     * @return Creation template for the class or <code>null</code> if the
     *         class is not known or no template is registered for the class
     * @throws UMSException if error occurs while getting the creation template.
     * @supported.api
     */
    public CreationTemplate getCreationTemplate(Class cls, Guid orgGuid,
            int scope) throws UMSException {
        if (cls == null) {
            String msg = i18n.getString(IUMSConstants.BAD_CLASS);
            throw new IllegalArgumentException(msg);
        }

        AttrSet attrSet = null;
        try {
            attrSet = m_configManager.getCreationTemplateForClass(orgGuid, cls
                    .getName(), scope);
        } catch (ConfigManagerException e) {
            throw new UMSException(e.getMessage());
        }
        if (attrSet == null) {
            return null;
        }

        return toCreationTemplate(attrSet);
    }

    /**
     * Returns a template from a supplied template name. This will traverse the
     * tree all the way to the root until the CreationTemplate is found.
     * 
     * @param name Name of template.
     * @param orgGuid GUID of the Organization where the config data is stored.
     * @return CreationTemplate matching the supplied name, or <code>null</code>
     *         if there is no matching template
     * @throws UMSException if error occurs while getting the creation template.
     * @supported.api
     */
    public CreationTemplate getCreationTemplate(String name, Guid orgGuid)
            throws UMSException {
        return getCreationTemplate(name, orgGuid, SCOPE_ANCESTORS);
    }

    /**
     * Returns a template from a supplied template name.
     * 
     * @param name Name of template.
     * @param orgGuid GUID of the Organization where the config data is stored.
     * @param scope Search scope for determining how to get the configuration
     *        data.
     * @return CreationTemplate matching the supplied name, or <code>null</code>
     *         if there is no matching template
     * @throws UMSException if an exception occurs while getting the creation
     *         template.
     * @supported.api
     */
    public CreationTemplate getCreationTemplate(String name, Guid orgGuid,
            int scope) throws UMSException {
        if (name == null) {
            String msg = i18n.getString(IUMSConstants.MISSING_TEMPL_NAME);
            throw new IllegalArgumentException(msg);
        }

        AttrSet attrSet = null;

        try {
            attrSet = m_configManager.getCreationTemplate(orgGuid, name, scope);
        } catch (ConfigManagerException e) {
            throw new UMSException(e.getMessage());
        }

        if (attrSet == null) {
            return null;
        }

        return toCreationTemplate(attrSet);
    }

    /**
     * Returns a template from a supplied template name. This will traverse the
     * tree all the way to the root till the SearchTemplate is found.
     * 
     * @param name Name of template.
     * @param orgGuid GUID of the Organization where the config data is stored
     * @return SearchTemplate matching the supplied name, or <code>null</code>
     *         if there is no matching template
     * @throws UMSException if error occurs while getting the search template.
     * @supported.api
     */
    public SearchTemplate getSearchTemplate(String name, Guid orgGuid)
            throws UMSException {
        return getSearchTemplate(name, orgGuid, SCOPE_ANCESTORS);
    }

    /**
     * Returns a template from a supplied template name.
     * 
     * @param name Name of Template.
     * @param orgGuid GUID of the Organization where the config data is stored.
     * @param scope Search scope for determining how to get the configuration
     *        data.
     * @return SearchTemplate matching the supplied name, or <code>null</code>
     *         if there is no matching template.
     * @throws UMSException if an exception occurs while getting the search
     *         template.
     * @supported.api
     */
    public SearchTemplate getSearchTemplate(
        String name,
        Guid orgGuid, 
        int scope
    ) throws UMSException {
        if (name == null) {
            String msg = i18n.getString(IUMSConstants.MISSING_TEMPL_NAME);

            throw new IllegalArgumentException(msg);
        }
        AttrSet attrSet = null;
        try {
            attrSet = m_configManager.getSearchTemplate(orgGuid, name, scope);
        } catch (ConfigManagerException e) {
            throw new UMSException(e.getMessage());
        }
        if (attrSet == null) {
            return null;
        }
        return toSearchTemplate(attrSet);
    }

    /**
     * Returns a set of known creation templates.
     * 
     * @param orgGuid GUID of the Organization where the config data is stored.
     * @return Names of known creation templates
     * @throws UMSException if an exception occurs.
     * @supported.api
     */
    public Set getCreationTemplateNames(Guid orgGuid) throws UMSException {
        Set names = null;
        try {
            names = m_configManager.getCreationTemplateNames(orgGuid);
        } catch (ConfigManagerException e) {
            throw new UMSException(e.getMessage());
        }
        return (names != null) ? names : Collections.EMPTY_SET;
    }

    /**
     * Returns a set of known search templates.
     * 
     * @param orgGuid GUID of the Organization where the config data is stored.
     * @return Names of known search templates.
     * @throws UMSException if an exception occurs.
     * @supported.api
     */
    public Set getSearchTemplateNames(Guid orgGuid) throws UMSException {
        Set names = null;
        try {
            names = m_configManager.getSearchTemplateNames(orgGuid);
        } catch (ConfigManagerException e) {
            throw new UMSException(e.getMessage());
        }
        return (names != null) ? names : Collections.EMPTY_SET;
    }

    /**
     * Replaces an existing CreationTemplate with the one specified.
     * 
     * @param template
     *            CreationTemplate to be modified
     * @param orgGuid
     *            the guid of the Organization where the config data is stored
     * @throws UMSException
     *             if an exception occurs
     */
    public void replaceCreationTemplate(CreationTemplate template, Guid orgGuid)
            throws UMSException {
        if (template == null) {
            return;
        }

        String templateName = template.getName();
        if (templateName == null) {
            String msg = i18n.getString(IUMSConstants.MISSING_TEMPL_NAME);

            throw new IllegalArgumentException(msg);
        }

        AttrSet attrSet = toAttrSet(template);
        try {
            m_configManager.replaceCreationTemplate(orgGuid, templateName,
                    attrSet);
        } catch (ConfigManagerException e) {
            throw new UMSException(e.getMessage());
        }
    }

    private AttrSet toAttrSet(CreationTemplate t) {
        AttrSet attrSet = new AttrSet();

        attrSet.add(new Attr(TemplateManager.TEMPLATE_NAME, t.getName()));

        attrSet.add(new Attr(TemplateManager.TEMPLATE_NAMINGATTRIBUTE, t
                .getNamingAttribute()));

        ArrayList classes = t.getCreationClasses();
        String[] classNames = new String[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            Class cls = (Class) classes.get(i);
            classNames[i] = cls.getName();
        }
        attrSet.add(new Attr(TemplateManager.TEMPLATE_JAVACLASS, classNames));

        Attr required = encodeAttrSet(TemplateManager.TEMPLATE_REQUIRED, t
                .getRequiredAttributeSet(), "=");
        if (required != null) {
            attrSet.add(required);
        }

        Attr optional = encodeAttrSet(TemplateManager.TEMPLATE_OPTIONAL, t
                .getOptionalAttributeSet(), "=");
        if (optional != null) {
            attrSet.add(optional);
        }

        Attr validated = encodeAttrSet(TemplateManager.TEMPLATE_VALIDATED, t
                .getValidation(), "=");
        if (validated != null) {
            attrSet.add(validated);
        }

        return attrSet;
    }

    /**
     * Gets the Java class given the id and attribute set of an entry. The Java
     * class is inferred from the default CreationTemplate(s) registered with
     * this template manager. A different inference rule could be implemented by
     * subclassing the TemplateManager and overriding this method. This
     * implementation figures out the Java class for the entry.
     * 
     * @param guid
     *            Guid of the entry
     * @param attrSet
     *            Attribute set of the entry
     * @return Java Class that maps to the list of LDAP object classes
     * @exception UMSException
     *                if the template manager has not been properly initialized
     */
    Class getJavaClassForEntry(String id, AttrSet attrSet) throws UMSException {
        Class javaClass = null;
        int i = m_resolvers.size() - 1;
        while ((javaClass == null) && (i >= 0)) {
            IClassResolver resolver = (IClassResolver) m_resolvers.elementAt(i);
            javaClass = resolver.resolve(id, attrSet);
            i--;
        }

        if (javaClass == null) {
            javaClass = PersistentObject.class;
        }

        return javaClass;
    }

    /**
     * Reads in a attribute set and converts name-value pairs to a
     * CreationTemplate object.
     * 
     * @param t
     *            attribute set contains template values
     * @return CreationTemplate object based on the name-value pairs
     */
    private CreationTemplate toCreationTemplate(AttrSet t) {
        Attr nameAttr = t.getAttribute(TEMPLATE_NAME);
        String name = null;
        if (nameAttr != null) {
            name = nameAttr.getValue();
        }

        Attr namingAttr = t.getAttribute(TEMPLATE_NAMINGATTRIBUTE);
        String namingAttribute = null;
        if (namingAttr != null) {
            namingAttribute = namingAttr.getValue();
        }

        Attr classAttr = t.getAttribute(TEMPLATE_JAVACLASS);
        String[] classNames = null;
        if (classAttr != null) {
            classNames = classAttr.getStringValues();
        }

        AttrSet required = decodeAttr(t.getAttribute(TEMPLATE_REQUIRED), "=");
        AttrSet optional = decodeAttr(t.getAttribute(TEMPLATE_OPTIONAL), "=");
        AttrSet validated = decodeAttr(t.getAttribute(TEMPLATE_VALIDATED), "=");

        CreationTemplate template = new CreationTemplate();
        ArrayList classes = new ArrayList();

        try {
            if (classNames != null) {
                for (int i = 0; i < classNames.length; i++) {
                    Class cls = Class.forName(classNames[i]);
                    classes.add(cls);
                }
            }
            template = new CreationTemplate(name, required, optional, classes);

        } catch (ClassNotFoundException e) {
            template = new CreationTemplate(name, required, optional);
        }

        if (validated != null) {
            template.setValidation(validated);
        }

        if (namingAttribute != null) {
            template.setNamingAttribute(namingAttribute);
        }

        return template;
    }

    /**
     * Reads in a attribute set and converts name-value pairs to a
     * SearchTemplate object.
     * 
     * @param t
     *            attribute set contains template values
     * @return SearchTemplate object based on the name-value pairs
     */
    private SearchTemplate toSearchTemplate(AttrSet t) {
        Attr nameAttr = t.getAttribute(TEMPLATE_NAME);
        String name = null;
        if (nameAttr != null) {
            name = nameAttr.getValue();
        }

        Attr filterAttr = t.getAttribute(SCHEMA2_SEARCH_FILTER);
        if (filterAttr == null) {
            filterAttr = t.getAttribute(TEMPLATE_SEARCH_FILTER);
        }
        String filter = null;
        if (filterAttr != null) {
            filter = filterAttr.getValue();
        }

        AttrSet attrSet = decodeAttr(t.getAttribute(TEMPLATE_ATTRS), "=");

        SearchTemplate template = new SearchTemplate();

        template = new SearchTemplate(name, attrSet, filter);

        return template;
    }

    /**
     * Decode single attribute with multiple values into an AttrSet. For
     * example:
     * 
     * <pre>
     *    Attribute:
     *       required: objectclass=top
     *       required: objectclass=groupofuniquenames
     *       required: cn
     *       required: sn
     *  
     *    Attribute Set:
     *       objectclass: top
     *       objectclass: groupofuniquenames
     *       cn:
     *       sn:
     * </pre>
     * 
     * @param attr
     *            Attribute to be decoded from
     * @param delimiter
     *            Delimiter used in the encoding
     * @return Decoded attribute set
     */
    private AttrSet decodeAttr(Attr attr, String delimiter) {

        if (attr == null)
            return null;

        String[] values = attr.getStringValues();
        AttrSet attrSet = new AttrSet();

        for (int i = 0, size = attr.size(); i < size; i++) {
            String value = values[i];
            String attrName = null;
            String attrValue = null;

            int index = value.indexOf('=');
            if (index < 0) {
                attrName = value;
            } else {
                attrName = value.substring(0, index);
                attrValue = value.substring(index + 1, value.length());
            }

            if (attrValue != null && attrValue.length() != 0) {
                attrSet.add(new Attr(attrName, attrValue));
            } else {
                attrSet.add(new Attr(attrName));
            }

        }
        return attrSet;
    }

    /**
     * Encode an attrSet in a single attribute with multiple values using the
     * given attribute name and the values (tag,value) found in the given
     * attribute set. For example:
     * 
     * <pre>
     *    Attribute:
     *       required: objectclass=top
     *       required: objectclass=groupofuniquenames
     *       required: cn
     *       required: sn
     *  
     *    Attribute Set:
     *       objectclass: top
     *       objectclass: groupofuniquenames
     *       cn:
     *       sn:
     * </pre>
     * 
     * @param attrName
     *            Name of the encoded attribute.
     * @param attrSet
     *            Attribute set to be encoded in a single attribut.e
     * @param delimiter
     *            String token used as delimiter for the encoding.
     * @return Encoded attribute or null object if attrSet is empty.
     */
    private Attr encodeAttrSet(String attrName, AttrSet attrSet,
            String delimiter) {
        if (attrSet == null || attrSet.size() == 0) {
            return null;
        }

        Enumeration attrEnum = attrSet.getAttributes();
        Attr encodedAttr = new Attr(attrName);

        while (attrEnum.hasMoreElements()) {
            Attr a = (Attr) attrEnum.nextElement();
            String[] values = a.getStringValues();
            String[] encodedValues = new String[values.length];

            if (values.length == 0) {
                encodedAttr.addValue(a.getName());
            } else {
                for (int i = 0; i < values.length; i++) {
                    encodedValues[i] = a.getName() + delimiter + values[i];
                }
                encodedAttr.addValues(encodedValues);
            }
        }

        return encodedAttr;
    }

    private Vector m_resolvers = new Vector();

    private ConfigManagerUMS m_configManager = null;

    // Single instance of TemplateManager
    private static TemplateManager m_mgr;

}
