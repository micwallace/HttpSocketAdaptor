package au.com.wallaceit.httpsocketadaptor;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
    private Button serverbtn;
    private TextView stattxt;
    private EditText desthost;
    private EditText destport;
    private EditText sourceport;
    private SharedPreferences prefs;
    private boolean serverstarted =false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
        serverbtn = (Button) findViewById(R.id.serverbtn);
        stattxt = (TextView) findViewById(R.id.stattxt);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        sourceport = (EditText) findViewById(R.id.sourceport);
        desthost = (EditText) findViewById(R.id.desthost);
        destport = (EditText) findViewById(R.id.destport);
        sourceport.setText(prefs.getString("prefsourceport", "8080"));
        desthost.setText(prefs.getString("prefdesthost", "192.168.1.87"));
        destport.setText(prefs.getString("prefdestport", "9100"));
        sourceport.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString("prefsourceport", sourceport.getText().toString()).apply();
            }
        });
        desthost.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString("prefdesthost", desthost.getText().toString()).apply();
            }
        });
        destport.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString("prefdestport", destport.getText().toString()).apply();
            }
        });

        serverstarted = isServiceRunning(RelayService.class);
        setButton(serverstarted);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startService(){
        Intent sintent = new Intent(MainActivity.this, RelayService.class);
        sintent.putExtra("sourceport", ((TextView) findViewById(R.id.sourceport)).getText().toString());
        sintent.putExtra("desthost", ((TextView) findViewById(R.id.desthost)).getText().toString());
        sintent.putExtra("destport", ((TextView) findViewById(R.id.destport)).getText().toString());
        startService(sintent);
        // set new button click
        setButton(true);
    }

    public void stopService(){
        Intent sintent = new Intent(MainActivity.this, RelayService.class);
        stopService(sintent);
        // set new button click
        setButton(false);
    }

    private void setButton(boolean started){
        if (started){
            serverbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopService();
                }
            });
            serverbtn.setText("Stop Relay");
            stattxt.setText("Relay Running");
        } else {
            serverbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startService();
                }
            });
            serverbtn.setText("Start Relay");
            stattxt.setText("Relay Stopped");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

}
