import java.net.*;
import java.nio.charset.Charset;

class UDPClient extends Display {
	UDPClient(){super();}

	// --- constants -------------------
	private static final Integer MAX_REPEATS = 10;

	private static final Integer start_packet = 0x001;
	private static final Integer data_packet = 0x010;
	private static final Integer last_packet = 0x100;

	private static final Integer RECV_DATA_SIZE = 3;
	private static final Integer SEND_FILE_DATA_SIZE = 512;


	// --- variables -------------------
	 // stored vars - do not need values
	 private static variable stored_session_number = new variable(new byte[2]);
	 private static variable stored_file_length = new variable(new byte[8]);
	 private static variable stored_file_name_length = new variable(new byte[2]);
	  private static variable stored_file_name = new variable(new byte[0]);
	
	private static Integer packet_type = 0;
	private static Integer bytes_read = 0;

	private static variable send_session_number = new variable(new byte[2]);

	// does not need to be an array, but it's simpler for further calculations, if it is
	private static variable send_packet_number = new variable(new byte[1]);
	
	// start packet
	private static variable send_keyword = new variable(new byte[5]);
	private static variable send_file_length = new variable(new byte[8]); // uint_64 = Long.Bytes
	private static variable send_file_name_length = new variable(new byte[2]); // uint_16
	private static variable send_file_name = new variable(new byte[0]); // variable from 0 to 255 // TODO: needs to get a check

	// non-start packet
	private static variable send_file_data = new variable(new byte[SEND_FILE_DATA_SIZE]);

	private static variable send_check_sum_CRC32 = new variable(new byte[4]);

	private static variable send_data = new variable(new byte[0]);


	private static variable recv_session_number = new variable(new byte[2]);
	private static variable recv_packet_number = new variable(new byte[1]);

	private static variable recv_data = new variable(new byte[RECV_DATA_SIZE]); // TODO: change to variable


	private static file_variable file = new file_variable();
	private static Integer port;


	private static void checkParameter(String args[])
	{
		if (args.length != 3) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 3!");
			System.out.println("Usage: " + "UDPClient" + " Hostname Port File");
			System.exit(1);
		}

