#!/usr/bin/perl

# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
#
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
# $Id
#

# The bulk account federation for SAML v2 in OpenSSO or
# Sun Java System Federation Manager is achieved through the following perl
# scripts.
# 1. saml2GenerateNI.pl - This script will generate random name identifiers for
# each user accounts from a service provider and an identity provider that
# have one to one mappings in a flat file separated by "|".
#
# For e.g. a flat file could like this:
#  uid=spuser1,ou=People,dc=sp,dc=com	| uid=idpuser1,ou=People,dc=idp,dc=com
#  uid=spuser2,ou=People,dc=sp,dc=com	| uid=idpuser2,ou=People,dc=idp,dc=com
#  uid=spuser3,ou=People,dc=sp,dc=com	| uid=idpuser3,ou=People,dc=idp,dc=com
#
# After running this script on the above flat file, it would generate two
# other flat files which contains user id to name identifier mappings.
#
# For e.g., the output may look like this.
#  uid=spuser1,ou=People,dc=sp,dc=com	| 1is341jv024lkw3j6pmpr0s82apqxn8a
#  uid=spuser2,ou=People,dc=sp,dc=com	| wkh34ldd88n8l54gzs4rftb34bs4837u
#  uid=spuser3,ou=People,dc=sp,dc=com	| l514znc34u34n34gf65hdg6truqh7f2x2424
#
# 2. saml2GenerateLDIF.pl - This script is useful if the service provider or the# identity provider is an OpenSSO. It helps in generating LDAP Vx
# based LDIF files so that they could easily uploaded to the user entries.
# In this case, it assumes that the entries are userDNs.
#
# This script will require input parameters as follows.
# saml2GenerateLDIF.pl <nameidmappingsfile> localentityid remoteentityid
#	entityrole
#
# After running this script, it generates an LDIF file like this:
# dn: uid=spuser1,ou=People,dc=sp,dc=com
# changetype: modify
# sun-fm-saml2-nameid-info: www.sp1.com|www.idp1.com|1is341jv024lkw3j6pmpr0s82apqxn8a|www.idp1.com|urn:oasis:names:tc:SAML:2.0:nameid-format:persistent|null|www.sp1.com|SPRole|false
# sun-fm-saml2-nameid-infokey: www.sp1.com|www.idp1.com|1is341jv024lkw3j6pmpr0s82apqxn8a
# dn: uid=spuser2,ou=People,dc=sp,dc=com
# changetype: modify
# sun-fm-saml2-nameid-info: www.sp1.com|www.idp1.com|wkh34ldd88n8l54gzs4rftb34bs4837u|www.idp1.com|urn:oasis:names:tc:SAML:2.0:nameid-format:persistent|null|www.sp1.com|SPRole|false
# sun-fm-saml2-nameid-infokey: www.sp1.com|www.idp1.com|wkh34ldd88n8l54gzs4rftb34bs4837u
# dn: uid=spuser3,ou=People,dc=sp,dc=com
# changetype: modify
# sun-fm-saml2-nameid-info: www.sp1.com|www.idp1.com|l514znc34u34n34gf65hdg6truqh7f2x2424|www.idp1.com|urn:oasis:names:tc:SAML:2.0:nameid-format:persistent|null|www.sp1.com|SPRole|false
# sun-fm-saml2-nameid-infokey: www.sp1.com|www.idp1.com|l514znc34u34n34gf65hdg6truqh7f2x2424
#
# The generated LDIF file could be loaded into the user repository using
# ldapmodify as follows.
# ldapmodify -D "cn=Directory Manager" -w 11111111 -h www.sp1.com -p 389
#		-f generatedfile.ldif
#

sub Main {

    if(@ARGV < 4 || @ARGV > 4) {
       print "Usage: saml2GenerateLDIF.pl nameidmappingfile hostentityid, remoteentityid, hostentityrole";
       print "\n";
       print "\n";
       print "Example Usage: saml2GenerateLDIF.pl provider.txt www.sp1.com www.idp1.com IDP";
       print "\n";
       exit 1;
    }

    my $fileName = $ARGV[0];
    $hostentityid = $ARGV[1];
    $remoteentityid = $ARGV[2];
    $role = $ARGV[3];

    if($role ne 'IDP' && $role ne 'SP') {
       print "$role: Invalid Provider Role. Role must be either IDP or SP\n";
       exit 1;
    }

    open(NIMH, $fileName) || die("Could not open file");
    @allusers=<NIMH>;
    close(NIMH);
    

    $format="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";

    foreach $line (@allusers) {
        chomp($line);

        if(!($line =~ /^#/) && !($line =~ /^$/)) {

           ($userdn, $nameidentifier)=split(/\|/, $line);

            # Remove leading-trailing spaces.
            $userdn=~ s/(^ *)||( *$)//g;
            $nameidentifier=~ s/(^ *)||( *$)//g;

            $fedkey="$hostentityid|$remoteentityid|$nameidentifier";

            $userinfo = $hostentityid."|".$remoteentityid."|";
            $userinfo = $userinfo.$nameidentifier."|";
            if($role eq 'SP') {
               $userinfo = $userinfo.$remoteentityid."|";
            } else {
               $userinfo = $userinfo.$hostentityid."|";
            }
            $userinfo =  $userinfo.$format."|null|null|";

            if($role eq 'SP') {
               $userinfo = $userinfo."SPRole|false";
            } else {
               $userinfo = $userinfo."IDPRole|false";
            }
            
            open(FH, ">>userdata.ldif");
            print FH  "dn: $userdn\n";
            print FH  "changetype: modify\n";
            print FH  "add: sun-fm-saml2-nameid-info\n";
            print FH  "sun-fm-saml2-nameid-info: $userinfo\n";
            print FH  "-\n";
            print FH  "add: sun-fm-saml2-nameid-infokey\n";
            print FH  "sun-fm-saml2-nameid-infokey: $fedkey\n";
            print FH  "\n";
        }
    }
}

Main;
