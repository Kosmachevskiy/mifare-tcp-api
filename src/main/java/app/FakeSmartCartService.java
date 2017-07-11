package app;

import javax.smartcardio.CardException;

public class FakeSmartCartService implements SmartCartService {
    String data = "";

    @Override
    public void write(String data) throws CardException {
        this.data = data;
    }

    @Override
    public String read() throws CardException {
        return data;
    }

    @Override
    public void erase() throws CardException {
        data = "";
    }
}
