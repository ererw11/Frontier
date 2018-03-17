package com.android.frontier.expense;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.frontier.R;
import com.android.frontier.utils.SignInActivity;
import com.android.frontier.utils.TripUtils;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ExpenseListFragment extends Fragment {

    private static final String TAG = ExpenseListFragment.class.getSimpleName();
    private static final String ARG_TRIP_ID = "com.android.frontier.expense.trip_id";

    @BindView(R.id.expense_recycler_view)
    RecyclerView mExpenseRecyclerView;
    @BindView(R.id.add_expense_fab)
    FloatingActionButton mFloatingActionButton;
    @BindView(R.id.expense_list_toolbar)
    Toolbar mExpenseListToolbar;
    @BindView(R.id.banner_ad_view_expense)
    AdView mExpenseAdView;
    @BindView(R.id.expense_list_no_logs_linear_layout)
    LinearLayout mNoExpenseLoggedLinearLayout;

    ArrayList<Expense> expenseList;
    private String mTripID;
    private ExpenseAdapter mAdapter;
    private Callbacks mCallbacks;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    public static ExpenseListFragment newInstance(String tripId) {
        Bundle arg = new Bundle();
        arg.putString(ARG_TRIP_ID, tripId);

        ExpenseListFragment fragment = new ExpenseListFragment();
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

        expenseList = new ArrayList<>();
        mTripID = getArguments().getString(ARG_TRIP_ID);
        // TODO - remove Log
        Log.i(TAG, mTripID);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference(mFirebaseAuth.getUid())
                .child(getString(R.string.trip_details_db_title))
                .child(mTripID)
                .child("expenseList");
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
        View view = inflater.inflate(R.layout.fragment_expense_list, container, false);
        ButterKnife.bind(this, view);

        mExpenseListToolbar.inflateMenu(R.menu.menu_list_layout);
        mExpenseListToolbar.setTitle(R.string.expenses);
        mExpenseListToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        mExpenseListToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
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
        mExpenseRecyclerView.setLayoutManager(layoutManager);
        mExpenseRecyclerView.setHasFixedSize(true);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Expense expense = new Expense();
                expense.setCategory(getString(R.string.automotive));
                updateUI();
                mCallbacks.onExpenseSelected(expense, mTripID);
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
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        mCallbacks = null;
    }

    // Update the list of Expenses
    public void updateUI() {
        expenseList.clear();
        retrieveExpenses();

        if (mAdapter == null) {
            mAdapter = new ExpenseAdapter();
            mExpenseRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    // Set up AdView
    private void buildAd() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mExpenseAdView.loadAd(adRequest);
    }

    // Retrieve the Expenses from the database
    private void retrieveExpenses() {
        mDatabaseReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        expenseList.clear();

                        Log.w(TAG, "count = " + String.valueOf(dataSnapshot.getChildrenCount()) + "expenses " + dataSnapshot.getKey());
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            Expense expense = data.getValue(Expense.class);
                            expenseList.add(expense);
                            if (expenseList.size() != 0) {
                                mNoExpenseLoggedLinearLayout.setVisibility(View.INVISIBLE);
                            } else {
                                mNoExpenseLoggedLinearLayout.setVisibility(View.VISIBLE);
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

    /*
    *   Required interface for hosting activities
    * */
    public interface Callbacks {
        void onExpenseSelected(Expense expense, String tripId);
    }

    private class ExpenseHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public int position;
        private Expense mExpense;
        private TextView mItemTextView;
        private TextView mCategoryTextView;
        private TextView mPriceTextView;
        private ImageView mCategoryImageView;

        public ExpenseHolder(View itemView) {
            super(itemView);
            mItemTextView = itemView.findViewById(R.id.expense_item_list_item);
            mCategoryTextView = itemView.findViewById(R.id.expense_category_list_item);
            mPriceTextView = itemView.findViewById(R.id.expense_price_list_item);
            mCategoryImageView = itemView.findViewById(R.id.expense_image_list_item);
            itemView.setOnClickListener(this);
        }

        public void bind(Expense expense) {
            mExpense = expense;
            mItemTextView.setText(mExpense.getItem());
            NumberFormat format = NumberFormat.getCurrencyInstance();
            mPriceTextView.setText(format.format(Double.parseDouble(mExpense.getPrice())));
            mCategoryTextView.setText(mExpense.getCategory());
            mCategoryImageView.setImageResource(TripUtils.getCategoryImage(mExpense.getCategory()));
        }

        @Override
        public void onClick(View v) {
            mCallbacks.onExpenseSelected(expenseList.get(position), mTripID);
        }
    }

    private class ExpenseAdapter extends RecyclerView.Adapter<ExpenseHolder> {

        @Override
        public ExpenseHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_expense, parent, false);
            return new ExpenseHolder(v);
        }

        @Override
        public void onBindViewHolder(ExpenseHolder holder, int position) {
            holder.position = position;
            Expense expense = expenseList.get(position);
            holder.bind(expense);
        }

        @Override
        public int getItemCount() {
            if (expenseList == null) {
                return 0;
            }
            return expenseList.size();
        }
    }
}