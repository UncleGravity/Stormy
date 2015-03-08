package com.angelviera.stormy;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;


public class AsyncTestActivity extends ActionBarActivity {

    Button button;
    Button button2;
    ProgressBar spinner;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_async_test);

        button = (Button) findViewById(R.id.asyncButton); // Download button
        button2 = (Button) findViewById(R.id.free_button); // Toast Button
        spinner = (ProgressBar) findViewById(R.id.progressBar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);



        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ASYNC CALL with Parameter!
                new DownloadFilesTask().execute("titties");

                button.setEnabled(false);
            }
        });


        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AsyncTestActivity.this, "Things are happenning!", Toast.LENGTH_SHORT).show();
            }
        });


    }

    ///////// Async Sub Class //////////

    private class DownloadFilesTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            spinner.setVisibility(Button.VISIBLE);
            progressBar.setVisibility(Button.VISIBLE);
            progressBar.setProgress(0);

        }

        @Override
        protected Long doInBackground(String... param) {

            Log.e("doInBackgroundParam",param[0]);

            long bytes = 100;

            for (int i = 0; i <= bytes; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                publishProgress(i);
                //progressBar.setProgress(i);
                // Escape early if cancel() is called
                if (isCancelled()) break;
            }

          return bytes;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBar.setProgress(progress[0]);
            Log.e("Running: ","#"+progress[0]);
        }

        @Override
        protected void onPostExecute(Long result) {
            Log.e("Log: ","Downloaded " + result + " bytes");
            spinner.setVisibility(Button.INVISIBLE);
            progressBar.setVisibility(Button.INVISIBLE);
            progressBar.setProgress(0);

            button.setEnabled(true);

            //Toast.makeText(AsyncTestActivity.this,"Download Finished!", Toast.LENGTH_SHORT).show();
        }
    }


}
