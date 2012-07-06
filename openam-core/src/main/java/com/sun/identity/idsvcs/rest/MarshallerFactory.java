/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MarshallerFactory.java,v 1.5 2008/06/25 05:43:34 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */
package com.sun.identity.idsvcs.rest;

import java.io.PrintWriter;
import java.io.Writer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.ListWrapper;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.Token;
import com.sun.identity.idsvcs.UserDetails;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Determines which marshaller to give based on class/type.
 */
public class MarshallerFactory {
    // All the various marshallers..
    public static MarshallerFactory XML = new MarshallerFactory("XML");
    public static MarshallerFactory JSON = new MarshallerFactory("JSON");
    public static MarshallerFactory PROPS = new MarshallerFactory("PROPS");

    // ===================================================================
    // Fields
    // ===================================================================
    private Map _map = new HashMap();
    
    private String protocol;

    private MarshallerFactory(String protocol) {
        this.protocol = protocol;
        // No support for JSON yet
        if (protocol.equals("JSON")) {
            _map.put(Token.class, JSONTokenMarshaller.class);
            _map.put(Boolean.class, JSONBooleanMarshaller.class);
            _map.put(String.class, JSONStringMarshaller.class);
            _map.put(String[].class, JSONStringArrayMarshaller.class);
            _map.put(UserDetails.class, JSONUserDetailsMarshaller.class);
            _map.put(IdentityDetails.class, JSONIdentityDetailsMarshaller.class);
            _map.put(Throwable.class, JSONThrowableMarshaller.class);
            _map.put(GeneralFailure.class, JSONThrowableMarshaller.class);
            _map.put(Throwable.class, JSONThrowableMarshaller.class);
            _map.put(ObjectNotFound.class, JSONThrowableMarshaller.class);
            _map.put(GeneralFailure.class, JSONThrowableMarshaller.class);
        } else if (protocol.equals("XML")) {
            _map.put(Token.class, XMLTokenMarshaller.class);
            _map.put(UserDetails.class, XMLUserDetailsMarshaller.class);
            _map.put(Boolean.class, XMLBooleanMarshaller.class);
            _map.put(String.class, XMLStringMarshaller.class);
            _map.put(String[].class, XMLStringArrayMarshaller.class);
            _map.put(IdentityDetails.class, XMLIdentityDetailsMarshaller.class);
            _map.put(List.class, PropertiesListMarshaller.class);
            _map.put(GeneralFailure.class, XMLGeneralFailureMarshaller.class);
            _map.put(ObjectNotFound.class, XMLObjectNotFoundMarshaller.class);
            _map.put(Throwable.class, XMLThrowableMarshaller.class);
        } else if (protocol.equals("PROPS"))  {
            _map.put(Token.class, PropertiesTokenMarshaller.class);
            _map.put(UserDetails.class, PropertiesUserDetailsMarshaller.class);
            _map.put(Boolean.class, PropertiesBooleanMarshaller.class);
            _map.put(String.class, PropertiesStringMarshaller.class);
            _map.put(String[].class, PropertiesStringArrayMarshaller.class);
            _map.put(IdentityDetails.class,
                PropertiesIdentityDetailsMarshaller.class);
            _map.put(IdentityDetails[].class,
                PropertiesIdentityDetailsArrayMarshaller.class);
            _map.put(List.class, PropertiesListMarshaller.class);
            _map.put(GeneralFailure.class,
                PropertiesGeneralFailureMarshaller.class);
            _map.put(ObjectNotFound.class,
                PropertiesObjectNotFoundMarshaller.class);
            _map.put(Throwable.class, PropertiesThrowableMarshaller.class);
        }
    }
    
    /**
     * Returns the Marshaller Map for the mechanism
     */
    Map getMarshallerMap() {
        return _map;
    }

