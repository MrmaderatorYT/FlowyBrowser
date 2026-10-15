package com.ccs.flowybrowser;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for browser settings.
 * Allows changing search engine and managing trusted domains for JavaScript.
 */
public class SettingsActivity extends AppCompatActivity {

    private Spinner searchEngineSpinner;
    private Switch javaScriptSwitch;
    private EditText trustedDomainInput;
    private TextView trustedDomainsList;
    private Button addDomainButton;

    private BrowserDataManager dataManager;
    private BrowserDataManager.BrowserSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dataManager = new BrowserDataManager(this);
        settings = dataManager.getSettings();

        initializeViews();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        searchEngineSpinner = findViewById(R.id.searchEngineSpinner);
        javaScriptSwitch = findViewById(R.id.javaScriptSwitch);
        trustedDomainInput = findViewById(R.id.trustedDomainInput);
        trustedDomainsList = findViewById(R.id.trustedDomainsList);
        addDomainButton = findViewById(R.id.addDomainButton);

        // Setup search engine spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.search_engines, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchEngineSpinner.setAdapter(adapter);
    }

    private void loadSettings() {
        // Set search engine
        String[] engines = getResources().getStringArray(R.array.search_engines);
        for (int i = 0; i < engines.length; i++) {
            if (engines[i].toLowerCase().contains(settings.searchEngine)) {
                searchEngineSpinner.setSelection(i);
                break;
            }
        }

        // Set JavaScript switch
        javaScriptSwitch.setChecked(settings.enableJavaScript);

        // Display trusted domains
        updateTrustedDomainsDisplay();
    }

    private void setupListeners() {
        javaScriptSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.enableJavaScript = isChecked;
            saveSettings();
        });

        addDomainButton.setOnClickListener(v -> addTrustedDomain());
    }

    private void addTrustedDomain() {
        String domain = trustedDomainInput.getText().toString().trim().toLowerCase();
        if (domain.isEmpty()) {
            Toast.makeText(this, "Enter domain name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!domain.startsWith(".")) {
            domain = "." + domain;
        }

        if (!settings.trustedDomains.contains(domain)) {
            settings.trustedDomains.add(domain);
            saveSettings();
            updateTrustedDomainsDisplay();
            trustedDomainInput.setText("");
            Toast.makeText(this, "Domain added", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Domain already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTrustedDomainsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (String domain : settings.trustedDomains) {
            sb.append("• ").append(domain).append("\n");
        }
        trustedDomainsList.setText(sb.toString());
    }

    private void saveSettings() {
        // Update search engine based on spinner selection
        int position = searchEngineSpinner.getSelectedItemPosition();
        String[] engines = getResources().getStringArray(R.array.search_engines_values);
        if (position < engines.length) {
            settings.searchEngine = engines[position];
        }

        dataManager.saveSettings(settings);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings(); // Save on exit
    }
}