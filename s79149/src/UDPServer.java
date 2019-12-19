import java.net.*;
import java.util.Arrays;

class UDPServer extends Display {
	// --- constants -------------------
	private static final Integer FILE_NAME_MAX = 255;

	private static final Integer start_packet = 0x001;
	private static final Integer data_packet = 0x010;
	private static final Integer last_packet = 0x100;

	private static final Integer RECV_FILE_DATA_SIZE = 512;
	private static final Integer RECV_DATA_SIZE = 2048;

	// --- variables -------------------
	 // stored vars
	 private static variable stored_session_number = new variable(new byte[2]);

	
	private static file_variable file = new file_variable();
	private static Integer bytes_wrote = 0;
	
	private static Boolean CRC32_is_valid = true;
	
	private static Integer packet_type = 0;
	
	private static variable recv_session_number = new variable(new byte[2]); // random uint_16
	// does not need to be an array, but it's simpler for further calculations, if it is
	private static variable recv_packet_number = new variable(new byte[1]); // 0 or 1
	
	// start packet
	private static variable recv_keyword = new variable(new byte[5]); // String 5
	private static variable recv_file_length = new variable(new byte[8]); // uint_64 = Long.Bytes
	private static variable recv_file_name_length = new variable(new byte[2]); // uint_16
	private static variable recv_file_name = new variable(new byte[0]); // variable from 0 to 255 // TODO: needs to get a check

	// non-start packet
	private static variable recv_file_data = new variable(new byte[RECV_FILE_DATA_SIZE]);
	private static variable recv_check_sum_CRC32 = new variable(new byte[4]);
	private static variable recv_data = new variable(new byte[RECV_DATA_SIZE]); // TODO: calculate
	private static variable send_data = new variable(new byte[0]); // TODO: change to variable


	private static void checkParameter(String args[])
	{
		if (args.length != 1) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 1!");
			System.out.println("Usage: " + "UDPServer" + " Port");
			System.exit(1);
		}

