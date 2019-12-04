import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;

class variable {
    private byte[] value = new byte[0];

    // needed for getBytes
    private static Integer position = 0;

    variable(byte[] value)
    {
        this.value = value;
    }

    public void setValue(byte[] value)
    {
        this.value = value;
    }
    public void setValue(Long value, Integer start, Integer length) throws Exception
    {
        // https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
        buf.putLong(value);

        for (int i = start; i < start+length; i++) {
            if (i-start >= this.value.length) {
                throw new Exception("i out of value array");
            }
            this.value[i-start] = buf.array()[i];
        }
    }
    public void setValue(Long value) throws Exception
    {
        setValue(value, 0, Long.BYTES);
    }
    public void setValue(Short value) throws Exception
    {
        // https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
        ByteBuffer buf = ByteBuffer.allocate(Short.BYTES);
        buf.putShort(value);
        
        if (this.value.length != Short.BYTES) {
            throw new Exception("variable size " + this.value.length + " is not " + Short.BYTES);
        }
        
        this.value = buf.array();
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
    public Short getShort() throws Exception
    {
        if (this.value.length != Short.BYTES) {
            throw new Exception("variable size " + this.value.length + " is not " + Short.BYTES);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.value.length);
        buf.put(this.value);
        buf.flip();
        return buf.getShort();
    }
    public Integer getInt() throws Exception
    {
        // TODO: what happens, if the bytebuf.len is lower than 4? -> java.nio.BufferOverflowException
        if (this.value.length != Integer.BYTES) {
            throw new Exception("variable size " + this.value.length + " is not " + Integer.BYTES);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.value.length);
        buf.put(this.value);
        buf.flip();
        return buf.getInt();
    }
    public Long getLong() throws Exception
    {
        if (this.value.length != Long.BYTES) {
            throw new Exception("variable size " + this.value.length + " is not " + Long.BYTES);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.value.length);
        buf.put(this.value);
        buf.flip();
        return buf.getLong();
    }
    private Byte getByte(Integer position)
    {
        return this.value[position];
    }
    public Byte getByte()
    {
        return getByte(0);
    }
    public byte[] getBytes(Integer start, Integer length) throws Exception
    {
        byte[] buf = new byte[length];
        if (length > this.value.length + start - 1) {
            throw new Exception("Variable size " + this.value.length + " is lower than " + length);
        }

        System.arraycopy(this.value, start, buf, 0, length);
        return buf;
    }
    public byte[] getBytes(Integer length) throws Exception
    {
        position += length;
        return this.getBytes(position - length, length);
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

    /**
     * calculates the CRC32 sum of the value
     * @param Long checksum
     */
    public Long calcCRC32()
    {
        CRC32 checksum = new CRC32();
        checksum.update(this.value);
        return checksum.getValue();
    }
}