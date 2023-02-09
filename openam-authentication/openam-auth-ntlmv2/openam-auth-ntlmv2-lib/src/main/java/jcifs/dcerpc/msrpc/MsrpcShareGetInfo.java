/* jcifs msrpc client library in Java
 * Copyright (C) 2006  "Michael B. Allen" <jcifs at samba dot org>
 *                     "Eric Glass" <jcifs at samba dot org>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jcifs.dcerpc.msrpc;

import jcifs.smb.ACE;
import jcifs.smb.SecurityDescriptor;

import java.io.IOException;

public class MsrpcShareGetInfo extends srvsvc.ShareGetInfo {

    public MsrpcShareGetInfo(String server, String sharename) {
        super(server, sharename, 502, new srvsvc.ShareInfo502());
        ptype = 0;
        flags = DCERPC_FIRST_FRAG | DCERPC_LAST_FRAG;
    }

    public ACE[] getSecurity() throws IOException {
        srvsvc.ShareInfo502 info502 = (srvsvc.ShareInfo502)info;
        if (info502.security_descriptor != null) {
            SecurityDescriptor sd;
            sd = new SecurityDescriptor(info502.security_descriptor, 0, info502.sd_size);
            return sd.aces;
        }
        return null;
    }
}
