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
 * $Id: IdentityServicesHandler.java,v 1.7 2008/12/15 19:50:21 arviranga Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.idsvcs.rest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.idsvcs.GeneralFailure;
import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.IdentityServicesImpl;
import com.sun.identity.idsvcs.IdentityServicesFactory;
import com.sun.identity.idsvcs.AccountExpired;
import com.sun.identity.idsvcs.ListWrapper;
import com.sun.identity.idsvcs.MaximumSessionReached;
import com.sun.identity.idsvcs.ObjectNotFound;
import com.sun.identity.idsvcs.OrgInactive;
import com.sun.identity.idsvcs.UserInactive;
import com.sun.identity.idsvcs.UserLocked;
import com.sun.identity.idsvcs.Token;
import com.sun.identity.idsvcs.UserDetails;
import java.io.StringWriter;

/**
 * Provides a marshall/unmarshall layer to the Security interface.
 */
public class IdentityServicesHandler extends HttpServlet {

    private static final long serialVersionUID = 2774677132209419157L;
    private static Debug debug = Debug.getInstance("amIdentityServices");

    // =======================================================================
    // Constants
    // =======================================================================
    private static final String PARAM_PROVIDER = "provider";
    private static final Class PROVIDER_DEFAULT = IdentityServicesImpl.class;

    // =======================================================================
    // Fields
    // =======================================================================
    private IdentityServicesFactory factory;
    private String lbCookieName;
    private String lbCookieValue;

    // =======================================================================
    // Initialize/Destroy
    // =======================================================================
    /**
     * Loads the init parameters for use in the HTTP methods.
     *
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException {
        super.init();
        // determine if the provider is correct..
        try {
            // get the security provider from the params..
            String def = PROVIDER_DEFAULT.toString();
            String provider = getInitParameter(PARAM_PROVIDER, def);
            this.factory = IdentityServicesFactory.getInstance(provider);
        } catch (Exception e) {
            // wrap in a servlet exception as to not scare the natives..
            throw new ServletException(e);
        }
        
        lbCookieName = AuthClientUtils.getlbCookieName();
        lbCookieValue = AuthClientUtils.getlbCookieValue();
    }

    // =======================================================================
    // HTTP Methods
    // =======================================================================
    /**
     * Determines unmarshalls the request and executes the proper method based
     * on the request parameters.
     *
     * @see javax.servlet.http.HttpServlet#service(HttpServletRequest request,
     *      HttpServletResponse response)
     */
    protected void service(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {
        //set headers before executing the method, so they are set even if exception is being thrown
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.addHeader("Pragma", "no-cache");
        IdentityServicesImpl security = this.factory.newInstance();
        SecurityMethod.execute(security, request, response);
        
        // check/set LB cookie
        setLbCookie(request, response);
    }

    // =======================================================================
    // Helper Methods
    // =======================================================================
    /**
     * Get a consistent behaviour between application servers..
     */
    String getInitParameter(String param, String def) {
        String ret = getInitParameter(param);
        if (isBlank(param)) {
            ret = def;
        }
        return ret;
    }

    private static boolean isBlank(String val)
    {
        return (val == null) ? true :
            ((val.trim().length() == 0) ? true : false);
    }
    
    private void setLbCookie(HttpServletRequest request, HttpServletResponse response) {
        if (lbCookieName == null || lbCookieValue == null) {
            return;
        }

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (int c = 0; c < cookies.length; c++) {
                if (cookies[c].getName().equals(lbCookieName)) {
                    return;
                }
            }
        }
        
        try {
            AuthClientUtils.setlbCookie(request, response);
        } catch (Exception ex) {
            // unable to set lb cookie
        }
    }
    
    /**
     * Enum to get the request parameters and test w/ the SecurityMethods.
     */
    public static class SecurityParameter {

