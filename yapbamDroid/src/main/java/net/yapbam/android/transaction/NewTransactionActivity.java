package net.yapbam.android.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fathzer.android.keyboard.DecimalKeyboard;

import net.astesana.android.Log;
import net.yapbam.android.R;
import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.SelectFileActivity;
import net.yapbam.android.Yapbam;
import net.yapbam.android.datamanager.DataManager.State;
import net.yapbam.android.keyboard.AutoHideDecimalKeyboard;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;
import net.yapbam.data.Mode;
import net.yapbam.data.Transaction;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NewTransactionActivity extends AbstractYapbamActivity {
	//FIXME Not finished
	public static final String TRANSACTION_NUMBER = "transaction_id"; //$NON-NLS-1$
	public static final String ACCOUNT_NAME = "account_name"; //$NON-NLS-1$

	private DecimalKeyboard mCustomKeyboard;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.activity_new_transaction);
		mCustomKeyboard= new AutoHideDecimalKeyboard(this, R.id.keyboardview, R.xml.deckbd );
		mCustomKeyboard.registerEditText(R.id.amount);
	}

	/** Fills the account spinner and sets the selected account.
	 * @param accountName The currently selected account
	 */
	private void fillAccountSpinner(String accountName) {
		GlobalData data = getDataManager().getData();
		List<String> accounts = new ArrayList<>(data.getAccountsNumber());
		for (int i=0;i<data.getAccountsNumber();i++) {
			accounts.add(data.getAccount(i).getName());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, accounts);
		final Spinner spinner = (Spinner) findViewById(R.id.account);
		spinner.setAdapter(adapter);
		spinner.setSelection(accounts.indexOf(accountName));
	}

	// Fills the mode spinner
	private void fillModeSpinner() {
		//TODO
	}

	@Override
	protected void onDataStateChanged() {
		Log.v(this,"onDataStateChanged called"); //$NON-NLS-1$
		if (!getDataManager().getState().equals(State.OK)) {
			return;
		}
		GlobalData data = getDataManager().getData();
		int transactionNum = getIntent().getIntExtra(TRANSACTION_NUMBER, -1);
		Transaction transaction = (transactionNum==-1 || data.getTransactionsNumber()<=transactionNum) ? null : data.getTransaction(transactionNum);
		// Transaction is null if we are creating a new activity
		String accountName = transaction==null ? getIntent().getStringExtra(ACCOUNT_NAME) : transaction.getAccount().getName();
		fillAccountSpinner(accountName);
		double amount = transaction==null?0.0:transaction.getAmount();
		NumberFormat currencyInstance = DecimalFormat.getCurrencyInstance();
		NumberFormat formatter = new DecimalFormat();
		formatter.setGroupingUsed(currencyInstance.isGroupingUsed());
		formatter.setMinimumFractionDigits(currencyInstance.getMinimumFractionDigits());
		formatter.setMaximumFractionDigits(currencyInstance.getMaximumFractionDigits());
		formatter.setMinimumIntegerDigits(currencyInstance.getMinimumIntegerDigits());
		formatter.setMaximumIntegerDigits(currencyInstance.getMaximumIntegerDigits());
		((TextView) findViewById(R.id.amount)).setText(formatter.format(amount));

		DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
		Date date = transaction==null ? new Date() : transaction.getDate();
		((TextView) findViewById(R.id.date)).setText(format.format(date));
		Date valueDate = transaction==null ? date : transaction.getValueDate();
		((TextView) findViewById(R.id.valueDate)).setText(format.format(valueDate));
		Category category = transaction==null ? Category.UNDEFINED:transaction.getCategory();
		((TextView) findViewById(R.id.category)).setText(getCompound(R.string.category, category.getName()));
		Mode mode = transaction==null? Mode.UNDEFINED:transaction.getMode();
		((TextView) findViewById(R.id.mode)).setText(getCompound(R.string.mode, mode.getName()));
		if (transaction!=null) {
			if (transaction.getStatement()!=null) {
				((TextView) findViewById(R.id.statement)).setText(transaction.getStatement());
			}
			((TextView) findViewById(R.id.description)).setText(transaction.getDescription());
			((TextView) findViewById(R.id.comment)).setText(transaction.getComment());
			((TextView) findViewById(R.id.number)).setText(getCompound(R.string.number, transaction.getNumber()));
		}
//		if (transaction.getSubTransactionSize()==0) {
			findViewById(R.id.subtransactionsPanel).setVisibility(View.GONE);
//		} else {
//			ListView list = (ListView) findViewById(R.id.subtransactions);
//			list.setAdapter(new SubTransactionsAdapter(this, transaction));
//		}
	}

	@Override
	protected void onPause() {
		Log.v(this, "onPause"); //$NON-NLS-1$
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.v(this,"onResume"); //$NON-NLS-1$
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.v(this,"onSaveInstanceState"); //$NON-NLS-1$
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.v(this,"onRestoreInstanceState"); //$NON-NLS-1$
		super.onRestoreInstanceState(savedInstanceState);
	}

	public void onReceiptClicked(View v) {
		fillModeSpinner();
	}
	
	private String getCompound (int resId, String string) {
		return MessageFormat.format(getString(R.string.twoPointsFormat), getString(resId), string);
	}

	@Override
	protected ViewGroup getMainViewGroup() {
		return (ViewGroup) findViewById(R.id.frameLayout);
	}
	
	@Override
	protected View getContentView() {
		return findViewById(R.id.content);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.new_transaction, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.commit) {
			Toast.makeText(NewTransactionActivity.this, "Commit was pressed", Toast.LENGTH_SHORT).show(); //TODO
		}
		return true;
	}
}
