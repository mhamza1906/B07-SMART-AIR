package com.example.smart_air;

import android.content.Context;
import android.content.Intent;
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

    private TechniqueHelperActivity parent;

    private boolean cooler1 = false;  // 1 minute cooldown for "Another Puff"

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        parent = (TechniqueHelperActivity) context;
    }

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

        btnFinish.setOnClickListener(view -> {

            Intent finish = new Intent(parent, TakeMedicineActivityPost.class);

            finish.putExtra("childID", parent.getChildID());
            finish.putExtra("type", parent.getMedType());
            finish.putExtra("date", parent.getDate());

            startActivity(finish);

            parent.finishHelper();
        });

        return v;
    }
}
