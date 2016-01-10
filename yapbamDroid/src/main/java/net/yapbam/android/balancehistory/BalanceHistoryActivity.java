package net.yapbam.android.balancehistory;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.fathzer.android.spinner.MultiSpinner;
import com.fathzer.android.spinner.MultiSpinner.MultiSpinnerListener;
import com.fathzer.android.seekbar.RangeSeekBar;
import com.fathzer.android.seekbar.RangeSeekBar.OnRangeSeekBarChangeListener;

import net.yapbam.android.R;
import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.Yapbam;
import net.yapbam.android.datamanager.DataManager.State;
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
	private static final String ACCOUNT_NAMES_KEY = "accountNames";
	public static final String DATE_PICKER = "datePicker";

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
				logger.trace("Setting account to {}",Arrays.toString(selectedNames));
				BalanceHistoryActivity.this.accountNames = selectedNames;
				history = build(Yapbam.getDataManager().getData());
				Log.i("BalanceHistory", "from "+history.get(0).getFrom().toString()); //TODO
				Log.i("BalanceHistory", "to "+history.get(history.size()-1).getTo().toString()); //TODO
//Change range date seekbar settings				dateRangeUpdated(0f, 1.0f);
			}
		}
	}

	private String[] accountNames;
	private BalanceHistory history;
	private AlertThreshold alertThreshold;
	private RangeSeekBar seekBar;
	private MultiSpinner spinner;
	private boolean spinnerActivated;
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.activity_balance_history);
		// Add the date range seek bar 
		ViewGroup layout = (ViewGroup) findViewById(R.id.bottomLayout);
		seekBar = (RangeSeekBar) findViewById(R.id.seekBar);
		seekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener() {
			@Override
			public void onRangeSeekBarValuesChanged(RangeSeekBar bar, int minValue, int maxValue) {
				// handle changed range values
				logger.trace("User selected new range values: MIN={}, MAX=", minValue, maxValue);
				dateRangeUpdated(minValue, maxValue);
			}
		});
		spinner = (MultiSpinner)findViewById(R.id.spinner);
		spinner.setDialogTitle(getString(R.string.account));
		spinner.setSelectAllText(getString(R.string.all_male));
		spinner.setMultiSpinnerListener(new SpinnerListener());
		if (savedInstanceState==null) {
			accountNames = new String[]{getIntent().getStringExtra(Yapbam.ACCOUNT_NAME)};
		} else {
			accountNames = (String[]) savedInstanceState.getCharSequenceArray(ACCOUNT_NAMES_KEY);
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
				Toast.makeText(v.getContext(), "long on From", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		findViewById(R.id.to).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Toast.makeText(v.getContext(), "long on To", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
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
			if (data.getAccountsNumber()==1) {
				accountNameView.setText(data.getAccountsNumber()==0?"No account" : data.getAccount(0).getName());
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
			dateRangeUpdated(seekBar.getSelectedMinValue(), seekBar.getSelectedMaxValue());
			accountNameView.setVisibility(data.getAccountsNumber()==1?View.VISIBLE:View.GONE);
			spinner.setVisibility(data.getAccountsNumber()>1?View.VISIBLE:View.GONE);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putCharSequenceArray(ACCOUNT_NAMES_KEY, accountNames);
	}
	
	private static final double MILLIS_PER_DAY = 24 * 3600 * 1000;
	private void dateRangeUpdated(int minValue, int maxValue) {
		logger.trace("dateRangeUpdated {}-{}", minValue, maxValue);
		TextView commentView = (TextView) findViewById(R.id.dateRange);
		String comment = null;
		Date dFrom = null;
		Date dTo = null;
		if (history.size()<=1) {
			comment = "This account contains no transaction";
		} else if (history.size()>2) {
			int dayDistance = getDayDistance(history);
			Date start = history.get(0).getTo();
			Calendar c = Calendar.getInstance();
			c.setTime(start);
			c.add(Calendar.DATE, minValue);
			dFrom = c.getTime();
			c = Calendar.getInstance();
			c.setTime(start);
			c.add(Calendar.DATE, maxValue);
			dTo = c.getTime();
			comment = MessageFormat.format("From {0} to {1}", Yapbam.formatShort(dFrom), Yapbam.formatShort(dTo));
		}
		if (comment!=null) {
			commentView.setText(comment);
			commentView.setVisibility(View.VISIBLE);
		} else {
			commentView.setVisibility(View.GONE);
		}
		
		// Setup the list
		ExpandableListView tv = (ExpandableListView)findViewById(R.id.balanceHistory);
		tv.setAdapter(new BalanceHistoryElementAdapter(history, alertThreshold, dFrom, dTo));
	}

	private int getDayDistance(BalanceHistory history) {
		Date start = history.get(0).getTo();
		Date end = history.get(history.size()-1).getFrom();
		return (int) Math.round((end.getTime() - start.getTime()) / MILLIS_PER_DAY);
	}

	@Override
	protected ViewGroup getMainViewGroup() {
		return (ViewGroup) findViewById(R.id.frameLayout);
	}

	@Override
	protected View getContentView() {
		return findViewById(R.id.mainLayout);
	}

	public void selectTo(View view) {
		DatePickerFragment dialog = new ToPicker();
		dialog.setDate(new GregorianCalendar(2016,2,15)); //TODO
		dialog.show(getFragmentManager(), DATE_PICKER);
	}

	public void selectFrom(View view) {
		DatePickerFragment dialog = new FromPicker();
		dialog.setDate(new GregorianCalendar(2010,0,1)); //TODO
		dialog.show(getFragmentManager(), DATE_PICKER);
	}

	public abstract static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
		private static final String INIT_DATE_KEY = "dateKey";

		public void setDate(Calendar date) {
			//Pass the date in a bundle.
			Bundle bundle = new Bundle();
			bundle.putSerializable(INIT_DATE_KEY, date);
			setArguments(bundle);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			super.onCreateDialog(savedInstanceState);
			Calendar initialDate = (Calendar) getArguments().getSerializable(INIT_DATE_KEY);
			if (initialDate==null) {
				initialDate = Calendar.getInstance();
				initialDate.set(Calendar.HOUR_OF_DAY, 0);
				initialDate.set(Calendar.MINUTE, 0);
				initialDate.set(Calendar.SECOND, 0);
			}
			return new DatePickerDialog(getActivity(), this, initialDate.get(Calendar.YEAR), initialDate.get(Calendar.MONTH), initialDate.get(Calendar.DATE));
		}
	}

	public static class FromPicker extends DatePickerFragment {
		@Override
		public void onDateSet(DatePicker view, int year, int month, int day) {
			Calendar result = new GregorianCalendar();
			result.set(year, month, day, 0, 0, 0);
			((TextView)getActivity().findViewById(R.id.from)).setText(result.getTime().toString());
			//TODO
		}
	}

	public static class ToPicker extends DatePickerFragment {
		@Override
		public void onDateSet(DatePicker view, int year, int month, int day) {
			Calendar result = new GregorianCalendar();
			result.set(year, month, day, 0, 0, 0);
			((TextView)getActivity().findViewById(R.id.to)).setText(result.getTime().toString());
			//TODO
		}
	}
}
