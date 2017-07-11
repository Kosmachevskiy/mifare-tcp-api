package app;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.smartcardio.*;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class SmartCartServiceImpl implements SmartCartService {

    private static final byte KEY_NUMBER = 0;
    private CardChannel channel;
    private Card card;
    private static final KeyType KEY_TYPE = KeyType.A;
    private final byte block;
    private final String key;

    private enum KeyType {
        A, B
    }

    public SmartCartServiceImpl(int block, String key) {
        this.block = (byte) block;
        this.key = key;
    }

    @Override
    public void write(String data) throws CardException {
        write(data.getBytes());
    }

    @Override
    public String read() throws CardException {
        return new String(readBytes());
    }

    void write(byte[] data) throws CardException {
        erase();

        connect();
        authenticate();

        if (data == null)
            throw new RuntimeException("No data to write");
        if (data.length > 15)
            throw new RuntimeException("Data too large. Max size is 15 chars. Size of data buffer = " + data.length);


        // Send data //
        byte[] buffer = new byte[16];
        if (data.length < 16) {
            System.arraycopy(data, 0, buffer, 0, data.length);
        }
        byte receive[] = channel.transmit(new CommandAPDU(0xff, 0xd6, 0x00, block, buffer)).getBytes();


        if (!bytestohex(receive).matches("9000")) {
            throw new RuntimeException("Write error");
        }

        disconnect();
    }

    byte[] readBytes() throws CardException {
        connect();
        authenticate();

        byte receive[];
        CommandAPDU c1 = new CommandAPDU(0xff, 0xb0, 0x00, block, null, 0x00, 0x00, 0x12);
        ResponseAPDU r1 = channel.transmit(c1);
        receive = r1.getBytes();
        int size = calcLength(receive);
        byte[] result = new byte[size];
        System.arraycopy(receive, 0, result, 0, size);

        disconnect();

        return result;
    }

    @Override
    public void erase() throws CardException {
        connect();
        authenticate();
        byte[] blank = new byte[16];
        Arrays.fill(blank, (byte) 0);
        channel.transmit(new CommandAPDU(0xff, 0xd6, 0x00, block, blank));
        disconnect();
    }

    private int calcLength(byte[] data) {
        if (data == null)
            return 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0)
                return i;
        }
        return data.length;
    }

    private void authenticate() throws CardException {
        loadKey(key);

        byte a1[] = new byte[1];
        byte receive[];

        a1[0] = KEY_NUMBER;

        byte data[] = new byte[5];
        data[0] = (byte) 0x1;
        data[1] = (byte) 0x0;
        data[2] = block;
        data[3] = KEY_TYPE == KeyType.A ? (byte) 0x60 : (byte) 0x61;
        data[4] = KEY_NUMBER;
        CommandAPDU c2 = new CommandAPDU(0xff, 0x86, 0x00, 0x00, data);
        ResponseAPDU r2 = channel.transmit(c2);
        receive = r2.getBytes();
        if (!bytestohex(receive).matches("9000")) {
            throw new RuntimeException("Authentication error");
        }
    }

    private void loadKey(String key) {
        byte receive[];
        byte str3[] = hexToBytes(key);
        CommandAPDU c1 = new CommandAPDU(0xff, 0x82, 0x20, KEY_NUMBER, str3);
        ResponseAPDU r1 = null;
        try {
            r1 = channel.transmit(c1);
        } catch (CardException e) {
            throw new RuntimeException("Key loading error.", e);
        }
        receive = r1.getBytes();
        if (!bytestohex(receive).matches("9000")) {
            throw new RuntimeException("Key loading error.");
        }
    }

    private byte[] hexToBytes(String hexString) {
        HexBinaryAdapter adapter = new HexBinaryAdapter();
        byte[] bytes = adapter.unmarshal(hexString);
        return bytes;
    }

    private String bytestohex(byte hexbyte[]) {
        String s = "";
        String s1 = "";
        int n, x;
        for (n = 0; n < hexbyte.length; n++) {
            x = (int) (0x000000FF & hexbyte[n]);  // byte to int conversion
            s = Integer.toHexString(x).toUpperCase();
            if (s.length() == 1) s = "0" + s;
            s1 = s1 + s;
        }
        return s1;
    }

    @SneakyThrows
    private void disconnect() {
        try {
            card.disconnect(false);
        } catch (CardException e) {
            throw new RuntimeException("Disconnection error.", e);
        }
        if (!card.toString().contains("DISCONNECT")) {
            throw new RuntimeException("Disconnection error.");
        }
    }

    @SneakyThrows
    private void connect() throws CardException {

        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        CardTerminal terminal = terminals.get(1);

        String s = "";
        String s1 = "";
        String atr_temp = "";
        String uid_temp = "";
        int atr_byte = 0;
        int n, x;

        Card card = terminal.connect("T=1");

        channel = card.getBasicChannel();
        ATR r2 = channel.getCard().getATR();
        byte atr[] = r2.getBytes();


        for (n = 0; n < atr.length; n++) {
            x = (int) (0x000000FF & atr[n]);  // byte to int conversion
            s = Integer.toHexString(x).toUpperCase();
            if (s.length() == 1) s = "0" + s;
            s1 = s1 + s + " ";

        }
        atr_temp = s1;

        try {
            atr_byte = atr[14];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(e);
        }

        s1 = "";
        CommandAPDU c2 = new CommandAPDU(0xff, 0xCA, 0x00, 0x00, null, 0x00, 0x00, 0x1);
        ResponseAPDU r1 = channel.transmit(c2);
        byte uid[] = r1.getBytes();


        for (n = 0; n < uid.length - 2; n++) {
            x = (int) (0x000000FF & uid[n]);  // byte to int conversion
            s = Integer.toHexString(x).toUpperCase();
            if (s.length() == 1) s = "0" + s;
            s1 = s1 + s + " ";
        }
        uid_temp = s1;

        int card_Type;
        if (atr_byte == 1) {
            card_Type = 1;
        } else if (atr_byte == 2) {
            card_Type = 2;
        } else new RuntimeException("Cart init error. Not supported card.");

        this.card = card;
    }
}
