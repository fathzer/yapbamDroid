package net.yapbam.android.transaction;

import android.content.Context;
import android.text.Html;

import net.yapbam.android.R;
import net.yapbam.android.Yapbam;
import net.yapbam.data.Account;
import net.yapbam.data.Statement;
import net.yapbam.data.Transaction;
import net.yapbam.util.NullUtils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class StatementSplitter extends AbstractTransactionSplitter<Statement> {
	private List<Statement> statements;

	protected StatementSplitter(Context context, String accountName) {
		super(context, accountName);
	}

	@Override
	protected List<Statement> getPages() {
		if (statements==null) {
			Account account = Yapbam.getDataManager().getData().getAccount(accountName);
			statements = Collections.unmodifiableList(Arrays.asList(Statement.getStatements(account)));
		}
		return statements;
	}

	/* (non-Javadoc)
	 * @see net.yapbam.android.transaction.AbstractTransactionSplitter#sort(java.util.ArrayList)
	 */
	@Override
	public void sort(List<Transaction> result) {
		Collections.sort(result, new Comparator<Transaction>() {
			@Override
			public int compare(Transaction lhs, Transaction rhs) {
				return getDisplayedDate(lhs)-getDisplayedDate(rhs);
			}
		});
	}

	@Override
	public boolean isInPage(Transaction transaction, Statement page) {
		return NullUtils.areEquals(page.getId(), transaction.getStatement());
	}
	
	@Override
	public Integer getDisplayedDate(Transaction transaction) {
		return transaction.getValueDateAsInteger();
	}
	
	@Override
	public CharSequence getTitle(int index) {
		Statement page = getPage(index);
		return page.getId()==null ? context.getString(R.string.statement_not_checked): page.getId();
	}

	/* (non-Javadoc)
	 * @see net.yapbam.android.transaction.AbstractTransactionSplitter#getSummary()
	 */
	@Override
	public CharSequence getSummary(int index) {
		//FIXME Android TextView does not respect non breaking spaces !!! ... one more bug in Android platform :-(
		Statement page = getPage(index);
		String format = context.getString(R.string.statement_summary_format);
		String message = MessageFormat.format(format,
				Yapbam.CURRENCY_FORMAT.format(page.getStartBalance()), Yapbam.CURRENCY_FORMAT.format(page.getEndBalance()),
				page.getNbTransactions(), Yapbam.CURRENCY_FORMAT.format(page.getPositiveBalance()), Yapbam.CURRENCY_FORMAT.format(page.getNegativeBalance()));
		return Html.fromHtml(message);
	}
}
