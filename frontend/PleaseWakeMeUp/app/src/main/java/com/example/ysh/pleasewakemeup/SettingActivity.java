package com.example.ysh.pleasewakemeup;


import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingActivity extends AppCompatActivity {

    private AlarmManager alarmManager;
    private TimePicker timePicker;

    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        this.alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        this.timePicker = findViewById(R.id.timePicker);

        findViewById(R.id.btnStart).setOnClickListener(mClickListener);
        findViewById(R.id.btnStop).setOnClickListener(mClickListener);

        File WindowFolder = new File(getExternalFilesDir(null) + "/WindowAlarm");
        File Window1 = new File(WindowFolder, "Window1.jpg");
        File Window2 = new File(WindowFolder, "Window2.jpg");

        Button button1 = (Button) findViewById(R.id.window1);
        Button button2 = (Button) findViewById(R.id.window2);
        Button button3 = (Button) findViewById(R.id.alarmTest);
        Button button4 = (Button) findViewById(R.id.Rwindow1);
        Button button5 = (Button) findViewById(R.id.Rwindow2);

        if(Window1.exists())
        {
            button2.setEnabled(true);
            button3.setEnabled(true);
            button4.setEnabled(true);
        }
        else
        {
            button2.setEnabled(false);
            button3.setEnabled(false);
            button4.setEnabled(false);
        }

        if(Window2.exists())
        {
            button5.setEnabled(true);
        }
        else
        {
            button5.setEnabled(false);
        }

        button1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(SettingActivity.this, CameraKit.class);
                intent.putExtra("CameraMod", 1);
                startActivity(intent);
            }
        });

        button2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(SettingActivity.this, CameraKit.class);
                intent.putExtra("CameraMod", 2);
                startActivity(intent);
            }
        });

        button3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(SettingActivity.this, CameraKit.class);
                intent.putExtra("CameraMod", 0);
                startActivity(intent);
            }
        });

        button4.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showDialog();
            }

            private void showDialog()
            {
                AlertDialog.Builder msgBuilder = new AlertDialog.Builder(SettingActivity.this)
                        .setTitle("창문1 삭제...")
                        .setMessage("창문1을 삭제 합니다.")
                        .setPositiveButton("삭제", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Window1.delete();
                                button2.setEnabled(false);
                                button3.setEnabled(false);
                                button4.setEnabled(false);
                                Toast.makeText(getApplicationContext(), "창문1 삭제."
                                        , Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Toast.makeText(getApplicationContext(), "창문1 삭제 취소."
                                        , Toast.LENGTH_SHORT).show();
                            }
                        });
                AlertDialog msgDlg = msgBuilder.create();
                msgDlg.show();
            }
        });

        button5.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showDialog();
            }

            private void showDialog()
            {
                AlertDialog.Builder msgBuilder = new AlertDialog.Builder(SettingActivity.this)
                        .setTitle("창문2 삭제...")
                        .setMessage("창문2을 삭제 합니다.")
                        .setPositiveButton("삭제", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Window2.delete();
                                button5.setEnabled(false);
                                Toast.makeText(getApplicationContext(), "창문2 삭제."
                                        , Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Toast.makeText(getApplicationContext(), "창문2 삭제 취소."
                                        , Toast.LENGTH_SHORT).show();
                            }
                        });
                AlertDialog msgDlg = msgBuilder.create();
                msgDlg.show();
            }
        });
    }

    /* 알람 시작 */
    private void start() {
        // 시간 설정
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, this.timePicker.getHour());
        calendar.set(Calendar.MINUTE, this.timePicker.getMinute());
        calendar.set(Calendar.SECOND, 0);

        // 현재시간보다 이전이면
        if (calendar.before(Calendar.getInstance())) {
            // 다음날로 설정
            calendar.add(Calendar.DATE, 1);
        }

        // Receiver 설정
        Intent intent = new Intent(this, AlarmReceiver.class);
        // state 값이 on 이면 알람시작, off 이면 중지
        intent.putExtra("state", "on");

        this.pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // 알람 설정
        this.alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        // Toast 보여주기 (알람 시간 표시)
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Toast.makeText(this, "Alarm : " + format.format(calendar.getTime()), Toast.LENGTH_SHORT).show();
    }

    /* 알람 중지 */
    private void stop() {
        if (this.pendingIntent == null) {
            return;
        }

        // 알람 취소
        this.alarmManager.cancel(this.pendingIntent);

        // 알람 중지 Broadcast
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("state","off");

        sendBroadcast(intent);

        this.pendingIntent = null;
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart:
                    // 알람 시작
                    start();

                    break;
                case R.id.btnStop:
                    // 알람 중지
                    stop();

                    break;
            }
        }
    };


    @Override
    protected void onRestart()
    {
        super.onRestart();
        recreate();
    }
}
