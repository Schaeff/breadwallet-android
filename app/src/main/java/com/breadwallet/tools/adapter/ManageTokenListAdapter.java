package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.animation.ItemTouchHelperAdapter;
import com.breadwallet.tools.animation.ItemTouchHelperViewHolder;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;

public class ManageTokenListAdapter extends RecyclerView.Adapter<ManageTokenListAdapter.ManageTokenItemViewHolder> implements ItemTouchHelperAdapter {

    private static final String TAG = ManageTokenListAdapter.class.getSimpleName();
    private Context mContext;
    private ArrayList<TokenItem> mTokens;
    private OnTokenShowOrHideListener mListener;

    public interface OnTokenShowOrHideListener{

        void onShowToken(TokenItem item);
        void onHideToken(TokenItem item);
    }

    public ManageTokenListAdapter(Context context, ArrayList<TokenItem> tokens, OnTokenShowOrHideListener listener){
        this.mContext = context;
        this.mTokens = tokens;
        this.mListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull final ManageTokenListAdapter.ManageTokenItemViewHolder holder, int position) {

        final TokenItem item = mTokens.get(position);
        String tickerName = item.symbol.toLowerCase();

        if (tickerName.equals("1st")) {
            tickerName = "first";
        }


        String iconResourceName = tickerName;
        int iconResourceId = mContext.getResources().getIdentifier(tickerName, "drawable", mContext.getPackageName());

        holder.tokenName.setText(mTokens.get(position).name);
        holder.tokenTicker.setText(mTokens.get(position).symbol);
        try {
            holder.tokenIcon.setBackground(mContext.getDrawable(iconResourceId));
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Error finding icon for -> " + iconResourceName);
        }

        if(KVStoreManager.getInstance().getTokenListMetaData(mContext).isCurrencyEnabled(item.symbol)){
            Log.d(TAG, "Currency is HIDDEN");
            holder.showHide.setBackground(mContext.getDrawable(R.drawable.remove_wallet_button));
            holder.showHide.setText("Hide");
            holder.showHide.setTextColor(mContext.getColor(R.color.red));
        }else{
            Log.d(TAG, "Currency is SHOWING");
            holder.showHide.setBackground(mContext.getDrawable(R.drawable.add_wallet_button));
            holder.showHide.setText("Show");
            holder.showHide.setTextColor(mContext.getColor(R.color.dialog_button_positive));

        }


            holder.showHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //First, check if this token is hidden on HomeScreen
                if(!KVStoreManager.getInstance().getTokenListMetaData(mContext).isCurrencyEnabled(item.symbol)){
                    holder.showHide.setText("Show");
                    holder.showHide.setTextColor(mContext.getColor(R.color.dialog_button_positive));
                    holder.showHide.setBackground(mContext.getDrawable(R.drawable.add_wallet_button));
                    mListener.onShowToken(item);
                    Log.d(TAG, "SHOWING token");

                }else{
                    holder.showHide.setText("Hide");
                    holder.showHide.setTextColor(mContext.getColor(R.color.red));
                    holder.showHide.setBackground(mContext.getDrawable(R.drawable.remove_wallet_button));
                    mListener.onHideToken(item);
                    Log.d(TAG, "HIDING token");


                }
            }
        });

    }

    @NonNull
    @Override
    public ManageTokenListAdapter.ManageTokenItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.manage_wallets_list_item, parent, false);
        return new ManageTokenItemViewHolder(convertView);
    }

    @Override
    public int getItemCount() {
        return mTokens.size();
    }


    public class ManageTokenItemViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder{

        private ImageButton dragHandle;
        private BRText tokenTicker;
        private BRText tokenName;
        private BRText tokenBalance;
        private Button showHide;
        private ImageView tokenIcon;

        public ManageTokenItemViewHolder(View view) {
            super(view);

            dragHandle = view.findViewById(R.id.drag_icon);
            tokenTicker = view.findViewById(R.id.token_ticker);
            tokenName = view.findViewById(R.id.token_name);
            tokenBalance = view.findViewById(R.id.token_balance);
            showHide = view.findViewById(R.id.show_hide_button);
            tokenIcon = view.findViewById(R.id.token_icon);
        }

        @Override
        public void onItemClear() {

        }

        @Override
        public void onItemSelected() {

        }
    }


    @Override
    public void onItemDismiss(int position) {

    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        TokenItem item = mTokens.remove(fromPosition);
        mTokens.add(toPosition > fromPosition ? toPosition - 1 : toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }
}
