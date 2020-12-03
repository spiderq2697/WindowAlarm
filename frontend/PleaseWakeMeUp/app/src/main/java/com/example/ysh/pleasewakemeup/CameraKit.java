package com.example.ysh.pleasewakemeup;

import android.content.Intent;
import android.content.res.AssetManager;
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
import java.io.InputStream;
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
import org.opencv.core.DMatch;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
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

    private Bitmap b1;
    private Bitmap b2;

    private final static String TAG = CameraKit.class.getClass().getSimpleName();

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
        else if(cameraMod == AlarmCallMod)
        {
            imageViewGallery.setVisibility(View.INVISIBLE);
            textViewGallery.setVisibility(View.INVISIBLE);
            imageViewCheck.setVisibility(View.INVISIBLE);

            File WindowFolder = new File(getExternalFilesDir(null) + "/WindowAlarm");
            File Window1 = new File(WindowFolder, "Window1.jpg");
            File Window2 = new File(WindowFolder, "Window2.jpg");
            if(Window1.exists())
            {
                b2 = BitmapFactory.decodeFile(getExternalFilesDir(null)
                        + "/WindowAlarm/Window1.jpg");
                imageViewResult.setImageBitmap(b2);
                textViewResult.setText("<- 1번째 (창)문을 찍어주세요.");
                if(Window2.exists())
                {
                    RemainingShots = 2;
                }
                else
                {
                    RemainingShots = 1;
                }
            }
            else
            {
                Toast.makeText(getApplicationContext(), "(창)문을 먼저 등록하세요.", Toast.LENGTH_LONG).show();
                finish();
            }
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

                Mat image_original = new Mat();
                Utils.bitmapToMat(b1, image_original);
                Rect rectCrop = new Rect(x1, y1, l1, l1);
                Mat image_output = new Mat(image_original, rectCrop);
                Bitmap croppedBitmap = Bitmap.createBitmap(image_output.cols(), image_output.rows(), null);
                Utils.matToBitmap(image_output, croppedBitmap);
                b1 = croppedBitmap;

                Bitmap bitmap = b1;
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                b1 = Bitmap.createScaledBitmap(b1, 1000, 1000, true);
                if(cameraMod == TestMod)
                {
                    imageViewResult.setImageBitmap(b1);
                    textViewResult.setText(results.toString());
                }
                else
                {
                    String s1 = "";

                    DetectLoop:
                    for(int i = 0; i < results.size(); i++)
                    {
                        s1 = results.get(i).toString();
                        if(s1.contains("notebook") || s1.contains("laptop"))///
                        {
                            if(cameraMod == AlarmCallMod)
                            {
                                int r1 = compareHistogram(b1, b2);
                                if(r1 > 0)
                                {
                                    if(RemainingShots == 1)
                                    {
                                        Toast.makeText(getApplicationContext(), "등록된 (창)문이 모두 찍혔습니다."
                                                , Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                    else if(RemainingShots == 2)
                                    {
                                        Toast.makeText(getApplicationContext(), "1번째 (창)문 완료, 2번째를 찍어주세요."
                                                , Toast.LENGTH_LONG).show();
                                        b2 = BitmapFactory.decodeFile(getExternalFilesDir(null)
                                                + "/WindowAlarm/Window2.jpg");
                                        imageViewResult.setImageBitmap(b2);
                                        textViewResult.setText("<- 2번째 (창)문을 찍어주세요.");
                                        RemainingShots = 1;
                                        break DetectLoop;
                                    }
                                }
                                else
                                {
                                    Toast.makeText(getApplicationContext(), "올바른 (창)문을 찍어주세요."
                                            , Toast.LENGTH_SHORT).show();
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
                                    break DetectLoop;
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
                                }
                            }
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), "(창)문이 찍히지 않았습니다, 다시 찍어 주세요."
                                    , Toast.LENGTH_SHORT).show();
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
            }
        });
        textViewGallery.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(textViewGallery.getText().toString().equals("<- Load the image first."))
                {
                    Toast.makeText(getApplicationContext(), "이미지를 먼저 불러와 주세요.", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    cameraView.captureImage();

                    /*AssetManager assetmanager = getAssets();
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
                        e.printStackTrace();
                    }*/

                    int ret = compareHistogram(b1, b2);
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
        });

        btnDetectObject.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cameraView.captureImage();
            }
        });

        initTensorFlowAndLoadModel();
    }

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
    {
        Cursor cursor = getContentResolver().
                query(uri, null, null, null, null );

        cursor.moveToNext();

        String path = cursor.getString( cursor.getColumnIndex( "_data" ) );

        cursor.close();

        return path;
    }

    protected static int compareHistogram(Bitmap CameraPhoto, Bitmap GalleryPhoto)
    {
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
