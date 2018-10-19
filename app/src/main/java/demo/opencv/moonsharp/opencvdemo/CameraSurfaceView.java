package demo.opencv.moonsharp.opencvdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private String TAG = CameraSurfaceView.class.getSimpleName();
    private Context mContext;
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private int screenHeight;//屏幕的高度
    private int screenWidth;//屏幕的宽度

    /***
     * 是否支持自动对焦
     */
    private boolean isSupportAutoFocus;
    public static Camera.Size pictureSize;
    private Camera.Size previewSize;

    private CaptureCallback mCallback;
    private int mFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    private SurfaceHolder mSurfaceHolder;

    public interface CaptureCallback {
        void onCapture(Bitmap bitmap);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        mHolder = this.getHolder();
        mHolder.addCallback(this);
    }

    private void init(Context context) {
        mContext = context;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        isSupportAutoFocus = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }

    public void setFacing(int facing) {
        this.mFacing = facing;
    }

    public void turnFacing() {
        if (mFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        closeCamera();
        openCamera();
    }

    private void openCamera() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            openCamera1();
        } else {
            Toast.makeText(mContext, "Camera2", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera1() {
        if (null == mSurfaceHolder) {
            Toast.makeText(mContext, "SurfaceHolder null", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            //必须先检查权限，否则Camera.open()会报错
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPerimission();
                return;
            }
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == mFacing) {
                    mCamera = Camera.open(i);
                    break;
                }
            }

            if (mCamera == null) {
                Toast.makeText(mContext, "找不到摄像头", Toast.LENGTH_LONG).show();
                return;
            }
            mCamera.setDisplayOrientation(90);
            // 设置holder主要是用于surfaceView的图片的实时预览，以及获取图片等功能，可以理解为控制camera的操作..
            mCamera.setPreviewDisplay(mSurfaceHolder);
            setCameraParms();
            mCamera.startPreview();
            mCamera.cancelAutoFocus();
            mCamera.setPreviewCallback(this);
            requestLayout();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mContext, "打开摄像头失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void closeCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        openCamera();
    }

    private void setCameraParms() {
        Camera.Parameters myParam = mCamera.getParameters();
        List<String> flashModes = myParam.getSupportedFlashModes();
        String flashMode = myParam.getFlashMode();
        // Check if camera flash exists
        if (flashModes == null) {
            return;
        }
        if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
            // Turn off the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                myParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
        }

        float percent = calcPreviewPercent();
        List<Camera.Size> supportedPreviewSizes = myParam.getSupportedPreviewSizes();
        previewSize = getPreviewMaxSize(supportedPreviewSizes, percent);
        Log.e(TAG, "预览尺寸w===" + previewSize.width + ",h===" + previewSize.height);
        // 获取摄像头支持的各种分辨率
        List<Camera.Size> supportedPictureSizes = myParam.getSupportedPictureSizes();
        pictureSize = findSizeFromList(supportedPictureSizes, previewSize);
        if (pictureSize == null) {
            pictureSize = getPictureMaxSize(supportedPictureSizes, previewSize);
        }
        Log.e(TAG, "照片尺寸w===" + pictureSize.width + ",h===" + pictureSize.height);
        // 设置照片分辨率，注意要在摄像头支持的范围内选择
        myParam.setPictureSize(pictureSize.width, pictureSize.height);
        // 设置预浏尺寸，注意要在摄像头支持的范围内选择
        myParam.setPreviewSize(previewSize.width, previewSize.height);
        myParam.setJpegQuality(70);

        mCamera.setParameters(myParam);
    }

    private float calcPreviewPercent() {
        float d = screenHeight;
        return d / screenWidth;
    }

    private Camera.Size findSizeFromList(List<Camera.Size> supportedPictureSizes, Camera.Size size) {
        Camera.Size s = null;
        if (supportedPictureSizes != null && !supportedPictureSizes.isEmpty()) {
            for (Camera.Size su : supportedPictureSizes) {
                if (size.width == su.width && size.height == su.height) {
                    s = su;
                    break;
                }
            }
        }
        return s;
    }

    // 根据摄像头的获取与屏幕分辨率最为接近的一个分辨率
    private Camera.Size getPictureMaxSize(List<Camera.Size> l, Camera.Size size) {
        Camera.Size s = null;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).width >= size.width && l.get(i).height >= size.width
                    && l.get(i).height != l.get(i).width) {
                if (s == null) {
                    s = l.get(i);
                } else {
                    if (s.height * s.width > l.get(i).width * l.get(i).height) {
                        s = l.get(i);
                    }
                }
            }
        }
        return s;
    }

    // 获取预览的最大分辨率
    private Camera.Size getPreviewMaxSize(List<Camera.Size> l, float j) {
        int idx_best = 0;
        int best_width = 0;
        float best_diff = 100.0f;
        for (int i = 0; i < l.size(); i++) {
            int w = l.get(i).width;
            int h = l.get(i).height;
            if (w * h < screenHeight * screenWidth)
                continue;
            float previewPercent = (float) w / h;
            float diff = Math.abs(previewPercent - j);
            if (diff < best_diff) {
                idx_best = i;
                best_diff = diff;
                best_width = w;
            } else if (diff == best_diff && w > best_width) {
                idx_best = i;
                best_diff = diff;
                best_width = w;
            }
        }
        return l.get(idx_best);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.e(TAG, "surfaceDestroyed");
        closeCamera();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 50, baos);
        byte[] jpgData = baos.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpgData, 0, jpgData.length);
        Bitmap resultBitmap = Utils.rotateBitmap(bitmap, mFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? 90 : -90);
        if (null != mCallback && null != resultBitmap) {
            mCallback.onCapture(resultBitmap);
        }
    }


    public void setCaptureCallback(CaptureCallback callback) {
        this.mCallback = callback;
    }

    /**
     * 申请相机权限
     */
    public void requestPerimission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) mContext, Manifest.permission.CAMERA)) {
                new AlertDialog.Builder(mContext)
                        .setMessage("检测人脸需要相机权限")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //申请权限，字符串数组内是一个或多个要申请的权限，1是申请权限结果的返回参数，在onRequestPermissionsResult可以得知申请结果
                                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.CAMERA,}, 1);
                            }
                        }).show();
            } else {
                //申请权限，字符串数组内是一个或多个要申请的权限，1是申请权限结果的返回参数，在onRequestPermissionsResult可以得知申请结果
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.CAMERA,}, 1);
            }
        } else {
            //已经拥有权限
            openCamera();
        }
    }

}
