package com.sun.identity.admin;

import com.sun.identity.shared.encode.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class AsciiSerializer {

    public static String serialize(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(o);
        os.flush();
        os.close();

        String es = Base64.encode(baos.toByteArray());

        return es;
    }

    public static Object deserialize(String es) throws IOException, ClassNotFoundException {
        byte[] ba = Base64.decode(es);
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        ObjectInputStream is = new ObjectInputStream(bais);

        Object o = is.readObject();
        is.close();

        return o;
    }
}
