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
 * $Id: AMCRLStore.java,v 1.7 2009/01/28 05:35:12 ww203982 Exp $
 *
 */

package com.sun.identity.security.cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSet;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPUrl;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.GeneralNames;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X509CertImpl;

import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.DistributionPoint;

import com.iplanet.security.x509.IssuingDistributionPointExtension;
import com.iplanet.security.x509.X500Name;
//import com.iplanet.am.util.AMURLEncDec;
import com.sun.identity.shared.encode.URLEncDec;

import com.sun.identity.common.HttpURLConnectionManager;
import org.apache.commons.lang.ArrayUtils;

/**
* The class is used to manage crl store in LDAP server
* This class does get crl and update crl with CRLDistribution
* PointsExtension in client certificate or IssuingDistribution 
* PointExtension in CRL. This class should be used 
* in order to manage CRL store in LDAP
* id-ce-cRLDistributionPoints OBJECT IDENTIFIER ::=  { id-ce 31 }
*
* RLDistributionPoints ::= SEQUENCE SIZE (1..MAX) OF DistributionPoint
*
* DistributionPoint ::= SEQUENCE {
*        distributionPoint       [0]     DistributionPointName OPTIONAL,
*        reasons                 [1]     ReasonFlags OPTIONAL,
*        cRLIssuer               [2]     GeneralNames OPTIONAL }
*
* DistributionPointName ::= CHOICE {
*        fullName                [0]     GeneralNames,
*        nameRelativeToCRLIssuer [1]     RelativeDistinguishedName }
*
* ReasonFlags ::= BIT STRING {
*        unused                  (0),
*        keyCompromise           (1),
*        cACompromise            (2),
*        affiliationChanged      (3),
*        superseded              (4),
*        cessationOfOperation    (5),
*        certificateHold         (6),
*        privilegeWithdrawn      (7),
*        aACompromise            (8) }
* 
*/

public class AMCRLStore extends AMCertStore {

    // In memory CRL cache
    private static Hashtable cachedcrls = new Hashtable();

    private String mCrlAttrName = null;

    /**
     * Class AMCRLStore is special cased CRL store for LDAP.
     * A AMCRLStore instance has to have all the information for ldap 
     * and all the access information for CRLDistributionPointExtension and
     * CRLIssuingDistributionPoint Extension
     *
     * @param param
     */
    public AMCRLStore(AMLDAPCertStoreParameters param) {
	super(param);
    }

    /**
     * Checks certificate and returns corresponding stored CRL in ldap store
     * @param certificate
     */
    public X509CRL getCRL(X509Certificate certificate) throws IOException  {
        LDAPEntry crlEntry = null;
        X509CRL crl = (X509CRL) getCRLFromCache(certificate);
    	LDAPConnection ldc = getConnection();

        try {
	    if (crl == null) {
                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore.getCRL: crl is null");
                }
		crlEntry = getLdapEntry(ldc);
		crl = getCRLFromEntry(crlEntry);
	    }

            if (needCRLUpdate(crl)) {
	        if (debug.messageEnabled()) {
                    debug.message("AMCRLStore.getCRL: need CRL update");
                }

	    	X509CRL tmpcrl = null;
	    	IssuingDistributionPointExtension crlIDPExt = null;
                try {
                    if (crl != null) {
                        crlIDPExt = getCRLIDPExt(crl);
                    }
                } catch (Exception e) {
                    debug.message("AMCRLStore.getCRL: crlIDPExt is null");
                }
                
	        CRLDistributionPointsExtension crlDPExt = null;
                try {
                    crlDPExt = getCRLDPExt(certificate);
                } catch (Exception e) {
                    debug.message("AMCRLStore.getCRL: crlDPExt is null");
                }

                if ((tmpcrl == null) && (crlIDPExt != null)) {
	    	    tmpcrl = getUpdateCRLFromCrlIDP(crlIDPExt);
		}
			    
		if ((tmpcrl == null) && (crlDPExt != null)) {
		    tmpcrl = getUpdateCRLFromCrlDP(crlDPExt);
		}

		if (tmpcrl != null) {
		    if (crlEntry == null) {
		       	crlEntry = getLdapEntry(ldc);
		    }

                    if (debug.messageEnabled()) {
                        debug.message("AMCRLStore.getCRL: new crl = " + tmpcrl);
                    }

                    if (crlEntry != null) {
                        updateCRL(ldc, crlEntry.getDN().toString(), 
                            tmpcrl.getEncoded());
                    }
		}
	        crl = tmpcrl;
 	    }
		    
	    updateCRLCache(certificate, crl);
        } catch (Exception e) {
            debug.error("AMCRLStore.getCRL: Error in getting CRL : ", e);
        } finally {
            if ((ldc != null) && (ldc.isConnected())) {
                try {
                    ldc.disconnect();
                } catch (LDAPException ex) {
                    //ignored
                }
            }
        }
        	
