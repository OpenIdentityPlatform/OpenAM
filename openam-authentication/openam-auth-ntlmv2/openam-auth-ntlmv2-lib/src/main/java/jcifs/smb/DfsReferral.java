/* jcifs smb client library in Java
 * Copyright (C) 2003  "Michael B. Allen" <jcifs at samba dot org>
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

package jcifs.smb;

import java.util.Map;

public class DfsReferral extends SmbException {

    public int pathConsumed;
    public long ttl;
    public String server;   // Server
    public String share;    // Share
    public String link;
    public String path;     // Path relative to tree from which this referral was thrown
    public boolean resolveHashes;
    public long expiration;

    DfsReferral next;
    Map map;
    String key = null;

    public DfsReferral()
    {
        this.next = this;
    }

    void append(DfsReferral dr)
    {
        dr.next = next;
        next = dr;
    }

    public String toString() {
        return "DfsReferral[pathConsumed=" + pathConsumed +
            ",server=" + server +
            ",share=" + share +
            ",link=" + link +
            ",path=" + path +
            ",ttl=" + ttl +
            ",expiration=" + expiration +
            ",resolveHashes=" + resolveHashes + "]";
    }
}