		// TODO: check parameter if it is an integer and if it is a valid port etc.
	}

	private static void parseSendData()
	{
		// all
		send_data.append(send_session_number);
		send_data.append(send_packet_number);

		// start packet
		if (packet_type == start_packet) {
			send_data.append(send_keyword);
			send_data.append(send_file_length);
			send_data.append(send_file_name_length);
			send_data.append(send_file_name);

			appendCRC32();
		}

		// non-start packet
		if (packet_type == data_packet || packet_type == last_packet) {
			send_data.append(send_file_data);
		}

		if (packet_type == last_packet) {
			appendCRC32(); // TODO: over complete file!!
		}

	}
	
	private static void appendCRC32()
	{
		print("appendCRC");
		send_check_sum_CRC32.setValue(send_data.calcCRC32(), 4, 4);
		send_data.append(send_check_sum_CRC32);
	}

	private static void configureStartPacket() throws Exception
	{
		try {
			send_session_number.random();
			
			System.out.println("Session number: " + (send_session_number.getShort() & 0xffffl));
			
			// send_packet_number is null-initialized
			
			// https://stackoverflow.com/questions/18571223/how-to-convert-java-string-into-byte
			send_keyword.setValue("Start".getBytes(Charset.forName("US-ASCII")));
			send_file_length.setValue((long) file.getSize());
			send_file_name_length.setValue((short) file.getName().length());
			send_file_name.setValue(file.getName().getBytes(Charset.forName("UTF-8")));

			stored_file_length.setValue(send_file_length.getValue());
			stored_file_name_length.setValue(send_file_name_length.getValue());
			stored_file_name.setValue(send_file_name.getValue());
		} catch (Exception e) {
			throw e;
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

		send_file_data.setValue(getDataBytes());
	}

	protected static byte[] getDataBytes()
	{
		byte[] ret = null;
		Integer bytes_to_read = 0;

		if (SEND_FILE_DATA_SIZE > stored_file_length.getLong() - bytes_read) {
			bytes_to_read = ((int) (long) stored_file_length.getLong()) - bytes_read;
		} else {
			bytes_to_read = SEND_FILE_DATA_SIZE;
		}

		print("bytes to read: " + bytes_to_read);
		file.read(bytes_to_read);
		// send_file_data.setValue(file.getValue()); // too much bytes, only need 512
		// send_file_data.setValue(file.getBytes(bytes_read, bytes_to_read)); now it is the ret value
		ret = file.getBytes(bytes_read, bytes_to_read);

		bytes_read += bytes_to_read;

		System.out.println("Bytes read: " + bytes_read);

		return ret;
	}

	private static void configureLastPacket()
	{
		incrementPacketNumber(send_packet_number);

		// warum habe ich hier keine file data ergaenzt..?
		send_file_data.setValue(getDataBytes());

		// TODO: add end of file data with CRC32 over the file
		send_check_sum_CRC32.setValue(file.calcCRC32(), 4, 4);
		print("send/calcCRC: ");
		print(send_check_sum_CRC32.getValue());
	}

	private static void setDataToNull()
	{
		recv_session_number = new variable(new byte[2]);
		recv_packet_number = new variable(new byte[1]);
		recv_data = new variable(new byte[RECV_DATA_SIZE]); // TODO: change to variable
	}

	private static Boolean has_valid_session_number()
	{
		// if (stored_session_number.getSize() == 0) {
		// 	stored_session_number = recv_session_number;
		// }

		// if (Arrays.equals(stored_session_number.getValue(), recv_session_number.getValue())) {
		// 	System.out.println("Session number isn't valid");
		// 	for (int i = 0; i < stored_session_number.getSize(); i++) {
		// 		System.out.println("Recv: " + recv_session_number.getBytes(i, 1)[0]);
		// 		System.out.println("Stored: " + stored_session_number.getBytes(i, 1)[0]);
		// 	}
		// 	return true;
		// } else {
		// 	return false;
		// }

		return true; // TODO: fix it
	}

	private static class InvalidSessionNumberException extends Exception {}

	private static void parseRecvData() throws Exception
	{
		try {
			recv_session_number.setValue(recv_data.getBytes(recv_session_number.getSize()));
			recv_packet_number.setValue(recv_data.getBytes(recv_packet_number.getSize()));

			recv_data.resetPosition();

			// TODO: check packet number

			if (has_valid_session_number()) {
				// valid session number
			} else {
				throw new InvalidSessionNumberException();
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static void sendFile(String hostname) throws Exception
	{
		Integer timeout = 1000; // timeout in millisecs // TODO: set real timeout
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(timeout);

		InetAddress IPAddress = InetAddress.getByName(hostname);

		final int num_start_packets = 1;
		
		// TODO: was ist, wenn paket schon voll ist, d.h. die CRC nicht mehr mit rein passt?
		final int num_data_packets = (int) (file.getSize()/SEND_FILE_DATA_SIZE) + 1;

		final int packet_max_num = num_start_packets + num_data_packets;
		for (int packet_num = 0; packet_num < packet_max_num; packet_num++) {
			send_data.setValue(new byte[0]);
			if (packet_num == 0) { // start packet
				packet_type = start_packet;
				print("Start packet");
				configureStartPacket();
			} else if (packet_num == packet_max_num - 1) { // last packet
				packet_type = last_packet;
				print("Last packet");
				configureLastPacket();
			} else {
				packet_type = data_packet;
				print("Data packet");
				configureDataPacket();
			}
			parseSendData();
			
			Integer seq;
			for (seq = 0; seq<MAX_REPEATS; seq++) {
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
						System.out.println("Recv data size: " + recv_packet.getData().length);
						parseRecvData();
						break;
					}
				} catch (InvalidSessionNumberException e) {
					System.out.println("InvalidSessionNumber");
					seq--;
					continue;
				} catch (java.net.SocketTimeoutException e) {
					System.out.println("No answer from Server");
				} catch (Exception e) {
					throw new Exception(e);
				}
			}
			if (seq == MAX_REPEATS) {
				System.out.println("Got no answer after 10 retries");
				clientSocket.close();
				System.exit(1);
			} else {
				System.out.println("Successfully send packet with size " + send_data.getSize() + ".");
			}

			// TODO: unsauber, da Long weg-gecastet wird
			if (bytes_read == (int) (long) stored_file_length.getLong()) {
				break;
			}
		}

		// TODO: Auswertung der Zeiten

    	clientSocket.close();
	}
	
    public static void main(String args[]) throws Exception
    {
		checkParameter(args);

		String hostname = args[0];
		port = Integer.parseInt(args[1]);
		String file_name = args[2];

		
		try {
			file.openFile(file_name);
			sendFile(hostname);
			file.close();
		} catch (Exception e) {
			throw new Exception(e);
		}

		return;
	}
}