    public Marshaller newInstance(Class type) {
        Marshaller ret = null;
        // initalize the map..
        if (_map == null) {
            _map = getMarshallerMap();
        }
        Class clazz = (Class) _map.get(type);
        if (clazz == null) {
            // something is not right throw..
            throw new IllegalArgumentException();
        }
        try {
            ret = (Marshaller) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }
    
    public String getProtocol() {
        return (protocol);
    }

    //=======================================================================
    // Marshalling w/ XML
    //=======================================================================
    
    /**
     * Marshall the Token into XML format.
     */
    static class XMLTokenMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null;
            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);
            xwrt.writeStartDocument();
            marshall(xwrt, (Token)value);
            xwrt.writeEndDocument();
        }
        public void marshall(XMLStreamWriter wrt, Token value)
            throws Exception {
            String token = (value != null) ? value.getId() : null;
            wrt.writeStartElement("token");
            if (token != null) {
                wrt.writeAttribute("id", token);
            }
            wrt.writeEndElement();
        }
    }
    /**
     * Marshall an Attribute into XML.
     */
    static class XMLAttributeMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null;
            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);
            xwrt.writeStartDocument();
            marshall(xwrt, (Attribute) value);
            xwrt.writeEndDocument();
        }
        public void marshall(XMLStreamWriter wrt, Attribute attr)
                throws Exception {
            assert wrt != null && attr != null;
            wrt.writeStartElement("attribute");
            if (attr != null) {
                wrt.writeAttribute("name", attr.getName());
                String[] vals = attr.getValues();
                for (int i = 0; (vals != null && i < vals.length); i++) {
                    String val = vals[i];
                    wrt.writeStartElement("value");
                    wrt.writeCharacters(val);
                    wrt.writeEndElement();
                }
            }
            wrt.writeEndElement();
        }
    }

    /**
     * Marshall an Exception class into XML format.
     */
    static class XMLGeneralFailureMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null;
            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);
            xwrt.writeStartDocument();
            marshall(xwrt, (GeneralFailure)value);
            xwrt.writeEndDocument();
        }

        public void marshall(XMLStreamWriter wrt, GeneralFailure gf)
                throws Exception {
            assert gf != null;
            wrt.writeStartElement("exception");
            wrt.writeAttribute("name", gf.getClass().getName());
            String msg = gf.getMessage();
            if (msg != null) {
               wrt.writeAttribute("message", gf.getMessage());
            // StringWriter sw = new StringWriter(200);
            // gf.printStackTrace(new PrintWriter(sw));
            // wrt.writeAttribute("stacktrace", sw.toString());
            // sw.close();
            }
            wrt.writeEndElement();
        }
    }

    /**
     * Marshall an Throwable exception into XML format.
     */
    static class XMLThrowableMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert ((wrt != null) && (value != null));
            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);
            xwrt.writeStartDocument();
            marshall(xwrt, (Throwable)value);
            xwrt.writeEndDocument();
        }

        public void marshall(XMLStreamWriter wrt, Throwable thr)
            throws Exception
        {
            wrt.writeStartElement("exception");
            wrt.writeAttribute("name", thr.getClass().getName());
            String msg = thr.getMessage();
            if (msg != null) {
                wrt.writeAttribute("message", thr.getMessage());
            }
            wrt.writeEndElement();
        }
    }

    /**
     * Marshall an ObjectNotFound exception into XML format.
     */
    static class XMLObjectNotFoundMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert ((wrt != null) && (value != null));

            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);
            xwrt.writeStartDocument();
            marshall(xwrt, (ObjectNotFound)value);
            xwrt.writeEndDocument();
        }

        public void marshall(XMLStreamWriter wrt, ObjectNotFound onf)
            throws Exception
        {
            wrt.writeStartElement("exception");
            wrt.writeAttribute("name", onf.getClass().getName());
            String msg = onf.getMessage();
            if (msg != null) {
                wrt.writeAttribute("message", onf.getMessage());
            }
            wrt.writeEndElement();
        }
    }

    /**
     * Marshall the UserDetails into Xml format.
     */
    static class XMLUserDetailsMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null;
            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);
            xwrt.writeStartDocument();
            marshall(xwrt, (UserDetails)value);
            xwrt.writeEndDocument();
        }

        public void marshall(XMLStreamWriter wrt, UserDetails value)
                throws Exception {
            // write out the userdetails element..
            wrt.writeStartElement("userdetails");
            if (value != null) {
                // marshall the token using the XMLTokenMarshaller..
                Token token = value.getToken();
                XMLTokenMarshaller mar = new XMLTokenMarshaller();
                mar.marshall(wrt, token);
                // write each of the roles..
                String[] values = value.getRoles();
                for (int i = 0; (values != null && i < values.length); i++) {
                    String role = values[i];
                    wrt.writeStartElement("role");
                    wrt.writeAttribute("id", role);
                    wrt.writeEndElement();
                }
                // write each of the attributes
                XMLAttributeMarshaller attrMarshaller =
                    new XMLAttributeMarshaller();
                Attribute[] vals = value.getAttributes();
                for (int i = 0; (vals != null && i < vals.length); i++) {
                    Attribute attr = vals[i];
                    attrMarshaller.marshall(wrt, attr);
                }
            }
            // end the userdetails..
            wrt.writeEndElement();
        }
    }

    /**
     * Marshall the UserDetails into Xml format.
     */
    static class XMLIdentityDetailsMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert wrt != null;
            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);
            xwrt.writeStartDocument();
            marshall(xwrt, (IdentityDetails)value);
            xwrt.writeEndDocument();
        }

        public void marshall(XMLStreamWriter wrt, IdentityDetails value)
            throws Exception
        {
            // write out the identitydetails element..
            wrt.writeStartElement("identitydetails");
            if (value != null) {
                // marshall the name.
                String name = value.getName();
                wrt.writeStartElement("name");
                wrt.writeAttribute("value", name);
                wrt.writeEndElement();

                // marshall the identity type
                String identityType = value.getType();
                wrt.writeStartElement("type");
                wrt.writeAttribute("value", identityType);
                wrt.writeEndElement();

                // marshall the realm
                String realm = value.getRealm();
                wrt.writeStartElement("realm");
                wrt.writeAttribute("value", realm);
                wrt.writeEndElement();

                // write the roles.
                ListWrapper roleList = value.getRoleList();
                if (roleList != null) {
                    String[] roles = roleList.getElements();
                    for (int i = 0; (roles != null && i < roles.length); i++) {
                        wrt.writeStartElement("role");
                        wrt.writeAttribute("id", roles[i]);
                        wrt.writeEndElement();
                    }
                }

                // write the groups.
                ListWrapper groupList = value.getGroupList();
                if (groupList != null) {
                    String[] groups = groupList.getElements();
                    for (int i = 0; (groups != null && i < groups.length); i++) {
                        wrt.writeStartElement("group");
                        wrt.writeAttribute("id", groups[i]);
                        wrt.writeEndElement();
                    }
                }

                // write the memberships.
                ListWrapper memberList = value.getMemberList();
                if (memberList != null) {
                    String[] members = memberList.getElements();
                    for (int i = 0; (members != null && i < members.length); i++) {
                        wrt.writeStartElement("member");
                        wrt.writeAttribute("id", members[i]);
                        wrt.writeEndElement();
                    }
                }

                // write each of the attributes
                XMLAttributeMarshaller attrMarshaller =
                    new XMLAttributeMarshaller();
                Attribute[] vals = value.getAttributes();

                for (int i = 0; ((vals != null) && (i < vals.length)); i++) {
                    Attribute attr = vals[i];
                    attrMarshaller.marshall(wrt, attr);
                }
            }

            // end the identitydetails..
            wrt.writeEndElement();
        }
    }

    /**
     * Marshall a List into Xml format.
     */
    static class XMLListMarshaller
        implements Marshaller
    {
        Writer wrt;
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert ((wrt != null) && (value != null));
            this.wrt = wrt;
            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);

            xwrt.writeStartDocument();
            marshall(xwrt, (List) value);
            xwrt.writeEndDocument();
        }

        public void marshall(XMLStreamWriter wrt, List value)
            throws Exception
        {
            // write out the identitydetails element..
            wrt.writeStartElement("List");

            if (value != null) {
                Iterator iter = value.iterator();

                while (iter.hasNext()) {
                    Object nextObj = iter.next();
                    Marshaller mar = MarshallerFactory.XML.newInstance(
                        nextObj.getClass());
                    Class[] params = { XMLStreamWriter.class,
                        nextObj.getClass() };
                    Method m = mar.getClass().getMethod("marshall", params);
                    Object[] objs = { wrt, value };
                    m.invoke(mar, objs);
                }
            }

            // end the List
            wrt.writeEndElement();
        }
    }

    /**
     * Marshall the Boolean into XML format.
     */
    static class XMLBooleanMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null && value != null;
            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);
            xwrt.writeStartDocument();
            marshall(xwrt, (Boolean)value);
            xwrt.writeEndDocument();
        }
        public void marshall(XMLStreamWriter wrt, Boolean value)
            throws Exception {
            String token = value.toString();
            assert ((token != null) && (token.length() != 0));
            wrt.writeStartElement("result");
            wrt.writeAttribute("boolean", token);
            wrt.writeEndElement();
        }
    }

    /**
     * Marshall a String into XML format.
     */
    static class XMLStringMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert ((wrt != null) && (value != null));

            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);

            xwrt.writeStartDocument();
            marshall(xwrt, (String)value);
            xwrt.writeEndDocument();
        }

        public void marshall(XMLStreamWriter wrt, String value)
            throws Exception
        {
            assert (value != null);
            wrt.writeStartElement("result");
            wrt.writeAttribute("string", value);
            wrt.writeEndElement();
        }
    }
    
    static class XMLStringArrayMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert (wrt != null);

            // get an XMl factory for use..
            XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xwrt = xmlFactory.createXMLStreamWriter(wrt);

            xwrt.writeStartDocument();
            marshall(xwrt, (String[]) value);
            xwrt.writeEndDocument();
        }

        public void marshall(XMLStreamWriter wrt, String value[])
            throws Exception
        {
            wrt.writeStartElement("result");
            if (value != null) {
                for (int i = 0; i < value.length; i++) {
                    wrt.writeStartElement("string");
                    wrt.writeCharacters(value[i]);
                    wrt.writeEndElement();
                }
            }
            wrt.writeEndElement();
        }
    }
    
    //=======================================================================
    // Marshalling w/ JSON
    //=======================================================================
    /**
     * Marshall the Token into JSON format.
     */
    static class JSONTokenMarshaller implements Marshaller {

        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null && value != null;
            wrt.write(getObject((Token) value).toString());
        }

        public JSONObject getObject(Token value) throws Exception {
            JSONObject token = new JSONObject();
            token.put("tokenId", value.getId());
            return token;
        }
    }

    static class JSONBooleanMarshaller implements Marshaller {

        public void marshall(Writer wrt, Object value) throws Exception {
            wrt.write(getObject((Boolean) value).toString());
        }

        public JSONObject getObject(Boolean value) throws Exception {
            JSONObject ret = new JSONObject();
            ret.put("boolean", value.booleanValue());
            return ret;
        }
    }

    static class JSONStringMarshaller implements Marshaller {

        public void marshall(Writer wrt, Object value) throws Exception {
            wrt.write(getObject((String) value).toString());
        }

        public JSONObject getObject(String value) throws Exception {
            JSONObject ret = new JSONObject();
            ret.put("string", value);
            return ret;
        }
    }

    static class JSONStringArrayMarshaller implements Marshaller {

        public void marshall(Writer wrt, Object value) throws Exception {
            wrt.write(getObject((String[]) value).toString());
        }

        public JSONObject getObject(String[] strings) throws Exception {
            JSONObject ret = new JSONObject();
            JSONArray array = new JSONArray(strings);
            ret.put("string", array);
            return ret;
        }
    }

    static class JSONUserDetailsMarshaller implements Marshaller {

        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null && value != null;
            wrt.write(getObject((UserDetails) value).toString());
        }

        public JSONObject getObject(UserDetails ud) throws Exception {
            JSONObject obj = new JSONObject();
            // add token..
            JSONObject token = new JSONTokenMarshaller()
                .getObject(ud.getToken());
            obj.put("token", token);
            // add roles..
            JSONArray roles = new JSONArray();
            String[] rols = ud.getRoles();
            if (rols != null) {
                for (String role : rols) {
                    roles.put(role);
                }
            }
            obj.put("roles", roles);
            // add attributes..
            JSONArray attributes = new JSONArray();
            JSONAttributeMarshaller attrMar = new JSONAttributeMarshaller();
            Attribute[] ats = ud.getAttributes();
            if (ats != null) {
                for (Attribute attr : ats) {
                    attributes.put(attrMar.getObject(attr));
                }
            }
            obj.put("attributes", attributes);
            return obj;
        }
    }

    /**
     * Marshall an Attribute object into JSON.
     */
    static class JSONAttributeMarshaller implements Marshaller {

        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null && value != null;
            wrt.write(getObject((Attribute) value).toString());
        }

        public JSONObject getObject(Attribute attr) throws Exception {
            JSONObject obj = new JSONObject();
            // add name..
            obj.put("name", attr.getName());
            // add values..
            JSONArray array = new JSONArray();
            String[] vals = attr.getValues();
            if (vals != null) {
                for (String val : vals) {
                    array.put(val);
                }
            }
            obj.put("values", array);

            return obj;
        }
    }

    static class JSONIdentityDetailsMarshaller implements Marshaller {

        public void marshall(Writer wrt, Object value) throws Exception {
            wrt.write(getObject((IdentityDetails) value).toString());
        }

        public JSONObject getObject(IdentityDetails id) throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("name", id.getName());
            obj.put("type", id.getType());
            obj.put("realm", id.getRealm());
            JSONArray roles = new JSONArray();
            ListWrapper roleList = id.getRoleList();
            if (roleList != null) {
                String[] values = roleList.getElements();
                if (values != null) {
                    for (String val : values) {
                        roles.put(val);
                    }
                }
            }
            obj.put("roles", roles);
            JSONArray groups = new JSONArray();
            ListWrapper groupList = id.getGroupList();
            if (groupList != null) {
                String[] values = groupList.getElements();
                if (values != null) {
                    for (String val : values) {
                        groups.put(val);
                    }
                }
            }
            obj.put("groups", groups);
            JSONArray members = new JSONArray();
            ListWrapper memberList = id.getMemberList();
            if (memberList != null) {
                String[] values = memberList.getElements();
                if (values != null) {
                    for (String val : values) {
                        members.put(val);
                    }
                }
            }
            obj.put("members", members);

            JSONArray attributes = new JSONArray();
            JSONAttributeMarshaller attrMar = new JSONAttributeMarshaller();
            Attribute[] ats = id.getAttributes();
            if (ats != null) {
                for (Attribute attr : ats) {
                    attributes.put(attrMar.getObject(attr));
                }
            }
            obj.put("attributes", attributes);

            return obj;
        }
    }

    /**
     * Marshall an Exception class into JSON format.
     */
    static class JSONThrowableMarshaller implements Marshaller {

        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null && value != null;
            wrt.write(getObject((Throwable)value).toString());
        }

        public JSONObject getObject(Throwable t) throws Exception {
            JSONObject obj = new JSONObject();
            JSONObject ex = new JSONObject();
            ex.put("name", t.getClass().getName());
            ex.put("message", t.getMessage());
            String msg = null;
            Throwable cause = t.getCause();
            if (cause != null) {
                msg = cause.getMessage();
            }
            ex.put("initCause", msg);
            obj.put("exception", ex);
            return obj;
        }
    }

        
    //=======================================================================
    // Marshalling w/ Properties
    //=======================================================================
    /**
     * Marshall the Token into Properties format.
     */
    static class PropertiesTokenMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null;
            marshall(new PrintWriter(wrt), "", (Token) value);
        }

        public void marshall(PrintWriter wrt, String prefix, Token value)
                throws Exception {
            wrt.print(prefix);
            wrt.print("token.id=");
            if (value != null) {
                wrt.println(value.getId());
            }
        }
    }
    
    /**
     * Marshall an Attribute object into Properties format.
     */
    static class PropertiesAttributeMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null;
            marshall(new PrintWriter(wrt), "", (Attribute) value);
        }

        public void marshall(PrintWriter wrt, String prefix, Attribute attr)
                throws Exception {
            String prfx = prefix + "attribute.";
            wrt.print(prfx);
            if (attr != null) {
                wrt.print("name=");
                wrt.println(attr.getName());
                String[] vals = attr.getValues();
                for (int i = 0; (vals != null && i < vals.length); i++) {
                    String value = vals[i];
                    wrt.print(prfx);
                    wrt.print("value=");
                    wrt.println(value);
                }
            }
        }
    }

    /**
     * Marshall a UserDetails object into Properties format.
     */
    static class PropertiesUserDetailsMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null;
            marshall(new PrintWriter(wrt), "", (UserDetails)value);
        }

        public void marshall(PrintWriter wrt, String prefix, UserDetails ud)
                throws Exception {
            String prfx = prefix + "userdetails.";
            if (ud != null) {
                // write the token..
                PropertiesTokenMarshaller tokenMarshaller =
                    new PropertiesTokenMarshaller();
                tokenMarshaller.marshall(wrt, prfx, ud.getToken());
                // write the roles..
                String[] rols = ud.getRoles();
                for (int i = 0; (rols != null && i < rols.length); i++) {
                    String v = rols[i];
                    // add prefix to denote a parent object..
                    wrt.print(prfx);
                    wrt.print("role=");
                    wrt.println(v);
                }
                // write the attributes..
                PropertiesAttributeMarshaller attrMarshaller =
                    new PropertiesAttributeMarshaller();
                Attribute[] atts = ud.getAttributes();
                for (int i = 0; (atts != null && i < atts.length); i++) {
                    Attribute attr = atts[i];
                    attrMarshaller.marshall(wrt, prfx, attr);
                }
            }
        }
    }

    /**
     * Marshall an IdentityDetails object into Properties format.
     */
    static class PropertiesIdentityDetailsMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert((wrt != null) && (value != null));
            marshall(new PrintWriter(wrt), "", (IdentityDetails)value);
        }

        public void marshall(PrintWriter wrt, String prefix,
            IdentityDetails details) throws Exception
        {
            String prfx = prefix + "identitydetails.";
            if (details != null) {
                // write the name
                wrt.print(prfx);
                wrt.print("name=");
                wrt.println(details.getName());

                // write the identity type
                wrt.print(prfx);
                wrt.print("type=");
                wrt.println(details.getType());

                // write the realm
                wrt.print(prfx);
                wrt.print("realm=");
                wrt.println(details.getRealm());

                // write the roles.
                ListWrapper roleList = details.getRoleList();
                if (roleList != null) {
                    String[] roles = roleList.getElements();
                    for (int i = 0; (roles != null && i < roles.length); i++) {
                        // add prefix to denote a parent object..
                        wrt.print(prfx);
                        wrt.print("role=");
                        wrt.println(roles[i]);
                    }
                }

                // write the groups.
                ListWrapper groupList = details.getGroupList();
                if (groupList != null) {
                    String[] groups = groupList.getElements();
                    for (int i = 0; (groups != null && i < groups.length); i++) {
                        // add prefix to denote a parent object..
                        wrt.print(prfx);
                        wrt.print("group=");
                        wrt.println(groups[i]);
                    }
                }

                // write the members.
                ListWrapper memberList = details.getMemberList();
                if (memberList != null) {
                    String[] members = memberList.getElements();
                    for (int i = 0; (members != null && i < members.length); i++) {
                        // add prefix to denote a parent object..
                        wrt.print(prfx);
                        wrt.print("member=");
                        wrt.println(members[i]);
                    }
                }

                // write the attributes..
                PropertiesAttributeMarshaller attrMarshaller =
                    new PropertiesAttributeMarshaller();
                Attribute[] atts = details.getAttributes();
                for (int i = 0; ((atts != null) && (i < atts.length)); i++) {
                    Attribute attr = atts[i];

                    wrt.print(prfx);
                    wrt.println("attribute=");
                    attrMarshaller.marshall(wrt, prfx, attr);
                }
            }
        }
    }

    /**
     * Marshall an IdentityDetails array into Properties format.
     */
    static class PropertiesIdentityDetailsArrayMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert((wrt != null) && (value != null));
            marshall(new PrintWriter(wrt), "", (IdentityDetails[])value);
        }

        public void marshall(PrintWriter wrt, String prefix,
            IdentityDetails[] value) throws Exception
        {
            String prfx = prefix + "identitydetails";

            if ((value != null) && (value.length > 0)) {
                PropertiesIdentityDetailsMarshaller detailsMarshaller =
                    new PropertiesIdentityDetailsMarshaller();

                for (int i = 0; i < value.length; i++) {
                    wrt.println(prfx + "=");
                    detailsMarshaller.marshall(wrt, prefix, value[i]);
                }
            }
        }
    }

    /**
     * Marshall an IdentityDetails object into Properties format.
     */
    static class PropertiesListMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert((wrt != null) && (value != null));
            marshall(new PrintWriter(wrt), "", (List)value);
        }

        public void marshall(PrintWriter wrt, String prefix, List value)
            throws Exception
        {
            String prfx = prefix + "list.";

            if (value != null) {
                Iterator iter = value.iterator();

                while (iter.hasNext()) {
                    Object nextObj = iter.next();
                    Marshaller mar = MarshallerFactory.PROPS.newInstance(
                        nextObj.getClass());
                    Class[] params = { XMLStreamWriter.class,
                        nextObj.getClass() };
                    Method m = mar.getClass().getMethod("marshall", params);
                    Object[] objs = { wrt, value };
                    m.invoke(mar, objs);
                }
            }
        }
    }

    /**
     * Marshall an Exception class into Properties format.
     */
    static class PropertiesGeneralFailureMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) {
            assert wrt != null && value != null;
            marshall(new PrintWriter(wrt), "", (GeneralFailure)value);
        }

        public void marshall(PrintWriter wrt, String prefix,
                             GeneralFailure value)
        {
            String msg = value.getMessage();

            wrt.print(prefix);
            wrt.print("exception.name=");

            if ((msg != null) && (msg.length() > 0)) {
                wrt.print(value.getClass().getName());
                wrt.println(" " + msg);
            } else {
                wrt.println(value.getClass().getName());
            }
        }
    }

    /**
     * Marshall the Throwable exception class into Properties format.
     */
    static class PropertiesThrowableMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
        {
            assert ((wrt != null) && (value != null));
            marshall(new PrintWriter(wrt), "", (Throwable)value);
        }

        public void marshall(PrintWriter wrt, String prefix,
                             Throwable value)
        {
            String msg = value.getMessage();

            wrt.print(prefix);
            wrt.print("exception.name=");

            if ((msg != null) && (msg.length() > 0)) {
                wrt.print(value.getClass().getName());
                wrt.println(" " + msg);
            } else {
                wrt.println(value.getClass().getName());
            }
        }
    }

    /**
     * Marshall the ObjectNotFound exception class into Properties format.
     */
    static class PropertiesObjectNotFoundMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
        {
            assert ((wrt != null) && (value != null));
            marshall(new PrintWriter(wrt), "", (ObjectNotFound)value);
        }

        public void marshall(PrintWriter wrt, String prefix,
                             ObjectNotFound value)
        {
            String msg = value.getMessage();

            wrt.print(prefix);
            wrt.print("exception.name=");

            if ((msg != null) && (msg.length() > 0)) {
                wrt.print(value.getClass().getName());
                wrt.println(" " + msg);
            } else {
                wrt.println(value.getClass().getName());
            }
        }
    }

    /**
     * Marshall the Boolean into Properties format.
     */
    static class PropertiesBooleanMarshaller implements Marshaller {
        public void marshall(Writer wrt, Object value) throws Exception {
            assert wrt != null && value != null;
            marshall(new PrintWriter(wrt), "", (Boolean) value);
        }

        public void marshall(PrintWriter wrt, String prefix, Boolean value)
                throws Exception {
            wrt.print(prefix);
            wrt.print("boolean=");
            wrt.println(value.toString());
        }
    }

    /**
     * Marshall a String into Properties format.
     */
    static class PropertiesStringMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert ((wrt != null) && (value != null));

            marshall(new PrintWriter(wrt), "", (String)value);
        }

        public void marshall(PrintWriter wrt, String prefix, String value)
            throws Exception
        {
            wrt.print(prefix);
            wrt.print("string=");
            wrt.println(value);
        }
    }

    /**
     * Marshall a String array into Properties format.
     */
    static class PropertiesStringArrayMarshaller
        implements Marshaller
    {
        public void marshall(Writer wrt, Object value)
            throws Exception
        {
            assert((wrt != null) && (value != null));

            marshall(new PrintWriter(wrt), "", (String[])value);
        }

        public void marshall(PrintWriter wrt, String prefix, String[] value)
            throws Exception
        {
            if ((value != null) && (value.length > 0)) {
                PropertiesStringMarshaller stringMarshaller =
                    new PropertiesStringMarshaller();

                for (int i = 0; i < value.length; i++) {
                    stringMarshaller.marshall(wrt, prefix, value[i]);
                }
            }
        }
    }
}
