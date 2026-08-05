package org.kde.necessitas.mucephi.android_xcas.adapteroperations;

import android.graphics.Bitmap;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.kde.necessitas.mucephi.android_xcas.Plot3DView;
import org.kde.necessitas.mucephi.android_xcas.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by leonel on 1/11/17.
 */

public class AdapterOperations extends RecyclerView.Adapter<AdapterOperations.ViewHolder> {

    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_3D = 1;

    private List<HolderOperation> mDataset = new ArrayList<HolderOperation>();
    private InputListener inputListener;
    private View.OnCreateContextMenuListener menuListener;
    private ChangeListener changeListener;

    public interface ChangeListener {
        void onDatasetChanged();
    }

    public void setChangeListener(ChangeListener listener) {
        this.changeListener = listener;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView imgInput;
        public ImageView imgOutput;

        public ViewHolder(View v) {
            super(v);
            imgInput = v.findViewById(R.id.img_input);
            imgOutput = v.findViewById(R.id.img_ouput);
        }
    }

    public static class ViewHolder3D extends ViewHolder {

        public Plot3DView plot3DOutput;

        public ViewHolder3D(View v) {
            super(v);
            plot3DOutput = v.findViewById(R.id.plot3d_output);
        }
    }

    public interface InputListener{

        public void onInputClick(String result);
        public void onOutputClick(String result);
        public void onInputLongClick(View v, int position);
        public void onOutputLongClick(View v, int position);
    }

    public AdapterOperations(List<HolderOperation> myDataset, InputListener inputListener, View.OnCreateContextMenuListener menuListener) {
        mDataset = myDataset;
        this.inputListener = inputListener;
        this.menuListener = menuListener;
    }


    @Override
    public int getItemViewType(int position) {
        HolderOperation op = mDataset.get(position);
        if (op != null && op.getPlot3DData() != null) {
            return VIEW_TYPE_3D;
        }
        return VIEW_TYPE_NORMAL;
    }

    @Override
    public AdapterOperations.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == VIEW_TYPE_3D) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_prettyprint_3d, parent, false);
            return new ViewHolder3D(v);
        }

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_prettyprint_operation, parent, false);


        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        if (holder instanceof ViewHolder3D) {
            bind3D((ViewHolder3D) holder, position);
            return;
        }

        Bitmap bitmapInput = mDataset.get(position).getBmpInput();

        if(bitmapInput != null) {
            holder.imgInput.setImageBitmap(bitmapInput);
        }

        Bitmap bitmapOutput = mDataset.get(position).getBmpOutput();

        if(bitmapOutput != null) {
            holder.imgOutput.setImageBitmap(bitmapOutput);
        }

        holder.imgInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    inputListener.onInputClick(mDataset.get(pos).getStrInput());
                }
            }
        });

        holder.imgOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    inputListener.onOutputClick(mDataset.get(pos).getStrOutput());
                }
            }
        });

        holder.imgInput.setOnCreateContextMenuListener(menuListener);
        holder.imgOutput.setOnCreateContextMenuListener(menuListener);

        holder.imgInput.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    inputListener.onInputLongClick(v, pos);
                }
                return true;
            }
        });

        holder.imgOutput.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    inputListener.onOutputLongClick(v, pos);
                }
                return true;
            }
        });

    }

    private void bind3D(final ViewHolder3D holder, final int position) {
        HolderOperation op = mDataset.get(position);

        Bitmap bitmapInput = op.getBmpInput();
        if (bitmapInput != null) {
            holder.imgInput.setImageBitmap(bitmapInput);
        }

        holder.plot3DOutput.setPlotData(op.getPlot3DData());

        holder.imgInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    inputListener.onInputClick(mDataset.get(pos).getStrInput());
                }
            }
        });

        holder.plot3DOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    inputListener.onOutputClick(mDataset.get(pos).getStrOutput());
                }
            }
        });

        holder.imgInput.setOnCreateContextMenuListener(menuListener);
        holder.plot3DOutput.setOnCreateContextMenuListener(menuListener);

        holder.imgInput.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    inputListener.onInputLongClick(v, pos);
                }
                return true;
            }
        });

        holder.plot3DOutput.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    inputListener.onOutputLongClick(v, pos);
                }
                return true;
            }
        });
    }

    public HolderOperation remove(int position){
        if (position < 0 || position >= mDataset.size()) {
            return null;
        }
        HolderOperation removed = mDataset.remove(position);
        notifyDataSetChanged();
        if (changeListener != null) {
            changeListener.onDatasetChanged();
        }
        return removed;
    }

    public void insert(int position, HolderOperation op){
        if (op == null) {
            return;
        }
        if (position < 0 || position > mDataset.size()) {
            position = mDataset.size();
        }
        mDataset.add(position, op);
        notifyDataSetChanged();
        if (changeListener != null) {
            changeListener.onDatasetChanged();
        }
    }

    public void swap(int i, int j){
        Collections.swap(mDataset, i, j);
        notifyItemMoved(i, j);
        if (changeListener != null) {
            changeListener.onDatasetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public HolderOperation getItem(int position){
        return mDataset.get(position);
    }
}

