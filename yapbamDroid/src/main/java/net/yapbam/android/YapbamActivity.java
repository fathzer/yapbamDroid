package net.yapbam.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;

import com.fathzer.android.dropbox.FileState;

import net.astesana.android.Log;
import net.yapbam.android.converter.CurrencyConverterActivity;
import net.yapbam.android.test.TestActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;
import java.util.Observer;

/** The main Yapbam activity. */
public class YapbamActivity extends Activity {
	/** Sets the test button's visibility.*/
	private static final boolean SHOW_TEST = false;
	private static final Logger LOGGER = LoggerFactory.getLogger(YapbamActivity.class);
	private Observer observer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_yapbam);
		observer = new Observer() {
			@Override
			public void update(Observable observable, Object data) {
				if (FileState.UPDATE_AVAILABLE.equals(data)) {
					LOGGER.trace("refresh data on {} update", observable); //NON-NLS
//					Yapbam.getDataManager().refreshData();
				}
			}
		};
//		findViewById(R.id.button3).setVisibility(SHOW_TEST?View.VISIBLE:View.GONE);
	}

	public void doTest(View view) {
		// This button is a place to make tests
//		Toast.makeText(this, "This is an empty test button", Toast.LENGTH_LONG).show(); //NON-NLS
        startActivity(new Intent(this, TestActivity.class));
    }
		
	@Override
	public void onResume() {
		super.onResume();
		Yapbam.getDataManager().addObserver(observer);
		Log.v(this, "resumed"); //$NON-NLS-1$
	}
	
	@Override
	protected void onPause() {
		Yapbam.getDataManager().deleteObserver(observer);
		super.onPause();
	}

	public void accounts(View view) {
		if (Yapbam.getDataManager().getSelectedFile()==null) {
			startActivityForResult(new Intent(this, SelectFileActivity.class), SelectFileActivity.SELECT_FILE_REQUEST);
		} else {
			startActivity(new Intent(this, AccountsActivity.class));
		}
	}
	
	public void converter(View view) {
		startActivity(new Intent(this, CurrencyConverterActivity.class));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(this, "activity result=" + resultCode); //$NON-NLS-1$
		if (requestCode == SelectFileActivity.SELECT_FILE_REQUEST && Yapbam.getDataManager().getSelectedFile()!=null) {
			startActivity(new Intent(this, AccountsActivity.class));
		}
	}
	
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		// Required to have a perfect gradient representation
		getWindow().setFormat(PixelFormat.RGBA_8888);
	}
}
