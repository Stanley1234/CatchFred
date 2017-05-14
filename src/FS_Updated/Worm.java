package FS_Updated;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

public class Worm {

	// size and number of dots in a worm
	private static final int MAX_POINTS = 40;
	private static final int DOT_SIZE = 12; // pixel
	private static final int RADIUS = DOT_SIZE / 2;

	// compass and direction
	private static final int NUM_DIRS = 8;
	private static final int N = 0;
	private static final int NE = 1;
	private static final int E = 2;
	private static final int SE = 3;
	private static final int S = 4;
	private static final int SW = 5;
	private static final int W = 6;
	private static final int NW = 7;
	private int curCompass;

	// offsets
	private Point2D.Double incrs[];
	{
		incrs = new Point2D.Double[NUM_DIRS];
		incrs[N] = new Point2D.Double(0.0, -1.0);
		incrs[NE] = new Point2D.Double(0.7, -0.7);
		incrs[E] = new Point2D.Double(1.0, 0.0);
		incrs[SE] = new Point2D.Double(0.7, 0.7);
		incrs[S] = new Point2D.Double(0.0, 1.0);
		incrs[SW] = new Point2D.Double(-0.7, 0.7);
		incrs[W] = new Point2D.Double(-1.0, 0.0);
		incrs[NW] = new Point2D.Double(-0.7, -0.7);

	}

	private static final int NUM_PROB = 10;
	private int[] probsForOffset;
	{
		// 0 means keep the original direction
		// 1 means slight 1 step clockwise
		// 2 means sharp 2 steps clockwise
		probsForOffset = new int[NUM_PROB];
		probsForOffset[0] = 0;
		probsForOffset[1] = 0;
		probsForOffset[2] = 0;
		probsForOffset[3] = 0;
		probsForOffset[4] = 1;
		probsForOffset[5] = 1;
		probsForOffset[6] = -1;
		probsForOffset[7] = -1;
		probsForOffset[8] = 2;
		probsForOffset[9] = -2;
	}

	
	// panel dimension
	private int pWidth, pHeight;
	// stores the dots making up a worm
	private Point[] cell = new Point[MAX_POINTS]; // treated like a cicular buffer(integer precision)
	private int nPoints = 0;
	private int tailPosn = -1, headPosn = -1; // the index of tail and head of
		
	// obstacle
	private Obstacle obs;
	
	// hole
	private Hole holeObjs;
	
	// 0 represents all appear
	// 1 represents starting to disappear
	// 2 represents complete disappear
	// 3 represents starting to appear// buffer
	private boolean[] DisappearCell = new boolean[MAX_POINTS];
	private int existStatus = 0;
	private int startingTime = 0; // denotes when it starts to disappear
	private int loopCounter = 0; // slow down the speed it appears and disappers
	
	public Worm(int pW, int pH, Obstacle obs, Hole holeObjs) {
		this.pWidth = pW;
		this.pHeight = pH;
		this.obs = obs;
		this.holeObjs = holeObjs;
	}

	public void draw(Graphics dbg) {
		/*
		 * draw() renders the worm into the game panel worms has a read head and
		 * black body
		 */
		if (dbg != null && nPoints > 0) {		
			//  --------------- draw the worm body -------------
			// The body is black
			dbg.setColor(Color.BLACK);
			int i = tailPosn;
			while (i != headPosn) {
				if (DisappearCell[i] == false)
					dbg.fillOval(cell[i].x, cell[i].y, DOT_SIZE, DOT_SIZE);
				i = (i + 1) % MAX_POINTS;
			}
			// The head is red
			dbg.setColor(Color.RED);
			if (DisappearCell[headPosn] == false)
				dbg.fillOval(cell[headPosn].x, cell[headPosn].y, DOT_SIZE, DOT_SIZE);
			
		}
	}

	public boolean touchAtHead(int x0, int y0) {
		if (nPoints > 0) {
			if ((Math.abs(cell[headPosn].x + RADIUS - x0) < DOT_SIZE)
					&& (Math.abs(cell[headPosn].y + RADIUS - y0) < DOT_SIZE)) {
				/*
				 * This condition can be proven by mathematics using absolute
				 * inequality Note within two radii of the center of head can be
				 * viewd as touching at head Becasue 80FPS makes it impossible
				 * to hit at the head
				 */
				return true;
			}
		}
		return false;
	}

	public boolean touchAtBody(int x0, int y0) {
		if (nPoints > 0) {
			int i = tailPosn;
			while (i != headPosn) {
				if ((Math.abs(cell[i].x + RADIUS - x0) < RADIUS) && (Math.abs(cell[i].y + RADIUS - y0) < RADIUS)) {

					// Only checks a single intersection within a radius instead
					// of dot_size
					return true;
				}
				i = (i + 1) % MAX_POINTS;
			}
		}
		return false;
	}

	// updates the worm posn
	public void move() {
		/*
		 * A move causes the addition of a new dot to the front of the worm,
		 * which becomes its new head. A dot has a position and compass
		 * direction/bearing, which is derived from the position and bearing of
		 * the old head.
		 * 
		 * move() is complicated by having to deal with 3 cases: 1. when the
		 * worm is first created 2. when the worm is growing 3. when the worm is
		 * MAXPOINTS long (then the addition of a new head must be balanced by
		 * the removal of a tail dot)
		 */

		int prevPosn = headPosn; // save old head posn
		headPosn = (headPosn + 1) % MAX_POINTS; // circular buffer(cell)

		if (nPoints == 0) {
			// initialize the posn of beginning coordinate and direction
			tailPosn = headPosn;
			curCompass = (int) (Math.random() * NUM_DIRS);
			cell[headPosn] = new Point(pWidth / 2, pHeight / 2);
			nPoints++;
		} else if (nPoints >= MAX_POINTS) {
			tailPosn = (tailPosn + 1) % MAX_POINTS;
			newHead(prevPosn);
		} else {
			newHead(prevPosn);
			nPoints++;
		}

	}

