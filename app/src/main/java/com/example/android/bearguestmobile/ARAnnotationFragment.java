/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bearguestmobile;

import android.arch.lifecycle.*;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.example.android.bearguestmobile.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
public class ARAnnotationFragment extends Fragment {

  private ArFragment arFragment;
  private ImageView fitToScanView;
  private View ARAnnotationView;

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AnnotationNode> augmentedImageMap = new HashMap<>();


  public ARAnnotationFragment() {
    // Required empty public constructor
  }

  /* @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);


  } */

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    ARAnnotationView = inflater.inflate(R.layout.fragment_arscanner, container, false);

    FragmentManager f = getChildFragmentManager();
    arFragment = (ArFragment)f.findFragmentById(R.id.ux_fragment);
    fitToScanView = (ImageView) ARAnnotationView.findViewById(R.id.image_view_fit_to_scan);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

    // Hide the back arrow and set title
    ToolbarViewModel toolbarViewModel = ViewModelProviders.of((MainActivity)getActivity()).get(ToolbarViewModel.class);
    toolbarViewModel.setShowBackArrow(true);
    toolbarViewModel.setToolbarTitle("AR Annotation Tool");

    // Inflate the layout for this fragment
    return ARAnnotationView;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      fitToScanView.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Registered with the Sceneform Scene object, this method is called at the start of each frame.
   *
   * @param frameTime - time since last frame.
   */
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();

    // If there is no frame or ARCore is not tracking yet, just return.
    if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      return;
    }

    Collection<AugmentedImage> updatedAugmentedImages =
        frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          // String text = "Detected Image " + augmentedImage.getIndex();
          // SnackbarHelper.getInstance().showMessage(getActivity(), text);
          break;

        case TRACKING:
          // Have to switch to UI Thread to update View.
          fitToScanView.setVisibility(View.GONE);

          // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage)) {
            AnnotationNode node = new AnnotationNode(getContext(), this.getViewLifecycleOwner(), ARAnnotationView);
            node.setImage(augmentedImage);
            augmentedImageMap.put(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);
          }
          break;

        case STOPPED:
          augmentedImageMap.remove(augmentedImage);
          break;
      }
    }
  }
}