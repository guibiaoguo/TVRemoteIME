package com.android.tvremoteime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tvremoteime.adb.AdbHelper;
import com.android.tvremoteime.server.RemoteServer;

public class MainActivity extends Activity implements View.OnClickListener {

    private ImageView qrCodeImage;
    private TextView addressView;
    private EditText dlnaNameText;
    private EditText ftpUserText;
    private EditText ftpPasswordText;
    private EditText ftpPortText;
    private EditText ftpHomeText;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };


    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        verifyStoragePermissions(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qrCodeImage = this.findViewById(R.id.ivQRCode);
        addressView = this.findViewById(R.id.tvAddress);
        dlnaNameText = this.findViewById(R.id.etDLNAName);
        ftpUserText = this.findViewById(R.id.etFtpName);
        ftpPasswordText = this.findViewById(R.id.etFtpPassword);
        ftpPortText = this.findViewById(R.id.etFtpPort);
        ftpHomeText = this.findViewById(R.id.etFtpHome);
        this.setTitle(this.getResources().getString( R.string.app_name) + "  V" + AppPackagesHelper.getCurrentPackageVersion(this));
//        dlnaNameText.setText(DLNAUtils.getDLNANameSuffix(this.getApplicationContext()));
        ftpUserText.setText("anonymous");
        ftpPortText.setText("2121");
        ftpHomeText.setText(android.os.Environment.getExternalStorageDirectory().getPath());
        refreshQRCode();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnUseIME:
                openInputMethodSettings();
                if(Environment.isEnableIME(this)){
                    Environment.toast(getApplicationContext(), "太棒了，您已经激活启用了" + getString(R.string.keyboard_name) +"输入法！");
                }
                break;
            case R.id.btnSetIME:
                if(!Environment.isEnableIME(this)) {
                    Environment.toast(getApplicationContext(), "抱歉，请您先激活启用" + getString(R.string.keyboard_name) +"输入法！");
                    openInputMethodSettings();
                    if(!Environment.isEnableIME(this)) return;
                }
                try {
                    ((InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
                }catch (Exception ignored) {
                    Environment.toast(getApplicationContext(), "抱歉，无法设置为系统默认输入法，请手动启动服务！");
                }
                if(Environment.isDefaultIME(this)){
                    Environment.toast(getApplicationContext(), "太棒了，" + getString(R.string.keyboard_name) +"已是系统默认输入法！");
                }
                break;
            case R.id.btnStartService:
                startService(new Intent(IMEService.ACTION));
                if(!Environment.isDefaultIME(this)) {
                    if (AdbHelper.getInstance() == null) AdbHelper.createInstance();
                }
                Environment.toast(getApplicationContext(), "服务已手动启动，稍后可尝试访问控制端页面");
                break;
            case R.id.btnSetDLNA:
//                DLNAUtils.setDLNANameSuffix(this.getApplicationContext(), dlnaNameText.getText().toString());
                break;
            case R.id.btnSetFtp:
                FtpUtils.setFtpConfig(this.getApplicationContext(),ftpUserText.getText().toString(),ftpPasswordText.getText().toString(),ftpPortText.getText().toString(),ftpHomeText.getText().toString());
                break;
        }
        refreshQRCode();
    }
    private void openInputMethodSettings(){
        try {
            this.startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
        }catch (Exception ignored){
            Environment.toast(getApplicationContext(), "抱歉，无法激活启用输入法，请手动启动服务！");
        }
    }
    private void refreshQRCode(){
        String address = RemoteServer.getServerAddress(this);
        addressView.setText(address);
        qrCodeImage.setImageBitmap(QRCodeGen.generateBitmap(address, 150, 150));
    }




}
