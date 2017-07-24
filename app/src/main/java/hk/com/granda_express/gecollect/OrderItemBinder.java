package hk.com.granda_express.gecollect;

import android.databinding.BindingAdapter;
import android.databinding.ObservableArrayList;
import android.widget.ListView;

/**
 * Created by keith on 7/20/2017.
 */

public class OrderItemBinder {
    @BindingAdapter("bind:order_items")
    public static void bindList(ListView view, ObservableArrayList<Order> list) {
        OrderItemAdapter adapter = new OrderItemAdapter(list);
        view.setAdapter(adapter);
    }
}
