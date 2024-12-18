import java.io.*;
import java.net.*;
import java.util.*;

public class PacmanServer {
    private static final int PORT = 5000;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private final int MAX_PLAYERS = 2; // 최대 플레이어 수 제한
    private int nextPlayerId = 0; // 고유한 플레이어 ID를 위한 카운터

    public static void main(String[] args) {
        new PacmanServer().startServer();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            clients = Collections.synchronizedList(new ArrayList<>());

            System.out.println("서버가 시작되었으며 포트 " + PORT + "에서 대기 중입니다.");

            while (true) {
                Socket socket = serverSocket.accept();

                synchronized (clients) {
                    if (clients.size() >= MAX_PLAYERS) {
                        // 추가 클라이언트 연결 거부
                        PrintWriter tempOut = new PrintWriter(socket.getOutputStream(), true);
                        tempOut.println("SERVER_FULL");
                        socket.close();
                        System.out.println("연결을 거부했습니다: 서버가 가득 찼습니다.");
                        continue;
                    }

                    ClientHandler clientHandler = new ClientHandler(socket, nextPlayerId++);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();

                    System.out.println("플레이어 " + clientHandler.getPlayerId() + "가(이) 연결되었습니다.");

                    if (clients.size() == MAX_PLAYERS) {
                        // 두 플레이어가 연결되면 게임 시작
                        broadcast("START");
                        System.out.println("두 플레이어가 연결되었습니다. 게임이 시작되었습니다.");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 모든 클라이언트에게 메시지 전송
    private void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    // 클라이언트 연결을 처리하는 내부 클래스
    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int playerId;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // 클라이언트에게 플레이어 ID 전송
                out.println("PLAYER_ID " + playerId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public int getPlayerId() {
            return playerId;
        }

        // 클라이언트에게 메시지 전송
        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            String input;
            try {
                while ((input = in.readLine()) != null) {
                    // 수신된 메시지를 모든 클라이언트에게 브로드캐스트
                    broadcast(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // 클라이언트 연결 해제 처리
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (PacmanServer.this) {
                    clients.remove(this);
                    System.out.println("플레이어 " + playerId + "가(이) 연결을 끊었습니다.");
                }
                // 남은 클라이언트에게 플레이어 연결 해제 알림
                broadcast("PLAYER_DISCONNECTED " + playerId);
            }
        }
    }
}
