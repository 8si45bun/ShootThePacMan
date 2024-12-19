import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PacmanServer {
    private static final int PORT = 5000;
    private ServerSocket serverSocket;
    private ExecutorService pool;

    // 방 이름과 해당 방의 클라이언트 리스트를 관리하는 맵
    private ConcurrentHashMap<String, Room> rooms;

    // 클라이언트 ID 관리
    private int clientIdCounter = 0;

    public static void main(String[] args) {
        new PacmanServer().start();
    }

    public PacmanServer() {
        rooms = new ConcurrentHashMap<>();
        pool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("서버가 시작되었습니다. 포트: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, clientIdCounter++);
                pool.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 모든 클라이언트에게 메시지 전송 (특정 방 내)
    private void broadcastToRoom(Room room, String message) {
        for (ClientHandler client : room.getClients()) {
            client.sendMessage(message);
        }
    }

    // 방 목록을 모든 클라이언트에게 전송
    private void sendRoomList(ClientHandler requester) {
        StringBuilder sb = new StringBuilder();
        sb.append("ROOM_LIST ");
        for (String roomName : rooms.keySet()) {
            sb.append(roomName).append(",");
        }
        // 마지막 쉼표 제거
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }
        requester.sendMessage(sb.toString());
    }

    // 클라이언트 연결을 처리하는 내부 클래스
    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int playerId;
        private Room currentRoom = null;

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
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
                    System.out.println("플레이어 " + playerId + "로부터 수신: " + input);
                    handleCommand(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        private void handleCommand(String command) {
            if (command.startsWith("GET_ROOMS")) {
                sendRoomList(this);
            } else if (command.startsWith("CREATE_ROOM")) {
                String roomName = command.substring("CREATE_ROOM".length()).trim();
                createRoom(roomName);
            } else if (command.startsWith("JOIN_ROOM")) {
                String roomName = command.substring("JOIN_ROOM".length()).trim();
                joinRoom(roomName);
            } else if (command.startsWith("MOVE") || command.startsWith("FIRE_BULLET") || command.startsWith("BULLET_HIT")
                    || command.startsWith("WALL_DESTROYED") || command.startsWith("ITEM_CREATED")) {
                if (currentRoom != null) {
                    broadcastToRoom(currentRoom, command);
                } else {
                    sendMessage("ERROR 방에 참가하지 않았습니다.");
                }
            } else {
                sendMessage("ERROR 알 수 없는 명령입니다.");
            }
        }

        private void createRoom(String roomName) {
            if (roomName.isEmpty()) {
                sendMessage("ERROR 방 이름을 입력해주세요.");
                return;
            }
            if (rooms.containsKey(roomName)) {
                sendMessage("ERROR 이미 존재하는 방 이름입니다.");
                return;
            }
            Room newRoom = new Room(roomName);
            rooms.put(roomName, newRoom);
            joinRoom(roomName);
            sendMessage("ROOM_CREATED " + roomName);
            System.out.println("방 생성: " + roomName + " (플레이어 " + playerId + ")");
        }

        private void joinRoom(String roomName) {
            if (!rooms.containsKey(roomName)) {
                sendMessage("ERROR 존재하지 않는 방입니다.");
                return;
            }
            Room room = rooms.get(roomName);
            synchronized (room) {
                if (room.getClients().size() >= room.getMaxPlayers()) {
                    sendMessage("ERROR 방이 가득 찼습니다.");
                    return;
                }
                if (currentRoom != null) {
                    currentRoom.removeClient(this);
                    broadcastToRoom(currentRoom, "PLAYER_DISCONNECTED " + playerId);
                }
                currentRoom = room;
                room.addClient(this);
                sendMessage("ROOM_JOINED " + roomName);
                broadcastToRoom(currentRoom, "PLAYER_JOINED " + playerId);

                // 방에 필요한 추가 로직 (예: 모든 플레이어가 준비되면 게임 시작)
                if (room.getClients().size() == room.getMaxPlayers()) {
                    broadcastToRoom(room, "START");
                    System.out.println("방 '" + roomName + "'에서 게임 시작");
                }
            }
        }

        private void disconnect() {
            try {
                if (currentRoom != null) {
                    currentRoom.removeClient(this);
                    broadcastToRoom(currentRoom, "PLAYER_DISCONNECTED " + playerId);
                    if (currentRoom.getClients().isEmpty()) {
                        rooms.remove(currentRoom.getRoomName());
                        System.out.println("방 삭제: " + currentRoom.getRoomName());
                    }
                }
                in.close();
                out.close();
                socket.close();
                System.out.println("플레이어 " + playerId + " 연결 해제");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 방을 관리하는 클래스
    class Room {
        private String roomName;
        private List<ClientHandler> clients;
        private final int maxPlayers = 2; // 방당 최대 플레이어 수

        public Room(String roomName) {
            this.roomName = roomName;
            this.clients = new CopyOnWriteArrayList<>();
        }

        public String getRoomName() {
            return roomName;
        }

        public List<ClientHandler> getClients() {
            return clients;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public void addClient(ClientHandler client) {
            clients.add(client);
            System.out.println("플레이어 " + client.getPlayerId() + "가(이) 방 '" + roomName + "'에 참가했습니다.");
        }

        public void removeClient(ClientHandler client) {
            clients.remove(client);
            System.out.println("플레이어 " + client.getPlayerId() + "가(이) 방 '" + roomName + "'에서 나갔습니다.");
        }
    }
}
