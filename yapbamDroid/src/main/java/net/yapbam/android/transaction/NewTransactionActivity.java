package net.yapbam.android.transaction;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fathzer.android.keyboard.DecimalKeyboard;

import net.yapbam.android.R;
import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.datamanager.DataManager.State;
import net.yapbam.android.keyboard.AutoHideDecimalKeyboard;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;
import net.yapbam.data.Mode;
import net.yapbam.data.Transaction;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;


public class NewTransactionActivity extends AbstractYapbamActivity {
	//FIXME Not finished
	public static final String TRANSACTION_NUMBER = "transaction_id"; //$NON-NLS-1$
	public static final String ACCOUNT_NAME = "account_name"; //$NON-NLS-1$

	private DecimalKeyboard mCustomKeyboard;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.activity_new_transaction);
		mCustomKeyboard= new AutoHideDecimalKeyboard(this, R.id.keyboardview, R.xml.deckbd );
		mCustomKeyboard.registerEditText(R.id.amount);
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.transaction, menu);
//		return true;
//	}

	@Override
	protected void onDataStateChanged() {
		if (!getDataManager().getState().equals(State.OK)) {
			return;
		}
		GlobalData data = getDataManager().getData();
		int transactionNum = getIntent().getIntExtra(TRANSACTION_NUMBER, -1);
		Transaction transaction = (data==null || transactionNum==-1 || data.getTransactionsNumber()<=transactionNum) ? null : data.getTransaction(transactionNum);
		// Transaction is null if we are creating a new activity
		String accountName = transaction==null ? getIntent().getStringExtra(ACCOUNT_NAME) : transaction.getAccount().getName();
		((TextView) findViewById(R.id.account)).setText(accountName);
		DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
		Date date = transaction==null ? new Date() : transaction.getDate();
		((TextView) findViewById(R.id.date)).setText(format.format(date));
		double amount = transaction==null?0.0:transaction.getAmount();
		NumberFormat currencyInstance = DecimalFormat.getCurrencyInstance();
		NumberFormat formatter = new DecimalFormat();
		formatter.setGroupingUsed(currencyInstance.isGroupingUsed());
		formatter.setMinimumFractionDigits(currencyInstance.getMinimumFractionDigits());
		formatter.setMaximumFractionDigits(currencyInstance.getMaximumFractionDigits());
		formatter.setMinimumIntegerDigits(currencyInstance.getMinimumIntegerDigits());
		formatter.setMaximumIntegerDigits(currencyInstance.getMaximumIntegerDigits());
		((TextView) findViewById(R.id.amount)).setText(formatter.format(amount));
		Date valueDate = transaction==null ? date : transaction.getValueDate();
		((TextView) findViewById(R.id.valueDate)).setText(format.format(valueDate));
		Category category = transaction==null ? Category.UNDEFINED:transaction.getCategory();
		((TextView) findViewById(R.id.category)).setText(getCompound(R.string.category, category.getName()));
		Mode mode = transaction==null? Mode.UNDEFINED:transaction.getMode();
		((TextView) findViewById(R.id.mode)).setText(getCompound(R.string.mode, mode.getName()));
		if (transaction!=null) {
			if (transaction.getStatement()!=null) {
				((TextView) findViewById(R.id.statement)).setText(transaction.getStatement());
			}
			((TextView) findViewById(R.id.description)).setText(transaction.getDescription());
			((TextView) findViewById(R.id.comment)).setText(transaction.getComment());
			((TextView) findViewById(R.id.number)).setText(getCompound(R.string.number, transaction.getNumber()));
		}
//		if (transaction.getSubTransactionSize()==0) {
			findViewById(R.id.subtransactionsPanel).setVisibility(View.GONE);
//		} else {
//			ListView list = (ListView) findViewById(R.id.subtransactions);
//			list.setAdapter(new SubTransactionsAdapter(this, transaction));
//		}
	}
	
	private String getCompound (int resId, String string) {
		return MessageFormat.format(getString(R.string.twoPointsFormat), getString(resId), string);
	}

	@Override
	protected ViewGroup getMainViewGroup() {
		return (ViewGroup) findViewById(R.id.frameLayout);
	}
	
	@Override
	protected View getContentView() {
		return findViewById(R.id.content);
	}
}
