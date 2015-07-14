package net.astesana.android;

import java.util.logging.Level;
import java.util.logging.Logger;

/** A very simple and limited log, based on java.util.Logger.
 * <br>The main additions of this class upon the original java.util.Logger are:<ul>
 * <li>Messages are tagged by the name of the class of an origin instance.</li>
 * <li>Shorter to write than Logger calls (inspired by android.util.Log)</li>
 * </ul> 
 */
public abstract class Log {
	private Log() {
		// To prevent instantiation
	}
	
	private static void v(Class<? extends Object> theClass, String message) {
		Logger.getLogger(theClass.getName()).finest(message);
	}

	public static void v(Object obj, String message) {
		Log.v(obj.getClass(), message);
	}

	public static void w(Object obj, String message) {
		Logger.getLogger(obj.getClass().getName()).log(Level.WARNING, message);
	}

	public static void w(Object obj, String message, Throwable e) {
		Logger.getLogger(obj.getClass().getName()).log(Level.WARNING, message, e);
	}

	public static void v(Object obj, String message, Throwable e) {
		v(obj, message + ": "+e.toString());
	}
}
