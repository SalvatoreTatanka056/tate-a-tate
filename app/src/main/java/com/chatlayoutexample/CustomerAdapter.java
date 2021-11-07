package com.chatlayoutexample;


import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class CustomAdapter extends ArrayAdapter<Contatto> {

    public CustomAdapter(Context context, int textViewResourceId,
                         List<Contatto> objects) {
        super(context, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getViewOptimize(position, convertView, parent);
    }

    public View getViewOptimize(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_utenti, null);
            viewHolder = new ViewHolder();
            viewHolder.name = (TextView)convertView.findViewById(R.id.textViewName);
            viewHolder.number = (TextView)convertView.findViewById(R.id.textViewNumber);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Contatto contatto = getItem(position);
        viewHolder.name.setText(contatto.getNome()+" "+contatto.getCognome());
        viewHolder.number.setText(contatto.getTelefono());
        return convertView;
    }

    private class ViewHolder {
        public TextView name;
        public TextView number;
    }
}