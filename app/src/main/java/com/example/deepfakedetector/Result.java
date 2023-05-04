package com.example.deepfakedetector;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class Result extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        VideoView videoView = findViewById(R.id.videoView);
        Bundle bundle = getIntent().getExtras();
        String prediction = bundle.getString("prediction");
        Uri vid_uri = Uri.parse(bundle.getString("selectedUri"));
        System.out.println(vid_uri);
        videoView.setVideoURI(vid_uri);
        videoView.start();
        TextView result = (TextView) findViewById(R.id.result2);
        result.setText(prediction);
        if (prediction.equals("Real"))
            result.setTextColor(Color.parseColor("#00ff00"));
        else
            result.setTextColor(Color.parseColor("#ff0000"));
    }

}
