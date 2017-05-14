package FS_Updated;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferStrategy;
import java.awt.image.Kernel;
import java.awt.print.PrinterGraphics;
import java.security.KeyStore.PrivateKeyEntry;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class WormChase extends JFrame implements Runnable {

	// image effects
	private static final int CELL_PER_LOOP = 1;    // rates of disappearing/appearing
	private static final int HOLE_EXIST_TIME = 3;  // secs
	private static final int FAKE_EXIST_TIME = 10;
	
	// page flipping methods
	private static final int NUM_BUFFERS = 2; // used for page flipping
	private BufferStrategy bStrategy;

	// measure FPS/UPS
	private static final int MAX_STATS_INTERVAL = 1000;
	private static final int MAX_FPS_STORE = 10;
	private static final int MAX_UPS_STORE = 10;

	private int frameCount = 0;
	private int frameskip = 0;

	private int stats_interval = 0;
	private long prev_interval = 0;

	double averageFPS = 0;
	double averageUPS = 0;

	DecimalFormat df_2 = new DecimalFormat("0.##");

	private int fpsCount = 0;
	private double[] fpsStore = new double[MAX_FPS_STORE];
	private int upsCount = 0;
	private double[] upsStore = new double[MAX_UPS_STORE];
	{
		Arrays.fill(fpsStore, 0);
		Arrays.fill(upsStore, 0);
	}

	// animation loop
	private static final int MAX_NO_DELAYS = 20;
	private static final int MAX_SKIP_FRAMES = 10;

	Graphics dbg;
	private long beforeTime; // used to measure the time of an animation loop
	private long period;

	// size
	private int pWidth, pHeight;
	// score, box used, time elapsed
	private int score = 0;
	private int boxUsed = 0;
	private int total_time = 0;

	// game components
	private Obstacle obs;
	private Worm worm;
	private FakeSnake fakeSnake;
	private Hole holeObjs;
	
	// Interaction components
	private Rectangle pauseArea;
	private Rectangle quitArea;

	private boolean isOverPause = false;
	private boolean isOverQuit = false;

	// flag and thread
	private volatile boolean running = false;
	private volatile boolean isPause = false;
	private volatile boolean gameOver = false;
	private Thread animator = null;

	public WormChase(long period) {
		super();
		
		// Initiate the screen first
		// switch off repaint and resizeing because it tends to act badly int
		// the full-screen mode
		this.setUndecorated(true); // no menu bars, borders, etc.
		this.setIgnoreRepaint(true); // why turn off painting events
		this.setResizable(false);

		if (!GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isFullScreenSupported()) {
			// Ideally, changes to AFS
			System.out.println("Full Screen Does not support");
			System.exit(0);
		}
		GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
		setBufferStrategy(); // apply page-flipping to double buffer

		//setDisplayMode(800, 600, 32);

		// calculates the screen size
		Rectangle screenRect = this.getGraphicsConfiguration().getBounds();
		this.pWidth = screenRect.width;
		this.pHeight = screenRect.height;

		// initiate some variables
		this.period = period;
		this.pauseArea = new Rectangle(pWidth - 100, pHeight - 45, 70, 15);
		this.quitArea = new Rectangle(pWidth - 100, pHeight - 20, 70, 15);
		this.obs = new Obstacle(this);
		this.holeObjs = new Hole(HOLE_EXIST_TIME);
		this.worm = new Worm(pWidth, pHeight, obs, holeObjs);
		this.fakeSnake = new FakeSnake(pWidth, pHeight, obs, holeObjs, 3, FAKE_EXIST_TIME);

		// initiate listener
		this.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();

				if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_Q
						|| (keyCode == KeyEvent.VK_C && e.isControlDown())) {

					running = false;
					System.exit(0);
				}
			}
		});

		this.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {

				if (running) {
					isOverPause = pauseArea.contains(e.getX(), e.getY());
					isOverQuit = quitArea.contains(e.getX(), e.getY());
				}
			}
		});

		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {

				if (isOverPause) {
					isPause = !isPause;
				} else if (isOverQuit) {
					running = false;
					System.exit(0);
				} else if (!isPause && !gameOver) {
					if (worm.touchAtHead(e.getX(), e.getY())) {
						gameOver = true;
					} else {
						if (!worm.touchAtHead(e.getX(), e.getY())) {
							obs.add(e.getX(), e.getY());
						}
					}
				}
			}
		});

		// Perform the task when the game is closing
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// If the game is exited, save all the game details
				// .....
				// running = false;
				// System.exit(0);

				
				// GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
			}
		});
		
		startGame();
	}

	private void startGame() {
		if (animator == null || !running) {
			animator = new Thread(this);
			animator.start();
			running = true;
		}

	}

	public void stopGame() {
		// called when the JFrame is closing
		gameOver = true;

	}

	public void resumeGame() {
		// called when the JFrame is activated / deconified
		isPause = false;

	}

	public void pauseGame() {
		// called when the JFrame is deactivated / iconified
		isPause = true;

	}

	private void setBufferStrategy() {
		try {

			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					createBufferStrategy(NUM_BUFFERS);
				}
			});

		} catch (Exception e) {
			System.err.println("Cannot create buffer strategy");
			System.exit(0);
		}

		// give time for buffer strategy to be done
		try {
			Thread.sleep(500);
		} catch (Exception e) {
		}

		bStrategy = this.getBufferStrategy(); // store for later
	}


	private void gameUpdate() {
		if (!isPause && !gameOver) {
			worm.move();
			fakeSnake.updateFakeSnake(total_time);
			worm.updateExistStatus(total_time, CELL_PER_LOOP);
			holeObjs.updateExistency(total_time);
		}

	}

	private void gameRendering(Graphics dbg) {

		// There is no longer need to create a back buffer

		// basic setting
		dbg.setColor(Color.WHITE);
		dbg.fillRect(0, 0, pWidth, pHeight); // The rectangle is filled using
												// the graphics context's
												// current color

		dbg.setColor(Color.GREEN);
		dbg.setFont(new Font("SansSerif", Font.BOLD, 24));

		// create game component
		// the drawing task is performed by each class to reduce code complexity
		// in WormPanel
		
		
		obs.draw(dbg);
		holeObjs.draw(dbg);
		worm.draw(dbg);
		fakeSnake.draw(dbg);
		this.drawButtom();

		// Box used, time elapsed
		dbg.setColor(Color.BLUE);
		dbg.drawString("Box used:" + boxUsed, 10, pHeight - 30);
		dbg.drawString("Time Elapsed:" + total_time, 190, pHeight - 30);

		// FPS, UPS
		dbg.drawString("FPS:" + df_2.format(averageFPS) + "  UPS:" + df_2.format(averageUPS), 20, 20);

		if (gameOver) {
			gameOverMessage(dbg);
		}

	}

	private void paintUpdate() {
		try {
			dbg = bStrategy.getDrawGraphics();
			gameRendering(dbg);
			dbg.dispose();
			if (!bStrategy.contentsLost())
				bStrategy.show();
			else
				System.out.println("Content Lost");
			Toolkit.getDefaultToolkit().sync();

		} catch (Exception e) {
		}
	}

	@Override
	public void run() {

		/*
		 * Nanosecond: timeDiff, beforeTime, afterTime, sleepingTime,
		 * oversleepTime Millsecond: excess, period
		 */
		long timeDiff, afterTime, sleepingTime, oversleepTime = 0L;
		int no_delays = 0;
		long excess = 0L;

		beforeTime = System.nanoTime();
		prev_interval = System.nanoTime();

		while (running) {

			// after game is over, running = false is not good
			gameUpdate();
			// gameRendering();
			paintUpdate();

			afterTime = System.nanoTime();
			timeDiff = afterTime - beforeTime;
			sleepingTime = period * 1000000L - timeDiff - oversleepTime;

			if (sleepingTime > 0) { // enough time for threads to sleep
				try {
					Thread.sleep(sleepingTime / 1000000L); // nano -> ms
					oversleepTime = (System.nanoTime() - afterTime) - sleepingTime; // error
				} catch (Exception e) {
				}
			} else {
				oversleepTime = 0L;
				excess -= sleepingTime / 1000000L; // nano -> ms
				no_delays++;
			}

			// garbage collector
			if (no_delays >= MAX_NO_DELAYS) {
				Thread.yield();
				no_delays = 0;
			}

			// excess time
			int skips = 0;
			while ((excess >= period) && (skips < MAX_SKIP_FRAMES)) {
				gameUpdate();
				excess -= period;
				skips++;
			}
			frameskip += skips;
			reportStats();

			beforeTime = System.nanoTime();
		}
	}

	private void reportStats() {
		frameCount++;
		stats_interval += period;

		if (stats_interval >= MAX_STATS_INTERVAL) {

			long cur_interval = System.nanoTime();
			double realElapsedTime = (double) (cur_interval - prev_interval) / 1000000L; // nano
																							// ->
																							// ms
			double timeError = (double) (realElapsedTime - stats_interval) / stats_interval * 100; // %

			double realFPS = (double) (frameCount / realElapsedTime * stats_interval);
			fpsStore[fpsCount % MAX_FPS_STORE] = realFPS;
			fpsCount++;
			double last_fps_sum = 0;
			for (int i = 0; i < MAX_FPS_STORE; i++)
				last_fps_sum += fpsStore[i];
			averageFPS = 0;
			if (fpsCount < MAX_FPS_STORE)
				averageFPS = last_fps_sum / fpsCount;
			else
				averageFPS = last_fps_sum / MAX_FPS_STORE;

			double realUPS = (double) (frameCount + frameskip) / realElapsedTime * stats_interval;
			upsStore[upsCount % MAX_UPS_STORE] = realUPS;
			upsCount++;
			double last_ups_sum = 0;
			for (int i = 0; i < MAX_UPS_STORE; i++)
				last_ups_sum += upsStore[i];
			averageUPS = 0;
			if (upsCount < MAX_UPS_STORE)
				averageUPS = last_ups_sum / upsCount;
			else
				averageUPS = last_ups_sum / MAX_UPS_STORE;

			/*
			 * System.out.println(stats_interval + "ms " +
			 * df_2.format(realElapsedTime) + "ms " + df_2.format(timeError) +
			 * "% " + frameCount + "c " + frameskip + " loss " +
			 * df_2.format(realFPS) + "fps " + df_2.format(realUPS) + "ups " +
			 * df_2.format(averageFPS) + "fps " + df_2.format(averageUPS) +
			 * "ups");
			 */

			prev_interval = System.nanoTime();
			stats_interval = 0;
			frameCount = 0;
			frameskip = 0;

			if (!isPause && !gameOver)
				total_time += 1;
		}
	}

	private void gameOverMessage(Graphics dbg) {

		final int FPS_WEIGHT = 40;
		final int BOX_WEIGHT = 10;
		final int TIME_WEIGHT = 5;

		score = (FPS_WEIGHT * (1000 / (int) period) - (BOX_WEIGHT * obs.getBoxNum())) - TIME_WEIGHT * total_time;

		dbg.setColor(Color.MAGENTA);
		dbg.setFont(new Font(Font.MONOSPACED, Font.BOLD, 80));
		dbg.drawString("Game Over Score:" + score, pWidth / 6, pHeight / 2);
	}

	private void drawButtom() {

		// Each bottom is a oval with a string over it
		// Highlighting triggers a change in the ground color

		if (isOverPause)
			dbg.setColor(Color.GREEN);
		else
			dbg.setColor(Color.BLACK);
		dbg.drawOval(pauseArea.x, pauseArea.y, pauseArea.width, pauseArea.height);

		dbg.setColor(Color.BLACK);
		if (isPause)
			dbg.drawString("Paused", pauseArea.x + 5, pauseArea.y + 5);
		else
			dbg.drawString("Pause", pauseArea.x + 5, pauseArea.y + 5);

		if (isOverQuit)
			dbg.setColor(Color.GREEN);
		else
			dbg.setColor(Color.BLACK);
		dbg.drawOval(quitArea.x, quitArea.y, quitArea.width, quitArea.height);

		dbg.setColor(Color.BLACK);
		dbg.drawString("Quit", quitArea.x + 5, quitArea.y + 5);

	}

	public void setBoxNum(int num) {
		this.boxUsed = num;
	}

	public static void main(String[] args) {
		new WormChase(1000 / 80);
	}

}
