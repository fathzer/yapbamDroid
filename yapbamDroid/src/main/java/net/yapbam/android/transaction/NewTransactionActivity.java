package net.yapbam.android.transaction;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.fathzer.android.spinner.UserOnlySpinnerListener;

import net.astesana.android.Log;
import net.yapbam.android.R;
import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.datamanager.DataManager.State;
import net.yapbam.android.date.DatePickerFragment;
import net.yapbam.android.keyboard.AutoHideDecimalKeyboard;
import net.yapbam.data.GlobalData;
import net.yapbam.data.Mode;
import net.yapbam.data.Transaction;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class NewTransactionActivity extends AbstractYapbamActivity {
	//FIXME Not finished
    public static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
	public static final String TRANSACTION_NUMBER = "transaction_id"; //$NON-NLS-1$
	public static final String ACCOUNT_NAME = "account_name"; //$NON-NLS-1$
    private static final String DATE = "date"; //$NON-NLS-1$
    private static final String VALUE_DATE = "value_date"; //$NON-NLS-1$
    private static final String CATEGORY = "category"; //$NON-NLS-1$

    public static final String DATE_PICKER = "datePicker"; //NON-NLS

    private String accountName;
    private int categoryIndex;

    public static class DatePicker extends DatePickerFragment {
        private static final String FIELD_ID_KEY = "id"; //NON-NLS

        private void setFieldId(int id) {
            getBundle().putInt(FIELD_ID_KEY, id);
        }

        @Override
        public void onDateSet(android.widget.DatePicker view, int year, int month, int day) {
            Calendar result = new GregorianCalendar();
            result.set(year, month, day, 0, 0, 0);
            int id = getArguments().getInt(FIELD_ID_KEY);
            ((TextView)getActivity().findViewById(id)).setText(DATE_FORMAT.format(result.getTime()));
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(this, "onCreate");
		super.onCreate(savedInstanceState);
        DecimalKeyboard mCustomKeyboard = new AutoHideDecimalKeyboard(this, R.id.keyboardview, R.xml.deckbd );
		mCustomKeyboard.registerEditText(R.id.amount);
		final Spinner accountSpinner = (Spinner) findViewById(R.id.account);
        final UserOnlySpinnerListener accountListener = new UserOnlySpinnerListener() {
            protected void doSelect(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                accountName = getDataManager().getData().getAccount(position).getName();
                Log.v(this, "set account to "+accountName);

            }
        };
        accountSpinner.setOnItemSelectedListener(accountListener);
        accountSpinner.setOnTouchListener(accountListener);
        final Spinner categorySpinner = (Spinner) findViewById(R.id.category);
        final UserOnlySpinnerListener categoryListener = new UserOnlySpinnerListener() {
            protected void doSelect(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                categoryIndex = position;
                Log.v(this, "set cat to "+categoryIndex);
            }
        };
        categorySpinner.setOnItemSelectedListener(categoryListener);
        categorySpinner.setOnTouchListener(categoryListener);
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
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner spinner = (Spinner) findViewById(R.id.account);
		spinner.setAdapter(adapter);
		spinner.setSelection(accounts.indexOf(accountName));
    }

    /** Fills the mode spinner.
     */
	private void fillModeSpinner() {
		//TODO
	}

    /** Fills the category spinner.
     */
    private void fillCategorySpinner() {
        GlobalData data = getDataManager().getData();
        List<String> categories = new ArrayList<>(data.getCategoriesNumber());
        for (int i=0;i<data.getCategoriesNumber();i++) {
            categories.add(data.getCategory(i).getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner spinner = (Spinner) findViewById(R.id.category);
        spinner.setAdapter(adapter);
        spinner.setSelection(categoryIndex);
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

            Date date = transaction==null ? new Date() : transaction.getDate();
            ((TextView) findViewById(R.id.date)).setText(DATE_FORMAT.format(date));
            Date valueDate = transaction==null ? date : transaction.getValueDate();
            ((TextView) findViewById(R.id.valueDate)).setText(DATE_FORMAT.format(valueDate));

            categoryIndex = transaction==null ? 0:data.indexOf(transaction.getCategory());

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
        fillModeSpinner();
        fillCategorySpinner();
	}

	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		Log.v(this,"onSaveInstanceState"); //$NON-NLS-1$
		bundle.putString(ACCOUNT_NAME, accountName);
        bundle.putString(DATE, ((TextView) findViewById(R.id.date)).getText().toString());
        bundle.putString(VALUE_DATE, ((TextView)findViewById(R.id.valueDate)).getText().toString());
        bundle.putInt(CATEGORY, categoryIndex);
		super.onSaveInstanceState(bundle);
	}

	@Override
	protected void onRestoreInstanceState(Bundle bundle) {
		Log.v(this,"onRestoreInstanceState"); //$NON-NLS-1$
		accountName = bundle.getString(ACCOUNT_NAME);
        ((TextView) findViewById(R.id.date)).setText(bundle.getString(DATE));
        ((TextView) findViewById(R.id.valueDate)).setText(bundle.getString(VALUE_DATE));
        categoryIndex = bundle.getInt(CATEGORY);
        Log.v(this, "restore account to "+accountName+", cat to "+categoryIndex);
		super.onRestoreInstanceState(bundle);
	}

	public void onReceiptClicked(View v) {
		fillModeSpinner();
	}

    public void onDateClicked(View v) {
        DatePicker dialog = new DatePicker();
        dialog.setFieldId(v.getId());
        final Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(DATE_FORMAT.parse(((TextView)v).getText().toString()));
            dialog.setDate(cal);
            dialog.show(getFragmentManager(), DATE_PICKER);
        } catch (ParseException e) {
            Log.w(this,"Unable to parse date field");
        }
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
