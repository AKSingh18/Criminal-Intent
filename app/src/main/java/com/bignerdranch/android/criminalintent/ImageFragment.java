package com.bignerdranch.android.criminalintent;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.File;

public class ImageFragment extends DialogFragment {
    private static final String ARG_PHOTO_PATH = "photo_path";

    ImageView imageView;

    public static ImageFragment newInstance(File mPhotoFile) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_PHOTO_PATH, mPhotoFile);

        ImageFragment fragment = new ImageFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_image, null);
        File photoPath = (File) getArguments().getSerializable(ARG_PHOTO_PATH);

        imageView = (ImageView) view.findViewById(R.id.crime_photo_view);
        imageView.setImageBitmap(PictureUtils.getScaledBitmap(photoPath.toString(), getActivity()));

        return view;
    }
}
