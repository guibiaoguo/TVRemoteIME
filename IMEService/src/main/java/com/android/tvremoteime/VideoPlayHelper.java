package com.android.tvremoteime;

import android.content.Context;

import java.net.URLDecoder;

//import player.XLVideoPlayActivity;
//import xllib.DownloadManager;

/**
 * Created by kingt on 2018/2/22.
 */

public class VideoPlayHelper {
    public static void playUrl(Context context, String url, int videoIndex, boolean useSystem){
        if (url!=null){
            try {
                url = URLDecoder.decode(url, "utf-8");
            }catch (Exception e){
                e.printStackTrace();
            }

        }
//        if(useSystem) {
//            //外部播放
//            DownloadManager downloadManager = DownloadManager.instance();
//            downloadManager.init(context);
//            downloadManager.taskInstance().setUrl(url);
//            if(videoIndex > 0)downloadManager.taskInstance().changePlayItem(videoIndex);
//            downloadManager.taskInstance().startTask();
//            String playUrl = downloadManager.taskInstance().getPlayUrl();
//            if(!TextUtils.isEmpty(playUrl)) {
//                if(Environment.needDebug){
//                    Environment.debug(IMEService.TAG, "外部调用播放视频, playUrl=" + playUrl + ", video/*");
//                }
//
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.parse(playUrl), "video/*");
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(intent);
//            }
//        }else {
//            //内部播放
//            XLVideoPlayActivity.intentTo(XLVideoPlayActivity.class, context, url, url, videoIndex);
//        }
    }
}
