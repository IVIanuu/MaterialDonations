package com.ivianuu.materialdonations;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDTintHelper;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Author IVIanuu.
 */

public class MaterialDonationsDialog extends DialogFragment implements BillingProcessor.IBillingHandler {

    private static final String TAG = MaterialDonationsDialog.class.getSimpleName();

    public static final String DIALOG_TAG = MaterialDonationsDialog.class.getCanonicalName();

    private static final String EXTRA_LICENCE_KEY = "licence_key";
    private static final String EXTRA_DONATION_IDS = "donation_ids";
    private static final String EXTRA_SORT_BY = "sort_by";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_LOADING_MESSAGE = "loading_message";
    private static final String EXTRA_THANK_YOU_MESSAGE = "thank_you_message";
    private static final String EXTRA_SHOW_THANK_YOU_MESSAGE = "show_thank_you_message";

    private BillingProcessor mBillingProcessor;

    private String mLicenceKey;
    private ArrayList<String> mDonationIds;
    private SORT mSortBy;
    private String mTitle;
    private String mLoadingMessage;
    private String mThankYouMessage;
    private boolean mShowThankYouMessage;

    private AsyncTask mDonationsLoadAsyncTask;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set variables
        Bundle bundle = getArguments();
        mLicenceKey = bundle.getString(EXTRA_LICENCE_KEY);
        mDonationIds = bundle.getStringArrayList(EXTRA_DONATION_IDS);
        mSortBy = (SORT) bundle.getSerializable(EXTRA_SORT_BY);
        mTitle = bundle.getString(EXTRA_TITLE);
        mLoadingMessage = bundle.getString(EXTRA_LOADING_MESSAGE);
        mThankYouMessage = bundle.getString(EXTRA_THANK_YOU_MESSAGE);
        mShowThankYouMessage = bundle.getBoolean(EXTRA_SHOW_THANK_YOU_MESSAGE);

        mBillingProcessor = new BillingProcessor(getContext(), mLicenceKey, this);

