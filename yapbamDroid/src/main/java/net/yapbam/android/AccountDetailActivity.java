package net.yapbam.android;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.yapbam.android.datamanager.DataManager;
import net.yapbam.android.datamanager.DataManager.State;
import net.yapbam.data.Account;
import net.yapbam.data.Alert;
import net.yapbam.data.BalanceData;

import java.text.MessageFormat;
import java.util.Date;

public class AccountDetailActivity extends AbstractYapbamActivity {
	private String accountName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.activity_account_detail);
		Intent intent = getIntent();
		accountName = intent.getStringExtra(Yapbam.ACCOUNT_NAME);
	}

	@Override
	protected void onDataStateChanged() {
		DataManager dataManager = Yapbam.getDataManager();
		if (dataManager.getState().equals(State.OK)) {
			Account account = dataManager.getData().getAccount(accountName);
			if (account==null) {
				// Data was updated and the account does not exist anymore
				finish();
			} else {
				setContent(account);
			}
		}
	}

	private void setContent(Account account) {
		TextView nameTV = (TextView)findViewById(R.id.accountName);
		nameTV.setText(account.getName());

		TextView summaryTV = (TextView)findViewById(R.id.summary);
		summaryTV.setText(getNbTransactions(account));
		
		TextView balancesTV = (TextView)findViewById(R.id.balances);
		balancesTV.setText(getBalances(account));

		TextView alertsTV = (TextView)findViewById(R.id.alerts);
		String alerts = getAlerts(account);
		if (alerts.length()>0) {
			alertsTV.setText(Html.fromHtml(alerts));
		}
		alertsTV.setVisibility(alerts.length()==0?View.GONE:View.VISIBLE);

		TextView commentTV = (TextView)findViewById(R.id.comment);
		String comment = account.getComment();
		if (comment!=null) {
			commentTV.setText(comment);
		}
		commentTV.setVisibility(comment==null?View.GONE:View.VISIBLE);
	}

	private String getAlerts(Account account) {
		StringBuilder builder = new StringBuilder();
		double less = account.getAlertThreshold().getLessThreshold();
		if (less!=Double.NEGATIVE_INFINITY) {
			builder.append(MessageFormat.format(getString(R.string.account_alert_under),
					Yapbam.CURRENCY_FORMAT.format(less)));
		}
		double more = account.getAlertThreshold().getMoreThreshold();
		if (more!=Double.POSITIVE_INFINITY) {
			if (builder.length()>0) {
				builder.append("<br>");
			}
			builder.append(MessageFormat.format(getString(R.string.account_alert_over),
					Yapbam.CURRENCY_FORMAT.format(more)));
		}
		Alert nextAlert = account.getFirstAlert(new Date(), null);
		if (nextAlert!=null) {
			if (builder.length()>0) {
				builder.append("<br>");
			}
			builder.append(MessageFormat.format(getString(R.string.account_next_alert),
					Yapbam.CURRENCY_FORMAT.format(nextAlert.getBalance()), Yapbam.formatShort(nextAlert.getDate())));
		}
		return builder.toString();
	}

	@Override
	protected ViewGroup getMainViewGroup() {
		return (ViewGroup) findViewById(R.id.frameLayout);
	}

	@Override
	protected View getContentView() {
		return findViewById(R.id.content);
	}
	
	private CharSequence getBalances(Account account) {
		//FIXME Android TextView does not respect non breaking spaces !!! ... one more bug in Android platform :-(
		String format = getString(R.string.account_detail_balances_format);
		BalanceData balanceData = account.getBalanceData();
		String message = MessageFormat.format(format,
				getString(R.string.account_initial_balance),
				Yapbam.CURRENCY_FORMAT.format(account.getInitialBalance()),
				getString(R.string.account_checked_balance),
				Yapbam.CURRENCY_FORMAT.format(balanceData.getCheckedBalance()),
				getString(R.string.account_current_balance),
				Yapbam.CURRENCY_FORMAT.format(balanceData.getCurrentBalance()),
				getString(R.string.account_final_balance),
				Yapbam.CURRENCY_FORMAT.format(balanceData.getFinalBalance()),
				account.getTransactionsNumber());
		return Html.fromHtml(message);
	}
	
	private CharSequence getNbTransactions(Account account) {
		String format = getString(R.string.account_detail_nbTransactions_format);
		String message = MessageFormat.format(format, account.getTransactionsNumber());
		return Html.fromHtml(message);
	}
}
