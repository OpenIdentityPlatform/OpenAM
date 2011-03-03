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

# The bulk account federation in OpenSSO is achieved through couple of
# perl scripts.
# 1. amGenerateNI.pl - This script will generate random name identifiers for
# each user accounts from a service provider and an identity provider that
# have one to one mappings in a flat file separated by "|". 
#
# For e.g. a flat file could like this:
#  uid=spuser1,ou=People,dc=sp,dc=com   | uid=idpuser1,ou=People,dc=idp,dc=com
#  uid=spuser2,ou=People,dc=sp,dc=com   | uid=idpuser2,ou=People,dc=idp,dc=com
#  uid=spuser3,ou=People,dc=sp,dc=com   | uid=idpuser3,ou=People,dc=idp,dc=com
#
# After running this script on the above flat file, it would generate two
# other flat files which contains user id to name identifier mappings. 
#
# For e.g., the output may look like this.
#  uid=spuser1,ou=People,dc=sp,dc=com   | 1is341jv024lkw3j6pmpr0s82apqxn8a
#  uid=spuser2,ou=People,dc=sp,dc=com   | wkh34ldd88n8l54gzs4rftb34bs4837u
#  uid=spuser3,ou=People,dc=sp,dc=com	| l514znc34u34n34gf65hdg6truqh7f2x2424
#
# 2. amGenerateLDIF.pl - This script is useful if the service provider or the
# identity provider is an OpenSSO. It helps in generating LDAP Vx
# based LDIF files so that they could easily uploaded to the user entries. 
# In this case, it assumes that the entries are userDNs.
#
# This script will require input parameters as follows.
# generateLDIF.pl <nameidmappingsfile> ServiceProviderID IdentityProviderID
#      ProviderRole 
#    Where 
#        nameidmappingsfile - a file that has userid/nameid mappings.
#        ServiceProviderID - Service ProviderID For e.g. http://www.sp1.com
#        IdentityProviderID - Identity ProviderID For e.g. http://www.idp1.com
#        ProviderRole - ProviderRole For e.g. IDP or SP.
#
# After running this script, it generates an LDIF file like this:
# dn: uid=spuser1,ou=People,dc=sp,dc=com
# changetype: modify
# iplanet-am-user-federation-info: |http://www.idp1.com|null|null|null|1is341jv024lkw3j6pmpr0s82apqxn8a|http://www.sp1.com|urn:liberty:iff:nameid:federated|IDPRole|Active|
# iplanet-am-user-federation-info-key: |http://www.sp1.com|1is341jv024lkw3j6pmpr0s82apqxn8a|

# dn: uid=spuser2,ou=People,dc=sp,dc=com
# changetype: modify
# iplanet-am-user-federation-info: |http://www.idp1.com|null|null|null|wkh34ldd88n8l54gzs4rftb34bs4837u|http://www.sp1.com|urn:liberty:iff:nameid:federated|IDPRole|Active|
# iplanet-am-user-federation-info-key: |http://www.sp1.com|wkh34ldd88n8l54gzs4rftb34bs4837u|

# dn: uid=spuser3,ou=People,dc=sp,dc=com
# changetype: modify
# iplanet-am-user-federation-info: |http://www.idp1.com|null|null|null|l514znc34u34n34gf65hdg6truqh7f2x2424|http://www.sp1.com|urn:liberty:iff:nameid:federated|IDPRole|Active|
# iplanet-am-user-federation-info-key: |http://www.sp1.com|l514znc34u34n34gf65hdg6truqh7f2x2424|
#
# The generated LDIF file could be loaded into the user repository using
# ldapmodify as follows.
# ldapmodify -D "cn=Directory Manager" -w 11111111 -h www.sp1.com -p 389 
#            -f generatedfile.ldif 
#


sub Main {

    if(@ARGV < 4 || @ARGV > 4) {
       print "Usage: amGenerateLDIF.pl NameIDMappingFile SPProviderID, IDPProviderID, ProviderRole";
       print "\n";
       print "\n";
       print "Example Usage: amGenerateLDIF.pl provider.txt http://www.sp1.com http://www.idp1.com IDP|SP";
       print "\n";
       exit 1;
    }

    my $fileName = $ARGV[0];
    $spproviderid = $ARGV[1];
    $idpproviderid = $ARGV[2];
    $role = $ARGV[3];

    if($role ne 'IDP' && $role ne 'SP') {
       print "$role: Invalid Provider Role. Role must be either IDP or SP\n";
       exit 1;
    }

    open(NIMH, $fileName) || die("Could not open file");
    @allusers=<NIMH>;
    close(NIMH);
    

    $format="urn:liberty:iff:nameid:federated";

    foreach $line (@allusers) {
        chomp($line);

        if(!($line =~ /^#/) && !($line =~ /^$/)) {

           ($userdn, $nameidentifier)=split(/\|/, $line);

            # Remove leading-trailing spaces.
            $userdn=~ s/(^ *)||( *$)//g;
            $nameidentifier=~ s/(^ *)||( *$)//g;

            $fedkey="|$spproviderid|$nameidentifier|";

            if($role eq 'SP') {

               open(UULSP, ">>spuserdata.ldif");
               $userinfo = "|".$idpproviderid."|null|null|null|";
               $userinfo = $userinfo.$nameidentifier."|".$spproviderid."|";
               $userinfo = $userinfo.$format."|IDPRole|Active|";

               print UULSP  "dn: $userdn\n";
               print UULSP  "changetype: modify\n";
               print UULSP  "add: iplanet-am-user-federation-info\n";
               print UULSP  "iplanet-am-user-federation-info: $userinfo\n";
               print UULSP  "-\n";
               print UULSP  "add: iplanet-am-user-federation-info-key\n";
               print UULSP  "iplanet-am-user-federation-info-key: $fedkey\n";
               print UULSP  "\n";

            } elsif ($role eq 'IDP') {

               open(UULIDP, ">>idpuserdata.ldif");
               $userinfo = "|".$spproviderid."|".$nameidentifier."|";
               $userinfo = $userinfo.$spproviderid."|".$format;
               $userinfo = $userinfo."|null|null|null|SPRole|Active";

               print UULIDP "dn: $userdn\n";
               print UULIDP "changetype: modify\n";
               print UULIDP "add: iplanet-am-user-federation-info\n";
               print UULIDP "iplanet-am-user-federation-info: $userinfo\n";
               print UULIDP "-\n";
               print UULIDP  "add: iplanet-am-user-federation-info-key\n";
               print UULIDP "iplanet-am-user-federation-info-key: $fedkey\n";
               print UULIDP "\n";

            } else {
               print "Invalid Provider Role\n";
               exit 1;
            }
        }
    }
}

Main;
