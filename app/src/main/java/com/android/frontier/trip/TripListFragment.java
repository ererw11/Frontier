package com.android.frontier.trip;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.frontier.R;
import com.android.frontier.utils.SignInActivity;
import com.android.frontier.utils.TripUtils;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TripListFragment extends Fragment {

    private static final String TAG = TripListFragment.class.getSimpleName();
    private static final int RC_SIGN_IN = 11;

    @BindView(R.id.trip_list_toolbar)
    private
    Toolbar mTripListToolBar;
    @BindView(R.id.trip_recycler_view)
    private
    RecyclerView mTripRecyclerView;
    @BindView(R.id.trip_list_no_logs_linear_layout)
    private
    LinearLayout mNoTripsLoggedLinearLayout;
    @BindView(R.id.add_trip_fab)
    private
    FloatingActionButton mAddTripFloatingActionButton;

    private ArrayList<Trip> tripList;
    private TripAdapter mAdapter;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tripList = new ArrayList<>();

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trip_list, container, false);
        ButterKnife.bind(this, view);

        mTripListToolBar.inflateMenu(R.menu.menu_list_layout);
        mTripListToolBar.setTitle(R.string.app_name);
        mTripListToolBar.setTitleTextColor(getResources().getColor(R.color.white));
        mTripListToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.refresh:
                        if (mFirebaseAuth.getUid() != null) {
                            updateUI(mFirebaseAuth.getUid());
                        } else {
                            TripUtils.confirmSignedIn(getActivity(), mFirebaseAuth.getCurrentUser());
                        }
                        return true;
                    case R.id.logout:
                        AuthUI.getInstance().signOut(getActivity())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            startActivity(SignInActivity.createIntent(getActivity()));
                                            getActivity().finish();
                                        } else {
                                            // Signout Failed
                                        }
                                    }
                                });
                        return true;
                    default:
                        return false;
                }
            }
        });

        RecyclerView.LayoutManager layoutManager;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600 || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutManager = new GridLayoutManager(getActivity(), 2);
        } else {
            layoutManager = new LinearLayoutManager(getContext());
        }
        mTripRecyclerView.setLayoutManager(layoutManager);
        mTripRecyclerView.setHasFixedSize(true);

        mAddTripFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Trip trip = new Trip();
                Intent intent = TripActivity.newIntent(getActivity(), trip);
                startActivity(intent);
            }
        });

        updateUI(mFirebaseAuth.getUid());

        return view;
    }

    private void updateUI(String userId) {
        attachDatabaseReference(userId);

        tripList.clear();
        retrieveTrips();

        if (mAdapter == null) {
            mAdapter = new TripAdapter();
            mTripRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void attachDatabaseReference(String userId) {
        mDatabaseReference = mFirebaseDatabase.getReference(
                userId)
                .child("tripKey");
    }

    private void retrieveTrips() {
        if (mFirebaseAuth.getUid() != null) {
            mDatabaseReference.addValueEventListener(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            tripList.clear();

                            Log.w(TAG, "count = " + String.valueOf(dataSnapshot.getChildrenCount()) + " fill ups" + dataSnapshot.getKey());
                            for (DataSnapshot data : dataSnapshot.getChildren()) {
                                Trip trip = data.getValue(Trip.class);
                                tripList.add(trip);
                                if (tripList.size() != 0) {
                                    mNoTripsLoggedLinearLayout.setVisibility(View.INVISIBLE);
                                } else {
                                    mNoTripsLoggedLinearLayout.setVisibility(View.VISIBLE);
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                        }
                    }
            );
        }
    }

    private class TripHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        int position;
        final TextView mTripTitleTextView;
        final TextView mNumberOfFillUps;
        final TextView mNumberOFExpenses;

        TripHolder(View itemView) {
            super(itemView);
            mTripTitleTextView = itemView.findViewById(R.id.list_trip_title);
            mNumberOfFillUps = itemView.findViewById(R.id.list_trip_fill_up_count);
            mNumberOFExpenses = itemView.findViewById(R.id.list_trip_expense_count);
            itemView.setOnClickListener(this);
        }

        void bind(Trip trip) {
            mTripTitleTextView.setText(trip.getTripName());
            mNumberOfFillUps.setText(String.format("%s %s", Integer.toString(trip.getNumberOfFillUps()), getString(R.string.space_fill_ups)));
            mNumberOFExpenses.setText(String.format("%s %s", Integer.toString(trip.getNumberOfExpenses()), getString(R.string.space_expenses)));
        }

        @Override
        public void onClick(View v) {
            Intent openTripIntent = TripActivity.newIntent(getActivity(), tripList.get(position));
            startActivity(openTripIntent);
        }
    }

    private class TripAdapter extends RecyclerView.Adapter<TripHolder> {

        @Override
        public TripHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false);
            return new TripHolder(v);
        }

        @Override
        public void onBindViewHolder(TripHolder holder, int position) {
            holder.position = position;
            Trip trip = tripList.get(position);
            holder.bind(trip);
        }

        @Override
        public int getItemCount() {
            if (tripList == null) {
                return 0;
            }
            return tripList.size();
        }
    }
}
