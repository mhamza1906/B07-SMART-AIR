package com.example.smart_air;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class Step7Fragment extends Fragment {

    private boolean cooler1 = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.technique_step7, container, false);

        ImageView gif = v.findViewById(R.id.imgTechniqueStep7);
        Button btnNext = v.findViewById(R.id.btnNext);

        Glide.with(this)
                .asGif()
                .load(R.drawable.technique_step7)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(gif);

        CooldownHelper.startCooldown(8000, () -> cooler1 = true);

        btnNext.setOnClickListener(view -> {
            if (!cooler1) {
                Toast.makeText(getContext(), "You're doing it too fast, please slow down a bit", Toast.LENGTH_SHORT).show();
                return;
            }
            ((TechniqueHelperActivity) requireActivity())
                    .loadFragment(new StepRestFragment());
        });

        return v;
    }
}
