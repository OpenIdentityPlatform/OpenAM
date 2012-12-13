#!/usr/bin/perl
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
 # $Id: headers.cgi,v 1.5 2008/05/21 17:16:28 nithyas Exp $
 #

# Please dont modify the script, the agents tests rely on the
# white spaces introduced by this script, while looking up the
# HTTP headers
print "Content-type: text/html\n\n";
use CGI::Cookie;
# retrieve the cookie collection
my %cookies = fetch CGI::Cookie;
print "Cookies: EOL is |<BR>\n";
print "--------------------------------<BR>\n";
foreach (keys %cookies) {
        $name = $cookies{$_}->name;
        $value = $cookies{$_}->value;
        @array=split(/\|/,$value);
        # sort 'em
        @sorted=sort(@array);
        # put them back as | separated
        $sorted_val=join("|",@sorted);
        print "$name:$sorted_val|<BR>\n";

}

print "Headers <BR>\n";
print "----------------------------------<BR>\n";
for my $header ( sort keys %ENV) {
        # Since the Server send the multivalued attributes in
        # an unordered manner, this script will sort those multivalued
        # attributes in ascending order, so that matching
        # can be determinstic
        $value=$ENV{$header};
        #split the multi values
        if ($value =~/\|/){
                @array=split(/\|/,$value);
                # sort 'em
                @sorted=sort(@array);
                # put them back as | separated
                $sorted_val=join("|",@sorted);
                print "$header:$sorted_val\$\$<BR>\n";
             }
        else
             {
                # if not multi valued then print them as it is
                print "$header:$ENV{$header}\$\$<BR>\n";
             }
 }
