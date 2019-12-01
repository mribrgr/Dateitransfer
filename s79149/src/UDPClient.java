import java.io.*;
import java.net.*;
import java.util.Random;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;

class UDPClient {
	private static byte[] session_number = new byte[2];
	// does not need to be an array, but it's simpler for further calculations, if it is
	private static byte[] packet_number = new byte[1];
	private static byte[] keyword = new byte[5];
	private static byte[] file_length = new byte[8]; // uint_64 = Long.Bytes
	private static byte[] file_name_length = new byte[2]; // uint_16
	private static byte[] file_name; // variable from 0 to 255 // TODO: needs to get a check
	private static byte[] check_sum_CRC32 = new byte[4];

	private static byte[] sendData;

	/**
	 * CRC32
	 */
	public static class CRC32 {
		private static byte[] checksum;

		public byte[] getChecksum()
		{
			return checksum;
		}

		/**
		 * Calculates Checksum
		 * @param data data, which wants to get checked
		 */
		public void calcChecksum(byte[] data, Integer length)
		{
			/* TODO: add algorithm */
		}
	}

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
		sendData = new byte[
			session_number.length
			+ packet_number.length
			+ keyword.length 
			+ file_length.length
			+ file_name_length.length
			+ file_name.length
			+ check_sum_CRC32.length
		];
		Integer currentSendDataLength = 0;
		// https://www.javatpoint.com/how-to-merge-two-arrays-in-java
		System.arraycopy(session_number, 0, sendData, currentSendDataLength, session_number.length);
		currentSendDataLength += session_number.length;  
		System.arraycopy(packet_number, 0, sendData, currentSendDataLength, packet_number.length);
		currentSendDataLength += packet_number.length;  
		System.arraycopy(keyword, 0, sendData, currentSendDataLength, keyword.length);
		currentSendDataLength += keyword.length;
		System.arraycopy(file_length, 0, sendData, currentSendDataLength, file_length.length);
		currentSendDataLength += file_length.length;
		System.arraycopy(file_name_length, 0, sendData, currentSendDataLength, file_name_length.length);
		currentSendDataLength += file_name_length.length;
		System.arraycopy(file_name, 0, sendData, currentSendDataLength, file_name.length);
	}
	
	private static void addCRC32()
	{
		CRC32 checksum = new CRC32();
		checksum.calcChecksum(sendData, sendData.length - check_sum_CRC32.length);
		check_sum_CRC32 = checksum.getChecksum();
	
		System.arraycopy(check_sum_CRC32, 0, sendData, sendData.length - check_sum_CRC32.length, check_sum_CRC32.length);
	}

	private static void sendFile(String hostname, File file) throws Exception
	{
		Integer timeout = 1000; // timeout in millisecs // TODO: set real timeout
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(timeout);

		InetAddress IPAddress = InetAddress.getByName(hostname);

		byte[] receiveData = new byte[1024]; // TODO: set fix size


		// packet_start
		Random random = new Random();
		random.nextBytes(session_number);
		packet_number[0] = (byte) 0;
		// https://stackoverflow.com/questions/18571223/how-to-convert-java-string-into-byte
		keyword = "Start".getBytes(Charset.forName("UTF-8"));

		// https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES); // 8 Bytes = 64 bit
		buffer.putLong(file.length());
		file_length = buffer.array();
		
		buffer = ByteBuffer.allocate(2); // 2 Byte = 16 bit
		buffer.putLong(file.getName().length());
		file_name_length = buffer.array();

		file_name = file.getName().getBytes(Charset.forName("UTF-8"));


		// at the end
		mergeSendData();
		addCRC32();

		/*
		for (int seq = 0; seq<MAX_PINGS; seq++) {
			long time_send = System.currentTimeMillis();
			String sentence = "PING " + seq + " " + time_send + " \r\n";

			sendData = sentence.getBytes();
				DatagramPacket sendPacket =
			new DatagramPacket(sendData, sendData.length, IPAddress, port);

			clientSocket.send(sendPacket);

			DatagramPacket receivePacket =
			new DatagramPacket(receiveData, receiveData.length);

			clientSocket.receive(receivePacket);

			if (receivePacket.getData() == null) {
				// Packetverlust
			}
			else {
				String receiveSentence =
				new String(receivePacket.getData());
				String[] receiveArgs = receiveSentence.split(" ");

				if (receiveArgs.length != 4) {
					System.out.println("Unknown Receive. Length " + receiveArgs.length + " != 4!");
				}
				else {
					long time_diff = System.currentTimeMillis() - Long.parseLong(receiveArgs[2]);
					System.out.println("Time diff: " + time_diff);
				}
			}
		}
		*/

		// TODO: Auswertung der Zeiten

    	clientSocket.close();
	}
	
    public static void main(String args[]) throws Exception
    {
		checkParamter(args);

		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		String file_name = args[2];

		try {
			File file = getFile(file_name);
			sendFile(hostname, file);
		} catch (Exception e) {
			throw new Exception(e);
		}

		return;
	}
}