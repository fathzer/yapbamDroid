package net.yapbam.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountInfo;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SelectFileActivity extends Activity {
	public static final int SELECT_FILE_REQUEST = 0;
	private static final Logger LOGGER = LoggerFactory.getLogger(SelectFileActivity.class);
	private static final int CONNECT_DROPBOX = 0;
	private DbxAccount.Listener accountListener;
	private List<String> paths;
	private Handler handler;
	private Timer timer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.handler = new Handler();
		this.paths = new ArrayList<String>();
		accountListener = new DbxAccount.Listener() {
			@Override
			public void onAccountChange(DbxAccount acct) {
				LOGGER.trace("onAccountChange is called");
				updateAccount();
			}
		};
		setContentView(R.layout.activity_select_file);
	    final ListView listview = (ListView) findViewById(R.id.listView1);
	    listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				LOGGER.trace("click on {}",arg2); //$NON-NLS-1$
				SelectFileActivity activity = SelectFileActivity.this;
				Yapbam.getDataManager().setSelectFile(paths.get(arg2));
				activity.setResult(RESULT_OK);
				activity.finish();
			}
	    });
	    ((TextView)findViewById(R.id.message)).setText(Html.fromHtml(getString(R.string.PleaseWait)));
	}

	public void clearAccount(View view) {
		startActivityForResult(new Intent(this, ConnectDropboxActivity.class), CONNECT_DROPBOX);
	}
		
	private DbxFileSystem getFileSystem() {
		try {
			return DbxFileSystem.forAccount(Yapbam.getDropboxManager().getLinkedAccount());
		} catch (Unauthorized e) {
			LOGGER.warn("No Dropbox file system", e);
			return null;
		}
	}
	
	private void updateAccount() {
		LOGGER.trace("updateAccount is called");
		new Thread(new Runnable() {
			@Override
			public void run() {
				DbxAccountInfo accountInfo = Yapbam.getDropboxManager().getLinkedAccount().getAccountInfo();
				final String message = accountInfo==null?getString(R.string.Connecting):
					MessageFormat.format(getString(R.string.ConnectedAs), accountInfo.displayName);
				handler.post(new Runnable() {
					@Override
					public void run() {
						TextView textView = (TextView) findViewById(R.id.fileName);
						textView.setText(Html.fromHtml(message));
					}
				});
			}
		}).start();
	}
	
	private void updateList() {
		LOGGER.trace("updateList is called");
		final DbxFileSystem fs = getFileSystem();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final List<DbxFileInfo> list = fs.listFolder(new DbxPath("/"));
					handler.post(new Runnable() {
						@Override
						public void run() {
							fillList(list);
						}
					});
				} catch (DbxException e) {
					LOGGER.error("Error while getting file list", e);
				}
			}
		}).start();
	}

	private void fillList(List<DbxFileInfo> listFolder) {
		LOGGER.trace("fillList"); //$NON-NLS-1$
		List<String> values = new ArrayList<String>();
		paths.clear();
		for (DbxFileInfo fileInfo : listFolder) {
			if (isValidFile(fileInfo)) {
				paths.add(fileInfo.path.getName());
				values.add(Yapbam.getFileDisplayName(fileInfo.path.getName()));
			}
		}
		final ListAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				view.setBackgroundResource((position % 2 != 0) ? R.color.listViewOddBkg : R.color.listViewEvenBkg);
				return view;
			}
		};
		// Warning: This method could be called by a thread that is not the UI thread, so we will use post
		final ListView listView = (ListView) findViewById(R.id.listView1);
		listView.setAdapter(adapter);
		findViewById(R.id.message).setVisibility(View.GONE);
		findViewById(R.id.progressBar).setVisibility(View.GONE);
	}

	public static boolean isValidFile(DbxFileInfo fileInfo) {
		return !fileInfo.isFolder && fileInfo.path.getName().toLowerCase().endsWith(".zip"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		LOGGER.trace("onResume"); //$NON-NLS-1$
		super.onResume();
		DbxAccountManager dropboxManager = Yapbam.getDropboxManager();
		if (!dropboxManager.hasLinkedAccount()) {
			// Launch the connect to Dropbox activity
			Intent intent = new Intent(this, ConnectDropboxActivity.class);
			startActivityForResult(intent, CONNECT_DROPBOX);
		} else {
			dropboxManager.getLinkedAccount().addListener(accountListener);
			findViewById(R.id.message).setVisibility(View.VISIBLE);
			findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
			updateAccount();
			this.timer = new Timer(true);
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					updateList();
				}
			}, 0, 6000);
		}
		LOGGER.trace("onResumed"); //$NON-NLS-1$
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == CONNECT_DROPBOX) && !Yapbam.getDropboxManager().hasLinkedAccount()) {
			finish();
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		LOGGER.trace("onPause"); //$NON-NLS-1$
		// Remove listeners
		DbxAccount linkedAccount = Yapbam.getDropboxManager().getLinkedAccount();
		if (linkedAccount!=null) {
			linkedAccount.removeListener(accountListener);
//			getFileSystem().removeSyncStatusListener(contentListener);
			timer.cancel();
			this.timer = null;
		}
		super.onPause();
		LOGGER.trace("Paused"); //$NON-NLS-1$
	}
}
