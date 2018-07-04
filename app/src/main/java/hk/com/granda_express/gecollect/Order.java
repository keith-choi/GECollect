package hk.com.granda_express.gecollect;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by keith on 9/2/2016.
 */
public class Order {
    public String id;
    public String SenderId;
    public String SenderName;
    public String SenderPhoneNo;
    public String SenderAddress;
    public Date OrderTime;
    public String Description;
    public int Qty;
    public String DeliveryContact;
    public String DeliveryPhoneNo;
    public String DeliveryAddress;
    public float CollectAmount;
    public Date CollectTime;
    public String CollectedBy;
    public Date WeightTime;
    public String WeightBy;
    public float Weight;
    public String CustomerCode;
    public int DeliveryMethod;
    public int PaymentMethod;
    public int OrderNo;
    public String Remarks;
    public String SenderCompany;
    public String DeliveryCompany;
    public String PosNo;
    public Date ExpectTime;
    public String Handler;
    public Date HandleResponseTime;
    public float OverSizeAmount;

    public String getContent() {
        String content = Integer.toString(this.Qty) + "件";
        if (this.Description != null) {
            content += " " + this.Description.trim();
        }
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        String time = (String) df.format("HH:mm", this.ExpectTime);
        content += " 请在 " + time + " 后取件";
        return content;
    }

    public String getHandleInfo() {
        String content = "";
        if (this.Handler != null) {
            android.text.format.DateFormat df = new android.text.format.DateFormat();
            String time = (String)df.format("HH:mm", new Date());
            if (this.HandleResponseTime != null) {
                time = (String) df.format("HH:mm", this.HandleResponseTime);
            }
            return "于 " + time + " 由 " + this.Handler + " 回应";
        }
        return content;
    }

    public String getMaskedText(String text) {
        String result = text;
        int l = text.length();
        l = Math.min(5, l) - 1;
        if (l > 1) {
            result = text.substring(0, text.length() - l) + "****";
        }
        return result;
    }

    public String getMultipartMaskedText(String text) {
        String[] parts = text.split(",");
        List<String> _parts = new ArrayList<>();
        for (String part: parts) {
            _parts.add(this.getMaskedText(part));
        }
        return TextUtils.join(",", _parts);
    }
}
