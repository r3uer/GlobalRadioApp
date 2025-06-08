package com.example.globalradioapp;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class SpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacing;

    public SpacingItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.bottom = spacing;
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = spacing;
        }
        outRect.left = spacing;
        outRect.right = spacing;
    }
}