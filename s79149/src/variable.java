import java.nio.ByteBuffer;
import java.util.Random;

class variable {
    private byte[] value = new byte[0];

    variable(byte[] value)
    {
        this.value = value;
    }

    public void setValue(byte[] value)
    {
        this.value = value;
    }
    public byte[] getValue()
    {
        return this.value;
    }
    public Integer getSize()
    {
        return this.value.length;
    }
    public String getString()
    {
        char[] value = new char[this.value.length];
        for (int i = 0; i < this.value.length; i++) {
            value[i] = (char) this.value[i];
        }

        return String.copyValueOf(value);

        // // https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
        // ByteBuffer buffer = ByteBuffer.allocate(1);
        // buffer.put(recv_packet_number);
        // buffer.flip(); //need flip
        // System.out.println("Buffer size: " + buffer.array().length);
    }
    public Short getShort()
    {
        if (this.value.length != Short.BYTES) {
            System.out.println("variable size is not 2");
            System.exit(2);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.value.length);
        buf.put(this.value);
        buf.flip();
        return buf.getShort();
    }
    public Integer getInt()
    {
        // TODO: what happens, if the bytebuf.len is lower than 4? -> java.nio.BufferOverflowException
        if (this.value.length != Integer.BYTES) {
            System.out.println("variable size is not 4");
            System.exit(2);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.value.length);
        buf.put(this.value);
        buf.flip();
        return buf.getInt();
    }
    public Long getLong()
    {
        if (this.value.length != Long.BYTES) {
            System.out.println("variable size is not 8");
            System.exit(2);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.value.length);
        buf.put(this.value);
        buf.flip();
        return buf.getLong();
    }

    public void random()
    {
        Random random = new Random();
		random.nextBytes(this.value);
    }

    // unused? -> maybe  remove later
    private void append(variable var, Integer size)
    {
        // https://www.javatpoint.com/how-to-merge-two-arrays-in-java
        byte[] buf = new byte[this.value.length + size];
        System.arraycopy(this.value, 0, buf, 0, this.value.length);
        this.value = buf;
        System.arraycopy(var.getValue(), 0, this.value, this.value.length - size, size);
    }
    public void append(variable var)
    {
        this.append(var, var.getSize());
    }

    public static void merge(variable var1, variable var2)
    {
        var1.append(var2, var2.getSize());
    }
}