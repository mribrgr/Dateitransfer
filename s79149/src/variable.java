import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;

class variable extends Display {
    protected byte[] value = new byte[0];

    // needed for getBytes
    protected static Integer position = 0;

    protected variable(byte[] value)
    {
        this.setValue(value);
    }
    protected variable(){}

    public Integer getPosition()
    {
        return position;
    }
    public void resetPosition()
    {
        position = 0;
    }

    public void setValue(byte[] value)
    {
        this.value = value;
    }
    public void setValue(Long value, Integer start, Integer length)
    {
        // https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
        buf.putLong(value);

        for (int i = start; i < start+length; i++) {
            this.value[i-start] = buf.array()[i];
        }
    }
    public void setValue(Long value)
    {
        setValue(value, 0, Long.BYTES);
    }
    public void setValue(Short value)
    {
        // https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
        ByteBuffer buf = ByteBuffer.allocate(Short.BYTES);
        buf.putShort(value);
        
        if (this.getSize() != Short.BYTES) {
            throw new RuntimeException("variable size " + this.getSize() + " is not " + Short.BYTES);
        }
        
        this.setValue(buf.array());
    }
    // maybe unused?
    public void setValue(Integer value)
    {
        // https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
        buf.putInt(value);
        
        if (this.getSize() != Integer.BYTES) {
            throw new RuntimeException("variable size " + this.getSize() + " is not " + Integer.BYTES);
        }
        
        this.setValue(buf.array());
    }
    public byte[] getValue()
    {
        return this.value;
    }
    public Integer getSize()
    {
        return this.getValue().length;
    }
    public String getString()
    {
        char[] value = new char[this.getSize()];
        for (int i = 0; i < this.getSize(); i++) {
            value[i] = (char) this.getValue()[i];
        }

        return String.copyValueOf(value);
    }
    public Short getShort()
    {
        if (this.getSize() != Short.BYTES) {
            throw new RuntimeException("variable size " + this.getSize() + " is not " + Short.BYTES);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.getSize());
        buf.put(this.getValue());
        buf.flip();
        return buf.getShort();
    }
    public Integer getInt()
    {
        // TODO: what happens, if the bytebuf.len is lower than 4? -> java.nio.BufferOverflowException
        if (this.getSize() != Integer.BYTES) {
            throw new RuntimeException("variable size " + this.getSize() + " is not " + Integer.BYTES);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.getSize());
        buf.put(this.getValue());
        buf.flip();
        return buf.getInt();
    }
    public Long getLong()
    {
        if (this.getSize() != Long.BYTES) {
            throw new RuntimeException("variable size " + this.getSize() + " is not " + Long.BYTES);
        }

        ByteBuffer buf = ByteBuffer.allocate(this.getSize());
        buf.put(this.getValue());
        buf.flip();
        return buf.getLong();
    }
    protected Byte getByte(Integer position)
    {
        return this.getValue()[position];
    }
    public Byte getByte()
    {
        return getByte(0);
    }
    public byte[] getBytes(Integer start, Integer length)
    {
        byte[] buf = new byte[length];
        System.arraycopy(this.getValue(), start, buf, 0, length);
        return buf;
    }
    public byte[] getBytes(Integer length)
    {
        position += length;
        return this.getBytes(position - length, length);
    }

    public void random()
    {
        Random random = new Random();
		random.nextBytes(this.getValue());
    }

    protected void append(byte[] buf, Integer size)
    {
        // https://www.javatpoint.com/how-to-merge-two-arrays-in-java

        // you need to use this.getValue().length here, because for example in file_variable
        //  you use this.getSize for the size of the file which is greater than this.getValue.len
        byte[] buf2 = new byte[this.getValue().length + size];
        System.arraycopy(this.getValue(), 0, buf2, 0, this.getValue().length);
        this.setValue(buf2);
        System.arraycopy(buf, 0, this.getValue(), this.getValue().length - size, size);
    }
    // unused? -> maybe  remove later
    // protected void append(variable var, Integer size)
    // {
    //     // https://www.javatpoint.com/how-to-merge-two-arrays-in-java
    //     byte[] buf = new byte[this.getSize() + size];
    //     System.arraycopy(this.getValue(), 0, buf, 0, this.getSize());
    //     this.getValue() = buf;
    //     System.arraycopy(var.getValue(), 0, this.getValue(), this.getSize() - size, size);
    // }
    public void append(variable var)
    {
        this.append(var.getValue(), var.getSize());
    }

    public static void merge(variable var1, variable var2)
    {
        var1.append(var2.getValue(), var2.getSize());
    }

    /**
     * calculates the CRC32 sum of the value
     * @param Long checksum
     */
    public Long calcCRC32()
    {
        try {
            return calcCRC32(0, this.getSize());
        } catch (Exception e) {
            throw e;
        }
    }
    public Long calcCRC32(Integer start, Integer length)
    {
        print("lenght: " + length);
        try {
            CRC32 checksum = new CRC32();
            checksum.update(this.getBytes(start, length)); // before: length-1

            print("this.getBytes: ");
            print(this.getBytes(start, length));
            return checksum.getValue();
        } catch (Exception e) {
            throw e;
        }
    }
}