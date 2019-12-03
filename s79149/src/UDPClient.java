import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.zip.CRC32;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;

class UDPClient {
	// constants
	private static final Integer MAX_REPEATS = 10;

	private static variable send_session_number = new variable(new byte[2], 2);

	// does not need to be an array, but it's simpler for further calculations, if it is
	private static variable send_packet_number = new variable(new byte[1], 1);
	
	// start packet
	private static variable send_keyword = new variable(new byte[5], 5);
	private static variable send_file_length = new variable(new byte[8], 8); // uint_64 = Long.Bytes
	private static variable send_file_name_length = new variable(new byte[2], 2); // uint_16
	private static variable send_file_name = new variable(new byte[0], 0); // variable from 0 to 255 // TODO: needs to get a check

	// non-start packet
	private static variable send_file_data = new variable(new byte[512], 512);

	private static variable send_check_sum_CRC32 = new variable(new byte[4], 4);

	private static variable send_data = new variable(new byte[0], 0);


	private static variable recv_session_number = new variable(new byte[2], 2);
	private static variable recv_packet_number = new variable(new byte[1], 1);

	private static variable recv_data = new variable(new byte[1024], 1024); // TODO: change to variable


	private static File file;
	private static Integer port;


	private static void checkParamter(String args[])
	{
		if (args.length != 3) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 3!");
			System.out.println("Usage: " + "UDPClient" + " Hostname Port File");
			System.exit(1);
		}

		// TODO: check parameter if it is an integer and if it is a valid port etc.
	}

	private static File getFile(String file_name) throws Exception
	{
		File ret = new File(file_name);

		if (!ret.exists()) {
			throw new Exception(file_name + " does not exists");
		}
		if (!ret.isFile()) {
			throw new Exception(file_name + " is not a file");
		}
		if (!ret.canRead()) {
			System.out.println(file_name + " is not readable");
			if (!ret.setReadable(true)) {
				throw new Exception("Do not have permissions to set " + file_name + " to readable");
			}
		}

		return ret;
	}

	private static void mergeSendData()
	{
		send_data.append(send_session_number);
		send_data.append(send_packet_number);

		// start packet
		send_data.append(send_keyword);
		send_data.append(send_file_length);
		send_data.append(send_file_name_length);
		send_data.append(send_file_name);

		// non-start packet
		send_data.append(send_file_data);
	}
	
	private static void addCRC32()
	{
		System.out.println("Size of send_data: " + send_data.length);

		CRC32 checksum = new CRC32();
		byte[] newArr = new byte[send_data.length - send_check_sum_CRC32.length];

		System.out.println("Size of newArr: " + newArr.length);

		System.arraycopy(send_data, 0, newArr, 0, send_data.length - send_check_sum_CRC32.length);
		checksum.update(newArr);
		send_data = newArr;

		// https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(checksum.getValue());

		System.arraycopy(buffer.array(), buffer.array().length - send_check_sum_CRC32.length, send_check_sum_CRC32, 0, buffer.array().length - send_check_sum_CRC32.length);

		System.out.println("Checksum: " +  checksum.getValue());

		for (int i = 0; i < send_check_sum_CRC32.length; i++) {
			System.out.format("SumVar: %x\n", send_check_sum_CRC32[i]);
		}

		newArr = new byte[send_data.length + send_check_sum_CRC32.length];
		System.arraycopy(send_data, 0, newArr, 0, send_data.length - send_check_sum_CRC32.length);
		send_data = newArr;

		if (send_check_sum_CRC32 != null) {
			System.arraycopy(send_check_sum_CRC32, 0, send_data, send_data.length - send_check_sum_CRC32.length, send_check_sum_CRC32.length);
		}

		System.out.println("Size of send_data: " + send_data.length);

		checksum.reset();
	}

	private static void configureStartPacket()
	{
		send_session_number.random();

		System.out.println("Session number: " + send_session_number.getShort());

		// TODO: add byte[] buf = {(byte) 0};
		send_packet_number[0] = (byte) 0;
		// https://stackoverflow.com/questions/18571223/how-to-convert-java-string-into-byte
		send_keyword = "Start".getBytes(Charset.forName("US-ASCII"));

		// https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES); // 8 Bytes = 64 bit
		buffer.putLong(file.length());
		send_file_length = buffer.array();
		
		buffer = ByteBuffer.allocate(2); // 2 Byte = 16 bit
		buffer.putShort((short) file.getName().length());
		send_file_name_length = buffer.array();

		send_file_name = file.getName().getBytes(Charset.forName("UTF-8"));
	}

	private static void configureDataPacket()
	{
		// TODO: set "old" data to null
		// send_session_number is const
		send_packet_number[0] = (byte) ((send_packet_number[0] + 1) % 2);

		// start_packet data
		// TODO: add bools and change current vars to send_..._data or ..._value
		send_keyword = new byte[0];
		send_file_length = new byte[0];
		send_file_name_length = new byte[0];
		send_file_name = new byte[0];

		// TODO: add file data
	}

	private static void configureLastPacket()
	{
		send_packet_number[0] = (byte) ((send_packet_number[0] + 1) % 2);

		// TODO: add end of file data
	}

	private static void sendFile(String hostname) throws Exception
	{
		Integer timeout = 1000; // timeout in millisecs // TODO: set real timeout
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(timeout);

		InetAddress IPAddress = InetAddress.getByName(hostname);

		Integer packet_num = 3; // TODO: calculate from size of file
		for (Integer i = 0; i < packet_num; i++) {
			if (i == 0) { // start packet
				configureStartPacket();
				mergeSendData();
				addCRC32();
			} else if (i == packet_num - 1) { // evtl. packet_num
				configureLastPacket();
				mergeSendData();
				addCRC32(); // TODO: over complete file!!
			} else {
				configureDataPacket();
				mergeSendData();
			}
			
			Integer seq;
			for (seq = 0; seq<MAX_REPEATS; seq++) {
				System.out.println((byte) send_packet_number[0]);
				// long time_send = System.currentTimeMillis();
	
				DatagramPacket send_packet =
				 new DatagramPacket(send_data, send_data.length, IPAddress, port);
				clientSocket.send(send_packet);

				DatagramPacket recv_packet =
				 new DatagramPacket(recv_data, recv_data.length);
	
				try {
					clientSocket.receive(recv_packet);
					if (recv_packet.getData() == null) {
						throw new Exception("Receive Packet data is null");
					}
					else {
						System.out.println("Recv data: " + recv_packet.getData());
						break;
					}
				} catch (java.net.SocketTimeoutException e) {
					System.out.println("No answer from Server");
				} catch (Exception e) {
					throw new Exception(e);
				}
			}
			if (seq == MAX_REPEATS) {
				throw new Exception("Got no answer after 10 retries");
			}
		}

		// TODO: Auswertung der Zeiten

    	clientSocket.close();
	}
	
    public static void main(String args[]) throws Exception
    {
		checkParamter(args);

		String hostname = args[0];
		port = Integer.parseInt(args[1]);
		String file_name = args[2];

		try {
			file = getFile(file_name);
			sendFile(hostname);
		} catch (Exception e) {
			throw new Exception(e);
		}

		return;
	}
}