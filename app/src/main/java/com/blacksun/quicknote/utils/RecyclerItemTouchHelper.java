package com.blacksun.quicknote.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.blacksun.quicknote.R;

public class RecyclerItemTouchHelper extends ItemTouchHelper.SimpleCallback {
    private RecyclerItemTouchHelperListener listener;

    public RecyclerItemTouchHelper(int dragDirs, int swipeDirs, RecyclerItemTouchHelperListener listener) {
        super(dragDirs, swipeDirs);
        this.listener = listener;

    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        listener.onSwiped(viewHolder, direction, viewHolder.getAdapterPosition());
    }

//    @Override
//    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
//        super.onSelectedChanged(viewHolder, actionState);
//    }

//    @Override
//    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
//        super.clearView(recyclerView, viewHolder);
//    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        Drawable deleteIcon = ContextCompat.getDrawable(viewHolder.itemView.getContext(), R.drawable.ic_baseline_delete_forever_24px);
        ColorDrawable colorDrawableBackground = new ColorDrawable(ContextCompat.getColor(viewHolder.itemView.getContext(), R.color.colorAccent));

        View itemView = viewHolder.itemView;

        //new text delete
        Rect bounds = new Rect();
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        //            paint.setTextAlign(Paint.Align.CENTER);
        String sDelete = itemView.getContext().getResources().getString(R.string.delete);
        paint.getTextBounds(sDelete,0, sDelete.length(), bounds);


        int iconMarginVertical = (viewHolder.itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;

        if (dX > 0) {
            colorDrawableBackground.setBounds(itemView.getLeft(), itemView.getTop(), Math.round(dX), itemView.getBottom());
            deleteIcon.setBounds(itemView.getLeft() + iconMarginVertical, itemView.getTop() + iconMarginVertical,
                    itemView.getLeft() + iconMarginVertical + deleteIcon.getIntrinsicWidth(), itemView.getBottom() - iconMarginVertical);

        } else {
            colorDrawableBackground.setBounds(itemView.getRight() + Math.round(dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
            deleteIcon.setBounds(itemView.getRight() - iconMarginVertical - deleteIcon.getIntrinsicWidth(), itemView.getTop() + iconMarginVertical,
                    itemView.getRight() - iconMarginVertical, itemView.getBottom() - iconMarginVertical);
            deleteIcon.setLevel(0);
        }

        colorDrawableBackground.draw(c);

        c.save();

        if (dX > 0) {
            c.clipRect(itemView.getLeft(), itemView.getTop(), Math.round(dX), itemView.getBottom());

            c.drawText(sDelete, itemView.getLeft() + iconMarginVertical + deleteIcon.getIntrinsicWidth(),
                    (itemView.getTop() + itemView.getBottom()) / 2 + (- itemView.getTop() + itemView.getBottom()) / 10 , paint);
        } else {
            c.clipRect(itemView.getRight() + Math.round(dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());

            c.drawText(sDelete, itemView.getRight() - iconMarginVertical - deleteIcon.getIntrinsicWidth() - bounds.width(),
                    (itemView.getTop() + itemView.getBottom()) / 2 + (- itemView.getTop() + itemView.getBottom()) / 10 , paint);
        }

        deleteIcon.draw(c);


        c.restore();


        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

//    @Override
//    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
//        super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
//    }

    public interface RecyclerItemTouchHelperListener {
        void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position);
    }
}
