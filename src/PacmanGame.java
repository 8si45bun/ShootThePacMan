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
    private int ghost_hp = 5; // 유령의 HP
    private int bullets; // 총알 수를 저장하는 변수 추가
    private int pacmanDirection; // 팩맨의 현재 방향을 저장하는 변수 추가

    private Timer enemyTimer; // 적 이동 타이머를 클래스 변수로 변경

    private static final int FIELD_ROW_SIZE = 11;
    private static final int FIELD_COL_SIZE = 20;
    private static final int FRAME_WIDTH = 1000;
    private static final int FRAME_HEIGHT = 550;

    private boolean canMove = true; // 팩맨이 이동 가능한지 여부를 저장
    private final int MOVE_DELAY = 100; // 팩맨 이동 딜레이 (밀리초)

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
        bulletIcon = new ImageIcon("yellowBullet.png");

        // 게임 변수 초기화
        random = new Random();
        pacmanRow = 9;
        pacmanCol = 10;
        enemyRow = 5;
        enemyCol = 9;
        numOfDots = 104; // 초기화되는 점의 개수
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
                grid[i][j] = new JLabel(smallDotIcon);
                gridState[i][j] = "dot"; // 초기 상태를 점으로 설정
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
        scoreLabel.setForeground(Color.BLACK);
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
                if (canMove) { // 팩맨이 이동 가능할 때만 키 입력 처리
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_SPACE) {
                        fireBullet();
                    } else {
                        handlePacmanMovement(e);
                        // 이동 후 딜레이를 적용하여 일정 시간 동안 입력 제한
                        canMove = false;
                        new Timer(MOVE_DELAY, new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                canMove = true; // 일정 시간이 지나면 다시 이동 가능
                                ((Timer) e.getSource()).stop(); // 타이머 정지
                            }
                        }).start();
                    }
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

	// 블럭 배치
