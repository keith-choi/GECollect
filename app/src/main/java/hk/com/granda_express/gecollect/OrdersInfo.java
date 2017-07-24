package hk.com.granda_express.gecollect;

import android.databinding.ObservableArrayList;

import java.util.ArrayList;

/**
 * Created by keith on 7/20/2017.
 */

public class OrdersInfo {
    public ObservableArrayList<Order> list = new ObservableArrayList<>();

    public OrdersInfo(ArrayList<Order> orders) {
        this.list.addAll(orders);
    }
}
