package hk.com.granda_express.gecollect;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bxl.config.editor.BXLConfigLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.ObjectConstructor;
import com.google.zxing.Result;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import jpos.JposException;
import jpos.POSPrinterConst;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.ErrorCallback;
import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class MainActivity extends AppCompatActivity
        implements ZXingScannerView.ResultHandler,
        OperatorFragment.OnFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener {

    private final String GE_PREFS = "GE_PREFS";
    private final String KEY_OPERATOR = "operator";
    private final String KEY_PRINTERNAME = "printname";
    private final String KEY_LED = "led";

    EditText inputIDText;
    ZXingScannerView mScannerView;
    String printerName;

    private static HubConnection mHubConnection;
    private static HubProxy mHubProxy;
    private static String mConnectionID;
    private static Object mSync = new Object();
    private static Date mLastNotification;

    private Button newOrdersButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Platform.loadPlatformComponent(new AndroidPlatformComponent());
        try {
            mHubConnection = new HubConnection(ServiceUrl.BASE_SERVICE_URL);
            mHubProxy = mHubConnection.createHubProxy("AppEventHub");
            mHubConnection.error(new ErrorCallback() {
                @Override
                public void onError(Throwable error) {
                    System.err.println("There was an error communicating with the server.");
                    System.err.println("Error detail: " + error.toString());

                    error.printStackTrace(System.err);
                }
            });
            mHubProxy.subscribe(new Object() {
                @SuppressWarnings("unused")
                public void AckBC(final String msg) {
                    synchronized (mSync) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (msg.equals("新单") || msg.equals("取消")) {
                                    mLastNotification = new Date();
                                    DateFormat dateFormat = new SimpleDateFormat("MMM dd,yyyy  hh:mm a");
                                    if (msg.equals("取消")) {
                                        newOrdersButton.setBackgroundColor(Color.RED);;
                                    } else {
                                        newOrdersButton.setBackgroundColor(Color.BLUE);
                                    }
                                    newOrdersButton.setTextColor(Color.WHITE);
                                    newOrdersButton.setText(msg + ": " + dateFormat.format(mLastNotification));

                                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                                    r.play();
                                }
                            }
                        });
                    }
                }
            });
            ClientTransport transport = new ServerSentEventsTransport(mHubConnection.getLogger());
            mHubConnection.start(transport);
        } catch (Exception e) {
            e.printStackTrace();
        }

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_user:
                // Create and show the dialog.
                DialogFragment userFragment = OperatorFragment.newInstance();
                userFragment.show(getSupportFragmentManager(), "operator_dialog");
                break;
            case R.id.action_settings:
                DialogFragment settingsFragment = SettingsFragment.newInstance();
                settingsFragment.show(getSupportFragmentManager(), "settings_dialog");
                break;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScannerView != null) {
            mScannerView.setFlash(false);
            mScannerView.stopCamera();           // Stop camera on pause
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle("收件管理");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences prefs = getSharedPreferences(GE_PREFS, MODE_PRIVATE);
        String userName = prefs.getString(KEY_OPERATOR, null);
        ((TextView) findViewById(R.id.textOperator)).setText(userName);
        boolean enableFlash = prefs.getBoolean(KEY_LED, false);
        ((CheckBox) findViewById(R.id.enableFlash)).setChecked(enableFlash);
        this.printerName = prefs.getString(KEY_PRINTERNAME, null);

        if (mLastNotification != null) {
            newOrdersButton.setBackgroundColor(Color.LTGRAY);
            newOrdersButton.setTextColor(Color.BLACK);
            DateFormat dateFormat = new SimpleDateFormat("MMM dd,yyyy  hh:mm a");
            newOrdersButton.setText("上次接收：" + dateFormat.format(mLastNotification));
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        setContentView(R.layout.activity_main);
        processOrderId(rawResult.getText());
    }

    public String getUserName() {
        TextView view = (TextView) findViewById(R.id.textOperator);
        return view.getText().toString();
    }

    public void setUserName(String userName) {
        TextView view = (TextView) findViewById(R.id.textOperator);
        view.setText(userName);
        SharedPreferences.Editor editor = getSharedPreferences(GE_PREFS, MODE_PRIVATE).edit();
        editor.putString(KEY_OPERATOR, userName);
        editor.commit();
    }

    public String getPrinterName() {
        return this.printerName;
    }

    public void setPrinterName(String printerName) {
        SharedPreferences.Editor editor = getSharedPreferences(GE_PREFS, MODE_PRIVATE).edit();
        editor.putString(KEY_PRINTERNAME, printerName);
        editor.commit();
    }

    private void init() {
        inputIDText = (EditText) findViewById(R.id.editText);
        inputIDText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                processOrderId(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        newOrdersButton = (Button) findViewById(R.id.button3);
        newOrdersButton.setBackgroundColor(Color.LTGRAY);;
        newOrdersButton.setTextColor(Color.BLACK);
    }

    private void processOrderId(String id) {
        SharedPreferences prefs = getSharedPreferences(GE_PREFS, MODE_PRIVATE);
        String userName = prefs.getString(KEY_OPERATOR, null);
        ((TextView) findViewById(R.id.textOperator)).setText(userName);

        if (id.trim().length() == 36) {
            if (TextUtils.isEmpty(userName)) {
                Toast.makeText(getApplicationContext(), "请输入操作员名称", Toast.LENGTH_LONG).show();
            } else {
                new RetrieveOrder().execute(id);
            }
            inputIDText.setText("");
        }
    }

    public void checkLed(View view) {
        boolean flashEnabled = ((CheckBox) view).isChecked();
        SharedPreferences.Editor editor = getSharedPreferences(GE_PREFS, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_LED, flashEnabled);
        editor.commit();
    }

    public void QrScanner(View view) {
        CheckBox flash = (CheckBox) findViewById(R.id.enableFlash);
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
        mScannerView.setFlash(flash.isChecked());
    }

    public void showNewOrders(View view) {
        TextView msg = (TextView) findViewById(R.id.messageView);
        msg.setText("正在载入寄件，请稍候");
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        String date = (String) df.format("yyyy-MM-dd", new Date());
        new RetrieveOrders().execute(date);
    }

    class RetrieveOrder extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView messageView = (TextView) findViewById(R.id.messageView);
                    messageView.setText("请稍候，下载中　．．．");
                }
            });
            try {
                URL url = new URL(new ServiceUrl().REST_SERVICE_URL + "/" + params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    connection.disconnect();
                }
            } catch (final MalformedURLException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            if (result != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView messageView = (TextView) findViewById(R.id.messageView);
                        messageView.setText("");
                        Intent intent = new Intent(MainActivity.this, OrderActivity.class);
                        intent.putExtra("Order", result);
                        intent.putExtra("UserName", getUserName());
                        startActivity(intent);
                    }
                });
            }
        }
    }

    class RetrieveOrders extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView messageView = (TextView) findViewById(R.id.messageView);
                    messageView.setText("请稍候，正在载入寄件　．．．");
                }
            });
            try {
                URL url = new URL(new ServiceUrl().PENDING_ORDERS_URL + params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    connection.disconnect();
                }
            } catch (MalformedURLException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            if (result != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView messageView = (TextView) findViewById(R.id.messageView);
                        messageView.setText("");
                        Intent intent = new Intent(MainActivity.this, NewOrdersActivity.class);
                        intent.putExtra("Orders", result);
                        intent.putExtra("UserName", getUserName());
                        startActivity(intent);
                    }
                });
            }
        }
    }
}