private void initializeGameGrid() {
    for (int i = 0; i < FIELD_ROW_SIZE; i++) {
        for (int j = 0; j < FIELD_COL_SIZE; j++) {
            if (i == 0 || i == FIELD_ROW_SIZE - 1 || j == 0 || j == FIELD_COL_SIZE - 1) {
                grid[i][j].setIcon(wallIcon); // 외곽 벽
                gridState[i][j] = "wall";
            }                 
        }
    }
    
    grid[2][2].setIcon(wallIcon); // 내부 벽
    gridState[2][2] = "wall";        
    grid[3][2].setIcon(wallIcon); // 내부 벽
    gridState[3][2] = "wall";        
    grid[4][2].setIcon(wallIcon); // 내부 벽
    gridState[4][2] = "wall";       
    grid[6][2].setIcon(wallIcon); // 내부 벽
    gridState[6][2] = "wall";        
    grid[7][2].setIcon(wallIcon); // 내부 벽
    gridState[7][2] = "wall";       
    grid[8][2].setIcon(wallIcon); // 내부 벽
    gridState[8][2] = "wall";
    
    
    grid[2][3].setIcon(wallIcon); // 내부 벽
    gridState[2][3] = "wall";        
    grid[8][3].setIcon(wallIcon); // 내부 벽
    gridState[8][3] = "wall";
    
    
    grid[4][4].setIcon(wallIcon); // 내부 벽
    gridState[4][4] = "wall";       
    grid[6][4].setIcon(wallIcon); // 내부 벽
    gridState[6][4] = "wall";
    
    
    grid[4][5].setIcon(wallIcon); // 내부 벽
    gridState[4][5] = "wall";      
    grid[6][5].setIcon(wallIcon); // 내부 벽
    gridState[6][5] = "wall";       
    grid[1][5].setIcon(wallIcon); // 내부 벽
    gridState[1][5] = "wall";       
    grid[2][5].setIcon(wallIcon); // 내부 벽
    gridState[2][5] = "wall";
    grid[9][5].setIcon(wallIcon); // 내부 벽
    gridState[9][5] = "wall";
    grid[8][5].setIcon(wallIcon); // 내부 벽
    gridState[8][5] = "wall";
    
    
    grid[2][12].setIcon(wallIcon); // 내부 벽
    gridState[2][12] = "wall";
    grid[2][7].setIcon(wallIcon); // 내부 벽
    gridState[2][7] = "wall";
    grid[2][8].setIcon(wallIcon); // 내부 벽
    gridState[2][8] = "wall";
    grid[2][9].setIcon(wallIcon); // 내부 벽
    gridState[2][9] = "wall";
    grid[2][10].setIcon(wallIcon); // 내부 벽
    gridState[2][10] = "wall";
    grid[2][11].setIcon(wallIcon); // 내부 벽
    gridState[2][11] = "wall";
    
    
    grid[8][12].setIcon(wallIcon); // 내부 벽
    gridState[8][12] = "wall";
    grid[8][7].setIcon(wallIcon); // 내부 벽
    gridState[8][7] = "wall";
    grid[8][8].setIcon(wallIcon); // 내부 벽
    gridState[8][8] = "wall";
    grid[8][9].setIcon(wallIcon); // 내부 벽
    gridState[8][9] = "wall";
    grid[8][10].setIcon(wallIcon); // 내부 벽
    gridState[8][10] = "wall";
    grid[8][11].setIcon(wallIcon); // 내부 벽
    gridState[8][11] = "wall";
    
    
    grid[6][12].setIcon(wallIcon); // 내부 벽
    gridState[6][12] = "wall";
    grid[6][7].setIcon(wallIcon); // 내부 벽
    gridState[6][7] = "wall";
    grid[6][8].setIcon(wallIcon); // 내부 벽
    gridState[6][8] = "wall";
    grid[6][9].setIcon(wallIcon); // 내부 벽
    gridState[6][9] = "wall";
    grid[6][10].setIcon(wallIcon); // 내부 벽
    gridState[6][10] = "wall";
    grid[6][11].setIcon(wallIcon); // 내부 벽
    gridState[6][11] = "wall";
    
    grid[5][12].setIcon(wallIcon); // 내부 벽
    gridState[5][12] = "wall";
    grid[5][7].setIcon(wallIcon); // 내부 벽
    gridState[5][7] = "wall";
    grid[4][12].setIcon(wallIcon); // 내부 벽
    gridState[4][12] = "wall";
    grid[4][7].setIcon(wallIcon); // 내부 벽
    gridState[4][7] = "wall";
    grid[4][8].setIcon(wallIcon); // 내부 벽
    gridState[4][8] = "wall";        
    grid[4][11].setIcon(wallIcon); // 내부 벽
    gridState[4][11] = "wall";
    
    
    grid[4][15].setIcon(wallIcon); // 내부 벽
    gridState[4][15] = "wall";       
    grid[6][15].setIcon(wallIcon); // 내부 벽
    gridState[6][15] = "wall";
    
    
    grid[4][14].setIcon(wallIcon); // 내부 벽
    gridState[4][14] = "wall";      
    grid[6][14].setIcon(wallIcon); // 내부 벽
    gridState[6][14] = "wall";       
    grid[1][14].setIcon(wallIcon); // 내부 벽
    gridState[1][14] = "wall";       
    grid[2][14].setIcon(wallIcon); // 내부 벽
    gridState[2][14] = "wall";
    grid[9][14].setIcon(wallIcon); // 내부 벽
    gridState[9][14] = "wall";
    grid[8][14].setIcon(wallIcon); // 내부 벽
    gridState[8][14] = "wall";
    
    
    grid[2][17].setIcon(wallIcon); // 내부 벽
    gridState[2][17] = "wall";        
    grid[3][17].setIcon(wallIcon); // 내부 벽
    gridState[3][17] = "wall";        
    grid[4][17].setIcon(wallIcon); // 내부 벽
    gridState[4][17] = "wall";       
    grid[6][17].setIcon(wallIcon); // 내부 벽
    gridState[6][17] = "wall";        
    grid[7][17].setIcon(wallIcon); // 내부 벽
    gridState[7][17] = "wall";       
    grid[8][17].setIcon(wallIcon); // 내부 벽
    gridState[8][17] = "wall";
    
    
    grid[2][16].setIcon(wallIcon); // 내부 벽
    gridState[2][16] = "wall";        
    grid[8][16].setIcon(wallIcon); // 내부 벽
    gridState[8][16] = "wall";
    

        grid[pacmanRow][pacmanCol].setIcon(pacmanRightIcon);
        gridState[pacmanRow][pacmanCol] = "pacman";

        grid[enemyRow][enemyCol].setIcon(enemyIcon);
        gridState[enemyRow][enemyCol] = "enemy";

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

        if (key == KeyEvent.VK_UP) {
            newRow--;
            pacmanCurrentIcon = pacmanUpIcon;
            pacmanDirection = 1;
        } else if (key == KeyEvent.VK_DOWN) {
            newRow++;
            pacmanCurrentIcon = pacmanDownIcon;
            pacmanDirection = 2;
        } else if (key == KeyEvent.VK_LEFT) {
            newCol--;
            pacmanCurrentIcon = pacmanLeftIcon;
            pacmanDirection = 3;
        } else if (key == KeyEvent.VK_RIGHT) {
            newCol++;
            pacmanCurrentIcon = pacmanRightIcon;
            pacmanDirection = 4;
        }

        if (isValidMove(newRow, newCol)) {
            String nextState = gridState[newRow][newCol];
            if (nextState.equals("dot")) {
                dotsEaten++;
                bullets++;
                updateScoreLabel();
                updateBulletsLabel();
            }

            if (nextState.equals("enemy")) {
                grid[enemyRow][enemyCol].setIcon(enemyIcon);
                dialog.add(button);
                dialog.setVisible(true);
            } else if (!nextState.equals("wall")) {
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
            moveBullet(pacmanRow, pacmanCol, pacmanDirection);
        }
    }

    private void moveBullet(int bulletRow, int bulletCol, int direction) {
        Timer bulletTimer = new Timer(100, null);
        bulletTimer.addActionListener(new ActionListener() {
            int currentRow = bulletRow;
            int currentCol = bulletCol;
            String previousState = "empty";

            @Override
            public void actionPerformed(ActionEvent e) {
                int nextRow = currentRow;
                int nextCol = currentCol;

                if (!(currentRow == pacmanRow && currentCol == pacmanCol)) {
                    grid[currentRow][currentCol].setIcon(getIconForState(previousState));
                    gridState[currentRow][currentCol] = previousState;
                }

                switch (direction) {
                    case 1: nextRow--; break;
                    case 2: nextRow++; break;
                    case 3: nextCol--; break;
                    case 4: nextCol++; break;
                }

                if (isValidMove(nextRow, nextCol)) {
                    String nextState = gridState[nextRow][nextCol];
                    previousState = nextState;

                    if (nextState.equals("wall")) {
                        grid[nextRow][nextCol].setIcon(emptyIcon);
                        gridState[nextRow][nextCol] = "empty";
                        bulletTimer.stop();
                        return;
                    } else if (nextState.equals("enemy")) {
                        ghost_hp--;
                        if (ghost_hp <= 0) {
                            grid[enemyRow][enemyCol].setIcon(emptyIcon);
                            gridState[enemyRow][enemyCol] = "empty";
                            enemyRow = -1;
                            enemyCol = -1;
                            bulletTimer.stop();
                            enemyTimer.stop();
                            JOptionPane.showMessageDialog(frame, "적을 물리쳤습니다!");
                            return;
                        }
                    }

                    grid[nextRow][nextCol].setIcon(bulletIcon);
                    gridState[nextRow][nextCol] = "bullet";

                    currentRow = nextRow;
                    currentCol = nextCol;
                } else {
                    bulletTimer.stop();
                }
            }
        });
        bulletTimer.start();
    }


    private void handleEnemyMovement() {
        if (enemyRow < 0 || enemyCol < 0) {
            return;
        }

        if (startDelay <= 0) {
            direction = 1 + random.nextInt(4);
        } else {
            direction = 1;
            startDelay--;
        }

        int newRow = enemyRow;
        int newCol = enemyCol;

        switch (direction) {
            case 1: newRow--; break;
            case 2: newRow++; break;
            case 3: newCol--; break;
            case 4: newCol++; break;
        }

        if (isValidMove(newRow, newCol) && !gridState[newRow][newCol].equals("wall")) {
            String nextState = gridState[newRow][newCol];

            grid[enemyRow][enemyCol].setIcon(getIconForState(tempState));
            gridState[enemyRow][enemyCol] = tempState;

            tempState = nextState;
            grid[newRow][newCol].setIcon(enemyIcon);
            gridState[newRow][newCol] = "enemy";

            enemyRow = newRow;
            enemyCol = newCol;

            if (enemyRow == pacmanRow && enemyCol == pacmanCol) {
                grid[enemyRow][enemyCol].setIcon(enemyIcon);
                dialog.add(button);
                dialog.setVisible(true);
            }
        }
    }

    private boolean isValidMove(int row, int col) {
        return row >= 0 && row < FIELD_ROW_SIZE && col >= 0 && col < FIELD_COL_SIZE;
    }

    private void updateScoreLabel() {
        scoreLabel.setText("점수: " + dotsEaten + " / " + numOfDots);
    }

    private void updateBulletsLabel() {
        bulletsLabel.setText("총알: " + bullets);
    }

    private ImageIcon getIconForState(String state) {
        switch (state) {
            case "empty": return emptyIcon;
            case "wall": return wallIcon;
            case "dot": return smallDotIcon;
            case "pacman":
                switch (pacmanDirection) {
                    case 1: return pacmanUpIcon;
                    case 2: return pacmanDownIcon;
                    case 3: return pacmanLeftIcon;
                    case 4: default: return pacmanRightIcon;
                }
            case "enemy": return enemyIcon;
            case "bullet": return bulletIcon;
            default: return emptyIcon;
        }
    }
}
