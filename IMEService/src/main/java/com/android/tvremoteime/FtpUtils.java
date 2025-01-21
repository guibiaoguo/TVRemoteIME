package com.android.tvremoteime;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.tvremoteime.server.SimpleFtpServer;

import org.apache.ftpserver.usermanager.impl.BaseUser;

public class FtpUtils {

    private static SimpleFtpServer simpleFtpServer;

    public static void setFtpConfig(Context context, String user,String password,String port,String home){
        SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences("ftp_settings", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("ftpUser",user);
        editor.putString("ftpPassword",password);
        editor.putString("port",port);
        editor.putString("ftpHome",home);
        editor.commit();
        stopFtpService();
        startFtpService(context);
    }

    public static void startFtpService(final Context context){
        try {
            SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences("ftp_settings", 0);
            String userName = sharedPreferences.getString("ftpUser", "anonymous");
            String password = sharedPreferences.getString("ftpPassword", "");
            String home = sharedPreferences.getString("ftpHome", android.os.Environment.getExternalStorageDirectory().getPath());
            int port = sharedPreferences.getInt("ftpPort", 2121);
            simpleFtpServer = SimpleFtpServer.create();
            if (userName.equals("anonymous")) {
                simpleFtpServer.setPort(port).addAnonymous(home).start();
            } else {
                BaseUser user = new BaseUser();
                user.setName(userName);
                user.setPassword(password);
                user.setHomeDirectory(home);
                simpleFtpServer.setPort(port).addUser(user).start();
            }
            Environment.toastInHandler(context, context.getString(R.string.app_name) + "ftp服务已启动,端口2121");
        } catch (Exception e) {
            e.printStackTrace();
            Environment.toastInHandler(context, context.getString(R.string.app_name) + "ftp服务启动失败"+e.getMessage());
        }
    }

    public static void stopFtpService() {
        if (simpleFtpServer != null) {
            simpleFtpServer.stop();
        }
    }
}
