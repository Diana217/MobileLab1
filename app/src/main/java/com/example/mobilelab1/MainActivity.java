package com.example.mobilelab1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.os.Build;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private Spinner spinnerCreditProgram;
    private EditText editTextSum;
    private EditText editTextRepayment;
    private EditText editTextPeriod;
    private TextView textResult;
    private LineChart chart;
    File file;
    HashMap<String, String> savedValues = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupFile();
        initializeViews();
        setListeners();
        restoreValues();
    }

    private void initializeViews() {
        spinnerCreditProgram = findViewById(R.id.spinnerCreditProgram);
        editTextSum = findViewById(R.id.editTextSum);
        editTextRepayment = findViewById(R.id.editTextRepayment);
        editTextPeriod = findViewById(R.id.editTextPeriod);
        textResult = findViewById(R.id.textResult);
        chart = findViewById(R.id.chart);
        chart.setVisibility(View.GONE);
    }

    private void setListeners() {
        Button calculateButton = findViewById(R.id.buttonCalculate);
        calculateButton.setOnClickListener(v -> calculateResult());

        Button aboutMeButton = findViewById(R.id.buttonAboutMe);
        aboutMeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutMeActivity.class);
            startActivity(intent);
        });
    }

    public void setupFile(){
        File dir = Environment.getExternalStorageDirectory();
        file = new File(dir,"data");
        if (!file.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= 30){
                    if (!Environment.isExternalStorageManager()){
                        Intent getpermission = new Intent();
                        getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(getpermission);
                    }
                }
                file.createNewFile();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void saveToFile(HashMap<String, String> values) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            for (Map.Entry<String, String> entry : values.entrySet()) {
                outputStreamWriter.write(entry.getKey() + ":" + entry.getValue() + "\n");
            }
            outputStreamWriter.flush();
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, String> getFromFile() {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    savedValues.put(parts[0], parts[1]);
                }
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return savedValues;
    }

    private void restoreValues() {
        getFromFile();

        String savedProgram = savedValues.get("creditprogram");
        int programPosition = ((ArrayAdapter<String>) spinnerCreditProgram.getAdapter()).getPosition(savedProgram);
        spinnerCreditProgram.setSelection(programPosition);
        editTextSum.setText(savedValues.get("sum"));
        editTextPeriod.setText(savedValues.get("period"));
        editTextRepayment.setText(savedValues.get("repayment"));
    }

    private void saveToHashMap(String key, String value) {
        savedValues.put(key, value);
    }

    private void calculateResult() {
        String creditProgram = spinnerCreditProgram.getSelectedItem().toString();

        String sumStr = editTextSum.getText().toString();
        String periodStr = editTextPeriod.getText().toString();
        String repaymentStr = editTextRepayment.getText().toString();

        saveToHashMap("creditprogram", creditProgram);
        saveToHashMap("sum", sumStr);
        saveToHashMap("period", periodStr);
        saveToHashMap("repayment", repaymentStr);

        saveToFile(savedValues);

        StringBuilder resultStrBuilder = new StringBuilder();
        try {
            int sum = Integer.parseInt(sumStr);
            int period = Integer.parseInt(periodStr);
            int repayment = Integer.parseInt(repaymentStr);
            double result = 0;
            double percent = 0;
            int programPeriod = 0;

            switch (creditProgram) {
                case "7% річних, 24 місяці":
                    percent = 7;
                    programPeriod = 24;
                    break;
                case "5% річних, 36 місяців":
                    percent = 5;
                    programPeriod = 36;
                    break;
                case "4,5% річних, 48 місяців":
                    percent = 4.5;
                    programPeriod = 48;
                    break;
                case "3,75% річних, 60 місяців":
                    percent = 3.75;
                    programPeriod = 60;
                    break;
                default:
                    resultStrBuilder.append("Щось пішло не так..\n");
                    break;
            }
            double fullSum = calculateFullSum(sum, programPeriod, percent);
            result = calculateDebt(fullSum, repayment, period);
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedResult = df.format(result);
            resultStrBuilder.append("Сума боргу: ").append(formattedResult).append("\n");

            ArrayList<Entry> entries = new ArrayList<>();
            while (result > repayment) {
                entries.add(new Entry(period, (float) result));
                result -= repayment;
                period++;
            }
            updateChart(entries);
            chart.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            resultStrBuilder.append("Щось пішло не так..");
        }
        textResult.setText(resultStrBuilder.toString());
    }

    private double calculateFullSum(double sum, int programPeriod, double percent) {
        double result = sum * (1 + (percent * programPeriod) / (100 * 12));
        return result;
    }

    private double calculateDebt(double fullSum, double repayment, int period) {
        double result = fullSum - repayment * period;
        return result;
    }

    private void updateChart(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Залишок заборгованості");

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);

        chart.setData(lineData);
        chart.getDescription().setText("Графік залишку заборгованості");

        chart.invalidate();
    }
}