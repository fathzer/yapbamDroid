package net.yapbam.android.keyboard;

import android.app.Activity;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;

import com.fathzer.android.keyboard.DecimalKeyboard;
import com.fathzer.android.keyboard.DecimalKeyboardActionListener;

public class AutoHideDecimalKeyboard extends DecimalKeyboard {
	public AutoHideDecimalKeyboard(Activity host, int viewid, int layoutid) {
		super(host, viewid, layoutid);
	}

	protected OnKeyboardActionListener buildActionListener() {
		return new DecimalKeyboardActionListener(this) {
			@Override
			public void swipeDown() {
				hideCustomKeyboard();
			}
		}; 
	}
}