package net.yapbam.android;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFileInfo;

import net.astesana.PatchLog;
import net.astesana.android.Log;
import net.yapbam.android.converter.AndroidFileCache;
import net.yapbam.android.datamanager.DataManager;
import net.yapbam.currency.AbstractCurrencyConverter;
import net.yapbam.currency.YahooCurrencyConverter;

import java.io.IOException;
import java.net.Proxy;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;

/** This is the Yapbam application.*/
public class Yapbam extends Application {
	public static final NumberFormat CURRENCY_FORMAT = DecimalFormat.getCurrencyInstance();
	// Be aware that android.text.format.DateFormat.getDateInstance returns wrong format with some locales
	// Verified with my own phone fr_DE
	private static final DateFormat SHORT_DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);

	/** Extra data for the intent when launching activity that needs an account number */
	public static final String ACCOUNT_NAME = "AccountName"; //$NON-NLS-1$

	private static final String APP_KEY = "6iee8hc1r12pbj8"; //$NON-NLS-1$
	private static final String APP_SECRET = "0lfxsw3pamsrthb"; //$NON-NLS-1$

	private AbstractCurrencyConverter converter;
	private static DataManager data;
	private static DbxAccountManager mDbxAcctMgr;
    private static Context context;

	@Override
	public void onCreate() {
		super.onCreate();
		PatchLog.patch(Level.FINEST);
		Log.v(this, "Creating application"); //$NON-NLS-1$
//		BugSenseHandler.initAndStartSession(this, "47889974");
		doInit(getApplicationContext());
		this.converter = new YahooCurrencyConverter(Proxy.NO_PROXY, new AndroidFileCache(this));
		Log.v(this, "Application is created"); //$NON-NLS-1$
	}
	
	private static void doInit(Context context) {
		Yapbam.context = context;
		mDbxAcctMgr = DbxAccountManager.getInstance(context, APP_KEY, APP_SECRET);
		data = new DataManager();
	}

	@Override
	public void onTerminate() {
		Log.v(this, "Application terminated"); //$NON-NLS-1$
		super.onTerminate();
	}
	
	public static Context getContext() {
		return context;
	}

    /** Gets the currency converter.
	 * @return a Currency converter
	 * @throws IOException
	 * @throws ParseException
	 */
	public AbstractCurrencyConverter getConverter() {
		return converter;
	}
	
	public static DbxAccountManager getDropboxManager() {
		return mDbxAcctMgr;
	}
	
	/** Tests whether an network connection is available.
	 * @return true if network is available
	 */
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return (activeNetworkInfo != null) && activeNetworkInfo.isConnected();
	}

	/** Gets the displayed name of a path.
	 * @param name name of a valid path
	 * @return a String. Note that calling this method on a invalid file may have unpredictable results.
	 * @see SelectFileActivity#isValidFile(DbxFileInfo)
	 */
	public static String getFileDisplayName(String name) {
		if (name==null) {
			return null;
		}
		return name.substring(0, name.length()-4);
	}

	public static DataManager getDataManager() {
		return data;
	}
	
	public static String formatShort(Date date) {
		synchronized (SHORT_DATE_FORMAT) {
			return SHORT_DATE_FORMAT.format(date);
		}
	}
}
