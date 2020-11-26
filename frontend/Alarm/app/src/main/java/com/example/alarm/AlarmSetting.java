package com.example.alarm;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;

public class AlarmSetting extends AppCompatActivity {
    AlarmManager alarm_manager;
    TimePicker alarm_timepicker;
    Context context;
    PendingIntent pendingIntent;
    ImageView imageView;
    File file;
//    CameraSurfaceView surfaceView;

    //사진 찍기
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_setting);

        File sdcard = Environment.getExternalStorageDirectory();
        file = new File(sdcard, "capture.jpg");

//        imageView = (ImageView) findViewById(R.id.imageView);
//        surfaceView = (CameraSurfaceView) findViewById(R.id.surfaceview);

//        Button picture = (Button) findViewById(R.id.picture);
//        picture.setOnClickListener(new View.OnClickListener(){
//
//            @Override
//            public void onClick(View v) {
//                capture();
//            }
//        });



        this.context = this;

        // 알람매니저 설정
        alarm_manager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // 타임피커 설정
        alarm_timepicker = findViewById(R.id.time_picker);

        // Calendar 객체 생성
        final Calendar calendar = Calendar.getInstance();

        // 알람리시버 intent 생성
        final Intent my_intent = new Intent(this.context, Alarm_Receiver.class);

        // 알람 시작 버튼
        Button alarm_on = findViewById(R.id.btn_start);

        alarm_on.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                // calendar에 시간 셋팅
                calendar.set(Calendar.HOUR_OF_DAY, alarm_timepicker.getHour());
                calendar.set(Calendar.MINUTE, alarm_timepicker.getMinute());

                // 시간 가져옴
                int hour = alarm_timepicker.getHour();
                int minute = alarm_timepicker.getMinute();
                Toast.makeText(AlarmSetting.this,"Alarm 예정 " + hour + "시 " + minute + "분",Toast.LENGTH_SHORT).show();

                // reveiver에 string 값 넘겨주기
                my_intent.putExtra("state","alarm on");

                pendingIntent = PendingIntent.getBroadcast(AlarmSetting.this, 0, my_intent, PendingIntent.FLAG_UPDATE_CURRENT);

                // 알람셋팅
                alarm_manager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        pendingIntent);
            }
        });

        Button button1;
        button1 = (Button)findViewById(R.id.window1);
        button1.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Intent intent = new Intent(AlarmSetting.this, CameraKit.class);
                startActivity(intent);
            }
        });

        Button button2;
        button2 = (Button)findViewById(R.id.window2);
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Intent intent = new Intent(AlarmSetting.this, CameraKit.class);
                startActivity(intent);
            }
        });

        // 알람 정지 버튼
//        Button alarm_off = findViewById(R.id.btn_finish);
//        alarm_off.setOnClickListener(new View.OnClickListener(){
//
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(AlarmSetting.this,"Alarm 종료",Toast.LENGTH_SHORT).show();
//                // 알람매니저 취소
//                alarm_manager.cancel(pendingIntent);
//
//                my_intent.putExtra("state","alarm off");
//
//                // 알람취소
//                sendBroadcast(my_intent);
//            }
//        });


    }

    //    카메라 어플에서 찍고 사진 가져오기
//    public void capture() {
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if(Build.VERSION.SDK_INT>=24){
//            try{
//                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//                StrictMode.setVmPolicy(builder.build());
//                builder.detectFileUriExposure();
//            }catch(Exception e){}
//        }
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
//        startActivityForResult(intent, 101);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        //크기를 줄여서 로딩(픽셀제한 걸 수 있음?)
//        if(requestCode == 101 && resultCode == Activity.RESULT_OK){
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inSampleSize = 8;
//            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
//            imageView.setImageBitmap(bitmap);
//        }
//    }

//    public void capture(){
//        surfaceView.capture(new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inSampleSize = 8;
//                Bitmap bitmap = decodeByteArray(data, 0, data.length);
//
//                imageView.setImageBitmap(bitmap);
//
//                camera.startPreview();
//            }
//        });
//    }
}