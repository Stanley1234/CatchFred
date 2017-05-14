package FS_Updated;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import org.w3c.dom.css.Rect;

public class Obstacle {
	
	private static final int BOX_LENGTH = 12;
	
	private ArrayList<Rectangle> boxes = new ArrayList<Rectangle>();
	WormChase wcTop;
	
	public Obstacle(WormChase wc) {
		this.wcTop = wc;
	}
	
	synchronized public int getBoxNum() {
		return boxes.size();
	}
	
	synchronized public void add(int x, int y) {
		boxes.add(new Rectangle(x, y, BOX_LENGTH, BOX_LENGTH));
		wcTop.setBoxNum(boxes.size());
		
	}
	
	synchronized public void draw(Graphics dbg) {
		/*
		 * draw a series of blue boxes
		 * */
		dbg.setColor(Color.BLUE);
		for (Rectangle box : boxes) 
			dbg.fillRect(box.x, box.y, box.width, box.height);
		
	}
	
	public boolean hit(Point head, int size) {
		// only check whether the head hits or not
		for (Rectangle box : boxes) {
			Rectangle r = new Rectangle(head.x, head.y, size, size);
			if (box.intersects(r)) 
				return true;
		}
		return false;
	}
}
