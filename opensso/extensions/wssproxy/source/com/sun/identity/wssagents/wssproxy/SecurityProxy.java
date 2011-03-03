/*
 * SecurityProxy.java
 *
 * Created on July 23, 2007, 3:59 PM
 */

package com.sun.identity.wssagents.wssproxy;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.wss.security.handler.SOAPRequestHandler;
import com.sun.identity.wss.security.SecurityException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 * This Proxy secures web service messages by using Federated
 * Access Manager Web Service Security API.
 */
public class SecurityProxy extends HttpServlet {
    private static Debug debug = Debug.getInstance("WSProxy");
    private static final String URL_PARAM_WSDL = "wsdl";
    private static final String ATTR_WSS_PROXY_ENDPOINT = "WSPProxyEndpoint";
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request Servlet request.
     * @param response Servlet response.
     * @throws ServletException if unable to process request.
     * @throws IOException if IO operation fails.
     */
    protected void processRequest(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        String contentType = request.getContentType();
        String wsdl = request.getParameter(URL_PARAM_WSDL);

        if (wsdl != null) {
            String agentId = getAgentId(request);
            processWSDLRequest(agentId, request, response);
        } else if ((contentType != null) &&
            (contentType.indexOf("text/xml") != -1)
        ) {
            String agentId = getAgentId(request);
            processWSRequest(agentId, request, response);
        } else {
            response.setContentType("text/html; charset=\"utf-8\"");
            PrintWriter out = response.getWriter();
            out.println("Web Security Proxy Application.");
            out.flush();
            out.close();
        }
    }

    // this method is called by doGet and doPost
    private void processWSRequest(
        String agentId,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        try {
            String wsrequest = getProxyEndPoint("/", agentId);
            String uri = request.getRequestURI();
            int idx = uri.lastIndexOf('/');
            wsrequest += uri.substring(idx);
        
            String agentType = getAgentType("/", agentId);

            boolean isWSC = (agentType != null) && 
                agentType.equalsIgnoreCase("WSCAgent");
            String soapMessage = getRequestInput(request);
            String soapMessageEx = isWSC ?
                secureRequest(agentId, soapMessage) :
                validateRequest(agentId, soapMessage);

            String result = postRequest(wsrequest, soapMessageEx);
            String resultEx = isWSC ? validateResponse(agentId, result) :
               secureResponse(agentId, result);
            response.setContentType("text/xml; charset=\"utf-8\"");
            PrintWriter out = response.getWriter();

            out.print(resultEx);
            out.flush();
            out.close();
        } catch (SOAPException e) {
            debug.error("SecurityProxy.processWSRequest: " + stack2string(e));
       } catch (SecurityException e) {
            debug.error("SecurityProxy.processWSRequest: " + stack2string(e));
        }
    }

    private SOAPRequestHandler getSOAPRequestHandler(String agentId)
        throws SecurityException {
        SOAPRequestHandler handler = new SOAPRequestHandler();
        HashMap params = new HashMap();

        params.put("providername", agentId);
        handler.init(params);
        return handler;
    }

    private SOAPMessage getSOAPMessage(String soapMessage)
        throws IOException, SOAPException {
        MimeHeaders mimeHeader = new MimeHeaders();
        mimeHeader.addHeader("Content-Type", "text/xml");
        MessageFactory msgFactory = MessageFactory.newInstance();
        return msgFactory.createMessage(mimeHeader,
            new ByteArrayInputStream(soapMessage.toString().getBytes()));
    }

