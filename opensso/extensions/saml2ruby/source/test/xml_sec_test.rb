$LOAD_PATH.insert(0, File.expand_path(File.dirname(__FILE__)) + "/../lib")
load "xml_sec.rb"
require "logger"
require "test/unit"

class XMLSecurityTest < Test::Unit::TestCase

  
  def setup
    @log = Logger.new("test_output.log")
    @log.level = Logger::DEBUG
    @log.info "starting"
  end
  
  def test_open_fed_response
    puts "Processing OpenFederation file"
    @log.info "Processing OpenFederation file"
    File.open("test/authNResponseOpenFed.xml", aModeString="r") {|file| 
    
      saml_response_doc = XMLSecurity::SignedDocument.new(file)
               
      base64_cert = saml_response_doc.elements["//X509Certificate"].text
      assert saml_response_doc.validate_doc(base64_cert, @log) 
    }
  end
  
  def test_rsa_fim_response
    puts "Processing RSA file"
    @log.info "Processing RSA file"
    File.open("test/authNResponseRSAFIM.xml", aModeString="r") {|file| 
    
      saml_response_doc = XMLSecurity::SignedDocument.new(file)
               
      base64_cert = "MIICODCCAaECBEatbN8wDQYJKoZIhvcNAQEEBQAwYzELMAkGA1UEBhMCTloxEDAOBgNVBAgTB1Vua25vd24xEzARBgNVBAcTCldlbGxpbmd0b24xDDAKBgNVBAoTA1NTQzEMMAoGA1UECxMDU1NDMREwDwYDVQQDEwhHTFMgU0FNTDAeFw0wNzA3MzAwNDQ1MTlaFw0xMjA3MjgwNDQ1MTlaMGMxCzAJBgNVBAYTAk5aMRAwDgYDVQQIEwdVbmtub3duMRMwEQYDVQQHEwpXZWxsaW5ndG9uMQwwCgYDVQQKEwNTU0MxDDAKBgNVBAsTA1NTQzERMA8GA1UEAxMIR0xTIFNBTUwwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAOy62p6wMNRRVCSs7/4fnPrHqFehVBaYeg9yayD1I/yqYXvmOgbYX/OnuIE+RylVDwkzmKHgNsLpw8jfw1Ee2f0qmZY1rs6x+jmE4yDLDR1eKNGCkB4hSep2cVknWCBBzvmrk1nKet8Aw460FU2+C5H67Iwj5sVqshi5noLXSckTAgMBAAEwDQYJKoZIhvcNAQEEBQADgYEAL2ebLISFR6F0RzeEpLOjv4kSfDhsELSzEi4vVqlmCm+YcRWH0ASik3Ynl1B/K05cosqD5RMJG71t6ZWNf/s5F4NX0blU0ZAWewQXIUS4CUZPcQT3K/WXbpWBjRDIY0Cj6Dim/yBdmYSxZV51sDAIfOq4FXb5bEPSCopKK1YQRLE="     
      assert saml_response_doc.validate_doc(base64_cert, @log)
    }
  end
  
  def test_rsa_fim_response_after_enc
    puts "Processing RSA file after encryption"
    @log.info "Processing RSA file after encryption"
    File.open("test/authNResponseRSAFIMAfterEncryption.xml", aModeString="r") {|file| 
    
      saml_response_doc = XMLSecurity::SignedDocument.new(file)
               
      base64_cert = "MIICODCCAaECBEatbN8wDQYJKoZIhvcNAQEEBQAwYzELMAkGA1UEBhMCTloxEDAOBgNVBAgTB1Vua25vd24xEzARBgNVBAcTCldlbGxpbmd0b24xDDAKBgNVBAoTA1NTQzEMMAoGA1UECxMDU1NDMREwDwYDVQQDEwhHTFMgU0FNTDAeFw0wNzA3MzAwNDQ1MTlaFw0xMjA3MjgwNDQ1MTlaMGMxCzAJBgNVBAYTAk5aMRAwDgYDVQQIEwdVbmtub3duMRMwEQYDVQQHEwpXZWxsaW5ndG9uMQwwCgYDVQQKEwNTU0MxDDAKBgNVBAsTA1NTQzERMA8GA1UEAxMIR0xTIFNBTUwwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAOy62p6wMNRRVCSs7/4fnPrHqFehVBaYeg9yayD1I/yqYXvmOgbYX/OnuIE+RylVDwkzmKHgNsLpw8jfw1Ee2f0qmZY1rs6x+jmE4yDLDR1eKNGCkB4hSep2cVknWCBBzvmrk1nKet8Aw460FU2+C5H67Iwj5sVqshi5noLXSckTAgMBAAEwDQYJKoZIhvcNAQEEBQADgYEAL2ebLISFR6F0RzeEpLOjv4kSfDhsELSzEi4vVqlmCm+YcRWH0ASik3Ynl1B/K05cosqD5RMJG71t6ZWNf/s5F4NX0blU0ZAWewQXIUS4CUZPcQT3K/WXbpWBjRDIY0Cj6Dim/yBdmYSxZV51sDAIfOq4FXb5bEPSCopKK1YQRLE="     
      assert saml_response_doc.validate_doc(base64_cert, @log)
    }
  end
  
  def test_ak_assertion
    puts "Processing AK file"
    @log.info "Processing AK file"
    File.open("test/assertion_from_AK.xml", aModeString="r") {|file| 
    
      saml_response_doc = XMLSecurity::SignedDocument.new(file)
               
      base64_cert = saml_response_doc.elements["//X509Certificate"].text
      assert saml_response_doc.validate_doc(base64_cert, @log)
    }
  end
end