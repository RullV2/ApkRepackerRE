package com.mrikso.apkrepacker.fragment.dialogs.bottomsheet;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mrikso.apkrepacker.R;

public class ProjectFileOptionDialog extends BottomSheetDialogFragment implements  View.OnClickListener{

    public static final String TAG = "ProjectFileOptionDialogFragment";

    private ItemClickListener mListener;

    public static ProjectFileOptionDialog newInstance() {
        return new ProjectFileOptionDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_project_file_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.create_class_file).setOnClickListener(this);
        view.findViewById(R.id.create_xml_file).setOnClickListener(this);
        view.findViewById(R.id.add_new_folder).setOnClickListener(this);
        view.findViewById(R.id.select_file).setOnClickListener(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Fragment parent= getParentFragment();
        if(parent!=null){
            mListener = (ItemClickListener) parent;
        }else{
            mListener = (ItemClickListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        mListener.onFileItemClick(view.getId());
        dismiss();
    }

    public interface ItemClickListener {
        void onFileItemClick(Integer item);
    }
}
