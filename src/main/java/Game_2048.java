import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Random;
import java.util.Stack;

public class Game_2048 extends JPanel implements Serializable {

    private static final int SIDE = 4;
    private int[][] gameField = new int[SIDE][SIDE];
    private boolean isGameStopped = false;
    private int score;
    private int scoreCopy;

    private Stack<int[][]> previousStates = new Stack();
    private Stack<Integer> previousScores = new Stack<>();
    private boolean isSaveNeeded;

    final Color[] colorTable = {
            new Color(0x701710), new Color(0xFFE4C3), new Color(0xfff4d3),
            new Color(0xffdac3), new Color(0xe7b08e), new Color(0xe7bf8e),
            new Color(0xffc4c3), new Color(0xE7948e), new Color(0xbe7e56),
            new Color(0xbe5e56), new Color(0x9c3931), new Color(0x701710)
    };

    enum State {
        start, won, running, over
    }

    private State gameState = State.start;
    private Color gridColor = new Color(0xBBADA0);
    private Color emptyColor = new Color(0xCDC1B4);
    private Color startColor = new Color(0xFFEBCD);

    private Random rand = new Random();

    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = new JFrame();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setTitle("2048");
                f.setResizable(true);
                f.add(new Game_2048(), BorderLayout.CENTER);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }

    private void createGame() {
        isSaveNeeded = true;
        score = 0;
        gameState = State.running;
        gameField = new int[SIDE][SIDE];
        createNewNumber();
        createNewNumber();
    }

    private void createNewNumber() {
        if (getMaxTileValue() == 2048) {
            isGameStopped = true;
            gameState = State.won;
            repaint();
        }
        int x, y;
        do {
            x = rand.nextInt(SIDE);
            y = rand.nextInt(SIDE);
        }
        while (gameField[y][x] != 0);
        gameField[y][x] = rand.nextInt(10) == 9 ? 4 : 2;
    }

    private boolean compressRow(int[] row) {
        boolean isChanged = false;
        for (int i = 0; i < row.length - 1; i++) {
            if (row[i] == 0 && row[i] != row[i + 1]) {
                isChanged = true;
                row[i] = row[i + 1];
                row[i + 1] = 0;
            }
        }
        if (isChanged) {
            compressRow(row);
        }
        return isChanged;
    }

    private boolean mergeRow(int[] row) {
        boolean isChanged = false;
        for (int i = 0; i < row.length - 1; i++) {
            if (row[i] == row[i + 1] && row[i] != 0) {
                isChanged = true;
                row[i] *= 2;
                row[i + 1] = 0;
                score += row[i];
            }
        }
        return isChanged;
    }

    public Game_2048() {
        setPreferredSize(new Dimension(900, 700));
        setBackground(new Color(0xFAF8EF));
        setFont(new Font("SansSerif", Font.BOLD, 48));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isGameStopped = false;
                createGame();
                repaint();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isGameStopped == true) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        isGameStopped = false;
                        createGame();
                    } else {

                    }
                } else {
                    if (canUserMove() == true) {
                        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                            moveLeft();
                            repaint();
                        }
                        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                            moveRight();
                            repaint();
                        }
                        if (e.getKeyCode() == KeyEvent.VK_UP) {
                            moveUp();
                            repaint();
                        }
                        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                            moveDown();
                            repaint();
                        }
                        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                            isGameStopped = false;
                            createGame();
                            repaint();
                        }
                        if (e.getKeyCode() == KeyEvent.VK_Z) {
                            rollback();
                            repaint();
                        }
                        if (e.getKeyCode() == KeyEvent.VK_R) {
                            randomMove();
                            repaint();
                        }
                        if (e.getKeyCode() == KeyEvent.VK_L) {
                            deserialization();
                            repaint();
                        }

                        if (e.getKeyCode() == KeyEvent.VK_S) {
                            serialization();
                        }
                    } else {
                        isGameStopped = true;
                        gameState = State.over;
                        repaint();
                    }
                }
            }

        });
    }

    private void deserialization() {
        try {
            score = scoreCopy;
            FileInputStream fileInputStream = new FileInputStream("saveGame2048.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            gameField = (int[][]) objectInputStream.readObject();
            fileInputStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    private void serialization() {
        scoreCopy = score;
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream("saveGame2048.ser");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(gameField);
            objectOutputStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g);
    }

    void drawGrid(Graphics2D g) {
        g.setColor(gridColor);
        g.fillRoundRect(200, 100, 499, 499, 15, 15);

        if (gameState == State.running) {
            drawScore(g);
            for (int i = 0; i < SIDE; i++) {
                for (int j = 0; j < SIDE; j++) {
                    if (gameField[i][j] == 0) {
                        g.setColor(emptyColor);
                        g.fillRoundRect(215 + j * 121, 115 + i * 121, 106, 106, 7, 7);
                    } else {
                        drawTile(g, i, j);
                    }
                }
            }
        } else {
            g.setColor(startColor);
            g.fillRoundRect(215, 115, 469, 469, 7, 7);

            g.setColor(gridColor.darker());
            g.setFont(new Font("SansSerif", Font.BOLD, 128));
            g.drawString("2048", 310, 270);

            g.setFont(new Font("SansSerif", Font.BOLD, 20));

            if (gameState == State.won) {
                g.drawString("Ты сделал это!", 375, 350);

            } else if (gameState == State.over)
                g.drawString("Game over", 400, 350);

            g.setColor(gridColor);
            g.drawString("Клик или пробел для начала новой игры", 246, 545);
            g.drawString("(Используйте стрелки для движения плиток)", 225, 570);
            g.drawString("(S - сохранить)", 225, 450);
            g.drawString("(L - загрузить)", 525, 450);
            g.drawString("(R - случайный ход)", 225, 500);
            g.drawString("(Z - ход назад)", 525, 500);
        }
    }

    void drawTile(Graphics2D g, int i, int j) {

        int value = gameField[i][j];

        g.setColor(colorTable[(int) (Math.log(value) / Math.log(2)) + 1]);
        g.fillRoundRect(215 + j * 121, 115 + i * 121, 106, 106, 7, 7);
        String s = String.valueOf(value);

        g.setColor(value < 128 ? colorTable[0] : colorTable[1]);

        FontMetrics fm = g.getFontMetrics();
        int asc = fm.getAscent();
        int dec = fm.getDescent();

        int x = 215 + j * 121 + (106 - fm.stringWidth(s)) / 2;
        int y = 115 + i * 121 + (asc + (106 - (asc + dec)) / 2);

        g.drawString(s, x, y);
    }

    void drawScore(Graphics2D g) {
        g.setColor(new Color(0xBBADA0));
        FontMetrics fm = g.getFontMetrics();
        int asc = fm.getAscent();
        int dec = fm.getDescent();
        String v = String.valueOf(score);

        int x = 633 - fm.stringWidth(v) / 2;
        int y = 8 + (asc + (85 - (asc + dec)) / 2);
        g.fillRoundRect(623 - fm.stringWidth(v) / 2, 15, fm.stringWidth(v) + 20, 70, 7, 7);

        g.setColor(score < 100000 ? colorTable[0] : colorTable[1]);
        g.drawString(v, x, y);
    }

    private void moveLeft() {
        if (isSaveNeeded) saveState(gameField);
        boolean isChanged = false;
        for (int x = 0; x < SIDE; x++) {
            int[] row = gameField[x];
            if (compressRow(row)) {
                isChanged = true;
            }
            if (mergeRow(row)) {
                isChanged = true;
            }
            if (compressRow(row)) {
                isChanged = true;
            }
        }
        if (isChanged) {
            createNewNumber();
        }
        isSaveNeeded = true;
    }

    private void moveRight() {
        saveState(gameField);
        rotateClockwise();
        rotateClockwise();
        moveLeft();
        rotateClockwise();
        rotateClockwise();
    }

    private void moveUp() {
        saveState(gameField);
        rotateClockwise();
        rotateClockwise();
        rotateClockwise();
        moveLeft();
        rotateClockwise();
    }

    private void moveDown() {
        saveState(gameField);
        rotateClockwise();
        moveLeft();
        rotateClockwise();
        rotateClockwise();
        rotateClockwise();
    }

    private void rotateClockwise() {
        int n = SIDE;
        int[][] newField = new int[n][n];
        for (int i = 0; i < n / 2; i++) {
            for (int j = i; j < n - i - 1; j++) {
                int tmp = gameField[i][j];
                newField[i][j] = gameField[n - j - 1][i];
                newField[n - j - 1][i] = gameField[n - i - 1][n - j - 1];
                newField[n - i - 1][n - j - 1] = gameField[j][n - i - 1];
                newField[j][n - i - 1] = tmp;

            }
        }
        gameField = newField;
    }

    private int getMaxTileValue() {
        int max = 0;
        for (int x = 0; x < SIDE; x++) {
            for (int y = 0; y < SIDE; y++) {
                if (max < gameField[x][y]) {
                    max = gameField[x][y];
                }
            }
        }
        return max;
    }

    private boolean canUserMove() {
        boolean isTrue = false;
        for (int i = 0; i < SIDE; i++) {
            for (int j = 0; j < SIDE; j++) {
                if (gameField[i][j] == 0)
                    isTrue = true;
            }
            for (int j = 0; j < SIDE - 1; j++) {
                if (gameField[i][j] == gameField[i][j + 1])
                    isTrue = true;
            }
            for (int j = 0; j < SIDE - 1; j++) {
                if (gameField[j][i] == gameField[j + 1][i])
                    isTrue = true;
            }
        }
        return isTrue;
    }

    public void saveState(int[][] tiles) {
        int[][] newTile = new int[SIDE][SIDE];
        for (int i = 0; i < SIDE; i++) {
            for (int j = 0; j < SIDE; j++) {
                newTile[i][j] = tiles[i][j];
            }
        }
        previousStates.push(newTile);
        previousScores.push(score);
        this.isSaveNeeded = false;
    }

    public void rollback() {
        if (previousScores.isEmpty() | previousStates.isEmpty()) return;
        score = previousScores.pop();
        for (int i = 0; i < SIDE; i++) {
            for (int j = 0; j < SIDE; j++) {
                gameField[i][j] = previousStates.peek()[i][j];
            }
        }
        gameField = previousStates.pop();
    }

    public void randomMove() {
        int n = ((int) (Math.random() * 100)) % 4;

        switch (n) {
            case 0:
                moveLeft();
                break;
            case 1:
                moveUp();
                break;
            case 2:
                moveRight();
                break;
            case 3:
                moveDown();
                break;
        }
    }
}

