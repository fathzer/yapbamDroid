package net.yapbam.android.transaction;

import android.content.Context;

import net.astesana.android.Log;
import net.yapbam.android.Yapbam;
import net.yapbam.data.Account;
import net.yapbam.data.Transaction;
import net.yapbam.util.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DateSplitter extends AbstractTransactionSplitter<Integer> {
	private static final int SPLIT_IF_MORE_THAN = 100;

	private List<Integer> years;
	private boolean useValueDate;

	protected DateSplitter(Context context, String accountName, boolean useValueDate) {
		super(context, accountName);
		this.useValueDate = useValueDate;
	}

	@Override
	/** Gets the years available in an account.
	 * @return an ordered list of years
	 */
	public List<Integer> getPages() {
		if (years==null) {
			Account account = Yapbam.getDataManager().getData().getAccount(accountName);
			years = new ArrayList<Integer>();
			Log.v(this, "Start collecting years"); //$NON-NLS-1$
			for (int i = 0; i < account.getTransactionsNumber(); i++) {
				Transaction transaction = account.getBalanceData().getBalanceHistory().getTransaction(i);
				int year = DateUtils.getYear(useValueDate ? transaction.getValueDateAsInteger() : transaction.getDateAsInteger());
				int index = Collections.binarySearch(years, year);
				if (index<0) {
					years.add(-index-1, year);
				}
			}
			Log.v(this, "End collecting years"); //$NON-NLS-1$
			years = Collections.unmodifiableList(years);
		}
		return years;
	}

	@Override
	public boolean isInPage(Transaction transaction, Integer year) {
		return year== DateUtils.getYear(useValueDate ? transaction.getValueDateAsInteger() : transaction.getDateAsInteger());
	}

	/* (non-Javadoc)
	 * @see net.yapbam.android.transaction.AbstractTransactionSplitter#sort(java.util.ArrayList)
	 */
	@Override
	public void sort(List<Transaction> result) {
		Collections.sort(result, new Comparator<Transaction>() {
			@Override
			public int compare(Transaction lhs, Transaction rhs) {
				return getDisplayedDate(rhs)-getDisplayedDate(lhs);
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.yapbam.android.transaction.AbstractTransactionSplitter#shouldBeSplit()
	 */
	@Override
	public boolean shouldBeSplit() {
		Account account = Yapbam.getDataManager().getData().getAccount(accountName);
		int nb = account.getTransactionsNumber();
		return (nb==0) || (nb > SPLIT_IF_MORE_THAN);
	}
	
	@Override
	public Integer getDisplayedDate(Transaction transaction) {
		return useValueDate ? transaction.getValueDateAsInteger() : transaction.getDateAsInteger();
	}
}
