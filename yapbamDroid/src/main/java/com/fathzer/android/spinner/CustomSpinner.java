package com.fathzer.android.spinner;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

import android.view.View;
import android.widget.AdapterView;

import net.astesana.android.Log;

/**
 * A spinner that doesn't throw OnItemSelected event when attaching a listener to it and
 * that allows to change its selection without throwing event.
 * <br>Warning: To change the selection programmatically, always call {@link #setSelection(int, boolean, boolean)},
 * {@link #setSelection(int, boolean)} should have unpredictable results in terms of events throwing.
 */
public class CustomSpinner extends Spinner implements AdapterView.OnItemSelectedListener {
    private OnItemSelectedListener mListener;

    /**
     * used to be sure that the user selected an item on spinner (and not programmatically)
     */
    private boolean throwEvents = false;

    public CustomSpinner(Context context) {
        super(context);
        super.setOnItemSelectedListener(this);
    }

    public CustomSpinner(Context context, int mode) {
        super(context, mode);
        super.setOnItemSelectedListener(this);
    }

    public CustomSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnItemSelectedListener(this);
    }

    public CustomSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnItemSelectedListener(this);
    }

    public CustomSpinner(Context context, AttributeSet attrs, int defStyle, int mode) {
        super(context, attrs, defStyle, mode);
        super.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.v("CustomSpinner", "onItemSelected: throwEvents=" + throwEvents);
        if (throwEvents && mListener != null) {
            mListener.onItemSelected(parent, view, position, id);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.v("CustomSpinner", "onNothingSelected of CustomSpinner called with throwEvents set to " + throwEvents);
        if (mListener != null) {
            mListener.onNothingSelected(parent);
        }
    }

    /** Sets the position.
     * @param pos The new position
     * @param animate same as in setPosition(int, animate)
     * @param enableEvent true to have OnItemSelectedListener called, false to not have it called.
     */
    public void setSelection(int pos, boolean animate, boolean enableEvent) {
        if (pos!=getSelectedItemPosition()) {
            throwEvents = enableEvent;
            setSelection(pos, animate);
            Log.v("CustomSpinner", "eventFreeSetPosition of CustomSpinner called");
        }
    }

    public void setOnItemSelectedListener (OnItemSelectedListener listener) {
        mListener = listener;
    }

    @Override
    public boolean performClick() {
        // register that the Spinner was opened so we have a status indicator for the activity
        // (which may lose focus for some other reasons)
        throwEvents = true;
        return super.performClick();
    }
}