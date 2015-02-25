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

struct sess;
struct VCL_conf;
struct vmod_priv;

void vmod_init(struct sess *, struct vmod_priv *, const char *, const char *);
void vmod_cleanup(struct sess *, struct vmod_priv *);
void vmod_request_cleanup(struct sess *, struct vmod_priv *);
unsigned vmod_authenticate(struct sess *, struct vmod_priv *, const char *, const char *, const char *, int, const char *, struct sockaddr_storage *);
void vmod_done(struct sess *, struct vmod_priv *);
void vmod_ok(struct sess *, struct vmod_priv *);
int init_am(struct vmod_priv *, const struct VCL_conf *);
extern const void * const Vmod_Id;
