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

package jcifs.dcerpc;

import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;

public class DcerpcBind extends DcerpcMessage {

    static final String[] result_message = {
        "0",
        "DCERPC_BIND_ERR_ABSTRACT_SYNTAX_NOT_SUPPORTED",
        "DCERPC_BIND_ERR_PROPOSED_TRANSFER_SYNTAXES_NOT_SUPPORTED",
        "DCERPC_BIND_ERR_LOCAL_LIMIT_EXCEEDED"
    };

    static String getResultMessage(int result) {
        return result < 4 ?
                result_message[result] :
                "0x" + jcifs.util.Hexdump.toHexString(result, 4);
    }
    public DcerpcException getResult() {
        if (result != 0)
            return new DcerpcException(getResultMessage(result));
        return null;
    }

    DcerpcBinding binding;
    int max_xmit, max_recv;

    public DcerpcBind() {
    }
    DcerpcBind(DcerpcBinding binding, DcerpcHandle handle) {
        this.binding = binding;
        max_xmit = handle.max_xmit;
        max_recv = handle.max_recv;
        ptype = 11;
        flags = DCERPC_FIRST_FRAG | DCERPC_LAST_FRAG;
    }

    public int getOpnum() {
        return 0;
    }
    public void encode_in(NdrBuffer buf) throws NdrException {
        buf.enc_ndr_short(max_xmit);
        buf.enc_ndr_short(max_recv);
        buf.enc_ndr_long(0); /* assoc. group */
        buf.enc_ndr_small(1); /* num context items */
        buf.enc_ndr_small(0); /* reserved */
        buf.enc_ndr_short(0); /* reserved2 */
        buf.enc_ndr_short(0); /* context id */
        buf.enc_ndr_small(1); /* number of items */
        buf.enc_ndr_small(0); /* reserved */
        binding.uuid.encode(buf);
        buf.enc_ndr_short(binding.major);
        buf.enc_ndr_short(binding.minor);
        DCERPC_UUID_SYNTAX_NDR.encode(buf);
        buf.enc_ndr_long(2); /* syntax version */
    }
    public void decode_out(NdrBuffer buf) throws NdrException {
        buf.dec_ndr_short(); /* max transmit frag size */
        buf.dec_ndr_short(); /* max receive frag size */
        buf.dec_ndr_long();  /* assoc. group */
        int n = buf.dec_ndr_short(); /* secondary addr len */
        buf.advance(n); /* secondary addr */
        buf.align(4);
        buf.dec_ndr_small(); /* num results */
        buf.align(4);
        result = buf.dec_ndr_short();
        buf.dec_ndr_short();
        buf.advance(20);     /* transfer syntax / version */
    }
}
