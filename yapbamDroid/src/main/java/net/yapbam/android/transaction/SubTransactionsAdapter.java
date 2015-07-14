package net.yapbam.android.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.yapbam.android.R;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;
import net.yapbam.data.SubTransaction;
import net.yapbam.data.Transaction;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class SubTransactionsAdapter extends ArrayAdapter<SubTransaction> {
	static final NumberFormat FORMAT = DecimalFormat.getCurrencyInstance();

	private Context context;

	/** Constructor.
	 * @param context
	 * @param transaction The transaction
	 */
	public SubTransactionsAdapter(Context context, Transaction transaction) {
		super(context, R.layout.view_transaction, toList(transaction));
		this.context = context;
	}
	
	private static List<SubTransaction> toList(Transaction transaction) {
		ArrayList<SubTransaction> result = new ArrayList<SubTransaction>();
		for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
			result.add(transaction.getSubTransaction(i));
		}
		if (GlobalData.AMOUNT_COMPARATOR.compare(transaction.getComplement(),0.0)!=0) {
			result.add(new SubTransaction(transaction.getComplement(), null, Category.UNDEFINED));
		}
		return result;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.view_subtransaction, null);
		}
		SubTransaction transaction = (SubTransaction) getItem(position);
		TextView descriptionView = (TextView) v.findViewById(R.id.description);
		String description = transaction.getDescription();
		if (description==null) {
			description = context.getString(R.string.complement);
		}
		descriptionView.setText(description);
		TextView categoryView = (TextView) v.findViewById(R.id.category);
		categoryView.setText(transaction.getCategory().getName());
		TextView amountView = (TextView) v.findViewById(R.id.amount);
		amountView.setText(FORMAT.format(transaction.getAmount()));
		v.setBackgroundResource((position % 2 != 0) ? R.color.listViewOddBkg : R.color.listViewEvenBkg);
		return v;
	}
}
