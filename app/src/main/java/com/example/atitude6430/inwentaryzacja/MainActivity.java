package com.example.atitude6430.inwentaryzacja;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.annotation.RequiresPermission;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CNewProduct.OnNewProductListener {
    // Tag used for logging errors
    private static final String TAG = MainActivity.class.getSimpleName();

    // Let's define some intent strings
    // This intent string contains the source of the data as a string
    private static final String SOURCE_TAG = "com.motorolasolutions.emdk.datawedge.source";
    // This intent string contains the barcode symbology as a string
    private static final String LABEL_TYPE_TAG = "com.motorolasolutions.emdk.datawedge.label_type";
    // This intent string contains the barcode data as a byte array list
    private static final String DECODE_DATA_TAG = "com.motorolasolutions.emdk.datawedge.decode_data";

    // This intent string contains the captured data as a string
    // (in the case of MSR this data string contains a concatenation of the track data)
    private static final String DATA_STRING_TAG = "com.motorolasolutions.emdk.datawedge.data_string";

    // Let's define the MSR intent strings (in case we want to use these in the future)
    private static final String MSR_DATA_TAG = "com.motorolasolutions.emdk.datawedge.msr_data";
    private static final String MSR_TRACK1_TAG = "com.motorolasolutions.emdk.datawedge.msr_track1";
    private static final String MSR_TRACK2_TAG = "com.motorolasolutions.emdk.datawedge.msr_track2";
    private static final String MSR_TRACK3_TAG = "com.motorolasolutions.emdk.datawedge.msr_track3";
    private static final String MSR_TRACK1_STATUS_TAG = "com.motorolasolutions.emdk.datawedge.msr_track1_status";
    private static final String MSR_TRACK2_STATUS_TAG = "com.motorolasolutions.emdk.datawedge.msr_track2_status";
    private static final String MSR_TRACK3_STATUS_TAG = "com.motorolasolutions.emdk.datawedge.msr_track3_status";
    private static final String MSR_TRACK1_ENCRYPTED_TAG = "com.motorolasolutions.emdk.datawedge.msr_track1_encrypted";
    private static final String MSR_TRACK2_ENCRYPTED_TAG = "com.motorolasolutions.emdk.datawedge.msr_track2_encrypted";
    private static final String MSR_TRACK3_ENCRYPTED_TAG = "com.motorolasolutions.emdk.datawedge.msr_track3_encrypted";
    private static final String MSR_TRACK1_HASHED_TAG = "com.motorolasolutions.emdk.datawedge.msr_track1_hashed";
    private static final String MSR_TRACK2_HASHED_TAG = "com.motorolasolutions.emdk.datawedge.msr_track2_hashed";
    private static final String MSR_TRACK3_HASHED_TAG = "com.motorolasolutions.emdk.datawedge.msr_track3_hashed";

    // Let's define the API intent strings for the soft scan trigger
    private static final String ACTION_SOFTSCANTRIGGER = "com.motorolasolutions.emdk.datawedge.api.ACTION_SOFTSCANTRIGGER";
    private static final String EXTRA_PARAM = "com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER";
    private static final String DWAPI_START_SCANNING = "START_SCANNING";
    private static final String DWAPI_STOP_SCANNING = "STOP_SCANNING";
    private static final String DWAPI_TOGGLE_SCANNING = "TOGGLE_SCANNING";

    private static String ourIntentAction = "testName";

    EditText barCode;
    CsdCard sdOperations;
    TextView description;
    TextView number;
    boolean firstRun;//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barCode = (EditText) findViewById(R.id.editTextCode);
        barCode.addTextChangedListener(new TextWatcher() {
            Timer timer = new Timer();
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (timer!=null)
                    timer.cancel();
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (barCode.getText().toString().equals("")){
                    Log.d("do","nothing");
                    //tu mozna ustawic wartosci domysle
                }else {
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    findCode();
                                    int com;

                                }
                            });
                        }
                    },3000);
                }
            }
        });
        sdOperations = new CsdCard(getApplicationContext());
        description = (TextView) findViewById(R.id.textViewDescription);
        number = (TextView) findViewById(R.id.textViewIlosc);
        firstRun = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_button_layout,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.read:
                ReadSD("test2");
                break;
            case R.id.save:
                WriteSD();
                break;
            case R.id.loadLast:
                ReadSD("result");
                break;
            case R.id.exit:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //działania inwentaryzacyjne
    Cinwentura InvObject;
    String[] foundedElement;
    public void findCode(){
        InvObject = new Cinwentura(loadedData,getApplicationContext());

        //description.setText("opis: "+InvObject.findDescription(barCode.getText().toString()));
        foundedElement = InvObject.findDescription(barCode.getText().toString());
        if (foundedElement!=null){
            description.setText("description: "+foundedElement[0]);
            number.setText("count: "+foundedElement[2]);
        }else {
            //add new code
            DialogFragment newProduct = new CNewProduct();
            newProduct.show(getFragmentManager(),"warning");
        }
        Log.d("kod i opis","kod: "+barCode.getText()+ " opis: "+InvObject.findDescription(barCode.getText().toString()));
    }
    @Override
    public void OnNewProductOK(String NewCode, String NewDesc) {
        //wywołac to w klasie CNewProduct
        String[] temp = {NewDesc,NewCode,"0"};
        loadedData.add(temp);
        InvObject.ShowAll(loadedData);
        Log.d("new","product added");
    }
//SD card operations=====================================================================
    List<String[]> loadedData = new ArrayList<String[]>();
    public void ReadSD(String fileName){
        loadedData.clear();
        loadedData=sdOperations.ReadData(fileName);
        for (int i=0;i<sdOperations.ReadData(fileName).size();i++){
            Log.d("R data", "->" + loadedData.get(i)[0] + " "+loadedData.get(i)[1]+" "+loadedData.get(i)[2]);
        }
    }
    public void WriteSD(){
        sdOperations.WriteData(loadedData);
    }
//FOR datawedge==========================================================================
    @Override
    protected void onNewIntent(Intent intent) {
        handleDecodeData(intent);
    }

    private void handleDecodeData(Intent i) {
        barCode.getText().clear();
        if (i.getAction().contentEquals(ourIntentAction)) {
            String out = "";
            String source = i.getStringExtra(SOURCE_TAG);
            if (source == null) source = "scanner";
            String data = i.getStringExtra(DATA_STRING_TAG);
            Integer data_len = 0;
            if (data != null) data_len = data.length();
            Editable txt = barCode.getText();
            SpannableStringBuilder stringbuilder = new SpannableStringBuilder(txt);
            stringbuilder.append(out);
            stringbuilder.setSpan(new StyleSpan(Typeface.BOLD), txt.length(), stringbuilder.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringbuilder.append(data);
            barCode.setText(stringbuilder);
            txt = barCode.getText();
            barCode.setSelection(txt.length());
        }
    }


}
