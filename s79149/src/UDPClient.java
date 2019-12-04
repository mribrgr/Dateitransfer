import java.io.*;
import java.net.*;
import java.nio.charset.Charset;

class UDPClient {
	// constants
	private static final Integer MAX_REPEATS = 10;

	private static variable send_session_number = new variable(new byte[2]);

	// does not need to be an array, but it's simpler for further calculations, if it is
	private static variable send_packet_number = new variable(new byte[1]);
	
	// start packet
	private static variable send_keyword = new variable(new byte[5]);
	private static variable send_file_length = new variable(new byte[8]); // uint_64 = Long.Bytes
	private static variable send_file_name_length = new variable(new byte[2]); // uint_16
	private static variable send_file_name = new variable(new byte[0]); // variable from 0 to 255 // TODO: needs to get a check

	// non-start packet
	private static variable send_file_data = new variable(new byte[512]);

	private static variable send_check_sum_CRC32 = new variable(new byte[4]);

	private static variable send_data = new variable(new byte[0]);


	private static variable recv_session_number = new variable(new byte[2]);
	private static variable recv_packet_number = new variable(new byte[1]);

	private static variable recv_data = new variable(new byte[1024]); // TODO: change to variable


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
	
	private static void addCRC32() throws Exception
	{
		try {
			send_check_sum_CRC32.setValue(send_data.calcCRC32(), 4, 4);
			
			System.out.println("Checksum: " +  (send_check_sum_CRC32.getInt() & 0xffffffffl));
			
			send_data.append(send_check_sum_CRC32);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	private static void configureStartPacket() throws Exception
	{
		try {
			send_session_number.random();
			
			System.out.println("Session number: " + (send_session_number.getShort() & 0xffffl));
			
			// send_packet_number is null-initialized
			
			// https://stackoverflow.com/questions/18571223/how-to-convert-java-string-into-byte
			send_keyword.setValue("Start".getBytes(Charset.forName("US-ASCII")));
			send_file_length.setValue(file.length());
			send_file_name_length.setValue((short) file.getName().length());
			send_file_name.setValue(file.getName().getBytes(Charset.forName("UTF-8")));

			System.out.println("File name: " + send_file_name.getString());
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	private static void incrementPacketNumber(variable var_packet_number)
	{
		byte[] packet_number = new byte[1];
		packet_number[0] = (byte) ((var_packet_number.getByte() + 1) % 2);
		var_packet_number.setValue(packet_number);
	}

	private static void configureDataPacket()
	{
		// send_session_number is const

		incrementPacketNumber(send_packet_number);
		
		// start_packet data
		send_keyword.setValue(new byte[0]);
		send_file_length.setValue(new byte[0]);
		send_file_name_length.setValue(new byte[0]);
		send_file_name.setValue(new byte[0]);

		// TODO: add file data
	}

	private static void configureLastPacket()
	{
		incrementPacketNumber(send_packet_number);

		// TODO: add end of file data with CRC32 over the file
	}

	private static void sendFile(String hostname) throws Exception
	{
		Integer timeout = 1000; // timeout in millisecs // TODO: set real timeout
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(timeout);

		InetAddress IPAddress = InetAddress.getByName(hostname);

		final Integer packet_max_num = 3; // TODO: calculate from size of file
		for (Integer packet_num = 0; packet_num < packet_max_num; packet_num++) {
			if (packet_num == 0) { // start packet
				configureStartPacket();
				mergeSendData();
				addCRC32();
			} else if (packet_num == packet_max_num - 1) { // data packet
				configureLastPacket();
				mergeSendData();
				addCRC32(); // TODO: over complete file!!
			} else {
				configureDataPacket();
				mergeSendData();
			}
			
			Integer seq;
			for (seq = 0; seq<MAX_REPEATS; seq++) {
				System.out.println((byte) send_packet_number.getByte());
				// long time_send = System.currentTimeMillis();
	
				DatagramPacket send_packet =
				 new DatagramPacket(send_data.getValue(), send_data.getSize(), IPAddress, port);
				clientSocket.send(send_packet);

				DatagramPacket recv_packet =
				 new DatagramPacket(recv_data.getValue(), recv_data.getSize());
	
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