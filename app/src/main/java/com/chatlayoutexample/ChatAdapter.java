package com.chatlayoutexample;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final Context context;
    private Runnable onDataChangedListener;

    public ChatAdapter(Context context, List<ChatMessage> chatMessages) {
        this.context = context;
        this.chatMessages = chatMessages != null ? chatMessages : new ArrayList<>();
    }

    public void setOnDataChangedListener(Runnable listener) {
        this.onDataChangedListener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(20f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .start();

        ChatMessage message = chatMessages.get(position);
        boolean isMe = message.getIsme();

        holder.txtMessage.setText(message.getMessage());
        holder.txtInfo.setText(message.getDate());

        LinearLayout.LayoutParams containerParams =
                (LinearLayout.LayoutParams) holder.contentWithBackground.getLayoutParams();

        if (isMe) {
            containerParams.gravity = Gravity.END;
            holder.contentWithBackground.setBackgroundResource(R.drawable.bg_message_outgoing);
            holder.txtMessage.setTextColor(ContextCompat.getColor(context, R.color.bubble_outgoing_text));
            holder.txtInfo.setTextColor(ContextCompat.getColor(context, R.color.bubble_outgoing_time));

            LinearLayout.LayoutParams infoParams =
                    (LinearLayout.LayoutParams) holder.txtInfo.getLayoutParams();
            infoParams.gravity = Gravity.END;
            holder.txtInfo.setLayoutParams(infoParams);
        } else {
            containerParams.gravity = Gravity.START;
            holder.contentWithBackground.setBackgroundResource(R.drawable.bg_message_incoming);
            holder.txtMessage.setTextColor(ContextCompat.getColor(context, R.color.bubble_incoming_text));
            holder.txtInfo.setTextColor(ContextCompat.getColor(context, R.color.bubble_incoming_time));

            LinearLayout.LayoutParams infoParams =
                    (LinearLayout.LayoutParams) holder.txtInfo.getLayoutParams();
            infoParams.gravity = Gravity.START;
            holder.txtInfo.setLayoutParams(infoParams);
        }

        holder.contentWithBackground.setLayoutParams(containerParams);
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void add(ChatMessage message) {
        chatMessages.add(message);
        notifyItemInserted(chatMessages.size() - 1);
        notifyDataChanged();
    }

    public void add(List<ChatMessage> messages) {
        int startPos = chatMessages.size();
        chatMessages.addAll(messages);
        notifyItemRangeInserted(startPos, messages.size());
        notifyDataChanged();
    }

    public void clear() {
        int count = chatMessages.size();
        chatMessages.clear();
        notifyItemRangeRemoved(0, count);
        notifyDataChanged();
    }

    private void notifyDataChanged() {
        if (onDataChangedListener != null) {
            onDataChangedListener.run();
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public final TextView txtMessage;
        public final TextView txtInfo;
        public final LinearLayout contentWithBackground;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            contentWithBackground = itemView.findViewById(R.id.contentWithBackground);
        }
    }
}
