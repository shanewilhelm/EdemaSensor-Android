package edu.wsu.edemasensor_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class RecordingSetupActivity extends AppCompatActivity {

    BluetoothDevice mSensor;
    String header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_setup);

        // Acquire mSensor from Intent's extras
        if(getIntent().getExtras() != null) {
            mSensor = getIntent().getExtras().getParcelable("mSensor");
        }
        else {
            // Cannot continue without bluetooth device
            // Return to ConnectActivity to get a working BT device
            Intent intent = new Intent(this, ConnectActivity.class);
            startActivity(intent);
        }
    }

    public void onClickStartRecording(View view) {
        if (validateInput())
        {
            createHeader();
            Intent intent = new Intent(this, RecordingFeedActivity.class);
            intent.putExtra("mSensor", mSensor);
            intent.putExtra("header", header);

            startActivity(intent);
        }
    }

    protected boolean validateInput() {
        EditText subjectId = (EditText) findViewById(R.id.subjectIdField);
        EditText experimentId = (EditText) findViewById(R.id.experimentIdField);
        Spinner activitySpinner = (Spinner) findViewById(R.id.activitySpinner);

        if (subjectId.getText().toString().equals("") || experimentId.getText().toString().equals("")){
            // Not all fields filled
            simpleAlert("All fields must be filled");
            return false;
        }
        else if (activitySpinner.getSelectedItem().toString().equals("")){
            // No activity selected
            simpleAlert("You must select an activity");
            return false;
        }
        return true;
    }

    // Creates the CSV header and adds it to the 'header' var
    protected void createHeader(){
        EditText subjectId = (EditText) findViewById(R.id.subjectIdField);
        EditText experimentId = (EditText) findViewById(R.id.experimentIdField);
        Spinner activitySpinner = (Spinner) findViewById(R.id.activitySpinner);

        header = "Subject ID: " + subjectId.getText() + "\n"
                +"Experiment ID: " + experimentId.getText() + "\n"
                +"Activity: " + activitySpinner.getSelectedItem().toString() + "\n"
                +SensorData.createCsvHeader() + "\n";
    }

    void simpleAlert(String message_body) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message_body)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
