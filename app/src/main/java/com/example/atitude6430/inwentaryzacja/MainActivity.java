package com.example.atitude6430.inwentaryzacja;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Environment;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class MainActivity extends AppCompatActivity implements CNewProduct.OnNewProductListener,CWorning.OnWarningStateListener {
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
    EditText enterNumber;
    boolean firstRun;//


    public String CheckLicence(){
        String licence;
        SharedPreferences preferences = this.getSharedPreferences("Licence", Context.MODE_PRIVATE);
        licence = preferences.getString("licence","error");
        return licence;
    }
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
                    SetTextValues("","");
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
                                }
                            });
                        }
                    },500); //bylo 1500
                }
            }
        });
        sdOperations = new CsdCard(getApplicationContext());
        description = (TextView) findViewById(R.id.textViewDescription);
        number = (TextView) findViewById(R.id.textViewIlosc);
        enterNumber = (EditText) findViewById(R.id.editTextTypeIlosc);
        firstRun = true;
        SetTextValues("","");

        if (CheckLicence().equals("error")){
            DialogFragment licence = new CLicence();
            licence.show(getFragmentManager(), "lic");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_button_layout,menu);
        return super.onCreateOptionsMenu(menu);
    }

    String fileName;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.read:
                fileName = "dane";
                //ReadSD(fileName);
                //loadedData=ReadingTest();
                ReadingTest(fileName);
                block = true;
                break;
            case R.id.save:
                WriteSD();
                break;
            case R.id.loadLast:
                fileName = "result";
                //ReadSD(fileName);
                ReadingTest(fileName);
                block = true;
                break;
            case R.id.exit:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
       //działania inwentaryzacyjne============================================
    Cinwentura InvObject;
    String[] foundedElement;
    Integer foundedElementNumb;

    public void SetTextValues(String SetDescription, String SetQuantity){
        description.setText("description: " + SetDescription);
        number.setText("quantity: " + SetQuantity);
    }
    public void ClearFields(){
        SetTextValues("","");
        enterNumber.getText().clear();
        barCode.getText().clear();
    }
    boolean block;//blokuje nowy kod jezeli nie znaleziono w bazie
    public void findCode(){
        InvObject = new Cinwentura(loadedData,getApplicationContext());
        foundedElement = InvObject.findDescription(barCode.getText().toString());
        if (foundedElement!=null){
            SetTextValues(foundedElement[0],foundedElement[2]);
            foundedElementNumb = Integer.parseInt(foundedElement[3]);
            block= true;
        }else {
            block=false;
            SetTextValues("not found","not found");
            DialogFragment newProduct = new CNewProduct();
            newProduct.show(getFragmentManager(),"warning5");
        }
        Log.d("kod i opis", "kod: " + barCode.getText() + " opis: " + InvObject.findDescription(barCode.getText().toString()));
    }
    @Override
    public void OnNewProductOK(String NewCode, String NewDesc) {
        //wywołac to w klasie CNewProduct
        String[] temp = {NewDesc,NewCode,"0"};
        loadedData.add(temp);
        InvObject.ShowAll(loadedData);
        Log.d("new", "product added");
        ClearFields();
        block = true;
    }

    @Override
    public void OnNewProductCancel() {
        block = true;
    }

    public void ChangeQuantity(boolean state){
        if (enterNumber.getText().toString().equals("")||barCode.getText().toString().equals("")){
            Toast.makeText(this,"fill fields",Toast.LENGTH_SHORT).show();
        }else {
            Integer temp= Integer.parseInt(loadedData.get(foundedElementNumb)[2]);//+=Integer.parseInt(numb);
            Log.d("licznik","przed zmiana: "+temp);
            if (state){
                temp +=Integer.parseInt(enterNumber.getText().toString());
            }else {
                temp -=Integer.parseInt(enterNumber.getText().toString());
                if (temp<0){
                    temp=0;
                    Toast.makeText(this,"new quantity 0",Toast.LENGTH_SHORT).show();
                }
            }
            Log.d("licznik", "po zmianie: " + temp);
            String[] putValue = {loadedData.get(foundedElementNumb)[0],loadedData.get(foundedElementNumb)[1],String.valueOf(temp)};
            loadedData.set(foundedElementNumb, putValue);
            Log.d("po zmianach", " " + loadedData.get(foundedElementNumb)[0] + " " + loadedData.get(foundedElementNumb)[1] + " " + loadedData.get(foundedElementNumb)[2]);
            ClearFields();
        }
    }
    public void Zawteirdz(View view){
        ChangeQuantity(true);
        barCode.requestFocus();
    }
    public void Delete(View view){
        ChangeQuantity(false);
    }

//SD card operations=====================================================================
    List<String[]> loadedData = new ArrayList<String[]>();
    public void ReadSD(String fileName){
        if (firstRun){
            loadedData.clear();
            loadedData=sdOperations.ReadData(fileName);

            if (loadedData.size()!=0)
                Toast.makeText(this, "load successfull", Toast.LENGTH_SHORT).show();
            firstRun = false;

            for (int i=0;i<sdOperations.ReadData(fileName).size();i++){
                Log.d("R data", "->" + loadedData.get(i)[0] + " "+loadedData.get(i)[1]+" "+loadedData.get(i)[2]);
            }
            return;
        }
        if (firstRun==false){
            DialogFragment warning = new CWorning();
            warning.show(getFragmentManager(),"warning2");
        }

    }
    public void WriteSD(){
        sdOperations.WriteData(loadedData);
    }
    @Override
    public void OnOKPressed() {
        firstRun = true;
        //ReadSD(fileName);
        ReadingTest(fileName);
    }
    File root1 = Environment.getExternalStorageDirectory();
    public void ReadingTest(String fileName){
        if (firstRun){
            File data = new File(root1,fileName+".csv");
            CSVReader read = null;
            // List<String[]> dataFromSD = new ArrayList<String[]>();
            loadedData.clear();
            String line[] = {};

            try{
                read = new CSVReader(new InputStreamReader(new FileInputStream(data)),';');
                while (true){
                    line = read.readNext();
                    if (line!=null){
                        loadedData.add(line);
                    }else {
                        //Toast.makeText(context, "load successfull", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }catch (FileNotFoundException e) {
                Toast.makeText(this,"no file found",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (loadedData.size()!=0)
                Toast.makeText(this, "load successfull", Toast.LENGTH_SHORT).show();
            firstRun = false;

           /* for (int i=0;i<sdOperations.ReadData(fileName).size();i++){
                Log.d("R data", "->" + loadedData.get(i)[0] + " "+loadedData.get(i)[1]+" "+loadedData.get(i)[2]);
            }*/
            return;
        }
        if (firstRun==false){
            DialogFragment warning = new CWorning();
            warning.show(getFragmentManager(),"warning2");
        }
        //Toast.makeText(context, "load successfull", Toast.LENGTH_SHORT).show();
        //return dataFromSD;
    }
//FOR datawedge==========================================================================
    @Override
    protected void onNewIntent(Intent intent) {
        handleDecodeData(intent);
    }

    private void handleDecodeData(Intent i) {
       if (block){
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
}
