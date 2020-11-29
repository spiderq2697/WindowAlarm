package com.example.alarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraKit extends AppCompatActivity {

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
    //모델_파일 과 라벨_파일 은 텐서플로우 사용시 쓰는 파일의 경로, assets폴더에 해당 데이터 존재
    //위의 나머지 변수들도 텐서플로우에서 사용함, 가능하면 손대지 맙시다.

    private TextView textViewResult;
    private Button btnDetectObject, btnToggleCamera;
    private ImageView imageViewResult;
    //예제에 원래 있던 버튼과 뷰들, Detect Object버튼을 누르면
    //사진을 찍어서 탐지된 물체들의 정보가 텍스트 뷰쪽에 뜸
    //이미지 뷰에 뜨는건 텐서플로우가 받은 이미지의 모습

    private CameraView cameraView;
    int cameraLeft = 0;
    int cameraTop = 0;
    int cameraRight = 0;
    int cameraBottom = 0;
    //예제에 원래 있던 카메라뷰
    //UI상으로는 크기가 지정되있지만
    //막상 찍으면 사용하는 기기의 전체화면비율(예: 20.5:9)로 사진이 찍히는것으로 보임
    //텐서플로우에 넣을때는 특정 해상도로 처리를 추가로 하고 넣음

    private ImageView imageViewGallery;
    private TextView textViewGallery;
    //추가로 만든 뷰들
    //이미지뷰는 기기의 갤러리에서 불러온 이미지를 표시
    //텍스트뷰는 그 파일의 경로를 표시
    //양쪽다 온 클릭 리스너를 달아서 동작을 하게 만들어 놓음
    //이미지뷰 터치 = 갤러리을 불러옴 -> 이미지 하나 선택 -> 선택한 이미지가 뜸
    //텍스트뷰 터치 = 위의 이미지 뷰가 로드 됬을때 이미지 비교를 실행
    //               아래의 변수들(b1, b2)을 이용함
    //               현재는 작동 안되고 튕김
    Bitmap b1;//카메라에서 찍은 사진을 담을 변수
    Bitmap b2;//갤러리에서 둘러오 이미지를 담을 변수

    private final static String TAG = MainActivity.class.getClass().getSimpleName();
    private boolean isOpenCvLoaded = false;
    //OpenCV를 쓰는데 필요한 변수들
    //자세히는 모르지만, OpenCV가 잘 로드 됬는지에 쓰는 변수 같음

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_kit);

        cameraView = (CameraView) findViewById(R.id.cameraView);
        imageViewResult = (ImageView) findViewById(R.id.imageViewResult);
        textViewResult = (TextView) findViewById(R.id.textViewResult);
        textViewResult.setMovementMethod(new ScrollingMovementMethod());
        textViewResult.setText("For object detection.");

        imageViewGallery = (ImageView) findViewById(R.id.imageViewGallery);
        textViewGallery = (TextView) findViewById(R.id.textViewGallery);
        textViewGallery.setMovementMethod(new ScrollingMovementMethod());
        textViewGallery.setText("<- Load the image first.");

        btnToggleCamera = (Button) findViewById(R.id.btnToggleCamera);
        btnDetectObject = (Button) findViewById(R.id.btnDetectObject);

        ViewTreeObserver vto = cameraView.getViewTreeObserver();//카메라뷰 좌표 구하기
        vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener()
                                       {
                                           @Override
                                           public void onGlobalLayout()
                                           {
                                               cameraView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                               RealCameraViewPoint(cameraView);
                                           }
                                       }
        );

        cameraView.addCameraKitListener(new CameraKitEventListener()
        {
            //카메라가 사진을 찍을 때 행하는 동작
            //사진을 하나 찍어서 텐서플오우에 줄것과 이미지 비교에 쓸것 총 2개를 저장
            //이미지 비교용은 일단 별다른 처리는 안함
            //텐서플로우에 줄 것은 별도의 처리를 하고 넘김
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

                int x1 = 0;//가로세로 1:1
                if(b1.getHeight() >= b1.getWidth())
                {
                    x1 = 0;
                }
                else
                {
                    x1 = ((b1.getWidth()) - (b1.getHeight())) / 2;
                }

                int y1 = 0;
                if(b1.getHeight() >= b1.getWidth())
                {
                    y1 = ((b1.getHeight()) - (b1.getWidth())) / 2;
                }
                else
                {
                    y1 = 0;
                }

                int l1 = 0;
                if(b1.getHeight() >= b1.getWidth())
                {
                    l1 = b1.getWidth();
                }
                else
                {
                    l1 = b1.getHeight();
                }

                /*
                Mat gray = new Mat();
                Utils.bitmapToMat(bitmap, gray);

                Imgproc.cvtColor(gray, gray, Imgproc.COLOR_RGBA2GRAY);

                Bitmap grayBitmap = Bitmap.createBitmap(gray.cols(), gray.rows(), null);
                Utils.matToBitmap(gray, grayBitmap);
                bitmap = grayBitmap;
                */
                //이미지를 흑백으로 바꾸는데 쓴 코드

                Mat image_original = new Mat();
                Utils.bitmapToMat(b1, image_original);
                Rect rectCrop = new Rect(x1, y1, l1, l1);
                Mat image_output = new Mat(image_original, rectCrop);
                Bitmap croppedBitmap = Bitmap.createBitmap(image_output.cols(), image_output.rows(), null);
                Utils.matToBitmap(image_output, croppedBitmap);
                b1 = croppedBitmap;
                //이미지를 특정 부분민큼 자르고 그 부분을 저장하는 코드

                Bitmap bitmap = b1;
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                //예제에 있던 코드, 텐서플로우에 사진을 넘기고 결과를 받아옴

                croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 1000, 1000, true);
                b1 = croppedBitmap;

                imageViewResult.setImageBitmap(b1);
                textViewResult.setText(results.toString());

                String s1 = "";
                for(int i = 0; i < results.size(); i++)
                {
                    s1 = results.get(i).toString();
                    if(s1.contains("notebook") || s1.contains("laptop"))
                    {
                        //Toast.makeText(getApplicationContext(), "노트북 !", Toast.LENGTH_LONG).show();
                        //(창)문 포착 시 뭐 하는 곳...
                    }
                }
                //텐서플로우가 받은 이미지를 표시 + 탐지 결과를 표시
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo)
            {

            }
        });


        imageViewGallery.setOnClickListener(new View.OnClickListener()
    {//누르면 갤러리에서 이미지를 불러와 보여주는 코드
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            startActivityForResult(intent, REQUEST_CODE);
        }
    }
        );
        textViewGallery.setOnClickListener(new View.OnClickListener()
    {//갤러리에서 이미지가 로드되면 그 파일의 결로를 표시 + 누르면 이미지 비교 실행
        @Override
        public void onClick(View v)
        {
            if(textViewGallery.getText().toString().equals("<- Load the image first."))
            {
                Toast.makeText(getApplicationContext(), "이미지를 먼저 불러와 주세요.", Toast.LENGTH_LONG).show();
            }
            else
            {
                //b1 = 카메라에서 찍은 이미지
                cameraView.captureImage();
                //b2 = 갤러리에서 가져온 이미지

                    /*AssetManager assetmanager = getAssets();
                    InputStream is;
                    try
                    {
                        is = assetmanager.open("dawn2.jpg");
                        b1 = BitmapFactory.decodeStream(is);
                        is = assetmanager.open("dawn3.jpg");
                        b2 = BitmapFactory.decodeStream(is);
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }*/
                //'assets'에서 파일 불러오기

                int ret = 999;
                ret = compareFeature(b1, b2);
                textViewGallery.setText("retVal: " + ret);
                //Toast.makeText(getApplicationContext(), "retVal: " + ret, Toast.LENGTH_LONG).show();

            }
        }
    });

        btnToggleCamera.setOnClickListener(new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            cameraView.toggleFacing();//예제에 원래 있던 코드, 전<->후면 카메라
        }
    });

        btnDetectObject.setOnClickListener(new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            cameraView.captureImage();
            //예제에 원래 있던 코드, 사진을 찍고 텐서플로우에 보냄
        }
    });

    // initTensorFlowAndLoadModel();//예제에 원래 있던 텐서플로우 실행 코드
    private void RealCameraViewPoint(View view)
    {
        View parentView = view.getRootView();
        int sumL = 0;
        int sumT = 0;
        int sumR = view.getMeasuredWidth();
        int sumB = view.getMeasuredHeight();

        boolean chk = false;
        while (!chk)
        {
            sumL = sumL + view.getLeft();
            sumT = sumT + view.getTop();

            view = (View) view.getParent();
            if(parentView == view)
            {
                cameraLeft = sumL;
                cameraTop = sumT;
                cameraRight = sumL + sumR;
                cameraBottom = sumT + sumB;
                chk = true;
            }
        }
    }

    // LoaderCallback
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {//OpenCV로딩과 관련된것으로 보임
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
    {//카메라 + OpenCV 준비로 보임
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
            isOpenCvLoaded = true;
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

    private void initTensorFlowAndLoadModel()//예제에 원래 있던 텐서플로우 초기화 + 실행 코드
    {
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
    {
        if(requestCode == REQUEST_CODE)//갤러리에서 가져온 이미지 + 그 파일의 경로 표시
        {
            if(resultCode == RESULT_OK)
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
                b2 = Bitmap.createScaledBitmap(b2, 1000, 1000, true);

                imageViewGallery.setImageBitmap(b2);
                textViewGallery.setText(realPath);
            }
            else if(resultCode == RESULT_CANCELED)
            {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected String getRealPath(Uri uri)
    //파일의 진짜경로를 알아내는 코드
    {

        Cursor cursor = getContentResolver().
                query(uri, null, null, null, null );

        cursor.moveToNext();

        String path = cursor.getString( cursor.getColumnIndex( "_data" ) );

        cursor.close();

        return path;

    }

    protected static int compareFeature(Bitmap CameraPhoto, Bitmap GalleryPhoto)
    {
        int retVal = 0;
        long startTime = System.currentTimeMillis();

        // Load images to compare
        Mat img1 = new Mat();
        Utils.bitmapToMat(CameraPhoto, img1);
        Mat img2 = new Mat();
        Utils.bitmapToMat(GalleryPhoto, img2);

        // Declare key point of images
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();

        // Definition of ORB key point detector and descriptor extractors
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

        // Detect key points
        detector.detect(img1, keypoints1);
        detector.detect(img2, keypoints2);

        // Extract descriptors
        extractor.compute(img1, keypoints1, descriptors1);
        extractor.compute(img2, keypoints2, descriptors2);

        // Definition of descriptor matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        // Match points of two images
        MatOfDMatch matches = new MatOfDMatch();
        if (descriptors2.cols() == descriptors1.cols())
        {
            matcher.match(descriptors1, descriptors2 ,matches);

            // Check matches of key points
            DMatch[] match = matches.toArray();
            double max_dist = 0; double min_dist = 100;

            for (int i = 0; i < descriptors1.rows(); i++)
            {
                double dist = match[i].distance;
                if( dist < min_dist ) min_dist = dist;
                if( dist > max_dist ) max_dist = dist;
            }

            // Extract good images (distances are under 10)
            for (int i = 0; i < descriptors1.rows(); i++)
            {
                if (match[i].distance <= 30)
                {
                    retVal++;
                }
            }
        }

        long estimatedTime = System.currentTimeMillis() - startTime;

        return retVal;
    }
}