public class FileTransferLayer {
    String start = "start";
    private String host_name;
    private Integer port;
    private String file_name;

    FileTransferLayer(String host_name, Integer port, String file_name)
    {
        this.host_name = host_name;
        this.port = port;
        this.file_name = file_name;
    }

    public void sendFile() throws Exception
    {
        try {
            openFile(file_name);
            
            StopWaitProtocol stop_wait_protocol = new StopWaitProtocol();
            stop_wait_protocol.setConnection(host_name, port);
            
            StartPacket start_packet = new Startpacket();
            stop_wait_protocol.send(start_packet);
            
            
            stop_wait_protocol.endConnection();
            closeFile();
        } catch (Exception e) {
            throw e;
        }
    }
}