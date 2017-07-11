package app;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class SmartCartServiceTest {

    private SmartCartServiceImpl service;

    @Test
    public void name() throws Exception {
        String wrongKey = "FFFFFFFFFFFA";
        try {
            service = new SmartCartServiceImpl(60, wrongKey);
            service.erase();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Authentication error", e.getMessage());
        }
    }

    @Test
    public void test() throws Exception {
        service = new SmartCartServiceImpl(60, "FFFFFFFFFFFF");

        service.erase();

        byte[] read = service.readBytes();
        Assert.assertArrayEquals(new byte[0], read);

        String data = "TEST_DATA_123";
        service.write(data.getBytes());
        Assert.assertEquals("TEST_DATA_123", new String(service.read()));

        service.write("123456789012345".getBytes());
        Assert.assertEquals("123456789012345", new String(service.read()));

        service.write("1".getBytes());
        Assert.assertEquals("1", new String(service.read()));

        service.write("String");
        String s = service.read();
        Assert.assertEquals("String", s);

        try {
            service.write("Too long String!!!");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Data too large. Max size is 15 chars. Size of data buffer = 18", e.getMessage());
        }
    }
}