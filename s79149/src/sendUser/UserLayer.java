public class UserLayer {
    private static void checkParameter()
    {

    }
    public static void main(String args[]) throws Exception
    {
        checkParameter();

        String host_name = "localhost";
        Integer port = 3333;
        String file_name = "UserLayer.class";

        FileTransferLayer file_transfer_layer =
         new FileTransferLayer(host_name, port, file_name);
        
        try {
            file_transfer_layer.sendFile();
        } catch (Exception e) {
            throw e;
        }
    }
}