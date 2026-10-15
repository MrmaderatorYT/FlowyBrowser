package com.ccs.flowybrowser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing downloads.
 * Shows list of downloaded files with ability to open them.
 */
public class DownloadsActivity extends AppCompatActivity {

    private ListView downloadsListView;
    private List<File> downloadFiles;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        initializeViews();
        loadDownloads();
        setupListView();
    }

    private void initializeViews() {
        downloadsListView = findViewById(R.id.downloadsListView);
        TextView emptyText = findViewById(R.id.emptyDownloadsText);
        downloadsListView.setEmptyView(emptyText);
    }

    private void loadDownloads() {
        downloadFiles = new ArrayList<>();
        File downloadsDir = getExternalFilesDir("Downloads");

        if (downloadsDir != null && downloadsDir.exists()) {
            File[] files = downloadsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        downloadFiles.add(file);
                    }
                }
            }
        }

        String[] fileNames = new String[downloadFiles.size()];
        for (int i = 0; i < downloadFiles.size(); i++) {
            File file = downloadFiles.get(i);
            long sizeKB = file.length() / 1024;
            fileNames[i] = file.getName() + " (" + sizeKB + " KB)";
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        downloadsListView.setAdapter(adapter);
    }

    private void setupListView() {
        downloadsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < downloadFiles.size()) {
                File file = downloadFiles.get(position);
                openFile(file);
            }
        });
    }

    private void openFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", file);

            String mimeType = getMimeType(file.getName());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt": return "text/plain";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            case "zip": return "application/zip";
            default: return "application/octet-stream";
        }
    }
}