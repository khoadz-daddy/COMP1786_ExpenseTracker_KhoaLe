package com.mk183.exercise1;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etTemperature;
    private Spinner spFromUnit;
    private Spinner spToUnit;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTemperature = findViewById(R.id.etTemperature);
        spFromUnit = findViewById(R.id.spFromUnit);
        spToUnit = findViewById(R.id.spToUnit);
        tvResult = findViewById(R.id.tvResult);
        Button btnConvert = findViewById(R.id.btnConvert);

        String[] units = {"Celsius", "Fahrenheit", "Kelvin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFromUnit.setAdapter(adapter);
        spToUnit.setAdapter(adapter);
        spToUnit.setSelection(1);

        btnConvert.setOnClickListener(v -> convertTemperature());
    }

    private void convertTemperature() {
        String input = etTemperature.getText().toString().trim();
        if (!isValidInput(input)) {
            etTemperature.setError("Please enter a valid number");
            tvResult.setText(R.string.default_result);
            return;
        }

        double value = Double.parseDouble(input);
        String fromUnit = spFromUnit.getSelectedItem().toString();
        String toUnit = spToUnit.getSelectedItem().toString();

        double result = convertTemperatureValue(value, fromUnit, toUnit);
        String message = String.format("Result: %.2f %s", result, toUnit);
        tvResult.setText(message);
    }

    private boolean isValidInput(String input) {
        if (input.isEmpty()) {
            return false;
        }

        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private double convertTemperatureValue(double value, String fromUnit, String toUnit) {
        double celsius;
        switch (fromUnit) {
            case "Fahrenheit":
                celsius = (value - 32) * 5 / 9;
                break;
            case "Kelvin":
                celsius = value - 273.15;
                break;
            default:
                celsius = value;
                break;
        }

        switch (toUnit) {
            case "Fahrenheit":
                return celsius * 9 / 5 + 32;
            case "Kelvin":
                return celsius + 273.15;
            default:
                return celsius;
        }
    }
}
