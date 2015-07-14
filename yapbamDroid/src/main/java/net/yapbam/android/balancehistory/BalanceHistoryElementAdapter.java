package net.yapbam.android.balancehistory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import net.yapbam.android.R;
import net.yapbam.android.Yapbam;
import net.yapbam.android.transaction.TransactionDateDisplayer;
import net.yapbam.android.transaction.TransactionsAdapter;
import net.yapbam.data.AlertThreshold;
import net.yapbam.data.BalanceHistory;
import net.yapbam.data.BalanceHistoryElement;
import net.yapbam.data.Transaction;

import java.util.Date;

public class BalanceHistoryElementAdapter extends BaseExpandableListAdapter {
	private BalanceHistory history;
	private AlertThreshold alertThreshold;
	private int offset;
	private int count;

	public BalanceHistoryElementAdapter(BalanceHistory history, AlertThreshold alertThreshold, Date from, Date to) {
		this.alertThreshold = alertThreshold;
		this.history = history;
		if (from==null) {
			from = history.get(0).getTo();
		}
		if (to==null) {
			to = history.get(history.size()-1).getFrom();
		}
		this.offset = from==null?0:history.find(from);
		this.count = to==null?0:history.find(to)+1-this.offset;
	}

	@Override
	public int getGroupCount() {
		return count;
	}

	@Override
	public Object getGroup(int position) {
		return history.get(groupPositionToIndex(position));
	}

	private int groupPositionToIndex(int groupPosition) {
		return offset+getGroupCount()-groupPosition-1;
	}

	@Override
	public long getGroupId(int position) {
		return position;
	}

	@Override
	public View getGroupView(int position, boolean isExpanded, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.view_history_element, null);
		}

		TextView dateView = (TextView) v.findViewById(R.id.date);
		TextView balanceView = (TextView) v.findViewById(R.id.balance);
		
		BalanceHistoryElement element = (BalanceHistoryElement) getGroup(position);
		Date date = element.getFrom();
		double balance = element.getBalance();

		dateView.setText(date==null?"": Yapbam.formatShort(date));
		balanceView.setText(Yapbam.CURRENCY_FORMAT.format(balance));
		
		boolean alert = (date!=null) /*&& (date.compareTo(new Date())>0) */&& (alertThreshold!=null) && (alertThreshold.getTrigger(balance) != 0);
		int color;
		if (alert) {
			color = R.color.listBalanceHistoryAlertBkg;
		} else {
			color = (position % 2 != 0) ? R.color.listViewOddBkg : R.color.listViewEvenBkg;
		}
		v.setBackgroundResource(color);
		return v;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return history.getTransactions(groupPositionToIndex(groupPosition)).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return 0;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		return TransactionsAdapter.getView(convertView, parent, childPosition, (Transaction) getChild(groupPosition, childPosition),
				new TransactionDateDisplayer() {
					@Override
					public int getLayoutResource() {
						return R.layout.view_balance_history_transaction;
					}
			
					@Override
					public Integer getDisplayedDate(Transaction transaction) {
						return null;
					}

					@Override
					public int getBackgroundColorResource(int position) {
						return R.color.listBalanceHistoryTransactionBkg;
					}
				});
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return history.getTransactions(groupPositionToIndex(groupPosition)).size();
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
