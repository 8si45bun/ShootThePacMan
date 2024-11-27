import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.Random;

public class PacmanClient {
    private JFrame frame;
    private JDialog dialog;
    private JButton button;

    private ImageIcon successIcon;
    private ImageIcon smallDotIcon;
    private ImageIcon wallIcon;
    private ImageIcon enemyIcon;
    private ImageIcon pacmanUpIcon;
    private ImageIcon pacmanDownIcon;
    private ImageIcon pacmanLeftIcon;
    private ImageIcon pacmanRightIcon;
    private ImageIcon emptyIcon;
    private ImageIcon bulletIcon;
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
    private int enemyRow, enemyCol;
    private int numOfDots;
    private int dotsEaten;
    private int direction;
    private int startDelay;
    private String tempState;

    private int bullets;
    private int pacmanDirection;

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

    public static void main(String[] args) {
        new PacmanClient().startGame();
    }

    public void startGame() {
        try {
            // 서버에 연결
            socket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 서버로부터 플레이어 ID 수신
            String response = in.readLine();
            if (response.startsWith("PLAYER_ID")) {
                playerId = Integer.parseInt(response.split(" ")[1]);
                System.out.println("플레이어 ID: " + playerId);
            }

            // 게임 초기화
            initializeGame();

            // 서버로부터 메시지 수신
            new Thread(new ServerListener()).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeGame() {
        // 메인 프레임 초기화
        frame = new JFrame("Pacman Game - Player " + playerId);
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 게임 오버 또는 성공 시 표시되는 다이얼로그 초기화
        dialog = new JDialog(frame, "게임 종료", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(false);

        // 아이콘 로드
        successIcon = new ImageIcon("successIcon.png");
        smallDotIcon = new ImageIcon("smallDot.png");
        wallIcon = new ImageIcon("wall.png");
        enemyIcon = new ImageIcon("ghosts.gif");
        pacmanUpIcon = new ImageIcon("pacman_up.gif");
        pacmanDownIcon = new ImageIcon("pacman_down.gif");
        pacmanLeftIcon = new ImageIcon("pacman_left.gif");
        pacmanRightIcon = new ImageIcon("pacman_right.gif");
        emptyIcon = new ImageIcon("empty.png");
        bulletIcon = new ImageIcon("yellowBullet.png");
        extraLifeIcon = new ImageIcon("heart.png");
        invincibleIcon = new ImageIcon("Super.png");
        freezeEnemyIcon = new ImageIcon("ICE.png");

        // 게임 변수 초기화
        random = new Random();
        pacmanRow = (playerId == 0) ? 9 : 1;
        pacmanCol = (playerId == 0) ? 10 : 10;

        enemyRow = 5;
        enemyCol = 9;

        numOfDots = 104;
        dotsEaten = 0;
        startDelay = 2;
        tempState = "empty";
        bullets = 0;
        pacmanDirection = 4;

        // 게임 그리드 초기화
        grid = new JLabel[FIELD_ROW_SIZE][FIELD_COL_SIZE];
        gridState = new String[FIELD_ROW_SIZE][FIELD_COL_SIZE];
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                grid[i][j] = new JLabel(smallDotIcon);
                gridState[i][j] = "dot";
            }
        }

        // 내부 벽 설정
        // ... (기존 내부 벽 설정 코드)

        // 게임 패널 설정
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(FIELD_ROW_SIZE, FIELD_COL_SIZE));
        panel.setBackground(Color.BLACK);

        // 그리드 레이블을 패널에 추가
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                panel.add(grid[i][j]);
            }
        }

        // 좌측 패널 설정 (점수 및 목숨 표시)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayout(3, 1));
        leftPanel.setBackground(Color.BLACK);
        scoreLabel = new JLabel("점수: 0 / " + numOfDots);
        scoreLabel.setForeground(Color.WHITE);
        livesLabel = new JLabel("목숨: " + lives);
        livesLabel.setForeground(Color.WHITE);
        bulletsLabel = new JLabel("총알: " + bullets);
        bulletsLabel.setForeground(Color.WHITE);
        leftPanel.add(scoreLabel);
        leftPanel.add(livesLabel);
        leftPanel.add(bulletsLabel);
        frame.add(leftPanel, BorderLayout.WEST);

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);

        // 그리드에 게임 요소 초기화
        initializeGameGrid();

        // 팩맨 이동을 위한 키 리스너 추가
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

        // 적의 이동을 위한 타이머 설정
        enemyTimer = new Timer(250, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleEnemyMovement();
            }
        });
        enemyTimer.start();

        // 다이얼로그 버튼 설정
        button = new JButton("게임 종료");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 버튼 클릭 시 게임 종료
                System.exit(0);
            }
        });
    }

    // 블럭 배치 (내부 벽 설정은 기존 코드와 동일하므로 생략)
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

        // 내부 벽 설정
        // ... (기존 내부 벽 설정 코드)

        // 팩맨 배치
        grid[pacmanRow][pacmanCol].setIcon(pacmanRightIcon);
        gridState[pacmanRow][pacmanCol] = "pacman" + playerId;

        // 팩맨 시작 위치의 점 제거
        if (gridState[pacmanRow][pacmanCol].equals("dot")) {
            grid[pacmanRow][pacmanCol].setIcon(pacmanRightIcon);
            gridState[pacmanRow][pacmanCol] = "pacman" + playerId;
            numOfDots--;
        }
    }

    private void handlePacmanMovement(KeyEvent e) {
        int key = e.getKeyCode();
        int newRow = pacmanRow;
        int newCol = pacmanCol;
        ImageIcon pacmanCurrentIcon = pacmanRightIcon;

        // 방향 결정 및 아이콘 업데이트
        if (key == KeyEvent.VK_UP) {
            pacmanCurrentIcon = pacmanUpIcon;
            pacmanDirection = 1; // 위쪽
            newRow = pacmanRow - 1;
        } else if (key == KeyEvent.VK_DOWN) {
            pacmanCurrentIcon = pacmanDownIcon;
            pacmanDirection = 2; // 아래쪽
            newRow = pacmanRow + 1;
        } else if (key == KeyEvent.VK_LEFT) {
            pacmanCurrentIcon = pacmanLeftIcon;
            pacmanDirection = 3; // 왼쪽
            newCol = pacmanCol - 1;
        } else if (key == KeyEvent.VK_RIGHT) {
            pacmanCurrentIcon = pacmanRightIcon;
            pacmanDirection = 4; // 오른쪽
            newCol = pacmanCol + 1;
        }

        // 현재 위치의 팩맨 아이콘 업데이트
        grid[pacmanRow][pacmanCol].setIcon(pacmanCurrentIcon);
        gridState[pacmanRow][pacmanCol] = "pacman" + playerId;

        // 이동 가능 여부 확인
        if (isValidMove(newRow, newCol)) {
            String nextState = gridState[newRow][newCol];

            if (!nextState.equals("wall")) {
                // 팩맨 이동
                grid[pacmanRow][pacmanCol].setIcon(emptyIcon);
                gridState[pacmanRow][pacmanCol] = "empty";

                if (nextState.equals("dot")) {
                    dotsEaten++;
                    bullets++; // 총알 충전
                    updateScoreLabel();
                    updateBulletsLabel();
                } else if (isItem(nextState)) {
                    applyItemEffect(nextState);
                }

                grid[newRow][newCol].setIcon(pacmanCurrentIcon);
                gridState[newRow][newCol] = "pacman" + playerId;
                pacmanRow = newRow;
                pacmanCol = newCol;

                // 이동 정보를 서버에 전송
                out.println("MOVE " + playerId + " " + pacmanRow + " " + pacmanCol + " " + pacmanDirection);
            }
        }
    }

    private void fireBullet() {
        if (bullets > 0) {
            bullets--;
            updateBulletsLabel();
            // 총알 생성 및 이동 시작
            int bulletRow = pacmanRow;
            int bulletCol = pacmanCol;
            moveBullet(bulletRow, bulletCol, pacmanDirection);
        }
    }

    private void moveBullet(int bulletRow, int bulletCol, int direction) {
        Timer bulletTimer = new Timer(100, null); // 총알 이동 타이머
        bulletTimer.addActionListener(new ActionListener() {
            int currentRow = bulletRow;
            int currentCol = bulletCol;
            String previousState = "empty"; // 총알이 지나간 자리의 원래 상태

            @Override
            public void actionPerformed(ActionEvent e) {
                int nextRow = currentRow;
                int nextCol = currentCol;

                // 현재 위치에서 총알 제거 (팩맨 위치 제외)
                if (!(currentRow == pacmanRow && currentCol == pacmanCol)) {
                    grid[currentRow][currentCol].setIcon(getIconForState(previousState));
                    gridState[currentRow][currentCol] = previousState;
                }

                // 다음 위치 계산
                switch (direction) {
                    case 1: // 위쪽
                        nextRow--;
                        break;
                    case 2: // 아래쪽
                        nextRow++;
                        break;
                    case 3: // 왼쪽
                        nextCol--;
                        break;
                    case 4: // 오른쪽
                        nextCol++;
                        break;
                }

                if (isValidMove(nextRow, nextCol)) {
                    String nextState = gridState[nextRow][nextCol];
                    previousState = "empty"; // 다음 위치의 상태를 저장

                    if (nextState.equals("wall")) {
                        // 벽 파괴
                        grid[nextRow][nextCol].setIcon(emptyIcon);
                        gridState[nextRow][nextCol] = "empty";
                        bulletTimer.stop();

                        // 일정 확률로 아이템 생성
                        if (random.nextInt(100) < 30) { // 30% 확률
                            String item = generateRandomItem();
                            grid[nextRow][nextCol].setIcon(getIconForItem(item));
                            gridState[nextRow][nextCol] = item;
                        }
                        return;
                    } else if (nextState.startsWith("pacman")) {
                        // 다른 플레이어를 맞췄을 때
                        bulletTimer.stop();
                        JOptionPane.showMessageDialog(frame, "상대를 맞췄습니다!");
                        // 추가로 상대의 목숨을 줄이거나 게임 종료 처리를 할 수 있습니다.
                        return;
                    } else if (nextState.equals("empty") || nextState.equals("dot")) {
                        // 총알을 다음 위치로 이동
                        grid[nextRow][nextCol].setIcon(bulletIcon);
                        gridState[nextRow][nextCol] = "bullet";

                        currentRow = nextRow;
                        currentCol = nextCol;
                    } else {
                        // 총알이 아이템이나 그 외에 부딪혔을 때
                        bulletTimer.stop();
                    }
                } else {
                    // 그리드 밖으로 나갔을 때
                    bulletTimer.stop();
                }
            }
        });
        bulletTimer.start();
    }

    private void handleEnemyMovement() {
        // 멀티플레이에서는 적의 이동을 서버에서 관리하거나, 혹은 제거할 수 있습니다.
        // 여기서는 간단히 적의 이동을 제거하겠습니다.
    }

    private boolean isValidMove(int row, int col) {
        // 그리드 범위 확인
        return row >= 0 && row < FIELD_ROW_SIZE && col >= 0 && col < FIELD_COL_SIZE;
    }

    private void updateScoreLabel() {
        // 점수 라벨 업데이트
        scoreLabel.setText("점수: " + dotsEaten + " / " + numOfDots);
    }

    private void updateBulletsLabel() {
        // 총알 라벨 업데이트
        bulletsLabel.setText("총알: " + bullets);
    }

    private void updateLivesLabel() {
        // 목숨 라벨 업데이트
        livesLabel.setText("목숨: " + lives);
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
            case "pacman1":
                switch (pacmanDirection) {
                    case 1:
                        return pacmanUpIcon;
                    case 2:
                        return pacmanDownIcon;
                    case 3:
                        return pacmanLeftIcon;
                    case 4:
                    default:
                        return pacmanRightIcon;
                }
            case "bullet":
                return bulletIcon;
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
                Timer invincibleTimer = new Timer(10000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        pacmanInvincible = false;
                    }
                });
                invincibleTimer.setRepeats(false);
                invincibleTimer.start();
                break;
            case "freezeEnemy":
                // 멀티플레이에서는 상대방을 얼리는 효과를 구현할 수 있습니다.
                break;
        }
    }

    private void resetPacmanPosition() {
        // 현재 위치에서 팩맨 제거
        grid[pacmanRow][pacmanCol].setIcon(emptyIcon);
        gridState[pacmanRow][pacmanCol] = "empty";

        // 팩맨 위치 초기화
        pacmanRow = (playerId == 0) ? 9 : 1;
        pacmanCol = 10;
        grid[pacmanRow][pacmanCol].setIcon(pacmanRightIcon);
        gridState[pacmanRow][pacmanCol] = "pacman" + playerId;
        pacmanDirection = 4;
    }

    // 서버로부터 메시지를 수신하는 스레드
    class ServerListener implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    // 서버로부터 메시지를 수신하여 처리
                    if (message.startsWith("MOVE")) {
                        String[] parts = message.split(" ");
                        int otherPlayerId = Integer.parseInt(parts[1]);
                        if (otherPlayerId != playerId) {
                            int otherRow = Integer.parseInt(parts[2]);
                            int otherCol = Integer.parseInt(parts[3]);
                            int otherDirection = Integer.parseInt(parts[4]);

                            // 다른 플레이어의 위치 업데이트
                            updateOtherPlayerPosition(otherPlayerId, otherRow, otherCol, otherDirection);
                        }
                    } else if (message.equals("START")) {
                        // 게임 시작 신호
                        System.out.println("게임이 시작되었습니다.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateOtherPlayerPosition(int otherPlayerId, int row, int col, int direction) {
        // 이전 위치에서 다른 플레이어 제거
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                if (gridState[i][j].equals("pacman" + otherPlayerId)) {
                    grid[i][j].setIcon(emptyIcon);
                    gridState[i][j] = "empty";
                }
            }
        }

        // 새로운 위치에 다른 플레이어 배치
        ImageIcon otherPacmanIcon = pacmanRightIcon;
        switch (direction) {
            case 1:
                otherPacmanIcon = pacmanUpIcon;
                break;
            case 2:
                otherPacmanIcon = pacmanDownIcon;
                break;
            case 3:
                otherPacmanIcon = pacmanLeftIcon;
                break;
            case 4:
                otherPacmanIcon = pacmanRightIcon;
                break;
        }

        grid[row][col].setIcon(otherPacmanIcon);
        gridState[row][col] = "pacman" + otherPlayerId;
    }
}
