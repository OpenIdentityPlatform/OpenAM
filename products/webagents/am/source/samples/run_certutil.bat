@echo off
rem 
rem "$Id: run_certutil.bat,v 1.1 2006/05/03 22:42:53 madan_ranganath Exp $"
rem
rem  PROPRIETARY/CONFIDENTIAL.  Use of this product is subject to license terms.
rem  Copyright 2006 Sun Microsystems, Inc.  All rights reserved.
rem 
set PATH=.;..\lib;%PATH%

certutil %*
