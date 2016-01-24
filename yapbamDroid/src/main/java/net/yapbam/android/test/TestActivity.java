package net.yapbam.android.test;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.fathzer.android.keyboard.DecimalKeyboard;
import com.fathzer.android.spinner.CustomSpinner;

import net.yapbam.android.AbstractYapbamActivity;
import net.yapbam.android.R;
import net.yapbam.android.keyboard.AutoHideDecimalKeyboard;

public class TestActivity extends AbstractYapbamActivity {
    private CustomSpinner spinner;
    private Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DecimalKeyboard mCustomKeyboard = new AutoHideDecimalKeyboard(this, R.id.keyboardview, R.xml.deckbd );
        mCustomKeyboard.registerEditText(R.id.amount);
        addItemsOnSpinner2();
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
        spinner = (CustomSpinner) findViewById(R.id.spinner2);
        List list = new ArrayList();
        for (int i=1;i<20;i++) {
            list.add("Item " + i);
        }
        ArrayAdapter dataAdapter = new ArrayAdapter(this,android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(parent.getContext(), "Selected: " + parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Nothing to do
            }
        };
        spinner.setOnItemSelectedListener(listener);
    }

    public void showSelected(View v) {
        Toast.makeText(TestActivity.this, "Result: Spinner : " + String.valueOf(spinner.getSelectedItem()), Toast.LENGTH_SHORT).show();
    }

    public void select0(View v) {
        spinner.setSelection(0, false, ((CheckBox)findViewById(R.id.checkBox)).isChecked());
    }
}