    private String soapToString(SOAPMessage message)
        throws IOException, SOAPException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        return baos.toString();
    }

    private String validateRequest(String agentId, String soapMessage)
        throws IOException, SOAPException, SecurityException {
        SOAPMessage message = getSOAPMessage(soapMessage);
        SOAPRequestHandler handler = getSOAPRequestHandler(agentId);
        handler.validateRequest(message, new Subject(), Collections.EMPTY_MAP,
            null, null);
        return soapToString(message);
    }
    
    private String validateResponse(String agentId, String soapMessage)
        throws IOException, SOAPException, SecurityException {
        SOAPMessage message = getSOAPMessage(soapMessage);
        SOAPRequestHandler handler = getSOAPRequestHandler(agentId);
        handler.validateResponse(message, Collections.EMPTY_MAP);
        return soapToString(message);
    }

    private String secureRequest(String agentId, String soapMessage)
        throws IOException, SOAPException, SecurityException {
        SOAPMessage message = getSOAPMessage(soapMessage);
        SOAPRequestHandler handler = getSOAPRequestHandler(agentId);
        SOAPMessage encMessage = handler.secureRequest(
            message, new Subject(), Collections.EMPTY_MAP);
        return soapToString(message);
   }

    private String secureResponse(String agentId, String soapMessage)
        throws IOException, SOAPException, SecurityException {
        SOAPMessage message = getSOAPMessage(soapMessage);
        SOAPRequestHandler handler = getSOAPRequestHandler(agentId);
        SOAPMessage encMessage = handler.secureResponse(
            message, Collections.EMPTY_MAP);
        return soapToString(message);
   }

    private String getWSResponse(HttpURLConnection conn)
        throws IOException {
        BufferedReader d = null;
        StringBuffer buff = new StringBuffer();

        try {
            d = new BufferedReader(new InputStreamReader(
                conn.getInputStream()));
            String line;
            while ((line = d.readLine()) != null) {
                buff.append(line).append("\n");
            }
        } finally {
            if (d != null) {
                d.close();
            }
        }
        return buff.toString();
    }

    private String postRequest(String strURL, String content)
        throws IOException {
        String response = null;
        URL url = new URL(strURL);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setDoOutput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
            "text/xml; charset=\"utf-8\"");
        byte[] data = content.getBytes("UTF-8");
        conn.setRequestProperty("Content-Length",
            Integer.toString(data.length));
        OutputStream os = null;
        try {
            os = conn.getOutputStream();
            os.write(data);
            os.flush();
            response = getWSResponse(conn);
        } catch (ConnectException e) {
            debug.error("SecurityProxy.processWSRequest: " + stack2string(e));
        } finally {
            if (os != null) {
                os.close();
            }
        }
        return response;
    }

    private String getRequestInput(HttpServletRequest request)
        throws IOException {
        BufferedReader d = null;
        StringBuffer buff = new StringBuffer();

        try {
            d = new BufferedReader(new InputStreamReader(
                request.getInputStream()));
            String str;
            while ((str = d.readLine()) != null) {
                buff.append(str).append("\n");
            }
        } finally {
            if (d != null) {
                d.close();
            }
        }
        return buff.toString();
    }

    private void processWSDLRequest(
        String agentId,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        String wssEndPoint = getProxyEndPoint("/", agentId);
        String reqURL = request.getRequestURL().toString();
        int idx = reqURL.lastIndexOf('/');
        String wsdl = wssEndPoint + reqURL.substring(idx) + "?wsdl";
        response.setContentType("text/xml; charset=\"utf-8\"");
        PrintWriter out = response.getWriter();
        BufferedReader d = null;
        try {
            URL url = new URL(wsdl);
            URLConnection conn = url.openConnection();
            d = new BufferedReader(new InputStreamReader(
                conn.getInputStream()));
            StringBuffer buff = new StringBuffer();
            String str;
            while ((str = d.readLine()) != null) {
                buff.append(str).append("\n");
            }
            out.println(alterPortAddress(request, buff.toString()));
        } finally {
            if (d != null) {
                d.close();
            }

            out.flush();
            out.close();
        }
    }

    private String getAgentId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int idx = uri.lastIndexOf('/');
        String id = uri.substring(uri.lastIndexOf('/', idx-1) +1, idx);
        if (id.endsWith("req")) {
            id = id.substring(0, id.length()-3);
        }
        return id;
    }

    private String alterPortAddress(HttpServletRequest request, String buff) {
        int idx = buff.indexOf("<soap:address ");
        if (idx != -1) {
            idx = buff.indexOf("location", idx);
            int quote = buff.indexOf("\"", idx);
            int endQuote = buff.indexOf("\"", quote+1);

            String origSOAPEndPoint = buff.substring(quote+1, endQuote);
            String url = request.getRequestURL().toString();
            int index = url.lastIndexOf('/');
            buff = buff.substring(0, quote+1) + url + buff.substring(endQuote);
        }
        return buff;
    }

   private String getAgentType(String realm, String name) {
        try {
            AMIdentity amid = getAgentEntity(realm, name);
            Set setType = amid.getAttribute(IdConstants.AGENT_TYPE);
            return ((setType != null) && !setType.isEmpty()) ? 
                (String)setType.iterator().next() : "";
        } catch (IdRepoException e) {
            debug.error("SecurityProxy.getAgentType", e);
        } catch (SSOException e) {
            debug.error("SecurityProxy.getAgentType", e);
        }
        return "";
    }
    
    private String getProxyEndPoint(String realm, String name) {
        AMIdentity amid = getAgentEntity(realm, name);
        String endPoint = null;
        
        if (amid != null) {
            try {
                Set setValues = amid.getAttribute(ATTR_WSS_PROXY_ENDPOINT);
                if ((setValues != null) && !setValues.isEmpty()) {
                    endPoint = (String)setValues.iterator().next();
                }
            } catch (SSOException e) {
                debug.error("SecurityProxy.getProxyEndPoint", e);
            } catch (IdRepoException e) {
                debug.error("SecurityProxy.getProxyEndPoint", e);
            }
        }
        return endPoint;
    }
    
    private AMIdentity getAgentEntity(String realm, String name) {
        AMIdentity amid = null;
        try {
            AMIdentityRepository idRepo = new AMIdentityRepository(
                getAdminToken(), realm);
            IdSearchResults results = idRepo.searchIdentities(IdType.AGENT,
                name, new IdSearchControl());
            Set agents = results.getSearchResults();
            if (!agents.isEmpty()) {
                amid = (AMIdentity)agents.iterator().next();
            }
        } catch (IdRepoException e) {
            debug.error("SecurityProxy.getAgentEntity", e);
        } catch (SSOException e) {
            debug.error("SecurityProxy.getAgentEntity", e);
        }
        return amid;
    }

    private static SSOToken getAdminToken() {
        SSOToken adminToken = null;
        try {
            adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            if (adminToken != null) {
                SSOTokenManager.getInstance().refreshSession(adminToken);
            }
        } catch (SSOException se) {
            debug.message("SecurityProxy.getAdminToken: Retry");
            adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        }
        return adminToken;
    }
    
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request Servlet request
     * @param response Servlet response
     * @throws ServletException if unable to process request.
     * @throws IOException if IO operations failed.
     */
    protected void doGet(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request Servlet request
     * @param response Servlet response
     * @throws ServletException if unable to process request.
     * @throws IOException if IO operations failed.
     */
    protected void doPost(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /**
     * Returns a short description of the servlet.
     * 
     * @return a short description of the servlet.
     */
    public String getServletInfo() {
        return "Web Service Security Proxy";
    }
    
    private static String stack2string(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e2) {
            e.printStackTrace();
            return "";
        }
    }
}
