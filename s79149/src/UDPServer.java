import java.net.*;
import java.util.Arrays;
import java.util.Random;

class UDPServer extends Display {
	// --- constants -------------------
	private static final Integer FILE_NAME_MAX = 255;

	private static final Integer start_packet = 0x001;
	private static final Integer data_packet = 0x010;
	private static final Integer last_packet = 0x100;
	private static final Integer safe_last_packet = 0x101;

	private static final Integer RECV_FILE_DATA_SIZE = 512;
	private static final Integer RECV_DATA_SIZE = 2048;

	// --- variables -------------------
	 // stored vars
	 private static variable stored_session_number = new variable(new byte[2]);

	
	private static file_variable file = new file_variable();
	private static Integer bytes_wrote = 0;
	
	private static Boolean CRC32_is_valid = true;

	private static Integer packet_type = 0;
	private static Integer stored_packet_type = 0;
	
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
		if (args.length != 3) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 3!");
			System.out.println("Usage: " + "UDPServer" + " Port PacketLostRate PacketSendDelay");
			System.exit(1);
		}
		
		if (Double.parseDouble(args[1]) < 0 || Double.parseDouble(args[1]) > 1) {
			throw new RuntimeException("Packet lost rate not from 0 to 1");
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
			if (file.getSize() == 0) {
				throw new Exception("file length is null!");
			}
			
			variable calc_check_sum_CRC32 = new variable(new byte[4]);
			calc_check_sum_CRC32.setValue(file.calcCRC32(), 4, 4);
			Boolean CRC32_is_valid = Arrays.equals(calc_check_sum_CRC32.getValue(), recv_check_sum_CRC32.getValue());
			
			if (!CRC32_is_valid) {
				System.out.println("last CRC32-check failed");
			} else {
				// System.out.println("Successfully checked last CRC32");
			}
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

				file.createFile(recv_file_name.getString());
			}

			// non-start packet
			// TODO: rm last_packet here, it is not possible, that packet_type is last_packet here
			if (packet_type == data_packet || packet_type == last_packet || packet_type == safe_last_packet) {
				Integer bytes_to_write = 0;
				if (RECV_FILE_DATA_SIZE > recv_file_length.getLong() - bytes_wrote) {
					bytes_to_write = ((int) (long) recv_file_length.getLong())  - bytes_wrote;
					if (packet_type == data_packet) {
						packet_type = last_packet;
					}
				} else {
					bytes_to_write = RECV_FILE_DATA_SIZE;
				}
				recv_file_data.setValue(recv_data.getBytes(bytes_to_write));
				
				if (packet_type != safe_last_packet) {
					file.write(recv_file_data.getValue());
					bytes_wrote += bytes_to_write;
				}
			}

			// first and last packet
			if (packet_type == start_packet || packet_type == last_packet || packet_type == safe_last_packet) {
				recv_check_sum_CRC32.setValue(recv_data.getBytes(recv_check_sum_CRC32.getSize()));
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static Boolean has_valid_session_number()
	{
		if (stored_session_number.getShort() == 0) {
			stored_session_number = recv_session_number;
		}

		if (Arrays.equals(stored_session_number.getValue(), recv_session_number.getValue())) {
			return true;
		} else {
			return false;
		}
	}

	private static void resetPacketType()
	{
		packet_type = stored_packet_type;
	}

	private static void recvFile(int port, Double packet_lost_rate, int packet_send_delay) throws Exception
	{
		DatagramSocket socket = new DatagramSocket(port);
		socket.setSoTimeout(10000);

		packet_type = start_packet;
		while (true) {
			// TODO: change to the calculated maximum
			DatagramPacket request = new DatagramPacket(recv_data.getValue(), recv_data.getSize());

			Random random = new Random();
			Boolean packet_will_get_send = random.nextInt(Integer.MAX_VALUE) > packet_lost_rate * Integer.MAX_VALUE;	

			try {
				System.out.println("Start listening...");
				socket.receive(request);
				System.out.println("Packet received");
				
				recv_data.setValue(request.getData());
	
				stored_packet_type = packet_type;
				parseRecvData();
				if (packet_type == start_packet) {
					packet_type = data_packet;
				}
			} catch (InvalidSessionNumberException e) {
				print("Invalid session number received");
				resetPacketType();
				continue;
			} catch (java.net.SocketTimeoutException e) {
				print("Got timeout, exit.");
				if (packet_type == safe_last_packet) {
					break;
				} else {
					socket.close();
					System.exit(1);
				}
			} catch (Exception e) {
				socket.close();
				throw e;
			}


/*

Todo: als naechstes muss implementiert werden, dass der Server checkt, welche Pakete
	   er schon hat und welche somit akzeptiert werden. Hierbei muss eingebaut werden,
	   dass nur abwechselnd die Paketnummern erlaubt werden, sodass somit kein Paket
	   doppelt vorhanden ist.
*/


			if (packet_type == start_packet) {
				print("start packet");
				checkCRC32();
			}
			if (packet_type == last_packet || packet_type == safe_last_packet) {
				print("last packet");
				last_checkCRC32();
			}

			recv_data.resetPosition();
			
			send_data.setValue(new byte[0]);

			send_data.append(recv_session_number);
			send_data.append(recv_packet_number);
			
			InetAddress clientHost = request.getAddress();
			int clientPort = request.getPort();
			DatagramPacket reply = new DatagramPacket(recv_data.getValue(), recv_data.getSize(), clientHost, clientPort);
			if (CRC32_is_valid) {
				if (packet_will_get_send) {
					socket.send(reply);
					System.out.println("Reply sent");
				} else {
					print("Sorry, this packet will not be sent.");
					resetPacketType();
				}
			} else {
				System.out.println("Reply not sent because CRC is invalid");
			}

			// The middle expression is neccessary, because otherwise it interupts in the first packet.
			// This expressions lets the algorithm be only for files which are longer than 0 bytes.
			Boolean file_end_reached = (file.getSize() == (int) (long) recv_file_length.getLong()) && file.getSize() != 0 && packet_type == last_packet;
			if (file_end_reached) {
				print("EOF reached");
				print("Waiting for client to send again, if ACK got lost");
				packet_type = safe_last_packet;
				continue;
			}
		}
		socket.close();
	}

    public static void main(String args[]) throws Exception
    {
		checkParameter(args);
		int port = Integer.parseInt(args[0]);
		Double packet_lost_rate = Double.parseDouble(args[1]);
		int packet_send_delay = Integer.parseInt(args[2]);

		try {
			recvFile(port, packet_lost_rate, packet_send_delay);
			file.close();
			print("file \"" + recv_file_name.getString() + "\" completely recieved");
		} catch (Exception e) {
			throw new Exception(e);
		}

		return;
	}
}