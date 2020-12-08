package com.example.ysh.pleasewakemeup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent sIntent = new Intent(context, AlarmService.class);//서시브 실행을 위한 인텐트

        String state = intent.getStringExtra("state");
        sIntent.putExtra("state", state);//받아온 엑스트라를 그대로 다시 넘겨줌.

        context.startForegroundService(sIntent);
        //오레오(>=26) 이후부터는 백그라운드 실행이 원칙적으로 금지됨.
        // -> 포그라운드에 올려 실행
    }
}
