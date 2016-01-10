package net.yapbam.android.date;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import java.util.Calendar;

public abstract class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    private static final String INIT_DATE_KEY = "date"; //NON-NLS
    private static final String MIN_DATE_KEY = "minDate"; //NON-NLS
    private static final String MAX_DATE_KEY = "maxDate"; //NON-NLS

    public void setDate(Calendar date) {
        getBundle().putSerializable(INIT_DATE_KEY, date);
    }

    public void setMinDate(Calendar date) {
        getBundle().putSerializable(MIN_DATE_KEY, date);
    }

    public void setMaxDate(Calendar date) {
        getBundle().putSerializable(MAX_DATE_KEY, date);
    }

    private Bundle getBundle() {
        Bundle result = getArguments();
        if (result==null) {
            result = new Bundle();
            setArguments(result);
        }
        return result;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        Calendar initialDate = (Calendar) getArguments().getSerializable(INIT_DATE_KEY);
        if (initialDate==null) {
            initialDate = Calendar.getInstance();
        }
        final DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, initialDate.get(Calendar.YEAR), initialDate.get(Calendar.MONTH), initialDate.get(Calendar.DATE));
        Calendar min = (Calendar) getArguments().getSerializable(MIN_DATE_KEY);
        if (min!=null) {
            dialog.getDatePicker().setMinDate(min.getTimeInMillis());
        }
        Calendar max = (Calendar) getArguments().getSerializable(MAX_DATE_KEY);
        if (max!=null) {
            dialog.getDatePicker().setMaxDate(max.getTimeInMillis());
        }
        return dialog;
    }
}