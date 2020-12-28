import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sun.identity.authentication.modules.jdbc.MD5Transform;
import com.sun.identity.authentication.spi.AuthLoginException;

public class Test_MD5 {

	@Test
	public void test() throws AuthLoginException {
		assertEquals("1bc29b36f623ba82aaf6724fd3b16718",new MD5Transform().transform("md5"));
	}
}
