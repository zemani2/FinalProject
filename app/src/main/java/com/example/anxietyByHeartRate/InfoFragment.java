package com.example.anxietyByHeartRate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InfoFragment extends Fragment {

    private static final String ARG_AGE = "age";
    private static final String ARG_WEIGHT = "weight";
    private static final String ARG_HEIGHT = "height";
    private static final String ARG_USERNAME = "username";

    private int age;
    private int weight;
    private int height;
    private String username;
    private TextView ageTextView;
    private TextView weightTextView;
    private TextView heightTextView;
    public InfoFragment() {
        // Required empty public constructor
    }

    public static InfoFragment newInstance(int age, int weight, int height, String username) {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_AGE, age);
        args.putInt(ARG_WEIGHT, weight);
        args.putInt(ARG_HEIGHT, height);
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            age = getArguments().getInt(ARG_AGE);
            weight = getArguments().getInt(ARG_WEIGHT);
            height = getArguments().getInt(ARG_HEIGHT);
            username = getArguments().getString(ARG_USERNAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        ageTextView = view.findViewById(R.id.ageTextView);
        weightTextView = view.findViewById(R.id.weightTextView);
        heightTextView = view.findViewById(R.id.heightTextView);
        ImageButton editButton = view.findViewById(R.id.editButton);

        ageTextView.setText("Age: " + age);
        weightTextView.setText("Weight: " + weight + " kg");
        heightTextView.setText("Height: " + height + " cm");

        editButton.setOnClickListener(v -> {
            EditFragment editFragment = EditFragment.newInstance(age, weight, height, username);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, editFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
    public void updateData(int selectedAge, int selectedWeight, int selectedHeight) {
        age = selectedAge;
        weight = selectedWeight;
        height = selectedHeight;

        if (isAdded()) {
            ageTextView.setText("Age: " + age);
            weightTextView.setText("Weight: " + weight + " kg");
            heightTextView.setText("Height: " + height + " cm");
        }
    }
}
