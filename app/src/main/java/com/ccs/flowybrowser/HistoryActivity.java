package com.ccs.flowybrowser;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for browsing history.
 * Shows list of visited sites with timestamps.
 */
public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private BrowserDataManager dataManager;
    private ArrayAdapter<String> adapter;
    private List<BrowserDataManager.HistoryItem> history;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dataManager = new BrowserDataManager(this);
        initializeViews();
        loadHistory();
        setupListView();
    }

    private void initializeViews() {
        historyListView = findViewById(R.id.historyListView);
        TextView emptyText = findViewById(R.id.emptyHistoryText);
        historyListView.setEmptyView(emptyText);
    }

    private void loadHistory() {
        history = dataManager.getHistory();
        String[] historyTitles = new String[history.size()];
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

        for (int i = 0; i < history.size(); i++) {
            BrowserDataManager.HistoryItem item = history.get(i);
            String date = sdf.format(new Date(item.timestamp));
            historyTitles[i] = item.title + "\n" + date;
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyTitles);
        historyListView.setAdapter(adapter);
    }

    private void setupListView() {
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < history.size()) {
                    String url = history.get(position).url;
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("url", url);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        });
    }
}