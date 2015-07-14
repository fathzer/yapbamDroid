package net.astesana.android.async;

import android.os.AsyncTask;

public abstract class ExtendedAsyncTask<T, V, W> extends AsyncTask<T, V, W> {
	private Runnable postExecuteAction;
	private Throwable e;
	
	public ExtendedAsyncTask() {
		this.postExecuteAction = null;
	}
	
	public void setPostExecuteAction(Runnable action) {
		this.postExecuteAction = action;
	}

	@Override
	protected W doInBackground(T... params) {
		try {
			return exceptionFreeDoInBackground(params);
		} catch (Exception e) {
			this.e = e;
			return null;
		}
	}

	protected abstract W exceptionFreeDoInBackground(T[] params) throws Exception;

	@Override
	protected void onPostExecute(W result) {
		if (postExecuteAction!=null) {
			postExecuteAction.run();
		}
		super.onPostExecute(result);
	}
	
	public Throwable getError() {
		return e;
	}
}
