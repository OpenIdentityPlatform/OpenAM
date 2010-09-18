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
# 2. saml2GenerateLDIF.pl - This script is useful if the service provider or the
# identity provider is an OpenSSO. It helps in generating LDAP Vx
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



# This function generates an alphanumeric sequence of random identifiers. The
# uniqueness can be extended by modifying the random seed. 
sub generateRandom() {

     $length = 30;
     $rndstr = "";

     for ($i=0; $i < $length; $i++)
     {
      $value = int rand 36 ;
      if($value eq '0') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '1') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '2') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '3') {
         $rndstr = $rndstr . $value;
      } elsif($value =~ '4') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '5') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '6') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '7') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '8') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '9') {
         $rndstr = $rndstr . $value;
      } elsif($value eq '10') {
         $rndstr = $rndstr . "a";
      } elsif($value eq '11') {
         $rndstr = $rndstr . "b";
      } elsif($value eq '12') {
         $rndstr = $rndstr . "c";
      } elsif($value eq '13') {
         $rndstr = $rndstr . "d";
      } elsif($value eq '14') {
         $rndstr = $rndstr . "e";
      } elsif($value eq '15') {
         $rndstr = $rndstr . "f";
      } elsif($value eq '16') {
         $rndstr = $rndstr . "g";
      } elsif($value eq '17') {
         $rndstr = $rndstr . "h";
      } elsif($value eq '18') {
         $rndstr = $rndstr . "i";
      } elsif($value eq '19') {
         $rndstr = $rndstr . "k";
      } elsif($value eq '20') {
         $rndstr = $rndstr . "j";
      } elsif($value eq '21') {
         $rndstr = $rndstr . "l";
      } elsif($value eq '22') {
         $rndstr = $rndstr . "m";
      } elsif($value eq '23') {
         $rndstr = $rndstr . "n";
      } elsif($value eq '24') {
         $rndstr = $rndstr . "o";
      } elsif($value eq '25') {
         $rndstr = $rndstr . "p";
      } elsif($value eq '26') {
         $rndstr = $rndstr . "q";
      } elsif($value eq '27') {
         $rndstr = $rndstr . "r";
      } elsif($value eq '28') {
         $rndstr = $rndstr . "s";
      } elsif($value eq '29') {
         $rndstr = $rndstr . "t";
      } elsif($value eq '30') {
         $rndstr = $rndstr . "u";
      } elsif($value eq '31') {
         $rndstr = $rndstr . "v";
      } elsif($value eq '32') {
         $rndstr = $rndstr . "w";
      } elsif($value eq '33') {
         $rndstr = $rndstr . "x";
      } elsif($value eq '34') {
         $rndstr = $rndstr . "y";
      } elsif($value eq '35') {
         $rndstr = $rndstr . "z";
      }
    }
    return $rndstr;
   
}


# Main function starts here. This script expects a users data file which
# could possibly contain user ids or  common attributes with "|" separated
# entries. After execution of this script, it will generate two files with
# localnameidentifiers.txt and remotenameidentifiers.txt data files with a
# generated unique ranom name identifier along with their userid/common 
# attributes. 

sub Main {

   my $fileName = $ARGV[0];

   open(USERFH, $fileName) || die("Could not open file!");
   @allusers=<USERFH>;
   close(USERFH);

   unlink 'localnameidentifiers.txt', 'remotenameidentifiers.txt';
   open (PH1, ">>localnameidentifiers.txt"); 
   print PH1 "# This is a generated file. Modify at your risk!"; 
   print PH1 "\n";
   open(PH2, ">>remotenameidentifiers.txt");
   print PH2 "# This is a generated file. Modify at your risk!"; 
   print PH2 "\n";

   foreach $line (@allusers) {
      chomp ($line);
      if(!($line =~ /^#/) && !($line =~ /^$/)) {

        ($firstuser, $seconduser)=split(/\|/, $line);
         $firstuser=~ s/(^ *)||( *$)//g;
         $seconduser=~ s/(^ *)||( *$)//g;
         $random=generateRandom();
         print PH1 "$firstuser      |  $random";
         print PH1 "\n";
         print PH2 "$seconduser     |  $random";
         print PH2 "\n";
        
      }
   }
}
 
Main;
