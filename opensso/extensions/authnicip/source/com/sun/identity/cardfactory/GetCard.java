/* The contents of this file are subject to the terms
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
 * $Id: GetCard.java,v 1.3 2008/04/28 21:11:22 superpat7 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cardfactory;

import java.io.*;
import java.net.*;
import java.rmi.server.UID;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Properties;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xmldap.exceptions.KeyStoreException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.infocard.InfoCard;
import org.xmldap.infocard.TokenServiceReference;
import org.xmldap.infocard.policy.SupportedClaim;
import org.xmldap.infocard.policy.SupportedClaimList;
import org.xmldap.infocard.policy.SupportedToken;
import org.xmldap.infocard.policy.SupportedTokenList;
import org.xmldap.sts.db.SupportedClaims;
import org.xmldap.util.Base64;
import org.xmldap.util.KeystoreUtil;
import org.xmldap.util.XSDDateTime;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.Constants;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.wss.sts.STSConstants;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import java.security.MessageDigest;

public class GetCard extends HttpServlet {

    private X509Certificate cert = null;
    private PrivateKey privateKey = null;
    private SupportedClaims supportedClaimsImpl = null;
    public static Debug debug = Debug.getInstance("famGetCard");
    
    //private String serverName; 
    //private String endpointPath; 

    @Override
    public void init(ServletConfig config) throws ServletException {

        debug.message("In GetCard:init");
        
        super.init(config);

        ServletContext context = config.getServletContext();
        /*
        Properties properties = new Properties();

        try {
            properties.load(context.getResourceAsStream(config.getInitParameter("cardfactory.propertiesfile")));
        } catch (IOException ex) {
            throw new ServletException(ex);
        }

        // initialize variables
        String keystorePath = properties.getProperty("cardfactory.keystore.path");
        String keystorePassword = properties.getProperty("cardfactory.keystore.password");
        String key = properties.getProperty("cardfactory.key.name");
        String keyPassword = properties.getProperty("cardfactory.key.password");

        serverName = properties.getProperty("cardfactory.servername");
        endpointPath = properties.getProperty("cardfactory.endpointpath");
        */
        
        //String supportedClaimsClass = properties.getProperty("supportedClaimsClass");
        String supportedClaimsClass = SystemConfigurationUtil.getProperty(
                    CardSpaceConstants.supportedClaimsClass,
                    CardSpaceConstants.defaultSupportedClaimsClass);
        try {
            supportedClaimsImpl = SupportedClaims.getInstance(supportedClaimsClass);
        } catch (InstantiationException ex) {
            throw new ServletException(ex);
        } catch (IllegalAccessException ex) {
            throw new ServletException(ex);
        } catch (ClassNotFoundException ex) {
            debug.error("The configured claims class is not supported");
            throw new ServletException("The configured claims class is not supported");
        }
        String wsdlLocation = "./WEB-INF/wsdl/famsts.wsdl";

        // load the WSDL from file into a DOM
        DocumentBuilder db;
        Document wsdl;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            db = dbf.newDocumentBuilder();
            wsdl = db.parse(context.getResourceAsStream(wsdlLocation));
        } catch (SAXException ex) {
            debug.error("WSDL file not properly formated.");
            throw new ServletException("WSDL file not properly formated.");
        } catch (IOException ex) {
            debug.error("WSDL file location not properly configured.");
            throw new ServletException("WSDL file location not properly configured.");
        } catch (ParserConfigurationException ex) {
            debug.error("XML Parser Configuration error for WSDL");
            throw new ServletException("XML Parser Configuration error for WSDL");
        }
        
        // crypto setup
        try {
            String kprovider = SystemConfigurationUtil.getProperty(
                    SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                    SAMLConstants.JKS_KEY_PROVIDER);
            KeyProvider keystore = (KeyProvider) Class.forName(kprovider).newInstance();

            String certAlias = SystemConfigurationUtil.getProperty(
                    Constants.SAML_XMLSIG_CERT_ALIAS);
            
            debug.message("certAlias  : " + certAlias);
            
            cert = keystore.getX509Certificate(certAlias);
            if (cert == null) {
                debug.message("cert is null");
                throw new ServletException("cert is null");
            }
            privateKey = keystore.getPrivateKey(certAlias);
            if (privateKey == null) {
                debug.message("privateKey is null");
                throw new ServletException("privateKey is null");
            }
        } catch (Exception e) {
            debug.error("Error in keystore setup : " ,e);
            throw new ServletException(e);
        }
        debug.message("CERT : " + cert);
        debug.message("Private Key : " + privateKey);
        
        /* KeystoreUtil keystore;
        try {
            keystore = new KeystoreUtil(context.getRealPath(keystorePath), keystorePassword);
            privateKey = keystore.getPrivateKey(key, keyPassword);
            if (privateKey == null) {
                throw new ServletException("privateKey is null");
            }
            cert = keystore.getCertificate(key);
            if (cert == null) {
                throw new ServletException("cert is null");
            }
        } catch (KeyStoreException ex) {
            throw new ServletException(ex);
        }*/


        // configure the WSDL
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        Node nodeCert;
        //Node nodeEpr;
        //Node nodeSoapAddress;
        //Node nodeWsdlDefs;
        //Node nodeSchema;

        try {
            xp.setNamespaceContext(new CardFactoryNamespaceContext());
            nodeCert = (Node) xp.evaluate("//ds:X509Certificate", wsdl, XPathConstants.NODE);
            //nodeEpr = (Node) xp.evaluate("//wsa:Address", wsdl, XPathConstants.NODE);
            //nodeSoapAddress = (Node) xp.evaluate("//soap12:address", wsdl, XPathConstants.NODE);
            //nodeWsdlDefs = (Node) xp.evaluate("//wsdl:definitions", wsdl, XPathConstants.NODE);
            //nodeSchema = (Node) xp.evaluate("//xsd:schema", wsdl, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            throw new ServletException(ex);
        }

        // encode the cert into base64 and put it in the WSDL
        try {
            ((Text) nodeCert.getFirstChild()).replaceWholeText(Base64.encodeBytes(cert.getEncoded()));
        } catch (CertificateEncodingException ex) {
            throw new ServletException(ex);
        }

        /*
        // set the namespace attributes
        ((Element) nodeWsdlDefs).removeAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "tns");
        ((Element) nodeWsdlDefs).removeAttribute("targetNamespace");
        ((Element) nodeSchema).removeAttribute("targetNamespace");

        ((Element) nodeWsdlDefs).setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:tns", "http://" + serverName + endpointPath);
        ((Element) nodeWsdlDefs).setAttribute("targetNamespace", "http://" + serverName + endpointPath);
        ((Element) nodeSchema).setAttribute("targetNameSpace", "http://" + serverName + "/schemas");

        // set the WS-A EPR for the service
        ((Text) ((Element) nodeEpr).getFirstChild()).replaceWholeText("http://" + serverName + endpointPath);
        ((Element) nodeSoapAddress).removeAttribute("location"); 
        ((Element) nodeSoapAddress).setAttribute("location", "http://" + serverName + endpointPath);
        */
        
        try {
            TransformerFactory tff = TransformerFactory.newInstance();
            Transformer trans = tff.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes"); 
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); 
            trans.setOutputProperty(OutputKeys.METHOD, "xml");
            trans.setOutputProperty(OutputKeys.STANDALONE, "yes"); 
            trans.setOutputProperty(OutputKeys.VERSION, "1.1"); 
            
            trans.transform(new DOMSource(wsdl), new StreamResult(new FileOutputStream(context.getRealPath(wsdlLocation)))); 
        } catch (TransformerException ex) {
            debug.error("TransformerException : " ,ex);
            throw new ServletException(ex);
        } catch (FileNotFoundException ex) {
            throw new ServletException(ex);
        } 
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        debug.message("In GetCard:processRequest");
        PrintWriter out = response.getWriter();
        //response.setContentType("text/html;charset=UTF-8");
        
        // Check for FAM session validity, else redirect to FAM login page
        SessionID sessionID = new SessionID(request);
        debug.message("sessionID : " + sessionID);
        
        SSOToken ssoToken = null;
        String serverProtocol = 
                SystemConfigurationUtil.getProperty(Constants.AM_SERVER_PROTOCOL);
        String serverHost = 
                SystemConfigurationUtil.getProperty(Constants.AM_SERVER_HOST);
        String serverPort = 
                SystemConfigurationUtil.getProperty(Constants.AM_SERVER_PORT);
        String serviceUri = 
                SystemConfigurationUtil.getProperty(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        String famLoginUrl = 
                serverProtocol + "://" + serverHost + ":" + serverPort + serviceUri + 
                "/UI/Login?goto=" + URLEncDec.encode( serverProtocol + "://" + serverHost + ":" + 
                serverPort + serviceUri + "/GetCard" + "?" +
                request.getQueryString());
        debug.message("famLoginUrl : " + famLoginUrl);
        
        try {
            if (sessionID != null) {
                String sidString = sessionID.toString();
                SSOTokenManager manager = SSOTokenManager.getInstance();
                ssoToken = manager.createSSOToken(sidString);
                if (!manager.isValidToken(ssoToken)) {
                    debug.message("NO valid token, redirecting to FAM login...");
                    response.sendRedirect(famLoginUrl);
                    return;
                }
            } else {
                debug.message("sessionID is NULL, redirecting to FAM login...");
                response.sendRedirect(famLoginUrl);
                return;
            }
        } catch (Exception e) {
            debug.message("Error in retrieving / validating FAM SSOToken : " 
                    + e.toString());
            debug.message("Redirecting to FAM login...");
            response.sendRedirect(famLoginUrl);
            return;
        }
        
        String userName = "TestUser";
        String userDN = null;
        try {
            if (ssoToken != null) {
                debug.message("Valid SSOToken : " + ssoToken.getTokenID());
                debug.message("Valid User DN from SSOToken : " 
                        + ssoToken.getPrincipal().getName());
                userDN = ssoToken.getProperty("Principal");
                userName = ssoToken.getProperty("UserId");
            }
        } catch (Exception e) {
            debug.error("Error in retrieving SSOToken properties : " 
                    + e.toString());            
        }
        debug.message("Valid userDN : " + userDN);
        debug.message("Valid userName : " + userName);
        //String endpointURL = "https://" + serverHost + endpointPath;
        String endpointURL = null;
        String mexEndpointURL = null;
        try {
            URL stsService = 
                    WebtopNaming.getServiceURL(
                    "sts", serverProtocol, serverHost, serverPort, serviceUri);
            endpointURL = stsService.toString();
            URL stsMexService = 
                    WebtopNaming.getServiceURL(
                    "sts-mex", serverProtocol, serverHost, serverPort, serviceUri);
            mexEndpointURL = stsMexService.toString();
        } catch (Exception e) {
            debug.error("Error in retrieving STS service URL : " + e.toString());            
        }
        debug.message("endpointURL : " + endpointURL);
        debug.message("mexEndpointURL : " + mexEndpointURL);
        
        // Retrieve User attributes from AMIdentity
        // Attribute names :
        // "uid", "givenname", "sn", "cn", "userPassword", "mail", "employeenumber",
        // "postaladdress", "locality", "stateorprovince", "postalcode", "country",
        // "telephonenumber", "otherphone", "mobilephone", "dateofbirth", "gender",
        // "privatepersonalidentifier", "webpage".
        String uid = null;
        String givenname = null;
        String sn = null;
        String cn = null;
        String mail = null;
        String postaladdress = null;
        String locality = null;
        String stateorprovince = null;
        String postalcode = null;
        String country = null;
        String homephone = null;
        String otherphone = null;
        String mobilephone = null;
        String dateofbirth = null;
        String gender = null;
        String privatepersonalidentifier = null;
        String webpage = null;
        
        try {
            AMIdentity amid = IdUtils.getIdentity(ssoToken);
            Map attrs = amid.getAttributes();
            debug.message("User Attributes: ");

            for (Iterator i = attrs.keySet().iterator(); i.hasNext(); ) {
                String attrName = (String)i.next();
                Set values = (Set)attrs.get(attrName);
                debug.message(attrName + "=" + values);
            }
            uid = (String) ((Set)attrs.get("uid")).iterator().next();
            givenname = (String) ((Set)attrs.get("givenname")).iterator().next();
            sn = (String) ((Set)attrs.get("sn")).iterator().next();
            cn = (String) ((Set)attrs.get("cn")).iterator().next();
            mail = (String) ((Set)attrs.get("mail")).iterator().next();
            postaladdress = (String) ((Set)attrs.get("postaladdress")).iterator().next();
            locality = (String) ((Set)attrs.get("locality")).iterator().next();
            stateorprovince = (String) ((Set)attrs.get("stateorprovince")).iterator().next();
            postalcode = (String) ((Set)attrs.get("postalcode")).iterator().next();
            country = (String) ((Set)attrs.get("country")).iterator().next();
            homephone = (String) ((Set)attrs.get("telephonenumber")).iterator().next();
            otherphone = (String) ((Set)attrs.get("otherphone")).iterator().next();
            mobilephone = (String) ((Set)attrs.get("mobilephone")).iterator().next();
            dateofbirth = (String) ((Set)attrs.get("dateofbirth")).iterator().next();
            gender = (String) ((Set)attrs.get("gender")).iterator().next();
            privatepersonalidentifier = (String) ((Set)attrs.get("privatepersonalidentifier")).iterator().next();
            webpage = (String) ((Set)attrs.get("webpage")).iterator().next();
            
        } catch (Exception e) {
            debug.error("Error in retrieving User attributes from AMIdentity : " 
                    + e.toString());            
        }        
        debug.message("given name : " + givenname);
        debug.message("last name : " + sn);
        debug.message("email : " + mail);
        debug.message("privatepersonalidentifier : " + privatepersonalidentifier);
        
        UID cardGuid = new UID ();   
        
        
        InfoCard card = new InfoCard(cert, privateKey); 
        
        card.setCardId(endpointURL + "/cardID/" + cardGuid.toString());
        card.setCardName("OpenSSO Test Card for " + userName);
        card.setCardVersion(1);
        
        card.setIssuerName (serverHost);
        card.setIssuer(endpointURL);
        
        if ((request.getParameter("requireAppliesTo") != null) &&
                ((request.getParameter("requireAppliesTo")).length() != 0) &&
                (request.getParameter("requireAppliesTo").equals("true"))) {
            card.setRequireAppliesTo(true); 
        } else {
            card.setRequireAppliesTo(false);
        }
        
        card.setSignCard(true);
        card.setUserName(userName);
                        
        XSDDateTime issued = new XSDDateTime();
        card.setTimeIssued(issued.getDateTime());
        
        // at this time we only support username password - this should change asap
        //TokenServiceReference tsr = new TokenServiceReference (endpointURL, endpointURL + "?wsdl", cert);
        String authMethod = request.getParameter("authMethod");
        debug.message("authMethod : " + authMethod);
        TokenServiceReference tsr = new TokenServiceReference (endpointURL, mexEndpointURL, cert);
        if ((authMethod == null) || (authMethod.length() == 0) || (authMethod.equals("username"))) {
           // username password authentication
           tsr.setAuthType (TokenServiceReference.USERNAME, userName);
        } else if (authMethod.equals("x509")) {
           // X.509 certificate authentication
           X509Certificate[] allCerts = 
                   (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
           cert = allCerts[0];
           debug.message("cert : " + cert);
           byte[] thumbPrintIdentifier = null;

           try {
               thumbPrintIdentifier = MessageDigest.getInstance("SHA-1").digest(cert.getEncoded());
           } catch ( Exception ex ) {
               throw new RuntimeException("Problem creating the digest from the cert.");
           }
           BASE64Encoder enc = new BASE64Encoder();
           tsr.setAuthType(TokenServiceReference.X509, enc.encode(thumbPrintIdentifier));
        }
        card.setTokenServiceReference(tsr);
        
        // Add SAML11 or SAML20 token
        SupportedTokenList tokenList = new SupportedTokenList();
        if (request.getSession().getServletContext().getInitParameter("com.sun.identity.cardfactory.GetCard.tokenTypes") !=null) {
           StringTokenizer tokenTypes = new StringTokenizer(request.getSession().getServletContext().getInitParameter("com.sun.identity.cardfactory.GetCard.tokenTypes"), ",");
           debug.message("tokenTypes : " + tokenTypes);
           while (tokenTypes.hasMoreTokens()) {
               String strToken = tokenTypes.nextToken().trim();
               if(strToken.equals("SAML1") || strToken.equals("SAML11") || strToken.equals("SAML1.1")) {
                   tokenList.addSupportedToken(new SupportedToken(SupportedToken.SAML11));
                   tokenList.addSupportedToken(new SupportedToken(SupportedToken.WSS_SAML11));
               } else if (strToken.equals("SAML2") || strToken.equals("SAML20") || strToken.equals("SAML2.0")) {
                   SupportedToken token = new SupportedToken(SupportedToken.SAML20);
                   tokenList.addSupportedToken(token);
               }
           }
        } else {
           // Do SAML 1.1 by default
           SupportedToken token = new SupportedToken(SupportedToken.SAML11);
           tokenList.addSupportedToken(token);
        }
        card.setTokenList(tokenList); 
                               
        //populate the supported claims list for the card
        SupportedClaimList claimList = new SupportedClaimList();
        //We - essentially - always need a PPID claim
        claimList.addSupportedClaim(CardSpaceConstants.ClaimType.PPID.getSupportedClaim());
        //check if there this has been configured
        if (request.getSession().getServletContext().getInitParameter("com.sun.identity.cardfactory.GetCard.supportedClaims") != null) {
           StringTokenizer supportedClaims = 
                   new StringTokenizer(request.getSession().getServletContext().getInitParameter("com.sun.identity.cardfactory.GetCard.supportedClaims"), ",");
           while (supportedClaims.hasMoreTokens()) {
               String claim = supportedClaims.nextToken().trim();
               // We should come up with a better way of describing the claim type - this should be ok for now
               CardSpaceConstants.ClaimType type = CardSpaceConstants.ClaimType.claimTypeFromUri(claim);
               if (type != null) {
                   claimList.addSupportedClaim(type.getSupportedClaim());
               } else {
                   claimList.addSupportedClaim(new SupportedClaim(claim.substring(claim.lastIndexOf("/")),
                       claim,
                       claim.substring(claim.lastIndexOf("/"))));
               }
           }
        } else {
           // default claims - just in case
           claimList.addSupportedClaim(CardSpaceConstants.ClaimType.GivenName.getSupportedClaim());
           claimList.addSupportedClaim(CardSpaceConstants.ClaimType.LastName.getSupportedClaim());
           claimList.addSupportedClaim(CardSpaceConstants.ClaimType.EmailAddress.getSupportedClaim());
        }
        // TODO: we need to get a couple of supported claims (i.e. attributes) from OpenSSO here
        card.setClaimList(claimList);
        
        // add our own card picture
        if ( request.getSession().getServletContext().getInitParameter("com.sun.identity.cardfactory.GetCard.image") != null) {
            FileInputStream fisImage = new FileInputStream(request.getSession().getServletContext().getInitParameter("com.sun.identity.cardfactory.GetCard.image"));
            BufferedInputStream bis = new BufferedInputStream(fisImage);
           
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
           
            BASE64Encoder enc = new BASE64Encoder();
            enc.encode(bis, baos);
           
            card.setBase64BinaryCardImage(baos.toString());           
        }
        
        //PrintWriter out = response.getWriter();
        response.setContentType("application/x-mscardfile; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename="+userName+".crd");

        try {
            debug.message("******** CARD ********* : " + card.toXML());
            out.println(card.toXML());
        } catch (SerializationException ex) {
            throw new ServletException(ex);
        }

        out.flush();
        out.close();
        return;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     */
    @Override
    public String getServletInfo() {
        return "InfoCard Managed Card Factory Servlet";
    }
    // </editor-fold>
}

// <editor-fold defaultstate="collapsed" desc="CardFactoryNameSpaceContext helper class">
class CardFactoryNamespaceContext implements NamespaceContext {

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new NullPointerException("Null prefix");
        } else if ("wsdl".equals(prefix)) {
            return CardSpaceConstants.WSDL_NS_URI;
        } else if ("ds".equals(prefix)) {
            return CardSpaceConstants.XML_DSIG_NS_URI;
        } else if ("wsa".equals(prefix)) {
            return CardSpaceConstants.WS_ADRESSING_08_2005_NS_URI;
        } else if ("soap12".equals(prefix)) {
            return CardSpaceConstants.SOAP_12_NS_URI;
        } else if ("xsd".equals(prefix)) {
            return XMLConstants.W3C_XML_SCHEMA_NS_URI;
        } else if ("xml".equals(prefix)) {
            return XMLConstants.XML_NS_URI;
        }
        return XMLConstants.NULL_NS_URI;
    }

    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }
}
// </editor-fold>