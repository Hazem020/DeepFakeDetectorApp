package com.example.deepfakedetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public class Upload extends AppCompatActivity {
    private static final int PICKFILE_REQUEST_CODE = 100;
    String selectedPath;
    Uri selectedUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        TextView upload = (TextView) findViewById(R.id.upload_video);
        Button upload_button = (Button) findViewById(R.id.upload_bt);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectVideo(view);
            }
        });
        upload_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    connectServer(view);
                } catch (FileNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Please choose a file", Toast.LENGTH_LONG).show();
                } catch (NullPointerException e) {
                    Toast.makeText(getApplicationContext(), "please choose a file", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    void connectServer(View v) throws FileNotFoundException, NullPointerException {
        String ipv4Address = "192.168.1.19";
        String portNumber = "4000";
        String postUrl = "http://" + ipv4Address + ":" + portNumber + "/";

        File videoFile = new File(selectedPath);
        final long fileSize;
        fileSize = videoFile.length();
        FileInputStream fileInputStream = new FileInputStream(videoFile);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        RequestBody requestBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("video");
            }

            @Override
            public long contentLength() throws IOException {
                return fileSize;
            }

            @Override
            public void writeTo(@NonNull BufferedSink sink) throws IOException {
                byte[] buffer = new byte[65536];
                int bytesRead = 0;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    sink.write(buffer, 0, bytesRead);
                }
            }
        };
        RequestBody postBodyVideo = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("video", videoFile.getName(), requestBody).build();
        postRequest(postUrl, postBodyVideo);
    }

    void postRequest(String postUrl, RequestBody postBody) {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(postUrl).post(postBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println(e.toString());
                // Cancel the post on failure.
                call.cancel();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getApplicationContext(), "Failed to Connect to Server", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                            String model_response = response.body().string();
                            JsonObject jsonObject = new JsonParser().parse(model_response).getAsJsonObject();
                            String prediction;
                            try {
                                prediction = jsonObject.get("prediction_label").getAsString();
                            } catch (Exception e) {
                                progressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(getApplicationContext(), "Error processing this video!", Toast.LENGTH_LONG).show();
                                return;
                            }
                            //System.out.println(prediction);
                            Intent intent = new Intent(Upload.this, Result.class);
                            intent.putExtra("prediction", prediction);
                            System.out.println(selectedPath);
                            intent.putExtra("selectedUri", selectedUri.toString());
                            startActivity(intent);
                            System.out.println(prediction);
                            progressBar.setVisibility(View.INVISIBLE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void selectVideo(View view) {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("video/*");
        startActivityForResult(Intent.createChooser(chooseFile, "Choose a file"), PICKFILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        TextView vidPath = (TextView) findViewById(R.id.vidPath);
        if (resCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            selectedUri = uri;
            selectedPath = getPath(getApplicationContext(), uri);
            vidPath.setText(selectedPath);
        }
    }

    public static String getPath(final Context context, final Uri uri) {

        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                return "storage" + "/" + docId.replace(":", "/");
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                String fileName = getFilePath(context, uri);
                if (fileName != null) {
                    return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                }
                String id = DocumentsContract.getDocumentId(uri);
                //System.out.println(uri);
                if (id.startsWith("raw:")) {
                    id = id.replaceFirst("raw:", "");
                    return id;
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                final Uri contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
               // System.out.println(contentUri);
               // System.out.println(selection);
               // System.out.println(Arrays.toString(selectionArgs));
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getFilePath(Context context, Uri uri) {
        final String column = MediaStore.MediaColumns.DISPLAY_NAME;
        final String[] projection = {
                column
        };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}