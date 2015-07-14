package net.yapbam.android.datamanager;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileStatus;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.fathzer.android.dropbox.DropboxFileWatcher;
import com.fathzer.android.dropbox.DropboxUtils;
import com.fathzer.android.dropbox.FileState;

import net.yapbam.android.Yapbam;
import net.yapbam.data.GlobalData;
import net.yapbam.data.ProgressReport;
import net.yapbam.data.xml.Serializer;
import net.yapbam.data.xml.UnsupportedFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DataManager extends Observable {
	private final class FileObserver implements Observer {
		@Override
		public void update(Observable observable, Object data) {
			FileState fileState = ((DropboxFileWatcher)observable).getState();
			logger.debug("state changed from {} to {}", data, fileState);
			if (fileState==FileState.NOT_READY) {
				// The file is not ready to be red => Do nothing
				return;
			} else if (fileState==FileState.UPDATE_AVAILABLE) {
				// An remote update is available, download it
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							boolean updated = file.update();
							logger.debug("file.update() returned {}", updated);
						} catch (DbxException e) {
							throw new RuntimeException(e);
						}
					}
				});
			} else if (fileState==FileState.UPTODATE) {
				if (getData()==null) {
					refreshData();
				} else {
					// An update is locally available, the current date is outdated
					state = State.OUTDATED;
					notifyUpdate("Dropbox");
				}
			} else if (fileState==FileState.OLD && getData()==null) {
				refreshData();
			} else if (fileState==FileState.DELETED) {
				state = State.DELETED;
				notifyUpdate("Dropbox");
			}
		}
	}

	public enum State {
		OK, PASSWORD_REQUIRED, UNSUPPORTED_FORMAT, ERROR, NOT_READY, OUTDATED, DELETED
	}
	
	private Logger logger;
	private State state;  
	private volatile GlobalData data;
	private String password;
	private String fileName;
	private Executor executor;
	private SimpleProgressReport progressReport;
	private Observer flistener;
	private DbxFile file;
	private DropboxFileWatcher watcher;
	
	public DataManager() {
		this.logger = LoggerFactory.getLogger(getClass());
		this.flistener = new FileObserver();
		this.executor = Executors.newSingleThreadExecutor();
		this.password = null;
		init();
	}

	private void init() {
		if (file!=null) {
			this.file.close();
		}
		this.file = null;
		this.set(null, State.NOT_READY);
		this.progressReport = new SimpleProgressReport();
		this.fileName = getSelectedFile();
		if (fileName!=null) {
			try {
				DbxFileSystem fs = DbxFileSystem.forAccount(Yapbam.getDropboxManager().getLinkedAccount());
				file = fs.open(new DbxPath(DbxPath.ROOT, fileName));
				watcher = new DropboxFileWatcher(file);
			} catch (DbxException.NotFound e) {
				logger.trace("File doesn't exist");
				this.set(null, State.DELETED);
			} catch (DbxException e) {
				throw new RuntimeException(e);
			}
			if (countObservers()>0) {
				installListener();
			}
		}
	}
	
	private static final String SELECTED_FILE_PREF_KEY = "path"; //$NON-NLS-1$
	public final void setSelectFile(String path) {
		Editor edit = getPrefs().edit();
		edit.putString(SELECTED_FILE_PREF_KEY, path);
		edit.apply();
		setPassword(null);
	}

	/** Sets the current password.
	 * @param pwd The file's password, null to forget password.
	 */
	public void setPassword(String pwd) {
		//FIXME Probably will have some problems when account is unlinked
		logger.trace("SetPassword is called with {}",pwd); //$NON-NLS-1$
		// Stop current reading
		this.progressReport.cancel();
		uninstallListener();
		this.password = pwd;
		init();
	}

	/** Gets the selected file.
	 * @return a string or null if no file is selected
	 */
	public final String getSelectedFile() {
		if (Yapbam.getDropboxManager().hasLinkedAccount()) {
			return getPrefs().getString(SELECTED_FILE_PREF_KEY, null);
		} else {
			return null;
		}
	}

	private static SharedPreferences getPrefs() {
		return PreferenceManager.getDefaultSharedPreferences(Yapbam.getContext());
	}
	
	public void setProgressReport(ProgressReport report) {
		this.progressReport.setProgressReport(report);
	}
	
	/** Schedule reading data on a background thread.
	 */
	public final void refreshData() {
		final String userId = Yapbam.getDropboxManager().getLinkedAccount().getUserId();
		final String path = fileName;
		this.set(null, State.NOT_READY);
		notifyUpdate("refreshData");
		this.executor.execute(new Runnable(){
			@Override
			public void run() {
				readFile(userId, path);
			}
		});
	}
	
	private void installListener() {
		if (watcher!=null) {
			if (watcher.getState()!=FileState.NOT_READY && getData()==null) {
				refreshData();
			}
			watcher.addObserver(flistener);
			logger.trace("File watcher is installed"); //$NON-NLS-1$
		}
	}

	private void uninstallListener() {
		if (watcher!=null) {
			watcher.deleteObserver(flistener);
			logger.trace("Dropbox listener is uninstalled"); //$NON-NLS-1$
		}
	}
	
	private synchronized void set(GlobalData data, State state) {
		this.data = data;
		this.state = state;
	}
	
	public synchronized GlobalData getData() {
		return this.data;
	}
	
	/**
	 * @return the state
	 */
	public synchronized State getState() {
		return state;
	}
	
	/** Reads a data file.
	 * <br>As it can block, this method requires to be executed on a different thread than the UI thread. 
	 */
	private void readFile(String userId, String fileName) {
		logger.trace("Read file {}",fileName); //$NON-NLS-1$
		try {
			// Verify that user account or file name were not changed since readFile was posted
			if (!stillUptoDate(userId, fileName)) {
				return;
			}
			file.update();
			DbxFileStatus status = file.getSyncStatus();
			logger.trace("Sync status is {}",DropboxUtils.toString(status)); //$NON-NLS-1$
			if ((this.getData()==null) || status.isLatest) {
				doRead(userId, fileName);
			}
		} catch (DbxException e) {
			throw new RuntimeException(e);
		}
	}

	private void doRead(String userId, String fileName) {
		// Read the file.
		logger.trace("reading file"); //$NON-NLS-1$
		long start = System.currentTimeMillis();
		try {
			InputStream in = file.getReadStream();
			logger.trace("opening stream in {}ms",System.currentTimeMillis()-start); //$NON-NLS-1$
			try {
				try {
					progressReport.setWorking(true);
					GlobalData streamData = new Serializer().read(password, in, this.progressReport);
					if (stillUptoDate(userId, fileName)) {
						this.set(streamData, State.OK);
					}
				} catch (UnsupportedFormatException e) {
					logger.warn("Unsupported", e); //$NON-NLS-1$
					this.state = State.UNSUPPORTED_FORMAT;
				} catch (AccessControlException e) {
					logger.trace("Invalid password", e);
					this.state = State.PASSWORD_REQUIRED;
				}
			} finally {
				progressReport.setWorking(false);
				in.close();
			}
		} catch (IOException e) {
			if (stillUptoDate(userId, fileName)) {
				// If the userId or file name has changed, this error is normal.
				logger.warn("Error while reading data", e); //$NON-NLS-1$
				this.state = State.ERROR;
			}
		} finally {
			logger.trace("readFile in {}ms",System.currentTimeMillis()-start); //$NON-NLS-1$
			notifyUpdate("doRead"); //$NON-NLS-1$
		}
	}

	private void notifyUpdate(String origin) {
		FileState fileState = watcher==null?FileState.NOT_READY:watcher.getState();
		if (State.NOT_READY.equals(getState()) || !FileState.NOT_READY.equals(fileState)) {
			// We do not notify state change when file is not ready (except when data state is not ready)
			logger.info("{} sends notification to {} observers. State: {}", //$NON-NLS-1$
					new Object[]{origin, this.countObservers(), this.getState()});
			this.setChanged();
			notifyObservers();
		}
	}

	private boolean stillUptoDate(String userId, String fileName) {
		return Yapbam.getDropboxManager().hasLinkedAccount() &&
				userId.equals(Yapbam.getDropboxManager().getLinkedAccount().getUserId()) &&
				fileName.equals(getSelectedFile());
	}

	/* (non-Javadoc)
	 * @see java.util.Observable#addObserver(java.util.Observer)
	 */
	@Override
	public void addObserver(Observer observer) {
		super.addObserver(observer);
		logger.trace("Observer added: {}",countObservers()); //$NON-NLS-1$
		if (countObservers()==1) {
			installListener();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Observable#deleteObserver(java.util.Observer)
	 */
	@Override
	public synchronized void deleteObserver(Observer observer) {
		super.deleteObserver(observer);
		logger.trace("Observer deleted: {}",countObservers()); //$NON-NLS-1$
		if (countObservers()==0) {
			uninstallListener();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Observable#deleteObservers()
	 */
	@Override
	public synchronized void deleteObservers() {
		super.deleteObservers();
		logger.trace("Observers deleted: {}",countObservers()); //$NON-NLS-1$
		uninstallListener();
	}
}
