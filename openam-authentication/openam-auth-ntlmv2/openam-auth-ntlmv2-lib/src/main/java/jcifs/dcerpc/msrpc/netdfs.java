package jcifs.dcerpc.msrpc;

import jcifs.dcerpc.DcerpcMessage;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrLong;
import jcifs.dcerpc.ndr.NdrObject;

public class netdfs {

    public static String getSyntax() {
        return "4fc742e0-4a10-11cf-8273-00aa004ae673:3.0";
    }

    public static final int DFS_VOLUME_FLAVOR_STANDALONE = 0x100;
    public static final int DFS_VOLUME_FLAVOR_AD_BLOB = 0x200;
    public static final int DFS_STORAGE_STATE_OFFLINE = 0x0001;
    public static final int DFS_STORAGE_STATE_ONLINE = 0x0002;
    public static final int DFS_STORAGE_STATE_ACTIVE = 0x0004;
    public static class DfsInfo1 extends NdrObject {

        public String entry_path;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_referent(entry_path, 1);

            if (entry_path != null) {
                _dst = _dst.deferred;
                _dst.enc_ndr_string(entry_path);

            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            int _entry_pathp = _src.dec_ndr_long();

            if (_entry_pathp != 0) {
                _src = _src.deferred;
                entry_path = _src.dec_ndr_string();

            }
        }
    }
    public static class DfsEnumArray1 extends NdrObject {

        public int count;
        public DfsInfo1[] s;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(count);
            _dst.enc_ndr_referent(s, 1);

