package jcifs.smb;

public interface FileEntry {

    String getName();
    int getType();
    int getAttributes();
    long createTime();
    long lastModified();
    long length();
}
