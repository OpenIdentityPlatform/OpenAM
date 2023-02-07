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

import jcifs.dcerpc.msrpc.lsarpc;
import jcifs.dcerpc.msrpc.netdfs;
import jcifs.dcerpc.msrpc.samr;
import jcifs.dcerpc.msrpc.srvsvc;

import java.util.HashMap;
import java.util.Iterator;

public class DcerpcBinding {

    private static HashMap INTERFACES;

    static {
        INTERFACES = new HashMap();
        INTERFACES.put("srvsvc", srvsvc.getSyntax());
        INTERFACES.put("lsarpc", lsarpc.getSyntax());
        INTERFACES.put("samr", samr.getSyntax());
        INTERFACES.put("netdfs", netdfs.getSyntax());
    }

    public static void addInterface(String name, String syntax)
    {
        INTERFACES.put(name, syntax);
    }

    String proto;
    String server;
    String endpoint = null;
    HashMap options = null;
    UUID uuid = null;
    int major;
    int minor;

    DcerpcBinding(String proto, String server) {
        this.proto = proto;
        this.server = server;
    }

    void setOption(String key, Object val) throws DcerpcException {
        if (key.equals("endpoint")) {
            endpoint = val.toString();
            String lep = endpoint.toLowerCase();
            if (lep.startsWith("\\pipe\\")) {
                String iface = (String)INTERFACES.get(lep.substring(6));
                if (iface != null) {
                    int c, p;
                    c = iface.indexOf(':');
                    p = iface.indexOf('.', c + 1);
                    uuid = new UUID(iface.substring(0, c));
                    major = Integer.parseInt(iface.substring(c + 1, p));
                    minor = Integer.parseInt(iface.substring(p + 1));
                    return;
                }
            }
            throw new DcerpcException("Bad endpoint: " + endpoint);
        }
        if (options == null)
            options = new HashMap();
        options.put(key, val);
    }
    Object getOption(String key) {
        if (key.equals("endpoint"))
            return endpoint;
        if (options != null)
            return options.get(key);
        return null;
    }

    public String toString() {
        String ret = proto + ":" + server + "[" + endpoint;
        if (options != null) {
            Iterator iter = options.keySet().iterator();
            while (iter.hasNext()) {
                Object key = iter.next();
                Object val = options.get(key);
                ret += "," + key + "=" + val;
            }
        }
        ret += "]";
        return ret;
    }
}
