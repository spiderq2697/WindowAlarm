package com.example.ysh.pleasewakemeup;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class CameraKit extends AppCompatActivity
{
    private static final int REQUEST_CODE = 0;

    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    //텐서플로우가 사용할 변수 + 상수들을 선언.

    private TextView textViewResult;
    private Button btnDetectObject, btnToggleCamera;
    private ImageView imageViewResult;
    //알람 작동 모드 시: 찍어야 할 (창)문을 표시.
    //테스트 모드 시: 텐서플로우에 입력된 사진과 탐지 결과를 출력.

    private CameraView cameraView;//상단에 뜰 카메라부
    private static final int Window1Mod = 1;// = 1번째 (창)문 등록 모드
    private static final int Window2Mod = 2;// = 2번째 (창)문 등록 모드
    private static final int AlarmCallMod = 0;// = 알람 작동 모드
    private static final int TestMod = 3;// = 단순 물체 탐지 모드(테스트 모드), getExtra가 잘못될 경우 기본값, 일반적으론 절대 볼일이 없음.
    private int RemainingShots = 999;//남은 (창)문의 수를 저장

    private ImageView imageViewGallery;
    private TextView textViewGallery;
    //테스트 모드에서 임의로 이미지 비교를 돌릴때 쓸 이미지뷰, 텍스트뷰.

    private ImageView imageViewCheck;
    //창문 등록 모드에서 사용, 창문이 인식되서 저장까지 완료되면 그 등록된 창문을 표시.

    private Bitmap b1;//카메라로 찍은 사진을 담을 비트맵.
    private Bitmap b2;
    //알람 작동 모드: 창문1( + 창문2)을(를) (번갈아서) 담을 비트맵
    //테스트 모드: 갤러리에서 불러온 이미지를 담음.

    private final static String TAG = CameraKit.class.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_kit);

        cameraView = (CameraView) findViewById(R.id.cameraView);//카메라뷰, 1:1비율

        imageViewResult = (ImageView) findViewById(R.id.imageViewResult);
        textViewResult = (TextView) findViewById(R.id.textViewResult);
        textViewResult.setMovementMethod(new ScrollingMovementMethod());
        textViewResult.setText("For object detection.");
        //알람 작동 모드: 찍어야 할 창문 표시.
        //테스트 모드: 방금 찍을 사진과 탐지 결과 출력.

        imageViewGallery = (ImageView) findViewById(R.id.imageViewGallery);
        textViewGallery = (TextView) findViewById(R.id.textViewGallery);
        textViewGallery.setMovementMethod(new ScrollingMovementMethod());
        textViewGallery.setText("<- Load the image first.");
        //테스트 모드 전용, 갤러리에서 불러온 이미지와 그 파일의 경로 출력.

        imageViewCheck = (ImageView) findViewById(R.id.imageViewCheck);
        //창문 등록 모드 전용, 저장 완료된 (창)문을 표시

        btnToggleCamera = (Button) findViewById(R.id.btnToggleCamera);//카메라 토글
        btnDetectObject = (Button) findViewById(R.id.btnDetectObject);//사진 찍기

        final Intent intent = getIntent();
        final int cameraMod = intent.getIntExtra("CameraMod", TestMod);
        //셋팅 익티비티에서 넘어오는 엑스트라를 받음.
        //엑스트라를 어떤 이유에서든 받질 못하면 카메라킷은 테스트 모드로 설정됨.
        //알람이 정상 작동되면 앱 사용자들은 테스트 모드를 볼 일이 없음.

        if(cameraMod == Window1Mod || cameraMod == Window2Mod)
        {
            imageViewGallery.setVisibility(View.INVISIBLE);
            textViewGallery.setVisibility(View.INVISIBLE);
            imageViewResult.setVisibility(View.INVISIBLE);
            textViewResult.setVisibility(View.INVISIBLE);
            //창문 등록 모드 시에 안쓸 뷰들을 걷어냄.
        }
        else if(cameraMod == AlarmCallMod)
        {
            imageViewGallery.setVisibility(View.INVISIBLE);
            textViewGallery.setVisibility(View.INVISIBLE);
            imageViewCheck.setVisibility(View.INVISIBLE);
            //알람 작동 모드 시에 안쓸 뷰들을 걷어냄.

            File WindowFolder = new File(getExternalFilesDir(null) + "/WindowAlarm");
            File Window1 = new File(WindowFolder, "Window1.jpg");
            File Window2 = new File(WindowFolder, "Window2.jpg");
            //창문들을 미리 불러와서 준비.

            if(Window1.exists())
            {
                b2 = BitmapFactory.decodeFile(getExternalFilesDir(null)
                        + "/WindowAlarm/Window1.jpg");
                imageViewResult.setImageBitmap(b2);
                textViewResult.setText("<- 1번째 (창)문을 찍어주세요.");
                //1번째 창문을 설정하고 출력.

                if(Window2.exists())
                {
                    RemainingShots = 2;
                    //2번째 창문도 있으면, 남은 창문 수: 2.
                }
                else
                {
                    RemainingShots = 1;
                    //남은 창문 수: 1.
                }
            }
            else
            {
                Toast.makeText(getApplicationContext(), "1번째 (창)문을 먼저 등록하세요.", Toast.LENGTH_LONG).show();

                Intent TheEnd = new Intent(CameraKit.this, AlarmReceiver.class);
                TheEnd.putExtra("state","off");
                sendBroadcast(TheEnd);

                finish();
                //1번째 창문을 불러오지 못하면 토스트를 띄우고 알람 종료.
            }
        }
        else
        {
            imageViewCheck.setVisibility(View.INVISIBLE);
            //테스트 모드 시 안쓸 뷰를 걷어냄.
        }

        Toast.makeText(getApplicationContext(), "사진은 보이는 칸에 맞춰 바르게 찍어주세요.", Toast.LENGTH_LONG).show();
        // = 카메라킷 준비 완료.

        cameraView.addCameraKitListener(new CameraKitEventListener()
        {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent)
            {

            }

            @Override
            public void onError(CameraKitError cameraKitError)
            {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage)
            {
                b1 = cameraKitImage.getBitmap();
                // = 카메라로 찍은 사진의 원본

                //사진을 1:1비율로 편집하기 위한 작업들.
                int x1 = 0;
                if(b1.getHeight() >= b1.getWidth())
                {
                    x1 = 0;
                }
                else
                {
                    x1 = ((b1.getWidth()) - (b1.getHeight())) / 2;
                }
                //사진을 자르기 시작할 지점의 x좌표를 구함.

                int y1 = 0;
                if(b1.getHeight() >= b1.getWidth())
                {
                    y1 = ((b1.getHeight()) - (b1.getWidth())) / 2;
                }
                else
                {
                    y1 = 0;
                }
                //사진을 자르기 시작할 지점의 y좌표를 구함.

                int l1 = 0;
                if(b1.getHeight() >= b1.getWidth())
                {
                    l1 = b1.getWidth();
                }
                else
                {
                    l1 = b1.getHeight();
                }
                //찍은 사진의 해상도를 봐서 더 적은 쪽으로 길이를 설정.

                Mat image_original = new Mat();
                Utils.bitmapToMat(b1, image_original);
                Rect rectCrop = new Rect(x1, y1, l1, l1);
                Mat image_output = new Mat(image_original, rectCrop);
                Bitmap croppedBitmap = Bitmap.createBitmap(image_output.cols(), image_output.rows(), null);
                Utils.matToBitmap(image_output, croppedBitmap);
                b1 = croppedBitmap;
                //사진을 1:1비율로 자르는 작업 수행.
                //기기마다 화소 수가 다르므로
                //이 단계에서 얻는 이미지의 해상도는 기기마다 다 다름.

                Bitmap bitmap = b1;
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                //텐서플로우에 넘겨줄 바트맵을 선언 후
                //특정 사이즈로 가공 하고, 물체 탐지 결과를 받아옴.

                b1 = Bitmap.createScaledBitmap(b1, 1000, 1000, true);
                //원활한 이미지 비교를 위해 해상도를 임의로 변경.
                //여기서는 1,000 X 1,000 으로 지정.

                if(cameraMod == TestMod)
                {
                    imageViewResult.setImageBitmap(b1);
                    textViewResult.setText(results.toString());
                    //테스트 모드 시에는 찍은 사진과 탐지 결과를 바로 보여줌.
                }
                else
                {
                    String s1 = "";//탐지 결과를 문자열로 받을 변수 선언.

                    //루프 선언.
                    //탐지 결과를 순차적으로 받아와 비교함.
                    //각 모드마다 해당하는 창문을 탐지 하면 루프를 빠져 나옴.
                    DetectLoop:
                    for(int i = 0; i < results.size(); i++)
                    {
                        s1 = results.get(i).toString();
                        //탐지 결과를 문자열로 받음.

                        if(s1.contains("window") || s1.contains("door"))//그 결과에 '창문'이나 '문'이 있다면...
                        {
                            if(cameraMod == AlarmCallMod)//알람 작동 모드 시...
                            {
                                int r1 = compareHistogram(b1, b2);//이미지 비교를 수행.
                                if(r1 > 0)// = 두 이미지가 일치함.
                                {
                                    if(RemainingShots == 1)//남은 창문이 1개 였다면...
                                    {
                                        Toast.makeText(getApplicationContext(), "등록된 (창)문이 모두 찍혔습니다."
                                                , Toast.LENGTH_SHORT).show();

                                        Intent TheEnd = new Intent(CameraKit.this, AlarmReceiver.class);
                                        TheEnd.putExtra("state","off");
                                        sendBroadcast(TheEnd);

                                        finish();
                                        // = 알람 종료
                                    }
                                    else if(RemainingShots == 2)//남은 창문이 '2개' 였다면...
                                    {
                                        Toast.makeText(getApplicationContext(), "1번째 (창)문 완료, 2번째를 찍어주세요."
                                                , Toast.LENGTH_SHORT).show();
                                        b2 = BitmapFactory.decodeFile(getExternalFilesDir(null)
                                                + "/WindowAlarm/Window2.jpg");
                                        imageViewResult.setImageBitmap(b2);
                                        textViewResult.setText("<- 2번째 (창)문을 찍어주세요.");
                                        RemainingShots = 1;
                                        break DetectLoop;
                                        //다음 창문을 설정하고 루프를 빠져나옴.
                                    }
                                }
                                else
                                {
                                    Toast.makeText(getApplicationContext(), "(창)문을 열고 찍어주세요."
                                            , Toast.LENGTH_SHORT).show();
                                    // = 등록된 창문이 아님.
                                }
                            }
                            else// = 창문 등록 모드.
                            {
                                File folder = new File(getExternalFilesDir(null) + "/WindowAlarm");
                                if(!folder.exists())
                                {
                                    folder.mkdir();
                                }
                                //등록할 창문들을 넣을 폴더를 찾거나 만듬.

                                if(cameraMod == Window1Mod)// = 1번째 창문 등록...
                                {
                                    String filename = "Window1.jpg";
                                    //창문1의 이름: Window1.jpg

                                    File file = new File(folder, filename);
                                    if(file.exists())
                                    {
                                        file.delete();
                                    }
                                    //이미 해당 파일이 있다면, 기존 거는 제거를 먼저 함.

                                    try
                                    {
                                        FileOutputStream out = new FileOutputStream(file);
                                        b1.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                        out.flush();
                                        out.close();
                                        //jpg로 창문을 저장.
                                    }
                                    catch (IOException e)
                                    {
                                        e.printStackTrace();
                                    }
                                    imageViewCheck.setImageBitmap(b1);
                                    Toast.makeText(getApplicationContext(), "1번째 (창)문 저장 완료, 나가셔도 됩니다."
                                            , Toast.LENGTH_LONG).show();
                                    break DetectLoop;
                                    //저장한 창문을 보여주고 루프를 나옴.
                                }
                                else if(cameraMod == Window2Mod)
                                {
                                    String filename = "Window2.jpg";

                                    File file = new File(folder, filename);
                                    if(file.exists())
                                    {
                                        file.delete();
                                    }

                                    try
                                    {
                                        FileOutputStream out = new FileOutputStream(file);
                                        b1.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                        out.flush();
                                        out.close();
                                    }
                                    catch (IOException e)
                                    {
                                        e.printStackTrace();
                                    }
                                    imageViewCheck.setImageBitmap(b1);
                                    Toast.makeText(getApplicationContext(), "2번째 (창)문 저장 완료, 나가셔도 됩니다."
                                            , Toast.LENGTH_LONG).show();
                                    break DetectLoop;
                                    // ''
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo)
            {

            }
        });

        imageViewGallery.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, REQUEST_CODE);
                //테스트 모드 전용, 이 이미지 뷰를 터치 하면 갤러리에서 사진을 가져오는 작업 수행.
            }
        });
        textViewGallery.setOnClickListener(new View.OnClickListener()
        {//테스트 모드 전용, 이 텍스트 뷰를 터치하면 갤러리 이미지와 카메라 이미지를 가지고
            //임의로 이미지 비교를 수행함.
            @Override
            public void onClick(View v)
            {
                if(textViewGallery.getText().toString().equals("<- Load the image first."))
                {
                    Toast.makeText(getApplicationContext(), "이미지를 먼저 불러와 주세요.", Toast.LENGTH_SHORT).show();
                    //갤러리에서 이미지를 불러오지 않았다면 아무것도 안함.
                }
                else
                {
                    cameraView.captureImage();// = 카메라 이미지 설정.

                    b2 = Bitmap.createScaledBitmap(b2, 1000, 1000, true);
                    int ret = compareHistogram(b1, b2);
                    //갤러리에서 가져온 이미지의 해상도 카매라 이미지와 같게 하고
                    //이미지 비교를 실행.

                    textViewGallery.setText("retVal: " + ret);
                    //그 결과를 표시.
                }
            }
        });

        btnToggleCamera.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //카메라 토글
                cameraView.toggleFacing();
            }
        });

        btnDetectObject.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //각 모드에 따라 사진을 찍고 작업 수행.
                cameraView.captureImage();
            }
        });

        initTensorFlowAndLoadModel();
        //텐서플로우 설정 + 작동 준비.
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {//OpenCV 로딩.
        @Override
        public void onManagerConnected(int status)
        {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onResume()
    {
        super.onResume();
        cameraView.start();

        if (!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
        else
        {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause()
    {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel()
    {//텐서플로우 준비 작업.
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                    makeButtonVisible();
                }
                catch (final Exception e)
                {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void makeButtonVisible()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                btnDetectObject.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {//테스트 모드 전용, 갤러리에서 고른 이미지를 보여주고, 그 파일의 경로를 표시.
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE)
        {
            if (resultCode == RESULT_OK)
            {
                Uri selectedImageUri = data.getData();
                String realPath = getRealPath(selectedImageUri);

                try
                {
                    b2 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                imageViewGallery.setImageBitmap(b2);
                textViewGallery.setText(realPath);
            }
        }
    }

    protected String getRealPath(Uri uri)
    {//테스트 모드 전용, 그 파일의 절대 경로를 알아냄.
        Cursor cursor = getContentResolver().
                query(uri, null, null, null, null );

        cursor.moveToNext();

        String path = cursor.getString( cursor.getColumnIndex( "_data" ) );

        cursor.close();

        return path;
    }

    protected static int compareHistogram(Bitmap CameraPhoto, Bitmap GalleryPhoto)
    {//두 이미지를 비교해서 어느정도 일차하면 1, 아니면 0을 반환, 방식은 히스토그램.
        int retVal = 0;

        Mat img1 = new Mat();
        Utils.bitmapToMat(CameraPhoto, img1);
        Mat img2 = new Mat();
        Utils.bitmapToMat(GalleryPhoto, img2);

        Mat hsvImg1 = new Mat();
        Mat hsvImg2 = new Mat();

        Imgproc.cvtColor(img1, hsvImg1, Imgproc.COLOR_BGR2HSV);
        Imgproc.cvtColor(img2, hsvImg2, Imgproc.COLOR_BGR2HSV);

        List<Mat> listImg1 = new ArrayList<Mat>();
        List<Mat> listImg2 = new ArrayList<Mat>();

        listImg1.add(hsvImg1);
        listImg2.add(hsvImg2);

        MatOfFloat ranges = new MatOfFloat(0,255);
        MatOfInt histSize = new MatOfInt(50);
        MatOfInt channels = new MatOfInt(0);

        Mat histImg1 = new Mat();
        Mat histImg2 = new Mat();

        Imgproc.calcHist(listImg1, channels, new Mat(), histImg1, histSize, ranges);
        Imgproc.calcHist(listImg2, channels, new Mat(), histImg2, histSize, ranges);

        Core.normalize(histImg1, histImg1, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        Core.normalize(histImg2, histImg2, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        double result0, result1, result2, result3;
        result0 = Imgproc.compareHist(histImg1, histImg2, 0);
        result1 = Imgproc.compareHist(histImg1, histImg2, 1);
        result2 = Imgproc.compareHist(histImg1, histImg2, 2);
        result3 = Imgproc.compareHist(histImg1, histImg2, 3);

        int count=0;

        if (result0 > 0.9)
        {
            count++;
        }
        if (result1 < 0.1)
        {
            count++;
        }
        if (result2 > 1.5)
        {
            count++;
        }
        if (result3 < 0.3)
        {
            count++;
        }

        if (count >= 3) retVal = 1;

        return retVal;
    }
}