        public static final SecurityParameter URI =
            new SecurityParameter("URI");
        public static final SecurityParameter ACTION =
            new SecurityParameter("ACTION");
        public static final SecurityParameter USERNAME =
            new SecurityParameter("USERNAME");
        public static final SecurityParameter PASSWORD =
            new SecurityParameter("PASSWORD");
        public static final SecurityParameter CLIENT =
                new SecurityParameter("CLIENT");
        public static final SecurityParameter TOKENID =
            new SecurityParameter("TOKENID", Token.class);
        public static final SecurityParameter SUBJECTID =
            new SecurityParameter("SUBJECTID", Token.class);
        public static final SecurityParameter IDENTITY =
            new SecurityParameter("IDENTITY", IdentityDetails.class);
        public static final SecurityParameter ATTRIBUTENAMES =
            new SecurityParameter("ATTRIBUTENAMES",
            (new String[1]).getClass());
        public static final SecurityParameter LOGNAME =
            new SecurityParameter("LOGNAME");
        public static final SecurityParameter MESSAGE =
            new SecurityParameter("MESSAGE");
        public static final SecurityParameter MESSAGECODE =
            new SecurityParameter("MESSAGECODE");
        public static final SecurityParameter APPID =
            new SecurityParameter("APPID", Token.class);
        public static final SecurityParameter ADMIN =
            new SecurityParameter("ADMIN", Token.class);
        public static final SecurityParameter NAME =
            new SecurityParameter("NAME");
        public static final SecurityParameter FILTER =
            new SecurityParameter("FILTER");
        public static final SecurityParameter ATTRIBUTES =
            new SecurityParameter("ATTRIBUTES", Attribute[].class);
        public static final SecurityParameter REFRESH =
                new SecurityParameter("REFRESH", Boolean.class);
        // ===================================================================
        // Fields
        // ===================================================================
        final Class type;
        final String name;

        SecurityParameter(String name) {
            this.name = name;
            this.type = String.class;
        }

        SecurityParameter(String name, Class type) {
            this.name = name;
            this.type = type;
        }

        String name() {
            return name;
        }

        Object getValue(ServletRequest request) {
            Object ret = null;
            if (this.type == Token.class) {
                ret = getToken(request);
            } else if (this.type == List.class) {
                ret = getList(request);
            } else if (this.type == String[].class) {
                ret = getArray(request);
            } else if (this.type == Attribute[].class) {
                ret = getAttributeArray(request);
            } else if (this.type == IdentityDetails.class) {
                ret = getIdentityDetails(request);
            } else if (type == Boolean.class) {
                ret = getBoolean(request);
            } else {
                ret = getString(request);
            }
            return ret;
        }

        public Boolean getBoolean(ServletRequest request) {
            String name = name().toLowerCase();
            return Boolean.valueOf(request.getParameter(name));
        }

        public String getString(ServletRequest request) {
            String name = name().toLowerCase();
            String ret = request.getParameter(name);
            if (isBlank(ret)) {
                ret = null;
            }
            return ret;
        }

        public Token getToken(ServletRequest request) {
            Token ret = null;
            String n = name().toLowerCase();
            String id = request.getParameter(n);
            if (isBlank(id)) {
                try {
                    // Check the cookie value "iPlanetDirectoryPro"
                    SSOTokenManager mgr = SSOTokenManager.getInstance();
                    SSOToken token = mgr.createSSOToken(
                        (HttpServletRequest) request);
                    if (token != null) {
                        id = token.getTokenID().toString();
                    }
                } catch (SSOException ex) {
                    // Ignore the exception, and no valid token
                }
            }
            if (!isBlank(id)) {
                ret = new Token();
                ret.setId(id);
            }
            return ret;
        }

        public List getList(ServletRequest request) {
            List ret = null;
            String n = name().toLowerCase();
            String[] values = request.getParameterValues(n);
            if (values != null) {
                ret = new ArrayList();
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (!isBlank(value)) {
                        ret.add(value);
                    }
                }
            }
            return ret;
        }

