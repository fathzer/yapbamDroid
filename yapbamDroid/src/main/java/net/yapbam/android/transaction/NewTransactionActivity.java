package net.yapbam.android.transaction;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fathzer.android.keyboard.DecimalKeyboard;

import net.astesana.android.Log;
import net.yapbam.android.R;
import net.yapbam.android.AbstractYapbamActivity;
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
    private String accountName;

    private class AccountSpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        private boolean userSelect = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            if (userSelect) {
                userSelect = false;
                accountName = getDataManager().getData().getAccount(position).getName();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
            userSelect = false;
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(this, "onCreate");
		super.onCreate(savedInstanceState);
        mCustomKeyboard = new AutoHideDecimalKeyboard(this, R.id.keyboardview, R.xml.deckbd );
		mCustomKeyboard.registerEditText(R.id.amount);
		final Spinner spinner = (Spinner) findViewById(R.id.account);
        final AccountSpinnerListener listener = new AccountSpinnerListener();
        spinner.setOnItemSelectedListener(listener);
        spinner.setOnTouchListener(listener);
	}

	/** Fills the account spinner and sets the selected account.
	 */
	private void fillAccountSpinner() {
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
		Log.v(this,"onDataStateChanged"); //$NON-NLS-1$
		if (!getDataManager().getState().equals(State.OK)) {
			return;
		}
        String name = getIntent().getStringExtra(ACCOUNT_NAME);
        getIntent().removeExtra(ACCOUNT_NAME);
        int transactionNum = getIntent().getIntExtra(TRANSACTION_NUMBER, -1);
        getIntent().removeExtra(TRANSACTION_NUMBER);
        GlobalData data = getDataManager().getData();
        if (transactionNum != -1 && transactionNum<data.getTransactionsNumber() || name!=null) {
            // if activity has been launched by user
            Log.v(this,"Initializing view");
            Transaction transaction = transactionNum!=-1?data.getTransaction(transactionNum):null;
            accountName = name!=null?name:transaction.getAccount().getName();

            NumberFormat currencyInstance = DecimalFormat.getCurrencyInstance();
            // Currency instance formatter displays the currency symbol, create a new format without this symbol.
            NumberFormat formatter = new DecimalFormat();
            formatter.setGroupingUsed(currencyInstance.isGroupingUsed());
            formatter.setMinimumFractionDigits(currencyInstance.getMinimumFractionDigits());
            formatter.setMaximumFractionDigits(currencyInstance.getMaximumFractionDigits());
            formatter.setMinimumIntegerDigits(currencyInstance.getMinimumIntegerDigits());
            formatter.setMaximumIntegerDigits(currencyInstance.getMaximumIntegerDigits());
            // Render the amount
            double amount = transaction==null?0.0:transaction.getAmount();
            ((TextView) findViewById(R.id.amount)).setText(formatter.format(Math.abs(amount)));
            ((CheckBox) findViewById(R.id.receipt)).setChecked(amount > 0);

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
            if (transaction==null || transaction.getSubTransactionSize()==0) {
                findViewById(R.id.subtransactionsPanel).setVisibility(View.GONE);
            } else {
                ListView list = (ListView) findViewById(R.id.subtransactions);
                list.setAdapter(new SubTransactionsAdapter(this, transaction));
            }
        }
        fillAccountSpinner();
	}

	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		Log.v(this,"onSaveInstanceState"); //$NON-NLS-1$
		bundle.putString(ACCOUNT_NAME, accountName);
		super.onSaveInstanceState(bundle);
	}

	@Override
	protected void onRestoreInstanceState(Bundle bundle) {
		Log.v(this,"onRestoreInstanceState"); //$NON-NLS-1$
		accountName = bundle.getString(ACCOUNT_NAME);
		super.onRestoreInstanceState(bundle);
	}

	public void onReceiptClicked(View v) {
		fillModeSpinner();
	}
	
	private String getCompound (int resId, String string) {
		return MessageFormat.format(getString(R.string.twoPointsFormat), getString(resId), string);
	}

    @Override
    protected int getLayoutId() {
        return R.layout.activity_new_transaction;
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
