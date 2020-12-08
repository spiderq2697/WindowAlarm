package com.example.ysh.pleasewakemeup;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class SettingActivity extends AppCompatActivity
{
    private AlarmManager alarmManager;
    private TimePicker timePicker;
    private PendingIntent pendingIntent;
    //알람을 걸기 위한 변수들.

    private boolean daily = false;
    //알람을 매일 반복해서 틀지를 결정할 부울 변수

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        this.alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        this.timePicker = findViewById(R.id.timePicker);

        File WindowFolder = new File(getExternalFilesDir(null) + "/WindowAlarm");
        File Window1 = new File(WindowFolder, "Window1.jpg");
        File Window2 = new File(WindowFolder, "Window2.jpg");
        //이미 등록한 (창)문들이 있는지 미리 확인하기 위한 파일 변수들

        Button StartButton = findViewById(R.id.btnStart);//알람을 거는 버튼
        Button StopButton = findViewById(R.id.btnStop);//걸린 앙람을 취소하는 버튼

        Button window1 = findViewById(R.id.window1);//1번째 창문을 등록하는 버튼
        Button window2 = findViewById(R.id.window2);//2번째 창문을 등록하는 버튼
        Button remove1 = findViewById(R.id.Rwindow1);//1번째 창문을 제거하는 버튼
        Button remove2 = findViewById(R.id.Rwindow2);//2번째 창문을 제거하는 버튼

        Switch DailyRepeat = findViewById(R.id.DailyRepeat);//매일 반복 여부 결정 스위치

        StartButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                start();
                //원하는 시간에 알람을 거는 작업 시작.
            }
        });

        StopButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                stop();
                //알람이 설정된 경우 취소작업 시작.
            }
        });

        window1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(SettingActivity.this, CameraKit.class);
                intent.putExtra("CameraMod", 1);
                startActivity(intent);
                //카메라킷을 '창문1 등록'모드로 설정하고 액티비티 시작
            }
        });

        window2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(Window1.exists())
                {
                    Intent intent = new Intent(SettingActivity.this, CameraKit.class);
                    intent.putExtra("CameraMod", 2);
                    startActivity(intent);
                    //카메라킷을 '창문2 등록'모드로 설정하고 액티비티 시작.
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "1번째 (창)문을 먼저 등록하세요."
                            , Toast.LENGTH_LONG).show();
                    //1번째 창문이 없는 경우엔 아무것도 안함.
                }
            }
        });

        remove1.setOnClickListener(new View.OnClickListener()
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
                                if(Window1.exists())
                                {
                                    Window1.delete();
                                    Toast.makeText(getApplicationContext(), "창문1 삭제."
                                            , Toast.LENGTH_SHORT).show();
                                    //삭제 버튼을 눌렀을 때
                                    //1번째 창문이 있으면 그 파일을 제거하고
                                    //토스트를 띄움.
                                }
                                else
                                {
                                    Toast.makeText(getApplicationContext(), "창문1이 없습니다..."
                                            , Toast.LENGTH_SHORT).show();
                                    //1번째 창문이 없는데 지우려 하면 아무것도 안하고 빠져 나옴.
                                }
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Toast.makeText(getApplicationContext(), "창문1 삭제 취소."
                                        , Toast.LENGTH_SHORT).show();
                                //취소버튼을 눌러도 아무것도 안하고 나옴.
                            }
                        });
                AlertDialog msgDlg = msgBuilder.create();
                msgDlg.show();
            }
        });

        remove2.setOnClickListener(new View.OnClickListener()
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
                                if(Window2.exists())
                                {
                                    Window2.delete();
                                    Toast.makeText(getApplicationContext(), "창문2 삭제."
                                            , Toast.LENGTH_SHORT).show();
                                }
                                else
                                {
                                    Toast.makeText(getApplicationContext(), "창문2가 없습니다..."
                                            , Toast.LENGTH_SHORT).show();
                                }
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

        DailyRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if(isChecked)
                {
                    daily = true;// = 알람이 매일 울림.
                }
                else
                {
                    daily = false;// = 알림이 1번만 울림.
                }
            }
        });
    }

    /* 알람 시작 */
    private void start()
    {
        // 시간 설정
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, this.timePicker.getHour());
        calendar.set(Calendar.MINUTE, this.timePicker.getMinute());
        calendar.set(Calendar.SECOND, 0);

        // 현재시간보다 이전이면
        if (calendar.before(Calendar.getInstance()))
        {
            // 다음날로 설정
            calendar.add(Calendar.DATE, 1);
        }

        // Receiver 설정
        Intent intent = new Intent(this, AlarmReceiver.class);
        // state 값이 on 이면 알람시작, off 이면 중지
        intent.putExtra("state", "on");

        this.pendingIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // 알람 설정
        if(daily == true)
        {
            this.alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis()
                    , AlarmManager.INTERVAL_DAY, pendingIntent);
            // Toast 보여주기 (알람 시간 표시)
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Toast.makeText(this, "알람 설정됨: 매일 " + format.format(calendar.getTime()), Toast.LENGTH_SHORT).show();
        }
        else
        {
            this.alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            // Toast 보여주기 (알람 시간 표시)
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Toast.makeText(this, "알람 설정됨: " + format.format(calendar.getTime()), Toast.LENGTH_SHORT).show();
        }
    }

    /* 알람 중지 */
    private void stop()
    {
        if (this.pendingIntent == null)
        {
            Toast.makeText(this, "설정한 알람이 없습니다."
                    , Toast.LENGTH_SHORT).show();
            return;
        }

        // 알람 취소
        this.alarmManager.cancel(this.pendingIntent);
        this.pendingIntent = null;
        Toast.makeText(this, "알람 삭제됨"
                , Toast.LENGTH_SHORT).show();
    }
}