        public String[] getArray(ServletRequest request)
        {
            String[] ret = null;
            List valuesList = getList(request);

            if ((valuesList != null) && (valuesList.size() > 0)) {
                ret = new String[valuesList.size()];

                valuesList.toArray(ret);
            }

            return ret;
        }

        public Attribute[] getAttributeArray(ServletRequest request)
        {
            Attribute[] ret = null;
            List attributeList = null;
            String n = name().toLowerCase();
            String[] attrNames = request.getParameterValues(n + "_names");

            if (attrNames != null) {
                for (int i = 0; i < attrNames.length; i++) {
                    String attrName = attrNames[i];

                    if (isBlank(attrName)) {
                        break;
                    }

                    String attrValues[] =
                        request.getParameterValues(n + "_values_" + attrName);

                    if ((attrValues != null) && (attrValues.length > 0)) {
                        List attrValueList = new ArrayList();

                        for (int j = 0; j < attrValues.length; j++) {
                            String attrValue = attrValues[j];

                            if (!isBlank(attrValue)) {
                                attrValueList.add(attrValue);
                            }
                        }

                        String[] attrValuesArray =
                            new String[attrValueList.size()];

                        attrValueList.toArray(attrValuesArray);

                        Attribute attribute = new Attribute();
                        attribute.setName(attrName);
                        attribute.setValues(attrValuesArray);

                        if (attributeList == null) {
                            attributeList = new ArrayList();
                        }

                        attributeList.add(attribute);
                    } else {
                        // Add empyt attribute
                        Attribute attribute = new Attribute();
                        attribute.setName(attrName);
                        if (attributeList == null) {
                            attributeList = new ArrayList();
                        }
                        attributeList.add(attribute);
                    }
                }
            }

            if ((attributeList != null) && (attributeList.size() > 0)) {
                ret = new Attribute[attributeList.size()];
                attributeList.toArray(ret);
            }

            return ret;
        }

