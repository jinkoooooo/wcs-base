package operato.logis.connector.socket.sync;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class SocketClient {
    private int port;
    private String address;
    private Socket socket;
    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private InputStream inputStream;
    private OutputStream outputStream;

    public SocketClient(String address, int port){
        this.address = address;
        this.port = port;
    }

    private void socketInit() throws IOException {
        try{
            socket = new Socket(address, port);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            printWriter = new PrintWriter(outputStream);
            log.info(address + ":" + port + " INIT COMPLETE");
        }catch(Exception e){
            log.error(e.toString());
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
            socket = null;
        }catch(Exception e){
            log.error(e.toString());
            throw e;
        }
    }

    public boolean isAvailable() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
    public void connect() throws IOException {
        try {
            if(socket != null){
                if(socket.isClosed()){
                    socketRelease();
                }else if(socket.isConnected()){
                    log.info("connected ..");
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

    public void reconnect() throws IOException {
        try {
            log.info("try connect ..");
            socketInit();
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
    }
    public void disconnet() throws IOException {
        try {
            if(socket != null){
                socketRelease();
            }
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
    }
    private String bytesToHexString(byte[] bytes, int length) {
        return IntStream.range(0, length)
                .mapToObj(i -> String.format("%02X", bytes[i])) // 바이트를 2자리 Hex로 변환
                .collect(Collectors.joining(""));
    }


    public byte[] readRawBytes() throws IOException {
        if (inputStream != null) {
            var readBytes = new byte[4096];
            var readCount = inputStream.read(readBytes);
            if (readCount > 0) {
                return Arrays.copyOf(readBytes, readCount);
            } else if (readCount == -1) {
                socketRelease();
            }
        }
        return null;
    }


    /**
     * 바이너리 데이터 처리
     * @return
     * @throws IOException
     */
    public String readByte2HexString() throws IOException {
        try {
            if (inputStream != null) {
                var readBytes = new byte[4096];
                var readCount = inputStream.read(readBytes);
                if (readCount > 0) {
                    return bytesToHexString(readBytes, readCount);
                } else if (readCount == -1) {
                    // EOF
                    socketRelease();
                }
            }
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            throw e;
        }
        return null;
    }

    /**
     * JSON, XML, 등 텍스트 처리
     * @return
     * @throws IOException
     */
    public String readByte2Utf8() throws IOException {
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
            if(outputStream != null){
                outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
    }

    public void send(byte[]  message) throws IOException {
        try {
            if(outputStream != null){
                outputStream.write(message);
                outputStream.flush();
            }
        }catch (Exception e){
            log.error(e.toString());
            throw e;
        }
    }

    public String readLine() throws IOException {
        try {
            if(bufferedReader != null){
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
