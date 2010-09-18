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
# $Id: xml_sec.rb,v 1.6 2007/10/24 00:28:41 todddd Exp $
#
# Copyright 2007 Sun Microsystems Inc. All Rights Reserved
# Portions Copyrighted 2007 Todd W Saxton.

require "rexml/document"
require "rexml/xpath"
require "openssl"
require "xmlcanonicalizer"
require "digest/sha1"
 
#
# WARNING, WARNING: VERY rudimentary
#
#
module XMLSecurity

  class SignedDocument < REXML::Document

    def validate (idp_cert_fingerprint, logger)
        
      # get cert from response
      base64_cert = self.elements["//X509Certificate"].text
      cert_text = Base64.decode64(base64_cert)
      cert = OpenSSL::X509::Certificate.new(cert_text)
      
      # check cert matches registered idp cert
      fingerprint = Digest::SHA1.hexdigest(cert.to_der)
      logger.info("fingerprint = " + fingerprint) if !logger.nil?
      valid_flag = fingerprint == idp_cert_fingerprint.gsub(":", "").downcase
      
      return valid_flag if !valid_flag 
      
      validate_doc(base64_cert, logger)
    end
    
    def validate_doc(base64_cert, logger)
      
      #        
      #validate references
      #
      
      # remove signature node
      sig_element = XPath.first(self, "//ds:Signature", {"ds"=>"http://www.w3.org/2000/09/xmldsig#"})
      sig_element.remove
      
      #check digests
      logger.info("checking digests") if !logger.nil?
      XPath.each(sig_element, "//ds:Reference", {"ds"=>"http://www.w3.org/2000/09/xmldsig#"}) do | ref |          
        uri = ref.attributes.get_attribute("URI").value
        logger.info("URI = " + uri[1,uri.size]) if !logger.nil?
        hashed_element = XPath.first(self, "//[@ID='#{uri[1,uri.size]}']")
        logger.info("hashed element = " + hashed_element.to_s) if !logger.nil?
        canoner = XML::Util::XmlCanonicalizer.new(false, true)
        canon_hashed_element = canoner.canonicalize_element(hashed_element)
        logger.info("canon hashed element = " + canon_hashed_element) if !logger.nil?
        hash = Base64.encode64(Digest::SHA1.digest(canon_hashed_element)).chomp
        digest_value = XPath.first(ref, "//ds:DigestValue", {"ds"=>"http://www.w3.org/2000/09/xmldsig#"}).text
        logger.info("hashed_element_hash = " + hash) if !logger.nil?
        logger.info("digest_value_element = " + digest_value) if !logger.nil?
        
        valid_flag = hash == digest_value 
        return valid_flag if !valid_flag
      end
 
      #
      # verify dig sig
      # 
      logger.info("checking dig sig") if !logger.nil?

      # signed_info_element.add_namespace("http://www.w3.org/2000/09/xmldsig#") if signed_info_element.namespace.nil? || signed_info_element.namespace.empty?
      canoner = XML::Util::XmlCanonicalizer.new(false, true)
      signed_info_element = XPath.first(sig_element, "//ds:SignedInfo", {"ds"=>"http://www.w3.org/2000/09/xmldsig#"})
      canon_string = canoner.canonicalize_element(signed_info_element)
      logger.info("canon INFO = " + canon_string) if !logger.nil?

      base64_signature = XPath.first(sig_element, "//ds:SignatureValue", {"ds"=>"http://www.w3.org/2000/09/xmldsig#"}).text
      logger.info("base 64 SIG = " + base64_signature) if !logger.nil?
      signature = Base64.decode64(base64_signature)
      logger.info("SIG = " + signature) if !logger.nil?
      
      # get cert object
      cert_text = Base64.decode64(base64_cert)
      cert = OpenSSL::X509::Certificate.new(cert_text)
      
      valid_flag = cert.public_key.verify(OpenSSL::Digest::SHA1.new, signature, canon_string)
        
      return valid_flag
    end
   
  end
end
