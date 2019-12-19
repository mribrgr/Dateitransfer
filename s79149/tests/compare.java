import java.io.File;
import java.io.FileInputStream;

public class compare {
    private static void checkParameter(String args[])
	{
		if (args.length != 2) {
			System.out.println("Unknown parameter array. Length " + args.length + " != 2!");
			System.out.println("Usage: " + "first_file" + " second_file");
			System.exit(1);
		}
	}
    public static void main(String[] args) throws Exception {
        checkParameter(args);

        String file_name_1 = args[0];
        String file_name_2 = args[1];
        
        File file1 = new File(file_name_1);
        File file2 = new File(file_name_2);
        
        FileInputStream file_input_stream_1 = new FileInputStream(file1);
        FileInputStream file_input_stream_2 = new FileInputStream(file2);

        byte[] buf1 = file_input_stream_1.readAllBytes();
        byte[] buf2 = file_input_stream_2.readAllBytes();

        long file_size_1 = file1.length();
        long file_size_2 = file2.length();

        int min = (file_size_1 < file_size_2) ? (int) file_size_1 : (int) file_size_2;

        int i;

        for (i = 0; i < min; i++) {
            if (buf1[i] != buf2[i]) {
                break;
            }
        }

        if (i == min) {
            System.out.println("files are equal!");
        } else {
            System.out.println("files are not equal! first error at " + i);
        }

        file_input_stream_1.close();
        file_input_stream_2.close();
    }
}