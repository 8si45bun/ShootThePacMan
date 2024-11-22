import java.io.*;
import java.net.*;
import java.util.*;

public class PacmanServer {
    private static final int PORT = 5000;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private String[][] gridState;
    private int FIELD_ROW_SIZE = 11;
    private int FIELD_COL_SIZE = 20;

    public static void main(String[] args) {
        new PacmanServer().startServer();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            clients = new ArrayList<>();
            initializeGrid();

            System.out.println("서버가 시작되었습니다.");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, clients.size());
                clients.add(clientHandler);
                new Thread(clientHandler).start();

                if (clients.size() == 2) {
                    // 두 명의 플레이어가 접속하면 게임 시작
                    broadcast("START");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeGrid() {
        gridState = new String[FIELD_ROW_SIZE][FIELD_COL_SIZE];
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                gridState[i][j] = "dot";
            }
        }
        // 벽이나 다른 초기 상태 설정은 필요에 따라 추가하세요.
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

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
                out.println("PLAYER_ID " + playerId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            String input;
            try {
                while ((input = in.readLine()) != null) {
                    // 클라이언트로부터 메시지를 받았을 때 처리
                    broadcast(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
