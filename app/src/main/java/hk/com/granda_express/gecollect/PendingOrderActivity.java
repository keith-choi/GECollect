package hk.com.granda_express.gecollect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static hk.com.granda_express.gecollect.R.id.phoneNos;

public class PendingOrderActivity extends AppCompatActivity {
    private String userName;
    private Order order;

    private Spinner phoneNos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_order);

        Bundle extras = getIntent().getExtras();
        userName = extras.getString("UserName");
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
        this.order = gson.fromJson(extras.getString("Order"), Order.class);

        setTitle("单号：" + Integer.toString(this.order.OrderNo));

        StringBuilder sb = new StringBuilder(this.order.SenderName);
        if (!TextUtils.isEmpty(this.order.SenderCompany)) {
            sb.append("\n" + this.order.SenderCompany);
        }
        if (!TextUtils.isEmpty(this.order.SenderPhoneNo)) {
            sb.append("\n" + this.order.SenderPhoneNo);
        }
        if (!TextUtils.isEmpty(this.order.SenderAddress)) {
            sb.append("\n" + this.order.SenderAddress);
        }
        ((TextView) findViewById(R.id.senderView)).setText(sb.toString());

        phoneNos = (Spinner) findViewById(R.id.phoneNos);
        ArrayList<String> noList = new ArrayList<String>(Arrays.asList(order.SenderPhoneNo.split(",")));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, noList);
        phoneNos.setAdapter(adapter);

        ((TextView) findViewById(R.id.descriptionView)).setText(Integer.toString(this.order.Qty) + "件 " + this.order.Description);

        NumberFormat format = NumberFormat.getCurrencyInstance();
        ((TextView) findViewById(R.id.collectAmountView)).setText(format.format(this.order.CollectAmount));

        ((TextView) findViewById(R.id.remarksView)).setText(this.order.Remarks);
    }

    public void makeCall(View view) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNos.getSelectedItem().toString()));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(intent);
    }

    public void submitOrder(View view) {
        try {
            this.order.Handler = userName;
            this.order.HandleResponseTime = new Date();

            new saveOrder().execute(this.order);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class saveOrder extends AsyncTask<Order, Integer, String> {
        @Override
        protected String doInBackground(Order... params) {
            Order order = params[0];
            try {
                Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
                String json = gson.toJson(order);

                URL url = new URL(new ServiceUrl().REST_SERVICE_URL + "/" + order.id);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                try {
                    connection.setRequestMethod("PUT");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "application/json");
                    OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                    out.write(json);
                    out.flush();
                    out.close();
                    int responseCode = connection.getResponseCode();
                    if (responseCode > 299) {
                        throw new Exception(connection.getResponseMessage());
                    }
                    return json;
                } catch (Exception e) {
                    final String msg = e.getMessage().toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                final String msg = e.getMessage().toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
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
                        Intent intent = new Intent(PendingOrderActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }
    }
}
