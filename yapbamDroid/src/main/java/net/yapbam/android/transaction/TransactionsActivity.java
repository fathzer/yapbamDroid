package net.yapbam.android.transaction;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.fathzer.android.gesture.SwipeDetector;
import com.fathzer.android.gesture.SwipeInterface;

import net.yapbam.android.R;
import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.Yapbam;
import net.yapbam.android.datamanager.DataManager.State;
import net.yapbam.data.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionsActivity extends AbstractYapbamActivity {
	private static final String TRANSACTION_SPLITTER_PREF = "transactionSplitter"; //$NON-NLS-1$

	private static final String CURRENT_PAGE = "CurrentPage"; //$NON-NLS-1$
	private static final String SCROLL_POSITION = "scrollPosition"; //$NON-NLS-1$
	
	private final class SwipeActions implements SwipeInterface {
		@Override
		public void top2bottom(View v) {
			// Do nothing
		}

		@Override
		public void right2left(View v) {
			next(v);
		}

		@Override
		public void left2right(View v) {
			prev(v);
		}

		@Override
		public void bottom2top(View v) {
			// Do nothing
		}
	}

	private AbstractTransactionSplitter<? extends Object> splitter;
	private String accountName;
	private int pageIndex;
	private int restoredScrollPosition;
	private Logger logger;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.logger = LoggerFactory.getLogger(getClass());
		logger.trace("onCreate with {} empty bundle",savedInstanceState==null?"":"non "); //$NON-NLS-1$
		super.onCreate(savedInstanceState, R.layout.activity_transactions);
		this.pageIndex = -1;
		
		Intent intent = getIntent();
		accountName = intent.getStringExtra(Yapbam.ACCOUNT_NAME);
		if (accountName!=null) {
			// The ACCOUNT_NUMBER in the intent is used to distinguish between opening the account and navigating through the account
			intent.removeExtra(Yapbam.ACCOUNT_NAME);
		}
		// Note: The onRestoreInstanceState method will recover accountIndex if it is < 0
		
		final ListView content = (ListView) findViewById(R.id.content);
		SwipeDetector swipe = new SwipeDetector(this, new SwipeActions());
		content.setOnTouchListener(swipe);
		content.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				Transaction transaction = (Transaction)content.getAdapter().getItem(position);
				Intent intent = new Intent(TransactionsActivity.this, TransactionActivity.class);
				intent.putExtra(TransactionActivity.TRANSACTION_NUMBER, Yapbam.getDataManager().getData().indexOf(transaction));
				startActivity(intent);
			}
		});

	}
	
	public void prev(View v) {
		if (splitter.shouldBeSplit() && (pageIndex>0)) {
			pageIndex--;
			setListAdapter();
		}
	}

	public void next(View v) {
		if (splitter.shouldBeSplit() && pageIndex<splitter.getPageCount()-1) {
			pageIndex++;
			setListAdapter();
		}
	}

	@Override
	protected void onDataStateChanged() {
		if (State.OK.equals(getDataManager().getState())) {
			logger.trace("open page {} of account {}",pageIndex,accountName); //$NON-NLS-1$
			if (Yapbam.getDataManager().getData().getAccount(accountName)==null) {
				// Account has been removed by the update
				finish();
			}
			setSplitter();
			TextView title = (TextView) findViewById(R.id.accountName);
			title.setText(getDataManager().getData().getAccount(accountName).getName());
			setListAdapter();
			logger.trace("restoredScrollPosition is {}",restoredScrollPosition); //$NON-NLS-1$
			if (restoredScrollPosition>0) {
				ListView list = (ListView)findViewById(R.id.content);
				list.setSelectionFromTop(restoredScrollPosition, 0);
				logger.trace("Scroll position set to {}",restoredScrollPosition); //$NON-NLS-1$
			}
		}
	}

	private void setSplitter() {
		int splitterKind = getSplitterKind();
		if (splitterKind==1) {
			this.splitter = new DateSplitter(this, accountName, true);
		} else if (splitterKind==2) {
			this.splitter = new StatementSplitter(this, accountName);
		} else {
			this.splitter = new DateSplitter(this, accountName, false);
		}
	}

	private int getSplitterKind() {
		return PreferenceManager.getDefaultSharedPreferences(this).getInt(TRANSACTION_SPLITTER_PREF, 0);
	}

	/** Sets the list adapter accordingly to the currently selected year.
	 */
	public void setListAdapter() {
		logger.trace("Entering setListAdapter with pageIndex={}",pageIndex);
		
		if (splitter.shouldBeSplit()) {
			int pageCount = splitter.getPageCount();
			if ((pageIndex<0) || (pageIndex>pageCount)) {
				pageIndex = pageCount-1;
			}
			TextView pageView = (TextView) findViewById(R.id.page);
			findViewById(R.id.prev).setVisibility(pageIndex>0?View.VISIBLE:View.INVISIBLE);
			findViewById(R.id.next).setVisibility(pageIndex<pageCount-1?View.VISIBLE:View.INVISIBLE);
			pageView.setText(pageIndex<0?getString(R.string.no_transaction):splitter.getTitle(pageIndex));
		}
		ListView list = (ListView)findViewById(R.id.content);
		findViewById(R.id.navigation).setVisibility(splitter.shouldBeSplit()?View.VISIBLE:View.GONE);
		logger.trace("Creating adapter"); //$NON-NLS-1$
		Object page = splitter.getPage(pageIndex);
		TransactionsAdapter adapter = new TransactionsAdapter(this, accountName, page, splitter);
		logger.trace("Adapter is ready"); //$NON-NLS-1$
		list.setAdapter(adapter);
		TextView summary = (TextView) findViewById(R.id.summary);
		summary.setText(splitter.getSummary(pageIndex));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.transactions, menu);
		return true;
	}
	
	private int getSplitterMenuItemId(int splitterKind) {
		int id = R.id.byDate;
		if (splitterKind==1) {
			id = R.id.byValueDate;
		} else if (splitterKind==2) {
			id = R.id.byStatement;
		}
		return id;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.addTransaction) {
			Intent intent = new Intent(this, NewTransactionActivity.class);
			intent.putExtra(NewTransactionActivity.ACCOUNT_NAME, accountName);
			startActivity(intent);
		} else if (itemId == R.id.action_more_menu) {
			onPrepareOptionsMenu(item.getSubMenu());
		} else if (itemId == R.id.byDate || itemId == R.id.byValueDate
				|| itemId == R.id.byStatement) {
			int currentId = getSplitterMenuItemId(getSplitterKind());
			if (currentId != itemId) {
				pageIndex = -1;
				int splitterKind = 0;
				if (itemId == R.id.byValueDate) {
					splitterKind = 1;
				} else if (itemId == R.id.byStatement) {
					splitterKind = 2;
				}
				Editor editor = PreferenceManager.getDefaultSharedPreferences(
						this).edit();
				editor.putInt(TRANSACTION_SPLITTER_PREF, splitterKind);
				editor.apply();
				setSplitter();
				setListAdapter();
			}
		}
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		int currentId = getSplitterMenuItemId(getSplitterKind());
		for (int i=0;i<menu.size();i++) {
			MenuItem item = menu.getItem(i);
			int itemId = item.getItemId();
			if ((itemId==R.id.byDate || itemId==R.id.byValueDate || itemId==R.id.byStatement) && (itemId==currentId)) {
				item.setCheckable(true);
				item.setChecked(itemId==currentId);
				logger.trace("{} is selected",item.getTitle()); //$NON-NLS-1$
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		logger.trace("onSaveInstanceState"); //$NON-NLS-1$
		outState.putString(Yapbam.ACCOUNT_NAME, accountName);
		outState.putInt(CURRENT_PAGE, pageIndex);
		ListView list = (ListView) findViewById(R.id.content);
		outState.putInt(SCROLL_POSITION, list.getFirstVisiblePosition());
		logger.trace("Saved scroll position = {}",list.getFirstVisiblePosition()); //$NON-NLS-1$
		super.onSaveInstanceState(outState);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		logger.trace("onRestoreInstanceState"); //$NON-NLS-1$
		// This is only called when process is killed.
		logger.trace("accountIndex = {}",accountName); //$NON-NLS-1$
		super.onRestoreInstanceState(savedInstanceState);
		logger.trace("accountIndex = {}",accountName); //$NON-NLS-1$
		// Be aware that accountIndex should have been specified by the intent
		if (accountName==null) {
			accountName = savedInstanceState.getString(Yapbam.ACCOUNT_NAME);
			pageIndex = savedInstanceState.getInt(CURRENT_PAGE);
			restoredScrollPosition = savedInstanceState.getInt(SCROLL_POSITION);
			logger.trace("Restored scroll position = {}",restoredScrollPosition); //$NON-NLS-1$
		}
	}
	
	public void selectPage(View view) {
		CharSequence[] items = new String[splitter.getPageCount()];
		for (int i = 0; i < items.length; i++) {
			items[i] = splitter.getTitle(items.length - 1 - i);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	dialog.dismiss();
		    	pageIndex = splitter.getPageCount() - 1 - item;
		    	setListAdapter();
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	protected ViewGroup getMainViewGroup() {
		return (ViewGroup) findViewById(R.id.frameLayout);
	}

	@Override
	protected View getContentView() {
		return findViewById(R.id.mainContent);
	}
}
