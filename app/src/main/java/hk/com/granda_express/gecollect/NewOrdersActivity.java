package hk.com.granda_express.gecollect;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Date;

import hk.com.granda_express.gecollect.databinding.ActivityNewOrdersBinding;

public class NewOrdersActivity extends AppCompatActivity {
    private String userName;
    private ArrayList<Order> orders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_new_orders);

        ActivityNewOrdersBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_new_orders);

        Bundle extras = getIntent().getExtras();
        userName = extras.getString("UserName");
        String o = extras.getString("Orders");

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
        orders = gson.fromJson(o, new TypeToken<ArrayList<Order>>(){}.getType());

        binding.orderList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), PendingOrderActivity.class);
                Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
                String json = gson.toJson(orders.get(i));
                intent.putExtra("Order", json);
                intent.putExtra("UserName", userName);
                startActivity(intent);
            }
        });
        binding.setData(new OrdersInfo(orders));

        setTitle("待处理寄件 - " + Integer.toString(orders.size()));
    }
}
