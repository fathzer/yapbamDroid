package com.fathzer.android.dropbox;

/** The possible states of a file. */
public enum FileState {
	/** The file is not yet downloaded*/
	NOT_READY,
	/** Latest version of the file is available and the file points to it*/
	UPTODATE,
	/** An old version of the file is available, the new version is not yet available*/
	OLD,
	/** A newer version of the file is available, but the file points to an old one*/
	UPDATE_AVAILABLE,
	/** File is not available remotely (or doesn't exist)*/
	DELETED
}