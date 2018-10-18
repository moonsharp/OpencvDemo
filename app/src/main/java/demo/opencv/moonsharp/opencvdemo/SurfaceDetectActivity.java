package demo.opencv.moonsharp.opencvdemo;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class SurfaceDetectActivity extends AppCompatActivity {

    CameraSurfaceView cameraSurfaceView;
    ImageView ivPreview;
    private int mCameraFacing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_detect);
        mCameraFacing = getIntent().getIntExtra("camera_facing", Camera.CameraInfo.CAMERA_FACING_BACK);

        cameraSurfaceView = findViewById(R.id.surfaceView);
        ivPreview = findViewById(R.id.iv_preview);
        findViewById(R.id.ib_turn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraSurfaceView.turnFacing();
            }
        });

        cameraSurfaceView.setFacing(mCameraFacing);
        cameraSurfaceView.setCaptureCallback(new CameraSurfaceView.CaptureCallback() {
            @Override
            public void onCapture(Bitmap bitmap) {
                ivPreview.setImageBitmap(bitmap);
            }
        });
    }
}
