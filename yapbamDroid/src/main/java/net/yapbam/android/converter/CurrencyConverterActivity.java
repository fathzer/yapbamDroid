package net.yapbam.android.converter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.fathzer.android.keyboard.DecimalKeyboard;

import net.astesana.android.Log;
import net.yapbam.android.R;

import net.yapbam.android.Yapbam;
import net.yapbam.android.keyboard.AutoHideDecimalKeyboard;
import net.yapbam.currency.AbstractCurrencyConverter;
import net.yapbam.currency.CurrencyNames;
import net.yapbam.util.HtmlUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

public class CurrencyConverterActivity extends Activity {
	private static final String USD_CODE = "USD"; //$NON-NLS-1$
	private static final String EURO_CODE = "EUR";//$NON-NLS-1$
	private static final String CURRENCY_1_PREF_KEY = "Currency1"; //$NON-NLS-1$
	private static final String CURRENCY_2_PREF_KEY = "Currency2"; //$NON-NLS-1$
	private static final String CURRENCY_KEYBOARD_VISIBLE_KEY = "Currency_keyboard"; //$NON-NLS-1$
	private static final String CURRENCY_AMOUNT_KEY = "Currency_amount"; //$NON-NLS-1$
	private static final String PREFERENCE_TIME_STAMP_KEY = "Currency_time_stamp"; //$NON-NLS-1$

	private static final int IO_ERR_DIALOG = 1;
	private static final int PARSING_ERR_DIALOG = 2;
	private static final int WAIT_DIALOG = 3;
	private static final int NETWORK_REQUIRED = 4;
	private static final int NO_NETWORK_AVAILABLE = 5;

	private static ConverterUpdater task;

