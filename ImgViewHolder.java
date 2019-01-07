package com.example.adrian.mob.viewholder;

    import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.adrian.mob.R;

    public class ImgViewHolder extends RecyclerView.ViewHolder {

        public TextView nameView;
        public ImageView imageView;

        public ImgViewHolder(View itemView) {
            super(itemView);

            nameView = (TextView) itemView.findViewById(R.id.tv_img_name);
            imageView = (ImageView) itemView.findViewById(R.id.img_view);
        }
    }
