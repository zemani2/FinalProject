package com.example.anxietyByHeartRate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

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
    private TextView usernameTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public InfoFragment() {
        // Required empty public constructor
    }

    public static InfoFragment newInstance() {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void setUserData(View view, String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> userDetails = document.getData();
                            if (userDetails != null) {
                                age = Integer.parseInt(String.valueOf(userDetails.get("age")));
                                weight = Integer.parseInt(String.valueOf(userDetails.get("weight")));
                                height = Integer.parseInt(String.valueOf(userDetails.get("height")));

                                ageTextView.setText("Age: " + age);
                                weightTextView.setText("Weight: " + weight + " kg");
                                heightTextView.setText("Height: " + height + " cm");
                            }
                        } else {
                            // Document doesn't exist
                            Log.d("Firestore", "No such document");
                        }
                    } else {
                        // Error fetching document
                        Log.d("Firestore", "get failed with ", task.getException());
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_info, container, false);

        ageTextView = view.findViewById(R.id.ageTextView);
        weightTextView = view.findViewById(R.id.weightTextView);
        heightTextView = view.findViewById(R.id.heightTextView);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        assert currentUser != null;
        String userId = currentUser.getUid();
        setUserData(view, userId);

        ImageButton editButton = view.findViewById(R.id.editButton);
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