        public IdentityDetails getIdentityDetails(ServletRequest request)
        {
            IdentityDetails rv = null;
            String n = name().toLowerCase();
            String identityName = request.getParameter(n + "_name");

            if (!isBlank(identityName)) {
                rv = new IdentityDetails();
                rv.setName(identityName);

                String objType = request.getParameter(n + "_type");
                if (!isBlank(objType)) {
                    rv.setType(objType);
                }

                String realm = request.getParameter(n + "_realm");
                if (!isBlank(realm)) {
                    rv.setRealm(realm);
                }

                String[] roles = request.getParameterValues(n + "_roles");
                if (roles != null) {
                    List rolesList = new ArrayList();

                    for (int i = 0; i < roles.length; i++) {
                        String role = roles[i];

                        if (!isBlank(role)) {
                            rolesList.add(role);
                        }
                    }

                    String[] rolesArray = new String[rolesList.size()];

                    rolesList.toArray(rolesArray);
                    rv.setRoleList(new ListWrapper(rolesArray));
                }

                String[] groups = request.getParameterValues(n + "_groups");
                if (groups != null) {
                    List groupsList = new ArrayList();

                    for (int i = 0; i < groups.length; i++) {
                        String group = groups[i];

                        if (!isBlank(group)) {
                            groupsList.add(group);
                        }
                    }

                    String[] groupsArray = new String[groupsList.size()];

                    groupsList.toArray(groupsArray);
                    rv.setGroupList(new ListWrapper(groupsArray));
                }

                String[] members = request.getParameterValues(n + "_members");
                if (members != null) {
                    List membersList = new ArrayList();

                    for (int i = 0; i < members.length; i++) {
                        String member = members[i];

                        if (!isBlank(member)) {
                            membersList.add(member);
                        }
                    }

                    String[] membersArray = new String[membersList.size()];

                    membersList.toArray(membersArray);
                    rv.setMemberList(new ListWrapper(membersArray));
                }

                List attrList = new ArrayList();
                String[] attrNames =
                        request.getParameterValues(n + "_attribute_names");

                if (attrNames != null) {
                    for (int i = 0; i < attrNames.length; i++) {
                        String attrName = attrNames[i];

                        if (isBlank(attrName)) {
                            break;
                        }

                        Attribute attribute = null;
                        String attrValues[] =
                            request.getParameterValues(n + "_attribute_values_" + attrName);

                        if (attrValues != null) {
                            List attrValueList = new ArrayList();

                            for (int j = 0; j < attrValues.length; j++) {
                                String attrValue = attrValues[j];

                                if (!isBlank(attrValue)) {
                                    attrValueList.add(attrValue);
                                }
                            }

                            String[] attrValuesArray =
                                new String[attrValueList.size()];

                            attrValueList.toArray(attrValuesArray);
                            attribute = new Attribute();
                            attribute.setName(attrName);
                            attribute.setValues(attrValuesArray);
                            attrList.add(attribute);
                        } else {
                            attribute = new Attribute();
                            attribute.setName(attrName);
                            attrList.add(attribute);
                        }

                        if (attrList.size() > 0) {
                            Attribute[] attrArray = new Attribute[attrList.size()];

                            attrList.toArray(attrArray);
                            rv.setAttributes(attrArray);
                        }
                    }
                }
            }

            return rv;
        }
    }

   /**
     * Defined by the interface 'com.sun.identity.idsvcs.Security'.
     */
    public static class SecurityMethod {

        public static final SecurityMethod AUTHENTICATE = new SecurityMethod(
            "AUTHENTICATE", Token.class, new SecurityParameter[]{
            SecurityParameter.USERNAME, SecurityParameter.PASSWORD,
            SecurityParameter.URI, SecurityParameter.CLIENT});
        public static final SecurityMethod ISTOKENVALID = new SecurityMethod(
            "ISTOKENVALID", Boolean.class, SecurityParameter.TOKENID);
        public static final SecurityMethod TOKENCOOKIE = new SecurityMethod(
            "GETCOOKIENAMEFORTOKEN", String.class, (SecurityParameter[]) null);
        public static final SecurityMethod ALLCOOKIES = new SecurityMethod(
            "GETCOOKIENAMESTOFORWARD", String[].class, (SecurityParameter[]) null);
        public static final SecurityMethod LOGOUT = new SecurityMethod(
                "LOGOUT", Void.class, SecurityParameter.SUBJECTID);
        public static final SecurityMethod AUTHORIZE = new SecurityMethod(
            "AUTHORIZE", Boolean.class, SecurityParameter.URI,
            SecurityParameter.ACTION, SecurityParameter.SUBJECTID);
        public static final SecurityMethod ATTRIBUTES = new SecurityMethod(
            "ATTRIBUTES", UserDetails.class, SecurityParameter.ATTRIBUTENAMES,
            SecurityParameter.SUBJECTID, SecurityParameter.REFRESH);
        public static final SecurityMethod LOG = new SecurityMethod(
                "LOG", Void.class, new SecurityParameter[]
                {SecurityParameter.APPID, SecurityParameter.SUBJECTID,
                 SecurityParameter.LOGNAME, SecurityParameter.MESSAGE});
        public static final SecurityMethod SEARCH =	new SecurityMethod(
            "SEARCH", String[].class, new SecurityParameter[]
            {SecurityParameter.FILTER, SecurityParameter.ATTRIBUTES,
             SecurityParameter.ADMIN});
        public static final SecurityMethod CREATE =   new SecurityMethod(
                "CREATE", Void.class, new SecurityParameter[]
                {SecurityParameter.IDENTITY, SecurityParameter.ADMIN});
        public static final SecurityMethod READ = new SecurityMethod(
            "READ", IdentityDetails.class, new SecurityParameter[]
            {SecurityParameter.NAME, SecurityParameter.ATTRIBUTES,
             SecurityParameter.ADMIN});
        public static final SecurityMethod UPDATE =   new SecurityMethod(
                "UPDATE", Void.class, new SecurityParameter[]
                {SecurityParameter.IDENTITY, SecurityParameter.ADMIN});
        public static final SecurityMethod DELETE =   new SecurityMethod(
                "DELETE", Void.class, new SecurityParameter[]
                {SecurityParameter.IDENTITY, SecurityParameter.ADMIN});

        // ===================================================================
        // Constructors
        // ===================================================================
        private SecurityMethod(String name, Class clazz,
            SecurityParameter[] params) {
            
            final Method[] SECURITY_METHODS =
                IdentityServicesImpl.class.getMethods();
            // find the method
            Method imethod = null;
            String lname = name.toLowerCase();
            for (int i = 0; i < SECURITY_METHODS.length; i++) {
                Method m = SECURITY_METHODS[i];
                // found the method by name..
                String mname = m.getName().toLowerCase();
                if (mname.equals(lname)) {
                    // lets check based on parameters..
                    imethod = m;
                    break;
                }
            }
            // need to throw if we can't find it..
            if (imethod == null) {
                throw new IllegalArgumentException();
            }
            // set the internal fields
            this.type = clazz;
            this.method = imethod;
            this.parameters = params;
        }

        private SecurityMethod(String name, Class clazz,
            SecurityParameter param1) {
            this(name, clazz, new SecurityParameter[]{param1});
        }

        private SecurityMethod(String name, Class clazz,
            SecurityParameter param1, SecurityParameter param2) {
            this(name, clazz, new SecurityParameter[]{param1, param2});
        }

        private SecurityMethod(String name, Class clazz,
            SecurityParameter param1, SecurityParameter param2,
            SecurityParameter param3) {
            this(name, clazz, new SecurityParameter[]{param1, param2, param3});
        }

        // ===================================================================
        // Fields
        // ===================================================================
        final Class type;
        final Method method;
        final SecurityParameter[] parameters;

        public static void execute(IdentityServicesImpl security,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            
            // find the security method from the path..
            response.setCharacterEncoding("UTF-8");
            Writer wrt = response.getWriter();
            StringWriter sw = null;
            String path = request.getPathInfo();
            MarshallerFactory mar = getMarshaller(path);
            // Set the respone content type
            if (mar.getProtocol().equalsIgnoreCase("XML")) {
                response.setContentType("text/xml");
            } else if (mar.getProtocol().equals("JSON")) {
                response.setContentType("application/json");
            } else {
                response.setContentType("text/plain");
            }
            path = path.substring(path.lastIndexOf('/') + 1).toUpperCase();
            SecurityMethod method = null;
            if (path.equals("AUTHENTICATE")) {
                method = SecurityMethod.AUTHENTICATE;
            } else if (path.equals("LOGOUT")) {
                method = SecurityMethod.LOGOUT;
            } else if (path.equals("AUTHORIZE")) {
                method = SecurityMethod.AUTHORIZE;
            } else if (path.equals("ATTRIBUTES")) {
                method = SecurityMethod.ATTRIBUTES;
            } else if (path.equals("LOG")) {
                method = SecurityMethod.LOG;
            } else if (path.equals("SEARCH")) {
                method = SecurityMethod.SEARCH;
            } else if (path.equals("CREATE")) {
                method = SecurityMethod.CREATE;
            } else if (path.equals("READ")) {
                method = SecurityMethod.READ;
            } else if (path.equals("UPDATE")) {
                method = SecurityMethod.UPDATE;
            } else if (path.equals("DELETE")) {
                method = SecurityMethod.DELETE;
            } else if (path.equals("ISTOKENVALID")) {
                method = SecurityMethod.ISTOKENVALID;
            } else if (path.equals("GETCOOKIENAMEFORTOKEN")) {
                method = SecurityMethod.TOKENCOOKIE;
            } else if (path.equals("GETCOOKIENAMESTOFORWARD")) {
                method = SecurityMethod.ALLCOOKIES;
            }
            
            try {
                if (method == null) {
                    // Throw Unsupported Operation Exception
                    response.setStatus(501);
                    mar.newInstance(Throwable.class).marshall(wrt,
                        new UnsupportedOperationException(path));
                    return;
                }
                // execute the method w/ the parameters..
                Object value = method.invoke(security, request);
                // marshall the response..
                if (method.type != Void.class && value != null) {
                    mar.newInstance(method.type).marshall(wrt, value);
                } else {
                    response.setContentType("text/plain");
                    if (value == null) {
                        wrt.write("NULL");
                    }
                }
            } catch (ObjectNotFound ex) {
                // write out the proper ObjectNotFound exception.
                // set the response error code
                try {
                    mar.newInstance(ObjectNotFound.class).marshall(wrt, ex);
                    response.setStatus(401);
                } catch (Exception e) {
                    // something really went wrong so just give up..
                    throw new ServletException(e);
                }
            } catch (GeneralFailure ex) {
                // write out the proper security based exception..
                try {
                    mar.newInstance(GeneralFailure.class).marshall(wrt, ex);
                    response.setStatus(500);
                } catch (Exception e) {
                    // something really went wrong so just give up..
                    throw new ServletException(e);
                }
            } catch (Throwable e) {
                try {
                    // something really went wrong so just give up..
                    mar.newInstance(Throwable.class).marshall(wrt, e);
                    if (e instanceof UnsupportedOperationException) {
                        response.setStatus(501);
                    } else if (e instanceof OrgInactive || e instanceof UserLocked
                            || e instanceof UserInactive || e instanceof AccountExpired
                            || e instanceof MaximumSessionReached){
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    } else if (e instanceof InvocationTargetException) {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } catch (Exception ex) {
                    throw new ServletException(ex);
                }
            } finally {
                if (sw != null) {
                    sw.close();
                }
            }
        }

        /**
         * If both exist on the path then return JSON, XML, and then Properites
         * in that order.
         */
        private static MarshallerFactory getMarshaller(String path) {
            // default is properties format
            boolean xml = path.indexOf("xml/") != -1;
            boolean json = path.indexOf("json/") != -1;
            return (json) ? MarshallerFactory.JSON :
                (xml) ? MarshallerFactory.XML : MarshallerFactory.PROPS;
        }

        private Object invoke(IdentityServicesImpl security,
                              ServletRequest request)
            throws Throwable
        {
            // find the value for each parameter..
            Object[] params = null;
            if (parameters != null) {
                params = new Object[this.parameters.length];
                for (int i = 0; i < this.parameters.length; i++) {
                    SecurityParameter param = this.parameters[i];
                    params[i] = param.getValue(request);
                }
            }
            
            Object ret = null;
            try {
                // Special case for authentication.
                // If parameters are null and if already authenitcated
                // i.e., iPlanetDirectoryPro cookie is present, send the
                // SSOToken
                if ((method == SecurityMethod.AUTHENTICATE.method) &&
                    (params != null) &&  (params.length > 1) &&
                    (params[0] == null) && (params[1] == null)) {
                    // username & password is null for the authenticate method
                    // Check for iPlanetDirectoryPro cookie
                    try {
                        SSOTokenManager mgr = SSOTokenManager.getInstance();
                        SSOToken token = mgr.createSSOToken(
                            (HttpServletRequest) request);
                        if (mgr.isValidToken(token)) {
                            // Contruct Token object
                            Token t = new Token();
                            t.setId(token.getTokenID().toString());
                            ret = t;
                        }
                    } catch (SSOException ssoe) {
                        // SSOToken not present, ignore the exception
                        ret = method.invoke(security, params);
                    }
                } else {
                    // invoke the actual security param..
                    ret = method.invoke(security, params);
                }
            } catch (IllegalArgumentException e) {
                throw new GeneralFailure(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new GeneralFailure(e.getMessage());
            } catch (InvocationTargetException e) {
                if (debug.warningEnabled()) {
                    debug.warning("Exception during invocation", e);
                }
                
                throw (e.getTargetException());
            }

            return ret;
        }
    }
}