	private DecimalKeyboard mCustomKeyboard;
	private List<String> currencies;
	private boolean isActive;
	private AbstractCurrencyConverter converter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_currency_converter);

		mCustomKeyboard= new AutoHideDecimalKeyboard(this, R.id.keyboardview, R.xml.deckbd);
		mCustomKeyboard.registerEditText(R.id.currencyConverterAmount1);
		this.converter = ((Yapbam)getApplication()).getConverter();
		
		TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// Do nothing
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// Do nothing
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				doConvert();
			}
		};
    
		OnItemSelectedListener selectedListener = new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> view, View arg1, int arg2, long arg3) {
				// Save user choices
				String key = null;
				if (view.getId()==R.id.currencyConverter_currency1) {
					key = CURRENCY_1_PREF_KEY;
				} else if (view.getId()==R.id.currencyConverter_currency2) {
					key = CURRENCY_2_PREF_KEY;
				}
				Editor editor = PreferenceManager.getDefaultSharedPreferences(CurrencyConverterActivity.this).edit();
				editor.putString(key, currencies.get(view.getSelectedItemPosition()));
				editor.apply();
				doConvert();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// Do nothing
			}
		};
		((EditText)findViewById(R.id.currencyConverterAmount1)).addTextChangedListener(textWatcher);
		((Spinner)findViewById(R.id.currencyConverter_currency1)).setOnItemSelectedListener(selectedListener);
		((Spinner)findViewById(R.id.currencyConverter_currency2)).setOnItemSelectedListener(selectedListener);

		this.isActive = false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.currency_converter, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_hide_keyboard).setVisible(mCustomKeyboard.isCustomKeyboardVisible());
		MenuItem shRefresh = menu.findItem(R.id.action_hide_refresh);
		boolean ratesDateIsVisible = findViewById(R.id.topLayout).getVisibility()==View.VISIBLE;
		shRefresh.setTitle(ratesDateIsVisible ? R.string.currency_converter_hide_refresh:R.string.currency_converter_show_refresh);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_more_menu) {
			onPrepareOptionsMenu(item.getSubMenu());
		} else if (itemId == R.id.action_hide_keyboard && mCustomKeyboard.isCustomKeyboardVisible()) {
			mCustomKeyboard.hideCustomKeyboard();
		} else if (itemId == R.id.action_hide_refresh) {
			View view = findViewById(R.id.topLayout);
			view.setVisibility(view.getVisibility()==View.VISIBLE ? View.GONE : View.VISIBLE);
		}
		return true;
	}


	@Override
	protected void onResume() {
		Log.v(this,"Resumed"); //$NON-NLS-1$
		super.onResume();
		this.converter.addObserver(new Observer() {
			@Override
			public void update(Observable observable, Object data) {
				findViewById(R.id.currencyConverter_updateDate).post(new Runnable() {
					@Override
					public void run() {
						CurrencyConverterActivity.this.update();
					}
				});
			}
		});
		if (!isConverterInitialized()) {
			if (!Yapbam.isNetworkAvailable(getBaseContext())) {
				// First run requests an Internet connection
				Log.v(this, "no internet access"); //$NON-NLS-1$
				showDialog(NETWORK_REQUIRED);
			} else {
				launchTask(true);
			}
		} else if (CurrencyConverterActivity.task!=null) {
			Log.v(this, "Restart task"); //$NON-NLS-1$
			restartTask();
		} else {
			launchTask(false);
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		long now = System.currentTimeMillis();
		long prefTs = prefs.getLong(PREFERENCE_TIME_STAMP_KEY, 0);
		if (now-prefTs<86400000) {
			// If saved data is less than 1 day old 
			EditText amountField = (EditText)findViewById(R.id.currencyConverterAmount1);
			String amount = prefs.getString(CURRENCY_AMOUNT_KEY, "");
			amountField.setText(amount);
			amountField.setSelection(amount.length());
			if (!prefs.getBoolean(CURRENCY_KEYBOARD_VISIBLE_KEY, true) && (now-prefTs<1000)) {
				findViewById(R.id.currencyConverterAmount1).post(new Runnable() {
					@Override
					public void run() {
						mCustomKeyboard.hideCustomKeyboard();
					}
				});
			}
		}
		this.isActive = true;
		this.update();
	}

	/** Tests whether the converter is initialized or not.
	 * @return true if the converter is initialized.
	 */
	private boolean isConverterInitialized() {
		return converter.getTimeStamp()>0;
	}
	
	@Override
	protected void onPause() {
		this.isActive = false;
		if (CurrencyConverterActivity.task!=null) {
			CurrencyConverterActivity.task.setPostExecuteAction(null);
		}
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		// Selected currencies are saved each time a currency is selected => Don't save it again there
		editor.putBoolean(CURRENCY_KEYBOARD_VISIBLE_KEY, mCustomKeyboard.isCustomKeyboardVisible());
		editor.putString(CURRENCY_AMOUNT_KEY, ((EditText)findViewById(R.id.currencyConverterAmount1)).getText().toString());
		editor.putLong(PREFERENCE_TIME_STAMP_KEY, System.currentTimeMillis());
		editor.apply();
		this.converter.deleteObservers();
		Log.v(this,"Paused"); //$NON-NLS-1$
		super.onPause();
	}
	
	public void swapCurrencies(View v) {
		// Deactivate conversion while swapping in GUI
		this.isActive = false;
		Spinner firstSpinner = (Spinner)findViewById(R.id.currencyConverter_currency1);
		int first = firstSpinner.getSelectedItemPosition();
		Spinner secondSpinner = (Spinner)findViewById(R.id.currencyConverter_currency2);
		firstSpinner.setSelection(secondSpinner.getSelectedItemPosition());
		secondSpinner.setSelection(first);
		this.isActive = true;
		// update result
		doConvert();
	}

	/** Forces the rates to be updated.
	 * @param v The view that asks the update.
	 */
	public void refreshRates(View v) {
		if (Yapbam.isNetworkAvailable(getBaseContext())) {
			launchTask(true);
		} else {
			showDialog(NO_NETWORK_AVAILABLE);
		}
	}

	/** Attach the activity to an asynchronous task that updates the converter.
	 * <br>If the task is null, creates a new one.
	 * <br>If the task is pending, launches the task. If it is not finished, open a wait dialog
	 * <br>This method should be called when the activity is resumed.
	 */
	private void launchTask(boolean force) {
		CurrencyConverterActivity.task = new ConverterUpdater(converter, force);
		restartTask();
	}

	private void restartTask() {
		Status status = CurrencyConverterActivity.task.getStatus();
		if (!status.equals(Status.FINISHED)) {
			CurrencyConverterActivity.task.setPostExecuteAction(new Runnable() {
				public void run() {
					onTaskFinished();
				}
			});
			if (status.equals(Status.PENDING)) {
				CurrencyConverterActivity.task.execute();
			}
			if (task.isForced()) {
				showDialog(WAIT_DIALOG);
			}
		}
	}
	
	private void onTaskFinished() {
		Log.v(this, "Task finished"); //$NON-NLS-1$
		if (task.isForced()) {
			dismissDialog(WAIT_DIALOG);
			if (task.getError()!=null) {
				processError(task.getError());			
			}
		}
	}

	private void processError(Throwable e) {
		if (e instanceof IOException) {
			Log.w(this, "Converter io exception", e); //$NON-NLS-1$
			if (Yapbam.isNetworkAvailable(getBaseContext())) {
				showDialog(IO_ERR_DIALOG);
			} else {
				showDialog(NO_NETWORK_AVAILABLE);
			}
		} else if (e instanceof ParseException) {
			Log.w(this, "Converter parsing exception", e); //$NON-NLS-1$
			showDialog(PARSING_ERR_DIALOG);
		} else {
			Log.w(this, "Converter unexpected exception", e); //$NON-NLS-1$
			throw new RuntimeException(e);
		}
	}
	
	private void onTaskCancelled() {
		Log.v(this, "Getting currency rates cancelled"); //$NON-NLS-1$
		task = null;
		// If the converter is not initialized, quit the activity
		if (!isConverterInitialized()) {
			finish();
		}
	}

	private void update() {
		if (isConverterInitialized()) {
			TextView text = (TextView) findViewById(R.id.currencyConverter_updateDate);
			Date date = new Date(converter.getRefreshTimeStamp());
			DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
			text.setText(MessageFormat.format(getString(R.string.rates_publish_date), formatter.format(date)));
		}
		currencies = Arrays.asList(converter.getCurrencies());
		Collections.sort(currencies, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				return CurrencyNames.get(lhs).compareTo(CurrencyNames.get(rhs));
			}
		});
		String[] currencyNames = new String[currencies.size()];
		for (int i = 0; i < currencies.size(); i++) {
			currencyNames[i] = CurrencyNames.get(currencies.get(i));
		}
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_list_item_1, currencyNames);
		Spinner spinner = (Spinner) findViewById(R.id.currencyConverter_currency1);
		spinner.setAdapter(adapter);
		Spinner spinner2 = (Spinner) findViewById(R.id.currencyConverter_currency2);
		spinner2.setAdapter(adapter);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (!currencies.isEmpty()) {
			// Gets the default currency according to the locale
			String defaultCurrency = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
			// Gets the default currency according to the user preferences
			int first = currencies.indexOf(prefs.getString(CURRENCY_1_PREF_KEY, defaultCurrency));
			// If the user preferred currency is not available, choose the first one
			if (first<0) {
				first = 0;
			}
			// If the first currency is not euro, choose euro as default second currency, else, choose USD
			String defaultCurrency2 = currencies.get(first).equals(EURO_CODE)?USD_CODE:EURO_CODE;
			// Gets the second default currency according to the user preferences
			int second = currencies.indexOf(prefs.getString(CURRENCY_2_PREF_KEY, defaultCurrency2));
			if (second<0) {
				// If second currency is unknown
				second = first!=0 ? 0 : currencies.size()-1;
			}
			spinner.setSelection(first);
			spinner2.setSelection(second);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id==WAIT_DIALOG) {
			ProgressDialog loadingDialog = new ProgressDialog(this);
			loadingDialog.setTitle(R.string.contacting_ecb);
			loadingDialog.setMessage(getString(R.string.PleaseWait));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					task.cancel(false);
					onTaskCancelled();
				}
			});
			return loadingDialog;
		} else {
			return createAlertDialog(id);
		}
	}

	private Dialog createAlertDialog(int id) {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		switch (id) {
		case IO_ERR_DIALOG:
			alertBuilder = alertBuilder.setMessage(getString(R.string.error_contacting_ecb));
			break;
		case PARSING_ERR_DIALOG:
			alertBuilder = alertBuilder.setMessage(getString(R.string.error_reading_rates));
			break;
		case NETWORK_REQUIRED:
			String trailer = HtmlUtils.removeHtmlTags(getString(R.string.error_no_network_available));
			String message = getString(R.string.error_network_required) + HtmlUtils.NEW_LINE_TAG + trailer;
			alertBuilder = alertBuilder.setMessage(Html.fromHtml(HtmlUtils.START_TAG+message+HtmlUtils.END_TAG));
			break;
		case NO_NETWORK_AVAILABLE:
			alertBuilder = alertBuilder.setMessage(Html.fromHtml(getString(R.string.error_no_network_available)));
			break;
		default:
			return null;
		}
		alertBuilder = alertBuilder.setCancelable(false)
		.setPositiveButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
		    	if (!isConverterInitialized()) {
		    		finish();
		    	}
		    }
		});
		return alertBuilder.create();
	}

	private void doConvert() {
		if (isActive) {
			Double amount = getAmount();
			String result;
			if (amount!=null) {
				int currency1 = ((Spinner)findViewById(R.id.currencyConverter_currency1)).getSelectedItemPosition();
				int currency2 = ((Spinner)findViewById(R.id.currencyConverter_currency2)).getSelectedItemPosition();
				double converted = converter.convert(amount, currencies.get(currency1), currencies.get(currency2));
				result = DecimalFormat.getInstance().format(converted);
			} else {
				result = ""; //$NON-NLS-1$
			}
			((EditText)findViewById(R.id.currencyConverterAmount2)).setText(result);
		}
	}
	
	private Double getAmount() {
		String content = ((EditText)findViewById(R.id.currencyConverterAmount1)).getText().toString();
		content = content.replace(mCustomKeyboard.getDecimalSeparator(), '.');
		if (content.startsWith(".")) {
			content = "0"+content;
		}
		return content.isEmpty() ? null : Double.valueOf(content);
	}
}
