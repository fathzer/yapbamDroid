package net.yapbam.android.transaction;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import net.yapbam.android.R;
import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.datamanager.DataManager.State;
import net.yapbam.data.Category;
import net.yapbam.data.GlobalData;
import net.yapbam.data.Mode;
import net.yapbam.data.Transaction;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;

public class TransactionActivity extends AbstractYapbamActivity {
	public static final String TRANSACTION_NUMBER = "transaction_id"; //$NON-NLS-1$
	private int transactionNum;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_transaction;
    }


    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.transaction, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.editTransaction) {
			Intent intent = new Intent(this, NewTransactionActivity.class);
			intent.putExtra(NewTransactionActivity.TRANSACTION_NUMBER, transactionNum);
			startActivity(intent);
			//TODO The activity should return the new number of the edited transaction
		} else if (itemId == R.id.deleteTransaction) {
			//TODO Icon is not very visible, choose another one
			new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.generic_confirm_action)
	        .setMessage(R.string.confirm_delete_transaction)
	        .setPositiveButton(R.string.generic_yes, new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	    			GlobalData data = getDataManager().getData();
	    			data.remove(data.getTransaction(transactionNum));
	    			finish();
	            }
	        })
	        .setNegativeButton(R.string.generic_no, null)
	        .show();
		}
		return true;
	}

	@Override
	protected void onDataStateChanged() {
		if (!getDataManager().getState().equals(State.OK)) {
			return;
		}
		GlobalData data = getDataManager().getData();
		transactionNum = getIntent().getIntExtra(TRANSACTION_NUMBER, -1);
		Transaction transaction = (data==null || data.getTransactionsNumber()<=transactionNum) ? null : data.getTransaction(transactionNum);
		if (transaction==null) {
			// If the transaction is unknown => finish the activity
			finish();
			return;
		}
		boolean landscape = getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE;

		((TextView) findViewById(R.id.account)).setText(transaction.getAccount().getName());
		((TextView) findViewById(R.id.amount)).setText(DecimalFormat.getCurrencyInstance().format(transaction.getAmount()));
		DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
		final String date = format.format(transaction.getDate());
		((TextView) findViewById(R.id.date)).setText(landscape?getCompound(R.string.date, date) : date);
		final String valueDate = format.format(transaction.getValueDate());
		((TextView) findViewById(R.id.valueDate)).setText(landscape?getCompound(R.string.valueDate, valueDate):valueDate);
		((TextView) findViewById(R.id.description)).setText(transaction.getDescription());
		if (transaction.getComment()==null) {
			findViewById(R.id.comment).setVisibility(View.GONE);
		} else {
			((TextView) findViewById(R.id.comment)).setText(transaction.getComment());
		}

		String category = Category.UNDEFINED.equals(transaction.getCategory()) ? "Undefined" : transaction.getCategory().getName();
		if (landscape) {
			category = getCompound(R.string.category, category);
		}
		((TextView) findViewById(R.id.category)).setText(category);

		String mode = Mode.UNDEFINED.equals(transaction.getMode()) ? "Undefined" : transaction.getMode().getName();
		if (landscape) {
			mode = getCompound(R.string.mode, mode);
		}
		((TextView) findViewById(R.id.mode)).setText(mode);

		final String number = transaction.getNumber();
		if (number ==null) {
			findViewById(R.id.number).setVisibility(View.GONE);
		} else {
			((TextView) findViewById(R.id.number)).setText(landscape?getCompound(R.string.number, number):number);
		}

		final String statement = transaction.getStatement();
		if (statement ==null) {
			findViewById(R.id.statement).setVisibility(View.GONE);
		} else {
			((TextView) findViewById(R.id.statement)).setText(landscape?getCompound(R.string.statement, statement): statement);
		}
/*
		TableLayout table = (TableLayout) findViewById(R.id.subtransactions);
		if (transaction.getSubTransactionSize()==0) {
			table.setVisibility(View.GONE);
		} else {
			for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
				SubTransaction st = transaction.getSubTransaction(i);
				View row = LayoutInflater.from(this).inflate(R.layout.subtransaction_row, null);
				TextView tv = (TextView) row.findViewById(R.id.description); tv.setText(st.getDescription());
				tv = (TextView) row.findViewById(R.id.category); tv.setText(st.getCategory().getName());
				tv = (TextView) row.findViewById(R.id.amount); tv.setText(DecimalFormat.getCurrencyInstance().format(st.getAmount())); tv.setGravity(Gravity.RIGHT);
				table.addView(row);
			}
			if (GlobalData.AMOUNT_COMPARATOR.compare(transaction.getComplement(),0.0)!=0) {
				View row = LayoutInflater.from(this).inflate(R.layout.subtransaction_row, null);
				TextView tv = (TextView) row.findViewById(R.id.description); tv.setText(getString(R.id.account));
				tv = (TextView) row.findViewById(R.id.category); tv.setText(""); //$NON-NLS-1$
				tv = (TextView) row.findViewById(R.id.amount); tv.setText(DecimalFormat.getCurrencyInstance().format(transaction.getComplement())); tv.setGravity(Gravity.RIGHT);
				table.addView(row);
			}
		}
*/
		if (transaction.getSubTransactionSize()==0) {
			findViewById(R.id.subtransactionsPanel).setVisibility(View.GONE);
		} else {
			ListView list = (ListView) findViewById(R.id.subtransactions);
			list.setAdapter(new SubTransactionsAdapter(this, transaction));
		}
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
