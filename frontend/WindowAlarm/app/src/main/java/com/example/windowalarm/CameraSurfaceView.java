package com.example.windowalarm;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
    SurfaceHolder holder;
    Camera camera = null;

    public CameraSurfaceView(Context context) {
        super(context);

        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    private void init(Context context){
        holder = getHolder();
        holder.addCallback(this);
    }

    @Override //메모리 만들어질 때
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        camera = Camera.open();

        try {
            camera.setPreviewDisplay(holder);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override // 크기가 변경될 때
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        camera.startPreview();
    }

    @Override // 없어질 때
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        camera.stopPreview();
        camera.release(); //리소스 삭제(꼭 필요)
        camera = null;
    }

    public boolean capture(Camera.PictureCallback callback){
        if(camera != null){
            camera.takePicture(null, null, callback);
            return true;
        }
        else{
            return false;
        }
    }
}
