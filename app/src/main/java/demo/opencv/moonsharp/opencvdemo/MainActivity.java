package demo.opencv.moonsharp.opencvdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();

    private static final int REQUEST_CODE_IMAGE_CAMERA = 1;
    private static final int REQUEST_CODE_IMAGE_OP = 2;
    private static final int REQUEST_CODE_OP = 3;

    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_register).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPerimission();
            }
        });

        findViewById(R.id.btn_detect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDetectDialog();
            }
        });
    }

    private void showRegisterDialog() {
        new AlertDialog.Builder(this)
                .setTitle("请选择注册方式")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setItems(new String[]{"打开图片", "拍摄照片"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Intent getImageByalbum = new Intent(Intent.ACTION_GET_CONTENT);
                                getImageByalbum.addCategory(Intent.CATEGORY_OPENABLE);
                                getImageByalbum.setType("image/jpeg");
                                startActivityForResult(getImageByalbum, REQUEST_CODE_IMAGE_OP);
                                break;

                            case 1:
                                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                                ContentValues values = new ContentValues(1);
                                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                                mImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                                startActivityForResult(intent, REQUEST_CODE_IMAGE_CAMERA);
                                break;

                            default:
                                break;
                        }
                    }
                })
                .show();
    }

    private void showDetectDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("请选择相机")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setItems(new String[]{"后置相机", "前置相机"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startDetector(which);
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMAGE_OP && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String imagPath = Utils.getUriPath(this, uri);
            Bitmap bmp = Utils.decodeImage(imagPath);
            if (bmp == null || bmp.getWidth() <= 0 || bmp.getHeight() <= 0) {
                Log.e(TAG, "error");
            } else {
                Log.i(TAG, "bmp [" + bmp.getWidth() + "," + bmp.getHeight());
            }
            startRegister(bmp, imagPath);
        }
        if (requestCode == REQUEST_CODE_IMAGE_CAMERA && resultCode == RESULT_OK) {
            String imgPath = Utils.getUriPath(this, mImageUri);
            Bitmap bmp = Utils.decodeImage(imgPath);
            startRegister(bmp, imgPath);
        }
    }

    /**
     * @param bitmap
     */
    private void startRegister(Bitmap bitmap, String imgPath) {
        if (TextUtils.isEmpty(imgPath)) {
            Toast.makeText(this, "获取照片路径失败", Toast.LENGTH_SHORT).show();
            return;
        }
//        Intent it = new Intent(MainActivity.this, RegisterActivity.class);
//        Bundle bundle = new Bundle();
//        bundle.putString("imagePath", file);
//        it.putExtras(bundle);
//        startActivityForResult(it, REQUEST_CODE_OP);
        Log.d(TAG, "startRegister: " + imgPath);
    }

    private void startDetector(int cameraFacing) {
//        Intent intent = new Intent(MainActivity.this, DetectActivity.class);
        Intent intent = new Intent(MainActivity.this, SurfaceDetectActivity.class);
        cameraFacing = cameraFacing == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        intent.putExtra("camera_facing", cameraFacing);
        startActivityForResult(intent, REQUEST_CODE_OP);
    }

    /**
     * 申请存储权限
     */
    public void requestPerimission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setMessage("注册人脸需要存储空间权限")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //申请权限，字符串数组内是一个或多个要申请的权限，1是申请权限结果的返回参数，在onRequestPermissionsResult可以得知申请结果
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,}, 1);
                            }
                        }).show();
            } else {
                //申请权限，字符串数组内是一个或多个要申请的权限，1是申请权限结果的返回参数，在onRequestPermissionsResult可以得知申请结果
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,}, 1);
            }
        } else {
            //已经拥有权限
            showRegisterDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    showRegisterDialog();
                } else {
                    Toast.makeText(this, "" + "权限" + permissions[i] + "申请失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}

