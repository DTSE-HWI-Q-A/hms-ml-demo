/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.mlkit.sample.activity;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;

import com.mlkit.sample.R;
import com.mlkit.sample.camera.CameraConfiguration;
import com.mlkit.sample.camera.LensEngine;
import com.mlkit.sample.camera.LensEnginePreview;
import com.mlkit.sample.transactor.HandKeypointTransactor;
import com.mlkit.sample.util.Constant;
import com.mlkit.sample.views.AddPictureDialog;
import com.mlkit.sample.views.overlay.GraphicOverlay;

import java.io.IOException;

public final class HandKeypointActivity extends BaseActivity
        implements OnRequestPermissionsResultCallback,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private static final String TAG = "ClassificationActivity";
    private LensEngine lensEngine = null;
    private LensEnginePreview preview;
    private GraphicOverlay graphicOverlay;
    private ToggleButton facingSwitch;
    private ImageView imageSwitch;
    CameraConfiguration cameraConfiguration = null;
    private int facing = CameraConfiguration.CAMERA_FACING_BACK;
    private Camera mCamera;
    private AddPictureDialog addPictureDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_handkeypoint);
        if (savedInstanceState != null) {
            this.facing = savedInstanceState.getInt(Constant.CAMERA_FACING);
        }
        this.preview = this.findViewById(R.id.preview);
        this.findViewById(R.id.back).setOnClickListener(this);
        this.imageSwitch = this.findViewById(R.id.static_pic);
        this.imageSwitch.setOnClickListener(this);
        this.graphicOverlay = this.findViewById(R.id.overlay);
        this.cameraConfiguration = new CameraConfiguration();
        this.cameraConfiguration.setCameraFacing(this.facing);
        this.facingSwitch = this.findViewById(R.id.facingSwitch);
        this.facingSwitch.setOnCheckedChangeListener(this);
        if (Camera.getNumberOfCameras() == 1) {
            this.facingSwitch.setVisibility(View.GONE);
        }
        this.createDialog();
        this.createLensEngine();
        this.setStatusBar();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back) {
            releaseLensEngine();
            this.finish();
        } else if (view.getId() == R.id.static_pic) {
            showDialog();
        }
    }

    private void createDialog() {
        this.addPictureDialog = new AddPictureDialog(this);
        final Intent intent = new Intent(HandKeypointActivity.this, HandKeypointImageActivity.class);
        intent.putExtra(Constant.MODEL_TYPE, Constant.CLOUD_IMAGE_CLASSIFICATION);
        this.addPictureDialog.setClickListener(new AddPictureDialog.ClickListener() {
            @Override
            public void takePicture() {
                intent.putExtra(Constant.ADD_PICTURE_TYPE, Constant.TYPE_TAKE_PHOTO);
                preview.stop();
                HandKeypointActivity.this.startActivity(intent);
            }

            @Override
            public void selectImage() {
                intent.putExtra(Constant.ADD_PICTURE_TYPE, Constant.TYPE_SELECT_IMAGE);
                preview.stop();
                HandKeypointActivity.this.startActivity(intent);
            }

            @Override
            public void doExtend() {

            }
        });
    }

    private void showDialog() {
        this.addPictureDialog.show();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (this.lensEngine != null) {
            if (isChecked) {
                this.facing = CameraConfiguration.CAMERA_FACING_FRONT;
                this.cameraConfiguration.setCameraFacing(this.facing);
            } else {
                this.facing = CameraConfiguration.CAMERA_FACING_BACK;
                this.cameraConfiguration.setCameraFacing(this.facing);
            }
        }
        this.preview.stop();
        restartLensEngine();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(Constant.CAMERA_FACING, this.facing);
        super.onSaveInstanceState(outState);
    }


    private void createLensEngine() {
        if (this.lensEngine == null) {
            this.lensEngine = new LensEngine(this, this.cameraConfiguration, this.graphicOverlay);
        }
        try {
            this.lensEngine.setMachineLearningFrameTransactor(new HandKeypointTransactor(this.getApplicationContext()));
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Can not create image transactor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    public void restartLensEngine() {
        this.startLensEngine();
        if (null != this.lensEngine) {
            this.mCamera = this.lensEngine.getCamera();
            try {
                this.mCamera.setPreviewDisplay(this.preview.getSurfaceHolder());
            } catch (IOException e) {
                Log.d(HandKeypointActivity.TAG, "initViews IOException");
            }
        }
    }

    private void startLensEngine() {
        if (this.lensEngine != null) {
            try {
                this.preview.start(this.lensEngine, false);
            } catch (IOException e) {
                Log.e(HandKeypointActivity.TAG, "Unable to start lensEngine.", e);
                this.lensEngine.release();
                this.lensEngine = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.startLensEngine();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.preview.stop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        releaseLensEngine();
    }

    private void releaseLensEngine(){
        if (this.lensEngine != null) {
            this.lensEngine.release();
            this.lensEngine = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseLensEngine();
    }
}

