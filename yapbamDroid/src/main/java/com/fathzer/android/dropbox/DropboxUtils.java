package com.fathzer.android.dropbox;

import com.dropbox.sync.android.DbxFileStatus;

public class DropboxUtils {
	private DropboxUtils() {
		// To prevent instantiation
	}

	public static String toString(DbxFileStatus status) {
		if (status==null) {
			return "null";
		}
		return (status.isCached?"cached":"not cached")+", "+(status.isLatest?"latest":"old")+", "+status.pending+", "+ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //NOSONAR
				status.bytesTransferred+"/"+status.bytesTotal+" ("+status.failure+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //NOSONAR
	}
}
