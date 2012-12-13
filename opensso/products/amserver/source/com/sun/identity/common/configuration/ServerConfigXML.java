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
 * $Id: ServerConfigXML.java,v 1.3 2009/08/24 21:04:20 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.common.configuration;

import com.iplanet.services.ldap.DSConfigMgr;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Server Configuration XML object.
 */
public class ServerConfigXML implements Serializable {
    private ServerGroup defaultServerGroup;
    private ServerGroup smsServerGroup;
    
    /**
     * Creates an object representing server configuration XML.
     *
     * @param xml the server configuration XML.
     * @throws Exception if the XML is invalid.
     */
    public ServerConfigXML(String xml)
        throws Exception
    {
        Document doc = XMLUtils.getXMLDocument(
            new ByteArrayInputStream(xml.getBytes("UTF-8")));
        Node root = XMLUtils.getRootNode(doc,
            DSConfigMgr.ROOT);
        
        defaultServerGroup = new ServerGroup(XMLUtils.getNamedChildNode(root,
            DSConfigMgr.SERVERGROUP, DSConfigMgr.NAME, DSConfigMgr.DEFAULT));
        smsServerGroup = new ServerGroup(XMLUtils.getNamedChildNode(root,
            DSConfigMgr.SERVERGROUP, DSConfigMgr.NAME, "sms"));
    }
    
    /**
     * Returns the default server group.
     *
     * @return the default server group.
     */
    public ServerGroup getDefaultServerGroup() {
        return defaultServerGroup;
    }

    /**
     * Returns the SMS server group.
     *
     * @return the SMS server group.
     */
    public ServerGroup getSMSServerGroup() {
        return smsServerGroup;
    }

    /**
     * Returns the XML representation of this object.
     *
     * @return the XML representation of this object.
     */
    public String toXML() {
        StringBuilder buff = new StringBuilder();
        buff.append("<").append(DSConfigMgr.ROOT).append(">\n");
        buff.append(defaultServerGroup.toXML(DSConfigMgr.DEFAULT));
        buff.append(smsServerGroup.toXML("sms"));
        buff.append("</").append( DSConfigMgr.ROOT).append(">\n");
        return buff.toString();
    }
    
    
    /**
     * Server Group Object.
     */
    public class ServerGroup implements Serializable {
        public int minPool;
        public int maxPool;
        public List hosts = new ArrayList();
        public List dsUsers = new ArrayList();
        public String dsBaseDN;

