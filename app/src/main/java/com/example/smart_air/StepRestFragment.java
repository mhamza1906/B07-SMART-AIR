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

public class StepRestFragment extends Fragment {

    private boolean cooler1 = false;  // 1 minute cooldown for "Another Puff"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.technique_step_rest, container, false);

        ImageView gif = v.findViewById(R.id.imgTechniqueStepRest);
        Button btnAnother = v.findViewById(R.id.btnAnother);
        Button btnFinish = v.findViewById(R.id.btnFinish);

        Glide.with(this)
                .asGif()
                .load(R.drawable.technique_step_rest)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(gif);

        // 1-minute cooldown
        CooldownHelper.startCooldown(60000, () -> cooler1 = true);

        btnAnother.setOnClickListener(view -> {
            if (!cooler1) {
                Toast.makeText(getContext(), "You're doing it too fast, please slow down a bit", Toast.LENGTH_SHORT).show();
                return;
            }
            ((TechniqueHelperActivity) requireActivity())
                    .loadFragment(new Step4Fragment());
        });

        btnFinish.setOnClickListener(view -> ((TechniqueHelperActivity) requireActivity()).finishHelper());

        return v;
    }
}
