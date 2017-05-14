package FS_Updated;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.beans.Expression;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.midi.VoiceStatus;
import javax.swing.CellEditor;

public class FakeSnake{

	// number of the fake snakes
	private static final int MAX_FAKE_SNAKES = 3;
	private int snakeNum;
	
	// size and number of dots in a worm
	private static final int MAX_POINTS = 40;
	private static final int DOT_SIZE = 12; // pixel

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
	// obstacle
	private Obstacle obs;
	// hole
	private Hole holeObjs;
	private int existPeriod = 0;
	private int startTime = 0;

	// store the information of snakes
	private ArrayList<Integer> headPosns = new ArrayList<Integer>();
	private ArrayList<Integer> tailPosns = new ArrayList<Integer>();
	private ArrayList<Point[]> cells = new ArrayList<Point[]>();
	private ArrayList<Integer> curCompass = new ArrayList<Integer>();
	// the existence flag
	private int existence = 0; 
	// 0 represents inexistent
	// 1 represents starting to appear
	// 2 represents completely appearing
	// 3 represents disappear
	
	// store the information of holes
	private boolean isAddHoles = false;    // when disappearing 
	public FakeSnake(int pWidth, int pHeight, Obstacle obs, Hole holeObjs, int snakeNum, int existPeriod) {
		this.pWidth = pWidth;
		this.pHeight = pHeight;
		this.obs = obs;
		this.holeObjs = holeObjs;
		this.snakeNum = snakeNum;     
		this.existPeriod = existPeriod;
		if(snakeNum > MAX_FAKE_SNAKES) {
			snakeNum = MAX_FAKE_SNAKES;
			System.err.println("Exceed the largest number of fake snakes");
		}
	}
	
	
	
	public void draw(Graphics dbg) {
		if(existence == 0) 
			return;
		
		for(int i = 0;i < snakeNum;i ++) {
			// for each fake snake, the head is red and the body is black
			Point[] cell = cells.get(i);
			int headPosn = headPosns.get(i);
			int tailPosn = tailPosns.get(i);
			
			// draw the red head 
			if(existence != 3) {
				dbg.setColor(Color.RED);
				dbg.fillOval(cell[headPosn].x, cell[headPosn].y, DOT_SIZE, DOT_SIZE);
			}
			// draw the black body
			dbg.setColor(Color.BLACK);
			int j = tailPosn;
			while(j != headPosn) {
				dbg.fillOval(cell[j].x, cell[j].y, DOT_SIZE, DOT_SIZE);
				j = (j + 1) % MAX_POINTS;
			}
		}
	}
	
	public void updateFakeSnake(int totalTime) {
		if (existence == 0) {
			if(totalTime % 15 == 0 && totalTime != 0)   
				existence = 1;    // ready to appear
			
		} else if (existence == 1) {
			initPosn(totalTime);   // intialize the positions
			startTime = totalTime;   // set starting time
			existence = 2;           // change existStatus to complete appearing
		} else if (existence == 2) {
			move();
			if (totalTime - startTime >= existPeriod)  // ready to disappear 
				existence = 3;  // change status	
		} else if (existence == 3) {
			// starting to disappear
			disappearHole(totalTime);
			disappear();
			if(isCompleteDisappear()) {
				isAddHoles = false;
				existence = 0;
			}
			
		}
	}
	
	
	private void initPosn(int startTime) {
		// remove previous info
		curCompass.clear();
		headPosns.clear();
		tailPosns.clear();
		cells.clear();
		for(int i = 0;i < snakeNum;i ++) {
			// update curCompass
			curCompass.add(0);
			
			// updates the head posn
			int headPosn = MAX_POINTS - 1;
			headPosns.add(headPosn);
			
			// update the tail posn
			int tailPosn = 0;
			tailPosns.add(tailPosn);
			
			// update the cell information
			Point start_points = new Point((int)(Math.random() * pWidth), (int)(Math.random() * pHeight));
			Point[] cell = new Point[MAX_POINTS];
			Arrays.fill(cell, start_points);	
			cells.add(cell);
			
			// adds a hole
			holeObjs.addHole(start_points.x, start_points.y, startTime);
			
		}
	}
	