        /**
         * Constructs a Server Group object.
         *
         * @param node XML node for Server Group blob.
         */
        public ServerGroup(Node node) {
            Element elm = (Element)node;
            minPool = Integer.parseInt(elm.getAttribute(
                DSConfigMgr.MIN_CONN_POOL));
            maxPool = Integer.parseInt(elm.getAttribute(
                DSConfigMgr.MAX_CONN_POOL));
        
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeName().equalsIgnoreCase(DSConfigMgr.SERVER)) {
                    hosts.add(new ServerObject(n));
                } else if (n.getNodeName().equals(DSConfigMgr.USER)) {
                    dsUsers.add(new DirUserObject(n));
                } else if (n.getNodeName().equals(DSConfigMgr.BASE_DN)) {
                    dsBaseDN = ((Text)n.getFirstChild()).getNodeValue();
                }
            }
        }
        
        /**
         * Adds a host to the group.
         *
         * @param name Server name.
         * @param host Host name.
         * @param port Port number.
         * @param type Connection type i.e. Simple/SSL.
         */
        public void addHost(String name, String host, String port, String type)
            throws ConfigurationException {
            for (Iterator i = hosts.iterator(); i.hasNext(); ) {
                ServerObject test = (ServerObject)i.next();
                if (test.name.equals(name)) {
                    String[] param = {name};
                    throw new ConfigurationException(
                        "exception.serverconfig.xml.server.name.exist", param);
                }
            }
            try {
                Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                String[] param = {port};
                throw new ConfigurationException(
                    "exception.serverconfig.xml.port.number.no.integer", param);
            }
            
            ServerObject obj = new ServerObject();
            obj.name = name;
            obj.host = host;
            obj.port = port;
            obj.type = type;
            hosts.add(obj);
        }
        
        /**
         * Returns the XML representation of this object.
         *
         * @param groupName Server Group Name.
         * @return the XML representation of this object.
         */
        public String toXML(String groupName) {
            StringBuilder buff = new StringBuilder();
            buff.append("<").append(DSConfigMgr.SERVERGROUP)
                .append(" ")
                .append(DSConfigMgr.NAME).append("=\"")
                .append(groupName).append("\"")
                .append(" ")
                .append(DSConfigMgr.MIN_CONN_POOL).append("=\"")
                .append(Integer.toString(minPool)).append("\"")
                .append(" ")
                .append(DSConfigMgr.MAX_CONN_POOL).append("=\"")
                .append(Integer.toString(maxPool)).append("\"")
                .append(">\n");

            for (Iterator i = hosts.iterator(); i.hasNext(); ) {
                ServerObject s = (ServerObject)i.next();
                buff.append(s.toXML());
            }

            for (Iterator i = dsUsers.iterator(); i.hasNext(); ) {
                DirUserObject s = (DirUserObject)i.next();
                buff.append(s.toXML());
            }

            buff.append("<").append(DSConfigMgr.BASE_DN).append(">");
            buff.append(dsBaseDN);
            buff.append("</").append(DSConfigMgr.BASE_DN).append(">\n");

            buff.append("</").append(DSConfigMgr.SERVERGROUP).append(">\n");
            return buff.toString();
        }
    }
    
    /**
     * Server object.
     */
    public class ServerObject implements Serializable {
        public String name;
        public String host;
        public String port;
        public String type;

        /**
         * Constructs a Server object.
         */
        public ServerObject() {
        }
        
        /**
         * Constructs a Server object.
         *
         * @param node XML node for directory user blob.
         */
        public ServerObject(Node node) {
            Element elm = (Element)node;
            name = elm.getAttribute(DSConfigMgr.NAME);
            host = elm.getAttribute(DSConfigMgr.HOST);
            port = elm.getAttribute(DSConfigMgr.PORT);
            type = elm.getAttribute(DSConfigMgr.AUTH_TYPE);
        }
        
        /**
         * Returns the XML representation of this object.
         *
         * @return the XML representation of this object.
         */
        public String toXML() {
            StringBuilder buff = new StringBuilder();
            buff.append("<").append(DSConfigMgr.SERVER)
                .append(" ")
                .append(DSConfigMgr.NAME).append("=\"")
                .append(name).append("\"")
                .append(" ")
                .append(DSConfigMgr.HOST).append("=\"")
                .append(host).append("\"")
                .append(" ")
                .append(DSConfigMgr.PORT).append("=\"")
                .append(port).append("\"")
                .append(" ")
                .append(DSConfigMgr.AUTH_TYPE).append("=\"")
                .append(type).append("\"")
                .append(" />\n");
            return buff.toString();
        }
    }


    /**
     * Directory User object.
     */
    public class DirUserObject implements Serializable {
        public String name;
        public String type;
        public String dn;
        public String password;

        /**
         * Constructs a directory User object.
         *
         * @param node XML node for directory user blob.
         */
        public DirUserObject(Node node) {
            Element elm = (Element)node;
            name = elm.getAttribute(DSConfigMgr.NAME);
            type = elm.getAttribute(DSConfigMgr.AUTH_TYPE);
            
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeName().equalsIgnoreCase(DSConfigMgr.AUTH_ID)) {
                    dn = ((Text)n.getFirstChild()).getNodeValue();
                } else if (n.getNodeName().equalsIgnoreCase(
                    DSConfigMgr.AUTH_PASSWD)
                ) {
                    password = ((Text)n.getFirstChild()).getNodeValue();
                }
            }
        }

        /**
         * Returns the XML representation of this object.
         *
         * @return the XML representation of this object.
         */
        public String toXML() {
            StringBuilder buff = new StringBuilder();
            buff.append("<").append(DSConfigMgr.USER)
                .append(" ")
                .append(DSConfigMgr.NAME).append("=\"")
                .append(name).append("\"")
                .append(" ")
                .append(DSConfigMgr.AUTH_TYPE).append("=\"")
                .append(type).append("\"")
                .append(">\n");

            buff.append("<").append(DSConfigMgr.AUTH_ID).append(">");
            buff.append(dn);
            buff.append("</").append(DSConfigMgr.AUTH_ID).append(">\n");
            buff.append("<").append(DSConfigMgr.AUTH_PASSWD).append(">");
            buff.append(password);
            buff.append("</").append(DSConfigMgr.AUTH_PASSWD).append(">\n");

            buff.append("</" + DSConfigMgr.USER + ">\n");
            return buff.toString();
        }
    }
}
