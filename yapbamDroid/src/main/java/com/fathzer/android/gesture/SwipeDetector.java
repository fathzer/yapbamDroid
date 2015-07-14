package com.fathzer.android.gesture;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import net.astesana.android.Log;

/** A Swipe detector.
 * @author Jean-Marc Astesana
 * Based on work of Thomas Fankhauser and Marek Sebera (http://stackoverflow.com/questions/937313/android-basic-gesture-detection)
 */
public class SwipeDetector implements View.OnTouchListener {
	private static final boolean LOG = false;

	private int anglePrecision;
	private SwipeInterface activity;
	private int minDistance;
	private float downX, downY;

	public SwipeDetector(Context context, SwipeInterface activity) {
		this.activity = activity;
		this.anglePrecision = 15;
		minDistance = ViewConfiguration.get(context).getScaledTouchSlop();
	}
	
	/** Gets the maximum angle gap tolerated to validate a swipe.
	 *	return an int (angle in degrees)
	 */
	public int getAnglePrecision() {
		return anglePrecision;
	}

	/** Sets the maximum angle gap tolerated to validate a swipe.
	 * <br>Default value is 15°.
	 * @param anglePrecision an angle in degrees
	 */
	public void setAnglePrecision(int anglePrecision) {
		this.anglePrecision = anglePrecision;
	}

	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		if (action==MotionEvent.ACTION_DOWN) {
			downX = event.getX();
			downY = event.getY();
			return true;
		} else if (action==MotionEvent.ACTION_UP) {
			float upX = event.getX();
			float upY = event.getY();

			float deltaX = downX - upX;
			float deltaY = downY - upY;
			
			int distance = (int) Math.round(Math.sqrt(deltaX*deltaX+deltaY*deltaY));
			if (distance<minDistance) {
				// Too small to be a scroll gesture
				if (LOG) {
					Log.v(this, "Swipe was only " + distance + " long, need at least " + minDistance); //$NON-NLS-1$ //$NON-NLS-2$
				}
				v.performClick();
			} else {
				return doSwipe(v, deltaX, deltaY);
			}
		}
		return false;
	}

	private boolean doSwipe(View v, float deltaX, float deltaY) {
		// We are in front of a scroll gesture, let's have a look to its direction
		double angle = Math.atan2(deltaY, deltaX);
		int degrees = (int) Math.round(Math.toDegrees(angle));

		// which swipe
		if (inRange(degrees, 0)) {
			// right to left
			activity.right2left(v);
		} else if (inRange(degrees, 180) || inRange(degrees,-180)) {
			// left or right
			activity.left2right(v);
		} else if (inRange(degrees, 90)) {
			activity.bottom2top(v);
		} else if (inRange(degrees, -90)) {
			activity.top2bottom(v);
		} else {
			if (LOG) {
				Log.v(this, "Swipe direction was not clear enough: " + degrees); //$NON-NLS-1$
			}
			return false;
		}
		return true;
	}

	private boolean inRange(int angle, int target) {
		return Math.abs(angle-target)<anglePrecision;
	}
}