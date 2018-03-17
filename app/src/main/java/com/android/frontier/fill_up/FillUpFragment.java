package com.android.frontier.fill_up;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.frontier.R;
import com.android.frontier.utils.SignInActivity;
import com.android.frontier.utils.TripUtils;
import com.android.frontier.widget.FrontierActionService;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class FillUpFragment extends Fragment
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = FillUpFragment.class.getSimpleName();
    private static final String ARG_FILL_UP = "com.android.frontier.fill_up.fill_up";
    private static final String ARG_TRIP_ID = "com.android.frontier.fill_up.trip_id";
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 0;

    @BindView(R.id.fill_up_location)
    TextView mFillUpLocationTextView;
    @BindView(R.id.enter_location_header)
    TextView mFillUpLocationHeader;
    @BindView(R.id.fill_up_mileage)
    EditText mFillUpTripMileageField;
    @BindView(R.id.fill_up_price_per_gallon)
    EditText mFillUpPricePerGallonField;
    @BindView(R.id.fill_up_total_gallons)
    EditText mFillUpTotalGallonsField;
    @BindView(R.id.fill_up_date)
    TextView mFillUpDateTextView;
    @BindView(R.id.share_fill_up_button)
    ImageButton mFillUpShareFillUpButton;
    @BindView(R.id.fill_up_cost)
    TextView mFillUpTotalCostTextView;
    @BindView(R.id.fill_up_mpg)
    TextView mFillUpMilesPerGallonTextView;
    @BindView(R.id.fill_up_address)
    TextView mFillUpAddressTextView;
    @BindView(R.id.save_fill_up_fab)
    FloatingActionButton mFloatingActionButton;
    @BindView(R.id.fill_up_toolbar)
    Toolbar mFillUpToolbar;

    private FillUp mFillUp;
    private String mPlaceId;
    private String mWidgetFillUpLocation;
    private Callbacks mCallbacks;
    private String mTripId;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    public static FillUpFragment newFillUpInstance(FillUp fillUp, String tripId) {
        Bundle arg = new Bundle();
        arg.putSerializable(ARG_FILL_UP, fillUp);
        arg.putString(ARG_TRIP_ID, tripId);

        FillUpFragment fragment = new FillUpFragment();
        fragment.setArguments(arg);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFillUp = new FillUp();
        mFillUp = (FillUp) getArguments().getSerializable(ARG_FILL_UP);
        mTripId = getArguments().getString(ARG_TRIP_ID);
        //TODO = remove Log
        Log.i(TAG, mTripId);

        mGoogleApiClient = new GoogleApiClient
                .Builder(getContext())
                .enableAutoManage(getActivity(), 0, this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addOnConnectionFailedListener(this)
                .build();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                TripUtils.confirmSignedIn(getActivity(), user);
            }
        };
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference(
                mFirebaseAuth.getUid())
                .child(getString(R.string.trip_details_db_title))
                .child(mTripId)
                .child("fillUpList");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_fill_up, container, false);
        ButterKnife.bind(this, v);

        mFillUpToolbar.inflateMenu(R.menu.menu_item_layout);
        mFillUpToolbar.setTitle(R.string.fill_up_details);
        mFillUpToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        mFillUpToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete:
                        deleteFillUp();
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

        // Show the current date and time
        mFillUpDateTextView.setText(TripUtils.formatDateWithTime(new Date()));

        // OnClickListener for the Location field that will open the Google Places
        mFillUpLocationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findPlace();
            }
        });

        // OnClickListener for the FAB
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFillUp();
            }
        });

        // OnClickListener for the share button
        mFillUpShareFillUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, getFillUpReport());
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fill_up_report_subject));
                intent = Intent.createChooser(intent, getString(R.string.send_report));
                startActivity(intent);
            }
        });

        showExistingFillUp(mFillUp);


        return v;
    }

    // If this is a previously created Fill Up, show the details
    private void showExistingFillUp(FillUp fillUp) {

        // Send the Fill Up to the widget since it is now the most recent
        FrontierActionService.startActionUpdateFillUp(getContext(), mFillUp, mWidgetFillUpLocation);

        // Update the UI to show all the data
        if (fillUp != null) {
            getPlaceByIdAndDisplay(fillUp.getPlaceId());
            mPlaceId = fillUp.getPlaceId();
            mFillUpTripMileageField.setText(fillUp.getTripMileage());
            mFillUpPricePerGallonField.setText(TripUtils.turnStringToCash(fillUp.getPricePerGallon()));
            mFillUpTotalGallonsField.setText(fillUp.getGallons());
            mFillUpDateTextView.setText(fillUp.getDate());
            updateFillUpData(fillUp);
        }
    }

    // Creates the String that is the sharable Fill Up report
    private String getFillUpReport() {
        return getString(R.string.fill_up_report,
                mFillUp.getGallons(),
                mFillUp.getDate(),
                mFillUp.getPricePerGallon(),
                TripUtils.getTotalCost(mFillUp),
                mFillUpLocationTextView.getText());
    }

    // Use the Google Place Widget to select the location of the Fill Up
    public void findPlace() {
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .build(getActivity());
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    // A place has been received; use requestCode to track the request.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(getActivity(), data);
                mFillUpLocationTextView.setText(place.getName());
                mFillUpAddressTextView.setText(place.getAddress());
                mPlaceId = place.getId();
                mWidgetFillUpLocation = place.getName().toString();
                Log.i(TAG, "Place: " + place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(getActivity(), data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    // Save the Fill Up to the Firebase Realtime Database
    private void saveFillUp() {
        Date date = new Date();

        FillUp fillUp = new FillUp();

        // Id of FillUp
        String key;
        if (mFillUp.getId() == null) {
            key = mDatabaseReference.push().getKey();
        } else {
            key = mFillUp.getId();
        }
        fillUp.setId(key);

        // Date of FillUp
        fillUp.setDate(TripUtils.formatDateWithTime(date));
        mFillUpDateTextView.setText(fillUp.getDate());

        // Place Id
        if (mPlaceId != null) {
            fillUp.setPlaceId(mPlaceId);
        } else {
            FancyToast.makeText(getContext(), getString(R.string.no_location_selected), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
            return;
        }
        // Trip Mileage
        if (mFillUpTripMileageField.getText().toString().isEmpty()) {
            FancyToast.makeText(getContext(), getString(R.string.mileage_not_entered), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
            return;
        } else {
            fillUp.setTripMileage(mFillUpTripMileageField.getText().toString());
        }
        // Price Per Gallon
        if (mFillUpPricePerGallonField.getText().toString().isEmpty()) {
            FancyToast.makeText(getContext(), getString(R.string.price_per_gallon_not_entered), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
            return;
        } else {
            fillUp.setPricePerGallon(mFillUpPricePerGallonField.getText().toString().replace("$", ""));
        }
        // Total Gallons
        if (mFillUpTotalGallonsField.getText().toString().isEmpty()) {
            FancyToast.makeText(getContext(), getString(R.string.gallons_not_entered), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
            return;
        } else {
            fillUp.setGallons(mFillUpTotalGallonsField.getText().toString());
        }

        updateFillUpData(fillUp);
        mFillUp = fillUp;

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(key, fillUp.toFirebaseObject());
        mDatabaseReference.updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    FancyToast.makeText(getContext(), getString(R.string.save_successful), FancyToast.LENGTH_LONG, FancyToast.SUCCESS, R.drawable.ic_local_gas_station_white_48dp).show();
                    getActivity().finish();
                }
            }
        });

        // Send the newly created Fill Up to the widget
        FrontierActionService.startActionUpdateFillUp(getContext(), mFillUp, mWidgetFillUpLocation);

        mWidgetFillUpLocation = null;

    }

    // Removes a Fill Up from the database
    private void deleteFillUp() {
        Query thisFillUp = mDatabaseReference.orderByChild("id").equalTo(mFillUp.getId());

        thisFillUp.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot fillUpSnapshot : dataSnapshot.getChildren()) {
                    fillUpSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });
        getActivity().finish();
    }

    // Add the place name to the Place EditText and the address to the Address TextView
    public void getPlaceByIdAndDisplay(String placeId) {
        if (placeId != null) {
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                    .setResultCallback(new ResultCallback<PlaceBuffer>() {
                        @Override
                        public void onResult(@NonNull PlaceBuffer places) {
                            if (places.getStatus().isSuccess()) {
                                Place place = places.get(0);
                                mFillUpLocationTextView.setText(place.getName());
                                mFillUpAddressTextView.setText(place.getAddress());
                                mWidgetFillUpLocation = place.getName().toString();
                            }
                        }
                    });
        } else {
            mFillUpLocationTextView.setText(getString(R.string.click_here_location));
            mFillUpAddressTextView.setText(getString(R.string.location_address));
        }

    }

    // Update the Total Cost and Miles Per Gallon fields
    public void updateFillUpData(FillUp fillUp) {
        mFillUpMilesPerGallonTextView.setText(TripUtils.milesPerGallon(fillUp));
        mFillUpTotalCostTextView.setText(TripUtils.getTotalCost(fillUp));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPlaceByIdAndDisplay(mPlaceId);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(getActivity());
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.stopAutoManage(getActivity());
            mGoogleApiClient.disconnect();
        }
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPlaceId != null) {
            outState.putString(getString(R.string.place_id_key), mPlaceId);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            getPlaceByIdAndDisplay(savedInstanceState.getString(getString(R.string.place_id_key)));
        }
    }

    /*
    *   Required interface for hosting activities
    * */
    public interface Callbacks {
        void onFillUpUpdated(FillUp fillUp);
    }
}
