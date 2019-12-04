import java.io.*;
import java.net.*;

class UDPServer {
	private static final Integer FILE_NAME_MAX = 255;

	// --- variables -------------------
	private static variable recv_session_number = new variable(new byte[2]); // random uint_16
	// does not need to be an array, but it's simpler for further calculations, if it is
	private static variable recv_packet_number = new variable(new byte[1]); // 0 or 1
	
	// start packet
	private static variable recv_keyword = new variable(new byte[5]); // String 5
	private static variable recv_file_length = new variable(new byte[8]); // uint_64 = Long.Bytes
	private static variable recv_file_name_length = new variable(new byte[2]); // uint_16
	private static variable recv_file_name = new variable(new byte[0]); // variable from 0 to 255 // TODO: needs to get a check

	// non-start packet
	private static variable recv_file_data = new variable(new byte[512]);

	private static variable recv_check_sum_CRC32 = new variable(new byte[4]);

	private static variable recv_data = new variable(new byte[2048]); // TODO: calculate


	private static variable send_session_number = new variable(new byte[2]);
	private static variable send_packet_number = new variable(new byte[1]);

	private static variable send_data = new variable(new byte[1024]); // TODO: change to variable


	private static void checkParameter(String args[])
	{
		if (args.length != 1) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 1!");
			System.out.println("Usage: " + "UDPServer" + " Port");
			System.exit(1);
		}

		// TODO: check parameter if it is an integer and if it is a valid port etc.
	}

	private static void printData() throws Exception
	{
		try {
			System.out.println("recv_length: " + recv_data.getSize());
			
			System.out.println("Session number: " + (recv_session_number.getShort() & 0xffffl));
			System.out.println("Packet Number: " + recv_packet_number.getByte());
			
			// sets encoding to US-ASCII
			// System.setProperty("file.encoding", "US-ASCII");
			
			System.out.println("keyword: " + recv_keyword.getString());
			
			System.out.println("file length: " + (recv_file_length.getLong() & 0xffffffffl));
			System.out.println("file name length: " + (recv_file_name_length.getShort() & 0xffffl));
			System.out.println("file name: " + recv_file_name.getString());
			System.out.println("CRC32: " + (recv_check_sum_CRC32.getInt() & 0xfffffffl));
			
			
			// TODO: get last packet durch erreichen der file laenge
			
			
			for (int i = 0; i < recv_data.getSize(); i++) {
				if (recv_data.getValue()[i] != 0)
				System.out.format("i: %d - %x\n", i, recv_data.getValue()[i]);
			}
		} catch (Exception e) {
			throw e;
		}
	}
		
	public static void mergeRecvData() throws Exception
	{
		try {
			// start packet
			recv_session_number.setValue(recv_data.getBytes(recv_session_number.getSize()));
			recv_packet_number.setValue(recv_data.getBytes(recv_packet_number.getSize()));
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
			
			// non-start packet
			recv_file_data.setValue(recv_data.getBytes(recv_file_data.getSize()));
			
			// last packet
			recv_check_sum_CRC32.setValue(recv_data.getBytes(recv_check_sum_CRC32.getSize()));
			
			// TODO: check checksum
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Gets file from client
	 * @param port port, where to listen
	 * @return String filename
	 * @throws Exception
	 */
	private static String recvFile(int port) throws Exception
	{
		 DatagramSocket socket = new DatagramSocket(port);
		 socket.setSoTimeout(10000);

		long time_send = System.currentTimeMillis();
		for (int j=0; true; j++) {
			// TODO: change to the calculated maximum
			DatagramPacket request = new DatagramPacket(new byte[2048], 2048);

			try {
				System.out.println("Start listening...");
				socket.receive(request);
			} catch (java.net.SocketTimeoutException e) {
				System.out.println("Got timeout, exit.");
				socket.close();
				System.exit(1);
			} catch (Exception e) {
				socket.close();
				throw e;
			}

			recv_data.setValue(request.getData());
			
			mergeRecvData();
			printData();

			// TODO: set send data
			// send_data = ...
   
			InetAddress clientHost = request.getAddress();
			int clientPort = request.getPort();
			DatagramPacket reply = new DatagramPacket(send_data.getValue(), send_data.getSize(), clientHost, clientPort);
			socket.send(reply);
   
			System.out.println("Reply sent " + j);

			System.out.println(System.currentTimeMillis());
			System.out.println(time_send);

			Boolean finished = false;
			// TODO: add functionality
			if (finished) {
				break;
			}
		}

		socket.close();

		return recv_file_name.getString();
	}

	private static void saveFile(String filename) throws Exception
	{
		File file = new File(filename);
		Boolean isAvaiable = file.exists();
		if (file.isDirectory()) {
			throw new Exception("filename matches to a directory");
		}
		Integer filecount = 1;
		
		while (isAvaiable) {
			file = new File(String.join(filename, Integer.toString(filecount)));
			isAvaiable = file.exists();
			if (file.isDirectory()) {
				throw new Exception("filename matches to a directory");
			}
			filecount++;
		}

		// now the filename is avaiable.
		System.out.println(filename + " is avaiable.");

		return;
	}

    public static void main(String args[]) throws Exception
    {
		checkParameter(args);
		int port = Integer.parseInt(args[0]);
		String filename;

		try {
			filename = recvFile(port);
			saveFile(filename);
		} catch (Exception e) {
			throw new Exception(e);
		}

		return;
	}
}