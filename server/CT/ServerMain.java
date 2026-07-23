package server.CT;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import server.board.DataStore;

public class ServerMain {
    public static final int PORT = 5000;

    public static void main(String[] args) throws IOException {
        DataStore dataStore = new DataStore();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버 시작: 포트 " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트 연결: " + clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket, dataStore)).start();
            }
        }
    }
}
