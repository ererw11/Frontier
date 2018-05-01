package com.android.frontier.fill_up;

import android.content.Context;
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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FillUpListFragment extends Fragment
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = FillUpListFragment.class.getSimpleName();
    private static final String ARG_TRIP_ID = "com.android.frontier.fill_up.trip_id";

    @BindView(R.id.fill_up_recycler_view)
    private
    RecyclerView mFillUpRecyclerView;
    @BindView(R.id.add_fill_up_fab)
    private
    FloatingActionButton mFloatingActionButton;
    @BindView(R.id.fill_up_list_toolbar)
    private
    Toolbar mFillUpListToolbar;
    @BindView(R.id.banner_ad_view_fill_up)
    private
    AdView mFillUpAdView;
    @BindView(R.id.fill_up_list_no_logs_linear_layout)
    private
    LinearLayout mNoFillUpsLoggedLinearLayout;

    private ArrayList<FillUp> fillUpList;
    private FillUpAdapter mAdapter;
    private Callbacks mCallbacks;
    private String mTripId;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DatabaseReference mDatabaseReference;

    public static FillUpListFragment newInstance(String tripId) {
        Bundle arg = new Bundle();
        arg.putString(ARG_TRIP_ID, tripId);

        FillUpListFragment fragment = new FillUpListFragment();
        fragment.setArguments(arg);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        fillUpList = new ArrayList<>();
        mTripId = getArguments().getString(ARG_TRIP_ID);
        // TODO = remove Log
        Log.i(TAG, mTripId);

        mGoogleApiClient = new GoogleApiClient
                .Builder(getContext())
                .enableAutoManage(getActivity(), 10, this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addOnConnectionFailedListener(this)
                .build();

        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = firebaseDatabase.getReference(
                mFirebaseAuth.getUid())
                .child(getString(R.string.trip_details_db_title))
                .child(mTripId)
                .child("fillUpList");
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                TripUtils.confirmSignedIn(getActivity(), user);
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fill_up_list, container, false);
        ButterKnife.bind(this, view);

        mFillUpListToolbar.inflateMenu(R.menu.menu_list_layout);
        mFillUpListToolbar.setTitle(R.string.fill_ups);
        mFillUpListToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        mFillUpListToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.refresh:
                        updateUI();
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
        mFillUpRecyclerView.setLayoutManager(layoutManager);
        mFillUpRecyclerView.setHasFixedSize(true);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FillUp fillUp = new FillUp();
                updateUI();
                mCallbacks.onFillUpSelected(fillUp, mTripId);
            }
        });

        updateUI();

        buildAd();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
        mAdapter.notifyDataSetChanged();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        mCallbacks = null;
    }

    public void updateUI() {
        mFillUpRecyclerView.removeAllViews();
        fillUpList.clear();
        retrieveFillUps();

        if (mAdapter == null) {
            mAdapter = new FillUpAdapter();
            mFillUpRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void buildAd() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mFillUpAdView.loadAd(adRequest);
    }

    private void retrieveFillUps() {
        mDatabaseReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        fillUpList.clear();

                        Log.w(TAG, "count = " + String.valueOf(dataSnapshot.getChildrenCount()) + " fill ups " + dataSnapshot.getKey());
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            FillUp fillUp = data.getValue(FillUp.class);
                            fillUpList.add(fillUp);
                            if (fillUpList.size() != 0) {
                                mNoFillUpsLoggedLinearLayout.setVisibility(View.INVISIBLE);
                            } else {
                                mNoFillUpsLoggedLinearLayout.setVisibility(View.VISIBLE);
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

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /*
    *   Required interface for hosting activities
    * */
    public interface Callbacks {
        void onFillUpSelected(FillUp fillUp, String tripId);
    }

    private class FillUpHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        int position;
        final TextView mLocationTextView;
        final TextView mDateTextView;
        final TextView mFillUpCostTextView;
        private FillUp mFillUp;

        FillUpHolder(View itemView) {
            super(itemView);
            mLocationTextView = itemView.findViewById(R.id.fill_up_location_list_item);
            mDateTextView = itemView.findViewById(R.id.fill_up_date_list_item);
            mFillUpCostTextView = itemView.findViewById(R.id.fill_up_total_cost_list_item);
            itemView.setOnClickListener(this);
        }

        void bind(FillUp fillUp) {
            mFillUp = fillUp;
            if (mFillUp.getPlaceId() != null) {
                getPlaceNameByIdAndDisplay(mFillUp.getPlaceId());
            } else {
                mLocationTextView.setText(getString(R.string.fill_up_location_text));
            }
            mDateTextView.setText(mFillUp.getDate());
            mFillUpCostTextView.setText(TripUtils.getTotalCost(mFillUp));
        }

        void getPlaceNameByIdAndDisplay(String placeId) {
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                    .setResultCallback(new ResultCallback<PlaceBuffer>() {
                        @Override
                        public void onResult(@NonNull PlaceBuffer places) {
                            if (places.getStatus().isSuccess()) {
                                Place place = places.get(0);
                                mLocationTextView.setText(place.getName().toString());
                                mLocationTextView.setText(place.getName().toString());
                            } else {
                                mLocationTextView.setText(getString(R.string.fill_up_location_text));
                            }
                        }
                    });
        }

        @Override
        public void onClick(View v) {
            mCallbacks.onFillUpSelected(fillUpList.get(position), mTripId);
        }
    }

    class FillUpAdapter extends RecyclerView.Adapter<FillUpHolder> {

        @Override
        public FillUpHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_fill_up, parent, false);
            return new FillUpHolder(v);
        }

        @Override
        public void onBindViewHolder(FillUpHolder holder, int position) {
            holder.position = position;
            FillUp fillUp = fillUpList.get(position);
            holder.bind(fillUp);
        }

        @Override
        public int getItemCount() {
            if (fillUpList == null) {
                return 0;
            }
            return fillUpList.size();
        }
    }
}