	private void newHead(int prevPosn) {
		/*
		 * Create new head position and compass direction/bearing.
		 * 
		 * This has two main parts. Initially we try to generate a head by
		 * varying the old position/bearing. But if the new head hits an
		 * obstacle, then we shift to a SECOND PHASE.
		 * 
		 * In the second phase we try a head which is 90 degrees clockwise, 90
		 * degress clockwise, or 180 degrees reversed so that the obstacle is
		 * avoided. These bearings are stored in fixedOffs[].
		 * 
		 * key to this strategy is the assumption that a worm can always move
		 * around
		 */
		final int[] offset = {-2, 2, 4}; // offsets to avoid an obstacle
		int newBearing = varyBearing();

		Point newPosn = nextPoint(prevPosn, newBearing);

		// checks if the new head is placed as the same spot as an obstacle
		if (obs.hit(newPosn, DOT_SIZE)) {
			for (int i = 0; i < offset.length; i++) {
				newBearing = curCompass + offset[i]; // produces a new direction
				if (newBearing < 0)
					newBearing += NUM_DIRS;
				newBearing %= NUM_DIRS;

				newPosn = nextPoint(prevPosn, newBearing);
				if (!obs.hit(newPosn, DOT_SIZE))
					break;
			}
		}

		cell[headPosn] = newPosn;
		curCompass = newBearing;
	}

	private int varyBearing() {

		/*
		 * Create a new bearing by randomly producing a direction
		 * 
		 * The direction is restricted by probsForOffset For each new direction,
		 * it is highly likely to be the same as before It might turn slightly,
		 * and in very few cases, it will sharply turns.
		 * 
		 * Produces the index of NEW direction
		 */

		int newOffset = probsForOffset[(int) (Math.random() * NUM_PROB)];

		// the mod(%) operation is complicated by when the parameter is a
		// negative
		// use if expression instead
		if ((curCompass + newOffset) < 0)
			return (curCompass + newOffset) + NUM_DIRS;
		return (curCompass + newOffset) % NUM_DIRS;
	}

	private Point nextPoint(int prevPosn, int bearing) {

		/*
		 * prevPosn indicates the index of previous head in the cell bearing
		 * indicates the new direction Produces a new point of head according to
		 * the bearing
		 * 
		 * When the worm moves out of the screen, apply wraparound so that it
		 * can appear on another edge
		 */
		// get the increment for the compass bearing

		Point2D.Double incr = incrs[bearing];

		// If the dot size changes, the distance it moves will also change.
		// Having a constant dos_size can effectively solve this problem.
		int newX = cell[prevPosn].x + (int) (DOT_SIZE * incr.x);
		int newY = cell[prevPosn].y + (int) (DOT_SIZE * incr.y);
		// newX and newY stores the left upper position

		// apply wraparound
		if (newX + DOT_SIZE < 0)
			newX = newX + pWidth;
		else if (newX > pWidth)
			newX = newX - pWidth;
		if (newY + DOT_SIZE < 0)
			newY = newY + pHeight;
		else if (newY > pHeight)
			newY = newY - pHeight;

		return (new Point(newX, newY));
	}

	// updates the exist status
	public void updateExistStatus(int totalTime, int num) {

		if (totalTime != 0 && totalTime % 5 == 0 && existStatus == 0) {
			existStatus = 1; // start to disappear
			startingTime = totalTime;
			holeObjs.addHole(cell[headPosn].x, cell[headPosn].y, totalTime);  // adds a hole
		}

		if ((totalTime != 0) && (totalTime - startingTime) >= 2 && existStatus == 2) {
			existStatus = 3; // start to appear
			holeObjs.addHole(cell[headPosn].x, cell[headPosn].y, totalTime);  // adds a hole
		}

		updateDisappearCell(num);
		loopCounter = (loopCounter + 1) % 100;  // keep loop counter small to avoid probable overflow
	}

	private void updateDisappearCell(int num) {

		int prevHead = headPosn - 1;
		if (prevHead < 0)
			prevHead += nPoints;
		DisappearCell[headPosn] = DisappearCell[prevHead];

		if (loopCounter % 7 == 0) {
			if (existStatus == 1) { // starting to disappear
				int i = headPosn;
				// find the first one that still exists
				// the head disappears first
				while (i != tailPosn) {
					if (DisappearCell[i] == false)
						break;
					i--;
					if (i < 0)
						i += nPoints;
				}

				if (i == tailPosn) {
					DisappearCell[tailPosn] = true;
					existStatus = 2; // complete disappearing
				} else {
					// set specific number of cells disappear
					int counter = 0;
					while (counter < num) {
						DisappearCell[i] = true;
						i--;
						if (i < 0)
							i += nPoints;
						counter++;
					}
				}
			}
			if (existStatus == 3) { // starting to appear
				// find the first one that disappear
				// the head appears first
				int i = headPosn;
				while (i != tailPosn) {
					if (DisappearCell[i] == true)
						break;
					i--;
					if (i < 0)
						i += nPoints;
				}
				if (i == tailPosn) {
					DisappearCell[tailPosn] = false;
					existStatus = 0; // all appear
				} else {
					int counter = 0;
					while (counter < num) {
						DisappearCell[i] = false;
						i--;
						if (i < 0)
							i += nPoints;
						counter++;
					}
				}
			}
		}
	}
	
}
