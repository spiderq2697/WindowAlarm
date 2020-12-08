package com.example.ysh.pleasewakemeup;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;


public class AlarmService extends Service
{
    private MediaPlayer mediaPlayer;//음악 재생 시에 씀
    private boolean isRunning;//켜고 끄는 역활을 할 일종의 스위치

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // Oreo(26) 버전 이후 버전부터는 channel 이 필요함
        String channelId = createNotificationChannel();

        String state = intent.getStringExtra("state");
        //알람 리시버에서 받아온 "상태"엑스트라를 불러옴

        if (!this.isRunning && state.equals("on"))// = 켜는 신호일 경우...
        {
            Intent CameraCall = new Intent(this, CameraKit.class);
            CameraCall.putExtra("CameraMod", 0);
            CameraCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //카메라킷을 부르기 위한 인텐트
            //엑스트라로 주는 "카메라모드": 0 은...
            //...이번 카메라킷은 '알람 작동 모드'로 돌아가야함을 의미
            //플래그를 세워 새 액티비티를 돌려야 한다는 설정도 추가.

            PendingIntent CameraCallPending = PendingIntent.getActivity(this, 0,
                    CameraCall, PendingIntent.FLAG_UPDATE_CURRENT);
            //알림창에 띄울때 쓸 페딩 인텐트 선언.

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channelId)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("WindowAlarm")
                            .setContentText("알람이 울리고 있습니다...")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setCategory(NotificationCompat.CATEGORY_SERVICE)
                            .setAutoCancel(true)
                            .setFullScreenIntent(CameraCallPending, true);

            notificationBuilder.setContentIntent(CameraCallPending);
            Notification incomingCallNotification = notificationBuilder.build();
            startForeground(1, incomingCallNotification);
            //알림창을 설정하는 부분.
            //중요도를 높게 줘서 헤드업 알림이 울리도록 설정.
            //오토갠슬을 설정해 알람 해제 완료 시 알림이 제거되도록 설정.
            //풀스크린인텐트를 이용하여 알림을 터치해 들어갈 경우( OR 화면 잠금을 풀 경우)...
            //...전체화면으로 카메라킷이 돌아가게 설정.

            this.mediaPlayer = MediaPlayer.create(this, R.raw.ouu);
            this.mediaPlayer.start();

            this.isRunning = true;
            //을막을 절정하고 알람이 울리면 재생 시작.
        }
        else if (this.isRunning && state.equals("off"))// = 끄는 신호일 경우...
        {
            this.mediaPlayer.stop();
            this.mediaPlayer.reset();

            this.isRunning = false;

            this.stopSelf();
            //음악을 끄고, 서비스를 종료 시킴.
        }
        return START_NOT_STICKY;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel()
    {
        String channelId = "Alarm";
        String channelName = getString(R.string.app_name);
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        return channelId;
    }
}

