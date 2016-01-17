package net.yapbam.android.test;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.fathzer.android.keyboard.DecimalKeyboard;
import com.fathzer.android.spinner.UserOnlySpinnerListener;

import net.astesana.android.Log;
import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.R;
import net.yapbam.android.keyboard.AutoHideDecimalKeyboard;

public class TestActivity extends AbstractYapbamActivity {

    private Spinner spinner;
    private Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DecimalKeyboard mCustomKeyboard = new AutoHideDecimalKeyboard(this, R.id.keyboardview, R.xml.deckbd );
        mCustomKeyboard.registerEditText(R.id.amount);
        addItemsOnSpinner2();
        addListenerOnButton();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_test;
    }

    @Override
    protected ViewGroup getMainViewGroup() {
        return (ViewGroup) findViewById(R.id.frameLayout);
    }

    @Override
    protected View getContentView() {
        return (ViewGroup) findViewById(R.id.content);
    }

    @Override
    protected void onDataStateChanged() {
        //TODO
    }

    // add items into spinner dynamically
    public void addItemsOnSpinner2() {
        spinner = (Spinner) findViewById(R.id.spinner2);
        List list = new ArrayList();
        for (int i=1;i<20;i++) {
            list.add("Item " + i);
        }
        ArrayAdapter dataAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        UserOnlySpinnerListener listener = new UserOnlySpinnerListener() {
            @Override
            protected void doSelect(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Toast.makeText(parentView.getContext(), "Selected: " + parentView.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
            }
        };
        spinner.setOnTouchListener(listener);
        spinner.setOnItemSelectedListener(listener);
    }

    public void addListenerOnButton() {
        spinner = (Spinner) findViewById(R.id.spinner2);
        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(TestActivity.this,
                        "Result: Spinner : " + String.valueOf(spinner.getSelectedItem()),
                        Toast.LENGTH_SHORT).show();
                spinner.setSelection(0);
            }

        });
    }
}