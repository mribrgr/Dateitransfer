import java.io.*;
import java.net.*;

class UDPClient {
    public static void main(String args[]) throws Exception
    {

		if (args.length != 3) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 3!");
			System.out.println("Usage: " + "UDPClient" + " Hostname Port File");
			return;
		}

		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		String file = args[2];

		// global vars
		int timeout = 1000; // timeout in millisecs
		int MAX_PINGS = 10;

		return;
	}
}