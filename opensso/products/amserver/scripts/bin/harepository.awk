#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
# $Id: harepository.awk,v 1.2 2008/06/25 05:41:16 qcheng Exp $
#
#

BEGIN{
}
{
  if (index($0,SECTIONNAME)!=0)
  {
    getline
    while (index($0,"[")==0)
    {
	if (NF > 0 && substr($0,0,1)!="#")
	{
	   if (SECTIONNAME == "[HADBINFO]")
	   {
		if (length($2)!=0)
			printf(" --%s=%s",$1,$2);
		else
			printf(" %s\n",$0);

	  }
	  if (SECTIONNAME != "[HADBINFO]")
	  {
	  	if (length($2)!=0)
		    printf(" --%s",$0);
	        else
	    	    printf(" %s\n",$0);
	  }
	}

	if (getline == 0)
	  break
	}
  }
}
END{
	if (SECTIONNAME == "[SESSION_STORE]" )
		printf("\n");
} 

