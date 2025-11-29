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

public class Step1Fragment extends Fragment {

    private boolean cooler1 = false;  // 8-second cooldown

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.technique_step1, container, false);

        ImageView gif = v.findViewById(R.id.imgTechniqueStep1);
        Button btnEmpty = v.findViewById(R.id.btnEmpty);
        Button btnNotEmpty = v.findViewById(R.id.btnNotEmpty);

        Glide.with(this)
                .asGif()
                .load(R.drawable.technique_step1)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(gif);

        // Start cooldown
        CooldownHelper.startCooldown(8000, () -> cooler1 = true);

        btnEmpty.setOnClickListener(view -> {
            if (!cooler1) {
                Toast.makeText(getContext(), "You're doing it too fast, please slow down a bit", Toast.LENGTH_SHORT).show();
                return;
            }
            ((TechniqueHelperActivity) requireActivity())
                    .loadFragment(new Step2EmptyFragment());

        });

        btnNotEmpty.setOnClickListener(view -> {
            if (!cooler1) {
                Toast.makeText(getContext(), "You're doing it too fast, please slow down a bit", Toast.LENGTH_SHORT).show();
                return;
            }
            ((TechniqueHelperActivity) requireActivity())
                    .loadFragment(new Step2NotEmptyFragment());

        });

        return v;
    }
}
