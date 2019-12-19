public class Display {
    protected static Boolean show_messages = true;
    Display(){
        super();
    }

    protected static void print(java.lang.String format, java.lang.Object... args)
    {
        if (show_messages) {
            System.out.format(format, args);
        }
    }
    protected static void print(java.lang.String string)
    {
        if (show_messages) {
            System.out.println(string);
        }
    }
    protected static void print(variable var)
    {
        print(var.getString());
    }
    protected static void print(Integer number)
    {
        print(Integer.toString(number));
    }
    /**
     * prints an byte array without printing the same line more than twice in a row
     * @param array
     */
    protected static void print(byte[] array)
    {
        int count = 0;
        byte old = 0x00;
        for (int i = 0; i < array.length; i++) {
            if (i > 0 && old == array[i]) {
                count++;
            } else {
                if (count>2) {
                    print("%x       %d times\n", old, count-1);
                } else if (count==2) {
                    print("%x\n", old);
                }
                count = 1;
                print("%x\n", array[i]);
            }
            if (i != array.length - 1) {
                old = array[i];
            }
        }
        if (count>2) {
            print("%x       %d times\n", old, count-1);
        } else if (count==2) {
            print("%x\n", old);
        }
    } 
}