package com.example.smart_air;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class Step3NotEmptyFragment extends Fragment {


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.technique_step3_if_not_empty, container, false);

        ImageView gif = v.findViewById(R.id.imgTechniqueStep3NotEmpty);
        Button btnNext = v.findViewById(R.id.btnNext);

        Glide.with(this)
                .asGif()
                .load(R.drawable.technique_step3_if_not_empty)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(gif);


        btnNext.setOnClickListener(view -> ((TechniqueHelperActivity) requireActivity())
                .loadFragment(new Step4Fragment()));

        return v;
    }
}
