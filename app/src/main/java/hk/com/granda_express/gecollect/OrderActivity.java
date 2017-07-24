package hk.com.granda_express.gecollect;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

public class OrderActivity extends AppCompatActivity {
    Order order;
    String username;
    String printerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init();
    }

    private void init() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.username = extras.getString("UserName");
            this.printerName = extras.getString("PrinterName");

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
            ((TextView)findViewById(R.id.senderView)).setText(sb.toString());

            ((TextView)findViewById(R.id.descriptionView)).setText(this.order.Description);

            sb = new StringBuilder(this.order.DeliveryAddress);
            if (!TextUtils.isEmpty(this.order.DeliveryCompany)) {
                sb.append("\n" + this.order.DeliveryCompany);
            }
            if (!TextUtils.isEmpty(this.order.DeliveryContact)) {
                sb.append("\n" + this.order.DeliveryContact);
            }
            if (!TextUtils.isEmpty(this.order.DeliveryPhoneNo)) {
                sb.append("\n" + this.order.DeliveryPhoneNo);
            }
            if (!TextUtils.isEmpty(this.order.CustomerCode)) {
                sb.append("\n" + this.order.CustomerCode);
            }
            ((TextView)findViewById(R.id.deliveryView)).setText(sb.toString());

            EditText qtyEdit = (EditText)findViewById(R.id.quantityText);
            qtyEdit.setText(Integer.toString(this.order.Qty));
            ((EditText)findViewById(R.id.collectAmountText)).setText(Float.toString(this.order.CollectAmount));

            RadioButton dmDelivery = (RadioButton)findViewById(R.id.dmDelivery);
            dmDelivery.setChecked(this.order.DeliveryMethod == 0);

            RadioButton pmHK = (RadioButton)findViewById(R.id.pmHK);
            pmHK.setChecked(this.order.PaymentMethod == 0);

            EditText remarksEdit = (EditText)findViewById(R.id.remarksText);
            remarksEdit.setText(this.order.Remarks);

            String status = "";
            if (this.order.CollectTime != null) {
                qtyEdit.setInputType(0);
                qtyEdit.setEnabled(false);
                ((RadioButton)findViewById(R.id.dmDelivery)).setEnabled(false);
                ((RadioButton)findViewById(R.id.dmSelfPickup)).setEnabled(false);
                ((RadioButton)findViewById(R.id.pmSZ)).setEnabled(false);
                ((RadioButton)findViewById(R.id.pmHK)).setEnabled(false);
                ((RadioButton)findViewById(R.id.pmSZ)).setEnabled(false);
                remarksEdit.setEnabled(false);
                ((EditText)findViewById(R.id.collectAmountText)).setEnabled(false);
                status = "貨件于" + android.text.format.DateFormat.format("yyyy-MM-dd hh:mm", this.order.CollectTime) + ", 由" + this.order.CollectedBy + "收取";

                ((Button)findViewById(R.id.submit)).setEnabled(false);
                ((Button)findViewById(R.id.reprint)).setEnabled(true);
                ((TextView)findViewById(R.id.pageNoLabel)).setEnabled(true);
                ((EditText)findViewById(R.id.pageNoText)).setEnabled(true);

                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            } else {
                ((RadioButton)findViewById(R.id.dmDelivery)).setEnabled(true);
                ((RadioButton)findViewById(R.id.dmSelfPickup)).setEnabled(true);
                ((RadioButton)findViewById(R.id.pmHK)).setEnabled(true);
                ((RadioButton)findViewById(R.id.pmSZ)).setEnabled(true);
                remarksEdit.setEnabled(true);

                ((Button)findViewById(R.id.submit)).setEnabled(true);
                ((Button)findViewById(R.id.reprint)).setEnabled(false);
                ((TextView)findViewById(R.id.pageNoLabel)).setEnabled(false);
                ((EditText)findViewById(R.id.pageNoText)).setEnabled(false);
            }
            ((TextView)findViewById(R.id.statusView)).setText(status);
        }
    }

    public void submitOrder(View view) {
        try {
            TextView statusView = (TextView) findViewById(R.id.statusView);
            statusView.setText("更新中 ...");

            EditText editText = (EditText) findViewById(R.id.quantityText);
            order.Qty = Integer.parseInt(editText.getText().toString());
            editText = (EditText) findViewById(R.id.collectAmountText);
            order.CollectAmount = Float.parseFloat(editText.getText().toString());
            order.CollectTime = new Date();
            order.CollectedBy = this.username;
            RadioButton dmDelivery = (RadioButton) findViewById(R.id.dmDelivery);
            order.DeliveryMethod = dmDelivery.isChecked() ? 0 : 1;
            RadioButton pmHK = (RadioButton) findViewById(R.id.pmHK);
            order.PaymentMethod = pmHK.isChecked() ? 0 : 1;
            EditText remarksText = (EditText) findViewById(R.id.remarksText);
            order.Remarks = remarksText.getText().toString();

            new saveOrder().execute(this.order);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printPage(View view) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
        String json = gson.toJson(this.order);

        String pageNo = ((EditText)findViewById(R.id.pageNoText)).getText().toString();
        try {
            int pn = 0;
            if (!pageNo.equals("")) {
                pn = Integer.parseInt(pageNo);
            }
            if (pn <= this.order.Qty) {
                if (pn < 1) {
                    pageNo = "reprintall";
                }

                Toast.makeText(getApplicationContext(), "打印收据", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(OrderActivity.this, PrintActivity.class);
                intent.putExtra("Order", json);
                intent.putExtra("Pages", pageNo);
                startActivity(intent);
            }
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
                }
                catch (Exception e) {
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
                        TextView statusView = (TextView) findViewById(R.id.statusView);
                        statusView.setText("更新完成");

                        Toast.makeText(getApplicationContext(), "打印收据", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(OrderActivity.this, PrintActivity.class);
                        intent.putExtra("Order", result);
                        intent.putExtra("Pages", "all");
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }

    }
}
