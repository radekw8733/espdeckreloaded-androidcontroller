package net.radekw8733.espdeckreloaded;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                configureButtonClickListener();
            }
        });
    }

    public void configureButtonClickListener() {
        Intent intent = new Intent(this,ConfiguratorActivity.class);
        startActivity(intent);
    }
}