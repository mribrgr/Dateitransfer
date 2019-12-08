public class Display {
    protected static Boolean show_messages = true;
    Display(){}

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
    protected static void print(byte[] array)
    {
        for (int i = 0; i < array.length; i++) {
            print("%x\n", array[i]);
        }
    } 
}