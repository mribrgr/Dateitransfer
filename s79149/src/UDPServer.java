import java.io.*;
import java.net.*;

class UDPServer {
    public static void main(String args[]) throws Exception
    {

		if (args.length != 1) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 1!");
			System.out.println("Usage: " + "UDPServer" + " Port");
			return;
		}

		int port = Integer.parseInt(args[0]);

		return;
	}
}