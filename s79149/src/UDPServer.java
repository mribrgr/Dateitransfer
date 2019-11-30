import java.io.*;
import java.net.*;

class UDPServer {
	private static void checkParameter(String args[])
	{
		if (args.length != 1) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 1!");
			System.out.println("Usage: " + "UDPServer" + " Port");
			System.exit(1);
		}

		// TODO: check parameter if it is an integer and if it is a valid port etc.
	}

	/**
	 * Gets file from client
	 * @param port port, where to listen
	 * @return String filename
	 * @throws Exception
	 */
	private static String getFile(int port) throws Exception
	{
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

		return;
	}

    public static void main(String args[]) throws Exception
    {
		checkParameter(args);
		int port = Integer.parseInt(args[0]);
		String filename;

		try {
			filename = getFile(port);
			saveFile(filename);
		} catch (Exception e) {
			throw new Exception(e);
		}

		return;
	}
}