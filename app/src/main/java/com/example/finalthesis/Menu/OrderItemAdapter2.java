package com.example.finalthesis.Menu;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalthesis.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class OrderItemAdapter2 extends RecyclerView.Adapter<OrderItemAdapter2.MyViewHolder> {

    Context context;
    ArrayList<OrderItem> items;

    public OrderItemAdapter2(Context context, ArrayList<OrderItem> items) {
        this.context = context;
        this.items = items;
    }


    @NonNull
    @Override
    public OrderItemAdapter2.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history2,parent,false);
        return new OrderItemAdapter2.MyViewHolder(view);
    }

    @Override
    public int getItemCount() {

        return items.size();
    }

    public void searchDataList(ArrayList<OrderItem> searchList){
        items = searchList;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemAdapter2.MyViewHolder holder, int position) {
        Glide.with(context).load(items.get(position).getImage()).into(holder.images);
        holder.titleTextView.setText(items.get(position).getTitle());
        holder.date.setText(items.get(position).getDate());

        // Make position final to ensure it's correctly captured in the click listener
        final int finalPosition = position;

        holder.history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                DatabaseReference paymentRef = databaseReference.child("Payment");

                paymentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                String textTrue = "true";
                                String paymentDate = dataSnapshot.child("paymentDate").getValue(String.class);
                                String transID = dataSnapshot.child("transactionId").getValue(String.class);
                                String totalP = dataSnapshot.child("totalPrice").getValue(String.class);
                                String received = dataSnapshot.child("recieved").getValue(String.class);
                                String itemDate = items.get(finalPosition).getDate();

                                if (paymentDate != null && paymentDate.equals(itemDate)) {
                                    if (received.equals(textTrue) ){
                                        Intent intent = new Intent(context, OrderHistory2.class);
                                        intent.putExtra("Date", paymentDate);
                                        intent.putExtra("TransID", transID);
                                        intent.putExtra("TotalP", totalP);
                                        context.startActivity(intent);
                                        return;
                                    }

                                }
                            }
                            // Toast and log if paymentDate doesn't match any itemDate
                            Log.e("PaymentDateError", "Payment date does not match or is null");
                            Toast.makeText(context, "Payment date does not match or is null", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("PaymentDateError", "Payment node does not exist");
                            Toast.makeText(context, "Payment information not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("PaymentDateError", "Error retrieving payment date", error.toException());
                    }
                });
            }
        });
    }




    public static class MyViewHolder extends RecyclerView.ViewHolder{
        ImageView images;
        TextView titleTextView,date;
        RelativeLayout history;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            images = itemView.findViewById(R.id.images_recycler_view);
            titleTextView = itemView.findViewById(R.id.title_text_view);
            date = itemView.findViewById(R.id.date);
            history = itemView.findViewById(R.id.history);
        }
    }

}