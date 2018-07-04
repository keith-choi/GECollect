package hk.com.granda_express.gecollect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import com.bxl.config.editor.BXLConfigLoader;
import com.bxl.config.simple.editor.JposEntryComparable;
import com.bxl.printer.POSCommand;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import jpos.JposConst;
import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.config.JposEntry;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import mf.org.apache.xml.resolver.readers.SAXParserHandler;

public class PrintActivity extends AppCompatActivity
    implements ErrorListener {

    private final String GE_PREFS = "GE_PREFS";
    private final String KEY_OPERATOR = "operator";
    private final String KEY_PRINTERNAME = "printname";

    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> btDevices;
    String printerName;

    BXLConfigLoader bxlConfigLoader;
    POSPrinter posPrinter;
    private static String ESCAPE_SEQUENCE = new String(new byte[] {0x1b, 0x7c});
    private static String INITIALIZE_PRINTER = new String(new byte[] {0x1b, 0x40});
    private static String HORIZONTAL_TAB = new String(new byte[] {0x09});
    private static String LINE_FEED = new String(new byte[] {0x0A});
    private static String FORM_FEED = new String(new byte[] {0x0C});
    private static String EMPHASIZED_ON = new String(new byte[] {0x1b, 0x45, 0x01});
    private static String EMPHASIZED_OFF = new String(new byte[] {0x1b, 0x45, 0x00});
    private static String UNDERLINE_ON = new String(new byte[] {0x1b, 0x2d, 0x02});
    private static String UNDERLINE_OFF = new String(new byte[] {0x1b, 0x2d, 0x00});
    private static String ALIGNMENT_LEFT = new String(new byte[] {0x1b, 0x61, 0x0});
    private static String ALIGNMENT_CENTER = new String(new byte[] {0x1b, 0x61, 0x1});
    private static String ALIGNMENT_RIGHT = new String(new byte[] {0x1b, 0x61, 0x2});
    private static String LEFT_MARGIN = new String(new byte[] {0x1d, 0x4c, 0x40, 0x00});
    private static String DEFALUT_LEFT_MARGIN = new String(new byte[] {0x1d, 0x4c, 0x00, 0x00});
    private static String LABEL_MODE = new String(new byte[] {0x08, 0x4C, 0x4C});

    TextView messageView;
    Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);

        messageView = (TextView)findViewById(R.id.messageView);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        bxlConfigLoader = new BXLConfigLoader(this);
        try {
            bxlConfigLoader.openFile();
        } catch (Exception e) {
            e.printStackTrace();
            bxlConfigLoader.newFile();
        }
        posPrinter = new POSPrinter(this);

        init();
    }

    public void init() {
        SharedPreferences prefs = getSharedPreferences(GE_PREFS, MODE_PRIVATE);
        this.printerName = prefs.getString(KEY_PRINTERNAME, null);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (btAdapter == null) {
                Toast.makeText(getApplicationContext(), "沒有蓝牙", Toast.LENGTH_LONG).show();
                finish();
            } else {
                if (!btAdapter.isEnabled()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, 1);
                }

                try {
                    getPairedDevices();
                } catch (JposException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
            this.order = gson.fromJson(extras.getString("Order"), Order.class);

            String pagesToPrint = extras.getString("Pages");
            if (pagesToPrint.equals("all")) {
                oposPrintWithCustomerReceipt();
            } else if (pagesToPrint.equals("reprintall")) {
                try {
                    oposPrint();
                    posPrinter.close();
                } catch (JposException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    finish();
                }
            } else {
                try {
                    int pageNo = Integer.parseInt(pagesToPrint);
                    oposPrintPage(pageNo);
                    posPrinter.close();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    finish();
                }
            }
        }
    }

    private void oposPrintWithCustomerReceipt() {
        oposPrintPage(0);
        try {
            oposPrint();
            oposPrintPage(-1);
            posPrinter.close();
        } catch (JposException e) {
            e.printStackTrace();
        }
        finish();
    }

    private void oposPrint() throws JposException {
        for (int i = 1; i < this.order.Qty + 1; i++) {
            oposPrintPage(i);
        }
    }

    private void oposPrintPage(int pageNo){
        try {
            oposPrintText(INITIALIZE_PRINTER, false);
            // oposPrintText(LABEL_MODE, false);
            oposPrintText(LEFT_MARGIN, false);
            // Skip top margin
            for (int i = 0; i < 10; i++) {
                oposPrintText(LINE_FEED, false);
            }
            oposPrintText("单号： " + HORIZONTAL_TAB + Integer.toString(order.OrderNo), false);
            if (pageNo == 0) {
                oposPrintText(HORIZONTAL_TAB + HORIZONTAL_TAB + HORIZONTAL_TAB + "客户联", false);
            } else if (pageNo == -1) {
                oposPrintText(HORIZONTAL_TAB + HORIZONTAL_TAB + HORIZONTAL_TAB + "公司副本", false);
            }

            oposPrintText(LINE_FEED, false);

            Integer l = 6;
            oposPrintText(LINE_FEED, false);
            // Print delivery information
            if (TextUtils.isEmpty(order.CustomerCode) || order.CustomerCode.length() == 6) {
                if (!TextUtils.isEmpty(order.DeliveryAddress)) {
                    if (pageNo == 0) {
                        oposPrintText(order.getMultipartMaskedText(order.DeliveryAddress), true);
                    } else {
                        oposPrintText(order.DeliveryAddress, true);
                    }
                    l--;
                }
                if (!TextUtils.isEmpty(order.DeliveryCompany)) {
                    oposPrintText(order.DeliveryCompany, true);
                    l--;
                }
                if (!TextUtils.isEmpty(order.DeliveryContact)) {
                    oposPrintText(order.DeliveryContact, true);
                    l--;
                }
                if (!TextUtils.isEmpty(order.DeliveryPhoneNo)) {
                    if (pageNo == 0) {
                        oposPrintText(order.getMultipartMaskedText(order.DeliveryPhoneNo), true);
                    } else {
                        oposPrintText(order.DeliveryPhoneNo, true);
                    }
                    l--;
                }
            }
            if (!TextUtils.isEmpty(order.CustomerCode)) {
                oposPrintText(order.CustomerCode, true);
                l--;
            }
            for (int i = 0; i < l; i++) {
                oposPrintText(LINE_FEED, false);
            }

            l = 8;
            oposPrintText(LINE_FEED, false);
            if (!TextUtils.isEmpty(order.SenderCompany)) {
                oposPrintText(order.SenderCompany, true);
                l--;
            }
            if (!TextUtils.isEmpty(order.PosNo)) {
                if (pageNo > 0) {
                    oposPrintText(order.getMultipartMaskedText(order.PosNo), true);
                } else {
                    oposPrintText(order.PosNo, true);
                }
                l--;
            }
            if (!TextUtils.isEmpty(order.SenderName)) {
                oposPrintText(order.SenderName, true);
                l--;
            }
            if (!TextUtils.isEmpty(order.SenderAddress)) {
                if (pageNo > 0) {
                    oposPrintText(order.getMultipartMaskedText(order.SenderAddress), true);
                } else {
                    oposPrintText(order.SenderAddress, true);
                }
                l--;
            }
            if (!TextUtils.isEmpty(order.SenderPhoneNo)) {
                if (pageNo > 0) {
                    oposPrintText(order.getMultipartMaskedText(order.SenderPhoneNo), true);
                } else {
                    oposPrintText(order.SenderPhoneNo, true);
                }
                l--;
            }
            for (int i = 0; i < l; i++) {
                oposPrintText(LINE_FEED, false);
            }

            oposPrintText(DEFALUT_LEFT_MARGIN, false);
            // Print order information
            String line = order.Description + "   " + Integer.toString(order.Qty) + "件";
            oposPrintText(line, true);
            line = "服务类型： " + (order.DeliveryMethod == 1 ? "自提" : "送货");
            oposPrintText(line, true);
            line = "付款方式： " + (order.PaymentMethod == 1 ? "深圳付" : "香港付");
            oposPrintText(line, true);
            if (order.CollectAmount > 0) {
                DecimalFormat df = new DecimalFormat("0.00");
                line = "代收款（人民币）： " + df.format(order.CollectAmount);
                oposPrintText(line, true);
            }
            if (!TextUtils.isEmpty(order.Remarks)) {
                line = "备注： " + HORIZONTAL_TAB + order.Remarks;
                oposPrintText(line, true);
            }

            // Print barcode of order id
            // oposPrintText(new String(new byte[] {0x1d, 0x28, 0x6b, 0x04, 0x00, 0x31, 0x43, 0x06}), false);
            String suffix = pageNo > 0 ? "-" + Integer.toString(pageNo) : "";
            posPrinter.printBarCode(
                    POSPrinterConst.PTR_S_RECEIPT,
                    order.id + suffix,
                    POSPrinterConst.PTR_BCS_QRCODE,
                    0, 300, 0, 0
            );

            // Print carton information
            if (pageNo > 0) {
                String text = String.format("箱号 %s / %s", pageNo, order.Qty);
                oposPrintText(ALIGNMENT_RIGHT + text, true);
            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

            oposPrintText("由" + order.CollectedBy + "于" + formatter.format(order.CollectTime) + "取件", true);
            oposPrintText("打印时间： " + formatter.format(new Date()), true);

            if (pageNo > 0) {
                String barcode = Integer.toString(order.OrderNo);
                if (order.Qty > 1) {
                    barcode += "-" + Integer.toString(pageNo);
                }
                oposPrintText(LINE_FEED, false);
                posPrinter.printBarCode(
                        POSPrinterConst.PTR_S_RECEIPT,
                        barcode,
                        POSPrinterConst.PTR_BCS_Code39,
                        50, 3,
                        POSPrinterConst.PTR_BC_CENTER,
                        POSPrinterConst.PTR_BC_TEXT_BELOW
                );
            }

            oposPrintText(FORM_FEED, false);

            Thread.currentThread().sleep(2000);

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private void oposPrintText(String data, boolean lineFeed) throws JposException {
        posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, data);
        if (lineFeed) {
            posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, "\n");
        }
    }

    private void getPairedDevices() throws JposException {
        btDevices = btAdapter.getBondedDevices();
        if (btDevices.size() > 0) {
            for (BluetoothDevice device:btDevices) {
                messageView.setText(device.getName());
                if (device.getName().contains(this.printerName)) {
                    try {
                        bxlConfigLoader.removeAllEntries();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    bxlConfigLoader.addEntry(
                            this.printerName,
                            BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER,
                            device.getName(),
                            BXLConfigLoader.DEVICE_BUS_BLUETOOTH,
                            device.getAddress()
                    );
                    try {
                        bxlConfigLoader.saveFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    posPrinter.open(this.printerName);
                    posPrinter.claim(0);
                    if (posPrinter.getClaimed()) {
                        posPrinter.setDeviceEnabled(true);
                        posPrinter.setCharacterSet(POSPrinterConst.PTR_CS_UNICODE);
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "蓝牙必须开启才可继续", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void errorOccurred(ErrorEvent errorEvent) {

    }
}
