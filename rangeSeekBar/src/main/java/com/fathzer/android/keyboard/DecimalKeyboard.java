package com.fathzer.android.keyboard;

import java.text.DecimalFormat;
import java.util.List;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * When an activity needs a decimal keyboard, this class allows several EditText's to register for it.
 *
 * @author Jean-Marc Astesana (based on <a href="http://www.fampennings.nl/maarten/android/09keyboard/index.htm">the original work of Maarten Pennings</a> distributed under the Apache License, Version 2.0).
 */
public class DecimalKeyboard {
	/** Decimal separator key */
	static final int SEPARATOR_CODE = -55000;

	/** A link to the KeyboardView that is used to render this CustomKeyboard. */
	private KeyboardView mKeyboardView;
	/** A link to the activity that hosts the {@link #mKeyboardView}. */
	private Activity mHostActivity;
	/** The decimal separator */
	private char decimalSeparator;
    
	/**
	 * Create a custom keyboard, that uses the KeyboardView (with resource id
	 * <var>viewid</var>) of the <var>host</var> activity, and load the keyboard
	 * layout from xml file <var>layoutid</var> (see {@link Keyboard} for
	 * description). Note that the <var>host</var> activity must have a
	 * <var>KeyboardView</var> in its layout (typically aligned with the bottom of
	 * the activity). Note that the keyboard layout xml file may include key codes
	 * for navigation; see the constants in this class for their values. Note that
	 * to enable EditText's to use this custom keyboard, call the
	 * {@link #registerEditText(int)}.
	 * 
	 * @param host The hosting activity.
	 * @param viewid The id of the KeyboardView.
	 * @param layoutid The id of the xml file containing the keyboard layout.
	 */
	public DecimalKeyboard(Activity host, int viewid, int layoutid) {
		this.decimalSeparator = new DecimalFormat().getDecimalFormatSymbols().getDecimalSeparator();
		mHostActivity = host;
		mKeyboardView = (KeyboardView) mHostActivity.findViewById(viewid);
		Keyboard keyboard = new Keyboard(mHostActivity, layoutid);
		List<Key> keys = keyboard.getKeys();
		for (Key key : keys) {
			if (key.codes[0] == SEPARATOR_CODE) {
				key.label = Character.toString(decimalSeparator);
			}
		}
		mKeyboardView.setKeyboard(keyboard);
		// Do not show the preview balloons
		mKeyboardView.setPreviewEnabled(false);
		mKeyboardView.setOnKeyboardActionListener(buildActionListener());
		// Hide the standard keyboard initially
		mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	/** Returns whether the CustomKeyboard is visible. */
	public boolean isCustomKeyboardVisible() {
		return mKeyboardView.getVisibility() == View.VISIBLE;
	}

	/** Make the CustomKeyboard visible, and hide the system keyboard for view v. */
	public void showCustomKeyboard(View v) {
		mKeyboardView.setVisibility(View.VISIBLE);
		mKeyboardView.setEnabled(true);
		if (v != null) {
			((InputMethodManager) mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
					v.getWindowToken(), 0);
		}
	}

	/** Makes the CustomKeyboard invisible. */
	public void hideCustomKeyboard() {
		mKeyboardView.setVisibility(View.GONE);
		mKeyboardView.setEnabled(false);
	}

	/**
	 * Register <var>EditText<var> with resource id <var>resid</var> (on the
	 * hosting activity) for using this custom keyboard.
	 * 
	 * @param resid
	 *          The resource id of the EditText that registers to the custom
	 *          keyboard.
	 */
	public void registerEditText(int resid) {
		// Find the EditText 'resid'
		final EditText edittext = (EditText) mHostActivity.findViewById(resid);
		// In case the edittext type was not set to text
		edittext.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL);
		// Make the custom keyboard appear
		edittext.setOnFocusChangeListener(new OnFocusChangeListener() {
			// NOTE By setting the on focus listener, we can show the custom keyboard
			// when the edit box gets focus, but also hide it when the edit box loses
			// focus
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					showCustomKeyboard(v);
				}	else {
					hideCustomKeyboard();
				}
			}
		});
		edittext.setOnClickListener(new OnClickListener() {
			// NOTE By setting the on click listener, we can show the custom keyboard
			// again, by tapping on an edit box that already had focus (but that had
			// the keyboard hidden).
			@Override
			public void onClick(View v) {
				showCustomKeyboard(v);
			}
		});
		// Disable spell check (hex strings look like words to Android)
		edittext.setInputType(edittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	}

	public char getDecimalSeparator() {
		return decimalSeparator;
	}
	
	Activity getHostActivity() {
		return mHostActivity;
	}
	
	protected OnKeyboardActionListener buildActionListener() {
		return new DecimalKeyboardActionListener(this); 
	}
}