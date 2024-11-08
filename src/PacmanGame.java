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
    private ImageIcon bigDotIcon;
    private ImageIcon wallIcon;
    private ImageIcon enemyIcon;
    private ImageIcon pacmanUpIcon;
    private ImageIcon pacmanDownIcon;
    private ImageIcon pacmanLeftIcon;
    private ImageIcon pacmanRightIcon;
    private ImageIcon emptyIcon;

    private JLabel[][] grid;
    private JLabel scoreLabel; // 점수 표시를 위한 라벨

    private Random random;

    private int pacmanRow, pacmanCol;
    private int enemyRow, enemyCol;
    private int numOfDots;
    private int dotsEaten; // 먹은 점의 개수
    private int direction;
    private int startDelay;
    private Icon tempIcon;

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
        bigDotIcon = new ImageIcon("bigDot.png");
        wallIcon = new ImageIcon("wall.png");
        enemyIcon = new ImageIcon("ghosts.gif");
        pacmanUpIcon = new ImageIcon("pacman_up.gif");
        pacmanDownIcon = new ImageIcon("pacman_down.gif");
        pacmanLeftIcon = new ImageIcon("pacman_left.gif");
        pacmanRightIcon = new ImageIcon("pacman_right.gif");
        emptyIcon = new ImageIcon("empty.png");

        // 게임 변수 초기화
        random = new Random();
        pacmanRow = 23;
        pacmanCol = 18;
        enemyRow = 14;
        enemyCol = 18;
        numOfDots = 0; // 초기화 후에 실제 점의 개수를 셀 것입니다.
        dotsEaten = 0;
        startDelay = 2;
        tempIcon = emptyIcon;

        // 게임 그리드 초기화
        grid = new JLabel[FIELD_ROW_SIZE][FIELD_COL_SIZE];
        for (int i = 0; i < FIELD_ROW_SIZE; i++) {
            for (int j = 0; j < FIELD_COL_SIZE; j++) {
                grid[i][j] = new JLabel(emptyIcon);
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

        // 점수 라벨 초기화 및 우측 상단에 추가
        scoreLabel = new JLabel("점수: 0");
        scoreLabel.setForeground(Color.WHITE);
        frame.add(scoreLabel, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);

        // 그리드에 게임 요소 초기화
        initializeGameGrid();

        // 팩맨 이동을 위한 키 리스너 추가
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handlePacmanMovement(e);
            }
        });

        // 적의 이동을 위한 타이머 설정
        Timer enemyTimer = new Timer(600, new ActionListener() {
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
                } else if ((i % 2 == 0 && j % 2 == 0)) {
                    grid[i][j].setIcon(wallIcon); // 내부 벽
                } else {
                    grid[i][j].setIcon(smallDotIcon); // 작은 점
                    numOfDots++;
                }
            }
        }

        // 팩맨 배치
        grid[pacmanRow][pacmanCol].setIcon(pacmanRightIcon);

        // 적 배치
        grid[enemyRow][enemyCol].setIcon(enemyIcon);

        // 팩맨 시작 위치의 점 제거
        if (grid[pacmanRow][pacmanCol].getIcon().equals(smallDotIcon)) {
            grid[pacmanRow][pacmanCol].setIcon(pacmanRightIcon);
            numOfDots--;
        }
    }

    private void handlePacmanMovement(KeyEvent e) {
        // 모든 점을 먹었을 때 승리 처리
        if (dotsEaten >= numOfDots) {
            dialog.add(button);
            dialog.setVisible(true);
        }

        int key = e.getKeyCode();
        int newRow = pacmanRow;
        int newCol = pacmanCol;
        ImageIcon pacmanCurrentIcon = pacmanRightIcon;

        // 키 입력에 따른 새로운 위치 결정 및 팩맨 아이콘 변경
        if (key == KeyEvent.VK_UP) {
            newRow--;
            pacmanCurrentIcon = pacmanUpIcon;
        } else if (key == KeyEvent.VK_DOWN) {
            newRow++;
            pacmanCurrentIcon = pacmanDownIcon;
        } else if (key == KeyEvent.VK_LEFT) {
            newCol--;
            pacmanCurrentIcon = pacmanLeftIcon;
        } else if (key == KeyEvent.VK_RIGHT) {
            newCol++;
            pacmanCurrentIcon = pacmanRightIcon;
        }

        // 이동 가능 여부 확인
        if (isValidMove(newRow, newCol)) {
            Icon nextIcon = grid[newRow][newCol].getIcon();

            if (nextIcon.equals(smallDotIcon)) {
                dotsEaten++;
                updateScoreLabel();
            }

            if (nextIcon.equals(enemyIcon)) {
                // 팩맨이 적과 충돌했을 때
                grid[enemyRow][enemyCol].setIcon(enemyIcon);
                dialog.add(button);
                dialog.setVisible(true);
            } else if (!nextIcon.equals(wallIcon)) {
                // 팩맨 이동
                grid[pacmanRow][pacmanCol].setIcon(emptyIcon);
                grid[newRow][newCol].setIcon(pacmanCurrentIcon);
                pacmanRow = newRow;
                pacmanCol = newCol;
            }
        }
    }

    private void handleEnemyMovement() {
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
        if (isValidMove(newRow, newCol) && !grid[newRow][newCol].getIcon().equals(wallIcon)) {
            Icon nextIcon = grid[newRow][newCol].getIcon();

            // 적의 위치 업데이트
            grid[enemyRow][enemyCol].setIcon(tempIcon);
            tempIcon = nextIcon;
            grid[newRow][newCol].setIcon(enemyIcon);
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
}