	private void disappearHole(int startTime) {
		// adds a hole when the snake starts to disappear
		if(!isAddHoles) {
			for(int i = 0;i < snakeNum;i ++) {
				Point[] cell = cells.get(i);
				int worm_head_x = cell[headPosns.get(i)].x;
				int worm_head_y = cell[headPosns.get(i)].y;
				holeObjs.addHole(worm_head_x, worm_head_y, startTime);
			}
			
			
			isAddHoles = true;
		}
	}
	
	private void disappear() {
		for(int i = 0;i < snakeNum;i ++) {
			int tailPosn = tailPosns.get(i);
			tailPosn = (tailPosn + 1) % MAX_POINTS;
			tailPosns.set(i, tailPosn);
		}
	}
	
	private boolean isCompleteDisappear() {
		// because the updating rates of the three snakes are the same
		// one snake reaches the end implying all reaching
		return headPosns.get(0) == tailPosns.get(0);
	}
	
	private void move() {
		// the same methods as move() in Worm.class
		// except that the initial length of fake head is MAX_POINTS
		for(int i = 0;i < snakeNum;i ++) {
			int prevHead = headPosns.get(i);
			int prevTail = tailPosns.get(i);
			
			int newHeadPosn = (prevHead + 1) % MAX_POINTS;
			int newTailPosn = (prevTail + 1) % MAX_POINTS;
			
			// updates the headPosns and tailPosns
			headPosns.set(i, newHeadPosn);
			tailPosns.set(i, newTailPosn);                  
			
			// updates the cells
			newHead(i, prevHead);
		}
		
	}
	
	private void newHead(int index, int prevHead) {
		final int offsets[] = {2, -2, 4};
		
		int newBearing = varyBearing(index);
		Point newhead = nextPoint(prevHead, newBearing, index);
		
		if(obs.hit(newhead, DOT_SIZE)) {
			// choose some sharp turn
			for(int i = 0;i < offsets.length;i ++) {
				newBearing = curCompass.get(index) + offsets[i];
				if(newBearing < 0)
					newBearing += NUM_DIRS;
				newBearing %= NUM_DIRS;    
				
				newhead = nextPoint(prevHead, newBearing, index);
				
				if(!obs.hit(newhead, DOT_SIZE)) 
					break;
			}
		}
		
		curCompass.set(index, newBearing);
		// updates the cell
		Point[] cell = cells.get(index);
		int headPosn = headPosns.get(index);
		cell[headPosn] = newhead;
		cells.set(index, cell);
	}
	
	private int varyBearing(int index) {
		int newOffsets = probsForOffset[(int)(Math.random() * NUM_PROB)];
		
		if(curCompass.get(index) + newOffsets < 0)
			return curCompass.get(index) + newOffsets + NUM_DIRS;
		return (curCompass.get(index) + newOffsets) % NUM_DIRS;
	}
	
	private Point nextPoint(int prevHead, int newBearing, int index) {
		Point[] cell = cells.get(index);
		int prev_x = cell[prevHead].x;
		int prev_y = cell[prevHead].y;
		
		int new_x = prev_x + (int)(incrs[newBearing].x * DOT_SIZE);
		if(new_x < 0)
			new_x += pWidth;
		else if(new_x + DOT_SIZE >= pWidth)
			new_x -= pWidth;
		
		int new_y = prev_y + (int)(incrs[newBearing].y * DOT_SIZE);
		if (new_y < 0) 
			new_y += pHeight;
		else if(new_y + DOT_SIZE >= pHeight)
			new_y -= pHeight;
		return new Point(new_x, new_y);
	}
	
	
}
