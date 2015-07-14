/**
 * @author Jean-Marc Astesana
 * Small changes to work of Thomas Fankhauser and Marek Sebera (http://stackoverflow.com/questions/937313/android-basic-gesture-detection)
 */
package com.fathzer.android.gesture;

import android.view.View;

public interface SwipeInterface {
	public void bottom2top(View v);
	public void left2right(View v);
	public void right2left(View v);
	public void top2bottom(View v);
}