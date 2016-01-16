package net.yapbam.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import net.yapbam.data.GlobalData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountsActivity extends AbstractYapbamActivity {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountsActivity.class);
	private ListView contentView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.contentView = (ListView) findViewById(R.id.content);
		final AccountAdapter adapter = new AccountAdapter(this, this.getDataManager());
		contentView.setAdapter(adapter);
		registerForContextMenu(contentView);
		contentView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				adapter.openTransactions(AccountsActivity.this, position);
			}
		});
	}

    @Override
    protected int getLayoutId() {
        return R.layout.activity_accounts;
    }

    @Override
	protected ViewGroup getMainViewGroup() {
		return (ViewGroup)findViewById(R.id.frameLayout);
	}
	
	@Override
	protected View getContentView() {
		return findViewById(R.id.contentLayout);
	}
	
	/** Launches the file selection activity.
	 * @param view
	 */
	public void selectFile(View view) {
		startActivityForResult(new Intent(this, SelectFileActivity.class), SelectFileActivity.SELECT_FILE_REQUEST);
	}

	@Override
	protected void onDataStateChanged() {
		TextView fileNameView = (TextView) findViewById(R.id.fileName);
		fileNameView.setText(Yapbam.getFileDisplayName(getSelectedFile()));
		GlobalData gData = getDataManager().getData();
		if (gData!=null) {
			// Compute the global accounts
			double currentBalance = 0.0;
			double finalBalance = 0.0;
			if (gData.getAccountsNumber()>1) {
				for (int i = 0; i < gData.getAccountsNumber(); i++) {
					currentBalance += gData.getAccount(i).getBalanceData().getCurrentBalance();
					finalBalance += gData.getAccount(i).getBalanceData().getFinalBalance();
				}
			}
			TextView summaryView = (TextView) findViewById(R.id.summary);
			summaryView.setText(AccountAdapter.getBalances(this, currentBalance, finalBalance));
			((AccountAdapter) contentView.getAdapter()).notifyDataSetChanged();
			findViewById(R.id.contentLayout).setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (getSelectedFile()==null) {
			selectFile(this.getCurrentFocus());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.accounts, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.selectFile) {
			startActivityForResult(new Intent(this, SelectFileActivity.class), SelectFileActivity.SELECT_FILE_REQUEST);
		}
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		LOGGER.trace("onActivityResult is called. RequestCode = {}, resultCode = {}", requestCode, resultCode); //NON-NLS
		if ((requestCode == SelectFileActivity.SELECT_FILE_REQUEST) && (getSelectedFile()==null)) {
			LOGGER.trace("no file selected => call finish"); //NON-NLS
			finish();
		}
	}

	private String getSelectedFile() {
		return Yapbam.getDataManager().getSelectedFile();
	}
}
