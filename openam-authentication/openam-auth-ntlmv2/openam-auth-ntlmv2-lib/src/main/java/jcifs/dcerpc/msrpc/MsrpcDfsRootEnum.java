/* jcifs msrpc client library in Java
 * Copyright (C) 2008  "Michael B. Allen" <jcifs at samba dot org>
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

import jcifs.dcerpc.ndr.NdrLong;
import jcifs.smb.FileEntry;
import jcifs.smb.SmbShareInfo;

public class MsrpcDfsRootEnum extends netdfs.NetrDfsEnumEx {

    public MsrpcDfsRootEnum(String server) {
        super(server, 200, 0xFFFF, new netdfs.DfsEnumStruct(), new NdrLong(0));
        info.level = level;
        info.e = new netdfs.DfsEnumArray200();
        ptype = 0;
        flags = DCERPC_FIRST_FRAG | DCERPC_LAST_FRAG;
    }

    public FileEntry[] getEntries() {
        netdfs.DfsEnumArray200 a200 = (netdfs.DfsEnumArray200)info.e;
        SmbShareInfo[] entries = new SmbShareInfo[a200.count];
        for (int i = 0; i < a200.count; i++) {
            entries[i] = new SmbShareInfo(a200.s[i].dfs_name, 0, null);
        }
        return entries;
    }
}
