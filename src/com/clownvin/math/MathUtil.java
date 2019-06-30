package com.clownvin.math;

public class MathUtil {
	
	public static float distance(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt(((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2)));
	}
	
	public static float distanceNoRoot(float x1, float y1, float x2, float y2) {
		return ((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2));
	}
	
	public static boolean doIntersect(float r1x, float r1y, float r1x2, float r1y2, float r2x, float r2y, float r2x2, float r2y2) {
		if (r1x > r2x2 || r1x2 > r2x || r1y < r2y2 || r1y2 < r2y)
			return false;
		return true;
	}
	
	public static boolean doIntersect(float oX1, float oY1, float r1, float oX2, float oY2, float r2) {
		float dist = distanceNoRoot(oX1, oY1, oX2, oY2);
		float radDist = (r1 + r2) * (r1 + r2);
		return radDist >= dist;
	}
	
	public static boolean onSegment(int pX, int pY, int qX, int qY, int rX, int rY) {
		return qX <= Math.max(pX, rX) && qX >= Math.min(pX, rX) &&
			qY <= Math.max(pY, rY) && qY >= Math.min(pY, rY);
	}
	
	public static final int COLINEAR = 0;
	public static final int CLOCKWISE = 1;
	public static final int COUNTER_CLOCKWISE= 2;
	
	public static int orientation(int pX, int pY, int qX, int qY, int rX, int rY) {
		int val = (qY - pY) * (rX - qX) -
				  (qX - pX) * (rY - qY);
		if (val == 0)
			return 0;
		return val > 0 ? 1 : 2;
	}
	
	public static boolean doIntersect(int p1X, int p1Y, int q1X, int q1Y, int p2X, int p2Y, int q2X, int q2Y) {
		int o1 = orientation(p1X, p1Y, q1X, q1Y, p2X, p2Y);
		int o2 = orientation(p1X, p1Y, q1X, q1Y, q2X, q2Y); 
	    int o3 = orientation(p2X, p2Y, q2X, q2Y, p1X, p1Y); 
	    int o4 = orientation(p2X, p2Y, q2X, q2Y, q1X, q1Y);
	    
	    if (o1 != o2 && o3 != o4)
	    	return true;
	    
	    
	    if (o1 == 0 && onSegment(p1X, p1Y, p2X, p2Y, q1X, q1Y))
	    	return true;
	    if (o2 == 0 && onSegment(p1X, p1Y, q2X, q2Y, q1X, q1Y))
	    	return true;
	    if (o3 == 0 && onSegment(p2X, p2Y, p1X, p1Y, q2X, q2Y))
	    	return true;
	    if (o4 == 0 && onSegment(p2X, p2Y, q1X, q1Y, q2X, q2Y))
	    	return true;
	    return false;
	}
	
	public static boolean lineIntersectsLine(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		float uA = ((x4-x3)*(y1-y3) - (y4-y3)*(x1-x3)) / ((y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));
		float uB = ((x2-x1)*(y1-y3) - (y2-y1)*(x1-x3)) / ((y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));
		if (uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1) {
//			float intersectionX = x1 + (uA * (x2-x1));
//		    float intersectionY = y1 + (uA * (y2-y1));
		    return true;
		  }
		  return false;
	}
	
	public static boolean lineIntersectsRectangle(float x1, float y1, float x2, float y2, float rX, float rY, float rW, float rH) {
		  boolean left =   lineIntersectsLine(x1,y1,x2,y2, rX,rY,rX, rY+rY);
		  boolean right =  lineIntersectsLine(x1,y1,x2,y2, rX+rW,rY, rX+rW,rY+rH);
		  boolean top =    lineIntersectsLine(x1,y1,x2,y2, rX,rY, rX+rW,rY);
		  boolean bottom = lineIntersectsLine(x1,y1,x2,y2, rX,rY+rH, rX+rW,rY+rH);

		  if (left || right || top || bottom) {
		    return true;
		  }
		  return false;
	}
	
	public static boolean inside(float x, float y, float x1, float y1, float x2, float y2) {
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
	}
	
	public static boolean insideLessThan(float x, float y, float x1, float y1, float x2, float y2) {
		return x1 < x && x < x2 && y1 < y && y < y2;
	}

	public static boolean inside(int x, int y, int x1, int y1, int x2, int y2) {
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
	}
}
