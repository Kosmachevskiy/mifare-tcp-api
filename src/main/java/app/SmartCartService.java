package app;

import javax.smartcardio.CardException;

/**
 * @author Konstantin Kosmachevskiy
 */
public interface SmartCartService {
    void write(String data) throws CardException;

    String read() throws CardException;

    void erase() throws CardException;
}
