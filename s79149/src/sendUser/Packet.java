import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;

/**
 * Packet
 */
public class Packet {
    // Exceptions
    public class DataIsNullException extends RuntimeException {
        DataIsNullException(String msg) {
            super(msg);
        }
    }

    private static final int SESSION_NUMBER_SIZE = 2;
    private static final int PACKET_NUMBER_SIZE = 1;
    private static final int KEYWORD_SIZE = 5;
    private static final int FILE_LENGTH_SIZE = 8;
    private static final int FILE_NAME_LENGTH_SIZE = 2;
    private static final int FILE_NAME_MAX_SIZE = 255;
    private static final int CRC32_SIZE = 4;

    private static variable session_number = new variable(new byte[SESSION_NUMBER_SIZE]);
    private static variable packet_number = new variable(new byte[PACKET_NUMBER_SIZE]);
    private static variable keyword = new variable(new byte[KEYWORD_SIZE]);
    private static variable file_length = new variable(new byte[FILE_LENGTH_SIZE]);
    private static variable file_name_length = new variable(new byte[FILE_NAME_LENGTH_SIZE]);
    private static variable file_name = new variable(new byte[FILE_NAME_MAX_SIZE]);
    private static variable CRC32 = new variable(new byte[CRC32_SIZE]);

    private int size = 0;
    private DatagramPacket datagram;

    // for every var a getter and a setter

    Packet(int size)
    {
        this.size = size;
        this.datagram = new DatagramPacket(new byte[size], size);
    }

    DatagramPacket request()
    {
        return this.datagram;
    }

    void parseData()
    {
        if (this.datagram.getData() == null) {
            throw new DataIsNullException("Data is null");
        }
    }
}