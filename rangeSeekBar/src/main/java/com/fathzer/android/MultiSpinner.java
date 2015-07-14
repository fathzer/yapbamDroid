package com.fathzer.android;

import java.util.List;

import com.fathzer.android.rangeseekbar.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

/** A spinner with multiple choices.
 * <br>This work is an improved version the work of "Destil" that can be found at <a href="http://stackoverflow.com/questions/5015686/android-spinner-with-multiple-choice">http://stackoverflow.com/questions/5015686/android-spinner-with-multiple-choice</a>.
 * <br>It adds some features:<ul>
 * <li>{@link #getSelected} method.</li>
 * <li>{@link #getItems} method.</li>
 * <li>{@link #setSelected(boolean[])} method.</li>
 * <li>Separate for setting items and "all text".</li> 
 * <li>The ability to override the {@link #getDisplayedText} method.</li>
 * <li>Adds a box to select/deselect all items.</li>
 * <li>The user can cancel the dialog discarding its choices.</li>
 * <li>The listener is only invoked when selection is really changed.</li>
 * </ul>
 * @author Jean-Marc Astesana.
 */
public class MultiSpinner extends Spinner {
	public interface MultiSpinnerListener {
		public void onItemsSelected(boolean[] selected);
	}
	
	private List<String> items;
	private boolean[] selected;
	private CharSequence allText;
	private CharSequence dialogTitle;
	private CharSequence selectAllText;
	private MultiSpinnerListener listener;

	public MultiSpinner(Context context) {
		super(context);
	}

	public MultiSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MultiSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean performClick() {
		final boolean[] tmpSelection = selected.clone();
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		View titleView = LayoutInflater.from(getContext()).inflate(R.layout.multi_spinner_dialog_title_view, null);
		CheckBox checkBox = (CheckBox) titleView.findViewById(R.id.checkBox);
		if (selectAllText!=null) {
			TextView allTitle = (TextView)titleView.findViewById(R.id.all);
			allTitle.setText(selectAllText);
		}
		if (selected.length<2) {
			checkBox.setVisibility(View.GONE);
		} else {
			boolean isSelected = true;
			for (boolean b : selected) {
				if (!b) {
					isSelected = false;
					break;
				}
			}
			checkBox.setChecked(isSelected);
		}
		if (dialogTitle!=null) {
			TextView dialogTitleView = (TextView)titleView.findViewById(R.id.title);
			dialogTitleView.setText(this.dialogTitle);
		}
		builder.setCustomTitle(titleView);
		builder.setMultiChoiceItems(items.toArray(new CharSequence[items.size()]), tmpSelection, new OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				tmpSelection[which] = isChecked;
			}
		});
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.v(this.getClass().getName(), "onClick");
				boolean hasChanged = false;
				for (int i = 0; i < tmpSelection.length; i++) {
					if (tmpSelection[i]!=selected[i]) {
						hasChanged = true;
						break;
					}
				}
				if (!hasChanged) {
					return;
				}
				setSelected(tmpSelection);
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Do nothing, just let the dialog dismiss
			}
		});
		AlertDialog dialog = builder.create();
		final ListView listView = dialog.getListView();
		checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Log.v(MultiSpinner.this.getClass().getName(), "All -> "+isChecked);
				for (int i = 0; i < tmpSelection.length; i++) {
					tmpSelection[i] = isChecked;
					listView.setItemChecked(i, isChecked);
				}
			}
		});
		dialog.show();
		return true;
	}
	
	/** Gets the selected state of each item.
	 * @return An array of boolean. If item i is selected, then element i of this array is true.  
	 */
	public boolean[] getSelected() {
		return selected;
	}

	/** Gets the items of the spinner.
	 * @return The list of the spinner's items.
	 */
	public List<String> getItems() {
		return items;
	}
	
	public void setMultiSpinnerListener(MultiSpinnerListener listener) {
		this.listener = listener;
	}
	
	/** Sets the text that is displayed when all the items are selected.
	 * <br>By default, this attribute is null.
	 * @param allText The new text, or null to display all the items.
	 */
	public void setAllText(String allText) {
		this.allText = allText;
	}
	
	/** Sets the title of the item selection dialog.
	 * <br>By default, this title is defined in the string resource multi_splinner_dialog_title.
	 * @param dialogTitle The new title, or null to use the default one.
	 */
	public void setDialogTitle(CharSequence dialogTitle) {
		this.dialogTitle = dialogTitle;
	}

	/** Sets the text of the "select all" check box in the item selection dialog.
	 * <br>By default, this title is defined in the string resource multi_splinner_dialog_select_all.
	 * @param text The new checkbox's text, or null to use the default one.
	 */
	public void setSelectAllText(CharSequence text) {
		this.selectAllText = text;
	}

	/** Sets the items.
	 * <br><b>WARNING</b>: This method changes the selection. All items are selected after this method is called.
	 * @param items The new items list.
	 */
	public void setItems(List<String> items) {
		this.items = items;
		// all selected by default
		boolean[] newSelected = new boolean[items.size()];
		for (int i = 0; i < newSelected.length; i++) {
			newSelected[i] = true;
		}
		setSelected(newSelected);
	}
	
	/** Sets the selected state of the items.
	 * @param selected an array of boolean. If element i is true, item i will be selected
	 * @throw IllegalArgumentException if selected has not the same length as items 
	 */
	public void setSelected(boolean[] selected) {
		if (selected.length!=items.size()) {
			throw new IllegalArgumentException();
		}
		this.selected = selected.clone();
		setDisplay();
		if (listener!=null) {
			listener.onItemsSelected(selected); 
		}
	}

	/** Sets spinner's text.
	 */
	private void setDisplay() {
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getContext(), android.R.layout.simple_spinner_item,
				new CharSequence[] { getDisplayedText() });
		setAdapter(adapter);
	}

	/** Gets the text displayed by this widget.
	 * <br>You can override this method to display what you want.
	 * @return The selected items separated by a colon.
	 * If all items are selected and setAllText was called with a non null string, it returns the text specified in setAllText.
	 * @see #setAllText(String)
	 */
	protected CharSequence getDisplayedText() {
		// refresh text on spinner
		StringBuilder spinnerBuffer = new StringBuilder();
		boolean someUnselected = false;
		for (int i = 0; i < items.size(); i++) {
			if (selected[i]) {
				spinnerBuffer.append(items.get(i));
				spinnerBuffer.append(", ");
			} else {
				someUnselected = true;
			}
		}
		String spinnerText;
		if (someUnselected || (allText==null)) {
			spinnerText = spinnerBuffer.toString();
			if (spinnerText.length() > 2) {
				spinnerText = spinnerText.substring(0, spinnerText.length() - 2);
			}
			return spinnerText;
		} else {
			return allText;
		}
	}
}