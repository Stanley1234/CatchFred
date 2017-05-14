package FS_Updated;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

public class Hole {
	private static final int MAX_HOLE_NUM = 100;
	private static final int TYPE_HOLE = 0; // black area
	private static final int TYPE_HOLE_2 = 1; // gray area
	private static final int MAX_HOLE_SIZE = 90;
	private static final int MAX_HOLE_2_SIZE = 70;
	private static final int START_HOLE_SIZE = 20;
	private static final int START_HOLE_2_SIZE = 0;
	private static final int INCR_RATES = 1; // 1 pixels per loop
	
	
	private ArrayList<Rectangle> hole;
	private ArrayList<Rectangle> hole_2;
	private ArrayList<Integer> startTime;
	private boolean[] isDisappear;
	private int existTime; // how long does the hole appear(s)

	public Hole(int existTime) {
		this.existTime = existTime;

		hole = new ArrayList<Rectangle>();
		hole_2 = new ArrayList<Rectangle>();
		startTime = new ArrayList<Integer>();
		
		isDisappear = new boolean[MAX_HOLE_NUM];
		Arrays.fill(isDisappear, false);
	}

	public void draw(Graphics dbg) {
		// --------------- draw the hole -------------
		// draw the shadow first, use grey color
		dbg.setColor(Color.GRAY);
		for (Rectangle h : hole) {
			dbg.fillOval(h.x, h.y, h.width, h.height);
		}
		// draw the hole next, use black color
		dbg.setColor(Color.BLACK);
		for (Rectangle s : hole_2) {
			dbg.fillOval(s.x, s.y, s.width, s.height);
		}
	}

	public void addHole(int worm_head_x, int worm_head_y, Integer start) {
		hole.add(new Rectangle(worm_head_x - START_HOLE_SIZE / 2, worm_head_y - START_HOLE_SIZE / 2, START_HOLE_SIZE,
				START_HOLE_SIZE));
		hole_2.add(new Rectangle(worm_head_x - START_HOLE_2_SIZE / 2, worm_head_y - START_HOLE_2_SIZE / 2,
				START_HOLE_2_SIZE, START_HOLE_2_SIZE));
		startTime.add(start);
	}

	public void updateExistency(int curTime) {

		swellHoles();

		// checks the holes that exist for certain period
		for (int i = 0; i < hole.size(); i++)
			if (curTime - startTime.get(i) >= existTime)
				isDisappear[i] = true;
		
		shrinkHoles();

		// remove the holes that complete shrinking
		int counter = 0;
		for (int i = 0; i < hole.size(); i++) {
			int hole_width = hole.get(i - counter).width;
			int hole_height = hole.get(i - counter).height;
			int hole_2_width = hole_2.get(i - counter).width;
			int hole_2_height = hole_2.get(i - counter).height;
			
			// ddetermines whether complete shrinking
			if(isCompleteShrink(hole_width, hole_height) && isCompleteShrink(hole_2_width, hole_2_height)) {
				System.out.println(1);
				hole.remove(i - counter);
				hole_2.remove(i - counter);
				startTime.remove(i - counter);
				isDisappear[i] = false;
				counter ++;
			}
		}
	}

	private void swellHoles() {
		/*
		 * create image effects so that the hole is swelling
		 */

		for (int i = 0; i < hole.size(); i++) {

			if(isDisappear[i])   // if it's shrinking, continue 
				continue;
			
			int hole_width = hole.get(i).width;
			int hole_height = hole.get(i).height;
			int hole_2_width = hole_2.get(i).width;
			int hole_2_height = hole_2.get(i).height;

			int worm_head_x = hole.get(i).x + hole_width / 2;
			int worm_head_y = hole.get(i).y + hole_height / 2;

			// increases the radius
			// in order to be extendable, regard it as a oval instead of a circle
			hole_width += INCR_RATES;
			hole_height += INCR_RATES;
			hole_2_width += INCR_RATES;
			hole_2_height += INCR_RATES;

			if (!isReachMaxSize(hole_width, hole_height, TYPE_HOLE))
				hole.set(i, new Rectangle(worm_head_x - hole_width / 2, worm_head_y - hole_height / 2, hole_width,
						hole_height));

			if (!isReachMaxSize(hole_2_width, hole_2_height, TYPE_HOLE_2))
				hole_2.set(i, new Rectangle(worm_head_x - hole_2_width / 2, worm_head_y - hole_2_height / 2,
						hole_2_width, hole_2_height));

		}
	}

	private void shrinkHoles() {
		/*
		 * create image effects so that the hole is shrinking
		 */

		for (int i = 0; i < hole.size(); i++) {

			if(!isDisappear[i])
				continue;
			
			int hole_width = hole.get(i).width;
			int hole_height = hole.get(i).height;
			int hole_2_width = hole_2.get(i).width;
			int hole_2_height = hole_2.get(i).height;

			int worm_head_x = hole.get(i).x + hole_width / 2;
			int worm_head_y = hole.get(i).y + hole_height / 2;

			// increases the radius
			// in order to be extendable, regard it as a oval instead of a
			// circle
			hole_width -= INCR_RATES;
			hole_height -= INCR_RATES;
			hole_2_width -= INCR_RATES;
			hole_2_height -= INCR_RATES;

			// because hole_2 is smaller, which completes shrinking first
			if(hole_2_height < 0)
				hole_2_height = 0;
			if(hole_2_width < 0)
				hole_2_width = 0;   
			
			
			hole.set(i, new Rectangle(worm_head_x - hole_width / 2, worm_head_y - hole_height / 2, hole_width,
						hole_height));
			hole_2.set(i, new Rectangle(worm_head_x - hole_2_width / 2, worm_head_y - hole_2_height / 2,
						hole_2_width, hole_2_height));
			
		}
	}

	private boolean isReachMaxSize(int width, int height, int holeType) {
		if (holeType == TYPE_HOLE)
			return width == MAX_HOLE_SIZE && height == MAX_HOLE_SIZE;
		return width == MAX_HOLE_2_SIZE && height == MAX_HOLE_2_SIZE;
	}

	private boolean isCompleteShrink(int width, int height) {
		return width <= 0 && height <= 0;
	}
}
