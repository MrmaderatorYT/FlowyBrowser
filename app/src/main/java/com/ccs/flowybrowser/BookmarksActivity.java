package com.ccs.flowybrowser;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

/**
 * Activity for managing bookmarks.
 * Shows list of saved bookmarks with ability to open or remove them.
 */
public class BookmarksActivity extends AppCompatActivity {

    private ListView bookmarksListView;
    private BrowserDataManager dataManager;
    private ArrayAdapter<String> adapter;
    private List<BrowserDataManager.BookmarkItem> bookmarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        dataManager = new BrowserDataManager(this);
        initializeViews();
        loadBookmarks();
        setupListView();
    }

    private void initializeViews() {
        bookmarksListView = findViewById(R.id.bookmarksListView);
        TextView emptyText = findViewById(R.id.emptyBookmarksText);
        bookmarksListView.setEmptyView(emptyText);
    }

    private void loadBookmarks() {
        bookmarks = dataManager.getBookmarks();
        String[] bookmarkTitles = new String[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) {
            bookmarkTitles[i] = bookmarks.get(i).title;
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bookmarkTitles);
        bookmarksListView.setAdapter(adapter);
    }

    private void setupListView() {
        bookmarksListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < bookmarks.size()) {
                    String url = bookmarks.get(position).url;
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("url", url);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }
            }
        });

        bookmarksListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < bookmarks.size()) {
                    String url = bookmarks.get(position).url;
                    dataManager.removeBookmark(url);
                    loadBookmarks(); // Refresh list
                    Toast.makeText(BookmarksActivity.this, "Bookmark removed", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }
}