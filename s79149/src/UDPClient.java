import java.io.*;
import java.net.*;

class UDPClient {
	private static void checkParamter(String args[])
	{
		if (args.length != 3) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 3!");
			System.out.println("Usage: " + "UDPClient" + " Hostname Port File");
			System.exit(1);
		}

		// TODO: check parameter if it is an integer and if it is a valid port etc.
	}
	
    public static void main(String args[]) throws Exception
    {
		checkParamter(args);

		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		String file = args[2];



		return;
	}
}