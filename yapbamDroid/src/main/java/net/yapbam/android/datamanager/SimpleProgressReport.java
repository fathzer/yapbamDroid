package net.yapbam.android.datamanager;

import net.yapbam.data.ProgressReport;

class SimpleProgressReport implements ProgressReport {
	private volatile ProgressReport internalProgressReport;
	private int max;
	private int progress;
	private volatile boolean isCancelled;
	private volatile boolean isWorking;
	
	public SimpleProgressReport() {
		this.internalProgressReport = null;
		this.max = -1;
		this.progress = 0;
		this.isCancelled = false;
		this.isWorking = false;
	}
	
	@Override
	public synchronized boolean isCancelled() {
		return this.isCancelled;
	}

	@Override
	public synchronized void setMax(int length) {
		this.max = length;
		if (this.internalProgressReport!=null) {
			this.internalProgressReport.setMax(length);
		}
	}

	@Override
	public synchronized void reportProgress(int progress) {
		this.progress = progress;
		if (internalProgressReport!=null) {
			internalProgressReport.reportProgress(progress);
		}
	}
	
	public synchronized void setProgressReport(ProgressReport report) {
		this.internalProgressReport = report;
		if ((internalProgressReport!=null) && (this.max>=0)) {
			internalProgressReport.setMax(this.max);
			internalProgressReport.reportProgress(this.progress);
		}
	}

	public synchronized void cancel() {
		this.isCancelled = true;
		this.isWorking = false;
	}
	
	public synchronized boolean isWorking() {
		return isWorking;
	}

	public synchronized void setWorking(boolean isWorking) {
		this.isWorking = isWorking;
		if (this.isWorking) {
			this.isCancelled = false;
		}
	}
}