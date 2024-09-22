package com.example.finalthesis.PestandDisease;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalthesis.R;

import java.util.List;

public class ImageDiseaseAdapter extends RecyclerView.Adapter<ImageDiseaseAdapter.PlantViewHolder> {
    private Context mContext;
    private List<ImageDiseaseData> mPlantList;

    public ImageDiseaseAdapter(Context context, List<ImageDiseaseData> plantList) {
        mContext = context;
        mPlantList = plantList;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_pest_image, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        ImageDiseaseData plantData = mPlantList.get(position);
        Glide.with(mContext).load(plantData.getdiseaseImages()).into(holder.imageView);

    }

    @Override
    public int getItemCount() {
        return mPlantList.size();
    }

    public void searchDataList(List<ImageDiseaseData> searchList) {
        mPlantList = searchList;
        notifyDataSetChanged();
    }

    public class PlantViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
        }
    }
}
