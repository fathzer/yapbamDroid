package com.fathzer.android.keyboard;

import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

public class DecimalKeyboardActionListener implements OnKeyboardActionListener {
	/** Keyboard delete key code */
	private static final int DELETE_CODE = -5;
	private DecimalKeyboard keyboard;
	
	public DecimalKeyboardActionListener(DecimalKeyboard keyboard) {
		this.keyboard = keyboard;
	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
		// NOTE We can say '<Key android:codes="49,50" ... >' in the xml file; all
		// codes come in keyCodes, the first in this list in primaryCode
		// Get the EditText and its Editable
		View focusCurrent = keyboard.getHostActivity().getWindow().getCurrentFocus();
		if (focusCurrent == null || focusCurrent.getClass() != EditText.class) {
			return;
		}
		EditText edittext = (EditText) focusCurrent;
		Editable editable = edittext.getText();
		int start = edittext.getSelectionStart();
		int end = edittext.getSelectionEnd();
		// Apply the key to the edittext
		if (primaryCode == DELETE_CODE) {
			if (editable != null) {
				if (start==end && start > 0) {
					editable.delete(start - 1, start);
				} else if (start != end) {
					editable.delete(start, end);
				}
			}
		} else if (primaryCode == DecimalKeyboard.SEPARATOR_CODE) {
			// insert decimal separator
			int index = editable.toString().indexOf(keyboard.getDecimalSeparator());
			if (index>=0) {
				// If there's already a decimal separator, remove it
				editable.delete(index, index+1);
				if (index<start) {
					start--;
				}
			}
			editable.insert(start, Character.toString(keyboard.getDecimalSeparator()));
		} else {
			if (start != end) {
				editable.delete(start, end);
			}
			// insert character
			editable.insert(start, Character.toString((char) primaryCode));
		}
	}

	@Override
	public void onPress(int arg0) {
		// Do nothing
	}

	@Override
	public void onRelease(int primaryCode) {
		// Do nothing
	}

	@Override
	public void onText(CharSequence text) {
		// Do nothing
	}

	@Override
	public void swipeDown() {
		// Do nothing
	}

	@Override
	public void swipeLeft() {
		// Do nothing
	}

	@Override
	public void swipeRight() {
		// Do nothing
	}

	@Override
	public void swipeUp() {
		// Do nothing
	}
}