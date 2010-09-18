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
# $Id: account_controller.rb,v 1.1 2007/03/20 05:26:56 todddd Exp $
#
# Copyright 2007 Sun Microsystems Inc. All Rights Reserved
# Portions Copyrighted 2007 Todd W Saxton.

require "pathname"
require "cgi"
require "relying_party"


class AccountController < ApplicationController
  
  def login
      relying_party = SAML::RelyingParty.new(
        :assertion_consumer_service_URL => "http://localhost:3008/account/complete",
        :issuer => "localhost_ruby",
        :sp_name_qualifier => "localhost_ruby",
        :idp_sso_target_url => "http://localhost:8080/openfm-samples-ip/SSORedirect/metaAlias/ip_meta_alias",
        :idp_slo_target_url => "http://localhost:8080/openfm-samples-ip/IDPSloRedirect/metaAlias/ip_meta_alias",
        :idp_cert_fingerprint => "93:bd:43:a4:60:65:a4:05:95:98:a9:d8:f4:8b:4c:c8:5f:31:87:e9" 
      )
      relying_party.logger = logger
      session[:relying_party] = relying_party
      request = relying_party.create_auth_request      
      redirect_to(request)
  end

  def complete    

    if params[:SAMLResponse].empty?
        flash[:notice] = 'Unknown response from SAML server.'
        redirect_to :action => 'index' 
    end

    valid_flag = session[:relying_party].process_auth_response(params[:SAMLResponse])
    
    if valid_flag
      redirect_to :action => "welcome"
    else
      flash[:notice] = 'invalid SAML Response.'
      redirect_to :action => "index"
    end
    
  end
  
  def logout
  
    request = session[:relying_party].create_logout_request
    session[:relying_party] = nil

    redirect_to(request)
    
  end
     
end
