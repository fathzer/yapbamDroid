package net.yapbam.android.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.yapbam.android.R;
import net.yapbam.android.Yapbam;
import net.yapbam.data.Account;
import net.yapbam.data.Transaction;
import net.yapbam.util.DateUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class TransactionsAdapter extends ArrayAdapter<Transaction> {
	static final NumberFormat FORMAT = DecimalFormat.getCurrencyInstance();

	private static AbstractTransactionSplitter<? extends Object> splitter;

	/** Constructor.
	 * @param context
	 * @param accountName The account's index
	 * @param page The page title to select
	 */
	public TransactionsAdapter(Context context, String accountName, Object page, AbstractTransactionSplitter<? extends Object> splitter) {
		super(context, R.layout.view_transaction, toList(accountName, page, splitter));
	}
	
	private static List<Transaction> toList(String accountName, Object page, @SuppressWarnings("rawtypes") AbstractTransactionSplitter splitter) {
		TransactionsAdapter.splitter = splitter;
		Account account = Yapbam.getDataManager().getData().getAccount(accountName);
		ArrayList<Transaction> result = new ArrayList<Transaction>();
		for (int i = 0; i < account.getTransactionsNumber(); i++) {
			Transaction transaction = account.getBalanceData().getBalanceHistory().getTransaction(i);
			if ((page==null) || splitter.isInPage(transaction, page)) {
				result.add(transaction);
			}
		}
		splitter.sort(result);
		return result;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getView(convertView, parent, position, (Transaction) getItem(position), splitter);
	}
	
	public static View getView(View convertView, ViewGroup parent, int position, Transaction transaction, TransactionDateDisplayer displayer) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(displayer.getLayoutResource(), null);
		}

		TextView descriptionView = (TextView) v.findViewById(R.id.description);
		descriptionView.setText(transaction.getDescription());
//		TextView commentView = (TextView) v.findViewById(R.id.comment);
//		if (transaction.getComment()!=null) {
//			commentView.setText(transaction.getComment());
//		} else {
//			commentView.setVisibility(View.GONE);
//		}
		Integer date = displayer.getDisplayedDate(transaction);
		if (date!=null) {
			TextView dateView = (TextView) v.findViewById(R.id.date);
			dateView.setText(Yapbam.formatShort(DateUtils.integerToDate(date)));
		}
		TextView amountView = (TextView) v.findViewById(R.id.amount);
		amountView.setText(FORMAT.format(transaction.getAmount()));
		v.setBackgroundResource(displayer.getBackgroundColorResource(position));
		return v;
	}
}