        return crl;
    }

    /**
     * Checks certificate and returns corresponding stored CRL 
     * in cached CRL store
     * @param certificate
     */
    public X509CRL getCRLFromCache(X509Certificate certificate) 
                                          throws IOException  {
	X500Name issuerDN = getIssuerDN(certificate);
	 
	X509CRL crl = null;
	
	crl = (X509CRL) cachedcrls.get(issuerDN.toString());
	
	return crl;
    }

    /**
     * Checks certificate and update CRL in cached CRL store
     * @param certificate
     */
    public void updateCRLCache(X509Certificate certificate, X509CRL crl) 
                                         throws IOException  {
	X500Name issuerDN = getIssuerDN(certificate);
		
	if (crl == null) {
	    cachedcrls.remove(issuerDN.toString());
	} else {
	    cachedcrls.put(issuerDN.toString(), crl);
	}
    }

        
    private X509CRL getCRLFromEntry(LDAPEntry crlEntry) 
        throws Exception  {

         if (debug.messageEnabled()) {
             debug.message("AMCRLStore.getCRLFromEntry:");
         }

         if (crlEntry == null) {
             return null;
         }
         
         LDAPAttributeSet attributeSet = crlEntry.getAttributeSet();
         LDAPAttribute crlAttribute = null;
         X509CRL crl = null;
         
         try {
            /*
             * Retrieve the certificate revocation list if available.
             */
                                                                                
	    if (mCrlAttrName == null) {
                crlAttribute = 
                        attributeSet.getAttribute("certificaterevocationlist");
                if (crlAttribute == null) {
                    crlAttribute = attributeSet.getAttribute(
                        "certificaterevocationlist;binary");
                    if (crlAttribute == null) {
                        debug.error("No CRL Cache is configured");
                        return null;
                    }
                }

	 	mCrlAttrName = crlAttribute.getName();
	    } else {
		crlAttribute = attributeSet.getAttribute(mCrlAttrName);
            }
                 
            if (crlAttribute.size() > 1) {
                debug.error("More than one CRL entries are configured");
                return null;
            }
        } catch (Exception e) {
            debug.error("Error in getting Cached CRL");
            return null;
        }

        try {
            Enumeration Crls = crlAttribute.getByteValues();
            byte[] bytes = (byte []) Crls.nextElement();
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getCRLFromEntry: crl size = " +
                              bytes.length);
            }
            cf = CertificateFactory.getInstance("X.509");
            crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            debug.error("Certificate: CertRevoked = ", e);
        }

        return crl;
    }

    /**
     * It checks whether the certificate has CRLDistributionPointsExtension
     * or not. If there is, it returns the extension.
     * @param X509Certificate certificate
     */
    private CRLDistributionPointsExtension 
        getCRLDPExt(X509Certificate certificate){
    	CRLDistributionPointsExtension dpExt = null;
	CertificateExtensions exts = null;
	
	try {
	    X509CertImpl certImpl = new X509CertImpl(certificate.getEncoded());
            dpExt = certImpl.getCRLDistributionPointsExtension();
	} catch (Exception e) {
            debug.error("Error finding CRL distribution Point configured: ", e);	
	}

        return dpExt;
    }
    
            
    /**
     * It checks whether the crl has IssuingDistributionPointExtension
     * or not. If there is, it returns the extension.
     * @param X509CRL crl
     */
    private IssuingDistributionPointExtension getCRLIDPExt(X509CRL crl){
        IssuingDistributionPointExtension idpExt = null;
        
        if (crl == null) {
            return null;
        }
            
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLIDPExt: crl = " + crl);
        }
        try {
            byte[] ext = 
              crl.getExtensionValue(
                      PKIXExtensions.IssuingDistributionPoint_Id.toString());
            if (ext != null) {
              idpExt = new IssuingDistributionPointExtension(ext);
            }
        } catch (Exception e) {
            debug.error("Error finding CRL distribution Point configured: ", e);
        }   

        return idpExt;
    }
            
    /**
     * It updates CRL under the dn in the directory server.
     * It retrieves CRL distribution points from the parameter 
     * CRLDistributionPointsExtension dpExt.
     * @param CRLDistributionPointsExtension dpExt
     */
    private synchronized X509CRL 
        getUpdateCRLFromCrlDP(CRLDistributionPointsExtension dpExt) {
	// Get CRL Distribution points
        if (dpExt == null) {
            return null;
        }

        List dps = null;
        try {
            dps = (List)dpExt.get(CRLDistributionPointsExtension.POINTS);
        } catch (IOException ioex) {
            if (debug.warningEnabled()) {
                debug.warning("AMCRLStore.getUpdateCRLFromCrlDP: ", ioex);
            }
        }

        if (dps == null || dps.isEmpty()) {
            return null;
        }

        for (Iterator iter = dps.iterator(); iter.hasNext(); ) {
            DistributionPoint dp = (DistributionPoint)iter.next();
            GeneralNames gName = dp.getFullName();
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getUpdateCRLFromCrlDP: DP = " +
                              gName); 
            }

	    byte[] Crls = getCRLsFromGeneralNames(gName);
            if (Crls != null && Crls.length > 0) {
                try {
	            return (X509CRL)cf.generateCRL(
                                             new ByteArrayInputStream(Crls));	
                } catch (Exception ex) {
                    if (debug.warningEnabled()) {
		        debug.warning("AMCRLStore.getUpdateCRLFromCrlDP: " +
                                      "Error in generating X509CRL", ex);
                    }
	        }
            }
        }    

        return null;
    }        

    /**
     * It updates CRL under the dn in the directory server.
     * It retrieves CRL distribution points from the parameter 
     * CRLDistributionPointsExtension dpExt.
     * @param LDAPConnection ldc
     * @param String dn
     * @param CRLDistributionPointsExtension dpExt
     */
    private synchronized X509CRL 
            getUpdateCRLFromCrlIDP(IssuingDistributionPointExtension idpExt) {

	GeneralNames gName = idpExt.getFullName();
        if (gName == null) {
            return null;
        }

        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getUpdateCRLFromCrlIDP: gName = "+ gName);
        }
        byte[] Crls = getCRLsFromGeneralNames(gName);
	        
	X509CRL crl = null;
	if (Crls != null) {
	    try {
	        crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(Crls));	
	    } catch (Exception e) {
		debug.error("Error in generating X509CRL" + e.toString());
	    }
	}
	                                
	return crl;
    }        

    private byte[] getCRLsFromGeneralNames(GeneralNames gName) {
    	byte[] Crls = null;
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLsFromGeneralNames: gNames.size = " +
                          gName.size());
        }
	int idx = 0;
	do {
	    String uri = gName.get(idx++).toString().trim();
	    String protocol = uri.toLowerCase();
	    int proto_pos;
	    if((proto_pos = protocol.indexOf("http")) == -1) {
	        if((proto_pos = protocol.indexOf("https")) == -1) {
	            if((proto_pos = protocol.indexOf("ldap")) == -1) {
		        if((proto_pos = protocol.indexOf("ldaps")) == -1) {
		            continue;
			}
		    }
		}
	    }
	
	    uri = uri.substring(proto_pos, uri.length());
	    if (debug.messageEnabled()) {
	        debug.message("DP Name : " + uri);
	    }
	    Crls = getCRLByURI(uri);
	} while ((Crls != null) && (idx < gName.size())); 

	return Crls;
    }
    
    /**
     * It replaces attribute value under the DN.
     * It is used to replace old CRL with new one.
     * @param LDAPConnection ldc
     * @param String dn
     * @param LDAPAttribute attr
     */
    private boolean updateCRL(LDAPConnection ldc, String dn, byte[] crls) {
	LDAPAttribute crlAttribute = new LDAPAttribute(mCrlAttrName, crls);
	LDAPModification mod =
		new LDAPModification(LDAPModification.REPLACE, crlAttribute);
	try {
            ldc.modify(dn, mod);
	} catch (LDAPException e){
            debug.error("Error updating CRL Cache : ", e);
            return false;
	}

        return true;
    }        
    
    /**
     * It is checking uri's protocol.
     * Protocol has to be http(s) or ldap.
     * Based on checked protocol, it gets new CRL by invking 
     * getCRLByLdapURI() or getCRLByHttpURI()
     * @param String uri
     */
    private byte[] getCRLByURI(String uri) {
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLByURI : uri = " + uri);
        }
    	if (uri == null) {
            return null;
    	}
            	
    	String protocol = uri.trim().toLowerCase();
        if (protocol.startsWith("http") || protocol.startsWith("https")) {
            return getCRLByHttpURI(uri);
        } else if (protocol.startsWith("ldap") 
                   || protocol.startsWith("ldaps")) {
            return getCRLByLdapURI(uri);
        }
        
        return null;
    }        

    /**
     * It gets the new CRL from ldap server. 
     * If it is ldap URI, the URI has to be a dn that can be accessed 
     * with ldap anonymous bind. 
     * (example : ldap://server:port/uid=ca,o=company.com) 
     * This dn entry has to have CRL in attribute certificaterevocationlist 
     * or certificaterevocationlist;binary.
     * 
     * @param String uri
     */
    private byte[] getCRLByLdapURI(String uri){

        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.getCRLByLdapURI: uri = " + uri);
        }

        LDAPUrl url = null;
        LDAPConnection ldc = null;
        byte[] crl = null;

        try {
            url = new LDAPUrl(uri);
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getCRLByLdapURI: url.dn = " +
                              url.getDN());
            }
            // Check ldap over SSL
            if (url.isSecure()) {
                ldc = new LDAPConnection(storeParam.getSecureSocketFactory());
            } else { // non-ssl
                ldc = new LDAPConnection();
            }

            ldc.connect(url.getHost(), url.getPort(), "", "");
            LDAPSearchResults results = 
            ldc.search(url.getDN().toString(),LDAPConnection.SCOPE_BASE,
                    null,null,false);

            if (results == null || results.hasMoreElements() == false) {
                debug.error("verifyCertificate - " + 
                    "No CRL distribution Point configured");
                return null;
            }

            LDAPEntry entry = results.next();
            LDAPAttributeSet attributeSet = entry.getAttributeSet();

            /* 
            * Retrieve the certificate revocation list if available.
            */
            LDAPAttribute crlAttribute = 
                        attributeSet.getAttribute("certificaterevocationlist");
            if (crlAttribute == null) {
                crlAttribute = 
                  attributeSet.getAttribute("certificaterevocationlist;binary");
                if (crlAttribute == null) {
		    debug.error("verifyCertificate - " + 
                        "No CRL distribution Point configured");
                    return null;
                }
            }
            
            crl = (byte[])crlAttribute.getByteValues().nextElement();

        } catch (Exception e) {
            debug.error("getCRLByLdapURI : Error in getting CRL",e);
        } finally {
            if ((ldc != null) && (ldc.isConnected())) {
                try {
                    ldc.disconnect();
                } catch(LDAPException ex) {
                    //ignored
                }
            }
        }

        return crl;
    }
            
    private byte[] getCRLByHttpURI(String url){
        String argString = "";  //default
        StringBuffer params = null;
        HttpURLConnection con = null;
        byte[] crl = null;

	String uriParamsCRL = storeParam.getURIParams();
		
        try {
            
            if (uriParamsCRL != null) {
                params = new StringBuffer();
                StringTokenizer st1 = new StringTokenizer(uriParamsCRL, ",");
                while (st1.hasMoreTokens()) {
                    String token = st1.nextToken();
                    StringTokenizer st2 = new StringTokenizer(token, "=");
                    if (st2.countTokens() == 2) {
                        String param = st2.nextToken();
                        String value = st2.nextToken();
                        params.append(URLEncDec.encode(param) + "=" + URLEncDec.encode(value));
                    } else {
                        continue;
                    }
                    
                    if (st1.hasMoreTokens()) {
                        params.append("&");
                    }                        
                }                    
            }
            
            URL uri = new URL(url); 
            con = HttpURLConnectionManager.getConnection(uri);
            
            // Prepare for both input and output
            con.setDoInput( true );
            
            // Turn off Caching
            con.setUseCaches(false);
            
            if (params != null) {
                byte[] paramsBytes = params.toString().trim().getBytes("UTF-8");
                if (paramsBytes.length > 0) {
                    con.setDoOutput( true );
                    con.setRequestProperty("Content-Length", 
                                          Integer.toString(paramsBytes.length));

                    // Write the arguments as post data
                    BufferedOutputStream out = 
                                new BufferedOutputStream(con.getOutputStream());
                    out.write(paramsBytes, 0, paramsBytes.length);
                    out.flush();
                    out.close();
		}
            }
            // Input ...
            InputStream in = con.getInputStream();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int len;
            byte[] buf = new byte[1024];
            while((len = in.read(buf,0,buf.length)) != -1) {
                bos.write(buf, 0, len);
            }
            crl = bos.toByteArray();
            
            if (debug.messageEnabled()) {
                debug.message("AMCRLStore.getCRLByHttpURI: crl.length = " +
                              crl.length);
            }
        } catch (Exception e) {
            debug.error("getCRLByHttpURI : Error in getting CRL",e);
        }
        
        return crl;
    }                

    // It returns NextCRLUpdate for current cached CRL
    // It gets CRL from crlAttribue member variable
    private boolean needCRLUpdate(X509CRL crl) {
        Date nextCRLUpdate = null;
        if (crl == null) {
            return true;
        }
        // Check CRLNextUpdate in CRL
        nextCRLUpdate = crl.getNextUpdate(); 
        if (debug.messageEnabled()) {
            debug.message("AMCRLStore.needCRLUpdate: nextCrlUpdate = " + 
                          nextCRLUpdate);
        }

        return ((nextCRLUpdate != null) && nextCRLUpdate.before(new Date()));
    }

    /**
     * It gets the new CRL from ldap server. 
     * If it is ldap URI, the URI has to be a dn that can be accessed 
     * with ldap anonymous bind. 
     * (example : ldap://server:port/uid=ca,o=company.com) 
     * This dn entry has to have CRL in attribute certificaterevocationlist 
     * or certificaterevocationlist;binary.
     * 
     * if attrNames does only contain one value the ldap search filter will be
     * (attrName=Value_of_the_corresponding_Attribute_from_SubjectDN)
     * e.g. SubjectDN of issuer cert 'C=BE, CN=Citizen CA, serialNumber=201007'
     * attrNames is 'CN', search filter used will be (CN=Citizen CA)
     * 
     * if attrNames does contain serveral values the ldap search filter value will be
     * a comma separated list of name attribute values, the search attribute will be 'cn'
     * (cn="attrNames[0]=Value_of_the_corresponding_Attribute_from_SubjectDN,
     * attrNames[1]=Value_of_the_corresponding_Attribute_from_SubjectDN")
     * 
     * e.g. SubjectDN of issuer cert 'C=BE, CN=Citizen CA, serialNumber=201007'
     * attrNames is {"CN","serialNumber"}, search filter used will be
         * (cn=CN=Citizen CA,serialNumber=201007)
     * 
     * The order of the values of attrNames matter as they must match the value of the
     * 'cn' attribute of a crlDistributionPoint entry in the directory server
     * 
     * @param ldapParam 
     * @param cert
     * @param attrNames, attributes names from the subjectDN of the issuer cert
     */
    static public X509CRL getCRL(AMLDAPCertStoreParameters ldapParam, 
                                 X509Certificate cert, String... attrNames) {
        X509CRL crl = null;
        
        try {
        
            if (!ArrayUtils.isEmpty(attrNames)) {

                X500Name dn = null;

                try {
                    dn = AMCRLStore.getIssuerDN(cert);
                } catch (Exception ex) {
                    if (debug.messageEnabled()) {
                        debug.message("AMCRLStore:getCRL " 
                            + "Certificate - getIssuerDN: ",ex);
                    }
                    return crl;
                }

                String searchFilter = null;

                if (attrNames.length < 2) {
                    /*
                     * Get the CN of the input certificate
                     */
                    String attrValue = null;

                    if (null != dn) {
                        // Retrieve attribute value of the attribute name
                        attrValue = dn.getAttributeValue(attrNames[0]);
                    }

                    if (null == attrValue) {
                        return crl;
                    }

                    searchFilter = setSearchFilter(attrNames[0], attrValue);


                } else {         

                    String searchFilterValue = buildSearchFilterValue(attrNames, dn);

                    if (searchFilterValue.isEmpty()) {
                        return crl;
                    }
                    searchFilter = setSearchFilter("cn", searchFilterValue);
                }

                if (debug.messageEnabled()) {
                    debug.message("AMCRLStore:getCRL using searchFilter " + searchFilter);
                }

                /*
                 * Lookup the certificate in the LDAP certificate directory
                 */ 

                ldapParam.setSearchFilter(searchFilter);

                AMCRLStore store = new AMCRLStore(ldapParam);	        
                crl = store.getCRL(cert);
            }
            
        } catch (Exception e) {
            debug.error("AMCRLStore:getCRL ",e);
        }
		
        return crl; 
    }
    
    private static String buildSearchFilterValue(String[] attrNames, X500Name dn) throws IOException {
        StringBuilder searchFilterBuilder = new StringBuilder();
        for (int i = 0; i < attrNames.length; i++) {
            String attrName = attrNames[i];
            String attrValue = dn.getAttributeValue(attrName);

            if (null != attrValue) {
                searchFilterBuilder.append(attrName);
                searchFilterBuilder.append("=");
                searchFilterBuilder.append(attrValue);
                if (i < attrNames.length - 1) {
                    searchFilterBuilder.append(",");
                }
            }
        }
        return searchFilterBuilder.toString();
    }    
}
