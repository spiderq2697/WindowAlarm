package com.example.ysh.pleasewakemeup;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.InputStream;
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
import org.opencv.core.Mat;

import org.opencv.core.DMatch;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

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

    private TextView textViewResult;
    private Button btnDetectObject, btnToggleCamera;
    private ImageView imageViewResult;

    private CameraView cameraView;
    private static final int Window1Mod = 1;
    private static final int Window2Mod = 2;
    private static final int AlarmCallMod = 0;
    private static final int TestMod = 3;
    private int RemainingShots = 999;

    private ImageView imageViewGallery;
    private TextView textViewGallery;

    private ImageView imageViewCheck;

    private Bitmap b1;//카메라에서 찍은 사진을 담을 변수
    private Bitmap b2;//이미 찍은 장문 이미지(1,000 x 1,000)

    private final static String TAG = CameraKit.class.getClass().getSimpleName();
    private boolean isOpenCvLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
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

        imageViewCheck = (ImageView) findViewById(R.id.imageViewCheck);

        btnToggleCamera = (Button) findViewById(R.id.btnToggleCamera);
        btnDetectObject = (Button) findViewById(R.id.btnDetectObject);

        final Intent intent = getIntent();
        final int cameraMod = intent.getIntExtra("CameraMod", TestMod);

        if(cameraMod == Window1Mod || cameraMod == Window2Mod)
        {
            imageViewGallery.setVisibility(View.INVISIBLE);
            textViewGallery.setVisibility(View.INVISIBLE);
            imageViewResult.setVisibility(View.INVISIBLE);
            textViewResult.setVisibility(View.INVISIBLE);
        }
        else
        {
            imageViewCheck.setVisibility(View.INVISIBLE);
        }

        Toast.makeText(getApplicationContext(), "사진은 보이는 칸에 맞춰 바르게 찍어주세요.", Toast.LENGTH_LONG).show();

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

                int x1 = 0;
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
                Rect rectCrop = new Rect(x1, y1, l1, l1);//OpenCv의 Rect
                Mat image_output = new Mat(image_original, rectCrop);
                Bitmap croppedBitmap = Bitmap.createBitmap(image_output.cols(), image_output.rows(), null);
                Utils.matToBitmap(image_output, croppedBitmap);
                b1 = croppedBitmap;
                //이미지를 특정 부분민큼 자르고 그 부분을 저장하는 코드

                Bitmap bitmap = b1;
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                b1 = Bitmap.createScaledBitmap(b1, 1000, 1000, true);
                imageViewResult.setImageBitmap(b1);
                textViewResult.setText(results.toString());

                if(cameraMod != TestMod)
                {
                    String s1 = "";
                    for(int i = 0; i < results.size(); i++)
                    {
                        s1 = results.get(i).toString();
                        if(s1.contains("notebook") || s1.contains("laptop"))//해당 물체가 발견되면...
                        {
                            if(cameraMod == AlarmCallMod)
                            {
                                if(RemainingShots == 1)
                                {
                                    Toast.makeText(getApplicationContext(), "등록된 (창)문이 모두 찍혔습니다."
                                            , Toast.LENGTH_LONG).show();
                                    RemainingShots -= 1;
                                    //그 후, 알람 종료...???
                                }
                                else if(RemainingShots == 2)
                                {
                                    Toast.makeText(getApplicationContext(), "1번째 (창)문 완료, 2번째를 찍어주세요."
                                            , Toast.LENGTH_LONG).show();
                                    RemainingShots -= 1;
                                }
                            }
                            else
                            {
                                File folder = new File(getExternalFilesDir(null) + "/WindowAlarm");
                                if(!folder.exists())
                                {
                                    folder.mkdir();
                                }

                                if(cameraMod == Window1Mod)
                                {
                                    String filename = "Window1.jpg";

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
                                    Toast.makeText(getApplicationContext(), "1번째 (창)문 저장 완료, 나가셔도 됩니다."
                                            , Toast.LENGTH_LONG).show();
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
                                }
                            }
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "(창)문이 찍히지 않았습니다, 다시 찍어 주세요."
                                    , Toast.LENGTH_LONG).show();
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
                    cameraView.captureImage();

                    AssetManager assetmanager = getAssets();
                    InputStream is;
                    try
                    {
                        is = assetmanager.open("dawn1.jpg");
                        b1 = BitmapFactory.decodeStream(is);
                        is = assetmanager.open("dawn2.jpg");
                        b2 = BitmapFactory.decodeStream(is);
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();//'assets'에서 파일 불러오기
                    }

                    int ret = 999;
                    ret = compareFeature(b1, b2);
                    textViewGallery.setText("retVal: " + ret);
                }
            }
        });

        btnToggleCamera.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cameraView.toggleFacing();
            }
        }
        );

        btnDetectObject.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cameraView.captureImage();
            }
        }
        );

        initTensorFlowAndLoadModel();
    }

    // LoaderCallback
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this)
    {
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
        }
        );
    }

    private void initTensorFlowAndLoadModel()
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
        }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE)//갤러리에서 가져온 이미지 + 그 파일의 경로 표시
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
            else if (resultCode == RESULT_CANCELED)
            {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected String getRealPath(Uri uri)
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

        // Avoid to assertion failed
        // Assertion failed (type == src2.type() && src1.cols == src2.cols && (type == CV_32F || type == CV_8U)
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
                if (match[i].distance <= 10)
                {
                    retVal++;
                }
            }
        }

        return retVal;
    }
}
