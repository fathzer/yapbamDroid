package net.yapbam.android.converter;

import android.content.Context;

import net.yapbam.remote.Cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class AndroidFileCache implements Cache {
	private static final String FILE_NAME = "converterData"; //$NON-NLS-1$
	private Context context;
	
	public AndroidFileCache(Context context) {
		this.context = context;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new DeflaterOutputStream(context.openFileOutput(getTmpFile(),Context.MODE_PRIVATE));
	}

	private String getTmpFile() {
		return FILE_NAME+".tmp"; //$NON-NLS-1$
	}

	@Override
	public InputStream getInputStream(boolean tmp) throws IOException {
		return new InflaterInputStream(context.openFileInput(tmp?getTmpFile():FILE_NAME));
	}

	@Override
	public void commit() throws IOException {
		File tmpFile = context.getFileStreamPath(getTmpFile());
		if (tmpFile.exists()) {
			File file = context.getFileStreamPath(FILE_NAME);
			if (file.exists()) {
				file.delete();
			}
			if (!tmpFile.renameTo(context.getFileStreamPath(FILE_NAME))) {
				throw new IOException("Commit fails");
			}
		}
	}

	@Override
	public boolean isEmpty() {
		return !context.getFileStreamPath(getTmpFile()).exists() && !context.getFileStreamPath(FILE_NAME).exists();
	}

	@Override
	public long getTimeStamp() {
		File fs = context.getFileStreamPath(FILE_NAME);
		return fs.exists() ? fs.lastModified(): -1;
	}
}
