/* jcifs msrpc client library in Java
 * Copyright (C) 2007  "Michael B. Allen" <jcifs at samba dot org>
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

import jcifs.dcerpc.DcerpcError;
import jcifs.dcerpc.DcerpcException;
import jcifs.dcerpc.DcerpcHandle;
import jcifs.dcerpc.rpc;

import java.io.IOException;

public class SamrPolicyHandle extends rpc.policy_handle {

    public SamrPolicyHandle(DcerpcHandle handle, String server, int access) throws IOException {
        if (server == null)
            server = "\\\\";
        MsrpcSamrConnect4 rpc = new MsrpcSamrConnect4(server, access, this);
        try {
            handle.sendrecv(rpc);
        } catch (DcerpcException de) {
            if (de.getErrorCode() != DcerpcError.DCERPC_FAULT_OP_RNG_ERROR)
                throw de;

            MsrpcSamrConnect2 rpc2 = new MsrpcSamrConnect2(server, access, this);
            handle.sendrecv(rpc2);
        }
    }

    public void close() throws IOException {
    }
}

