package kr.ac.tukorea.ge.spgp.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private ImageView ball;
    private GridLayout gridLayout;
    private FrameLayout[][] blocks;
    private int[][] lifePoints;
    private final Random random = new Random();
    private float initialTouchX, initialTouchY;
    private float velocityX = 0, velocityY = 0;
    private boolean allBlocksClear = false;
    private int collidedBlockCount = 0;
    private int hitCount = 0;

    private TextView collidedBlockCountTextView;
    private ImageView enemy;
    private int enemyHealth = 50;
    private Button restartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ball = findViewById(R.id.ball);
        gridLayout = findViewById(R.id.gridLayout);
        collidedBlockCountTextView = findViewById(R.id.collidedBlockCount);
        restartButton = new Button(this);
        restartButton.setText("Restart");
        restartButton.setOnClickListener(v -> restartGame());
        restartButton.setVisibility(View.GONE);
        ((FrameLayout) findViewById(android.R.id.content)).addView(restartButton);

        initializeBlocks();
        resetBallPosition();
        spawnEnemy();

        gridLayout.post(() -> {
            float gridLayoutY = ball.getRootView().getHeight() * 2 / 3.0f - gridLayout.getHeight();
            gridLayout.setY(gridLayoutY);
        });
    }

    private void initializeBlocks() {
        int rows = 4;
        int cols = 5;
        blocks = new FrameLayout[rows][cols];
        lifePoints = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                lifePoints[i][j] = random.nextInt(2) + 1;
                blocks[i][j] = new FrameLayout(this);

                ImageView brickImageView = new ImageView(this);
                brickImageView.setImageResource(R.drawable.brick_image);
                brickImageView.setScaleType(ImageView.ScaleType.FIT_XY);

                TextView label = new TextView(this);
                label.setText(String.valueOf(lifePoints[i][j]));
                label.setTextColor(Color.WHITE);
                label.setTextSize(16);
                label.setGravity(Gravity.CENTER);

                FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                blocks[i][j].addView(brickImageView, labelParams);
                blocks[i][j].addView(label);

                GridLayout.LayoutParams param = new GridLayout.LayoutParams();
                param.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                param.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                param.rightMargin = 5;
                param.topMargin = 5;
                param.setGravity(Gravity.CENTER);
                param.columnSpec = GridLayout.spec(j);
                param.rowSpec = GridLayout.spec(i);
                blocks[i][j].setLayoutParams(param);
                gridLayout.addView(blocks[i][j]);
            }
        }

        gridLayout.post(() -> {
            float gridLayoutY = ball.getRootView().getHeight() * 2 / 3.0f - gridLayout.getHeight();
            gridLayout.setY(gridLayoutY);
        });
    }

    private void resetBallPosition() {
        ball.post(() -> {
            float x = (ball.getRootView().getWidth() - ball.getWidth()) / 2.0f;
            float y = ball.getRootView().getHeight() * 4 / 5.0f;
            ball.setX(x);
            ball.setY(y);
            velocityX = 0;
            velocityY = 0;

            collidedBlockCount = countCollidedBlocks();
            if (collidedBlockCount > 0) {

            }

            if (allBlocksClear) {
                allBlocksClear = false;
                initializeBlocks();
            }
        });
    }

    private int countCollidedBlocks() {
        int rows = 4;
        int cols = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                FrameLayout block = blocks[i][j];
                if (block.getVisibility() == View.VISIBLE && isCollision(block)) {
                    hitCount++;
                }
            }
        }
        return hitCount;
    }

    private void fireBullet(int damage) {
        float goblinX = 20 * getResources().getDisplayMetrics().density;
        float goblinY = 140 * getResources().getDisplayMetrics().density;

        ImageView bullet = new ImageView(this);
        bullet.setImageResource(R.drawable.bullet_image);

        int bulletSize = (int) (80 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(bulletSize, bulletSize);
        params.leftMargin = (int) goblinX;
        params.topMargin = (int) goblinY;
        bullet.setLayoutParams(params);

        ((FrameLayout) findViewById(android.R.id.content)).addView(bullet);

        float endX = getResources().getDisplayMetrics().widthPixels;

        final long startTime = System.currentTimeMillis();
        final long duration = 2000;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - startTime;
                float fraction = (float) elapsedTime / duration;

                if (fraction >= 1.0f) {
                    runOnUiThread(() -> {
                        ((FrameLayout) findViewById(android.R.id.content)).removeView(bullet);

                    });
                    timer.cancel();
                } else {
                    float currentX = goblinX + fraction * (endX - goblinX);
                    runOnUiThread(() -> bullet.setX(currentX));

                    if (isCollisionWithEnemy(bullet)) {
                        runOnUiThread(() -> {
                            ((FrameLayout) findViewById(android.R.id.content)).removeView(bullet);
                            damageEnemy(damage);
                            moveEnemy();
                            timer.cancel();
                        });
                    }
                }
            }
        }, 0, 16);
    }

    private void spawnEnemy() {
        enemy = new ImageView(this);
        enemy.setImageResource(R.drawable.enemy_image);

        int enemySize = (int) (80 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(enemySize, enemySize);
        params.leftMargin = getResources().getDisplayMetrics().widthPixels - enemySize;
        params.topMargin = (int) (120 * getResources().getDisplayMetrics().density);
        enemy.setLayoutParams(params);

        ((FrameLayout) findViewById(android.R.id.content)).addView(enemy);
    }

    private void moveEnemy() {
        float newEnemyX = enemy.getX() - 30 * getResources().getDisplayMetrics().density;
        if (newEnemyX <= 0) {
            newEnemyX = 0;
        }
        enemy.setX(newEnemyX);

        if (isCollision(enemy)) {
            endGame();
        }
    }

    private void damageEnemy(int damage) {
        enemyHealth -= damage;
        if (enemyHealth <= 0) {
            ((FrameLayout) findViewById(android.R.id.content)).removeView(enemy);
            enemyHealth = 50;
            spawnEnemy();
        }
    }

    private void endGame() {
        runOnUiThread(() -> {
            restartButton.setVisibility(View.VISIBLE);
            restartButton.setX((ball.getRootView().getWidth() - restartButton.getWidth()) / 2);
            restartButton.setY((ball.getRootView().getHeight() - restartButton.getHeight()) / 2);
        });
    }

    private void restartGame() {
        runOnUiThread(() -> {
            restartButton.setVisibility(View.GONE);
            enemyHealth = 50;
            spawnEnemy();
            initializeBlocks();
            resetBallPosition();
        });
    }

    private boolean isCollision(View block) {
        int[] ballLocation = new int[2];
        ball.getLocationOnScreen(ballLocation);
        float ballLeft = ballLocation[0];
        float ballRight = ballLeft + ball.getWidth();
        float ballTop = ballLocation[1];
        float ballBottom = ballTop + ball.getHeight();

        int[] blockLocation = new int[2];
        block.getLocationOnScreen(blockLocation);
        float blockLeft = blockLocation[0];
        float blockRight = blockLeft + block.getWidth();
        float blockTop = blockLocation[1];
        float blockBottom = blockTop + block.getHeight();

        return ballRight > blockLeft && ballLeft < blockRight && ballBottom > blockTop && ballTop < blockBottom;
    }

    private boolean isCollisionWithEnemy(View bullet) {
        int[] bulletLocation = new int[2];
        bullet.getLocationOnScreen(bulletLocation);
        float bulletLeft = bulletLocation[0];
        float bulletRight = bulletLeft + bullet.getWidth();
        float bulletTop = bulletLocation[1];
        float bulletBottom = bulletTop + bullet.getHeight();

        int[] enemyLocation = new int[2];
        enemy.getLocationOnScreen(enemyLocation);
        float enemyLeft = enemyLocation[0];
        float enemyRight = enemyLeft + enemy.getWidth();
        float enemyTop = enemyLocation[1];
        float enemyBottom = enemyTop + enemy.getHeight();

        return bulletRight > enemyLeft && bulletLeft < enemyRight && bulletBottom > enemyTop && bulletTop < enemyBottom;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (velocityX == 0 && velocityY == 0) {
                    initialTouchX = event.getX();
                    initialTouchY = event.getY();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                float finalTouchX = event.getX();
                float finalTouchY = event.getY();
                velocityX = (finalTouchX - initialTouchX) / 10;
                velocityY = (finalTouchY - initialTouchY) / 10;
                moveBallContinuously();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void moveBallContinuously() {
        if (ball != null && (velocityX != 0 || velocityY != 0)) {
            ball.postDelayed(() -> {
                float newX = ball.getX() + velocityX;
                float newY = ball.getY() + velocityY;

                if (newX <= 0 || newX + ball.getWidth() >= ball.getRootView().getWidth()) {
                    velocityX *= -1;
                }
                if (newY <= 0 || newY <= ball.getRootView().getHeight() / 3.0f) {
                    velocityY *= -1;
                }

                if (newY + ball.getHeight() >= ball.getRootView().getHeight()) {
                    resetBallPosition();
                    fireBullet(collidedBlockCount);
                    hitCount = 0;
                    collidedBlockCount = 0;
                } else {
                    ball.setX(newX);
                    ball.setY(newY);
                    moveBallContinuously();
                }

                checkCollisionWithBlocks();
            }, 16);
        }
    }

    private void checkCollisionWithBlocks() {
        int rows = 4;
        int cols = 5;
        boolean allBlocksInvisible = true;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                FrameLayout block = blocks[i][j];
                if (block.getVisibility() == View.VISIBLE) {
                    allBlocksInvisible = false;

                    if (isCollision(block)) {
                        lifePoints[i][j]--;
                        if (lifePoints[i][j] <= 0) {
                            block.setVisibility(View.INVISIBLE);
                        } else {
                            TextView label = (TextView) block.getChildAt(1);
                            if (label != null) {
                                label.setText(String.valueOf(lifePoints[i][j]));
                            }
                        }
                        reflectBall(block);
                        collidedBlockCount++;
                    }
                }
            }
        }

        collidedBlockCountTextView.setText(String.valueOf(collidedBlockCount));

        if (allBlocksInvisible) {
            allBlocksClear = true;
        }
    }

    private void reflectBall(View block) {
        float ballLeft = ball.getX();
        float ballRight = ball.getX() + ball.getWidth();
        float ballTop = ball.getY();
        float ballBottom = ball.getY() + ball.getHeight();

        float blockLeft = block.getX();
        float blockRight = block.getX() + block.getWidth();
        float blockTop = block.getY();
        float blockBottom = block.getY() + block.getHeight();

        if (ballRight > blockLeft && ballLeft < blockRight) {
            velocityX *= -1;
        }
        if (ballBottom > blockTop && ballTop < blockBottom) {
            velocityY *= -1;
        }
    }
}