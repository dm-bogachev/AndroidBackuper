package com.robowizard.backuper;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import com.robowizard.backuper.driver.UsbSerialDriver;
import com.robowizard.backuper.driver.UsbSerialProber;
import com.robowizard.backuper.driver.UsbSerialPort;
import com.robowizard.backuper.util.HexDump;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String USB_FDEL = "USB_FDEL ";
    private static final String USB_MKDIR = "USB_MKDIR ";
    private static final String USB_SAVE = "USB_SAVE ";
    private static final String USB_SAVE_OPLOG = "USB_SAVE/OPLOG ";
    private static final String USB_SAVE_ELOG = "USB_SAVE/ELOG ";
    private static final String ID = "ID ";

    private TextView statusLabel;
    private TextView consoleWindow;
    private Switch loadRobotNameSwitch;
    private EditText robotNameField;
    private Button backupButton;

    private UsbSerialPort port;
    private UsbDeviceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);

        if (availableDrivers.isEmpty()) {
            consoleWriteLine("No RS232 devices found");
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);

        connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            consoleWriteLine("No connection achieved");
            return;
        }

        statusLabel.setText("Connected");
        statusLabel.setTextColor(Color.GREEN);

        port = driver.getPorts().get(0);

        try {

            port.open(connection);
            port.setParameters(9600, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        }
        catch (IOException e) {

        }
    }

    private void initUI() {

        statusLabel = (TextView) findViewById(R.id.statusLabel);
        consoleWindow = (TextView) findViewById(R.id.consoleWindow);
        consoleWindow.setMovementMethod(new ScrollingMovementMethod());
        loadRobotNameSwitch = (Switch) findViewById(R.id.loadRobotNameSwitch);
        robotNameField = (EditText) findViewById(R.id.robotNameField);
        backupButton = (Button)findViewById(R.id.backupButton);

    }

    private void consoleWriteLine(final String msg) {

        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        final String time = sdf.format(d);

        consoleWindow.post(new Runnable() {
            public void run() {
                consoleWindow.append(time + ": " + msg + "\n");
            }
        });
    }

    public void backupButtonClicked(View view) {

        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd");
        final String date = sdf.format(d).replaceAll(":", "");
        final String robotName = robotNameField.getText().toString();

        backupButton.setEnabled(false);
        final String[] response = new String[1];

        try {
            port.purgeHwBuffers(true,true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {

            public void run() {

                try {

                    consoleWriteLine("Saving backup...");

                    String tr = USB_FDEL + robotName + "\n";
                    port.write(tr.getBytes(), 5000);
                    response[0] = readTo(">", 0);
                    consoleWriteLine(response[0]);

                    tr = USB_MKDIR + robotName + "\n";
                    port.write(tr.getBytes(), 5000);
                    response[0] = readTo(">", 0);
                    consoleWriteLine("Saving main backup");
                    consoleWriteLine(response[0]);

                    tr = USB_SAVE + robotName + "\\" + date + "\n";
                    port.write(tr   .getBytes(), 5000);
                    response[0] = readTo(">", 0);
                    consoleWriteLine("Main backup saved!");
                    consoleWriteLine(response[0]);
                    consoleWriteLine("Saving operation log");

                    tr = USB_SAVE_OPLOG + robotName + "\\" + date + "\n";
                    port.write(tr.getBytes(), 5000);
                    response[0] = readTo(">", 0);
                    consoleWriteLine("Operation log saved!");
                    consoleWriteLine(response[0]);
                    consoleWriteLine("Saving error log");

                    tr = USB_SAVE_ELOG + robotName + "\\" + date + "\n";
                    port.write(tr.getBytes(), 5000);
                    response[0] = readTo(">", 0);
                    consoleWriteLine("Error log saved!");
                    consoleWriteLine(response[0]);
                    consoleWriteLine("Backuped!");

                    backupButton.post(new Runnable() {
                        public void run() {
                            backupButton.setEnabled(true);
                        }
                    });

                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        }).start();
    }

    public void testMethod(View view) {

        try {
            port.purgeHwBuffers(true,true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {

            public void run() {
                String[] response = new String[1];
                try {

                    String tr = ID + "\n \n \n";
                    port.write(tr.getBytes(), 5000);
                    response[0] = readTo("\n", 0);
                    response[0] = readTo("\n", 0);

                    tr = " \n \n \n";
                    port.write(tr.getBytes(), 5000);
                    response[0] = response[0].replaceAll("[^a-zA-Z0-9 ]", "");
                    String[] IDName = response[0].split(" ");
                    ArrayList<String> splittedString = new ArrayList<String>();
                    for (int i = 0; i < IDName.length; i++)
                    {
                        if (IDName[i].length() !=0)
                        {
                            splittedString.add(IDName[i]);
                        }
                    }
                    String robotInfo = "";
                    for(int i = 0; i < splittedString.size(); i++)
                    {
                        if (splittedString.get(i).contains("name"))
                        {
                            robotInfo += splittedString.get(i+1) + "_";
                        }
                        if (splittedString.get(i).contains("No"))
                        {
                            robotInfo += splittedString.get(i+1);
                        }
                    }
                    final String finalRobotInfo = robotInfo;
                    loadRobotNameSwitch.post(new Runnable() {
                        public void run() {
                            if (loadRobotNameSwitch.isChecked())
                            {
                                robotNameField.setText(finalRobotInfo);
                            }
                        }
                    });

                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    };


    private void checkConnection() {
        try {
            port.purgeHwBuffers(true,true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {

            public void run() {
                String[] response = new String[1];
                try {

                    String tr = ID + "\n \n \n";
                    port.write(tr.getBytes(), 5000);
                    response[0] = readTo("\n", 0);
                    response[0] = readTo("\n", 0);

                    tr = " \n \n \n";
                    port.write(tr.getBytes(), 5000);
                    response[0] = response[0].replaceAll("[^a-zA-Z0-9 ]", "");
                    String[] IDName = response[0].split(" ");
                    ArrayList<String> splittedString = new ArrayList<String>();
                    for (int i = 0; i < IDName.length; i++)
                    {
                        if (IDName[i].length() !=0)
                        {
                            splittedString.add(IDName[i]);
                        }
                    }
                    String robotInfo = "";
                    for(int i = 0; i < splittedString.size(); i++)
                    {
                        if (splittedString.get(i).contains("name"))
                        {
                            robotInfo += splittedString.get(i+1) + "_";
                        }
                        if (splittedString.get(i).contains("No"))
                        {
                            robotInfo += splittedString.get(i+1);
                        }
                    }
                    final String finalRobotInfo = robotInfo;
                    loadRobotNameSwitch.post(new Runnable() {
                        public void run() {
                            if (loadRobotNameSwitch.isChecked())
                            {
                                robotNameField.setText(finalRobotInfo);
                            }
                        }
                    });

                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String readTo(String to, int timeout) {

        String ret = "";
        byte buffer[];
        int i = 0;
        while (true) {

            String res = "";
            buffer = new byte[100];
            try {
                port.read(buffer, 100);
                res = new String(buffer);
                ret += res;
                if (res.contains(to)) {
                    return ret;
                }

                if (i != 0 && i < timeout)
                {
                    return null;
                }

            } catch (IOException e) {

                e.printStackTrace();
                break;
            }
        }
        return null;
    }
}



