package net.yapbam.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.yapbam.android.datamanager.DataManager;
import net.yapbam.android.datamanager.DataManager.State;
import net.yapbam.data.GlobalData;
import net.yapbam.data.ProgressReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Observable;
import java.util.Observer;

public abstract class AbstractYapbamActivity extends Activity {
	private DataManager dataManager;
	private Observer observer;
	protected Logger logger;

	public void onCreate(Bundle savedInstanceState, int layoutId) {
		super.onCreate(savedInstanceState);
		setContentView(layoutId);
		getLayoutInflater().inflate(R.layout.view_data_management, getMainViewGroup());
		this.dataManager = Yapbam.getDataManager();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	/**
	 * Gets the view group where the "update is available" component should be
	 * attached.
	 * 
	 * @return a ViewGroup
	 */
	protected abstract ViewGroup getMainViewGroup();

	/** Gets the content view (the view that contains the data).
	 * <br>This view is automatically shown/hidden when data is available or not.
	 * @return a View
	 */
	protected abstract View getContentView();

	/**
	 * Refreshes the activity when data is changed.
	 */
	protected abstract void onDataStateChanged();

	/**
	 * Gets the data manager.
	 * 
	 * @return The data manager
	 */
	protected DataManager getDataManager() {
		return dataManager;
	}

	protected void post(Runnable runnable) {
		findViewById(R.id.updatedPanel).post(runnable);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.dataManager.getSelectedFile() != null) {
			observer = new Observer() {
				@Override
				public void update(Observable observable, Object data) {
					logger.trace("DataManager notifies change: {}",dataManager.getState()); //$NON-NLS-1$
					post(new Runnable() {
						@Override
						public void run() {
							refresh();
						}
					});
				}
			};
			this.dataManager.addObserver(observer);
			refresh();
		}
		logger.trace("Resumed"); //$NON-NLS-1$
	}

	private void refresh() {
		State state = getDataManager().getState();
		logger.trace("refresh called with state {}",state); //NON-NLS
		if (state.equals(State.ERROR) || state.equals(State.UNSUPPORTED_FORMAT) || state.equals(State.PASSWORD_REQUIRED)) {
			setStatus(Status.ERROR);
			if (state.equals(State.ERROR)) {
				((TextView)findViewById(R.id.error)).setText(Html.fromHtml(getString(R.string.open_error)));
			} else if (state.equals(State.UNSUPPORTED_FORMAT)) {
				((TextView)findViewById(R.id.error)).setText(Html.fromHtml(getString(R.string.open_unsupported)), TextView.BufferType.SPANNABLE);
			} else if (state.equals(State.PASSWORD_REQUIRED)) {
				logger.trace("Password is required"); //NON-NLS
				doPasswordRequired();
			}
		} else if (state.equals(State.DELETED)) {
			TextView tv = (TextView) findViewById(R.id.deletedMessage);
			tv.setText(MessageFormat.format(getString(R.string.file_was_deleted), Yapbam.getFileDisplayName(getDataManager().getSelectedFile())));
			setStatus(Status.DELETED);
		} else {
			GlobalData gData = getDataManager().getData();
			if (gData==null) {
				((TextView)findViewById(R.id.pleaseWait)).setText(Html.fromHtml(getString(R.string.PleaseWait)));
				getDataManager().setProgressReport(new MyProgressReport());
			} else {
				getDataManager().setProgressReport(null);
			}
			setStatus(gData!=null? Status.OK: Status.LOADING);
		}
		// Test if an update is available
		findViewById(R.id.updatedPanel).setVisibility(getDataManager().getState().equals(State.OUTDATED)?View.VISIBLE:View.GONE);
		
		// Inform the child activity that data changed
		onDataStateChanged();
	}

	private void doPasswordRequired() {
		((TextView) findViewById(R.id.error))
				.setText(getString(R.string.open_is_password_protected));
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);
		// Add edit text to alert
		alert.setView(input);
		// Set title
		alert.setTitle(R.string.password_prompt);
		alert.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Yapbam.getDataManager().setPassword(input.getText().toString());
					}
				});
		alert.setNegativeButton(getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Cancelled.
					}
				});
		AlertDialog dialog = alert.create();
		dialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}

	private final class MyProgressReport implements ProgressReport {
		private ProgressBar pg;

		private MyProgressReport() {
			this.pg = (ProgressBar) findViewById(R.id.progressBar);
		}

		@Override
		public void setMax(final int length) {
			post(new Runnable() {
				@Override
				public void run() {
					pg.setMax(length);
				}
			});
		}

		@Override
		public void reportProgress(final int progress) {
			post(new Runnable() {
				@Override
				public void run() {
					pg.setProgress(progress);
				}
			});
		}

		@Override
		public boolean isCancelled() {
			return false;
		}
	}

	private enum Status {
		LOADING, ERROR, OK, DELETED
	}

	private void setStatus(Status status) {
		findViewById(R.id.progressLayout).setVisibility(status.equals(Status.LOADING) ? View.VISIBLE : View.GONE);
		findViewById(R.id.error).setVisibility(status.equals(Status.ERROR) ? View.VISIBLE : View.GONE);
		findViewById(R.id.deletedPanel).setVisibility(status.equals(Status.DELETED)?View.VISIBLE:View.GONE);
		getContentView().setVisibility(status.equals(Status.OK)?View.VISIBLE:View.GONE);
	}

	@Override
	protected void onPause() {
		if (observer != null) {
			getDataManager().deleteObserver(observer);
		}
		logger.trace("Paused"); //$NON-NLS-1$
		super.onPause();
	}

	/**
	 * Called by the refresh button.<br>
	 * You can override this method to perform extra actions. For example, you can leave the current activity.
	 * @param view
	 *            The refresh button view
	 */
	public void refreshFile(View view) {
		logger.trace("Refresh file"); //$NON-NLS-1$
		getDataManager().refreshData();
	}

	/**
	 * Called by the change file button. <br>
	 * You can override this method to perform extra actions. For example, you
	 * can leave the current activity.
	 * 
	 * @param view
	 *            The refresh button view
	 */
	public void changeFile(View view) {
		logger.trace("change file"); //$NON-NLS-1$
		Toast.makeText(this, "Change file was called", Toast.LENGTH_SHORT).show();
	}
}
