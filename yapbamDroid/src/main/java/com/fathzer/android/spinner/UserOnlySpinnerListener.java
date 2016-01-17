package com.fathzer.android.spinner;

import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;

import net.astesana.android.Log;

public abstract class UserOnlySpinnerListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
    private boolean userSelect = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.v(this, "onTouch");
        userSelect = true;
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
        if (userSelect) {
            userSelect = false;
            doSelect(parentView, selectedItemView, position, id);
        }
    }

    protected abstract void doSelect(AdapterView<?> parentView, View selectedItemView, int position, long id);

    @Override
    public void onNothingSelected(AdapterView<?> parentView) {
        Log.v(this, "nothing selected");
        userSelect = false;
    }

}