            if (s != null) {
                _dst = _dst.deferred;
                int _ss = count;
                _dst.enc_ndr_long(_ss);
                int _si = _dst.index;
                _dst.advance(4 * _ss);

                _dst = _dst.derive(_si);
                for (int _i = 0; _i < _ss; _i++) {
                    s[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            count = (int)_src.dec_ndr_long();
            int _sp = _src.dec_ndr_long();

            if (_sp != 0) {
                _src = _src.deferred;
                int _ss = _src.dec_ndr_long();
                int _si = _src.index;
                _src.advance(4 * _ss);

                if (s == null) {
                    if (_ss < 0 || _ss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    s = new DfsInfo1[_ss];
                }
                _src = _src.derive(_si);
                for (int _i = 0; _i < _ss; _i++) {
                    if (s[_i] == null) {
                        s[_i] = new DfsInfo1();
                    }
                    s[_i].decode(_src);
                }
            }
        }
    }
    public static class DfsStorageInfo extends NdrObject {

        public int state;
        public String server_name;
        public String share_name;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(state);
            _dst.enc_ndr_referent(server_name, 1);
            _dst.enc_ndr_referent(share_name, 1);

            if (server_name != null) {
                _dst = _dst.deferred;
                _dst.enc_ndr_string(server_name);

            }
            if (share_name != null) {
                _dst = _dst.deferred;
                _dst.enc_ndr_string(share_name);

            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            state = (int)_src.dec_ndr_long();
            int _server_namep = _src.dec_ndr_long();
            int _share_namep = _src.dec_ndr_long();

            if (_server_namep != 0) {
                _src = _src.deferred;
                server_name = _src.dec_ndr_string();

            }
            if (_share_namep != 0) {
                _src = _src.deferred;
                share_name = _src.dec_ndr_string();

            }
        }
    }
    public static class DfsInfo3 extends NdrObject {

        public String path;
        public String comment;
        public int state;
        public int num_stores;
        public DfsStorageInfo[] stores;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_referent(path, 1);
            _dst.enc_ndr_referent(comment, 1);
            _dst.enc_ndr_long(state);
            _dst.enc_ndr_long(num_stores);
            _dst.enc_ndr_referent(stores, 1);

            if (path != null) {
                _dst = _dst.deferred;
                _dst.enc_ndr_string(path);

            }
            if (comment != null) {
                _dst = _dst.deferred;
                _dst.enc_ndr_string(comment);

            }
            if (stores != null) {
                _dst = _dst.deferred;
                int _storess = num_stores;
                _dst.enc_ndr_long(_storess);
                int _storesi = _dst.index;
                _dst.advance(12 * _storess);

                _dst = _dst.derive(_storesi);
                for (int _i = 0; _i < _storess; _i++) {
                    stores[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            int _pathp = _src.dec_ndr_long();
            int _commentp = _src.dec_ndr_long();
            state = (int)_src.dec_ndr_long();
            num_stores = (int)_src.dec_ndr_long();
            int _storesp = _src.dec_ndr_long();

            if (_pathp != 0) {
                _src = _src.deferred;
                path = _src.dec_ndr_string();

            }
            if (_commentp != 0) {
                _src = _src.deferred;
                comment = _src.dec_ndr_string();

            }
            if (_storesp != 0) {
                _src = _src.deferred;
                int _storess = _src.dec_ndr_long();
                int _storesi = _src.index;
                _src.advance(12 * _storess);

                if (stores == null) {
                    if (_storess < 0 || _storess > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    stores = new DfsStorageInfo[_storess];
                }
                _src = _src.derive(_storesi);
                for (int _i = 0; _i < _storess; _i++) {
                    if (stores[_i] == null) {
                        stores[_i] = new DfsStorageInfo();
                    }
                    stores[_i].decode(_src);
                }
            }
        }
    }
    public static class DfsEnumArray3 extends NdrObject {

        public int count;
        public DfsInfo3[] s;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(count);
            _dst.enc_ndr_referent(s, 1);

            if (s != null) {
                _dst = _dst.deferred;
                int _ss = count;
                _dst.enc_ndr_long(_ss);
                int _si = _dst.index;
                _dst.advance(20 * _ss);

                _dst = _dst.derive(_si);
                for (int _i = 0; _i < _ss; _i++) {
                    s[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            count = (int)_src.dec_ndr_long();
            int _sp = _src.dec_ndr_long();

            if (_sp != 0) {
                _src = _src.deferred;
                int _ss = _src.dec_ndr_long();
                int _si = _src.index;
                _src.advance(20 * _ss);

                if (s == null) {
                    if (_ss < 0 || _ss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    s = new DfsInfo3[_ss];
                }
                _src = _src.derive(_si);
                for (int _i = 0; _i < _ss; _i++) {
                    if (s[_i] == null) {
                        s[_i] = new DfsInfo3();
                    }
                    s[_i].decode(_src);
                }
            }
        }
    }
    public static class DfsInfo200 extends NdrObject {

        public String dfs_name;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_referent(dfs_name, 1);

            if (dfs_name != null) {
                _dst = _dst.deferred;
                _dst.enc_ndr_string(dfs_name);

            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            int _dfs_namep = _src.dec_ndr_long();

            if (_dfs_namep != 0) {
                _src = _src.deferred;
                dfs_name = _src.dec_ndr_string();

            }
        }
    }
    public static class DfsEnumArray200 extends NdrObject {

        public int count;
        public DfsInfo200[] s;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(count);
            _dst.enc_ndr_referent(s, 1);

            if (s != null) {
                _dst = _dst.deferred;
                int _ss = count;
                _dst.enc_ndr_long(_ss);
                int _si = _dst.index;
                _dst.advance(4 * _ss);

                _dst = _dst.derive(_si);
                for (int _i = 0; _i < _ss; _i++) {
                    s[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            count = (int)_src.dec_ndr_long();
            int _sp = _src.dec_ndr_long();

            if (_sp != 0) {
                _src = _src.deferred;
                int _ss = _src.dec_ndr_long();
                int _si = _src.index;
                _src.advance(4 * _ss);

                if (s == null) {
                    if (_ss < 0 || _ss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    s = new DfsInfo200[_ss];
                }
                _src = _src.derive(_si);
                for (int _i = 0; _i < _ss; _i++) {
                    if (s[_i] == null) {
                        s[_i] = new DfsInfo200();
                    }
                    s[_i].decode(_src);
                }
            }
        }
    }
    public static class DfsInfo300 extends NdrObject {

        public int flags;
        public String dfs_name;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(flags);
            _dst.enc_ndr_referent(dfs_name, 1);

            if (dfs_name != null) {
                _dst = _dst.deferred;
                _dst.enc_ndr_string(dfs_name);

            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            flags = (int)_src.dec_ndr_long();
            int _dfs_namep = _src.dec_ndr_long();

            if (_dfs_namep != 0) {
                _src = _src.deferred;
                dfs_name = _src.dec_ndr_string();

            }
        }
    }
    public static class DfsEnumArray300 extends NdrObject {

        public int count;
        public DfsInfo300[] s;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(count);
            _dst.enc_ndr_referent(s, 1);

            if (s != null) {
                _dst = _dst.deferred;
                int _ss = count;
                _dst.enc_ndr_long(_ss);
                int _si = _dst.index;
                _dst.advance(8 * _ss);

                _dst = _dst.derive(_si);
                for (int _i = 0; _i < _ss; _i++) {
                    s[_i].encode(_dst);
                }
            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            count = (int)_src.dec_ndr_long();
            int _sp = _src.dec_ndr_long();

            if (_sp != 0) {
                _src = _src.deferred;
                int _ss = _src.dec_ndr_long();
                int _si = _src.index;
                _src.advance(8 * _ss);

                if (s == null) {
                    if (_ss < 0 || _ss > 0xFFFF) throw new NdrException( NdrException.INVALID_CONFORMANCE );
                    s = new DfsInfo300[_ss];
                }
                _src = _src.derive(_si);
                for (int _i = 0; _i < _ss; _i++) {
                    if (s[_i] == null) {
                        s[_i] = new DfsInfo300();
                    }
                    s[_i].decode(_src);
                }
            }
        }
    }
    public static class DfsEnumStruct extends NdrObject {

        public int level;
        public NdrObject e;

        public void encode(NdrBuffer _dst) throws NdrException {
            _dst.align(4);
            _dst.enc_ndr_long(level);
            int _descr = level;
            _dst.enc_ndr_long(_descr);
            _dst.enc_ndr_referent(e, 1);

            if (e != null) {
                _dst = _dst.deferred;
                e.encode(_dst);

            }
        }
        public void decode(NdrBuffer _src) throws NdrException {
            _src.align(4);
            level = (int)_src.dec_ndr_long();
            _src.dec_ndr_long(); /* union discriminant */
            int _ep = _src.dec_ndr_long();

            if (_ep != 0) {
                if (e == null) { /* YOYOYO */
                    e = new DfsEnumArray1();
                }
                _src = _src.deferred;
                e.decode(_src);

            }
        }
    }
    public static class NetrDfsEnumEx extends DcerpcMessage {

        public int getOpnum() { return 0x15; }

        public int retval;
        public String dfs_name;
        public int level;
        public int prefmaxlen;
        public DfsEnumStruct info;
        public NdrLong totalentries;

        public NetrDfsEnumEx(String dfs_name,
                    int level,
                    int prefmaxlen,
                    DfsEnumStruct info,
                    NdrLong totalentries) {
            this.dfs_name = dfs_name;
            this.level = level;
            this.prefmaxlen = prefmaxlen;
            this.info = info;
            this.totalentries = totalentries;
        }

        public void encode_in(NdrBuffer _dst) throws NdrException {
            _dst.enc_ndr_string(dfs_name);
            _dst.enc_ndr_long(level);
            _dst.enc_ndr_long(prefmaxlen);
            _dst.enc_ndr_referent(info, 1);
            if (info != null) {
                info.encode(_dst);

            }
            _dst.enc_ndr_referent(totalentries, 1);
            if (totalentries != null) {
                totalentries.encode(_dst);

            }
        }
        public void decode_out(NdrBuffer _src) throws NdrException {
            int _infop = _src.dec_ndr_long();
            if (_infop != 0) {
                if (info == null) { /* YOYOYO */
                    info = new DfsEnumStruct();
                }
                info.decode(_src);

            }
            int _totalentriesp = _src.dec_ndr_long();
            if (_totalentriesp != 0) {
                totalentries.decode(_src);

            }
            retval = (int)_src.dec_ndr_long();
        }
    }
}
