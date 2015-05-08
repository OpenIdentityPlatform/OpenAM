/*
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
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */

package com.sun.identity.idsvcs.rest;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.Token;
import org.json.JSONArray;
import org.json.JSONObject;

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
    private Map<Class<?>, Class<?>> _map = new HashMap<>();
    
    private String protocol;

    private MarshallerFactory(String protocol) {
        this.protocol = protocol;
        // No support for JSON yet
        if (protocol.equals("JSON")) {
            _map.put(Token.class, JSONTokenMarshaller.class);
            _map.put(Boolean.class, JSONBooleanMarshaller.class);
            _map.put(String.class, JSONStringMarshaller.class);
            _map.put(String[].class, JSONStringArrayMarshaller.class);
            _map.put(Throwable.class, JSONThrowableMarshaller.class);
            _map.put(GeneralFailure.class, JSONThrowableMarshaller.class);
            _map.put(Throwable.class, JSONThrowableMarshaller.class);
            _map.put(GeneralFailure.class, JSONThrowableMarshaller.class);
        } else if (protocol.equals("XML")) {
            _map.put(Token.class, XMLTokenMarshaller.class);
            _map.put(Boolean.class, XMLBooleanMarshaller.class);
            _map.put(String.class, XMLStringMarshaller.class);
            _map.put(String[].class, XMLStringArrayMarshaller.class);
            _map.put(List.class, PropertiesListMarshaller.class);
            _map.put(GeneralFailure.class, XMLGeneralFailureMarshaller.class);
            _map.put(Throwable.class, XMLThrowableMarshaller.class);
        } else if (protocol.equals("PROPS"))  {
            _map.put(Token.class, PropertiesTokenMarshaller.class);
            _map.put(Boolean.class, PropertiesBooleanMarshaller.class);
            _map.put(String.class, PropertiesStringMarshaller.class);
            _map.put(String[].class, PropertiesStringArrayMarshaller.class);
            _map.put(List.class, PropertiesListMarshaller.class);
            _map.put(GeneralFailure.class, PropertiesGeneralFailureMarshaller.class);
            _map.put(Throwable.class, PropertiesThrowableMarshaller.class);
        }
    }
    
    /**
     * Returns the Marshaller Map for the mechanism
     */
    Map<Class<?>, Class<?>> getMarshallerMap() {
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
