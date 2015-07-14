package net.astesana;

import net.astesana.android.Log;

import org.teleal.android.util.FixedAndroidHandler;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PatchLog {
	private PatchLog() {
		// To prevent instance to be instantiated
	}
	
	public static void patch(Level level) {
		// Remove buggy handlers from the root logger
		Logger logger = Logger.getAnonymousLogger().getParent();
		Handler[] handlers = logger.getHandlers();
		for (Handler handler : handlers) {
			logger.removeHandler(handler);
		}
		// Create a new bug free handler
		FixedAndroidHandler handler = new FixedAndroidHandler();
		logger.addHandler(handler);
		// Set the right level limit
		handler.setLevel(level);
		logger.setLevel(level);
		Log.v(PatchLog.class, "Logger setup done"); //$NON-NLS-1$
	}
}