        Log.d(TAG, "init");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // setup view
        View customView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_material_donations, null);

        ProgressBar progressBar = (ProgressBar) customView.findViewById(R.id.progress);
        MDTintHelper.setTint(progressBar, ColorUtils.getAccentColor(getContext()));

        TextView loadingMessage = (TextView) customView.findViewById(R.id.loading_message);
        loadingMessage.setText(mLoadingMessage);

        return new MaterialDialog.Builder(getContext())
                .title(mTitle)
                .customView(customView, false)
                .build();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mBillingProcessor.release();
        Log.d(TAG, "destroying");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "on activity result");
        if (!mBillingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        Log.d(TAG, "on product purchased");
        loadDonations();

        if (mShowThankYouMessage)
            Toast.makeText(getContext(), mThankYouMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        // ignore
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Log.d(TAG, "on error " + error.toString());
    }

    @Override
    public void onBillingInitialized() {
        Log.d(TAG, "on billing initializied");
        loadDonations();
    }

    private void donate(String id) {
        Log.d(TAG, "donate " + id);
        mBillingProcessor.purchase(getActivity(), id);
    }

    private void loadDonations() {
        if (mDonationsLoadAsyncTask != null) {
            mDonationsLoadAsyncTask.cancel(false);
        }
        mDonationsLoadAsyncTask = new DonationsLoadAsyncTask(this).execute();
    }

    private static class DonationsLoadAsyncTask extends AsyncTask<Void, Void, List<SkuDetails>> {

        private final WeakReference<MaterialDonationsDialog> dialogWeakReference;

        private DonationsLoadAsyncTask(MaterialDonationsDialog dialog) {
            this.dialogWeakReference = new WeakReference<>(dialog);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MaterialDonationsDialog dialog = dialogWeakReference.get();
            if (dialog == null) return;

            View customView = ((MaterialDialog) dialog.getDialog()).getCustomView();
            customView.findViewById(R.id.progress_container).setVisibility(View.VISIBLE);
            customView.findViewById(R.id.list).setVisibility(View.GONE);
        }

        @Override
        protected List<SkuDetails> doInBackground(Void... params) {
            MaterialDonationsDialog dialog = dialogWeakReference.get();

            List<SkuDetails> objects = dialog.mBillingProcessor.getPurchaseListingDetails(dialog.mDonationIds);

            switch (dialog.mSortBy) {
                case NO_SORTING:
                    // do nothing
                    break;
                case TITLE_ASC:
                    Collections.sort(objects, new Comparator<SkuDetails>() {
                        @Override
                        public int compare(SkuDetails o1, SkuDetails o2) {
                            return o1.title.compareTo(o2.title);
                        }
                    });
                    break;
                case TITLE_DESC:
                    Collections.sort(objects, new Comparator<SkuDetails>() {
                        @Override
                        public int compare(SkuDetails o1, SkuDetails o2) {
                            return o2.title.compareTo(o1.title);
                        }
                    });
                    break;
                case PRICE_ASC:
                    Collections.sort(objects, new Comparator<SkuDetails>() {
                        @Override
                        public int compare(SkuDetails o1, SkuDetails o2) {
                            return o1.priceValue.compareTo(o2.priceValue);
                        }
                    });
                    break;
                case PRICE_DESC:
                    Collections.sort(objects, new Comparator<SkuDetails>() {
                        @Override
                        public int compare(SkuDetails o1, SkuDetails o2) {
                            return o2.priceValue.compareTo(o2.priceValue);
                        }
                    });
                    break;
            }

            if (dialog != null) {
                return objects;
            }
            cancel(false);
            return null;
        }

        @Override
        protected void onPostExecute(List<SkuDetails> skuDetails) {
            super.onPostExecute(skuDetails);
            MaterialDonationsDialog dialog = dialogWeakReference.get();
            if (dialog == null) return;

            if (skuDetails == null || skuDetails.isEmpty()) {
                dialog.dismiss();
                return;
            }

            View customView = ((MaterialDialog) dialog.getDialog()).getCustomView();
            //noinspection ConstantConditions
            customView.findViewById(R.id.progress_container).setVisibility(View.GONE);
            ListView listView = (ListView) customView.findViewById(R.id.list);
            listView.setAdapter(new DonationsAdapter(dialog, skuDetails));
            listView.setVisibility(View.VISIBLE);
        }
    }

    private static class DonationsAdapter extends ArrayAdapter<SkuDetails> {

        @LayoutRes
        private static int LAYOUT_RES_ID = R.layout.item_donation;

        MaterialDonationsDialog dialog;

        private DonationsAdapter(@NonNull MaterialDonationsDialog dialog, @NonNull List<SkuDetails> objects) {
            super(dialog.getContext(), LAYOUT_RES_ID, objects);
            this.dialog = dialog;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(LAYOUT_RES_ID, parent, false);
            }

            final SkuDetails skuDetails = getItem(position);
            ViewHolder viewHolder = new ViewHolder(convertView);

            viewHolder.title.setText(skuDetails.title.replace("(Phonograph Music Player)", "").trim());
            viewHolder.text.setText(skuDetails.description);
            viewHolder.price.setText(skuDetails.priceText);

            final boolean purchased = dialog.mBillingProcessor.isPurchased(skuDetails.productId);
            int titleTextColor = purchased ? ColorUtils.getHintColor(getContext())
                    : ColorUtils.getPrimaryTextColor(getContext());
            int contentTextColor = purchased ? titleTextColor : ColorUtils.getSecondaryTextColor(getContext());

            //noinspection ResourceAsColor
            viewHolder.title.setTextColor(titleTextColor);
            //noinspection ResourceAsColor
            viewHolder.text.setTextColor(contentTextColor);
            //noinspection ResourceAsColor
            viewHolder.price.setTextColor(titleTextColor);

            strikeThrough(viewHolder.title, purchased);
            strikeThrough(viewHolder.text, purchased);
            strikeThrough(viewHolder.price, purchased);

            convertView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return purchased;
                }
            });

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.donate(skuDetails.productId);
                }
            });

            return convertView;
        }

        private static void strikeThrough(TextView textView, boolean strikeThrough) {
            textView.setPaintFlags(strikeThrough ? textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        static class ViewHolder {
            private TextView title;
            private TextView text;
            private TextView price;

            private ViewHolder(View view) {
                title = (TextView) view.findViewById(R.id.title);
                text = (TextView) view.findViewById(R.id.text);
                price = (TextView) view.findViewById(R.id.price);
            }
        }
    }

    public static class Builder {

        private AppCompatActivity activity;

        private String licenceKey;
        private ArrayList<String> donationIds = new ArrayList<>();
        private SORT sortBy = SORT.PRICE_ASC;
        private String title;
        private String loadingMessage;
        private String thankYouMessage;
        private boolean showThankYouMessage = true;

        public Builder(AppCompatActivity activity) {
            this.activity = activity;
        }

        public Builder withLicenceKey(String licenceKey) {
            this.licenceKey = licenceKey;
            return this;
        }

        public Builder withDonationId(String donationId) {
            this.donationIds.add(donationId);
            return this;
        }

        public Builder withDonationIds(ArrayList<String> donationIds) {
            this.donationIds.addAll(donationIds);
            return this;
        }

        public Builder withDonationIds(String... donationIds) {
            Collections.addAll(this.donationIds, donationIds);
            return this;
        }

        public Builder withSortBy(SORT sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withTitleRes(int title) {
            this.title = activity.getString(title);
            return this;
        }

        public Builder withLoadingMessage(String loadingMessage) {
            this.loadingMessage = loadingMessage;
            return this;
        }

        public Builder withLoadingMessageRes(int loadingMessageRes) {
            this.loadingMessage = activity.getString(loadingMessageRes);
            return this;
        }

        public Builder withThankYouMessage(String thankYouMessage) {
            this.thankYouMessage = thankYouMessage;
            return this;
        }

        public Builder withThankYouMessageRes(int thankYouMessageRes) {
            this.thankYouMessage = activity.getString(thankYouMessageRes);
            return this;
        }

        public Builder withShowThankYouMessage(boolean showThankYouMessage) {
            this.showThankYouMessage = showThankYouMessage;
            return this;
        }

        public MaterialDonationsDialog show() {
            // check preconditions
            if (licenceKey == null) {
                throw new IllegalStateException("licence key has to be set");
            }
            if (donationIds.isEmpty()) {
                throw new IllegalStateException("donations ids has to be set and not empty");
            }

            // set default title
            if (title == null) {
                title = activity.getString(R.string.dialog_title);
            }

            // Set default loading message
            if (loadingMessage == null) {
                loadingMessage = activity.getString(R.string.loading_message);
            }

            // set default thank you message
            if (thankYouMessage == null) {
                thankYouMessage = activity.getString(R.string.thank_you_message);
            }

            // Create args
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_LICENCE_KEY, licenceKey);
            bundle.putStringArrayList(EXTRA_DONATION_IDS, donationIds);
            bundle.putSerializable(EXTRA_SORT_BY, sortBy);
            bundle.putString(EXTRA_TITLE, title);
            bundle.putString(EXTRA_LOADING_MESSAGE, loadingMessage);
            bundle.putString(EXTRA_THANK_YOU_MESSAGE, thankYouMessage);
            bundle.putBoolean(EXTRA_SHOW_THANK_YOU_MESSAGE, showThankYouMessage);

            MaterialDonationsDialog dialog = new MaterialDonationsDialog();
            dialog.setArguments(bundle);

            // show dialog
            dialog.show(activity.getSupportFragmentManager(), DIALOG_TAG);

            return dialog;
        }
    }

    public enum  SORT {
        NO_SORTING, TITLE_ASC, TITLE_DESC, PRICE_ASC, PRICE_DESC;
    }
}
