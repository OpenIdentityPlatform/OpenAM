# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# https://opensso.dev.java.net/public/CDDLv1.0.html or
# opensso/legal/CDDLv1.0.txt
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at opensso/legal/CDDLv1.0.txt.
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#
# $Id: relying_party.rb,v 1.1 2007/03/19 22:45:54 todddd Exp $
#
# Copyright 2007 Sun Microsystems Inc. All Rights Reserved
# Portions Copyrighted 2007 Todd W Saxton.


require "base64"
require "rexml/document"

require "xml_sec" 

module SAML

  class RelyingParty
  
    cattr_accessor :logger
    attr_accessor :name_id
          
    def initialize(metadata)
      @assertion_consumer_service_URL = metadata[:assertion_consumer_service_URL]
      @issuer = metadata[:issuer]
      @sp_name_qualifier = metadata[:sp_name_qualifier]
      @idp_sso_target_url = metadata[:idp_sso_target_url]
      @idp_slo_target_url = metadata[:idp_slo_target_url]
      @idp_cert_fingerprint = metadata[:idp_cert_fingerprint]
    end
    
    def create_auth_request

      id = generateUniqueHexCode(42)
      issue_instant = Time.new().strftime("%Y-%m-%dT%H:%M:%SZ")
      
      auth_request = "<samlp:AuthnRequest  " +
        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
        "ID=\"" + id + "\" " +
        "Version=\"2.0\" " +
        "IssueInstant=\"" + issue_instant + "\" " +
        "ForceAuthn=\"false\" " +
        "isPassive=\"false\" " +
        "ProtocolBinding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" " +
        "AssertionConsumerServiceURL=\"" + @assertion_consumer_service_URL + "\">\n" +
          "<saml:Issuer " +
          "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" +
            @issuer +
          "</saml:Issuer>\n" +
          "<samlp:NameIDPolicy  " +
          "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
          "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
          "SPNameQualifier=\"" + @sp_name_qualifier + "\" " +
          "AllowCreate=\"true\">\n" +
          "</samlp:NameIDPolicy>\n" +
          "<samlp:RequestedAuthnContext " +
          "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
          "Comparison=\"exact\">" +
            "<saml:AuthnContextClassRef " +
            "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" +
              "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport" +
            "</saml:AuthnContextClassRef>" +
          "</samlp:RequestedAuthnContext>\n" +
        "</samlp:AuthnRequest>"
      
      logger.info(auth_request) if !logger.nil?
      
      deflated_auth_request = Zlib::Deflate.deflate(auth_request, 9)[2..-5]     
      base64_auth_request = Base64.encode64(deflated_auth_request)  
      encoded_auth_request = CGI.escape(base64_auth_request)  
    

      redirect_url = @idp_sso_target_url + "?SAMLRequest=" + encoded_auth_request 
  
      return redirect_url
      
    end

    def process_auth_response(raw_response)
    
        logger.info("Raw Response: " + raw_response ) if !logger.nil?

        # raw_response is all ready URL decoded...
        @saml_response = Base64.decode64( raw_response )
        logger.info("Authn response = " + @saml_response) if !logger.nil?

        saml_response_doc = XMLSecurity::SignedDocument.new @saml_response
                  
        if valid_flag = saml_response_doc.validate(@idp_cert_fingerprint, logger)
          self.name_id = saml_response_doc.elements["/samlp:Response/saml:Assertion/saml:Subject/saml:NameID"].text
        else
          # error
          logger.error("Invalid SAML response")
        end          

        return valid_flag
    end
    


    def create_logout_request

      saml_response_doc = REXML::Document.new @saml_response      
      id = generateUniqueHexCode(42)
      issue_instant = Time.new().strftime("%Y-%m-%dT%H:%M:%SZ")
      name_id = saml_response_doc.elements["/samlp:Response/saml:Assertion/saml:Subject/saml:NameID"].text
      name_qualifier = saml_response_doc.elements["/samlp:Response/saml:Assertion/saml:Subject/saml:NameID"].attributes["NameQualifier"]
      session_index = saml_response_doc.elements["/samlp:Response/saml:Assertion/saml:AuthnStatement"].attributes["SessionIndex"]
      
      logout_request = "<samlp:LogoutRequest " +
        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " + 
        "ID=\"" + id + "\" " +
        "Version=\"2.0\" " +
        "IssueInstant=\"" + issue_instant + "\"> " +
          "<saml:Issuer " + 
          "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" +
            @issuer +
          "</saml:Issuer>" +
          "<saml:NameID " + 
          "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " + 
          "NameQualifier=\"" + name_qualifier + "\" " + 
          "SPNameQualifier=\"" + @sp_name_qualifier + "\" " + 
          "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">" + 
            name_id +
          "</saml:NameID>" + 
          "<samlp:SessionIndex " +
          "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">" + 
            session_index +
          "</samlp:SessionIndex>" +
        "</samlp:LogoutRequest>";
  
      logger.info("Logout request = " + logout_request) if !logger.nil?
  
      deflated_logout_request = Zlib::Deflate.deflate(logout_request, 9)[2..-5]     
      base64_logout_request = Base64.encode64(deflated_logout_request)  
      encoded_logout_request = CGI.escape(base64_logout_request)  

      redirect_url = @idp_slo_target_url + "?SAMLRequest=" + encoded_logout_request 
  
      return redirect_url
      
    end
    
    private
    
    def generateUniqueHexCode( codeLength )
        validChars = ("A".."F").to_a + ("0".."9").to_a
        length = validChars.size
    
        hexCode = ""
        1.upto(codeLength) { |i| hexCode << validChars[rand(length-1)] }
    
        hexCode
    end
  end
  
end
