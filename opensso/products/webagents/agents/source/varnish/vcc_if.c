/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

#include "vrt.h"
#include "vcc_if.h"
#include "vmod_abi.h"


typedef void td_am_init(struct sess *, struct vmod_priv *, const char *, const char *);
typedef void td_am_cleanup(struct sess *, struct vmod_priv *);
typedef void td_am_request_cleanup(struct sess *, struct vmod_priv *);
typedef unsigned td_am_authenticate(struct sess *, struct vmod_priv *, const char *, const char *, const char *, int, const char *, struct sockaddr_storage *);
typedef void td_am_done(struct sess *, struct vmod_priv *);
typedef void td_am_ok(struct sess *, struct vmod_priv *);

const char Vmod_Name[] = "am";

const struct Vmod_Func_am {
    td_am_init *init;
    td_am_cleanup *cleanup;
    td_am_request_cleanup *request_cleanup;
    td_am_authenticate *authenticate;
    td_am_done *done;
    td_am_ok *ok;
    vmod_init_f *_init;
} Vmod_Func = {
    vmod_init,
    vmod_cleanup,
    vmod_request_cleanup,
    vmod_authenticate,
    vmod_done,
    vmod_ok,
    init_am,
};

const int Vmod_Len = sizeof (Vmod_Func);

const char Vmod_Proto[] =
        "typedef void td_am_init(struct sess *, struct vmod_priv *, const char *, const char *);\n"
        "typedef void td_am_cleanup(struct sess *, struct vmod_priv *);\n"
        "typedef void td_am_request_cleanup(struct sess *, struct vmod_priv *);\n"
        "typedef unsigned td_am_authenticate(struct sess *, struct vmod_priv *, const char *, const char *, const char *, int, const char *, struct sockaddr_storage *);\n"
        "typedef void td_am_done(struct sess *, struct vmod_priv *);\n"
        "typedef void td_am_ok(struct sess *, struct vmod_priv *);\n"
        "\n"
        "struct Vmod_Func_am {\n"
        "	td_am_init	*init;\n"
        "	td_am_cleanup	*cleanup;\n"
        "	td_am_request_cleanup	*request_cleanup;\n"
        "	td_am_authenticate	*authenticate;\n"
        "	td_am_done	*done;\n"
        "	td_am_ok	*ok;\n"
        "	vmod_init_f	*_init;\n"
        "} Vmod_Func_am;\n"
        ;

const char * const Vmod_Spec[] = {
    "am.init\0Vmod_Func_am.init\0VOID\0PRIV_VCL\0STRING\0STRING\0",
    "am.cleanup\0Vmod_Func_am.cleanup\0VOID\0PRIV_VCL\0",
    "am.request_cleanup\0Vmod_Func_am.request_cleanup\0VOID\0PRIV_VCL\0",
    "am.authenticate\0Vmod_Func_am.authenticate\0BOOL\0PRIV_VCL\0STRING\0STRING\0STRING\0INT\0STRING\0IP\0",
    "am.done\0Vmod_Func_am.done\0VOID\0PRIV_VCL\0",
    "am.ok\0Vmod_Func_am.ok\0VOID\0PRIV_VCL\0",
    "INIT\0Vmod_Func_am._init",
    0
};
const char Vmod_Varnish_ABI[] = VMOD_ABI_Version;
const void * const Vmod_Id = &Vmod_Id;

