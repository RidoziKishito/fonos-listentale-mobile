package com.example.fonoss;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;

public class DownloadHistoryDialog extends BottomSheetDialogFragment {

    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_download_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_download_history);
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);

        WorkManager.getInstance(requireContext())
                .getWorkInfosByTagLiveData("DOWNLOAD_QUEUE")
                .observe(getViewLifecycleOwner(), workInfos -> {
                    if (workInfos != null) {
                        adapter.updateList(workInfos);
                    }
                });
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<WorkInfo> list = new ArrayList<>();

        public void updateList(List<WorkInfo> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_history, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            WorkInfo info = list.get(position);
            String title = info.getProgress().getString("TITLE");
            if (title == null) title = info.getOutputData().getString("TITLE");
            
            // Try to get TITLE from initial input data if progress/output is not available yet
            // Wait, WorkInfo doesn't expose input data easily unless it's in progress/output.
            // I should have put it in progress immediately.
            
            if (title == null) title = "Loading...";

            holder.textTitle.setText(title);
            int progress = info.getProgress().getInt("PROGRESS", 0);
            holder.progressBar.setProgress(progress);

            String status = info.getState().name();
            if (info.getState() == WorkInfo.State.RUNNING) status = progress + "%";
            else if (info.getState() == WorkInfo.State.ENQUEUED) status = "Waiting...";
            
            holder.textStatus.setText(status);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textTitle, textStatus;
            ProgressBar progressBar;
            ViewHolder(View v) {
                super(v);
                textTitle = v.findViewById(R.id.text_download_title);
                textStatus = v.findViewById(R.id.text_download_status);
                progressBar = v.findViewById(R.id.progress_download_worker);
            }
        }
    }
}