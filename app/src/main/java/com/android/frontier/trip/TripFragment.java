package com.android.frontier.trip;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.android.frontier.R;
import com.android.frontier.expense.Expense;
import com.android.frontier.expense.ExpenseListActivity;
import com.android.frontier.fill_up.FillUp;
import com.android.frontier.fill_up.FillUpListActivity;
import com.android.frontier.utils.SignInActivity;
import com.android.frontier.utils.TripUtils;
import com.firebase.ui.auth.AuthUI;
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

import java.text.NumberFormat;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TripFragment extends Fragment {

    private static final String TAG = TripFragment.class.getSimpleName();
    private static final String ARG_TRIP = "com.android.frontier.trip.trip";

    @BindView(R.id.trip_fragment_toolbar)
    private
    Toolbar mToolbar;
    @BindView(R.id.trip_title_edit_text)
    private
    EditText mTripTitleEditText;
    @BindView(R.id.add_gas_layout)
    private
    ConstraintLayout mFillUpLayout;
    @BindView(R.id.add_expenses_layout)
    private
    ConstraintLayout mExpenseLayout;
    @BindView(R.id.fill_up_count_text_view)
    private
    TextView mFillUpCountTextView;
    @BindView(R.id.expense_count_text_view)
    private
    TextView mExpenseCountTextView;
    @BindView(R.id.miles_driven_text_view)
    private
    TextView mMilesDrivenTextView;
    @BindView(R.id.miles_per_gallon_text_view)
    private
    TextView mMilesPerGallonTextView;
    @BindView(R.id.gas_cost_text_view)
    private
    TextView mGasCostTextView;
    @BindView(R.id.price_per_fill_up_text_view)
    private
    TextView mPricePerFillUp;
    @BindView(R.id.expense_cost_text_view)
    private
    TextView mExpenseCostTextView;
    @BindView(R.id.cost_per_expense_text_view)
    private
    TextView mCostPerExpenseTextView;

    private Trip mTrip;
    private String mTripId;
    private ArrayList<FillUp> mFillUps;
    private ArrayList<Expense> mExpenses;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mTripDatabaseReference;
    private DatabaseReference mFillUpsDatabaseReference;
    private DatabaseReference mExpenseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    public static TripFragment newInstance(Trip trip) {
        Bundle arg = new Bundle();
        arg.putSerializable(ARG_TRIP, trip);

        TripFragment fragment = new TripFragment();
        fragment.setArguments(arg);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mTrip = new Trip();
        mTrip = (Trip) getArguments().getSerializable(ARG_TRIP);

        mFillUps = new ArrayList<>();
        mExpenses = new ArrayList<>();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                TripUtils.confirmSignedIn(getActivity(), user);
            }
        };
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mTripDatabaseReference = mFirebaseDatabase.getReference(
                mFirebaseAuth.getUid())
                .child("tripKey");
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_trip, container, false);
        ButterKnife.bind(this, v);

        mToolbar.inflateMenu(R.menu.menu_trip_layout);
        mToolbar.setTitle(R.string.trip_details);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
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
                    case R.id.refresh:
                        updateUI(mFirebaseAuth.getCurrentUser());
                        return true;
                    case R.id.delete:
                        deleteTrip();
                        return true;
                    default:
                        return false;
                }
            }
        });

        // If this is an existing Trip, show the title
        if (mTrip.getTripName() != null) {
            mTripTitleEditText.setText(mTrip.getTripName());
        }

        mTripTitleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This space intentionally left blank
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTrip.setTripName(s.toString());
                mTripDatabaseReference.child(mTripId).child("tripName").setValue(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Fill Up Layout that when clicked directs the user to the FillUpListActivity
        mFillUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrip.getTripName() != null) {
                    Intent intent = FillUpListActivity.fillUpListIntent(getContext(), mTripId);
                    startActivity(intent);
                } else {
                    FancyToast.makeText(getContext(), getString(R.string.no_trip_title_fill_up), FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                }
            }
        });

        // Expense Layout that when clicked directs the user to the ExpenseListActivity
        mExpenseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrip.getTripName() != null) {
                    Intent intent = ExpenseListActivity.expenseListIntent(getContext(), mTripId);
                    startActivity(intent);
                } else {
                    FancyToast.makeText(getContext(), getString(R.string.no_trip_title_expense), FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                }
            }
        });

        // Set the TripId
        if (mTrip.getId() != null) {
            mTripId = mTrip.getId();
        } else {
            mTrip.setId(mTripDatabaseReference.push().getKey());
            mTripId = mTrip.getId();
            mTripDatabaseReference.child(mTripId).child("id").setValue(mTripId);
        }

        updateUI(mFirebaseAuth.getCurrentUser());

        return v;
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            resetUI();
            getFillUps();
            getExpenses();
        }
    }

    // Retrieve Fill Ups and update Fill Up data
    private void getFillUps() {

        mFillUpsDatabaseReference = mFirebaseDatabase.getReference(
                mFirebaseAuth.getUid())
                .child(getString(R.string.trip_details_db_title))
                .child(mTripId)
                .child("fillUpList");

        mFillUpsDatabaseReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mFillUps.clear();
                        Long fillUpCount = dataSnapshot.getChildrenCount();
                        mTrip.setNumberOfFillUps(fillUpCount.intValue());
                        if (dataSnapshot.getChildrenCount() >= 1) {
                            mFillUpCountTextView.setText(String.format(getString(R.string.fill_up_count), Long.toString(dataSnapshot.getChildrenCount())));
                        }

                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            FillUp fillUp = data.getValue(FillUp.class);
                            mFillUps.add(fillUp);
                        }

                        if (mFillUps.size() >= 1) {
                            double gasCost = 0.0;
                            double milesDriven = 0.0;
                            double totalTripMileage = 0.0;
                            double totalTripGallons = 0.0;
                            for (int i = 0; i < mFillUps.size(); i++) {
                                double totalCost = Double.parseDouble(mFillUps.get(i).getPricePerGallon()) * Double.parseDouble(mFillUps.get(i).getGallons());
                                double tripMileage = Double.parseDouble(mFillUps.get(i).getTripMileage());
                                double tripGallons = Double.parseDouble(mFillUps.get(i).getGallons());
                                gasCost = gasCost + totalCost;
                                milesDriven = milesDriven + Double.parseDouble(mFillUps.get(i).getTripMileage());
                                totalTripMileage = tripMileage + totalTripMileage;
                                totalTripGallons = tripGallons + totalTripGallons;
                            }
                            NumberFormat format = NumberFormat.getCurrencyInstance();
                            mGasCostTextView.setText(format.format(gasCost));
                            mMilesDrivenTextView.setText(String.format("%.2f", milesDriven));
                            double milesPerGallon = totalTripMileage / totalTripGallons;
                            mMilesPerGallonTextView.setText(String.format("%.2f", milesPerGallon));
                            mPricePerFillUp.setText(format.format((gasCost / mFillUps.size())));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        mTripDatabaseReference.child(mTripId).child("numberOfFillUps").setValue(mTrip.getNumberOfFillUps());

    }

    // Retrieve Expenses and update Expense data
    private void getExpenses() {
        mExpenseDatabaseReference = mFirebaseDatabase.getReference(
                mFirebaseAuth.getUid())
                .child(getString(R.string.trip_details_db_title))
                .child(mTripId)
                .child("expenseList");
        mExpenseDatabaseReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mExpenses.clear();
                        Long expenseCount = dataSnapshot.getChildrenCount();
                        mTrip.setNumberOfExpenses(expenseCount.intValue());
                        if (dataSnapshot.getChildrenCount() >= 1) {
                            mExpenseCountTextView.setText(String.format(getString(R.string.expense_count), Long.toString(dataSnapshot.getChildrenCount())));
                        }
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            Expense expense = data.getValue(Expense.class);
                            mExpenses.add(expense);
                        }

                        if (mExpenses.size() >= 1) {
                            double expenseCost = 0.0;
                            for (int i = 0; i < mExpenses.size(); i++) {
                                expenseCost = expenseCost + Double.parseDouble(mExpenses.get(i).getPrice());
                            }
                            NumberFormat format = NumberFormat.getCurrencyInstance();
                            mExpenseCostTextView.setText(format.format(expenseCost));
                            mCostPerExpenseTextView.setText(format.format((expenseCost / mExpenses.size())));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
        mTripDatabaseReference.child(mTripId).child("numberOfExpenses").setValue(mTrip.getNumberOfExpenses());
    }

    // Delete the trip from the database
    private void deleteTrip() {
        Query thisTrip = mTripDatabaseReference.child(mTripId);
        Query tripFillUps = mFillUpsDatabaseReference;
        Query tripExpenses = mExpenseDatabaseReference;

        thisTrip.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot tripSnapshot : dataSnapshot.getChildren()) {
                    tripSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });

        tripFillUps.addListenerForSingleValueEvent(new ValueEventListener() {
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

        tripExpenses.addListenerForSingleValueEvent(new ValueEventListener() {
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

//        getActivity().finish();
    }

    // Clear the FillUp and Expense data to refresh
    private void resetUI() {
        mFillUpCountTextView.setText(R.string.touch_here_to_create_a_fill_up);
        mGasCostTextView.setText(R.string.no_price_yet);
        mMilesDrivenTextView.setText(R.string.no_miles_yet);
        mMilesPerGallonTextView.setText(R.string.no_mile_per_gallon);
        mExpenseCountTextView.setText(R.string.touch_here_to_create_an_expense);
        mPricePerFillUp.setText(R.string.no_price_yet);
        mExpenseCostTextView.setText(R.string.no_price_yet);
        mCostPerExpenseTextView.setText(R.string.no_price_yet);
    }

    @Override
    public void onPause() {
        super.onPause();
        updateUI(mFirebaseAuth.getCurrentUser());
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        updateUI(mFirebaseAuth.getCurrentUser());
    }

}