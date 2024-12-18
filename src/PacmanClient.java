import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.Timer;

public class PacmanClient {
    private JFrame frame;
    private JDialog dialog;
    private JButton button;

    private ImageIcon successIcon;
    private ImageIcon smallDotIcon;
    private ImageIcon wallIcon;
    private ImageIcon enemyIcon;
    private ImageIcon yellowPacmanUpIcon;
    private ImageIcon yellowPacmanDownIcon;
    private ImageIcon yellowPacmanLeftIcon;
    private ImageIcon yellowPacmanRightIcon;

    private ImageIcon bluePacmanUpIcon;
    private ImageIcon bluePacmanDownIcon;
    private ImageIcon bluePacmanLeftIcon;
    private ImageIcon bluePacmanRightIcon;

    private ImageIcon emptyIcon;
    private ImageIcon yellowBulletIcon;
    private ImageIcon blueBulletIcon;
    private ImageIcon extraLifeIcon;
    private ImageIcon invincibleIcon;
    private ImageIcon freezeEnemyIcon;

    private JLabel[][] grid;
    private String[][] gridState;

    private JLabel scoreLabel;
    private JLabel bulletsLabel;
    private JLabel livesLabel;

    private Random random;

    private int pacmanRow, pacmanCol;
    private int numOfDots;
    private int dotsEaten;
    private int pacmanDirection;

    private int bullets; // 총알 수

    private Timer enemyTimer;

    private int lives = 3;
    private boolean pacmanInvincible = false;

    private static final int FIELD_ROW_SIZE = 11;
    private static final int FIELD_COL_SIZE = 20;
    private static final int FRAME_WIDTH = 1000;
    private static final int FRAME_HEIGHT = 550;

    // 네트워크 관련 변수
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int playerId;

    // 다른 플레이어의 위치 및 방향 추적
    private int otherPlayerRow = -1, otherPlayerCol = -1;
    private int otherPlayerDirection = 4; // 초기 방향 (오른쪽)

