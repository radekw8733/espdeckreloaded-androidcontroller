package net.radekw8733.espdeckreloaded;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ConfiguratorActivity extends AppCompatActivity {
    private WifiManager wifi;
    private TextView statusText;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurator);
        context = getApplicationContext();
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        statusText = findViewById(R.id.statusText);
        statusText.setText(R.string.configurator_statusText_searching);

        if (ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        else {
            scanWifiForEspdeck();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanWifiForEspdeck();
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ConfiguratorActivity.this);
                    builder
                            .setMessage(R.string.configurator_permRequestFailRationaleDialogMessage)
                            .setPositiveButton(R.string.configurator_permRequestFailRequestAgain, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ActivityCompat.requestPermissions(ConfiguratorActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                                }})
                            .setTitle(R.string.configurator_permRequestFailRationaleDialogTitle)
                            .setCancelable(false)
                            .show();
                }
                break;
        }
    }

    private void scanWifiForEspdeck() {
        BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<ScanResult> wifiList = wifi.getScanResults();
                Log.d("Configurator",wifi.getScanResults().toString());
                for (ScanResult wifiEntry : wifiList) {
                    if (wifiEntry.SSID.equals("Espdeck Reloaded")) {
                        Log.d("Configurator","ESPDECK WIFI FOUND!");
                        statusText.setText(R.string.configurator_statusText_wifiFound);
                        getApplicationContext().unregisterReceiver(this);
                        connectToEspdeckWifi();
                        return;
                    }
                }
                statusText.setText(R.string.configurator_statusText_stillSearching);
                wifi.startScan();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getApplicationContext().registerReceiver(wifiReceiver,filter);
        wifi.startScan();
    }

    private void connectToEspdeckWifi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            WifiNetworkSpecifier espdeckWifiSpecifier = new WifiNetworkSpecifier.Builder().setSsid("Espdeck Reloaded").build();
            NetworkRequest espdeckWifiRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(espdeckWifiSpecifier)
                    .build();
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(espdeckWifiRequest,new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    connectivityManager.bindProcessToNetwork(network);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText(R.string.configurator_statusText_configuring);
                        }
                    });
                    configureEspdeck();
                }
                @Override
                public void onLost(Network network) {
                    connectivityManager.bindProcessToNetwork(null);
                    connectivityManager.unregisterNetworkCallback(this);
                }
            });
        }
        else {
            WifiConfiguration espdeckWifi = new WifiConfiguration();
            espdeckWifi.SSID = "\"" + "Espdeck Reloaded" + "\"";
            espdeckWifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifi.addNetwork(espdeckWifi);
            List<WifiConfiguration> list = wifi.getConfiguredNetworks();
            for( WifiConfiguration wifiNetwork : list ) {
                if(wifiNetwork.SSID != null && wifiNetwork.SSID.equals("\"" + "Espdeck Reloaded" + "\"")) {
                    Log.d("Configurator","Connecting to Espdeck wifi now");
                    wifi.disconnect();
                    wifi.enableNetwork(wifiNetwork.networkId, true);
                    configureEspdeck();
                    break;
                }
            }
        }
    }

    private void configureEspdeck() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ConfiguratorActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.configurator_wifisetup,null);
        Switch dhcpSwitch = dialogView.findViewById(R.id.dhcpSwitch);
        dhcpSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                EditText editText1 = dialogView.findViewById(R.id.staticIp);
                EditText editText2 = dialogView.findViewById(R.id.defaultGateway);
                EditText editText3 = dialogView.findViewById(R.id.networkMask);
                if (b) {
                    editText1.setVisibility(View.VISIBLE);
                    editText2.setVisibility(View.VISIBLE);
                    editText3.setVisibility(View.VISIBLE);
                }
                else {
                    editText1.setVisibility(View.GONE);
                    editText2.setVisibility(View.GONE);
                    editText3.setVisibility(View.GONE);
                }
            }
        });
        builder
                .setView(dialogView)
                .setTitle(R.string.configurator_wifiDialogTitle)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        EditText wifiSSIDtext = dialogView.findViewById(R.id.wifiSSID);
                        EditText wifiPasswordtext = dialogView.findViewById(R.id.wifiPassword);
                        String wifiSSID = wifiSSIDtext.getText().toString();
                        String wifiPassword = wifiPasswordtext.getText().toString();
                        OkHttpClient client = new OkHttpClient();
                        String url = HttpUrl.parse("http://192.168.4.1/setup").newBuilder().addQueryParameter("wifiSSID",wifiSSID).addQueryParameter("wifiPASS",wifiPassword).build().toString();
                        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                List<WifiConfiguration> list = wifi.getConfiguredNetworks();
                                for (WifiConfiguration wifiNetwork : list) {
                                    if (wifiNetwork.SSID.equals("\"" + "Espdeck Reloaded" + "\"")) {
                                        wifi.removeNetwork(wifiNetwork.networkId);
                                    }
                                }
                            }
                        });
                    }})
                .show();
    }
}