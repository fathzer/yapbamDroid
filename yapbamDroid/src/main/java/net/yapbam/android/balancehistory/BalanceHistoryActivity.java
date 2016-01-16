package net.yapbam.android.balancehistory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

import com.fathzer.android.spinner.MultiSpinner;
import com.fathzer.android.spinner.MultiSpinner.MultiSpinnerListener;

import net.yapbam.android.R;
import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.Yapbam;
import net.yapbam.android.datamanager.DataManager.State;
import net.yapbam.android.date.DatePickerFragment;
import net.yapbam.android.transaction.TransactionActivity;
import net.yapbam.data.Account;
import net.yapbam.data.AlertThreshold;
import net.yapbam.data.BalanceHistory;
import net.yapbam.data.Filter;
import net.yapbam.data.FilteredData;
import net.yapbam.data.GlobalData;
import net.yapbam.data.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class BalanceHistoryActivity extends AbstractYapbamActivity {
	private static final String ACCOUNT_NAMES_KEY = "accountNames"; //NON-NLS
	private static final String MIN_DATE_KEY ="minDate"; //NON-NLS
	private static final String MAX_DATE_KEY ="maxDate"; //NON-NLS
	public static final String DATE_PICKER = "datePicker"; //NON-NLS

	private final class SpinnerListener implements MultiSpinnerListener {
		@Override
		public void onItemsSelected(boolean[] selected) {
			if (!spinnerActivated) {
				return;
			}
			List<String> listAccountNames = new ArrayList<>();
			for (int i = 0; i < selected.length; i++) {
				if (selected[i]) {
					listAccountNames.add(spinner.getItems().get(i));
				}
			}
			String[] selectedNames = listAccountNames.toArray(new String[listAccountNames.size()]);
			if (!Arrays.equals(selectedNames, BalanceHistoryActivity.this.accountNames)) {
				// If we selected a new account
				logger.trace("Setting account to {}",Arrays.toString(selectedNames)); //NON-NLS
				BalanceHistoryActivity.this.accountNames = selectedNames;
				history = build(Yapbam.getDataManager().getData());
				Date hFrom = history.get(0).getTo();
				Date hTo = history.get(history.size()-1).getFrom();
				dateRangeUpdated();
			}
		}
	}

	private String[] accountNames;
	private BalanceHistory history;
	private AlertThreshold alertThreshold;
	private MultiSpinner spinner;
	private boolean spinnerActivated;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private Date minDate;
	private Date maxDate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Add the date range seek bar 
		ViewGroup layout = (ViewGroup) findViewById(R.id.bottomLayout);
		spinner = (MultiSpinner)findViewById(R.id.spinner);
		spinner.setDialogTitle(getString(R.string.account));
		spinner.setSelectAllText(getString(R.string.all_male));
		spinner.setMultiSpinnerListener(new SpinnerListener());
		if (savedInstanceState==null) {
			accountNames = new String[]{getIntent().getStringExtra(Yapbam.ACCOUNT_NAME)};
		} else {
			accountNames = (String[]) savedInstanceState.getCharSequenceArray(ACCOUNT_NAMES_KEY);
			minDate = (Date) savedInstanceState.getSerializable(MIN_DATE_KEY);
			maxDate = (Date) savedInstanceState.getSerializable(MAX_DATE_KEY);
		}
		final ExpandableListView list = (ExpandableListView)findViewById(R.id.balanceHistory);
        list.setOnChildClickListener(new OnChildClickListener() {
             @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
 				Transaction transaction = (Transaction)list.getExpandableListAdapter().getChild(groupPosition, childPosition);
 				Intent intent = new Intent(BalanceHistoryActivity.this, TransactionActivity.class);
 				intent.putExtra(TransactionActivity.TRANSACTION_NUMBER, Yapbam.getDataManager().getData().indexOf(transaction));
 				startActivity(intent);
                return true;
            }
        });
		findViewById(R.id.from).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				minDate = null;
				dateRangeUpdated();
				return true;
			}
		});
		findViewById(R.id.to).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				maxDate = null;
				dateRangeUpdated();
				return true;
			}
		});
	}

    @Override
    protected int getLayoutId() {
        return R.layout.activity_balance_history;
    }

    private BalanceHistory build(GlobalData data) {
		if (accountNames.length==1) {
			Account account = data.getAccount(accountNames[0]);
			if (account==null) {
				return null;
			}
			alertThreshold = account.getAlertThreshold();
			return account.getBalanceData().getBalanceHistory();
		} else {
			FilteredData fData = new FilteredData(data);
			Filter filter = fData.getFilter();
			List<Account> accounts = new ArrayList<>(accountNames.length);
			for (int i = 0; i < accountNames.length; i++) {
				Account account = data.getAccount(accountNames[i]);
				if (account==null) {
					return null;
				}
				accounts.add(account);
			}
			filter.setValidAccounts(accounts);
			alertThreshold = null;
			return fData.getBalanceData().getBalanceHistory();
		}
	}

	@Override
	protected void onDataStateChanged() {
		if (Yapbam.getDataManager().getState().equals(State.OK)) {
			GlobalData data = Yapbam.getDataManager().getData();
			this.history = build(data);
			if (history==null) {
				finish();
			}
			TextView accountNameView = (TextView)findViewById(R.id.accountName);
			if (data.getAccountsNumber()==0) {
				accountNameView.setText("No account");
			} else if (data.getAccountsNumber()==1) {
				accountNameView.setText(data.getAccount(0).getName());
			} else {
				// Create an ArrayAdapter using the string array and a default spinner layout
				final String[] allAccountNames = new String[data.getAccountsNumber()];
				for (int i = 0; i < allAccountNames.length; i++) {
					allAccountNames[i] = data.getAccount(i).getName();
				}
				// Deactivate spinner events
				spinnerActivated = false;
				spinner.setItems(Arrays.asList(allAccountNames));
				Arrays.sort(allAccountNames);
				boolean[] selected = new boolean[allAccountNames.length];
				for (String name : this.accountNames) {
					selected[Arrays.binarySearch(allAccountNames, name)] = true;
				}
				spinnerActivated = true;
				spinner.setSelected(selected);
			}
			dateRangeUpdated();
			accountNameView.setVisibility(data.getAccountsNumber()==1?View.VISIBLE:View.GONE);
			spinner.setVisibility(data.getAccountsNumber()>1?View.VISIBLE:View.GONE);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putCharSequenceArray(ACCOUNT_NAMES_KEY, accountNames);
		if (minDate!=null) {
			outState.putSerializable(MIN_DATE_KEY, minDate);
		}
		if (maxDate!=null) {
			outState.putSerializable(MAX_DATE_KEY, maxDate);
		}
	}
	
	private void dateRangeUpdated() {
		logger.trace("dateRangeUpdated {}-{}", minDate, maxDate); //NON-NLS
		TextView commentView = (TextView) findViewById(R.id.dateRange);
		String comment = null;
		if (history.size()<=1) {
			comment = getResources().getString(accountNames.length==0?R.string.no_account_selected:R.string.no_transaction);
		} else if (history.size()>2) {
			final Date min = minDate==null?history.get(0).getTo():minDate;
			final Date max = maxDate==null?history.get(history.size()-1).getFrom():maxDate;
			((TextView) findViewById(R.id.from)).setText(Yapbam.formatShort(min));
			((TextView) findViewById(R.id.to)).setText(Yapbam.formatShort(max));
		}
		if (comment!=null) {
			commentView.setText(comment);
			commentView.setVisibility(View.VISIBLE);
			findViewById(R.id.dateSettings).setVisibility(View.GONE);
		} else {
			commentView.setVisibility(View.GONE);
			findViewById(R.id.dateSettings).setVisibility(View.VISIBLE);
		}
		
		// Setup the list
		ExpandableListView tv = (ExpandableListView)findViewById(R.id.balanceHistory);
		tv.setAdapter(new BalanceHistoryElementAdapter(history, alertThreshold, minDate, maxDate));
	}

	@Override
	protected ViewGroup getMainViewGroup() {
		return (ViewGroup) findViewById(R.id.frameLayout);
	}

	@Override
	protected View getContentView() {
		return findViewById(R.id.mainLayout);
	}
	public void selectFrom(View view) {
		DatePickerFragment dialog = new FromPicker();
		setUpDatePicker(dialog);
		if (minDate!=null) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(minDate);
			dialog.setDate(cal);
		}
		dialog.show(getFragmentManager(), DATE_PICKER);
	}

	public void selectTo(View view) {
		DatePickerFragment dialog = new ToPicker();
		setUpDatePicker(dialog);
		if (maxDate!=null) {
			final Calendar cal = Calendar.getInstance();
			cal.setTime(maxDate);
			dialog.setDate(cal);
		}
		dialog.show(getFragmentManager(), DATE_PICKER);
	}

	private void setUpDatePicker(DatePickerFragment dp) {
		if (history.size()>0) {
			Calendar min = Calendar.getInstance();
			min.setTime(history.get(0).getTo());
			dp.setMinDate(min);
			Calendar max = Calendar.getInstance();
			max.setTime(history.get(history.size()-1).getFrom());
			dp.setMaxDate(max);
		}
	}

	public static class FromPicker extends DatePickerFragment {
		@Override
		public void onDateSet(DatePicker view, int year, int month, int day) {
			Calendar result = new GregorianCalendar();
			result.set(year, month, day, 0, 0, 0);
			BalanceHistoryActivity activity = (BalanceHistoryActivity) getActivity();
			activity.minDate = result.getTime();
			activity.dateRangeUpdated();
		}
	}

	public static class ToPicker extends DatePickerFragment {
		@Override
		public void onDateSet(DatePicker view, int year, int month, int day) {
			Calendar result = new GregorianCalendar();
			result.set(year, month, day, 0, 0, 0);
			BalanceHistoryActivity activity = (BalanceHistoryActivity) getActivity();
			activity.maxDate = result.getTime();
			activity.dateRangeUpdated();
		}
	}
}