    // 다른 플레이어의 총알을 추적하기 위한 리스트
    private List<Bullet> otherBullets = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PacmanClient().startGame());
    }

    public void startGame() {
        try {
            // 서버에 연결
            socket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 서버로부터 플레이어 ID 수신
            String response = in.readLine();
            if (response != null && response.startsWith("PLAYER_ID")) {
                playerId = Integer.parseInt(response.split(" ")[1]);
                System.out.println("Player ID: " + playerId);
            } else if (response != null && response.equals("SERVER_FULL")) {
                JOptionPane.showMessageDialog(null, "서버가 가득 찼습니다. 나중에 다시 시도해주세요.", "연결 실패", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            } else {
                JOptionPane.showMessageDialog(null, "서버로부터 플레이어 ID를 받지 못했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }

            // 게임 초기화
            initializeGame();

            // 서버 메시지 수신 시작
            new Thread(new ServerListener()).start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "서버에 연결할 수 없습니다.", "연결 오류", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void initializeGame() {
        // 메인 프레임 초기화
        frame = new JFrame("Pacman 게임 - 플레이어 " + playerId);
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // 게임 종료 대화상자 초기화
        dialog = new JDialog(frame, "게임 종료", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        // 아이콘 로드
        successIcon = loadIcon("successIcon.png");
        smallDotIcon = loadIcon("smallDot.png");
        wallIcon = loadIcon("wall.png");
        enemyIcon = loadIcon("ghosts.gif");

        bluePacmanUpIcon = loadIcon("blue_pacman_up.gif");
        bluePacmanDownIcon = loadIcon("blue_pacman_down.gif");
        bluePacmanLeftIcon = loadIcon("blue_pacman_left.gif");
        bluePacmanRightIcon = loadIcon("blue_pacman_right.gif");

        yellowPacmanUpIcon = loadIcon("pacman_up.gif");
        yellowPacmanDownIcon = loadIcon("pacman_down.gif");
        yellowPacmanLeftIcon = loadIcon("pacman_left.gif");
        yellowPacmanRightIcon = loadIcon("pacman_right.gif");

        emptyIcon = loadIcon("empty.png");
        yellowBulletIcon = loadIcon("yellowBullet.png");
        blueBulletIcon = loadIcon("blueBullet.png");
        extraLifeIcon = loadIcon("heart.png");
        invincibleIcon = loadIcon("Super.png");
        freezeEnemyIcon = loadIcon("ICE.png");

        // 게임 변수 초기화
        random = new Random();
        pacmanRow = (playerId == 0) ? 9 : 1;
        pacmanCol = 10;

        numOfDots = 104;
        dotsEaten = 0;
        bullets = 5; // 초기 총알 수 설정 (예: 5)
        pacmanDirection = 4; // 초기 방향: 오른쪽

        // 게임 그리드 초기화
        grid = new JLabel[FIELD_ROW_SIZE][FIELD_COL_SIZE];
        gridState = new String[FIELD_ROW_SIZE][FIELD_COL_SIZE];
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                grid[i][j] = new JLabel(smallDotIcon);
                gridState[i][j] = "dot";
            }
        }

        // 내부 벽 설정 (필요 시)
        // 예시:
        /*
        grid[5][5].setIcon(wallIcon);
        gridState[5][5] = "wall";
        */

        // 게임 패널 설정
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(FIELD_ROW_SIZE, FIELD_COL_SIZE));
        panel.setBackground(Color.BLACK);

        // 그리드 라벨을 패널에 추가
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                panel.add(grid[i][j]);
            }
        }

        // 왼쪽 패널 설정 (점수, 생명, 총알)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayout(3, 1));
        leftPanel.setBackground(Color.BLACK);
        scoreLabel = new JLabel("Score: 0 / " + numOfDots);
        scoreLabel.setForeground(Color.WHITE);
        livesLabel = new JLabel("Lives: " + lives);
        livesLabel.setForeground(Color.WHITE);
        bulletsLabel = new JLabel("Bullets: " + bullets);
        bulletsLabel.setForeground(Color.WHITE);
        leftPanel.add(scoreLabel);
        leftPanel.add(livesLabel);
        leftPanel.add(bulletsLabel);
        frame.add(leftPanel, BorderLayout.WEST);

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);

        // 게임 그리드에 요소 초기화
        initializeGameGrid();

        // Pacman 이동 및 총알 발사를 위한 키 리스너 추가
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_SPACE) {
                    fireBullet();
                } else {
                    handlePacmanMovement(e);
                }
            }
        });

        // 멀티플레이어에서는 적 이동 타이머 비활성화
        enemyTimer = new Timer(250, e -> handleEnemyMovement());
        enemyTimer.start();

        // 게임 종료 대화상자 버튼 설정
        button = new JButton("게임 종료");
        button.addActionListener(e -> {
            // 버튼 클릭 시 게임 종료
            System.exit(0);
        });
    }

    // 아이콘 로드 도우미 메서드 (리소스 폴더 없이 파일 경로 사용)
    private ImageIcon loadIcon(String path) {
        File imgFile = new File(path);
        if (imgFile.exists()) {
            System.out.println("이미지 로드 성공: " + imgFile.getAbsolutePath());
            return new ImageIcon(imgFile.getAbsolutePath());
        } else {
            System.err.println("이미지 로드 실패: " + path + " (현재 디렉토리: " + new File(".").getAbsolutePath() + ")");
            return new ImageIcon(); // null 참조 방지를 위해 빈 아이콘 반환
        }
    }

    // 게임 그리드 초기화
    private void initializeGameGrid() {
        // 외곽 벽 설정
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                if (i == 0 || i == FIELD_ROW_SIZE - 1 || j == 0 || j == FIELD_COL_SIZE - 1) {
                    grid[i][j].setIcon(wallIcon);
                    gridState[i][j] = "wall";
                }
            }
        }

        // 내부 벽 설정 (필요 시)
        // 예시:
        /*
        grid[5][5].setIcon(wallIcon);
        gridState[5][5] = "wall";
        */

        // Pacman을 그리드에 배치
        if (gridState[pacmanRow][pacmanCol].equals("dot")) {
            dotsEaten++;
            bullets++; // 총알 충전
            updateScoreLabel();
            updateBulletsLabel();
        }
        grid[pacmanRow][pacmanCol].setIcon(yellowPacmanRightIcon);
        gridState[pacmanRow][pacmanCol] = "pacman" + playerId;
    }

    private void handlePacmanMovement(KeyEvent e) {
        int key = e.getKeyCode();
        int newRow = pacmanRow;
        int newCol = pacmanCol;
        ImageIcon pacmanCurrentIcon = yellowPacmanRightIcon;

        // 방향 결정 및 아이콘 업데이트
        if (key == KeyEvent.VK_UP) {
            pacmanCurrentIcon = yellowPacmanUpIcon;
            pacmanDirection = 1; // 위
            newRow = pacmanRow - 1;
        } else if (key == KeyEvent.VK_DOWN) {
            pacmanCurrentIcon = yellowPacmanDownIcon;
            pacmanDirection = 2; // 아래
            newRow = pacmanRow + 1;
        } else if (key == KeyEvent.VK_LEFT) {
            pacmanCurrentIcon = yellowPacmanLeftIcon;
            pacmanDirection = 3; // 왼쪽
            newCol = pacmanCol - 1;
        } else if (key == KeyEvent.VK_RIGHT) {
            pacmanCurrentIcon = yellowPacmanRightIcon;
            pacmanDirection = 4; // 오른쪽
            newCol = pacmanCol + 1;
        }

        // 새로운 위치가 유효한지 확인
        if (isValidMove(newRow, newCol)) {
            String nextState = gridState[newRow][newCol];

            if (!nextState.equals("wall")) {
                // 현재 위치에서 Pacman 제거
                grid[pacmanRow][pacmanCol].setIcon(emptyIcon);
                gridState[pacmanRow][pacmanCol] = "empty";

                // 다음 셀 처리
                if (nextState.equals("dot")) {
                    dotsEaten++;
                    bullets++; // 총알 충전
                    updateScoreLabel();
                    updateBulletsLabel();
                } else if (isItem(nextState)) {
                    applyItemEffect(nextState);
                }

                // 새로운 위치에 Pacman 배치
                grid[newRow][newCol].setIcon(pacmanCurrentIcon);
                gridState[newRow][newCol] = "pacman" + playerId;
                pacmanRow = newRow;
                pacmanCol = newCol;

                // 이동 정보를 서버로 전송
                out.println("MOVE " + playerId + " " + pacmanRow + " " + pacmanCol + " " + pacmanDirection);
            }
        }
    }

    private void fireBullet() {
        if (bullets > 0) {
            bullets--;
            updateBulletsLabel();
            // 총알 생성 및 이동
            int bulletRow = pacmanRow;
            int bulletCol = pacmanCol;

            // 발사 방향에 따른 초기 총알 위치 계산
            switch (pacmanDirection) {
                case 1: // 위
                    bulletRow -= 1;
                    break;
                case 2: // 아래
                    bulletRow += 1;
                    break;
                case 3: // 왼쪽
                    bulletCol -= 1;
                    break;
                case 4: // 오른쪽
                    bulletCol += 1;
                    break;
            }

            // 초기 총알 위치가 유효한지 및 벽이 아닌지 확인
            if (!isValidMove(bulletRow, bulletCol) || gridState[bulletRow][bulletCol].startsWith("wall")) {
                // 총알을 발사할 수 없는 경우
                JOptionPane.showMessageDialog(frame, "총알을 발사할 수 없습니다.", "발사 실패", JOptionPane.ERROR_MESSAGE);
                bullets++; // 총알 수를 원래대로 복원
                updateBulletsLabel();
                return;
            }

            // Pacman이 있는 셀에 총알을 발사하지 않도록 추가 확인
            if (gridState[bulletRow][bulletCol].startsWith("pacman")) {
                JOptionPane.showMessageDialog(frame, "총알을 발사할 수 없습니다. Pacman이 있는 셀입니다.", "발사 실패", JOptionPane.ERROR_MESSAGE);
                bullets++; // 총알 수를 원래대로 복원
                updateBulletsLabel();
                return;
            }

            // 총알 생성
            Bullet bullet = new Bullet(playerId, bulletRow, bulletCol, pacmanDirection);
            otherBullets.add(bullet); // 자신의 총알도 otherBullets에 추가
            bullet.start();

            // 총알 발사 정보를 서버로 전송
            out.println("FIRE_BULLET " + playerId + " " + bulletRow + " " + bulletCol + " " + pacmanDirection);
        }
    }

    private void moveBullet(int bulletRow, int bulletCol, int direction, int ownerId) {
        Bullet bullet = new Bullet(ownerId, bulletRow, bulletCol, direction);
        otherBullets.add(bullet);
        bullet.start();
    }

    private void handleEnemyMovement() {
        // 멀티플레이어에서는 적 이동을 서버에서 관리하거나 비활성화
        // 현재는 적 이동이 비활성화되어 있음
    }

    private boolean isValidMove(int row, int col) {
        // 그리드 범위 내인지 확인
        return row >= 0 && row < FIELD_ROW_SIZE && col >= 0 && col < FIELD_COL_SIZE;
    }

    private void updateScoreLabel() {
        // 점수 라벨 업데이트
        scoreLabel.setText("Score: " + dotsEaten + " / " + numOfDots);
    }

    private void updateBulletsLabel() {
        // 총알 라벨 업데이트
        bulletsLabel.setText("Bullets: " + bullets);
    }

    private void updateLivesLabel() {
        // 생명 라벨 업데이트
        livesLabel.setText("Lives: " + lives);
    }

    private ImageIcon getIconForState(String state) {
        switch (state) {
            case "empty":
                return emptyIcon;
            case "wall":
                return wallIcon;
            case "dot":
                return smallDotIcon;
            case "pacman0":
                // Pacman의 현재 방향에 따라 아이콘 설정
                switch (pacmanDirection) {
                    case 1:
                        return yellowPacmanUpIcon;
                    case 2:
                        return yellowPacmanDownIcon;
                    case 3:
                        return yellowPacmanLeftIcon;
                    case 4:
                    default:
                        return yellowPacmanRightIcon;
                }
            case "pacman1":
                // 다른 플레이어의 방향에 따라 아이콘 설정
                switch (otherPlayerDirection) {
                    case 1:
                        return bluePacmanUpIcon;
                    case 2:
                        return bluePacmanDownIcon;
                    case 3:
                        return bluePacmanLeftIcon;
                    case 4:
                    default:
                        return bluePacmanRightIcon;
                }
            case "bullet_yellow":
                return yellowBulletIcon;
            case "bullet_blue":
                return blueBulletIcon;
            case "extraLife":
                return extraLifeIcon;
            case "invincible":
                return invincibleIcon;
            case "freezeEnemy":
                return freezeEnemyIcon;
            default:
                return emptyIcon;
        }
    }

    private ImageIcon getIconForItem(String item) {
        switch (item) {
            case "extraLife":
                return extraLifeIcon;
            case "invincible":
                return invincibleIcon;
            case "freezeEnemy":
                return freezeEnemyIcon;
            default:
                return emptyIcon;
        }
    }

    private String generateRandomItem() {
        String[] items = {"extraLife", "invincible", "freezeEnemy"};
        int index = random.nextInt(items.length);
        return items[index];
    }

    private boolean isItem(String state) {
        return state.equals("extraLife") || state.equals("invincible") || state.equals("freezeEnemy");
    }

    private void applyItemEffect(String item) {
        switch (item) {
            case "extraLife":
                lives++;
                updateLivesLabel();
                break;
            case "invincible":
                pacmanInvincible = true;
                Timer invincibleTimer = new Timer(10000, e -> pacmanInvincible = false);
                invincibleTimer.setRepeats(false);
                invincibleTimer.start();
                break;
            case "freezeEnemy":
                // 다른 플레이어를 얼리는 기능 구현 가능
                break;
        }
    }

    private void resetPacmanPosition() {
        // 현재 위치에서 Pacman 제거
        grid[pacmanRow][pacmanCol].setIcon(emptyIcon);
        gridState[pacmanRow][pacmanCol] = "empty";

        // Pacman의 위치 재설정
        pacmanRow = (playerId == 0) ? 9 : 1;
        pacmanCol = 10;
        grid[pacmanRow][pacmanCol].setIcon(yellowPacmanRightIcon);
        gridState[pacmanRow][pacmanCol] = "pacman" + playerId;
        pacmanDirection = 4;
    }

    // 서버 메시지를 수신하는 스레드
    class ServerListener implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    // 서버 메시지 처리
                    if (message.startsWith("MOVE")) {
                        String[] parts = message.split(" ");
                        if (parts.length < 5) continue; // 잘못된 MOVE 메시지
                        int otherPlayerId = Integer.parseInt(parts[1]);
                        if (otherPlayerId != playerId) {
                            int otherRow = Integer.parseInt(parts[2]);
                            int otherCol = Integer.parseInt(parts[3]);
                            int otherDirection = Integer.parseInt(parts[4]);

                            // 다른 플레이어의 위치 업데이트
                            updateOtherPlayerPosition(otherPlayerId, otherRow, otherCol, otherDirection);
                        }
                    } else if (message.startsWith("FIRE_BULLET")) { // 총알 발사 처리
                        String[] parts = message.split(" ");
                        if (parts.length < 5) continue; // 잘못된 FIRE_BULLET 메시지
                        int firingPlayerId = Integer.parseInt(parts[1]);
                        int bulletRow = Integer.parseInt(parts[2]);
                        int bulletCol = Integer.parseInt(parts[3]);
                        int bulletDirection = Integer.parseInt(parts[4]);

                        if (firingPlayerId != playerId) { // 자신의 총알은 재생성하지 않음
                            moveBullet(bulletRow, bulletCol, bulletDirection, firingPlayerId);
                        }
                    } else if (message.startsWith("WALL_DESTROYED")) { // 벽 파괴 처리
                        String[] parts = message.split(" ");
                        if (parts.length < 3) continue; // 잘못된 WALL_DESTROYED 메시지
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);

                        SwingUtilities.invokeLater(() -> {
                            grid[row][col].setIcon(emptyIcon);
                            gridState[row][col] = "empty";
                        });
                    } else if (message.startsWith("ITEM_CREATED")) { // 아이템 생성 처리
                        String[] parts = message.split(" ");
                        if (parts.length < 4) continue; // 잘못된 ITEM_CREATED 메시지
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);
                        String item = parts[3];

                        SwingUtilities.invokeLater(() -> {
                            grid[row][col].setIcon(getIconForItem(item));
                            gridState[row][col] = item;
                        });
                    } else if (message.startsWith("BULLET_HIT")) { // 총알 타격 처리
                        String[] parts = message.split(" ");
                        if (parts.length < 2) continue; // 잘못된 BULLET_HIT 메시지
                        int targetPlayerId = Integer.parseInt(parts[1]);

                        if (targetPlayerId == playerId) { // 자신의 Pacman이 맞았을 경우
                            lives--;
                            updateLivesLabel();
                            if (lives <= 0) {
                                showGameOverDialog("패배하였습니다!");
                            }
                        } else {
                            // 다른 플레이어가 맞았을 경우 처리 (필요 시 구현)
                            // 예: 다른 플레이어의 생명 감소
                        }
                    } else if (message.equals("START")) {
                        // 게임 시작 신호
                        System.out.println("게임이 시작되었습니다!");
                    } else if (message.startsWith("PLAYER_DISCONNECTED")) {
                        // 플레이어 연결 해제 처리
                        String[] parts = message.split(" ");
                        if (parts.length >= 2) {
                            int disconnectedPlayerId = Integer.parseInt(parts[1]);
                            if (disconnectedPlayerId != playerId) {
                                JOptionPane.showMessageDialog(frame, "플레이어 " + disconnectedPlayerId + "이(가) 연결을 끊었습니다. 승리하였습니다!", "플레이어 연결 해제", JOptionPane.INFORMATION_MESSAGE);
                                System.exit(0);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 다른 플레이어가 발사한 총알을 생성하고 관리
    private void createOtherBullet(int firingPlayerId, int bulletRow, int bulletCol, int bulletDirection) {
        SwingUtilities.invokeLater(() -> {
            Bullet bullet = new Bullet(firingPlayerId, bulletRow, bulletCol, bulletDirection);
            otherBullets.add(bullet);
            bullet.start();
        });
    }

    // 다른 플레이어의 위치 업데이트
    private void updateOtherPlayerPosition(int otherPlayerId, int row, int col, int direction) {
        SwingUtilities.invokeLater(() -> {
            // 이전 위치에서 다른 플레이어 제거
            if (otherPlayerRow >= 0 && otherPlayerCol >= 0) {
                grid[otherPlayerRow][otherPlayerCol].setIcon(emptyIcon);
                gridState[otherPlayerRow][otherPlayerCol] = "empty";
            }

            // 새로운 위치로 업데이트
            otherPlayerRow = row;
            otherPlayerCol = col;
            otherPlayerDirection = direction;

            // 방향에 따른 다른 플레이어의 아이콘 결정
            ImageIcon otherPacmanIcon;
            switch (direction) {
                case 1:
                    otherPacmanIcon = bluePacmanUpIcon;
                    break;
                case 2:
                    otherPacmanIcon = bluePacmanDownIcon;
                    break;
                case 3:
                    otherPacmanIcon = bluePacmanLeftIcon;
                    break;
                case 4:
                default:
                    otherPacmanIcon = bluePacmanRightIcon;
                    break;
            }

            // 그리드에 다른 플레이어 아이콘 설정
            grid[row][col].setIcon(otherPacmanIcon);
            gridState[row][col] = "pacman" + otherPlayerId;
        });
    }

    // 게임 종료 대화상자 표시
    private void showGameOverDialog(String message) {
        SwingUtilities.invokeLater(() -> {
            dialog.setTitle("게임 종료");
            JLabel msgLabel = new JLabel(message, SwingConstants.CENTER);
            msgLabel.setFont(new Font("Arial", Font.BOLD, 16));
            dialog.getContentPane().removeAll();
            dialog.getContentPane().add(msgLabel, BorderLayout.CENTER);
            dialog.getContentPane().add(button, BorderLayout.SOUTH);
            dialog.setVisible(true);
        });
    }

    // 내부 클래스: 총알 관리
    private class Bullet {
        private int ownerId;
        private int row;
        private int col;
        private int direction;
        private Timer timer;
        private String previousState; // 이전 상태 저장
        private ImageIcon bulletIcon;
        private String bulletState; // "bullet_yellow" 또는 "bullet_blue"

        public Bullet(int ownerId, int row, int col, int direction) {
            this.ownerId = ownerId;
            this.row = row;
            this.col = col;
            this.direction = direction;
            this.previousState = gridState[row][col]; // 초기 상태 저장

            // 자신의 총알과 상대방의 총알을 구분하여 아이콘 설정
            if (ownerId == PacmanClient.this.playerId) { // 자신의 총알
                bulletIcon = yellowBulletIcon;
                bulletState = "bullet_yellow";
            } else { // 상대방의 총알
                bulletIcon = blueBulletIcon;
                bulletState = "bullet_blue";
            }
        }

        public void start() {
            // 현재 위치에 총알 표시 (Pacman과 동일 셀에 있을 경우 Pacman을 유지)
            SwingUtilities.invokeLater(() -> {
                if (!gridState[row][col].startsWith("pacman")) { // Pacman이 없는 경우에만 총알 아이콘 설정
                    grid[row][col].setIcon(bulletIcon);
                    gridState[row][col] = bulletState;
                } else {
                    // Pacman과 총알이 같은 셀에 있을 경우, Pacman의 생명 감소 및 총알 제거
                    String[] parts = gridState[row][col].split("pacman");
                    if (parts.length >= 2) {
                        int targetPlayerId = Integer.parseInt(parts[1]);
                        // 서버에 타격 정보 전송
                        out.println("BULLET_HIT " + targetPlayerId);
                        if (targetPlayerId == playerId) {
                            lives--;
                            updateLivesLabel();
                            if (lives <= 0) {
                                showGameOverDialog("패배하였습니다!");
                            }
                        }
                    }
                    stop(); // 총알 제거
                }
            });

            timer = new Timer(100, new ActionListener() {
                int currentRow = row;
                int currentCol = col;
                String previousStateLocal = previousState; // 초기 상태 저장

                @Override
                public void actionPerformed(ActionEvent e) {
                    int nextRow = currentRow;
                    int nextCol = currentCol;

                    // 방향에 따른 다음 위치 계산
                    switch (direction) {
                        case 1: // 위
                            nextRow--;
                            break;
                        case 2: // 아래
                            nextRow++;
                            break;
                        case 3: // 왼쪽
                            nextCol--;
                            break;
                        case 4: // 오른쪽
                            nextCol++;
                            break;
                    }

                    // final 변수로 복사하여 람다에서 사용
                    final int finalRow = nextRow;
                    final int finalCol = nextCol;

                    if (isValidMove(nextRow, nextCol)) {
                        String nextState = gridState[nextRow][nextCol];

                        if (nextState.startsWith("wall")) {
                            // 벽 파괴
                            SwingUtilities.invokeLater(() -> {
                                grid[finalRow][finalCol].setIcon(emptyIcon);
                                gridState[finalRow][finalCol] = "empty";
                            });

                            // 서버에 벽 파괴 정보 전송
                            out.println("WALL_DESTROYED " + finalRow + " " + finalCol);

                            // 30% 확률로 아이템 생성 및 서버에 전송
                            if (random.nextInt(100) < 30) { // 30% 확률
                                String item = generateRandomItem();
                                SwingUtilities.invokeLater(() -> {
                                    grid[finalRow][finalCol].setIcon(getIconForItem(item));
                                    gridState[finalRow][finalCol] = item;
                                });
                                // 서버에 아이템 생성 정보 전송
                                out.println("ITEM_CREATED " + finalRow + " " + finalCol + " " + item);
                            }

                            stop();
                            return;
                        } else if (nextState.startsWith("pacman")) {
                            // 플레이어 타격
                            String[] parts = nextState.split("pacman");
                            if (parts.length < 2) return; // 잘못된 형식
                            int targetPlayerId = Integer.parseInt(parts[1]);

                            // 서버에 타격 정보 전송
                            out.println("BULLET_HIT " + targetPlayerId);

                            if (targetPlayerId == playerId) {
                                lives--;
                                updateLivesLabel();
                                if (lives <= 0) {
                                    showGameOverDialog("패배하였습니다!");
                                }
                            }

                            stop();
                            return;
                        } else if (nextState.equals("empty") || nextState.equals("dot")) {
                            // 총알을 다음 위치로 이동
                            SwingUtilities.invokeLater(() -> {
                                grid[finalRow][finalCol].setIcon(bulletIcon);
                                gridState[finalRow][finalCol] = bulletState;
                            });

                            // 이전 위치의 상태 복원
                            SwingUtilities.invokeLater(() -> {
                                if (!previousStateLocal.startsWith("pacman")) { // Pacman이 없을 경우에만 복원
                                    grid[currentRow][currentCol].setIcon(getIconForState(previousStateLocal));
                                    gridState[currentRow][currentCol] = previousStateLocal;
                                }
                            });

                            // 현재 위치 업데이트
                            currentRow = nextRow;
                            currentCol = nextCol;
                            previousStateLocal = nextState;
                        } else {
                            // 기타 상태: 총알 제거
                            stop();
                        }
                    } else {
                        // 총알이 범위를 벗어남: 이전 상태 복원 및 타이머 중지
                        SwingUtilities.invokeLater(() -> {
                            if (!previousStateLocal.startsWith("pacman")) { // Pacman이 없을 경우에만 복원
                                grid[currentRow][currentCol].setIcon(getIconForState(previousStateLocal));
                                gridState[currentRow][currentCol] = previousStateLocal;
                            }
                        });
                        stop();
                    }
                }
            });
            timer.start();
        }

        public void stop() {
            timer.stop();
            // 현재 위치에서 총알 제거 및 상태 복원
            SwingUtilities.invokeLater(() -> {
                if (isValidMove(row, col)) {
                    String state = gridState[row][col];
                    if (state.equals(bulletState)) { // 현재 총알 상태와 일치할 경우에만 제거
                        grid[row][col].setIcon(emptyIcon);
                        gridState[row][col] = "empty";
                    }
                }
            });
            otherBullets.remove(this);
        }
    }
}
