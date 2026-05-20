package com.hm.synapse;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<SynapseBlockEntity> blocks;
    private int stalenessThreshold = 5;
    private final OnBlockInteractionListener listener;

    public interface OnBlockInteractionListener {
        void onFinanceEdit(SynapseBlockEntity block, FinanceViewHolder holder);
        void onContentChanged(SynapseBlockEntity block);
        void onStatusChanged(SynapseBlockEntity block);
        void onDeleteBlock(SynapseBlockEntity block);
    }

    public MainAdapter(List<SynapseBlockEntity> blocks, OnBlockInteractionListener listener) {
        this.blocks = blocks;
        this.listener = listener;
    }

    public void setStalenessThreshold(int threshold) {
        this.stalenessThreshold = threshold;
    }

    @Override
    public int getItemViewType(int position) {
        String type = blocks.get(position).getType();
        switch (type) {
            case "HEADER": return 0;
            case "TODO": return 1;
            case "FINANCE": return 2;
            case "AI_SUGGESTION": return 3;
            default: return 4;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case 0: return new HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false));
            case 1: return new TodoViewHolder(inflater.inflate(R.layout.item_todo, parent, false));
            case 2: return new FinanceViewHolder(inflater.inflate(R.layout.item_finance, parent, false));
            case 3: return new AIViewHolder(inflater.inflate(R.layout.item_ai_suggestion, parent, false));
            default: return new TextViewHolder(inflater.inflate(R.layout.item_text, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SynapseBlockEntity block = blocks.get(position);
        applyStalenessHighlighting(holder.itemView, block);

        if (holder instanceof HeaderViewHolder) ((HeaderViewHolder) holder).bind(block, listener);
        else if (holder instanceof TodoViewHolder) ((TodoViewHolder) holder).bind(block, listener);
        else if (holder instanceof FinanceViewHolder) ((FinanceViewHolder) holder).bind(block, listener);
        else if (holder instanceof AIViewHolder) ((AIViewHolder) holder).bind(block);
        else if (holder instanceof TextViewHolder) ((TextViewHolder) holder).bind(block, listener);
    }

    private void applyStalenessHighlighting(View itemView, SynapseBlockEntity block) {
        if (block.isCompleted()) {
            itemView.setBackgroundColor(Color.TRANSPARENT);
            return;
        }
        long diff = System.currentTimeMillis() - block.getLastAccessed();
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (days >= stalenessThreshold) {
            itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.neglected_highlight));
        } else if (days >= Math.max(1, stalenessThreshold / 2)) {
            itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.stale_highlight));
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() { return blocks.size(); }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        EditText et;
        HeaderViewHolder(View v) { super(v); et = v.findViewById(R.id.et_header); }
        void bind(SynapseBlockEntity b, OnBlockInteractionListener l) {
            et.setText(b.getContent());
            et.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) { b.setContent(et.getText().toString()); l.onContentChanged(b); }
            });
        }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        EditText et;
        TextViewHolder(View v) { super(v); et = v.findViewById(R.id.et_text); }
        void bind(SynapseBlockEntity b, OnBlockInteractionListener l) {
            et.setText(b.getContent());
            et.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) { b.setContent(et.getText().toString()); l.onContentChanged(b); }
            });
        }
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        CheckBox cb; EditText et;
        TodoViewHolder(View v) { super(v); cb = v.findViewById(R.id.checkbox); et = v.findViewById(R.id.et_todo); }
        void bind(SynapseBlockEntity b, OnBlockInteractionListener l) {
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(b.isCompleted());
            et.setText(b.getContent());
            updateStrikethrough(b.isCompleted());
            
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                b.setCompleted(isChecked);
                updateStrikethrough(isChecked);
                l.onStatusChanged(b);
            });

            et.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) { b.setContent(et.getText().toString()); l.onContentChanged(b); }
            });
        }

        private void updateStrikethrough(boolean completed) {
            if (completed) {
                et.setPaintFlags(et.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                et.setTextColor(Color.GRAY);
            } else {
                et.setPaintFlags(et.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                et.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }
        }
    }

    public static class FinanceViewHolder extends RecyclerView.ViewHolder {
        TextView cat, amt;
        FinanceViewHolder(View v) { super(v); cat = v.findViewById(R.id.tv_category); amt = v.findViewById(R.id.tv_amount); }
        void bind(SynapseBlockEntity b, OnBlockInteractionListener l) {
            cat.setText(b.getContent());
            amt.setText(String.format("$%.2f", b.getAmount()));
            itemView.setOnClickListener(v -> l.onFinanceEdit(b, this));
        }
    }

    public static class AIViewHolder extends RecyclerView.ViewHolder {
        TextView tv;
        AIViewHolder(View v) { super(v); tv = v.findViewById(R.id.tv_suggestion); }
        void bind(SynapseBlockEntity b) { tv.setText(b.getContent()); }
    }
}
