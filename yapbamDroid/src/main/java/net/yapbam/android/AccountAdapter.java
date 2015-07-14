package net.yapbam.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.yapbam.android.balancehistory.BalanceHistoryActivity;
import net.yapbam.android.datamanager.DataManager;
import net.yapbam.android.transaction.TransactionsActivity;
import net.yapbam.data.Account;
import net.yapbam.data.Alert;
import net.yapbam.data.Alert.Kind;
import net.yapbam.data.BalanceData;
import net.yapbam.data.GlobalData;
import net.yapbam.data.comparator.AccountComparator;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

public class AccountAdapter extends BaseAdapter {
	private static final String GT = ">"; //$NON-NLS-1$
	private static final String LT = "<"; //$NON-NLS-1$

	private final class ClickListener implements View.OnClickListener {
		private final int accountIndex;

		private ClickListener(int accountIndex) {
			this.accountIndex = accountIndex;
		}

		@Override
		public void onClick(View v) {
			final Context context = v.getContext();
			final CharSequence[] items = {context.getString(R.string.transactions_list), context.getString(R.string.account_details), context.getString(R.string.balance_history)};
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(v.getContext().getString(R.string.generic_choose_action));
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	Class<? extends Activity> cls = BalanceHistoryActivity.class;
			    	if (item==0) {
			    		cls = TransactionsActivity.class;
			    	} else if (item==1) {
			    		cls = AccountDetailActivity.class;
			    	}
			    	open(context, cls, accountIndex);
			    }
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	private DateFormat dateFormat;
	private Account[] accounts;
	private DataManager manager;

	public AccountAdapter(Context context, DataManager manager) {
		this.dateFormat = android.text.format.DateFormat.getLongDateFormat(context);
		this.manager = manager;
		updateData();
	}

	@Override
	public void notifyDataSetChanged() {
		updateData();
		super.notifyDataSetChanged();
	}

	private void updateData() {
		GlobalData data = manager.getData();
		this.accounts = data==null?new Account[0]: AccountComparator.getSortedAccounts(data, Locale.getDefault());
	}
	
	@Override
	public int getCount() {
		return accounts.length;
//		GlobalData data = manager.getData();
//		return data==null?0:data.getAccountsNumber();
	}

	@Override
	public Object getItem(int position) {
		return accounts[position];
//		return manager.getData().getAccount(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.view_account_summary, null);
		}

		TextView titleView = (TextView) v.findViewById(R.id.title);
		TextView detailView = (TextView) v.findViewById(R.id.detail);

//		Account account = manager.getData().getAccount(position);
		Account account = accounts[position];
		titleView.setText(account.getName());
		BalanceData balance = account.getBalanceData();
		detailView.setText(getBalances(detailView.getContext(), balance.getCurrentBalance(), balance.getFinalBalance()));
		Alert alert = account.getFirstAlert(new Date(), null);
		View alertGroup = v.findViewById(R.id.alertGroup);
		if (alert!=null) {
			String format = v.getContext().getString(R.string.alert_format);
			String comment = MessageFormat.format(format, dateFormat.format(alert.getDate()),
					Yapbam.CURRENCY_FORMAT.format(alert.getBalance()),
					alert.getKind().equals(Kind.IS_LESS)?LT:GT,
					Yapbam.CURRENCY_FORMAT.format(alert.getThreshold()));
			TextView alertView = (TextView) v.findViewById(R.id.alert);
			alertView.setText(comment);
			alertGroup.setVisibility(View.VISIBLE);
		} else {
			alertGroup.setVisibility(View.GONE);
		}
		// This is a good example of adding an specific handling for a part of the Adapter
		// Here, we ask the user to choose the next action when he taps on a button
		ImageView image = (ImageView) v.findViewById(R.id.detailBtn);
		final int accountIndex = position;
		image.setOnClickListener(new ClickListener(accountIndex));
		v.setBackgroundResource((position % 2 != 0) ? R.color.listViewOddBkg : R.color.listViewEvenBkg);
		return v;
	}
	
	public static Spanned getBalances(Context context, double currentBalance, double finalBalance) {
		String sCurrentBalance = Yapbam.CURRENCY_FORMAT.format(currentBalance);
		String sFinalBalance = Yapbam.CURRENCY_FORMAT.format(finalBalance);
		String total = sCurrentBalance.equals(sFinalBalance)?MessageFormat.format("<b>{0}</b>",sCurrentBalance): //$NON-NLS-1$
			MessageFormat.format(context.getString(R.string.balance_format),sCurrentBalance, sFinalBalance);
		return Html.fromHtml(total);
	}
	
	void openTransactions(Context context, int index) {
		open(context, TransactionsActivity.class, index);
	}
	
	void open(Context context, Class<? extends Activity> cls, int index) {
		Intent intent = new Intent(context, cls);
//		intent.putExtra(Yapbam.ACCOUNT_NAME, manager.getData().getAccount(index).getName());
		intent.putExtra(Yapbam.ACCOUNT_NAME, accounts[index].getName());
		context.startActivity(intent);
	}
}
