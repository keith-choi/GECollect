package hk.com.granda_express.gecollect;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import hk.com.granda_express.gecollect.databinding.OrderItemBinding;

/**
 * Created by keith on 7/20/2017.
 */

public class OrderItemAdapter extends BaseAdapter {
    private ObservableArrayList<Order> list;
    private LayoutInflater inflater;

    public OrderItemAdapter(ObservableArrayList<Order> orders) { list = orders; };

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null) {
            inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        OrderItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.order_item, parent, false);
        binding.setOrder(list.get(position));

        return binding.getRoot();
    }
}