		// TODO: check parameter if it is an integer and if it is a valid port etc.
	}

	private static void checkCRC32() throws Exception
	{
		try {
			variable calc_check_sum_CRC32 = new variable(new byte[4]);
			calc_check_sum_CRC32.setValue(recv_data.calcCRC32(0, recv_data.getPosition()-4), 4, 4);
			Boolean CRC32_is_valid = Arrays.equals(calc_check_sum_CRC32.getValue(), recv_check_sum_CRC32.getValue());
			if (!CRC32_is_valid) {
				System.out.println("CRC32-check failed");
			} else {
				System.out.println("Successfully checked CRC32");
			}
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	private static void last_checkCRC32() throws Exception
	{
		try {
			System.out.println("file length: " + file.getSize());
			if (file.getSize() == 0) {
				throw new Exception("file length is null!");
			}
			
			variable calc_check_sum_CRC32 = new variable(new byte[4]);
			calc_check_sum_CRC32.setValue(file.calcCRC32(), 4, 4);
			Boolean CRC32_is_valid = Arrays.equals(calc_check_sum_CRC32.getValue(), recv_check_sum_CRC32.getValue());
			print("calc: ");
			print(calc_check_sum_CRC32.getValue());
			print("recv: ");
			print(recv_check_sum_CRC32.getValue());
			
			if (!CRC32_is_valid) {
				System.out.println("last CRC32-check failed");
			} else {
				System.out.println("Successfully checked last CRC32");
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static void printData() throws Exception
	{
		try {
			// System.out.println("recv_length: " + recv_data.getSize());
			
			// System.out.println("Session number: " + (recv_session_number.getShort() & 0xffffl));
			// System.out.println("Packet Number: " + recv_packet_number.getByte());
			
			// // sets encoding to US-ASCII
			// // System.setProperty("file.encoding", "US-ASCII");
			
			// System.out.println("keyword: " + recv_keyword.getString());
			
			// System.out.println("file length: " + (recv_file_length.getLong() & 0xffffffffl));
			// System.out.println("file name length: " + (recv_file_name_length.getShort() & 0xffffl));
			// System.out.println("file name: " + recv_file_name.getString());
			// System.out.println("CRC32: " + (recv_check_sum_CRC32.getInt() & 0xffffffffl));
			
			// // TODO: get last packet durch erreichen der file laenge
			
			
			// for (int i = 0; i < recv_data.getSize(); i++) {
			// 	if (recv_data.getValue()[i] != 0)
			// 	System.out.format("i: %d - %x\n", i, recv_data.getValue()[i]);
			// }
		} catch (Exception e) {
			throw e;
		}
	}

	private static class InvalidSessionNumberException extends Exception {}

	public static void parseRecvData() throws Exception
	{
		try {
			// all
			recv_session_number.setValue(recv_data.getBytes(recv_session_number.getSize()));
			recv_packet_number.setValue(recv_data.getBytes(recv_packet_number.getSize()));
			
			if (!has_valid_session_number()) {
				throw new InvalidSessionNumberException();
			}

			// start packet
			if (packet_type == start_packet) {
				recv_keyword.setValue(recv_data.getBytes(recv_keyword.getSize()));
				recv_file_length.setValue(recv_data.getBytes(recv_file_length.getSize()));
				recv_file_name_length.setValue(recv_data.getBytes(recv_file_name_length.getSize()));

				if ((recv_file_name_length.getShort() & 0xffffl) == 0)
				{
					System.out.println("Warning: File Name length is null");
				}
				if ((recv_file_name_length.getShort() & 0xffffl) > FILE_NAME_MAX) {
					throw new Exception("File name is longer than the maximum of " + FILE_NAME_MAX);
				}
				recv_file_name.setValue(new byte[recv_file_name_length.getShort()]);
				recv_file_name.setValue(recv_data.getBytes(recv_file_name.getSize()));

				System.out.println("Recv File name length: " + recv_file_name_length.getShort());
				file.createFile(recv_file_name.getString());
			}

			// non-start packet
			// TODO: rm last_packet here, it is not possible, that packet_type is last_packet here
			if (packet_type == data_packet || packet_type == last_packet) {
				Integer bytes_to_write = 0;
				if (RECV_FILE_DATA_SIZE > recv_file_length.getLong() - bytes_wrote) {
					bytes_to_write = ((int) (long) recv_file_length.getLong())  - bytes_wrote;
					packet_type = last_packet;
				} else {
					bytes_to_write = RECV_FILE_DATA_SIZE;
				}
				recv_file_data.setValue(recv_data.getBytes(bytes_to_write));
				file.write(recv_file_data.getValue());
				System.out.println("Successfully wrote " + bytes_to_write +" bytes of data.");
				bytes_wrote += bytes_to_write;
			}

			// first and last packet
			if (packet_type == start_packet || packet_type == last_packet) {
				recv_check_sum_CRC32.setValue(recv_data.getBytes(recv_check_sum_CRC32.getSize()));
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static void setDataToNull()
	{
		// TODO: replace array sizes with constants
		recv_session_number = new variable(new byte[2]); // random uint_16
		// does not need to be an array, but it's simpler for further calculations, if it is
		recv_packet_number = new variable(new byte[1]); // 0 or 1
		
		// start packet
		recv_keyword = new variable(new byte[5]); // String 5
		recv_file_length = new variable(new byte[8]); // uint_64 = Long.Bytes
		recv_file_name_length = new variable(new byte[2]); // uint_16
		recv_file_name = new variable(new byte[0]); // variable from 0 to 255 // TODO: needs to get a check
	
		// non-start packet
		recv_file_data = new variable(new byte[RECV_FILE_DATA_SIZE]);
	
		recv_check_sum_CRC32 = new variable(new byte[4]);
	
		recv_data = new variable(new byte[RECV_DATA_SIZE]); // TODO: calculate
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

	private static void recvFile(int port) throws Exception
	{
		DatagramSocket socket = new DatagramSocket(port);
		socket.setSoTimeout(10000);

		packet_type = start_packet;
		while (true) {
			// TODO: change to the calculated maximum
			DatagramPacket request = new DatagramPacket(recv_data.getValue(), recv_data.getSize());

			try {
				System.out.println("Start listening...");
				socket.receive(request);
				System.out.println("Packet received");
				
				recv_data.setValue(request.getData());
	
				parseRecvData();
				if (packet_type == start_packet) {
					packet_type = data_packet;
				}
			} catch (InvalidSessionNumberException e) {
				print("Invalid session number received");
				packet_type = start_packet;
				continue;
			} catch (java.net.SocketTimeoutException e) {
				System.out.println("Got timeout, exit.");
				socket.close();
				System.exit(1);
			} catch (Exception e) {
				socket.close();
				throw e;
			}


			if (packet_type == start_packet) {
				print("start packet");
				checkCRC32();
			}
			if (packet_type == last_packet) {
				print("last packet");
				last_checkCRC32();
			}

			recv_data.resetPosition();
			
			// printData();

			send_data.setValue(new byte[0]);

			send_data.append(recv_session_number);
			send_data.append(recv_packet_number);
			
			InetAddress clientHost = request.getAddress();
			int clientPort = request.getPort();
			DatagramPacket reply = new DatagramPacket(recv_data.getValue(), recv_data.getSize(), clientHost, clientPort);
			if (CRC32_is_valid) {
				socket.send(reply);
				System.out.println("Send size: " + send_data.getSize());
				System.out.println("Reply sent");
			} else {
				System.out.println("Reply not sent");
			}
   

			Boolean finished = false;
			// TODO: add functionality to test the length of the file
			if (finished) {
				break;
			}
		}

		socket.close();
	}

    public static void main(String args[]) throws Exception
    {
		checkParameter(args);
		int port = Integer.parseInt(args[0]);

		try {
			recvFile(port);
			file.close();
		} catch (Exception e) {
			throw new Exception(e);
		}

		return;
	}
}