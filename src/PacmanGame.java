import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Random;

public class PacmanGame {
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
    private ImageIcon bulletIcon; // 총알 아이콘 추가

    private JLabel[][] grid;
    private String[][] gridState; // 그리드 상태를 저장하는 배열 추가

    private JLabel scoreLabel; // 점수 표시를 위한 라벨
    private JLabel bulletsLabel; // 총알 수 표시를 위한 라벨

    private Random random;

    private int pacmanRow, pacmanCol;
    private int enemyRow, enemyCol;
    private int numOfDots;
    private int dotsEaten; // 먹은 점의 개수
    private int direction;
    private int startDelay;
    private String tempState; // 적이 이동할 때 이전 위치의 상태를 저장

    private int bullets; // 총알 수를 저장하는 변수 추가
    private int pacmanDirection; // 팩맨의 현재 방향을 저장하는 변수 추가

    private Timer enemyTimer; // 적 이동 타이머를 클래스 변수로 변경

    private static final int FIELD_ROW_SIZE = 28;
    private static final int FIELD_COL_SIZE = 36;
    private static final int FRAME_WIDTH = 1600;
    private static final int FRAME_HEIGHT = 1300;

    public static void main(String[] args) {
        new PacmanGame().startGame();
    }

    public void startGame() {
        // 메인 프레임 초기화
        frame = new JFrame("Pacman Game");
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
        bulletIcon = new ImageIcon("redBullet.png"); 
        
        // 게임 변수 초기화
        random = new Random();
        pacmanRow = 23;
        pacmanCol = 18;
        enemyRow = 14;
        enemyCol = 18;
        numOfDots = 0; // 초기화 후에 실제 점의 개수를 셀 것입니다.
        dotsEaten = 0;
        startDelay = 2;
        tempState = "empty";
        bullets = 0; // 총알 수 초기화
        pacmanDirection = 4; // 초기 방향을 오른쪽으로 설정

        // 게임 그리드 초기화
        grid = new JLabel[FIELD_ROW_SIZE][FIELD_COL_SIZE];
        gridState = new String[FIELD_ROW_SIZE][FIELD_COL_SIZE]; // 그리드 상태 배열 초기화
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                grid[i][j] = new JLabel(emptyIcon);
                gridState[i][j] = "empty"; // 초기 상태를 empty로 설정
            }
        }

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

        // 점수 라벨 초기화 및 상단에 추가
        scoreLabel = new JLabel("점수: 0");
        scoreLabel.setForeground(Color.WHITE);
        frame.add(scoreLabel, BorderLayout.NORTH);

        // 오른쪽 패널 생성 및 총알 라벨 추가
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        bulletsLabel = new JLabel("총알: " + bullets);
        bulletsLabel.setForeground(Color.WHITE);
        rightPanel.add(bulletsLabel, BorderLayout.NORTH);
        rightPanel.setBackground(Color.BLACK);
        frame.add(rightPanel, BorderLayout.EAST);

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
        enemyTimer = new Timer(600, new ActionListener() {
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

    private void initializeGameGrid() {
        // 벽과 작은 점 배치 (예시로 임의의 패턴을 생성합니다)
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                if (i == 0 || i == FIELD_ROW_SIZE - 1 || j == 0 || j == FIELD_COL_SIZE - 1) {
                    grid[i][j].setIcon(wallIcon); // 외곽 벽
                    gridState[i][j] = "wall";
                } else if ((i % 2 == 0 && j % 2 == 0)) {
                    grid[i][j].setIcon(wallIcon); // 내부 벽
                    gridState[i][j] = "wall";
                } else {
                    grid[i][j].setIcon(smallDotIcon); // 작은 점
                    gridState[i][j] = "dot";
                    numOfDots++;
                }
            }
        }

        // 팩맨 배치
        grid[pacmanRow][pacmanCol].setIcon(pacmanRightIcon);
        gridState[pacmanRow][pacmanCol] = "pacman";

        // 적 배치
        grid[enemyRow][enemyCol].setIcon(enemyIcon);
        gridState[enemyRow][enemyCol] = "enemy";

        // 팩맨 시작 위치의 점 제거
        if (gridState[pacmanRow][pacmanCol].equals("dot")) {
            grid[pacmanRow][pacmanCol].setIcon(pacmanRightIcon);
            gridState[pacmanRow][pacmanCol] = "pacman";
            numOfDots--;
        }
    }

    private void handlePacmanMovement(KeyEvent e) {
        int key = e.getKeyCode();
        int newRow = pacmanRow;
        int newCol = pacmanCol;
        ImageIcon pacmanCurrentIcon = pacmanRightIcon;

        // 키 입력에 따른 새로운 위치 결정 및 팩맨 아이콘 변경
        if (key == KeyEvent.VK_UP) {
            newRow--;
            pacmanCurrentIcon = pacmanUpIcon;
            pacmanDirection = 1; // 위쪽
        } else if (key == KeyEvent.VK_DOWN) {
            newRow++;
            pacmanCurrentIcon = pacmanDownIcon;
            pacmanDirection = 2; // 아래쪽
        } else if (key == KeyEvent.VK_LEFT) {
            newCol--;
            pacmanCurrentIcon = pacmanLeftIcon;
            pacmanDirection = 3; // 왼쪽
        } else if (key == KeyEvent.VK_RIGHT) {
            newCol++;
            pacmanCurrentIcon = pacmanRightIcon;
            pacmanDirection = 4; // 오른쪽
        }

        // 이동 가능 여부 확인
        if (isValidMove(newRow, newCol)) {
            String nextState = gridState[newRow][newCol];

            if (nextState.equals("dot")) {
                dotsEaten++;
                bullets++; // 총알 충전
                updateScoreLabel();
                updateBulletsLabel();
            }

            if (nextState.equals("enemy")) {
                // 팩맨이 적과 충돌했을 때
                grid[enemyRow][enemyCol].setIcon(enemyIcon);
                dialog.add(button);
                dialog.setVisible(true);
            } else if (!nextState.equals("wall")) {
                // 팩맨 이동
                grid[pacmanRow][pacmanCol].setIcon(emptyIcon);
                gridState[pacmanRow][pacmanCol] = "empty";

                grid[newRow][newCol].setIcon(pacmanCurrentIcon);
                gridState[newRow][newCol] = "pacman";

                pacmanRow = newRow;
                pacmanCol = newCol;
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

                if (isValidMove(nextRow, nextCol) && !gridState[nextRow][nextCol].equals("wall")) {
                    previousState = gridState[nextRow][nextCol];

                    // 총알이 적을 맞췄는지 확인
                    if (nextRow == enemyRow && nextCol == enemyCol) {
                        // 총알이 적을 맞춤
                        grid[enemyRow][enemyCol].setIcon(emptyIcon);
                        gridState[enemyRow][enemyCol] = "empty";
                        enemyRow = -1; // 적 제거
                        enemyCol = -1;
                        bulletTimer.stop();
                        enemyTimer.stop(); // 적 이동 중지
                        JOptionPane.showMessageDialog(frame, "적을 물리쳤습니다!");
                        return;
                    }

                    // 총알을 다음 위치로 이동
                    grid[nextRow][nextCol].setIcon(bulletIcon);
                    gridState[nextRow][nextCol] = "bullet";

                    currentRow = nextRow;
                    currentCol = nextCol;
                } else {
                    // 총알이 벽이나 그리드 밖으로 나갔을 때
                    bulletTimer.stop();
                }
            }
        });
        bulletTimer.start();
    }

    private void handleEnemyMovement() {
        // 적이 제거되었을 경우 이동하지 않음
        if (enemyRow < 0 || enemyCol < 0) {
            return;
        }

        // 적의 이동 로직
        if (startDelay <= 0) {
            direction = 1 + random.nextInt(4); // 무작위 방향
        } else {
            direction = 1; // 초기 방향
            startDelay--;
        }

        int newRow = enemyRow;
        int newCol = enemyCol;

        switch (direction) {
            case 1: // 위쪽
                newRow--;
                break;
            case 2: // 아래쪽
                newRow++;
                break;
            case 3: // 왼쪽
                newCol--;
                break;
            case 4: // 오른쪽
                newCol++;
                break;
        }

        // 이동 가능 여부 확인
        if (isValidMove(newRow, newCol) && !gridState[newRow][newCol].equals("wall")) {
            String nextState = gridState[newRow][newCol];

            // 적의 위치 업데이트
            grid[enemyRow][enemyCol].setIcon(getIconForState(tempState));
            gridState[enemyRow][enemyCol] = tempState;

            tempState = nextState;
            grid[newRow][newCol].setIcon(enemyIcon);
            gridState[newRow][newCol] = "enemy";

            enemyRow = newRow;
            enemyCol = newCol;

            // 팩맨과의 충돌 확인
            if (enemyRow == pacmanRow && enemyCol == pacmanCol) {
                grid[enemyRow][enemyCol].setIcon(enemyIcon);
                dialog.add(button);
                dialog.setVisible(true);
            }
        }
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

    private ImageIcon getIconForState(String state) {
        switch (state) {
            case "empty":
                return emptyIcon;
            case "wall":
                return wallIcon;
            case "dot":
                return smallDotIcon;
            case "pacman":
                // 팩맨의 방향에 따라 아이콘 반환
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
            case "enemy":
                return enemyIcon;
            case "bullet":
                return bulletIcon;
            default:
                return emptyIcon;
        }
    }
}
