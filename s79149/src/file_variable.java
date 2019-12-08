import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class file_variable extends variable {
    protected File file;
    protected FileInputStream file_input_stream;
    protected FileOutputStream file_output_stream;

    protected file_variable()
    {
        super();
    }

    public Long getLength()
    {
        return this.file.length();
    }

    public String getName()
    {
        return file.getName();
    }

    public void read(Integer bytes)
    {
        try {
            if (this.file_input_stream == null) {
                this.file_input_stream = new FileInputStream(this.file);
                System.out.println("only at the beginning");
            }
            byte[] buf = new byte[bytes];
            if (this.value == null) {
                this.value = buf;
            } else {
                append(buf, buf.length);
            }
            this.file_input_stream.read(this.value);
            System.out.println("Read " + bytes + " bytes.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void write(byte[] bytes)
    {
        try {
            if (this.file_output_stream == null) {
                this.file_output_stream = new FileOutputStream(this.file.getName());
            }
            file_output_stream.write(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	public void openFile(String file_name)
	{
		this.file = new File(file_name);

		if (!this.file.exists()) {
			throw new RuntimeException(file_name + " does not exists");
		}
		if (!this.file.isFile()) {
			throw new RuntimeException(file_name + " is not a file");
		}
		if (!this.file.canRead()) {
			System.out.println(file_name + " is not readable");
			if (!this.file.setReadable(true)) {
				throw new RuntimeException("Do not have permissions to set " + file_name + " to readable");
			}
		}
    }

    public void close()
    {
        try {
            if (this.file_input_stream != null) {
                this.file_input_stream.close();
            }
            if (this.file_output_stream != null) {
                this.file_output_stream.close();
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
    
    public void createFile(String file_name)
	{
		File file = new File(file_name);
		Boolean isAvaiable = file.exists();
		if (file.isDirectory()) {
			throw new RuntimeException("file_name matches to a directory");
		}
		Integer filecount = 1;
        
		while (!isAvaiable) {
			file = new File(String.join(file_name, Integer.toString(filecount)));
			isAvaiable = file.exists();
			if (file.isDirectory()) {
				throw new RuntimeException("file_name matches to a directory");
			}
			filecount++;
		}

		// now the file_name is avaiable.
        System.out.println(file_name + " is avaiable.");
        
        this.file = file;

		return;
	}
}