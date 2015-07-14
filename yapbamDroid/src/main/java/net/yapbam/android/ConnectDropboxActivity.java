package net.yapbam.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.dropbox.sync.android.DbxAccountManager;

import net.astesana.android.Log;

public class ConnectDropboxActivity extends Activity {
	private static final int DIALOG_NO_CONNECTION = 0;
	private static final int CONNECT_DROPBOX = 0;

	private boolean authenticationInProgress;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect_dropbox);
	}

	public void authenticate(View view) {
		// The phone is not linked with a Dropbox account
		if (!Yapbam.isNetworkAvailable(this)) {
			// If the network is not available
			showDialog(DIALOG_NO_CONNECTION);
		} else {
			this.authenticationInProgress = true;
			DbxAccountManager dropboxManager = Yapbam.getDropboxManager();
			if (dropboxManager.hasLinkedAccount()) {
				dropboxManager.unlink();
			}
			dropboxManager.startLink((Activity)this, CONNECT_DROPBOX);
		}
	}
	
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		if (id == DIALOG_NO_CONNECTION) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.init_no_internet_dialog);
			builder = builder.setCancelable(false).setPositiveButton(R.string.generic_close, null);
			dialog = builder.create();
		}
		return dialog;
	}
	
	protected void onResume() {
		super.onResume();
		Log.v(this, "is resumed"); //$NON-NLS-1$
		if (authenticationInProgress) {
			authenticationInProgress = false;
			DbxAccountManager dropboxManager = Yapbam.getDropboxManager();
			if (dropboxManager.hasLinkedAccount()) {
				try {
					Log.v(this, "Account is " + dropboxManager.getLinkedAccount()); //$NON-NLS-1$
					setResult(RESULT_OK);					
				} catch (IllegalStateException e) {
					Log.w(this, "Error authenticating", e); //$NON-NLS-1$
					setResult(RESULT_CANCELED);
				}
			} else {
				Log.w(this, "Authentication failed"); //$NON-NLS-1$
				setResult(RESULT_CANCELED);
			}
			finish();
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != CONNECT_DROPBOX) {
			return;
		}
		if (resultCode == RESULT_OK) {
			// The dropbox connection is valid.
			Log.v(this, "Dropbox connection is set"); //$NON-NLS-1$
//				// Provide your own storeKeys to persist the access token pair
//				// A typical way to store tokens is using SharedPreferences
//				Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
//				edit.putString(YapbamConstants.DROPBOX_ACCESS_KEY, tokens.key);
//				edit.putString(YapbamConstants.DROPBOX_ACCESS_SECRET, tokens.secret);
//				edit.commit();
			setResult(RESULT_OK);
		} else if (resultCode == RESULT_CANCELED) {
			// If a Dropbox account is selected, leave the activity
			if (Yapbam.getDropboxManager().hasLinkedAccount()) {
				finish();
			}
		} else {
			Log.w(this, "Invalid Dropbox connection"); //$NON-NLS-1$
		}
	}
}
