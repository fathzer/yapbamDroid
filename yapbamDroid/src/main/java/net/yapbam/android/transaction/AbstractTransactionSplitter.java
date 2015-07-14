package net.yapbam.android.transaction;

import android.content.Context;

import net.yapbam.android.R;
import net.yapbam.android.AccountAdapter;
import net.yapbam.android.Yapbam;
import net.yapbam.data.BalanceData;
import net.yapbam.data.Transaction;

import java.util.List;

public abstract class AbstractTransactionSplitter<T> implements TransactionDateDisplayer {
	protected String accountName;
	protected Context context;
	
	protected AbstractTransactionSplitter (Context context, String accountName) {
		this.accountName = accountName;
		this.context = context;
	}
	
	/** Gets the list of pages.
	 * @return an unmodifiable list of pages. For performance considerations, it is recommended that the list is not computed again each time the method is called.
	 */
	protected abstract List<T> getPages();
	public T getPage(int index) {
		return index<0 || index >=getPageCount() ? null : getPages().get(index);
	}
	public int getPageCount() {
		return getPages().size();
	}

	public abstract boolean isInPage(Transaction transaction, T page);
	
	public abstract void sort(List<Transaction> result);
	
	public boolean shouldBeSplit() {
		return getPageCount()!=0;
	}
	
	@Override
	public Integer getDisplayedDate(Transaction transaction) {
		return transaction.getDateAsInteger();
	}
	
	@Override
	public int getLayoutResource() {
		return R.layout.view_transaction;
	}

	@Override
	public int getBackgroundColorResource(int position) {
		return (position % 2 != 0) ? R.color.listViewOddBkg : R.color.listViewEvenBkg;
	}

	public CharSequence getTitle(int index) {
		T page = getPage(index);
		return page==null?null:page.toString();
	}
	
	public CharSequence getSummary(int index) {
		BalanceData balance = Yapbam.getDataManager().getData().getAccount(accountName).getBalanceData();
		return AccountAdapter.getBalances(context, balance.getCurrentBalance(), balance.getFinalBalance());
	}
}
