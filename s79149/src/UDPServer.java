import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

class UDPServer {
	// --- variables -------------------
	private static byte[] recv_session_number = new byte[2]; // random uint_16
	// does not need to be an array, but it's simpler for further calculations, if it is
	private static byte[] recv_packet_number = new byte[1]; // 0 or 1
	
	// start packet
	private static byte[] recv_keyword = new byte[5]; // String 5
	private static byte[] recv_file_length = new byte[8]; // uint_64 = Long.Bytes
	private static byte[] recv_file_name_length = new byte[2]; // uint_16
	private static byte[] recv_file_name = new byte[0]; // variable from 0 to 255 // TODO: needs to get a check

	// non-start packet
	private static byte[] recv_file_data = new byte[512];

	private static byte[] recv_check_sum_CRC32 = new byte[4];

	private static byte[] recv_data = new byte[1024]; // TODO: calculate


	private static byte[] send_session_number = new byte[2];
	private static byte[] send_packet_number = new byte[1];

	private static byte[] send_data = new byte[1024]; // TODO: change to variable


	private static void checkParameter(String args[])
	{
		if (args.length != 1) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 1!");
			System.out.println("Usage: " + "UDPServer" + " Port");
			System.exit(1);
		}

		// TODO: check parameter if it is an integer and if it is a valid port etc.
	}

	private static void printData()
	{
		Integer currentRecvDataLength = 0;
		// https://www.javatpoint.com/how-to-merge-two-arrays-in-java
		System.arraycopy(recv_data, 0, recv_session_number, currentRecvDataLength, recv_session_number.length);
		currentRecvDataLength += recv_session_number.length;
		System.arraycopy(recv_data, currentRecvDataLength, recv_packet_number, 0, recv_packet_number.length);
		currentRecvDataLength += recv_packet_number.length;

		// start packet
		System.arraycopy(recv_data, currentRecvDataLength, recv_keyword, 0, recv_keyword.length);
		currentRecvDataLength += recv_keyword.length;
		System.arraycopy(recv_data, currentRecvDataLength, recv_file_length, 0, recv_file_length.length);
		currentRecvDataLength += recv_file_length.length;
		System.arraycopy(recv_data, currentRecvDataLength, recv_file_name_length, 0, recv_file_name_length.length);
		currentRecvDataLength += recv_file_name_length.length;

		// TODO: add variable recv_file_name!!!
		// recv_file_name = new byte[];

		System.arraycopy(recv_data, currentRecvDataLength, recv_file_name, 0, recv_file_name.length);
		currentRecvDataLength += recv_file_name.length;

		// non-start packet
		System.arraycopy(recv_data, currentRecvDataLength, recv_file_data, 0, recv_file_data.length);
		currentRecvDataLength += recv_file_data.length;
		// currently throws an exception, because the recv_file_data size needs to get calculated
		 
		// last packet
		// TODO: rm 512
		System.arraycopy(recv_data, currentRecvDataLength, recv_check_sum_CRC32, 0, recv_check_sum_CRC32.length);
		currentRecvDataLength += recv_check_sum_CRC32.length;

		System.out.println("recv_length: " + recv_data.length);
		System.out.println("curr recv length: " + currentRecvDataLength);



		// https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.put(recv_session_number);
		buffer.flip(); //need flip 
		System.out.println("Buffer size: " + buffer.array().length);
		System.out.println("Session number: " + buffer.getShort());

		// https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
		buffer = ByteBuffer.allocate(1);
		buffer.put(recv_packet_number);
		buffer.flip(); //need flip
		System.out.println("Buffer size: " + buffer.array().length);
		System.out.println(String.format("Packet number: %d", buffer.array()[0]));

		// https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
		buffer = ByteBuffer.allocate(5);
		buffer.put(recv_keyword);
		buffer.flip(); //need flip

		// sets encoding to US-ASCII
		// System.setProperty("file.encoding", "US-ASCII");

		System.out.println("Buffer size: " + buffer.array().length);
		System.out.println(String.format("keyword: %c", buffer.getChar()));

		// https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
		buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(recv_file_length);
		buffer.flip(); //need flip
		System.out.println("Buffer size: " + buffer.array().length);
		System.out.println("file length: " + buffer.getLong());

		// https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
		buffer = ByteBuffer.allocate(4);
		buffer.put(recv_check_sum_CRC32);
		buffer.flip(); //need flip
		System.out.println("Buffer size: " + buffer.array().length);
		System.out.println("CRC32: " + buffer.getInt());

		// TODO: get last packet durch erreichen der file laenge


		for (int i = 0; i < recv_data.length; i++) {
			if (recv_data[i] != 0)
				System.out.format("i: %d - %x\n", i, recv_data[i]);
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
			DatagramPacket request = new DatagramPacket(new byte[1024], 1024);

			try {
				socket.receive(request);
			} catch (java.net.SocketTimeoutException e) {
				System.out.println("Got timeout, exit.");
				socket.close();
				System.exit(1);
			} catch (Exception e) {
				throw e;
			}

			recv_data = request.getData();
			
			printData();

			// send_data = ...
   
			InetAddress clientHost = request.getAddress();
			int clientPort = request.getPort();
			DatagramPacket reply = new DatagramPacket(send_data, send_data.length, clientHost, clientPort);
			socket.send(reply);
   
			System.out.println("Reply sent " + j);

			System.out.println(System.currentTimeMillis());
			System.out.println(time_send);

			if (System.currentTimeMillis() - time_send > 5000) {
				break;
			}
		}

		socket.close();

		return "file.file";
	}

	private static void saveFile(String filename) throws Exception
	{
		File file = new File(filename);
		Boolean isAvaiable = file.exists();
		if (file.isDirectory()) {
			throw new Exception("filename matches to a directory");
		}
		Integer filecount = 1;
		
		while (!isAvaiable) {
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