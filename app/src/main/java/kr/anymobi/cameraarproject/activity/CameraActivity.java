package kr.anymobi.cameraarproject.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import kr.anymobi.cameraarproject.arview.ARCanvas;
import kr.anymobi.cameraarproject.R;
import kr.anymobi.cameraarproject.camera.AutoFitTextureView;
import kr.anymobi.cameraarproject.camera.DeviceOrientation;
import kr.anymobi.cameraarproject.util.CommFunc;
import kr.anymobi.cameraarproject.util.Loading;

import static kr.anymobi.cameraarproject.util.CommConst.DEBUGGING;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    ///////////////////////////////////////////////////////
    ///// def
    ///////////////////////////////////////////////////////
    private final int DEF_IMAGE_WIDTH = 1980;
    private final int DEF_IMAGE_HEIGHT = 1080;
    public static final String DEF_CODE_ID = "id";
    public static final String DEF_CODE_FILE = "file";
    public static final String DEF_CODE_TYPE = "type";
    public static final String DEF_CODE_TIME_OUT = "time_out";
    private final String LOG_TAG = getClass().getSimpleName();

    ///////////////////////////////////////////////////////
    ///// 변수
    ///////////////////////////////////////////////////////
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceViewHolder;
    private Handler mHandler;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mSession;
    private int mDeviceRotation;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private SensorManager mSensorManager;
    private DeviceOrientation deviceOrientation;
    int mDSI_height, mDSI_width;
    Context m_context;
    Dialog dialog;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initSurfaceView();
        Window window = this.getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.camera_background));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CommFunc.permissionCheck_file(this) && !CommFunc.permissionCheck_camera(this))
            CommFunc.checkPermission_common(getApplicationContext());

        // 화면 켜진 상태를 유지합니다.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        m_context = this;
        setContentView(R.layout.activity_camera);
        reConnectedWidget();
    }

    @Override
    protected void onDestroy() {
        ((ARCanvas) findViewById(R.id.testView)).destroyView();
        super.onDestroy();
    }

    private void reConnectedWidget() {
        mSurfaceView = findViewById(R.id.cameraView);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        assert mSensorManager != null;
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        deviceOrientation = new DeviceOrientation();

        ImageButton button = findViewById(R.id.btn_picture);
        button.setOnClickListener(this);

        ImageButton m_btnClose = findViewById(R.id.btn_close);
        m_btnClose.setOnClickListener(this);

        dialog = new Loading(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    public void initSurfaceView() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDSI_height = displayMetrics.heightPixels;
        mDSI_width = displayMetrics.widthPixels;

        mSurfaceViewHolder = mSurfaceView.getHolder();

        mSurfaceViewHolder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCameraAndPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }


        });
    }

    public void initCameraAndPreview() {
        HandlerThread handlerThread = new HandlerThread("CAMERA2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        Handler mainHandler = new Handler(getMainLooper());
        try {
            String mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT; // 후면 카메라 사용

            CameraManager mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            assert mCameraManager != null;
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            assert map != null;
            Size largestPreviewSize = map.getOutputSizes(ImageFormat.JPEG)[0];
            AutoFitTextureView autoFitTextureView = new AutoFitTextureView(this);
            autoFitTextureView.setAspectRatio(largestPreviewSize.getHeight(), largestPreviewSize.getWidth());

            /**
             * 사진 결과물 해상도 조절하는 부분
             **/
            mImageReader = ImageReader.newInstance(largestPreviewSize.getWidth(), largestPreviewSize.getHeight(), ImageFormat.JPEG,/*maxImages*/1);

            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mainHandler);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                CommFunc.settingOpen(this);
                return;
            }
            mCameraManager.openCamera(mCameraId, deviceStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "카메라를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public void takePreview() throws CameraAccessException {
        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(mSurfaceViewHolder.getSurface());
        mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceViewHolder.getSurface(), mImageReader.getSurface()), mSessionPreviewStateCallback, null);
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            // The camera preview can be run in a background thread. This is a Handler for camera
            // preview.
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            Handler backgroundHandler = new Handler(thread.getLooper());

            // Finally, we start displaying the camera preview.
            mSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        // In this sample, we just let the camera device pick the automatic settings.
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    public void takePicture() {
        try {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            assert audio != null;
            switch (audio.getRingerMode()) {
                case AudioManager.RINGER_MODE_NORMAL:
                case AudioManager.RINGER_MODE_SILENT:
                case AudioManager.RINGER_MODE_VIBRATE:
                    MediaActionSound sound = new MediaActionSound();
                    if (!DEBUGGING)
                        sound.play(MediaActionSound.SHUTTER_CLICK);
                    break;
            }

            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            // 화면 회전 안되게 고정시켜 놓은 상태에서는 아래 로직으로 방향을 얻을 수 없어서
            // 센서를 사용하는 것으로 변경
            mDeviceRotation = ORIENTATIONS.get(deviceOrientation.getOrientation());
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mDeviceRotation);
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mSession.stopRepeating();
            mSession.capture(mCaptureRequest, mSessionCaptureCallback, mHandler);
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) {
        if (bitmap == null) return null;
        if (degrees == 0) return bitmap;

        Matrix m = new Matrix();
        m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mSession.capture(mPreviewBuilder.build(), mSessionCaptureCallback,
                    mHandler);
            // After this, the camera will go back to the normal state of preview.
            mSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback,
                    mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAspectRatioTextureView(int ResolutionWidth, int ResolutionHeight) {
        if (ResolutionWidth > ResolutionHeight) {
            int newWidth = mDSI_width;
            int newHeight = ((mDSI_width * ResolutionWidth) / ResolutionHeight);
            updateTextureViewSize(newWidth, newHeight);

        } else {
            int newWidth = mDSI_width;
            int newHeight = ((mDSI_width * ResolutionHeight) / ResolutionWidth);
            updateTextureViewSize(newWidth, newHeight);
        }

    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        Log.d(LOG_TAG, "TextureView Width : " + viewWidth + " TextureView Height : " + viewHeight);
        mSurfaceView.setLayoutParams(new RelativeLayout.LayoutParams(viewWidth, viewHeight));
    }

    public void createImageFile(Bitmap bitmap) {
        String relativeLocation = Environment.DIRECTORY_PICTURES;

        String fileName = "에너지관리공단" + ".jpeg";

        int num = 1;

        File file = new File(relativeLocation, fileName);

        while (file.exists()) {
            fileName = "에너지관리공단" + " (" + (num++) + ")" + ".jpeg";
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        }

        ContentResolver cr = getContentResolver();
        Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {

            assert uri != null;
            OutputStream os = cr.openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, os);


            // 파일을 모두 write하고 다른곳에서 사용할 수 있도록 0으로 업데이트를 해줍니다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
            }


            cr.update(uri, values, null, null);
            //values.clear();

            /**
             * 결과 화면 이동 부분
             * AR 카메라에는 필요 없어서 비활성화 처리
             * 2020-11-26 김종우
             */
           /* Intent intent = new Intent(CameraActivity.this, ResultActivity.class);

            intent.putExtra("imagePath", uri.toString());
            unlockFocus();
            startActivity(intent);*/
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            cr.delete(uri, null, null);
        }

    }

    public void createImageFile_underQ(final Bitmap bitmap, Context ctx) {
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        String imageFileName = "에너지관리공단" + ".Jpeg";


        int num = 1;
        File file = new File(path, imageFileName);

        while (file.exists()) {
            imageFileName = "에너지관리공단" + " (" + (num++) + ")" + ".Jpeg";
            file = new File(path, imageFileName);
        }

        try {
            // Make sure the Pictures directory exists.
            if (path.mkdirs()) {
                //Toast.makeText(ctx, "Not exist :" + path.getName(), Toast.LENGTH_SHORT).show();
            }

            OutputStream os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, os);
            os.flush();
            os.close();
            //Log.i("ExternalStorage", "Writed " + path + file.getName());
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.

            /**
             * 결과 화면 이동 부분
             * AR 카메라에는 필요 없어서 비활성화 처리
             * 2020-11-26 김종우
             */
            /*MediaScannerConnection.scanFile(ctx,
                    new String[]{file.toString()}, null,
                    (path1, uri) -> {

                        Intent intent = new Intent(CameraActivity.this, ResultActivity.class);
                        intent.putExtra("imagePath", uri.toString());
                        startActivity(intent);
                        finish();
                    });*/
        } catch (Exception e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            //Log.w("ExternalStorage", "Error writing " + file, e);
        }

    }

    @SuppressLint("StaticFieldLeak")
    public class SaveImageTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setCancelable(false); // 외부 터치 방지
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
        }

        @Override
        protected Void doInBackground(Bitmap... data) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    createImageFile(getRotatedBitmap(data[0], mDeviceRotation));
                    Log.d(LOG_TAG, "Q");
                } else {
                    createImageFile(getRotatedBitmap(data[0], mDeviceRotation));
                    Log.d(LOG_TAG, "under Q");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            Toast.makeText(CameraActivity.this, "사진을 저장하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_picture:
                if (!CommFunc.permissionCheck_file(this)) {
                    CommFunc.settingOpen(this);
                    break;
                } else {
                    takePicture();
                }
                break;
            case R.id.btn_close:
                onBackPressed();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "request_state");
        if (!CommFunc.permissionCheck_camera(this) && !CommFunc.permissionCheck_file(this)) {
            CommFunc.settingOpen(this);
        } else {
            recreate();
        }
    }

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            mSession = session;
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            mSession = session;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mSession = session;
            updatePreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            //Toast.makeText(CameraActivity.this, "카메라 구성 실패", Toast.LENGTH_SHORT).show();
        }
    };

    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            try {
                takePreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            //Toast.makeText(CameraActivity.this, "카메라를 열지 못했습니다.", Toast.LENGTH_SHORT).show();
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    };

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            new SaveImageTask().execute(bitmap);
            image.close();
        }
    };


}
