package com.android.frontier.expense;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.android.frontier.utils.TripUtils.formatDateWithTime;
import static com.android.frontier.utils.TripUtils.getCategoryImage;
import static com.android.frontier.utils.TripUtils.getCategoryInt;
import static com.android.frontier.utils.TripUtils.turnStringToCash;

public class ExpenseFragment extends Fragment
        implements GoogleApiClient.OnConnectionFailedListener,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = ExpenseFragment.class.getSimpleName();
    private static final String ARG_EXPENSE = "com.android.frontier.expense.expense";
    private static final String ARG_TRIP_ID = "com.android.frontier.expense.trip_id";
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 0;

    @BindView(R.id.expense_location)
    TextView mExpenseLocationTextView;
    @BindView(R.id.expense_item)
    EditText mExpenseItemEditText;
    @BindView(R.id.expense_price)
    EditText mExpensePriceEditText;
    @BindView(R.id.expense_date)
    TextView mExpenseDateTextView;
    @BindView(R.id.expense_category)
    Spinner mExpenseSpinner;
    @BindView(R.id.share_expense_item_button)
    ImageButton mShareExpenseButton;
    @BindView(R.id.expense_photo)
    ImageView mExpenseImageView;
    @BindView(R.id.expense_address)
    TextView mExpenseAddressTextView;
    @BindView(R.id.save_expense_fab)
    FloatingActionButton mSaveFloatingActionButton;
    @BindView(R.id.expense_toolbar)
    Toolbar mExpenseToolbar;

    private Expense mExpense;
    private String mPlaceId;
    private Callbacks mCallbacks;
    private String mTripId;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    public static ExpenseFragment newExpenseInstance(Expense expense, String tripId) {
        Bundle arg = new Bundle();
        arg.putSerializable(ARG_EXPENSE, expense);
        arg.putString(ARG_TRIP_ID, tripId);

        ExpenseFragment fragment = new ExpenseFragment();
        fragment.setArguments(arg);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mExpense = new Expense();
        mExpense = (Expense) getArguments().getSerializable(ARG_EXPENSE);
        mTripId = getArguments().getString(ARG_TRIP_ID);

        mGoogleApiClient = new GoogleApiClient
                .Builder(getContext())
                .enableAutoManage(getActivity(), 100, this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addOnConnectionFailedListener(this)
                .build();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                TripUtils.confirmSignedIn(getActivity(), user);
            }
        };
        mDatabaseReference = mFirebaseDatabase.getReference(
                mFirebaseAuth.getUid())
                .child(getString(R.string.trip_details_db_title))
                .child(mTripId)
                .child("expenseList");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_expense, container, false);
        ButterKnife.bind(this, v);

        mExpenseToolbar.inflateMenu(R.menu.menu_item_layout);
        mExpenseToolbar.setTitle(R.string.expense_details);
        mExpenseToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        mExpenseToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete:
                        deleteExpense();
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

        mExpenseDateTextView.setText(formatDateWithTime(new Date()));
        mExpenseLocationTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findPlace();
            }
        });

        mSaveFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveExpense();
            }
        });

        // Expense Spinner
        expenseSpinner();

        // If this is an already existing expense, show the details
        showExistingExpense(mExpense);

        // Expense Image
        updateImage();

        // Share Button
        updateShareButton();

        return v;
    }

    // If this is an Expense that has already been saved, show the details of the Expense
    private void showExistingExpense(Expense expense) {

        // Send the Expense to the widget
        FrontierActionService.startActionUpdateExpense(getContext(), expense);

        // Update the UI to show all the data
        if (expense != null) {
            mExpenseItemEditText.setText(expense.getItem());
            getPlaceByIdAndDisplay(expense.getPlaceId());
            mPlaceId = expense.getPlaceId();
            mExpensePriceEditText.setText(turnStringToCash(expense.getPrice()));
            mExpenseSpinner.setSelection(getCategoryInt(expense.getCategory()));
            mExpense.setCategory(expense.getCategory());
            updateImage();
            mExpenseDateTextView.setText(expense.getDate());
        }
    }

    // Only activate the share button if the required data is provided
    private void updateShareButton() {
        mShareExpenseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, getExpenseShareReport());
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.expense_item_report_subject));
                intent = Intent.createChooser(intent, getString(R.string.send_report));
                startActivity(intent);
            }
        });

    }

    // Create the message to share the Expense
    private String getExpenseShareReport() {
        return getString(R.string.expense_item_report,
                mExpense.getItem(),
                mExpense.getDate(),
                mExpense.getPrice());
    }

    // Use the Google Place API to locate the location of the Expense
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
                mExpenseLocationTextView.setText(place.getName());
                mExpenseAddressTextView.setText(place.getAddress());
                mPlaceId = place.getId();
                Log.i(TAG, "Place: " + place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(getActivity(), data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    // Save the Expense to the Firebase Realtime Database
    private void saveExpense() {
        Date date = new Date();

        Expense expense = new Expense();

        // Id of Expense
        // Update the Expense in the Database
        String key;
        if (mExpense.getId() == null) {
            key = mDatabaseReference.push().getKey();
        } else {
            key = mExpense.getId();
        }
        expense.setId(key);

        // Expense Item
        if (!mExpenseItemEditText.getText().toString().isEmpty()) {
            expense.setItem(mExpenseItemEditText.getText().toString().trim());
        } else {
            FancyToast.makeText(getContext(), getString(R.string.no_item_entered), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
            return;
        }

        // Date of Expense
        expense.setDate(formatDateWithTime(date));
        mExpenseDateTextView.setText(expense.getDate());

        // PlaceId of Expense
        if (mPlaceId != null) {
            expense.setPlaceId(mPlaceId);
        } else {
            FancyToast.makeText(getContext(), getString(R.string.no_location_selected), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
            return;
        }

        // Price of Expense
        if (!mExpensePriceEditText.getText().toString().isEmpty()) {
            expense.setPrice(mExpensePriceEditText.getText().toString().trim().replace("$", ""));
        } else {
            FancyToast.makeText(getContext(), getString(R.string.price_not_entered), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
            return;
        }

        expense.setCategory(mExpense.getCategory());

        mExpense = expense;

        // Send the newly created Expense to the widget
        FrontierActionService.startActionUpdateExpense(getContext(), mExpense);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(key, expense.toFirebaseObject());
        mDatabaseReference.updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                FancyToast.makeText(getContext(), getString(R.string.expense_successfully_saved), FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                getActivity().finish();
            }
        });

    }

    // Delete the Expense from the Database
    private void deleteExpense() {
        Query thisExpense = mDatabaseReference.orderByChild("id").equalTo(mExpense.getId());

        thisExpense.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot expenseSnapshot : dataSnapshot.getChildren()) {
                    expenseSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });

        getActivity().finish();
    }

    // Using the PlaceId, determine the name of the Location and Address
    public void getPlaceByIdAndDisplay(final String placeId) {
        if (placeId != null) {
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                    .setResultCallback(new ResultCallback<PlaceBuffer>() {
                        @Override
                        public void onResult(@NonNull PlaceBuffer places) {
                            if (places.getStatus().isSuccess()) {
                                Place place = places.get(0);
                                mExpenseLocationTextView.setText(place.getName());
                                mExpenseAddressTextView.setText(place.getAddress());
                            }
                        }
                    });
        }
    }

    // Change the Image that shows on the top left depending on the category that is currently selected
    public void updateImage() {
        mExpenseImageView.setImageResource(getCategoryImage(mExpense.getCategory()));
    }

    // Set up the Spinner to select Expense category
    private void expenseSpinner() {
        // Spinner click listener
        mExpenseSpinner.setOnItemSelectedListener(this);

        // Spinner drop down elements
        List<String> categories = new ArrayList<>();
        categories.add(getString(R.string.automotive));
        categories.add(getString(R.string.lodging));
        categories.add(getString(R.string.meals));
        categories.add(getString(R.string.snacks));
        categories.add(getString(R.string.parking));
        categories.add(getString(R.string.entertainment));

        // Creating adapter for spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mExpenseSpinner.setAdapter(spinnerAdapter);
    }

    // When a spinner option is selected
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();
        // Showing selected spinner item
        mExpense.setCategory(item);
        updateImage();
        Snackbar.make(getView(), item, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_layout, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                AuthUI.getInstance().signOut(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        getPlaceByIdAndDisplay(mExpense.getPlaceId());
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
    public void onNothingSelected(AdapterView<?> parent) {
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
    *  Required interface for hosting activities
    * */
    public interface Callbacks {
        void onExpenseUpdated(Expense expense);
    }
}
