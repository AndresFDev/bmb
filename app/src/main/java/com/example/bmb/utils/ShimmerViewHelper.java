package com.example.bmb.utils;

import android.view.View;

import com.facebook.shimmer.ShimmerFrameLayout;

public class ShimmerViewHelper {

    private ShimmerFrameLayout shimmerFrameLayout;

    public ShimmerViewHelper(ShimmerFrameLayout shimmerFrameLayout) {
        this.shimmerFrameLayout = shimmerFrameLayout;
    }

    public void startShimmer() {
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.startShimmer();
    }

    public void stopShimmer() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
    }
}
