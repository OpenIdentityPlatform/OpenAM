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
import jcifs.dcerpc.ndr.NdrObject;

public abstract class DcerpcMessage extends NdrObject implements DcerpcConstants {

    protected int ptype = -1;
    protected int flags = 0;
    protected int length = 0;
    protected int call_id = 0;
    protected int alloc_hint = 0;
    protected int result = 0;

    public boolean isFlagSet(int flag) {
        return (flags & flag) == flag;
    }
    public void unsetFlag(int flag) {
        flags &= ~flag;
    }
    public void setFlag(int flag) {
        flags |= flag;
    }
    public DcerpcException getResult() {
        if (result != 0)
            return new DcerpcException(result);
        return null;
    }

    void encode_header(NdrBuffer buf) {
        buf.enc_ndr_small(5); /* RPC version */
        buf.enc_ndr_small(0); /* minor version */
        buf.enc_ndr_small(ptype);
        buf.enc_ndr_small(flags);
        buf.enc_ndr_long(0x00000010); /* Little-endian / ASCII / IEEE */
        buf.enc_ndr_short(length);
        buf.enc_ndr_short(0); /* length of auth_value */
        buf.enc_ndr_long(call_id);
    }
    void decode_header(NdrBuffer buf) throws NdrException {
         /* RPC major / minor version */
        if (buf.dec_ndr_small() != 5 || buf.dec_ndr_small() != 0)
            throw new NdrException("DCERPC version not supported");
        ptype = buf.dec_ndr_small();
        flags = buf.dec_ndr_small();
        if (buf.dec_ndr_long() != 0x00000010) /* Little-endian / ASCII / IEEE */
            throw new NdrException("Data representation not supported");
        length = buf.dec_ndr_short();
        if (buf.dec_ndr_short() != 0)
            throw new NdrException("DCERPC authentication not supported");
        call_id = buf.dec_ndr_long();
    }
    public void encode(NdrBuffer buf) throws NdrException {
        int start = buf.getIndex();
        int alloc_hint_index = 0;

        buf.advance(16); /* momentarily skip header */
        if (ptype == 0) { /* Request */
            alloc_hint_index = buf.getIndex();
            buf.enc_ndr_long(0); /* momentarily skip alloc hint */
            buf.enc_ndr_short(0); /* context id */
            buf.enc_ndr_short(getOpnum());
        }

        encode_in(buf);
        length = buf.getIndex() - start;

        if (ptype == 0) {
            buf.setIndex(alloc_hint_index);
            alloc_hint = length - alloc_hint_index;
            buf.enc_ndr_long(alloc_hint);
        }

        buf.setIndex(start);
        encode_header(buf);
        buf.setIndex(start + length);
    }
    public void decode(NdrBuffer buf) throws NdrException {
        decode_header(buf);

        if (ptype != 12 && ptype != 2 && ptype != 3 && ptype != 13)
            throw new NdrException("Unexpected ptype: " + ptype);

        if (ptype == 2 || ptype == 3) { /* Response or Fault */
            alloc_hint = buf.dec_ndr_long();
            buf.dec_ndr_short();        /* context id */
            buf.dec_ndr_short();        /* cancel count */
        }
        if (ptype == 3 || ptype == 13) {               /* Fault */
            result = buf.dec_ndr_long();
        } else {                        /* Bind_ack or Response */
            decode_out(buf);
        }
    }

    public abstract int getOpnum();
    public abstract void encode_in(NdrBuffer buf) throws NdrException;
    public abstract void decode_out(NdrBuffer buf) throws NdrException;
}
