package operato.logis.connector.socket.sync;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
public class SocketServer {
    private int port;
    private ServerSocket socket;
    private Socket connection;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private InputStream inputStream;
    private OutputStream outputStream;

    public SocketServer(int port){
        this.port = port;
    }

    private void socketInit() throws IOException {
        try{
            socket = new ServerSocket(port);
            connection = socket.accept();
            inputStream = connection.getInputStream();
            outputStream = connection.getOutputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            printWriter = new PrintWriter(outputStream);
        }catch(Exception e){
            log.error("[SocketServer] socketInit : {}", e);
            throw e;
        }
    }

    private void socketRelease() throws IOException {
        try{
            if(bufferedReader != null)
                bufferedReader.close();
            if(printWriter != null)
                printWriter.close();
            if(inputStream != null)
                inputStream.close();
            if(outputStream != null)
                outputStream.close();
            if(socket != null)
                socket.close();
            inputStream = null;
            outputStream = null;
            bufferedReader = null;
            printWriter = null;
            connection = null;
            socket = null;
        }catch(Exception e){
            log.error(e.toString());
            throw e;
        }
    }


    public void connect() throws IOException {
        try {
            if(socket != null){
                if(connection != null){
                    if(connection.isClosed()){
                        socketRelease();
                    }else if(connection.isConnected()){
                        log.info("connected ..");
                    }
                }
            }else{
                log.info("try connect ..");
                socketInit();
            }
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
    }

    public String read() throws IOException {
        try {
            if(inputStream != null){
                var readBytes = new byte[4096];
                var readCount = inputStream.read(readBytes);
                if (readCount > 0) {
                    return new String(readBytes, 0, readCount, "UTF-8");
                }else if (readCount == -1){
                    // EOF
                    socketRelease();
                }
            }
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
        return null;
    }

    public void send(String message) throws IOException {
        try {
            if(outputStream != null) {
                outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
    }

    public String readLine() throws IOException {
        try {
            if(bufferedReader != null) {
                return bufferedReader.readLine();
            }
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
        return null;
    }

    public void sendLine(String message){
        try {
            if(printWriter != null){
                printWriter.println(message);
                printWriter.flush();
            }
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
    }
}
