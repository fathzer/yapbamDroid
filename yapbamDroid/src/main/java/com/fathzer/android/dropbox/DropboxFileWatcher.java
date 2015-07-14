package com.fathzer.android.dropbox;

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;
import java.util.Observer;

/** A utility class that listens to changes of a Dropbox file.
 * A file can have four states, described by the {@link FileState} enum.
 * Each time the state changes, Observers registered with this instance are notified.
 * <br>Please note that closing the file prevents any notification.
 * @author Jean-Marc Astesana
 */
public class DropboxFileWatcher extends Observable {
	private static final boolean TRACE = true;
	private static final Logger LOGGER = LoggerFactory.getLogger(DropboxFileWatcher.class);
	
	private DbxFile file;
	private DbxFile.Listener listener;
	private FileState currentState;

	/** Constructor.
	 * @param file The file to watch for changes.
	 * @throws NullPointerException if file is null
	 */
	public DropboxFileWatcher(DbxFile file) {
		this.file = file;
		listener = new DbxFile.Listener() {
			@Override
			public void onFileChange(DbxFile file) {
				try {
					if (TRACE) {
						LOGGER.trace("Receive file change event"); //$NON-NLS-1$
						LOGGER.trace("  Sync: {}",DropboxUtils.toString(file.getSyncStatus())); //$NON-NLS-1$
						try {
							LOGGER.trace("  Newest: {}",DropboxUtils.toString(file.getNewerStatus())); //$NON-NLS-1$
						} catch (DbxException.NotFound e) {
							LOGGER.trace("  Newest: DELETED"); //$NON-NLS-1$
						}
					}
					FileState old = getState();
					currentState = getCurrentState();
					if (!currentState.equals(old)) {
						if (TRACE) {
							LOGGER.trace("  -> State changed from {} to {}",old,currentState); //$NON-NLS-1$
						}
						setChanged();
						notifyObservers(old);
					}
				} catch (DbxException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
	
	private FileState getCurrentState() {
		try {
			DbxFileStatus status = file.getSyncStatus();
			if (TRACE) {
				LOGGER.trace("getCurrentState obtain status: {}", status);
			}
			if (!status.isCached) {
				// No file available
				return FileState.NOT_READY;
			}
			if (status.isLatest) {
				// The file is available and is the latest
				return FileState.UPTODATE;
			}
			try {
				DbxFileStatus newerStatus = file.getNewerStatus();
				if (TRACE) {
					LOGGER.trace("getCurrentState obtain newerStatus: {}", newerStatus);
				}
				if (newerStatus==null) {
					return FileState.UPTODATE;
				} else {
					return newerStatus.isCached?FileState.UPDATE_AVAILABLE:FileState.OLD;
				}
			} catch (DbxException.NotFound e) {
				if (TRACE) {
					LOGGER.trace("getCurrentState obtain newerStatus: DELETED");
				}
				return FileState.DELETED;
			}
		} catch (DbxException e) {
			throw new RuntimeException(e);
		}
	}

	public FileState getState() {
		if (currentState==null) {
			currentState = getCurrentState();
		}
		return currentState;
	}
		
	/** Stops the watcher.
	 * <br>Removes the file listener and clears the file status.
	 */
	private void stop() {
		file.removeListener(listener);
		if (TRACE) {
			LOGGER.trace("Dropbox listener is uninstalled"); //$NON-NLS-1$
		}
	}
	
	/** Starts the watcher.
	 * <br>Adds the file listener and init the file status.
	 */
	private void start() {
		this.currentState = getCurrentState();
		file.addListener(listener);
		if (TRACE) {
			LOGGER.trace("Initial file state: {}", this.currentState);
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Observable#addObserver(java.util.Observer)
	 */
	@Override
	public void addObserver(Observer observer) {
		super.addObserver(observer);
		if (TRACE) {
			LOGGER.trace("Observer added: {} remaining",countObservers()); //$NON-NLS-1$
		}
		if (countObservers()==1) {
			start();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Observable#deleteObserver(java.util.Observer)
	 */
	@Override
	public synchronized void deleteObserver(Observer observer) {
		super.deleteObserver(observer);
		if (TRACE) {
			LOGGER.trace("Observer deleted: {} remaining",countObservers()); //$NON-NLS-1$
		}
		if (countObservers()==0) {
			stop();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Observable#deleteObservers()
	 */
	@Override
	public synchronized void deleteObservers() {
		super.deleteObservers();
		if (TRACE) {
			LOGGER.trace("Observers deleted: {} remaining",countObservers()); //$NON-NLS-1$
		}
		stop();
	}
}
