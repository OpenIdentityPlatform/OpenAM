package jcifs.dcerpc.msrpc;

import jcifs.smb.SID;

class LsarSidArrayX extends lsarpc.LsarSidArray {

    LsarSidArrayX(SID[] sids) {
        this.num_sids = sids.length;
        this.sids = new lsarpc.LsarSidPtr[sids.length];
        for (int si = 0; si < sids.length; si++) {
            this.sids[si] = new lsarpc.LsarSidPtr();
            this.sids[si].sid = sids[si];
        }
    }
}
