#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2012 ForgeRock AS. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# http://forgerock.org/license/CDDLv1.0.html
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at http://forgerock.org/license/CDDLv1.0.html
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#

# This makefile is used to grab all the components needed to build.
# overrides what's in defines.mk and defines_SunOS.mk

ifeq ($(BUILD_TYPE), 64)
DSAME_DROP_FILE := common_3_0_$(OS_ARCH)_sparc_x86_64
else
DSAME_DROP_FILE := common_3_0_$(OS_ARCH)_sparc_x86
endif
